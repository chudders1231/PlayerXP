package chadlymasterson.playerxp;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.api.reactive.ObservableSubscription;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static chadlymasterson.playerxp.DataComponents.BOUND_PLAYERS;

public class PlayerXp implements ModInitializer {
    public static String MOD_ID = "playerxp";
    public static Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Config CONFIG = new Config("playerxp.json");

    @Override
    public void onInitialize() {

        DataComponents.init();

        UseEntityCallback.EVENT.register(this::onEntityCallback);

        ItemTooltipCallback.EVENT.register(this::onItemTooltipCallback);
    }


    ObservableSubscription<PokemonCapturedEvent> captureEvent = CobblemonEvents.POKEMON_CAPTURED.subscribe( Priority.LOW, event -> {
        ServerPlayer player = event.getPlayer();

        handleXP(player, event.getPokemon().getLevel());
    });


    // Subscribe to BattleVictoryEvent
    ObservableSubscription<BattleVictoryEvent> subscription = CobblemonEvents.BATTLE_VICTORY.subscribe( Priority.LOW,event -> {

        var winners = event.getWinners();
        var losers = event.getLosers();

        AtomicInteger loserLevel = new AtomicInteger();

        var isTrainer = isTrainerBattle(event.getBattle());

        for(BattleActor actor : losers) {

            LOGGER.info(String.format("PokemonList: %s", actor.getPokemonList().toString()));

            List<BattlePokemon> pkmn = actor.getPokemonList();

                pkmn.forEach( (i) -> {

                    loserLevel.getAndAdd(i.getOriginalPokemon().getLevel());

                });

        }

        for(BattleActor actor: winners) {
            if(actor instanceof PlayerBattleActor player) {

                List< ActiveBattlePokemon> activePokemon = player.getActivePokemon();

                var shouldGiveXP = isTrainer && CONFIG.shouldGiveXpFromTrainerBattles();

                if( !isTrainer ) shouldGiveXP = true;

                if(shouldGiveXP) {
                    handleXP(player.getEntity(), loserLevel.get());
                }
            }
        }

        return null;
    });

    private boolean isTrainerBattle(PokemonBattle battle) {
        return battle.isPvP() || battle.isPvN();
    }

    private void handleXP(ServerPlayer player, int pokemonLevel) {

        if (!CONFIG.shouldGiveLevels()) {
            int xp = CONFIG.getBaseXP() * pokemonLevel < 1 ? 1 : (int)Math.floor(CONFIG.getBaseXP() * pokemonLevel);
            if ( CONFIG.shouldLuckyXpBoost() && hasEggInInv(player) ) {
                xp *= CONFIG.getLuckyEggXpMultiplier();
            }

            if(CONFIG.getShouldExpShare() && hasExpShareInInv(player)) {
                Item share = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("cobblemon","exp_share"));
                List<ServerPlayer> players = new ArrayList<>();

                player.getInventory().items.stream().filter( stack -> !stack.isEmpty() && stack.getItem() == share).forEach(stack -> {
                    List<DataComponents.PlayerEntry> entry = stack.get(BOUND_PLAYERS);
                    if( entry != null && !entry.isEmpty()) {
                        for (DataComponents.PlayerEntry p : entry) {
                            ServerPlayer sP = player.getServer().getPlayerList().getPlayer(p.uuid());
                            if ( sP != null && player.distanceTo(sP) <= CONFIG.getExpShareRadius()){
                                players.add(sP);
                            }
                        }
                    }
                });

                players.add(player);
                for (ServerPlayer serverPlayer : players) {
                    spawnXPOrbs( serverPlayer, xp / players.size());
                }
            } else {
                spawnXPOrbs(player, xp);
            }
        } else {

            int xpToGive = handleLevelReward(player, pokemonLevel);
            if ( CONFIG.shouldLuckyXpBoost() && hasEggInInv(player) ) {
                xpToGive *= CONFIG.getLuckyEggXpMultiplier();
            }

            if(CONFIG.getShouldExpShare() && hasExpShareInInv(player)) {
                Item share = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("cobblemon","exp_share"));

                Set<ServerPlayer> players = new HashSet<>();

                player.getInventory().items.stream()
                        .filter( stack -> !stack.isEmpty() && stack.getItem() == share)
                        .forEach(stack -> {
                            List<DataComponents.PlayerEntry> entry = stack.get(BOUND_PLAYERS);

                            if( entry != null && !entry.isEmpty()) {
                                for (DataComponents.PlayerEntry p : entry) {
                                    ServerPlayer sP = player.getServer().getPlayerList().getPlayer(p.uuid());
                                    if ( sP != null && player.distanceTo(sP) <= CONFIG.getExpShareRadius()){
                                        players.add(sP);
                                    }
                                }
                            }
                });

                players.add(player);
                for (ServerPlayer serverPlayer : players) {
                    spawnXPOrbs( serverPlayer, xpToGive / players.size());
                }
            } else {
                spawnXPOrbs(player, xpToGive);
            }
        }
    }

    public boolean hasExpShareInInv( ServerPlayer player ) {

        Item share = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("cobblemon","exp_share"));

        return player.getInventory().items.stream().anyMatch(stack -> !stack.isEmpty() && stack.getItem() == share);
    }

    public int handleLevelReward(ServerPlayer player, int pokemonLevel) {
        var levelsToGive = CONFIG.getBaseLevels() * pokemonLevel;

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

    public boolean hasEggInInv( ServerPlayer player ) {

        Item egg = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("cobblemon","lucky_egg"));

        return player.getInventory().items.stream().anyMatch(stack -> !stack.isEmpty() && stack.getItem() == egg);
    }

    private void onItemTooltipCallback(ItemStack itemStack, Item.TooltipContext tooltipContext, TooltipFlag tooltipFlag, List<Component> components) {
        if( itemStack.is(CobblemonItems.EXP_SHARE) && CONFIG.getShouldExpShare()) {
            List<DataComponents.PlayerEntry> list = itemStack.get(BOUND_PLAYERS);
            if(list == null || list.isEmpty()) {
                components.add(Component.literal(""));
                components.add(Component.literal("No players bound"));
                components.add(Component.literal(""));
                components.add(Component.literal(String.format("Shares Exp with bound players within %s blocks", CONFIG.getExpShareRadius())).withStyle(ChatFormatting.GRAY));
            } else {
                components.add(Component.literal(String.format("Bound players (%s / %s): ", list.size(), CONFIG.getExpShareSize())).withStyle(ChatFormatting.GOLD));

                Minecraft mc = Minecraft.getInstance();
                if (mc.getConnection() == null) return;

                for(DataComponents.PlayerEntry entry : list) {
                    String name = entry.name();

                    components.add(Component.literal(name));
                }
                components.add(Component.literal(""));
                components.add(Component.literal(String.format("Shares Exp with bound players within %s blocks", CONFIG.getExpShareRadius())).withStyle(ChatFormatting.GRAY));
            }
        };
    }

    public static void removePlayer(ItemStack stack, UUID uuid) {
        List<DataComponents.PlayerEntry> old = stack.getOrDefault(BOUND_PLAYERS, DataComponents.PlayerEntry.EMPTY);

        ArrayList<DataComponents.PlayerEntry> updated = new ArrayList<>(old);

        updated.removeIf(entry -> entry.uuid().equals(uuid));

        stack.set(BOUND_PLAYERS, updated);
    }

    public static void addPlayer(ItemStack stack, UUID uuid, String name) {
        List<DataComponents.PlayerEntry> old = stack.getOrDefault(BOUND_PLAYERS, DataComponents.PlayerEntry.EMPTY);

        ArrayList<DataComponents.PlayerEntry> updated = new ArrayList<>(old);

        // optional: prevent duplicates
        for (DataComponents.PlayerEntry entry : updated) {
            if (entry.uuid().equals(uuid)) {
                removePlayer(stack, uuid);

                return;
            }
        }

        // optional: enforce max 4
        if (updated.size() >= CONFIG.getExpShareSize()) return;

        updated.add(new DataComponents.PlayerEntry(uuid, name));

        stack.set(BOUND_PLAYERS, updated);
    }

    private InteractionResult onEntityCallback(Player player, Level level, InteractionHand hand, Entity entity, @Nullable EntityHitResult entityHitResult) {
        if (entity instanceof Player targetPlayer) {
            // item used on another player
            ItemStack stack = player.getItemInHand(hand);

            if(stack.is(CobblemonItems.EXP_SHARE)) {
                List<DataComponents.PlayerEntry> list = stack.get(BOUND_PLAYERS);

                addPlayer(stack, targetPlayer.getUUID(), targetPlayer.getDisplayName().getString());
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    };
}
