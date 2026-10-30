package tech.sadnes.client.ui.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.IconHoverAnimator;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.render.font.ShimmerText;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.component.SmoothScrollbar;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

public final class ChangelogPanel implements IRenderable {
    private static final int ADDED_COLOR = -9449606;
    private static final String UPDATE_DATE = "26.09.2026, 05:33";
    private static final float MAX_PADDING = 13.0F;
    private static final float TITLE_HEIGHT = 10.0F;
    private static final float HEADER_FONT_SIZE = 6.5F;
    private static final float BODY_FONT_SIZE = 5.8F;
    private static final float LINE_HEIGHT = 7.5F;
    private static final float PANEL_Y = 12.0F;
    private static final float TOGGLE_Y = 34.0F;
    private static final float TOGGLE_HEIGHT = 36.0F;
    private static final float TOGGLE_WIDTH = 14.0F;
    private static final float TOGGLE_HOVER_EXPAND = 6.0F;
    private static final float TOGGLE_RADIUS = 7.0F;
    private static final float TOGGLE_HITBOX_Y = 30.0F;
    private static final float TOGGLE_HITBOX_HEIGHT = 44.0F;
    private static final float ENTRY_SPACING = 10.0F;
    private static final List<ChangelogPanel.Entry> ENTRIES = List.of(
        new ChangelogPanel.Entry(
            ChangelogPanel.ChangeKind.ADDED,
            "AutoGamble",
            "changelog.autogamble",
            "Новый модуль: сам ищет и добывает якоря возрождения, продаёт их и переходит на новое место, если долго ничего не находит, при низком здоровье или когда рядом игрок. Умеет сам есть, перезаходить на сервер после кика, выкидывать мусор и подсвечивать найденные якоря."
        ),
        new ChangelogPanel.Entry(
            ChangelogPanel.ChangeKind.ADDED,
            "AttackAura",
            "changelog.attackaura.legit",
            "Новый режим наводки «Легит»: камера сама плавно доводится до цели, как рука с мышкой, и отпускает её, как только прицел на цели."
        ),
        new ChangelogPanel.Entry(
            ChangelogPanel.ChangeKind.ADDED,
            "Target ESP",
            "changelog.targetesp",
            "Новый стиль «Разряд» вместо «Пылающего черепа»: цель окутывает электрическая оболочка, по телу бегает волна заряда, а удар и убийство дают белую вспышку."
        ),
        new ChangelogPanel.Entry(
            ChangelogPanel.ChangeKind.ADDED,
            "TargetHud",
            "changelog.targethud",
            "Новый стиль «Кольцо»: компактная карточка с аватаром, ником и экипировкой, справа кольцо здоровья с числом — сжимается от урона, краснеет на низком хп, а при ударе из него летят искры. Когда цель ест или пьёт, у её головы появляется этот предмет и «жуёт»."
        ),
        new ChangelogPanel.Entry(
            ChangelogPanel.ChangeKind.CHANGED,
            "AttackAura",
            "changelog.attackaura.swap",
            "Свап сферы или талисмана на удар на FunTime переписан: предмет, который сильнее всего прибавляет к урону, ложится в левую руку прямо перед ударом и сразу возвращается. В руке его не видно, а тотем от AutoTotem больше не выдёргивается."
        ),
        new ChangelogPanel.Entry(
            ChangelogPanel.ChangeKind.CHANGED,
            "TargetPearl",
            "changelog.targetpearl",
            "Точнее рассчитывает бросок. Новое: «Учитывать пинг» (жемчуг не врежется в цель раньше её телепорта) и «Задержка» перед броском. За каждым жемчугом кидает только один раз, а «Только за таргетом» учитывает и цель AutoCrystal."
        ),
        new ChangelogPanel.Entry(
            ChangelogPanel.ChangeKind.ADDED,
            "FarmBase",
            "changelog.farmbase",
            "«Хранилища»: подсветка сундуков, бочек, шалкеров, эндер-сундуков, печей и воронок, в том числе там, где сервер прячет часть блоков. «Точки спавна» — крестики, где может появиться моб: видно, откуда полезут и где ферма их не ловит."
        ),
        new ChangelogPanel.Entry(
            ChangelogPanel.ChangeKind.CHANGED,
            "Waypoints",
            "changelog.waypoints",
            "Полностью переработаны: аккуратная плашка с иконкой ивента, названием и дистанцией. У ивентов кольцо показывает, сколько осталось, метки больше не налезают друг на друга, а для ивента на другой анархии сразу видна команда перехода."
        ),
        new ChangelogPanel.Entry(
            ChangelogPanel.ChangeKind.ADDED,
            "Removals",
            "changelog.removals",
            "Скрытие скалка, скалк-сенсоров и их частиц: заметно поднимает FPS там, где их много."
        ),
        new ChangelogPanel.Entry(
            ChangelogPanel.ChangeKind.ADDED,
            "Ambience",
            "changelog.ambience.sky",
            "Новое небо «Звездопад», «Звёздное поле» стало «Лунной ночью». «Вид облаков» — объёмные или обычные ванильные в цвете из настроек."
        ),
        new ChangelogPanel.Entry(
            ChangelogPanel.ChangeKind.CHANGED,
            "Ambience",
            "changelog.ambience.reflections",
            "Отражения больше не дают полос под кронами деревьев и никами, а их сила и дальность подбираются сами."
        )
    );
    private final SmoothScrollbar scroll = new SmoothScrollbar();
    private final Animation openAnimation = new Animation();
    private final ShimmerText titleTextEffect = new ShimmerText();
    private final Animation toggleHoverAnimation = new Animation();
    private final Animation togglePressAnimation = new Animation();
    private final IconHoverAnimator listIcon = new IconHoverAnimator();
    private final IconHoverAnimator chevronIcon = new IconHoverAnimator();
    private final List<List<String>> wrappedLines = new ArrayList<>();
    private float wrapWidth = -1.0F;
    private String wrapLocale;
    private boolean open;
    private long openTime;
    private float panelX;
    private float panelY;
    private float panelWidth;
    private float panelHeight;
    private float padding;
    private float contentY;
    private float contentHeight;
    private float contentTotalHeight;
    private float toggleX;
    private float toggleWidth;
    private float toggleAlpha;

    public boolean isOpen() {
        return this.open;
    }

    public void open() {
        if (!this.open) {
            this.open = true;
            long l = System.currentTimeMillis();
            if (this.openAnimation.get() > 0.05F) {
                this.openTime = l - 1000L;
                this.openAnimation.animate(1.0, 0.38, Easings.QUART_OUT);
            } else {
                this.openTime = l;
                this.scroll.reset();
                this.openAnimation.animate(1.0, 0.38, Easings.QUART_OUT);
            }
        }
    }

    public void close() {
        if (this.open) {
            this.open = false;
            this.openAnimation.animate(0.0, 0.24, Easings.CUBIC_OUT);
        }
    }

    public float[] getToggleBounds() {
        return this.toggleAlpha > 0.004F ? new float[]{this.toggleX, 30.0F, this.toggleWidth, 44.0F} : null;
    }

    public float[] getToggleIconBounds() {
        return this.toggleAlpha > 0.004F ? new float[]{this.toggleX, 30.0F, 24.0F, 44.0F} : null;
    }

    private boolean isToggleHovered(double d, double d2) {
        return this.toggleAlpha > 0.004F && this.isHovered(d, d2, this.toggleX, 30.0, this.toggleWidth, 44.0);
    }

    public boolean isVisible() {
        return this.open || this.openAnimation.get() > 0.01F;
    }

    public float getOpenProgress() {
        return this.openAnimation.get();
    }

    public void render(DrawContext drawContext, int i, int i2, float f, float f2) {
        this.openAnimation.update();
        float f3 = this.openAnimation.get();
        float f4 = GuiScale.getScaledWidth();
        float f5 = GuiScale.getScaledHeight();
        this.panelWidth = Math.min(MathHelper.clamp(f4 * 0.22F, 210.0F, 260.0F), f4 - 48.0F);
        this.panelY = 12.0F;
        this.padding = Math.min(13.0F, this.panelWidth * 0.055F);
        this.updateWrappedLines(this.panelWidth - this.padding * 2.0F - 4.0F);
        this.contentTotalHeight = this.computeContentHeight();
        this.contentY = this.panelY + 38.0F;
        float f6 = Math.min(f5 - 24.0F, Math.max(f5 * 0.72F, 200.0F));
        this.panelHeight = MathHelper.clamp(38.0F + this.contentTotalHeight + this.padding, 90.0F, f6);
        this.contentHeight = Math.max(0.0F, this.panelY + this.panelHeight - this.padding - this.contentY);
        this.panelX = MathHelper.lerp(f3, -this.panelWidth - 12.0F, 12.0F);
        RenderUtil.BlurQuality blurQuality = GraphicsQuality.getCurrent().isEffectsEnabled() ? RenderUtil.BlurQuality.MAX : RenderUtil.BlurQuality.MIDDLE;
        if (this.open || f3 > 0.01F) {
            this.renderPanel(drawContext, i, i2, f3, f2, blurQuality);
        }

        this.renderToggleButton(drawContext, i, i2, f3, f, blurQuality);
    }

    private void renderPanel(DrawContext drawContext, int i, int i2, float f, float f2, RenderUtil.BlurQuality blurQuality) {
        float f3 = smoothstep(0.0F, 0.25F, f) * f2;
        float f4 = this.open ? 1.0F : smoothstep(0.3F, 0.8F, f);
        float f5 = (float)(System.currentTimeMillis() - this.openTime) / 1000.0F;
        Radius radius = Radius.all(14.0F);
        new RenderUtil(
                drawContext,
                MathUtil.roundToStep(this.panelX, 0.5F),
                MathUtil.roundToStep(this.panelY, 0.5F),
                MathUtil.roundToStep(this.panelWidth, 0.5F),
                MathUtil.roundToStep(this.panelHeight, 0.5F)
            )
            .blurGlowBorder(0.95F * f3, Theme.getBase(), 0.68F, Theme.getNearBackground(), 28.0F, 0.0F, Easings.QUAD_IN, radius, blurQuality, true);
        new RenderUtil(drawContext, this.panelX, this.panelY, this.panelWidth, this.panelHeight)
            .rect(Theme.withAlpha(ColorUtil.interpolate(ColorUtil.BLACK, ColorUtil.WHITE, 0.04F), 0.55F * f3), radius);
        float f6 = getStaggerProgress(f5, 0.02F);
        float f7 = f3 * f4 * f6;
        float f8 = this.panelX + this.padding - 8.0F * (1.0F - f6);
        String string = Language.translate("changelog.title", "Что нового");
        int i3 = Theme.withAlpha(Theme.getTextColor(), f7);
        float f9 = this.panelWidth - this.padding * 2.0F;
        float f10 = fitFontSize(Fonts.INTER_SEMIBOLD, string, f9, 8.0F, 6.5F);
        float f11 = Fonts.INTER_SEMIBOLD.getWidth(string, f10);
        new RenderUtil(drawContext, f8, this.panelY + 12.0F, this.panelWidth, 10.0F)
            .textTopLeft(
                Fonts.INTER_SEMIBOLD,
                this.titleTextEffect.applyThemed(Fonts.INTER_SEMIBOLD, string, f10, Theme.getPrimary(), i3, 1.0F),
                0.0F,
                0.0F,
                i3,
                f10
            );
        if (f11 + 6.0F + Fonts.INTER_REGULAR.getWidth("1.21.11", 5.8F) <= f9) {
            new RenderUtil(drawContext, f8 + f11 + 6.0F, this.panelY + 14.0F, this.panelWidth, 8.0F)
                .textTopLeft(Fonts.INTER_REGULAR, "1.21.11", 0.0F, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), 0.7F * f7), 5.8F);
        }

        String string2 = String.format(Language.translate("changelog.updated", "Последнее обновление вышло %s"), "26.09.2026, 05:33");
        float f12 = this.panelWidth - this.padding * 2.0F;
        float f13 = fitFontSize(Fonts.INTER_REGULAR, string2, f12, 5.5F, 4.8F);
        new RenderUtil(drawContext, f8, this.panelY + 23.0F, this.panelWidth, 7.0F)
            .textTopLeft(
                Fonts.INTER_REGULAR,
                truncateToWidth(Fonts.INTER_REGULAR, string2, f12, f13),
                0.0F,
                0.0F,
                Theme.withAlpha(Theme.getSecondaryText(), 0.55F * f7),
                f13
            );
        this.scroll.update(this.contentTotalHeight, this.contentHeight);
        this.scroll.updateHover(i, i2);
        float f14 = f3 * f4;
        float f15 = Math.max(this.panelX, 0.0F);
        if (this.panelX + this.panelWidth > f15) {
            new RenderUtil(drawContext, f15, this.contentY, this.panelX + this.panelWidth - f15, this.contentHeight)
                .scissor(() -> this.renderEntries(drawContext, f5, f14));
        }

        this.scroll.render(drawContext, this.panelX + this.panelWidth - this.padding + 4.0F, this.contentY, this.contentHeight, this.contentTotalHeight, f14);
    }

    private void renderEntries(DrawContext drawContext, float f, float f2) {
        float f3 = -this.scroll.getScroll();
        float f4 = MathHelper.clamp(f3 / 10.0F, 0.0F, 1.0F);
        float f5 = MathHelper.clamp((this.contentTotalHeight - this.contentHeight - f3) / 10.0F, 0.0F, 1.0F);
        float f6 = this.contentY + this.contentHeight;
        float f7 = this.contentY - f3;
        int i = ENTRIES.size();

        for (int i2 = 0; i2 < i; i2++) {
            if (f7 > f6) {
                return;
            }

            List<String> strings = this.wrappedLines.get(i2);
            float f8 = 9.5F + strings.size() * 7.5F;
            if (f7 + f8 >= this.contentY) {
                ChangelogPanel.Entry entry = ENTRIES.get(i2);
                float f9 = getStaggerProgress(f, 0.05F + Math.min(i2, 8) * 0.02F);
                float f10 = this.panelX + this.padding - 12.0F * (1.0F - f9);
                float f11 = f2 * f9;
                boolean flag;
                int i3;
                if (entry.kind() == ChangelogPanel.ChangeKind.ADDED) {
                    flag = true;
                    i3 = -9449606;
                } else {
                    flag = false;
                    i3 = Theme.getPrimary();
                }

                int i4 = Theme.withAlpha(i3, f11 * this.getEdgeFade(f7, 6.5F, f4, f5));
                new RenderUtil(drawContext, f10, f7, 9.0F, 6.5F).textTopLeft(Fonts.INTER_BOLD, flag ? "+" : "~", 0.0F, 0.0F, i4, 6.5F);
                new RenderUtil(drawContext, f10 + 10.0F, f7, this.panelWidth, 6.5F).textTopLeft(Fonts.INTER_SEMIBOLD, entry.module(), 0.0F, 0.0F, i4, 6.5F);
                float f12 = f7 + 6.5F + 3.0F;

                for (String string : strings) {
                    float f13 = 0.9F * f11 * this.getEdgeFade(f12, 7.5F, f4, f5);
                    if (f13 > 0.004F) {
                        new RenderUtil(drawContext, f10 + 10.0F, f12, this.panelWidth, 7.5F)
                            .textTopLeft(Fonts.INTER_REGULAR, string, 0.0F, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), f13), 5.8F);
                    }

                    f12 += 7.5F;
                }
            }

            f7 += f8 + 10.0F;
        }
    }

    private float getEdgeFade(float f, float f2, float f3, float f4) {
        float f5 = 1.0F - f3 * (1.0F - smoothstep(0.0F, 10.0F, f - this.contentY));
        float f6 = 1.0F - f4 * (1.0F - smoothstep(0.0F, 10.0F, this.contentY + this.contentHeight - (f + f2)));
        return f5 * f6;
    }

    private void renderToggleButton(DrawContext drawContext, int i, int i2, float f, float f2, RenderUtil.BlurQuality blurQuality) {
        this.toggleHoverAnimation.update();
        this.togglePressAnimation.update();
        this.togglePressAnimation.animate(0.0, 0.22, Easings.BACK_OUT);
        this.toggleAlpha = f2;
        float f3 = Math.max(0.0F, this.panelX + this.panelWidth);
        float f4 = this.toggleHoverAnimation.get();
        float f5 = this.togglePressAnimation.get();
        float f6 = (14.0F + 6.0F * f4) * (1.0F - 0.18F * f5);
        this.toggleX = f3;
        this.toggleWidth = f6 + 4.0F;
        this.toggleHoverAnimation.animate(this.isToggleHovered(i, i2) ? 1.0 : 0.0, 0.22, Easings.BACK_OUT);
        if (!(f2 <= 0.004F)) {
            float f7 = MathHelper.clamp(f4, 0.0F, 1.0F);
            float f8 = 36.0F * (1.0F + 0.04F * f5);
            float f9 = 34.0F - (f8 - 36.0F) / 2.0F;
            Radius radius = Radius.all(7.0F);
            RenderUtil renderUtil = new RenderUtil(drawContext, f3, f9, f6, f8);
            int i3 = ColorUtil.interpolate(ColorUtil.BLACK, ColorUtil.interpolate(ColorUtil.BLACK, ColorUtil.WHITE, 0.04F), f);
            renderUtil.scissor(
                () -> {
                    new RenderUtil(
                            drawContext,
                            MathUtil.roundToStep(f3 - 7.0F, 0.5F),
                            MathUtil.roundToStep(f9, 0.5F),
                            MathUtil.roundToStep(f6 + 7.0F, 0.5F),
                            MathUtil.roundToStep(f8, 0.5F)
                        )
                        .blurGlowBorder(
                            f2 * MathHelper.lerp(f, 0.4F, 0.95F),
                            Theme.getBase(),
                            0.68F,
                            Theme.getNearBackground(),
                            14.0F,
                            0.0F,
                            Easings.QUAD_IN,
                            radius,
                            f > 0.5F ? blurQuality : RenderUtil.BlurQuality.MIDDLE,
                            true
                        );
                    new RenderUtil(drawContext, f3 - 7.0F, f9, f6 + 7.0F, f8)
                        .rect(Theme.withAlpha(i3, MathHelper.lerp(f, 0.45F, 0.55F) * f2), radius);
                }
            );
            if (GraphicsQuality.getCurrent().isEffectsEnabled() && f7 > 0.004F) {
                renderUtil.scissor(
                    () -> new RenderUtil(drawContext, f3 - 7.0F, f9, f6 + 7.0F, f8)
                        .water(Theme.getPrimary(), f2, f7, 0.5F, 0.5F, (float)(System.currentTimeMillis() % 1000000L) / 1000.0F, Radius.all(7.0F))
                );
            }

            float f10 = f3 + f6 - 7.0F;
            int i4 = ColorUtil.interpolate(Theme.getSecondaryText(), Theme.getTextColor(), Math.max(f7, f));
            this.listIcon.render(drawContext, Icon.RECTANGLE_LIST, f10 - 3.5F, f9 + 9.0F, 7.0F, Theme.withAlpha(i4, (0.8F + 0.2F * f7) * f2), f7);
            float f11 = f9 + 24.5F;
            MatrixUtil.push(drawContext);
            MatrixUtil.rotate(drawContext, f10, f11, 180.0F * f);
            this.chevronIcon.render(drawContext, Icon.CHEVRON_RIGHT, f10 - 2.25F, f11 - 2.25F, 4.5F, Theme.withAlpha(i4, (0.5F + 0.35F * f7) * f2), f7);
            MatrixUtil.pop(drawContext);
        }
    }

    private void updateWrappedLines(float f) {
        String string = Language.getLocale();
        if (f != this.wrapWidth || !string.equals(this.wrapLocale)) {
            this.wrappedLines.clear();

            for (ChangelogPanel.Entry entry : ENTRIES) {
                this.wrappedLines.add(this.wrapText(Language.translate(entry.key(), entry.fallback()), f - 10.0F));
            }

            this.wrapWidth = f;
            this.wrapLocale = string;
        }
    }

    private float computeContentHeight() {
        float f = 0.0F;

        for (List list : this.wrappedLines) {
            f += 9.5F + list.size() * 7.5F + 10.0F;
        }

        return Math.max(0.0F, f - 10.0F);
    }

    private static float getStaggerProgress(float f, float f2) {
        return (float)Easings.QUART_OUT.ease(MathHelper.clamp((f - f2) / 0.26F, 0.0F, 1.0F));
    }

    private static float smoothstep(float f, float f2, float f3) {
        float f4 = MathHelper.clamp((f3 - f) / (f2 - f), 0.0F, 1.0F);
        return f4 * f4 * (3.0F - 2.0F * f4);
    }

    private static float fitFontSize(MsdfFont msdfFont, String string, float f, float f2, float f3) {
        float f4 = msdfFont.getWidth(string, f2);
        return !(f4 <= f) && !(f4 <= 0.0F) ? Math.max(f3, f2 * f / f4) : f2;
    }

    private static String truncateToWidth(MsdfFont msdfFont, String string, float f, float f2) {
        if (msdfFont.getWidth(string, f2) <= f) {
            return string;
        }

        float f3 = msdfFont.getWidth("…", f2);
        int i = string.length();

        while (i > 1 && msdfFont.getWidth(string.substring(0, i), f2) + f3 > f) {
            i--;
        }

        return string.substring(0, i) + "…";
    }

    private List<String> wrapText(String string, float f) {
        ArrayList arrayList = new ArrayList();
        StringBuilder builder = new StringBuilder();

        for (String string2 : string.split(" ")) {
            String string3 = builder.isEmpty() ? string2 : builder + " " + string2;
            if (!builder.isEmpty() && Fonts.INTER_REGULAR.getWidth(string3, 5.8F) > f) {
                arrayList.add(builder.toString());
                builder.setLength(0);
                builder.append(string2);
            } else {
                builder.setLength(0);
                builder.append(string3);
            }
        }

        if (!builder.isEmpty()) {
            arrayList.add(builder.toString());
        }

        return arrayList;
    }

    public boolean mouseClicked(double d, double d2) {
        if (this.isToggleHovered(d, d2)) {
            this.togglePressAnimation.forceValue(1.0);
            if (this.open) {
                this.close();
                return true;
            }

            this.open();
        } else {
            if (!this.open) {
                return false;
            }

            if (this.scroll.mouseClicked(d, d2)) {
                return true;
            }

            if (!this.isHovered(d, d2, this.panelX, this.panelY, this.panelWidth, this.panelHeight)) {
                this.close();
                return true;
            }
        }

        return true;
    }

    public void mouseReleased() {
        this.scroll.mouseReleased();
    }

    public boolean mouseDragged(double d, double d2) {
        return this.open && this.scroll.mouseDragged(d, d2);
    }

    public boolean mouseScrolled(double d) {
        if (!this.open) {
            return false;
        }

        this.scroll.onScroll(d);
        return true;
    }

    enum ChangeKind {
        ADDED,
        CHANGED;
    }

    record Entry(ChangelogPanel.ChangeKind kind, String module, String key, String fallback) {
    }
}
