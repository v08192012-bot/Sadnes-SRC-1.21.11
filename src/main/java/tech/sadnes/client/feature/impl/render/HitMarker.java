package tech.sadnes.client.feature.impl.render;

import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.animation.PerspectiveAnimation;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.AttackEntityEvent;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.PlayerTickEvent;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.MeshDrawUtil;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.shader.Pipelines;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.ui.RadialMenu;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.TimerUtil;

@FeatureInfo(
    name = "HitMarker",
    description = "feature.impl.render.hitmarker",
    displayName = "feature.impl.render.hitmarker",
    category = Category.VISUALS,
    subcategory = Subcategory.HUD_HELPERS
)
public final class HitMarker extends Feature {
    private final GroupSetting marker2d = new GroupSetting("hitmarker.2d", true).setIcon(Icon.CROSSHAIRS);
    private final NumberSetting marker2dDuration = new NumberSetting(this.marker2d, "hitmarker.2d.duration", 500.0F, 100.0F, 2000.0F, 50.0F, Unit.MILLISECOND);
    private final NumberSetting marker2dSize = new NumberSetting(this.marker2d, "hitmarker.2d.size", 100.0F, 30.0F, 150.0F, 5.0F, Unit.PERCENT);
    private final GroupSetting marker3d = new GroupSetting("hitmarker.3d", true).setIcon(Icon.PERSON_BURST);
    private final NumberSetting marker3dDuration = new NumberSetting(this.marker3d, "hitmarker.3d.duration", 500.0F, 100.0F, 2000.0F, 50.0F, Unit.MILLISECOND);
    private final NumberSetting marker3dSize = new NumberSetting(this.marker3d, "hitmarker.3d.size", 100.0F, 30.0F, 150.0F, 5.0F, Unit.PERCENT);
    private final GroupSetting damage = new GroupSetting("hitmarker.damage", true).setIcon(Icon.HEART_CRACK);
    private final NumberSetting damageDuration = new NumberSetting(this.damage, "hitmarker.damage.duration", 1000.0F, 200.0F, 3000.0F, 50.0F, Unit.MILLISECOND);
    private final NumberSetting damageSize = new NumberSetting(this.damage, "hitmarker.damage.size", 100.0F, 30.0F, 150.0F, 5.0F, Unit.PERCENT);
    private final TimerUtil hitTimer = new TimerUtil();
    private final List<HitMarker.WorldMarker> worldMarkers = new ArrayList<>();
    private final List<HitMarker.DamageText> damageTexts = new ArrayList<>();
    private final List<HitMarker.TrackedHit> trackedHits = new ArrayList<>();

    @Override
    public void onDisable() {
        super.onDisable();
        this.hitTimer.setTime(0L);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.clearMarkers();
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.clearMarkers();
    }

    private void clearMarkers() {
        this.worldMarkers.clear();
        this.damageTexts.clear();
        this.trackedHits.clear();
    }

    @EventHandler
    public void onAttack(AttackEntityEvent attackEntityEvent) {
        if (attackEntityEvent.getStage() == AttackEntityEvent.Stage.POST && mc.player != null) {
            Entity entity2 = attackEntityEvent.getTarget();
            if (entity2 == null) {
                return;
            }

            this.hitTimer.reset();
            Vec3d vec3d = this.getHitPos(entity2);
            if (this.worldMarkers.size() >= 16) {
                this.worldMarkers.remove(0);
            }

            Vec3d vec3d2 = new Vec3d(0.0, 1.0, 0.0);
            Vec3d vec3d3 = mc.player.getRotationVec(1.0F).normalize();
            Vec3d vec3d4 = vec3d3.crossProduct(vec3d2).normalize();
            Vec3d vec3d5 = vec3d4.crossProduct(vec3d3).normalize();
            Vec3d vec3d6 = vec3d4.add(vec3d5).normalize();
            Vec3d vec3d7 = vec3d4.subtract(vec3d5).normalize();
            this.worldMarkers.add(new HitMarker.WorldMarker(vec3d, new TimerUtil(), vec3d6, vec3d7));
            if (entity2 instanceof LivingEntity livingEntity) {
                this.trackedHits.removeIf(trackedHit -> trackedHit.entity() == livingEntity);
                this.trackedHits.add(new HitMarker.TrackedHit(livingEntity, vec3d, getTotalHealth(livingEntity), new TimerUtil()));
                return;
            }
        }
    }

    private Vec3d getHitPos(Entity entity) {
        if (mc.crosshairTarget instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() == entity) {
            return entityHitResult.getPos();
        } else {
            Box box = entity.getBoundingBox();
            return box.raycast(mc.player.getEyePos(), box.getCenter()).orElse(box.getCenter());
        }
    }

    private static float getTotalHealth(LivingEntity livingEntity) {
        return livingEntity.getHealth() + livingEntity.getAbsorptionAmount();
    }

    @EventHandler
    public void onPlayerTick(PlayerTickEvent playerTickEvent) {
        if (!this.trackedHits.isEmpty()) {
            Iterator iterator2 = this.trackedHits.iterator();

            while (iterator2.hasNext()) {
                HitMarker.TrackedHit trackedHit = (HitMarker.TrackedHit)iterator2.next();
                float f = trackedHit.health() - getTotalHealth(trackedHit.entity());
                if (trackedHit.timer().getElapsed() > 1000L || trackedHit.entity().isRemoved() || !(f <= 0.0F)) {
                    iterator2.remove();
                    if (!(f <= 0.0F)) {
                        if (this.damageTexts.size() >= 16) {
                            this.damageTexts.remove(0);
                        }

                        this.damageTexts.add(new HitMarker.DamageText(trackedHit.pos(), formatDamage(f), new TimerUtil()));
                    }
                }
            }
        }
    }

    private static String formatDamage(float f) {
        float f2 = Math.round(f * 10.0F) / 10.0F;
        return f2 % 1.0F == 0.0F ? "-" + (int)f2 : String.format(Locale.ROOT, "-%.1f", f2);
    }

    @EventHandler
    public void onRender(RenderEvent renderEvent) {
        if (mc.player != null && mc.world != null) {
            if (renderEvent.isWorld()) {
                this.render3dMarkers(renderEvent);
            } else if (renderEvent.isHudPost()) {
                this.render2dMarker(renderEvent.getContext());
                this.renderDamageTexts(renderEvent.getContext());
            }
        }
    }

    private void render2dMarker(DrawContext drawContext) {
        if (this.marker2d.isEnabled()) {
            long l = this.marker2dDuration.getValue().longValue();
            long l2 = this.hitTimer.getElapsed();
            if (l2 <= l) {
                if (!PerspectiveAnimation.INSTANCE.isThirdPerson() && !RadialMenu.isAnyOpen()) {
                    float f = mc.getWindow().getScaledWidth() / 2.0F;
                    float f2 = mc.getWindow().getScaledHeight() / 2.0F;
                    float f3 = (float)l2 / (float)l;
                    float f4 = 1.0F - (float)Easings.EXPO_OUT.ease(Math.min(1.0F, f3 * 2.5F));
                    float f5 = 1.0F - (float)Easings.QUAD_IN.ease(f3);
                    float f6 = this.marker2dSize.getValue() / 100.0F;
                    float f7 = (7.0F + 4.0F * f4) * f6;
                    float f8 = (5.6F + 2.6F * f4) * f6;
                    float f9 = 1.5F * f6;
                    ColorUtil.multiplyAlpha(Theme.getPrimary(), f5 * 0.52F);
                    int i = ColorUtil.multiplyAlpha(Theme.getTextColor(), f5 * 0.96F);
                    ColorUtil.multiplyAlpha(Theme.getPrimary(), f5 * 0.75F);

                    for (int i2 = 0; i2 < 4; i2++) {
                        MatrixUtil.push(drawContext);
                        MatrixUtil.rotate(drawContext, f, f2, 45.0F + i2 * 90.0F);
                        float f10 = f2 - f8 - f7;
                        new RenderUtil(drawContext, f - f9 / 2.0F, f10 + 0.5F * f6, f9, f7 - 0.1F * f6)
                            .outline(2.0F * f6, i, 0, i, 0, Radius.all(0.7F * f6))
                            .rect(i, Radius.all(0.7F * f6));
                        MatrixUtil.pop(drawContext);
                    }
                }
            }
        }
    }

    private void render3dMarkers(RenderEvent renderEvent) {
        if (this.marker3d.isEnabled() && !this.worldMarkers.isEmpty()) {
            long l = this.marker3dDuration.getValue().longValue();
            this.worldMarkers.removeIf(worldMarker -> worldMarker.timer().getElapsed() > l);
            if (!this.worldMarkers.isEmpty()) {
                Vec3d vec3d = renderEvent.getCamera().getCameraPos();
                Matrix4f matrix4f = renderEvent.getMatrices().peek().getPositionMatrix();
                float f = this.marker3dSize.getValue() / 100.0F;
                int i = this.worldMarkers.size();

                try (BufferAllocator bufferAllocator = new BufferAllocator(i * 512 + 1024)) {
                    BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_COLOR);

                    for (int i2 = 0; i2 < i; i2++) {
                        HitMarker.WorldMarker worldMarker2 = this.worldMarkers.get(i2);
                        Vec3d vec3d2 = worldMarker2.pos();
                        if (ProjectionUtil.isVisible(vec3d2.x, vec3d2.y, vec3d2.z)) {
                            float f2 = (float)worldMarker2.timer().getElapsed() / (float)l;
                            float f3 = 1.0F - (float)Easings.EXPO_OUT.ease(Math.min(1.0F, f2 * 1.5F));
                            float f4 = 1.0F - (float)Easings.QUAD_IN.ease(f2);
                            int i3 = ColorUtil.multiplyAlpha(Theme.getPrimary(), f4);
                            ColorUtil.multiplyAlpha(Theme.getPrimary(), f4 * 0.35F);
                            float f5 = (0.055F + 0.03F * f3) * f;
                            float f6 = f5 + 0.08F * f;
                            float f7 = 0.006F * f;
                            Vec3d vec3d3 = vec3d2.subtract(vec3d);
                            Vec3d vec3d4 = worldMarker2.diagonalA();
                            Vec3d vec3d5 = worldMarker2.diagonalB();
                            addMarkerArm(bufferBuilder, matrix4f, vec3d3, vec3d4, vec3d5, f5, f6, f7, i3);
                            addMarkerArm(bufferBuilder, matrix4f, vec3d3, vec3d5, vec3d4, f5, f6, f7, i3);
                            addMarkerArm(bufferBuilder, matrix4f, vec3d3, vec3d4.negate(), vec3d5, f5, f6, f7, i3);
                            addMarkerArm(bufferBuilder, matrix4f, vec3d3, vec3d5.negate(), vec3d4, f5, f6, f7, i3);
                        }
                    }

                    BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                    if (builtBuffer != null) {
                        MeshDrawUtil.drawLabeled(Pipelines.BOX_QUADS, builtBuffer, "sadnes hitmarker");
                    }
                }
            }
        }
    }

    private static void addMarkerArm(BufferBuilder bufferBuilder, Matrix4f matrix4f, Vec3d vec3d, Vec3d vec3d2, Vec3d vec3d3, float f, float f2, float f3, int i) {
        addVertex(bufferBuilder, matrix4f, vec3d, vec3d2, vec3d3, f, f3, i);
        addVertex(bufferBuilder, matrix4f, vec3d, vec3d2, vec3d3, f, -f3, i);
        addVertex(bufferBuilder, matrix4f, vec3d, vec3d2, vec3d3, f2, -f3, i);
        addVertex(bufferBuilder, matrix4f, vec3d, vec3d2, vec3d3, f2, f3, i);
    }

    private static void addVertex(BufferBuilder bufferBuilder, Matrix4f matrix4f, Vec3d vec3d, Vec3d vec3d2, Vec3d vec3d3, float f, float f2, int i) {
        bufferBuilder.vertex(
                matrix4f,
                (float)(vec3d.x + vec3d2.x * f + vec3d3.x * f2),
                (float)(vec3d.y + vec3d2.y * f + vec3d3.y * f2),
                (float)(vec3d.z + vec3d2.z * f + vec3d3.z * f2)
            )
            .color(i);
    }

    private void renderDamageTexts(DrawContext drawContext) {
        if (this.damage.isEnabled() && !this.damageTexts.isEmpty()) {
            long l = this.damageDuration.getValue().longValue();
            this.damageTexts.removeIf(damageText -> damageText.timer().getElapsed() > l);
            float f = this.damageSize.getValue() / 100.0F;
            int i = this.damageTexts.size();

            for (int i2 = 0; i2 < i; i2++) {
                HitMarker.DamageText damageText2 = this.damageTexts.get(i2);
                float f2 = (float)damageText2.timer().getElapsed() / (float)l;
                float f3 = 1.0F - (float)Easings.QUAD_IN.ease(f2);
                float f4 = (float)Easings.QUAD_OUT.ease(f2) * 0.6F;
                float f5 = (float)Easings.BACK_OUT.ease(Math.min(1.0F, f2 * 3.0F));
                Vector2f vector2f = ProjectionUtil.worldToScreen(damageText2.pos().x, damageText2.pos().y + f4, damageText2.pos().z);
                if (vector2f != null) {
                    float f6 = (6.0F + 3.0F * f5) * f;
                    new RenderUtil(drawContext, vector2f.x - 30.0F * f, vector2f.y - 8.0F * f, 60.0F * f, 16.0F * f)
                        .textCenter(Fonts.INTER_BOLD, damageText2.text(), ColorUtil.multiplyAlpha(Theme.getTextColor(), f3), f6, true);
                }
            }
        }
    }

    record DamageText(Vec3d pos, String text, TimerUtil timer) {
    }

    record TrackedHit(LivingEntity entity, Vec3d pos, float health, TimerUtil timer) {
    }

    record WorldMarker(Vec3d pos, TimerUtil timer, Vec3d diagonalA, Vec3d diagonalB) {
    }
}
