package com.deveisia.enceladus.entityshredder.jei;

import com.deveisia.enceladus.entityshredder.EntityShredder;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Renders a "dummy" entity inside a JEI recipe, like the player model in the inventory screen,
 * but for any entity type and scaled so the whole model fits the given box.
 * <p>
 * Fitting uses the model's real geometry, not its hitbox: the first time an entity is shown it is rendered
 * once into a vertex recorder that only measures how far the model reaches (wings, tails, tentacles, held
 * items and armor included). The horizontal extent is the model's radius around its vertical axis, so the
 * fit holds at every spin angle. The box is also scissored as a last line of defence.
 * <p>
 * Anything that fails to create or render is remembered as broken and the caller draws a fallback, so one
 * bad modded mob can never crash the JEI screen.
 */
public final class EntityPreview {
    private EntityPreview() {}

    private static final float DEG = (float) (Math.PI / 180.0);
    /** Largest tilt the caller may ask for; the fit reserves room for it. */
    public static final float MAX_PITCH = 12f;
    /** Fraction of the box the model may occupy. */
    private static final float FILL = 0.9f;

    /** Model extent in blocks, entity-local, y up from the feet. */
    private record Bounds(float radius, float minY, float maxY) {
        float height() {
            return Math.max(0.05f, maxY - minY);
        }
    }

    private static final Map<ResourceLocation, Entity> CACHE = new HashMap<>();
    private static final Map<ResourceLocation, Bounds> BOUNDS = new HashMap<>();
    private static final Set<ResourceLocation> BROKEN = new HashSet<>();
    @Nullable
    private static Level cachedLevel;

    @Nullable
    private static Entity get(ResourceLocation id) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return null;
        if (level != cachedLevel) {
            CACHE.clear();
            BOUNDS.clear();
            BROKEN.clear();
            cachedLevel = level;
        }
        if (BROKEN.contains(id)) return null;
        Entity cached = CACHE.get(id);
        if (cached != null) return cached;

        Entity entity = null;
        try {
            if (ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
                EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
                if (type != null) entity = type.create(level);
            }
        } catch (Throwable t) {
            EntityShredder.LOGGER.debug("[{}] cannot create JEI preview for {}: {}", EntityShredder.MOD_ID, id,
                    t.toString());
            entity = null;
        }
        if (entity == null) {
            BROKEN.add(id);
            return null;
        }
        CACHE.put(id, entity);
        return entity;
    }

    /**
     * Draws the entity centered in the box (x, y, w, h), in recipe-local coordinates.
     *
     * @param yaw   degrees around the vertical axis, 0 = facing the viewer
     * @param pitch degrees, clamped to +-{@link #MAX_PITCH}
     * @return false if nothing was drawn
     */
    public static boolean render(GuiGraphics gg, ResourceLocation id, int x, int y, int w, int h, float yaw,
                                 float pitch) {
        Entity entity = get(id);
        if (entity == null) return false;
        Bounds b = bounds(id, entity);
        pitch = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch));

        // Horizontal: the model spins, so its full diameter must fit at any angle.
        // Vertical: height plus what a maximal tilt can add at the top and bottom.
        float tiltPad = 2f * b.radius() * (float) Math.sin(MAX_PITCH * DEG);
        float scale = Math.min(w * FILL / (2f * b.radius()), h * FILL / (b.height() + tiltPad));
        scale = Math.max(0.5f, Math.min(scale, 200f));

        float cx = x + w / 2f;
        // Put the middle of the model (not its feet) at the middle of the box.
        float baseY = y + h / 2f + (b.minY() + b.maxY()) / 2f * scale;

        setRotations(entity, yaw, pitch);

        Minecraft mc = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        PoseStack pose = gg.pose();
        boolean ok = true;
        boolean scissor = enableScissor(gg, x, y, w, h);
        pose.pushPose();
        try {
            pose.translate(cx, baseY, 50.0);
            pose.mulPoseMatrix(new Matrix4f().scaling(scale, scale, -scale));
            Quaternionf flip = new Quaternionf().rotateZ((float) Math.PI);
            Quaternionf tilt = new Quaternionf().rotateX(pitch * DEG);
            flip.mul(tilt);
            pose.mulPose(flip);

            Lighting.setupForEntityInInventory();
            tilt.conjugate();
            dispatcher.overrideCameraOrientation(tilt);
            dispatcher.setRenderShadow(false);
            final float entityYaw = 180f + yaw;
            RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0.0, 0.0, 0.0, entityYaw, 1.0f, pose,
                    gg.bufferSource(), LightTexture.FULL_BRIGHT));
            gg.flush();
        } catch (Throwable t) {
            ok = false;
            markBroken(id, t);
            try {
                gg.bufferSource().endBatch();
            } catch (Throwable ignored) {}
        } finally {
            dispatcher.setRenderShadow(true);
            pose.popPose();
            Lighting.setupFor3DItems();
            if (scissor) gg.disableScissor();
        }
        return ok;
    }

    private static void markBroken(ResourceLocation id, Throwable t) {
        BROKEN.add(id);
        CACHE.remove(id);
        BOUNDS.remove(id);
        EntityShredder.LOGGER.warn("[{}] JEI preview of {} failed, using fallback: {}", EntityShredder.MOD_ID, id,
                t.toString());
    }

    /** GuiGraphics scissor works in screen space, so map the recipe-local box through the current pose. */
    private static boolean enableScissor(GuiGraphics gg, int x, int y, int w, int h) {
        try {
            Matrix4f m = gg.pose().last().pose();
            Vector3f a = m.transformPosition(new Vector3f(x, y, 0));
            Vector3f c = m.transformPosition(new Vector3f(x + w, y + h, 0));
            gg.enableScissor((int) Math.floor(Math.min(a.x, c.x)), (int) Math.floor(Math.min(a.y, c.y)),
                    (int) Math.ceil(Math.max(a.x, c.x)), (int) Math.ceil(Math.max(a.y, c.y)));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Bounds bounds(ResourceLocation id, Entity entity) {
        Bounds b = BOUNDS.get(id);
        if (b != null) return b;
        b = measure(entity);
        if (b == null) b = hitbox(entity);
        BOUNDS.put(id, b);
        return b;
    }

    private static Bounds hitbox(Entity entity) {
        float half = Math.max(0.125f, entity.getBbWidth() / 2f);
        // Corner of the square footprint is what sticks out furthest while spinning.
        return new Bounds(half * 1.4143f, 0f, Math.max(0.25f, entity.getBbHeight()));
    }

    /** Renders once into a recorder that keeps only the vertex extents. Nothing is drawn on screen. */
    @Nullable
    private static Bounds measure(Entity entity) {
        Recorder rec = new Recorder();
        MultiBufferSource source = type -> rec;
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        try {
            setRotations(entity, 0, 0);
            dispatcher.overrideCameraOrientation(new Quaternionf());
            dispatcher.setRenderShadow(false);
            dispatcher.render(entity, 0.0, 0.0, 0.0, 180f, 1.0f, new PoseStack(), source, LightTexture.FULL_BRIGHT);
        } catch (Throwable t) {
            return null;
        } finally {
            dispatcher.setRenderShadow(true);
        }
        if (rec.count < 3 || !Float.isFinite(rec.radius2) || rec.maxY <= rec.minY) return null;
        float radius = (float) Math.sqrt(rec.radius2);
        if (radius < 0.01f) return null;
        // Guard against a stray vertex far away (e.g. a beam or particle quad) making the mob tiny.
        Bounds hb = hitbox(entity);
        float limit = 8f * Math.max(hb.radius(), hb.height());
        if (radius > limit || rec.maxY - rec.minY > limit) return hb;
        return new Bounds(radius, rec.minY, rec.maxY);
    }

    private static final class Recorder implements VertexConsumer {
        int count;
        float radius2;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) {
                count++;
                radius2 = Math.max(radius2, (float) (x * x + z * z));
                minY = Math.min(minY, (float) y);
                maxY = Math.max(maxY, (float) y);
            }
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }

        @Override
        public void endVertex() {}

        @Override
        public void defaultColor(int r, int g, int b, int a) {}

        @Override
        public void unsetDefaultColor() {}
    }

    private static void setRotations(Entity entity, float yaw, float pitch) {
        float body = 180f + yaw;
        entity.setYRot(body);
        entity.yRotO = body;
        entity.setXRot(0);
        entity.xRotO = 0;
        if (entity instanceof LivingEntity living) {
            living.yBodyRot = body;
            living.yBodyRotO = body;
            living.yHeadRot = body;
            living.yHeadRotO = body;
            living.setXRot(-pitch * 0.5f);
            living.xRotO = living.getXRot();
        }
    }
}
