package com.anvilclicker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class AnvilClickerConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("anvilclicker.json");

    /** Delay between clicks in milliseconds. Supports decimals e.g. 0.05 = 20/sec. */
    public double clickDelayMs = 100.0;

    /** Always starts disabled on launch. */
    public transient boolean enabled = false;

    private static AnvilClickerConfig instance;

    public static AnvilClickerConfig getInstance() {
        if (instance == null) instance = load();
        return instance;
    }

    private static AnvilClickerConfig load() {
        File file = CONFIG_PATH.toFile();
        if (file.exists()) {
            try (Reader r = new FileReader(file)) {
                AnvilClickerConfig cfg = GSON.fromJson(r, AnvilClickerConfig.class);
                if (cfg != null) return cfg;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        AnvilClickerConfig defaults = new AnvilClickerConfig();
        defaults.save();
        return defaults;
    }

    public void save() {
        try (Writer w = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(this, w);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
