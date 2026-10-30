package tech.sadnes.client.feature.impl.render;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.player.RaycastUtil;

@FeatureInfo(
    name = "Removals",
    description = "feature.impl.render.removals",
    displayName = "feature.impl.render.removals",
    category = Category.VISUALS,
    subcategory = Subcategory.WORLD
)
public final class Removals extends Feature {
    private static final Set<Block> SCULK_BLOCKS = Set.of(Blocks.SCULK, Blocks.SCULK_VEIN, Blocks.SCULK_CATALYST, Blocks.SCULK_SHRIEKER);
    private static final Set<Block> SCULK_SENSOR_BLOCKS = Set.of(Blocks.SCULK_SENSOR, Blocks.CALIBRATED_SCULK_SENSOR);
    private static final Set<ParticleType<?>> SCULK_PARTICLES = Set.of(
        ParticleTypes.VIBRATION, ParticleTypes.SCULK_CHARGE, ParticleTypes.SCULK_CHARGE_POP, ParticleTypes.SCULK_SOUL, ParticleTypes.SHRIEK
    );
    private static volatile Set<Block> hiddenBlocks = Set.of();
    private final MultiSetting elements = new MultiSetting(
            "removals.elements",
            BooleanSetting.of("fire", true),
            BooleanSetting.of("fog", false),
            BooleanSetting.of("lava", false),
            BooleanSetting.of("water", false),
            BooleanSetting.of("shake", true),
            BooleanSetting.of("bossbar", false),
            BooleanSetting.of("player_glow", true),
            BooleanSetting.of("enchant_glint", false),
            BooleanSetting.of("bad_effects", true),
            BooleanSetting.of("black_hearts", true),
            BooleanSetting.of("scoreboard", false),
            BooleanSetting.of("totem_anim", false),
            BooleanSetting.of("hand_anim", true),
            BooleanSetting.of("block_particles", false),
            BooleanSetting.of("mace_particles", false),
            BooleanSetting.of("explosion_particles", false),
            BooleanSetting.of("vegetation", false),
            BooleanSetting.of("sculk", false),
            BooleanSetting.of("sculk_sensors", false),
            BooleanSetting.of("sculk_particles", false),
            BooleanSetting.of("eat_bob", false),
            BooleanSetting.of("remove_sounds", false)
        )
        .setOnChange(this::updateHiddenBlocks);
    private final MultiSetting soundCheck = new MultiSetting(
            "removals.soundCheck",
            BooleanSetting.of("event_spawn", true),
            BooleanSetting.of("wither_hit", true),
            BooleanSetting.of("trident", true),
            BooleanSetting.of("exp_bottle", true),
            BooleanSetting.of("thunder", true),
            BooleanSetting.of("totem", true)
        )
        .setVisibleWhen(() -> this.elements.has("remove_sounds"));
    private final BooleanSetting hideEntities = new BooleanSetting("removals.hideEntities", false);
    private final MultiSetting hideTypes = new MultiSetting("removals.hideTypes", BooleanSetting.of("items", true), BooleanSetting.of("players", false))
        .setVisibleWhen(() -> this.hideEntities.getValue());
    private final ModeSetting hideCondition = new ModeSetting("removals.hideCondition", new String[]{"always", "closer", "farther", "wall"})
        .setVisibleWhen(() -> this.hideEntities.getValue());
    private final NumberSetting hideDistance = new NumberSetting("removals.hideDistance", 16.0F, 1.0F, 128.0F, 1.0F)
        .setVisibleWhen(() -> !this.hideEntities.getValue() || !this.hideCondition.is("closer") && !this.hideCondition.is("farther") ? false : true);

    public static Removals getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(Removals.class);
    }

    public static boolean isRemoved(String string) {
        Removals removals = getInstance();
        return removals != null && removals.isEnabled() && removals.elements.has(string);
    }

    public static boolean isEntityHidden(Entity entity) {
        Removals removals = getInstance();
        return removals != null && removals.isEnabled() && removals.shouldHideEntity(entity);
    }

    public static boolean isBlockHidden(Block block) {
        return hiddenBlocks.contains(block);
    }

    public static boolean shouldCullFace(BlockState blockState, BlockState blockState2) {
        boolean flag;
        if (blockState.getBlock() == blockState2.getBlock()) {
            flag = isBlockHidden(blockState2.getBlock());
        } else {
            flag = false;
        }

        return flag && blockState.getFluidState() == blockState2.getFluidState();
    }

    public static boolean isParticleRemoved(ParticleEffect particleEffect) {
        boolean flag = SCULK_PARTICLES.contains(particleEffect.getType()) || particleEffect == DustColorTransitionParticleEffect.DEFAULT;
        boolean flag2 = particleEffect.getType() == ParticleTypes.EXPLOSION || particleEffect.getType() == ParticleTypes.EXPLOSION_EMITTER;
        return flag && isRemoved("sculk_particles") || flag2 && isRemoved("explosion_particles");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.updateHiddenBlocks();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.updateHiddenBlocks();
    }

    private void updateHiddenBlocks() {
        HashSet hashSet = new HashSet();
        if (this.isEnabled() && this.elements.has("sculk")) {
            hashSet.addAll(SCULK_BLOCKS);
        }

        if (this.isEnabled() && this.elements.has("sculk_sensors")) {
            hashSet.addAll(SCULK_SENSOR_BLOCKS);
        }

        if (this.isEnabled() && this.elements.has("vegetation")) {
            hashSet.addAll(
                Set.of(
                    Blocks.SHORT_GRASS,
                    Blocks.TALL_GRASS,
                    Blocks.FERN,
                    Blocks.LARGE_FERN,
                    Blocks.SHORT_DRY_GRASS,
                    Blocks.TALL_DRY_GRASS,
                    Blocks.DEAD_BUSH,
                    Blocks.BUSH,
                    Blocks.FIREFLY_BUSH,
                    Blocks.SEAGRASS,
                    Blocks.TALL_SEAGRASS,
                    Blocks.CRIMSON_ROOTS,
                    Blocks.WARPED_ROOTS,
                    Blocks.NETHER_SPROUTS,
                    Blocks.DANDELION,
                    Blocks.POPPY,
                    Blocks.BLUE_ORCHID,
                    Blocks.ALLIUM,
                    Blocks.AZURE_BLUET,
                    Blocks.RED_TULIP,
                    Blocks.ORANGE_TULIP,
                    Blocks.WHITE_TULIP,
                    Blocks.PINK_TULIP,
                    Blocks.OXEYE_DAISY,
                    Blocks.CORNFLOWER,
                    Blocks.LILY_OF_THE_VALLEY,
                    Blocks.WITHER_ROSE,
                    Blocks.SUNFLOWER,
                    Blocks.LILAC,
                    Blocks.ROSE_BUSH,
                    Blocks.PEONY,
                    Blocks.TORCHFLOWER,
                    Blocks.PITCHER_PLANT,
                    Blocks.OPEN_EYEBLOSSOM,
                    Blocks.CLOSED_EYEBLOSSOM,
                    Blocks.PINK_PETALS,
                    Blocks.WILDFLOWERS,
                    Blocks.SPORE_BLOSSOM
                )
            );
        }

        if (!hashSet.equals(hiddenBlocks)) {
            hiddenBlocks = Set.copyOf(hashSet);
            if (mc.world != null) {
                mc.execute(mc.worldRenderer::reload);
            }
        }
    }

    private boolean shouldHideEntity(Entity entity) {
        if (!this.hideEntities.getValue() || entity == null || mc.player == null) {
            return false;
        } else if (entity == mc.player) {
            return false;
        } else {
            return !this.isHideType(entity) ? false : this.matchesHideCondition(entity);
        }
    }

    private boolean isHideType(Entity entity) {
        if (entity instanceof ItemEntity) {
            return this.hideTypes.has("items");
        } else {
            return entity instanceof PlayerEntity ? this.hideTypes.has("players") : false;
        }
    }

    private boolean matchesHideCondition(Entity entity) {
        if (this.hideCondition.is("always")) {
            return true;
        }

        if (this.hideCondition.is("wall")) {
            return this.isBehindWall(entity);
        }

        double d = mc.player.distanceTo(entity);
        return this.hideCondition.is("closer") ? d <= this.hideDistance.getValue().floatValue() : d >= this.hideDistance.getValue().floatValue();
    }

    private boolean isBehindWall(Entity entity) {
        return !RaycastUtil.canSee(mc.player.getEyePos(), entity.getBoundingBox().getCenter());
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive()) {
            Packet packet = packetEvent.getPacket();
            if (this.elements.has("mace_particles") && packet instanceof WorldEventS2CPacket && ((WorldEventS2CPacket)packet).getEventId() == 2013) {
                packetEvent.cancel();
            } else if (this.elements.has("remove_sounds")) {
                if (packet instanceof PlaySoundS2CPacket playSoundS2CPacket && this.isSoundBlocked(playSoundS2CPacket.getSound().value())) {
                    packetEvent.cancel();
                }

                if (packet instanceof PlaySoundFromEntityS2CPacket playSoundFromEntityS2CPacket && this.isSoundBlocked(playSoundFromEntityS2CPacket.getSound().value())) {
                    packetEvent.cancel();
                }

                if (packet instanceof WorldEventS2CPacket worldEventS2CPacket && worldEventS2CPacket.getEventId() == 2002 && worldEventS2CPacket.getData() == -13083194 && this.soundCheck.has("exp_bottle")) {
                    packetEvent.cancel();
                }
            }
        }
    }

    private boolean isSoundBlocked(SoundEvent soundEvent) {
        String string = soundEvent.id().getPath();
        if (string.equals("ui.toast.challenge_complete") && this.soundCheck.has("event_spawn")) {
            return true;
        } else if (string.equals("entity.experience_orb.pickup") && this.soundCheck.has("wither_hit")) {
            return true;
        } else if (string.startsWith("item.trident") && this.soundCheck.has("trident")) {
            return true;
        } else {
            return string.equals("item.totem.use") && this.soundCheck.has("totem")
                ? true
                : string.startsWith("entity.lightning_bolt") && this.soundCheck.has("thunder");
        }
    }

    public MultiSetting getElements() {
        return this.elements;
    }

    public MultiSetting getSoundCheck() {
        return this.soundCheck;
    }

    public BooleanSetting getHideEntities() {
        return this.hideEntities;
    }

    public MultiSetting getHideTypes() {
        return this.hideTypes;
    }

    public ModeSetting getHideCondition() {
        return this.hideCondition;
    }

    public NumberSetting getHideDistance() {
        return this.hideDistance;
    }
}
