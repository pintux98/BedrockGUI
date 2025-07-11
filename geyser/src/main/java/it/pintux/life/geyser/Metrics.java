package it.pintux.life.geyser;

import org.geysermc.geyser.api.extension.Extension;

import java.io.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import java.util.zip.GZIPOutputStream;

public class Metrics {

    private final Extension extension;
    private final MetricsBase metricsBase;

    /**
     * Creates a new Metrics instance.
     *
     * @param extension Your extension instance.
     * @param serviceId The id of the service. It can be found at <a
     *     href="https://bstats.org/what-is-my-plugin-id">https://bstats.org/what-is-my-plugin-id</a>
     */
    public Metrics(Extension extension, int serviceId) {
        this.extension = extension;

        // Get the config file
        File bStatsFolder = new File(extension.dataFolder().toFile().getParentFile(), "bStats");
        File configFile = new File(bStatsFolder, "config.txt");
        MetricsConfig config = new MetricsConfig(configFile, true);

        // Get the config values
        String serverUUID = config.getServerUUID();
        boolean logErrors = config.isLogErrorsEnabled();
        boolean logSentData = config.isLogSentDataEnabled();
        boolean logResponseStatusText = config.isLogResponseStatusTextEnabled();

        metricsBase =
                new MetricsBase(
                        "geyser",
                        serverUUID,
                        serviceId,
                        config.isEnabled(),
                        this::appendPlatformData,
                        this::appendServiceData,
                        submitDataTask -> Executors.newScheduledThreadPool(1).scheduleAtFixedRate(submitDataTask, 1L, 30L, TimeUnit.MINUTES),
                        () -> true,
                        (message, error) -> extension.logger().warning(message + (error != null ? ": " + error.getMessage() : "")),
                        (message) -> extension.logger().info(message),
                        logErrors,
                        logSentData,
                        logResponseStatusText,
                        false);
    }

    /**
     * Adds a custom chart.
     *
     * @param chart The chart to add.
     */
    public void addCustomChart(CustomChart chart) {
        metricsBase.addCustomChart(chart);
    }

    private void appendPlatformData(JsonObjectBuilder builder) {
        builder.appendField("playerAmount", 0); // Geyser extensions don't have direct player count
        builder.appendField("onlineMode", 1);
        builder.appendField("geyserVersion", "2.2.3"); // This should be dynamic
        builder.appendField("javaVersion", System.getProperty("java.version"));
        builder.appendField("osName", System.getProperty("os.name"));
        builder.appendField("osArch", System.getProperty("os.arch"));
        builder.appendField("osVersion", System.getProperty("os.version"));
        builder.appendField("coreCount", Runtime.getRuntime().availableProcessors());
    }

    private void appendServiceData(JsonObjectBuilder builder) {
        builder.appendField("extensionVersion", extension.description().version());
    }

    public static class MetricsBase {

        /** The version of the Metrics class. */
        public static final String METRICS_VERSION = "3.0.2";

        private static final ScheduledExecutorService scheduler =
                Executors.newScheduledThreadPool(1, task -> new Thread(task, "bStats-Metrics"));

        private static final String REPORT_URL = "https://bStats.org/api/v2/data/%s";

        private final String platform;
        private final String serverUuid;
        private final int serviceId;
        private final Consumer<JsonObjectBuilder> appendPlatformDataConsumer;
        private final Consumer<JsonObjectBuilder> appendServiceDataConsumer;
        private final Consumer<Runnable> submitTaskConsumer;
        private final Supplier<Boolean> checkServiceEnabledSupplier;
        private final BiConsumer<String, Throwable> errorLogger;
        private final Consumer<String> infoLogger;
        private final boolean logErrors;
        private final boolean logSentData;
        private final boolean logResponseStatusText;
        private final Set<CustomChart> customCharts = new HashSet<>();
        private final boolean enabled;

        /**
         * Creates a new MetricsBase class instance.
         *
         * @param platform The platform the plugin is running on
         * @param serverUuid The server uuid
         * @param serviceId The id of the service
         * @param enabled Whether or not data sending is enabled
         * @param appendPlatformDataConsumer A consumer that receives a {@code JsonObjectBuilder} and
         *     appends all platform-specific data
         * @param appendServiceDataConsumer A consumer that receives a {@code JsonObjectBuilder} and
         *     appends all service-specific data
         * @param submitTaskConsumer A consumer that takes a runnable with the submit task. This can be
         *     used to delegate the data collection to a another thread to prevent errors caused by
         *     concurrency. Can be {@code null}.
         * @param checkServiceEnabledSupplier A supplier to check if the service is still enabled.
         * @param errorLogger A consumer that accepts log message and an error.
         * @param infoLogger A consumer that accepts info log messages.
         * @param logErrors Whether or not errors should be logged.
         * @param logSentData Whether or not the sent data should be logged.
         * @param logResponseStatusText Whether or not the response status text should be logged.
         * @param checkRelocation Whether or not to check if the class was relocated.
         */
        public MetricsBase(
                String platform,
                String serverUuid,
                int serviceId,
                boolean enabled,
                Consumer<JsonObjectBuilder> appendPlatformDataConsumer,
                Consumer<JsonObjectBuilder> appendServiceDataConsumer,
                Consumer<Runnable> submitTaskConsumer,
                Supplier<Boolean> checkServiceEnabledSupplier,
                BiConsumer<String, Throwable> errorLogger,
                Consumer<String> infoLogger,
                boolean logErrors,
                boolean logSentData,
                boolean logResponseStatusText,
                boolean checkRelocation) {
            this.platform = platform;
            this.serverUuid = serverUuid;
            this.serviceId = serviceId;
            this.enabled = enabled;
            this.appendPlatformDataConsumer = appendPlatformDataConsumer;
            this.appendServiceDataConsumer = appendServiceDataConsumer;
            this.submitTaskConsumer = submitTaskConsumer;
            this.checkServiceEnabledSupplier = checkServiceEnabledSupplier;
            this.errorLogger = errorLogger;
            this.infoLogger = infoLogger;
            this.logErrors = logErrors;
            this.logSentData = logSentData;
            this.logResponseStatusText = logResponseStatusText;

            if (enabled) {
                startSubmitting();
            }
        }

        public void addCustomChart(CustomChart chart) {
            customCharts.add(chart);
        }

        private void startSubmitting() {
            final Runnable submitTask =
                    () -> {
                        if (!enabled || !checkServiceEnabledSupplier.get()) {
                            scheduler.shutdown();
                            return;
                        }
                        if (submitTaskConsumer != null) {
                            submitTaskConsumer.accept(this::submitData);
                        } else {
                            this.submitData();
                        }
                    };

            long initialDelay = (long) (1000 * 60 * (3 + Math.random() * 3));
            long secondDelay = (long) (1000 * 60 * (Math.random() * 30));
            scheduler.schedule(submitTask, initialDelay, TimeUnit.MILLISECONDS);
            scheduler.scheduleAtFixedRate(submitTask, initialDelay + secondDelay, 1000 * 60 * 30, TimeUnit.MILLISECONDS);
        }

        private void submitData() {
            final JsonObjectBuilder baseJsonBuilder = new JsonObjectBuilder();
            appendPlatformDataConsumer.accept(baseJsonBuilder);
            final JsonObjectBuilder serviceJsonBuilder = new JsonObjectBuilder();
            appendServiceDataConsumer.accept(serviceJsonBuilder);
            JsonObjectBuilder.JsonObject[] chartData =
                    customCharts.stream()
                            .map(customChart -> customChart.getRequestJsonObject(errorLogger, logErrors))
                            .filter(Objects::nonNull)
                            .toArray(JsonObjectBuilder.JsonObject[]::new);
            serviceJsonBuilder.appendField("id", serviceId);
            serviceJsonBuilder.appendField("customCharts", chartData);
            baseJsonBuilder.appendField("service", serviceJsonBuilder.build());
            baseJsonBuilder.appendField("serverUUID", serverUuid);
            baseJsonBuilder.appendField("metricsVersion", METRICS_VERSION);
            JsonObjectBuilder.JsonObject data = baseJsonBuilder.build();

            scheduler.execute(() -> {
                try {
                    sendData(data);
                } catch (Exception e) {
                    if (logErrors) {
                        errorLogger.accept("Could not submit bStats metrics data", e);
                    }
                }
            });
        }

        private void sendData(JsonObjectBuilder.JsonObject data) throws Exception {
            if (logSentData) {
                infoLogger.accept("Sent bStats metrics data: " + data.toString());
            }
            String url = String.format(REPORT_URL, platform);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Content-Encoding", "gzip")
                    .header("User-Agent", "Metrics-Service/1")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(compress(data.toString())))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (logResponseStatusText) {
                infoLogger.accept("Sent data to bStats and received response: " + response.statusCode());
            }
        }

        /** Gzips the given string. */
        private static byte[] compress(final String str) throws IOException {
            if (str == null) {
                return new byte[0];
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(outputStream)) {
                gzip.write(str.getBytes(StandardCharsets.UTF_8));
            }
            return outputStream.toByteArray();
        }
    }

    public static class MetricsConfig {

        private final File configFile;
        private final boolean defaultEnabled;

        public MetricsConfig(File configFile, boolean defaultEnabled) {
            this.configFile = configFile;
            this.defaultEnabled = defaultEnabled;
        }

        public boolean isEnabled() {
            return getConfigValue("enabled", defaultEnabled);
        }

        public String getServerUUID() {
            return getConfigValue("serverUuid", UUID.randomUUID().toString());
        }

        public boolean isLogErrorsEnabled() {
            return getConfigValue("logFailedRequests", false);
        }

        public boolean isLogSentDataEnabled() {
            return getConfigValue("logSentData", false);
        }

        public boolean isLogResponseStatusTextEnabled() {
            return getConfigValue("logResponseStatusText", false);
        }

        @SuppressWarnings("unchecked")
        private <T> T getConfigValue(String key, T defaultValue) {
            try {
                if (!configFile.exists()) {
                    writeConfig();
                }
                List<String> lines = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new FileReader(configFile, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                    }
                }
                for (String line : lines) {
                    String[] split = line.split("=");
                    if (split.length == 2 && split[0].equals(key)) {
                        if (defaultValue instanceof Boolean) {
                            return (T) Boolean.valueOf(split[1]);
                        } else {
                            return (T) split[1];
                        }
                    }
                }
            } catch (IOException ignored) {
            }
            return defaultValue;
        }

        private void writeConfig() throws IOException {
            configFile.getParentFile().mkdirs();
            List<String> configLines = new ArrayList<>();
            configLines.add("# bStats (https://bStats.org) collects some basic information for plugin authors, like how");
            configLines.add("# many people use their plugin and their total player count. It's recommended to keep bStats");
            configLines.add("# enabled, but if you're not comfortable with this, you can turn this setting off. There is no");
            configLines.add("# performance penalty associated with having metrics enabled, and data sent to bStats is fully");
            configLines.add("# anonymous.");
            configLines.add("enabled=" + defaultEnabled);
            configLines.add("serverUuid=" + UUID.randomUUID());
            configLines.add("logFailedRequests=false");
            configLines.add("logSentData=false");
            configLines.add("logResponseStatusText=false");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile, StandardCharsets.UTF_8))) {
                for (String line : configLines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        }
    }

    public static abstract class CustomChart {

        private final String chartId;

        protected CustomChart(String chartId) {
            if (chartId == null) {
                throw new IllegalArgumentException("chartId must not be null");
            }
            this.chartId = chartId;
        }

        public JsonObjectBuilder.JsonObject getRequestJsonObject(
                BiConsumer<String, Throwable> errorLogger, boolean logErrors) {
            JsonObjectBuilder builder = new JsonObjectBuilder();
            builder.appendField("chartId", chartId);
            try {
                JsonObjectBuilder.JsonObject data = getChartData();
                if (data == null) {
                    return null;
                }
                builder.appendField("data", data);
            } catch (Throwable t) {
                if (logErrors) {
                    errorLogger.accept("Failed to get data for custom chart with id " + chartId, t);
                }
                return null;
            }
            return builder.build();
        }

        protected abstract JsonObjectBuilder.JsonObject getChartData() throws Exception;
    }

    public static class SimplePie extends CustomChart {

        private final Callable<String> callable;

        public SimplePie(String chartId, Callable<String> callable) {
            super(chartId);
            this.callable = callable;
        }

        @Override
        protected JsonObjectBuilder.JsonObject getChartData() throws Exception {
            String value = callable.call();
            if (value == null || value.isEmpty()) {
                return null;
            }
            return new JsonObjectBuilder().appendField("value", value).build();
        }
    }

    public static class JsonObjectBuilder {

        private StringBuilder builder = new StringBuilder();
        private boolean hasAtLeastOneField = false;

        public JsonObjectBuilder() {
            builder.append("{");
        }

        public JsonObjectBuilder appendNull(String key) {
            appendFieldUnescaped(key, "null");
            return this;
        }

        public JsonObjectBuilder appendField(String key, String value) {
            if (value == null) {
                throw new IllegalArgumentException("JSON value must not be null");
            }
            appendFieldUnescaped(key, "\"" + escape(value) + "\"");
            return this;
        }

        public JsonObjectBuilder appendField(String key, int value) {
            appendFieldUnescaped(key, String.valueOf(value));
            return this;
        }

        public JsonObjectBuilder appendField(String key, JsonObject object) {
            if (object == null) {
                throw new IllegalArgumentException("JSON object must not be null");
            }
            appendFieldUnescaped(key, object.toString());
            return this;
        }

        public JsonObjectBuilder appendField(String key, JsonObject[] values) {
            if (values == null) {
                throw new IllegalArgumentException("JSON values must not be null");
            }
            String escapedValues =
                    Arrays.stream(values)
                            .map(JsonObject::toString)
                            .collect(java.util.stream.Collectors.joining(","));
            appendFieldUnescaped(key, "[" + escapedValues + "]");
            return this;
        }

        private void appendFieldUnescaped(String key, String escapedValue) {
            if (hasAtLeastOneField) {
                builder.append(",");
            }
            builder.append("\"").append(escape(key)).append("\":").append(escapedValue);
            hasAtLeastOneField = true;
        }

        public JsonObject build() {
            if (!hasAtLeastOneField) {
                throw new IllegalStateException("JSON object must have at least one field");
            }
            return new JsonObject(builder.append("}").toString());
        }

        private static String escape(String value) {
            final StringBuilder builder = new StringBuilder();
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '"') {
                    builder.append("\\\"");
                } else if (c == '\\') {
                    builder.append("\\\\");
                } else if (c <= '\u001F') {
                    builder.append("\\u").append(String.format("%04X", (int) c));
                } else {
                    builder.append(c);
                }
            }
            return builder.toString();
        }

        public static class JsonObject {

            private final String value;

            private JsonObject(String value) {
                this.value = value;
            }

            @Override
            public String toString() {
                return value;
            }
        }
    }
}