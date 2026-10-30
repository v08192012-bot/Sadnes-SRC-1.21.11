package tech.sadnes.client.feature.impl.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.impl.misc.Sounds;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.DragSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.drag.DragComponent;
import tech.sadnes.client.ui.hud.Draggable;
import tech.sadnes.client.ui.hud.HudPanelUtil;
import tech.sadnes.client.ui.notification.Notification;
import tech.sadnes.client.ui.notification.NotificationType;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

@FeatureInfo(
    name = "Notifications",
    description = "feature.impl.hud.notification.notifications",
    displayName = "feature.impl.hud.notification.notifications",
    category = Category.INTERFACE,
    subcategory = Subcategory.HUD_INFORMATION,
    autoEnabled = true
)
public class Notifications extends Feature implements Draggable {
    private static final float HEIGHT = 24.0F;
    private static final float RADIUS = 8.0F;
    private static final float PADDING2 = 4.0F;
    private static final float ICON_SIZE = 8.0F;
    private static final float ICON_BOX_SIZE = 16.0F;
    private static final float PROGRESS_THICKNESS = 1.25F;
    private static final float ICON_BG_INSET = 2.5F;
    private static final float RIGHT_PADDING = 8.0F;
    private static final float ICON_TEXT_GAP = 5.0F;
    private static final float TITLE_FONT_SIZE = 6.0F;
    private static final float MESSAGE_FONT_SIZE = 6.0F;
    private static final float LINE_GAP = 2.0F;
    private static final float TEXT_TOP = 5.0F;
    private static final float COUNTER_FONT_SIZE = 5.0F;
    private static final float COUNTER_INSET = 2.5F;
    private static final float SPACING2 = 3.0F;
    private static final float TEXT_SHIFT = 3.0F;
    private static final float MIN_TEXT_WIDTH = 40.0F;
    private static final float MAX_MESSAGE_WIDTH = 180.0F;
    private static final long PROGRESS_SPIN_PERIOD_MS = 1400L;
    private static final float COUNTER_GAP = 3.0F;
    private static final double SHOW_DURATION = 0.28;
    private static final double HIDE_DURATION = 0.22;
    private static final double WIDTH_DURATION = 0.3;
    private static final double ANCHOR_DURATION = 0.3;
    public final NumberSetting duration = new NumberSetting("notification.duration", 3.0F, 1.0F, 10.0F, 0.5F);
    public final NumberSetting maxStack = new NumberSetting("notification.maxStack", 5.0F, 1.0F, 12.0F, 1.0F);
    public final BooleanSetting progress = new BooleanSetting("notification.progress", true);
    public final ModeSetting direction = new ModeSetting("notification.direction", "Down", new String[]{"Down", "Up"});
    private final DragSetting draggable;
    private final List<Notification> notifications = new CopyOnWriteArrayList<>();
    private float lastTotalHeight = -1.0F;
    private final Animation anchorAnimation = new Animation();

    public Notifications() {
        this.draggable = new DragSetting(
            this.getInfo().name(), () -> mc.getWindow() == null ? 100.0F : Math.max(5.0F, mc.getWindow().getScaledWidth() / 2.0F - 70.0F), () -> 12.0F
        );
        this.draggable.setSize(140.0F, 24.0F);
        this.draggable.setScreenAnchor(4, 1, -1, -1);
        this.draggable.setDraggable(this::isEnabled);
        this.draggable.onInput((drawContext, i, i2, f) -> this.render(drawContext));
        this.draggable.onInput(this);
        DragComponent.register(this.draggable);
    }

    public DragSetting getDraggable() {
        return this.draggable;
    }

    public void show(NotificationType notificationType, String string, Text text, String string2, ItemStack itemStack, RegistryEntry<StatusEffect> registryEntry, String string3) {
        if (this.isEnabled() && notificationType != null) {
            if (text != null || string2 != null && !string2.isEmpty()) {
                Notification notification = this.findExisting(string3, notificationType, string, string2, text, itemStack, registryEntry);
                if (notification == null) {
                    this.notifications.add(0, new Notification(notificationType, string, string2, text, itemStack, registryEntry, string3));
                } else if (string3 != null) {
                    notification.replaceContent(notificationType, string, string2, text, itemStack, registryEntry);
                } else {
                    notification.incrementCount();
                }

                Sounds sounds = Sounds.getInstance();
                if (sounds != null) {
                    sounds.playNotificationSound(notificationType);
                }
            }
        }
    }

    private Notification findExisting(String string, NotificationType notificationType, String string2, String string3, Text text, ItemStack itemStack, RegistryEntry<StatusEffect> registryEntry) {
        for (Notification notification : this.notifications) {
            if (!notification.removing) {
                boolean flag;
                label24: {
                    boolean flag2;
                    if (string != null) {
                        flag2 = string.equals(notification.key);
                    } else {
                        if (notification.key == null) {
                            flag = notification.matches(notificationType, string2, string3, text, itemStack, registryEntry);
                            break label24;
                        }

                        flag2 = false;
                    }

                    flag = flag2;
                }

                if (flag) {
                    return notification;
                }
            }
        }

        return null;
    }

    public boolean hasNoNotifications() {
        return this.notifications.isEmpty();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.notifications.clear();
    }

    @Override
    public void render(DrawContext drawContext) {
        if (this.isEnabled()) {
            this.anchorAnimation.update();
            this.anchorAnimation.animate(this.getAnchorAlignment(), 0.3, Easings.QUART_OUT);
            float f = this.anchorAnimation.get() * 0.5F;
            float f2 = this.draggable.position2.x + this.draggable.size2.x * f;
            boolean flag = this.direction.is("Up");
            float f3 = this.draggable.position2.x;
            float f4 = this.draggable.position2.y;
            boolean flag2;
            if (!(mc.currentScreen instanceof ClickGuiScreen)) {
                flag2 = mc.currentScreen instanceof ChatScreen;
            } else {
                flag2 = true;
            }

            List<Notification> notifications2 = new ArrayList<>(this.notifications);
            if (notifications2.isEmpty()) {
                this.lastTotalHeight = -1.0F;
                if (flag2) {
                    Notification notification2 = new Notification(
                        NotificationType.INFO, null, Language.translate("notification.demo.message", "Drag to position"), null, null, null, null
                    );
                    float f5 = this.getNotificationWidth(notification2);
                    this.renderNotification(drawContext, notification2, f2 - f5 * f, f4, f5, 1.0F);
                    this.draggable.size2.set(Math.max(f5, 1.0F), 24.0F);
                } else {
                    this.draggable.size2.set(1.0F, 1.0F);
                }
            } else {
                this.expireNotifications(notifications2);

                for (Notification notification3 : notifications2) {
                    notification3.visibilityAnimation.update();
                    notification3.visibilityAnimation
                        .animate(notification3.removing ? 0.0 : 1.0, notification3.removing ? 0.22 : 0.28, notification3.removing ? Easings.QUAD_IN : Easings.QUART_OUT);
                    notification3.transitionAnimation.update();
                    notification3.bumpAnimation.update();
                }

                this.notifications.removeIf(notification4 -> notification4.removing && notification4.visibilityAnimation.get() <= 0.01F);
                float f6 = 0.0F;
                float f7 = 0.0F;

                for (Notification notification5 : notifications2) {
                    notification5.widthAnimation.update();
                    float f8 = this.getNotificationWidth(notification5);
                    if (notification5.widthAnimation.get() <= 0.0F) {
                        notification5.widthAnimation.forceValue(f8);
                    } else {
                        notification5.widthAnimation.animate(f8, 0.3, Easings.QUART_OUT);
                    }

                    f6 = Math.max(f6, notification5.widthAnimation.get());
                    f7 += 27.0F * notification5.visibilityAnimation.get();
                }

                float f9 = Math.max(f7 - 3.0F, 24.0F);
                if (flag && !this.draggable.isDragging() && this.lastTotalHeight >= 0.0F) {
                    float f10 = f9 - this.lastTotalHeight;
                    if (Math.abs(f10) > 0.001F) {
                        this.draggable.setPosition(f3, f4 - f10);
                        f4 = this.draggable.position2.y;
                    }
                }

                this.lastTotalHeight = f9;
                float f11 = 0.0F;

                for (Notification notification6 : notifications2) {
                    float f12 = notification6.visibilityAnimation.get();
                    if (f12 > 0.001F) {
                        float f13 = notification6.widthAnimation.get();
                        float f14 = flag ? f4 + f9 - 24.0F - f11 : f4 + f11;
                        this.renderNotification(drawContext, notification6, f2 - f13 * f, f14, f13, f12);
                    }

                    f11 += 27.0F * f12;
                }

                this.draggable.size2.set(Math.max(f6, 1.0F), f9);
            }
        }
    }

    private int getAnchorAlignment() {
        if (this.draggable.getScreenGridX() >= 0) {
            return this.draggable.getScreenSelfAnchorX();
        } else {
            return this.draggable.isAnchored() && this.draggable.getAnchorTargetX() >= 0 ? this.draggable.getAnchorSelfX() : 0;
        }
    }

    private void expireNotifications(List<Notification> notifications2) {
        long l = (long)(this.duration.getValue() * 1000.0F);
        int i = Math.max(1, this.maxStack.getValue().intValue());
        int i2 = notifications2.size();

        for (int i3 = 0; i3 < i2; i3++) {
            Notification notification = (Notification)notifications2.get(i3);
            if (!notification.removing && (i3 >= i || notification.getAge() >= l)) {
                notification.removing = true;
            }
        }
    }

    private void renderNotification(DrawContext drawContext, Notification notification, float f, float f2, float f3, float f4) {
        float f5 = notification.transitionAnimation.get();
        int i = notification.previousType == null ? notification.type.getColor() : ColorUtil.interpolate(notification.previousType.getColor(), notification.type.getColor(), f5);
        float f6 = (float)Easings.EXPO_OUT.ease(f4);
        Matrix3x2fStack matrix3x2fStack = drawContext.getMatrices();
        matrix3x2fStack.pushMatrix();
        matrix3x2fStack.translate(f, f2 + 12.0F);
        matrix3x2fStack.scale(1.0F, f6);
        matrix3x2fStack.translate(-f, -(f2 + 12.0F));
        HudPanelUtil.drawPanel(drawContext, f, f2, f3, 24.0F, f4, 8.0F);
        float f7 = f + 4.0F;
        float f8 = f2 + 4.0F;
        this.renderIcon(drawContext, notification, f7, f8, i, f4, f5);
        float f9 = f7 + 16.0F + 5.0F;
        float f10 = Math.max(f3 - 33.0F, 1.0F);
        if (f5 < 1.0F) {
            this.renderText(drawContext, f9, f2, f10, f4 * (1.0F - f5), -3.0F * f5, notification.previousTitle, notification.previousMessage, notification.previousMessageText);
        }

        this.renderText(drawContext, f9, f2, f10, f4 * f5, 3.0F * (1.0F - f5), notification.title, notification.message, notification.messageText);
        if (notification.count > 1) {
            float f11 = HEADER_FONT.getWidth(notification.title, 6.0F);
            float f12 = Math.max(0.0F, Math.min(1.0F, (f10 - f11 - 3.0F) / this.getCounterWidth(notification)));
            this.renderCounter(drawContext, notification, f9 + f11 + 3.0F, f2 + 5.0F, i, f4 * f12);
        }

        matrix3x2fStack.popMatrix();
    }

    private void renderText(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, String string, String string2, Text text) {
        if (string != null && !(f4 <= 0.004F)) {
            float f6 = 5.0F + f5;
            new RenderUtil(drawContext, f, f2, f3, 24.0F).textTopLeft(HEADER_FONT, string, 0.0F, f6, ColorUtil.setAlphaf(Theme.getTextColor(), f4), 6.0F);
            float f7 = f6 + 6.0F + 2.0F;
            int i = ColorUtil.setAlphaf(Theme.getSecondaryText(), f4);
            RenderUtil renderUtil = new RenderUtil(drawContext, f, f2, f3, 24.0F);
            if (text != null) {
                renderUtil.textTopLeft(BODY_FONT, text, 0.0F, f7, i, 6.0F);
            } else {
                renderUtil.scrollingTextTop(BODY_FONT, string2, 0.0F, f7, f3, i, 6.0F, false);
            }
        }
    }

    private void renderCounter(DrawContext drawContext, Notification notification, float f, float f2, int i, float f3) {
        if (!(f3 <= 0.004F)) {
            float f4 = this.getCounterWidth(notification);
            float f5 = f + f4 / 2.0F;
            float f6 = f2 + 3.0F;
            float f7 = 1.0F + 0.3F * notification.bumpAnimation.get();
            Matrix3x2fStack matrix3x2fStack = drawContext.getMatrices();
            matrix3x2fStack.pushMatrix();
            matrix3x2fStack.translate(f5, f6);
            matrix3x2fStack.scale(f7, f7);
            matrix3x2fStack.translate(-f5, -f6);
            new RenderUtil(drawContext, f, f6 - 7.5F / 2.0F, f4, 7.5F)
                .rect(ColorUtil.setAlphaf(i, f3 * 0.18F), Radius.all(7.5F / 2.0F))
                .textCenter(HEADER_FONT, notification.countText, ColorUtil.setAlphaf(i, f3), 5.0F);
            matrix3x2fStack.popMatrix();
        }
    }

    private float getCounterWidth(Notification notification) {
        return HEADER_FONT.getWidth(notification.countText, 5.0F) + 5.0F;
    }

    private void renderIcon(DrawContext drawContext, Notification notification, float f, float f2, int i, float f3, float f4) {
        new RenderUtil(drawContext, f + 2.5F, f2 + 2.5F, 11.0F, 11.0F).rect(ColorUtil.setAlphaf(i, f3 * 0.16F), Radius.all(11.0F / 2.0F));
        float f5 = f + 4.0F;
        float f6 = f2 + 4.0F;
        if (f4 < 1.0F) {
            this.renderIconContent(drawContext, notification.previousType, notification.previousItemStack, notification.previousEffect, f5, f6, f3 * (1.0F - f4), i);
        }

        this.renderIconContent(drawContext, notification.type, notification.itemStack, notification.effect, f5, f6, f3 * f4, i);
        if (this.progress.getValue()) {
            new RenderUtil(drawContext, f, f2, 16.0F, 16.0F).circle(1.25F, ColorUtil.setAlphaf(Theme.getSecondaryText(), f3 * 0.2F));
            float f7 = 360.0F * this.getRemainingProgress(notification);
            if (!(f7 <= 0.5F)) {
                float f8 = (float)(System.currentTimeMillis() % 1400L) / 1400.0F;
                new RenderUtil(drawContext, f, f2, 16.0F, 16.0F).arc(0.0F, f7, 1.25F, ColorUtil.setAlphaf(i, f3), 1.0F, f8, 0.85F);
            }
        }
    }

    private void renderIconContent(
        DrawContext drawContext, NotificationType notificationType, ItemStack itemStack, RegistryEntry<StatusEffect> registryEntry, float f, float f2, float f3, int i
    ) {
        if (notificationType != null && !(f3 <= 0.004F)) {
            if (itemStack != null && !itemStack.isEmpty()) {
                this.renderItemIcon(drawContext, itemStack, f, f2, f3);
            } else if (registryEntry != null) {
                this.renderEffectIcon(drawContext, registryEntry, f, f2, f3);
            } else {
                new RenderUtil(drawContext, f, f2, 8.0F, 8.0F).icon(notificationType.getIcon(), 0.0F, 0.0F, 8.0F, ColorUtil.setAlphaf(i, f3));
            }
        }
    }

    private float getRemainingProgress(Notification notification) {
        float f = Math.max(this.duration.getValue() * 1000.0F, 1.0F);
        float f2 = Math.max(0.0F, Math.min(1.0F, 1.0F - (float)notification.getAge() / f));
        return (float)Easings.SINE_IN_OUT.ease(f2);
    }

    private float getContentWidth(Notification notification) {
        float f = notification.messageText != null ? BODY_FONT.getTextWidth(notification.messageText, 6.0F) : BODY_FONT.getWidth(notification.message, 6.0F);
        if (notification.messageText == null) {
            f = Math.min(f, 180.0F);
        }

        float f2 = HEADER_FONT.getWidth(notification.title, 6.0F);
        if (notification.count > 1) {
            f2 += 3.0F + this.getCounterWidth(notification);
        }

        return Math.max(40.0F, Math.max(f, f2));
    }

    private float getNotificationWidth(Notification notification) {
        return 25.0F + this.getContentWidth(notification) + 8.0F;
    }

    private void renderItemIcon(DrawContext drawContext, ItemStack itemStack, float f, float f2, float f3) {
        float f4 = MathUtil.roundToStep(f, 0.5F);
        float f5 = MathUtil.roundToStep(f2, 0.5F);
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(drawContext, f4, f5, 0.5F);
        GuiRenderUtil.drawItem(drawContext, itemStack, f4, f5, f3);
        MatrixUtil.pop(drawContext);
    }

    private void renderEffectIcon(DrawContext drawContext, RegistryEntry<StatusEffect> registryEntry, float f, float f2, float f3) {
        Identifier identifier = InGameHud.getEffectTexture(registryEntry);
        SpriteAtlasTexture spriteAtlasTexture = mc.getAtlasManager().getAtlasTexture(Atlases.GUI);
        Sprite sprite = spriteAtlasTexture.getSprite(identifier);
        GuiRenderUtil.drawSprite(drawContext, sprite, f, f2, 8.0F, 8.0F, f3);
    }
}
