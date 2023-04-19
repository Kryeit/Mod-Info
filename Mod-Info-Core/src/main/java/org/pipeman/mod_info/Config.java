package org.pipeman.mod_info;

import org.pipeman.pconf.AbstractConfig;

import java.nio.file.Paths;

public class Config extends AbstractConfig {
    public Config(String file) {
        super(file);
        store(Paths.get(file), "");
    }

    public final int port = get("server-port", 4242);
    public final boolean prepareZipBeforeSending = get("prepare-zip-before-sending", true);
}
