package com.pistacium.modcheck.mod.resource;

import com.google.gson.JsonObject;
import com.pistacium.modcheck.mod.version.ModVersion;
import com.pistacium.modcheck.mod.version.VersionPick;

import java.util.ArrayList;
import java.util.List;

public class DirectURLModResources extends ModResources<JsonObject, String> {

    private final String url;
    private final String fileName;

    public DirectURLModResources(String url, List<VersionPick> versionPicks, String defaultBuild, ArrayList<ModVersion> defaultMCVersions) {
        super(null, versionPicks, defaultBuild, defaultMCVersions);
        this.url = url;
        String[] urlArr = url.split("/");
        this.fileName = urlArr[urlArr.length - 1];
    }

    @Override
    protected JsonObject convertData(String data) {
        return null;
    }

    @Override
    public Iterable<String> getChildAssets(JsonObject assets) {
        return List.of();
    }

    @Override
    public boolean isPreRelease(String asset) {
        return false;
    }

    @Override
    public List<ModResource> convertToModResources(String asset) {
        ArrayList<ModResource> modResources = new ArrayList<>();
        for (ModVersion defaultMCVersion : this.getDefaultMCVersions()) {
            modResources.add(new ModResource(defaultMCVersion, ModVersion.of(this.getDefaultBuild()), url, fileName));
        }
        return modResources;
    }
}
