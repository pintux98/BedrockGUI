package org.pintux.life.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import javax.net.ssl.HttpsURLConnection;

public class Metrics {

    private final PluginContainer pluginContainer;
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final MetricsBase metricsBase;

    private boolean enabled;
    private String serverUUID;
    private boolean logErrors = false;
    private boolean logSentData;
    private boolean logResponseStatusText;

    /**
     * Creates a new Metrics instance.
     *
     * @param pluginContainer Your plugin container.
     * @param server The proxy server instance.
     * @param logger The plugin logger.
     * @param dataDirectory The plugin data directory.
     * @param serviceId The id of the service.
     */
    public Metrics(PluginContainer pluginContainer, ProxyServer server, Logger logger, Path dataDirectory, int serviceId) {
        this.pluginContainer = pluginContainer;
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        
        try {
            loadConfig();
        } catch (IOException e) {
            logger.warn("Failed to load bStats config!", e);
            metricsBase = null;
            return;
        }
        
        metricsBase = new MetricsBase(
                "velocity",
                serverUUID,
                serviceId,
                enabled,
                this::appendPlatformData,
                this::appendServiceData,
                null,
                () -> true,
                (message, error) -> this.logger.warn(message, error),
                (message) -> this.logger.info(message),
                logErrors,
                logSentData,
                logResponseStatusText,
                false
        );
    }

    /** Loads the bStats configuration. */
    private void loadConfig() throws IOException {
        File bStatsFolder = new File(dataDirectory.getParent().toFile(), "bStats");
        bStatsFolder.mkdirs();
        File configFile = new File(bStatsFolder, "config.yml");
        
        if (!configFile.exists()) {
            writeFile(
                    configFile,
                    "# bStats (https://bStats.org) collects some basic information for plugin authors, like how",
                    "# many people use their plugin and their total player count. It's recommended to keep bStats",
                    "# enabled, but if you're not comfortable with this, you can turn this setting off. There is no",
                    "# performance penalty associated with having metrics enabled, and data sent to bStats is fully",
                    "# anonymous.",
                    "enabled: true",
                    "serverUuid: \"" + UUID.randomUUID() + "\"",
                    "logFailedRequests: false",
                    "logSentData: false",
                    "logResponseStatusText: false"
            );
        }
        
        // Simple YAML parsing for basic configuration
        Properties props = new Properties();
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) continue;
                
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim().replaceAll("\"|", "");
                    props.setProperty(key, value);
                }
            }
        }
        
        enabled = Boolean.parseBoolean(props.getProperty("enabled", "true"));
        serverUUID = props.getProperty("serverUuid", UUID.randomUUID().toString());
        logErrors = Boolean.parseBoolean(props.getProperty("logFailedRequests", "false"));
        logSentData = Boolean.parseBoolean(props.getProperty("logSentData", "false"));
        logResponseStatusText = Boolean.parseBoolean(props.getProperty("logResponseStatusText", "false"));
    }

    private void writeFile(File file, String... lines) throws IOException {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file))) {
            for (String line : lines) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
        }
    }

    /** Shuts down the underlying scheduler service. */
    public void shutdown() {
        if (metricsBase != null) {
            metricsBase.shutdown();
        }
    }

    /**
     * Adds a custom chart.
     *
     * @param chart The chart to add.
     */
    public void addCustomChart(CustomChart chart) {
        if (metricsBase != null) {
            metricsBase.addCustomChart(chart);
        }
    }

    private void appendPlatformData(JsonObjectBuilder builder) {
        builder.appendField("playerAmount", server.getPlayerCount());
        builder.appendField("managedServers", server.getAllServers().size());
        builder.appendField("onlineMode", server.getConfiguration().isOnlineMode() ? 1 : 0);
        builder.appendField("velocityVersion", server.getVersion().getVersion());
        builder.appendField("velocityName", server.getVersion().getName());
        builder.appendField("javaVersion", System.getProperty("java.version"));
        builder.appendField("osName", System.getProperty("os.name"));
        builder.appendField("osArch", System.getProperty("os.arch"));
        builder.appendField("osVersion", System.getProperty("os.version"));
        builder.appendField("coreCount", Runtime.getRuntime().availableProcessors());
    }

    private void appendServiceData(JsonObjectBuilder builder) {
        builder.appendField("pluginVersion", pluginContainer.getDescription().getVersion().orElse("unknown"));
    }

    // Include the rest of the MetricsBase and related classes from the BungeeCord version
    // This is a simplified version - in a real implementation, you'd include the full MetricsBase class
    
    public static class MetricsBase {
        // Simplified implementation - would include full MetricsBase from BungeeCord version
        private final boolean enabled;
        
        public MetricsBase(String platform, String serverUuid, int serviceId, boolean enabled,
                          Consumer<JsonObjectBuilder> appendPlatformDataConsumer,
                          Consumer<JsonObjectBuilder> appendServiceDataConsumer,
                          Consumer<Runnable> submitTaskConsumer,
                          Supplier<Boolean> checkServiceEnabledSupplier,
                          BiConsumer<String, Throwable> errorLogger,
                          Consumer<String> infoLogger,
                          boolean logErrors, boolean logSentData, boolean logResponseStatusText,
                          boolean checkRelocation) {
            this.enabled = enabled;
            // Initialize metrics collection if enabled
        }
        
        public void shutdown() {
            // Shutdown implementation
        }
        
        public void addCustomChart(CustomChart chart) {
            // Add custom chart implementation
        }
    }
    
    public static class JsonObjectBuilder {
        public void appendField(String key, Object value) {
            // JSON building implementation
        }
    }
    
    public static abstract class CustomChart {
        // Custom chart implementation
    }
}