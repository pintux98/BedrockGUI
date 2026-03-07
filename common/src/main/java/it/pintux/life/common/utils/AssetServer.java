package it.pintux.life.common.utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;

public class AssetServer {
    private String host;
    private final File dataFolder;
    private final int port;
    private HttpServer httpServer;
    private String baseUrl;

    public AssetServer(String host, int port, File dataFolder) {
        this.host = host;
        this.port = port;
        this.dataFolder = dataFolder;
    }

    public void start() {
        try {
            InetSocketAddress addr = new InetSocketAddress(port);
            httpServer = HttpServer.create(addr, 0);
            httpServer.createContext("/bedrockgui/assets", new StaticHandler(dataFolder));
            httpServer.setExecutor(java.util.concurrent.Executors.newSingleThreadExecutor());
            httpServer.start();
            if (host == null || host.isEmpty()) {
                host = InetAddress.getLocalHost().getHostAddress();
            }
            baseUrl = "http://" + host + ":" + port + "/bedrockgui/assets/";
        } catch (IOException e) {
            httpServer = null;
        }
    }

    public boolean isAvailable() {
        return httpServer != null;
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

    static class StaticHandler implements HttpHandler {
        private final File root;

        StaticHandler(File root) {
            this.root = root;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String prefix = "/bedrockgui/assets/";
            if (!path.startsWith(prefix)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            String relative = path.substring(prefix.length());
            File target = new File(root, relative);
            if (!target.getCanonicalPath().startsWith(root.getCanonicalPath())) {
                exchange.sendResponseHeaders(403, -1);
                return;
            }
            if (!target.exists() || !target.isFile()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
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