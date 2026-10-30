package tech.sadnes.client.feature.impl.hud;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import tech.sadnes.api.optmedia.MediaInfo;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseScrollEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.HorizontalFadeMask;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.render.texture.MipmappedTexture;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.DragSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.drag.DragComponent;
import tech.sadnes.client.ui.hud.Draggable;
import tech.sadnes.client.ui.hud.HudPanelUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.media.LyricsFetcher;
import tech.sadnes.client.util.media.OptMediaNative;

@FeatureInfo(
    name = "MediaPlayer",
    description = "feature.impl.hud.mediaplayer",
    displayName = "feature.impl.hud.mediaplayer",
    category = Category.INTERFACE,
    subcategory = Subcategory.HUD_MISC
)
public class MediaPlayer extends Feature implements Draggable {
    private static final float WIDTH = 100.0F;
    private static final float HEIGHT = 45.0F;
    private static final float PADDING_X = 5.0F;
    private static final float PADDING_Y = 5.0F;
    private static final float COVER_SIZE = 45.0F;
    private static final int MAX_COVER_SIZE = 256;
    private static final int COVER_MIP_LEVELS = 4;
    private static final float LYRIC_FONT_SIZE = 6.0F;
    private static final float SEEK_BAR_HEIGHT = 2.0F;
    private static final float BUTTON_SIZE = 13.0F;
    private static final float TITLE_FONT_SIZE = 7.0F;
    private static final float ARTIST_FONT_SIZE = 6.0F;
    private static final float TIME_FONT_SIZE = 6.0F;
    private static final float SEEK_BAR_PADDING = 6.0F;
    private static final float LYRICS_OFFSET_Y = 19.5F;
    private static final float LYRICS_HEIGHT = 13.0F;
    private static final float ACTIVE_WORD_SCALE = 0.16F;
    private static final float LYRICS_FADE_WIDTH = 8.0F;
    private static final float LYRICS_RISE = 3.5F;
    private static final float WORD_FADE_DURATION = 0.4F;
    private static final float OLD_LINE_STAGGER = 0.3F;
    private static final float NEW_LINE_DELAY = 0.25F;
    private static final float NEW_LINE_STAGGER = 0.35F;
    private static final long POLL_INTERVAL_MS = 250L;
    private static final long VOLUME_OVERLAY_MS = 950L;
    private static final ExecutorService POLL_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "OptMedia-poll");
        thread.setDaemon(true);
        return thread;
    });
    private static final Identifier COVER_TEXTURE_ID = Identifier.of(ClientInfo.MOD_ID, "dynamic/media_cover");
    private static volatile boolean nativeLoaded;
    private static volatile boolean nativeLoadAttempted;
    private final DragSetting draggable;
    private final NumberSetting size = new NumberSetting("mediaplayer.size", 100.0F, 75.0F, 115.0F, 1.0F, Unit.PERCENT);
    private final BooleanSetting lyrics = new BooleanSetting("mediaplayer.lyrics", true).setOnChange(() -> {
        this.resetLyrics();
        LyricsFetcher.clear();
    });
    private final NumberSetting lyricsOffset = new NumberSetting("mediaplayer.lyricsOffset", 0.0F, -3.0F, 3.0F, 0.1F, Unit.SECOND);
    private final Animation showAnimation = new Animation();
    private final Animation progressAnimation = new Animation();
    private final Animation controlsAnimation = new Animation();
    private final Animation[] buttonHoverAnimations = new Animation[]{new Animation(), new Animation(), new Animation()};
    private final Animation volumeOverlayAnimation = new Animation();
    private final Animation volumeAnimation = new Animation();
    private final Animation lyricTransitionAnimation = new Animation();
    private final Animation wordHighlightAnimation = new Animation();
    private final Animation lyricScrollAnimation = new Animation();
    private final StopWatch pollTimer = new StopWatch();
    private final StopWatch positionTimer = new StopWatch();
    private final StopWatch volumeChangeTimer = new StopWatch();
    private final StopWatch pollDurationTimer = new StopWatch();
    private final StopWatch slowPollWarnTimer = new StopWatch();
    private final AtomicBoolean polling = new AtomicBoolean();
    private float mouseX;
    private float mouseY;
    private final float[] buttonX = new float[3];
    private final boolean[] buttonHovered = new boolean[3];
    private float buttonY;
    private float seekBarX;
    private float seekBarY;
    private float seekBarWidth;
    private volatile boolean hovered;
    private boolean seeking;
    private long seekPositionMs;
    private long pendingSeekMs = -1L;
    private final StopWatch seekTimer = new StopWatch();
    private boolean hasMedia;
    private boolean playing;
    private String title = "";
    private String artist = "";
    private String source = "";
    private long durationMs;
    private long positionMs;
    private volatile float volume = -1.0F;
    private MediaPlayer.LyricLineLayout currentLyricLine = new MediaPlayer.LyricLineLayout();
    private MediaPlayer.LyricLineLayout previousLyricLine = new MediaPlayer.LyricLineLayout();
    private int currentLyricIndex = -1;
    private int currentWordIndex = -1;
    private int previousWordIndex = -1;
    private float previousLyricScroll;
    private MediaInfo mediaInfo;
    private int failedPolls;
    private String trackKey = "";
    private boolean coverLoaded;
    private int coverHash;
    private byte[] coverBytes;
    private boolean coverReloading;
    private boolean awaitingCover;
    private final StopWatch coverWaitTimer = new StopWatch();
    private byte[] lastAlbumArt;

    public MediaPlayer() {
        this.draggable = new DragSetting(this.getInfo().name(), 10.0F, 160.0F);
        this.draggable.setSize(100.0F, 45.0F);
        this.draggable.setDraggable(this::isEnabled);
        this.draggable.setInteractive(this::isHoveringControl);
        this.draggable.onInput((drawContext, i, i2, f) -> {
            this.mouseX = this.toLocalX(i);
            this.mouseY = this.toLocalY(i2);
            this.render(drawContext);
        });
        this.draggable.onInput(this);
        this.draggable.setRenderCondition(() -> this.isEnabled() && this.showAnimation.get() > 0.01F ? true : false);
        DragComponent.register(this.draggable);
        this.pollTimer.setElapsed(250L);
    }

    public DragSetting getDraggable() {
        return this.draggable;
    }

    @Override
    public void render(DrawContext drawContext) {
        if (!this.isEnabled()) {
            this.seeking = false;
            this.hovered = false;
        } else {
            this.schedulePoll();
            float f = this.draggable.position2.x;
            float f2 = this.draggable.position2.y;
            boolean flag = mc.currentScreen instanceof ClickGuiScreen;
            boolean flag2;
            boolean flag3;
            if (this.hasMedia && !this.title.isEmpty()) {
                flag2 = true;
                flag3 = true;
            } else {
                flag2 = false;
                flag3 = flag;
            }

            this.showAnimation.update();
            this.showAnimation.animate(flag3 ? 1.0 : 0.0, 0.2, Easings.QUART_OUT);
            float f3 = this.showAnimation.get();
            if (f3 <= 0.01F) {
                this.hovered = false;
            } else {
                this.hovered = this.isInteractiveScreen() && this.isHovered(this.mouseX, this.mouseY, f, f2, 100.0F, 45.0F);
                float f4 = this.getScale();
                this.draggable.setSize(100.0F * f4, 45.0F * f4);
                MatrixUtil.push(drawContext);
                MatrixUtil.scale(drawContext, f, f2, f4);
                this.renderBackground(drawContext, f, f2, f3);
                this.renderCover(drawContext, f, f2, f3);
                this.renderTrackInfo(drawContext, f, f2, f3, flag2);
                this.renderLyrics(drawContext, f, f2, f3);
                this.renderSeekBar(drawContext, f, f2, f3);
                this.renderControls(drawContext, f, f2, f3, flag2);
                this.renderVolumeOverlay(drawContext, f, f2, f3);
                MatrixUtil.pop(drawContext);
            }
        }
    }

    private float getScale() {
        return this.size.getValue() / 100.0F;
    }

    private float toLocalX(double d) {
        return this.draggable.position2.x + (float)(d - this.draggable.position2.x) / this.getScale();
    }

    private float toLocalY(double d) {
        return this.draggable.position2.y + (float)(d - this.draggable.position2.y) / this.getScale();
    }

    private void renderBackground(DrawContext drawContext, float f, float f2, float f3) {
        HudPanelUtil.drawAnimatedPanel(
            drawContext, f, f2, 100.0F, 45.0F, f3, Radius.smoothAll(15.0F), 12.0F, Theme.getShadowStrength(f3) * 0.5F, Easings.CUBIC_IN
        );
    }

    private void renderCover(DrawContext drawContext, float f, float f2, float f3) {
        float f4 = f + 100.0F - 45.0F;
        Radius radius = Radius.smooth(15.0F, 15.0F, 0.0F, 0.0F);
        if (this.coverLoaded) {
            GuiRenderUtil.bindTexture(COVER_TEXTURE_ID);
            if (GuiRenderUtil.getBoundTexture() != null) {
                new RenderUtil(drawContext, f4, f2, 45.0F, 45.0F).glassGradient(0.0F, 1.0F, f3, radius);
                return;
            }

            this.reloadCover();
        }

        new RenderUtil(drawContext, f4, f2, 45.0F, 45.0F)
            .rect(ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.getBackground(), Theme.getWhite(), 0.12F), f3 * 0.6F), radius)
            .icon(Icon.MUSIC, 14.4F, 14.4F, 16.2F, ColorUtil.setAlphaf(Theme.getSecondaryText(), f3 * 0.8F));
    }

    private void renderTrackInfo(DrawContext drawContext, float f, float f2, float f3, boolean flag) {
        float f4 = f + 5.0F;
        String string = flag ? this.title : Language.translate("mediaplayer.idle", "Nothing playing");
        new RenderUtil(drawContext, f4, f2 + 5.0F, 50.0F, 7.0F)
            .scrollingTextTop(HEADER_FONT, string, 0.0F, 0.0F, 50.0F, ColorUtil.setAlphaf(Theme.getTextColor(), f3), 7.0F, this.hovered);
        if (flag) {
            String string2 = !this.artist.isEmpty()
                ? this.artist
                : (!this.source.isEmpty() ? this.source : Language.translate("mediaplayer.unknownArtist", "Unknown artist"));
            new RenderUtil(drawContext, f4, f2 + 5.0F + 7.0F + 2.0F, 50.0F, 6.0F)
                .scrollingTextTop(BODY_FONT, string2, 0.0F, 0.0F, 50.0F, ColorUtil.setAlphaf(Theme.getSecondaryText(), f3), 6.0F, this.hovered);
        }
    }

    private void renderSeekBar(DrawContext drawContext, float f, float f2, float f3) {
        this.seekBarX = f + 6.0F;
        this.seekBarWidth = 88.0F;
        this.seekBarY = f2 + 45.0F - 2.0F - 5.0F;
        if (this.seeking && this.durationMs > 0L) {
            float f4 = MathHelper.clamp((this.mouseX - this.seekBarX) / Math.max(1.0F, this.seekBarWidth), 0.0F, 1.0F);
            this.seekPositionMs = (long)((float)this.durationMs * f4);
        }

        long l = this.seeking ? this.seekPositionMs : this.getCurrentPositionMs();
        if (this.durationMs > 0L) {
            l = Math.max(0L, Math.min(this.durationMs, l));
        }

        float f5 = this.durationMs > 0L ? MathHelper.clamp((float)l / (float)this.durationMs, 0.0F, 1.0F) : 0.0F;
        this.progressAnimation.update();
        if (this.seeking) {
            this.progressAnimation.forceValue(f5);
        } else {
            this.progressAnimation.animate(f5, 0.18, Easings.QUAD_OUT);
        }

        float f6 = MathHelper.clamp(this.progressAnimation.get(), 0.0F, 1.0F);
        boolean flag;
        if (this.hovered) {
            flag = this.isOverSeekBar(this.mouseX, this.mouseY);
        } else {
            flag = false;
        }

        Radius radius = Radius.all(1.0F);
        int i = ColorUtil.multiplyAlpha(
            ColorUtil.interpolate(Theme.getTextColor(), Theme.getSecondaryText(), 0.5F), f3 * (!flag && !this.seeking ? 0.14F : 0.22F)
        );
        new RenderUtil(drawContext, this.seekBarX, this.seekBarY, this.seekBarWidth, 2.0F).rect(i, radius);
        if (f6 > 0.001F) {
            new RenderUtil(drawContext, this.seekBarX, this.seekBarY, this.seekBarWidth * f6, 2.0F).rect(ColorUtil.setAlphaf(Theme.getPrimary(), f3), radius);
        }

        float f7 = this.seekBarX + this.seekBarWidth * f6 - 4.0F / 2.0F;
        float f8 = this.seekBarY + 1.0F - 4.0F / 2.0F;
        new RenderUtil(drawContext, f7, f8, 4.0F, 4.0F).rect(ColorUtil.setAlphaf(Theme.getTextColor(), f3), Radius.all(4.0F / 2.0F));
        int i2 = ColorUtil.setAlphaf(Theme.getSecondaryText(), f3 * 0.9F);
        float f9 = this.seekBarY - 6.0F - 0.5F;
        new RenderUtil(drawContext, this.seekBarX, f9, this.seekBarWidth, 6.0F).textTopLeft(BODY_FONT, formatTime(l), 0.0F, 0.0F, i2, 6.0F);
        new RenderUtil(drawContext, this.seekBarX, f9, this.seekBarWidth, 6.0F).textTopRight(BODY_FONT, formatTime(this.durationMs), 0.0F, 0.0F, i2, 6.0F);
    }

    private void renderControls(DrawContext drawContext, float f, float f2, float f3, boolean flag) {
        this.buttonY = f2 + 22.5F - 13.0F / 2.0F;

        for (int i = 0; i < 3; i++) {
            this.buttonX[i] = f + 100.0F * (i * 2 + 1) / 6.0F - 13.0F / 2.0F;
        }

        this.controlsAnimation.update();
        this.controlsAnimation.animate(this.hovered && flag && !this.isVolumeOverlayVisible() ? 1.0 : 0.0, 0.15, Easings.QUART_OUT);
        float f4 = this.controlsAnimation.get();
        if (f4 <= 0.01F) {
            for (int i2 = 0; i2 < 3; i2++) {
                this.buttonHovered[i2] = false;
            }
        } else {
            for (int i3 = 0; i3 < 3; i3++) {
                this.buttonHovered[i3] = this.hovered
                    && this.isHovered(this.mouseX, this.mouseY, this.buttonX[i3] - 2.0F, this.buttonY - 2.0F, 13.0F + 4.0F, 13.0F + 4.0F);
                this.buttonHoverAnimations[i3].update();
                this.buttonHoverAnimations[i3].animate(this.buttonHovered[i3] ? 1.0 : 0.0, 0.3, Easings.EXPO_OUT);
                this.renderControlButton(drawContext, i3, f4 * f3, this.buttonHoverAnimations[i3].get());
            }
        }
    }

    private void renderControlButton(DrawContext drawContext, int i, float f, float f2) {
        boolean flag = i == 1;
        Icon icon2 = i == 0 ? Icon.SKIP_PREVIOUS : (flag ? (this.playing ? Icon.PAUSE : Icon.PLAY) : Icon.SKIP_NEXT);
        float f3 = 13.0F * (0.82F + 0.18F * f2);
        float f4 = this.buttonX[i] + (13.0F - f3) / 2.0F;
        float f5 = this.buttonY + (13.0F - f3) / 2.0F;
        Radius.all(f3 / 2.0F);
        float f6 = f3 * 0.78F;
        float f7 = (f3 - f6) / 2.0F;
        int i2 = ColorUtil.multiplyAlpha(
            flag ? Theme.getPrimary() : ColorUtil.interpolate(Theme.getTextColor(), Theme.getSecondaryText(), 0.5F),
            f * (this.buttonHovered[i] ? 0.3F : 0.14F)
        );
        Radius radius = Radius.all(f6 / 2.0F);
        new RenderUtil(drawContext, f4 + f7, f5 + f7, f6, f6).outline(4.0F, i2, f * 0.28F, radius).rect(i2, radius);
        int i3 = flag
            ? ColorUtil.setAlphaf(Theme.getTextColor(), f)
            : ColorUtil.setAlphaf(ColorUtil.interpolate(Theme.getSecondaryText(), Theme.getTextColor(), f2), f);
        float f8 = 6.5F * (0.9F + 0.1F * f2);
        new RenderUtil(drawContext, f4 + (f3 - f8) / 2.0F, f5 + (f3 - f8) / 2.0F, f8, f8).icon(icon2, 0.0F, 0.0F, f8, i3);
    }

    private void renderLyrics(DrawContext drawContext, float f, float f2, float f3) {
        if (this.lyrics.getValue()) {
            this.updateLyrics();
            this.lyricTransitionAnimation.update();
            this.lyricTransitionAnimation.animate(1.0, 0.45, Easings.LINEAR);
            float f4 = this.lyricTransitionAnimation.get();
            boolean flag = this.previousLyricLine.wordCount() > 0 && f4 < 1.0F;
            if (this.currentLyricLine.wordCount() != 0 || flag) {
                float f5 = f3 * (1.0F - this.controlsAnimation.get());
                if (!(f5 <= 0.02F)) {
                    float f6 = f2 + 19.5F;
                    float f7 = f + 5.0F;
                    float f8 = f6 + 3.5F;
                    if (ScissorStack.push(drawContext, f7, f6, 90.0F, 13.0F)) {
                        try {
                            if (flag) {
                                applyLyricsFade(f7, 90.0F, this.previousLyricLine.getTotalWidth(), this.previousLyricScroll);
                                this.renderPreviousLyricLine(drawContext, f7 - this.previousLyricScroll, f8, f5, f4);
                            }

                            if (this.currentLyricLine.wordCount() > 0) {
                                float f9 = this.computeLyricScroll(90.0F);
                                applyLyricsFade(f7, 90.0F, this.currentLyricLine.getTotalWidth(), f9);
                                this.renderCurrentLyricLine(drawContext, f7, 90.0F, f9, f8, f5, f4);
                            }
                        } finally {
                            HorizontalFadeMask.end();
                            ScissorStack.pop(drawContext);
                        }
                    }
                }
            }
        }
    }

    private static void applyLyricsFade(float f, float f2, float f3, float f4) {
        float f5 = MathHelper.clamp(f4 / 8.0F, 0.0F, 1.0F);
        float f6 = MathHelper.clamp((f3 - f2 - f4) / 8.0F, 0.0F, 1.0F);
        HorizontalFadeMask.begin(f, f + 8.0F, f5, f + f2 - 8.0F, f + f2, f6);
    }

    private float computeLyricScroll(float f) {
        float f2 = this.currentLyricLine.getTotalWidth();
        float f3 = 0.0F;
        if (f2 > f && this.currentWordIndex >= 0) {
            float f4 = this.currentLyricLine.getWordOffset(this.currentWordIndex) + this.currentLyricLine.getWordWidth(this.currentWordIndex) / 2.0F;
            f3 = MathHelper.clamp(f4 - f * 0.4F, 0.0F, f2 - f);
        }

        this.lyricScrollAnimation.update();
        this.lyricScrollAnimation.animate(f3, 0.35, Easings.QUART_OUT);
        return this.lyricScrollAnimation.get();
    }

    private void renderPreviousLyricLine(DrawContext drawContext, float f, float f2, float f3, float f4) {
        int i = this.previousLyricLine.wordCount();

        for (int i2 = 0; i2 < i; i2++) {
            float f5 = staggeredProgress(f4, i2, i, 0.0F, 0.3F);
            float f6 = f3 * (1.0F - f5) * 0.8F;
            if (!(f6 <= 0.02F)) {
                BODY_FONT.draw(
                    drawContext,
                    this.previousLyricLine.getWord(i2),
                    f + this.previousLyricLine.getWordOffset(i2),
                    f2 - 3.5F * f5,
                    ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f6),
                    6.0F
                );
            }
        }
    }

    private void renderCurrentLyricLine(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6) {
        float f7 = this.wordHighlightAnimation.get();
        int i = ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f5 * 0.7F);
        int i2 = ColorUtil.multiplyAlpha(Theme.getTextColor(), f5);
        int i3 = this.currentLyricLine.wordCount();
        float f8 = 0.0F;

        for (int i4 = 0; i4 < i3; i4++) {
            f8 += this.getWordExtraWidth(i4, f7);
        }

        float f9 = Math.max(0.0F, f2 - (this.currentLyricLine.getTotalWidth() + f8 - f3));
        float f10 = f - f3 + MathHelper.clamp(-f8 / 2.0F, -f3, f9);
        float f11 = 0.0F;

        for (int i5 = 0; i5 < i3; i5++) {
            float f12 = this.getWordHighlight(i5, f7);
            float f13 = staggeredProgress(f6, i5, i3, 0.25F, 0.35F);
            if (f13 > 0.02F) {
                float f14 = 6.0F * (1.0F + 0.16F * f12);
                int i6 = ColorUtil.multiplyAlpha(f12 <= 0.001F ? i : ColorUtil.interpolate(i, i2, f12), f13);
                BODY_FONT.draw(
                    drawContext,
                    this.currentLyricLine.getWord(i5),
                    f10 + this.currentLyricLine.getWordOffset(i5) + f11,
                    f4 - (f14 - 6.0F) / 2.0F + 3.5F * (1.0F - f13),
                    i6,
                    f14
                );
            }

            f11 += this.currentLyricLine.getWordWidth(i5) * 0.16F * f12;
        }
    }

    private float getWordHighlight(int i, float f) {
        if (i == this.currentWordIndex) {
            return f;
        } else {
            return i == this.previousWordIndex ? 1.0F - f : 0.0F;
        }
    }

    private float getWordExtraWidth(int i, float f) {
        return this.currentLyricLine.getWordWidth(i) * 0.16F * this.getWordHighlight(i, f);
    }

    private static float staggeredProgress(float f, int i, int i2, float f2, float f3) {
        float f4 = i2 > 1 ? f3 / (i2 - 1) : 0.0F;
        float f5 = MathHelper.clamp((f - f2 - i * f4) / 0.4F, 0.0F, 1.0F);
        return (float)Easings.QUART_OUT.ease(f5);
    }

    private void updateLyrics() {
        List list = LyricsFetcher.getLines();
        int i = list.size();
        long l = this.getCurrentPositionMs() + (long)(this.lyricsOffset.getValue() * 1000.0F);
        int i2 = i == 0 ? -1 : LyricsFetcher.getLineIndex(l);
        if (i2 != this.currentLyricIndex) {
            this.currentLyricIndex = i2;
            MediaPlayer.LyricLineLayout lyricLineLayout = this.previousLyricLine;
            this.previousLyricLine = this.currentLyricLine;
            this.currentLyricLine = lyricLineLayout;
            this.currentLyricLine.layout(BODY_FONT, i2 >= 0 ? ((LyricsFetcher.LyricLine)list.get(i2)).text() : "");
            this.previousLyricScroll = this.lyricScrollAnimation.get();
            this.lyricScrollAnimation.forceValue(0.0);
            this.lyricTransitionAnimation.forceValue(0.0);
            this.currentWordIndex = -1;
            this.previousWordIndex = -1;
        }

        if (i2 >= 0 && this.currentLyricLine.wordCount() != 0) {
            long l2 = ((LyricsFetcher.LyricLine)list.get(i2)).timeMs();
            long l3 = i2 + 1 < i ? ((LyricsFetcher.LyricLine)list.get(i2 + 1)).timeMs() : l2 + 5000L;
            int i3 = this.currentLyricLine.wordIndexAt(MathHelper.clamp((float)(l - l2) / (float)Math.max(1L, l3 - l2), 0.0F, 1.0F));
            if (i3 != this.currentWordIndex) {
                this.previousWordIndex = this.currentWordIndex;
                this.currentWordIndex = i3;
                this.wordHighlightAnimation.forceValue(0.0);
            }

            this.wordHighlightAnimation.update();
            this.wordHighlightAnimation.animate(1.0, 0.18, Easings.QUART_OUT);
        }
    }

    private void renderVolumeOverlay(DrawContext drawContext, float f, float f2, float f3) {
        this.volumeOverlayAnimation.update();
        this.volumeOverlayAnimation.animate(this.isVolumeOverlayVisible() ? 1.0 : 0.0, 0.18, Easings.QUART_OUT);
        float f4 = this.volumeOverlayAnimation.get();
        if (!(f4 <= 0.01F)) {
            this.volumeAnimation.update();
            this.volumeAnimation.animate(MathHelper.clamp(this.volume, 0.0F, 1.0F), 0.18, Easings.QUAD_OUT);
            float f5 = MathHelper.clamp(this.volumeAnimation.get(), 0.0F, 1.0F);
            float f6 = f4 * f3;
            float f7 = f + (100.0F - 38.0F) / 2.0F;
            float f8 = f2 + (45.0F - 15.0F) / 2.0F;
            Radius radius = Radius.all(5.0F);
            new RenderUtil(drawContext, f, f2, 100.0F, 45.0F).blur(f6, Radius.all(5.0F), RenderUtil.BlurQuality.LOW, true);
            new RenderUtil(drawContext, f, f2, 100.0F, 45.0F).rect(ColorUtil.setAlphaf(Theme.getBackground(), f6 * 0.25F), Radius.all(5.0F));
            new RenderUtil(drawContext, f7, f8, 38.0F, 15.0F)
                .outline(7.0F, Theme.getBase(), f6 * 0.5F, radius)
                .rect(ColorUtil.setAlphaf(Theme.getSurface(), f6), radius);
            Icon icon2 = f5 <= 0.01F ? Icon.VOLUME_MUTE : (f5 < 0.5F ? Icon.VOLUME_LOW : Icon.VOLUME_HIGH);
            int i = ColorUtil.setAlphaf(Theme.getTextColor(), f6);
            new RenderUtil(drawContext, f7 + 6.0F, f8 + (15.0F - 8.5F) / 2.0F, 8.5F, 8.5F).icon(icon2, 0.0F, 0.0F, 8.5F, i);
            new RenderUtil(drawContext, f7 + 17.0F, f8, 38.0F - 21.0F, 15.0F).textMiddleLeft(BODY_FONT, Math.round(f5 * 100.0F) + "%", 0.0F, 0.0F, i, 6.0F);
        }
    }

    private boolean isVolumeOverlayVisible() {
        return !this.volumeChangeTimer.hasPassed(950.0) && this.volume >= 0.0F;
    }

    private long getCurrentPositionMs() {
        return this.hasMedia && this.playing && this.durationMs > 0L
            ? Math.min(this.durationMs, this.positionMs + this.positionTimer.getElapsed())
            : this.positionMs;
    }

    @EventHandler
    public void onMouseClick(ScreenKeyEvent screenKeyEvent) {
        if (this.isEnabled() && this.isInteractiveScreen() && screenKeyEvent.isKey(0) && !(this.showAnimation.get() <= 0.01F)) {
            float f = this.toLocalX(screenKeyEvent.getMouseX());
            float f2 = this.toLocalY(screenKeyEvent.getMouseY());
            if (screenKeyEvent.isPress()) {
                if (this.durationMs > 0L && this.isOverSeekBar(f, f2)) {
                    this.seeking = true;
                    this.seekPositionMs = (long)(
                        (float)this.durationMs * MathHelper.clamp((f - this.seekBarX) / Math.max(1.0F, this.seekBarWidth), 0.0F, 1.0F)
                    );
                    return;
                }

                if (this.hasMedia && this.controlsAnimation.get() > 0.35F) {
                    for (int i = 0; i < 3; i++) {
                        if (this.isHovered(f, f2, this.buttonX[i] - 2.0F, this.buttonY - 2.0F, 17.0F, 17.0F)) {
                            this.pressControlButton(i);
                            return;
                        }
                    }
                }
            } else if (screenKeyEvent.isRelease() && this.seeking) {
                this.seeking = false;
                long l = this.seekPositionMs;
                if (nativeLoaded && this.durationMs > 0L) {
                    POLL_EXECUTOR.submit(() -> {
                        try {
                            OptMediaNative.library.SeekToMs(l);
                        } catch (Throwable throwable) {
                        }
                    });
                    this.positionMs = l;
                    this.positionTimer.reset();
                    this.pendingSeekMs = l;
                    this.seekTimer.reset();
                    this.progressAnimation.forceValue(MathHelper.clamp((float)l / (float)this.durationMs, 0.0F, 1.0F));
                    this.pollTimer.setElapsed(250L);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onMouseScroll(ScreenMouseScrollEvent screenMouseScrollEvent) {
        if (this.isEnabled() && nativeLoaded && !(this.showAnimation.get() <= 0.01F)) {
            if (this.isHovered(
                this.toLocalX(screenMouseScrollEvent.getMouseX()), this.toLocalY(screenMouseScrollEvent.getMouseY()), this.draggable.position2.x, this.draggable.position2.y, 100.0F, 45.0F
            )) {
                float f = screenMouseScrollEvent.getVerticalAmount() > 0.0 ? 0.05F : -0.05F;
                POLL_EXECUTOR.submit(() -> {
                    try {
                        float f2 = this.volume >= 0.0F ? this.volume : OptMediaNative.library.GetVolume();
                        if (f2 < 0.0F) {
                            return;
                        }

                        float f3 = MathHelper.clamp(f2 + f, 0.0F, 1.0F);
                        if (OptMediaNative.library.SetVolume(f3)) {
                            this.volume = f3;
                        }
                    } catch (Throwable throwable) {
                    }
                });
                this.volumeChangeTimer.reset();
                screenMouseScrollEvent.cancel();
            }
        }
    }

    private void pressControlButton(int i) {
        if (nativeLoaded) {
            POLL_EXECUTOR.submit(() -> {
                try {
                    if (i == 0) {
                        OptMediaNative.library.SkipPrevious();
                    } else if (i == 1) {
                        OptMediaNative.library.TogglePlayPause();
                    } else {
                        OptMediaNative.library.SkipNext();
                    }
                } catch (Throwable throwable) {
                }
            });
            this.pollTimer.setElapsed(250L);
        }
    }

    private boolean isInteractiveScreen() {
        return mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof ClickGuiScreen;
    }

    private boolean isHoveringControl() {
        if (this.isOverSeekBar(this.mouseX, this.mouseY)) {
            return true;
        }

        if (this.controlsAnimation.get() > 0.35F) {
            for (int i = 0; i < 3; i++) {
                if (this.isHovered(this.mouseX, this.mouseY, this.buttonX[i] - 2.0F, this.buttonY - 2.0F, 17.0F, 17.0F)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isOverSeekBar(double d, double d2) {
        return d >= this.seekBarX - 3.0F
            && d <= this.seekBarX + this.seekBarWidth + 3.0F
            && d2 >= this.seekBarY - 2.0F
            && d2 <= this.seekBarY + 2.0F + 3.0F;
    }

    private boolean isHovered(double d, double d2, float f, float f2, float f3, float f4) {
        return !this.isInteractiveScreen() ? false : d >= f && d <= f + f3 && d2 >= f2 && d2 <= f2 + f4;
    }

    private void schedulePoll() {
        if (!nativeLoadAttempted && this.pollTimer.hasPassed(250.0)) {
            if (this.polling.compareAndSet(false, true)) {
                this.pollTimer.reset();
                POLL_EXECUTOR.submit(() -> {
                    try {
                        this.pollMedia();
                    } finally {
                        this.polling.set(false);
                    }
                });
            }
        }
    }

    private void pollMedia() {
        if (!this.ensureNativeLoaded()) {
            return;
        }

        boolean received = false;

        try {
            this.pollDurationTimer.reset();
            received = OptMediaNative.library.GetCurrentMediaInfo(this.mediaInfo);
            long infoElapsed = this.pollDurationTimer.getElapsed();
            if (received) {
                this.failedPolls = 0;
                byte[] albumArt = this.mediaInfo.getAlbumArt();
                if (albumArt != null && albumArt.length == 0) {
                    albumArt = null;
                }

                String title = trimOrDefault(this.mediaInfo.getTitle(), "");
                if (title.isEmpty()) {
                    if (albumArt != null) {
                        this.lastAlbumArt = albumArt;
                    }
                    return;
                }

                if (albumArt == null) {
                    albumArt = this.lastAlbumArt;
                }

                this.lastAlbumArt = null;
                String artist = trimOrDefault(this.mediaInfo.getArtist(), "");
                String source = trimOrDefault(this.mediaInfo.getSource(), "");
                boolean playing = this.mediaInfo.isPlaying();
                long duration = Math.max(0L, this.mediaInfo.durationMs);
                long position = duration > 0L ? Math.max(0L, Math.min(duration, this.mediaInfo.positionMs)) : Math.max(0L, this.mediaInfo.positionMs);
                String key = title + "|" + artist;
                NativeImage[] cover = null;
                int coverHash = 0;
                byte[] coverBytes = albumArt;
                if (albumArt != null) {
                    coverHash = Arrays.hashCode(albumArt);
                    cover = decodeCover(albumArt);
                    if (cover == null) {
                        coverBytes = null;
                        Sadnes.getLogger().warn("[MediaPlayer] обложка не разобралась: {} байт", albumArt.length);
                    }
                }

                boolean shouldQueryVolume = this.volume < 0.0F || this.hovered && this.volumeChangeTimer.hasPassed(1000.0);
                this.pollDurationTimer.reset();
                float volume = shouldQueryVolume ? queryVolume() : Float.NaN;
                long volumeElapsed = this.pollDurationTimer.getElapsed();
                if (infoElapsed + volumeElapsed >= 500L && this.slowPollWarnTimer.hasPassedAndReset(5000.0)) {
                    Sadnes.getLogger().warn("[MediaPlayer] медленный опрос OptMedia: инфо {} мс, громкость {} мс", infoElapsed, volumeElapsed);
                }

                NativeImage[] finalCover = cover;
                int finalCoverHash = coverHash;
                byte[] finalCoverBytes = coverBytes;
                mc.execute(() -> this.applyMediaInfo(title, artist, source, playing, duration, position, key, finalCover, finalCoverBytes, finalCoverHash, volume));
            } else if (++this.failedPolls >= 4) {
                mc.execute(this::clearMediaState);
            }
        } catch (Throwable throwable) {
            if (++this.failedPolls >= 4) {
                mc.execute(this::clearMediaState);
            }
        } finally {
            if (received) {
                try {
                    OptMediaNative.library.FreeMediaInfo(this.mediaInfo);
                    this.mediaInfo.albumArtPng = null;
                    this.mediaInfo.albumArtSize = 0;
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private void applyMediaInfo(
        String string, String string2, String string3, boolean flag, long l, long l2, String string4, NativeImage[] nativeImages, byte[] bytes, int i, float f
    ) {
        boolean flag2;
        label67: {
            this.hasMedia = true;
            this.title = string;
            this.artist = string2;
            this.source = string3;
            this.playing = flag;
            this.durationMs = l;
            flag2 = !string4.equals(this.trackKey);
            long l3 = l2;
            if (flag2) {
                this.trackKey = string4;
                this.pendingSeekMs = -1L;
                this.progressAnimation.forceValue(l > 0L ? MathHelper.clamp((float)l2 / (float)l, 0.0F, 1.0F) : 0.0);
                this.resetLyrics();
                LyricsFetcher.clear();
            } else {
                if (this.seeking) {
                    break label67;
                }

                if (l > 0L && Math.abs(l2 - this.getCurrentPositionMs()) <= 1500L) {
                    l3 = this.getCurrentPositionMs();
                    this.pendingSeekMs = -1L;
                } else {
                    if (this.pendingSeekMs >= 0L && !this.seekTimer.hasPassed(3000.0)) {
                        break label67;
                    }

                    this.pendingSeekMs = -1L;
                }
            }

            this.positionMs = l3;
            this.positionTimer.reset();
        }

        if (!Float.isNaN(f) && f >= 0.0F) {
            this.volume = f;
        }

        if (this.lyrics.getValue()) {
            LyricsFetcher.request(string, string2, l);
        }

        if (nativeImages != null) {
            this.awaitingCover = false;
            if (i == this.coverHash && this.coverLoaded) {
                closeImages(nativeImages);
            } else {
                this.uploadCover(nativeImages, bytes, i);
            }
        } else if (flag2) {
            this.awaitingCover = true;
            this.coverWaitTimer.reset();
        } else {
            if (this.awaitingCover && this.coverWaitTimer.hasPassed(3000.0)) {
                Sadnes.getLogger().debug("[MediaPlayer] трек '{}' без миниатюры от SMTC", string4);
                this.awaitingCover = false;
                this.releaseCover();
            }
        }
    }

    private void clearMediaState() {
        this.hasMedia = false;
        this.playing = false;
        this.title = "";
        this.artist = "";
        this.source = "";
        this.durationMs = 0L;
        this.positionMs = 0L;
        this.trackKey = "";
        this.volume = -1.0F;
        this.seeking = false;
        this.pendingSeekMs = -1L;
        this.progressAnimation.forceValue(0.0);
        this.resetLyrics();
        LyricsFetcher.clear();
        this.awaitingCover = false;
        this.releaseCover();
    }

    private void resetLyrics() {
        this.currentLyricIndex = -1;
        this.currentWordIndex = -1;
        this.previousWordIndex = -1;
        this.previousLyricScroll = 0.0F;
        this.currentLyricLine.clear();
        this.previousLyricLine.clear();
        this.lyricScrollAnimation.forceValue(0.0);
        this.lyricTransitionAnimation.forceValue(1.0);
    }

    @Override
    public void onEnable() {
        this.seeking = false;
        this.hovered = false;
        this.showAnimation.forceValue(0.0);
        mc.execute(this::clearMediaState);
        if (nativeLoaded) {
            POLL_EXECUTOR.submit(() -> {
                try {
                    if (this.mediaInfo != null) {
                        OptMediaNative.library.FreeMediaInfo(this.mediaInfo);
                    }

                    OptMediaNative.library.Deinitialize();
                } catch (Throwable throwable) {
                }
            });
        }
    }

    private void uploadCover(NativeImage[] nativeImages, byte[] bytes, int i) {
        try {
            MipmappedTexture mipmappedTexture = new MipmappedTexture(() -> ClientInfo.MOD_ID + "_media_cover", nativeImages);
            TextureManager textureManager = mc.getTextureManager();
            textureManager.destroyTexture(COVER_TEXTURE_ID);
            textureManager.registerTexture(COVER_TEXTURE_ID, mipmappedTexture);
            this.coverHash = i;
            this.coverLoaded = true;
            this.coverBytes = bytes;
            Sadnes.getLogger().debug("[MediaPlayer] обложка залита: {}x{}, уровней {}", nativeImages[0].getWidth(), nativeImages[0].getHeight(), nativeImages.length);
        } catch (Throwable throwable) {
            Sadnes.getLogger().warn("[MediaPlayer] заливка обложки не удалась: {}", throwable.toString());
            this.releaseCover();
        } finally {
            closeImages(nativeImages);
            this.coverReloading = false;
        }
    }

    private void reloadCover() {
        this.coverLoaded = false;
        this.coverHash = 0;
        byte[] bytes = this.coverBytes;
        if (bytes != null && !this.coverReloading) {
            this.coverReloading = true;
            int i = Arrays.hashCode(bytes);
            POLL_EXECUTOR.submit(() -> {
                NativeImage[] nativeImages = decodeCover(bytes);
                mc.execute(() -> {
                    if (nativeImages == null) {
                        this.coverReloading = false;
                    } else if (!this.coverReloading) {
                        closeImages(nativeImages);
                    } else {
                        this.uploadCover(nativeImages, bytes, i);
                    }
                });
            });
        }
    }

    private void releaseCover() {
        this.coverBytes = null;
        this.coverReloading = false;
        if (this.coverLoaded) {
            mc.getTextureManager().destroyTexture(COVER_TEXTURE_ID);
            this.coverLoaded = false;
            this.coverHash = 0;
        }
    }

    private static void closeImages(NativeImage[] nativeImages) {
        for (NativeImage nativeImage : nativeImages) {
            if (nativeImage != null) {
                nativeImage.close();
            }
        }
    }

    private boolean ensureNativeLoaded() {
        if (nativeLoaded) {
            return true;
        }

        if (nativeLoadAttempted) {
            return false;
        }

        try {
            OptMediaNative.load();
            MediaInfo info = this.mediaInfo;
            this.mediaInfo = info != null ? info : new MediaInfo();
            nativeLoaded = true;
            return true;
        } catch (Throwable throwable) {
            nativeLoadAttempted = true;
            Sadnes.getLogger().warn("[MediaPlayer] РЅРµ СѓРґР°Р»РѕСЃСЊ Р·Р°РіСЂСѓР·РёС‚СЊ OptMedia: {}", throwable.toString());
            return false;
        }
    }

    private static NativeImage[] decodeCover(byte[] data) {
        NativeImage source = null;
        NativeImage square = null;

        try {
            source = NativeImage.read(new ByteArrayInputStream(data));
            int width = source.getWidth();
            int height = source.getHeight();
            int side = Math.min(width, height);
            int size = Math.min(side, 256);
            if (width == height && size == side) {
                square = source;
                source = null;
            } else {
                square = new NativeImage(source.getFormat(), size, size, false);
                source.resizeSubRectTo((width - side) / 2, (height - side) / 2, side, side, square);
            }

            NativeImage[] mipmaps = MipmappedTexture.generateMipmaps(square, 4);
            square = null;
            return mipmaps;
        } catch (Throwable throwable) {
            return null;
        } finally {
            if (source != null) {
                source.close();
            }

            if (square != null) {
                square.close();
            }
        }
    }

    private static float queryVolume() {
        try {
            return OptMediaNative.library.GetVolume();
        } catch (Throwable throwable) {
            return -1.0F;
        }
    }

    private static String trimOrDefault(String string, String string2) {
        if (string == null) {
            return string2;
        }

        String string3 = string.trim();
        return string3.isEmpty() ? string2 : string3;
    }

    private static String formatTime(long l) {
        if (l <= 0L) {
            return "0:00";
        }

        long l2 = l / 1000L;
        long l3 = l2 / 60L;
        long l4 = l2 % 60L;
        return l3 + ":" + (l4 < 10L ? "0" : "") + l4;
    }

    public static MediaPlayer getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(MediaPlayer.class);
    }

    static final class LyricLineLayout {
        private String[] words = new String[0];
        private float[] wordOffsets = new float[0];
        private float[] wordWidths = new float[0];
        private int[] wordEndChars = new int[0];
        private float totalWidth;
        private int charCount = 1;

        void layout(MsdfFont msdfFont, String string) {
            int i = string.length();
            int i2 = 0;

            for (int i3 = 0; i3 < i; i3++) {
                if (string.charAt(i3) != ' ' && (i3 == 0 || string.charAt(i3 - 1) == ' ')) {
                    i2++;
                }
            }

            this.words = new String[i2];
            this.wordOffsets = new float[i2];
            this.wordWidths = new float[i2];
            this.wordEndChars = new int[i2];
            this.charCount = Math.max(1, i);
            this.totalWidth = msdfFont.getWidth(string, 6.0F);
            int i4 = 0;
            int i5 = -1;

            for (int i6 = 0; i6 <= i; i6++) {
                boolean flag;
                if (i6 != i && string.charAt(i6) != ' ') {
                    flag = false;
                    if (i5 < 0) {
                        i5 = i6;
                    }
                } else {
                    flag = true;
                }

                if (flag && i5 >= 0) {
                    this.words[i4] = string.substring(i5, i6);
                    this.wordOffsets[i4] = msdfFont.getWidth(string.substring(0, i5), 6.0F);
                    this.wordWidths[i4] = msdfFont.getWidth(this.words[i4], 6.0F);
                    this.wordEndChars[i4] = i6;
                    i4++;
                    i5 = -1;
                }
            }
        }

        void clear() {
            this.words = new String[0];
            this.wordOffsets = new float[0];
            this.wordWidths = new float[0];
            this.wordEndChars = new int[0];
            this.totalWidth = 0.0F;
            this.charCount = 1;
        }

        int wordCount() {
            return this.words.length;
        }

        String getWord(int i) {
            return this.words[i];
        }

        float getWordOffset(int i) {
            return this.wordOffsets[i];
        }

        float getWordWidth(int i) {
            return this.wordWidths[i];
        }

        float getTotalWidth() {
            return this.totalWidth;
        }

        int wordIndexAt(float f) {
            int i = this.words.length;
            if (i == 0) {
                return -1;
            }

            float f2 = f * this.charCount;

            for (int i2 = 0; i2 < i; i2++) {
                if (f2 < this.wordEndChars[i2]) {
                    return i2;
                }
            }

            return i - 1;
        }
    }
}
