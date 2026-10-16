package com.deveisia.enceladus.entityshredder.net;

import com.deveisia.enceladus.entityshredder.EntityShredder;
import com.deveisia.enceladus.entityshredder.data.LootCache;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ShredderNetwork {
    private ShredderNetwork() {}

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(EntityShredder.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void init() {
        CHANNEL.registerMessage(0, SyncLootPacket.class, SyncLootPacket::encode, SyncLootPacket::decode,
                SyncLootPacket::handle);
    }

    public static void sendTo(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), SyncLootPacket.of(LootCache.get(false)));
    }
}
