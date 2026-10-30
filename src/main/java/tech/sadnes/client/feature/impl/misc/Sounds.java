package tech.sadnes.client.feature.impl.misc;

import java.util.Locale;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.ui.notification.NotificationType;
import tech.sadnes.client.util.SoundPlayer;
import tech.sadnes.client.util.math.StopWatch;

@FeatureInfo(
    name = "Sounds",
    description = "feature.impl.misc.sounds",
    displayName = "feature.impl.misc.sounds",
    category = Category.MISC,
    subcategory = Subcategory.MISC_CLIENT,
    autoEnabled = true,
    allowDisable = false
)
public final class Sounds extends Feature {
    private final MultiSetting types = new MultiSetting(
        "sounds.types",
        BooleanSetting.of("info", true),
        BooleanSetting.of("success", true),
        BooleanSetting.of("warning", true),
        BooleanSetting.of("error", true)
    );
    private final NumberSetting volume = new NumberSetting("sounds.volume", 0.5F, 0.1F, 1.0F, 0.1F);
    private final StopWatch cooldown = new StopWatch();

    public Sounds() {
        SoundPlayer.preload("info.wav", "success.wav", "warning.wav", "error.wav");
    }

    public static Sounds getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(Sounds.class);
    }

    public void playNotificationSound(NotificationType notificationType) {
        if (notificationType != null && mc.player != null && mc.world != null) {
            String string = notificationType.name().toLowerCase(Locale.ROOT);
            if (this.types.has(string) && this.cooldown.isElapsed(70L)) {
                this.cooldown.reset();
                SoundPlayer.play(string + ".wav", this.volume.getValue().floatValue());
            }
        }
    }
}
