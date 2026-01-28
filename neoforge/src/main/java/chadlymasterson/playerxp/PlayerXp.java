package chadlymasterson.playerxp;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.api.reactive.ObservableSubscription;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.common.eventbus.Subscribe;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Mod("playerxp")
public class PlayerXp {

    public static final String MOD_ID = "playerxp";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static Config config;
    private int days = 0;

    private Map<ServerPlayer, Integer> xpAwarded = new HashMap<>();

    public PlayerXp() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        config = getConfig();
    }

    ObservableSubscription<PokemonCapturedEvent> captureEvent = CobblemonEvents.POKEMON_CAPTURED.subscribe( Priority.LOW, event -> {
        ServerPlayer player = event.getPlayer();

        if(!config.isEnableDailyCap()) {
            handleXP(player, event.getPokemon().getLevel());
        } else {
            handleXPCap(player, event.getPokemon().getLevel());
        }

    });

    // Subscribe to BattleVictoryEvent
    ObservableSubscription<BattleVictoryEvent> subscription = CobblemonEvents.BATTLE_VICTORY.subscribe( Priority.LOW,event -> {

        var winners = event.getWinners();
        var losers = event.getLosers();

        AtomicInteger loserLevel = new AtomicInteger();

        for(BattleActor actor : losers) {
            if(actor instanceof PokemonBattleActor battleActor) {
                Pokemon pkmn = battleActor.getPokemon().getOriginalPokemon();

                loserLevel.addAndGet(pkmn.getLevel());
            }

            if(actor instanceof PlayerBattleActor battleActor) {
                List<BattlePokemon> pkmn = battleActor.getPokemonList();
                pkmn.forEach((i) -> {
                    Pokemon OG = i.getOriginalPokemon();
                    loserLevel.addAndGet(OG.getLevel());
                });
            }
        }

        for(BattleActor actor: winners) {
            if(actor instanceof PlayerBattleActor player && config.shouldGiveXpFromTrainerBattles()) {
                if(!config.isEnableDailyCap()) {
                    handleXP(player.getEntity(), loserLevel.get());
                } else {
                    handleXPCap(player.getEntity(), loserLevel.get());
                }
            }
        }

        return null;
    });

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Pre event) {
        ServerLevel level = event.getServer().getLevel(ServerLevel.OVERWORLD);

        long dayTime = level.dayTime();
        if( days == Math.round(level.dayTime() == 0 ? 0 : ((float) level.dayTime() / 24000))) {
            return;
        }
        days = Math.round(level.dayTime() == 0 ? 0 : ((float) level.dayTime() / 24000));

        xpAwarded.forEach((player, amount) -> {
            // Your logic here, e.g., player.giveExperiencePoints(amount);
            xpAwarded.put(player, 0);

            sendActionBar(player, "XP Cap has been reset!");

        });

        LOGGER.info(String.format("It is now day: %s. The XP cap has been reset!", days));
    }

    private void handleXP(ServerPlayer player, int pokemonLevel) {

        if (!config.shouldGiveLevels()) {
            int xp = config.getBaseXP() * pokemonLevel < 1 ? 1 : (int)Math.floor(config.getBaseXP() * pokemonLevel);
            spawnXPOrbs(player, xp);
        } else {

            int xpToGive = handleLevelReward(player, pokemonLevel);

            spawnXPOrbs(player, xpToGive);
        }
    }

    private void handleXPCap(ServerPlayer player, int pokemonLevel) {

        LOGGER.info(String.format("ShouldGiveLevels: %s", config.shouldGiveLevels()));

        if(!config.shouldGiveLevels()) {
            int xp = config.getBaseXP() * pokemonLevel < 1 ? 1 : (int)Math.floor(config.getBaseXP() * pokemonLevel);
            Integer xpCap = config.getDailyXpCap();
            Integer awarded = xpAwarded.get(player);

            if(awarded == null) {

                int diff = config.getDailyXpCap() - xp;
                int xpVal = xp;
                if(diff < 0) {
                    xpVal = (awarded + xp) + diff;
                }

                awarded = xpVal;
                xpAwarded.put(player, awarded);
                spawnXPOrbs(player, awarded);
            } else {
                if( awarded + xp > config.getDailyXpCap()) {
                    if(awarded >= config.getDailyXpCap()) {
                        sendActionBar(player, String.format("Daily XP maxed!"));
                        return;
                    }
                    int diff = config.getDailyXpCap() - (awarded + xp);
                    int xpVal = xp;
                    if(diff < 0) {
                        xpVal = (awarded + xp) + diff;
                    }

                    awarded += diff;
                    xpAwarded.put(player, awarded);
                    spawnXPOrbs(player, xp);
                } else {
                    awarded += xp;
                    xpAwarded.put(player, awarded + xp);
                    spawnXPOrbs(player, xp);
                }
            }

            sendActionBar(player, String.format("Daily XP: %s / %s", awarded, xpCap));
        } else {

            Integer lvlCap = config.getDailyLevelCap();
            Integer awarded = xpAwarded.get(player);
            Integer levelsToGive = 0;

            if (awarded == null) awarded = 0;

            if (awarded >= lvlCap) {
                sendActionBar(player, String.format("Daily XP maxed!"));
                return;
            }

            if(((pokemonLevel * config.getBaseLevels()) + awarded) > lvlCap) {
                levelsToGive = lvlCap - awarded;

                // Give levelsToGive;
                spawnXPOrbs(player, handleLevelReward(player, levelsToGive));

                xpAwarded.put(player, lvlCap);

            } else {
                levelsToGive = Math.min(pokemonLevel * config.getBaseLevels(), lvlCap - awarded);

                // Give levelsToGive;
                spawnXPOrbs(player, handleLevelReward(player, levelsToGive));

                xpAwarded.put(player, awarded + levelsToGive);

            }

            sendActionBar(player, String.format("Daily XP: %s / %s", xpAwarded.get(player), lvlCap));

        }
    }

    public int handleLevelReward(ServerPlayer player, int pokemonLevel) {
        var levelsToGive = config.getBaseLevels() * pokemonLevel;

        int currentLevel = player.experienceLevel;
        int targetLevel = currentLevel + levelsToGive;

        int currentTotalXP = getExperienceForLevel(currentLevel);
        int targetTotalXP = getExperienceForLevel(targetLevel);

        return targetTotalXP - currentTotalXP;
    }

    public int getExperienceForLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int)(2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int)(4.5 * level * level - 162.5 * level + 2220);
        }
    }

    private void spawnXPOrbs(ServerPlayer player, int xpAmount) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.getOnPos();
        ExperienceOrb orb = new ExperienceOrb(level, pos.getX(), pos.getY(), pos.getZ(), xpAmount);
        level.addFreshEntity(orb);
    }

    public static Config getConfig() {
        if (config == null) {
            config = new Config("playerxp.json");
            config.load();
        }

        return config;
    }

    public void sendActionBar(ServerPlayer player, String message) {
        Component text = Component.literal(message);
        ClientboundSetActionBarTextPacket packet = new ClientboundSetActionBarTextPacket(text);
        player.connection.send(packet);
    }

}
