package chadlymasterson.playerxp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class Config {

    public static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir();
    private final File CONFIG_FILE;

    // Give levels instead of experience
    private boolean shouldGiveLevels = false;
    private float baseXP = 5;
    private int baseLevels = 1;

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .setLenient()
            .create();

    public Config(String filename) {
        CONFIG_FILE = new File(String.valueOf(CONFIG_PATH) + "/playerxp", filename);

        load();
    }

    public void load() {
        if (!CONFIG_FILE.exists()) {
            save(); // Create the default values
            return;
        }

        try(FileReader reader = new FileReader(CONFIG_FILE)) {
            Config loaded = GSON.fromJson(reader, Config.class);

            this.shouldGiveLevels = loaded.shouldGiveLevels;
            this.baseXP = loaded.baseXP;
            this.baseLevels = loaded.baseLevels;

        } catch (IOException e) {
            PlayerXp.LOGGER.error("Failed to load config: " + e.getMessage());
        }
    }

    public void save() {
        CONFIG_FILE.getParentFile().mkdirs();
        try( FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            PlayerXp.LOGGER.error("Failed to save config: " + e.getMessage());
        }
    }

    public int getBaseLevels() {
        return this.baseLevels;
    }
    public float getBaseXP() {
        return this.baseXP;
    }
    public boolean shouldGiveLevels() {
        return this.shouldGiveLevels;
    }
}