package tech.sadnes.client.setting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.BindMode;
import tech.sadnes.client.feature.ValueBind;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class MultiSetting extends ValueSetting<List<String>> {
    private static final float MIN_HEIGHT = 20.0F;
    private static final float CHIP_HEIGHT = 10.0F;
    private static final float PADDING_X = 6.0F;
    private static final float PADDING_Y = 3.0F;
    private static final float ICON_SIZE = 8.0F;
    private final Animation dropdownAnimation = new Animation();
    private final Animation bindDropdownAnimation = new Animation();
    private final Map<String, BooleanSetting> options = new LinkedHashMap<>();
    private final Set<String> dragVisited = new HashSet<>();
    private boolean dropdownOpen;
    private boolean dragging;
    private double lastDragX;
    private double lastDragY;
    private boolean bindDropdownOpen;
    private boolean syncing;
    private boolean requireOne;
    private float rowHeight = 20.0F;
    private float dropdownX;
    private float dropdownY;
    private float dropdownWidth;
    private float dropdownHeight;
    private float dropdownScale = 1.0F;

    public MultiSetting(String string) {
        super(string, List.of());
    }

    public MultiSetting(String string, BooleanSetting... booleanSettings) {
        this(string);
        if (booleanSettings != null) {
            for (BooleanSetting booleanSetting : booleanSettings) {
                this.onInput(booleanSetting);
            }
        }

        this.syncFromOptions();
    }

    public MultiSetting(String string, String... strings) {
        this(string);
        if (strings != null) {
            for (String string2 : strings) {
                this.addOption(string2);
            }
        }

        this.syncFromOptions();
    }

    public MultiSetting(GroupSetting groupSetting, String string) {
        this(string);
        if (groupSetting != null) {
            groupSetting.onInput(this);
        }
    }

    public MultiSetting(GroupSetting groupSetting, String string, BooleanSetting... booleanSettings) {
        this(string, booleanSettings);
        if (groupSetting != null) {
            groupSetting.onInput(this);
        }
    }

    public MultiSetting(GroupSetting groupSetting, String string, String... strings) {
        this(string, strings);
        if (groupSetting != null) {
            groupSetting.onInput(this);
        }
    }

    public MultiSetting setValue(List<String> strings) {
        super.setValue(strings);
        this.syncOptions();
        return this;
    }

    public MultiSetting setVisibleWhen(Supplier<Boolean> supplier) {
        super.setVisibleWhen(supplier);
        return this;
    }

    public MultiSetting requireAtLeastOne() {
        this.requireOne = true;
        return this;
    }

    public MultiSetting setOnChange(Runnable runnable) {
        super.setOnChange(runnable);
        return this;
    }

    public MultiSetting setOnVisibilityChange(Runnable runnable) {
        super.setOnVisibilityChange(runnable);
        return this;
    }

    public MultiSetting mouseDragged(int i, BindMode bindMode, List<String> strings) {
        super.mouseDragged(i, bindMode, strings);
        this.syncOptions();
        return this;
    }

    public MultiSetting addBind(ValueBind<List<String>> valueBind) {
        super.addBind(valueBind);
        this.syncOptions();
        return this;
    }

    public MultiSetting onInput(BooleanSetting booleanSetting) {
        if (booleanSetting != null && booleanSetting.getName() != null && !booleanSetting.getName().isEmpty()) {
            String string = this.normalizeKey(booleanSetting.getName());
            if (this.options.containsKey(string)) {
                return this;
            }

            BooleanSetting booleanSetting2 = BooleanSetting.of(booleanSetting.getName(), Boolean.TRUE.equals(booleanSetting.getDefaultValue()))
                .setVisibleWhen(booleanSetting::isVisible)
                .setOnChange(() -> this.handleOptionChange(booleanSetting.getName()));

            for (ValueBind valueBind : booleanSetting.getBinds()) {
                booleanSetting2.addBind(valueBind);
            }

            this.options.put(string, booleanSetting2);
            this.syncFromOptions();
            return this;
        } else {
            return this;
        }
    }

    public MultiSetting addOption(String string) {
        return this.addOption(string, false);
    }

    public MultiSetting addOption(String string, boolean flag) {
        if (string != null && !string.isEmpty()) {
            String string2 = this.normalizeKey(string);
            if (this.options.containsKey(string2)) {
                return this;
            }

            this.options.put(string2, BooleanSetting.of(string, flag).setOnChange(() -> this.handleOptionChange(string)));
            this.syncFromOptions();
            return this;
        } else {
            return this;
        }
    }

    public MultiSetting removeOption(String string) {
        if (string != null && this.options.remove(this.normalizeKey(string)) != null) {
            this.syncFromOptions();
        }

        return this;
    }

    public MultiSetting addOptionBind(String string, int i, BindMode bindMode) {
        return this.bindOption(string, i, bindMode, false);
    }

    public MultiSetting bindOption(String string, int i, BindMode bindMode, boolean flag) {
        BooleanSetting booleanSetting = this.getOption(string);
        if (booleanSetting == null) {
            return this;
        }

        booleanSetting.bind(i, bindMode, flag);
        return this;
    }

    public MultiSetting setOptions(String... strings) {
        this.options.clear();
        if (strings != null) {
            for (String string : strings) {
                this.addOption(string);
            }
        }

        this.syncFromOptions();
        return this;
    }

    public BooleanSetting getOption(String string) {
        return string == null ? null : this.options.get(this.normalizeKey(string));
    }

    public BooleanSetting getOption(int i) {
        if (i >= 0 && i < this.options.size()) {
            int i2 = 0;

            for (BooleanSetting booleanSetting : this.options.values()) {
                if (i2 == i) {
                    return booleanSetting;
                }

                i2++;
            }
        }

        return null;
    }

    public List<String> getOptionNames() {
        ArrayList arrayList = new ArrayList(this.options.size());

        for (BooleanSetting booleanSetting : this.options.values()) {
            arrayList.add(booleanSetting.getName());
        }

        return Collections.unmodifiableList(arrayList);
    }

    public List<String> getSelected() {
        return Collections.unmodifiableList(this.getValue());
    }

    public boolean isSelected(String string) {
        return string != null && this.containsIgnoreCase(this.getValue(), string);
    }

    public boolean has(String string) {
        return this.isSelected(string);
    }

    public boolean isSelected(int i) {
        BooleanSetting booleanSetting = this.getOption(i);
        return booleanSetting != null && this.isSelected(booleanSetting.getName());
    }

    public Collection<MultiSetting.Entry> getEntries() {
        ArrayList arrayList = new ArrayList(this.options.size());

        for (BooleanSetting booleanSetting : this.options.values()) {
            arrayList.add(new MultiSetting.Entry(booleanSetting));
        }

        return Collections.unmodifiableList(arrayList);
    }

    public List<MultiSetting.Entry> getSelectedEntries() {
        ArrayList arrayList = new ArrayList();

        for (MultiSetting.Entry entry : this.getEntries()) {
            if (this.has(entry.value().getName())) {
                arrayList.add(entry);
            }
        }

        return arrayList;
    }

    public boolean hasAnySelected() {
        return !this.getValue().isEmpty();
    }

    public boolean isAllSelected() {
        return !this.options.isEmpty() && this.getValue().size() == this.options.size();
    }

    public boolean isNoneSelected() {
        return !this.hasAnySelected();
    }

    public MultiSetting setSelected(String string, boolean flag) {
        BooleanSetting booleanSetting = this.getOption(string);
        if (booleanSetting == null) {
            return this;
        }

        ArrayList arrayList = new ArrayList<>(this.getDefaultValue());
        if (flag) {
            if (!this.containsIgnoreCase(arrayList, booleanSetting.getName())) {
                arrayList.add(booleanSetting.getName());
                this.setValue(arrayList);
                return this;
            }
        } else {
            this.removeIgnoreCase(arrayList, booleanSetting.getName());
        }

        this.setValue(arrayList);
        return this;
    }

    protected List<String> sanitizeValue(List<String> strings) {
        ArrayList arrayList = new ArrayList();
        if (this.options.isEmpty()) {
            return arrayList;
        }

        if (strings != null && !strings.isEmpty()) {
            for (BooleanSetting booleanSetting : this.options.values()) {
                if (this.containsIgnoreCase(strings, booleanSetting.getName())) {
                    arrayList.add(booleanSetting.getName());
                }
            }
        }

        if (this.requireOne && arrayList.isEmpty()) {
            arrayList.add(this.options.values().iterator().next().getName());
        }

        return arrayList;
    }

    @Override
    public String formatBindValue(ValueBind<List<String>> valueBind) {
        return this.formatSelection(this.getSelectionFor(valueBind));
    }

    @Override
    public float resolveHeight(float f) {
        this.rowHeight = Math.max(20.0F, f);
        return f;
    }

    @Override
    public float getBindRowHeight() {
        return 20.0F;
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        ValueBind valueBind = this.editingBind;
        float f2 = this.position.x;
        float f3 = this.position.y;
        float f4 = this.size.x;
        float f5 = this.size.y;
        boolean flag = valueBind != null;
        String string = this.formatSelection(this.getSelectionFor(valueBind));
        float f6 = Math.min(this.rowHeight, f5);
        float f7 = flag ? f4 - 8.0F : Math.min(f4 / 2.0F, this.getChipWidth(string));
        float f8 = flag ? f2 + 4.0F : f2 + f4 - f7;
        float f9 = f3 + f6 / 2.0F - 5.0F;
        int i3 = ColorUtil.multiplyAlpha(Theme.getTextColor(), this.getAlpha());
        if (!flag) {
            this.drawLabel(drawContext, f2, f3, f4, f6, f4 - f7 - 6.0F, this.isHovered(i, i2, this.position, this.size));
        }

        GuiRenderUtil.drawSurface(drawContext, f8, f9, f7, 10.0F, this.getAlpha(), 3.0F)
            .scrollingTextMiddle(Fonts.INTER_MEDIUM, string, 4.0F, 0.0F, f7 - 8.0F - 6.0F, i3, 5.5F, false);
        float f10 = flag ? this.bindDropdownAnimation.get() : this.dropdownAnimation.get();
        SettingWidgets.drawExpandArrow(drawContext, f8, f9, f7, 10.0F, f10, this.getAlpha());
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        ValueBind valueBind = this.editingBind;
        boolean flag2 = valueBind != null;
        boolean flag3 = this.isLeftButton(click.button());
        boolean flag4 = this.isRightButton(click.button());
        if (!flag3 && !flag4) {
            return false;
        }

        List list = this.getVisibleOptions();
        boolean flag5 = flag2 ? this.bindDropdownOpen : this.dropdownOpen;
        if (list.isEmpty()) {
            if (!flag5) {
                return false;
            }

            this.hideDropdown(flag2);
            return true;
        } else {
            boolean flag6 = this.isHovered(click.x(), click.y(), this.position.x, this.position.y, this.size.x, this.size.y);
            if (flag5) {
                if (flag3 && this.isInsideDropdown(click.x(), click.y())) {
                    this.dragVisited.clear();
                    this.dragging = this.toggleAt(click.x(), click.y(), valueBind, list);
                    this.lastDragX = click.x();
                    this.lastDragY = click.y();
                    return true;
                } else {
                    this.hideDropdown(flag2);
                    return true;
                }
            } else if ((flag3 || flag4) && flag6) {
                this.showDropdown(flag2);
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        if (this.dragging && this.isLeftButton(click.button())) {
            if (this.editingBind != null ? this.bindDropdownOpen : this.dropdownOpen) {
                List list = this.getVisibleOptions();
                double d3 = click.x() - this.lastDragX;
                double d4 = click.y() - this.lastDragY;
                int i = Math.max(1, (int)Math.ceil(Math.max(Math.abs(d3), Math.abs(d4)) / (4.0 * this.dropdownScale)));

                for (int i2 = 1; i2 <= i; i2++) {
                    double d5 = (double)i2 / i;
                    this.toggleAt(this.lastDragX + d3 * d5, this.lastDragY + d4 * d5, this.editingBind, list);
                }

                this.lastDragX = click.x();
                this.lastDragY = click.y();
                return true;
            } else {
                this.dragging = false;
                this.dragVisited.clear();
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean onMouseRelease(Click click) {
        if (!this.isLeftButton(click.button())) {
            return false;
        }

        boolean flag = this.dragging;
        this.dragging = false;
        this.dragVisited.clear();
        return flag;
    }

    @Override
    public void renderOverlay(DrawContext drawContext, int i, int i2, float f) {
        float f2 = this.animateDropdown(false);
        if (this.editingBind == null && !(f2 <= 0.001F)) {
            this.renderDropdown(drawContext, i, i2, null, f2);
        }
    }

    @Override
    public boolean isOverlayOpen() {
        return this.dropdownOpen || this.dropdownAnimation.get() > 0.001F;
    }

    public boolean isOverDropdown(double d, double d2) {
        return (this.dropdownOpen || this.dropdownAnimation.get() > 0.001F) && this.isInsideDropdown(d, d2);
    }

    public BooleanSetting getHoveredOption(double d, double d2) {
        if (this.dropdownOpen && this.editingBind == null && !(this.dropdownWidth <= 0.0F)) {
            List<BooleanSetting> booleanSettings = this.getVisibleOptions();
            if (booleanSettings.isEmpty()) {
                return null;
            }

            this.layoutItems(booleanSettings);
            double d3 = this.unscaleX(d);
            double d4 = this.unscaleY(d2);

            for (BooleanSetting booleanSetting : booleanSettings) {
                if (this.isHovered(d3, d4, booleanSetting.position, booleanSetting.size)) {
                    return booleanSetting;
                }
            }
        }

        return null;
    }

    public boolean openDropdown() {
        if (this.editingBind != null) {
            return false;
        } else if (this.dropdownOpen) {
            return true;
        } else {
            List list = this.getVisibleOptions();
            if (list.isEmpty()) {
                return false;
            } else if (!(this.size.x <= 0.0F) && !(this.size.y <= 0.0F)) {
                this.showDropdown(false);
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean isBindDropdownOpen() {
        return this.bindDropdownOpen || this.bindDropdownAnimation.get() > 0.001F;
    }

    @Override
    public void renderBindDropdown(DrawContext drawContext, int i, int i2, float f) {
        float f2 = this.animateDropdown(true);
        if (!(f2 <= 0.001F)) {
            this.renderDropdown(drawContext, i, i2, this.editingBind, f2);
        }
    }

    @Override
    public void resize(int i, int i2) {
        this.close();
        this.dropdownAnimation.forceValue(0.0);
        this.bindDropdownAnimation.forceValue(0.0);
        this.syncOptions();
    }

    @Override
    public void removed() {
        this.close();
        this.dropdownAnimation.forceValue(0.0);
        this.bindDropdownAnimation.forceValue(0.0);
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        if (keyInput.key() != 256 || !this.dropdownOpen && !this.bindDropdownOpen) {
            return false;
        }

        this.close();
        return true;
    }

    @Override
    public void close() {
        this.dropdownOpen = false;
        this.bindDropdownOpen = false;
        this.dragging = false;
        this.dragVisited.clear();
    }

    public void closeDropdown() {
        if (this.dropdownOpen || this.bindDropdownOpen || !(this.dropdownAnimation.get() <= 0.001F) || !(this.bindDropdownAnimation.get() <= 0.001F)) {
            this.close();
            this.dropdownAnimation.forceValue(0.0);
            this.bindDropdownAnimation.forceValue(0.0);
        }
    }

    @Override
    protected void onKey(int i, boolean flag) {
    }

    @Override
    protected List<Setting<?>> getChildren() {
        ArrayList arrayList = new ArrayList(this.options.size());

        for (BooleanSetting booleanSetting : this.options.values()) {
            arrayList.add(booleanSetting);
        }

        return arrayList;
    }

    private void renderDropdown(DrawContext drawContext, int i, int i2, ValueBind<List<String>> valueBind, float f) {
        List list = this.getVisibleOptions();
        if (!list.isEmpty()) {
            float f2 = this.position.y + Math.min(this.rowHeight, this.size.y) / 2.0F;
            float f3 = this.layoutItems(list);
            this.dropdownX = GuiRenderUtil.placeBeside(this.position.x, this.position.x + this.size.x, this.dropdownWidth, 4.0F);
            this.dropdownY = GuiRenderUtil.clampToScreenY(f2 - f3 / 2.0F, f3);
            this.layoutItems(list);
            this.dropdownHeight = f3;
            float f4 = this.getAlpha() * Math.min(f, 1.0F);
            float f5 = 0.85F + 0.15F * f;
            this.dropdownScale = f5;
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, this.dropdownX + this.dropdownWidth / 2.0F, this.dropdownY + f3 / 2.0F, f5);
            GuiRenderUtil.drawPopup(drawContext, this.dropdownX, this.dropdownY, this.dropdownWidth, f3, f4, 10.0F)
                .scissor(() -> this.renderDropdownItems(drawContext, i, i2, list, f));
            MatrixUtil.pop(drawContext);
        }
    }

    private void renderDropdownItems(DrawContext drawContext, int i, int i2, List<BooleanSetting> booleanSettings, float f) {
        float f2 = 14.0F + this.getIconColumnWidth(booleanSettings);

        for (BooleanSetting booleanSetting : booleanSettings) {
            booleanSetting.alphaAnimation.forceValue(this.alphaAnimation.get() * f);
            booleanSetting.valueAnimation.update();
            booleanSetting.valueAnimation.animate(booleanSetting.getValue() ? 1.0 : 0.0, 0.24, Easings.QUART_OUT);
            float f3 = booleanSetting.valueAnimation.get() * this.dropdownAnimation.get();
            int i3 = ColorUtil.setAlphaf(Theme.getTextColor(), (0.5F + 0.5F * f3) * this.dropdownAnimation.get() * this.getAlpha());
            boolean flag = this.isHovered(i, i2, booleanSetting.position, booleanSetting.size);
            booleanSetting.hoverAnimation.update();
            booleanSetting.hoverAnimation.animate(flag ? 1.0 : 0.0, 0.16, Easings.QUAD_OUT);
            GuiRenderUtil.drawHighlight(
                drawContext, booleanSetting.position.x, booleanSetting.position.y + 1.0F, booleanSetting.size.x, booleanSetting.size.y - 2.0F, f * booleanSetting.hoverAnimation.get(), Radius.all(4.0F)
            );
            RenderUtil renderUtil = new RenderUtil(drawContext, booleanSetting.position.x, booleanSetting.position.y, booleanSetting.size.x, booleanSetting.size.y);
            renderUtil.icon(Icon.CHECK, 4.0F, booleanSetting.size.y / 2.0F - 4.0F, 8.0F, Theme.withAlpha(Theme.getTextColor(), f3 * this.getAlpha()));
            float f4 = (0.5F + 0.5F * f3) * this.dropdownAnimation.get() * this.getAlpha();
            Setting.IconRenderer iconRenderer = booleanSetting.getIconRenderer();
            Icon icon2 = booleanSetting.getIcon();
            if (iconRenderer != null) {
                iconRenderer.render(drawContext, booleanSetting.position.x + 14.0F, booleanSetting.position.y + booleanSetting.size.y / 2.0F - 4.0F, 8.0F, f4);
            } else if (icon2 != null) {
                renderUtil.icon(icon2, 14.0F, booleanSetting.size.y / 2.0F - 4.0F, 8.0F, Theme.withAlpha(Theme.getPrimary(), f4));
            }

            renderUtil.scrollingTextMiddle(
                Fonts.INTER_MEDIUM, Language.translateOption(this.getName(), booleanSetting.getName()), f2, 0.0F, booleanSetting.size.x - f2, i3, 5.5F, flag
            );
        }
    }

    private float getIconColumnWidth(List<BooleanSetting> booleanSettings) {
        for (BooleanSetting booleanSetting : booleanSettings) {
            if (booleanSetting.getIcon() != null || booleanSetting.getIconRenderer() != null) {
                return 12.0F;
            }
        }

        return 0.0F;
    }

    private void showDropdown(boolean flag) {
        float f = 0.0F;
        List<BooleanSetting> booleanSettings = this.getVisibleOptions();

        for (BooleanSetting booleanSetting : booleanSettings) {
            float f2 = Fonts.INTER_MEDIUM.getWidth(Language.translateOption(this.getName(), booleanSetting.getName()), 5.5F);
            if (f2 > f) {
                f = f2;
            }
        }

        this.dropdownWidth = 28.0F + this.getIconColumnWidth(booleanSettings) + f + 6.0F;
        if (flag) {
            this.bindDropdownOpen = true;
        } else {
            this.dropdownOpen = true;
        }
    }

    private void hideDropdown(boolean flag) {
        this.dragging = false;
        this.dragVisited.clear();
        if (flag) {
            this.bindDropdownOpen = false;
        } else {
            this.dropdownOpen = false;
        }
    }

    private float animateDropdown(boolean flag) {
        Animation animation = flag ? this.bindDropdownAnimation : this.dropdownAnimation;
        boolean flag2 = flag ? this.bindDropdownOpen : this.dropdownOpen;
        animation.update();
        if (flag2) {
            animation.animate(1.0, 0.3F, Easings.BACK_OUT);
            return animation.get();
        } else {
            animation.animate(0.0, 0.2F, Easings.QUAD_IN);
            return animation.get();
        }
    }

    private boolean toggleAt(double d, double d2, ValueBind<List<String>> valueBind, List<BooleanSetting> booleanSettings) {
        List list = this.getSelectionFor(valueBind);
        this.layoutItems(booleanSettings);
        double d3 = this.unscaleX(d);
        double d4 = this.unscaleY(d2);

        for (BooleanSetting booleanSetting : booleanSettings) {
            if (this.isHovered(d3, d4, booleanSetting.position, booleanSetting.size)) {
                if (!this.dragVisited.add(booleanSetting.getName())) {
                    return true;
                }

                boolean flag = valueBind != null
                    ? this.containsIgnoreCase(list, booleanSetting.getName())
                    : this.containsIgnoreCase(this.getDefaultValue(), booleanSetting.getName());
                if (this.requireOne && flag && (valueBind != null ? list.size() : this.sanitizeValue(this.getDefaultValue()).size()) <= 1) {
                    return true;
                }

                booleanSetting.setValue(flag);
                booleanSetting.alphaAnimation.forceValue(this.alphaAnimation.get());
                booleanSetting.toggle();
                if (valueBind != null) {
                    this.setBindOptionSelected(valueBind, booleanSetting.getName(), Boolean.TRUE.equals(booleanSetting.getValue()));
                    return true;
                }

                this.setSelected(booleanSetting.getName(), Boolean.TRUE.equals(booleanSetting.getDefaultValue()));
                return true;
            }
        }

        return false;
    }

    private float layoutItems(List<BooleanSetting> booleanSettings) {
        float f = this.dropdownX + 6.0F;
        float f2 = this.dropdownY + 3.0F;
        float f3 = this.dropdownWidth - 12.0F;

        for (BooleanSetting booleanSetting : booleanSettings) {
            booleanSetting.position.set(f, f2);
            booleanSetting.size.set(f3, 14.0F);
            f2 += booleanSetting.size.y;
        }

        return f2 - this.dropdownY + 3.0F;
    }

    private boolean isInsideDropdown(double d, double d2) {
        return this.dropdownWidth <= 0.0F
            ? false
            : this.isHovered(
                this.unscaleX(d), this.unscaleY(d2), this.dropdownX, this.dropdownY, this.dropdownWidth, this.layoutItems(this.getVisibleOptions())
            );
    }

    private double unscaleX(double d) {
        float f = this.dropdownScale <= 0.0F ? 1.0F : this.dropdownScale;
        float f2 = this.dropdownX + this.dropdownWidth / 2.0F;
        return f2 + (d - f2) / f;
    }

    private double unscaleY(double d) {
        float f = this.dropdownScale <= 0.0F ? 1.0F : this.dropdownScale;
        float f2 = this.dropdownY + this.dropdownHeight / 2.0F;
        return f2 + (d - f2) / f;
    }

    private List<BooleanSetting> getVisibleOptions() {
        ArrayList arrayList = new ArrayList(this.options.size());

        for (BooleanSetting booleanSetting : this.options.values()) {
            if (booleanSetting.isVisible()) {
                arrayList.add(booleanSetting);
            }
        }

        return arrayList;
    }

    private String formatSelection(List<String> strings) {
        StringBuilder builder = new StringBuilder();

        for (String string : this.sanitizeValue(strings)) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }

            builder.append(Language.translateOption(this.getName(), string));
        }

        return builder.isEmpty() ? Language.translate("setting.none", "Ничего") : builder.toString();
    }

    private List<String> getSelectionFor(ValueBind<List<String>> valueBind) {
        return valueBind == null ? this.sanitizeValue(this.getValue()) : this.sanitizeValue((List<String>)valueBind.getValue());
    }

    private void setBindOptionSelected(ValueBind<List<String>> valueBind, String string, boolean flag) {
        if (valueBind != null && string != null && !string.isEmpty()) {
            ArrayList arrayList = new ArrayList<>(this.getSelectionFor(valueBind));
            if (flag) {
                if (!this.containsIgnoreCase(arrayList, string)) {
                    arrayList.add(string);
                    super.setBindValue(valueBind, arrayList);
                    this.syncOptions();
                    return;
                }
            } else {
                this.removeIgnoreCase(arrayList, string);
            }

            super.setBindValue(valueBind, arrayList);
            this.syncOptions();
        }
    }

    private void syncFromOptions() {
        ArrayList arrayList = new ArrayList(this.options.size());

        for (BooleanSetting booleanSetting : this.options.values()) {
            if (Boolean.TRUE.equals(booleanSetting.getDefaultValue())) {
                arrayList.add(booleanSetting.getName());
            }
        }

        super.setValue(arrayList);
        this.syncOptions();
    }

    private void syncOptions() {
        List list = this.sanitizeValue(this.getDefaultValue());
        this.syncing = true;

        for (BooleanSetting booleanSetting : this.options.values()) {
            booleanSetting.setValue(this.containsIgnoreCase(list, booleanSetting.getName()));
        }

        this.syncing = false;
    }

    private void handleOptionChange(String string) {
        if (!this.syncing && string != null && !string.isEmpty()) {
            BooleanSetting booleanSetting = this.getOption(string);
            if (booleanSetting != null) {
                boolean flag = Boolean.TRUE.equals(booleanSetting.getValue());
                boolean flag2 = Boolean.TRUE.equals(booleanSetting.getDefaultValue());
                if (flag != flag2) {
                    ArrayList arrayList = new ArrayList(this.options.size());

                    for (BooleanSetting booleanSetting2 : this.options.values()) {
                        if (Boolean.TRUE.equals(booleanSetting2.getValue())) {
                            arrayList.add(booleanSetting2.getName());
                        }
                    }

                    this.setValueInternal(this.sanitizeValue(arrayList));
                } else {
                    this.setSelected(booleanSetting.getName(), flag2);
                }
            }
        }
    }

    private String normalizeKey(String string) {
        return string.toLowerCase(Locale.ROOT);
    }

    private boolean containsIgnoreCase(List<String> strings, String string) {
        if (string != null && strings != null && !strings.isEmpty()) {
            for (String string2 : strings) {
                if (string2 != null && string2.equalsIgnoreCase(string)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void removeIgnoreCase(List<String> strings, String string) {
        if (string != null && strings != null && !strings.isEmpty()) {
            strings.removeIf(string2 -> string2 != null && string2.equalsIgnoreCase(string));
        }
    }

    private float getChipWidth(String string) {
        return Fonts.INTER_MEDIUM.getWidth(string, 5.5F) + 9.0F + 6.0F;
    }

    @Override
    protected float getControlWidth() {
        return this.getChipWidth(this.formatSelection(this.getValue()));
    }

    public record Entry(BooleanSetting value) {
    }
}
