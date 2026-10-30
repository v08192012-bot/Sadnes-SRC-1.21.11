package tech.sadnes.client.feature.impl.render;

import java.util.Locale;
import java.util.Random;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import tech.sadnes.api.duck.TicksOnGroundAccessor;
import tech.sadnes.api.mixin.accessor.ClientPlayerEntityAccessor;
import tech.sadnes.client.animation.Easing;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.Render2DEvent;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.feature.Beta;
import tech.sadnes.client.feature.BetaScope;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Danger;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.impl.combat.aura.AttackDelayPattern;
import tech.sadnes.client.feature.impl.combat.aura.AttackTimer;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.EmojiRenderer;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.AnimatedIcon;
import tech.sadnes.client.render.world.StyledBoxRenderer;
import tech.sadnes.client.server.MarkerLevel;
import tech.sadnes.client.server.MarkerServer;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.color.ColorTagUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.network.PacketSender;
import tech.sadnes.client.util.player.CombatUtil;
import tech.sadnes.client.util.player.CriticalUtil;

@Beta(scope = BetaScope.ONLY_ADMIN)
@Danger(reasons = {MarkerServer.FUNTIME, MarkerServer.HOLY_WORLD}, level = MarkerLevel.CRITICAL)
@FeatureInfo(
    name = "TestRender",
    description = "feature.impl.render.testrender",
    displayName = "feature.impl.render.testrender",
    category = Category.VISUALS,
    subcategory = Subcategory.HUD_HELPERS
)
public class TestRender extends Feature {
    @Danger(reasons = MarkerServer.ALL_SERVER, level = MarkerLevel.WARNING)
    private final ModeSetting mode = new ModeSetting("Mode", new String[]{"Shapes", "Nametags", "Logo", "Boxes", "Debug", "Glass", "Emoji", "none"});
    private final NumberSetting shapes = new NumberSetting("Shapes", 1000.0F, 100.0F, 10000.0F, 100.0F).setVisibleWhen(() -> this.mode.getValue().equals("Shapes"));
    private final NumberSetting size = new NumberSetting("Size", 8.0F, 2.0F, 40.0F, 1.0F).setVisibleWhen(() -> this.mode.getValue().equals("Shapes"));
    private final NumberSetting nametags = new NumberSetting("Nametags", 100.0F, 10.0F, 500.0F, 10.0F).setVisibleWhen(() -> this.mode.getValue().equals("Nametags"));
    private final BooleanSetting rainbow = new BooleanSetting("Rainbow", true);
    private final BooleanSetting shadow = new BooleanSetting("Shadow", false);
    private final BooleanSetting outline = new BooleanSetting("Outline", false).setVisibleWhen(() -> this.mode.getValue().equals("Shapes"));
    private final BooleanSetting blur = new BooleanSetting("Blur", false).setVisibleWhen(() -> this.mode.getValue().equals("Shapes"));
    private final NumberSetting logoSize = new NumberSetting("Logo Size", 160.0F, 32.0F, 512.0F, 1.0F).setVisibleWhen(() -> this.mode.getValue().equals("Logo"));
    private final BooleanSetting logoGlow = new BooleanSetting("Logo Glow", true).setVisibleWhen(() -> this.mode.getValue().equals("Logo"));
    private final NumberSetting glowSize = new NumberSetting("Glow Size", 8.0F, 1.0F, 24.0F, 0.5F)
        .setVisibleWhen(() -> this.mode.getValue().equals("Logo") && this.logoGlow.getValue() ? true : false);
    private final NumberSetting glowStrength = new NumberSetting("Glow Strength", 0.6F, 0.05F, 1.0F, 0.05F)
        .setVisibleWhen(() -> this.mode.getValue().equals("Logo") && this.logoGlow.getValue() ? true : false);
    private final BooleanSetting logoOutline = new BooleanSetting("Logo Outline", false).setVisibleWhen(() -> this.mode.getValue().equals("Logo"));
    private final NumberSetting outlineSize = new NumberSetting("Outline Size", 1.5F, 0.5F, 6.0F, 0.5F)
        .setVisibleWhen(() -> this.mode.getValue().equals("Logo") && this.logoOutline.getValue() ? true : false);
    @Beta(scope = BetaScope.ALL)
    private final BooleanSetting stats = new BooleanSetting("Stats", true);
    private final NumberSetting emojiSize = new NumberSetting("Emoji Size", 32.0F, 8.0F, 128.0F, 1.0F).setVisibleWhen(() -> this.mode.getValue().equals("Emoji"));
    private final ModeSetting glassBlur = new ModeSetting("Glass Blur", new String[]{"Low", "Middle", "Max"}).setVisibleWhen(() -> this.mode.getValue().equals("Glass"));
    private final NumberSetting glassTint = new NumberSetting("Glass Tint", 0.35F, 0.0F, 1.0F, 0.01F).setVisibleWhen(() -> this.mode.getValue().equals("Glass"));
    private final NumberSetting glassCorner = new NumberSetting("Glass Corner", 18.0F, 0.0F, 40.0F, 1.0F).setVisibleWhen(() -> this.mode.getValue().equals("Glass"));
    private final NumberSetting glassSpread = new NumberSetting("Glass Spread", 18.0F, 0.0F, 60.0F, 1.0F).setVisibleWhen(() -> this.mode.getValue().equals("Glass"));
    private final NumberSetting glassShadow = new NumberSetting("Glass Shadow", 0.6F, 0.0F, 1.0F, 0.01F).setVisibleWhen(() -> this.mode.getValue().equals("Glass"));
    private final ModeSetting glassEasing = new ModeSetting(
            "Glass Easing",
            new String[]{"Linear",
            "QuadIn",
            "QuadOut",
            "QuadInOut",
            "CubicIn",
            "CubicOut",
            "CubicInOut",
            "QuartIn",
            "QuartOut",
            "QuartInOut",
            "SineIn",
            "SineOut",
            "SineInOut",
            "CircIn",
            "CircOut",
            "CircInOut",
            "ExpoIn",
            "ExpoOut",
            "ExpoInOut",
            "BackIn",
            "BackOut",
            "BackInOut",
            "ElasticIn",
            "ElasticOut",
            "ElasticInOut",
            "BounceIn",
            "BounceOut",
            "BounceInOut"}
        )
        .setVisibleWhen(() -> this.mode.getValue().equals("Glass"));
    private double avgSubmitMs;
    private String[] nametagNames;
    private static final String[] EMOJIS = new String[]{
        "✨",
        "\ud83d\udd25",
        "❤️",
        "\ud83d\ude00",
        "\ud83d\ude02",
        "\ud83e\udd7a",
        "\ud83d\udc80",
        "\ud83d\udc4d",
        "\ud83c\udf89",
        "\ud83d\ude0e",
        "\ud83e\udd14",
        "\ud83d\ude4f",
        "\ud83d\udcaf",
        "\ud83d\udc40",
        "\ud83c\udf1f",
        "⚡",
        "\ud83c\udf40",
        "\ud83c\udfae",
        "\ud83d\udc51",
        "\ud83d\ude80"
    };
    private static final AnimatedIcon[] DUCK_ICONS = new AnimatedIcon[]{
        AnimatedIcon.DUCK_0,
        AnimatedIcon.DUCK_1,
        AnimatedIcon.DUCK_2,
        AnimatedIcon.DUCK_3,
        AnimatedIcon.DUCK_4,
        AnimatedIcon.DUCK_5,
        AnimatedIcon.DUCK_6,
        AnimatedIcon.DUCK_7
    };

    @EventHandler
    public void onRender(RenderEvent renderEvent) {
        if (renderEvent.isWorld() && this.mode.getValue().equals("Boxes") && mc.player != null) {
            BlockPos blockPos = mc.player.getBlockPos();
            double d = blockPos.getY();
            double d2 = blockPos.getZ() + 3.0;
            int i = Theme.getPrimary();
            StyledBoxRenderer.Style[] styles = StyledBoxRenderer.Style.values();

            for (int i2 = 0; i2 < styles.length; i2++) {
                double d3 = blockPos.getX() + (i2 * 2 - 2);
                Box box = new Box(d3, d, d2, d3 + 1.0, d + 2.0, d2 + 1.0);
                StyledBoxRenderer.drawBox(renderEvent.getMatrices(), box, styles[i2], i);
            }
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent render2DEvent) {
        if (!this.mode.getValue().equals("Boxes")) {
            DrawContext drawContext = render2DEvent.getContext();
            if (this.mode.getValue().equals("Debug")) {
                this.renderDebug(drawContext);
            } else {
                boolean flag = this.mode.getValue().equals("Nametags");
                boolean flag2 = this.mode.getValue().equals("Logo");
                boolean flag3 = this.mode.getValue().equals("Glass");
                boolean flag4 = this.mode.getValue().equals("Emoji");
                long l = System.nanoTime();
                int i = flag4
                    ? this.renderEmoji(drawContext)
                    : (flag3 ? this.renderGlass(drawContext) : (flag2 ? this.renderLogo(drawContext) : (flag ? this.renderNametags(drawContext) : this.renderShapes(drawContext))));
                double d = (System.nanoTime() - l) / 1000000.0;
                this.avgSubmitMs = this.avgSubmitMs == 0.0 ? d : this.avgSubmitMs * 0.9 + d * 0.1;
                if (this.stats.getValue()) {
                    if (flag4) {
                        this.renderStats(drawContext, i, "emoji");
                        return;
                    }

                    if (flag3) {
                        this.renderStats(drawContext, i, "glass");
                        return;
                    }

                    if (flag2) {
                        this.renderStats(drawContext, i, "logo");
                        return;
                    }

                    if (flag) {
                        this.renderStats(drawContext, i, "tags");
                        return;
                    }

                    this.renderStats(drawContext, i, "shapes");
                }
            }
        }
    }

    private int renderLogo(DrawContext drawContext) {
        int i = mc.getWindow().getScaledWidth();
        int i2 = mc.getWindow().getScaledHeight();
        float f = this.logoSize.getValue();
        float f2 = (i - f) / 2.0F;
        float f3 = (i2 - f) / 2.0F;
        int i3 = this.rainbow.getValue() ? ColorUtil.rainbow(3, 0, 0.6F, 0.95F, 1.0F) : Theme.getPrimary();
        int i4 = this.rainbow.getValue() ? ColorUtil.rainbow(3, 40, 0.6F, 0.95F, 1.0F) : Theme.getPrimary();
        float f4 = this.logoGlow.getValue() ? this.glowSize.getValue() : 0.0F;
        float f5 = this.logoGlow.getValue() ? this.glowStrength.getValue() : 0.0F;
        float f6 = this.logoOutline.getValue() ? this.outlineSize.getValue() : 0.0F;
        new RenderUtil(drawContext, f2, f3, f, f).spinner(0.0F, 0.0F, f, f, i3, i4, f4, f5, f6);
        return 1;
    }

    private int renderGlass(DrawContext drawContext) {
        float f;
        float f2;
        int i;
        int i2;
        RenderUtil.BlurQuality blurQuality;
        label32: {
            label31: {
                int i3 = mc.getWindow().getScaledWidth();
                int i4 = mc.getWindow().getScaledHeight();
                f = (i3 - 240.0F) / 2.0F;
                f2 = (i4 - 150.0F) / 2.0F;
                i = this.rainbow.getValue() ? ColorUtil.rainbow(3, 0, 0.6F, 0.95F, 1.0F) : Theme.getBase();
                i2 = this.rainbow.getValue() ? ColorUtil.rainbow(3, 30, 0.6F, 0.95F, 1.0F) : Theme.getPrimary();
                String string = this.glassBlur.getValue();
                byte b = -1;
                switch (string.hashCode()) {
                    case 76596:
                        if (string.equals("Low")) {
                            break label31;
                        }
                        break;
                    case 77124:
                        if (string.equals("Max")) {
                            b = 1;
                        }
                }

                switch (b) {
                    case 0:
                        break;
                    case 1:
                        blurQuality = RenderUtil.BlurQuality.MAX;
                        break label32;
                    default:
                        blurQuality = RenderUtil.BlurQuality.MIDDLE;
                        break label32;
                }
            }

            blurQuality = RenderUtil.BlurQuality.LOW;
        }

        RenderUtil.BlurQuality blurQuality2 = blurQuality;
        new RenderUtil(drawContext, f, f2, 240.0F, 150.0F)
            .blurGlowBorder(
                1.0F,
                i,
                this.glassTint.getValue(),
                i2,
                this.glassSpread.getValue(),
                this.glassShadow.getValue(),
                this.getGlassEasing(),
                Radius.smoothAll(this.glassCorner.getValue()),
                blurQuality2,
                true
            )
            .textCenter(Fonts.INTER_MEDIUM, "Glass " + this.glassEasing.getValue(), 0.0F, 0.0F, Theme.getTextColor(), 8.0F);
        return 1;
    }

    private Easing getGlassEasing() {
        String string = this.glassEasing.getValue();
        byte b = -1;
        switch (string.hashCode()) {
            case -2050694675:
                if (string.equals("BounceIn")) {
                    return Easings.BOUNCE_IN;
                }
                break;
            case -1979362201:
                if (string.equals("QuadOut")) {
                    return Easings.QUAD_OUT;
                }
                break;
            case -1978910940:
                if (string.equals("QuartIn")) {
                    return Easings.QUART_IN;
                }
                break;
            case -1906657033:
                if (string.equals("ExpoInOut")) {
                    return Easings.EXPO_IN_OUT;
                }
                break;
            case -1864965908:
                if (string.equals("QuadIn")) {
                    return Easings.QUAD_IN;
                }
                break;
            case -1847391593:
                if (string.equals("CircOut")) {
                    return Easings.CIRC_OUT;
                }
                break;
            case -1818401614:
                if (string.equals("SineIn")) {
                    return Easings.SINE_IN;
                }
                break;
            case -1619100998:
                if (string.equals("ElasticIn")) {
                    return Easings.ELASTIC_IN;
                }
                break;
            case -1527609102:
                if (string.equals("CircInOut")) {
                    return Easings.CIRC_IN_OUT;
                }
                break;
            case -1518421521:
                if (string.equals("CubicIn")) {
                    return Easings.CUBIC_IN;
                }
                break;
            case -1216690897:
                if (string.equals("QuartOut")) {
                    return Easings.QUART_OUT;
                }
                break;
            case -1014628982:
                if (string.equals("QuartInOut")) {
                    return Easings.QUART_IN_OUT;
                }
                break;
            case -976355550:
                if (string.equals("BackInOut")) {
                    return Easings.BACK_IN_OUT;
                }
                break;
            case -699890977:
                if (string.equals("CubicInOut")) {
                    return Easings.CUBIC_IN_OUT;
                }
                break;
            case -630164959:
                if (string.equals("BounceInOut")) {
                    b = 26;
                }
                break;
            case -535869087:
                if (string.equals("SineOut")) {
                    return Easings.SINE_OUT;
                }
                break;
            case 173579204:
                if (string.equals("CubicOut")) {
                    return Easings.CUBIC_OUT;
                }
                break;
            case 355563484:
                if (string.equals("ExpoOut")) {
                    return Easings.EXPO_OUT;
                }
                break;
            case 420101436:
                if (string.equals("SineInOut")) {
                    return Easings.SINE_IN_OUT;
                }
                break;
            case 497655490:
                if (string.equals("QuadInOut")) {
                    return Easings.QUAD_IN_OUT;
                }
                break;
            case 852980614:
                if (string.equals("BounceOut")) {
                    return Easings.BOUNCE_OUT;
                }
                break;
            case 1317424327:
                if (string.equals("BackOut")) {
                    return Easings.BACK_OUT;
                }
                break;
            case 1347482713:
                if (string.equals("ElasticOut")) {
                    return Easings.ELASTIC_OUT;
                }
                break;
            case 1982160012:
                if (string.equals("BackIn")) {
                    return Easings.BACK_IN;
                }
                break;
            case 2018616508:
                if (string.equals("CircIn")) {
                    return Easings.CIRC_IN;
                }
                break;
            case 2089679575:
                if (string.equals("ExpoIn")) {
                    return Easings.EXPO_IN;
                }
                break;
            case 2139949620:
                if (string.equals("ElasticInOut")) {
                    return Easings.ELASTIC_IN_OUT;
                }
        }

        switch (b) {
            case 0:
                return Easings.QUAD_IN;
            case 1:
                return Easings.QUAD_OUT;
            case 2:
                return Easings.QUAD_IN_OUT;
            case 3:
                return Easings.CUBIC_IN;
            case 4:
                return Easings.CUBIC_OUT;
            case 5:
                return Easings.CUBIC_IN_OUT;
            case 6:
                return Easings.QUART_IN;
            case 7:
                return Easings.QUART_OUT;
            case 8:
                return Easings.QUART_IN_OUT;
            case 9:
                return Easings.SINE_IN;
            case 10:
                return Easings.SINE_OUT;
            case 11:
                return Easings.SINE_IN_OUT;
            case 12:
                return Easings.CIRC_IN;
            case 13:
                return Easings.CIRC_OUT;
            case 14:
                return Easings.CIRC_IN_OUT;
            case 15:
                return Easings.EXPO_IN;
            case 16:
                return Easings.EXPO_OUT;
            case 17:
                return Easings.EXPO_IN_OUT;
            case 18:
                return Easings.BACK_IN;
            case 19:
                return Easings.BACK_OUT;
            case 20:
                return Easings.BACK_IN_OUT;
            case 21:
                return Easings.ELASTIC_IN;
            case 22:
                return Easings.ELASTIC_OUT;
            case 23:
                return Easings.ELASTIC_IN_OUT;
            case 24:
                return Easings.BOUNCE_IN;
            case 25:
                return Easings.BOUNCE_OUT;
            case 26:
                return Easings.BOUNCE_IN_OUT;
            default:
                return Easings.LINEAR;
        }
    }

    private int renderEmoji(DrawContext drawContext) {
        EmojiRenderer emojiRenderer = EmojiRenderer.getInstance();
        float f = this.emojiSize.getValue();
        float f2 = Math.max(4.0F, f * 0.35F);
        int i = mc.getWindow().getScaledWidth();
        int i2 = Math.max(1, (int)((i - 20.0F * 2.0F) / (f + f2)));
        int i3 = 0;
        int i4 = EMOJIS.length;

        for (int i5 = 0; i5 < i4; i5++) {
            float f3 = 20.0F + i5 % i2 * (f + f2);
            float f4 = 40.0F + i5 / i2 * (f + f2);
            if (emojiRenderer.drawEmoji(drawContext, EMOJIS[i5], f3, f4, f, ColorUtil.WHITE)) {
                i3++;
            }
        }

        float f5 = 40.0F + (float)Math.ceil((double)i4 / i2) * (f + f2) + 20.0F;
        int i6 = i3 + this.renderMixedEmojiText(drawContext, 20.0F, f5);
        return i6 + this.renderAnimatedDucks(drawContext, 20.0F, f5 + 60.0F, f);
    }

    private int renderAnimatedDucks(DrawContext drawContext, float f, float f2, float f3) {
        Fonts.INTER_MEDIUM.draw(drawContext, "Animated ducks (.tgs → frames):", f, f2, Theme.getTextColor(), 8.0F);
        float f4 = Math.max(24.0F, f3);
        float f5 = f4 * 0.15F;
        float f6 = f2 + 14.0F;
        int i = DUCK_ICONS.length;

        for (int i2 = 0; i2 < i; i2++) {
            float f7 = f + i2 * (f4 + f5);
            new RenderUtil(drawContext, f7, f6, f4, f4).spriteBlended(DUCK_ICONS[i2], 0.0F, 0.0F, f4, f4, ColorUtil.WHITE);
        }

        return i;
    }

    private int renderMixedEmojiText(DrawContext drawContext, float f, float f2) {
        int i = Theme.getTextColor();
        String string = "sadnes ✨ mixed \ud83d\udd25 emoji \ud83d\ude02 in text ❤️\ud83d\udc4d";
        new RenderUtil(drawContext, f, f2, 400.0F, 20.0F).textTopLeft(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, i, 14.0F);
        new RenderUtil(drawContext, f, f2 + 22.0F, 400.0F, 20.0F)
            .textTopLeft(Fonts.INTER_MEDIUM, "with shadow \ud83d\ude0e \ud83c\udf89 stays single", 0.0F, 0.0F, i, 14.0F, true);
        return 0;
    }

    private int renderShapes(DrawContext drawContext) {
        int i = Math.round(this.shapes.getValue());
        float f = this.size.getValue();
        int i2 = mc.getWindow().getScaledWidth();
        int i3 = mc.getWindow().getScaledHeight();
        int i4 = Math.max(1, Math.round((float)Math.sqrt((double)i * i2 / i3)));
        float f2 = (float)i2 / i4;
        float f3 = (float)i3 / (int)Math.ceil((double)i / i4);
        Radius radius = Radius.all(f * 0.25F);
        int i5 = Theme.getPrimary();

        for (int i6 = 0; i6 < i; i6++) {
            float f4 = i6 % i4 * f2 + (f2 - f) / 2.0F;
            float f5 = i6 / i4 * f3 + (f3 - f) / 2.0F;
            int i7 = this.rainbow.getValue() ? ColorUtil.rainbow(3, i6, 0.6F, 0.95F, 1.0F) : i5;
            RenderUtil renderUtil = new RenderUtil(drawContext, f4, f5, f, f);
            if (this.blur.getValue()) {
                renderUtil.blur(radius);
            }

            if (this.shadow.getValue()) {
                renderUtil.outline(f * 0.5F, Theme.getNearBackground());
            }

            renderUtil.rect(i7, radius);
            if (this.outline.getValue()) {
                renderUtil.shadow(1.0F, Theme.getMutedText(), radius);
            }
        }

        return i;
    }

    private int renderNametags(DrawContext drawContext) {
        int i = Math.round(this.nametags.getValue());
        this.ensureNametagNames(i);
        Radius radius = Radius.all(3.0F);
        int i2 = mc.getWindow().getScaledWidth();
        int i3 = mc.getWindow().getScaledHeight();
        int i4 = Math.max(1, (int)Math.ceil(Math.sqrt(i)));
        float f = (float)i2 / i4;
        float f2 = (float)i3 / (int)Math.ceil((double)i / i4);
        int i5 = ColorUtil.setAlphaf(Theme.getSurfaceDarkest(), 0.7F);

        for (int i6 = 0; i6 < i; i6++) {
            String string = this.nametagNames[i6];
            String string2 = this.rainbow.getValue() ? this.rainbowText(string, i6) : string;
            int i7 = this.rainbow.getValue() ? Theme.getTextColor() : ColorUtil.rainbow(3, i6, 0.6F, 0.95F, 1.0F);
            float f3 = Fonts.INTER_MEDIUM.getWidth(string, 7.0F) + 3.0F * 2.0F;
            float f4 = i6 % i4 * f + (f - f3) / 2.0F;
            float f5 = i6 / i4 * f2 + (f2 - 11.0F) / 2.0F;
            RenderUtil renderUtil = new RenderUtil(drawContext, f4, f5, f3, 11.0F);
            if (this.shadow.getValue()) {
                renderUtil.outline(5.0F, Theme.getNearBackground());
            }

            renderUtil.rect(i5, radius).textMiddleLeft(Fonts.INTER_MEDIUM, string2, 3.0F, 0.0F, i7, 7.0F);
        }

        return i;
    }

    private String rainbowText(String string, int i) {
        int i2 = string.length();
        StringBuilder builder = new StringBuilder(i2 * 12);

        for (int i3 = 0; i3 < i2; i3++) {
            builder.append(ColorTagUtil.colorTag(ColorUtil.rainbow(3, i * 16 + i3, 0.6F, 0.95F, 1.0F)));
            builder.append(string.charAt(i3));
        }

        return builder.toString();
    }

    private void ensureNametagNames(int i) {
        if (this.nametagNames == null || this.nametagNames.length < i) {
            String string = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
            Random random = new Random(1234L);
            String[] strings = new String[i];

            for (int i2 = 0; i2 < i; i2++) {
                int i3 = 10 + random.nextInt(7);
                StringBuilder builder = new StringBuilder(i3);

                for (int i4 = 0; i4 < i3; i4++) {
                    builder.append(string.charAt(random.nextInt(string.length())));
                }

                strings[i2] = builder.toString();
            }

            this.nametagNames = strings;
        }
    }

    private void renderStats(DrawContext drawContext, int i, String string) {
        String string2 = String.format(Locale.US, "%d %s  submit %.2f ms  %d fps", i, string, this.avgSubmitMs, mc.getCurrentFps());
        float f = Fonts.INTER_MEDIUM.getWidth(string2, 7.0F) + 5.0F * 2.0F;
        new RenderUtil(drawContext, 4.0F, 4.0F, f, 14.0F)
            .rect(ColorUtil.setAlphaf(Theme.getSurfaceDarkest(), 0.85F), Radius.all(4.0F))
            .textMiddleLeft(Fonts.INTER_MEDIUM, string2, 5.0F, 0.0F, Theme.getTextColor(), 7.0F);
    }

    private void renderDebug(DrawContext drawContext) {
        if (mc.player != null) {
            Locale locale = Locale.US;
            int i = ((TicksOnGroundAccessor)mc.player).sadnes$getTicksOnGround();
            boolean flag = ((ClientPlayerEntityAccessor)mc.player).sadnes$getLastSprinting();
            String[] strings = new String[]{
                "ticksOnGround: " + i + "  (>6 = " + (i > 6) + ")",
                "onGround: " + mc.player.isOnGround() + "  groundCollision: " + mc.player.groundCollision,
                "velY: " + String.format(locale, "%.4f", mc.player.getVelocity().y) + "  fallDist: " + String.format(locale, "%.3f", mc.player.fallDistance),
                "convenientFallOffset: " + String.format(locale, "%.4f", CriticalUtil.getFallDistance()),
                "bestMoment(f,s,c): " + CriticalUtil.isBestMoment(true, true, true),
                "msCooldown: " + AttackTimer.getCooldownMs() + "  reached: " + AttackTimer.isReady(),
                "cooldownPC: " + String.format(locale, "%.2f", AttackTimer.getCooldownProgress()),
                "isSprinting: " + mc.player.isSprinting() + "  lastSprinting(srv): " + flag,
                "inputJump: " + mc.player.input.playerInput.jump(),
                "inThreeBlocks: " + CombatUtil.isUnderCeiling() + "  inTwoBlocks: " + CombatUtil.isUnderLowCeiling(),
                "yCap(2): " + String.format(locale, "%.5f", CombatUtil.getCeilingGap(2)),
                "yCap(3): " + String.format(locale, "%.5f", CombatUtil.getCeilingGap(3)),
                "badPotion: " + CriticalUtil.hasBadPotion() + "  isHead: " + CriticalUtil.isHeadBlocked(),
                "hit: " + AttackTimer.getAttackCount() + "/" + AttackTimer.getAttackThreshold() + "  ready: " + AttackTimer.isThresholdReached(),
                "pattern: " + AttackDelayPattern.getPreview() + "  seed: " + Long.toHexString(AttackDelayPattern.getSeed())
            };
            float f = 0.0F;

            for (String string : strings) {
                f = Math.max(f, Fonts.INTER_MEDIUM.getWidth(string, 7.0F));
            }

            float f2 = f + 5.0F * 2.0F;
            float f3 = strings.length * 9.0F + 5.0F * 2.0F;
            RenderUtil renderUtil = new RenderUtil(drawContext, 4.0F, 4.0F, f2, f3);
            if (this.shadow.getValue()) {
                renderUtil.outline(6.0F, Theme.getNearBackground());
            }

            renderUtil.rect(ColorUtil.setAlphaf(Theme.getSurfaceDarkest(), 0.85F), Radius.all(4.0F));

            for (int i2 = 0; i2 < strings.length; i2++) {
                int i3 = this.rainbow.getValue() ? ColorUtil.rainbow(3, i2, 0.6F, 0.95F, 1.0F) : Theme.getTextColor();
                renderUtil.textTopLeft(Fonts.INTER_MEDIUM, strings[i2], 5.0F, 5.0F + i2 * 9.0F, i3, 7.0F);
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive() && packetEvent.getPacket() instanceof ChatMessageC2SPacket chatMessageC2SPacket) {
            String string = Formatting.strip(chatMessageC2SPacket.chatMessage());
            if (!string.toLowerCase().contains(mc.player.getGameProfile().name())) {
                packetEvent.cancel();
            }
        }
    }

    @Override
    public void onKey(int i, boolean flag) {
        if (flag && this.isBoundTo(i) && mc.player != null && mc.getNetworkHandler() != null && mc.player.isInCreativeMode()) {
            int i2 = mc.player.playerScreenHandler.getStacks().size();

            for (int i3 = 1; i3 < i2; i3++) {
                PacketSender.sendSilent(new CreativeInventoryActionC2SPacket(i3, ItemStack.EMPTY));
            }
        }

        super.onKey(i, flag);
    }

    public ModeSetting getMode() {
        return this.mode;
    }

    public NumberSetting getShapes() {
        return this.shapes;
    }

    public NumberSetting getSize() {
        return this.size;
    }

    public NumberSetting getNametags() {
        return this.nametags;
    }

    public BooleanSetting getRainbow() {
        return this.rainbow;
    }

    public BooleanSetting getShadow() {
        return this.shadow;
    }

    public BooleanSetting getOutline() {
        return this.outline;
    }

    public BooleanSetting getBlur() {
        return this.blur;
    }

    public NumberSetting getLogoSize() {
        return this.logoSize;
    }

    public BooleanSetting getLogoGlow() {
        return this.logoGlow;
    }

    public NumberSetting getGlowSize() {
        return this.glowSize;
    }

    public NumberSetting getGlowStrength() {
        return this.glowStrength;
    }

    public BooleanSetting getLogoOutline() {
        return this.logoOutline;
    }

    public NumberSetting getOutlineSize() {
        return this.outlineSize;
    }

    public BooleanSetting getStats() {
        return this.stats;
    }

    public NumberSetting getEmojiSize() {
        return this.emojiSize;
    }

    public ModeSetting getGlassBlur() {
        return this.glassBlur;
    }

    public NumberSetting getGlassTint() {
        return this.glassTint;
    }

    public NumberSetting getGlassCorner() {
        return this.glassCorner;
    }

    public NumberSetting getGlassSpread() {
        return this.glassSpread;
    }

    public NumberSetting getGlassShadow() {
        return this.glassShadow;
    }

    public ModeSetting getGlassEasingSetting() {
        return this.glassEasing;
    }

    public double getAvgSubmitMs() {
        return this.avgSubmitMs;
    }

    public String[] getNametagNames() {
        return this.nametagNames;
    }
}
