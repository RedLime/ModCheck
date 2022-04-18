package com.pistacium.modcheck.mod.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pistacium.modcheck.mod.version.ModVersion;
import com.pistacium.modcheck.mod.version.VersionPick;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GitHubModResources extends ModResources<JsonArray, JsonObject> {

    public GitHubModResources(String data, List<VersionPick> versionPicks, String defaultBuild, ArrayList<ModVersion> defaultMCVersions) {
        super(data, versionPicks, defaultBuild, defaultMCVersions);
    }

    @Override
    protected JsonArray convertData(String data) {
        return JsonParser.parseString(data).getAsJsonArray();
    }

    @Override
    public Iterable<JsonObject> getChildAssets(JsonArray releases) {
        ArrayList<JsonObject> child = new ArrayList<>();
        for (JsonElement release : releases) {
            child.add(release.getAsJsonObject());
        }
        return child;
    }

    @Override
    public boolean isPreRelease(JsonObject release) {
        return release.get("prerelease").getAsBoolean();
    }

    @Override
    public List<ModResource> convertToModResources(JsonObject release) {
        ArrayList<ModResource> modResources = new ArrayList<>();
        for (JsonElement asset : release.getAsJsonArray("assets")) {
            JsonObject assetObject = asset.getAsJsonObject();

            String fileName = assetObject.get("name").getAsString();
            String downloadUrl = assetObject.get("browser_download_url").getAsString();
            ModVersion mcVersion = null;
            ModVersion modVersion = null;

            Map<VersionPick, ModVersion> versionMap = getVersionMapFromFileName(fileName);

            if (versionMap.containsKey(VersionPick.VERSION)) modVersion = versionMap.get(VersionPick.VERSION);
            if (versionMap.containsKey(VersionPick.MC_VERSION)) mcVersion = versionMap.get(VersionPick.MC_VERSION);

            if (modVersion == null && this.getDefaultBuild() != null) modVersion = ModVersion.of(this.getDefaultBuild());
            if (mcVersion == null && this.getDefaultMCVersions().size() > 0) {
                for (ModVersion defaultMCVersion : this.getDefaultMCVersions()) {
                    modResources.add(new ModResource(defaultMCVersion, modVersion, downloadUrl, fileName));
                }
            } else {
                modResources.add(new ModResource(mcVersion, modVersion, downloadUrl, fileName));
            }

        }
        return modResources;
    }
}
