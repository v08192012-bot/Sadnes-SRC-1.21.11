package tech.sadnes.client.feature.impl.misc;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.client.session.Session;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.network.message.ChatVisibility;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.util.Arm;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.SyncedOptionsEvent;
import tech.sadnes.client.event.impl.TextEvent;
import tech.sadnes.client.feature.Beta;
import tech.sadnes.client.feature.BetaScope;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.friend.FriendManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.TextSetting;

@Beta(scope = BetaScope.ALL)
@FeatureInfo(
    name = "ClientSpoof",
    description = "feature.impl.misc.clientspoof",
    displayName = "feature.impl.misc.clientspoof",
    category = Category.MISC,
    subcategory = Subcategory.MISC_CLIENT
)
public class ClientSpoof extends Feature {
    private static final String[] LANGUAGES = new String[]{"ru_ru", "en_us", "uk_ua"};
    private static final ParticlesMode[] CHAT_VISIBILITIES = ParticlesMode.values();
    private static final Arm[] ARMS = Arm.values();
    private final BooleanSetting replaceSettings = new BooleanSetting("clientspoof.replaceSettings", true);
    private final MultiSetting fields = new MultiSetting(
            "clientspoof.fields",
            BooleanSetting.of("language"),
            BooleanSetting.of("viewDistance"),
            BooleanSetting.of("chatVisibility"),
            BooleanSetting.of("chatColors"),
            BooleanSetting.of("modelParts"),
            BooleanSetting.of("mainArm"),
            BooleanSetting.of("textFilter"),
            BooleanSetting.of("serverListing"),
            BooleanSetting.of("particles")
        )
        .setVisibleWhen(this.replaceSettings::getValue);
    private final BooleanSetting replaceName = new BooleanSetting("clientspoof.replaceName", false);
    private final TextSetting replaceText = new TextSetting("clientspoof.replaceText", "Protected", "clientspoof.replaceText.hint")
        .setVisibleWhen(this.replaceName::getValue);
    private final BooleanSetting hideFriends = new BooleanSetting("clientspoof.hideFriends", true).setVisibleWhen(this.replaceName::getValue);
    private final Map<String, ClientSpoof.SpoofedOptions> spoofedOptionsCache = new HashMap<>();
    private static volatile boolean bypassed;

    public ClientSpoof() {
        this.fields.setOnChange(this::resendClientSettings);
        this.replaceSettings.setOnChange(this::resendClientSettings);
    }

    public static ClientSpoof getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(ClientSpoof.class);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.resendClientSettings();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.resendClientSettings();
    }

    public static void setBypassed(boolean flag) {
        bypassed = flag;
    }

    @EventHandler
    public void onTextReplace(TextEvent textEvent) {
        if (this.replaceName.getValue()) {
            boolean flag = bypassed;
            TextSetting textSetting = this.replaceText;
            if (!flag) {
                String string = textSetting.getValue();
                Iterator iterator2 = this.getHiddenNames().iterator();
                if (iterator2.hasNext()) {
                    do {
                        String string2 = (String)iterator2.next();
                        textEvent.replace(string2, string);
                    } while (iterator2.hasNext());

                    return;
                }
            }
        }
    }

    public static Text replaceNames(Text text) {
        ClientSpoof clientSpoof = getInstance();
        if (text == null) {
            return text;
        }

        if (clientSpoof != null) {
            if (!clientSpoof.isEnabled()) {
                return text;
            }

            if (clientSpoof.replaceName.getValue()) {
                List list = clientSpoof.getHiddenNames();
                if (list.isEmpty()) {
                    return text;
                }

                String string = text.getString();
                Iterator iterator2 = list.iterator();
                String string2;
                if (iterator2.hasNext()) {
                    String string3 = string;

                    do {
                        String string4 = (String)iterator2.next();
                        CharSequence charSequence = clientSpoof.replaceText.getValue();
                        string3 = string3.replace(string4, charSequence);
                    } while (iterator2.hasNext());

                    string2 = string3;
                } else {
                    string2 = string;
                }

                return !string2.equals(string) ? Text.literal(string2) : text;
            } else {
                return text;
            }
        } else {
            return text;
        }
    }

    public static OrderedText replaceNamesOrdered(StringVisitable stringVisitable) {
        ClientSpoof clientSpoof = getInstance();
        if (clientSpoof != null && clientSpoof.isEnabled() && clientSpoof.replaceName.getValue() && !bypassed) {
            List<String> strings = clientSpoof.getHiddenNames();
            if (strings.isEmpty()) {
                return null;
            }

            StringBuilder builder = new StringBuilder();
            stringVisitable.visit((style, string) -> {
                builder.append(string);
                return Optional.empty();
            }, Style.EMPTY);
            String string2 = builder.toString();

            for (String string3 : strings) {
                if (string2.contains(string3)) {
                    String string4 = clientSpoof.replaceText.getValue();
                    return characterVisitor -> stringVisitable.visit((style2, string5) -> {
                        String string6 = string5;

                        for (String string7 : strings) {
                            string6 = string6.replace(string7, string4);
                        }

                        return TextVisitFactory.visitFormatted(string6, style2, characterVisitor) ? Optional.empty() : StringVisitable.TERMINATE_VISIT;
                    }, Style.EMPTY).isEmpty();
                }
            }
        }

        return null;
    }

    private List<String> getHiddenNames() {
        ArrayList arrayList = new ArrayList();
        Session session = mc.getSession();
        BooleanSetting booleanSetting = this.hideFriends;
        if (session != null) {
            String string = mc.getSession().getUsername();
            if (string != null && !string.isEmpty()) {
                arrayList.add(string);
            }
        }

        if (booleanSetting.getValue()) {
            FriendManager friendManager = Sadnes.getInstance().getFriendManager();
            arrayList.addAll(friendManager);
            return arrayList;
        } else {
            return arrayList;
        }
    }

    @EventHandler
    public void onClientOptionsPacket(SyncedOptionsEvent syncedOptionsEvent) {
        if (this.replaceSettings.getValue()) {
            ClientSpoof.SpoofedOptions spoofedOptions = this.getSpoofedOptions();
            MultiSetting multiSetting = this.fields;
            if (spoofedOptions != null) {
                if (multiSetting.has("language")) {
                    String string = spoofedOptions.language;
                    syncedOptionsEvent.setLanguage(string);
                }

                if (multiSetting.has("viewDistance")) {
                    int i = spoofedOptions.viewDistance;
                    int i2 = mc.options.getViewDistance().getValue();
                    int i3 = Math.max(i, i2);
                    syncedOptionsEvent.setViewDistance(i3);
                }

                if (multiSetting.has("chatVisibility")) {
                    ChatVisibility chatVisibility2 = spoofedOptions.chatVisibility;
                    syncedOptionsEvent.setChatVisibility(chatVisibility2);
                }

                if (multiSetting.has("chatColors")) {
                    boolean flag = spoofedOptions.chatColors;
                    syncedOptionsEvent.setChatColorsEnabled(flag);
                }

                if (multiSetting.has("modelParts")) {
                    int i4 = spoofedOptions.modelParts;
                    syncedOptionsEvent.setPlayerModelParts(i4);
                }

                if (multiSetting.has("mainArm")) {
                    Arm arm = spoofedOptions.mainArm;
                    syncedOptionsEvent.setMainArm(arm);
                }

                if (multiSetting.has("textFilter")) {
                    boolean flag2 = spoofedOptions.textFilter;
                    syncedOptionsEvent.setFiltersText(flag2);
                }

                if (multiSetting.has("serverListing")) {
                    boolean flag3 = spoofedOptions.serverListing;
                    syncedOptionsEvent.setAllowsServerListing(flag3);
                }

                if (multiSetting.has("particles")) {
                    ParticlesMode particlesMode = spoofedOptions.particles;
                    syncedOptionsEvent.setParticleStatus(particlesMode);
                    return;
                }
            }
        }
    }

    private ClientSpoof.SpoofedOptions getSpoofedOptions() {
        String string = this.getAccountKey();
        if (string != null) {
            boolean flag = string.isEmpty();
            Map<String, ClientSpoof.SpoofedOptions> map = this.spoofedOptionsCache;
            if (flag) {
                return null;
            }

            Function<String, ClientSpoof.SpoofedOptions> function = ClientSpoof::createSpoofedOptions;
            return (ClientSpoof.SpoofedOptions)map.computeIfAbsent(string, function);
        } else {
            return null;
        }
    }

    private String getAccountKey() {
        if (mc.player != null && mc.player.getGameProfile() != null) {
            return mc.player.getGameProfile().name();
        } else {
            return mc.getSession() != null ? mc.getSession().getUsername() : null;
        }
    }

    private static ClientSpoof.SpoofedOptions createSpoofedOptions(String string) {
        int i = string.hashCode();
        long l = i;
        Random random = new Random(l);
        String[] strings = LANGUAGES;
        int i2 = Array.getLength(strings);
        int i3 = random.nextInt(i2);
        Object object = Array.get(strings, i3);
        int i4 = random.nextInt(11);
        ChatVisibility chatVisibility = ChatVisibility.FULL;
        boolean flag = random.nextBoolean();
        int i5 = randomModelParts(random);
        Arm[] arms = ARMS;
        int i6 = Array.getLength(arms);
        int i7 = random.nextInt(i6);
        Object object2 = Array.get(arms, i7);
        boolean flag2 = random.nextBoolean();
        ParticlesMode[] particlesModes = CHAT_VISIBILITIES;
        int i8 = Array.getLength(particlesModes);
        int i9 = random.nextInt(i8);
        Object object3 = Array.get(particlesModes, i9);
        int i10 = i4 + 6;
        return new ClientSpoof.SpoofedOptions((String)object, i10, chatVisibility, flag, i5, (Arm)object2, false, flag2, (ParticlesMode)object3);
    }

    private static int randomModelParts(Random random) {
        PlayerModelPart[] playerModelParts = PlayerModelPart.values();
        int i = Array.getLength(playerModelParts);
        int i2 = 0;
        int i3 = 0;
        if (0 >= i) {
            return 0;
        }

        do {
            Object object = Array.get(playerModelParts, i2);
            if (random.nextBoolean()) {
                int i4 = i3 & -2 ^ 1912029597;
                int i5 = i4 * i4;
                int i6 = (2 - i5) * i5;
                int i7 = (2 - i6) * i6;
                int i8 = (2 - i7) * i7;
                int i9 = (2 - i8) * i8;
                int i10 = (2 - i9) * i9;
                int i11 = ((PlayerModelPart)object).getBitFlag();
                i3 = (i3 | i11) * i10;
            }

            int i12 = i2 + 1;
            int i13 = i2 & -2 ^ 1132140619;
            int i14 = i13 * i13;
            int i15 = (2 - i14) * i14;
            int i16 = (2 - i15) * i15;
            int i17 = (2 - i16) * i16;
            int i18 = (2 - i17) * i17;
            int i19 = 2 - i18;
            i2 = i18 * i12 * i19;
        } while (i2 < i);

        return i3;
    }

    private void resendClientSettings() {
        if (mc.player != null && mc.player.networkHandler != null) {
            mc.options.sendClientSettings();
        }
    }

    record SpoofedOptions(
        String language,
        int viewDistance,
        ChatVisibility chatVisibility,
        boolean chatColors,
        int modelParts,
        Arm mainArm,
        boolean textFilter,
        boolean serverListing,
        ParticlesMode particles
    ) {
    }
}
