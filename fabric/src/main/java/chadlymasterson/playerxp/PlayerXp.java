package chadlymasterson.playerxp;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.reactive.ObservableSubscription;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class PlayerXp implements ModInitializer {
    static String MOD_ID = "playerxp";
    static Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static Config config;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStartTick);
    }

    private void onServerStartTick(MinecraftServer minecraftServer) {
        config = getConfig();
    }


    // Subscribe to BattleVictoryEvent
    ObservableSubscription<BattleVictoryEvent> subscription = CobblemonEvents.BATTLE_VICTORY.subscribe( Priority.LOW,event -> {

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

    private void handleXP(ServerPlayer player, int level) {
        if (!config.shouldGiveLevels()) {
            int xp = config.getBaseXP() * level < 1 ? 1 : (int)Math.floor(config.getBaseXP() * level);

            player.giveExperiencePoints(xp);
        } else {
            int levels = config.getBaseLevels() * level;
            player.giveExperienceLevels(levels);
        }
    }

    public static Config getConfig() {
        if (config == null) {
            config = new Config("playerxp.json");
            config.load();
        }

        return config;
    }
}
