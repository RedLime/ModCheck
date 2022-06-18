package com.pistacium.modcheck.mod.resource;

import com.pistacium.modcheck.mod.version.ModVersion;
import com.pistacium.modcheck.mod.version.VersionPick;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ModResources<T, R> {

    private final HashMap<ModVersion, ModResource> resourceMap = new HashMap<>();
    private final List<VersionPick> versionPicks;
    private final String defaultBuild;
    private final ArrayList<ModVersion> defaultMCVersions;

    public ModResources(String data, List<VersionPick> versionPicks, String defaultBuild, ArrayList<ModVersion> defaultMCVersions, String fileFormat) {
        T resourceAssets = convertData(data);
        this.versionPicks = versionPicks;
        this.defaultBuild = defaultBuild;
        this.defaultMCVersions = defaultMCVersions;

        Iterable<R> assetIterable = getChildAssets(resourceAssets);
        boolean isNeedCheckPreRelease = true;
        for (R childAsset : assetIterable) {
            if (!isPreRelease(childAsset)) {
                isNeedCheckPreRelease = false;
                break;
            }
        }
        if (fileFormat != null) isNeedCheckPreRelease = true;

        for (R childAsset : assetIterable) {
            if (isPreRelease(childAsset) && !isNeedCheckPreRelease) continue;

            List<ModResource> modResources = convertToModResources(childAsset);
            HashMap<ModVersion, ModResource> updateMap = new HashMap<>();

            for (ModResource modResource : modResources) {
                if (isInvalidResource(modResource)) continue;
                boolean isSupportVersion = false;
                if (fileFormat != null) {
                    Pattern pattern = Pattern.compile(fileFormat);
                    System.out.println(fileFormat + " : " + modResource.getFileName());
                    if (pattern.matcher(modResource.getFileName()).find()) {
                        isSupportVersion = true;
                    } else {
                        continue;
                    }
                }
                if (!isSupportVersion) {
                    for (ModVersion defaultMCVersion : this.defaultMCVersions) {
                        if (defaultMCVersion.compareTo(modResource.getSupportMCVersion()) == 0) {
                            isSupportVersion = true;
                            break;
                        }
                    }
                }
                if (!isSupportVersion) continue;

                boolean foundPrev = false;
                for (Map.Entry<ModVersion, ModResource> resourceEntry : resourceMap.entrySet()) {
                    if (resourceEntry.getKey().compareTo(modResource.getSupportMCVersion()) == 0) {
                        if (resourceEntry.getValue().getModVersion().compareTo(modResource.getModVersion()) < 0) {
                            updateMap.put(resourceEntry.getKey(), modResource);
                        }
                        foundPrev = true;
                    }
                }

                if (!foundPrev) {
                    updateMap.put(modResource.getSupportMCVersion(), modResource);
                }

                resourceMap.putAll(updateMap);
            }
        }
    }

    protected abstract T convertData(String data);

    public abstract Iterable<R> getChildAssets(T assets);

    public abstract boolean isPreRelease(R asset);

    public abstract List<ModResource> convertToModResources(R asset);

    public Map<VersionPick, ModVersion> getVersionMapFromFileName(String fileName) {
        Matcher versionMatcher = ModVersion.versionRegex.matcher(fileName);
        Matcher snapshotMatcher = ModVersion.snapshotRegex.matcher(fileName);

        HashMap<VersionPick, ModVersion> versionHashMap = new HashMap<>();

        for (VersionPick versionPick : versionPicks) {
            if (versionPick == VersionPick.VERSION && versionMatcher.find()) {

                versionHashMap.put(VersionPick.VERSION, ModVersion.of(versionMatcher.group()));

            } else if (versionPick == VersionPick.MC_MAJOR_VERSION || versionPick == VersionPick.MC_VERSION) {

                String version = snapshotMatcher.find() ? snapshotMatcher.group() : versionMatcher.find() ? versionMatcher.group() : "";
                if (version.isEmpty()) continue;

                if (version.split("\\.").length == 2 && versionPick == VersionPick.MC_MAJOR_VERSION) version += ".x";
                versionHashMap.put(VersionPick.MC_VERSION, ModVersion.of(version));

            }
        }

        return versionHashMap;
    }

    public boolean isInvalidResource(ModResource modResource) {
        int versionCount = 0;
        if (ModVersion.snapshotRegex.matcher(modResource.getFileName()).find()) versionCount++;
        Matcher versionMatcher = ModVersion.versionRegex.matcher(modResource.getFileName());
        while (versionMatcher.find()) {
            versionCount++;
        }

        return versionPicks.size() > versionCount || modResource.getFileName().endsWith("-sources.jar") || modResource.getFileName().endsWith("-dev.jar");
    }

    public ModResource getLatestResource(ModVersion mcVersion) {
        for (Map.Entry<ModVersion, ModResource> resourceEntry : resourceMap.entrySet()) {
            if (resourceEntry.getKey().compareTo(mcVersion) == 0) return resourceEntry.getValue();
        }
        return null;
    }

    ArrayList<ModVersion> getDefaultMCVersions() {
        return defaultMCVersions;
    }

    String getDefaultBuild() {
        return defaultBuild;
    }
}
