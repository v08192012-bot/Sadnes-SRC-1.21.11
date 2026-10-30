package tech.sadnes.client.feature.impl.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPart.Cuboid;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractWindChargeEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.LlamaSpitEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.AttackEntityEvent;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.render.ParticleBlurRenderer;
import tech.sadnes.client.render.particle.ParticlePhysicsSettings;
import tech.sadnes.client.render.particle.ParticleRenderer;
import tech.sadnes.client.render.particle.WorldParticle;
import tech.sadnes.client.render.shader.EntityWarpRenderer;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

@FeatureInfo(
    name = "Particles",
    description = "feature.impl.render.particles",
    displayName = "feature.impl.render.particles",
    category = Category.VISUALS,
    subcategory = Subcategory.HIGHLIGHTS
)
public class Particles extends Feature {
    private static final String[] MOTION_RULES = new String[]{"none", "swirl_left", "swirl_right", "swirl_random", "to_source", "from_source", "alternate"};
    static final String[] RENDER_MODES = new String[]{"textures", "fluff", "random"};
    private static final Texture[] FLUFF_TEXTURES = new Texture[]{
        Texture.FLUFF_0, Texture.FLUFF_1, Texture.FLUFF_2, Texture.FLUFF_3, Texture.FLUFF_4, Texture.FLUFF_5, Texture.FLUFF_6, Texture.FLUFF_7
    };
    static final String[] COLOR_MODES = new String[]{"client", "custom", "rainbow"};
    private static final String[] TOTEM_COLOR_MODES = new String[]{"totem", "client", "custom", "rainbow"};
    private static final int TOTEM_STATUS = 35;
    private static final int DEATH_STATUS = 3;
    private static final float DEATH_PARTICLE_DENSITY = 850.0F;
    private static final int MIN_DEATH_PARTICLES = 60;
    private static final int MAX_DEATH_PARTICLES = 700;
    private final GroupSetting worldGroup = new GroupSetting("particles.worldGroup", true);
    private final ModeSetting worldPreset = new ModeSetting(this.worldGroup, "particles.worldPreset", new String[]{"ambient", "rain"});
    private final ModeSetting worldMotionRule = withConfigKey(
        new ModeSetting(this.worldGroup, "particles.motionRule", MOTION_RULES), "particles.world.motionRule"
    );
    private final NumberSetting worldSpawnCount = withConfigKey(
        new NumberSetting(this.worldGroup, "particles.spawnCount", 4.0F, 1.0F, 15.0F, 1.0F, Unit.COUNT), "particles.world.spawnCount"
    );
    private final Particles.ParticleStyle worldStyle = new Particles.ParticleStyle(this.worldGroup, "world");
    private final GroupSetting attackGroup = new GroupSetting("particles.attackGroup", true);
    private final ModeSetting attackMotionRule = withConfigKey(
        new ModeSetting(this.attackGroup, "particles.motionRule", MOTION_RULES), "particles.attack.motionRule"
    );
    private final NumberSetting attackSpawnCount = withConfigKey(
        new NumberSetting(this.attackGroup, "particles.spawnCount", 40.0F, 5.0F, 150.0F, 5.0F, Unit.COUNT), "particles.attack.spawnCount"
    );
    private final Particles.ParticleStyle attackStyle = new Particles.ParticleStyle(this.attackGroup, "attack");
    private final GroupSetting totemGroup = new GroupSetting("particles.totemGroup", true);
    private final NumberSetting totemSpawnCount = withConfigKey(
        new NumberSetting(this.totemGroup, "particles.spawnCount", 60.0F, 5.0F, 150.0F, 5.0F, Unit.COUNT), "particles.totem.spawnCount"
    );
    private final Particles.ParticleStyle totemStyle = new Particles.ParticleStyle(this.totemGroup, "totem", TOTEM_COLOR_MODES);
    private final GroupSetting movementGroup = new GroupSetting("particles.movementGroup", false);
    private final ModeSetting movementMotionRule = withConfigKey(
        new ModeSetting(this.movementGroup, "particles.motionRule", MOTION_RULES), "particles.movement.motionRule"
    );
    private final NumberSetting movementSpawnCount = withConfigKey(
        new NumberSetting(this.movementGroup, "particles.spawnCount", 2.0F, 1.0F, 10.0F, 1.0F, Unit.COUNT), "particles.movement.spawnCount"
    );
    private final Particles.ParticleStyle movementStyle = new Particles.ParticleStyle(this.movementGroup, "movement");
    private final GroupSetting projectileGroup = new GroupSetting("particles.projectileGroup", true);
    private final MultiSetting projectileTypes = new MultiSetting(
        this.projectileGroup,
        "particles.projectileTypes",
        BooleanSetting.of("arrow", true),
        BooleanSetting.of("spectral_arrow", true),
        BooleanSetting.of("trident", true),
        BooleanSetting.of("pearl", true),
        BooleanSetting.of("snowball", true),
        BooleanSetting.of("egg", true),
        BooleanSetting.of("exp_bottle", true),
        BooleanSetting.of("potion", true),
        BooleanSetting.of("wind_charge", true),
        BooleanSetting.of("firework", false),
        BooleanSetting.of("bobber", false),
        BooleanSetting.of("fireball", false),
        BooleanSetting.of("small_fireball", false),
        BooleanSetting.of("dragon_fireball", false),
        BooleanSetting.of("wither_skull", false),
        BooleanSetting.of("shulker_bullet", false),
        BooleanSetting.of("llama_spit", false)
    );
    private final ModeSetting projectileMotionRule = withConfigKey(
        new ModeSetting(this.projectileGroup, "particles.motionRule", MOTION_RULES), "particles.projectile.motionRule"
    );
    private final NumberSetting projectileSpawnCount = withConfigKey(
        new NumberSetting(this.projectileGroup, "particles.spawnCount", 2.0F, 1.0F, 10.0F, 1.0F, Unit.COUNT), "particles.projectile.spawnCount"
    );
    private final Particles.ParticleStyle projectileStyle = new Particles.ParticleStyle(this.projectileGroup, "projectile");
    private final GroupSetting deathGroup = new GroupSetting("particles.deathGroup", true);
    private final Particles.ParticleStyle deathStyle = new Particles.ParticleStyle(this.deathGroup, "death");
    private final BooleanSetting brightMode = new BooleanSetting("particles.brightMode", true).setVisibleWhen(this::isAnyGroupEnabled);
    private final GroupSetting effectsGroup = new GroupSetting("particles.effectsGroup", true);
    private final MultiSetting effects = new MultiSetting(
        this.effectsGroup, "particles.effects", BooleanSetting.of("warp", false), BooleanSetting.of("blur", false), BooleanSetting.of("trail", false)
    );
    private final NumberSetting warpPower = new NumberSetting(this.effectsGroup, "particles.warpPower", 30.0F, 5.0F, 100.0F, 5.0F, Unit.PERCENT)
        .setVisibleWhen(() -> this.effects.isSelected("warp"));
    private final NumberSetting blurPower = new NumberSetting(this.effectsGroup, "particles.blurPower", 30.0F, 5.0F, 100.0F, 5.0F, Unit.PERCENT)
        .setVisibleWhen(() -> this.effects.isSelected("blur"));
    private final NumberSetting trailPower = new NumberSetting(this.effectsGroup, "particles.trailPower", 30.0F, 5.0F, 100.0F, 5.0F, Unit.PERCENT)
        .setVisibleWhen(() -> this.effects.isSelected("trail"));
    private final ParticleRenderer particleRenderer = new ParticleRenderer();
    private final ParticlePhysicsSettings worldPhysics = new ParticlePhysicsSettings();
    private final ParticlePhysicsSettings attackPhysics = new ParticlePhysicsSettings();
    private final ParticlePhysicsSettings totemPhysics = new ParticlePhysicsSettings();
    private final ParticlePhysicsSettings movementPhysics = new ParticlePhysicsSettings();
    private final ParticlePhysicsSettings projectilePhysics = new ParticlePhysicsSettings();
    private final ParticlePhysicsSettings deathPhysics = new ParticlePhysicsSettings();
    private final Queue<Particles.PendingParticleEvent> pendingEvents = new ConcurrentLinkedQueue<>();
    private final Map<Integer, float[]> projectileSpin = new HashMap<>();
    private final Set<Integer> activeProjectileIds = new HashSet<>();
    private final List<Particles.TotemEmitter> totemEmitters = new ArrayList<>();
    private double lastX;
    private double lastY;
    private double lastZ;
    private boolean hasLastPos;

    public Particles() {
        this.attackPhysics.setGravity(0.2F);
        this.attackPhysics.setShrinkWithFade(true);
        this.totemPhysics.setSpeed(0.6F);
        this.totemPhysics.setGravity(0.525F);
        this.totemPhysics.setCollisionMode(ParticlePhysicsSettings.CollisionMode.SLIDE_SLOW);
        this.totemPhysics.setMinSize(0.15F);
        this.totemPhysics.setMaxSize(0.3F);

        for (ParticlePhysicsSettings particlePhysicsSettings : new ParticlePhysicsSettings[]{this.movementPhysics, this.projectilePhysics}) {
            particlePhysicsSettings.setGravity(0.05F);
            particlePhysicsSettings.setShrinkWithFade(true);
        }

        this.worldPhysics.setShrinkWithFade(true);
        this.deathPhysics.setShrinkWithFade(true);
        this.deathPhysics.setAttractDelay(0.4F);
        this.deathPhysics.setAttractSpeed(0.04F);
    }

    public static Particles getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(Particles.class);
    }

    public boolean isTotemEnabled() {
        return this.isEnabled() && this.totemGroup.isEnabled();
    }

    @Override
    public void onEnable() {
        this.particleRenderer.clear();
        this.pendingEvents.clear();
        this.projectileSpin.clear();
        this.totemEmitters.clear();
        super.onEnable();
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.particleRenderer.clear();
        this.pendingEvents.clear();
        this.projectileSpin.clear();
        this.totemEmitters.clear();
        this.hasLastPos = false;
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        this.updatePhysicsModes();
        this.particleRenderer.tick();
        if (mc.player != null && mc.world != null) {
            this.processPendingEvents();
            this.tickTotemEmitters();
            this.spawnWorldParticles();
            this.spawnMovementParticles();
            this.spawnProjectileParticles();
        }
    }

    @EventHandler
    public void onAttack(AttackEntityEvent attackEntityEvent) {
        if (attackEntityEvent.getStage() == AttackEntityEvent.Stage.POST && this.attackGroup.isEnabled()) {
            Entity entity = attackEntityEvent.getTarget();
            if (entity != null) {
                this.spawnParticles(
                    entity,
                    this.attackPhysics,
                    this.attackStyle,
                    (int)this.attackSpawnCount.getValue().floatValue(),
                    0.5F,
                    2.5F,
                    "from_source",
                    0.3F,
                    1250L,
                    -90.0F,
                    90.0F
                );
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive() && mc.world != null && packetEvent.getPacket() instanceof EntityStatusS2CPacket entityStatusS2CPacket) {
            byte b = entityStatusS2CPacket.getStatus();
            boolean flag;
            if (b == 35) {
                flag = this.totemGroup.isEnabled();
            } else {
                flag = false;
            }

            boolean flag2;
            if (b == 3 && this.deathGroup.isEnabled()) {
                flag2 = true;
            } else {
                flag2 = false;
                if (!flag) {
                    return;
                }
            }

            if (this.pendingEvents.size() > 64) {
                return;
            }

            Entity entity = entityStatusS2CPacket.getEntity(mc.world);
            if (entity != null) {
                if (flag2 && !(entity instanceof LivingEntity)) {
                    return;
                }

                this.pendingEvents.add(new Particles.PendingParticleEvent(entity, flag));
                return;
            }
        }
    }

    @EventHandler
    public void onRender(RenderEvent renderEvent) {
        if (renderEvent.isWorld() && mc.player != null) {
            boolean flag = !this.particleRenderer.isEmpty();
            float f = mc.getWindow().getFramebufferHeight();
            if (flag && this.isEffectEnabled("warp")) {
                EntityWarpRenderer.INSTANCE.render(this.warpPower.getValue() * 0.001F, f * 0.012F, Theme.getPrimary(), () -> this.renderAll(renderEvent));
            }

            if (flag && this.isEffectEnabled("blur")) {
                ParticleBlurRenderer.INSTANCE.render(f * this.blurPower.getValue() * 2.0E-4F, 0.0F, () -> this.renderAll(renderEvent));
            } else {
                this.renderDefault(renderEvent);
            }
        }
    }

    private boolean isEffectEnabled(String string) {
        return this.effectsGroup.isEnabled() && this.effects.isSelected(string);
    }

    private float getTrailStrength() {
        return this.isEffectEnabled("trail") ? this.trailPower.getValue() * 0.02F : 0.0F;
    }

    private void renderDefault(RenderEvent renderEvent) {
        float f = this.getTrailStrength();
        if (f > 0.0F && !this.particleRenderer.isEmpty()) {
            float f2 = mc.getWindow().getFramebufferHeight() * this.trailPower.getValue() * 1.0E-4F;
            ParticleBlurRenderer.INSTANCE.render(f2, 0.0F, () -> {
                this.particleRenderer.renderTrails(renderEvent.getMatrices(), renderEvent.getCamera(), renderEvent.getTickDelta(), this.brightMode.getValue(), f);
                this.renderProjectileTrails(renderEvent, f);
            });
        }

        this.particleRenderer.render(renderEvent.getMatrices(), renderEvent.getCamera(), renderEvent.getTickDelta(), this.brightMode.getValue());
        this.renderProjectileSprites(renderEvent);
    }

    private void renderAll(RenderEvent renderEvent) {
        float f = this.getTrailStrength();
        if (f > 0.0F) {
            this.particleRenderer.renderTrails(renderEvent.getMatrices(), renderEvent.getCamera(), renderEvent.getTickDelta(), this.brightMode.getValue(), f);
            this.renderProjectileTrails(renderEvent, f);
        }

        this.particleRenderer.render(renderEvent.getMatrices(), renderEvent.getCamera(), renderEvent.getTickDelta(), this.brightMode.getValue());
        this.renderProjectileSprites(renderEvent);
    }

    private void renderProjectileSprites(RenderEvent renderEvent) {
        this.renderProjectiles(renderEvent, 0.0F, false);
    }

    private void renderProjectileTrails(RenderEvent renderEvent, float f) {
        this.renderProjectiles(renderEvent, f, true);
    }

    private void renderProjectiles(RenderEvent renderEvent, float f, boolean flag) {
        if (this.projectileGroup.isEnabled() && mc.world != null) {
            List list = this.getSelectedTextures(this.projectileStyle);
            if (list.isEmpty() && this.projectileStyle.renderMode.is("textures")) {
                return;
            }

            boolean flag2;
            if (!this.projectileStyle.renderMode.is("fluff")) {
                flag2 = list.isEmpty();
            } else {
                flag2 = true;
            }

            int i = list.size();
            int i2 = f > 0.0F ? 8 : 0;
            int i3 = flag ? 1 : 0;
            float f2 = renderEvent.getTickDelta();

            for (Entity entity : mc.world.getEntities()) {
                if (this.isTrackedProjectile(entity) && !(entity.getVelocity().lengthSquared() < 0.01)) {
                    float[] floats = this.projectileSpin.get(entity.getId());
                    if (floats != null) {
                        Texture texture = flag2
                            ? FLUFF_TEXTURES[Math.floorMod(entity.getId(), FLUFF_TEXTURES.length)]
                            : (Texture)list.get(Math.floorMod(entity.getId(), i));
                        int i4 = this.getProjectileColor(entity);
                        float f3 = entity.getHeight() / 2.0F;

                        for (int i5 = i2; i5 >= i3; i5--) {
                            float f4 = i5 == 0 ? 1.0F : ParticleRenderer.getTrailFade(i5);
                            float f5 = f2 - f * i5 / 8.0F;
                            Vec3d vec3d = entity.getLerpedPos(f5).add(0.0, f3, 0.0);
                            float f6 = MathUtil.lerp(floats[0], floats[1], f5);
                            float f7 = MathHelper.clamp((floats[2] + f5) / 5.0F, 0.0F, 1.0F);
                            this.particleRenderer
                                .renderSingle(
                                    renderEvent.getMatrices(),
                                    renderEvent.getCamera(),
                                    texture,
                                    vec3d.x,
                                    vec3d.y,
                                    vec3d.z,
                                    2.0F * (0.45F + 0.55F * f4),
                                    f6 * 0.2F,
                                    ColorUtil.multiplyAlpha(i4, f7 * f4),
                                    this.brightMode.getValue()
                                );
                        }
                    }
                }
            }
        }
    }

    private int getProjectileColor(Entity entity) {
        String string = this.projectileStyle.colorMode.getValue();
        byte b = -1;
        switch (string.hashCode()) {
            case -1349088399:
                if (string.equals("custom")) {
                    return this.projectileStyle.customColor.getValue();
                }
                break;
            case 973576630:
                if (string.equals("rainbow")) {
                    b = 1;
                }
        }

        switch (b) {
            case 0:
                return this.projectileStyle.customColor.getValue();
            case 1:
                return ColorUtil.rainbow(15, entity.getId() * 30, 0.75F, 1.0F, 1.0F);
            default:
                return Theme.getPrimary();
        }
    }

    private void spawnWorldParticles() {
        if (this.worldGroup.isEnabled()) {
            int i = (int)this.worldSpawnCount.getValue().floatValue();
            if (this.worldPreset.is("rain")) {
                boolean flag = this.worldMotionRule.isNot("none");
                String string = flag ? "random" : "none";
                if (flag) {
                    this.spawnParticles(mc.player, this.worldPhysics, this.worldStyle, i, 2.0F, 10.0F, string, 0.15F, 4000L, -90.0F, -20.0F);
                } else {
                    this.spawnParticles(mc.player, this.worldPhysics, this.worldStyle, i, 2.0F, 10.0F, string, 0.4F, 4000L, -90.0F, -20.0F);
                }
            } else {
                this.spawnParticles(mc.player, this.worldPhysics, this.worldStyle, i, 2.0F, 10.0F, "random", 0.08F, 2500L, -90.0F, 90.0F);
            }
        }
    }

    private void spawnMovementParticles() {
        double d = mc.player.getX();
        double d2 = mc.player.getY();
        double d3 = mc.player.getZ();
        double d4 = d - this.lastX;
        double d5 = d2 - this.lastY;
        double d6 = d3 - this.lastZ;
        boolean flag = !this.hasLastPos;
        this.lastX = d;
        this.lastY = d2;
        this.lastZ = d3;
        this.hasLastPos = true;
        if (!flag && this.movementGroup.isEnabled()) {
            double d7 = d4 * d4 + d5 * d5 + d6 * d6;
            if (!(d7 <= 1.0E-6) && !(d7 > 100.0)) {
                this.spawnParticles(
                    mc.player,
                    this.movementPhysics,
                    this.movementStyle,
                    (int)this.movementSpawnCount.getValue().floatValue(),
                    0.3F,
                    1.0F,
                    "random",
                    0.06F,
                    2500L,
                    -90.0F,
                    90.0F
                );
            }
        }
    }

    private void spawnProjectileParticles() {
        if (!this.projectileGroup.isEnabled()) {
            this.projectileSpin.clear();
        } else {
            int i = (int)this.projectileSpawnCount.getValue().floatValue();
            this.activeProjectileIds.clear();

            for (Entity entity : mc.world.getEntities()) {
                if (this.isTrackedProjectile(entity) && !(entity.getVelocity().lengthSquared() < 0.01)) {
                    this.updateProjectileSpin(entity);
                    this.activeProjectileIds.add(entity.getId());
                    this.spawnParticles(entity, this.projectilePhysics, this.projectileStyle, i, 0.1F, 0.5F, "from_source", 0.05F, 1200L, -90.0F, 90.0F);
                }
            }

            this.projectileSpin.keySet().retainAll(this.activeProjectileIds);
        }
    }

    private void updateProjectileSpin(Entity entity) {
        float[] floats = this.projectileSpin.computeIfAbsent(entity.getId(), integer -> new float[3]);
        floats[0] = floats[1];
        floats[1] += (float)entity.getVelocity().length() * 30.0F;
        floats[2]++;
    }

    private void processPendingEvents() {
        Particles.PendingParticleEvent pendingParticleEvent;
        while ((pendingParticleEvent = this.pendingEvents.poll()) != null) {
            Entity entity2 = pendingParticleEvent.entity();
            if (entity2 != null) {
                if (pendingParticleEvent.totem()) {
                    this.totemEmitters.add(new Particles.TotemEmitter(entity2));
                } else {
                    this.spawnDeathParticles(entity2);
                }
            }
        }
    }

    private void tickTotemEmitters() {
        if (!this.totemEmitters.isEmpty()) {
            if (!this.totemGroup.isEnabled()) {
                this.totemEmitters.clear();
            } else {
                List list = this.getSelectedTextures(this.totemStyle);
                if (list.isEmpty() && this.totemStyle.renderMode.is("textures")) {
                    this.totemEmitters.clear();
                } else {
                    int i = Math.max(1, Math.round(16.0F * this.totemSpawnCount.getValue() / 60.0F));
                    Iterator iterator2 = this.totemEmitters.iterator();

                    while (iterator2.hasNext()) {
                        Particles.TotemEmitter totemEmitter = (Particles.TotemEmitter)iterator2.next();
                        Entity entity2 = totemEmitter.entity;

                        for (int i2 = 0; i2 < i; i2++) {
                            float f = MathUtil.randomBetween(-1.0F, 1.0F);
                            float f2 = MathUtil.randomBetween(-1.0F, 1.0F);
                            float f3 = MathUtil.randomBetween(-1.0F, 1.0F);
                            if (!(f * f + f2 * f2 + f3 * f3 > 1.0F)) {
                                Vec3d vec3d = new Vec3d(entity2.getBodyX(f / 4.0), entity2.getBodyY(0.5 + f2 / 4.0), entity2.getBodyZ(f3 / 4.0));
                                float f4 = f2 + 0.2F;
                                float f5 = (float)Math.sqrt(f * f + f4 * f4 + f3 * f3);
                                float[] floats = WorldParticle.velocityToRotation(f, f4, f3);
                                this.particleRenderer
                                    .addParticle(
                                        this.createParticle(
                                            this.totemStyle,
                                            list,
                                            vec3d,
                                            this.createTargetSupplier(entity2),
                                            -f5,
                                            floats[0],
                                            floats[1],
                                            false,
                                            3300L,
                                            this.totemPhysics
                                        )
                                    );
                            }
                        }

                        if (--totemEmitter.ticksLeft <= 0) {
                            iterator2.remove();
                        }
                    }
                }
            }
        }
    }

    private void spawnDeathParticles(Entity entity) {
        List list = this.getSelectedTextures(this.deathStyle);
        if (!list.isEmpty() || !this.deathStyle.renderMode.is("textures")) {
            EntityRenderer entityRenderer = mc.getEntityRenderDispatcher().getRenderer(entity);
            if (entityRenderer instanceof LivingEntityRenderer livingEntityRenderer && entityRenderer.getAndUpdateRenderState(entity, 1.0F) instanceof LivingEntityRenderState livingEntityRenderState) {
                EntityModel entityModel = livingEntityRenderer.getModel();
                entityModel.setAngles(livingEntityRenderState);
                MatrixStack matrixStack = new MatrixStack();
                float f = livingEntityRenderState.baseScale;
                matrixStack.scale(f, f, f);
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - livingEntityRenderState.bodyYaw));
                matrixStack.scale(-1.0F, -1.0F, 1.0F);
                matrixStack.translate(0.0F, -1.501F, 0.0F);
                ModelPart modelPart = entityModel.getRootPart();
                double[] doubles = new double[]{0.0};
                modelPart.forEachCuboid(matrixStack, (entry, string, i, cuboid) -> doubles[0] += this.getCuboidVolume(cuboid));
                if (!(doubles[0] <= 0.0)) {
                    int i2 = MathHelper.clamp((int)Math.round(doubles[0] * 850.0), 60, 700);
                    double d = i2 / doubles[0];
                    Vec3d vec3d = entity.getEyePos().subtract(0.0, entity.getStandingEyeHeight() / 2.0F, 0.0);
                    Supplier supplier = () -> vec3d;
                    double d2 = entity.getX();
                    double d3 = entity.getY();
                    double d4 = entity.getZ();
                    modelPart.forEachCuboid(
                        matrixStack,
                        (entry2, string2, i3, cuboid2) -> {
                            int i4 = Math.max(1, (int)Math.round(this.getCuboidVolume(cuboid2) * d));
                            Matrix4f matrix4f = entry2.getPositionMatrix();

                            for (int i5 = 0; i5 < i4; i5++) {
                                float f2 = (cuboid2.minX + (cuboid2.maxX - cuboid2.minX) * MathUtil.randomBetween(0.0F, 1.0F)) / 16.0F;
                                float f3 = (cuboid2.minY + (cuboid2.maxY - cuboid2.minY) * MathUtil.randomBetween(0.0F, 1.0F)) / 16.0F;
                                float f4 = (cuboid2.minZ + (cuboid2.maxZ - cuboid2.minZ) * MathUtil.randomBetween(0.0F, 1.0F)) / 16.0F;
                                Vector3f vector3f = matrix4f.transformPosition(f2, f3, f4, new Vector3f());
                                Vec3d vec3d2 = new Vec3d(d2 + vector3f.x, d3 + vector3f.y, d4 + vector3f.z);
                                this.particleRenderer
                                    .addParticle(this.createParticle(this.deathStyle, list, vec3d2, supplier, 0.0F, 0.0F, 0.0F, true, 1500L, this.deathPhysics));
                            }
                        }
                    );
                }
            }
        }
    }

    private double getCuboidVolume(Cuboid cuboid) {
        double d = (cuboid.maxX - cuboid.minX) / 16.0;
        double d2 = (cuboid.maxY - cuboid.minY) / 16.0;
        double d3 = (cuboid.maxZ - cuboid.minZ) / 16.0;
        return d * d2 * d3;
    }

    private boolean isTrackedProjectile(Entity entity) {
        if (!(entity instanceof ProjectileEntity)) {
            return false;
        } else if (entity instanceof ArrowEntity) {
            return this.projectileTypes.isSelected("arrow");
        } else if (entity instanceof SpectralArrowEntity) {
            return this.projectileTypes.isSelected("spectral_arrow");
        } else if (entity instanceof TridentEntity) {
            return this.projectileTypes.isSelected("trident");
        } else if (entity instanceof EnderPearlEntity) {
            return this.projectileTypes.isSelected("pearl");
        } else if (entity instanceof SnowballEntity) {
            return this.projectileTypes.isSelected("snowball");
        } else if (entity instanceof EggEntity) {
            return this.projectileTypes.isSelected("egg");
        } else if (entity instanceof ExperienceBottleEntity) {
            return this.projectileTypes.isSelected("exp_bottle");
        } else if (entity instanceof PotionEntity) {
            return this.projectileTypes.isSelected("potion");
        } else if (entity instanceof FireworkRocketEntity) {
            return this.projectileTypes.isSelected("firework");
        } else if (entity instanceof FishingBobberEntity) {
            return this.projectileTypes.isSelected("bobber");
        } else if (entity instanceof AbstractWindChargeEntity) {
            return this.projectileTypes.isSelected("wind_charge");
        } else if (entity instanceof FireballEntity) {
            return this.projectileTypes.isSelected("fireball");
        } else if (entity instanceof SmallFireballEntity) {
            return this.projectileTypes.isSelected("small_fireball");
        } else if (entity instanceof DragonFireballEntity) {
            return this.projectileTypes.isSelected("dragon_fireball");
        } else if (entity instanceof WitherSkullEntity) {
            return this.projectileTypes.isSelected("wither_skull");
        } else if (entity instanceof ShulkerBulletEntity) {
            return this.projectileTypes.isSelected("shulker_bullet");
        } else {
            return entity instanceof LlamaSpitEntity ? this.projectileTypes.isSelected("llama_spit") : false;
        }
    }

    private void spawnParticles(
        Entity entity,
        ParticlePhysicsSettings particlePhysicsSettings,
        Particles.ParticleStyle particleStyle,
        int i,
        float f,
        float f2,
        String string,
        float f3,
        long l,
        float f4,
        float f5
    ) {
        if (mc.world != null && entity != null && i > 0) {
            List list = this.getSelectedTextures(particleStyle);
            if (list.isEmpty() && particleStyle.renderMode.is("textures")) {
                return;
            }

            Vec3d vec3d = entity.getEyePos().subtract(0.0, entity.getStandingEyeHeight() / 2.0F, 0.0);

            for (int i2 = 0; i2 < i; i2++) {
                Vec3d vec3d2 = this.findSpawnPosition(vec3d, f, f2, f4, f5);
                if (vec3d2 != null) {
                    boolean flag = string.equals("none");
                    float[] floats = this.computeMotionAngles(string, vec3d2, entity.getEyePos());
                    this.particleRenderer
                        .addParticle(this.createParticle(particleStyle, list, vec3d2, this.createTargetSupplier(entity), f3, floats[0], floats[1], flag, l, particlePhysicsSettings));
                }
            }
        }
    }

    private Vec3d findSpawnPosition(Vec3d vec3d, float f, float f2, float f3, float f4) {
        for (int i = 0; i < 10; i++) {
            double[] doubles = WorldParticle.rotationToVelocity(
                MathUtil.randomBetween(-180.0F, 180.0F), MathUtil.randomBetween(f3, f4), MathUtil.randomBetween(f, f2)
            );
            Vec3d vec3d2 = vec3d.add(doubles[0], doubles[1], doubles[2]);
            BlockPos blockPos = BlockPos.ofFloored(vec3d2.x, vec3d2.y, vec3d2.z);
            if (mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty()) {
                return vec3d2;
            }
        }

        return null;
    }

    private float[] computeMotionAngles(String string, Vec3d vec3d, Vec3d vec3d2) {
        byte b = -1;
        switch (string.hashCode()) {
            case -1515903393:
                if (string.equals("to_source")) {
                    return WorldParticle.getRotationTowards(vec3d.x, vec3d.y, vec3d.z, vec3d2.x, vec3d2.y, vec3d2.z);
                }
                break;
            case -938285885:
                if (string.equals("random")) {
                    return new float[]{MathUtil.randomBetween(-180.0F, 180.0F), MathUtil.randomBetween(-90.0F, 90.0F)};
                }
                break;
            case 207632976:
                if (string.equals("from_source")) {
                    b = 2;
                }
        }

        switch (b) {
            case 0:
                return new float[]{MathUtil.randomBetween(-180.0F, 180.0F), MathUtil.randomBetween(-90.0F, 90.0F)};
            case 1:
                return WorldParticle.getRotationTowards(vec3d.x, vec3d.y, vec3d.z, vec3d2.x, vec3d2.y, vec3d2.z);
            case 2:
                return WorldParticle.getRotationTowards(vec3d2.x, vec3d2.y, vec3d2.z, vec3d.x, vec3d.y, vec3d.z);
            default:
                return new float[]{0.0F, 0.0F};
        }
    }

    private Supplier<Vec3d> createTargetSupplier(Entity entity) {
        float f = MathUtil.randomBetween(0.0F, 1.0F);
        return () -> {
            Entity entity2 = entity != null && entity.isAlive() ? entity : mc.player;
            return entity2 == null ? Vec3d.ZERO : entity2.getEyePos().subtract(0.0, entity2.getStandingEyeHeight() * f, 0.0);
        };
    }

    private List<Texture> getSelectedTextures(Particles.ParticleStyle particleStyle) {
        MultiSetting multiSetting = particleStyle.textures;
        ArrayList arrayList = new ArrayList();
        if (multiSetting.isSelected("bloom")) {
            arrayList.add(Texture.BLOOM);
        }

        if (multiSetting.isSelected("star")) {
            arrayList.add(Texture.STAR);
        }

        if (multiSetting.isSelected("star_five")) {
            arrayList.add(Texture.STAR_FIVE);
        }

        if (multiSetting.isSelected("heart")) {
            arrayList.add(Texture.HEART);
        }

        if (multiSetting.isSelected("dollar")) {
            arrayList.add(Texture.DOLLAR);
        }

        if (multiSetting.isSelected("snow")) {
            arrayList.add(Texture.SNOW);
        }

        if (multiSetting.isSelected("pumpkin")) {
            arrayList.add(Texture.PUMPKIN);
        }

        if (multiSetting.isSelected("sadnes")) {
            arrayList.add(Texture.SADNES);
        }

        return arrayList;
    }

    private WorldParticle createParticle(
        Particles.ParticleStyle particleStyle,
        List<Texture> textures,
        Vec3d vec3d,
        Supplier<Vec3d> supplier,
        float f,
        float f2,
        float f3,
        boolean flag,
        long l,
        ParticlePhysicsSettings particlePhysicsSettings
    ) {
        String string = particleStyle.renderMode.getValue();
        boolean flag2 = switch (string) {
            case "fluff" -> true;
            case "random" -> textures.isEmpty() || MathUtil.random(0, 1) == 1;
            default -> false;
        };
        Texture texture = flag2
            ? FLUFF_TEXTURES[MathUtil.random(0, FLUFF_TEXTURES.length - 1)]
            : textures.get(MathUtil.random(0, textures.size() - 1));
        return new WorldParticle(texture, vec3d, supplier, f, f2, f3, flag, l, this.pickColor(particleStyle), particlePhysicsSettings);
    }

    private int pickColor(Particles.ParticleStyle particleStyle) {
        String string = particleStyle.colorMode.getValue();
        byte b = -1;
        switch (string.hashCode()) {
            case -1349088399:
                if (string.equals("custom")) {
                    return particleStyle.customColor.getValue();
                }
                break;
            case 110549953:
                if (string.equals("totem")) {
                    return this.randomTotemColor();
                }
                break;
            case 973576630:
                if (string.equals("rainbow")) {
                    b = 2;
                }
        }

        switch (b) {
            case 0:
                return this.randomTotemColor();
            case 1:
                return particleStyle.customColor.getValue();
            case 2:
                return ColorUtil.fromHsb(MathUtil.randomBetween(0.0F, 1.0F), 0.75F, 1.0F);
            default:
                return Theme.getPrimary();
        }
    }

    private int randomTotemColor() {
        boolean flag;
        float f;
        if (MathUtil.random(0, 3) == 0) {
            flag = true;
            f = 0.6F;
        } else {
            flag = false;
            f = 0.1F;
        }

        float f2 = f + MathUtil.randomBetween(0.0F, 0.2F);
        float f3 = (flag ? 0.6F : 0.4F) + MathUtil.randomBetween(0.0F, 0.3F);
        float f4 = MathUtil.randomBetween(0.0F, 0.2F);
        return ColorUtil.rgb(Math.round(f2 * 255.0F), Math.round(f3 * 255.0F), Math.round(f4 * 255.0F));
    }

    private void updatePhysicsModes() {
        boolean flag = this.worldPreset.is("rain");
        this.worldPhysics.setMotionMode(this.toMotionRule(this.worldMotionRule.getValue()));
        this.worldPhysics.setGravity(flag ? 0.3F : 0.0F);
        this.worldPhysics.setCollisionMode(flag ? ParticlePhysicsSettings.CollisionMode.DIE : ParticlePhysicsSettings.CollisionMode.BOUNCE);
        this.attackPhysics.setMotionMode(this.toMotionRule(this.attackMotionRule.getValue()));
        this.movementPhysics.setMotionMode(this.toMotionRule(this.movementMotionRule.getValue()));
        this.projectilePhysics.setMotionMode(this.toMotionRule(this.projectileMotionRule.getValue()));
    }

    private boolean isAnyGroupEnabled() {
        return this.worldGroup.isEnabled()
            || this.attackGroup.isEnabled()
            || this.totemGroup.isEnabled()
            || this.movementGroup.isEnabled()
            || this.projectileGroup.isEnabled()
            || this.deathGroup.isEnabled();
    }

    private ParticlePhysicsSettings.MotionMode toMotionRule(String string) {
        byte b = -1;
        switch (string.hashCode()) {
            case -1872236345:
                if (string.equals("swirl_left")) {
                    return ParticlePhysicsSettings.MotionMode.SWIRL_LEFT;
                }
                break;
            case -1515903393:
                if (string.equals("to_source")) {
                    return ParticlePhysicsSettings.MotionMode.TO_SOURCE;
                }
                break;
            case -1408024454:
                if (string.equals("alternate")) {
                    b = 5;
                }
                break;
            case 207632976:
                if (string.equals("from_source")) {
                    return ParticlePhysicsSettings.MotionMode.FROM_SOURCE;
                }
                break;
            case 540476803:
                if (string.equals("swirl_random")) {
                    return ParticlePhysicsSettings.MotionMode.SWIRL_RANDOM;
                }
                break;
            case 2095876444:
                if (string.equals("swirl_right")) {
                    return ParticlePhysicsSettings.MotionMode.SWIRL_RIGHT;
                }
        }

        switch (b) {
            case 0:
                return ParticlePhysicsSettings.MotionMode.SWIRL_LEFT;
            case 1:
                return ParticlePhysicsSettings.MotionMode.SWIRL_RIGHT;
            case 2:
                return ParticlePhysicsSettings.MotionMode.SWIRL_RANDOM;
            case 3:
                return ParticlePhysicsSettings.MotionMode.TO_SOURCE;
            case 4:
                return ParticlePhysicsSettings.MotionMode.FROM_SOURCE;
            case 5:
                return ParticlePhysicsSettings.MotionMode.ALTERNATE;
            default:
                return ParticlePhysicsSettings.MotionMode.NONE;
        }
    }

    private static <T extends Setting<?>> T withConfigKey(T value, String string) {
        value.setDisplayName(string);
        return (T)value;
    }

    static final class ParticleStyle {
        final ModeSetting renderMode;
        final MultiSetting textures;
        final ModeSetting colorMode;
        final ColorSetting customColor;

        ParticleStyle(GroupSetting groupSetting, String string) {
            this(groupSetting, string, Particles.COLOR_MODES);
        }

        ParticleStyle(GroupSetting groupSetting, String string, String[] strings) {
            this.renderMode = new ModeSetting(groupSetting, "particles.renderMode", Particles.RENDER_MODES);
            ((Setting<?>) this.renderMode).setDisplayName("particles." + string + ".renderMode");
            this.textures = new MultiSetting(
                groupSetting,
                "particles.textures",
                BooleanSetting.of("bloom", true),
                BooleanSetting.of("star", false),
                BooleanSetting.of("star_five", false),
                BooleanSetting.of("heart", false),
                BooleanSetting.of("dollar", false),
                BooleanSetting.of("snow", false),
                BooleanSetting.of("pumpkin", false),
                BooleanSetting.of("sadnes", false)
            );
            this.textures.setDisplayName("particles." + string + ".textures");
            this.textures.requireAtLeastOne();
            this.textures.setVisibleWhen(() -> !this.renderMode.is("textures") && !this.renderMode.is("random") ? false : true);
            this.colorMode = new ModeSetting(groupSetting, "particles.colorMode", strings);
            ((Setting<?>) this.colorMode).setDisplayName("particles." + string + ".colorMode");
            this.customColor = new ColorSetting(groupSetting, "particles.customColor", -1);
            this.customColor.setDisplayName("particles." + string + ".customColor");
            this.customColor.setVisibleWhen(() -> this.colorMode.is("custom"));
        }
    }

    record PendingParticleEvent(Entity entity, boolean totem) {
    }

    static final class TotemEmitter {
        final Entity entity;
        int ticksLeft = 30;

        TotemEmitter(Entity entity2) {
            this.entity = entity2;
        }
    }
}
