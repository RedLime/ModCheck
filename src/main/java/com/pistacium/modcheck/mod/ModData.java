package com.pistacium.modcheck.mod;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pistacium.modcheck.mod.resource.ModResource;
import com.pistacium.modcheck.mod.version.ModVersion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ModData {
    private final String name;
    private final String description;
    private final String warningMessage;
    private final List<String> incompatibleMods = new ArrayList<>();
    private final List<ModResource> resourceList = new ArrayList<>();
    private final String readme;

    public ModData(JsonObject jsonObject) {
        this.name = jsonObject.get("name").getAsString();
        this.description = jsonObject.get("description").getAsString();
        this.warningMessage = jsonObject.has("warn") ? jsonObject.get("warn").getAsString() : "";
        this.readme = jsonObject.has("readme") ? jsonObject.get("readme").getAsString() : "";
        for (JsonElement jsonElement : jsonObject.getAsJsonArray("incompatible")) {
            this.incompatibleMods.add(jsonElement.getAsString());
        }

        for (JsonElement jsonElement : jsonObject.getAsJsonArray("versions")) {
            JsonObject versionObject = jsonElement.getAsJsonObject();

            ModResource resource = new ModResource(
                    ModVersion.of(versionObject.get("targetVersion").getAsString()),
                    ModVersion.of(versionObject.get("buildVersion").getAsString()),
                    versionObject.get("downloadUrl").getAsString(),
                    versionObject.get("filename").getAsString()
            );

            resourceList.add(resource);
        }
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public String getReadme() {
        return readme;
    }

    public List<String> getIncompatibleMods() {
        return incompatibleMods;
    }

    public ModResource getLatestVersionResource(ModVersion minecraftVersion) {
        if (minecraftVersion == null) return null;
        for (ModResource modResource : resourceList) {
            if (modResource.getSupportMCVersion().compareTo(minecraftVersion) == 0) return modResource;
        }
        return null;
    }

    public boolean downloadModJarFile(ModVersion minecraftVersion, Stack<File> instancePath) {
        ModResource resource = getLatestVersionResource(minecraftVersion);
        if (resource != null) {
            try {
                resource.downloadFile(instancePath);
                return true;
            } catch (Throwable e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }
}
