package tech.sadnes.client.ui.clickgui.component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.Bind;
import tech.sadnes.client.feature.BindMode;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureBind;
import tech.sadnes.client.feature.ValueBind;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.SettingWidgets;
import tech.sadnes.client.setting.ValueSetting;
import tech.sadnes.client.util.Key;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

public final class BindPopupComponent extends Component {
    private static final float BIND_LIST_WIDTH = 82.0F;
    private static final float EDITOR_WIDTH = 100.0F;
    private static final float PADDING = 4.5F;
    private static final float ROW_HEIGHT = 16.0F;
    private static final float ROW_SPACING = 0.0F;
    private static final float SEPARATOR_GAP = 2.0F;
    private static final int MAX_BINDS = 5;
    private static final float PILL_HEIGHT = 11.0F;
    private static final float PILL_ICON_SIZE = 7.0F;
    private static final float PILL_ICON_PADDING = 2.0F;
    private static final float PILL_TEXT_GAP = 1.5F;
    private final Feature feature;
    private final Animation openAnimation = new Animation();
    private final Animation editorAnimation = new Animation();
    private final List<BindPopupComponent.Button> listButtons = new ArrayList<>();
    private final List<BindPopupComponent.Button> editorButtons = new ArrayList<>();
    final Map<Object, Animation> valueToggleAnimations = new IdentityHashMap<>();
    private final Map<Object, Animation> visibleToggleAnimations = new IdentityHashMap<>();
    private final Map<Object, Animation> valueSectionAnimations = new IdentityHashMap<>();
    private final Map<Object, Animation> modeAnimations = new IdentityHashMap<>();
    private final Map<Object, Animation> chevronAnimations = new IdentityHashMap<>();
    private final Map<Object, Animation> rowVisibilityAnimations = new IdentityHashMap<>();
    private final Map<Object, Animation> rowHoverAnimations = new IdentityHashMap<>();
    private final Map<Object, BindPopupComponent.BindEntry> bindEntries = new IdentityHashMap<>();
    private final List<Object> bindOrder = new ArrayList<>();
    private final List<Integer> pressedKeys = new ArrayList<>();
    private final BindPopupComponent.Bounds listBounds = new BindPopupComponent.Bounds();
    private final BindPopupComponent.Bounds editorBounds = new BindPopupComponent.Bounds();
    private final BindPopupComponent.Bounds valueBounds = new BindPopupComponent.Bounds();
    private BindPopupComponent.BindSource source;
    private BindPopupComponent.BindEntry selectedBind;
    private float anchorX;
    private float anchorY;
    private boolean open;
    private boolean listeningForKey;
    private boolean valueInteracting;
    private boolean firstLayout;
    private BindPopupComponent.BindSource pendingSource;
    private float pendingX;
    private float pendingY;
    private Runnable allHotKeysAction;

    public Bind getSelectedBind() {
        if (this.selectedBind == null) {
            return null;
        } else {
            return this.selectedBind.featureBind != null ? this.selectedBind.featureBind : this.selectedBind.settingBind;
        }
    }

    public BindPopupComponent(Feature feature2) {
        this.feature = feature2;
    }

    public void openForFeature(double d, double d2) {
        this.openSource(new BindPopupComponent.FeatureBindSource(this.feature), d, d2);
    }

    public void openForSetting(ValueSetting<?> valueSetting, double d, double d2) {
        if (valueSetting != null) {
            this.openSource(new BindPopupComponent.SettingBindSource(valueSetting), d, d2);
        }
    }

    public void addBindAndListen() {
        if (this.source != null && this.source.getBinds().size() < 5) {
            BindPopupComponent.BindEntry bindEntry = this.source.createBind();
            if (bindEntry != null) {
                this.selectedBind = bindEntry;
                this.valueInteracting = false;
                this.pressedKeys.clear();
                this.listeningForKey = true;
            }
        }
    }

    private void openSource(BindPopupComponent.BindSource bindSource, double d, double d2) {
        if (this.open || this.source != null && this.openAnimation.get() > 0.001F) {
            this.pendingSource = bindSource;
            this.pendingX = (float)d;
            this.pendingY = (float)d2;
            if (this.open) {
                this.close();
            }
        } else {
            this.source = bindSource;
            this.anchorX = (float)d;
            this.anchorY = (float)d2;
            this.open = true;
            this.listeningForKey = false;
            this.valueInteracting = false;
            this.selectedBind = null;
            this.firstLayout = true;
            this.clearBindCache();
        }
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        this.updateAnimations();
        this.listButtons.clear();
        this.editorButtons.clear();
        this.valueBounds.clear();
        if (this.source == null) {
            this.listBounds.clear();
            this.editorBounds.clear();
            this.clearBindCache();
        } else if (!this.open && this.openAnimation.get() <= 0.001F) {
            this.finishClose();
        } else {
            List list = this.source.getBinds();
            this.refreshSelectedBind(list);
            this.pruneAnimations(list);
            Set set = this.collectBindKeys(list);
            this.syncBindOrder(list, set);
            float f2 = this.getAlpha() * Math.min(this.openAnimation.get(), 1.0F);
            BindPopupComponent.PopupBounds popupBounds = this.renderBindList(drawContext, i, i2, list, set, f2);
            this.renderBindEditor(drawContext, i, i2, f, popupBounds, f2);
        }
    }

    private void updateAnimations() {
        this.openAnimation.update();
        if (this.open) {
            this.openAnimation.animate(1.0, 0.3F, Easings.BACK_OUT);
        } else {
            this.openAnimation.animate(0.0, 0.2F, Easings.QUAD_IN);
        }

        this.editorAnimation.update();
        if (this.open && this.selectedBind != null) {
            this.editorAnimation.animate(1.0, 0.3F, Easings.BACK_OUT);
        } else {
            this.editorAnimation.animate(0.0, 0.2F, Easings.QUAD_IN);
        }
    }

    private void finishClose() {
        this.listBounds.clear();
        this.editorBounds.clear();
        this.source = null;
        this.clearBindCache();
        if (this.pendingSource != null) {
            BindPopupComponent.BindSource bindSource = this.pendingSource;
            double d = this.pendingX;
            double d2 = this.pendingY;
            this.pendingSource = null;
            this.openSource(bindSource, d, d2);
        }
    }

    private BindPopupComponent.PopupBounds renderBindList(
        DrawContext drawContext, int i, int i2, List<BindPopupComponent.BindEntry> bindEntries2, Set<Object> objects, float f
    ) {
        boolean flag = bindEntries2.size() < 5;
        float f2 = 0.0F;

        for (Object object : this.bindOrder) {
            Animation animation = this.rowVisibilityAnimations.get(object);
            float f3 = animation != null ? animation.get() : 0.0F;
            if (f3 > 0.001F) {
                f2 += 16.0F * f3;
            }
        }

        int i3 = 2 + (flag ? 1 : 0);
        float f4 = 9.0F + f2 + i3 * 16.0F + (i3 - 1) * 0.0F + 2.0F;
        float f5 = GuiRenderUtil.clampToScreenX(this.anchorX, 82.0F);
        float f6 = GuiRenderUtil.clampToScreenY(this.anchorY, f4);
        this.listBounds.set(f5, f6, 82.0F, f4);
        float f7 = this.openAnimation.get();
        float f8 = 0.85F + 0.15F * f7;
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(drawContext, f5 + 41.0F, f6 + f4 / 2.0F, f8);
        GuiRenderUtil.drawPopup(drawContext, f5, f6, 82.0F, f4, f, 10.0F);
        float f9 = f6 + 4.5F;

        for (Object object2 : this.bindOrder) {
            BindPopupComponent.BindEntry bindEntry = this.bindEntries.get(object2);
            if (bindEntry != null) {
                Animation animation2 = this.rowVisibilityAnimations.get(object2);
                float f10 = animation2 != null ? animation2.get() : 1.0F;
                if (!(f10 <= 0.001F)) {
                    BindPopupComponent.Row row = BindPopupComponent.Row.bind(bindEntry, this.getBindLabel(bindEntry));
                    this.renderListRow(drawContext, i, i2, row, f5 + 4.5F, f9, 73.0F, f * f10);
                    if (objects.contains(object2)) {
                        this.listButtons.add(BindPopupComponent.Button.of(row.action(), f5 + 4.5F, f9, 73.0F, 16.0F * f10, bindEntry));
                    }

                    f9 += 16.0F * f10;
                }
            }
        }

        if (flag) {
            BindPopupComponent.Row row2 = BindPopupComponent.Row.add("New Bind");
            this.renderListRow(drawContext, i, i2, row2, f5 + 4.5F, f9, 73.0F, f);
            this.listButtons.add(BindPopupComponent.Button.of(row2.action(), f5 + 4.5F, f9, 73.0F, 16.0F, null));
            f9 += 16.0F;
        }

        BindPopupComponent.Row row3 = BindPopupComponent.Row.allHotKeys("All Hot Keys");
        this.renderListRow(drawContext, i, i2, row3, f5 + 4.5F, f9, 73.0F, f);
        this.listButtons.add(BindPopupComponent.Button.of(row3.action(), f5 + 4.5F, f9, 73.0F, 16.0F, null));
        float f11 = f9 + 16.0F;
        float f12 = f11 + 2.0F;
        this.renderSeparator(drawContext, f5 + 4.5F, this.getSeparatorY(f12), 73.0F, f);
        BindPopupComponent.Row row4 = BindPopupComponent.Row.reset("Reset");
        this.renderListRow(drawContext, i, i2, row4, f5 + 4.5F, f12, 73.0F, f);
        this.listButtons.add(BindPopupComponent.Button.of(row4.action(), f5 + 4.5F, f12, 73.0F, 16.0F, null));
        MatrixUtil.pop(drawContext);
        return new BindPopupComponent.PopupBounds(f5, f6, f4);
    }

    private void renderBindEditor(DrawContext drawContext, int i, int i2, float f, BindPopupComponent.PopupBounds popupBounds, float f2) {
        if (this.selectedBind != null && !(this.editorAnimation.get() <= 0.001F)) {
            float f3 = this.editorAnimation.get();
            float f4 = f2 * Math.min(f3, 1.0F);
            boolean flag = this.source.hasValueSection(this.selectedBind);
            float f5 = this.animateToggle(this.valueSectionAnimations, this.selectedBind, flag);
            float f6 = this.source.getValueHeight(this.selectedBind);
            float f7 = f6 * f5;
            float f8 = 73.0F + f7 + 2.0F;
            float f9 = GuiRenderUtil.clampToScreenX(popupBounds.x() + 82.0F - 4.0F, 100.0F);
            float f10 = GuiRenderUtil.clampToScreenY(popupBounds.y() + popupBounds.height() / 2.0F - f8 / 2.0F, f8);
            this.editorBounds.set(f9, f10, 100.0F, f8);
            float f11 = 0.85F + 0.15F * f3;
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f9 + 50.0F, f10 + f8 / 2.0F, f11);
            GuiRenderUtil.drawPopup(drawContext, f9, f10, 100.0F, f8, f2, 10.0F);
            float f12 = f10 + 4.5F;
            float f13 = this.renderEditorButton(
                drawContext, BindPopupComponent.Row.editor(BindPopupComponent.Action.EDIT_KEY, "Key", this.selectedBind.getKeyName()), f9, f12, 91.0F, f4
            );
            float f14 = this.renderModeRow(drawContext, f9, f13, 91.0F, f4);
            if (f5 > 0.001F) {
                new RenderUtil(drawContext, f9 + 4.5F, f14, 91.0F, f7).scissor(() -> {
                    this.valueBounds.set(f9 + 4.5F, f14, 91.0F, f6);
                    this.source.renderValue(drawContext, this.selectedBind, this.valueBounds, i, i2, f);
                });
            }

            this.valueBounds.set(f9 + 4.5F, f14, 91.0F, f7);
            float f15 = f14 + f7;
            float f16 = this.renderVisibleRow(drawContext, f9, f15, 91.0F, f4);
            float f17 = f16 + 2.0F;
            this.renderSeparator(drawContext, f9 + 4.5F, this.getSeparatorY(f17), 91.0F, f4);
            this.renderEditorButton(drawContext, BindPopupComponent.Row.delete("Delete"), f9, f17, 91.0F, f4);
            MatrixUtil.pop(drawContext);
            this.source.renderValueOverlay(drawContext, this.selectedBind, this.valueBounds, i, i2, f);
        } else {
            this.editorBounds.clear();
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        if (this.listeningForKey && this.selectedBind != null) {
            int i = click.button();
            if (!this.pressedKeys.contains(i)) {
                this.pressedKeys.add(i);
            }

            this.applyPressedKeys();
            return true;
        } else {
            if (!this.open && this.openAnimation.get() <= 0.001F) {
                return false;
            }

            if (this.selectedBind != null && this.source != null && this.source.isValueCapturing(this.selectedBind)) {
                this.valueInteracting = this.source.valueMouseClicked(this.selectedBind, click, this.valueBounds);
                return true;
            }

            if (this.valueBounds.contains(click.x(), click.y()) && this.selectedBind != null) {
                this.valueInteracting = this.source.valueMouseClicked(this.selectedBind, click, this.valueBounds);
                return this.valueInteracting;
            }

            BindPopupComponent.Button button2 = this.findButtonAt(this.editorButtons, click.x(), click.y());
            if (button2 != null) {
                return this.handleEditorAction(button2);
            }

            BindPopupComponent.Button button3 = this.findButtonAt(this.listButtons, click.x(), click.y());
            if (button3 != null) {
                return this.handleListAction(button3);
            }

            if (this.isMouseOver(click.x(), click.y())) {
                return true;
            }

            this.close();
            return false;
        }
    }

    @Override
    public boolean onMouseRelease(Click click) {
        if (this.valueInteracting && this.selectedBind != null) {
            this.source.valueMouseReleased(this.selectedBind, click);
            this.valueInteracting = false;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        return this.valueInteracting && this.selectedBind != null
            ? this.source.valueMouseDragged(this.selectedBind, click, d, d2, this.valueBounds)
            : false;
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        return this.isMouseOver(d, d2);
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        if (this.listeningForKey && this.selectedBind != null) {
            int i = keyInput.key();
            switch (i) {
                case 256:
                case 259:
                case 261:
                    this.selectedBind.setKey(-1);
                    this.selectedBind.setModifiers(FeatureBind.NO_MODIFIERS);
                    this.pressedKeys.clear();
                    this.listeningForKey = false;
                    return true;
                default:
                    if (!this.pressedKeys.contains(i)) {
                        this.pressedKeys.add(i);
                    }

                    return true;
            }
        } else if (!this.open || keyInput.key() != 256) {
            return false;
        } else if (this.selectedBind != null) {
            this.selectedBind = null;
            this.valueInteracting = false;
            this.listeningForKey = false;
            return true;
        } else {
            this.close();
            return true;
        }
    }

    private void applyPressedKeys() {
        if (this.selectedBind != null && !this.pressedKeys.isEmpty()) {
            ArrayList arrayList = new ArrayList();

            for (int i : this.pressedKeys) {
                int i2 = this.normalizeKey(i);
                if (!arrayList.contains(i2)) {
                    arrayList.add(i2);
                }
            }

            int i3 = arrayList.size() - 1;
            this.selectedBind.setKey((Integer)arrayList.get(i3));
            int[] ints = new int[i3];

            for (int i4 = 0; i4 < i3; i4++) {
                ints[i4] = (Integer)arrayList.get(i4);
            }

            this.selectedBind.setModifiers(ints);
            this.pressedKeys.clear();
            this.listeningForKey = false;
        } else {
            this.pressedKeys.clear();
            this.listeningForKey = false;
        }
    }

    private int normalizeKey(int i) {
        return Key.isModifier(i) ? Key.getModifier(i) : i;
    }

    @Override
    public boolean keyReleased(KeyInput keyInput) {
        if (this.listeningForKey && this.selectedBind != null && !this.pressedKeys.isEmpty()) {
            this.applyPressedKeys();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onCharType(CharInput charInput) {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void close() {
        this.open = false;
        this.listeningForKey = false;
        this.valueInteracting = false;
        this.selectedBind = null;
        this.pressedKeys.clear();
    }

    @Override
    public void init(int i, int i2) {
    }

    @Override
    public void resize(int i, int i2) {
    }

    @Override
    public void mouseMoved(double d, double d2) {
    }

    @Override
    public void removed() {
    }

    public void resetState() {
        this.close();
        this.pendingSource = null;
        this.source = null;
        this.listButtons.clear();
        this.editorButtons.clear();
        this.listBounds.clear();
        this.editorBounds.clear();
        this.valueBounds.clear();
        this.valueToggleAnimations.clear();
        this.visibleToggleAnimations.clear();
        this.valueSectionAnimations.clear();
        this.modeAnimations.clear();
        this.clearBindCache();
        this.openAnimation.forceValue(0.0);
        this.editorAnimation.forceValue(0.0);
    }

    public boolean isMouseOver(double d, double d2) {
        return this.listBounds.contains(d, d2) || this.editorBounds.contains(d, d2);
    }

    public boolean isOpen() {
        return this.open;
    }

    public boolean isListeningForKey() {
        return this.listeningForKey;
    }

    public Animation getOpenAnimation() {
        return this.openAnimation;
    }

    public Animation getEditorAnimation() {
        return this.editorAnimation;
    }

    private void clearBindCache() {
        this.bindOrder.clear();
        this.bindEntries.clear();
        this.rowVisibilityAnimations.clear();
        this.rowHoverAnimations.clear();
        this.chevronAnimations.clear();
    }

    private Set<Object> collectBindKeys(List<BindPopupComponent.BindEntry> bindEntries2) {
        HashSet hashSet = new HashSet();

        for (BindPopupComponent.BindEntry bindEntry : bindEntries2) {
            Object object = bindEntry.getBindObject();
            if (object != null) {
                hashSet.add(object);
            }
        }

        return hashSet;
    }

    private void refreshSelectedBind(List<BindPopupComponent.BindEntry> bindEntries2) {
        if (this.selectedBind != null) {
            for (BindPopupComponent.BindEntry bindEntry : bindEntries2) {
                if (bindEntry.isSameBind(this.selectedBind)) {
                    this.selectedBind = bindEntry;
                    return;
                }
            }

            this.selectedBind = null;
            this.listeningForKey = false;
            this.valueInteracting = false;
            this.pressedKeys.clear();
        }
    }

    private float renderEditorButton(DrawContext drawContext, BindPopupComponent.Row row, float f, float f2, float f3, float f4) {
        float f5 = f + 4.5F;
        this.renderEditorRow(drawContext, row, f5, f2, f3, f4);
        this.editorButtons.add(BindPopupComponent.Button.of(row.action(), f5, f2, f3, 16.0F, null));
        return f2 + 16.0F + 0.0F;
    }

    private float renderModeRow(DrawContext drawContext, float f, float f2, float f3, float f4) {
        float f5 = f + 4.5F;
        int i = this.getSecondaryColor(f4);
        new RenderUtil(drawContext, f5, f2, f3, 16.0F)
            .icon(Icon.SETTINGS, 3.0F, 8.0F - 8.5F / 2.0F, 8.5F, i)
            .textMiddleLeft(Fonts.INTER_MEDIUM, "Mode", 16.0F, 0.0F, i, 5.5F);
        float f6 = this.animateToggle(this.modeAnimations, this.selectedBind, this.selectedBind.getMode() == BindMode.HOLD);
        float f7 = this.getModePillWidth(BindMode.TOGGLE.getDisplayName(), 1.0F - f6);
        float f8 = this.getModePillWidth(BindMode.HOLD.getDisplayName(), f6);
        float f9 = f2 + 8.0F - 5.5F;
        float f10 = f5 + f3 - 3.0F - f8;
        float f11 = f10 - 1.5F - f7;
        this.renderModePill(drawContext, f11, f9, f7, Icon.TOGGLE_ON, BindMode.TOGGLE.getDisplayName(), 1.0F - f6, f4);
        this.renderModePill(drawContext, f10, f9, f8, Icon.HAND_POINTER, BindMode.HOLD.getDisplayName(), f6, f4);
        this.editorButtons.add(BindPopupComponent.Button.of(BindPopupComponent.Action.SET_MODE_TOGGLE, f11, f2, f7, 16.0F, null));
        this.editorButtons.add(BindPopupComponent.Button.of(BindPopupComponent.Action.SET_MODE_HOLD, f10, f2, f8, 16.0F, null));
        this.editorButtons.add(BindPopupComponent.Button.of(BindPopupComponent.Action.EDIT_MODE, f5, f2, f3, 16.0F, null));
        return f2 + 16.0F + 0.0F;
    }

    private float getModePillWidth(String string, float f) {
        return 11.0F + (1.5F + Fonts.INTER_MEDIUM.getWidth(string, 5.5F)) * f;
    }

    private void renderModePill(DrawContext drawContext, float f, float f2, float f3, Icon icon2, String string, float f4, float f5) {
        if (f4 > 0.001F) {
            new RenderUtil(drawContext, f, f2, f3, 11.0F).rect(Theme.withAlpha(Theme.getPrimary(), 0.18F * f4 * f5), Radius.smoothAll(5.5F));
        }

        int i = ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), Theme.getPrimary(), f4), f5 * (0.55F + 0.45F * f4));
        new RenderUtil(drawContext, f, f2, f3, 11.0F).icon(icon2, 2.0F, 2.0F, 7.0F, i);
        if (!(f4 <= 0.01F)) {
            int i2 = ColorUtil.multiplyAlpha(i, f4);
            new RenderUtil(drawContext, f, f2, f3, 11.0F)
                .scissor(() -> new RenderUtil(drawContext, f, f2, f3, 11.0F).textMiddleLeft(Fonts.INTER_MEDIUM, string, 10.5F, 0.0F, i2, 5.5F));
        }
    }

    private float renderVisibleRow(DrawContext drawContext, float f, float f2, float f3, float f4) {
        float f5 = f + 4.5F;
        int i = this.getSecondaryColor(f4);
        float f6 = this.animateToggle(this.visibleToggleAnimations, this.selectedBind, this.selectedBind != null && this.selectedBind.isVisible());
        new RenderUtil(drawContext, f5, f2, f3, 16.0F)
            .icon(Icon.EYE, 3.0F, 8.0F - 8.5F / 2.0F, 8.5F, i)
            .textMiddleLeft(Fonts.INTER_MEDIUM, "Visible", 16.0F, 0.0F, i, 5.5F);
        float f7 = f5 + f3 - 14.0F - 4.0F;
        float f8 = f2 + 8.0F - 4.5F;
        SettingWidgets.drawDefaultSwitch(drawContext, f7, f8, f6, this.getAlpha());
        this.editorButtons.add(BindPopupComponent.Button.of(BindPopupComponent.Action.EDIT_VISIBLE, f5, f2, f3, 16.0F, null));
        return f2 + 16.0F + 0.0F;
    }

    private boolean handleListAction(BindPopupComponent.Button button) {
        if (this.source == null) {
            return false;
        }

        switch (button.action) {
            case SELECT_BIND:
                BindPopupComponent bindPopupComponent;
                BindPopupComponent.BindEntry bindEntry;
                if (this.selectedBind != null && button.bind != null && button.bind.isSameBind(this.selectedBind)) {
                    bindPopupComponent = this;
                    bindEntry = null;
                } else {
                    bindPopupComponent = this;
                    bindEntry = button.bind;
                }

                bindPopupComponent.selectedBind = bindEntry;
                this.listeningForKey = false;
                this.valueInteracting = false;
                return true;
            case ADD_BIND:
                if (this.source.getBinds().size() >= 5) {
                    return false;
                } else {
                    BindPopupComponent.BindEntry bindEntry2 = this.source.createBind();
                    this.selectedBind = null;
                    this.listeningForKey = false;
                    this.valueInteracting = false;
                    if (bindEntry2 != null) {
                        return true;
                    }

                    return false;
                }
            case RESET_ALL:
                this.source.clearBinds();
                this.selectedBind = null;
                this.listeningForKey = false;
                this.valueInteracting = false;
                return true;
            case ALL_HOT_KEYS:
                this.selectedBind = null;
                this.listeningForKey = false;
                this.valueInteracting = false;
                if (this.allHotKeysAction != null) {
                    this.allHotKeysAction.run();
                }

                return true;
            default:
                return false;
        }
    }

    private boolean handleEditorAction(BindPopupComponent.Button button) {
        if (this.selectedBind == null) {
            return false;
        }

        switch (button.action) {
            case EDIT_KEY:
                this.pressedKeys.clear();
                this.listeningForKey = true;
                return true;
            case EDIT_MODE:
                BindPopupComponent.BindEntry bindEntry = this.selectedBind;
                if (this.selectedBind.getMode() == BindMode.HOLD) {
                    bindEntry.setMode(BindMode.TOGGLE);
                    return true;
                }

                bindEntry.setMode(BindMode.HOLD);
                return true;
            case SET_MODE_TOGGLE:
                this.selectedBind.setMode(BindMode.TOGGLE);
                return true;
            case SET_MODE_HOLD:
                this.selectedBind.setMode(BindMode.HOLD);
                return true;
            case EDIT_VISIBLE:
                BindPopupComponent.BindEntry bindEntry2 = this.selectedBind;
                if (!this.selectedBind.isVisible()) {
                    bindEntry2.setVisible(true);
                    return true;
                }

                bindEntry2.setVisible(false);
                return true;
            case DELETE_BIND:
                this.source.removeBind(this.selectedBind);
                this.selectedBind = null;
                this.listeningForKey = false;
                this.valueInteracting = false;
                return true;
            default:
                return false;
        }
    }

    private void renderListRow(DrawContext drawContext, int i, int i2, BindPopupComponent.Row row, float f, float f2, float f3, float f4) {
        boolean flag = row.action == BindPopupComponent.Action.RESET_ALL;
        int i3 = flag ? Theme.withAlpha(ColorUtil.rgb(255, 86, 86), f4) : this.getPrimaryColor(f4);
        int i4 = flag ? Theme.withAlpha(ColorUtil.rgb(255, 120, 120), f4) : this.getSecondaryColor(f4);
        boolean flag2;
        float f5;
        if (row.action == BindPopupComponent.Action.SELECT_BIND && row.bindReference != null) {
            flag2 = true;
            f5 = 10.0F;
        } else {
            flag2 = false;
            f5 = 0.0F;
        }

        float f6 = f5;
        float f7 = Math.max(0.0F, f3 - 16.0F - 4.0F - f6);
        Object object = flag2 ? row.bindReference.getBindObject() : row.action;
        Animation animation = this.rowHoverAnimations.computeIfAbsent(object, object2 -> new Animation());
        animation.update();
        animation.animate(this.isHovered(i, i2, f, f2, f3, 16.0) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        GuiRenderUtil.drawHighlight(drawContext, f, f2, f3, 16.0F, f4 * animation.get(), Radius.all(4.0F));
        new RenderUtil(drawContext, f, f2, f3, 16.0F)
            .icon(row.icon, 3.0F, 3.75F, 8.5F, i4)
            .scrollingTextTop(Fonts.INTER_MEDIUM, row.title, 16.0F, 5.5F, f7, i3, 5.5F);
        if (flag2) {
            float f8 = this.animateChevron(row.bindReference, this.selectedBind != null && row.bindReference.isSameBind(this.selectedBind));
            RenderUtil.drawChevron(drawContext, f + f3 - 10.0F, f2 + 3.5F, 9.0F, 180.0F * f8, i4);
        }
    }

    private void renderEditorRow(DrawContext drawContext, BindPopupComponent.Row row, float f, float f2, float f3, float f4) {
        int i;
        int i2;
        boolean flag;
        String string;
        label31: {
            boolean flag2 = row.action == BindPopupComponent.Action.DELETE_BIND;
            i = flag2 ? Theme.withAlpha(ColorUtil.rgb(255, 94, 94), f4) : this.getSecondaryColor(f4);
            i2 = flag2 ? Theme.withAlpha(ColorUtil.rgb(255, 120, 120), f4) : this.getPrimaryColor(f4);
            if (row.action == BindPopupComponent.Action.EDIT_KEY) {
                flag = true;
            } else {
                flag = false;
                if (row.description != null) {
                    string = row.description;
                    break label31;
                }
            }

            string = "";
        }

        String string2 = string;
        new RenderUtil(drawContext, f, f2, f3, 16.0F)
            .icon(row.icon, 3.0F, 8.0F - 8.5F / 2.0F, 8.5F, i)
            .textMiddleLeft(Fonts.INTER_MEDIUM, row.title, 16.0F, 0.0F, i, 5.5F)
            .textMiddleRight(Fonts.INTER_MEDIUM, string2, 4.0F, 0.0F, i2, 5.5F);
        if (flag) {
            this.renderKeyBadge(drawContext, row.description, f, f2, f3, f4);
        }
    }

    private void renderKeyBadge(DrawContext drawContext, String string, float f, float f2, float f3, float f4) {
        String string2 = this.listeningForKey ? SettingWidgets.getWaitingText() : string;
        float f5 = Math.max(22.0F, Fonts.INTER_MEDIUM.getWidth(string2, 5.5F) + 10.0F);
        float f6 = f + f3 - 4.0F - f5;
        float f7 = f2 + 8.0F - 10.0F / 2.0F;
        if (this.listeningForKey) {
            SettingWidgets.drawWaitingBind(drawContext, f6, f7, f5, 10.0F, f4);
        } else {
            SettingWidgets.drawBindBox(drawContext, f6, f7, f5, 10.0F, string2, 1.0F, f4);
        }
    }

    private void renderSeparator(DrawContext drawContext, float f, float f2, float f3, float f4) {
        float f5 = f3 - 8.0F;
        if (!(f5 <= 0.0F)) {
            GuiRenderUtil.drawOverlay(drawContext, f + 4.0F, MathUtil.roundToStep(f2, 0.5F), f5, 0.5F, f4);
        }
    }

    private float getSeparatorY(float f) {
        return f - 1.0F - 0.25F;
    }

    private BindPopupComponent.Button findButtonAt(List<BindPopupComponent.Button> buttons, double d, double d2) {
        for (BindPopupComponent.Button button : buttons) {
            if (button.contains(d, d2)) {
                return button;
            }
        }

        return null;
    }

    private void pruneAnimations(List<BindPopupComponent.BindEntry> bindEntries2) {
        HashSet hashSet = new HashSet<>(this.bindOrder);
        if (bindEntries2 != null) {
            for (BindPopupComponent.BindEntry bindEntry : bindEntries2) {
                Object object = bindEntry.getBindObject();
                if (object != null) {
                    hashSet.add(object);
                }
            }
        }

        if (hashSet.isEmpty()) {
            this.valueToggleAnimations.clear();
            this.visibleToggleAnimations.clear();
            this.valueSectionAnimations.clear();
            this.modeAnimations.clear();
            this.chevronAnimations.clear();
        } else {
            this.valueToggleAnimations.keySet().removeIf(object2 -> !hashSet.contains(object2));
            this.visibleToggleAnimations.keySet().removeIf(object3 -> !hashSet.contains(object3));
            this.valueSectionAnimations.keySet().removeIf(object4 -> !hashSet.contains(object4));
            this.modeAnimations.keySet().removeIf(object5 -> !hashSet.contains(object5));
            this.chevronAnimations.keySet().removeIf(object6 -> !hashSet.contains(object6));
        }
    }

    private void syncBindOrder(List<BindPopupComponent.BindEntry> bindEntries2, Set<Object> objects) {
        for (BindPopupComponent.BindEntry bindEntry : bindEntries2) {
            Object object = bindEntry.getBindObject();
            if (object != null) {
                this.bindEntries.put(object, bindEntry);
                if (!this.bindOrder.contains(object)) {
                    this.bindOrder.add(object);
                }
            }
        }

        for (Object object2 : this.bindOrder) {
            boolean flag = objects.contains(object2);
            Animation animation = this.rowVisibilityAnimations.computeIfAbsent(object2, object3 -> {
                Animation animation2 = new Animation();
                if (this.firstLayout) {
                    animation2.forceValue(1.0);
                    return animation2;
                } else {
                    animation2.forceValue(0.0);
                    return animation2;
                }
            });
            animation.update();
            animation.animate(flag ? 1.0 : 0.0, flag ? 0.2 : 0.15, flag ? Easings.QUAD_OUT : Easings.QUAD_IN);
        }

        this.bindOrder.removeIf(object4 -> {
            if (objects.contains(object4)) {
                return false;
            } else {
                Animation animation3 = this.rowVisibilityAnimations.get(object4);
                if (animation3 == null || !(animation3.get() > 0.001F) && animation3.isFinished()) {
                    this.rowVisibilityAnimations.remove(object4);
                    this.bindEntries.remove(object4);
                    return true;
                } else {
                    return false;
                }
            }
        });
        this.firstLayout = false;
    }

    float animateToggle(Map<Object, Animation> map, BindPopupComponent.BindEntry bindEntry, boolean flag) {
        Object object = bindEntry != null ? bindEntry.getBindObject() : null;
        if (object == null) {
            return flag ? 1.0F : 0.0F;
        } else {
            Animation animation = map.computeIfAbsent(object, object2 -> {
                Animation animation2 = new Animation();
                if (flag) {
                    animation2.forceValue(1.0);
                    return animation2;
                } else {
                    animation2.forceValue(0.0);
                    return animation2;
                }
            });
            animation.update();
            if (flag) {
                animation.animate(1.0, 0.25, Easings.EXPO_OUT);
                return animation.get();
            } else {
                animation.animate(0.0, 0.25, Easings.EXPO_OUT);
                return animation.get();
            }
        }
    }

    private float animateChevron(BindPopupComponent.BindEntry bindEntry, boolean flag) {
        Object object = bindEntry != null ? bindEntry.getBindObject() : null;
        if (object == null) {
            return flag ? 1.0F : 0.0F;
        } else {
            Animation animation = this.chevronAnimations.computeIfAbsent(object, object2 -> {
                Animation animation2 = new Animation();
                animation2.forceValue(0.0);
                return animation2;
            });
            animation.update();
            if (flag) {
                animation.animate(1.0, 0.22F, Easings.QUAD_OUT);
                return animation.get();
            } else {
                animation.animate(0.0, 0.22F, Easings.QUAD_OUT);
                return animation.get();
            }
        }
    }

    private String getBindLabel(BindPopupComponent.BindEntry bindEntry) {
        if (bindEntry == null) {
            return "";
        } else {
            return bindEntry.getKey() == -1 ? "None" : bindEntry.getMode().getDisplayName() + "\"" + bindEntry.getKeyName() + "\"";
        }
    }

    public void setAllHotKeysAction(Runnable runnable) {
        this.allHotKeysAction = runnable;
    }

    enum Action {
        SELECT_BIND,
        ADD_BIND,
        RESET_ALL,
        ALL_HOT_KEYS,
        EDIT_KEY,
        EDIT_MODE,
        SET_MODE_TOGGLE,
        SET_MODE_HOLD,
        EDIT_VISIBLE,
        DELETE_BIND;
    }

    static final class BindEntry {
        final FeatureBind featureBind;
        private final ValueSetting<?> setting;
        final ValueBind<?> settingBind;

        private BindEntry(FeatureBind featureBind2, ValueSetting<?> valueSetting, ValueBind<?> valueBind) {
            this.featureBind = featureBind2;
            this.setting = valueSetting;
            this.settingBind = valueBind;
        }

        static BindPopupComponent.BindEntry ofFeature(FeatureBind featureBind2) {
            return new BindPopupComponent.BindEntry(featureBind2, null, null);
        }

        static BindPopupComponent.BindEntry ofSetting(ValueSetting<?> valueSetting, ValueBind<?> valueBind) {
            return new BindPopupComponent.BindEntry(null, valueSetting, valueBind);
        }

        int getKey() {
            if (this.featureBind != null) {
                return this.featureBind.getKey();
            } else {
                return this.settingBind != null ? this.settingBind.getKey() : -1;
            }
        }

        void setKey(int i) {
            if (this.featureBind != null) {
                this.featureBind.setKey(i);
            } else {
                if (this.settingBind != null) {
                    this.settingBind.setKey(i);
                }
            }
        }

        void setModifiers(int[] ints) {
            if (this.featureBind != null) {
                this.featureBind.setModifiers(ints);
            } else {
                if (this.settingBind != null) {
                    this.settingBind.setModifiers(ints);
                }
            }
        }

        String getKeyName() {
            if (this.featureBind != null) {
                return this.featureBind.getDisplayString();
            } else {
                return this.settingBind != null ? this.settingBind.getDisplayString() : Key.getDisplayName(-1);
            }
        }

        BindMode getMode() {
            if (this.featureBind != null) {
                return this.featureBind.getMode();
            } else {
                return this.settingBind != null ? this.settingBind.getMode() : BindMode.HOLD;
            }
        }

        void setMode(BindMode bindMode) {
            if (this.featureBind != null) {
                this.featureBind.setMode(bindMode);
            } else {
                if (this.settingBind != null) {
                    this.settingBind.setMode(bindMode);
                }
            }
        }

        boolean isVisible() {
            return this.featureBind != null ? this.featureBind.isVisible() : this.settingBind != null && this.settingBind.isVisible();
        }

        void setVisible(boolean flag) {
            if (this.featureBind != null) {
                this.featureBind.setVisible(flag);
            } else {
                if (this.settingBind != null) {
                    this.settingBind.setVisible(flag);
                }
            }
        }

        Object getBindObject() {
            return this.featureBind != null ? this.featureBind : this.settingBind;
        }

        boolean isSameBind(BindPopupComponent.BindEntry bindEntry) {
            if (bindEntry == null) {
                return false;
            } else {
                return this.featureBind == null && bindEntry.featureBind == null
                    ? this.setting == bindEntry.setting && this.settingBind == bindEntry.settingBind
                    : this.featureBind == bindEntry.featureBind;
            }
        }
    }

    interface BindSource {
        List<BindPopupComponent.BindEntry> getBinds();

        BindPopupComponent.BindEntry createBind();

        void removeBind(BindPopupComponent.BindEntry bindEntry);

        void clearBinds();

        float getValueHeight(BindPopupComponent.BindEntry bindEntry);

        boolean hasValueSection(BindPopupComponent.BindEntry bindEntry);

        void renderValue(DrawContext drawContext, BindPopupComponent.BindEntry bindEntry, BindPopupComponent.Bounds bounds, int i, int i2, float f);

        void renderValueOverlay(DrawContext drawContext, BindPopupComponent.BindEntry bindEntry, BindPopupComponent.Bounds bounds, int i, int i2, float f);

        boolean isValueCapturing(BindPopupComponent.BindEntry bindEntry);

        boolean valueMouseClicked(BindPopupComponent.BindEntry bindEntry, Click click, BindPopupComponent.Bounds bounds);

        boolean valueMouseDragged(BindPopupComponent.BindEntry bindEntry, Click click, double d, double d2, BindPopupComponent.Bounds bounds);

        void valueMouseReleased(BindPopupComponent.BindEntry bindEntry, Click click);
    }

    public static final class Bounds {
        float x;
        float y;
        float width;
        float height;

        public void set(float f, float f2, float f3, float f4) {
            this.x = f;
            this.y = f2;
            this.width = f3;
            this.height = f4;
        }

        public void clear() {
            this.set(0.0F, 0.0F, 0.0F, 0.0F);
        }

        public boolean contains(double d, double d2) {
            return d >= this.x && d <= this.x + this.width && d2 >= this.y && d2 <= this.y + this.height;
        }
    }

    static final class Button {
        final BindPopupComponent.Action action;
        final BindPopupComponent.BindEntry bind;
        private final BindPopupComponent.Bounds bounds = new BindPopupComponent.Bounds();

        private Button(BindPopupComponent.Action action2, float f, float f2, float f3, float f4, BindPopupComponent.BindEntry bindEntry) {
            this.action = action2;
            this.bind = bindEntry;
            this.bounds.set(f, f2, f3, f4);
        }

        static BindPopupComponent.Button of(BindPopupComponent.Action action2, float f, float f2, float f3, float f4, BindPopupComponent.BindEntry bindEntry) {
            return new BindPopupComponent.Button(action2, f, f2, f3, f4, bindEntry);
        }

        boolean contains(double d, double d2) {
            return this.bounds.contains(d, d2);
        }
    }

    final class FeatureBindSource implements BindPopupComponent.BindSource {
        private final Feature feature;

        FeatureBindSource(Feature feature2) {
            this.feature = feature2;
        }

        @Override
        public List<BindPopupComponent.BindEntry> getBinds() {
            ArrayList arrayList = new ArrayList();

            for (FeatureBind featureBind : this.feature.getBinds()) {
                arrayList.add(BindPopupComponent.BindEntry.ofFeature(featureBind));
            }

            return arrayList;
        }

        @Override
        public BindPopupComponent.BindEntry createBind() {
            FeatureBind featureBind = new FeatureBind(-1, BindMode.TOGGLE, true, true);
            this.feature.addBind(featureBind);
            return BindPopupComponent.BindEntry.ofFeature(featureBind);
        }

        @Override
        public void removeBind(BindPopupComponent.BindEntry bindEntry) {
            if (bindEntry != null && bindEntry.featureBind != null) {
                this.feature.removeBind(bindEntry.featureBind);
            }
        }

        @Override
        public void clearBinds() {
            this.feature.clearBinds();
        }

        @Override
        public float getValueHeight(BindPopupComponent.BindEntry bindEntry) {
            return 18.0F;
        }

        @Override
        public boolean hasValueSection(BindPopupComponent.BindEntry bindEntry) {
            return bindEntry != null && bindEntry.getMode() == BindMode.HOLD;
        }

        @Override
        public void renderValue(DrawContext drawContext, BindPopupComponent.BindEntry bindEntry, BindPopupComponent.Bounds bounds, int i, int i2, float f) {
            boolean flag;
            if (bindEntry != null && bindEntry.featureBind != null) {
                flag = bindEntry.featureBind.getEnableState();
            } else {
                flag = false;
            }

            float f2 = BindPopupComponent.this.animateToggle(BindPopupComponent.this.valueToggleAnimations, bindEntry, flag);
            new RenderUtil(drawContext, bounds.x, bounds.y, bounds.width, bounds.height)
                .icon(Icon.CHECK, 3.0F, bounds.height / 2.0F - 8.5F / 2.0F, 8.5F, BindPopupComponent.this.getSecondaryColor())
                .textMiddleLeft(Fonts.INTER_MEDIUM, "Value", 17.0F, 0.0F, BindPopupComponent.this.getSecondaryColor(), 5.5F);
            float f3 = bounds.x + bounds.width - 14.0F - 4.0F;
            float f4 = bounds.y + bounds.height / 2.0F - 4.5F;
            SettingWidgets.drawDefaultSwitch(drawContext, f3, f4, f2, BindPopupComponent.this.getAlpha());
        }

        @Override
        public void renderValueOverlay(DrawContext drawContext, BindPopupComponent.BindEntry bindEntry, BindPopupComponent.Bounds bounds, int i, int i2, float f) {
        }

        @Override
        public boolean isValueCapturing(BindPopupComponent.BindEntry bindEntry) {
            return false;
        }

        @Override
        public boolean valueMouseClicked(BindPopupComponent.BindEntry bindEntry, Click click, BindPopupComponent.Bounds bounds) {
            if (bindEntry != null && bindEntry.featureBind != null && BindPopupComponent.this.isLeftButton(click.button())) {
                if (!BindPopupComponent.this.isHovered(click.x(), click.y(), bounds.x, bounds.y, bounds.width, bounds.height)) {
                    return false;
                } else {
                    FeatureBind featureBind2 = bindEntry.featureBind;
                    if (!bindEntry.featureBind.getEnableState()) {
                        featureBind2.setEnableState(true);
                        return true;
                    } else {
                        featureBind2.setEnableState(false);
                        return true;
                    }
                }
            } else {
                return false;
            }
        }

        @Override
        public boolean valueMouseDragged(BindPopupComponent.BindEntry bindEntry, Click click, double d, double d2, BindPopupComponent.Bounds bounds) {
            return false;
        }

        @Override
        public void valueMouseReleased(BindPopupComponent.BindEntry bindEntry, Click click) {
        }
    }

    record PopupBounds(float x, float y, float height) {
    }

    record Row(BindPopupComponent.Action action, Icon icon, String title, String description, BindPopupComponent.BindEntry bindReference) {

        static BindPopupComponent.Row bind(BindPopupComponent.BindEntry bindEntry, String string) {
            return new BindPopupComponent.Row(BindPopupComponent.Action.SELECT_BIND, Icon.KEYBOARD, string, "", bindEntry);
        }

        static BindPopupComponent.Row add(String string) {
            return new BindPopupComponent.Row(BindPopupComponent.Action.ADD_BIND, Icon.PLUS, string, "", null);
        }

        static BindPopupComponent.Row reset(String string) {
            return new BindPopupComponent.Row(BindPopupComponent.Action.RESET_ALL, Icon.REFRESH, string, "", null);
        }

        static BindPopupComponent.Row allHotKeys(String string) {
            return new BindPopupComponent.Row(BindPopupComponent.Action.ALL_HOT_KEYS, Icon.RECTANGLE_LIST, string, "", null);
        }

        static BindPopupComponent.Row editor(BindPopupComponent.Action action2, String string, String string2) {
            return new BindPopupComponent.Row(action2, action2 == BindPopupComponent.Action.EDIT_KEY ? Icon.KEYBOARD : Icon.SETTINGS, string, string2, null);
        }

        static BindPopupComponent.Row delete(String string) {
            return new BindPopupComponent.Row(BindPopupComponent.Action.DELETE_BIND, Icon.TRASH, string, "", null);
        }
    }

    final class SettingBindSource implements BindPopupComponent.BindSource {
        private final ValueSetting<?> setting;

        SettingBindSource(ValueSetting<?> valueSetting) {
            this.setting = valueSetting;
        }

        @Override
        public List<BindPopupComponent.BindEntry> getBinds() {
            ArrayList arrayList = new ArrayList();

            for (ValueBind valueBind : this.setting.getBinds()) {
                arrayList.add(BindPopupComponent.BindEntry.ofSetting(this.setting, valueBind));
            }

            return arrayList;
        }

        @Override
        public BindPopupComponent.BindEntry createBind() {
            return BindPopupComponent.BindEntry.ofSetting(this.setting, this.newSettingBind());
        }

        @Override
        public void removeBind(BindPopupComponent.BindEntry bindEntry) {
            if (bindEntry != null && bindEntry.settingBind != null) {
                this.deleteSettingBind(bindEntry.settingBind);
            }
        }

        @Override
        public void clearBinds() {
            this.setting.clearBinds();
        }

        @Override
        public float getValueHeight(BindPopupComponent.BindEntry bindEntry) {
            return this.setting.getBindRowHeight();
        }

        @Override
        public boolean hasValueSection(BindPopupComponent.BindEntry bindEntry) {
            return !(this.setting instanceof BooleanSetting) && !(this.setting instanceof GroupSetting)
                ? true
                : bindEntry != null && bindEntry.getMode() == BindMode.HOLD;
        }

        @Override
        public void renderValue(DrawContext drawContext, BindPopupComponent.BindEntry bindEntry, BindPopupComponent.Bounds bounds, int i, int i2, float f) {
            this.runWithBind(bindEntry, bounds, () -> this.setting.render(drawContext, i, i2, f));
        }

        @Override
        public void renderValueOverlay(DrawContext drawContext, BindPopupComponent.BindEntry bindEntry, BindPopupComponent.Bounds bounds, int i, int i2, float f) {
            if (this.setting.isBindDropdownOpen()) {
                this.runWithBind(bindEntry, bounds, () -> this.setting.renderBindDropdown(drawContext, i, i2, f));
            }
        }

        @Override
        public boolean isValueCapturing(BindPopupComponent.BindEntry bindEntry) {
            return this.setting.isBindDropdownOpen();
        }

        @Override
        public boolean valueMouseClicked(BindPopupComponent.BindEntry bindEntry, Click click, BindPopupComponent.Bounds bounds) {
            return this.testWithBind(bindEntry, bounds, () -> this.setting.mouseClicked(click, false));
        }

        @Override
        public boolean valueMouseDragged(BindPopupComponent.BindEntry bindEntry, Click click, double d, double d2, BindPopupComponent.Bounds bounds) {
            return this.testWithBind(bindEntry, bounds, () -> this.setting.mouseDragged(click, d, d2));
        }

        @Override
        public void valueMouseReleased(BindPopupComponent.BindEntry bindEntry, Click click) {
            this.runWithBind(bindEntry, null, () -> this.setting.onMouseRelease(click));
        }

        private ValueBind<?> newSettingBind() {
            ValueSetting valueSetting = this.setting;
            return valueSetting.createBind(valueSetting.getValue());
        }

        private void deleteSettingBind(ValueBind<?> valueBind) {
            ValueSetting valueSetting = this.setting;
            valueSetting.removeBind(valueBind);
        }

        private void runWithBind(BindPopupComponent.BindEntry bindEntry, BindPopupComponent.Bounds bounds, Runnable runnable) {
            ValueSetting valueSetting = this.setting;
            ValueBind valueBind = valueSetting.editingBind;
            float f = valueSetting.position.x;
            float f2 = valueSetting.position.y;
            float f3 = valueSetting.size.x;
            float f4 = valueSetting.size.y;
            valueSetting.editingBind = (ValueBind)(bindEntry != null ? bindEntry.settingBind : null);
            if (bounds != null) {
                valueSetting.position.set(bounds.x, bounds.y);
                valueSetting.size.set(bounds.width, bounds.height);
            }

            try {
                runnable.run();
            } finally {
                valueSetting.editingBind = valueBind;
                valueSetting.position.set(f, f2);
                valueSetting.size.set(f3, f4);
            }
        }

        private boolean testWithBind(BindPopupComponent.BindEntry bindEntry, BindPopupComponent.Bounds bounds, BooleanSupplier booleanSupplier) {
            boolean[] booleans = new boolean[]{false};
            this.runWithBind(bindEntry, bounds, () -> booleans[0] = booleanSupplier.getAsBoolean());
            return booleans[0];
        }
    }
}
