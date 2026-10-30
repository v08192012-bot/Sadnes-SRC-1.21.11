package tech.sadnes.client.ui.clickgui.panel;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import org.joml.Vector2f;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.ValueBind;
import tech.sadnes.client.feature.impl.misc.ServerAssist;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.VerticalFadeMask;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.server.DonateItem;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.SettingWidgets;
import tech.sadnes.client.ui.clickgui.component.Component;
import tech.sadnes.client.ui.clickgui.component.SmoothScrollbar;
import tech.sadnes.client.ui.clickgui.component.TextFieldComponent;
import tech.sadnes.client.util.Key;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

public final class ServerAssistPanel extends Component {
    private static final float PADDING = 8.0F;
    private static final float HEADER_HEIGHT = 10.0F;
    private static final float CLOSE_ICON_SIZE = 10.0F;
    private static final float SERVER_TAB_HEIGHT = 15.0F;
    private static final float POPUP_PADDING = 3.0F;
    private static final float SIDEBAR_WIDTH = 104.0F;
    private static final float GAP = 6.0F;
    private static final float SECTION_LABEL_HEIGHT = 11.0F;
    private static final float HEADER_MARGIN = 5.0F;
    private static final float SELECTOR_HEIGHT = 13.0F;
    private static final float SETTING_ROW_HEIGHT = 13.0F;
    private static final float OPTION_HEIGHT = 14.0F;
    private static final int GRID_COLUMNS = 2;
    private static final float CARD_GAP = 4.0F;
    private static final float CARD_HEIGHT = 64.0F;
    private static final float CARD_RADIUS = 8.0F;
    private static final float SEARCH_HEIGHT = 17.0F;
    private static final float SEPARATOR_HEIGHT = 9.0F;
    private static final float SEARCH_GAP = 6.0F;
    private static final float FADE_HEIGHT = 14.0F;
    private static final String[] SERVERS = new String[]{"funtime", "holyworld", "reallyworld"};
    private final Animation openAnimation = new Animation();
    private final Animation contentAnimation = new Animation();
    private final Animation closeHoverAnimation = new Animation();
    private final Animation searchHoverAnimation = new Animation();
    private final Animation resetHoverAnimation = new Animation();
    private final Animation serverSelectHoverAnimation = new Animation();
    private float serverSelectX;
    private float serverSelectY;
    private float serverSelectWidth;
    private ModeSetting dropdownSetting;
    private boolean dropdownOpen;
    private Function<String, String> dropdownLabelProvider;
    private Function<String, ItemStack> dropdownItemProvider;
    private final Animation dropdownAnimation = new Animation();
    private final Map<String, Animation> dropdownHoverAnimations = new HashMap<>();
    private float dropdownX;
    private float dropdownY;
    private float dropdownWidth;
    private final SmoothScrollbar scroll = new SmoothScrollbar();
    private float resetButtonX;
    private final TextFieldComponent searchField = new TextFieldComponent(Fonts.INTER_MEDIUM, 6.0F, "serverassist.panel.search", false, false);
    private String lastSearchText = "";
    private String tooltipText;
    private float tooltipX;
    private float tooltipY;
    private ServerAssist serverAssist;
    private boolean closing;
    private String selectedServer = "funtime";
    private KeybindSetting bindingSetting;
    private final List<Integer> pressedKeys = new ArrayList<>();
    private final Map<DonateItem, ServerAssistPanel.ItemCard> itemCards = new EnumMap<>(DonateItem.class);
    private final Map<DonateItem, ItemStack> itemStacks = new EnumMap<>(DonateItem.class);
    private final Map<DonateItem, List<String>> descriptionLines = new EnumMap<>(DonateItem.class);
    private final Map<String, Animation> serverSelectAnimations = new HashMap<>();
    private final Map<String, Animation> serverHoverAnimations = new HashMap<>();
    private final Map<String, Animation> toggleAnimations = new HashMap<>();
    private final Map<String, Animation> rowVisibilityAnimations = new HashMap<>();
    private final List<ServerAssistPanel.SettingRow> settingRows = new ArrayList<>();
    private float leftX;
    private float innerWidth;
    private float closeButtonX;
    private float closeButtonY;
    private float serverListY;
    private float searchY;
    private float contentX;
    private float contentWidth;
    private float gridY;
    private float gridHeight;
    private float cardWidth;

    public boolean isOpen() {
        return this.serverAssist != null && !this.closing;
    }

    public boolean isVisible() {
        return this.openAnimation.get() > 0.001;
    }

    public boolean isMouseOver(double d, double d2) {
        return this.isVisible() && this.isHovered(d, d2, this.position.x, this.position.y, this.size.x, this.size.y);
    }

    public void open(ServerAssist serverAssist2) {
        this.serverAssist = serverAssist2;
        this.closing = false;
        serverAssist2.normalizeBinds();
        this.cancelBinding();
        String string = serverAssist2.getActiveServer();
        this.selectedServer = string.isEmpty() ? "funtime" : string;
        this.clearSearch();
        this.itemCards.clear();
        this.descriptionLines.clear();
        this.serverSelectAnimations.clear();
        this.serverHoverAnimations.clear();
        this.toggleAnimations.clear();
        this.rowVisibilityAnimations.clear();
        this.scroll.reset();
        this.contentAnimation.forceValue(0.0);
        this.contentAnimation.animate(1.0, 0.3, Easings.QUAD_OUT);
        this.openAnimation.animate(1.0, 0.35, Easings.QUART_OUT);
    }

    @Override
    public void close() {
        if (this.serverAssist != null && !this.closing) {
            this.closing = true;
            this.cancelBinding();
            this.closeDropdown();
            this.searchField.setFocused(false);
            this.openAnimation.animate(0.0, 0.22, Easings.QUAD_IN);
        }
    }

    public void reset() {
        this.closeDropdown();
        this.serverAssist = null;
        this.closing = false;
        this.cancelBinding();
        this.searchField.setFocused(false);
        this.openAnimation.forceValue(0.0);
    }

    private void selectServer(String string) {
        if (!string.equals(this.selectedServer)) {
            this.selectedServer = string;
            this.cancelBinding();
            this.closeDropdown();
            this.clearSearch();
            this.scroll.reset();
            this.contentAnimation.forceValue(0.0);
            this.contentAnimation.animate(1.0, 0.3, Easings.QUAD_OUT);
        }
    }

    private void clearSearch() {
        this.searchField.setText("");
        this.searchField.setCursor(0);
        this.searchField.setFocused(false);
        this.searchField.clearSelection();
        this.lastSearchText = "";
    }

    private ServerAssistPanel.ItemCard getCard(DonateItem donateItem) {
        return this.itemCards.computeIfAbsent(donateItem, ServerAssistPanel.ItemCard::new);
    }

    private ItemStack getItemStack(DonateItem donateItem) {
        return this.itemStacks.computeIfAbsent(donateItem, DonateItem::createStack);
    }

    public void render(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4, float f5) {
        this.openAnimation.update();
        this.contentAnimation.update();
        if (this.closing && this.openAnimation.isFinished() && this.openAnimation.get() <= 0.001) {
            this.serverAssist = null;
            this.closing = false;
            this.cancelBinding();
        }

        if (this.serverAssist != null && !(this.openAnimation.get() <= 0.001)) {
            this.position.set(f2, f3);
            this.size.set(f4, f5);
            float f6 = (float)Math.min(this.openAnimation.get(), 1.0);
            float f7 = f6 * this.getAlpha();
            RenderUtil renderUtil = new RenderUtil(drawContext, f2, f3, f4, f5);
            renderUtil.blur(f7, Radius.smoothAll(20.0F), RenderUtil.BlurQuality.LOW, true);
            renderUtil.rect(Theme.withAlpha(Theme.getBackground(), 0.4F), f7, Radius.smoothAll(20.0F));
            this.leftX = f2 + 8.0F;
            this.innerWidth = f4 - 16.0F;
            this.contentX = this.leftX + 104.0F + 6.0F;
            this.contentWidth = this.leftX + this.innerWidth - this.contentX;
            float f8 = f3 + 6.0F;
            this.closeButtonX = f2 + f4 - 5.0F - 22.0F + 6.0F;
            this.closeButtonY = f3 + 4.0F + 6.0F;
            this.searchY = f8 + 10.0F + 6.0F;
            this.serverListY = this.searchY + 10.0F;
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f2 + f4 / 2.0F, f3 + f5 / 2.0F, 0.96F + 0.04F * f6);
            this.renderHeader(drawContext, f8, i, i2, f7);
            this.renderSidebar(drawContext, i, i2, f7);
            this.renderSearchBar(drawContext, i, i2, f7);
            this.gridY = this.searchY + 17.0F + 5.0F;
            this.gridHeight = f3 + f5 - 8.0F - this.gridY;
            this.cardWidth = (this.contentWidth - 6.0F - 4.0F) / 2.0F;
            this.renderItemGrid(drawContext, i, i2, f7);
            if (this.tooltipText != null) {
                this.renderTooltip(drawContext, f7);
                this.tooltipText = null;
            }

            this.renderDropdown(drawContext, i, i2, f7);
            MatrixUtil.pop(drawContext);
        }
    }

    public void renderOverlay(DrawContext drawContext, int i, int i2, float f) {
    }

    private void renderHeader(DrawContext drawContext, float f, int i, int i2, float f2) {
        int i3 = ColorUtil.multiplyAlpha(Theme.getPrimary(), f2);
        new RenderUtil(drawContext, this.leftX, f, this.innerWidth - 20.0F, 10.0F)
            .icon(Icon.SERVER, 2.0F, 1.5F, 7.0F, i3)
            .textMiddleLeft(Fonts.INTER_MEDIUM, Language.translate("serverassist.panel.title", "Помощник сервера"), 12.0F, 0.0F, i3, 6.0F);
        this.renderIconButton(drawContext, this.closeButtonX - 3.0F, this.closeButtonY - 3.0F, 16.0F, Icon.CLOSE, 10.0F, this.closeHoverAnimation, i, i2, f2);
        this.renderServerSelector(drawContext, f, i, i2, f2);
    }

    private void renderServerSelector(DrawContext drawContext, float f, int i, int i2, float f2) {
        this.serverSelectX = this.contentX;
        this.serverSelectY = f + 5.0F - 6.5F;
        this.serverSelectWidth = this.closeButtonX - 9.0F - this.serverSelectX;
        this.serverSelectHoverAnimation.update();
        this.serverSelectHoverAnimation
            .animate(this.isHovered(i, i2, this.serverSelectX, this.serverSelectY, this.serverSelectWidth, 13.0) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        GuiRenderUtil.drawOutlined(
                drawContext,
                this.serverSelectX,
                this.serverSelectY,
                this.serverSelectWidth,
                13.0F,
                f2 * (0.75F + 0.2F * this.serverSelectHoverAnimation.get()),
                4.3333335F
            )
            .scrollingTextMiddle(
                Fonts.INTER_MEDIUM,
                this.translateServerName(this.serverAssist.getServer().getValue()),
                6.0F,
                0.0F,
                this.serverSelectWidth - 20.0F,
                ColorUtil.multiplyAlpha(Theme.getTextColor(), f2),
                6.0F
            );
        RenderUtil.drawChevron(
            drawContext,
            this.serverSelectX + this.serverSelectWidth - 12.0F,
            this.serverSelectY + 6.5F - 4.5F,
            9.0F,
            90.0F + 180.0F * this.getDropdownProgress(this.serverAssist.getServer()),
            ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f2)
        );
    }

    private String translateServerName(String string) {
        return Language.translateOption(this.serverAssist.getServer().getName(), string);
    }

    private float getDropdownProgress(ModeSetting modeSetting) {
        return this.dropdownSetting == modeSetting ? (float)Math.min(this.dropdownAnimation.get(), 1.0) : 0.0F;
    }

    private void openDropdown(ModeSetting modeSetting, Function<String, String> function, Function<String, ItemStack> function2, float f, float f2, float f3, float f4) {
        this.dropdownSetting = modeSetting;
        this.dropdownOpen = true;
        this.dropdownLabelProvider = function;
        this.dropdownItemProvider = function2;
        this.dropdownHoverAnimations.clear();
        float f5 = function2 != null ? 28.0F : 16.0F;
        float f6 = f3;

        for (String string : modeSetting.getModes()) {
            float f7 = f5 + 6.0F + Fonts.INTER_MEDIUM.getWidth((String)function.apply(string), 6.0F);
            if (f7 > f6) {
                f6 = f7;
            }
        }

        this.dropdownWidth = Math.min(f6, this.innerWidth);
        this.dropdownX = Math.min(f, this.leftX + this.innerWidth - this.dropdownWidth);
        float f8 = this.getDropdownHeight(modeSetting);
        float f9 = f2 + f4 + 3.0F;
        this.dropdownY = f9 + f8 <= this.position.y + this.size.y - 8.0F ? f9 : f2 - 3.0F - f8;
        this.dropdownY = Math.max(this.dropdownY, this.position.y + 8.0F);
    }

    private float getDropdownHeight(ModeSetting modeSetting) {
        return modeSetting.getModes().size() * 14.0F + 6.0F;
    }

    private void closeDropdown() {
        this.dropdownOpen = false;
        this.dropdownAnimation.forceValue(0.0);
        this.dropdownSetting = null;
    }

    private void renderDropdown(DrawContext drawContext, int i, int i2, float f) {
        this.dropdownAnimation.update();
        this.dropdownAnimation.animate(this.dropdownOpen ? 1.0 : 0.0, this.dropdownOpen ? 0.3 : 0.2, this.dropdownOpen ? Easings.BACK_OUT : Easings.QUAD_IN);
        if (this.dropdownSetting != null) {
            float f2 = (float)Math.min(this.dropdownAnimation.get(), 1.0);
            if (!this.dropdownOpen && f2 <= 0.001F) {
                this.dropdownSetting = null;
            } else {
                List list = this.dropdownSetting.getModes();
                float f3 = this.getDropdownHeight(this.dropdownSetting);
                float f4 = f * f2;
                float f5 = this.dropdownItemProvider != null ? 28.0F : 16.0F;
                MatrixUtil.push(drawContext);
                MatrixUtil.scale(drawContext, this.dropdownX + this.dropdownWidth / 2.0F, this.dropdownY + f3 / 2.0F, 0.92F + 0.08F * f2);
                GuiRenderUtil.drawPopup(drawContext, this.dropdownX, this.dropdownY, this.dropdownWidth, f3, f4, 8.0F);
                float f6 = this.dropdownX + 3.0F;
                float f7 = this.dropdownWidth - 6.0F;
                float f8 = this.dropdownY + 3.0F;
                int i3 = list.size();

                for (int i4 = 0; i4 < i3; i4++) {
                    String string = (String)list.get(i4);
                    boolean flag = string.equalsIgnoreCase(this.dropdownSetting.getValue());
                    Animation animation = this.dropdownHoverAnimations.computeIfAbsent(string, string2 -> new Animation());
                    animation.update();
                    animation.animate(this.isHovered(i, i2, f6, f8, f7, 14.0) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
                    GuiRenderUtil.drawHighlight(drawContext, f6, f8, f7, 14.0F, f4 * animation.get(), Radius.all(4.6666665F));
                    int i5 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), flag ? 1.0F : 0.6F), f4);
                    new RenderUtil(drawContext, f6, f8, f7, 14.0F)
                        .icon(Icon.CHECK, 4.0F, 3.0F, 8.0F, Theme.withAlpha(Theme.getPrimary(), (flag ? 1.0F : 0.0F) * f4))
                        .scrollingTextMiddle(Fonts.INTER_MEDIUM, this.dropdownLabelProvider.apply(string), f5, 0.0F, f7 - f5 - 4.0F, i5, 6.0F);
                    if (this.dropdownItemProvider != null) {
                        this.drawItem(drawContext, this.dropdownItemProvider.apply(string), f6 + 21.0F, f8 + 7.0F, 0.5F, f4);
                    }

                    f8 += 14.0F;
                }

                MatrixUtil.pop(drawContext);
            }
        }
    }

    private void clickDropdown(double d, double d2) {
        if (this.dropdownSetting != null) {
            List list = this.dropdownSetting.getModes();
            float f = this.dropdownY + 3.0F;
            int i = list.size();

            for (int i2 = 0; i2 < i; i2++) {
                if (this.isHovered(d, d2, this.dropdownX + 3.0F, f, this.dropdownWidth - 6.0F, 14.0)) {
                    this.dropdownSetting.setValue((String)list.get(i2));
                    return;
                }

                f += 14.0F;
            }
        }
    }

    private boolean renderIconButton(
        DrawContext drawContext, float f, float f2, float f3, Icon icon2, float f4, Animation animation, int i, int i2, float f5
    ) {
        boolean flag = this.isHovered(i, i2, f, f2, f3, f3);
        animation.update();
        animation.animate(flag ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f6 = animation.get();
        GuiRenderUtil.drawHighlight(drawContext, f, f2, f3, f3, f5 * f6, Radius.all(f3 / 3.0F));
        new RenderUtil(drawContext, f, f2, f3, f3)
            .icon(icon2, f3 / 2.0F - f4 / 2.0F, f3 / 2.0F - f4 / 2.0F, f4, Theme.withAlpha(Theme.getSecondaryText(), (0.55F + 0.4F * f6) * f5));
        return flag;
    }

    private void renderSidebar(DrawContext drawContext, int i, int i2, float f) {
        this.renderSectionLabel(drawContext, this.serverListY - 10.0F, Language.translate("serverassist.panel.section.servers", "Серверы"), f);
        this.renderServerTabs(drawContext, i, i2, f);
        float f2 = this.getServerTabY(SERVERS.length) + 4.0F;
        this.renderSectionLabel(drawContext, f2, Language.translate("serverassist.panel.section.settings", "Настройки"), f);
        this.rebuildSettingRows();
        this.renderSettingRows(drawContext, f2 + 11.0F, i, i2, f * this.contentAnimation.get());
    }

    private void renderSectionLabel(DrawContext drawContext, float f, String string, float f2) {
        new RenderUtil(drawContext, this.leftX, f, 104.0F, 7.0F)
            .textBottomLeft(Fonts.INTER_REGULAR, string, 2.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.65F), f2), 6.5F);
    }

    private void renderServerTabs(DrawContext drawContext, int i, int i2, float f) {
        for (int i3 = 0; i3 < SERVERS.length; i3++) {
            String string = SERVERS[i3];
            float f2 = this.getServerTabY(i3);
            boolean flag = string.equals(this.selectedServer);
            Animation animation = this.serverSelectAnimations.computeIfAbsent(string, string2 -> new Animation());
            animation.update();
            animation.animate(flag ? 1.0 : 0.0, 0.5, flag ? Easings.QUART_OUT : Easings.QUAD_IN);
            Animation animation2 = this.serverHoverAnimations.computeIfAbsent(string, string3 -> new Animation());
            animation2.update();
            animation2.animate(this.isHovered(i, i2, this.leftX, f2, 104.0, 15.0) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
            float f3 = animation.get();
            float f4 = Math.max(f3, animation2.get() * 0.5F);
            GuiRenderUtil.drawHighlight(drawContext, this.leftX, f2, 104.0F, 15.0F, f4 * f, Radius.all(6.0F));
            int i4 = Theme.withAlpha(Theme.getPrimary(), (0.6F + 0.4F * f3) * f);
            int i5 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.5F + 0.5F * f3), f);
            new RenderUtil(drawContext, this.leftX, f2, 104.0F, 15.0F)
                .icon(this.getServerIcon(string), 4.0F, 3.5F, 8.0F, i4)
                .textMiddleLeft(Fonts.INTER_MEDIUM, this.truncate(this.getServerDisplayName(string), 78.0F, 6.5F), 16.0F, 0.0F, i5, 6.5F);
            if (string.equals(this.serverAssist.getActiveServer())) {
                new RenderUtil(drawContext, this.leftX + 104.0F - 7.5F, f2 + 7.5F - 1.5F, 3.0F, 3.0F)
                    .rect(ColorUtil.multiplyAlpha(Theme.getPrimary(), f), Radius.all(1.5F));
            }
        }
    }

    private Icon getServerIcon(String string) {
        byte b = -1;
        switch (string.hashCode()) {
            case -509146932:
                if (string.equals("funtime")) {
                    return Icon.FLAME;
                }
                break;
            case 1825311550:
                if (string.equals("holyworld")) {
                    b = 1;
                }
        }

        switch (b) {
            case 0:
                return Icon.FLAME;
            case 1:
                return Icon.CROWN;
            default:
                return Icon.GLOBE;
        }
    }

    private float getServerTabY(int i) {
        return this.serverListY + i * 18.0F;
    }

    private String getServerDisplayName(String string) {
        byte b = -1;
        switch (string.hashCode()) {
            case -509146932:
                if (string.equals("funtime")) {
                    return Language.translate("serverassist.panel.tab.funtime", "FunTime · Spooky");
                }
                break;
            case 1825311550:
                if (string.equals("holyworld")) {
                    b = 1;
                }
        }

        switch (b) {
            case 0:
                return Language.translate("serverassist.panel.tab.funtime", "FunTime · Spooky");
            case 1:
                return Language.translate("serverassist.panel.tab.holyworld", "HolyWorld");
            default:
                return Language.translate("serverassist.panel.tab.reallyworld", "ReallyWorld");
        }
    }

    private String getBindText(KeybindSetting keybindSetting) {
        if (this.bindingSetting == keybindSetting) {
            return this.pressedKeys.isEmpty() ? "···" : this.getPressedKeysText();
        }

        ValueBind valueBind = this.serverAssist.getPrimaryBind(keybindSetting);
        return valueBind == null ? "—" : valueBind.getDisplayString();
    }

    private String getPressedKeysText() {
        StringBuilder builder = new StringBuilder();
        int i = this.pressedKeys.size();

        for (int i2 = 0; i2 < i; i2++) {
            if (i2 > 0) {
                builder.append(" + ");
            }

            builder.append(Key.getDisplayName(this.pressedKeys.get(i2)));
        }

        return builder.toString();
    }

    private void startBinding(KeybindSetting keybindSetting) {
        this.bindingSetting = keybindSetting;
        this.pressedKeys.clear();
    }

    private void cancelBinding() {
        this.bindingSetting = null;
        this.pressedKeys.clear();
    }

    private void applyBinding() {
        if (this.bindingSetting != null) {
            ArrayList arrayList = new ArrayList();
            int i = this.pressedKeys.size();

            for (int i2 = 0; i2 < i; i2++) {
                int i3 = this.normalizeKey(this.pressedKeys.get(i2));
                if (!arrayList.contains(i3)) {
                    arrayList.add(i3);
                }
            }

            if (arrayList.isEmpty()) {
                this.cancelBinding();
            } else {
                int i4 = arrayList.size() - 1;
                int[] ints = new int[i4];

                for (int i5 = 0; i5 < i4; i5++) {
                    ints[i5] = (Integer)arrayList.get(i5);
                }

                this.serverAssist.setSingleBind(this.bindingSetting, (Integer)arrayList.get(i4), ints);
                this.cancelBinding();
            }
        }
    }

    private int normalizeKey(int i) {
        return Key.isModifier(i) ? Key.getModifier(i) : i;
    }

    private boolean handleBindClick(Click click, double d, double d2, float f, float f2, float f3, float f4, KeybindSetting keybindSetting) {
        if (!this.isHovered(d, d2, f, f2, f3, f4)) {
            return false;
        }

        if (this.isRightButton(click.button())) {
            this.serverAssist.setSingleBind(keybindSetting, -1, null);
            this.cancelBinding();
            return true;
        }

        if (this.isLeftButton(click.button())) {
            this.startBinding(keybindSetting);
        }

        return true;
    }

    private void rebuildSettingRows() {
        this.settingRows.clear();
        BooleanSetting booleanSetting = this.serverAssist.getWheelEnabledByServer().get(this.selectedServer);
        if (booleanSetting != null) {
            this.settingRows.add(new ServerAssistPanel.SettingRow(booleanSetting, null, true));
            this.settingRows.add(new ServerAssistPanel.SettingRow(this.serverAssist.getWheelItemsBind(), null, booleanSetting.getValue()));
            if (this.selectedServer.equals("funtime")) {
                this.settingRows.add(new ServerAssistPanel.SettingRow(this.serverAssist.getWheelPotionsBind(), null, booleanSetting.getValue()));
            }
        }

        if (this.selectedServer.equals("funtime")) {
            this.settingRows
                .add(
                    new ServerAssistPanel.SettingRow(
                        this.serverAssist.getFuntimeUtils().getOption("auto_point"), Language.translateOption("serverassist.funtimeUtils", "auto_point"), true
                    )
                );
            this.settingRows
                .add(
                    new ServerAssistPanel.SettingRow(
                        this.serverAssist.getFuntimeUtils().getOption("structures"), Language.translateOption("serverassist.funtimeUtils", "structures"), true
                    )
                );
            this.settingRows
                .add(
                    new ServerAssistPanel.SettingRow(
                        this.serverAssist.getFuntimeUtils().getOption("event_delay"),
                        Language.translateOption("serverassist.funtimeUtils", "event_delay"),
                        true
                    )
                );
            this.settingRows.add(new ServerAssistPanel.SettingRow(this.serverAssist.getUseFromBundle(), null, true));
        } else {
            if (this.selectedServer.equals("holyworld")) {
                this.settingRows
                    .add(
                        new ServerAssistPanel.SettingRow(
                            this.serverAssist.getHolyUtils().getOption("auto_loot"), Language.translateOption("serverassist.holyUtils", "auto_loot"), true
                        )
                    );
                this.settingRows
                    .add(
                        new ServerAssistPanel.SettingRow(
                            this.serverAssist.getHolyUtils().getOption("traps"), Language.translateOption("serverassist.holyUtils", "traps"), true
                        )
                    );
            }
        }
    }

    private void renderSettingRows(DrawContext drawContext, float f, int i, int i2, float f2) {
        int i3 = this.settingRows.size();

        for (int i4 = 0; i4 < i3; i4++) {
            ServerAssistPanel.SettingRow settingRow = this.settingRows.get(i4);
            Animation animation = this.rowVisibilityAnimations.get(settingRow.getKey());
            if (animation == null) {
                animation = new Animation();
                animation.forceValue(settingRow.visible ? 1.0 : 0.0);
                this.rowVisibilityAnimations.put(settingRow.getKey(), animation);
            }

            animation.update();
            animation.animate(settingRow.visible ? 1.0 : 0.0, 0.25, settingRow.visible ? Easings.QUAD_OUT : Easings.QUAD_IN);
            float f3 = (float)Math.min(animation.get(), 1.0);
            settingRow.x = this.leftX + 2.0F;
            settingRow.y = f;
            settingRow.width = 100.0F;
            settingRow.height = 13.0F * f3;
            if (!(f3 <= 0.01F)) {
                if (f3 < 0.999F) {
                    boolean flag = ScissorStack.push(drawContext, settingRow.x, f, settingRow.width, 13.0F * f3);
                    if (flag) {
                        try {
                            this.renderSettingRow(drawContext, settingRow, i, i2, f2 * f3);
                        } finally {
                            ScissorStack.pop(drawContext);
                        }
                    }
                } else {
                    this.renderSettingRow(drawContext, settingRow, i, i2, f2);
                }

                f += 14.0F * f3;
            }
        }
    }

    private void renderSettingRow(DrawContext drawContext, ServerAssistPanel.SettingRow settingRow, int i, int i2, float f) {
        if (settingRow.setting instanceof KeybindSetting keybindSetting) {
            this.renderKeybindRow(drawContext, settingRow, keybindSetting, i, i2, Math.min(f, 1.0F));
        } else if (settingRow.label == null) {
            settingRow.setting.position.set(settingRow.x, settingRow.y);
            settingRow.setting.size.set(settingRow.width, settingRow.height);
            settingRow.setting.alphaAnimation.forceValue(Math.min(f, 1.0F));
            settingRow.setting.render(drawContext, i, i2, 0.0F);
        } else {
            BooleanSetting booleanSetting = (BooleanSetting)settingRow.setting;
            Animation animation = this.toggleAnimations.computeIfAbsent(settingRow.label, string -> new Animation());
            animation.update();
            animation.animate(booleanSetting.getValue() ? 1.0 : 0.0, 0.5, Easings.EXPO_OUT);
            float f2 = animation.get();
            boolean flag = this.isHovered(i, i2, settingRow.x, settingRow.y, settingRow.width, settingRow.height);
            int i3 = ColorUtil.multiplyAlpha(
                ColorUtil.interpolate(Theme.withAlpha(Theme.getTextColor(), 0.5F), Theme.getTextColor(), f2), Math.min(f, 1.0F)
            );
            new RenderUtil(drawContext, settingRow.x, settingRow.y, settingRow.width, settingRow.height)
                .scrollingTextMiddle(Fonts.INTER_MEDIUM, settingRow.label, 0.0F, 0.0F, settingRow.width - 14.0F - 6.0F, i3, 6.5F, flag);
            SettingWidgets.drawDefaultSwitch(drawContext, settingRow.x + settingRow.width - 14.0F, settingRow.y + settingRow.height / 2.0F - 4.5F, f2, Math.min(f, 1.0F));
        }
    }

    private void renderKeybindRow(DrawContext drawContext, ServerAssistPanel.SettingRow settingRow, KeybindSetting keybindSetting, int i, int i2, float f) {
        String string = this.getBindText(keybindSetting);
        float f2 = Math.max(28.0F, Fonts.INTER_MEDIUM.getWidth(string, 6.0F) + 10.0F);
        float f3 = settingRow.x + settingRow.width - f2;
        float f4 = settingRow.y + settingRow.height / 2.0F - 5.5F;
        boolean flag = this.isHovered(i, i2, settingRow.x, settingRow.y, settingRow.width, settingRow.height);
        int i3 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), this.serverAssist.getPrimaryBind(keybindSetting) == null ? 0.55F : 1.0F), f);
        new RenderUtil(drawContext, settingRow.x, settingRow.y, settingRow.width, settingRow.height)
            .scrollingTextMiddle(Fonts.INTER_MEDIUM, Language.translate(keybindSetting.getName()), 0.0F, 0.0F, settingRow.width - f2 - 6.0F, i3, 6.5F, flag);
        RenderUtil renderUtil = GuiRenderUtil.drawSurface(drawContext, f3, f4, f2, 11.0F, f * 0.8F, 4.0F);
        if (this.bindingSetting == keybindSetting) {
            renderUtil.shadow(0.5F, Theme.withAlpha(Theme.getPrimary(), f), Radius.all(4.0F));
        }

        renderUtil.textCenter(Fonts.INTER_MEDIUM, string, ColorUtil.multiplyAlpha(Theme.getTextColor(), f), 6.0F);
    }

    private boolean clickSettingRows(Click click, double d, double d2) {
        int i = this.settingRows.size();

        for (int i2 = 0; i2 < i; i2++) {
            ServerAssistPanel.SettingRow settingRow = this.settingRows.get(i2);
            if (this.isHovered(d, d2, settingRow.x, settingRow.y, settingRow.width, settingRow.height)) {
                if (settingRow.setting instanceof KeybindSetting keybindSetting) {
                    return this.handleBindClick(click, d, d2, settingRow.x, settingRow.y, settingRow.width, settingRow.height, keybindSetting);
                }

                if (this.isLeftButton(click.button())) {
                    ((BooleanSetting)settingRow.setting).toggle();
                }

                return true;
            }
        }

        return false;
    }

    private void renderSearchBar(DrawContext drawContext, int i, int i2, float f) {
        float f2 = this.contentWidth - 17.0F - 4.0F;
        this.searchHoverAnimation.update();
        boolean flag = this.isHovered(i, i2, this.contentX, this.searchY, f2, 17.0);
        this.searchHoverAnimation.animate(!flag && !this.searchField.isFocused() ? 0.0 : 1.0, 0.15, Easings.QUAD_OUT);
        GuiRenderUtil.drawOutlined(drawContext, this.contentX, this.searchY, f2, 17.0F, f * (0.7F + 0.2F * this.searchHoverAnimation.get()), 5.6666665F)
            .icon(Icon.SEARCH, 3.5F, 4.5F, 8.0F, ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f));
        this.searchField
            .render(drawContext, new Vector2f(this.contentX + 14.0F, this.searchY + 8.5F - 3.0F), ColorUtil.multiplyAlpha(Theme.getTextColor(), f), f2 - 17.0F);
        if (!this.searchField.getText().equals(this.lastSearchText)) {
            this.lastSearchText = this.searchField.getText();
            this.scroll.reset();
        }

        this.resetButtonX = this.contentX + this.contentWidth - 17.0F;
        if (this.renderIconButton(drawContext, this.resetButtonX, this.searchY, 17.0F, Icon.REFRESH, 9.0F, this.resetHoverAnimation, i, i2, f)) {
            this.tooltipText = Language.translate("serverassist.panel.reset", "Сбросить бинды и колесо");
            this.tooltipX = this.resetButtonX + 8.5F;
            this.tooltipY = this.searchY;
        }
    }

    private void renderItemGrid(DrawContext drawContext, int i, int i2, float f) {
        if (!(this.gridHeight <= 0.0F)) {
            ArrayList arrayList = new ArrayList();
            ArrayList arrayList2 = new ArrayList();
            this.collectItems(arrayList, arrayList2);
            boolean flag = !arrayList.isEmpty() && !arrayList2.isEmpty();
            float f2 = this.getRowCount(arrayList.size()) * 68.0F + (flag ? 9.0F : 0.0F) + this.getRowCount(arrayList2.size()) * 68.0F;
            this.scroll.update(f2, this.gridHeight);
            this.scroll.updateHover(i, i2);
            if (f2 > this.gridHeight) {
                this.scroll.render(drawContext, this.contentX + this.contentWidth - 2.5F, this.gridY, this.gridHeight, f2, f);
            }

            boolean flag2 = ScissorStack.push(drawContext, this.contentX, this.gridY, this.contentWidth, this.gridHeight);
            if (flag2) {
                try {
                    this.beginFadeMask(f2);
                    float f3 = this.contentAnimation.get();
                    float f4 = this.gridY + this.scroll.getScroll() + (1.0F - f3) * 8.0F;
                    float f5 = f * f3;
                    float f6 = this.renderCards(drawContext, arrayList, f4, i, i2, f5);
                    if (flag) {
                        f6 = this.renderSeparator(drawContext, f6, f5);
                    }

                    this.renderCards(drawContext, arrayList2, f6, i, i2, f5);
                } finally {
                    VerticalFadeMask.end();
                    ScissorStack.pop(drawContext);
                }
            }
        }
    }

    private void renderTooltip(DrawContext drawContext, float f) {
        float f2 = Fonts.INTER_MEDIUM.getWidth(this.tooltipText, 5.5F) + 10.0F;
        float f3 = Math.max(this.contentX, Math.min(this.tooltipX - f2 / 2.0F, this.contentX + this.contentWidth - f2));
        float f4 = this.tooltipY - 12.0F - 4.0F;
        GuiRenderUtil.drawPopup(drawContext, f3, f4, f2, 12.0F, f, 6.0F);
        new RenderUtil(drawContext, f3, f4, f2, 12.0F)
            .textCenter(Fonts.INTER_MEDIUM, this.tooltipText, ColorUtil.multiplyAlpha(Theme.getTextColor(), f), 5.5F);
    }

    private void beginFadeMask(float f) {
        float f2 = f - this.gridHeight;
        if (!(f2 <= 0.5F) && !(this.gridHeight <= 28.0F)) {
            float f3 = this.scroll.getScroll();
            float f4 = Math.min(Math.max(-f3 / 14.0F, 0.0F), 1.0F);
            float f5 = Math.min(Math.max((f2 + f3) / 14.0F, 0.0F), 1.0F);
            if (!(f4 <= 0.001F) || !(f5 <= 0.001F)) {
                VerticalFadeMask.begin(this.gridY, this.gridY + 14.0F, f4, this.gridY + this.gridHeight - 14.0F, this.gridY + this.gridHeight, f5);
            }
        }
    }

    private void collectItems(List<DonateItem> donateItems, List<DonateItem> donateItems2) {
        String string = this.searchField.getText().trim().toLowerCase(Locale.ROOT);

        for (DonateItem donateItem : DonateItem.values()) {
            if (donateItem.getServer().getApiName().equals(this.selectedServer) && (string.isEmpty() || donateItem.getDisplayName().toLowerCase(Locale.ROOT).contains(string))) {
                (this.isItemConfigured(donateItem) ? donateItems : donateItems2).add(donateItem);
            }
        }
    }

    private void resetItemBinds() {
        for (DonateItem donateItem : DonateItem.values()) {
            if (donateItem.getServer().getApiName().equals(this.selectedServer)) {
                KeybindSetting keybindSetting = this.serverAssist.getItemBinds().get(donateItem);
                keybindSetting.clearBinds();
                keybindSetting.setValue(Integer.valueOf(-1));
                this.serverAssist.getWheelItemToggles().get(donateItem).setValue(false);
            }
        }
    }

    private boolean isItemConfigured(DonateItem donateItem) {
        KeybindSetting keybindSetting = this.serverAssist.getItemBinds().get(donateItem);
        return keybindSetting.getDefaultValue() == -1 && keybindSetting.getBinds().isEmpty() ? this.serverAssist.getWheelItemToggles().get(donateItem).getValue() : true;
    }

    private int getRowCount(int i) {
        return (i + 2 - 1) / 2;
    }

    private float renderSeparator(DrawContext drawContext, float f, float f2) {
        GuiRenderUtil.drawOverlay(drawContext, this.contentX + 6.0F, MathUtil.roundToStep(f + 4.5F, 0.5F), this.contentWidth - 6.0F - 12.0F, 0.5F, f2);
        return f + 9.0F;
    }

    private float renderCards(DrawContext drawContext, List<DonateItem> donateItems, float f, int i, int i2, float f2) {
        int i3 = donateItems.size();

        for (int i4 = 0; i4 < i3; i4++) {
            int i5 = i4 % 2;
            int i6 = i4 / 2;
            float f3 = this.contentX + i5 * (this.cardWidth + 4.0F);
            float f4 = f + i6 * 68.0F;
            ServerAssistPanel.ItemCard itemCard = this.getCard((DonateItem)donateItems.get(i4));
            itemCard.x = f3;
            itemCard.y = f4;
            itemCard.width = this.cardWidth;
            itemCard.height = 64.0F;
            if (f4 + 64.0F >= this.gridY && f4 <= this.gridY + this.gridHeight) {
                this.renderCard(drawContext, itemCard, i, i2, f2);
            }
        }

        return f + this.getRowCount(i3) * 68.0F;
    }

    private void renderCard(DrawContext drawContext, ServerAssistPanel.ItemCard itemCard, int i, int i2, float f) {
        boolean flag = this.isHovered(i, i2, itemCard.x, itemCard.y, itemCard.width, itemCard.height);
        boolean flag2 = this.isItemConfigured(itemCard.item);
        itemCard.hoverAnimation.update();
        itemCard.flipAnimation.update();
        itemCard.activeAnimation.update();
        itemCard.hoverAnimation.animate(flag ? 1.0 : 0.0, 0.18, Easings.QUAD_OUT);
        itemCard.flipAnimation.animate(itemCard.flipped ? 1.0 : 0.0, 0.4, Easings.QUART_OUT);
        itemCard.activeAnimation.animate(flag2 ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        float f2 = itemCard.hoverAnimation.get();
        float f3 = itemCard.flipAnimation.get();
        float f4 = itemCard.activeAnimation.get();
        boolean flag3 = f3 > 0.5F;
        float f5 = Math.max(Math.abs(1.0F - 2.0F * f3), 0.02F);
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(drawContext, itemCard.x + itemCard.width / 2.0F, itemCard.y + itemCard.height / 2.0F, f5, 1.0F);
        float f6 = Math.max(f4 * (0.72F + 0.18F * f2), 0.67F * f2);
        GuiRenderUtil.drawHighlight(drawContext, itemCard.x, itemCard.y, itemCard.width, itemCard.height, f * f6, Radius.all(8.0F));
        if (flag3) {
            this.renderCardBack(drawContext, itemCard, i, i2, f);
            MatrixUtil.pop(drawContext);
        } else {
            this.renderCardFront(drawContext, itemCard, i, i2, f, f2);
            MatrixUtil.pop(drawContext);
        }
    }

    private void renderCardFront(DrawContext drawContext, ServerAssistPanel.ItemCard itemCard, int i, int i2, float f, float f2) {
        DonateItem donateItem = itemCard.item;
        float f3 = itemCard.x + itemCard.width / 2.0F;
        float f4 = itemCard.y + 21.0F;
        float f5 = f * VerticalFadeMask.getAlphaFactor(f4);
        if (donateItem == DonateItem.BUFF) {
            this.drawRotatedItem(
                drawContext, this.getItemStack(this.getSelectedItem(this.serverAssist.getRebuffFirst())), f3 - 8.5F, f4 + 1.5F, 1.0F, -12.0F, f5
            );
            this.drawItem(drawContext, this.getItemStack(DonateItem.AURA), f3, f4, 1.0F, f5);
            this.drawRotatedItem(
                drawContext, this.getItemStack(this.getSelectedItem(this.serverAssist.getRebuffSecond())), f3 + 8.5F, f4 + 1.5F, 1.0F, 12.0F, f5
            );
        } else {
            this.drawItem(drawContext, this.getItemStack(donateItem), f3, f4, 1.0F, f5);
        }

        int i3 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.5F + 0.5F * itemCard.activeAnimation.get()), f);
        new RenderUtil(drawContext, itemCard.x + 3.0F, itemCard.y + 31.0F, itemCard.width - 6.0F, 10.0F)
            .textCenter(Fonts.INTER_MEDIUM, this.truncate(donateItem.getDisplayName(), itemCard.width - 8.0F, 6.0F), i3, 6.0F);
        itemCard.infoSize = 7.0F;
        itemCard.infoX = itemCard.x + itemCard.width - itemCard.infoSize - 4.0F;
        itemCard.infoY = itemCard.y + 4.0F;
        if (f2 > 0.01F) {
            boolean flag = this.isHovered(i, i2, itemCard.infoX - 2.0F, itemCard.infoY - 2.0F, itemCard.infoSize + 4.0F, itemCard.infoSize + 4.0F);
            int i4 = ColorUtil.multiplyAlpha(Theme.withAlpha(flag ? Theme.getPrimary() : Theme.getSecondaryText(), flag ? 1.0F : 0.7F), f * f2);
            new RenderUtil(drawContext, itemCard.infoX, itemCard.infoY, itemCard.infoSize, itemCard.infoSize).icon(Icon.INFO_CIRCLE, 0.0F, 0.0F, itemCard.infoSize, i4);
        }

        itemCard.wheelWidth = 9.0F;
        itemCard.wheelHeight = 9.0F;
        itemCard.wheelX = itemCard.x + itemCard.width - 5.0F - itemCard.wheelWidth;
        itemCard.bindX = itemCard.x + 4.0F;
        itemCard.bindY = itemCard.y + itemCard.height - 15.0F;
        itemCard.wheelY = itemCard.bindY + (11.0F - itemCard.wheelHeight) / 2.0F;
        itemCard.bindWidth = itemCard.wheelX - 4.0F - itemCard.bindX;
        itemCard.bindHeight = 11.0F;
        KeybindSetting keybindSetting = this.serverAssist.getItemBinds().get(donateItem);
        String string = this.getBindText(keybindSetting);
        RenderUtil renderUtil = GuiRenderUtil.drawSurface(drawContext, itemCard.bindX, itemCard.bindY, itemCard.bindWidth, itemCard.bindHeight, f * 0.8F, 4.0F);
        if (this.bindingSetting == keybindSetting) {
            renderUtil.shadow(0.5F, Theme.withAlpha(Theme.getPrimary(), f), Radius.all(4.0F));
        }

        renderUtil.textCenter(Fonts.INTER_MEDIUM, this.truncate(string, itemCard.bindWidth - 4.0F, 6.0F), ColorUtil.multiplyAlpha(Theme.getTextColor(), f), 6.0F);
        BooleanSetting booleanSetting = this.serverAssist.getWheelEnabledByServer().get(this.selectedServer);
        boolean flag2;
        if (booleanSetting != null) {
            flag2 = booleanSetting.getValue();
        } else {
            flag2 = false;
        }

        BooleanSetting booleanSetting2 = this.serverAssist.getWheelItemToggles().get(donateItem);
        itemCard.wheelAnimation.update();
        itemCard.wheelAnimation.animate(booleanSetting2.getValue() ? 1.0 : 0.0, 0.3, Easings.BACK_OUT);
        float f6 = itemCard.wheelAnimation.get();
        boolean flag3 = this.isHovered(i, i2, itemCard.wheelX - 3.0F, itemCard.wheelY - 3.0F, itemCard.wheelWidth + 6.0F, itemCard.wheelHeight + 6.0F);
        itemCard.wheelHoverAnimation.update();
        itemCard.wheelHoverAnimation.animate(flag3 ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f7 = itemCard.wheelHoverAnimation.get();
        GuiRenderUtil.drawHighlight(
            drawContext,
            itemCard.wheelX - 3.0F,
            itemCard.wheelY - 3.0F,
            itemCard.wheelWidth + 6.0F,
            itemCard.wheelHeight + 6.0F,
            f * f7,
            Radius.all((itemCard.wheelWidth + 6.0F) / 3.0F)
        );
        int i5 = ColorUtil.multiplyAlpha(
            ColorUtil.interpolate(Theme.withAlpha(Theme.getSecondaryText(), 0.55F + 0.4F * f7), Theme.getPrimary(), Math.min(f6, 1.0F)),
            f * (flag2 ? 1.0F : 0.45F)
        );
        MatrixUtil.push(drawContext);
        MatrixUtil.rotate(drawContext, itemCard.wheelX + itemCard.wheelWidth / 2.0F, itemCard.wheelY + itemCard.wheelHeight / 2.0F, 45.0F * f6);
        new RenderUtil(drawContext, itemCard.wheelX, itemCard.wheelY, itemCard.wheelWidth, itemCard.wheelHeight).icon(Icon.PLUS, 0.0F, 0.0F, itemCard.wheelWidth, i5);
        MatrixUtil.pop(drawContext);
        if (flag3) {
            this.tooltipText = booleanSetting2.getValue()
                ? Language.translate("serverassist.panel.wheel_remove", "Убрать из колеса")
                : Language.translate("serverassist.panel.wheel_add", "Добавить в колесо");
            this.tooltipX = itemCard.wheelX + itemCard.wheelWidth / 2.0F;
            this.tooltipY = itemCard.wheelY;
        }
    }

    private void renderCardBack(DrawContext drawContext, ServerAssistPanel.ItemCard itemCard, int i, int i2, float f) {
        DonateItem donateItem = itemCard.item;
        int i3 = ColorUtil.multiplyAlpha(Theme.getTextColor(), f);
        new RenderUtil(drawContext, itemCard.x + 3.0F, itemCard.y + 3.0F, itemCard.width - 6.0F, 10.0F)
            .textCenter(Fonts.INTER_SEMIBOLD, this.truncate(donateItem.getDisplayName(), itemCard.width - 8.0F, 6.0F), i3, 6.0F);
        boolean flag = donateItem == DonateItem.BUFF;
        boolean flag2 = this.hasAimOptions(donateItem);
        BooleanSetting booleanSetting = this.serverAssist.getSmartAimToggles().get(donateItem);
        float f2 = flag ? 32.0F : (flag2 ? 16.0F : 4.0F) + (booleanSetting != null ? 14.0F : 0.0F);
        float f3 = itemCard.y + itemCard.height - f2;
        List list = this.getDescriptionLines(donateItem);
        int i4 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.85F), f);
        float f4 = itemCard.y + 15.0F;
        int i5 = list.size();

        for (int i6 = 0; i6 < i5 && !(f4 + 7.0F > f3); i6++) {
            new RenderUtil(drawContext, itemCard.x + 4.0F, f4, itemCard.width - 8.0F, 7.0F)
                .textMiddleLeft(Fonts.INTER_MEDIUM, (String)list.get(i6), 0.0F, 0.0F, i4, 5.5F);
            f4 += 7.0F;
        }

        if (flag) {
            itemCard.rebuffWidth = itemCard.width - 8.0F;
            itemCard.rebuffHeight = 12.0F;
            itemCard.rebuffX = itemCard.x + 4.0F;
            itemCard.rebuffFirstY = itemCard.y + itemCard.height - 29.0F;
            itemCard.rebuffSecondY = itemCard.y + itemCard.height - 15.0F;
            this.renderRebuffSelector(drawContext, itemCard.rebuffX, itemCard.rebuffFirstY, itemCard.rebuffWidth, itemCard.rebuffHeight, "1", this.serverAssist.getRebuffFirst(), f);
            this.renderRebuffSelector(
                drawContext, itemCard.rebuffX, itemCard.rebuffSecondY, itemCard.rebuffWidth, itemCard.rebuffHeight, "2", this.serverAssist.getRebuffSecond(), f
            );
        } else {
            if (flag2) {
                if (booleanSetting != null) {
                    this.renderCardToggle(drawContext, booleanSetting, itemCard, itemCard.y + itemCard.height - 29.0F, i, i2, f);
                }

                this.renderCardToggle(drawContext, this.serverAssist.getSyncHeadToggles().get(donateItem), itemCard, itemCard.y + itemCard.height - 15.0F, i, i2, f);
            }
        }
    }

    private void renderCardToggle(DrawContext drawContext, BooleanSetting booleanSetting, ServerAssistPanel.ItemCard itemCard, float f, int i, int i2, float f2) {
        booleanSetting.position.set(itemCard.x + 3.0F, f);
        booleanSetting.size.set(itemCard.width - 6.0F, 13.0F);
        booleanSetting.alphaAnimation.forceValue(Math.min(f2, 1.0F));
        booleanSetting.render(drawContext, i, i2, 0.0F);
    }

    private boolean hasAimOptions(DonateItem donateItem) {
        return donateItem != DonateItem.BUFF && donateItem != DonateItem.ANTIFLY && donateItem != DonateItem.EXPLOSIVE_STUFF;
    }

    private void renderRebuffSelector(DrawContext drawContext, float f, float f2, float f3, float f4, String string, ModeSetting modeSetting, float f5) {
        RenderUtil renderUtil = GuiRenderUtil.drawSurface(drawContext, f, f2, f3, f4, f5 * 0.8F, f4 / 3.0F);
        if (this.dropdownOpen && this.dropdownSetting == modeSetting) {
            renderUtil.shadow(0.5F, Theme.withAlpha(Theme.getPrimary(), f5), Radius.all(f4 / 3.0F));
        }

        renderUtil.textMiddleLeft(Fonts.INTER_MEDIUM, string, 4.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.6F), f5), 5.5F)
            .textMiddleLeft(
                Fonts.INTER_MEDIUM,
                this.truncate(this.getSelectedItem(modeSetting).getDisplayName(), f3 - 30.0F, 5.5F),
                11.0F,
                0.0F,
                ColorUtil.multiplyAlpha(Theme.getTextColor(), f5),
                5.5F
            );
        RenderUtil.drawChevron(
            drawContext,
            f + f3 - 10.0F,
            f2 + f4 / 2.0F - 3.5F,
            7.0F,
            90.0F + 180.0F * this.getDropdownProgress(modeSetting),
            ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f5)
        );
    }

    private DonateItem getSelectedItem(ModeSetting modeSetting) {
        return DonateItem.valueOf(modeSetting.getValue());
    }

    private void openItemDropdown(ModeSetting modeSetting, float f, float f2, float f3, float f4) {
        this.openDropdown(
            modeSetting, string -> DonateItem.valueOf(string).getDisplayName(), string2 -> this.getItemStack(DonateItem.valueOf(string2)), f, f2, f3, f4
        );
    }

    private void drawItem(DrawContext drawContext, ItemStack itemStack, float f, float f2, float f3, float f4) {
        this.drawRotatedItem(drawContext, itemStack, f, f2, f3, 0.0F, f4);
    }

    private void drawRotatedItem(DrawContext drawContext, ItemStack itemStack, float f, float f2, float f3, float f4, float f5) {
        float f6 = MathUtil.roundToStep(f, 0.5F);
        float f7 = MathUtil.roundToStep(f2, 0.5F);
        MatrixUtil.push(drawContext);
        if (f4 != 0.0F) {
            MatrixUtil.rotate(drawContext, f6, f7, f4);
        }

        MatrixUtil.scale(drawContext, f6, f7, f3);
        GuiRenderUtil.drawItem(drawContext, itemStack, f6 - 8.0F, f7 - 8.0F, f5);
        MatrixUtil.pop(drawContext);
    }

    private List<String> getDescriptionLines(DonateItem donateItem) {
        return this.descriptionLines
            .computeIfAbsent(
                donateItem,
                donateItem2 -> this.wrapText(
                    Language.translate("serveritem." + donateItem2.name().toLowerCase(Locale.ROOT) + ".desc", donateItem2.getDisplayName()), this.cardWidth - 8.0F, 5.5F
                )
            );
    }

    private List<String> wrapText(String string, float f, float f2) {
        ArrayList arrayList = new ArrayList();
        StringBuilder builder = new StringBuilder();

        for (String string2 : string.split(" ")) {
            String string3 = builder.isEmpty() ? string2 : builder + " " + string2;
            if (!builder.isEmpty() && Fonts.INTER_MEDIUM.getWidth(string3, f2) > f) {
                arrayList.add(builder.toString());
                builder = new StringBuilder(string2);
            } else {
                builder = new StringBuilder(string3);
            }
        }

        if (!builder.isEmpty()) {
            arrayList.add(builder.toString());
        }

        return arrayList;
    }

    private String truncate(String string, float f, float f2) {
        if (Fonts.INTER_MEDIUM.getWidth(string, f2) <= f) {
            return string;
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < string.length() && !(Fonts.INTER_MEDIUM.getWidth(builder.toString() + string.charAt(i) + "…", f2) > f); i++) {
            builder.append(string.charAt(i));
        }

        return builder + "…";
    }

    @Override
    public void mouseMoved(double d, double d2) {
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        if (this.serverAssist == null) {
            return false;
        }

        double d = click.x();
        double d2 = click.y();
        if (this.bindingSetting != null) {
            if (this.isLeftButton(click.button())) {
                this.cancelBinding();
                return true;
            }

            if (!this.pressedKeys.contains(click.button())) {
                this.pressedKeys.add(click.button());
            }

            this.applyBinding();
            return true;
        } else if (this.dropdownOpen) {
            if (this.isLeftButton(click.button())) {
                this.clickDropdown(d, d2);
            }

            this.dropdownOpen = false;
            return true;
        } else {
            this.searchField.mouseClicked(d, d2, click.button());
            if (this.isLeftButton(click.button()) && this.isHovered(d, d2, this.closeButtonX - 3.0F, this.closeButtonY - 3.0F, 16.0, 16.0)) {
                this.close();
                return true;
            }

            if (this.isLeftButton(click.button()) && this.isHovered(d, d2, this.serverSelectX, this.serverSelectY, this.serverSelectWidth, 13.0)) {
                this.openDropdown(
                    this.serverAssist.getServer(), this::translateServerName, null, this.serverSelectX, this.serverSelectY, this.serverSelectWidth, 13.0F
                );
                return true;
            }

            for (int i = 0; i < SERVERS.length; i++) {
                if (this.isLeftButton(click.button()) && this.isHovered(d, d2, this.leftX, this.getServerTabY(i), 104.0, 15.0)) {
                    this.selectServer(SERVERS[i]);
                    return true;
                }
            }

            if (this.clickSettingRows(click, d, d2)) {
                return true;
            }

            if (this.isLeftButton(click.button()) && this.isHovered(d, d2, this.resetButtonX - 2.0F, this.searchY - 2.0F, 21.0, 21.0)) {
                this.resetItemBinds();
                return true;
            }

            if (this.isHovered(d, d2, this.contentX, this.searchY, this.contentWidth - 17.0F - 4.0F, 17.0)) {
                if (this.isLeftButton(click.button()) && !this.searchField.isFocused()) {
                    this.searchField.setFocused(true);
                    this.searchField.setCursor(this.searchField.getText().length());
                }

                return true;
            } else if (this.isLeftButton(click.button()) && this.scroll.mouseClicked(d, d2)) {
                return true;
            } else {
                return this.isHovered(d, d2, this.contentX, this.gridY, this.contentWidth, this.gridHeight) && this.clickCards(click, d, d2)
                    ? true
                    : this.isMouseOver(d, d2);
            }
        }
    }

    private boolean clickCards(Click click, double d, double d2) {
        for (ServerAssistPanel.ItemCard itemCard : this.itemCards.values()) {
            if (itemCard.item.getServer().getApiName().equals(this.selectedServer) && this.isHovered(d, d2, itemCard.x, itemCard.y, itemCard.width, itemCard.height)) {
                if (itemCard.flipped) {
                    if (itemCard.item == DonateItem.BUFF && this.isLeftButton(click.button())) {
                        if (this.isHovered(d, d2, itemCard.rebuffX, itemCard.rebuffFirstY, itemCard.rebuffWidth, itemCard.rebuffHeight)) {
                            this.openItemDropdown(this.serverAssist.getRebuffFirst(), itemCard.rebuffX, itemCard.rebuffFirstY, itemCard.rebuffWidth, itemCard.rebuffHeight);
                            return true;
                        }

                        if (this.isHovered(d, d2, itemCard.rebuffX, itemCard.rebuffSecondY, itemCard.rebuffWidth, itemCard.rebuffHeight)) {
                            this.openItemDropdown(this.serverAssist.getRebuffSecond(), itemCard.rebuffX, itemCard.rebuffSecondY, itemCard.rebuffWidth, itemCard.rebuffHeight);
                            return true;
                        }
                    }

                    if (this.hasAimOptions(itemCard.item)) {
                        BooleanSetting booleanSetting = this.serverAssist.getSmartAimToggles().get(itemCard.item);
                        if (booleanSetting != null && booleanSetting.mouseClicked(click, false)) {
                            return true;
                        }

                        if (this.serverAssist.getSyncHeadToggles().get(itemCard.item).mouseClicked(click, false)) {
                            return true;
                        }
                    }

                    itemCard.flipped = false;
                    return true;
                }

                if (this.isHovered(d, d2, itemCard.infoX - 2.0F, itemCard.infoY - 2.0F, itemCard.infoSize + 4.0F, itemCard.infoSize + 4.0F)) {
                    itemCard.flipped = true;
                    return true;
                }

                KeybindSetting keybindSetting = this.serverAssist.getItemBinds().get(itemCard.item);
                if (this.handleBindClick(click, d, d2, itemCard.bindX, itemCard.bindY, itemCard.bindWidth, itemCard.bindHeight, keybindSetting)) {
                    return true;
                }

                if (this.isLeftButton(click.button())
                    && this.isHovered(d, d2, itemCard.wheelX - 3.0F, itemCard.wheelY - 3.0F, itemCard.wheelWidth + 6.0F, itemCard.wheelHeight + 6.0F)) {
                    this.serverAssist.getWheelItemToggles().get(itemCard.item).toggle();
                    return true;
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onMouseRelease(Click click) {
        this.scroll.mouseReleased();
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        return this.scroll.isDragging() && this.scroll.mouseDragged(click.x(), click.y());
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (this.serverAssist == null) {
            return false;
        } else if (this.isHovered(d, d2, this.contentX, this.gridY, this.contentWidth, this.gridHeight)) {
            this.closeDropdown();
            this.scroll.onScroll(d4);
            return true;
        } else {
            return this.isMouseOver(d, d2);
        }
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        if (this.serverAssist == null) {
            return false;
        }

        if (this.bindingSetting != null) {
            int i = keyInput.key();
            switch (i) {
                case 256:
                    this.cancelBinding();
                    return true;
                case 259:
                case 261:
                    this.serverAssist.setSingleBind(this.bindingSetting, -1, null);
                    this.cancelBinding();
                    return true;
                default:
                    if (!this.pressedKeys.contains(i)) {
                        this.pressedKeys.add(i);
                    }

                    return true;
            }
        } else {
            int i2 = keyInput.key();
            if (this.dropdownOpen && i2 == 256) {
                this.closeDropdown();
                return true;
            }

            if (this.searchField.isFocused()) {
                if (i2 == 256) {
                    this.searchField.setFocused(false);
                    return true;
                } else {
                    this.searchField.keyPressed(i2);
                    return true;
                }
            } else if (i2 == 256) {
                this.close();
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean keyReleased(KeyInput keyInput) {
        if (this.bindingSetting != null && !this.pressedKeys.isEmpty()) {
            this.applyBinding();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onCharType(CharInput charInput) {
        if (this.serverAssist != null && this.searchField.isFocused()) {
            this.searchField.charTyped(charInput.codepoint());
            return true;
        } else {
            return false;
        }
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

    static final class ItemCard {
        final DonateItem item;
        final Animation hoverAnimation = new Animation();
        final Animation flipAnimation = new Animation();
        final Animation wheelAnimation = new Animation();
        final Animation wheelHoverAnimation = new Animation();
        final Animation activeAnimation = new Animation();
        boolean flipped;
        float x;
        float y;
        float width;
        float height;
        float bindX;
        float bindY;
        float bindWidth;
        float bindHeight;
        float wheelX;
        float wheelY;
        float wheelWidth;
        float wheelHeight;
        float infoX;
        float infoY;
        float infoSize;
        float rebuffX;
        float rebuffFirstY;
        float rebuffSecondY;
        float rebuffWidth;
        float rebuffHeight;

        ItemCard(DonateItem donateItem) {
            this.item = donateItem;
        }
    }

    static final class SettingRow {
        final Setting<?> setting;
        final String label;
        final boolean visible;
        float x;
        float y;
        float width;
        float height;

        SettingRow(Setting<?> setting2, String string, boolean flag) {
            this.setting = setting2;
            this.label = string;
            this.visible = flag;
        }

        String getKey() {
            return this.setting.getDisplayName();
        }
    }
}
