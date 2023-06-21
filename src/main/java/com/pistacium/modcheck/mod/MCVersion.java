package com.pistacium.modcheck.mod;

public class MCVersion {
    private String name;
    private String value;

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
