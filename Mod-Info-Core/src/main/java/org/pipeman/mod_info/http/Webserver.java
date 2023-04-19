package org.pipeman.mod_info.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.pipeman.mod_info.PresetSupplier;
import org.pipeman.mod_info.PresetSupplier.Preset;
import org.pipeman.mod_info.Utils;
import org.pipeman.mod_info.Zipper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Webserver {
    private final PresetSupplier supplier = new PresetSupplier();
    private HttpServer server;

    public void start(int port, boolean prepareZip) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new FileHandler(Paths.get("index.html")));
        server.createContext("/api/download", new DownloadHandler(supplier, prepareZip));
        server.createContext("/api/info", new InfoHandler(supplier));
        server.start();
    }

    public void stop() {
        server.stop(42);
    }

    private static class FileHandler implements HttpHandler {
        private final String index;

        private FileHandler(Path file) throws IOException {
            if (!file.toFile().exists()) {
                Files.copy(getClass().getResourceAsStream("/index.html"), file);
            }
            this.index = new String(Files.readAllBytes(file));
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendResponse(exchange, 200, index);
//            sendResponse(exchange, 200, Files.readAllBytes(Paths.get("index.html")));
        }
    }

    private static class DownloadHandler implements HttpHandler {
        private final PresetSupplier supplier;
        private final boolean prepareZip;

        private DownloadHandler(PresetSupplier supplier, boolean prepareZip) {
            this.supplier = supplier;
            this.prepareZip = prepareZip;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String hashes = splitQuery(exchange.getRequestURI()).get("hashes");
            if (hashes == null) {
                sendResponse(exchange, 400, "Missing query parameter 'hashes'");
                return;
            }

            exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"mods.zip\"");
            exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");

            if (prepareZip) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                Zipper.getZip(Utils.map(hashes.split(","), String::trim), supplier.getPresets(), os);
                sendResponse(exchange, 200, os.toByteArray());
            } else {
                exchange.sendResponseHeaders(200, 0);
                Zipper.getZip(Utils.map(hashes.split(","), String::trim), supplier.getPresets(), exchange.getResponseBody());
            }
        }

        private Map<String, String> splitQuery(URI url) {
            try {
                Map<String, String> queryPairs = new LinkedHashMap<>();
                for (String pair : url.getQuery().split("&")) {
                    int idx = pair.indexOf("=");
                    queryPairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                }
                return queryPairs;
            } catch (Exception ignored) {
                return new HashMap<>();
            }
        }
    }

    private static class InfoHandler implements HttpHandler {
        private final PresetSupplier supplier;

        private InfoHandler(PresetSupplier supplier) {
            this.supplier = supplier;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            JSONArray presets = new JSONArray();
            for (Preset preset : supplier.getPresets()) {
                JSONArray mods = new JSONArray();
                for (File mod : preset.getMods()) {
                    mods.put(new JSONObject()
                            .put("name", mod.getName())
                            .put("hash", Utils.hash(mod.getPath()))
                    );
                }

                JSONObject presetObject = new JSONObject()
                        .put("mods", mods)
                        .put("required", preset.isRequired())
                        .put("name", preset.name());

                presets.put(presetObject);
            }

            sendResponse(exchange, 200, new JSONObject().put("presets", presets).toString());
        }
    }

    private static void sendResponse(HttpExchange exchange, int code, String message) throws IOException {
        sendResponse(exchange, code, message.getBytes());
    }

    private static void sendResponse(HttpExchange exchange, int code, byte[] message) throws IOException {
        exchange.sendResponseHeaders(code, message.length);
        OutputStream os = exchange.getResponseBody();
        os.write(message);
        os.close();
    }
}
