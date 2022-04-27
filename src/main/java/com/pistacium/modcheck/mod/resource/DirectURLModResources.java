package com.pistacium.modcheck.mod.resource;

import com.pistacium.modcheck.mod.version.ModVersion;
import com.pistacium.modcheck.mod.version.VersionPick;

import java.util.ArrayList;
import java.util.List;

public class DirectURLModResources extends ModResources<DirectURLModResources.DirectURLSource, DirectURLModResources.DirectURLSource> {

    public static class DirectURLSource {
        private final String url;
        private final String fileName;

        public DirectURLSource(String url, String fileName) {
            this.url = url;
            this.fileName = fileName;
        }
    }

    public DirectURLModResources(String url, List<VersionPick> versionPicks, String defaultBuild, ArrayList<ModVersion> defaultMCVersions) {
        super(url, versionPicks, defaultBuild, defaultMCVersions);
    }

    @Override
    protected DirectURLSource convertData(String data) {
        String[] urlArr = data.split("/");
        String fileName = urlArr[urlArr.length - 1];
        return new DirectURLSource(data, fileName);
    }

    @Override
    public Iterable<DirectURLSource> getChildAssets(DirectURLSource asset) {
        ArrayList<DirectURLSource> list = new ArrayList<>();
        list.add(asset);
        return list;
    }

    @Override
    public boolean isPreRelease(DirectURLSource asset) {
        return false;
    }

    @Override
    public List<ModResource> convertToModResources(DirectURLSource asset) {
        ArrayList<ModResource> modResources = new ArrayList<>();
        for (ModVersion defaultMCVersion : this.getDefaultMCVersions()) {
            modResources.add(new ModResource(defaultMCVersion, ModVersion.of(this.getDefaultBuild()), asset.url, asset.fileName));
        }
        return modResources;
    }
}
