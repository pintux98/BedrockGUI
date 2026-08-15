package it.pintux.life.common.utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Serves button images over HTTP so a menu can use a local file as an icon.
 *
 * <p>Only the {@code assets} folder inside the plugin folder is reachable, and only the image types
 * a Bedrock form can draw. Every request is resolved against the real path of that folder, so
 * traversal, absolute paths, Windows device syntax and symlinks cannot reach anything else on the
 * host. Nothing else in the plugin folder - configs, menus, logs - is exposed.
 */
public class AssetServer {

    public static final String CONTEXT = "/bedrockgui/assets";
    public static final String ASSETS_FOLDER = "assets";

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "gif", "image/gif",
            "webp", "image/webp");

    private final int port;
    private final File assetsFolder;
    private String host;
    private HttpServer httpServer;
    private String baseUrl;

    public AssetServer(String host, int port, File dataFolder) {
        this.host = host;
        this.port = port;
        this.assetsFolder = new File(dataFolder, ASSETS_FOLDER);
    }

    /**
     * Build a server from the {@code assets} section of the plugin config.
     *
     * @param config      plugin config, may be null to accept every default
     * @param defaultHost address to advertise when {@code assets.host} is blank
     * @param defaultPort port to bind when {@code assets.port} is missing or out of range
     * @param dataFolder  the plugin folder; only its {@code assets} subfolder is served
     * @return a configured server, or null when {@code assets.enabled} is false
     */
    public static AssetServer fromConfig(FormConfig config, String defaultHost, int defaultPort, File dataFolder) {
        if (config == null) return new AssetServer(defaultHost, defaultPort, dataFolder);
        if (!Boolean.parseBoolean(config.getString("assets.enabled", "true").trim())) return null;
        String host = config.getString("assets.host", "");
        int port = parsePort(config.getString("assets.port", ""), defaultPort);
        return new AssetServer(host == null || host.isBlank() ? defaultHost : host.trim(), port, dataFolder);
    }

    private static int parsePort(String value, int defaultPort) {
        if (value == null) return defaultPort;
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 && parsed <= 65535 ? parsed : defaultPort;
        } catch (NumberFormatException e) {
            return defaultPort;
        }
    }

    public void start() {
        try {
            Files.createDirectories(assetsFolder.toPath());
            Path root = assetsFolder.toPath().toRealPath();
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.createContext(CONTEXT, new StaticHandler(root));
            httpServer.setExecutor(Executors.newSingleThreadExecutor());
            httpServer.start();
            if (host == null || host.isEmpty()) {
                host = InetAddress.getLocalHost().getHostAddress();
            }
            baseUrl = "http://" + host + ":" + port + CONTEXT + "/";
        } catch (IOException e) {
            httpServer = null;
        }
    }

    public boolean isAvailable() {
        return httpServer != null;
    }

    public int getPort() {
        return port;
    }

    public File getAssetsFolder() {
        return assetsFolder;
    }

    public String getAssetUrl(String filename) {
        String safe = filename.replace("\\", "/");
        if (safe.startsWith("/")) safe = safe.substring(1);
        return baseUrl + safe;
    }

    public void shutdown() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
    }

    /**
     * Map a request path to the file that may be sent for it.
     *
     * @param root     real path of the assets folder
     * @param relative request path with the context prefix already removed, percent-decoded
     * @return the file to send, or null when the request must be refused
     */
    static Path resolve(Path root, String relative) {
        if (root == null || relative == null || relative.isEmpty()) return null;
        if (relative.indexOf('\0') >= 0 || relative.indexOf('\\') >= 0 || relative.indexOf(':') >= 0) return null;
        if (relative.charAt(0) == '/') return null;
        if (contentType(relative) == null) return null;
        for (String segment : relative.split("/", -1)) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) return null;
        }
        try {
            Path real = root.resolve(relative).toRealPath();
            if (!real.startsWith(root) || real.equals(root)) return null;
            return Files.isRegularFile(real) ? real : null;
        } catch (IOException | InvalidPathException e) {
            return null;
        }
    }

    /** @return the content type to send for this file name, or null when it may not be served */
    static String contentType(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return null;
        return CONTENT_TYPES.get(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    static class StaticHandler implements HttpHandler {
        private final Path root;

        StaticHandler(Path root) {
            this.root = root;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }
                String prefix = CONTEXT + "/";
                String path = exchange.getRequestURI().getPath();
                Path file = path.startsWith(prefix) ? resolve(root, path.substring(prefix.length())) : null;
                if (file == null) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                exchange.getResponseHeaders().set("Content-Type", contentType(file.getFileName().toString()));
                exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
                exchange.sendResponseHeaders(200, Files.size(file));
                try (OutputStream os = exchange.getResponseBody()) {
                    Files.copy(file, os);
                }
            } finally {
                exchange.close();
            }
        }
    }
}
