package chadlymasterson.playerxp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

public class DataComponents {

    public static DataComponentType<List<PlayerEntry>> BOUND_PLAYERS;
    private static Codec<List<PlayerEntry>> LIST_CODEC;
    public static StreamCodec<FriendlyByteBuf, List<PlayerEntry>> LIST_STREAM_CODEC;

    public static void init() {
        LIST_CODEC = PlayerEntry.CODEC.listOf();
        LIST_STREAM_CODEC = PlayerEntry.STREAM_CODEC.apply(ByteBufCodecs.list());

        BOUND_PLAYERS = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(PlayerXp.MOD_ID, "bound_players"),
                DataComponentType.<List<PlayerEntry>>builder()
                        .persistent(LIST_CODEC)
                        .networkSynchronized(LIST_STREAM_CODEC)
                        .build()
        );
    }

    public record PlayerEntry(UUID uuid, String name) {
        public static final List<PlayerEntry> EMPTY = List.of();

        public static final Codec<PlayerEntry> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        UUIDUtil.CODEC.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                        Codec.STRING.fieldOf("name").forGetter(PlayerEntry::name)
                ).apply(instance, PlayerEntry::new));

        public static final StreamCodec<FriendlyByteBuf, PlayerEntry> STREAM_CODEC =
                StreamCodec.of(
                        (buf, entry) -> {
                            buf.writeUUID(entry.uuid());
                            buf.writeUtf(entry.name());
                        },
                        buf -> new PlayerEntry(buf.readUUID(), buf.readUtf())
                );

    }

}


