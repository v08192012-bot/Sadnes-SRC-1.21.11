package tech.sadnes.client.ui.clickgui.panel;

import com.mojang.authlib.GameProfile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.cosmetic.Cosmetic;
import tech.sadnes.client.cosmetic.CosmeticType;
import tech.sadnes.client.feature.impl.render.Cosmetics;
import tech.sadnes.client.feature.impl.render.esp.EspTargetType;
import tech.sadnes.client.figura.FiguraAvatarManager;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.network.ModelRepository;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.AvatarIconCache;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.setting.SettingWidgets;
import tech.sadnes.client.ui.clickgui.component.Component;
import tech.sadnes.client.ui.clickgui.component.SmoothScrollbar;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class CosmeticsPanel extends Component implements IMinecraft {
    private static final float PADDING = 8.0F;
    private static final float TITLE_HEIGHT = 13.0F;
    private static final float TITLE_FONT_SIZE = 7.0F;
    private static final float UNDERLINE_HEIGHT = 1.5F;
    private static final float CLOSE_SIZE = 10.0F;
    private static final float SECTION_GAP = 8.0F;
    private static final float PREVIEW_WIDTH = 100.0F;
    private static final float PREVIEW_HEIGHT = 118.0F;
    private static final float TARGET_ROW_HEIGHT = 21.0F;
    private static final float TARGET_ROW_GAP = 4.0F;
    private static final float RADIUS = 6.0F;
    private static final int GRID_COLUMNS = 3;
    private static final float TAB_HEIGHT = 15.0F;
    private static final float CARD_GAP = 4.0F;
    private static final float CARD_LABEL_HEIGHT = 9.0F;
    private static final float GRID_PADDING = 6.0F;
    private static final float SECTION_TITLE_HEIGHT = 11.0F;
    private static final float SPINNER_THICKNESS = 2.0F;
    private static final long SPINNER_PERIOD_MS = 1100L;
    private static final float DEFAULT_PREVIEW_YAW = -28.0F;
    private static final float TAB_BAR_WIDTH = 84.0F;
    private static final float SEARCH_GAP = 4.0F;
    private final Animation openAnimation = new Animation();
    private final Animation contentAnimation = new Animation();
    private final Animation closeHoverAnimation = new Animation();
    private final Animation searchHoverAnimation = new Animation();
    private final Animation tabAnimation = new Animation();
    private final SmoothScrollbar scroll = new SmoothScrollbar();
    private Cosmetics cosmetics;
    private boolean closing;
    private CosmeticsPanel.Tab selectedTab = CosmeticsPanel.Tab.MODELS;
    private EspTargetType selectedTarget = EspTargetType.SELF;
    private List<FiguraAvatarManager.AvatarEntry> avatars = List.of();
    private final Map<EspTargetType, CosmeticsPanel.TargetRow> targetRows = new EnumMap<>(EspTargetType.class);
    private String searchText = "";
    private boolean searchFocused;
    private final Map<String, Animation> cardHoverAnimations = new HashMap<>();
    private final Animation selectFlashAnimation = new Animation();
    private String lastSelectedKey;
    private OtherClientPlayerEntity previewPlayer;
    private String previewModelName = "";
    private Path previewAvatarPath;
    private String hoveredModelName;
    private Cosmetic hoveredCosmetic;
    private float previewYaw = -28.0F;
    private boolean rotatingPreview;
    private double lastDragX;
    private float leftX;
    private float contentX;
    private float contentWidth;
    private final List<CosmeticsPanel.SectionHeader> sectionHeaders = new ArrayList<>();
    private float searchX;
    private float searchWidth;
    private float gridContentHeight;
    private float bodyY;
    private float closeButtonX;
    private float closeButtonY;
    private float previewY;
    private float targetsY;
    private float tabsY;
    private float gridY;
    private float gridHeight;
    private float cardSize = 50.0F;

    public boolean isOpen() {
        return this.cosmetics != null && !this.closing;
    }

    public boolean isVisible() {
        return this.openAnimation.get() > 0.001;
    }

    public float getOpenProgress() {
        return (float)Math.min(this.openAnimation.get(), 1.0);
    }

    public boolean isMouseOver(double d, double d2) {
        return this.isVisible() && this.isHovered(d, d2, this.position.x, this.position.y, this.size.x, this.size.y);
    }

    public boolean isBlockingInput() {
        return false;
    }

    public void open(Cosmetics cosmetics2) {
        this.cosmetics = cosmetics2;
        this.closing = false;
        this.avatars = FiguraAvatarManager.listAvatars();
        ModelRepository.fetchCatalog();
        Sadnes.getInstance().getCosmeticsManager().loadPack();
        List<EspTargetType> espTargetTypes = cosmetics2.getModelTargets();
        this.selectedTarget = espTargetTypes.isEmpty() ? EspTargetType.SELF : (EspTargetType)espTargetTypes.get(0);
        this.targetRows.clear();

        for (EspTargetType espTargetType : espTargetTypes) {
            this.targetRows.put(espTargetType, new CosmeticsPanel.TargetRow(espTargetType));
        }

        this.searchText = "";
        this.searchFocused = false;
        this.hoveredModelName = null;
        this.previewYaw = -28.0F;
        this.rotatingPreview = false;
        this.scroll.reset();
        this.cardHoverAnimations.clear();
        this.selectFlashAnimation.forceValue(0.0);
        this.contentAnimation.forceValue(0.0);
        this.contentAnimation.animate(1.0, 0.3, Easings.QUAD_OUT);
        this.openAnimation.animate(1.0, 0.35, Easings.QUART_OUT);
    }

    @Override
    public void close() {
        if (this.cosmetics != null && !this.closing) {
            this.closing = true;
            this.searchFocused = false;
            this.rotatingPreview = false;
            this.openAnimation.animate(0.0, 0.22, Easings.QUAD_IN);
        }
    }

    public void reset() {
        this.cosmetics = null;
        this.closing = false;
        this.searchFocused = false;
        this.rotatingPreview = false;
        this.openAnimation.forceValue(0.0);
        this.clearPreview();
    }

    private void clearPreview() {
        FiguraAvatarManager.clearPreview();
        this.previewPlayer = null;
        this.previewModelName = "";
        this.previewAvatarPath = null;
        this.hoveredModelName = null;
        this.hoveredCosmetic = null;
    }

    private void selectTarget(EspTargetType espTargetType) {
        this.selectedTarget = espTargetType;
    }

    private String getSelectedModel() {
        return this.cosmetics.getModel(this.selectedTarget);
    }

    private String getDisplayedModel() {
        if (this.hoveredModelName != null && !this.hoveredModelName.isBlank()) {
            return this.hoveredModelName;
        }

        String string = this.getSelectedModel();
        return string == null ? "" : string;
    }

    private List<CosmeticsPanel.Card> getInstalledCards() {
        ArrayList arrayList = new ArrayList();

        for (FiguraAvatarManager.AvatarEntry avatarEntry : this.avatars) {
            if (this.matchesSearch(avatarEntry.name())) {
                arrayList.add(new CosmeticsPanel.Card(avatarEntry.name(), avatarEntry, this.findRemoteModel(avatarEntry.name()), null));
            }
        }

        return arrayList;
    }

    private List<CosmeticsPanel.Card> getAvailableCards() {
        ArrayList arrayList = new ArrayList();

        for (ModelRepository.RemoteModel remoteModel : ModelRepository.getCatalog()) {
            if (this.matchesSearch(remoteModel.name()) && !this.isInstalled(remoteModel.name())) {
                arrayList.add(new CosmeticsPanel.Card(remoteModel.name(), null, remoteModel, null));
            }
        }

        return arrayList;
    }

    private List<CosmeticsPanel.Section> getSections() {
        ArrayList arrayList = new ArrayList();
        if (this.selectedTab == CosmeticsPanel.Tab.MODELS) {
            arrayList.add(new CosmeticsPanel.Section("", this.getInstalledCards()));
            arrayList.add(new CosmeticsPanel.Section(Language.translate("cosmetics.panel.available", "Доступные"), this.getAvailableCards()));
            return arrayList;
        }

        for (CosmeticType cosmeticType : CosmeticType.values()) {
            ArrayList arrayList2 = new ArrayList();

            for (Cosmetic cosmetic : Sadnes.getInstance().getCosmeticsManager().getCosmetics(cosmeticType)) {
                if (this.matchesSearch(cosmetic.name())) {
                    arrayList2.add(new CosmeticsPanel.Card(cosmetic.name(), null, null, cosmetic));
                }
            }

            arrayList.add(new CosmeticsPanel.Section(Language.translate(cosmeticType.getTranslationKey(), cosmeticType.getId()), arrayList2));
        }

        return arrayList;
    }

    private boolean isCardSelected(CosmeticsPanel.Card card) {
        return card.cosmetic() != null ? this.cosmetics.isWearing(this.selectedTarget, card.cosmetic()) : card.name().equalsIgnoreCase(this.getSelectedModel());
    }

    private boolean matchesSearch(String string) {
        return this.searchText.isBlank() || string.toLowerCase(Locale.ROOT).contains(this.searchText.toLowerCase(Locale.ROOT));
    }

    private boolean isInstalled(String string) {
        for (FiguraAvatarManager.AvatarEntry avatarEntry : this.avatars) {
            if (avatarEntry.name().equalsIgnoreCase(string)) {
                return true;
            }
        }

        return false;
    }

    private ModelRepository.RemoteModel findRemoteModel(String string) {
        for (ModelRepository.RemoteModel remoteModel : ModelRepository.getCatalog()) {
            if (remoteModel.name().equalsIgnoreCase(string)) {
                return remoteModel;
            }
        }

        return null;
    }

    public void render(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4, float f5) {
        this.openAnimation.update();
        this.contentAnimation.update();
        this.selectFlashAnimation.update();
        if (this.closing && this.openAnimation.isFinished() && this.openAnimation.get() <= 0.001) {
            this.cosmetics = null;
            this.closing = false;
            this.clearPreview();
        }

        if (this.cosmetics != null && !(this.openAnimation.get() <= 0.001)) {
            this.position.set(f2, f3);
            this.size.set(f4, f5);
            float f6 = this.getOpenProgress();
            float f7 = f6 * this.getAlpha();
            RenderUtil renderUtil = new RenderUtil(drawContext, f2, f3, f4, f5);
            renderUtil.blur(f7, Radius.all(8.0F), RenderUtil.BlurQuality.LOW, true);
            renderUtil.rect(Theme.withAlpha(Theme.getBackground(), 0.4F), f7, Radius.all(8.0F));
            float f8 = f3 + 6.0F;
            this.bodyY = f8 + 13.0F + 8.0F;
            float f9 = f3 + f5 - 8.0F;
            this.closeButtonX = f2 + f4 - 5.0F - 22.0F + 6.0F;
            this.closeButtonY = f3 + 4.0F + 6.0F;
            this.leftX = f2 + 8.0F;
            this.contentX = this.leftX + 100.0F + 8.0F;
            this.contentWidth = f2 + f4 - 8.0F - this.contentX;
            this.previewY = this.bodyY;
            this.targetsY = this.previewY + 118.0F + 7.0F;
            this.tabsY = this.bodyY;
            this.gridY = this.tabsY + 15.0F + 6.0F;
            this.gridHeight = f9 - this.gridY;
            this.cardSize = (this.contentWidth - 6.0F - 8.0F) / 3.0F;
            this.searchX = this.contentX + 84.0F + 4.0F;
            this.searchWidth = this.contentWidth - 84.0F - 4.0F;
            CosmeticsPanel.Card card = this.getHoveredCard(i, i2);
            this.hoveredModelName = card != null && card.local() != null ? card.name() : null;
            this.hoveredCosmetic = card == null ? null : card.cosmetic();
            this.cosmetics.setPreviewCosmetic(this.selectedTarget, this.hoveredCosmetic);
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f2 + f4 / 2.0F, f3 + f5 / 2.0F, 0.96F + 0.04F * f6);
            this.renderHeader(drawContext, f2, f8, f4, i, i2, f7);
            this.renderPreview(drawContext, f7);
            this.renderTargets(drawContext, i, i2, f7);
            this.renderTabs(drawContext, i, i2, f7);
            this.renderSearchBar(drawContext, i, i2, f7);
            this.renderGrid(drawContext, i, i2, f7);
            MatrixUtil.pop(drawContext);
        }
    }

    public void renderOverlay(DrawContext drawContext, int i, int i2, float f) {
    }

    private void renderHeader(DrawContext drawContext, float f, float f2, float f3, int i, int i2, float f4) {
        boolean flag = this.selectedTab == CosmeticsPanel.Tab.COSMETICS;
        String string = this.hoveredCosmetic != null ? this.hoveredCosmetic.name() : (flag ? "" : this.getDisplayedModel());
        String string2 = !string.isBlank() ? string : (flag ? CosmeticsPanel.Tab.COSMETICS.getDisplayName() : Language.translate("cosmetics.panel.title", "Модели"));
        int i3 = ColorUtil.multiplyAlpha(Theme.getTextColor(), f4);
        new RenderUtil(drawContext, f, f2, f3, 13.0F).textCenter(Fonts.INTER_SEMIBOLD, string2, 0.0F, 0.0F, i3, 7.0F);
        float f5 = Math.min(f3 * 0.42F, 150.0F);
        new RenderUtil(drawContext, f + f3 / 2.0F - f5 / 2.0F, f2 + 13.0F, f5, 1.5F)
            .rect(Theme.withAlpha(Theme.getPrimary(), 0.9F * f4), Radius.all(0.75F));
        this.closeHoverAnimation.update();
        boolean flag2 = this.isHovered(i, i2, this.closeButtonX - 2.0F, this.closeButtonY - 2.0F, 14.0, 14.0);
        this.closeHoverAnimation.animate(flag2 ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        int i4 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.5F + 0.4F * this.closeHoverAnimation.get()), f4);
        new RenderUtil(drawContext, this.closeButtonX, this.closeButtonY, 10.0F, 10.0F).icon(Icon.CLOSE, 0.0F, 0.0F, 10.0F, i4);
    }

    private void renderPreview(DrawContext drawContext, float f) {
        GuiRenderUtil.drawOutlined(drawContext, this.leftX, this.previewY, 100.0F, 118.0F, f * 0.5F, 8.0F);
        String string = this.getDisplayedModel();
        if (!string.equals(this.previewModelName)) {
            this.previewModelName = string;
            this.previewAvatarPath = FiguraAvatarManager.resolveAvatar(string);
        }

        FiguraAvatarManager.updatePreview(this.previewAvatarPath);
        if (mc.world != null) {
            if (this.previewPlayer == null || this.previewPlayer.getEntityWorld() != mc.world) {
                this.previewPlayer = new OtherClientPlayerEntity(mc.world, new GameProfile(FiguraAvatarManager.getPreviewUuid(), "Preview"));
            }

            boolean flag = this.cosmetics.getPreviewCosmetic(CosmeticType.SWORD) != null;
            if (flag == this.previewPlayer.getMainHandStack().isEmpty()) {
                this.previewPlayer.setStackInHand(Hand.MAIN_HAND, flag ? new ItemStack(Items.NETHERITE_SWORD) : ItemStack.EMPTY);
            }

            int i = (int)this.leftX;
            int i2 = (int)this.previewY;
            int i3 = (int)(this.leftX + 100.0F);
            int i4 = (int)(this.previewY + 118.0F);
            GuiRenderUtil.drawEntity(drawContext, i, i2, i3, i4, 40.12F, this.previewYaw, 0.0F, this.previewPlayer, f, 7.08F);
        }
    }

    private void renderTargets(DrawContext drawContext, int i, int i2, float f) {
        float f2 = this.targetsY;

        for (EspTargetType espTargetType : this.cosmetics.getModelTargets()) {
            CosmeticsPanel.TargetRow targetRow = this.targetRows.computeIfAbsent(espTargetType, CosmeticsPanel.TargetRow::new);
            targetRow.x = this.leftX;
            targetRow.y = f2;
            targetRow.width = 100.0F;
            targetRow.height = 21.0F;
            this.renderTargetRow(drawContext, targetRow, i, i2, f);
            f2 += 25.0F;
        }
    }

    private void renderTargetRow(DrawContext drawContext, CosmeticsPanel.TargetRow targetRow, int i, int i2, float f) {
        EspTargetType espTargetType = targetRow.target;
        boolean flag = espTargetType == this.selectedTarget;
        boolean flag2 = this.selectedTab == CosmeticsPanel.Tab.COSMETICS;
        Cosmetic cosmetic = this.cosmetics.getFirstCosmetic(espTargetType);
        boolean flag3 = flag2 ? cosmetic != null : this.cosmetics.isModelEnabled(espTargetType);
        boolean flag4 = this.isHovered(i, i2, targetRow.x, targetRow.y, targetRow.width, targetRow.height);
        targetRow.hoverAnimation.update();
        targetRow.selectAnimation.update();
        targetRow.enabledAnimation.update();
        targetRow.hoverAnimation.animate(flag4 ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
        targetRow.selectAnimation.animate(flag ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        targetRow.enabledAnimation.animate(flag3 ? 1.0 : 0.0, 0.25, Easings.EXPO_OUT);
        float f2 = targetRow.hoverAnimation.get();
        float f3 = targetRow.selectAnimation.get();
        float f4 = targetRow.enabledAnimation.get();
        GuiRenderUtil.drawOutlined(drawContext, targetRow.x, targetRow.y, targetRow.width, targetRow.height, f * (0.35F + 0.2F * f2 + 0.3F * f3), 6.0F);
        if (f4 > 0.001F) {
            new RenderUtil(drawContext, targetRow.x, targetRow.y, targetRow.width, targetRow.height).rect(Theme.withAlpha(Theme.getPrimary(), 0.12F * f4 * f), Radius.all(6.0F));
        }

        float f5 = targetRow.height - 7.0F;
        float f6 = targetRow.x + 4.0F;
        float f7 = targetRow.y + (targetRow.height - f5) / 2.0F;
        Identifier identifier = !flag2 ? this.getModelIcon(this.cosmetics.getModel(espTargetType)) : (cosmetic == null ? null : cosmetic.preview());
        boolean flag5 = false;
        if (identifier != null) {
            GuiRenderUtil.bindTexture(identifier);
            if (GuiRenderUtil.getBoundTexture() != null) {
                new RenderUtil(drawContext, f6, f7, f5, f5).frostedGlass(f * (0.85F + 0.15F * f3), Radius.all(4.0F));
                flag5 = true;
            }
        }

        if (!flag5) {
            int i3 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.4F), f);
            new RenderUtil(drawContext, f6 + f5 / 2.0F - 5.0F, f7 + f5 / 2.0F - 5.0F, 10.0F, 10.0F).icon(Icon.USER, 0.0F, 0.0F, 10.0F, i3);
        }

        float f8 = f6 + f5 + 5.0F;
        targetRow.switchWidth = 12.0F;
        targetRow.switchHeight = 7.0F;
        targetRow.switchX = targetRow.x + targetRow.width - 5.0F - 12.0F;
        targetRow.switchY = targetRow.y + targetRow.height / 2.0F - 7.0F / 2.0F;
        float f9 = targetRow.switchX - 5.0F - f8;
        int i4 = ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), Theme.getTextColor(), Math.max(f3, f2 * 0.5F)), f);
        new RenderUtil(drawContext, f8, targetRow.y, f9, targetRow.height)
            .textMiddleLeft(Fonts.INTER_MEDIUM, this.truncate(getTargetName(espTargetType), f9, 5.5F), 0.0F, 0.0F, i4, 5.5F);
        float f10 = flag2 ? 0.0F : f * f2;
        if (f10 > 0.001F) {
            SettingWidgets.drawSwitch(drawContext, targetRow.switchX, targetRow.switchY, 12.0F, 7.0F, f4, f10);
        }
    }

    private void renderTabs(DrawContext drawContext, int i, int i2, float f) {
        this.tabAnimation.update();
        this.tabAnimation.animate(this.selectedTab.ordinal(), 0.25, Easings.QUART_OUT);
        float f2 = 84.0F / CosmeticsPanel.Tab.values().length;
        GuiRenderUtil.drawOutlined(drawContext, this.contentX, this.tabsY, 84.0F, 15.0F, f * 0.5F, 6.0F);
        new RenderUtil(drawContext, this.contentX + f2 * this.tabAnimation.get(), this.tabsY, f2, 15.0F)
            .rect(Theme.withAlpha(Theme.getPrimary(), 0.18F * f), Radius.all(6.0F));

        for (CosmeticsPanel.Tab tab : CosmeticsPanel.Tab.values()) {
            float f3 = this.contentX + f2 * tab.ordinal();
            boolean flag = this.isHovered(i, i2, f3, this.tabsY, f2, 15.0);
            int i3 = Theme.withAlpha(Theme.getSecondaryText(), flag ? 1.0F : 0.7F);
            int i4 = ColorUtil.multiplyAlpha(tab == this.selectedTab ? Theme.getPrimary() : i3, f);
            new RenderUtil(drawContext, f3, this.tabsY, f2, 15.0F).textCenter(Fonts.INTER_MEDIUM, tab.getDisplayName(), 0.0F, 0.0F, i4, 5.5F);
        }
    }

    private void renderSearchBar(DrawContext drawContext, int i, int i2, float f) {
        this.searchHoverAnimation.update();
        boolean flag = this.isHovered(i, i2, this.searchX, this.tabsY, this.searchWidth, 15.0);
        this.searchHoverAnimation.animate(!flag && !this.searchFocused ? 0.0 : 1.0, 0.15, Easings.QUAD_OUT);
        GuiRenderUtil.drawOutlined(drawContext, this.searchX, this.tabsY, this.searchWidth, 15.0F, f * (0.5F + 0.25F * this.searchHoverAnimation.get()), 6.0F);
        int i3 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.6F), f);
        new RenderUtil(drawContext, this.searchX + 5.0F, this.tabsY + 7.5F - 4.0F, 8.0F, 8.0F).icon(Icon.SEARCH, 0.0F, 0.0F, 8.0F, i3);
        boolean flag2 = this.searchText.isEmpty();
        String string = flag2 ? Language.translate("cosmetics.panel.search", "Поиск") : this.searchText;
        if (!flag2 && this.searchFocused) {
            string = string + "_";
        }

        int i4 = ColorUtil.multiplyAlpha(Theme.withAlpha(flag2 ? Theme.getSecondaryText() : Theme.getTextColor(), flag2 ? 0.45F : 1.0F), f);
        new RenderUtil(drawContext, this.searchX + 16.0F, this.tabsY, this.searchWidth - 20.0F, 15.0F)
            .textMiddleLeft(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, i4, 6.0F);
    }

    private List<CosmeticsPanel.CardPosition> layoutCards(float f) {
        ArrayList arrayList = new ArrayList();
        this.sectionHeaders.clear();
        float f2 = this.getCardRowHeight();
        float f3 = f;

        for (CosmeticsPanel.Section section : this.getSections()) {
            List list = section.cards();
            if (!list.isEmpty()) {
                if (!section.title().isEmpty()) {
                    this.sectionHeaders.add(new CosmeticsPanel.SectionHeader(section.title(), f3));
                    f3 += 11.0F;
                }

                int i = list.size();

                for (int i2 = 0; i2 < i; i2++) {
                    arrayList.add(
                        new CosmeticsPanel.CardPosition(
                            (CosmeticsPanel.Card)list.get(i2), this.contentX + i2 % 3 * (this.cardSize + 4.0F), f3 + i2 / 3 * f2
                        )
                    );
                }

                f3 += (i + 3 - 1) / 3 * f2;
            }
        }

        this.gridContentHeight = f3 - f;
        return arrayList;
    }

    private void renderGrid(DrawContext drawContext, int i, int i2, float f) {
        if (!(this.gridHeight <= 0.0F)) {
            float f2 = this.contentAnimation.get();
            float f3 = this.gridY + this.scroll.getScroll() + (1.0F - f2) * 8.0F;
            List<CosmeticsPanel.CardPosition> cardPositions = this.layoutCards(f3);
            this.scroll.update(this.gridContentHeight, this.gridHeight);
            this.scroll.updateHover(i, i2);
            if (this.gridContentHeight > this.gridHeight) {
                this.scroll.render(drawContext, this.contentX + this.contentWidth - 2.5F, this.gridY, this.gridHeight, this.gridContentHeight, f);
            }

            boolean flag = ScissorStack.push(drawContext, this.contentX, this.gridY, this.contentWidth, this.gridHeight);
            if (flag) {
                try {
                    for (CosmeticsPanel.SectionHeader sectionHeader : this.sectionHeaders) {
                        this.renderSectionHeader(drawContext, sectionHeader.title(), sectionHeader.y(), f * f2);
                    }

                    for (CosmeticsPanel.CardPosition cardPosition : cardPositions) {
                        if (!(cardPosition.y() + this.getCardRowHeight() < this.gridY) && !(cardPosition.y() > this.gridY + this.gridHeight)) {
                            boolean flag2 = this.isCardSelected(cardPosition.card());
                            boolean flag3 = this.isHovered(i, i2, cardPosition.x(), cardPosition.y(), this.cardSize, this.cardSize + 9.0F);
                            this.renderCard(drawContext, cardPosition.card(), cardPosition.x(), cardPosition.y(), flag2, flag3, f * f2);
                        }
                    }
                } finally {
                    ScissorStack.pop(drawContext);
                }
            }
        }
    }

    private void renderSectionHeader(DrawContext drawContext, String string, float f, float f2) {
        int i = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.65F), f2);
        new RenderUtil(drawContext, this.contentX, f, this.contentWidth, 11.0F).textMiddleLeft(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, i, 5.5F);
        float f3 = Fonts.INTER_MEDIUM.getWidth(string, 5.5F) + 5.0F;
        new RenderUtil(drawContext, this.contentX + f3, f + 5.5F - 0.25F, this.contentWidth - f3 - 6.0F, 0.5F)
            .rect(ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.25F), f2), Radius.all(0.0F));
    }

    private void renderCard(DrawContext drawContext, CosmeticsPanel.Card card, float f, float f2, boolean flag, boolean flag2, float f3) {
        float f4;
        label104: {
            FiguraAvatarManager.AvatarEntry avatarEntry = card.local();
            Animation animation = this.cardHoverAnimations.computeIfAbsent(card.key(), string -> new Animation());
            animation.update();
            animation.animate(flag2 ? 1.0 : 0.0, 0.18, Easings.QUAD_OUT);
            f4 = animation.get();
            GuiRenderUtil.drawOutlined(drawContext, f, f2, this.cardSize, this.cardSize, f3 * (0.5F + 0.18F * f4), 7.0F);
            Identifier identifier = card.cosmetic() != null
                ? card.cosmetic().preview()
                : (card.installed() ? AvatarIconCache.getFromFile(avatarEntry.icon()) : ModelRepository.getIcon(card.remote()));
            if (identifier != null) {
                GuiRenderUtil.bindTexture(identifier);
                if (GuiRenderUtil.getBoundTexture() != null && ScissorStack.push(drawContext, f, f2, this.cardSize, this.cardSize)) {
                    try {
                        float f5 = this.cardSize * 0.06F * f4;
                        new RenderUtil(drawContext, f - f5, f2 - f5, this.cardSize + f5 * 2.0F, this.cardSize + f5 * 2.0F)
                            .frostedGlass(f3 * (0.9F + 0.1F * f4), Radius.all(7.0F));
                        break label104;
                    } finally {
                        ScissorStack.pop(drawContext);
                    }
                }
            }

            int i = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.35F), f3);
            new RenderUtil(drawContext, f + this.cardSize / 2.0F - 9.0F, f2 + this.cardSize / 2.0F - 9.0F, 18.0F, 18.0F)
                .icon(Icon.USER, 0.0F, 0.0F, 18.0F, i);
        }

        if (!card.installed()) {
            this.renderDownloadOverlay(drawContext, card, f, f2, f4, f3);
        } else if (card.remote() != null && ModelRepository.hasUpdate(card.remote())) {
            new RenderUtil(drawContext, f + 3.5F, f2 + 3.5F, 8.0F, 8.0F).icon(Icon.REFRESH, 0.0F, 0.0F, 8.0F, ColorUtil.multiplyAlpha(Theme.getPrimary(), f3));
        }

        float f6 = flag ? 0.24F : 0.0F;
        if (card.key().equals(this.lastSelectedKey)) {
            float f7 = this.selectFlashAnimation.get();
            if (f7 < 0.999F) {
                f6 += (1.0F - f7) * 0.3F;
            }
        }

        if (f6 > 0.001F) {
            new RenderUtil(drawContext, f, f2, this.cardSize, this.cardSize).rect(Theme.withAlpha(Theme.getPrimary(), f6 * f3), Radius.all(7.0F));
        }

        if (flag) {
            new RenderUtil(drawContext, f + this.cardSize - 12.0F, f2 + 3.5F, 8.0F, 8.0F)
                .icon(Icon.CHECK, 0.0F, 0.0F, 8.0F, ColorUtil.multiplyAlpha(Theme.getPrimary(), f3));
        }

        int i2 = ColorUtil.multiplyAlpha(
            flag ? Theme.getPrimary() : Theme.withAlpha(Theme.getSecondaryText(), (card.installed() ? 0.75F : 0.5F) + 0.25F * f4), f3
        );
        new RenderUtil(drawContext, f, f2 + this.cardSize, this.cardSize, 9.0F)
            .textCenter(Fonts.INTER_MEDIUM, this.truncate(card.name(), this.cardSize - 2.0F, 5.0F), 0.0F, 0.0F, i2, 5.0F, true);
    }

    private void renderDownloadOverlay(DrawContext drawContext, CosmeticsPanel.Card card, float f, float f2, float f3, float f4) {
        float f5 = ModelRepository.getProgress(card.name());
        boolean flag = f5 >= 0.0F;
        new RenderUtil(drawContext, f, f2, this.cardSize, this.cardSize)
            .rect(Theme.withAlpha(Theme.getBackground(), (flag ? 0.55F : 0.62F - 0.12F * f3) * f4), Radius.all(7.0F));
        if (!flag) {
            int i = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.7F + 0.3F * f3), f4);
            new RenderUtil(drawContext, f + this.cardSize / 2.0F - 6.0F, f2 + this.cardSize / 2.0F - 6.0F, 12.0F, 12.0F)
                .icon(Icon.CLOUD_DOWNLOAD, 0.0F, 0.0F, 12.0F, i);
        } else {
            float f6 = Math.min(26.0F, this.cardSize - 16.0F);
            float f7 = f + this.cardSize / 2.0F - f6 / 2.0F;
            float f8 = f2 + this.cardSize / 2.0F - f6 / 2.0F;
            float f9 = (float)(System.currentTimeMillis() % 1100L) / 1100.0F;
            new RenderUtil(drawContext, f7, f8, f6, f6).circle(2.0F, ColorUtil.setAlphaf(Theme.getSecondaryText(), 0.22F * f4));
            if (f5 < 0.02F) {
                new RenderUtil(drawContext, f7, f8, f6, f6).arc(f9 * 360.0F, 80.0F, 2.0F, ColorUtil.setAlphaf(Theme.getPrimary(), f4));
            } else {
                new RenderUtil(drawContext, f7, f8, f6, f6)
                    .arc(0.0F, 360.0F * f5, 2.0F, ColorUtil.setAlphaf(Theme.getPrimary(), f4), 1.0F, f9, 0.85F);
                new RenderUtil(drawContext, f7, f8, f6, f6)
                    .textCenter(
                        Fonts.INTER_MEDIUM,
                        Math.round(f5 * 100.0F) + "%",
                        0.0F,
                        0.0F,
                        ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.85F), f4),
                        5.0F
                    );
            }
        }
    }

    private float getCardRowHeight() {
        return this.cardSize + 9.0F + 4.0F;
    }

    private static String getTargetName(EspTargetType espTargetType) {
        return Language.translate("esp.category." + espTargetType.name().toLowerCase(Locale.ROOT), espTargetType.getDisplayName());
    }

    private Identifier getModelIcon(String string) {
        if (string != null && !string.isBlank()) {
            for (FiguraAvatarManager.AvatarEntry avatarEntry : this.avatars) {
                if (avatarEntry.name().equalsIgnoreCase(string)) {
                    return AvatarIconCache.getFromFile(avatarEntry.icon());
                }
            }
        }

        return null;
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
        if (this.cosmetics == null) {
            return false;
        }

        double d = click.x();
        double d2 = click.y();
        if (this.isLeftButton(click.button()) && this.isHovered(d, d2, this.closeButtonX - 3.0F, this.closeButtonY - 3.0F, 16.0, 16.0)) {
            this.close();
            return true;
        }

        if (this.isLeftButton(click.button()) && this.isHovered(d, d2, this.leftX, this.previewY, 100.0, 118.0)) {
            this.rotatingPreview = true;
            this.lastDragX = d;
            return true;
        }

        if (this.isLeftButton(click.button())) {
            for (CosmeticsPanel.TargetRow targetRow : this.targetRows.values()) {
                if (this.selectedTab == CosmeticsPanel.Tab.MODELS
                    && this.isHovered(d, d2, targetRow.switchX - 4.0F, targetRow.y, targetRow.switchWidth + 8.0F, targetRow.height)) {
                    Cosmetics cosmetics2 = this.cosmetics;
                    EspTargetType espTargetType = targetRow.target;
                    if (!this.cosmetics.isModelEnabled(targetRow.target)) {
                        cosmetics2.setModelEnabled(espTargetType, true);
                        return true;
                    }

                    cosmetics2.setModelEnabled(espTargetType, false);
                    return true;
                }

                if (this.isHovered(d, d2, targetRow.x, targetRow.y, targetRow.width, targetRow.height)) {
                    this.selectTarget(targetRow.target);
                    return true;
                }
            }
        }

        if (!this.isLeftButton(click.button()) || !this.isHovered(d, d2, this.contentX, this.tabsY, 84.0, 15.0)) {
            boolean flag2 = this.isHovered(d, d2, this.searchX, this.tabsY, this.searchWidth, 15.0);
            if (this.isLeftButton(click.button())) {
                this.searchFocused = flag2;
            }

            if (flag2) {
                return true;
            }

            if (this.isLeftButton(click.button()) && this.scroll.mouseClicked(d, d2)) {
                return true;
            }

            if (this.isLeftButton(click.button()) && this.isHovered(d, d2, this.contentX, this.gridY, this.contentWidth, this.gridHeight)) {
                CosmeticsPanel.Card card = this.getHoveredCard(d, d2);
                if (card != null) {
                    this.clickCard(card);
                }

                return true;
            } else {
                return this.isMouseOver(d, d2);
            }
        } else if (d < this.contentX + 42.0F) {
            this.selectedTab = CosmeticsPanel.Tab.MODELS;
            this.scroll.reset();
            return true;
        } else {
            this.selectedTab = CosmeticsPanel.Tab.COSMETICS;
            this.scroll.reset();
            return true;
        }
    }

    private void clickCard(CosmeticsPanel.Card card) {
        if (card.cosmetic() != null) {
            this.cosmetics.toggleWear(this.selectedTarget, card.cosmetic());
            this.flashCard(card);
        } else if (!card.installed()) {
            if (card.remote() != null) {
                ModelRepository.download(card.remote()).thenRun(this::reloadAvatars);
            }
        } else if (card.remote() != null && ModelRepository.hasUpdate(card.remote())) {
            ModelRepository.download(card.remote()).thenRun(this::reloadAvatars);
        } else {
            this.cosmetics.setModel(this.selectedTarget, card.name());
            this.flashCard(card);
        }
    }

    private void flashCard(CosmeticsPanel.Card card) {
        this.lastSelectedKey = card.key();
        this.selectFlashAnimation.forceValue(0.0);
        this.selectFlashAnimation.animate(1.0, 0.5, Easings.QUART_OUT);
    }

    private void reloadAvatars() {
        MinecraftClient minecraftClient = mc;
        if (minecraftClient != null) {
            minecraftClient.execute(() -> this.avatars = FiguraAvatarManager.listAvatars());
        }
    }

    private CosmeticsPanel.Card getHoveredCard(double d, double d2) {
        if (!(this.gridHeight <= 0.0F) && this.isHovered(d, d2, this.contentX, this.gridY, this.contentWidth, this.gridHeight)) {
            float f = this.gridY + this.scroll.getScroll() + (1.0F - this.contentAnimation.get()) * 8.0F;

            for (CosmeticsPanel.CardPosition cardPosition : this.layoutCards(f)) {
                if (this.isHovered(d, d2, cardPosition.x(), cardPosition.y(), this.cardSize, this.cardSize + 9.0F)) {
                    return cardPosition.card();
                }
            }
        }

        return null;
    }

    @Override
    public boolean onMouseRelease(Click click) {
        this.rotatingPreview = false;
        this.scroll.mouseReleased();
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        if (this.rotatingPreview) {
            this.previewYaw = this.previewYaw - (float)(click.x() - this.lastDragX) * 1.4F;
            this.lastDragX = click.x();
            return true;
        } else {
            return this.scroll.isDragging() && this.scroll.mouseDragged(click.x(), click.y());
        }
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (this.cosmetics == null) {
            return false;
        } else if (this.isHovered(d, d2, this.contentX, this.gridY, this.contentWidth, this.gridHeight)) {
            this.scroll.onScroll(d4);
            return true;
        } else {
            return this.isMouseOver(d, d2);
        }
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        if (this.cosmetics == null) {
            return false;
        }

        if (keyInput.key() == 256) {
            if (this.searchFocused) {
                this.searchFocused = false;
                return true;
            }

            this.close();
        } else {
            if (!this.searchFocused || keyInput.key() != 259) {
                return this.searchFocused;
            }

            if (!this.searchText.isEmpty()) {
                this.searchText = this.searchText.substring(0, this.searchText.length() - 1);
                this.scroll.reset();
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
        if (this.cosmetics != null && this.searchFocused) {
            if (charInput.codepoint() >= 65536) {
                return false;
            }

            char c = (char)charInput.codepoint();
            if (c >= ' ' && c != 127) {
                this.searchText = this.searchText + c;
                this.scroll.reset();
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

    record Card(String name, FiguraAvatarManager.AvatarEntry local, ModelRepository.RemoteModel remote, Cosmetic cosmetic) {
        boolean installed() {
            return this.local != null || this.cosmetic != null;
        }

        String key() {
            return this.cosmetic == null ? this.name : this.cosmetic.type().getId() + "/" + this.cosmetic.id();
        }
    }

    record CardPosition(CosmeticsPanel.Card card, float x, float y) {
    }

    record Section(String title, List<CosmeticsPanel.Card> cards) {
    }

    record SectionHeader(String title, float y) {
    }

    enum Tab {
        MODELS("cosmetics.panel.tab.models", "Модели"),
        COSMETICS("cosmetics.panel.tab.cosmetics", "Косметика");

        private final String translationKey;
        private final String defaultName;

        Tab(String string, String string2) {
            this.translationKey = string;
            this.defaultName = string2;
        }

        String getDisplayName() {
            return Language.translate(this.translationKey, this.defaultName);
        }
    }

    static final class TargetRow {
        final EspTargetType target;
        final Animation hoverAnimation = new Animation();
        final Animation selectAnimation = new Animation();
        final Animation enabledAnimation = new Animation();
        float x;
        float y;
        float width;
        float height;
        float switchX;
        float switchY;
        float switchWidth;
        float switchHeight;

        TargetRow(EspTargetType espTargetType) {
            this.target = espTargetType;
        }
    }
}
