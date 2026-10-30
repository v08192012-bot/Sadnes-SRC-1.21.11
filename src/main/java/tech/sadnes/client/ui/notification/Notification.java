package tech.sadnes.client.ui.notification;

import java.util.Objects;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;

public final class Notification {
    private static final double TRANSITION_DURATION = 0.3;
    private static final double BUMP_DURATION = 0.35;
    public final String key;
    public final Animation visibilityAnimation = new Animation();
    public final Animation widthAnimation = new Animation();
    public final Animation transitionAnimation = new Animation();
    public final Animation bumpAnimation = new Animation();
    public int count = 1;
    public String countText = "";
    public NotificationType type;
    public String title;
    public String message;
    public Text messageText;
    public ItemStack itemStack;
    public RegistryEntry<StatusEffect> effect;
    public boolean removing;
    public NotificationType previousType;
    public String previousTitle;
    public String previousMessage;
    public Text previousMessageText;
    public ItemStack previousItemStack;
    public RegistryEntry<StatusEffect> previousEffect;
    private long createdAt;

    public Notification(NotificationType notificationType, String string, String string2, Text text, ItemStack itemStack2, RegistryEntry<StatusEffect> registryEntry, String string3) {
        this.key = string3;
        this.replaceContent(notificationType, string, string2, text, itemStack2, registryEntry);
    }

    public void replaceContent(NotificationType notificationType, String string, String string2, Text text, ItemStack itemStack2, RegistryEntry<StatusEffect> registryEntry) {
        boolean flag = this.type != null;
        if (flag) {
            this.previousType = this.type;
            this.previousTitle = this.title;
            this.previousMessage = this.message;
            this.previousMessageText = this.messageText;
            this.previousItemStack = this.itemStack;
            this.previousEffect = this.effect;
        }

        this.type = notificationType;
        this.title = resolveTitle(notificationType, string);
        this.message = string2;
        this.messageText = text;
        this.itemStack = itemStack2;
        this.effect = registryEntry;
        this.createdAt = System.currentTimeMillis();
        this.count = 1;
        this.transitionAnimation.forceValue(flag ? 0.0 : 1.0);
        if (flag) {
            this.transitionAnimation.animate(1.0, 0.3, Easings.QUART_OUT);
        }
    }

    public void incrementCount() {
        this.count++;
        this.countText = "×" + this.count;
        this.createdAt = System.currentTimeMillis();
        this.bumpAnimation.forceValue(1.0);
        this.bumpAnimation.animate(0.0, 0.35, Easings.QUART_OUT);
    }

    public boolean matches(NotificationType notificationType, String string, String string2, Text text, ItemStack itemStack2, RegistryEntry<StatusEffect> registryEntry) {
        boolean flag;
        if (this.itemStack != itemStack2) {
            if (this.itemStack != null && itemStack2 != null) {
                boolean flag2 = ItemStack.areItemsAndComponentsEqual(this.itemStack, itemStack2);
                return this.type == notificationType
                    && this.title.equals(resolveTitle(notificationType, string))
                    && Objects.equals(this.message, string2)
                    && Objects.equals(this.messageText, text)
                    && flag2
                    && Objects.equals(this.effect, registryEntry);
            }

            flag = false;
        } else {
            flag = true;
        }

        boolean flag3 = flag;
        return this.type == notificationType
            && this.title.equals(resolveTitle(notificationType, string))
            && Objects.equals(this.message, string2)
            && Objects.equals(this.messageText, text)
            && flag3
            && Objects.equals(this.effect, registryEntry);
    }

    private static String resolveTitle(NotificationType notificationType, String string) {
        return string != null && !string.isEmpty() ? string : notificationType.getTitle();
    }

    public long getAge() {
        return System.currentTimeMillis() - this.createdAt;
    }
}
