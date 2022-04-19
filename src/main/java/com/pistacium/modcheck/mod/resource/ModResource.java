package com.pistacium.modcheck.mod.resource;

import com.pistacium.modcheck.mod.version.ModVersion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;

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

    public void downloadFile(Path modsPath) {
        try {
            URL url = new URL(downloadUrl);

            URLConnection con = url.openConnection();

            modsPath.toFile().mkdirs();
            File download = modsPath.resolve(fileName).toFile();

            ReadableByteChannel rbc = Channels.newChannel(con.getInputStream());
            try (FileOutputStream fos = new FileOutputStream(download)) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }
        } catch (IOException e) {
            e.printStackTrace();
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