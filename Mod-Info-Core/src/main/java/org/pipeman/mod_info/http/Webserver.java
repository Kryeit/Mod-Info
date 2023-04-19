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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Webserver {
    private final PresetSupplier supplier = new PresetSupplier();
    private Javalin server;

    public void start(int port, boolean prepareZip) throws IOException {
        server = Javalin.create(c -> c.showJavalinBanner = false);
        server.get("/", new FileHandler(Paths.get("index.html")));
        server.get("/api/download", new DownloadHandler(supplier, prepareZip));
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

        private DownloadHandler(PresetSupplier supplier, boolean prepareZip) {
            this.supplier = supplier;
            this.prepareZip = prepareZip;
        }

        @Override
        public void handle(Context ctx) throws IOException {
            String hashes = ctx.queryParam("hashes");
            if (hashes == null) {
                ctx.status(400).result("Missing query parameter 'hashes'");
                return;
            }

            ctx.header(Header.CONTENT_DISPOSITION, "attachment; filename=\"mods.zip\"");
            ctx.header(Header.CONTENT_TYPE, "application/octet-stream");

            if (prepareZip) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                Zipper.getZip(Utils.map(hashes.split(","), String::trim), supplier.getPresets(), os);
                ctx.result(os.toByteArray());
            } else {
                Zipper.getZip(Utils.map(hashes.split(","), String::trim), supplier.getPresets(), ctx.outputStream());
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
