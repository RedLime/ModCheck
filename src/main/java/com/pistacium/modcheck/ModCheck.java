package com.pistacium.modcheck;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.pistacium.modcheck.mod.ModData;
import com.pistacium.modcheck.mod.version.ModVersion;
import com.pistacium.modcheck.util.ModCheckStatus;
import com.pistacium.modcheck.util.ModCheckUtils;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModCheck {

    public static final Logger LOGGER = Logger.getLogger("ModCheck");

    private static ModCheckStatus STATUS = ModCheckStatus.IDLE;
    public static void setStatus(ModCheckStatus status) {
        STATUS = status;
        System.out.println(status.getDescription());
    }
    public static ModCheckStatus getStatus() { return STATUS; }

    public static final ExecutorService THREAD_EXECUTOR = Executors.newSingleThreadExecutor();

    public static ModCheckFrame FRAME_INSTANCE;

    public static final ArrayList<ModVersion> AVAILABLE_VERSIONS = new ArrayList<>();

    public static final ArrayList<ModData> AVAILABLE_MODS = new ArrayList<>();


    public static void main(String[] args) {
        FRAME_INSTANCE = new ModCheckFrame();
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
                        }
                    } catch (Throwable e) {
                        LOGGER.log(Level.WARNING, "Failed to init " + jsonElement.getAsJsonObject().get("name").getAsString() + "!", e);
                    }
                }


                setStatus(ModCheckStatus.IDLE);
            } catch (Throwable e) {
                LOGGER.log(Level.WARNING, "Exception in Initializing!", e);
            }
        });
        //System.out.println(new Gson().toJson(ModCheckUtils.getFabricJsonFileInJar(new File("D:/MultiMC/instances/1.16-1/.minecraft/mods/SpeedRunIGT-10.0+1.16.1.jar"))));
    }

}
