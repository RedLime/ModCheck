package com.pistacium.modcheck.mod;

import java.util.Map;

public class ModRule {
    private String action;
    private Map<String, String> properties;

    public String getAction() {
        return action;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
