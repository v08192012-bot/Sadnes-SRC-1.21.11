package tech.sadnes.client.setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
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

public final class ModeSetting extends ValueSetting<String> {
    private static final float MIN_HEIGHT = 20.0F;
    private static final float ITEM_ICON_WIDTH = 10.0F;
    private static final float PADDING_X = 6.0F;
    private static final float PADDING_Y = 3.0F;
    private final List<String> modes = new ArrayList<>();
    private Function<String, ItemStack> itemIconProvider;
    private final Map<String, BooleanSetting> modeToggles = new LinkedHashMap<>();
    private final Animation dropdownAnimation = new Animation();
    private final Animation bindDropdownAnimation = new Animation();
    private boolean dropdownOpen;
    private boolean bindDropdownOpen;
    private boolean syncing;
    private float rowHeight = 20.0F;
    private float dropdownX;
    private float dropdownY;
    private float dropdownWidth;
    private float dropdownHeight;
    private float dropdownScale = 1.0F;

    public ModeSetting(String string, String... strings) {
        this(string, strings != null && strings.length > 0 ? strings[0] : "", strings);
    }

    public ModeSetting(String string, String string2, String... strings) {
        super(string, string2);
        if (strings != null) {
            for (String string3 : strings) {
                this.addMode(string3);
            }
        }

        if (this.modes.isEmpty() && string2 != null && !string2.isEmpty()) {
            this.addMode(string2);
        }

        this.setValue(string2);
    }

    public ModeSetting(GroupSetting groupSetting, String string, String... strings) {
        this(string, strings);
        if (groupSetting != null) {
            groupSetting.onInput(this);
        }
    }

    public ModeSetting(GroupSetting groupSetting, String string, String string2, String... strings) {
        this(string, string2, strings);
        if (groupSetting != null) {
            groupSetting.onInput(this);
        }
    }

    public ModeSetting setValue(String string) {
        super.setValue(string);
        this.syncToggles(this.sanitizeValue(this.getDefaultValue()));
        return this;
    }

    public ModeSetting setVisibleWhen(Supplier<Boolean> supplier) {
        super.setVisibleWhen(supplier);
        return this;
    }

    public ModeSetting setOnChange(Runnable runnable) {
        super.setOnChange(runnable);
        return this;
    }

    public ModeSetting setOnVisibilityChange(Runnable runnable) {
        super.setOnVisibilityChange(runnable);
        return this;
    }

    public ModeSetting addMode(String string) {
        if (string == null || string.isEmpty()) {
            return this;
        } else if (this.hasMode(string)) {
            return this;
        } else {
            this.modes.add(string);
            this.modeToggles.put(this.normalizeKey(string), this.createToggle(string));
            if (this.modes.size() == 1) {
                this.setValue(string);
                return this;
            } else {
                this.syncToggles(this.sanitizeValue(this.getDefaultValue()));
                return this;
            }
        }
    }

    public ModeSetting setModes(Collection<String> strings) {
        this.modes.clear();
        this.modeToggles.clear();
        if (strings != null) {
            for (String string : strings) {
                this.addMode(string);
            }
        }

        this.setValue(this.getValue());
        return this;
    }

    public ModeSetting setModes(String[] strings, Collection<String> strings2) {
        ArrayList arrayList = new ArrayList<>(Arrays.asList(strings));
        arrayList.addAll(strings2);
        String string = this.getValue();
        this.setModes(arrayList);
        return string != null && arrayList.contains(string) ? this.setValue(string) : this.setValue(strings[0]);
    }

    public ModeSetting setModes(String... strings) {
        this.modes.clear();
        this.modeToggles.clear();
        if (strings != null) {
            for (String string : strings) {
                this.addMode(string);
            }
        }

        this.setValue(this.getValue());
        return this;
    }

    public ModeSetting setItemIconProvider(Function<String, ItemStack> function) {
        this.itemIconProvider = function;
        return this;
    }

    public ModeSetting select(String string) {
        return this.setValue(string);
    }

    public String getMode() {
        return this.getValue();
    }

    public List<String> getModes() {
        return Collections.unmodifiableList(this.modes);
    }

    public boolean is(String string) {
        return string != null && this.getValue().equalsIgnoreCase(string);
    }

    public boolean isNot(String string) {
        return string != null && !this.getValue().equalsIgnoreCase(string);
    }

    public boolean modeContains(String string) {
        return string != null && this.getValue().contains(string);
    }

    public boolean modeContainsIgnoreCase(String string) {
        return string != null && this.getValue().toLowerCase(Locale.ROOT).contains(string.toLowerCase(Locale.ROOT));
    }

    public ModeSetting mouseDragged(int i, BindMode bindMode2, String string) {
        super.mouseDragged(i, bindMode2, string);
        return this;
    }

    public ModeSetting addBind(ValueBind<String> valueBind) {
        super.addBind(valueBind);
        return this;
    }

    public ModeSetting addModeBind(String string, int i, BindMode bindMode2) {
        return this.bindMode(string, i, bindMode2, true);
    }

    public ModeSetting bindMode(String string, int i, BindMode bindMode2, boolean flag) {
        BooleanSetting booleanSetting = this.getModeToggle(string);
        if (booleanSetting == null) {
            return this;
        }

        booleanSetting.bind(i, bindMode2, flag);
        return this;
    }

    public BooleanSetting getModeToggle(String string) {
        return string == null ? null : this.modeToggles.get(this.normalizeKey(string));
    }

    protected String sanitizeValue(String string) {
        if (this.modes.isEmpty()) {
            return string != null ? string : "";
        }

        if (string != null && !string.isEmpty()) {
            for (String string2 : this.modes) {
                if (string2.equalsIgnoreCase(string)) {
                    return string2;
                }
            }
        }

        return this.modes.get(0);
    }

    @Override
    public String formatBindValue(ValueBind<String> valueBind) {
        return Language.translateOption(this.getName(), this.getModeFor(valueBind));
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
        String string = this.getModeFor(valueBind);
        float f6 = Math.min(this.rowHeight, f5);
        float f7 = this.itemIconProvider == null ? 0.0F : 10.0F;
        String string2 = Language.translateOption(this.getName(), string);
        float f8 = flag ? f4 - 8.0F : Math.min(f4 / 2.0F, this.getChipWidth(string2));
        float f9 = flag ? f2 + 4.0F : f2 + f4 - f8;
        float f10 = f3 + f6 / 2.0F - 5.0F;
        int i3 = ColorUtil.multiplyAlpha(Theme.getTextColor(), this.getAlpha());
        if (!flag) {
            this.drawLabel(drawContext, f2, f3, f4, f6, f4 - f8 - 6.0F, this.isHovered(i, i2, this.position, this.size));
        }

        GuiRenderUtil.drawSurface(drawContext, f9, f10, f8, 10.0F, this.getAlpha(), 3.0F)
            .scrollingTextMiddle(Fonts.INTER_MEDIUM, string2, 4.0F + f7, 0.0F, f8 - 8.0F - f7 - 6.0F, i3, 5.5F, false);
        this.renderItemIcon(drawContext, string, f9 + 2.0F, f10 + 5.0F - 4.0F, 8.0F, this.getAlpha());
        float f11 = flag ? this.bindDropdownAnimation.get() : this.dropdownAnimation.get();
        SettingWidgets.drawExpandArrow(drawContext, f9, f10, f8, 10.0F, f11, this.getAlpha());
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

        List list = this.getVisibleToggles();
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
                    this.handleBindDropdownClick(click, valueBind, list);
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

    public BooleanSetting getHoveredToggle(double d, double d2) {
        if (this.dropdownOpen && this.editingBind == null && !(this.dropdownWidth <= 0.0F)) {
            List<BooleanSetting> booleanSettings = this.getVisibleToggles();
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
            List list = this.getVisibleToggles();
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
        this.dropdownOpen = false;
        this.bindDropdownOpen = false;
        this.dropdownAnimation.forceValue(0.0);
        this.bindDropdownAnimation.forceValue(0.0);
        this.syncToggles(this.sanitizeValue(this.getDefaultValue()));
    }

    @Override
    public void removed() {
        this.dropdownOpen = false;
        this.bindDropdownOpen = false;
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
    }

    public void closeDropdown() {
        if (this.dropdownOpen || this.bindDropdownOpen || !(this.dropdownAnimation.get() <= 0.001F) || !(this.bindDropdownAnimation.get() <= 0.001F)) {
            this.dropdownOpen = false;
            this.bindDropdownOpen = false;
            this.dropdownAnimation.forceValue(0.0);
            this.bindDropdownAnimation.forceValue(0.0);
        }
    }

    @Override
    protected List<Setting<?>> getChildren() {
        ArrayList arrayList = new ArrayList(this.modeToggles.size());

        for (BooleanSetting booleanSetting : this.modeToggles.values()) {
            arrayList.add(booleanSetting);
        }

        return arrayList;
    }

    private void renderDropdown(DrawContext drawContext, int i, int i2, ValueBind<String> valueBind, float f) {
        List list = this.getVisibleToggles();
        if (!list.isEmpty()) {
            this.syncToggles(this.getModeFor(valueBind));
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
        for (BooleanSetting booleanSetting : booleanSettings) {
            booleanSetting.alphaAnimation.forceValue(this.alphaAnimation.get() * f);
            booleanSetting.valueAnimation.update();
            booleanSetting.valueAnimation.animate(booleanSetting.getValue() ? 1.0 : 0.0, 0.24, Easings.QUART_OUT);
            float f2 = booleanSetting.valueAnimation.get() * this.dropdownAnimation.get();
            int i3 = ColorUtil.setAlphaf(Theme.getTextColor(), (0.5F + 0.5F * f2) * this.dropdownAnimation.get() * this.getAlpha());
            boolean flag = this.isHovered(i, i2, booleanSetting.position, booleanSetting.size);
            booleanSetting.hoverAnimation.update();
            booleanSetting.hoverAnimation.animate(flag ? 1.0 : 0.0, 0.16, Easings.QUAD_OUT);
            GuiRenderUtil.drawHighlight(
                drawContext, booleanSetting.position.x, booleanSetting.position.y + 1.0F, booleanSetting.size.x, booleanSetting.size.y - 2.0F, f * booleanSetting.hoverAnimation.get(), Radius.all(4.0F)
            );
            RenderUtil renderUtil = new RenderUtil(drawContext, booleanSetting.position.x, booleanSetting.position.y, booleanSetting.size.x, booleanSetting.size.y);
            renderUtil.icon(Icon.CHECK, 4.0F, booleanSetting.size.y / 2.0F - 4.0F, 8.0F, Theme.withAlpha(Theme.getTextColor(), f2 * this.getAlpha()));
            float f3 = this.itemIconProvider == null ? 0.0F : 12.0F;
            this.renderItemIcon(
                drawContext,
                booleanSetting.getName(),
                booleanSetting.position.x + 14.0F,
                booleanSetting.position.y + booleanSetting.size.y / 2.0F - 5.0F,
                10.0F,
                this.dropdownAnimation.get() * this.getAlpha()
            );
            renderUtil.scrollingTextMiddle(
                Fonts.INTER_MEDIUM,
                Language.translateOption(this.getName(), booleanSetting.getName()),
                14.0F + f3,
                0.0F,
                booleanSetting.size.x - 14.0F - f3,
                i3,
                5.5F,
                flag
            );
        }
    }

    private void renderItemIcon(DrawContext drawContext, String string, float f, float f2, float f3, float f4) {
        if (this.itemIconProvider != null) {
            ItemStack itemStack = this.itemIconProvider.apply(string);
            if (itemStack != null && !itemStack.isEmpty()) {
                float f5 = f + f3 / 2.0F;
                float f6 = f2 + f3 / 2.0F;
                MatrixUtil.push(drawContext);
                MatrixUtil.scale(drawContext, f5, f6, f3 / 16.0F);
                GuiRenderUtil.drawItem(drawContext, itemStack, f5 - 8.0F, f6 - 8.0F, f4);
                MatrixUtil.pop(drawContext);
            }
        }
    }

    private void showDropdown(boolean flag) {
        float f = 0.0F;

        for (BooleanSetting booleanSetting : this.getVisibleToggles()) {
            float f2 = Fonts.INTER_MEDIUM.getWidth(Language.translateOption(this.getName(), booleanSetting.getName()), 5.5F);
            if (f2 > f) {
                f = f2;
            }
        }

        this.dropdownWidth = 28.0F + f + 6.0F + (this.itemIconProvider == null ? 0.0F : 12.0F);
        if (flag) {
            this.bindDropdownOpen = true;
        } else {
            this.dropdownOpen = true;
        }
    }

    private void hideDropdown(boolean flag) {
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

    private void handleBindDropdownClick(Click click, ValueBind<String> valueBind, List<BooleanSetting> booleanSettings) {
        String string = this.getModeFor(valueBind);
        this.layoutItems(booleanSettings);
        double d = this.unscaleX(click.x());
        double d2 = this.unscaleY(click.y());

        for (BooleanSetting booleanSetting : booleanSettings) {
            if (this.isHovered(d, d2, booleanSetting.position, booleanSetting.size)) {
                boolean flag = string.equalsIgnoreCase(booleanSetting.getName());
                booleanSetting.setValue(flag);
                booleanSetting.alphaAnimation.forceValue(this.alphaAnimation.get());
                booleanSetting.toggle();
                boolean flag2 = Boolean.TRUE.equals(booleanSetting.getValue());
                if (valueBind != null) {
                    this.toggleBindMode(valueBind, booleanSetting.getName(), flag2);
                    return;
                }

                this.applyToggle(booleanSetting.getName(), flag2);
                break;
            }
        }
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
                this.unscaleX(d), this.unscaleY(d2), this.dropdownX, this.dropdownY, this.dropdownWidth, this.layoutItems(this.getVisibleToggles())
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

    private List<BooleanSetting> getVisibleToggles() {
        ArrayList arrayList = new ArrayList(this.modeToggles.size());

        for (BooleanSetting booleanSetting : this.modeToggles.values()) {
            if (booleanSetting.isVisible()) {
                arrayList.add(booleanSetting);
            }
        }

        return arrayList;
    }

    private void applyToggle(String string, boolean flag) {
        if (string != null && !string.isEmpty()) {
            if (flag) {
                this.setValue(string);
            } else {
                String string2 = this.sanitizeValue(this.getDefaultValue());
                if (!string2.equalsIgnoreCase(string)) {
                    this.setValue(string2);
                } else if (this.sanitizeValue(this.getValue()).equalsIgnoreCase(string)) {
                    this.setValue(this.findOtherMode(string));
                }
            }
        }
    }

    private void toggleBindMode(ValueBind<String> valueBind, String string, boolean flag) {
        if (valueBind != null && string != null && !string.isEmpty()) {
            if (flag) {
                super.setBindValue(valueBind, string);
                this.syncToggles(this.getModeFor(valueBind));
            } else {
                String string2 = this.getModeFor(valueBind);
                if (string2.equalsIgnoreCase(string)) {
                    super.setBindValue(valueBind, this.findOtherMode(string));
                    this.syncToggles(this.getModeFor(valueBind));
                }
            }
        }
    }

    private void syncToggles(String string) {
        this.syncing = true;

        for (BooleanSetting booleanSetting : this.modeToggles.values()) {
            booleanSetting.setValue(booleanSetting.getName().equalsIgnoreCase(string));
        }

        this.syncing = false;
    }

    private void handleToggleChange(String string) {
        if (!this.syncing && string != null && !string.isEmpty()) {
            BooleanSetting booleanSetting = this.getModeToggle(string);
            if (booleanSetting != null) {
                boolean flag = Boolean.TRUE.equals(booleanSetting.getValue());
                boolean flag2 = Boolean.TRUE.equals(booleanSetting.getDefaultValue());
                if (flag == flag2) {
                    this.applyToggle(booleanSetting.getName(), flag2);
                } else if (flag) {
                    String string2 = booleanSetting.getName();
                    this.setValueInternal(this.sanitizeValue(string2));
                } else {
                    String string3 = this.findOtherMode(booleanSetting.getName());
                    this.setValueInternal(this.sanitizeValue(string3));
                }
            }
        }
    }

    private BooleanSetting createToggle(String string) {
        return BooleanSetting.of(string, false).setOnChange(() -> this.handleToggleChange(string));
    }

    private String findOtherMode(String string) {
        if (this.modes.isEmpty()) {
            return "";
        }

        for (String string2 : this.modes) {
            if (!string2.equalsIgnoreCase(string)) {
                return string2;
            }
        }

        return this.modes.get(0);
    }

    private boolean hasMode(String string) {
        if (string != null && !string.isEmpty()) {
            for (String string2 : this.modes) {
                if (string2.equalsIgnoreCase(string)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String getModeFor(ValueBind<String> valueBind) {
        return valueBind == null ? this.sanitizeValue(this.getValue()) : this.sanitizeValue((String)valueBind.getValue());
    }

    private String normalizeKey(String string) {
        return string.toLowerCase(Locale.ROOT);
    }

    private float getChipWidth(String string) {
        float f = this.itemIconProvider == null ? 0.0F : 10.0F;
        return Fonts.INTER_MEDIUM.getWidth(string, 5.5F) + 9.0F + f + 6.0F;
    }

    @Override
    protected float getControlWidth() {
        return this.getChipWidth(Language.translateOption(this.getName(), this.getValue()));
    }
}
