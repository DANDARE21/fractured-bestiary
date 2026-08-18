package net.dandare21.fracturedutils.sound.event;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.dandare21.fracturedutils.FracturedUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

public class EventMusicHttpServer {

    private HttpServer server;
    private int port;
    private Path packZipPath;
    private boolean running = false;

    public synchronized boolean start(int port, Path packZipPath) {
        if (running) {
            stop();
        }
        this.port = port;
        this.packZipPath = packZipPath;

        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/event_music_pack.zip", new PackHandler());
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            running = true;
            FracturedUtils.LOGGER.info("[EventMusicHttpServer] Started HTTP server on port {} serving /event_music_pack.zip", port);
            return true;
        } catch (Exception e) {
            FracturedUtils.LOGGER.error("[EventMusicHttpServer] Failed to start HTTP server on port {}", port, e);
            running = false;
            return false;
        }
    }

    public synchronized void stop() {
        if (server != null) {
            try {
                server.stop(0);
                FracturedUtils.LOGGER.info("[EventMusicHttpServer] Stopped HTTP server.");
            } catch (Exception e) {
                FracturedUtils.LOGGER.warn("[EventMusicHttpServer] Error stopping HTTP server", e);
            }
            server = null;
        }
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    private class PackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
                    exchange.close();
                    return;
                }

                if (packZipPath == null || !Files.exists(packZipPath)) {
                    FracturedUtils.LOGGER.warn("[EventMusicHttpServer] Client requested pack but file does not exist at {}", packZipPath);
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                    return;
                }

                File file = packZipPath.toFile();
                long fileLength = file.length();

                exchange.getResponseHeaders().set("Content-Type", "application/zip");
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"event_music_pack.zip\"");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Accept-Ranges", "bytes");

                if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(200, fileLength);
                    exchange.close();
                    return;
                }

                exchange.sendResponseHeaders(200, fileLength);

                try (OutputStream os = exchange.getResponseBody();
                     FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    os.flush();
                }
                FracturedUtils.LOGGER.info("[EventMusicHttpServer] Served event_music_pack.zip ({}) to {}", fileLength, exchange.getRemoteAddress());
            } catch (Exception e) {
                FracturedUtils.LOGGER.error("[EventMusicHttpServer] Error serving pack request", e);
            } finally {
                exchange.close();
            }
        }
    }
}
