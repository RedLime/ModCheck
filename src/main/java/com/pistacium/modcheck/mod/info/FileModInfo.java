package com.pistacium.modcheck.mod.info;

import com.google.gson.JsonObject;
import com.pistacium.modcheck.util.ModCheckUtils;
import com.pistacium.modcheck.mod.version.ModVersion;

import java.io.File;

public class FileModInfo {

    private final File file;
    private final JsonObject json;
    private final String modName;
    private final ModVersion modVersion;

    public FileModInfo(File modJar) {
        this.file = modJar;
        this.json = ModCheckUtils.getFabricJsonFileInJar(file);

        assert this.json != null;
        this.modName = this.json.get("name").getAsString();
        this.modVersion = ModVersion.of(this.json.get("version").getAsString());
    }

    public File getFile() {
        return file;
    }

    public JsonObject getJsonObject() {
        return json;
    }

    public String getModName() {
        return modName;
    }

    public ModVersion getModVersion() {
        return modVersion;
    }

    public ModVersion getMinecraftVersion() {
        return null;
    }
}
