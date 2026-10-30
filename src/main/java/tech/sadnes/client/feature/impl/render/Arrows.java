package tech.sadnes.client.feature.impl.render;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.TargetComponent;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.TexturedShapeRenderer;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.player.MoveUtil;

@FeatureInfo(
    name = "Arrows",
    description = "feature.impl.render.arrows",
    displayName = "feature.impl.render.arrows",
    category = Category.VISUALS,
    subcategory = Subcategory.ENTITIES
)
public final class Arrows extends Feature {
    private final MultiSetting targets = new MultiSetting(
        "arrows.targets",
        BooleanSetting.of("Players", true),
        BooleanSetting.of("Friends", false),
        BooleanSetting.of("Naked", false),
        BooleanSetting.of("Mobs", false),
        BooleanSetting.of("Animals", false),
        BooleanSetting.of("Items", false)
    );
    private final MultiSetting options = new MultiSetting(
        "arrows.options",
        BooleanSetting.of("OnlyOffscreen", true),
        BooleanSetting.of("DynamicRadius", true),
        BooleanSetting.of("DistanceScale", true),
        BooleanSetting.of("ShowDistance", false)
    );
    private final GroupSetting colors = new GroupSetting("arrows.colors", false);
    private final ColorSetting playersColor = new ColorSetting(this.colors, "arrows.color.players", ColorUtil.WHITE)
        .setVisibleWhen(() -> this.targets.isSelected("Players"));
    private final ColorSetting friendsColor = new ColorSetting(this.colors, "arrows.color.friends", ColorUtil.rgb(60, 200, 90))
        .setVisibleWhen(() -> this.targets.isSelected("Friends"));
    private final ColorSetting nakedColor = new ColorSetting(this.colors, "arrows.color.naked", ColorUtil.rgb(245, 205, 40))
        .setVisibleWhen(() -> this.targets.isSelected("Naked"));
    private final ColorSetting mobsColor = new ColorSetting(this.colors, "arrows.color.mobs", ColorUtil.rgb(235, 64, 52))
        .setVisibleWhen(() -> this.targets.isSelected("Mobs"));
    private final ColorSetting animalsColor = new ColorSetting(this.colors, "arrows.color.animals", ColorUtil.rgb(150, 90, 230))
        .setVisibleWhen(() -> this.targets.isSelected("Animals"));
    private final ColorSetting itemsColor = new ColorSetting(this.colors, "arrows.color.items", ColorUtil.rgb(40, 195, 205))
        .setVisibleWhen(() -> this.targets.isSelected("Items"));
    private final ModeSetting colorMode = new ModeSetting("arrows.colormode", new String[]{"Client", "Rainbow", "Distance"}).setVisibleWhen(this.colors::isEnabled);
    private final NumberSetting size = new NumberSetting("arrows.size", 13.0F, 8.0F, 26.0F, 0.5F);
    private final NumberSetting radius = new NumberSetting("arrows.radius", 78.0F, 30.0F, 170.0F, 1.0F);
    private final NumberSetting range = new NumberSetting("arrows.range", 128.0F, 8.0F, 256.0F, 1.0F);
    private final GroupSetting glow = new GroupSetting("arrows.glow", true);
    private final NumberSetting glowAlpha = new NumberSetting(this.glow, "arrows.glowalpha", 0.5F, 0.1F, 1.0F, 0.05F);
    private final Animation yawAnimation = new Animation();
    private final Animation radiusAnimation = new Animation();
    private final Map<Integer, Arrows.ArrowState> arrowStates = new HashMap<>();

    public static Arrows getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(Arrows.class);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.arrowStates.clear();
        this.radiusAnimation.forceValue(this.radius.getValue().floatValue());
        if (mc.player != null && mc.world != null) {
            this.yawAnimation.forceValue(mc.gameRenderer.getCamera().getYaw());
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.arrowStates.clear();
    }

    @EventHandler
    public void onRender2D(RenderEvent renderEvent) {
        if (renderEvent.isHudPost() && mc.player != null && mc.world != null) {
            DrawContext drawContext = renderEvent.getContext();
            float f = renderEvent.getTickDelta();
            float f2 = mc.gameRenderer.getCamera().getYaw();
            this.yawAnimation.update();
            this.yawAnimation.animate(f2, 0.4, Easings.BACK_OUT, true);
            double d = this.yawAnimation.get();
            double d2 = Math.cos(Math.toRadians(d));
            double d3 = Math.sin(Math.toRadians(d));
            this.radiusAnimation.update();
            this.radiusAnimation.animate(this.getTargetRadius(), 0.5, Easings.EXPO_OUT);
            float f3 = this.options.isSelected("DynamicRadius") ? this.radiusAnimation.get() : this.radius.getValue();
            int i = GuiScale.getScaledWidthInt();
            int i2 = GuiScale.getScaledHeightInt();
            float f4 = i / 2.0F;
            float f5 = i2 / 2.0F;
            double d4 = this.range.getValue().floatValue();
            Vec3d vec3d = mc.gameRenderer.getCamera().getCameraPos();
            Iterator iterator2 = this.arrowStates.values().iterator();

            while (iterator2.hasNext()) {
                ((Arrows.ArrowState)iterator2.next()).seen = false;
            }

            for (Entity entity : TargetComponent.ENTITIES) {
                if (this.isValidTarget(entity)) {
                    double d5 = mc.player.distanceTo(entity);
                    if (!(d5 > d4)) {
                        Vec3d vec3d2 = ProjectionUtil.getInterpolatedPos(entity, f);
                        double d6 = vec3d2.x - vec3d.x;
                        double d7 = vec3d2.z - vec3d.z;
                        double d8 = -(d7 * d2 - d6 * d3);
                        double d9 = -(d6 * d2 + d7 * d3);
                        double d10 = Math.toDegrees(Math.atan2(d8, d9));
                        boolean flag;
                        if (this.options.isSelected("OnlyOffscreen")) {
                            flag = !this.isOnScreen(entity, f, i, i2);
                        } else {
                            flag = true;
                        }

                        Arrows.ArrowState arrowState = this.arrowStates.computeIfAbsent(entity.getId(), integer -> new Arrows.ArrowState(integer));
                        arrowState.seen = true;
                        arrowState.angle = d10;
                        arrowState.distance = d5;
                        arrowState.color = this.getArrowColor(entity, d10, d5, d4);
                        arrowState.fadeAnimation.update();
                        arrowState.fadeAnimation.animate(flag ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
                    }
                }
            }

            TextureSetup textureSetup = this.glow.isEnabled() ? GuiRenderUtil.createTextureSetup(Texture.ARROW) : null;
            Iterator iterator3 = this.arrowStates.entrySet().iterator();

            while (iterator3.hasNext()) {
                Arrows.ArrowState arrowState2 = (Arrows.ArrowState)((Entry)iterator3.next()).getValue();
                if (!arrowState2.seen) {
                    arrowState2.fadeAnimation.update();
                    arrowState2.fadeAnimation.animate(0.0, 0.25, Easings.QUAD_OUT);
                    if (arrowState2.fadeAnimation.get() <= 0.01F) {
                        iterator3.remove();
                        continue;
                    }
                }

                if (arrowState2.fadeAnimation.get() > 0.01F) {
                    this.drawArrow(drawContext, f4, f5, f3, arrowState2, d4, textureSetup);
                }
            }
        }
    }

    private void drawArrow(DrawContext drawContext, float f, float f2, float f3, Arrows.ArrowState arrowState, double d, TextureSetup textureSetup) {
        float f4 = arrowState.fadeAnimation.get();
        double d2 = Math.toRadians(arrowState.angle);
        float f5 = f + (float)(Math.cos(d2) * f3);
        float f6 = f2 + (float)(Math.sin(d2) * f3);
        float f7 = this.size.getValue();
        if (this.options.isSelected("DistanceScale")) {
            float f8 = 1.0F - MathHelper.clamp((float)(arrowState.distance / d), 0.0F, 1.0F);
            f7 *= 0.75F + 0.5F * f8;
        }

        int i = ColorUtil.setAlpha(arrowState.color, 255);
        float f9 = (float)arrowState.angle + 90.0F;
        if (textureSetup != null && this.glowAlpha.getValue() > 0.001F) {
            float f10 = f7 * 1.25F;
            Matrix3x2f matrix3x2f = new Matrix3x2f(drawContext.getMatrices());
            matrix3x2f.translate(f5, f6).rotate((float)Math.toRadians(f9)).translate(-f5, -f6);
            TexturedShapeRenderer.drawBloom(
                drawContext, textureSetup, matrix3x2f, f5 - f10 / 2.0F, f6 - f10 / 2.0F, f10, f10, f4 * this.glowAlpha.getValue(), i
            );
        } else {
            MatrixUtil.push(drawContext);
            MatrixUtil.rotate(drawContext, f5, f6, f9);
            GuiRenderUtil.bindTexture(Texture.ARROW);
            new RenderUtil(drawContext, f5 - f7 / 2.0F, f6 - f7 / 2.0F, f7, f7).tintedGlass(i, f4, Radius.zero());
            MatrixUtil.pop(drawContext);
        }

        if (this.options.isSelected("ShowDistance")) {
            float f11 = f3 + f7 * 0.75F;
            float f12 = f + (float)(Math.cos(d2) * f11);
            float f13 = f2 + (float)(Math.sin(d2) * f11);
            String string = (int)arrowState.distance + "m";
            int i2 = ColorUtil.multiplyAlpha(i, f4);
            new RenderUtil(drawContext, f12 - 12.0F, f13 - 4.0F, 24.0F, 8.0F).textCenter(Fonts.INTER_MEDIUM, string, i2, 6.0F, true);
        }
    }

    private float getTargetRadius() {
        float f = this.radius.getValue();
        if (!this.options.isSelected("DynamicRadius")) {
            return f;
        } else if (MoveUtil.isMoving()) {
            return mc.player.isSneaking() ? f + 6.0F : f + 16.0F;
        } else {
            return mc.player.isSneaking() ? f - 10.0F : f;
        }
    }

    private boolean isOnScreen(Entity entity, float f, int i, int i2) {
        Vec3d vec3d = ProjectionUtil.getInterpolatedPos(entity, f);
        Vector2f vector2f = ProjectionUtil.worldToScreen(vec3d.x, vec3d.y + entity.getHeight() / 2.0, vec3d.z);
        return vector2f != null && vector2f.x >= 0.0F && vector2f.x <= i && vector2f.y >= 0.0F && vector2f.y <= i2;
    }

    private int getArrowColor(Entity entity, double d, double d2, double d3) {
        if (this.colors.isEnabled()) {
            return this.getCustomColor(entity);
        }

        String string = this.colorMode.getValue();
        byte b = -1;
        switch (string.hashCode()) {
            case -1656737386:
                if (string.equals("Rainbow")) {
                    return ColorUtil.rainbow(6, (int)(d * 2.0), 0.8F, 1.0F, 1.0F);
                }
                break;
            case 353103893:
                if (string.equals("Distance")) {
                    b = 1;
                }
        }

        switch (b) {
            case 0:
                return ColorUtil.rainbow(6, (int)(d * 2.0), 0.8F, 1.0F, 1.0F);
            case 1:
                float f = 1.0F - MathHelper.clamp((float)(d2 / d3), 0.0F, 1.0F);
                return ColorUtil.interpolate(ColorUtil.rgb(60, 200, 90), ColorUtil.rgb(235, 64, 52), f);
            default:
                return Theme.getPrimary();
        }
    }

    private int getCustomColor(Entity entity) {
        if (entity instanceof PlayerEntity playerEntity) {
            if (Sadnes.getInstance().getFriendManager().isFriend(playerEntity.getGameProfile().name())) {
                return this.friendsColor.getValue();
            } else {
                return isNaked(playerEntity) ? this.nakedColor.getValue() : this.playersColor.getValue();
            }
        } else if (entity instanceof AnimalEntity) {
            return this.animalsColor.getValue();
        } else {
            return entity instanceof MobEntity ? this.mobsColor.getValue() : this.itemsColor.getValue();
        }
    }

    private boolean isValidTarget(Entity entity) {
        if (entity != mc.player && entity.isAlive()) {
            if (entity == mc.getCameraEntity()) {
                return false;
            }

            if (entity instanceof PlayerEntity playerEntity) {
                if (Sadnes.getInstance().getFriendManager().isFriend(playerEntity.getGameProfile().name())) {
                    return this.targets.isSelected("Friends");
                }

                if (isNaked(playerEntity)) {
                    return this.targets.isSelected("Naked");
                }

                return this.targets.isSelected("Players");
            }

            if (entity instanceof AnimalEntity) {
                return this.targets.isSelected("Animals");
            }

            if (entity instanceof MobEntity) {
                return this.targets.isSelected("Mobs");
            }

            if (entity instanceof ItemEntity) {
                return this.targets.isSelected("Items");
            }
        }

        return false;
    }

    private static boolean isNaked(PlayerEntity playerEntity) {
        return playerEntity.getEquippedStack(EquipmentSlot.HEAD).isEmpty()
            && playerEntity.getEquippedStack(EquipmentSlot.CHEST).isEmpty()
            && playerEntity.getEquippedStack(EquipmentSlot.LEGS).isEmpty()
            && playerEntity.getEquippedStack(EquipmentSlot.FEET).isEmpty();
    }

    static final class ArrowState {
        final Animation fadeAnimation = new Animation();
        private final int entityId;
        double angle;
        double distance;
        int color;
        boolean seen;

        ArrowState(int i) {
            this.entityId = i;
        }
    }
}
