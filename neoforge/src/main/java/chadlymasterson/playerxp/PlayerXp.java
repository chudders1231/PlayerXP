package chadlymasterson.playerxp;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod("cobblemon_multiplatform_mdk")
public class PlayerXp {

    public PlayerXp() {
        NeoForge.EVENT_BUS.register(this);
    }

}
