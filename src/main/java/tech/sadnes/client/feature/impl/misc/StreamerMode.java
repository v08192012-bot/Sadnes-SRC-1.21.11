package tech.sadnes.client.feature.impl.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.AssetInfo.TextureAssetInfo;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.TextEvent;
import tech.sadnes.client.feature.Beta;
import tech.sadnes.client.feature.BetaScope;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.TextSetting;

@Beta(scope = BetaScope.ONLY_ADMIN)
@FeatureInfo(name = "StreamerMode", displayName = "feature.impl.misc.streamermode", category = Category.MISC, subcategory = Subcategory.MISC_CLIENT)
public final class StreamerMode extends Feature {
    private static final String[][] TEXT_REPLACEMENTS = new String[][]{
        {"spookytime.net", "FunTime.su"}, {"spookytime", "FunTime"}, {"spooky", "FunTime"}, {"Спуки", "ФанТайм"}, {"СпукиТайм", "ФанТайм"}
    };
    private static final String TRIGGER_WORD = "spook";
    private static final String[] SHOP_SPAM_MARKERS = new String[]{"/tshop", "Покупай", "купил"};
    private static final StreamerMode.AdReplacement[] AD_REPLACEMENTS = new StreamerMode.AdReplacement[]{
        new StreamerMode.AdReplacement("Вы играете на", StreamerMode::createFunTimeAd),
        new StreamerMode.AdReplacement("Наши сообщества", () -> buildAdLine("Наши сообщества и соц. сети ", "/links", Formatting.GRAY, false)),
        new StreamerMode.AdReplacement("Наш Сайт", () -> buildAdLine("Наш Сайт ", "FunTime.su", Formatting.GREEN, false)),
        new StreamerMode.AdReplacement("Наша группа ВК", () -> buildAdLine("Наша группа ВК ", "vk.com/funtime", Formatting.BLUE, false)),
        new StreamerMode.AdReplacement("Наш Телеграм", () -> buildAdLine("Наш Телеграм ", "t.me/funtime", Formatting.AQUA, false)),
        new StreamerMode.AdReplacement("Наш Дискорд", () -> buildAdLine("Наш Дискорд ", "dd.FunTime.su", Formatting.BLUE, false))
    };
    private static final String[] SCOREBOARD_HIDDEN_ENTRIES = new String[]{"Фортун"};
    private static final int SKIN_COUNT = 11;
    private static final SkinTextures[] SKIN_CACHE = new SkinTextures[11];
    private final BooleanSetting replaceText = new BooleanSetting("streamermode.replaceText", true);
    private final BooleanSetting spoofAds = new BooleanSetting("streamermode.spoofAds", true);
    private final BooleanSetting hideShopSpam = new BooleanSetting("streamermode.hideShopSpam", true);
    private final BooleanSetting cleanScoreboard = new BooleanSetting("streamermode.cleanScoreboard", true);
    private final BooleanSetting renameScoreboard = new BooleanSetting("streamermode.renameScoreboard", false);
    private final TextSetting scoreboardTitle = new TextSetting("streamermode.scoreboardTitle", "Дуэли - 1", "Дуэли - 1")
        .setVisibleWhen(this.renameScoreboard::getValue);
    private final BooleanSetting replaceSkins = new BooleanSetting("streamermode.replaceSkins", true);
    private final BooleanSetting debugLog = new BooleanSetting("streamermode.debugLog", false);
    private static volatile String lastLoggedText;

    public static StreamerMode getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(StreamerMode.class);
    }

    @EventHandler
    public void onTextRender(TextEvent textEvent) {
        if (this.replaceText.getValue()) {
            String string = textEvent.getText();
            if (string != null && !string.isEmpty()) {
                for (String[] strings : TEXT_REPLACEMENTS) {
                    string = replaceIgnoreCase(string, strings[0], strings[1]);
                }

                textEvent.setText(string);
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive()) {
            if (packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket && !gameMessageS2CPacket.overlay()) {
                String string = gameMessageS2CPacket.content().getString();
                if (string.isEmpty()) {
                    return;
                }

                if (this.hideShopSpam.getValue()) {
                    for (String string2 : SHOP_SPAM_MARKERS) {
                        if (containsIgnoreCase(string, string2)) {
                            packetEvent.cancel();
                            return;
                        }
                    }
                }

                if (this.spoofAds.getValue()) {
                    String string3 = gameMessageS2CPacket.content().getString();
                    if (string3.contains("╔") || string3.contains("╠") || string3.contains("╚") || string3.contains("═") || string3.contains("форт")) {
                        packetEvent.cancel();
                    }

                    for (StreamerMode.AdReplacement adReplacement : AD_REPLACEMENTS) {
                        if (containsIgnoreCase(string, adReplacement.marker())) {
                            packetEvent.cancel();
                            MutableText mutableText = adReplacement.builder().get();
                            mc.execute(() -> {
                                if (mc.inGameHud != null) {
                                    mc.inGameHud.getChatHud().addMessage(mutableText);
                                }
                            });
                            return;
                        }
                    }
                }
            }
        }
    }

    public static Collection<ScoreboardEntry> filterScoreboardEntries(Scoreboard scoreboard, Collection<ScoreboardEntry> scoreboardEntries) {
        StreamerMode streamerMode = getInstance();
        if (streamerMode != null && streamerMode.isEnabled() && streamerMode.cleanScoreboard.getValue()) {
            ArrayList arrayList = new ArrayList(scoreboardEntries.size());

            label28:
            for (ScoreboardEntry scoreboardEntry : scoreboardEntries) {
                Team team = scoreboard.getScoreHolderTeam(scoreboardEntry.owner());
                String string = Team.decorateName(team, scoreboardEntry.name()).getString();

                for (String string2 : SCOREBOARD_HIDDEN_ENTRIES) {
                    if (containsIgnoreCase(string, string2)) {
                        continue label28;
                    }
                }

                arrayList.add(scoreboardEntry);
            }

            return arrayList;
        } else {
            return scoreboardEntries;
        }
    }

    public static Text replaceScoreboardTitle(Text text) {
        StreamerMode streamerMode = getInstance();
        if (streamerMode != null && streamerMode.isEnabled() && streamerMode.renameScoreboard.getValue()) {
            String string = streamerMode.scoreboardTitle.getValue();
            return string.isEmpty() ? text : Text.literal("⚡ ").formatted(Formatting.GOLD).append(Text.literal(string).formatted(Formatting.RED, Formatting.BOLD));
        } else {
            return text;
        }
    }

    public static SkinTextures getReplacementSkin(AbstractClientPlayerEntity abstractClientPlayerEntity) {
        StreamerMode streamerMode = getInstance();
        if (streamerMode == null || !streamerMode.isEnabled() || !streamerMode.replaceSkins.getValue() || abstractClientPlayerEntity == null) {
            return null;
        } else {
            return abstractClientPlayerEntity == mc.player ? null : getOrCreateSkin(Math.floorMod(abstractClientPlayerEntity.getUuid().hashCode(), 11));
        }
    }

    private static SkinTextures getOrCreateSkin(int i) {
        SkinTextures skinTextures = SKIN_CACHE[i];
        if (skinTextures != null) {
            return skinTextures;
        }

        Identifier identifier = Identifier.of(ClientInfo.MOD_ID, "skins/skin_" + i);
        SkinTextures skinTextures2 = SkinTextures.create(new TextureAssetInfo(identifier), null, null, PlayerSkinType.WIDE);
        SKIN_CACHE[i] = skinTextures2;
        return skinTextures2;
    }

    private static MutableText buildAdLine(String string, String string2, Formatting formatting, boolean flag) {
        MutableText mutableText = Text.literal(string2).formatted(formatting);
        if (flag) {
            mutableText.formatted(Formatting.ITALIC);
        }

        return Text.literal("\n ⚡ ")
            .formatted(Formatting.YELLOW)
            .append(Text.literal(string).formatted(Formatting.WHITE))
            .append(mutableText)
            .append(Text.literal("\n"));
    }

    private static MutableText createFunTimeAd() {
        return Text.literal("\n ⚡ ")
            .formatted(Formatting.YELLOW)
            .append(Text.literal("Вы играете на ").formatted(Formatting.WHITE))
            .append(Text.literal("FunTime").formatted(Formatting.YELLOW))
            .append(Text.literal("! ").formatted(Formatting.WHITE))
            .append(Text.literal("play.funtime.su").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC))
            .append(Text.literal("\n"));
    }

    public static Boolean visitReplacedStyled(StringVisitable stringVisitable, Style style, CharacterVisitor characterVisitor) {
        StreamerMode streamerMode = getInstance();
        if (streamerMode != null && streamerMode.isEnabled() && streamerMode.replaceText.getValue()) {
            String string = collectPlainText(stringVisitable, style);
            if (!containsIgnoreCase(string, "spook")) {
                return null;
            }

            streamerMode.logReplacement("chat", string);
            return visitReplacedChars(stringVisitable, style, characterVisitor);
        } else {
            return null;
        }
    }

    public static OrderedText replaceOrderedText(StringVisitable stringVisitable, String string) {
        StreamerMode streamerMode = getInstance();
        if (streamerMode != null && streamerMode.isEnabled() && streamerMode.replaceText.getValue()) {
            String string2 = collectPlainText(stringVisitable, Style.EMPTY);
            if (!containsIgnoreCase(string2, "spook")) {
                return null;
            }

            streamerMode.logReplacement(string, string2);
            return characterVisitor -> visitReplacedChars(stringVisitable, Style.EMPTY, characterVisitor);
        } else {
            return null;
        }
    }

    private static String collectPlainText(StringVisitable stringVisitable, Style style) {
        StringBuilder builder = new StringBuilder();
        stringVisitable.visit((style2, string) -> {
            builder.append(string);
            return Optional.empty();
        }, style);
        return builder.toString();
    }

    private void logReplacement(String string, String string2) {
        if (this.debugLog.getValue() && !string2.equals(lastLoggedText)) {
            lastLoggedText = string2;
            Sadnes.getLogger().info("[StreamerMode] {} «{}»", string, string2.replace("\n", "\\n"));
        }
    }

    private static boolean visitReplacedChars(StringVisitable stringVisitable, Style style, CharacterVisitor characterVisitor) {
        return visitReplacedSegments(stringVisitable, style, (string, style2) -> TextVisitFactory.visitFormatted(string, 0, style2, characterVisitor));
    }

    public static Text replaceInText(Text text) {
        if (text == null) {
            return null;
        }

        StringVisitable stringVisitable = replaceInVisitable(text);
        return stringVisitable instanceof Text ? (Text)stringVisitable : text;
    }

    public static StringVisitable replaceInVisitable(StringVisitable stringVisitable) {
        StreamerMode streamerMode = getInstance();
        if (streamerMode != null && streamerMode.isEnabled() && streamerMode.replaceText.getValue()) {
            String string = collectPlainText(stringVisitable, Style.EMPTY);
            if (!containsIgnoreCase(string, "spook")) {
                return stringVisitable;
            }

            streamerMode.logReplacement("chat", string);
            MutableText mutableText = Text.empty();
            visitReplacedSegments(stringVisitable, Style.EMPTY, (string2, style) -> {
                mutableText.append(Text.literal(string2).setStyle(style));
                return true;
            });
            return mutableText;
        } else {
            return stringVisitable;
        }
    }

    private static boolean visitReplacedSegments(StringVisitable stringVisitable, Style style, StreamerMode.StyledSegmentConsumer styledSegmentConsumer) {
        ArrayList arrayList = new ArrayList();
        StringBuilder builder = new StringBuilder();
        stringVisitable.visit((style2, string) -> {
            int i = string.length();

            for (int i2 = 0; i2 < i; i2++) {
                arrayList.add(style2);
            }

            builder.append(string);
            return Optional.empty();
        }, style);
        String string2 = builder.toString();
        int i3 = string2.length();
        StringBuilder builder2 = new StringBuilder();
        Style style3 = null;
        int i4 = 0;

        while (i4 < i3) {
            Style style4 = (Style)arrayList.get(i4);
            if (style3 != null && style3 != style4 && !builder2.isEmpty()) {
                if (!styledSegmentConsumer.accept(builder2.toString(), style3)) {
                    return false;
                }

                builder2.setLength(0);
            }

            style3 = style4;
            String string3 = null;
            int i5 = 1;

            for (String[] strings : TEXT_REPLACEMENTS) {
                int i6 = strings[0].length();
                if (i4 + i6 <= i3 && string2.regionMatches(true, i4, strings[0], 0, i6)) {
                    string3 = strings[1];
                    i5 = i6;
                    break;
                }
            }

            builder2.append(string3 != null ? string3 : String.valueOf(string2.charAt(i4)));
            i4 += i5;
        }

        return builder2.isEmpty() || style3 == null || styledSegmentConsumer.accept(builder2.toString(), style3);
    }

    private static String replaceIgnoreCase(String string, String string2, String string3) {
        int i = string2.length();
        int i2 = string.length();
        if (i2 < i) {
            return string;
        }

        StringBuilder builder = null;
        int i3 = 0;

        while (i3 <= i2 - i) {
            if (string.regionMatches(true, i3, string2, 0, i)) {
                if (builder == null) {
                    builder = new StringBuilder(i2).append(string, 0, i3);
                }

                builder.append(string3);
                i3 += i;
            } else {
                if (builder != null) {
                    builder.append(string.charAt(i3));
                }

                i3++;
            }
        }

        if (builder == null) {
            return string;
        }

        builder.append(string, i3, i2);
        return builder.toString();
    }

    private static boolean containsIgnoreCase(String string, String string2) {
        int i = string2.length();
        if (i == 0) {
            return false;
        }

        int i2 = string.length() - i;

        for (int i3 = 0; i3 <= i2; i3++) {
            if (string.regionMatches(true, i3, string2, 0, i)) {
                return true;
            }
        }

        return false;
    }

    record AdReplacement(String marker, Supplier<MutableText> builder) {
    }

    @FunctionalInterface
    interface StyledSegmentConsumer {
        boolean accept(String string, Style style);
    }
}
