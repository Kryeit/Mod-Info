package org.pipeman.mod_info.http;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.Header;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;

public class Webserver {
    private final PresetSupplier supplier = new PresetSupplier();
    private Javalin server;

    public void start(int port, boolean prepareZip, Path multiMcPack, String fileName) throws IOException {
        server = Javalin.create(c -> c.showJavalinBanner = false);
        server.get("/", new FileHandler(Paths.get("index.html")));
        server.get("/api/download", new DownloadHandler(supplier, prepareZip, multiMcPack, fileName));
        server.get("/api/info", new InfoHandler(supplier));
        server.start(port);
    }

    public void stop() {
        server.stop();
    }

    private static class FileHandler implements Handler {
        private final String index;

        private FileHandler(Path file) throws IOException {
            if (!file.toFile().exists()) {
                Files.copy(getClass().getResourceAsStream("/index.html"), file);
            }
            this.index = new String(Files.readAllBytes(file));
        }

        @Override
        public void handle(Context ctx) {
            ctx.html(index);
        }
    }

    private static class DownloadHandler implements Handler {
        private final PresetSupplier supplier;
        private final boolean prepareZip;
        private final Path multiMcPackFile;
        private final String fileName;

        private DownloadHandler(PresetSupplier supplier, boolean prepareZip, Path multiMcPackFile, String fileName) {
            this.supplier = supplier;
            this.prepareZip = prepareZip;
            this.multiMcPackFile = multiMcPackFile;
            this.fileName = fileName;
        }

        @Override
        public void handle(Context ctx) throws IOException {
            String hashes = ctx.queryParam("hashes");
            if (hashes == null) {
                ctx.status(400).result("Missing query parameter 'hashes'");
                return;
            }

            ctx.header(Header.CONTENT_DISPOSITION, MessageFormat.format("attachment; filename=\"{0}\"", fileName));
            ctx.header(Header.CONTENT_TYPE, "application/octet-stream");

            boolean multiMCPack = Boolean.parseBoolean(ctx.queryParam("multimc-pack"));
            if (prepareZip) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                upload(hashes, multiMCPack, os);
                ctx.result(os.toByteArray());
            } else {
                upload(hashes, multiMCPack, ctx.outputStream());
            }
        }

        private void upload(String hashes, boolean multiMCPack, OutputStream outputStream) throws IOException {
            List<String> mappedHashes = Utils.map(hashes.split(","), String::trim);
            List<Preset> presets = supplier.getPresets();
            if (multiMCPack) {
                Zipper.addModsToExistingZip(mappedHashes, presets, multiMcPackFile.toFile(), outputStream);
            } else {
                Zipper.getZip(mappedHashes, presets, outputStream);
            }
        }
    }

    private static class InfoHandler implements Handler {
        private final PresetSupplier supplier;

        private InfoHandler(PresetSupplier supplier) {
            this.supplier = supplier;
        }

        @Override
        public void handle(Context ctx) {
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

            ctx.json(new JSONObject().put("presets", presets).toString());
        }
    }
}
