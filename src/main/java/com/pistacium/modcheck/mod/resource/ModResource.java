package com.pistacium.modcheck.mod.resource;

import com.pistacium.modcheck.mod.version.ModVersion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Stack;

public class ModResource {

    private final ModVersion supportMCVersion;
    private final ModVersion modVersion;
    private final String downloadUrl;
    private final String fileName;

    public ModResource(ModVersion supportMCVersion, ModVersion modVersion, String downloadUrl, String fileName) {
        this.supportMCVersion = supportMCVersion;
        this.modVersion = modVersion;
        this.downloadUrl = downloadUrl;
        this.fileName = fileName;
    }

    public ModVersion getModVersion() {
        return modVersion;
    }

    public ModVersion getSupportMCVersion() {
        return supportMCVersion;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void downloadFile(Stack<File> modsPaths) throws IOException {
        if (modsPaths.size() < 1) return;
        URL url = new URL(downloadUrl);

        URLConnection con = url.openConnection();

        File download = modsPaths.pop().toPath().resolve(fileName).toFile();

        ReadableByteChannel rbc = Channels.newChannel(con.getInputStream());
        try (FileOutputStream fos = new FileOutputStream(download)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        System.out.println("Downloaded "+fileName+" in "+download.getPath());

        while (modsPaths.size() > 0) {
            Path copyPath = modsPaths.pop().toPath().resolve(fileName);
            Files.copy(download.toPath(), copyPath);
            System.out.println("Copied to " + copyPath);
        }
    }

    @Override
    public String toString() {
        return "ModResource{" +
                "supportMCVersion=" + supportMCVersion +
                ", modVersion=" + modVersion +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", fileName='" + fileName + '\'' +
                '}';
    }
}
