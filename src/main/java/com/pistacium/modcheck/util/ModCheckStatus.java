package com.pistacium.modcheck.util;

public enum ModCheckStatus {

    IDLE(""),

    LOADING_AVAILABLE_VERSIONS("Loading available versions info"),

    LOADING_MOD_LIST("Loading mod list"),

    LOADING_MOD_RESOURCE("Loading mod's resource"),

    GETTING_INSTALLED_MODS("Getting installed mods info"),

    DOWNLOADING_MOD_FILE("Downloading file");

    private final String description;

    ModCheckStatus(String s) {
        this.description = s;
    }

    public String getDescription() {
        return description;
    }
}
