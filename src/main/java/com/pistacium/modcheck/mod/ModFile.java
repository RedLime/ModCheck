package com.pistacium.modcheck.mod;

import java.util.List;

public class ModFile {
    private String version;
    private List<String> game_versions;
    private String name;
    private String url;
    private String page;
    private String sha1;
    private int size;
    private List<ModRule> rules;

    public String getName() {
        return name;
    }

    public List<ModRule> getRules() {
        return rules;
    }

    public int getSize() {
        return size;
    }

    public String getPage() {
        return page;
    }

    public List<String> getGameVersions() {
        return game_versions;
    }

    public String getSha1() {
        return sha1;
    }

    public String getUrl() {
        return url;
    }

    public String getVersion() {
        return version;
    }
}
