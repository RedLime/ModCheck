package com.pistacium.modcheck.util;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

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
