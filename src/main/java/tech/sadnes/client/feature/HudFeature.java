package tech.sadnes.client.feature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.DragSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.drag.DragComponent;
import tech.sadnes.client.ui.hud.Draggable;
import tech.sadnes.client.ui.hud.HudEntry;
import tech.sadnes.client.ui.hud.HudPanelUtil;
import tech.sadnes.client.util.color.Theme;

public abstract class HudFeature<T extends HudEntry> extends Feature implements Draggable {
    public final ModeSetting style = new ModeSetting("hud.style", "Default", new String[]{"Default", "Inverted"}).setVisibleWhen(() -> !this.isCompact() ? true : false);
    public final BooleanSetting rightAligned = new BooleanSetting("hud.rightAligned", false);
    public final BooleanSetting dynamicHeader = new BooleanSetting("hud.dynamicHeader", false);
    public final ModeSetting chip = (ModeSetting)((Setting<?>)new ModeSetting("hud.chip", "Both", new String[]{"Both", "Icon", "Text"})).setDisplayName("Chip");
    protected final DragSetting draggable;
    protected final List<T> entries = new ArrayList<>();
    protected final Animation widthAnimation = new Animation();
    protected final Animation heightAnimation = new Animation();
    protected final Animation alphaAnimation = new Animation();
    protected final Animation headerAnimation = new Animation();

    public HudFeature(float f, float f2) {
        this.draggable = new DragSetting(this.getInfo().name(), f, f2);
        this.draggable.onInput((drawContext, i, i2, f3) -> this.render(drawContext));
        this.draggable.onInput(this);
        this.draggable.setRenderCondition(() -> this.isEnabled() && this.widthAnimation.get() > 0.001F ? true : false);
        DragComponent.register(this.draggable);
        this.heightAnimation.forceValue(this.draggable.size2.x);
    }

    public DragSetting getDraggable() {
        return this.draggable;
    }

    @Override
    public final void render(DrawContext drawContext) {
        this.draggable.setDraggable(this::isEnabled);
        if (this.isEnabled()) {
            this.entries.clear();
            this.collectEntries(this.entries);
            this.sortEntries(this.entries);

            for (HudEntry hudEntry : this.entries) {
                hudEntry.animation.update();
                hudEntry.animation.animate(hudEntry.active ? 1.0 : 0.0, 0.4, Easings.QUART_OUT);
            }

            boolean flag = this.isCompact();
            boolean flag2 = this.dynamicHeader.getValue();
            boolean flag3;
            float f;
            if (!this.isHeaderHidden()) {
                flag3 = true;
                f = 15.0F;
            } else {
                flag3 = false;
                f = 0.0F;
            }

            float f2 = f;
            String string = this.getTitle();
            Icon icon = this.getIcon();
            float f3 = HEADER_FONT.getWidth(string, 6.0F);
            float f4 = HudPanelUtil.getHeaderWidth(8.0F, f3, icon != null, string != null && !string.isEmpty());
            float f5 = 0.0F;
            float f6 = 0.0F;
            float f7 = flag3 ? 13.0F : 0.0F;
            float f8;
            if (flag) {
                float[] floats = this.getCornerRadii(this.getEntryHeight());
                f8 = floats[0];
                f7 += floats[1];
            } else {
                boolean flag4 = this.isVisible();

                for (HudEntry hudEntry2 : this.entries) {
                    float f9 = this.getEntryProgress((T)hudEntry2);
                    if (f9 > f5) {
                        f5 = f9;
                    }

                    if (flag4) {
                        float f10 = this.getEntryOffset((T)hudEntry2);
                        if (f10 > f6) {
                            f6 = f10;
                        }
                    }

                    f7 += 13.0F * hudEntry2.animation.get();
                }

                float f11 = f5 > 0.0F && f6 > 0.0F ? 2.0F : 0.0F;
                f8 = f5 + f11 + f6;
            }

            float f12 = !flag3 ? 0.0F : (flag2 ? Math.max(f4, Math.max(f8, 1.0F)) : f4);
            float f13 = Math.max(Math.max(f12, f8), 1.0F);
            float f14 = flag ? 0.0F : (this.isInverted() ? f6 : f5);
            this.updateBounds(this.showHeader(), f13, f7, f12, f14);
            float f15 = this.widthAnimation.get();
            if (!(f15 <= 0.001F)) {
                float f16 = this.draggable.position2.x;
                float f17 = this.draggable.position2.y;
                float f18 = Math.max(this.alphaAnimation.get(), 1.0F);
                if (flag3) {
                    HudPanelUtil.drawHeader(drawContext, f16, f17, f18, f15, string, icon, BODY_FONT, HEADER_FONT, 6.0F);
                }

                if (flag) {
                    this.renderHeader(drawContext, f16, f17, f15, f2);
                } else {
                    this.renderBackground(drawContext, f16, f17, f15, this.headerAnimation.get(), f2);
                }
            }
        }
    }

    protected abstract String getTitle();

    protected abstract Icon getIcon();

    protected abstract void collectEntries(List<T> list);

    protected void sortEntries(List<T> list) {
        list.sort(Comparator.comparingDouble(this::getSortKey).reversed());
    }

    protected double getSortKey(T value) {
        float f = this.showChipIcon() ? this.getEntryWidth((T)value) : 0.0F;
        float f2 = this.showChipText() ? this.getEntryValueWidth((T)value) : 0.0F;
        boolean flag = f > 0.0F && f2 > 0.0F;
        return flag ? f + 4.0F + f2 : this.getEntryY((T)value);
    }

    protected abstract String getEntryText(T value);

    protected float getEntryWidth(T value) {
        return 0.0F;
    }

    protected float getEntryValueWidth(T value) {
        return 0.0F;
    }

    protected void renderEntryIcon(DrawContext drawContext, float f, float f2, float f3, float f4, T value) {
    }

    protected void renderEntryContent(DrawContext drawContext, float f, float f2, float f3, float f4, T value) {
    }

    protected String getEntrySuffix(T value) {
        return "";
    }

    protected Text getEntryLabel(T value) {
        return null;
    }

    protected int getEntryColor(T value, float f) {
        return this.applyAlpha(Theme.getTextColor(), f);
    }

    protected int getEntryBackgroundColor(T value, float f) {
        return this.applyAlpha(Theme.getSecondaryText(), f);
    }

    protected int applyAlpha(int i, float f) {
        return HudPanelUtil.applyAlpha(i, f);
    }

    protected boolean isVisible() {
        return true;
    }

    public boolean showHeader() {
        return !this.entries.isEmpty() || mc.currentScreen instanceof ClickGuiScreen || mc.currentScreen instanceof ChatScreen;
    }

    protected boolean isCompact() {
        return false;
    }

    protected float getEntryHeight() {
        return 150.0F;
    }

    protected boolean isHeaderHidden() {
        return false;
    }

    private boolean showChipIcon() {
        return !"Text".equals(this.chip.getValue());
    }

    private boolean showChipText() {
        return !"Icon".equals(this.chip.getValue());
    }

    protected boolean isInverted() {
        return this.style.is("Inverted");
    }

    protected boolean isRightAligned() {
        return this.rightAligned.getValue();
    }

    protected float getEntryAlpha(T value) {
        boolean flag = this.showChipIcon();
        boolean flag2 = this.showChipText();
        float f = flag ? this.getEntryWidth((T)value) : 0.0F;
        float f2 = flag2 ? this.getEntryValueWidth((T)value) : 0.0F;
        if (f <= 0.0F && f2 <= 0.0F) {
            return 0.0F;
        } else {
            return f > 0.0F && f2 > 0.0F ? f + 4.0F + f2 : f + f2;
        }
    }

    private float getEntryOffset(T value) {
        return !this.isVisible() ? 0.0F : 4.0F + this.getEntryY((T)value) + 4.0F;
    }

    private float getEntryProgress(T value) {
        float f = this.getEntryAlpha((T)value);
        if (f <= 0.0F) {
            return 0.0F;
        } else {
            return this.isEntryVisible((T)value) ? Math.max(f + 4.0F, 12.0F) : 2.0F + f + 4.0F;
        }
    }

    private boolean isEntryVisible(T value) {
        return this.showChipIcon() && this.getEntryWidth((T)value) > 0.0F && (!this.showChipText() || this.getEntryValueWidth((T)value) <= 0.0F);
    }

    private float getEntryY(T value) {
        Text text = this.getEntryLabel((T)value);
        return text != null ? BODY_FONT.getTextWidth(text, 6.0F) : BODY_FONT.getWidth(this.getEntryText((T)value), 6.0F) + BODY_FONT.getWidth(this.getEntrySuffix((T)value), 6.0F);
    }

    private void renderEntry(DrawContext drawContext, float f, float f2, float f3, T value, float f4) {
        float f5 = value.animation.get() * f3;
        if (!(f5 <= 0.001F)) {
            float f6 = (float)Easings.EXPO_OUT.ease(f5);
            float f7 = 5.0F * (1.0F - f5);
            float f8 = this.getEntryProgress((T)value);
            boolean flag = f8 > 0.0F;
            float f9 = this.getEntryOffset((T)value);
            boolean flag2 = f9 > 0.0F;
            boolean flag3 = this.isInverted();
            boolean flag4 = this.isRightAligned();
            float f10;
            float f11;
            if (!flag || !flag2) {
                f11 = f10 = f - f7;
            } else if (flag3) {
                f10 = (flag4 ? f + f4 - f9 : f) - f7;
                f11 = f + f4 + 2.0F - f7;
            } else {
                f11 = (flag4 ? f + f4 - f8 : f) - f7;
                f10 = f + f4 + 2.0F - f7;
            }

            Matrix3x2fStack matrix3x2fStack = drawContext.getMatrices();
            matrix3x2fStack.pushMatrix();
            matrix3x2fStack.translate(f, f2 + 10.0F / 2.0F);
            matrix3x2fStack.scale(f6, f6);
            matrix3x2fStack.translate(-f, -(f2 + 10.0F / 2.0F));
            if (flag) {
                HudPanelUtil.drawSmallPanel(drawContext, f11, f2, f8, 10.0F, f5);
                this.renderEntryBackground(drawContext, f11, f8, f2, 10.0F, f5, (T)value);
            }

            if (flag2) {
                HudPanelUtil.drawSmallPanel(drawContext, f10, f2, f9, 10.0F, f5);
                RenderUtil renderUtil = new RenderUtil(drawContext, f10, f2, f9, 10.0F);
                Text text = this.getEntryLabel((T)value);
                if (text != null) {
                    renderUtil.textMiddleLeft(BODY_FONT, text, 4.0F, 0.0F, this.getEntryColor((T)value, f5), 6.0F);
                    matrix3x2fStack.popMatrix();
                    return;
                }

                String string = this.getEntryText((T)value);
                renderUtil.textMiddleLeft(BODY_FONT, string, 4.0F, 0.0F, this.getEntryColor((T)value, f5), 6.0F);
                String string2 = this.getEntrySuffix((T)value);
                if (!string2.isEmpty()) {
                    float f12 = BODY_FONT.getWidth(string, 6.0F);
                    renderUtil.textMiddleLeft(BODY_FONT, string2, 4.0F + f12, 0.0F, this.getEntryBackgroundColor((T)value, f5), 6.0F);
                }
            }

            matrix3x2fStack.popMatrix();
        }
    }

    private void renderBackground(DrawContext drawContext, float f, float f2, float f3, float f4, float f5) {
        float f6 = 0.0F;

        for (HudEntry hudEntry : this.entries) {
            float f7 = f2 + f5 + f6;
            hudEntry.x = f;
            hudEntry.width = f7;
            hudEntry.height = this.draggable.size2.x;
            hudEntry.alpha = 10.0F;
            this.renderEntry(drawContext, f, f7, f3, (T)hudEntry, f4);
            f6 += 12.0F * hudEntry.animation.get();
        }
    }

    private float[] getCornerRadii(float f) {
        float f2 = 0.0F;
        float f3 = 0.0F;
        int i = 1;
        float f4 = 0.0F;
        boolean flag = false;

        for (HudEntry hudEntry : this.entries) {
            float f5 = this.getEntryProgress((T)hudEntry);
            hudEntry.removing = f5 > 0.0F;
            if (hudEntry.removing) {
                if (f2 > 0.0F && f2 + f5 > f) {
                    i++;
                    f2 = 0.0F;
                }

                hudEntry.index = i - 1;
                hudEntry.y = f2;
                f2 += f5 + 2.0F;
                f3 = Math.max(f3, f2 - 2.0F);
                f4 = Math.max(f4, hudEntry.animation.get());
                flag = true;
            }
        }

        if (!flag) {
            return new float[]{0.0F, 0.0F};
        }

        float f6 = (2.0F + i * 10.0F + (i - 1) * 2.0F) * f4;
        return new float[]{f3, f6};
    }

    private void renderHeader(DrawContext drawContext, float f, float f2, float f3, float f4) {
        float f5 = f2 + f4;

        for (HudEntry hudEntry : this.entries) {
            if (hudEntry.removing) {
                float f6 = hudEntry.animation.get() * f3;
                if (!(f6 <= 0.001F)) {
                    float f7 = this.getEntryProgress((T)hudEntry);
                    float f8 = f + hudEntry.y;
                    float f9 = f5 + hudEntry.index * 12.0F;
                    float f10 = (float)Easings.EXPO_OUT.ease(f6);
                    Matrix3x2fStack matrix3x2fStack = drawContext.getMatrices();
                    matrix3x2fStack.pushMatrix();
                    matrix3x2fStack.translate(f8 + f7 / 2.0F, f9 + 10.0F / 2.0F);
                    matrix3x2fStack.scale(f10, f10);
                    matrix3x2fStack.translate(-(f8 + f7 / 2.0F), -(f9 + 10.0F / 2.0F));
                    HudPanelUtil.drawSmallPanel(drawContext, f8, f9, f7, 10.0F, f6);
                    this.renderEntryBackground(drawContext, f8, f7, f9, 10.0F, f6, (T)hudEntry);
                    matrix3x2fStack.popMatrix();
                }
            }
        }
    }

    private void renderEntryBackground(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, T value) {
        boolean flag = this.showChipIcon();
        boolean flag2 = this.showChipText();
        float f6 = flag ? this.getEntryWidth((T)value) : 0.0F;
        float f7 = flag2 ? this.getEntryValueWidth((T)value) : 0.0F;
        boolean flag3 = flag && f6 > 0.0F && flag2 && f7 > 0.0F;
        if (flag3) {
            float f8 = f + 2.0F + f6 + 1.0F;
            float f9 = f8 + 0.25F;
            HudPanelUtil.drawSeparator(drawContext, f8, f3 + f4 / 2.0F, f5);
            float f10 = f9 - f;
            float f11 = f + f2 - f9;
            this.renderEntryIcon(drawContext, f + (f10 - f6) / 2.0F, f3, f4, f5, (T)value);
            this.renderEntryContent(drawContext, f9 + (f11 - f7) / 2.0F, f3, f4, f5, (T)value);
        } else if (flag && f6 > 0.0F) {
            this.renderEntryIcon(drawContext, f + (f2 - f6) / 2.0F, f3, f4, f5, (T)value);
        } else {
            if (flag2 && f7 > 0.0F) {
                this.renderEntryContent(drawContext, f + (f2 - f7) / 2.0F, f3, f4, f5, (T)value);
            }
        }
    }

    private void updateBounds(boolean flag, float f, float f2, float f3, float f4) {
        this.widthAnimation.update();
        this.widthAnimation.animate(flag ? 1.0 : 0.0, 0.25, Easings.QUART_OUT);
        this.heightAnimation.update();
        this.heightAnimation.animate(Math.max(f, 1.0F), 0.4, Easings.QUART_OUT);
        this.alphaAnimation.update();
        this.alphaAnimation.animate(Math.max(f3, 1.0F), 0.4, Easings.QUART_OUT);
        this.headerAnimation.update();
        this.headerAnimation.animate(Math.max(f4, 0.0F), 0.4, Easings.QUART_OUT);
        this.draggable.size2.set(this.heightAnimation.get(), Math.max(f2, 1.0F));
    }
}
