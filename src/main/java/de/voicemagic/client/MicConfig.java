package de.voicemagic.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class MicConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static boolean enabled = false;
    public static String mixerName = "";
    public static int cooldownBro = 45;
    public static int cooldownSpeed = 30;
    public static int cooldownHeal = 25;
    public static int cooldownWater = 15;
    public static int cooldownLight = 20;
    public static int cooldownInvisible = 60;

    public static Path file() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve("config/voicemagic.json");
    }

    public static void load() {
        try {
            Path path = file();
            Files.createDirectories(path.getParent());
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    Data d = GSON.fromJson(reader, Data.class);
                    if (d != null) {
                        enabled = d.enabled;
                        mixerName = d.mixerName == null ? "" : d.mixerName;
                        cooldownBro = Math.max(0, d.cooldownBro);
                        cooldownSpeed = Math.max(0, d.cooldownSpeed);
                        cooldownHeal = Math.max(0, d.cooldownHeal);
                        cooldownWater = Math.max(0, d.cooldownWater);
                        cooldownLight = Math.max(0, d.cooldownLight);
                        cooldownInvisible = Math.max(0, d.cooldownInvisible);
                    }
                }
            } else save();
        } catch (Exception e) {
            System.err.println("[VoiceMagic] Config konnte nicht geladen werden: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            Path path = file();
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(new Data(), writer);
            }
        } catch (Exception e) {
            System.err.println("[VoiceMagic] Config konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    private static class Data {
        boolean enabled = MicConfig.enabled;
        String mixerName = MicConfig.mixerName;
        int cooldownBro = MicConfig.cooldownBro;
        int cooldownSpeed = MicConfig.cooldownSpeed;
        int cooldownHeal = MicConfig.cooldownHeal;
        int cooldownWater = MicConfig.cooldownWater;
        int cooldownLight = MicConfig.cooldownLight;
        int cooldownInvisible = MicConfig.cooldownInvisible;
    }
}
