package chadlymasterson.playerxp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class Config {

    public static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve("playerxp");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().setLenient().create();
    private static File CONFIG_FILE;

    private Boolean shouldGiveLevels = false; // False = xp / True = levels
    private Float baseXP = 5.0f; // How much xp to give
    private Integer baseLevels = 1; // How many levels to give
    private Boolean enableDailyCap = false; // Cap levels to in-game days (disabled by default)
    private Integer dailyXpCap = 1000; // How much xp can you earn daily?
    private Integer dailyLevelCap = 10; // How many levels can you earn daily?
    private Boolean xpFromPlayerBattles = true; // Should players get xp from player battles (enabled by default)?

    // Config class instance
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

            this.shouldGiveLevels = (loaded.shouldGiveLevels != null) ? loaded.shouldGiveLevels : true;
            this.baseXP = (loaded.baseXP != null) ? loaded.baseXP : 100;
            this.baseLevels = (loaded.baseLevels != null) ? loaded.baseLevels : 1;
            this.enableDailyCap = (loaded.enableDailyCap != null) ? loaded.enableDailyCap : false;
            this.dailyXpCap = (loaded.dailyXpCap != null) ? loaded.dailyXpCap : 1000;
            this.dailyLevelCap = (loaded.dailyLevelCap != null) ? loaded.dailyLevelCap : 5;
            this.xpFromPlayerBattles = (loaded.xpFromPlayerBattles != null) ? loaded.xpFromPlayerBattles : true;

            // Save back to file to add any new default options missing in the file
            save();

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
    public boolean isEnableDailyCap() {
        return this.enableDailyCap;
    }
    public int getDailyXpCap() {
        return this.dailyXpCap;
    }
    public int getDailyLevelCap() {
        return this.dailyLevelCap;
    }
    public boolean shouldGiveXpFromTrainerBattles() {
        return this.xpFromPlayerBattles;
    }
}
