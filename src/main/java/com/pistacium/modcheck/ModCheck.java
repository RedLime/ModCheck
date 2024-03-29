package com.pistacium.modcheck;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.pistacium.modcheck.mod.MCVersion;
import com.pistacium.modcheck.mod.ModInfo;
import com.pistacium.modcheck.util.ModCheckStatus;
import com.pistacium.modcheck.util.ModCheckUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModCheck {

    public static void setStatus(ModCheckStatus status) {
        FRAME_INSTANCE.getProgressBar().setString(status.getDescription());
    }

    public static final Gson GSON = new GsonBuilder().serializeNulls().create();
    public static final ExecutorService THREAD_EXECUTOR = Executors.newSingleThreadExecutor();

    public static ModCheckFrameForm FRAME_INSTANCE;

    public static final ArrayList<MCVersion> AVAILABLE_VERSIONS = new ArrayList<>();

    public static final ArrayList<ModInfo> AVAILABLE_MODS = new ArrayList<>();

    public static void main(String[] args) {
        THREAD_EXECUTOR.submit(() -> {
            try {
                FRAME_INSTANCE = new ModCheckFrameForm();

                // Get available versions
                setStatus(ModCheckStatus.LOADING_AVAILABLE_VERSIONS);
                JsonElement availableElement = JsonParser.parseString(Objects.requireNonNull(ModCheckUtils.getUrlRequest("https://redlime.github.io/MCSRMods/meta/v4/mc_versions.json")));
                FRAME_INSTANCE.getProgressBar().setValue(30);
                for (JsonElement jsonElement : availableElement.getAsJsonArray()) {
                    AVAILABLE_VERSIONS.add(GSON.fromJson(jsonElement, MCVersion.class));
                }

                // Get mod list
                setStatus(ModCheckStatus.LOADING_MOD_LIST);
                JsonElement modElement = JsonParser.parseString(Objects.requireNonNull(ModCheckUtils.getUrlRequest("https://redlime.github.io/MCSRMods/meta/v4/files.json")));
                FRAME_INSTANCE.getProgressBar().setValue(60);

                setStatus(ModCheckStatus.LOADING_MOD_RESOURCE);
                int count = 0, maxCount = modElement.getAsJsonArray().size();
                for (JsonElement jsonElement : modElement.getAsJsonArray()) {
                    try {
                        FRAME_INSTANCE.getProgressBar().setString("Loading information of "+jsonElement.getAsJsonObject().get("name"));
                        ModInfo modInfo = GSON.fromJson(jsonElement, ModInfo.class);
                        if (Objects.equals(modInfo.getType(), "fabric_mod")) AVAILABLE_MODS.add(modInfo);
                    } catch (Throwable e) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        System.out.println("Failed to init " + jsonElement.getAsJsonObject().get("name").getAsString() + "!\r\n" + sw);
                    } finally {
                        FRAME_INSTANCE.getProgressBar().setValue((int) (60 + (((++count * 1f) / maxCount) * 40)));
                    }
                }
                FRAME_INSTANCE.getProgressBar().setValue(100);
                setStatus(ModCheckStatus.IDLE);
                FRAME_INSTANCE.updateVersionList();
            } catch (Throwable e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                int result = JOptionPane.showOptionDialog(null, sw.toString(), "Error exception!",
                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                        new String[] { "Copy to clipboard a logs", "Cancel" }, "Copy to clipboard a logs");
                if (result == 0) {
                    StringSelection selection = new StringSelection(sw.toString());
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(selection, selection);
                }

                System.exit(0);
            }
        });
        //System.out.println(new Gson().toJson(ModCheckUtils.getFabricJsonFileInJar(new File("D:/MultiMC/instances/1.16-1/.minecraft/mods/SpeedRunIGT-10.0+1.16.1.jar"))));
    }

}
