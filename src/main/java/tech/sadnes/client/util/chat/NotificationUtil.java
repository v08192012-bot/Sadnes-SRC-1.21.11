package tech.sadnes.client.util.chat;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.impl.hud.Notifications;
import tech.sadnes.client.ui.notification.NotificationType;

public final class NotificationUtil {
    private NotificationUtil() {
    }

    private static void post(NotificationType notificationType, String string, Text text, String string2, ItemStack itemStack, RegistryEntry<StatusEffect> registryEntry, String string3) {
        Notifications notifications = Sadnes.getInstance().getFeatureManager().getFeature(Notifications.class);
        if (notifications != null) {
            notifications.show(notificationType, string, text, string2, itemStack, registryEntry, string3);
        }
    }

    public static void notifyKeyed(NotificationType notificationType, String string, String string2) {
        post(notificationType, string, null, string2, null, null, string);
    }

    public static boolean isSuppressed() {
        Notifications notifications = Sadnes.getInstance().getFeatureManager().getFeature(Notifications.class);
        return notifications == null || notifications.hasNoNotifications();
    }

    public static void notify(NotificationType notificationType, String string) {
        post(notificationType, null, null, string, null, null, null);
    }

    public static void notify(NotificationType notificationType, String string, String string2) {
        post(notificationType, string, null, string2, null, null, null);
    }

    public static void info(String string) {
        notify(NotificationType.INFO, string);
    }

    public static void success(String string) {
        notify(NotificationType.SUCCESS, string);
    }

    public static void warning(String string) {
        notify(NotificationType.WARNING, string);
    }

    public static void error(String string) {
        notify(NotificationType.ERROR, string);
    }

    public static void info(String string, String string2) {
        notify(NotificationType.INFO, string, string2);
    }

    public static void success(String string, String string2) {
        notify(NotificationType.SUCCESS, string, string2);
    }

    public static void warning(String string, String string2) {
        notify(NotificationType.WARNING, string, string2);
    }

    public static void error(String string, String string2) {
        notify(NotificationType.ERROR, string, string2);
    }

    public static void notify(NotificationType notificationType, Text text) {
        post(notificationType, null, text, null, null, null, null);
    }

    public static void notify(NotificationType notificationType, String string, Text text) {
        post(notificationType, string, text, null, null, null, null);
    }

    public static void info(Text text) {
        notify(NotificationType.INFO, text);
    }

    public static void success(Text text) {
        notify(NotificationType.SUCCESS, text);
    }

    public static void warning(Text text) {
        notify(NotificationType.WARNING, text);
    }

    public static void error(Text text) {
        notify(NotificationType.ERROR, text);
    }

    public static void info(String string, Text text) {
        notify(NotificationType.INFO, string, text);
    }

    public static void success(String string, Text text) {
        notify(NotificationType.SUCCESS, string, text);
    }

    public static void warning(String string, Text text) {
        notify(NotificationType.WARNING, string, text);
    }

    public static void error(String string, Text text) {
        notify(NotificationType.ERROR, string, text);
    }

    public static void notify(NotificationType notificationType, ItemStack itemStack, String string) {
        post(notificationType, null, null, string, itemStack, null, null);
    }

    public static void notify(NotificationType notificationType, ItemStack itemStack, Text text) {
        post(notificationType, null, text, null, itemStack, null, null);
    }

    public static void notify(NotificationType notificationType, String string, ItemStack itemStack, String string2) {
        post(notificationType, string, null, string2, itemStack, null, null);
    }

    public static void notify(NotificationType notificationType, String string, ItemStack itemStack, Text text) {
        post(notificationType, string, text, null, itemStack, null, null);
    }

    public static void notify(NotificationType notificationType, RegistryEntry<StatusEffect> registryEntry, String string) {
        post(notificationType, null, null, string, null, registryEntry, null);
    }

    public static void notify(NotificationType notificationType, RegistryEntry<StatusEffect> registryEntry, Text text) {
        post(notificationType, null, text, null, null, registryEntry, null);
    }

    public static void notify(NotificationType notificationType, String string, RegistryEntry<StatusEffect> registryEntry, String string2) {
        post(notificationType, string, null, string2, null, registryEntry, null);
    }

    public static void notify(NotificationType notificationType, String string, RegistryEntry<StatusEffect> registryEntry, Text text) {
        post(notificationType, string, text, null, null, registryEntry, null);
    }
}
