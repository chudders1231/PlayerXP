package chadlymasterson.playerxp;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.reactive.ObservableSubscription;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    public static Config config;

    public PlayerXp() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        config = getConfig();
    }

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

    private void handleXP(ServerPlayer player, int level) {
        if (!config.shouldGiveLevels) {
            var xp = config.baseXP * level;
            player.giveExperiencePoints(xp);
        } else if(config.shouldGiveLevels) {
            var levels = config.baseLevels * level;
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
