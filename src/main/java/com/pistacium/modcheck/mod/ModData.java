package com.pistacium.modcheck.mod;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pistacium.modcheck.mod.resource.*;
import com.pistacium.modcheck.mod.version.ModVersion;
import com.pistacium.modcheck.mod.version.VersionPick;
import com.pistacium.modcheck.util.ModCheckUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ModData {
    private final String name;
    private final String description;
    private final String warningMessage;
    private final List<String> incompatibleMods = new ArrayList<>();
    private final List<ModResources<?, ?>> resourcesList = new ArrayList<>();

    public ModData(JsonObject jsonObject) throws IllegalAccessException {
        this.name = jsonObject.get("name").getAsString();
        this.description = jsonObject.get("description").getAsString();
        this.warningMessage = jsonObject.has("warn") ? jsonObject.get("warn").getAsString() : "";
        for (JsonElement jsonElement : jsonObject.getAsJsonArray("incompatible")) {
            this.incompatibleMods.add(jsonElement.getAsString());
        }

        for (JsonElement jsonElement : jsonObject.getAsJsonArray("downloads")) {
            JsonObject assetObject = jsonElement.getAsJsonObject();
            ArrayList<ModVersion> supportVersions = new ArrayList<>();

            for (JsonElement versions : assetObject.getAsJsonArray("versions")) {
                supportVersions.add(ModVersion.of(versions.getAsString()));
            }

            JsonObject resource = assetObject.getAsJsonObject("resource");
            String type = resource.get("type").getAsString();

            ArrayList<VersionPick> versionPicks = new ArrayList<>();
            for (JsonElement element : resource.getAsJsonArray("values")) {
                versionPicks.add(VersionPick.valueOf(element.getAsString().toUpperCase(Locale.ROOT)));
            }


            // GitHub releases loader
            if (Objects.equals(type, "github_releases")) {
                GitHubModResources resources = new GitHubModResources(
                        ModCheckUtils.getUrlRequest(ModCheckUtils.getAPIUrl(resource.get("url").getAsString(), type)),
                        versionPicks, assetObject.has("build") ? assetObject.get("build").getAsString() : null, supportVersions
                );
                resourcesList.add(resources);
            }


            // Modrinth releases loader
            if (Objects.equals(type, "modrinth_releases")) {
                ModrinthModResources resources = new ModrinthModResources(
                        ModCheckUtils.getUrlRequest(ModCheckUtils.getAPIUrl(resource.get("url").getAsString(), type)),
                        versionPicks, assetObject.has("build") ? assetObject.get("build").getAsString() : null, supportVersions
                );
                resourcesList.add(resources);
            }


            // CurseForge files loader
            if (Objects.equals(type, "curseforge_files")) {
                CurseForgeModResources resources = new CurseForgeModResources(
                        ModCheckUtils.getUrlRequest(ModCheckUtils.getAPIUrl(resource.get("url").getAsString(), type)),
                        versionPicks, assetObject.has("build") ? assetObject.get("build").getAsString() : null, supportVersions
                );
                resourcesList.add(resources);
            }


            // Direct URL loader
            if (Objects.equals(type, "direct")) {
                DirectURLModResources resources = new DirectURLModResources(
                        ModCheckUtils.getAPIUrl(resource.get("url").getAsString(), type),
                        versionPicks, assetObject.get("build").getAsString(), supportVersions
                );
                resourcesList.add(resources);
            }
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

    public List<String> getIncompatibleMods() {
        return incompatibleMods;
    }

    public ModResource getLatestVersionResource(ModVersion minecraftVersion) {
        if (minecraftVersion == null) return null;
        ModResource resource = null;
        for (ModResources<?, ?> modResources : resourcesList) {
            ModResource newResource = modResources.getLatestResource(minecraftVersion);
            if (newResource != null) {
                if (resource == null) {
                    resource = newResource;
                } else {
                    resource = newResource.getModVersion().compareTo(resource.getModVersion()) > 0 ? newResource : resource;
                }
            }
        }
        return resource;
    }

    public boolean downloadModJarFile(ModVersion minecraftVersion, Path instancePath) {
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

    @Override
    public String toString() {
        return "ModData{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", resourcesList=" + resourcesList.size() +
                '}';
    }
}
