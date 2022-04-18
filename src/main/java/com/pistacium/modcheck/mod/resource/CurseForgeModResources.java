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

public class CurseForgeModResources extends ModResources<JsonArray, JsonObject> {

    public CurseForgeModResources(String data, List<VersionPick> versionPicks, String defaultBuild, ArrayList<ModVersion> defaultMCVersions) {
        super(data, versionPicks, defaultBuild, defaultMCVersions);
    }

    @Override
    protected JsonArray convertData(String data) {
        return JsonParser.parseString(data).getAsJsonObject().getAsJsonArray("data");
    }

    @Override
    public Iterable<JsonObject> getChildAssets(JsonArray files) {
        ArrayList<JsonObject> child = new ArrayList<>();
        for (JsonElement file : files) {
            child.add(file.getAsJsonObject());
        }
        return child;
    }

    @Override
    public boolean isPreRelease(JsonObject file) {
        return file.get("releaseType").getAsInt() != 1;
    }

    @Override
    public List<ModResource> convertToModResources(JsonObject file) {
        ArrayList<ModResource> modResources = new ArrayList<>();

        String fileName = file.get("fileName").getAsString();
        String downloadUrl = file.get("downloadUrl").getAsString();
        ModVersion mcVersion = null;
        ModVersion modVersion = null;

        Map<VersionPick, ModVersion> versionMap = getVersionMapFromFileName(fileName);

        if (versionMap.containsKey(VersionPick.VERSION)) modVersion = versionMap.get(VersionPick.VERSION);
        if (versionMap.containsKey(VersionPick.MC_VERSION)) mcVersion = versionMap.get(VersionPick.MC_VERSION);

        if (modVersion == null && this.getDefaultBuild() != null) modVersion = ModVersion.of(this.getDefaultBuild());
        if (mcVersion == null) {
            for (JsonElement gameVersion : file.getAsJsonArray("gameVersions")) {
                String gameVersionString = gameVersion.getAsString();
                if (ModVersion.versionRegex.matcher(gameVersionString).find() || ModVersion.snapshotRegex.matcher(gameVersionString).find()) {
                    modResources.add(new ModResource(ModVersion.of(gameVersionString), modVersion, downloadUrl, fileName));
                }
            }
        } else {
            modResources.add(new ModResource(mcVersion, modVersion, downloadUrl, fileName));
        }

        return modResources;
    }
}
