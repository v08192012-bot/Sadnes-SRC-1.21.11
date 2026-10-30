package tech.sadnes.client.feature.impl.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.cosmetic.Cosmetic;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.EntityRemoveEvent;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.render.ChinaHatPhysics;
import tech.sadnes.client.render.ChinaHatRenderer;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.util.color.Theme;

@FeatureInfo(
    name = "ChinaHat",
    description = "feature.impl.render.chinahat",
    displayName = "feature.impl.render.chinahat",
    category = Category.VISUALS,
    subcategory = Subcategory.ENTITIES
)
public final class ChinaHat extends Feature {
    private static final long CLEANUP_INTERVAL_NANOS = 2000000000L;
    private final ModeSetting mode = new ModeSetting("chinahat.mode", new String[]{"default", "mafia"});
    private final MultiSetting targets = new MultiSetting(
        "chinahat.targets",
        BooleanSetting.of("self", true),
        BooleanSetting.of("friends", true),
        BooleanSetting.of("party", true),
        BooleanSetting.of("others", false)
    );
    private final GroupSetting physics = new GroupSetting("chinahat.physics", true);
    private final NumberSetting inertia = new NumberSetting(this.physics, "chinahat.inertia", 100.0F, 20.0F, 200.0F, 5.0F, Unit.PERCENT);
    private final GroupSetting wave = new GroupSetting("chinahat.wave", true).setVisibleWhen(() -> this.mode.is("mafia"));
    private final NumberSetting waveStrength = new NumberSetting(this.wave, "chinahat.wave.strength", 100.0F, 20.0F, 250.0F, 5.0F, Unit.PERCENT);
    private final NumberSetting waveSpeed = new NumberSetting(this.wave, "chinahat.wave.speed", 100.0F, 20.0F, 300.0F, 10.0F, Unit.PERCENT);
    private final Map<Integer, ChinaHatPhysics> hatStates = new ConcurrentHashMap<>();
    private long lastCleanup;

    public static ChinaHat getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(ChinaHat.class);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.hatStates.clear();
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.hatStates.clear();
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveEvent entityRemoveEvent) {
        this.hatStates.remove(entityRemoveEvent.getEntityId());
    }

    public boolean shouldRender(boolean flag, boolean flag2, boolean flag3) {
        if (flag) {
            return this.targets.isSelected("self");
        } else if (this.targets.isSelected("others")) {
            return true;
        } else {
            return flag2 && this.targets.isSelected("friends") ? true : flag3 && this.targets.isSelected("party");
        }
    }

    public ChinaHatPhysics updateHatState(LivingEntity livingEntity, float f) {
        ChinaHatPhysics chinaHatPhysics = this.hatStates.computeIfAbsent(livingEntity.getId(), integer -> new ChinaHatPhysics());
        chinaHatPhysics.update(
            livingEntity,
            f,
            this.physics.isEnabled() ? this.inertia.getValue() / 100.0F : 0.0F,
            this.wave.isEnabled() ? this.waveStrength.getValue() / 100.0F : 0.0F,
            this.waveSpeed.getValue() / 100.0F
        );
        this.cleanupStates();
        return chinaHatPhysics;
    }

    private void cleanupStates() {
        long l = System.nanoTime();
        if (l - this.lastCleanup >= 2000000000L) {
            this.lastCleanup = l;
            ClientWorld clientWorld = mc.world;
            if (clientWorld == null) {
                this.hatStates.clear();
            } else {
                this.hatStates.keySet().removeIf(integer -> clientWorld.getEntityById(integer) == null);
            }
        }
    }

    public int getColor() {
        return Theme.getPrimary();
    }

    public ChinaHatRenderer.Style getHatStyle() {
        return this.mode.is("mafia") ? ChinaHatRenderer.Style.MAFIA : ChinaHatRenderer.Style.CLASSIC;
    }

    public float getAlpha() {
        return 0.6F;
    }

    public float getHeightOffset(boolean flag, Cosmetic[] cosmetics, float f) {
        boolean flag2 = !Float.isNaN(f);
        boolean flag3;
        if (flag) {
            flag3 = !flag2;
        } else {
            flag3 = false;
        }

        float f2 = this.mode.is("mafia") ? (flag3 ? 0.555F : 0.515F) : (flag3 ? 0.5F : 0.39F);
        float f3 = flag3 ? 0.59375F : 0.53125F;
        float f4 = flag2 ? f : f3;
        float f5 = Cosmetic.headLift(f);
        if (cosmetics != null) {
            for (Cosmetic cosmetic : cosmetics) {
                if (cosmetic != null && cosmetic.isOnHead()) {
                    f4 = Math.max(f4, cosmetic.heightAboveHeadPivot() + f5);
                }
            }
        }

        return f2 + f4 - f3;
    }

    public float getRadius() {
        return this.mode.is("mafia") ? 0.43F : 0.75F;
    }
}
