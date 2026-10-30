package tech.sadnes.client.ui.clickgui.config;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.Identifier;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.config.ConfigManager;
import tech.sadnes.client.config.cloud.CloudConfigInfo;
import tech.sadnes.client.config.cloud.CloudConfigMeta;
import tech.sadnes.client.config.cloud.CloudConfigSync;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.network.packet.CloudConfigEntry;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.Side;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.AvatarTextureLoader;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.ui.clickgui.component.Component;
import tech.sadnes.client.ui.clickgui.component.HoverTooltip;
import tech.sadnes.client.ui.clickgui.component.SmoothScrollbar;
import tech.sadnes.client.ui.clickgui.component.TextFieldComponent;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.util.ModVersion;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;
import tech.sadnes.client.util.math.StopWatch;
import ua.mintantileak.profile.Profile;

public class ConfigSelectorComponent extends Component {
    private static final float ROW_HEIGHT = 18.0F;
    private static final float NAME_INPUT_ROW_HEIGHT = 20.0F;
    private static final float SEARCH_HEIGHT = 18.0F;
    private static final float ROW_GAP = 2.0F;
    private static final float SCROLLBAR_WIDTH = 5.0F;
    private static final float DIVIDER_HEIGHT = 0.5F;
    private static final float SECTION_GAP = 3.0F;
    private static final float PADDING = 4.0F;
    private static final float POPUP_RADIUS = 8.0F;
    private static final float POPUP_WIDTH = 145.0F;
    private static final int MAX_VISIBLE_ROWS = 5;
    private static final float HEADER_BUTTON_WIDTH = 13.0F;
    private static final float HEADER_ICON_SIZE = 9.0F;
    private static final double RESET_CONFIRM_MS = 2500.0;
    private static final int DANGER_COLOR = ColorUtil.rgb(255, 94, 94);
    private static final float ACTION_MENU_WIDTH = 100.0F;
    private static final float ACTION_MENU_PADDING = 4.5F;
    private static final float ACTION_ROW_HEIGHT = 14.0F;
    private static final float ACTION_ICON_OFFSET = 3.0F;
    private static final float ACTION_MENU_GAP = 4.0F;
    private static final float TAB_HEIGHT = 14.0F;
    private static final float TAB_GAP = 3.0F;
    private static final float TAB_PADDING = 4.0F;
    private static final float ADD_CODE_POPUP_WIDTH = 130.0F;
    private static final float FIELD_HEIGHT = 16.0F;
    private static final float EDIT_POPUP_WIDTH = 150.0F;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneId.systemDefault());
    private static final Icon[] CONFIG_ICONS = new Icon[]{
        Icon.DEVICE_FLOPPY, Icon.SHIELD, Icon.FLAME, Icon.DRAGON, Icon.CROSS, Icon.FLAG, Icon.EYE, Icon.RUN, Icon.DICE, Icon.GAUGE, Icon.HEART, Icon.FACE_SMILE
    };
    private final List<ConfigEntryComponent> entries = new ArrayList<>();
    private final List<String> orderedIds = new ArrayList<>();
    private final Map<String, ConfigEntryComponent> entriesById = new HashMap<>();
    private final Map<String, Animation> entryAnimations = new HashMap<>();
    private final Animation chevronAnimation = new Animation();
    private final Animation popupAnimation = new Animation();
    private final Animation actionMenuAnimation = new Animation();
    private final Animation importHoverAnimation = new Animation();
    private final Animation newHoverAnimation = new Animation();
    private final Animation resetHoverAnimation = new Animation();
    private final Animation saveHoverAnimation = new Animation();
    private final Animation tooltipAnimation = new Animation();
    private final StopWatch tooltipTimer = new StopWatch();
    private CloudConfigInfo cachedActiveConfig;
    private final StopWatch resetConfirmTimer = new StopWatch();
    private boolean resetPending;
    private final Animation sortHoverAnimation = new Animation();
    private ConfigSelectorComponent.ConfigTab currentTab = ConfigSelectorComponent.ConfigTab.CLIENT;
    private final ConfigSelectorComponent.Bounds[] tabBounds = createBoundsArray(ConfigSelectorComponent.ConfigTab.VALUES.length);
    private final Animation[] tabHoverAnimations = createAnimationArray(ConfigSelectorComponent.ConfigTab.VALUES.length);
    private final Animation[] tabSelectAnimations = createAnimationArray(ConfigSelectorComponent.ConfigTab.VALUES.length);
    private final Animation loadHoverAnimation = new Animation();
    private final Animation selectHoverAnimation = new Animation();
    private final Animation shareHoverAnimation = new Animation();
    private final Animation deleteHoverAnimation = new Animation();
    private final SmoothScrollbar scrollbar = new SmoothScrollbar();
    private final Vector2f searchTextPos = new Vector2f();
    private final Vector2f nameInputTextPos = new Vector2f();
    private final ConfigSelectorComponent.Bounds buttonBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds saveButtonBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds selectorBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds popupBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds searchBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds sortButtonBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds listBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds nameInputBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds importButtonBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds newButtonBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds resetButtonBounds = new ConfigSelectorComponent.Bounds();
    private final HoverTooltip resetTooltip = new HoverTooltip();
    private final HoverTooltip importTooltip = new HoverTooltip();
    private final HoverTooltip newTooltip = new HoverTooltip();
    private final ConfigSelectorComponent.Bounds actionMenuBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds loadActionBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds selectActionBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds editActionBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds shareActionBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds deleteActionBounds = new ConfigSelectorComponent.Bounds();
    private TextFieldComponent searchField;
    private TextFieldComponent nameField;
    private boolean open;
    private boolean creatingNew;
    private boolean nameInvalid;
    private boolean initialized;
    private ConfigSelectorComponent.SortMode sortMode = ConfigSelectorComponent.SortMode.NAME_ASC;
    private String actionMenuConfigId;
    private CloudConfigInfo actionMenuConfig;
    private float actionMenuAnchorY;
    private float alphaFactor = 1.0F;
    private boolean addCodeOpen;
    private TextFieldComponent addCodeField;
    private final Animation addCodeAnimation = new Animation();
    private final Animation addCodeConfirmHoverAnimation = new Animation();
    private final ConfigSelectorComponent.Bounds addCodePopupBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds addCodeFieldBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds addCodeConfirmBounds = new ConfigSelectorComponent.Bounds();
    private final Vector2f addCodeTextPos = new Vector2f();
    private boolean editOpen;
    private String editConfigId;
    private long editCreatedAt;
    private TextFieldComponent editNameField;
    private TextFieldComponent editNoteField;
    private Icon editIcon;
    private final ColorSetting editColorPicker = new ColorSetting("config.edit.color", -1);
    private boolean editPublic;
    private boolean editNameInvalid;
    private boolean editNoteFocused;
    private final Animation editAnimation = new Animation();
    private final Animation applyHoverAnimation = new Animation();
    private final Animation editHoverAnimation = new Animation();
    private final Animation publicHoverAnimation = new Animation();
    private final Animation privateHoverAnimation = new Animation();
    private final Animation publicSelectAnimation = new Animation();
    private final Animation privateSelectAnimation = new Animation();
    private final ConfigSelectorComponent.Bounds editPopupBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds editNameBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds editNoteBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds applyButtonBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds publicButtonBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds privateButtonBounds = new ConfigSelectorComponent.Bounds();
    private final ConfigSelectorComponent.Bounds[] iconBounds = createBoundsArray(CONFIG_ICONS.length);
    private final Animation[] iconHoverAnimations = createAnimationArray(CONFIG_ICONS.length);
    private final Animation[] iconSelectAnimations = createAnimationArray(CONFIG_ICONS.length);
    private final Vector2f editNameTextPos = new Vector2f();
    private final Vector2f editNoteTextPos = new Vector2f();
    private int screenWidth = Integer.MAX_VALUE;
    private int screenHeight = Integer.MAX_VALUE;

    @Override
    public void init(int i, int i2) {
        this.screenWidth = i;
        this.screenHeight = i2;
    }

    @Override
    public void resize(int i, int i2) {
        this.scrollbar.reset();
        this.entries.clear();
        this.orderedIds.clear();
        this.entriesById.clear();
        this.entryAnimations.clear();
        this.initialized = false;
        this.open = false;
        this.creatingNew = false;
        this.nameInvalid = false;
        this.actionMenuConfigId = null;
        this.actionMenuConfig = null;
        this.addCodeOpen = false;
        this.editOpen = false;
        this.editConfigId = null;
        this.sortMode = ConfigSelectorComponent.SortMode.NAME_ASC;
        if (this.nameField != null) {
            this.nameField.setFocused(false);
            this.nameField.setReplaceOnInput(false);
        }

        if (this.searchField != null) {
            this.searchField.setText("");
            this.searchField.setFocused(false);
        }

        this.chevronAnimation.forceValue(0.0);
        this.popupAnimation.forceValue(0.0);
        this.actionMenuAnimation.forceValue(0.0);
        this.screenWidth = i;
        this.screenHeight = i2;
        this.refreshEntries();
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        if (this.getAlpha() <= 0.001F) {
            this.buttonBounds.clear();
            this.saveButtonBounds.clear();
            this.selectorBounds.clear();
            this.popupBounds.clear();
            this.searchBounds.clear();
            this.sortButtonBounds.clear();
            this.listBounds.clear();
            this.nameInputBounds.clear();
            this.actionMenuBounds.clear();
            this.resetTooltip.reset();
            this.importTooltip.reset();
            this.newTooltip.reset();
        } else {
            this.refreshEntries();
            this.renderButton(drawContext, i, i2);
            this.renderPopup(drawContext, i, i2, f);
            this.renderInfoTooltip(drawContext, i, i2);
        }
    }

    private void renderButton(DrawContext drawContext, int i, int i2) {
        float f = this.computeButtonWidth();
        this.buttonBounds.set(this.position.x, this.position.y, f, this.size.y);
        float f2 = this.size.y + 4.0F;
        this.saveButtonBounds.set(this.position.x, this.position.y, f2, this.size.y);
        float f3 = this.position.x + f2 + 4.0F;
        float f4 = f - f2 - 4.0F;
        this.selectorBounds.set(f3, this.position.y, f4, this.size.y);
        this.saveHoverAnimation.update();
        this.saveHoverAnimation.animate(this.saveButtonBounds.contains(i, i2) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f5 = this.saveHoverAnimation.get();
        GuiRenderUtil.drawOutlined(drawContext, this.position.x, this.position.y, f2, this.size.y, this.getAlpha() * 0.75F, this.size.y / 3.0F);
        if (f5 > 0.001F) {
            GuiRenderUtil.drawHighlight(drawContext, this.position.x, this.position.y, f2, this.size.y, this.getAlpha() * f5, Radius.all(this.size.y / 3.0F));
        }

        new RenderUtil(drawContext, this.position.x, this.position.y, f2, this.size.y)
            .icon(
                Icon.DEVICE_FLOPPY,
                f2 / 2.0F - 10.0F / 2.0F,
                this.size.y / 2.0F - 10.0F / 2.0F,
                10.0F,
                Theme.withAlpha(Theme.getPrimary(), (0.8F + 0.2F * f5) * this.getAlpha())
            );
        RenderUtil renderUtil = GuiRenderUtil.drawOutlined(drawContext, f3, this.position.y, f4, this.size.y, this.getAlpha() * 0.75F, this.size.y / 3.0F);
        float f6 = 4.0F;
        if (this.currentTab != ConfigSelectorComponent.ConfigTab.CLIENT) {
            renderUtil.icon(this.currentTab.icon, 4.0F, this.size.y / 2.0F - 3.0F, 6.0F, Theme.withAlpha(Theme.getPrimary(), this.getAlpha()));
            f6 = 4.0F + 9.0F;
        }

        renderUtil.scrollingTextMiddle(Fonts.INTER_MEDIUM, this.getActiveConfigName(), f6, 0.0F, f4 - f6 - 12.0F, this.getPrimaryColor(), 5.5F);
        this.chevronAnimation.update();
        this.chevronAnimation.animate(this.open ? 1.0 : 0.0, 0.25, Easings.BACK_OUT);
        RenderUtil.drawChevron(
            drawContext,
            f3 + f4 - 12.0F,
            this.position.y + this.size.y / 2.0F - 4.5F,
            9.0F,
            90.0F + 180.0F * this.chevronAnimation.get(),
            this.getSecondaryColor()
        );
    }

    private void renderInfoTooltip(DrawContext drawContext, int i, int i2) {
        boolean flag;
        label78: {
            this.tooltipAnimation.update();
            boolean flag2;
            if (!this.open) {
                if (!this.saveButtonBounds.contains(i, i2)) {
                    flag = this.selectorBounds.contains(i, i2);
                    break label78;
                }

                flag2 = true;
            } else {
                flag2 = false;
            }

            flag = flag2;
        }

        if (!flag || !HoverTooltip.isEnabled()) {
            this.tooltipTimer.reset();
            this.tooltipAnimation.animate(0.0, 0.2, Easings.QUAD_IN);
        } else if (this.tooltipTimer.hasPassed(500.0)) {
            this.tooltipAnimation.animate(1.0, 0.2, Easings.QUAD_OUT);
        }

        float f = this.tooltipAnimation.get();
        if (!(f <= 0.001F)) {
            CloudConfigInfo cloudConfigInfo = this.getCurrentStorage().isConnected() ? this.getActiveConfig() : null;
            List<ConfigSelectorComponent.InfoRow> infoRows = this.buildInfoRows(cloudConfigInfo);
            if (!infoRows.isEmpty()) {
                boolean flag3 = cloudConfigInfo != null;
                float f2 = flag3 ? 12.0F + 4.0F + Fonts.getWidth(cloudConfigInfo.getName(), 7.0F) : 0.0F;

                for (ConfigSelectorComponent.InfoRow infoRow : infoRows) {
                    float f3 = Fonts.getWidth(infoRow.label(), 6.0F) + 10.0F + Fonts.getWidth(infoRow.value(), 6.0F);
                    if (f3 > f2) {
                        f2 = f3;
                    }
                }

                float f4 = f2 + 5.0F * 2.0F;
                float f5 = infoRows.size() * 6.0F + Math.max(0, infoRows.size() - 1) * 3.0F;
                float f6 = 5.0F * 2.0F + f5 + (flag3 ? 12.0F + 4.0F : 0.0F);
                float f7 = GuiRenderUtil.clampToScreenX(this.selectorBounds.x + this.selectorBounds.width + 6.0F, f4);
                float f8 = GuiRenderUtil.clampToScreenY(this.buttonBounds.y + this.buttonBounds.height / 2.0F - f6 / 2.0F, f6);
                float f9 = this.getAlpha() * f;
                GuiRenderUtil.drawPopup(drawContext, f7, f8, f4, f6, f9, 6.0F);
                RenderUtil renderUtil = new RenderUtil(drawContext, f7, f8, f4, f6);
                int i3 = ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f9 * 0.7F);
                int i4 = ColorUtil.multiplyAlpha(Theme.getTextColor(), f9);
                float f10 = 5.0F;
                if (flag3) {
                    this.renderAvatar(drawContext, cloudConfigInfo, f7 + 5.0F, f8 + 5.0F, 12.0F, f9);
                    renderUtil.textTopLeft(Fonts.INTER_MEDIUM, cloudConfigInfo.getName(), 5.0F + 12.0F + 4.0F, 5.0F + 12.0F / 2.0F - 7.0F / 2.0F, i4, 7.0F);
                    f10 = 5.0F + (12.0F + 4.0F);
                }

                for (ConfigSelectorComponent.InfoRow infoRow2 : infoRows) {
                    renderUtil.textTopLeft(Fonts.INTER_MEDIUM, infoRow2.label(), 5.0F, f10, i3, 6.0F)
                        .textTopRight(Fonts.INTER_MEDIUM, infoRow2.value(), 5.0F, f10, i4, 6.0F);
                    f10 += 6.0F + 3.0F;
                }
            }
        }
    }

    private List<ConfigSelectorComponent.InfoRow> buildInfoRows(CloudConfigInfo cloudConfigInfo) {
        ArrayList arrayList = new ArrayList();
        if (cloudConfigInfo == null) {
            String string = this.getCurrentStorage().isConnected()
                ? Language.translate("config.default", "Default")
                : Language.translate("config.local", "Local config");
            arrayList.add(new ConfigSelectorComponent.InfoRow(Language.translate("config.info.name", "Name"), string));
            return arrayList;
        }

        String string2 = cloudConfigInfo.getAuthorName();
        if (string2 != null && !string2.isEmpty()) {
            arrayList.add(new ConfigSelectorComponent.InfoRow(Language.translate("config.info.owner", "Owner"), string2));
        }

        if (cloudConfigInfo.getModifiedAt() > 0L) {
            arrayList.add(
                new ConfigSelectorComponent.InfoRow(
                    Language.translate("config.info.modified", "Modified"), DATE_FORMAT.format(Instant.ofEpochMilli(cloudConfigInfo.getModifiedAt()))
                )
            );
        }

        if (cloudConfigInfo.getCreatedAt() > 0L) {
            arrayList.add(
                new ConfigSelectorComponent.InfoRow(
                    Language.translate("config.info.created", "Created"), DATE_FORMAT.format(Instant.ofEpochMilli(cloudConfigInfo.getCreatedAt()))
                )
            );
        }

        if (!cloudConfigInfo.getBuild().isEmpty()) {
            arrayList.add(new ConfigSelectorComponent.InfoRow(Language.translate("config.info.build", "Build"), cloudConfigInfo.getBuild()));
        }

        if (!cloudConfigInfo.isShared() && !cloudConfigInfo.getShareId().isEmpty()) {
            arrayList.add(
                new ConfigSelectorComponent.InfoRow(
                    Language.translate("config.info.access", "Access"),
                    cloudConfigInfo.isPublic() ? Language.translate("config.edit.public", "Public") : Language.translate("config.edit.private", "Private")
                )
            );
        }

        return arrayList;
    }

    private CloudConfigInfo getActiveConfig() {
        String string = this.getCurrentStorage().getActiveId();
        if (string.isEmpty()) {
            return null;
        }

        for (CloudConfigEntry cloudConfigEntry : this.getCurrentStorage().getEntries()) {
            if (cloudConfigEntry.id().equals(string)) {
                if (this.cachedActiveConfig == null
                    || !string.equals(this.cachedActiveConfig.getId())
                    || this.cachedActiveConfig.getModifiedAt() != cloudConfigEntry.modifiedAtMs()) {
                    this.cachedActiveConfig = new CloudConfigInfo(cloudConfigEntry);
                }

                return this.cachedActiveConfig;
            }
        }

        return null;
    }

    private String getActiveConfigName() {
        CloudConfigSync cloudConfigSync = this.getCurrentStorage();
        if (!cloudConfigSync.isConnected()) {
            return Language.translate("config.local", "Local config");
        }

        String string = cloudConfigSync.getActiveId();
        if (string.isEmpty()) {
            return Language.translate("config.default", "Default");
        }

        String string2 = this.findConfigName(string);
        return string2 != null ? string2 : Language.translate("config.default", "Default");
    }

    private float computeButtonWidth() {
        float f = this.size.y + 4.0F;
        float f2 = 145.0F - f - 4.0F - 23.0F;
        float f3 = Math.min(Fonts.getWidth(this.getActiveConfigName(), 5.5F), f2);
        float f4 = 4.0F + f3 + 4.0F + 9.0F + 6.0F + (this.currentTab != ConfigSelectorComponent.ConfigTab.CLIENT ? 9.0F : 0.0F);
        float f5 = f + 4.0F + f4;
        float f6 = Math.max(f5, this.size.x);
        float f7 = Math.max(this.size.x, this.screenWidth - this.position.x - 4.0F);
        return Math.min(f6, f7);
    }

    private void renderPopup(DrawContext drawContext, int i, int i2, float f) {
        this.popupAnimation.update();
        this.popupAnimation.animate(this.open ? 1.0 : 0.0, this.open ? 0.3 : 0.2, this.open ? Easings.BACK_OUT : Easings.QUAD_IN);
        this.resetTooltip.update(Language.translate("config.header.reset", "Reset"), Language.translate("config.header.reset.desc", ""), List.of());
        this.importTooltip.update(Language.translate("config.header.import", "Import"), Language.translate("config.header.import.desc", ""), List.of());
        this.newTooltip.update(Language.translate("config.header.new", "New config"), Language.translate("config.header.new.desc", ""), List.of());
        float f2 = this.popupAnimation.get();
        if (f2 <= 0.001F) {
            this.popupBounds.clear();
            this.searchBounds.clear();
            this.sortButtonBounds.clear();
            this.listBounds.clear();
            this.nameInputBounds.clear();
            this.actionMenuBounds.clear();
            this.actionMenuAnimation.forceValue(0.0);
            this.addCodePopupBounds.clear();
            this.addCodeAnimation.forceValue(0.0);
            this.addCodeOpen = false;
            this.editPopupBounds.clear();
            this.editAnimation.forceValue(0.0);
            this.editOpen = false;
            this.scrollbar.reset();
        } else {
            boolean flag = !this.getCurrentStorage().isConnected();
            float f3 = Math.min(f2, 1.0F) * this.getAlpha();
            float f4 = Math.min(f2, 1.0F);
            float f5 = this.computeListHeight();
            float f6 = Math.min(100.0F, f5);
            boolean flag2;
            float f7;
            if ((flag || this.entries.isEmpty()) && !this.creatingNew) {
                flag2 = true;
                f7 = 18.0F;
            } else {
                flag2 = false;
                f7 = f6;
            }

            float f8 = f7;
            float f9 = this.creatingNew ? 20.0F : 0.0F;
            float f10 = 58.5F + 6.5F + f9 + f8 + 4.0F;
            float f11 = GuiRenderUtil.clampToScreenX(this.buttonBounds.x, 145.0F);
            float f12 = GuiRenderUtil.clampToScreenY(this.buttonBounds.y, f10);
            this.popupBounds.set(f11, f12, 145.0F, f10);
            float f13 = 0.92F + 0.08F * f4;
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f11 + 145.0F / 2.0F, f12, f13);
            GuiRenderUtil.drawPopup(drawContext, f11, f12, 145.0F, f10, f3, 8.0F);
            this.renderHeader(drawContext, f11, f12, 145.0F, f3, i, i2);
            float f14 = f12 + 20.0F + 0.5F + 3.0F;
            this.renderDivider(drawContext, f11 + 4.0F, f12 + 20.0F, 145.0F - 8.0F, f3);
            this.renderTabs(drawContext, f11, f14, 145.0F, f3, i, i2);
            float f15 = f14 + 14.0F + 3.0F;
            this.renderSearchRow(drawContext, f11, f15, 145.0F, f3, i, i2);
            float f16 = f15 + 18.0F + 3.0F;
            this.renderDivider(drawContext, f11 + 4.0F, f16, 145.0F - 8.0F, f3);
            float f17 = f16 + 0.5F + 3.0F;
            if (this.creatingNew) {
                this.nameInputBounds.set(f11 + 4.0F, f17, 145.0F - 8.0F - 5.0F, 18.0F);
                this.renderNameInput(drawContext, this.nameInputBounds, f3);
                f17 += 20.0F;
            }

            if (flag2) {
                this.listBounds.clear();
                String string = flag
                    ? Language.translate("config.offline", "Cloud unavailable — no connection")
                    : Language.translate("config.empty", "No configs");
                new RenderUtil(drawContext, f11, f17, 145.0F, 18.0F)
                    .textCenter(Fonts.INTER_MEDIUM, string, ColorUtil.multiplyAlpha(Theme.getSecondaryText(), 0.5F * f3), 5.5F);
            } else {
                this.renderConfigList(drawContext, f11, f17, 145.0F, f6, f5, f3, f4, i, i2, f);
            }

            MatrixUtil.pop(drawContext);
            this.resetTooltip
                .render(
                    drawContext,
                    this.resetButtonBounds.x,
                    this.resetButtonBounds.y,
                    this.resetButtonBounds.width,
                    this.resetButtonBounds.height,
                    HoverTooltip.Side.RIGHT,
                    f3
                );
            this.importTooltip
                .render(
                    drawContext,
                    this.importButtonBounds.x,
                    this.importButtonBounds.y,
                    this.importButtonBounds.width,
                    this.importButtonBounds.height,
                    HoverTooltip.Side.RIGHT,
                    f3
                );
            this.newTooltip
                .render(
                    drawContext,
                    this.newButtonBounds.x,
                    this.newButtonBounds.y,
                    this.newButtonBounds.width,
                    this.newButtonBounds.height,
                    HoverTooltip.Side.RIGHT,
                    f3
                );
            this.renderActionMenu(drawContext, f3, i, i2);
            this.renderAddCodePopup(drawContext, f3, i, i2);
            this.renderEditPopup(drawContext, f3, i, i2, f);
        }
    }

    private void renderTabs(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2) {
        float f5 = f3 - 8.0F;
        int i3 = ConfigSelectorComponent.ConfigTab.VALUES.length;
        float f6 = (f5 - 3.0F * (i3 - 1)) / i3;

        for (int i4 = 0; i4 < i3; i4++) {
            ConfigSelectorComponent.ConfigTab configTab = ConfigSelectorComponent.ConfigTab.VALUES[i4];
            float f7 = f + 4.0F + (f6 + 3.0F) * i4;
            this.tabBounds[i4].set(f7, f2, f6, 14.0F);
            this.tabSelectAnimations[i4].update();
            this.tabSelectAnimations[i4].animate(configTab == this.currentTab ? 1.0 : 0.0, 0.35, configTab == this.currentTab ? Easings.QUART_OUT : Easings.QUAD_IN);
            this.tabHoverAnimations[i4].update();
            this.tabHoverAnimations[i4].animate(this.tabBounds[i4].contains(i, i2) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
            float f8 = this.tabSelectAnimations[i4].get();
            float f9 = Math.max(f8, this.tabHoverAnimations[i4].get() * 0.5F);
            if (f9 > 0.001F) {
                GuiRenderUtil.drawHighlight(drawContext, f7, f2, f6, 14.0F, f9 * f4, Radius.all(5.0F));
            }

            int i5 = Theme.withAlpha(Theme.getPrimary(), (0.55F + 0.45F * f8) * f4);
            int i6 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.5F + 0.5F * f8), f4);
            new RenderUtil(drawContext, f7, f2, f6, 14.0F)
                .icon(configTab.icon, 4.0F, 3.5F, 7.0F, i5)
                .scrollingTextMiddle(Fonts.INTER_MEDIUM, configTab.getDisplayName(), 13.5F, 0.0F, f6 - 16.0F, i6, 5.5F);
        }
    }

    private void renderHeader(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2) {
        int i3 = ColorUtil.multiplyAlpha(Theme.getPrimary(), f4);
        new RenderUtil(drawContext, f, f2, f3, 20.0F)
            .icon(Icon.FOLDER, 6.0F, 6.5F, 7.0F, i3)
            .textMiddleLeft(Fonts.INTER_MEDIUM, Language.translate("config.title", "Configs"), 16.0F, 0.0F, i3, 6.0F);
        if (this.resetPending && this.resetConfirmTimer.hasPassed(2500.0)) {
            this.resetPending = false;
        }

        float f5 = f + f3 - 4.0F;
        this.newButtonBounds.set(f5 - 13.0F, f2, 13.0F, 20.0F);
        this.importButtonBounds.set(this.newButtonBounds.x - 13.0F, f2, 13.0F, 20.0F);
        if (this.currentTab == ConfigSelectorComponent.ConfigTab.CLIENT) {
            this.resetButtonBounds.set(this.importButtonBounds.x - 13.0F, f2, 13.0F, 20.0F);
        } else {
            this.resetButtonBounds.clear();
        }

        if (this.resetButtonBounds.contains(i, i2)) {
            this.resetTooltip.markHovered();
        }

        if (this.importButtonBounds.contains(i, i2)) {
            this.importTooltip.markHovered();
        }

        if (this.newButtonBounds.contains(i, i2)) {
            this.newTooltip.markHovered();
        }

        if (this.currentTab == ConfigSelectorComponent.ConfigTab.CLIENT) {
            this.renderIconButtonColored(
                drawContext,
                this.resetButtonBounds,
                Icon.REFRESH,
                this.resetHoverAnimation,
                9.0F,
                f4,
                i,
                i2,
                this.resetPending ? DANGER_COLOR : Theme.getSecondaryText()
            );
        }

        this.renderIconButton(drawContext, this.importButtonBounds, Icon.DOWNLOAD, this.importHoverAnimation, 9.0F, f4, i, i2);
        this.renderIconButton(drawContext, this.newButtonBounds, Icon.PLUS, this.newHoverAnimation, 9.0F, f4, i, i2);
    }

    private void renderIconButton(DrawContext drawContext, ConfigSelectorComponent.Bounds bounds, Icon icon, Animation animation, float f, float f2, int i, int i2) {
        this.renderIconButtonColored(drawContext, bounds, icon, animation, f, f2, i, i2, Theme.getSecondaryText());
    }

    private void renderIconButtonColored(
        DrawContext drawContext, ConfigSelectorComponent.Bounds bounds, Icon icon2, Animation animation, float f, float f2, int i, int i2, int i3
    ) {
        animation.update();
        animation.animate(bounds.contains(i, i2) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f3 = animation.get();
        float f4 = Math.min(bounds.width, bounds.height);
        if (f3 > 0.001F) {
            GuiRenderUtil.drawHighlight(
                drawContext,
                bounds.x + bounds.width / 2.0F - f4 / 2.0F,
                bounds.y + bounds.height / 2.0F - f4 / 2.0F,
                f4,
                f4,
                f2 * f3,
                Radius.all(f4 / 3.0F)
            );
        }

        int i4 = Theme.withAlpha(i3, (0.55F + 0.4F * f3) * f2);
        new RenderUtil(drawContext, bounds.x, bounds.y, bounds.width, bounds.height)
            .icon(icon2, bounds.width / 2.0F - f / 2.0F, bounds.height / 2.0F - f / 2.0F, f, i4);
    }

    private void renderSearchRow(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2) {
        float f5 = f3 - 8.0F - 18.0F - 2.0F;
        this.searchBounds.set(f + 4.0F, f2, f5, 18.0F);
        this.sortButtonBounds.set(f + f3 - 4.0F - 18.0F, f2, 18.0F, 18.0F);
        GuiRenderUtil.drawOutlined(drawContext, this.searchBounds.x, this.searchBounds.y, this.searchBounds.width, this.searchBounds.height, f4 * 0.7F, 18.0F / 3.0F)
            .icon(Icon.SEARCH, 3.5F, 18.0F / 2.0F - 4.0F, 8.0F, ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f4));
        this.searchTextPos.set(this.searchBounds.x + 14.0F, f2 + 18.0F / 2.0F - 2.75F);
        this.getSearchField().render(drawContext, this.searchTextPos, ColorUtil.multiplyAlpha(Theme.getTextColor(), f4), f5 - 17.0F);
        this.renderIconButton(drawContext, this.sortButtonBounds, this.sortMode.icon, this.sortHoverAnimation, 9.0F, f4, i, i2);
    }

    private void renderConfigList(
        DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6, float f7, int i, int i2, float f8
    ) {
        float f9 = f + 4.0F;
        float f10 = f3 - 8.0F - 5.0F;
        this.listBounds.set(f9, f2, f10 + 5.0F, f4);
        this.scrollbar.update(f5, f4);
        this.scrollbar.updateHover(i, i2);
        if (f4 > 0.0F && ScissorStack.push(drawContext, f9, f2, f10, f4)) {
            try {
                float f11 = f2 + this.scrollbar.getScroll();

                for (ConfigEntryComponent configEntryComponent : this.entries) {
                    Animation animation = this.entryAnimations.get(configEntryComponent.getConfig().getId());
                    float f12 = animation != null ? animation.get() : 1.0F;
                    if (!(f12 <= 0.001F)) {
                        float f13 = 18.0F * f12;
                        configEntryComponent.alphaMultiplier = f7 * f12 * this.alphaFactor;
                        configEntryComponent.position.set(f9, f11);
                        configEntryComponent.size.set(f10, f13);
                        float f14 = f11 + f13;
                        if (f14 >= f2 && f11 <= f2 + f4) {
                            configEntryComponent.render(drawContext, i, i2, f8);
                        }

                        f11 += 20.0F * f12;
                    }
                }
            } finally {
                ScissorStack.pop(drawContext);
            }

            this.scrollbar.render(drawContext, f + f3 - 4.0F - 2.0F, f2, f4, f5, f6);
        }
    }

    private void renderActionMenu(DrawContext drawContext, float f, int i, int i2) {
        if (this.actionMenuConfigId != null) {
            ConfigEntryComponent configEntryComponent = this.entriesById.get(this.actionMenuConfigId);
            if (configEntryComponent != null) {
                this.actionMenuConfig = configEntryComponent.getConfig();
            }
        }

        this.actionMenuAnimation.update();
        this.actionMenuAnimation.animate(this.actionMenuConfigId != null ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
        float f2 = this.actionMenuAnimation.get();
        if (this.actionMenuConfigId == null && f2 <= 0.001F) {
            this.actionMenuBounds.clear();
            this.actionMenuConfig = null;
        } else {
            CloudConfigInfo cloudConfigInfo = this.actionMenuConfig;
            if (cloudConfigInfo == null) {
                this.actionMenuBounds.clear();
            } else {
                boolean flag = cloudConfigInfo.isShared();
                float f3 = 100.0F - 4.5F * 2.0F;
                int i3 = flag ? 1 : 4;
                float f4 = 4.5F + 16.0F + 6.5F + 14.0F * i3 + 6.5F + 14.0F + 4.5F;
                float f5 = this.actionMenuAnchorY > 0.0F ? this.popupBounds.y + this.actionMenuAnchorY : this.popupBounds.y + this.popupBounds.height / 2.0F;
                float f6 = GuiRenderUtil.placeBeside(this.popupBounds.x, this.popupBounds.x + this.popupBounds.width, 100.0F, 4.0F);
                float f7 = GuiRenderUtil.clampToScreenY(f5 - f4 / 2.0F, f4);
                float f8 = f * f2;
                this.actionMenuBounds.set(f6, f7, 100.0F, f4);
                float f9 = 0.92F + 0.08F * f2;
                MatrixUtil.push(drawContext);
                MatrixUtil.scale(drawContext, f6 + 50.0F, f7 + f4 / 2.0F, f9);
                GuiRenderUtil.drawPopup(drawContext, f6, f7, 100.0F, f4, f8, 10.0F);
                float f10 = f6 + 4.5F;
                float f11 = f7 + 4.5F + (16.0F - 14.0F) / 2.0F;
                this.renderAvatar(drawContext, cloudConfigInfo, f10, f11, 14.0F, f8);
                float f12 = f10 + 14.0F + 4.0F;
                String string = cloudConfigInfo.getAuthorName() != null && !cloudConfigInfo.getAuthorName().isEmpty() ? cloudConfigInfo.getAuthorName() : Profile.getUsername();
                String string2 = flag
                    ? Language.translate("config.shared.by", "Shared by") + " " + string
                    : (cloudConfigInfo.getModifiedAt() > 0L ? string + "  ·  " + DATE_FORMAT.format(Instant.ofEpochMilli(cloudConfigInfo.getModifiedAt())) : string);
                float f13 = f3 - 14.0F - 4.0F;
                new RenderUtil(drawContext, f12, f7 + 4.5F, f13, 16.0F)
                    .scrollingTextTop(Fonts.INTER_MEDIUM, cloudConfigInfo.getName(), 0.0F, 0.5F, f13, ColorUtil.multiplyAlpha(Theme.getTextColor(), f8), 6.0F)
                    .scrollingTextTop(Fonts.INTER_MEDIUM, string2, 0.0F, 9.0F, f13, ColorUtil.multiplyAlpha(Theme.getSecondaryText(), 0.7F * f8), 5.5F);
                float f14 = f7 + 4.5F + 16.0F + 3.0F;
                this.renderDivider(drawContext, f6 + 4.5F, f14, f3, f8);
                float f15 = f14 + 3.5F;
                this.loadActionBounds.set(f6 + 4.5F, f15, f3, 14.0F);
                this.renderActionRow(
                    drawContext,
                    this.loadActionBounds,
                    Icon.CLOUD_DOWNLOAD,
                    Language.translate("config.action.load", "Load"),
                    false,
                    f8,
                    this.loadHoverAnimation,
                    i,
                    i2
                );
                float f16 = f15 + 14.0F;
                if (flag) {
                    this.selectActionBounds.clear();
                    this.editActionBounds.clear();
                    this.shareActionBounds.clear();
                } else {
                    this.selectActionBounds.set(f6 + 4.5F, f16, f3, 14.0F);
                    this.renderActionRow(
                        drawContext,
                        this.selectActionBounds,
                        Icon.STAR,
                        Language.translate("config.action.select", "Select"),
                        false,
                        f8,
                        this.selectHoverAnimation,
                        i,
                        i2
                    );
                    float f17 = f16 + 14.0F;
                    this.editActionBounds.set(f6 + 4.5F, f17, f3, 14.0F);
                    this.renderActionRow(
                        drawContext,
                        this.editActionBounds,
                        Icon.PALETTE,
                        Language.translate("config.action.edit", "Edit"),
                        false,
                        f8,
                        this.editHoverAnimation,
                        i,
                        i2
                    );
                    float f18 = f17 + 14.0F;
                    this.shareActionBounds.set(f6 + 4.5F, f18, f3, 14.0F);
                    this.renderActionRow(
                        drawContext,
                        this.shareActionBounds,
                        Icon.LINK,
                        Language.translate("config.action.share", "Share"),
                        false,
                        f8,
                        this.shareHoverAnimation,
                        i,
                        i2
                    );
                    f16 = f18 + 14.0F;
                }

                float f19 = f16 + 3.0F;
                this.renderDivider(drawContext, f6 + 4.5F, f19, f3, f8);
                float f20 = f19 + 3.5F;
                this.deleteActionBounds.set(f6 + 4.5F, f20, f3, 14.0F);
                this.renderActionRow(
                    drawContext,
                    this.deleteActionBounds,
                    Icon.TRASH,
                    Language.translate("config.action.delete", "Delete"),
                    true,
                    f8,
                    this.deleteHoverAnimation,
                    i,
                    i2
                );
                MatrixUtil.pop(drawContext);
            }
        }
    }

    private void renderActionRow(
        DrawContext drawContext, ConfigSelectorComponent.Bounds bounds, Icon icon2, String string, boolean flag, float f, Animation animation, int i, int i2
    ) {
        animation.update();
        animation.animate(bounds.contains(i, i2) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f2 = animation.get();
        if (f2 > 0.001F) {
            GuiRenderUtil.drawHighlight(drawContext, bounds.x, bounds.y, bounds.width, bounds.height, f * f2 * 0.8F, Radius.all(bounds.height / 3.0F));
        }

        int i3 = flag ? ColorUtil.rgb(255, 94, 94) : Theme.getTextColor();
        int i4 = Theme.withAlpha(i3, (flag ? 1.0F : 0.8F + 0.2F * f2) * f);
        new RenderUtil(drawContext, bounds.x, bounds.y, bounds.width, bounds.height)
            .icon(icon2, 3.0F, bounds.height / 2.0F - 4.25F, 8.5F, i4)
            .scrollingTextMiddle(Fonts.INTER_MEDIUM, string, 16.0F, 0.0F, bounds.width - 18.0F, i4, 5.5F);
    }

    private void renderDivider(DrawContext drawContext, float f, float f2, float f3, float f4) {
        float f5 = f3 - 8.0F;
        if (!(f5 <= 0.0F)) {
            GuiRenderUtil.drawOverlay(drawContext, f + 4.0F, MathUtil.roundToStep(f2, 0.5F), f5, 0.5F, f4);
        }
    }

    private void renderAvatar(DrawContext drawContext, CloudConfigInfo cloudConfigInfo, float f, float f2, float f3, float f4) {
        Radius radius = Radius.all(f3 / 3.0F);
        String string = cloudConfigInfo.getAvatarUrl() != null && !cloudConfigInfo.getAvatarUrl().isEmpty() ? cloudConfigInfo.getAvatarUrl() : Profile.getAvatarUrl();
        Identifier identifier = AvatarTextureLoader.getTexture(string);
        if (identifier != null) {
            GuiRenderUtil.bindTexture(identifier);
            if (GuiRenderUtil.getBoundTexture() != null) {
                new RenderUtil(drawContext, f, f2, f3, f3).frostedGlass(f4, radius);
                return;
            }
        }

        GuiRenderUtil.bindTexture(Texture.AVATAR_DEFAULT);
        if (GuiRenderUtil.getBoundTexture() != null) {
            new RenderUtil(drawContext, f, f2, f3, f3).frostedGlass(f4, radius);
        } else {
            new RenderUtil(drawContext, f, f2, f3, f3)
                .rect(ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.getBackground(), Theme.getWhite(), 0.15F), f4 * 0.6F), radius)
                .icon(Icon.USER, f3 * 0.25F, f3 * 0.25F, f3 * 0.5F, ColorUtil.setAlphaf(Theme.getSecondaryText(), f4));
        }
    }

    private void renderNameInput(DrawContext drawContext, ConfigSelectorComponent.Bounds bounds, float f) {
        int i = this.nameInvalid ? Theme.withAlpha(ColorUtil.rgb(255, 94, 94), f) : ColorUtil.multiplyAlpha(Theme.getTextColor(), f);
        int i2 = this.nameInvalid ? Theme.withAlpha(ColorUtil.rgb(255, 94, 94), 0.16F * f) : 0;
        new RenderUtil(drawContext, bounds.x, bounds.y, bounds.width, bounds.height)
            .outline(8.0F, i2, Radius.all(5.0F))
            .icon(Icon.CHECK, 3.0F, bounds.height / 2.0F - 4.25F, 8.5F, i);
        this.nameInputTextPos.set(bounds.x + 16.0F, bounds.y + bounds.height / 2.0F - 2.75F);
        this.getNameField().render(drawContext, this.nameInputTextPos, i, bounds.width - 18.0F);
    }

    private void renderAddCodePopup(DrawContext drawContext, float f, int i, int i2) {
        this.addCodeAnimation.update();
        this.addCodeAnimation.animate(this.addCodeOpen ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
        float f2 = this.addCodeAnimation.get();
        if (!this.addCodeOpen && f2 <= 0.001F) {
            this.addCodePopupBounds.clear();
        } else {
            float f3 = 4.5F + 12.0F + 16.0F + 4.0F + 16.0F + 4.5F;
            float f4 = GuiRenderUtil.clampToScreenX(this.popupBounds.x + this.popupBounds.width / 2.0F - 130.0F / 2.0F, 130.0F);
            float f5 = GuiRenderUtil.clampToScreenY(this.popupBounds.y + this.popupBounds.height / 2.0F - f3 / 2.0F, f3);
            this.addCodePopupBounds.set(f4, f5, 130.0F, f3);
            float f6 = f * f2;
            float f7 = 0.92F + 0.08F * f2;
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f4 + 130.0F / 2.0F, f5 + f3 / 2.0F, f7);
            GuiRenderUtil.drawPopup(drawContext, f4, f5, 130.0F, f3, f6, 10.0F);
            new RenderUtil(drawContext, f4 + 4.5F + 2.0F, f5 + 4.5F, 130.0F - 4.5F * 2.0F - 4.0F, 12.0F)
                .textTopLeft(
                    Fonts.INTER_MEDIUM,
                    Language.translate("config.addcode.title", "Add by code"),
                    0.0F,
                    0.0F,
                    ColorUtil.multiplyAlpha(Theme.getTextColor(), f6),
                    6.0F
                );
            float f8 = 130.0F - 4.5F * 2.0F;
            float f9 = f5 + 4.5F + 12.0F;
            this.addCodeFieldBounds.set(f4 + 4.5F, f9, f8, 16.0F);
            GuiRenderUtil.drawOutlined(
                drawContext,
                this.addCodeFieldBounds.x,
                this.addCodeFieldBounds.y,
                this.addCodeFieldBounds.width,
                this.addCodeFieldBounds.height,
                f6 * 0.7F,
                5.3333335F
            );
            this.addCodeTextPos.set(f4 + 4.5F + 5.0F, f9 + 8.0F - 2.75F);
            this.getAddCodeField().render(drawContext, this.addCodeTextPos, ColorUtil.multiplyAlpha(Theme.getTextColor(), f6), f8 - 10.0F);
            float f10 = f9 + 16.0F + 4.0F;
            this.addCodeConfirmBounds.set(f4 + 4.5F, f10, f8, 16.0F);
            this.renderActionRow(
                drawContext,
                this.addCodeConfirmBounds,
                Icon.DOWNLOAD,
                Language.translate("config.addcode.confirm", "Add"),
                false,
                f6,
                this.addCodeConfirmHoverAnimation,
                i,
                i2
            );
            MatrixUtil.pop(drawContext);
        }
    }

    private void openAddCode() {
        this.closeEdit();
        this.addCodeOpen = true;
        TextFieldComponent textFieldComponent = this.getAddCodeField();
        textFieldComponent.setText("");
        textFieldComponent.setFocused(true);
    }

    private void closeAddCode() {
        this.addCodeOpen = false;
        if (this.addCodeField != null) {
            this.addCodeField.setFocused(false);
        }
    }

    private void submitAddCode() {
        String string = this.getAddCodeField().getText().trim();
        if (!string.isEmpty() && this.ensureOnline()) {
            this.getCurrentStorage()
                .importShared(string)
                .thenAccept(
                    string2 -> runOnMainThread(
                        () -> NotificationUtil.success(NotificationSources.config(), Language.translate("config.addcode.added", "Добавлен"))
                    )
                )
                .exceptionally(
                    throwable -> {
                        runOnMainThread(
                            () -> NotificationUtil.error(NotificationSources.config(), Language.translate("config.addcode.failed", "Couldn't add config"))
                        );
                        return null;
                    }
                );
            this.closeAddCode();
        }
    }

    private TextFieldComponent getAddCodeField() {
        if (this.addCodeField == null) {
            this.addCodeField = new TextFieldComponent(Fonts.INTER_MEDIUM, 5.5F, "config.addcode.placeholder", false, false);
        }

        return this.addCodeField;
    }

    @Override
    public void mouseMoved(double d, double d2) {
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        double d = click.x();
        double d2 = click.y();
        int i = click.button();
        if (!this.isPopupVisible() || !this.popupBounds.contains(d, d2)) {
            if (this.isLeftButton(i) && this.saveButtonBounds.contains(d, d2)) {
                this.quickSave();
                return true;
            }

            if (this.isLeftButton(i) && this.selectorBounds.contains(d, d2)) {
                this.toggleOpen();
                return true;
            }
        }

        if (!this.isPopupVisible()) {
            return false;
        }

        if (this.editOpen) {
            if (this.editColorPicker.mouseClicked(click, flag)) {
                return true;
            }

            if (this.isLeftButton(i) && this.applyButtonBounds.contains(d, d2)) {
                this.applyEdit();
                return true;
            }

            if (this.isLeftButton(i)) {
                for (int i2 = 0; i2 < this.iconBounds.length; i2++) {
                    if (this.iconBounds[i2].contains(d, d2)) {
                        this.editIcon = CONFIG_ICONS[i2];
                        return true;
                    }
                }

                if (this.publicButtonBounds.contains(d, d2)) {
                    this.editPublic = true;
                    return true;
                }

                if (this.privateButtonBounds.contains(d, d2)) {
                    this.editPublic = false;
                    return true;
                }

                if (this.editNameBounds.contains(d, d2)) {
                    this.editNoteFocused = false;
                    TextFieldComponent textFieldComponent = this.getEditNameField();
                    textFieldComponent.mouseClicked(d, d2, i);
                    if (!textFieldComponent.isFocused()) {
                        textFieldComponent.setFocused(true);
                    }

                    if (this.editNoteField != null) {
                        this.editNoteField.setFocused(false);
                    }

                    return true;
                }

                if (this.editNoteBounds.contains(d, d2)) {
                    this.editNoteFocused = true;
                    TextFieldComponent textFieldComponent2 = this.getEditNoteField();
                    textFieldComponent2.mouseClicked(d, d2, i);
                    if (!textFieldComponent2.isFocused()) {
                        textFieldComponent2.setFocused(true);
                    }

                    if (this.editNameField != null) {
                        this.editNameField.setFocused(false);
                        return true;
                    }

                    return true;
                }
            }

            if (this.editPopupBounds.contains(d, d2)) {
                return true;
            }

            this.closeEdit();
            return true;
        } else if (this.addCodeOpen) {
            if (this.isLeftButton(i) && this.addCodeConfirmBounds.contains(d, d2)) {
                this.submitAddCode();
                return true;
            }

            if (!this.addCodePopupBounds.contains(d, d2)) {
                this.closeAddCode();
                return true;
            }

            if (this.isLeftButton(i)) {
                TextFieldComponent textFieldComponent3 = this.getAddCodeField();
                textFieldComponent3.mouseClicked(d, d2, i);
                if (!textFieldComponent3.isFocused()) {
                    textFieldComponent3.setFocused(true);
                    return true;
                }
            }
        } else {
            if (this.actionMenuConfigId != null && this.actionMenuAnimation.get() > 0.001) {
                if (this.isLeftButton(i) && this.loadActionBounds.contains(d, d2)) {
                    this.loadConfig(this.actionMenuConfig);
                    this.actionMenuConfigId = null;
                    return true;
                }

                if (this.actionMenuConfig != null && !this.actionMenuConfig.isShared()) {
                    if (this.isLeftButton(i) && this.selectActionBounds.contains(d, d2)) {
                        this.selectConfig(this.actionMenuConfig);
                        this.actionMenuConfigId = null;
                        return true;
                    }

                    if (this.isLeftButton(i) && this.editActionBounds.contains(d, d2)) {
                        this.openEdit(this.actionMenuConfig);
                        return true;
                    }

                    if (this.isLeftButton(i) && this.shareActionBounds.contains(d, d2)) {
                        this.shareConfig(this.actionMenuConfig);
                        this.actionMenuConfigId = null;
                        return true;
                    }
                }

                if (this.isLeftButton(i) && this.deleteActionBounds.contains(d, d2)) {
                    this.deleteConfig(this.actionMenuConfig);
                    this.actionMenuConfigId = null;
                    return true;
                }

                if (this.actionMenuBounds.contains(d, d2)) {
                    return true;
                }
            }

            if (this.isLeftButton(i)) {
                for (int i3 = 0; i3 < this.tabBounds.length; i3++) {
                    if (this.tabBounds[i3].contains(d, d2)) {
                        this.switchTab(ConfigSelectorComponent.ConfigTab.VALUES[i3]);
                        return true;
                    }
                }
            }

            if (this.isLeftButton(i) && this.newButtonBounds.contains(d, d2)) {
                this.startCreating();
                return true;
            }

            if (this.isLeftButton(i) && this.importButtonBounds.contains(d, d2)) {
                this.openAddCode();
                return true;
            }

            if (this.isLeftButton(i) && this.currentTab == ConfigSelectorComponent.ConfigTab.CLIENT && this.resetButtonBounds.contains(d, d2)) {
                this.handleReset();
                return true;
            }

            if (this.isLeftButton(i) && this.sortButtonBounds.contains(d, d2)) {
                this.sortMode = this.sortMode.next();
                return true;
            }

            if (this.creatingNew && !this.nameInputBounds.contains(d, d2)) {
                this.creatingNew = false;
                this.nameInvalid = false;
                if (this.nameField != null) {
                    this.nameField.setFocused(false);
                }
            }

            if (this.creatingNew && this.nameInputBounds.contains(d, d2) && this.isLeftButton(i)) {
                if (this.isHovered(d, d2, this.nameInputBounds.x, this.nameInputBounds.y, 14.0, this.nameInputBounds.height)) {
                    this.submitNewConfig();
                    return true;
                }

                TextFieldComponent textFieldComponent4 = this.getNameField();
                textFieldComponent4.mouseClicked(d, d2, i);
                if (!textFieldComponent4.isFocused()) {
                    textFieldComponent4.setFocused(true);
                    return true;
                }
            } else {
                this.getSearchField().mouseClicked(d, d2, i);
                if (this.isLeftButton(i) && this.scrollbar.mouseClicked(d, d2)) {
                    return true;
                }

                if (this.listBounds.contains(d, d2)) {
                    for (ConfigEntryComponent configEntryComponent : this.entries) {
                        Animation animation = this.entryAnimations.get(configEntryComponent.getConfig().getId());
                        if (animation != null
                            && !(animation.get() < 0.75F)
                            && this.configExists(configEntryComponent.getConfig().getId())
                            && this.isHovered(d, d2, configEntryComponent.position, configEntryComponent.size)) {
                            if (this.isLeftButton(i) && configEntryComponent.isFavoriteHovered(d, d2)) {
                                if (!this.ensureOnline()) {
                                    return true;
                                }

                                this.getConfigManager()
                                    .toggleFavorite(configEntryComponent.getConfig().getId())
                                    .exceptionally(
                                        throwable -> {
                                            runOnMainThread(
                                                () -> NotificationUtil.error(
                                                    NotificationSources.config(), Language.translate("config.favorite.failed", "Couldn't update favorites")
                                                )
                                            );
                                            return null;
                                        }
                                    );
                                this.sortEntries();
                                return true;
                            }

                            boolean flag2;
                            if (this.isLeftButton(i)) {
                                flag2 = configEntryComponent.isSettingsHovered(d, d2);
                            } else {
                                flag2 = false;
                            }

                            if (this.isRightButton(i) || flag2) {
                                boolean flag3 = configEntryComponent.getConfig().getId().equals(this.actionMenuConfigId);
                                this.actionMenuConfigId = flag3 ? null : configEntryComponent.getConfig().getId();
                                if (!flag3) {
                                    this.actionMenuAnchorY = configEntryComponent.position.y + configEntryComponent.size.y / 2.0F - this.popupBounds.y;
                                    return true;
                                }
                            } else if (this.isLeftButton(i)) {
                                if (!configEntryComponent.isActive()) {
                                    this.loadConfig(configEntryComponent.getConfig());
                                    this.actionMenuConfigId = null;
                                    return true;
                                }

                                if (configEntryComponent.isActionHovered(d, d2)) {
                                    this.saveConfig(configEntryComponent.getConfig());
                                    return true;
                                }

                                return true;
                            }

                            return true;
                        }
                    }
                }

                if (this.popupBounds.contains(d, d2)) {
                    return true;
                }

                this.close();
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean onMouseRelease(Click click) {
        if (this.editOpen && this.editColorPicker.onMouseRelease(click)) {
            return true;
        }

        this.scrollbar.mouseReleased();
        return this.isPopupVisible() && this.popupBounds.contains(click.x(), click.y());
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        if (this.editOpen && this.editColorPicker.mouseDragged(click, d, d2)) {
            return true;
        } else {
            return this.scrollbar.isDragging() && this.scrollbar.mouseDragged(click.x(), click.y())
                ? true
                : this.isPopupVisible() && this.popupBounds.contains(click.x(), click.y());
        }
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (!this.isPopupVisible()) {
            return false;
        } else if (this.listBounds.contains(d, d2)) {
            this.scrollbar.onScroll(d4);
            return true;
        } else {
            return this.popupBounds.contains(d, d2) || this.actionMenuAnimation.get() > 0.001 && this.actionMenuBounds.contains(d, d2);
        }
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        if (this.editOpen) {
            if (this.editColorPicker.onKeyPress(keyInput)) {
                return true;
            }

            if (keyInput.key() == 256) {
                this.closeEdit();
                return true;
            }

            if (keyInput.key() == 257 || keyInput.key() == 335) {
                this.applyEdit();
                return true;
            }

            if (this.editNoteFocused) {
                if (this.editNoteField != null && this.editNoteField.isFocused()) {
                    this.editNoteField.keyPressed(keyInput.key());
                    return true;
                }
            } else if (this.editNameField != null && this.editNameField.isFocused()) {
                this.editNameField.keyPressed(keyInput.key());
                this.editNameInvalid = false;
            }
        } else if (this.addCodeOpen) {
            if (keyInput.key() == 256) {
                this.closeAddCode();
                return true;
            }

            if (keyInput.key() == 257 || keyInput.key() == 335) {
                this.submitAddCode();
                return true;
            }

            if (this.addCodeField != null && this.addCodeField.isFocused()) {
                this.addCodeField.keyPressed(keyInput.key());
                return true;
            }
        } else {
            if (!this.creatingNew) {
                if (this.searchField != null && this.searchField.isFocused()) {
                    if (keyInput.key() == 256) {
                        this.searchField.setFocused(false);
                        return true;
                    }

                    this.searchField.keyPressed(keyInput.key());
                    return true;
                }

                if (this.isPopupVisible() && keyInput.key() == 256) {
                    if (this.actionMenuConfigId != null) {
                        this.actionMenuConfigId = null;
                        return true;
                    }

                    this.close();
                    return true;
                }

                return false;
            }

            if (keyInput.key() != 256) {
                if (keyInput.key() != 257 && keyInput.key() != 335) {
                    this.getNameField().keyPressed(keyInput.key());
                    this.nameInvalid = false;
                    return true;
                }

                this.submitNewConfig();
                return true;
            }

            this.creatingNew = false;
            this.nameInvalid = false;
            if (this.nameField != null) {
                this.nameField.setFocused(false);
                this.nameField.setReplaceOnInput(false);
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean keyReleased(KeyInput keyInput) {
        return false;
    }

    @Override
    public boolean onCharType(CharInput charInput) {
        if (this.editOpen) {
            if (this.editColorPicker.onCharType(charInput)) {
                return true;
            }

            if (this.editNoteFocused) {
                if (this.editNoteField != null && this.editNoteField.isFocused()) {
                    this.editNoteField.charTyped(charInput.codepoint());
                    return true;
                }
            } else if (this.editNameField != null && this.editNameField.isFocused()) {
                this.editNameField.charTyped(charInput.codepoint());
                this.editNameInvalid = false;
            }

            return true;
        } else if (this.addCodeOpen && this.addCodeField != null && this.addCodeField.isFocused()) {
            this.addCodeField.charTyped(charInput.codepoint());
            return true;
        } else if (this.creatingNew && this.nameField != null && this.nameField.isFocused()) {
            this.nameField.charTyped(charInput.codepoint());
            this.nameInvalid = false;
            return true;
        } else if (this.searchField != null && this.searchField.isFocused()) {
            this.searchField.charTyped(charInput.codepoint());
            return true;
        } else {
            return false;
        }
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
        this.creatingNew = false;
        this.nameInvalid = false;
        if (this.nameField != null) {
            this.nameField.setFocused(false);
            this.nameField.setReplaceOnInput(false);
        }

        if (this.searchField != null) {
            this.searchField.setFocused(false);
        }

        this.actionMenuConfigId = null;
        this.actionMenuAnchorY = 0.0F;
        this.closeAddCode();
        this.closeEdit();
    }

    @Override
    public void removed() {
    }

    private void toggleOpen() {
        if (this.open) {
            this.close();
        } else {
            this.open = true;
            this.creatingNew = false;
            this.nameInvalid = false;
            this.actionMenuConfigId = null;
            this.actionMenuAnchorY = 0.0F;
            if (this.nameField != null) {
                this.nameField.setFocused(false);
                this.nameField.setReplaceOnInput(false);
            }

            if (this.searchField != null) {
                this.searchField.setFocused(false);
            }

            this.scrollbar.reset();
            this.getCurrentStorage().refresh();
        }
    }

    private void startCreating() {
        this.creatingNew = true;
        this.nameInvalid = false;
        this.actionMenuConfigId = null;
        if (this.searchField != null) {
            this.searchField.setFocused(false);
        }

        String string = this.suggestNewName();
        TextFieldComponent textFieldComponent = this.getNameField();
        textFieldComponent.setText(string);
        textFieldComponent.setCursor(string.length());
        textFieldComponent.setFocused(true);
        textFieldComponent.setReplaceOnInput(true);
    }

    public boolean isOpen() {
        return this.open;
    }

    public boolean isPopupVisible() {
        return this.popupAnimation.get() > 0.001;
    }

    public boolean isMouseOver(double d, double d2) {
        return this.getAlpha() <= 0.001F
            ? false
            : this.buttonBounds.contains(d, d2)
                || this.isPopupVisible() && this.popupBounds.contains(d, d2)
                || this.actionMenuAnimation.get() > 0.001 && this.actionMenuBounds.contains(d, d2)
                || this.addCodeOpen && this.addCodePopupBounds.contains(d, d2)
                || this.editOpen && this.editPopupBounds.contains(d, d2)
                || this.editOpen && this.editColorPicker.isMouseOverPicker(d, d2);
    }

    @Override
    public float getAlpha() {
        return super.getAlpha() * this.alphaFactor;
    }

    public void setAlphaFactor(float f) {
        this.alphaFactor = Math.max(0.0F, Math.min(f, 1.0F));
    }

    private ConfigManager getConfigManager() {
        return Sadnes.getInstance().getConfigManager();
    }

    private CloudConfigSync getCurrentStorage() {
        CloudConfigSync cloudConfigSync = switch (this.currentTab) {
            case CLIENT -> this.getConfigManager().getCloudSync();
            case AUTOBUY -> Sadnes.getInstance().getAutoBuyManager().getCloudSync();
            case COLLECTOR -> Sadnes.getInstance().getCollectorManager().getCloudConfig();
        };
        return cloudConfigSync != null ? cloudConfigSync : this.getConfigManager().getCloudSync();
    }

    private void switchTab(ConfigSelectorComponent.ConfigTab configTab) {
        if (configTab != this.currentTab) {
            this.currentTab = configTab;
            this.actionMenuConfigId = null;
            this.actionMenuConfig = null;
            this.creatingNew = false;
            this.nameInvalid = false;
            this.closeAddCode();
            this.closeEdit();
            if (this.searchField != null) {
                this.searchField.setText("");
            }

            this.scrollbar.reset();
            this.entries.clear();
            this.orderedIds.clear();
            this.entriesById.clear();
            this.entryAnimations.clear();
            this.refreshEntries();
            this.getCurrentStorage().refresh();
        }
    }

    private static void runOnMainThread(Runnable runnable) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient != null) {
            minecraftClient.execute(runnable);
        } else {
            runnable.run();
        }
    }

    private boolean ensureOnline() {
        if (this.getCurrentStorage().isConnected()) {
            return true;
        }

        NotificationUtil.error(NotificationSources.config(), Language.translate("config.offline", "Cloud unavailable — no connection"));
        return false;
    }

    private void quickSave() {
        if (this.ensureOnline()) {
            String string = this.getCurrentStorage().getActiveId();
            CloudConfigInfo cloudConfigInfo = string.isEmpty() ? null : this.findConfig(string);
            if (cloudConfigInfo == null) {
                if (!this.open) {
                    this.toggleOpen();
                }

                this.startCreating();
            } else {
                this.saveConfig(cloudConfigInfo);
            }
        }
    }

    private void loadConfig(CloudConfigInfo cloudConfigInfo) {
        if (cloudConfigInfo != null && this.ensureOnline()) {
            this.getCurrentStorage().load(cloudConfigInfo.getId()).exceptionally(throwable -> {
                runOnMainThread(() -> NotificationUtil.error(NotificationSources.config(), Language.translate("config.loadfailed", "Couldn't load config")));
                return null;
            });
        }
    }

    private void saveConfig(CloudConfigInfo cloudConfigInfo) {
        if (cloudConfigInfo != null && this.ensureOnline()) {
            CloudConfigMeta cloudConfigMeta = this.createMetadata(cloudConfigInfo.getIcon(), cloudConfigInfo.getColor(), cloudConfigInfo.getDescription(), cloudConfigInfo.getCreatedAt());
            this.getCurrentStorage()
                .update(cloudConfigInfo.getId(), cloudConfigInfo.getName(), cloudConfigMeta)
                .thenRun(() -> runOnMainThread(() -> NotificationUtil.success(NotificationSources.config(), Language.translate("config.saved", "Сохранён"))))
                .exceptionally(throwable -> {
                    runOnMainThread(() -> NotificationUtil.error(NotificationSources.config(), Language.translate("config.savefailed", "Couldn't save config")));
                    return null;
                });
        }
    }

    private void selectConfig(CloudConfigInfo cloudConfigInfo) {
        if (cloudConfigInfo != null && !cloudConfigInfo.isShared() && this.ensureOnline()) {
            this.getCurrentStorage()
                .setActive(cloudConfigInfo.getId())
                .exceptionally(
                    throwable -> {
                        runOnMainThread(
                            () -> NotificationUtil.error(NotificationSources.config(), Language.translate("config.selectfailed", "Couldn't select config"))
                        );
                        return null;
                    }
                );
        }
    }

    private void deleteConfig(CloudConfigInfo cloudConfigInfo) {
        if (cloudConfigInfo != null && this.ensureOnline()) {
            this.getCurrentStorage()
                .delete(cloudConfigInfo.getId())
                .exceptionally(
                    throwable -> {
                        runOnMainThread(
                            () -> NotificationUtil.error(NotificationSources.config(), Language.translate("config.deletefailed", "Couldn't delete config"))
                        );
                        return null;
                    }
                );
        }
    }

    private void shareConfig(CloudConfigInfo cloudConfigInfo) {
        if (cloudConfigInfo != null && !cloudConfigInfo.isShared() && this.ensureOnline()) {
            boolean flag = cloudConfigInfo.isPublic();
            this.getCurrentStorage().lookupShare(cloudConfigInfo.getId(), flag).thenAccept(shareResultPacket -> runOnMainThread(() -> {
                if (!shareResultPacket.isSuccess()) {
                    String string = shareResultPacket.getMessage();
                    String string2 = NotificationSources.config();
                    if (string != null && !string.isEmpty()) {
                        NotificationUtil.error(string2, string);
                    } else {
                        NotificationUtil.error(string2, Language.translate("config.share.failed", "Share failed"));
                    }
                } else {
                    MinecraftClient.getInstance().keyboard.setClipboard(shareResultPacket.getShareId());
                    String string3 = NotificationSources.config();
                    if (flag) {
                        NotificationUtil.success(string3, Language.translate("config.share.codecopied", "Code copied"));
                    } else {
                        NotificationUtil.success(string3, Language.translate("config.share.invitecopied", "One-time code copied"));
                    }
                }
            })).exceptionally(throwable -> {
                runOnMainThread(() -> NotificationUtil.error(NotificationSources.config(), Language.translate("config.share.failed", "Share failed")));
                return null;
            });
        }
    }

    private void handleReset() {
        if (this.resetPending && !this.resetConfirmTimer.hasPassed(2500.0)) {
            this.resetPending = false;
            ConfigManager configManager = this.getConfigManager();
            if (configManager != null && configManager.resetToDefaults()) {
                NotificationUtil.success(NotificationSources.config(), Language.translate("config.reset.done", "Settings reset"));
            } else {
                NotificationUtil.error(NotificationSources.config(), Language.translate("config.reset.failed", "Couldn't reset settings"));
            }
        } else {
            this.resetPending = true;
            this.resetConfirmTimer.reset();
            NotificationUtil.warning(NotificationSources.config(), Language.translate("config.reset.confirm", "Click again to reset settings"));
        }
    }

    private void submitNewConfig() {
        if (this.ensureOnline()) {
            TextFieldComponent textFieldComponent = this.getNameField();
            String string = textFieldComponent.getText().trim();
            if (!string.isEmpty() && !this.isInvalidName(string)) {
                this.creatingNew = false;
                this.nameInvalid = false;
                textFieldComponent.setFocused(false);
                textFieldComponent.setReplaceOnInput(false);
                this.actionMenuConfigId = null;
                CloudConfigMeta cloudConfigMeta = this.createMetadata(null, 0, "", 0L);
                this.getCurrentStorage()
                    .create(string, cloudConfigMeta)
                    .thenRun(
                        () -> runOnMainThread(() -> NotificationUtil.success(NotificationSources.config(), Language.translate("config.saved", "Сохранён")))
                    )
                    .exceptionally(
                        throwable -> {
                            runOnMainThread(
                                () -> NotificationUtil.error(NotificationSources.config(), Language.translate("config.savefailed", "Couldn't save config"))
                            );
                            return null;
                        }
                    );
            } else {
                this.nameInvalid = true;
                textFieldComponent.setFocused(true);
            }
        }
    }

    private CloudConfigMeta createMetadata(Icon icon, int i, String string, long l) {
        long l2 = System.currentTimeMillis();
        long l3 = l > 0L ? l : l2;
        return new CloudConfigMeta(
            icon != null ? icon.name() : "", i, string == null ? "" : string, Profile.getUsername(), Profile.getAvatarUrl(), l3, l2, ModVersion.getVersion()
        );
    }

    private TextFieldComponent getNameField() {
        if (this.nameField == null) {
            this.nameField = new TextFieldComponent(Fonts.INTER_MEDIUM, 5.5F, "config.placeholder.name", false, false);
        }

        return this.nameField;
    }

    private TextFieldComponent getSearchField() {
        if (this.searchField == null) {
            this.searchField = new TextFieldComponent(Fonts.INTER_MEDIUM, 5.5F, "search.placeholder", false, false);
        }

        return this.searchField;
    }

    private boolean isInvalidName(String string) {
        int i = string.length();
        if (i == 0) {
            return true;
        }

        if (string.getBytes(StandardCharsets.UTF_8).length > 64) {
            return true;
        }

        if (string.charAt(0) != ' ' && string.charAt(i - 1) != ' ') {
            int i2 = 0;

            while (i2 < i) {
                char c = string.charAt(i2);
                switch (c) {
                    case '"':
                    case '*':
                    case '/':
                    case ':':
                    case '<':
                    case '>':
                    case '?':
                    case '\\':
                        return true;
                    case '|':
                        return true;
                    default:
                        i2++;
                }
            }

            return string.contains("..");
        } else {
            return true;
        }
    }

    private String suggestNewName() {
        return this.uniqueName(this.currentTab.defaultConfigName);
    }

    private String uniqueName(String string) {
        HashSet hashSet = new HashSet();

        for (CloudConfigEntry cloudConfigEntry : this.getCurrentStorage().getEntries()) {
            hashSet.add(cloudConfigEntry.name());
        }

        if (!hashSet.contains(string)) {
            return string;
        }

        for (int i = 1; i < 1000; i++) {
            String string2 = string + " " + i;
            if (!hashSet.contains(string2)) {
                return string2;
            }
        }

        return string + " " + System.currentTimeMillis();
    }

    private CloudConfigInfo findConfig(String string) {
        if (string != null && !string.isEmpty()) {
            for (CloudConfigEntry cloudConfigEntry : this.getCurrentStorage().getEntries()) {
                if (cloudConfigEntry.id().equals(string)) {
                    return new CloudConfigInfo(cloudConfigEntry);
                }
            }
        }

        return null;
    }

    private String findConfigName(String string) {
        for (CloudConfigEntry cloudConfigEntry : this.getCurrentStorage().getEntries()) {
            if (cloudConfigEntry.id().equals(string)) {
                return cloudConfigEntry.name();
            }
        }

        return null;
    }

    private boolean configExists(String string) {
        for (CloudConfigEntry cloudConfigEntry : this.getCurrentStorage().getEntries()) {
            if (cloudConfigEntry.id().equals(string)) {
                return true;
            }
        }

        return false;
    }

    private void openEdit(CloudConfigInfo cloudConfigInfo) {
        if (cloudConfigInfo != null && !cloudConfigInfo.isShared()) {
            this.closeAddCode();
            this.editOpen = true;
            this.editConfigId = cloudConfigInfo.getId();
            this.editCreatedAt = cloudConfigInfo.getCreatedAt();
            this.editIcon = cloudConfigInfo.getIcon();

            for (int i = 0; i < CONFIG_ICONS.length; i++) {
                this.iconHoverAnimations[i].forceValue(0.0);
                this.iconSelectAnimations[i].forceValue(this.editIcon == CONFIG_ICONS[i] ? 1.0 : 0.0);
            }

            this.editPublic = cloudConfigInfo.isPublic();
            this.publicHoverAnimation.forceValue(0.0);
            this.privateHoverAnimation.forceValue(0.0);
            this.publicSelectAnimation.forceValue(this.editPublic ? 1.0 : 0.0);
            this.privateSelectAnimation.forceValue(this.editPublic ? 0.0 : 1.0);
            this.editColorPicker.close();
            this.editColorPicker.setValue(cloudConfigInfo.getColor() != 0 ? cloudConfigInfo.getColor() : Theme.getPrimary());
            this.editColorPicker.alphaAnimation.forceValue(1.0);
            this.editColorPicker.alphaMultiplier = 1.0F;
            this.editNameInvalid = false;
            this.editNoteFocused = false;
            this.actionMenuConfigId = null;
            TextFieldComponent textFieldComponent = this.getEditNameField();
            textFieldComponent.setText(cloudConfigInfo.getName());
            textFieldComponent.setCursor(cloudConfigInfo.getName().length());
            textFieldComponent.setFocused(true);
            textFieldComponent.setReplaceOnInput(false);
            TextFieldComponent textFieldComponent2 = this.getEditNoteField();
            textFieldComponent2.setText(cloudConfigInfo.getDescription() == null ? "" : cloudConfigInfo.getDescription());
            textFieldComponent2.setCursor(textFieldComponent2.getText().length());
            textFieldComponent2.setFocused(false);
        }
    }

    private void closeEdit() {
        this.editOpen = false;
        this.editConfigId = null;
        this.editNameInvalid = false;
        this.editNoteFocused = false;
        this.editColorPicker.close();
        if (this.editNameField != null) {
            this.editNameField.setFocused(false);
        }

        if (this.editNoteField != null) {
            this.editNoteField.setFocused(false);
        }
    }

    private void applyEdit() {
        if (this.editConfigId == null) {
            this.closeEdit();
        } else if (this.ensureOnline()) {
            String string = this.getEditNameField().getText().trim();
            boolean flag = !string.isEmpty();
            if (flag && this.isInvalidName(string)) {
                this.editNameInvalid = true;
                this.editNoteFocused = false;
                this.getEditNameField().setFocused(true);
            } else {
                CloudConfigInfo cloudConfigInfo = this.findConfig(this.editConfigId);
                String string2 = flag ? string : (cloudConfigInfo != null ? cloudConfigInfo.getName() : string);
                Icon icon = this.editIcon;
                int i = this.editColorPicker.getValue();
                String string3 = this.getEditNoteField().getText().trim();
                boolean flag2 = this.editPublic;
                CloudConfigMeta cloudConfigMeta = this.createMetadata(icon, i, string3, this.editCreatedAt);
                String string4 = this.editConfigId;
                this.getCurrentStorage()
                    .update(string4, string2, cloudConfigMeta)
                    .thenRun(
                        () -> runOnMainThread(() -> NotificationUtil.success(NotificationSources.config(), Language.translate("config.edit.saved", "Обновлён")))
                    )
                    .exceptionally(
                        throwable -> {
                            runOnMainThread(
                                () -> NotificationUtil.error(NotificationSources.config(), Language.translate("config.savefailed", "Couldn't save config"))
                            );
                            return null;
                        }
                    );
                this.getCurrentStorage().lookupShare(string4, flag2).thenAccept(shareResultPacket -> {
                    if (!shareResultPacket.isSuccess()) {
                        runOnMainThread(() -> {
                            String string5 = shareResultPacket.getMessage();
                            String string6 = NotificationSources.config();
                            if (string5 != null && !string5.isEmpty()) {
                                NotificationUtil.error(string6, string5);
                            } else {
                                NotificationUtil.error(string6, Language.translate("config.share.failed", "Share failed"));
                            }
                        });
                    }
                }).exceptionally(throwable2 -> {
                    runOnMainThread(() -> NotificationUtil.error(NotificationSources.config(), Language.translate("config.share.failed", "Share failed")));
                    return null;
                });
                this.closeEdit();
            }
        }
    }

    private void renderEditPopup(DrawContext drawContext, float f, int i, int i2, float f2) {
        this.editAnimation.update();
        this.editAnimation.animate(this.editOpen ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
        float f3 = this.editAnimation.get();
        if (!this.editOpen && f3 <= 0.001F) {
            this.editPopupBounds.clear();
        } else {
            float f4 = 150.0F - 8.0F * 2.0F;
            float f5 = (f4 - 5 * 3.0F) / 6;
            float f6 = 2 * f5 + 1 * 3.0F;
            float f7 = 8.0F
                + 12.0F
                + 4.0F
                + 16.0F
                + 4.0F
                + 9.0F
                + 4.0F
                + f6
                + 4.0F
                + 15.0F
                + 4.0F
                + 9.0F
                + 4.0F
                + 16.0F
                + 4.0F
                + 16.0F
                + 4.0F
                + 16.0F
                + 8.0F;
            float f8 = GuiRenderUtil.clampToScreenX(this.popupBounds.x + this.popupBounds.width / 2.0F - 150.0F / 2.0F, 150.0F);
            float f9 = GuiRenderUtil.clampToScreenY(this.popupBounds.y + this.popupBounds.height / 2.0F - f7 / 2.0F, f7);
            this.editPopupBounds.set(f8, f9, 150.0F, f7);
            float f10 = f * f3;
            float f11 = 0.92F + 0.08F * f3;
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f8 + 150.0F / 2.0F, f9 + f7 / 2.0F, f11);
            GuiRenderUtil.drawPopup(drawContext, f8, f9, 150.0F, f7, f10, 10.0F);
            float f12 = f8 + 8.0F;
            float f13 = f9 + 8.0F;
            new RenderUtil(drawContext, f12, f13, f4, 12.0F)
                .textTopLeft(
                    Fonts.INTER_MEDIUM,
                    Language.translate("config.edit.title", "Edit config"),
                    0.0F,
                    0.0F,
                    ColorUtil.multiplyAlpha(Theme.getTextColor(), f10),
                    6.0F
                );
            float f14 = f13 + (12.0F + 4.0F);
            this.editNameBounds.set(f12, f14, f4, 16.0F);
            GuiRenderUtil.drawOutlined(drawContext, f12, f14, f4, 16.0F, f10 * 0.7F, 16.0F / 3.0F);
            int i3 = this.editNameInvalid ? Theme.withAlpha(ColorUtil.rgb(255, 94, 94), f10) : ColorUtil.multiplyAlpha(Theme.getTextColor(), f10);
            this.editNameTextPos.set(f12 + 6.0F, f14 + 16.0F / 2.0F - 2.75F);
            this.getEditNameField().render(drawContext, this.editNameTextPos, i3, f4 - 12.0F);
            float f15 = f14 + (16.0F + 4.0F);
            new RenderUtil(drawContext, f12, f15, f4, 9.0F)
                .textTopLeft(
                    Fonts.INTER_MEDIUM,
                    Language.translate("config.edit.icon", "Icon"),
                    1.0F,
                    0.0F,
                    ColorUtil.multiplyAlpha(Theme.getSecondaryText(), 0.8F * f10),
                    5.5F
                );
            float f16 = f15 + (9.0F + 4.0F);
            this.renderIconGrid(drawContext, f12, f16, 6, f5, 3.0F, Radius.smoothAll(16.0F / 3.0F * 2.0F), f10, i, i2);
            float f17 = f16 + (f6 + 4.0F);
            this.editColorPicker.alphaMultiplier = f3;
            this.editColorPicker.position.set(f12, f17);
            this.editColorPicker.size.set(f4, 15.0F);
            this.editColorPicker.render(drawContext, i, i2, f2);
            float f18 = f17 + (15.0F + 4.0F);
            new RenderUtil(drawContext, f12, f18, f4, 9.0F)
                .textTopLeft(
                    Fonts.INTER_MEDIUM,
                    Language.translate("config.edit.visibility", "Visibility"),
                    1.0F,
                    0.0F,
                    ColorUtil.multiplyAlpha(Theme.getSecondaryText(), 0.8F * f10),
                    5.5F
                );
            float f19 = f18 + (9.0F + 4.0F);
            this.renderVisibilityToggle(drawContext, f12, f19, f4, 16.0F, f10, i, i2);
            float f20 = f19 + (16.0F + 4.0F);
            this.editNoteBounds.set(f12, f20, f4, 16.0F);
            GuiRenderUtil.drawOutlined(drawContext, f12, f20, f4, 16.0F, f10 * 0.7F, 16.0F / 3.0F);
            this.editNoteTextPos.set(f12 + 6.0F, f20 + 16.0F / 2.0F - 2.75F);
            this.getEditNoteField().render(drawContext, this.editNoteTextPos, ColorUtil.multiplyAlpha(Theme.getTextColor(), f10), f4 - 12.0F);
            float f21 = f20 + (16.0F + 4.0F);
            this.applyButtonBounds.set(f12, f21, f4, 16.0F);
            this.renderPrimaryButton(
                drawContext, this.applyButtonBounds, Icon.CHECK, Language.translate("config.edit.apply", "Apply"), f10, this.applyHoverAnimation, i, i2
            );
            MatrixUtil.pop(drawContext);
            this.editColorPicker.renderOverlay(drawContext, i, i2, f2);
        }
    }

    private void renderVisibilityToggle(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, int i, int i2) {
        float f6 = (f3 - 4.0F) / 2.0F;
        this.publicButtonBounds.set(f, f2, f6, f4);
        this.privateButtonBounds.set(f + f6 + 4.0F, f2, f6, f4);
        this.renderVisibilityButton(
            drawContext,
            this.publicButtonBounds,
            Icon.GLOBE,
            Language.translate("config.edit.public", "Public"),
            this.editPublic,
            this.publicHoverAnimation,
            this.publicSelectAnimation,
            f5,
            i,
            i2
        );
        this.renderVisibilityButton(
            drawContext,
            this.privateButtonBounds,
            Icon.USER,
            Language.translate("config.edit.private", "Private"),
            !this.editPublic,
            this.privateHoverAnimation,
            this.privateSelectAnimation,
            f5,
            i,
            i2
        );
    }

    private void renderVisibilityButton(
        DrawContext drawContext,
        ConfigSelectorComponent.Bounds bounds,
        Icon icon2,
        String string,
        boolean flag,
        Animation animation,
        Animation animation2,
        float f,
        int i,
        int i2
    ) {
        animation.update();
        animation.animate(this.editOpen && bounds.contains(i, i2) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        animation2.update();
        animation2.animate(flag ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
        float f2 = animation.get();
        float f3 = animation2.get();
        int i3 = Theme.getPrimary();
        int i4 = Theme.tintBackground(0.82F);
        int i5 = ColorUtil.interpolate(i4, Theme.getWhite(), 0.12F);
        Radius radius = Radius.smoothAll(bounds.height / 3.0F * 2.0F);
        int i6 = ColorUtil.interpolate(i4, i5, f2);
        new RenderUtil(drawContext, bounds.x, bounds.y, bounds.width, bounds.height).rect(Theme.withAlpha(i6, (0.55F + 0.25F * f2) * f), radius);
        if (f3 > 0.001F) {
            RenderUtil renderUtil = new RenderUtil(drawContext, bounds.x, bounds.y, bounds.width, bounds.height);
            renderUtil.rect(Theme.withAlpha(i3, 0.18F * f3 * f), radius);
            renderUtil.expand(Side.ALL, -0.5F);
            renderUtil.shadow(0.6F, i3, 0.55F * f3 * f, radius);
            renderUtil.expand(Side.ALL, 0.5F);
        }

        int i7 = Theme.withAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), i3, f3), (0.65F + 0.35F * Math.max(f2, f3)) * f);
        float f4 = 8.5F + 3.0F + Fonts.getWidth(string, 5.5F);
        float f5 = bounds.x + (bounds.width - f4) / 2.0F;
        new RenderUtil(drawContext, f5, bounds.y, f4, bounds.height)
            .icon(icon2, 0.0F, bounds.height / 2.0F - 8.5F / 2.0F, 8.5F, i7)
            .textTopLeft(Fonts.INTER_MEDIUM, string, 8.5F + 3.0F, bounds.height / 2.0F - 2.75F, i7, 5.5F);
    }

    private void renderIconGrid(DrawContext drawContext, float f, float f2, int i, float f3, float f4, Radius radius, float f5, int i2, int i3) {
        int i4 = Theme.getPrimary();
        int i5 = Theme.tintBackground(0.82F);
        int i6 = ColorUtil.interpolate(i5, Theme.getWhite(), 0.12F);

        for (int i7 = 0; i7 < CONFIG_ICONS.length; i7++) {
            float f6 = f + i7 % i * (f3 + f4);
            float f7 = f2 + i7 / i * (f3 + f4);
            this.iconBounds[i7].set(f6, f7, f3, f3);
            boolean flag = this.editIcon == CONFIG_ICONS[i7];
            boolean flag2;
            if (this.editOpen) {
                flag2 = this.iconBounds[i7].contains(i2, i3);
            } else {
                flag2 = false;
            }

            this.iconHoverAnimations[i7].update();
            this.iconHoverAnimations[i7].animate(flag2 ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
            this.iconSelectAnimations[i7].update();
            this.iconSelectAnimations[i7].animate(flag ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
            float f8 = this.iconHoverAnimations[i7].get();
            float f9 = this.iconSelectAnimations[i7].get();
            int i8 = ColorUtil.interpolate(i5, i6, f8);
            new RenderUtil(drawContext, f6, f7, f3, f3).rect(Theme.withAlpha(i8, (0.55F + 0.25F * f8) * f5), radius);
            if (f9 > 0.001F) {
                RenderUtil renderUtil = new RenderUtil(drawContext, f6, f7, f3, f3);
                renderUtil.rect(Theme.withAlpha(i4, 0.18F * f9 * f5), radius);
                renderUtil.expand(Side.ALL, -0.5F);
                renderUtil.shadow(0.6F, i4, 0.55F * f9 * f5, radius);
                renderUtil.expand(Side.ALL, 0.5F);
            }

            int i9 = ColorUtil.interpolate(Theme.getSecondaryText(), i4, f9);
            float f10 = (0.55F + 0.45F * Math.max(f8, f9)) * f5;
            new RenderUtil(drawContext, f6, f7, f3, f3)
                .icon(CONFIG_ICONS[i7], f3 / 2.0F - 11.0F / 2.0F, f3 / 2.0F - 11.0F / 2.0F, 11.0F, Theme.withAlpha(i9, f10));
        }
    }

    private void renderPrimaryButton(
        DrawContext drawContext, ConfigSelectorComponent.Bounds bounds, Icon icon2, String string, float f, Animation animation, int i, int i2
    ) {
        animation.update();
        animation.animate(bounds.contains(i, i2) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f2 = animation.get();
        int i3 = Theme.getPrimary();
        int i4 = ColorUtil.interpolate(i3, ColorUtil.WHITE, 0.12F * f2);
        Radius radius = Radius.smoothAll(bounds.height / 3.0F * 2.0F);
        new RenderUtil(drawContext, bounds.x, bounds.y, bounds.width, bounds.height).rect(Theme.withAlpha(i4, (0.9F + 0.1F * f2) * f), radius);
        int i5 = ColorUtil.setAlphaf(contrastTextColor(i3), f);
        float f3 = 8.0F + 3.0F + Fonts.getWidth(string, 5.5F);
        float f4 = bounds.x + (bounds.width - f3) / 2.0F;
        new RenderUtil(drawContext, f4, bounds.y, f3, bounds.height)
            .icon(icon2, 0.0F, bounds.height / 2.0F - 8.0F / 2.0F, 8.0F, i5)
            .textTopLeft(Fonts.INTER_MEDIUM, string, 8.0F + 3.0F, bounds.height / 2.0F - 2.75F, i5, 5.5F);
    }

    private static int contrastTextColor(int i) {
        float f = ColorUtil.redf(i) * 0.299F + ColorUtil.greenf(i) * 0.587F + ColorUtil.bluef(i) * 0.114F;
        return f > 0.62F ? ColorUtil.rgb(22, 22, 26) : ColorUtil.WHITE;
    }

    private TextFieldComponent getEditNameField() {
        if (this.editNameField == null) {
            this.editNameField = new TextFieldComponent(Fonts.INTER_MEDIUM, 5.5F, "config.placeholder.name", false, false);
        }

        return this.editNameField;
    }

    private TextFieldComponent getEditNoteField() {
        if (this.editNoteField == null) {
            this.editNoteField = new TextFieldComponent(Fonts.INTER_MEDIUM, 5.5F, "config.edit.note", false, false);
        }

        return this.editNoteField;
    }

    private void refreshEntries() {
        CloudConfigSync cloudConfigSync = this.getCurrentStorage();
        List<CloudConfigEntry> cloudConfigEntries = cloudConfigSync.getEntries();
        String string = cloudConfigSync.getActiveId();
        boolean flag;
        if (!this.initialized) {
            flag = this.orderedIds.isEmpty();
        } else {
            flag = false;
        }

        String string2 = this.searchField == null ? "" : this.searchField.getText().trim().toLowerCase();
        ArrayList arrayList = new ArrayList();
        HashSet hashSet = new HashSet();

        for (CloudConfigEntry cloudConfigEntry : cloudConfigEntries) {
            String string3 = cloudConfigEntry.id();
            boolean flag2;
            if (!cloudConfigEntry.shared() && string3.equals(string)) {
                flag2 = true;
            } else {
                flag2 = false;
                if (!string2.isEmpty() && !cloudConfigEntry.name().toLowerCase().contains(string2)) {
                    continue;
                }
            }

            arrayList.add(string3);
            hashSet.add(string3);
            CloudConfigInfo cloudConfigInfo = new CloudConfigInfo(cloudConfigEntry);
            ConfigEntryComponent configEntryComponent = this.entriesById.get(string3);
            if (configEntryComponent == null) {
                this.entriesById.put(string3, new ConfigEntryComponent(cloudConfigInfo, flag2));
                Animation animation = new Animation();
                animation.forceValue(flag ? 1.0 : 0.0);
                this.entryAnimations.put(string3, animation);
                this.insertOrdered(string3, arrayList);
            } else {
                configEntryComponent.update(cloudConfigInfo, flag2);
            }
        }

        for (String string4 : new ArrayList<>(this.orderedIds)) {
            Animation animation2 = this.entryAnimations.get(string4);
            if (animation2 != null) {
                boolean flag3 = hashSet.contains(string4);
                animation2.update();
                animation2.animate(flag3 ? 1.0 : 0.0, flag3 ? 0.22F : 0.18F, flag3 ? Easings.QUAD_OUT : Easings.QUAD_IN);
                if (!flag3 && animation2.get() <= 0.001F && animation2.isFinished()) {
                    this.orderedIds.remove(string4);
                    this.entriesById.remove(string4);
                    this.entryAnimations.remove(string4);
                }
            }
        }

        this.entries.clear();

        for (String string5 : this.orderedIds) {
            ConfigEntryComponent configEntryComponent2 = this.entriesById.get(string5);
            if (configEntryComponent2 != null) {
                this.entries.add(configEntryComponent2);
            }
        }

        this.sortEntries();
        if (this.actionMenuConfigId != null && !this.configExists(this.actionMenuConfigId)) {
            this.actionMenuConfigId = null;
        }

        this.initialized = true;
    }

    private void sortEntries() {
        String string = this.getCurrentStorage().getActiveId();
        Comparator<ConfigEntryComponent> comparator = Comparator.comparing((ConfigEntryComponent configEntryComponent) -> configEntryComponent.getConfig().getName().toLowerCase());
        switch (this.sortMode) {
            case NAME_ASC:
                this.entries.sort(this.favoriteThenActiveComparator(string).thenComparing(comparator));
                return;
            case NAME_DESC:
                this.entries.sort(this.favoriteThenActiveComparator(string).thenComparing(comparator.reversed()));
                return;
            case DATE_NEW:
                this.entries
                    .sort(
                        this.favoriteThenActiveComparator(string)
                            .thenComparing(Comparator.<ConfigEntryComponent>comparingLong(configEntryComponent2 -> configEntryComponent2.getConfig().getModifiedAt()).reversed())
                    );
                return;
            case ACTIVE_FIRST:
                this.entries.sort(this.favoriteThenActiveComparator(string).thenComparing(comparator));
        }
    }

    private Comparator<ConfigEntryComponent> favoriteThenActiveComparator(String string) {
        return Comparator.<ConfigEntryComponent, Integer>comparing(configEntryComponent -> configEntryComponent.isFavorite() ? 0 : 1).thenComparing(this.activeFirstComparator(string));
    }

    private Comparator<ConfigEntryComponent> activeFirstComparator(String string) {
        return Comparator.comparing(configEntryComponent -> configEntryComponent.getConfig().getId().equals(string) ? 0 : 1);
    }

    private void insertOrdered(String string, List<String> strings) {
        if (!this.orderedIds.contains(string)) {
            int i = strings.indexOf(string);
            if (i <= 0) {
                this.orderedIds.add(0, string);
            } else {
                for (int i2 = i - 1; i2 >= 0; i2--) {
                    int i3 = this.orderedIds.indexOf(strings.get(i2));
                    if (i3 >= 0) {
                        this.orderedIds.add(i3 + 1, string);
                        return;
                    }
                }

                this.orderedIds.add(string);
            }
        }
    }

    private float computeListHeight() {
        float f = 0.0F;

        for (ConfigEntryComponent configEntryComponent : this.entries) {
            Animation animation = this.entryAnimations.get(configEntryComponent.getConfig().getId());
            float f2 = animation != null ? animation.get() : 1.0F;
            if (!(f2 <= 0.001F)) {
                f += 20.0F * f2;
            }
        }

        return f;
    }

    private static ConfigSelectorComponent.Bounds[] createBoundsArray(int i) {
        ConfigSelectorComponent.Bounds[] boundses = new ConfigSelectorComponent.Bounds[i];

        for (int i2 = 0; i2 < i; i2++) {
            boundses[i2] = new ConfigSelectorComponent.Bounds();
        }

        return boundses;
    }

    private static Animation[] createAnimationArray(int i) {
        Animation[] animations = new Animation[i];

        for (int i2 = 0; i2 < i; i2++) {
            animations[i2] = new Animation();
        }

        return animations;
    }

    static final class Bounds {
        float x;
        float y;
        float width;
        float height;

        void set(float f, float f2, float f3, float f4) {
            this.x = f;
            this.y = f2;
            this.width = f3;
            this.height = f4;
        }

        void clear() {
            this.set(0.0F, 0.0F, 0.0F, 0.0F);
        }

        boolean contains(double d, double d2) {
            return d >= this.x && d <= this.x + this.width && d2 >= this.y && d2 <= this.y + this.height;
        }
    }

    enum ConfigTab {
        CLIENT(Icon.SETTINGS, "config.tab.client", "Client", "config"),
        AUTOBUY(Icon.CART_SHOPPING, "config.tab.autobuy", "AutoBuy", "autobuy"),
        COLLECTOR(Icon.PACKAGE, "config.tab.collector", "Collector", "collector");

        static final ConfigSelectorComponent.ConfigTab[] VALUES = values();
        final Icon icon;
        private final String translationKey;
        private final String fallbackName;
        final String defaultConfigName;

        ConfigTab(Icon icon2, String string, String string2, String string3) {
            this.icon = icon2;
            this.translationKey = string;
            this.fallbackName = string2;
            this.defaultConfigName = string3;
        }

        String getDisplayName() {
            return Language.translate(this.translationKey, this.fallbackName);
        }
    }

    record InfoRow(String label, String value) {
    }

    enum SortMode {
        NAME_ASC(Icon.SORT_ALPHA_DOWN),
        NAME_DESC(Icon.SORT_ALPHA_UP),
        DATE_NEW(Icon.CLOCK_HOUR_4),
        ACTIVE_FIRST(Icon.FILTER);

        final Icon icon;

        SortMode(Icon icon2) {
            this.icon = icon2;
        }

        ConfigSelectorComponent.SortMode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
    }
}
