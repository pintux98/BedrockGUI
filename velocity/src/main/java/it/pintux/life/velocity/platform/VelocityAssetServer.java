package it.pintux.life.velocity.platform;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.velocitypowered.api.proxy.ProxyServer;
import it.pintux.life.common.platform.PlatformAssetServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;

public class VelocityAssetServer implements PlatformAssetServer {
    private final ProxyServer server;
    private final File dataFolder;
    private final int port;
    private HttpServer httpServer;
    private String baseUrl;

    public VelocityAssetServer(ProxyServer server, File dataFolder, int port) {
        this.server = server;
        this.dataFolder = dataFolder;
        this.port = port;
    }

    public void start() {
        try {
            InetSocketAddress addr = new InetSocketAddress(port);
            httpServer = HttpServer.create(addr, 0);
            httpServer.createContext("/bedrockgui/assets", new StaticHandler(dataFolder));
            httpServer.setExecutor(java.util.concurrent.Executors.newSingleThreadExecutor());
            httpServer.start();
            InetSocketAddress bound = server.getBoundAddress();
            String host = bound != null ? bound.getHostString() : "127.0.0.1";
            if (host == null || host.isEmpty() || "0.0.0.0".equals(host)) {
                host = "127.0.0.1";
            }
            baseUrl = "http://" + host + ":" + port + "/bedrockgui/assets/";
        } catch (IOException e) {
            httpServer = null;
        }
    }

    @Override
    public boolean isAvailable() {
        return httpServer != null;
    }

    @Override
    public String getAssetUrl(String filename) {
        String safe = filename.replace("\\", "/");
        if (safe.startsWith("/")) safe = safe.substring(1);
        return baseUrl + safe;
    }

    @Override
    public void shutdown() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
    }

    static class StaticHandler implements HttpHandler {
        private final File root;
        StaticHandler(File root) { this.root = root; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String prefix = "/bedrockgui/assets/";
            if (!path.startsWith(prefix)) { exchange.sendResponseHeaders(404, -1); return; }
            String relative = path.substring(prefix.length());
            File target = new File(root, relative);
            if (!target.getCanonicalPath().startsWith(root.getCanonicalPath())) { exchange.sendResponseHeaders(403, -1); return; }
            if (!target.exists() || !target.isFile()) { exchange.sendResponseHeaders(404, -1); return; }
            String mime = Files.probeContentType(target.toPath());
            if (mime == null) mime = "application/octet-stream";
            exchange.getResponseHeaders().set("Content-Type", mime);
            exchange.sendResponseHeaders(200, target.length());
            try (OutputStream os = exchange.getResponseBody(); FileInputStream fis = new FileInputStream(target)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = fis.read(buf)) != -1) {
                    os.write(buf, 0, r);
                }
            }
        }
    }
}