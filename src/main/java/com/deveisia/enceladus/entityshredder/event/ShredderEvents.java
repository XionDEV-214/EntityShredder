package com.deveisia.enceladus.entityshredder.event;

import com.deveisia.enceladus.entityshredder.EntityShredder;
import com.deveisia.enceladus.entityshredder.data.LootCache;
import com.deveisia.enceladus.entityshredder.net.ShredderNetwork;

import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EntityShredder.MOD_ID)
public final class ShredderEvents {
    private ShredderEvents() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        LootCache.rebuildServer(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LootCache.clearServer();
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null || LootCache.get(false).isEmpty()) {
            LootCache.rebuildServer(event.getPlayerList().getServer());
        }
        if (event.getPlayer() != null) {
            ShredderNetwork.sendTo(event.getPlayer());
        } else {
            event.getPlayerList().getPlayers().forEach(ShredderNetwork::sendTo);
        }
    }
}
