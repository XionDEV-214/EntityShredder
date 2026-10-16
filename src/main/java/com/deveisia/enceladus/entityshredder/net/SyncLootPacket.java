package com.deveisia.enceladus.entityshredder.net;

import com.deveisia.enceladus.entityshredder.data.EntityLoot;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class SyncLootPacket {
    private final List<EntityLoot> entities;

    public SyncLootPacket(List<EntityLoot> entities) {
        this.entities = entities;
    }

    public static SyncLootPacket of(Map<ResourceLocation, EntityLoot> data) {
        return new SyncLootPacket(new ArrayList<>(data.values()));
    }

    public static void encode(SyncLootPacket msg, FriendlyByteBuf buf) {
        Map<ResourceLocation, Integer> index = new LinkedHashMap<>();
        for (EntityLoot e : msg.entities) {
            for (EntityLoot.Drop d : e.drops()) index.putIfAbsent(d.itemId(), index.size());
        }
        buf.writeVarInt(index.size());
        for (ResourceLocation id : index.keySet()) buf.writeResourceLocation(id);
        buf.writeVarInt(msg.entities.size());
        for (EntityLoot e : msg.entities) {
            buf.writeResourceLocation(e.entityId());
            buf.writeVarInt(e.drops().size());
            for (EntityLoot.Drop d : e.drops()) {
                buf.writeVarInt(index.get(d.itemId()));
                buf.writeVarInt(d.count());
            }
        }
    }

    public static SyncLootPacket decode(FriendlyByteBuf buf) {
        int itemCount = buf.readVarInt();
        List<ResourceLocation> items = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) items.add(buf.readResourceLocation());
        int entityCount = buf.readVarInt();
        List<EntityLoot> list = new ArrayList<>(entityCount);
        for (int i = 0; i < entityCount; i++) {
            ResourceLocation id = buf.readResourceLocation();
            int dropCount = buf.readVarInt();
            List<EntityLoot.Drop> drops = new ArrayList<>(dropCount);
            for (int j = 0; j < dropCount; j++) {
                ResourceLocation item = items.get(buf.readVarInt());
                drops.add(new EntityLoot.Drop(item, buf.readVarInt()));
            }
            list.add(new EntityLoot(id, List.copyOf(drops)));
        }
        return new SyncLootPacket(list);
    }

    public static void handle(SyncLootPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientLootSync.apply(msg.entities)));
        ctx.get().setPacketHandled(true);
    }
}
