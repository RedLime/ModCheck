package com.pistacium.modcheck.mod.version;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModVersion implements Comparable<ModVersion> {

    public static final Pattern versionRegex = Pattern.compile("((\\d+\\.)+(\\d+\\.)?(\\d+|x)|(v(\\d+\\.)?(\\d+\\.)?(\\d+|x)))");
    public static final Pattern snapshotRegex = Pattern.compile("\\d+w+\\d+\\w*");
    public static final List<String> masterVerString = new ArrayList<>();
    static {
        masterVerString.add("*");
        masterVerString.add("x");
        masterVerString.add("-");
    }

    private final String versionStr;
    private final int[] versionArray;
    private final String versionBuild;
    private final int versionRange;

    private static final HashMap<String, ModVersion> caches = new HashMap<>();
    public static ModVersion of(String raw) {
        if (caches.containsKey(raw)) return caches.get(raw);
        ModVersion modVersion = new ModVersion(raw);
        caches.put(raw, modVersion);
        return modVersion;
    }

    private ModVersion(String raw) {
        this.versionStr = raw;

        if (snapshotRegex.matcher(this.versionStr).find()) {
            this.versionArray = new int[] { -2 };
            this.versionBuild = "";
            this.versionRange = 0;
            return;
        }

        Matcher matcher = versionRegex.matcher(this.versionStr);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Can't parse version to '" + this.versionStr +"'");
        }
        String version = matcher.group();

        String[] vcArray = version.split("\\.");
        int[] versionInt = new int[vcArray.length];

        for (int i = 0; i < vcArray.length; i++) {
            String vc = vcArray[i];
            if (masterVerString.contains(vc) && i == vcArray.length - 1) {
                versionInt[i] = -1;
                break;
            }

            try {
                versionInt[i] = Integer.parseInt(vc.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException();
            }
        }

        this.versionBuild = vcArray[vcArray.length - 1].replace(String.valueOf(versionInt[vcArray.length - 1]), "");
        this.versionArray = versionInt;
        if (this.versionStr.startsWith("~")) {
            this.versionRange = -1;
        } else if (this.versionStr.endsWith("+")) {
            this.versionRange = 1;
        } else {
            this.versionRange = 0;
        }
    }

    public String getVersionName() {
        return versionStr;
    }

    @Override
    public int compareTo(ModVersion target) {
        if (this.versionArray[0] == -2 || target.versionArray[0] == -2)
            return Objects.equals(this.versionStr, target.versionStr) ? 0 : -1;

        int vs = 0;
        int result = 0;
        while (vs < this.versionArray.length || vs < target.versionArray.length) {
            int currentVersion = vs < this.versionArray.length ? this.versionArray[vs] : 0;
            int targetVersion = vs < target.versionArray.length ? target.versionArray[vs] : 0;

            int comparedVersion = Integer.compare(currentVersion, targetVersion);

            if (currentVersion == -1 || targetVersion == -1) return 0;

            if (comparedVersion != 0) {
                result = comparedVersion;
                break;
            }

            vs++;
        }

        if (result == 0 && !this.versionBuild.isEmpty() && !target.versionBuild.isEmpty()) {
            result = this.versionBuild.compareTo(target.versionBuild);
        }

        if (this.versionRange != 0 || target.versionRange != 0) {
            if (this.versionRange == -result || target.versionRange == result) {
                return 0;
            }
        }

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModVersion that = (ModVersion) o;
        return compareTo(that) == 0;
    }

    @Override
    public String toString() {
        return this.getVersionName();
    }
}
