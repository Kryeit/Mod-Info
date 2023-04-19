package org.pipeman.mod_info;

import org.pipeman.mod_info.PresetSupplier.Preset;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Zipper {
    public static void getZip(List<String> additionalModHashes, List<Preset> presets, OutputStream outputStream) throws IOException {
        ZipOutputStream zipOS = new ZipOutputStream(outputStream);

        for (Preset preset : presets) {
            if (preset.isRequired()) {
                for (File mod : preset.getMods()) {
                    zipOS.putNextEntry(new ZipEntry(mod.getName()));
                    zipOS.write(Files.readAllBytes(mod.toPath()));
                    zipOS.closeEntry();
                }
            } else {
                for (File mod : preset.getMods()) {
                    if (additionalModHashes.contains(Utils.hash(mod.getPath()))) {
                        zipOS.putNextEntry(new ZipEntry(mod.getName()));
                        zipOS.write(Files.readAllBytes(mod.toPath()));
                        zipOS.closeEntry();
                    }
                }
            }
        }

        zipOS.close();
    }
}
