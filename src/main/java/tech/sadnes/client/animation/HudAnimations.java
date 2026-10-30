package tech.sadnes.client.animation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import org.joml.Matrix3x2fStack;
import tech.sadnes.api.mixin.accessor.ChatHudAccessor;
import tech.sadnes.client.feature.impl.render.Animations;

public final class HudAnimations {
    public static final HudAnimations INSTANCE = new HudAnimations();
    private final Animation tabListAnimation = new Animation();
    private boolean tabListClosing;
    private long lastMessageTime;

    private HudAnimations() {
    }

    public void setTabListVisible(boolean flag) {
        boolean flag2 = this.isEnabled("tab");
        if (flag) {
            this.tabListClosing = false;
            this.tabListAnimation.forceValue(0.0);
            if (!flag2) {
                this.tabListAnimation.forceValue(1.0);
            } else {
                this.tabListAnimation.animate(1.0, 0.2, Easings.QUART_OUT);
            }
        } else if (!flag2) {
            this.tabListAnimation.forceValue(0.0);
        } else {
            this.tabListClosing = true;
            this.tabListAnimation.animate(0.0, 0.2, Easings.QUART_OUT);
        }
    }

    public boolean beginTabListTransform(DrawContext drawContext, float f) {
        if (!this.isEnabled("tab")) {
            return false;
        }

        this.tabListAnimation.update();
        float f2 = this.tabListAnimation.get();
        if (f2 >= 1.0F) {
            return false;
        }

        float f3 = f / 2.0F;
        Matrix3x2fStack matrix3x2fStack = drawContext.getMatrices();
        matrix3x2fStack.pushMatrix();
        matrix3x2fStack.translate(f3, 0.0F);
        matrix3x2fStack.scale(f2, f2);
        matrix3x2fStack.translate(-f3, 0.0F);
        return true;
    }

    public void endTabListTransform(DrawContext drawContext) {
        drawContext.getMatrices().popMatrix();
    }

    public void renderClosingTabList(DrawContext drawContext) {
        if (this.tabListClosing) {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (this.isEnabled("tab") && minecraftClient.world != null) {
                this.tabListAnimation.update();
                if (this.tabListAnimation.isFinished()) {
                    this.tabListClosing = false;
                } else {
                    Scoreboard scoreboard = minecraftClient.world.getScoreboard();
                    minecraftClient.inGameHud
                        .getPlayerListHud()
                        .render(drawContext, minecraftClient.getWindow().getScaledWidth(), scoreboard, scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.LIST));
                }
            } else {
                this.tabListClosing = false;
            }
        }
    }

    public void onChatMessage() {
        this.lastMessageTime = System.currentTimeMillis();
    }

    public int getChatMessageOffset() {
        if (!this.isEnabled("chat")) {
            return 0;
        }

        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (((ChatHudAccessor)minecraftClient.inGameHud.getChatHud()).sadnes$getScrolledLines() != 0) {
            return 0;
        }

        float f = Math.min((float)(System.currentTimeMillis() - this.lastMessageTime) / 170.0F, 1.0F);
        if (f >= 1.0F) {
            return 0;
        }

        int i = (int)(9.0 * (minecraftClient.options.getChatLineSpacing().getValue() + 1.0));
        return (int)(0.8F * i * (1.0F - f));
    }

    public float getChatOpenOffset(long l) {
        if (!this.isEnabled("chat_open")) {
            return 0.0F;
        }

        float f = 1.0F - Math.min((float)(System.currentTimeMillis() - l) / 170.0F, 1.0F);
        return f <= 0.0F ? 0.0F : (2.70158F * f * f * f - 1.70158F * f * f) * 10.0F;
    }

    public float getTypingProgress(long l) {
        if (l != 0L && this.isEnabled("typing")) {
            float f = Math.min((float)(System.currentTimeMillis() - l) / 130.0F, 1.0F);
            return (float)Easings.CUBIC_OUT.ease(f);
        } else {
            return 1.0F;
        }
    }

    private boolean isEnabled(String string) {
        Animations animations = Animations.getInstance();
        return animations != null && animations.isEnabled(string);
    }
}
