package chadlymasterson.playerxp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Config {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().setLenient().create();
    private final transient File configFile; // Config file instance


    public Boolean shouldGiveLevels = false; // False = xp / True = levels
    public Float baseXP = 5.0f; // How much xp to give
    public Integer baseLevels = 1; // How many levels to give
    public Boolean xpFromPlayerBattles = true; // Should players get xp from player battles (enabled by default)?
    public Boolean luckyEggXpBoost = true; // Should players get boosted exp when holding a lucky egg?
    public Float luckyEggXpMultiplier = 1.5f; // How much should the exp be multiplied by? Should always be above 1!
    public Boolean shouldExpShare = true; // Should the xp share item share xp between players when linked and in the players' inventory?
    public Integer expShareSize = 1; // How many additional players should be linked to the xp share?
    public Float expShareRadius = 35f; // How many blocks away can the additional players be to receive the xp from the xp share?

    // Config class instance
    public Config(String filename) {

        configFile = FabricLoader.getInstance().getConfigDir().resolve("playerxp").resolve(filename).toFile();

        load();
    }

    public void load() {

        if (!configFile.exists()) {
            save(); // Create the default values
            return;
        }

        try(FileReader reader = new FileReader(configFile)) {
            Config loaded = GSON.fromJson(reader, Config.class);

            if (loaded == null) {
                PlayerXp.LOGGER.warn("Config was empty. Using defaults.");
                save();
                return;
            }

            this.shouldGiveLevels = valueOrDefault(loaded.shouldGiveLevels, this.shouldGiveLevels);
            this.baseXP = valueOrDefault(loaded.baseXP, this.baseXP);
            this.baseLevels = valueOrDefault(loaded.baseLevels, this.baseLevels);
            this.xpFromPlayerBattles = valueOrDefault(loaded.xpFromPlayerBattles, this.xpFromPlayerBattles);
            this.luckyEggXpBoost = valueOrDefault(loaded.luckyEggXpBoost, this.luckyEggXpBoost);
            this.luckyEggXpMultiplier = valueOrDefault(loaded.luckyEggXpMultiplier, this.luckyEggXpMultiplier);
            this.shouldExpShare = valueOrDefault(loaded.shouldExpShare, this.shouldExpShare);
            this.expShareSize = valueOrDefault(loaded.expShareSize, this.expShareSize);
            this.expShareRadius = valueOrDefault(loaded.expShareRadius, this.expShareRadius);

            // Save back to file to add any new default options missing in the file
            save();

        } catch (Exception e) {
            PlayerXp.LOGGER.error("Failed to load config", e);
        }
    }

    public void save() {
        configFile.getParentFile().mkdirs();
        try( FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            PlayerXp.LOGGER.error("Failed to save config: " + e.getMessage());
        }
    }

    private static <T> T valueOrDefault(T loaded, T current) {
        return loaded != null ? loaded : current;
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
    public boolean shouldGiveXpFromTrainerBattles() {
        return this.xpFromPlayerBattles;
    }
    public boolean shouldLuckyXpBoost() { return this.luckyEggXpBoost; }
    public float getLuckyEggXpMultiplier () { return this.luckyEggXpMultiplier; }
    public boolean getShouldExpShare() { return this.shouldExpShare; }
    public int getExpShareSize() { return this.expShareSize; }
    public float getExpShareRadius() { return this.expShareRadius; }
}