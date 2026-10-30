package tech.sadnes.client.ui.notification;

import java.util.Locale;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public enum NotificationType {
    INFO(Icon.INFO_CIRCLE),
    SUCCESS(Icon.CIRCLE_CHECK),
    WARNING(Icon.WARNING),
    ERROR(Icon.CIRCLE_XMARK);

    private final Icon icon;

    NotificationType(Icon icon2) {
        this.icon = icon2;
    }

    public Icon getIcon() {
        return this.icon;
    }

    public String getTitle() {
        return Language.translate("notification.title." + this.name().toLowerCase(Locale.ROOT), this.name());
    }

    public int getColor() {
        switch (this) {
            case INFO:
                return Theme.getPrimary();
            case SUCCESS:
                return ColorUtil.rgb(70, 200, 130);
            case WARNING:
                return ColorUtil.rgb(240, 190, 75);
            case ERROR:
                return ColorUtil.rgb(235, 90, 90);
            default:
                throw new MatchException(null, null);
        }
    }
}
