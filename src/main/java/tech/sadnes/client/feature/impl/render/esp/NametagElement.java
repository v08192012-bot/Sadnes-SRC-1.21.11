package tech.sadnes.client.feature.impl.render.esp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.impl.misc.ClientSpoof;
import tech.sadnes.client.feature.impl.render.Esp;
import tech.sadnes.client.feature.impl.render.EspElement;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.network.BackendService;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.render.texture.AvatarTextureLoader;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.script.ScriptEspRegistry;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;
import tech.sadnes.libs.rhino.NativeObject;
import ua.mintantileak.profile.Profile;
import ua.mintantileak.profile.Role;

public final class NametagElement extends EspElement {
    public static final float FONT_SIZE = 6.0F;
    private static final float ITEM_SIZE = 16.0F;
    private static final float ITEM_RENDER_SIZE = 8.0F;
    private static final float ICON_SIZE = 6.0F;
    private static final float ICON_Y_OFFSET = 0.0F;
    private static final float AVATAR_SIZE = 7.0F;
    private static final Radius AVATAR_RADIUS = Radius.all(3.5F);
    private static final float SPINNER_SIZE = 16.0F;
    private static final float SPINNER_WIDTH = 5.28F;
    private static final float PADDING = 2.0F;
    private static final float ICON_GAP = 2.0F;
    private static final float ROW_GAP = 2.0F;
    private static final float DIVIDER_WIDTH = 0.5F;
    private static final float DIVIDER_HEIGHT = 3.5F;
    private static final float SEGMENT_GAP = 4.5F;
    private static final Radius DIVIDER_RADIUS = Radius.all(0.25F);
    private static final int HEALTH_COLOR = ColorUtil.fromHex("FF5555");
    private static final int PING_COLOR = ColorUtil.fromHex("55FFFF");
    private static final int FRIEND_TAG_COLOR = ColorUtil.fromHex("FFD24A");
    private static final String ELEMENT_NAME = "name";
    private static final String ELEMENT_HEALTH = "health";
    private static final String ELEMENT_PING = "ping";
    private static final String ELEMENT_FRIEND_TAG = "friend_tag";
    public final MultiSetting elements = this.createElementsSetting();
    public final BooleanSetting showHealth = this.getTarget() != EspTargetType.ITEM ? this.addHiddenSetting(new BooleanSetting("Show health", true)) : null;
    public final BooleanSetting showFriendTag = this.getTarget() == EspTargetType.FRIENDS
        ? this.addHiddenSetting(new BooleanSetting("Show friend tag", true))
        : null;
    public final BooleanSetting stackItems = this.getTarget() == EspTargetType.ITEM ? this.addSetting(new BooleanSetting("Stack items", true)) : null;
    private final TextSetting layout = this.createLayoutSetting(EspAnchor.TOP);
    private List<NametagElement.Segment> segments = Collections.emptyList();
    private List<ItemStack> items;
    private ItemStack[] itemStacks;
    private int itemCount;
    private float itemRowHeight;
    private boolean itemMode;
    private Entity lastEntity;
    private boolean previewMode;

    public NametagElement(EspTargetType espTargetType) {
        super("Nametag", true, espTargetType);
        if (this.elements != null) {
            this.elements.setOnChange(this::syncLegacyToggles);
        }

        if (this.showHealth != null) {
            this.showHealth.setOnChange(this::syncHealthElement);
        }

        if (this.showFriendTag != null) {
            this.showFriendTag.setOnChange(this::syncFriendTagElement);
        }
    }

    private MultiSetting createElementsSetting() {
        if (this.getTarget() == EspTargetType.ITEM) {
            return null;
        }

        MultiSetting multiSetting = new MultiSetting("Elements");
        multiSetting.onInput(BooleanSetting.of("name", true));
        multiSetting.onInput(BooleanSetting.of("health", true));
        if (this.getTarget().isPlayerType()) {
            multiSetting.onInput(BooleanSetting.of("ping", true));
        }

        if (this.getTarget() == EspTargetType.FRIENDS) {
            multiSetting.onInput(BooleanSetting.of("friend_tag", true));
        }

        return this.addSetting(multiSetting);
    }

    private void syncHealthElement() {
        if (this.elements != null && this.showHealth != null) {
            this.elements.setSelected("health", this.showHealth.getValue());
        }
    }

    private void syncFriendTagElement() {
        if (this.elements != null && this.showFriendTag != null) {
            this.elements.setSelected("friend_tag", this.showFriendTag.getValue());
        }
    }

    public static boolean isBuiltinElement(String string) {
        return "name".equalsIgnoreCase(string) || "health".equalsIgnoreCase(string) || "ping".equalsIgnoreCase(string) || "friend_tag".equalsIgnoreCase(string);
    }

    public boolean addElementOption(String string, boolean flag, Supplier<Boolean> supplier) {
        if (this.elements == null) {
            return false;
        }

        BooleanSetting booleanSetting = BooleanSetting.of(string, flag);
        booleanSetting.setVisibleWhen(supplier);
        this.elements.onInput(booleanSetting);
        return true;
    }

    public Boolean removeElementOption(String string) {
        if (this.elements != null && this.elements.getOption(string) != null) {
            boolean flag = this.elements.has(string);
            this.elements.removeOption(string);
            return flag;
        } else {
            return null;
        }
    }

    private void syncLegacyToggles() {
        if (this.elements != null) {
            if (this.showHealth != null) {
                this.showHealth.setValue(this.elements.has("health"));
            }

            if (this.showFriendTag != null) {
                this.showFriendTag.setValue(this.elements.has("friend_tag"));
            }
        }
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public boolean usesItems() {
        return true;
    }

    @Override
    public boolean isAttachable() {
        return true;
    }

    @Override
    public void renderForEntity(
        DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, float f7, float f8
    ) {
        if (this.measured && !(f8 <= 0.001F)) {
            if (this.itemMode) {
                this.renderItemsBackground(drawContext, f8);
                this.renderItemsContent(drawContext, f8);
            } else {
                this.renderPanel(drawContext, f8);
                this.renderDividers(drawContext, f8);
                this.renderSegments(drawContext, f8);
            }
        }
    }

    @Override
    public void measure(Entity entity, boolean flag) {
        this.measured = false;
        MsdfFont msdfFont = Fonts.INTER_MEDIUM;
        if (msdfFont != null) {
            this.itemMode = entity instanceof ItemEntity;
            if (this.itemMode) {
                this.measureItems(msdfFont, (ItemEntity)entity, flag);
            } else {
                List list = this.buildSegments(entity, flag, msdfFont);
                if (!list.isEmpty()) {
                    this.lastEntity = entity;
                    this.previewMode = flag;
                    float f = 2.0F;
                    int i = list.size();

                    for (int i2 = 0; i2 < i; i2++) {
                        NametagElement.Segment segment = (NametagElement.Segment)list.get(i2);
                        segment.offsetX = f;
                        f += segment.width;
                        if (i2 < i - 1) {
                            f += 4.5F;
                        }
                    }

                    this.width = f + 2.0F;
                    this.height = 10.0F;
                    this.segments = list;
                    this.measured = true;
                }
            }
        }
    }

    @Override
    public void updateItems(List<ItemStack> itemStacks2) {
        this.items = itemStacks2;
    }

    private void measureItems(MsdfFont msdfFont, ItemEntity itemEntity, boolean flag) {
        List list;
        if (!flag && this.items != null) {
            if (this.items.isEmpty()) {
                return;
            }

            list = this.items;
        } else {
            list = List.of(itemEntity.getStack());
        }

        int i = list.size();
        if (this.itemStacks == null || this.itemStacks.length < i) {
            this.itemStacks = new ItemStack[i];
        }

        float f = 0.0F;

        for (int i2 = 0; i2 < i; i2++) {
            ItemStack itemStack = (ItemStack)list.get(i2);
            this.itemStacks[i2] = itemStack;
            float f2 = 14.5F + msdfFont.getTextWidth(itemStack.getName(), 6.0F) + 2.0F;
            if (f2 > f) {
                f = f2;
            }
        }

        this.itemCount = i;
        this.itemRowHeight = 10.0F;
        this.width = f;
        this.height = i * this.itemRowHeight;
        this.measured = true;
    }

    @Override
    public void renderBackground(DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5) {
        if (this.measured) {
            if (this.itemMode) {
                this.renderItemsBackground(drawContext, 1.0F);
            } else {
                this.renderPanel(drawContext, 1.0F);
                this.renderDividers(drawContext, 1.0F);
            }
        }
    }

    @Override
    public void renderOverlay(DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5) {
        if (this.measured) {
            if (this.itemMode) {
                this.renderItemsContent(drawContext, 1.0F);
            } else {
                this.renderSegments(drawContext, 1.0F);
            }
        }
    }

    private List<NametagElement.Segment> buildSegments(Entity entity, boolean flag, MsdfFont msdfFont) {
        ArrayList arrayList = new ArrayList(4);
        if (this.isElementEnabled("name")) {
            Text text = ClientSpoof.replaceNames(flag ? this.getPreviewName(entity) : entity.getDisplayName());
            NametagElement.Segment segment = this.createTextSegment(msdfFont, text, Theme.getTextColor());
            if (!flag && entity instanceof PlayerEntity playerEntity) {
                applyAvatar(segment, playerEntity);
            }

            arrayList.add(segment);
        }

        if (this.isElementEnabled("health") && (flag || entity instanceof LivingEntity)) {
            float f = flag ? 20.0F : this.getHealth(entity);
            if (f > 0.0F) {
                arrayList.add(this.createHealthSegment(msdfFont, f));
            }
        }

        if (this.isElementEnabled("ping") && (flag || entity instanceof PlayerEntity)) {
            int i = flag ? 42 : this.getPing(entity);
            if (i >= 0) {
                arrayList.add(this.createPingSegment(msdfFont, i));
            }
        }

        if (this.isElementEnabled("friend_tag")) {
            arrayList.add(this.createIconSegment(Icon.STAR, FRIEND_TAG_COLOR));
        }

        for (ScriptEspRegistry.NametagSegment nametagSegment : ScriptEspRegistry.getActiveSegments()) {
            if (nametagSegment.shows(this.getTarget()) && this.isElementEnabled(nametagSegment.id())) {
                NametagElement.Segment segment2 = this.createScriptSegment(msdfFont, nametagSegment, entity, flag);
                if (segment2 != null) {
                    arrayList.add(segment2);
                }
            }
        }

        return arrayList;
    }

    private NametagElement.Segment createScriptSegment(MsdfFont msdfFont, ScriptEspRegistry.NametagSegment nametagSegment, Entity entity, boolean flag) {
        NativeObject nativeObject = ScriptEspRegistry.createContext(entity, this.getTarget(), flag);
        NametagElement.Segment segment = new NametagElement.Segment();
        if (nametagSegment.draw() != null) {
            float f = ScriptEspRegistry.resolveFloat(nametagSegment.script(), nametagSegment.width(), nativeObject, 0.0F);
            if (f <= 0.0F) {
                return null;
            }

            segment.scriptElement = nametagSegment;
            segment.scriptContext = nativeObject;
            segment.width = f;
            return segment;
        } else {
            String string = nametagSegment.text() == null ? "" : ScriptEspRegistry.resolveString(nametagSegment.script(), nametagSegment.text(), nativeObject);
            if (string != null && (!string.isEmpty() || nametagSegment.icon() != null)) {
                if (!string.isEmpty()) {
                    segment.text = Text.literal(string);
                    segment.textColor = ScriptEspRegistry.resolveInt(nametagSegment.script(), nametagSegment.color(), nativeObject, Theme.getTextColor());
                    segment.textWidth = msdfFont.getTextWidth(segment.text, 6.0F);
                }

                if (nametagSegment.icon() != null) {
                    segment.icon = nametagSegment.icon();
                    segment.iconColor = ScriptEspRegistry.resolveInt(nametagSegment.script(), nametagSegment.iconColor(), nativeObject, Theme.getPrimary());
                    segment.iconWidth = 6.0F;
                    segment.iconAfterText = nametagSegment.iconAfter();
                }

                float f2 = segment.iconWidth + segment.textWidth;
                if (segment.icon != null && segment.text != null) {
                    segment.width = f2 + 2.0F;
                    return segment;
                } else {
                    segment.width = f2 + 0.0F;
                    return segment;
                }
            } else {
                return null;
            }
        }
    }

    private boolean isElementEnabled(String string) {
        return this.elements != null && this.elements.has(string);
    }

    private static void applyAvatar(NametagElement.Segment segment, PlayerEntity playerEntity) {
        Role role = Profile.getRole();
        if (role != null && role.ordinal() >= Role.ADMIN.ordinal()) {
            BackendService backendService = Sadnes.getInstance().getBackend();
            if (backendService != null) {
                String string = backendService.getAvatarUrlForPlayer(playerEntity.getGameProfile().name());
                if (string != null) {
                    segment.avatarTexture = string.isEmpty() ? null : AvatarTextureLoader.getTexture(string);
                    segment.avatarLoading = segment.avatarTexture == null;
                    segment.iconWidth = segment.avatarLoading ? 5.28F : 7.0F;
                    segment.width = segment.iconWidth + 2.0F + segment.textWidth;
                }
            }
        }
    }

    private Text getPreviewName(Entity entity) {
        if (entity instanceof PlayerEntity playerEntity) {
            String string = playerEntity.getGameProfile().name();
            return string != null && !string.isBlank() ? Text.literal(string) : Text.literal("SadnesPreview");
        } else {
            return entity instanceof AnimalEntity ? Text.literal("Свинья") : Text.literal("Зомби");
        }
    }

    private float getHealth(Entity entity) {
        if (entity instanceof PlayerEntity) {
            return ServerManager.getHealth((PlayerEntity)entity);
        } else {
            return entity instanceof LivingEntity ? ((LivingEntity)entity).getHealth() : 0.0F;
        }
    }

    private int getPing(Entity entity) {
        if (entity instanceof PlayerEntity playerEntity) {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (minecraftClient.getNetworkHandler() == null) {
                return -1;
            }

            PlayerListEntry playerListEntry = minecraftClient.getNetworkHandler().getPlayerListEntry(playerEntity.getUuid());
            return playerListEntry == null ? -1 : Math.max(playerListEntry.getLatency(), 0);
        } else {
            return -1;
        }
    }

    private NametagElement.Segment createTextSegment(MsdfFont msdfFont, Text text2, int i) {
        NametagElement.Segment segment = new NametagElement.Segment();
        segment.text = text2;
        segment.textColor = i;
        segment.textWidth = msdfFont.getTextWidth(text2, 6.0F);
        segment.width = segment.textWidth;
        return segment;
    }

    private NametagElement.Segment createIconSegment(Icon icon2, int i) {
        NametagElement.Segment segment = new NametagElement.Segment();
        segment.icon = icon2;
        segment.iconColor = i;
        segment.iconWidth = 6.0F;
        segment.width = 6.0F;
        return segment;
    }

    private NametagElement.Segment createHealthSegment(MsdfFont msdfFont, float f) {
        NametagElement.Segment segment = new NametagElement.Segment();
        segment.icon = Icon.HEART;
        segment.iconColor = HEALTH_COLOR;
        segment.text = Text.literal(formatHealth(f)).formatted(Formatting.RED);
        segment.textColor = Theme.getTextColor();
        segment.textWidth = msdfFont.getTextWidth(segment.text, 6.0F);
        segment.iconWidth = 6.0F;
        segment.iconAfterText = true;
        segment.width = 8.0F + segment.textWidth;
        return segment;
    }

    private NametagElement.Segment createPingSegment(MsdfFont msdfFont, int i) {
        NametagElement.Segment segment = new NametagElement.Segment();
        segment.icon = Icon.SATELLITE_DISH;
        segment.iconColor = PING_COLOR;
        segment.text = Text.literal(i + Language.translate("nametag.ping.unit", " ms")).formatted(Formatting.AQUA);
        segment.textColor = Theme.getTextColor();
        segment.textWidth = msdfFont.getTextWidth(segment.text, 6.0F);
        segment.iconWidth = 6.0F;
        segment.iconAfterText = true;
        segment.width = 8.0F + segment.textWidth;
        return segment;
    }

    private static String formatHealth(float f) {
        float f2 = Math.round(f * 10.0F) / 10.0F;
        return f2 == (int)f2 ? Integer.toString((int)f2) : String.format(Locale.ROOT, "%.1f", f2);
    }

    private void renderPanel(DrawContext drawContext, float f) {
        float f2 = f * Esp.getInstance().backgroundAlpha.getValue();
        if (!ScriptEspRegistry.renderNametagHook(
            drawContext,
            this.lastEntity,
            this.getTarget(),
            this.previewMode,
            this.getLayoutX(),
            this.getLayoutY(),
            this.width,
            this.height,
            f2,
            this.isFriendTarget()
        )) {
            GuiRenderUtil.drawPanel(drawContext, this.getLayoutX(), this.getLayoutY(), this.width, this.height, f2, 3.0F, this.isFriendTarget());
        }
    }

    private void renderDividers(DrawContext drawContext, float f) {
        int i = this.segments.size();
        if (i >= 2) {
            float f2 = this.getLayoutX();
            float f3 = this.getLayoutY() + this.height / 2.0F;
            int i2 = ColorUtil.setAlphaf(Theme.getSecondaryText(), f * 0.3F);

            for (int i3 = 0; i3 < i - 1; i3++) {
                NametagElement.Segment segment = this.segments.get(i3);
                float f4 = f2 + segment.offsetX + segment.width + 2.0F;
                new RenderUtil(drawContext, f4, f3 - 1.75F, 0.5F, 3.5F).rect(i2, DIVIDER_RADIUS);
            }
        }
    }

    private void renderSegments(DrawContext drawContext, float f) {
        MsdfFont msdfFont = Fonts.INTER_MEDIUM;
        if (msdfFont != null) {
            float f2 = this.getLayoutX();
            float f3 = this.getLayoutY();
            RenderUtil renderUtil = new RenderUtil(drawContext, f2, f3, this.width, this.height);

            for (NametagElement.Segment segment : this.segments) {
                boolean flag;
                if (segment.icon == null && segment.avatarTexture == null) {
                    flag = segment.avatarLoading;
                } else {
                    flag = true;
                }

                float f4 = flag && segment.text != null ? 2.0F : 0.0F;
                float f5 = segment.iconAfterText ? segment.offsetX + segment.textWidth + f4 : segment.offsetX;
                float f6 = segment.iconAfterText ? segment.offsetX : segment.offsetX + segment.iconWidth + f4;
                float f7 = f3 + (this.height - 7.0F) / 2.0F;
                if (segment.avatarTexture != null) {
                    GuiRenderUtil.bindTexture(segment.avatarTexture);
                    new RenderUtil(drawContext, f2 + f5, f7, 7.0F, 7.0F).frostedGlass(f, AVATAR_RADIUS);
                }

                if (segment.avatarLoading) {
                    float f8 = f2 + f5 + -5.3599997F;
                    float f9 = f3 + (this.height - 16.0F) / 2.0F;
                    new RenderUtil(drawContext, f8, f9, 16.0F, 16.0F)
                        .spinner(0.0F, 0.0F, 16.0F, ColorUtil.multiplyAlpha(Theme.getPrimary(), f), ColorUtil.multiplyAlpha(Theme.getAccentLight(), f));
                }

                if (segment.icon != null) {
                    renderUtil.icon(segment.icon, f5, (this.height - 6.0F) / 2.0F + 0.0F, 6.0F, ColorUtil.multiplyAlpha(segment.iconColor, f));
                }

                if (segment.text != null) {
                    renderUtil.textMiddleLeft(msdfFont, segment.text, f6, 0.0F, ColorUtil.multiplyAlpha(segment.textColor, f), 6.0F);
                }

                if (segment.scriptElement != null) {
                    ScriptEspRegistry.putBounds(segment.scriptContext, f2 + segment.offsetX, f3, segment.width, this.height, f);
                    ScriptEspRegistry.invokeDraw(segment.scriptElement.script(), drawContext, f, segment.scriptElement.draw(), segment.scriptContext);
                }
            }
        }
    }

    private void renderItemsBackground(DrawContext drawContext, float f) {
        float f2 = this.getLayoutX();
        float f3 = this.getLayoutY();
        float f4 = f * Esp.getInstance().backgroundAlpha.getValue();
        GuiRenderUtil.drawPanel(drawContext, f2, f3, this.width, this.height, f4, 3.0F, this.isFriendTarget());
        int i = ColorUtil.setAlphaf(Theme.getSecondaryText(), f * 0.3F);
        float f5 = f2 + 2.0F + 8.0F + 2.0F;

        for (int i2 = 0; i2 < this.itemCount; i2++) {
            float f6 = f3 + i2 * this.itemRowHeight + (this.itemRowHeight - 3.5F) / 2.0F;
            new RenderUtil(drawContext, f5, f6, 0.5F, 3.5F).rect(i, DIVIDER_RADIUS);
        }
    }

    private void renderItemsContent(DrawContext drawContext, float f) {
        MsdfFont msdfFont = Fonts.INTER_MEDIUM;
        if (msdfFont != null) {
            float f2 = this.getLayoutX();
            float f3 = this.getLayoutY();
            int i = ColorUtil.multiplyAlpha(Theme.getTextColor(), f);
            RenderUtil renderUtil = new RenderUtil(drawContext, f2, f3, this.width, this.itemRowHeight);

            for (int i2 = 0; i2 < this.itemCount; i2++) {
                ItemStack itemStack = this.itemStacks[i2];
                float f4 = f3 + i2 * this.itemRowHeight;
                this.renderItem(drawContext, itemStack, f2 + 2.0F, f4 + (this.itemRowHeight - 8.0F) / 2.0F, f);
                renderUtil.setY(f4);
                renderUtil.textMiddleLeft(msdfFont, itemStack.getName(), 14.5F, 0.0F, i, 6.0F);
            }
        }
    }

    private void renderItem(DrawContext drawContext, ItemStack itemStack, float f, float f2, float f3) {
        float f4 = MathUtil.roundToStep(f, 0.5F);
        float f5 = MathUtil.roundToStep(f2, 0.5F);
        MatrixUtil.push(drawContext);
        MatrixUtil.translate(drawContext, f4, f5);
        MatrixUtil.scale(drawContext, 0.0F, 0.0F, 0.5F);
        GuiRenderUtil.drawCachedItem(drawContext, itemStack, itemStack, 0.0F, 0.0F, f3);
        if (f3 > 0.99F) {
            drawContext.drawStackOverlay(MinecraftClient.getInstance().textRenderer, itemStack, 0, 0);
        }

        MatrixUtil.pop(drawContext);
    }

    static final class Segment {
        Icon icon;
        Identifier avatarTexture;
        boolean avatarLoading;
        ScriptEspRegistry.NametagSegment scriptElement;
        NativeObject scriptContext;
        int iconColor;
        Text text;
        int textColor;
        boolean iconAfterText;
        float iconWidth;
        float textWidth;
        float width;
        float offsetX;
    }
}
