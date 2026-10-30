package tech.sadnes.client.ui.clickgui.panel;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.impl.render.ambience.Ambience;
import tech.sadnes.client.feature.impl.render.ambience.AmbienceProfile;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.setting.DragSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.SettingWidgets;
import tech.sadnes.client.ui.clickgui.component.Component;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

public final class AmbiencePanel extends Component {
    private static final float PADDING = 8.0F;
    private static final float LABEL_HEIGHT = 10.0F;
    private static final float ROW_HEIGHT = 24.0F;
    private static final float SECTION_GAP = 4.0F;
    private static final float CORNER_RADIUS = 8.0F;
    private static final float SETTING_ROW_HEIGHT = 24.0F;
    private static final float CLOSE_ICON_SIZE = 10.0F;
    private static final float FONT_SIZE = 5.5F;
    private final Animation openAnimation = new Animation();
    private final Animation settingsAnimation = new Animation();
    private Ambience ambience;
    private boolean closing;
    private AmbienceProfile selectedEntry;
    private List<Setting<?>> settings = List.of();
    private AmbiencePanel.EntryRow globalRow;
    private final Map<AmbienceProfile.AmbienceWorld, AmbiencePanel.EntryRow> mapRows = new EnumMap<>(AmbienceProfile.AmbienceWorld.class);
    private final Map<Setting<?>, Animation> settingHoverAnimations = new IdentityHashMap<>();
    private float listX;
    private float listWidth;
    private float settingsX;
    private float settingsWidth;
    private float contentY;
    private float contentHeight;
    private float closeX;
    private float closeY;

    public boolean isOpen() {
        return this.ambience != null && !this.closing;
    }

    public boolean isShowing() {
        return this.openAnimation.get() > 0.001;
    }

    public float getOpenProgress() {
        return (float)Math.min(this.openAnimation.get(), 1.0);
    }

    public boolean isMouseOver(double d, double d2) {
        return this.isShowing() && this.isHovered(d, d2, this.position.x, this.position.y, this.size.x, this.size.y);
    }

    public boolean hasOpenOverlay() {
        for (Setting setting : this.settings) {
            if (setting.isOverlayOpen()) {
                return true;
            }
        }

        return false;
    }

    public void open(Ambience ambience2) {
        this.ambience = ambience2;
        this.closing = false;
        if (this.globalRow == null || this.globalRow.entry != ambience2.getGlobalProfile()) {
            this.globalRow = new AmbiencePanel.EntryRow(ambience2.getGlobalProfile());
            this.mapRows.clear();

            for (AmbienceProfile.AmbienceWorld ambienceWorld : AmbienceProfile.AmbienceWorld.values()) {
                this.mapRows.put(ambienceWorld, new AmbiencePanel.EntryRow(ambience2.getProfile(ambienceWorld)));
            }
        }

        for (AmbiencePanel.EntryRow entryRow : this.getAllRows()) {
            entryRow.hoverAnimation.forceValue(0.0);
            entryRow.enabledAnimation.forceValue(entryRow.entry.isCustom() ? 1.0 : 0.0);
            entryRow.previewAnimation.forceValue(0.0);
        }

        this.selectEntry(ambience2.getGlobalProfile());
        this.globalRow.selectAnimation.forceValue(1.0);
        this.openAnimation.animate(1.0, 0.35, Easings.QUART_OUT);
    }

    @Override
    public void close() {
        if (this.ambience != null && !this.closing) {
            this.closeSettingOverlays();
            this.closing = true;
            this.openAnimation.animate(0.0, 0.22, Easings.QUAD_IN);
        }
    }

    public void reset() {
        this.closeSettingOverlays();
        this.ambience = null;
        this.closing = false;
        this.selectedEntry = null;
        this.settings = List.of();
        this.openAnimation.forceValue(0.0);
    }

    private void closeSettingOverlays() {
        Iterator iterator2 = this.settings.iterator();

        while (iterator2.hasNext()) {
            ((Setting)iterator2.next()).close();
        }
    }

    private void selectEntry(AmbienceProfile ambienceProfile) {
        this.closeSettingOverlays();
        this.selectedEntry = ambienceProfile;
        this.settings = this.ambience.getRootSettings(ambienceProfile);

        for (Setting setting : this.settings) {
            setting.resize(0, 0);
            setting.alphaAnimation.forceValue(setting.isVisible() ? 1.0 : 0.0);
        }

        this.settingsAnimation.forceValue(0.0);
        this.settingsAnimation.animate(1.0, 0.3, Easings.QUAD_OUT);
    }

    private List<AmbiencePanel.EntryRow> getAllRows() {
        ArrayList arrayList = new ArrayList(4);
        arrayList.add(this.globalRow);
        arrayList.addAll(this.mapRows.values());
        return arrayList;
    }

    private List<AmbiencePanel.EntryRow> getVisibleRows() {
        ArrayList arrayList = new ArrayList(6);
        arrayList.add(this.globalRow);
        AmbienceProfile.AmbienceWorld ambienceWorld = this.ambience.getCurrentWorld();

        for (AmbienceProfile.AmbienceWorld ambienceWorld2 : AmbienceProfile.AmbienceWorld.values()) {
            AmbiencePanel.EntryRow entryRow = this.mapRows.get(ambienceWorld2);
            if (entryRow.entry.isAdded() || ambienceWorld2 == ambienceWorld) {
                arrayList.add(entryRow);
            }
        }

        return arrayList;
    }

    public void render(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4, float f5) {
        this.openAnimation.update();
        this.settingsAnimation.update();
        if (this.closing && this.openAnimation.isFinished() && this.openAnimation.get() <= 0.001) {
            this.ambience = null;
            this.closing = false;
        }

        if (this.ambience != null && !(this.openAnimation.get() <= 0.001)) {
            this.position.set(f2, f3);
            this.size.set(f4, f5);
            float f6 = this.getOpenProgress();
            float f7 = f6 * this.getAlpha();
            RenderUtil renderUtil = new RenderUtil(drawContext, f2, f3, f4, f5);
            renderUtil.blur(f7, Radius.smoothAll(20.0F), RenderUtil.BlurQuality.LOW, true);
            renderUtil.rect(Theme.withAlpha(Theme.getBackground(), 0.4F), f7, Radius.smoothAll(20.0F));
            float f8 = f3 + 22.0F;
            this.contentY = f8 + 10.0F + 4.0F;
            this.contentHeight = f3 + f5 - 8.0F - this.contentY;
            this.settingsWidth = 140.0F;
            this.settingsX = f2 + f4 - 8.0F - this.settingsWidth;
            this.listX = f2 + 8.0F;
            this.listWidth = this.settingsX - 8.0F - this.listX;
            this.closeX = f2 + f4 - 5.0F - 22.0F + 6.0F;
            this.closeY = f3 + 4.0F + 6.0F;
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f2 + f4 / 2.0F, f3 + f5 / 2.0F, 0.96F + 0.04F * f6);
            this.renderHeader(drawContext, f8, f7);
            this.renderRows(drawContext, i, i2, f7);
            this.renderSettings(drawContext, i, i2, f, f7, f6);
            MatrixUtil.pop(drawContext);
        }
    }

    public void renderOverlays(DrawContext drawContext, int i, int i2, float f) {
        if (this.ambience != null) {
            for (Setting setting : this.settings) {
                if (!this.isHiddenSetting(setting) && !(setting.alphaAnimation.get() <= 0.001F)) {
                    setting.renderOverlay(drawContext, i, i2, f);
                }
            }
        }
    }

    private void renderHeader(DrawContext drawContext, float f, float f2) {
        int i = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.65F), f2);
        new RenderUtil(drawContext, this.listX + 2.0F, f, this.listWidth, 10.0F)
            .textMiddleLeft(Fonts.INTER_SEMIBOLD, Language.translate("ambience.panel.mapSelection"), 0.0F, 0.0F, i, 5.5F);
        new RenderUtil(drawContext, this.settingsX + 2.0F, f, this.settingsWidth - 10.0F - 14.0F, 10.0F)
            .textMiddleLeft(Fonts.INTER_SEMIBOLD, Language.translate("ambience.panel.effects"), 0.0F, 0.0F, i, 5.5F);
        new RenderUtil(drawContext, this.closeX, this.closeY, 10.0F, 10.0F).icon(Icon.CLOSE, 0.0F, 0.0F, 10.0F, Theme.withAlpha(Theme.getSecondaryText(), f2));
    }

    private void renderRows(DrawContext drawContext, int i, int i2, float f) {
        float f2 = this.contentY;

        for (AmbiencePanel.EntryRow entryRow : this.getVisibleRows()) {
            entryRow.x = this.listX;
            entryRow.y = f2;
            entryRow.width = this.listWidth;
            entryRow.height = 24.0F;
            this.renderRow(drawContext, entryRow, i, i2, f);
            f2 += 28.0F;
        }
    }

    private void renderRow(DrawContext drawContext, AmbiencePanel.EntryRow entryRow, int i, int i2, float f) {
        boolean flag;
        if (!entryRow.isGlobal()) {
            flag = entryRow.entry.isAdded();
        } else {
            flag = true;
        }

        boolean flag2 = this.isHovered(i, i2, entryRow.x, entryRow.y, entryRow.width, entryRow.height);
        entryRow.hoverAnimation.update();
        entryRow.selectAnimation.update();
        entryRow.enabledAnimation.update();
        entryRow.hoverAnimation.animate(flag2 ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
        entryRow.selectAnimation.animate(entryRow.entry == this.selectedEntry ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        entryRow.enabledAnimation.animate(entryRow.entry.isCustom() ? 1.0 : 0.0, 0.25, Easings.EXPO_OUT);
        float f2 = entryRow.hoverAnimation.get();
        float f3 = entryRow.selectAnimation.get();
        float f4 = flag ? 1.0F : 0.55F + 0.25F * f2;
        GuiRenderUtil.drawOutlined(drawContext, entryRow.x, entryRow.y, entryRow.width, entryRow.height, f * (0.4F + 0.2F * f2 + 0.3F * f3), 8.0F);
        this.renderRowPreview(drawContext, entryRow, f);
        Icon icon2 = entryRow.isGlobal() ? Icon.GLOBE : entryRow.entry.getWorld().getIcon();
        int i3 = ColorUtil.multiplyAlpha(Theme.withAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), Theme.getTextColor(), f3), 0.8F * f4), f);
        new RenderUtil(drawContext, entryRow.x + 5.0F, entryRow.y + entryRow.height / 2.0F - 4.5F, 9.0F, 9.0F).icon(icon2, 0.0F, 0.0F, 9.0F, i3);
        String string = Language.translate(entryRow.isGlobal() ? "ambience.panel.global" : entryRow.entry.getWorld().getTranslationKey());
        int i4 = ColorUtil.multiplyAlpha(
            Theme.withAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), Theme.getTextColor(), Math.max(f3, f2 * 0.5F)), f4), f
        );
        new RenderUtil(drawContext, entryRow.x + 18.0F, entryRow.y, entryRow.width - 18.0F, entryRow.height).textMiddleLeft(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, i4, 5.5F);
        if (!flag) {
            this.renderAddButton(drawContext, entryRow, f2, f);
        } else {
            float f5 = entryRow.x + entryRow.width - 4.0F - 8.0F;
            float f6 = f5 - 2.0F;
            float f7 = entryRow.y + entryRow.height / 2.0F - 6.0F;
            boolean flag3;
            if (flag2) {
                flag3 = this.isHovered(i, i2, f6, f7, 12.0, 12.0);
            } else {
                flag3 = false;
            }

            entryRow.settingsHoverAnimation.update();
            entryRow.settingsHoverAnimation.animate(flag3 ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
            float f8 = entryRow.settingsHoverAnimation.get();
            if (f8 > 0.001F) {
                GuiRenderUtil.drawHighlight(drawContext, f6, f7, 12.0F, 12.0F, f * f8, Radius.all(4.0F));
            }

            int i5 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.5F + 0.4F * Math.max(f8, f3)), f);
            new RenderUtil(drawContext, f5, entryRow.y + entryRow.height / 2.0F - 4.0F, 8.0F, 8.0F).icon(Icon.SETTINGS, 0.0F, 0.0F, 8.0F, i5);
            if (!entryRow.isGlobal()) {
                entryRow.toggleWidth = 12.0F;
                entryRow.toggleHeight = 7.0F;
                entryRow.toggleX = f5 - 4.0F - entryRow.toggleWidth;
                entryRow.toggleY = entryRow.y + entryRow.height / 2.0F - entryRow.toggleHeight / 2.0F;
                float f9 = f * f2;
                if (f9 > 0.001F) {
                    SettingWidgets.drawSwitch(drawContext, entryRow.toggleX, entryRow.toggleY, entryRow.toggleWidth, entryRow.toggleHeight, entryRow.enabledAnimation.get(), f9);
                }
            }
        }
    }

    private void renderRowPreview(DrawContext drawContext, AmbiencePanel.EntryRow entryRow, float f) {
        if (!entryRow.isGlobal()) {
            Texture texture = entryRow.entry.getWorld().getPreview();
            if (texture != null) {
                entryRow.previewAnimation.update();
                entryRow.previewAnimation.animate(1.0, 0.45, Easings.QUART_OUT);
                float f2 = f * entryRow.previewAnimation.get() * 0.4F;
                if (!(f2 <= 0.001F)) {
                    float f3 = entryRow.height * 1.7777778F;
                    float f4 = entryRow.x + entryRow.width - f3;
                    GuiRenderUtil.bindTexture(texture);
                    if (GuiRenderUtil.getBoundTexture() != null) {
                        new RenderUtil(drawContext, f4, entryRow.y, f3, entryRow.height).glassGradient(0.0F, 1.0F, f2, Radius.of(8.0F, 8.0F, 0.0F, 0.0F));
                    }
                }
            }
        }
    }

    private void renderAddButton(DrawContext drawContext, AmbiencePanel.EntryRow entryRow, float f, float f2) {
        String string = Language.translate("ambience.panel.add");
        float f3 = Fonts.INTER_MEDIUM.getWidth(string, 5.5F);
        float f4 = 14.0F + f3 + 4.0F;
        float f5 = entryRow.x + entryRow.width - 4.0F - f4;
        float f6 = entryRow.y + entryRow.height / 2.0F - 10.0F / 2.0F;
        float f7 = 0.6F + 0.4F * f;
        int i = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), f7), f2);
        GuiRenderUtil.drawSurface(drawContext, f5, f6, f4, 10.0F, f2 * f7, 3.0F)
            .icon(Icon.PLUS, 4.0F, 2.0F, 6.0F, i)
            .textMiddleLeft(Fonts.INTER_MEDIUM, string, 12.0F, 0.0F, i, 5.5F);
    }

    private void renderSettings(DrawContext drawContext, int i, int i2, float f, float f2, float f3) {
        float f4 = this.settingsAnimation.get();
        float f5 = 0.0F;
        Setting setting2 = null;
        Setting setting3 = null;

        for (Setting setting4 : this.settings) {
            if (!this.isHiddenSetting(setting4)) {
                float f6 = setting4.alphaAnimation.get();
                f5 += setting4.resolveHeight(24.0F) * f6;
                if (f6 > 0.01F) {
                    if (setting2 == null) {
                        setting2 = setting4;
                    }

                    setting3 = setting4;
                }
            }
        }

        float f7 = this.contentY + (1.0F - f4) * 8.0F;
        GuiRenderUtil.drawOutlined(drawContext, this.settingsX, f7, this.settingsWidth, f5, f2 * f4 * 0.75F, 8.0F);
        boolean flag;
        float f8;
        float f9;
        if (!this.hasOpenOverlay()) {
            flag = this.isHovered(i, i2, this.settingsX, this.contentY, this.settingsWidth, this.contentHeight);
            f8 = f7;
            f9 = 0.0F;
        } else {
            flag = false;
            f8 = f7;
            f9 = 0.0F;
        }

        for (Setting setting5 : this.settings) {
            if (!this.isHiddenSetting(setting5)) {
                setting5.alphaMultiplier = f3 * f4;
                setting5.alphaAnimation.update();
                setting5.alphaAnimation.animate(setting5.isVisible() ? 1.0 : 0.0, 0.2, Easings.EXPO_OUT);
                float f10 = setting5.alphaAnimation.get();
                float f11 = setting5.resolveHeight(24.0F);
                if (f9 > 0.01F && f10 > 0.01F) {
                    GuiRenderUtil.drawOverlay(
                        drawContext,
                        this.settingsX + 8.0F,
                        MathUtil.roundToStep(f8 - 0.25F, 0.5F),
                        this.settingsWidth - 16.0F,
                        0.5F,
                        f2 * f4 * Math.min(f9, f10)
                    );
                }

                float f12 = f11 * f10;
                boolean flag2;
                if (flag) {
                    flag2 = this.isHovered(i, i2, this.settingsX, f8, this.settingsWidth, f12);
                } else {
                    flag2 = false;
                }

                Animation animation = this.settingHoverAnimations.computeIfAbsent(setting5, setting6 -> new Animation());
                animation.update();
                animation.animate(flag2 ? 1.0 : 0.0, 0.16, Easings.QUAD_OUT);
                float f13 = animation.get();
                if (f13 > 0.001F) {
                    GuiRenderUtil.drawHighlight(
                        drawContext,
                        this.settingsX,
                        f8,
                        this.settingsWidth,
                        f12,
                        f2 * f4 * f13 * 0.67F * f10,
                        this.getHighlightRadius(setting5 == setting2, setting5 == setting3)
                    );
                }

                setting5.position.set(this.settingsX + 8.0F, f8);
                setting5.size.set(this.settingsWidth - 16.0F, f11);
                setting5.render(drawContext, i, i2, f);
                f8 += f11 * f10;
                if (f10 > 0.01F) {
                    f9 = f10;
                }
            }
        }
    }

    private boolean isHiddenSetting(Setting<?> setting) {
        return setting instanceof DragSetting;
    }

    private Radius getHighlightRadius(boolean flag, boolean flag2) {
        if (flag && flag2) {
            return Radius.all(8.0F);
        } else if (flag) {
            return Radius.of(0.0F, 8.0F, 0.0F, 8.0F);
        } else {
            return flag2 ? Radius.of(8.0F, 0.0F, 8.0F, 0.0F) : Radius.zero();
        }
    }

    @Override
    public void mouseMoved(double d, double d2) {
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        if (this.ambience == null) {
            return false;
        }

        for (Setting setting : this.settings) {
            if (!this.isHiddenSetting(setting) && !(setting.alphaAnimation.get() <= 0.001F) && setting.isOverlayOpen() && setting.mouseClicked(click, flag)) {
                return true;
            }
        }

        boolean flag2 = this.isHovered(click.x(), click.y(), this.closeX - 2.0F, this.closeY - 2.0F, 14.0, 14.0);
        if (this.isLeftButton(click.button()) && flag2) {
            this.close();
            return true;
        }

        if (this.isLeftButton(click.button()) && this.handleRowClick(click)) {
            return true;
        }

        if (this.isHovered(click.x(), click.y(), this.settingsX - 2.0F, this.contentY - 2.0F, this.settingsWidth + 4.0F, this.contentHeight + 4.0F)) {
            for (Setting setting2 : this.settings) {
                if (!this.isHiddenSetting(setting2) && !(setting2.alphaAnimation.get() <= 0.001F)) {
                    boolean flag3 = setting2.isOverlayOpen();
                    if (setting2.mouseClicked(click, flag)) {
                        if (!flag3 && setting2.isOverlayOpen()) {
                            this.closeOtherDropdowns(setting2);
                        }

                        return true;
                    }
                }
            }
        }

        return this.isMouseOver(click.x(), click.y());
    }

    private boolean handleRowClick(Click click) {
        for (AmbiencePanel.EntryRow entryRow : this.getVisibleRows()) {
            if (this.isHovered(click.x(), click.y(), entryRow.x, entryRow.y, entryRow.width, entryRow.height)) {
                if (!entryRow.isGlobal() && !entryRow.entry.isAdded()) {
                    entryRow.entry.setAdded(true);
                    entryRow.entry.setCustom(true);
                    this.selectEntry(entryRow.entry);
                    return true;
                }

                if (!entryRow.isGlobal()
                    && this.isHovered(click.x(), click.y(), entryRow.toggleX - 1.5F, entryRow.toggleY - 1.5F, entryRow.toggleWidth + 3.0F, entryRow.toggleHeight + 3.0F)) {
                    AmbienceProfile ambienceProfile = entryRow.entry;
                    if (!entryRow.entry.isCustom()) {
                        ambienceProfile.setCustom(true);
                        return true;
                    }

                    ambienceProfile.setCustom(false);
                    return true;
                }

                if (this.selectedEntry != entryRow.entry) {
                    this.selectEntry(entryRow.entry);
                }

                return true;
            }
        }

        return false;
    }

    private void closeOtherDropdowns(Setting<?> setting) {
        for (Setting setting2 : this.settings) {
            if (setting2 != setting) {
                if (setting2 instanceof MultiSetting) {
                    ((MultiSetting)setting2).closeDropdown();
                } else if (setting2 instanceof ModeSetting) {
                    ((ModeSetting)setting2).closeDropdown();
                } else if (setting2 instanceof GroupSetting) {
                    ((GroupSetting)setting2).closePopupImmediately();
                }
            }
        }
    }

    @Override
    public boolean onMouseRelease(Click click) {
        if (this.ambience == null) {
            return false;
        }

        for (Setting setting : this.settings) {
            if (!this.isHiddenSetting(setting)) {
                setting.onMouseRelease(click);
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        if (this.ambience == null) {
            return false;
        }

        for (Setting setting : this.settings) {
            if (!this.isHiddenSetting(setting) && !(setting.alphaAnimation.get() <= 0.001F) && setting.mouseDragged(click, d, d2)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (this.ambience == null) {
            return false;
        }

        for (Setting setting : this.settings) {
            if (!this.isHiddenSetting(setting) && !(setting.alphaAnimation.get() <= 0.001F) && setting.mouseScrolled(d, d2, d3, d4)) {
                return true;
            }
        }

        return this.isMouseOver(d, d2);
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        if (this.ambience == null) {
            return false;
        }

        for (Setting setting : this.settings) {
            if (!this.isHiddenSetting(setting) && !(setting.alphaAnimation.get() <= 0.001F) && setting.onKeyPress(keyInput)) {
                return true;
            }
        }

        if (keyInput.key() == 256) {
            this.close();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean keyReleased(KeyInput keyInput) {
        if (this.ambience == null) {
            return false;
        }

        for (Setting setting : this.settings) {
            if (!this.isHiddenSetting(setting)) {
                setting.keyReleased(keyInput);
            }
        }

        return false;
    }

    @Override
    public boolean onCharType(CharInput charInput) {
        if (this.ambience == null) {
            return false;
        }

        for (Setting setting : this.settings) {
            if (!this.isHiddenSetting(setting) && !(setting.alphaAnimation.get() <= 0.001F) && setting.onCharType(charInput)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void resize(int i, int i2) {
        this.reset();
    }

    @Override
    public void init(int i, int i2) {
    }

    @Override
    public void removed() {
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    static final class EntryRow {
        final AmbienceProfile entry;
        final Animation hoverAnimation = new Animation();
        final Animation selectAnimation = new Animation();
        final Animation enabledAnimation = new Animation();
        final Animation settingsHoverAnimation = new Animation();
        final Animation previewAnimation = new Animation();
        float x;
        float y;
        float width;
        float height;
        float toggleX;
        float toggleY;
        float toggleWidth;
        float toggleHeight;

        EntryRow(AmbienceProfile ambienceProfile) {
            this.entry = ambienceProfile;
        }

        boolean isGlobal() {
            return this.entry.getWorld() == null;
        }
    }
}
