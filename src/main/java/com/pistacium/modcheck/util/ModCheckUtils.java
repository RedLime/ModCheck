package com.pistacium.modcheck.util;

import com.google.gson.*;
import com.pistacium.modcheck.ModCheck;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModCheckUtils {
    private static final HashMap<String, String> urlReqCache = new HashMap<>();

    public static String getUrlRequest(String url) throws IllegalAccessException {
        if (urlReqCache.containsKey(url)) {
            return urlReqCache.get(url);
        }
        try {
            URL obj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));

            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            urlReqCache.put(url, response.toString());
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new IllegalAccessException("Couldn't loading url request data! check your internet status");
    }

    public static JsonObject getFabricJsonFileInJar(File jarFile) {
        try {
            JarFile file = new JarFile(jarFile);
            JarEntry entry =  file.getJarEntry("fabric.mod.json");

            int readBytes;
            if (entry != null) {
                InputStream is = file.getInputStream(entry);
                StringBuilder jsonString = new StringBuilder();

                while ((readBytes = is.read()) != -1) {
                    jsonString.append((char) readBytes);
                }

                return JsonParser.parseString(jsonString.toString()).getAsJsonObject();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getAPIUrl(String url, String type) {
        if (Objects.equals(type, "github_releases")) {
            return "https://me.redlimerl.com/mcsr/modcheck?type=github&target=" + url.split("/")[4] + "%2F" + url.split("/")[5];
        }
        if (Objects.equals(type, "curseforge_files")) {
            return "https://me.redlimerl.com/mcsr/modcheck?type=curseforge&target=" + url.split("/")[5];
        }

        return url;
    }

    public static Config readConfig() {
        Gson gson = new Gson();
        File file = new File("modcheck.json");
        if (!file.exists()) {
            return null;
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        assert br != null;
        return gson.fromJson(br, Config.class);
    }

    public static void writeConfig(File dir) {
        File file = new File("modcheck.json");
        Config config = new Config(dir.getPath());
        try (Writer writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create();
            gson.toJson(config, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
