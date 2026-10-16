package com.deveisia.enceladus.entityshredder.client;

import com.deveisia.enceladus.entityshredder.EntityShredder;
import com.deveisia.enceladus.entityshredder.registry.ShredderMachines;

import com.gregtechceu.gtceu.client.util.ExtendedBlockModelRotation;
import com.gregtechceu.gtceu.data.pack.GTDynamicResourcePack;
import com.gregtechceu.gtceu.data.pack.event.RegisterDynamicResourcesEvent;

import com.google.gson.JsonObject;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 멀티블록 컨트롤러의 블록스테이트 / 블록 모델 / 아이템 모델을 런타임에 GTCEu 동적 리소스 팩으로 주입한다.
 * <p>
 * GTCEu 7.5.x 의 {@code workableCasingModel(...)} 은 데이터젠 시점에만 JSON 을 만들기 때문에,
 * 데이터젠 결과물을 jar 에 넣지 않는 애드온은 직접 JSON 을 넣어줘야 한다.
 * 여기서 만드는 JSON 은 {@code GTMachineModels.createWorkableCasingMachineModel} 의 결과와 같은 구조다.
 * <p>
 * 핵심은 {@code texture_overrides.all}: 해치/버스 등 파트가 형성된 상태에서 컨트롤러 모델의
 * 이 값을 읽어 케이싱 외장재로 덮어씌워진다.
 */
@Mod.EventBusSubscriber(modid = EntityShredder.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ShredderModels {
    private ShredderModels() {}

    private static final String TEMPLATE = "gtceu:block/machine/template/cube_all/sided";

    @SubscribeEvent
    public static void onRegisterDynamicResources(RegisterDynamicResourcesEvent event) {
        register("entity_shredder");
        register("advanced_entity_shredder");
    }

    private static void register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(EntityShredder.MOD_ID, name);
        // GT 데이터젠과 같은 위치: assets/<modid>/models/block/machine/<name>.json
        ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(EntityShredder.MOD_ID, "machine/" + name);
        String modelPath = EntityShredder.MOD_ID + ":block/machine/" + name;

        GTDynamicResourcePack.addBlockState(id, blockState(modelPath));
        GTDynamicResourcePack.addBlockModel(modelId, blockModel(id));

        JsonObject item = new JsonObject();
        item.addProperty("parent", modelPath);
        GTDynamicResourcePack.addItemModel(id, item);
    }

    /**
     * 멀티블록 컨트롤러는 facing 과 upwards_facing 두 속성을 모두 가진다.
     * 바닐라 blockstate 는 부분 키를 매칭하지 않으므로 모든 조합(4 x 4)을 채워야 한다.
     */
    private static JsonObject blockState(String modelPath) {
        JsonObject variants = new JsonObject();
        for (Direction front : Direction.Plane.HORIZONTAL) {
            for (Direction up : Direction.Plane.HORIZONTAL) {
                ExtendedBlockModelRotation rot = ExtendedBlockModelRotation.get(front, up);
                JsonObject v = new JsonObject();
                v.addProperty("model", modelPath);
                if (rot.getAngleX() != 0) v.addProperty("x", rot.getAngleX());
                if (rot.getAngleY() != 0) v.addProperty("y", rot.getAngleY());
                // GTCEu 가 variant 에 추가한 z 회전 속성
                if (rot.getAngleZ() != 0) v.addProperty("gtceu:z", rot.getAngleZ());
                variants.add("facing=" + front.getName() + ",upwards_facing=" + up.getName(), v);
            }
        }
        JsonObject root = new JsonObject();
        root.add("variants", variants);
        return root;
    }

    private static JsonObject blockModel(ResourceLocation machineId) {
        String casing = ShredderMachines.BASE_CASING.toString();
        String overlay = ShredderMachines.OVERLAY.toString();

        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/block");
        model.addProperty("loader", "gtceu:machine");
        model.addProperty("machine", machineId.toString());

        // 상태 키는 속성 이름 알파벳 순: is_formed, recipe_logic_status
        JsonObject variants = new JsonObject();
        for (boolean formed : new boolean[] { false, true }) {
            // research_station 오버레이에는 _paused 텍스처가 없어 일반 텍스처로 대체된다(GT 와 동일한 폴백).
            addState(variants, formed, "idle", casing, overlay + "/overlay_front",
                    overlay + "/overlay_front_emissive");
            addState(variants, formed, "working", casing, overlay + "/overlay_front_active",
                    overlay + "/overlay_front_active_emissive");
            addState(variants, formed, "waiting", casing, overlay + "/overlay_front_active",
                    overlay + "/overlay_front_active_emissive");
            addState(variants, formed, "suspend", casing, overlay + "/overlay_front",
                    overlay + "/overlay_front_emissive");
        }
        model.add("variants", variants);

        // 형성된 멀티블록의 해치/버스가 컨트롤러 케이싱 텍스처로 덮어씌워지게 하는 부분
        JsonObject overrides = new JsonObject();
        overrides.addProperty("all", casing);
        model.add("texture_overrides", overrides);
        return model;
    }

    private static void addState(JsonObject variants, boolean formed, String status, String casing,
                                 String overlayFront, String overlayFrontEmissive) {
        JsonObject textures = new JsonObject();
        textures.addProperty("all", casing);
        textures.addProperty("overlay_front", overlayFront);
        textures.addProperty("overlay_front_emissive", overlayFrontEmissive);

        JsonObject nested = new JsonObject();
        nested.addProperty("parent", TEMPLATE);
        nested.add("textures", textures);

        JsonObject entry = new JsonObject();
        entry.add("model", nested);
        variants.add("is_formed=" + formed + ",recipe_logic_status=" + status, entry);
    }
}
