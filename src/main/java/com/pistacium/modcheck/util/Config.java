package com.pistacium.modcheck.util;

import java.io.File;

public class Config {
    final String filepath;

    public Config(String filepath) {
        this.filepath = filepath;
    }

    public File getDir() {
        return new File(filepath);
    }
}
