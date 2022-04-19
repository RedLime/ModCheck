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

    public static void setStatus(ModCheckStatus status) {
        FRAME_INSTANCE.getProgressBar().setString(status.getDescription());
    }

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
                FRAME_INSTANCE.getProgressBar().setValue(5);
                for (JsonElement jsonElement : availableElement.getAsJsonArray()) {
                    AVAILABLE_VERSIONS.add(ModVersion.of(jsonElement.getAsString()));
                }

                // Get mod list
                setStatus(ModCheckStatus.LOADING_MOD_LIST);
                JsonElement modElement = JsonParser.parseString(Objects.requireNonNull(ModCheckUtils.getUrlRequest("https://redlime.github.io/MCSRMods/mods.json")));
                FRAME_INSTANCE.getProgressBar().setValue(10);

                setStatus(ModCheckStatus.LOADING_MOD_RESOURCE);
                int count = 0, maxCount = modElement.getAsJsonArray().size();
                for (JsonElement jsonElement : modElement.getAsJsonArray()) {
                    try {
                        if (Objects.equals(jsonElement.getAsJsonObject().get("type").getAsString(), "fabric_mod")) {
                            FRAME_INSTANCE.getProgressBar().setString("Loading information of "+jsonElement.getAsJsonObject().get("name"));
                            ModData modData = new ModData(jsonElement.getAsJsonObject());
                            AVAILABLE_MODS.add(modData);
                            FRAME_INSTANCE.getProgressBar().setValue((int) (10 + (((++count * 1f) / maxCount) * 90)));
                        }
                    } catch (Throwable e) {
                        LOGGER.log(Level.WARNING, "Failed to init " + jsonElement.getAsJsonObject().get("name").getAsString() + "!", e);
                    }
                }
                FRAME_INSTANCE.getProgressBar().setValue(100);
                setStatus(ModCheckStatus.IDLE);
                FRAME_INSTANCE.updateVersionList();
            } catch (Throwable e) {
                LOGGER.log(Level.WARNING, "Exception in Initializing!", e);
            }
        });
        //System.out.println(new Gson().toJson(ModCheckUtils.getFabricJsonFileInJar(new File("D:/MultiMC/instances/1.16-1/.minecraft/mods/SpeedRunIGT-10.0+1.16.1.jar"))));
    }

}
