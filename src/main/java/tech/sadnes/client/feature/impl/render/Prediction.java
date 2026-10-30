package tech.sadnes.client.feature.impl.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractWindChargeEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Vector2f;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.impl.player.SuperToss;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.render.shader.Pipelines;
import tech.sadnes.client.render.world.Render3DUtil;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

@FeatureInfo(
    name = "Prediction",
    description = "feature.impl.render.prediction",
    displayName = "feature.impl.render.prediction",
    category = Category.VISUALS,
    subcategory = Subcategory.HIGHLIGHTS
)
public final class Prediction extends Feature {
    private final MultiSetting types = new MultiSetting(
        "prediction.types",
        BooleanSetting.of("pearl", true),
        BooleanSetting.of("snowball", false),
        BooleanSetting.of("egg", false),
        BooleanSetting.of("potion", true),
        BooleanSetting.of("arrow", true),
        BooleanSetting.of("trident", true),
        BooleanSetting.of("windcharge", true)
    );
    private final ColorSetting color = new ColorSetting("prediction.color", -12417035);
    private final ColorSetting targetColor = new ColorSetting("prediction.targetcolor", -50384);
    private final GroupSetting tracers = new GroupSetting("prediction.tracers", true);
    private final ModeSetting tracersMode = new ModeSetting(this.tracers, "prediction.tracers.mode", new String[]{"line", "beam"});
    private final ColorSetting tracersColor = new ColorSetting(this.tracers, "prediction.tracers.color", -8758017);
    private final NumberSetting tracersWidth = new NumberSetting(this.tracers, "prediction.tracers.width", 2.0F, 0.5F, 6.0F, 0.1F, Unit.PIXEL);
    private final NumberSetting tracersLifetime = new NumberSetting(this.tracers, "prediction.tracers.lifetime", 2.0F, 0.5F, 10.0F, 0.5F, Unit.SECOND);
    private final NumberSetting tracersLength = new NumberSetting(this.tracers, "prediction.tracers.length", 64.0F, 8.0F, 256.0F, 4.0F, Unit.METER);
    private final MultiSetting tracersTypes = new MultiSetting(
        this.tracers,
        "prediction.tracers.types",
        BooleanSetting.of("pearl", true),
        BooleanSetting.of("snowball", true),
        BooleanSetting.of("egg", true),
        BooleanSetting.of("potion", true),
        BooleanSetting.of("arrow", true),
        BooleanSetting.of("trident", true),
        BooleanSetting.of("windcharge", true)
    );
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final float LABEL_ICON_SIZE = 8.0F;
    private static final double LANDING_BOX_SIZE = 0.1;
    private static final int MAX_STEPS = 256;
    private final List<Prediction.LandingLabel> landingLabels = new ArrayList<>();
    private final List<LivingEntity> livingEntities = new ArrayList<>();
    private final List<Vec3d> trajectory = new ArrayList<>();
    private LivingEntity hitEntity;
    private final Map<Integer, List<Prediction.TrailPoint>> trails = new HashMap<>();
    private final List<Vec3d> trailPositions = new ArrayList<>();
    private int[] trailColors = new int[0];
    private int[] trailGlowColors = new int[0];
    private float[] trailWidths = new float[0];
    private float[] trailGlowWidths = new float[0];

    @EventHandler
    public void onRender(RenderEvent renderEvent) {
        if (mc.player != null && mc.world != null) {
            if (renderEvent.isWorld()) {
                this.onRender3D(renderEvent);
            } else {
                if (renderEvent.isHudPre()) {
                    this.onRender2D(renderEvent);
                }
            }
        }
    }

    private void onRender3D(RenderEvent renderEvent) {
        this.landingLabels.clear();
        this.collectLivingEntities();
        MatrixStack matrixStack = renderEvent.getMatrices();
        float f = renderEvent.getTickDelta();
        int i = this.color.getValue();
        this.renderOwnTrajectory(matrixStack, f, i);
        if (this.types.hasAnySelected()) {
            this.renderProjectileTrajectories(matrixStack, f, i);
        }

        if (this.tracers.isEnabled()) {
            this.renderTracers(matrixStack, f);
        } else {
            if (!this.trails.isEmpty()) {
                this.trails.clear();
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.trails.clear();
    }

    private void renderOwnTrajectory(MatrixStack matrixStack, float f, int i) {
        ClientPlayerEntity clientPlayerEntity = mc.player;
        List<Prediction.ThrowProfile> throwProfiles = this.getThrowProfiles(clientPlayerEntity);
        if (!throwProfiles.isEmpty()) {
            double d = clientPlayerEntity.getEyePos().y - clientPlayerEntity.getY();
            Vec3d vec3d = ProjectionUtil.getInterpolatedPos(clientPlayerEntity, f).add(0.0, d, 0.0);
            Vec3d vec3d2 = vec3d.add(clientPlayerEntity.getRotationVec(f).multiply(0.2));

            for (Prediction.ThrowProfile throwProfile : throwProfiles) {
                Vec3d vec3d3 = this.getLaunchVelocity(clientPlayerEntity, throwProfile);
                Vec3d vec3d4 = this.simulate(vec3d2, vec3d3, throwProfile.gravity(), throwProfile.motion(), clientPlayerEntity);
                if (this.trajectory.size() >= 2) {
                    int i2 = this.getTrajectoryColor(i);
                    this.renderTrajectoryLine(matrixStack, i2);
                    this.renderLandingBox(matrixStack, vec3d4, i2, 0.1);
                    this.renderHitEntityBox(matrixStack, f);
                }
            }
        }
    }

    private List<Prediction.ThrowProfile> getThrowProfiles(PlayerEntity playerEntity) {
        Prediction.ThrowProfile throwProfile = getThrowProfile(playerEntity.getMainHandStack().getItem());
        if (throwProfile == null) {
            throwProfile = getThrowProfile(playerEntity.getOffHandStack().getItem());
        }

        if (throwProfile != null) {
            return List.of(throwProfile);
        }

        if (playerEntity.isUsingItem()) {
            ItemStack itemStack = playerEntity.getActiveItem();
            if (itemStack.isOf(Items.BOW)) {
                float f = BowItem.getPullProgress(playerEntity.getItemUseTime());
                if (f > 0.1F) {
                    return List.of(new Prediction.ThrowProfile(f * 3.0F, 0.0F, 0.05, Prediction.MotionType.ARROW, 0.0F, true));
                }
            } else if (itemStack.isOf(Items.TRIDENT)) {
                return List.of(new Prediction.ThrowProfile(2.5F, 0.0F, 0.05, Prediction.MotionType.ARROW, 0.0F, true));
            }
        }

        ItemStack itemStack2 = this.getLoadedCrossbow(playerEntity);
        if (itemStack2 != null) {
            return this.hasMultishot(itemStack2)
                ? List.of(
                    new Prediction.ThrowProfile(3.15F, 0.0F, 0.05, Prediction.MotionType.ARROW, -10.0F, false),
                    new Prediction.ThrowProfile(3.15F, 0.0F, 0.05, Prediction.MotionType.ARROW, 0.0F, false),
                    new Prediction.ThrowProfile(3.15F, 0.0F, 0.05, Prediction.MotionType.ARROW, 10.0F, false)
                )
                : List.of(new Prediction.ThrowProfile(3.15F, 0.0F, 0.05, Prediction.MotionType.ARROW, 0.0F, false));
        } else {
            return List.of();
        }
    }

    private static Prediction.ThrowProfile getThrowProfile(Item item) {
        if (item == Items.ENDER_PEARL || item == Items.SNOWBALL || item == Items.EGG || item == Items.BLUE_EGG || item == Items.BROWN_EGG) {
            return new Prediction.ThrowProfile(1.5F, 0.0F, 0.03, Prediction.MotionType.THROWN, 0.0F, true);
        } else if (item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) {
            return new Prediction.ThrowProfile(0.5F, -20.0F, 0.05, Prediction.MotionType.THROWN, 0.0F, true);
        } else {
            return item == Items.WIND_CHARGE ? new Prediction.ThrowProfile(1.5F, 0.0F, 0.0, Prediction.MotionType.WIND, 0.0F, true) : null;
        }
    }

    private ItemStack getLoadedCrossbow(PlayerEntity playerEntity) {
        ItemStack itemStack = playerEntity.getMainHandStack();
        if (itemStack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(itemStack)) {
            return itemStack;
        }

        ItemStack itemStack2 = playerEntity.getOffHandStack();
        return itemStack2.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(itemStack2) ? itemStack2 : null;
    }

    private boolean hasMultishot(ItemStack itemStack) {
        if (mc.world != null && itemStack.hasEnchantments()) {
            Reference reference = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.MULTISHOT);
            return EnchantmentHelper.getLevel(reference, itemStack) > 0;
        } else {
            return false;
        }
    }

    private Vec3d getLaunchVelocity(PlayerEntity playerEntity, Prediction.ThrowProfile throwProfile) {
        float f = playerEntity.getYaw() + throwProfile.yawOffset() + this.getRotationYawOffset(playerEntity, throwProfile);
        float f2 = playerEntity.getPitch();
        float f3 = -MathHelper.sin(f * (float) (Math.PI / 180.0)) * MathHelper.cos(f2 * (float) (Math.PI / 180.0));
        float f4 = -MathHelper.sin((f2 + throwProfile.roll()) * (float) (Math.PI / 180.0));
        float f5 = MathHelper.cos(f * (float) (Math.PI / 180.0)) * MathHelper.cos(f2 * (float) (Math.PI / 180.0));
        Vec3d vec3d = new Vec3d(f3, f4, f5).normalize().multiply(throwProfile.power());
        if (!throwProfile.inheritVelocity()) {
            return vec3d;
        }

        Vec3d vec3d2 = playerEntity.getMovement();
        double d = vec3d2.x;
        return playerEntity.isOnGround() ? vec3d.add(d, 0.0, vec3d2.z) : vec3d.add(d, vec3d2.y, vec3d2.z);
    }

    private float getRotationYawOffset(PlayerEntity playerEntity, Prediction.ThrowProfile throwProfile) {
        if (!throwProfile.inheritVelocity()) {
            return 0.0F;
        }

        SuperToss superToss = SuperToss.getInstance();
        if (superToss == null) {
            return 0.0F;
        }

        float f = superToss.getCorrectedYaw();
        return Float.isNaN(f) ? 0.0F : MathHelper.wrapDegrees(f - playerEntity.getYaw());
    }

    private void renderProjectileTrajectories(MatrixStack matrixStack, float f, int i) {
        for (Entity entity : mc.world.getEntities()) {
            Prediction.ProjectileType projectileType = Prediction.ProjectileType.fromEntity(entity);
            if (projectileType != null
                && this.types.has(projectileType.key)
                && entity.age > 2
                && !(entity.getVelocity().lengthSquared() < 1.0E-6)
                && !(entity.squaredDistanceTo(entity.lastX, entity.lastY, entity.lastZ) < 1.0E-6)
                && (!(entity instanceof TridentEntity) || !((TridentEntity)entity).noClip)) {
                Vec3d vec3d = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
                Vec3d vec3d2 = this.simulate(vec3d, entity.getVelocity(), projectileType.gravity, projectileType.motion, entity);
                int i2 = this.trajectory.size() - 1;
                if (i2 >= 1) {
                    this.trajectory.set(0, ProjectionUtil.getInterpolatedPos(entity, f));
                    int i3 = this.getTrajectoryColor(i);
                    this.renderTrajectoryLine(matrixStack, i3);
                    this.renderHitEntityBox(matrixStack, f);
                    String string = String.format(Locale.US, "%.1f", i2 / 20.0F) + Language.translate("prediction.seconds");
                    this.landingLabels.add(new Prediction.LandingLabel(vec3d2, getProjectileStack(entity, projectileType), string));
                }
            }
        }
    }

    private void renderTracers(MatrixStack matrixStack, float f) {
        long l = System.currentTimeMillis();
        long l2 = (long)(this.tracersLifetime.getValue() * 1000.0F);

        for (Entity entity : mc.world.getEntities()) {
            Prediction.ProjectileType projectileType = Prediction.ProjectileType.fromEntity(entity);
            if (projectileType != null && this.tracersTypes.has(projectileType.key) && (!(entity instanceof TridentEntity) || !((TridentEntity)entity).noClip)) {
                Vec3d vec3d = ProjectionUtil.getInterpolatedPos(entity, f);
                List list = this.trails.computeIfAbsent(entity.getId(), integer -> new ArrayList<>());
                int i = list.size() - 1;
                if (i < 0 || ((Prediction.TrailPoint)list.get(i)).pos().squaredDistanceTo(vec3d) > 0.0064) {
                    list.add(new Prediction.TrailPoint(vec3d, l));
                }

                if (list.size() > 256) {
                    list.remove(0);
                }
            }
        }

        int i2 = this.tracersColor.getValue();
        float f2 = this.tracersWidth.getValue();
        boolean flag = this.tracersMode.is("beam");
        float f3 = this.tracersLength.getValue();
        boolean flag2 = f3 < this.tracersLength.getMax();
        Iterator iterator2 = this.trails.values().iterator();

        while (iterator2.hasNext()) {
            List list2 = (List)iterator2.next();

            while (!list2.isEmpty() && l - ((Prediction.TrailPoint)list2.get(0)).birth() >= l2) {
                list2.remove(0);
            }

            if (list2.isEmpty()) {
                iterator2.remove();
            } else {
                if (flag2) {
                    this.trimTrailLength(list2, f3);
                }

                if (list2.size() > 1) {
                    this.renderTrail(matrixStack, list2, l, l2, i2, f2, flag);
                }
            }
        }
    }

    private void trimTrailLength(List<Prediction.TrailPoint> trailPoints, float f) {
        int i = trailPoints.size();
        double d = 0.0;
        int i2 = 0;

        for (int i3 = i - 1; i3 > 0; i3--) {
            d += ((Prediction.TrailPoint)trailPoints.get(i3)).pos().distanceTo(((Prediction.TrailPoint)trailPoints.get(i3 - 1)).pos());
            if (d > f) {
                i2 = i3;
                break;
            }
        }

        if (i2 > 0) {
            trailPoints.subList(0, i2).clear();
        }
    }

    private void renderTrail(MatrixStack matrixStack, List<Prediction.TrailPoint> trailPoints, long l, long l2, int i, float f, boolean flag) {
        int i2 = trailPoints.size();
        this.trailPositions.clear();
        if (this.trailColors.length < i2) {
            this.trailColors = new int[i2];
            this.trailGlowColors = new int[i2];
            this.trailWidths = new float[i2];
            this.trailGlowWidths = new float[i2];
        }

        int i3 = i2 - 1;

        for (int i4 = 0; i4 < i2; i4++) {
            Prediction.TrailPoint trailPoint = (Prediction.TrailPoint)trailPoints.get(i4);
            float f2 = 1.0F - (float)(l - trailPoint.birth()) / (float)l2;
            float f3 = MathHelper.clamp(f2, 0.0F, 1.0F);
            float f4 = f3 * f3 * (3.0F - 2.0F * f3);
            float f5 = i3 > 0 ? (float)i4 / i3 : 1.0F;
            float f6 = MathHelper.clamp(f5 / 0.33F, 0.0F, 1.0F);
            float f7 = f6 * f6 * (3.0F - 2.0F * f6);
            float f8 = f * (0.15F + 0.85F * f7);
            this.trailPositions.add(trailPoint.pos());
            this.trailColors[i4] = ColorUtil.multiplyAlpha(i, f4);
            this.trailGlowColors[i4] = ColorUtil.multiplyAlpha(i, f4 * 0.45F);
            this.trailWidths[i4] = f8;
            this.trailGlowWidths[i4] = f8 * 2.6F;
        }

        if (flag) {
            Render3DUtil.drawGradientLines(matrixStack, this.trailPositions, this.trailGlowColors, this.trailGlowWidths, Pipelines.AA_LINE_GLOW);
        }

        Render3DUtil.drawGradientLines(matrixStack, this.trailPositions, this.trailColors, this.trailWidths, Pipelines.AA_LINE);
    }

    private Vec3d simulate(Vec3d vec3d, Vec3d vec3d2, double d, Prediction.MotionType motionType, Entity entity) {
        this.trajectory.clear();
        this.hitEntity = null;
        Vec3d vec3d3 = vec3d;
        Vec3d vec3d4 = vec3d2;
        this.trajectory.add(vec3d);

        for (int i = 0; i < 256; i++) {
            if (motionType == Prediction.MotionType.THROWN) {
                vec3d4 = vec3d4.subtract(0.0, d, 0.0).multiply(0.99);
            }

            Vec3d vec3d5 = vec3d3.add(vec3d4);
            Vec3d vec3d6 = this.raycastStep(vec3d3, vec3d5, entity);
            if (vec3d6 != null) {
                this.trajectory.add(vec3d6);
                return vec3d6;
            }

            vec3d3 = vec3d5;
            this.trajectory.add(vec3d5);
            if (motionType == Prediction.MotionType.ARROW) {
                vec3d4 = vec3d4.multiply(0.99).subtract(0.0, d, 0.0);
            }

            if (vec3d5.y < mc.world.getBottomY() - 16) {
                return vec3d5;
            }
        }

        return vec3d3;
    }

    private Vec3d raycastStep(Vec3d vec3d, Vec3d vec3d2, Entity entity) {
        BlockHitResult blockHitResult = mc.world.raycast(new RaycastContext(vec3d, vec3d2, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
        Vec3d vec3d3 = blockHitResult.getType() != Type.MISS ? blockHitResult.getPos() : vec3d2;
        Vec3d vec3d4 = this.raycastEntities(vec3d, vec3d3, entity);
        if (vec3d4 != null) {
            return vec3d4;
        } else {
            return blockHitResult.getType() != Type.MISS ? blockHitResult.getPos() : null;
        }
    }

    private Vec3d raycastEntities(Vec3d vec3d, Vec3d vec3d2, Entity entity) {
        double d = Double.MAX_VALUE;
        Vec3d vec3d3 = null;
        LivingEntity livingEntity = null;

        for (LivingEntity livingEntity2 : this.livingEntities) {
            if (livingEntity2 != entity) {
                Box box = livingEntity2.getBoundingBox().expand(0.1);
                if (!box.contains(vec3d)) {
                    Optional optional = box.raycast(vec3d, vec3d2);
                    if (!optional.isEmpty()) {
                        double d2 = vec3d.squaredDistanceTo((Vec3d)optional.get());
                        if (d2 < d) {
                            d = d2;
                            vec3d3 = (Vec3d)optional.get();
                            livingEntity = livingEntity2;
                        }
                    }
                }
            }
        }

        if (vec3d3 != null) {
            this.hitEntity = livingEntity;
        }

        return vec3d3;
    }

    private void renderTrajectoryLine(MatrixStack matrixStack, int i) {
        Render3DUtil.drawGlowLines(matrixStack, this.trajectory, ColorUtil.setAlphaf(i, 1.0F), 2.5F);
    }

    private void renderLandingBox(MatrixStack matrixStack, Vec3d vec3d, int i, double d) {
        Box box = new Box(vec3d.x - d, vec3d.y - d, vec3d.z - d, vec3d.x + d, vec3d.y + d, vec3d.z + d);
        Render3DUtil.drawGlowBox(matrixStack, box, i, ColorUtil.setAlphaf(i, 0.18F));
    }

    private int getTrajectoryColor(int i) {
        return this.hitEntity != null ? this.targetColor.getValue() : i;
    }

    private void renderHitEntityBox(MatrixStack matrixStack, float f) {
        if (this.hitEntity != null) {
            Vec3d vec3d = ProjectionUtil.getInterpolatedPos(this.hitEntity, f);
            Box box = this.hitEntity.getBoundingBox().offset(vec3d.x - this.hitEntity.getX(), vec3d.y - this.hitEntity.getY(), vec3d.z - this.hitEntity.getZ());
            int i = this.targetColor.getValue();
            Render3DUtil.drawGlowBox(matrixStack, box, i, ColorUtil.setAlphaf(i, 0.18F));
        }
    }

    private void collectLivingEntities() {
        this.livingEntities.clear();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity livingEntity && livingEntity.isAlive()) {
                this.livingEntities.add(livingEntity);
            }
        }
    }

    private void onRender2D(RenderEvent renderEvent) {
        if (!this.landingLabels.isEmpty()) {
            MsdfFont msdfFont = Fonts.INTER_MEDIUM;
            if (msdfFont != null) {
                DrawContext drawContext = renderEvent.getContext();

                for (Prediction.LandingLabel landingLabel : this.landingLabels) {
                    this.renderLandingLabel(drawContext, msdfFont, landingLabel);
                }
            }
        }
    }

    private void renderLandingLabel(DrawContext drawContext, MsdfFont msdfFont, Prediction.LandingLabel landingLabel) {
        Vector2f vector2f = ProjectionUtil.worldToScreen(landingLabel.landing.x, landingLabel.landing.y, landingLabel.landing.z);
        if (vector2f != null) {
            boolean flag;
            float f;
            if (landingLabel.icon != null && !landingLabel.icon.isEmpty()) {
                flag = true;
                f = 11.0F;
            } else {
                flag = false;
                f = 0.0F;
            }

            float f2 = f;
            float f3 = landingLabel.text != null ? msdfFont.getWidth(landingLabel.text, 6.0F) : 0.0F;
            float f4 = Math.max(6.0F, flag ? 8.0F : 0.0F);
            float f5 = 3.0F * 2.0F + f2 + f3;
            float f6 = 3.0F * 2.0F + f4;
            float f7 = GuiRenderUtil.clampToScreenX(vector2f.x - f5 / 2.0F, f5);
            float f8 = GuiRenderUtil.clampToScreenY(vector2f.y - f6 / 2.0F, f6);
            GuiRenderUtil.window(drawContext, f7, f8, f5, f6, 1.0F, 4.0F);
            if (flag) {
                this.renderItemIcon(drawContext, landingLabel.icon, f7 + 3.0F, f8 + (f6 - 8.0F) / 2.0F);
            }

            if (landingLabel.text != null) {
                new RenderUtil(drawContext, f7, f8, f5, f6).textMiddleLeft(msdfFont, landingLabel.text, 3.0F + f2, 0.0F, Theme.getTextColor(), 6.0F);
            }
        }
    }

    private void renderItemIcon(DrawContext drawContext, ItemStack itemStack, float f, float f2) {
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(drawContext, f, f2, 0.5F);
        GuiRenderUtil.drawItem(drawContext, itemStack, f, f2, 1.0F);
        MatrixUtil.pop(drawContext);
    }

    private static ItemStack getProjectileStack(Entity entity, Prediction.ProjectileType projectileType) {
        if (entity instanceof FlyingItemEntity flyingItemEntity) {
            ItemStack itemStack = flyingItemEntity.getStack();
            if (!itemStack.isEmpty()) {
                return itemStack;
            }
        }

        return projectileType.item.getDefaultStack();
    }

    record LandingLabel(Vec3d landing, ItemStack icon, String text) {
    }

    enum MotionType {
        THROWN,
        ARROW,
        WIND;
    }

    enum ProjectileType {
        PEARL("pearl", 0.03, Prediction.MotionType.THROWN, Items.ENDER_PEARL),
        SNOWBALL("snowball", 0.03, Prediction.MotionType.THROWN, Items.SNOWBALL),
        EGG("egg", 0.03, Prediction.MotionType.THROWN, Items.EGG),
        POTION("potion", 0.05, Prediction.MotionType.THROWN, Items.SPLASH_POTION),
        ARROW("arrow", 0.05, Prediction.MotionType.ARROW, Items.ARROW),
        TRIDENT("trident", 0.05, Prediction.MotionType.ARROW, Items.TRIDENT),
        WINDCHARGE("windcharge", 0.0, Prediction.MotionType.WIND, Items.WIND_CHARGE);

        final String key;
        final double gravity;
        final Prediction.MotionType motion;
        final Item item;

        ProjectileType(String string, double d, Prediction.MotionType motionType, Item item2) {
            this.key = string;
            this.gravity = d;
            this.motion = motionType;
            this.item = item2;
        }

        static Prediction.ProjectileType fromEntity(Entity entity) {
            if (entity instanceof EnderPearlEntity) {
                return PEARL;
            } else if (entity instanceof SnowballEntity) {
                return SNOWBALL;
            } else if (entity instanceof EggEntity) {
                return EGG;
            } else if (entity instanceof PotionEntity) {
                return POTION;
            } else if (entity instanceof TridentEntity) {
                return TRIDENT;
            } else if (entity instanceof ArrowEntity) {
                return ARROW;
            } else {
                return entity instanceof AbstractWindChargeEntity ? WINDCHARGE : null;
            }
        }
    }

    record ThrowProfile(float power, float roll, double gravity, Prediction.MotionType motion, float yawOffset, boolean inheritVelocity) {
    }

    record TrailPoint(Vec3d pos, long birth) {
    }
}
