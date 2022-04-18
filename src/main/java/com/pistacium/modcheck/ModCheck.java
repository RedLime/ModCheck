package com.pistacium.modcheck;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.pistacium.modcheck.mod.ModData;
import com.pistacium.modcheck.mod.resource.ModResource;
import com.pistacium.modcheck.mod.version.ModVersion;
import com.pistacium.modcheck.util.ModCheckStatus;
import com.pistacium.modcheck.util.ModCheckUtils;

import javax.swing.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModCheck extends JFrame {

    public static ModCheck INSTANCE;

    public static Logger LOGGER = Logger.getLogger("ModCheck");

    public static String APPLICATION_VERSION = "0.1";

    public static final ArrayList<ModVersion> AVAILABLE_VERSIONS = new ArrayList<>();

    public static final ArrayList<ModData> AVAILABLE_MODS = new ArrayList<>();

    private static ModCheckStatus STATUS = ModCheckStatus.IDLE;
    public static void setStatus(ModCheckStatus status) {
        STATUS = status;
        System.out.println(status.getDescription());
    }
    public static ModCheckStatus getStatus() { return STATUS; }

    public static ExecutorService THREAD_EXECUTOR = Executors.newSingleThreadExecutor();

    public static ModVersion MC1_16_1 = ModVersion.of("1.16.1");


    public static void main(String[] args) {
        INSTANCE = new ModCheck();
        THREAD_EXECUTOR.submit(() -> {
            try {
                // Get available versions
                setStatus(ModCheckStatus.LOADING_AVAILABLE_VERSIONS);
                JsonElement availableElement = JsonParser.parseString(Objects.requireNonNull(ModCheckUtils.getUrlRequest("https://redlime.github.io/MCSRMods/mod_versions.json")));
                for (JsonElement jsonElement : availableElement.getAsJsonArray()) {
                    AVAILABLE_VERSIONS.add(ModVersion.of(jsonElement.getAsString()));
                }


                // Get mod list
                setStatus(ModCheckStatus.LOADING_MOD_LIST);
                JsonElement modElement = JsonParser.parseString(Objects.requireNonNull(ModCheckUtils.getUrlRequest("https://redlime.github.io/MCSRMods/mods.json")));
                setStatus(ModCheckStatus.LOADING_MOD_RESOURCE);
                for (JsonElement jsonElement : modElement.getAsJsonArray()) {
                    try {
                        if (Objects.equals(jsonElement.getAsJsonObject().get("type").getAsString(), "fabric_mod")) {
                            ModData modData = new ModData(jsonElement.getAsJsonObject());
                            AVAILABLE_MODS.add(modData);
                            ModResource resource = modData.getLatestVersionResource(MC1_16_1);
                            System.out.printf("Successfully initialized %s(Latest Version of MC %s : %s)!%n", modData.getName(), MC1_16_1.getVersionName(), resource == null ? ("none for "+MC1_16_1.getVersionName()) : resource.getModVersion().getVersionName());
                        }
                    } catch (Throwable e) {
                        LOGGER.log(Level.WARNING, "Failed to init " + jsonElement.getAsJsonObject().get("name").getAsString() + "!", e);
                    }
                }


                setStatus(ModCheckStatus.IDLE);

                // Test downloading
                setStatus(ModCheckStatus.DOWNLOADING_MOD_FILE);
                
                setStatus(ModCheckStatus.IDLE);
            } catch (Throwable e) {
                LOGGER.log(Level.WARNING, "Exception in Initializing!", e);
            }
        });
        //System.out.println(new Gson().toJson(ModCheckUtils.getFabricJsonFileInJar(new File("D:/MultiMC/instances/1.16-1/.minecraft/mods/SpeedRunIGT-10.0+1.16.1.jar"))));
    }

    public ModCheck() {
        super("ModCheck v"+ APPLICATION_VERSION);
        setSize(800, 500);
        setResizable(false);
        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

}
