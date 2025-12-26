package chadlymasterson.playerxp;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.api.reactive.ObservableSubscription;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("playerxp")
public class PlayerXp {

    public static final String MOD_ID = "playerxp";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static Config config;

    public PlayerXp() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        config = getConfig();
    }

    ObservableSubscription<PokemonCapturedEvent> captureEvent = CobblemonEvents.POKEMON_CAPTURED.subscribe( Priority.LOW, event -> {
        ServerPlayer player = event.getPlayer();

        int pkmnLvl = event.getPokemon().getLevel();

        handleXP(player, pkmnLvl);
    });


    // Subscribe to BattleVictoryEvent
    ObservableSubscription<BattleVictoryEvent> subscription = CobblemonEvents.BATTLE_VICTORY.subscribe( Priority.LOW, event -> {

        var winners = event.getWinners();
        var losers = event.getLosers();

        var loserLevel = 0;

        for(BattleActor actor : losers) {
            if(actor instanceof PokemonBattleActor battleActor) {
                Pokemon pkmn = battleActor.getPokemon().getOriginalPokemon();

                loserLevel += pkmn.getLevel();
            }
        }

        for(BattleActor actor: winners) {
            if(actor instanceof PlayerBattleActor player) {

                handleXP(player.getEntity(), loserLevel);
            }
        }

        return null;
    });

    private void handleXP(ServerPlayer player, int pokemonLevel) {
        if (!config.shouldGiveLevels()) {
            int xp = config.getBaseXP() * pokemonLevel < 1 ? 1 : (int)Math.floor(config.getBaseXP() * pokemonLevel);
            spawnXPOrbs(player, xp);
        } else {
            int levelsToGive = config.getBaseLevels() * pokemonLevel;

            int currentLevel = player.experienceLevel;
            int targetLevel = currentLevel + levelsToGive;

            int currentTotalXP = getExperienceForLevel(currentLevel);
            int targetTotalXP = getExperienceForLevel(targetLevel);

            int xpToGive = targetTotalXP - currentTotalXP;

            spawnXPOrbs(player, xpToGive);
        }
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

}
