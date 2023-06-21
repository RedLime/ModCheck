package com.pistacium.modcheck.mod;

import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.fabricmc.loader.impl.util.version.VersionParser;
import net.fabricmc.loader.impl.util.version.VersionPredicateParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ModInfo {
    private String name;
    private String description;
    private String type;
    private boolean recommended;
    private List<ModFile> files;
    private List<String> incompatible;


    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public List<ModFile> getFiles() {
        return files;
    }

    public List<String> getIncompatible() {
        return incompatible == null ? new ArrayList<>() : incompatible;
    }

    public ModFile getFileFromVersion(MCVersion mcVersion, RuleIndicator ruleIndicator) {
        try {
            for (ModFile file : this.getFiles()) {
                if (!ruleIndicator.checkWithRules(file.getRules())) continue;
                for (String gameVersion : file.getGameVersions()) {
                    VersionPredicate versionPredicate = VersionPredicateParser.parse(gameVersion);
                    if (versionPredicate.test(VersionParser.parseSemantic(mcVersion.getValue()))) return file;
                }
            }
        } catch (VersionParsingException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public boolean downloadFile(MCVersion mcVersion, RuleIndicator ruleIndicator, Stack<File> downloadFiles) {
        ModFile modFile = this.getFileFromVersion(mcVersion, ruleIndicator);

        try {
            if (downloadFiles.size() < 1) return false;
            URL url = new URL(modFile.getUrl());

            URLConnection con = url.openConnection();
            con.setRequestProperty("User-Agent", "ModCheck-Client");

            File download = downloadFiles.pop().toPath().resolve(modFile.getName()).toFile();

            ReadableByteChannel rbc = Channels.newChannel(con.getInputStream());
            try (FileOutputStream fos = new FileOutputStream(download)) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }
            System.out.println("Downloaded "+modFile.getName()+" in "+download.getPath());

            while (downloadFiles.size() > 0) {
                Path copyPath = downloadFiles.pop().toPath().resolve(modFile.getName());
                Files.copy(download.toPath(), copyPath);
                System.out.println("Copied to " + copyPath);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
