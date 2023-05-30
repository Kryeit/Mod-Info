package org.pipeman.mod_info;

import org.pipeman.mod_info.PresetSupplier.Preset;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Zipper {
    public static void getZip(List<String> additionalModHashes, List<Preset> presets, OutputStream outputStream) throws IOException {
        ZipOutputStream zipOS = new ZipOutputStream(outputStream);
        addMods(additionalModHashes, presets, zipOS, "");
        zipOS.close();

//        for (Preset preset : presets) {
//            if (preset.isRequired()) {
//                for (File mod : preset.getMods()) {
//                    zipOS.putNextEntry(new ZipEntry(mod.getName()));
//                    zipOS.write(Files.readAllBytes(mod.toPath()));
//                    zipOS.closeEntry();
//                }
//            } else {
//                for (File mod : preset.getMods()) {
//                    if (additionalModHashes.contains(Utils.hash(mod.getPath()))) {
//                        zipOS.putNextEntry(new ZipEntry(mod.getName()));
//                        zipOS.write(Files.readAllBytes(mod.toPath()));
//                        zipOS.closeEntry();
//                    }
//                }
//            }
//        }

    }

    private static void addMods(List<String> additionalModHashes, List<Preset> presets, ZipOutputStream zipOS, String path) throws IOException {
        for (Preset preset : presets) {
            Predicate<File> predicate = mod -> preset.isRequired() || additionalModHashes.contains(Utils.hash(mod.getPath()));
            List<File> filtered = Utils.filter(preset.getMods(), predicate);
            for (File file : filtered) {
                zipOS.putNextEntry(new ZipEntry(path + file.getName()));
                zipOS.write(Files.readAllBytes(file.toPath()));
                zipOS.closeEntry();
            }


//            if (preset.isRequired()) {
//                for (File mod : preset.getMods()) {
//                    addMod(mod, path, zipOS);
//                }
//            } else {
//                for (File mod : preset.getMods()) {
//                    if (additionalModHashes.contains(Utils.hash(mod.getPath()))) {
//                        addMod(mod, path, zipOS);
//                    }
//                }
//            }
        }
    }

    public static void addModsToExistingZip(List<String> additionalModHashes, List<Preset> presets, File originalZip, OutputStream outputStream) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(outputStream);

        try (ZipFile zin = new ZipFile(originalZip)) {
            zin.stream().forEach(entry -> {
                try {
                    zos.putNextEntry(entry);
                    if (!entry.isDirectory()) {
                        zin.getInputStream(entry).transferTo(zos);
                    }
                    zos.closeEntry();
                } catch (Exception ignored) {
                }
            });
        }
        addMods(additionalModHashes, presets, zos, ".minecraft/mods/");
        zos.close();
    }
}
