package org.pipeman.mod_info;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PresetSupplier {
    private static final File MODS_FILE = new File("mods.json");
    private final List<Preset> presets = new ArrayList<>();

    public PresetSupplier() {
        try {
            if (!MODS_FILE.exists()) {
                Files.copy(getClass().getResourceAsStream("/mods.json"), MODS_FILE.toPath());
            }

            JSONArray presetsArray = new JSONObject(new String(Files.readAllBytes(MODS_FILE.toPath()))).getJSONArray("presets");
            for (Object key : presetsArray) {
                JSONObject preset = (JSONObject) key;
                boolean required = preset.optBoolean("required", false);
                List<String> include = Utils.map(preset.getJSONArray("include").toList(), String::valueOf);
                List<String> exclude = Utils.map(preset.getJSONArray("exclude").toList(), String::valueOf);
                presets.add(new Preset(include, exclude, preset.getString("name"), required));
            }
        } catch (JSONException | IOException e) {
            throw new RuntimeException("Failed to read mods.json file", e);
        }
    }

    public List<Preset> getPresets() {
        return presets;
    }

    public static class Preset {
        private final List<File> mods = new ArrayList<>();
        private final String name;
        private final boolean required;

        public Preset(List<String> include, List<String> exclude, String name, boolean required) {
            this.name = name;
            this.required = required;

            for (String s : include) {
                File file = new File(s);
                if (file.isDirectory()) {
                    File[] list = file.listFiles((dir, name1) -> Paths.get(dir.getPath(), name1).toFile().isFile());
                    Collections.addAll(mods, list == null ? new File[0] : list);
                } else {
                    mods.add(file);
                }
            }

            mods.removeIf(f -> exclude.contains(f.getPath()));
        }

        public List<File> getMods() {
            return mods;
        }

        public boolean isRequired() {
            return required;
        }

        public String name() {
            return name;
        }
    }
}
