package org.pipeman.mod_info;

import org.pipeman.mod_info.http.Webserver;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Config config = new Config("config.properties");
        new Webserver().start(config.port, config.prepareZipBeforeSending);
    }
}
