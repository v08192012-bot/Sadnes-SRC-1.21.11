package tech.sadnes.client.animation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.ReconfiguringScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.util.math.ColorHelper;
import org.joml.Matrix3x2fStack;
import tech.sadnes.client.feature.impl.render.Animations;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.menu.MainMenuScreen;
import tech.sadnes.client.ui.screen.CrosshairEditorScreen;
import tech.sadnes.client.ui.screen.LanguageScreen;
import tech.sadnes.client.ui.screen.ScriptScreen;
import tech.sadnes.client.ui.screen.settings.SettingsScreen;
import tech.sadnes.client.util.color.ColorUtil;

public final class ContainerAnimations {
    public static final ContainerAnimations INSTANCE = new ContainerAnimations();
    private long openTime;
    private long firstOpenTime;
    private float slideOffset;
    private boolean pushed;

    private ContainerAnimations() {
    }

    public void onScreenChange(Screen screen, Screen screen2) {
        if (screen2 != null) {
            if (screen == null || screen.getClass() != screen2.getClass()) {
                this.openTime = System.currentTimeMillis();
                if (screen == null) {
                    this.firstOpenTime = this.openTime;
                }
            }
        }
    }

    public void beginRender(DrawContext drawContext, Screen screen) {
        this.pushed = false;
        if (this.shouldAnimate(screen)) {
            this.slideOffset = ((float)Easings.BACK_OUT.ease(this.getProgress(this.openTime)) - 1.0F) * 9.0F;
            if (this.slideOffset != 0.0F) {
                this.pushTranslation(drawContext);
                this.pushed = true;
            }
        }
    }

    public void endRender(DrawContext drawContext) {
        if (this.pushed) {
            this.pushed = false;
            drawContext.getMatrices().popMatrix();
        }
    }

    public boolean popIfPushed(DrawContext drawContext) {
        if (!this.pushed) {
            return false;
        }

        drawContext.getMatrices().popMatrix();
        return true;
    }

    public void applyTranslation(DrawContext drawContext) {
        this.pushTranslation(drawContext);
    }

    public int applyFade(int i, Screen screen) {
        float f = this.getFadeProgress(screen);
        return f >= 1.0F ? i : ColorUtil.multiplyAlpha(i, f);
    }

    public int getFadeColor() {
        return ColorHelper.getWhite(this.getFadeProgress(MinecraftClient.getInstance().currentScreen));
    }

    private float getFadeProgress(Screen screen) {
        return !this.shouldAnimate(screen) ? 1.0F : Math.min(this.getProgress(this.firstOpenTime) * 2.6F, 1.0F);
    }

    private void pushTranslation(DrawContext drawContext) {
        Matrix3x2fStack matrix3x2fStack = drawContext.getMatrices();
        matrix3x2fStack.pushMatrix();
        matrix3x2fStack.translate(0.0F, this.slideOffset);
    }

    private boolean shouldAnimate(Screen screen) {
        Animations animations = Animations.getInstance();
        return animations == null || !animations.isEnabled() || !animations.elements.has("containers") ? false : screen != null && this.isAnimatedScreen(screen);
    }

    private boolean isAnimatedScreen(Screen screen) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.world == null && minecraftClient.player == null) {
            return false;
        }

        boolean flag;
        if (!(screen instanceof LevelLoadingScreen)
            && !(screen instanceof MessageScreen)
            && !(screen instanceof ProgressScreen)
            && !(screen instanceof ReconfiguringScreen)) {
            flag = screen instanceof ConnectScreen;
        } else {
            flag = true;
        }

        boolean flag2;
        if (!(screen instanceof ClickGuiScreen)
            && !(screen instanceof MainMenuScreen)
            && !(screen instanceof SettingsScreen)
            && !(screen instanceof LanguageScreen)
            && !(screen instanceof CrosshairEditorScreen)) {
            flag2 = screen instanceof ScriptScreen;
        } else {
            flag2 = true;
        }

        return !flag && !flag2 && !(screen instanceof ChatScreen) && !(screen instanceof DeathScreen);
    }

    private float getProgress(long l) {
        return Math.min((float)(System.currentTimeMillis() - l) / 220.0F, 1.0F);
    }
}
