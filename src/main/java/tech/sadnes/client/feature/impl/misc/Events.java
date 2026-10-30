package tech.sadnes.client.feature.impl.misc;

import java.util.Locale;
import java.util.Set;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;

@FeatureInfo(name = "Events", description = "feature.impl.misc.events", displayName = "feature.impl.misc.events", category = Category.MISC, hidden = true)
public final class Events extends Feature {
    private static final Set<String> FILTERABLE_EVENTS = Set.of(
        "boss", "cargo", "container", "cube", "golden_fortress", "jaycob", "mine", "parcels", "postal", "ship", "snowquarry"
    );
    private static final Set<String> FUNTIME_EVENTS = Set.of(
        "airdrop",
        "altar",
        "altarundead",
        "beacon",
        "deathchest",
        "geyser",
        "hellm",
        "meteor",
        "meteor_rain",
        "mystic",
        "myst_beacon",
        "portal",
        "santa",
        "volcano",
        "voting"
    );
    private boolean funTimeMode;
    public final MultiSetting filter = new MultiSetting(
        "eventtimer.filter",
        BooleanSetting.of("boss", true),
        BooleanSetting.of("cargo", true),
        BooleanSetting.of("container", true),
        BooleanSetting.of("cube", true),
        BooleanSetting.of("golden_fortress", true),
        BooleanSetting.of("jaycob", true),
        BooleanSetting.of("mine", true),
        BooleanSetting.of("parcels", true),
        BooleanSetting.of("postal", true),
        BooleanSetting.of("ship", true),
        BooleanSetting.of("snowquarry", true)
    );
    public final MultiSetting modes = new MultiSetting(
        "eventtimer.modes",
        BooleanSetting.of("classic", true),
        BooleanSetting.of("prime", true),
        BooleanSetting.of("light_120", true),
        BooleanSetting.of("light_116", true)
    );
    public final MultiSetting ftevents = new MultiSetting(
        "eventtimer.ftevents",
        BooleanSetting.of("airdrop", true),
        BooleanSetting.of("altar", true),
        BooleanSetting.of("altarundead", true),
        BooleanSetting.of("beacon", true),
        BooleanSetting.of("deathchest", true),
        BooleanSetting.of("geyser", true),
        BooleanSetting.of("hellm", true),
        BooleanSetting.of("meteor", true),
        BooleanSetting.of("meteor_rain", true),
        BooleanSetting.of("mystic", true),
        BooleanSetting.of("myst_beacon", true),
        BooleanSetting.of("portal", true),
        BooleanSetting.of("santa", true),
        BooleanSetting.of("volcano", true),
        BooleanSetting.of("voting", true)
    );
    public final MultiSetting ftloot = new MultiSetting(
        "eventtimer.ftloot",
        BooleanSetting.of("common", true),
        BooleanSetting.of("solid", true),
        BooleanSetting.of("rich", true),
        BooleanSetting.of("elite", true),
        BooleanSetting.of("legendary", true)
    );
    public final MultiSetting ftshow = new MultiSetting("eventtimer.ftshow", BooleanSetting.of("active", true), BooleanSetting.of("upcoming", true));
    public final MultiSetting ftanarchies = new MultiSetting(
        "eventtimer.ftanarchies",
        BooleanSetting.of("solo", true),
        BooleanSetting.of("duo", true),
        BooleanSetting.of("trio", true),
        BooleanSetting.of("clan5", true),
        BooleanSetting.of("clan10", true)
    );
    public final ModeSetting sort = new ModeSetting("eventtimer.sort", "time", new String[]{"time", "rarity"});

    public Events() {
        this.filter.setVisibleWhen(() -> !this.funTimeMode ? true : false);
        this.modes.setVisibleWhen(() -> !this.funTimeMode ? true : false);
        this.ftevents.setVisibleWhen(() -> this.funTimeMode);
        this.ftloot.setVisibleWhen(() -> this.funTimeMode);
        this.ftshow.setVisibleWhen(() -> this.funTimeMode);
    }

    public static Events getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(Events.class);
    }

    public void setFunTimeMode(boolean flag) {
        this.funTimeMode = flag;
    }

    public boolean isAnarchyAllowed(int i) {
        switch (String.valueOf(i).charAt(0)) {
            case '2':
                String string = "duo";
                return this.ftanarchies.isSelected(string);
            case '3':
                String string2 = "trio";
                return this.ftanarchies.isSelected(string2);
            case '4':
            case '6':
            case '7':
            case '8':
            default:
                String string3 = "solo";
                return this.ftanarchies.isSelected(string3);
            case '5':
                String string4 = "clan5";
                return this.ftanarchies.isSelected(string4);
            case '9':
                String string5 = "clan10";
                return this.ftanarchies.isSelected(string5);
        }
    }

    public boolean isEventAllowed(String string) {
        if (string != null && !string.isEmpty()) {
            String string2 = string.toLowerCase(Locale.US);
            return !FILTERABLE_EVENTS.contains(string2) || this.filter.isSelected(string2);
        } else {
            return true;
        }
    }

    public boolean isModeAllowed(String string) {
        String string2 = string.toLowerCase(Locale.US);
        if (string2.contains("классик")) {
            String string3 = "classic";
            return this.modes.isSelected(string3);
        } else if (string2.contains("прайм")) {
            String string4 = "prime";
            return this.modes.isSelected(string4);
        } else if (string2.contains("1.20")) {
            String string5 = "light_120";
            return this.modes.isSelected(string5);
        } else {
            String string6 = "light_116";
            return this.modes.isSelected(string6);
        }
    }

    public boolean isFunTimeEventAllowed(String string) {
        return string == null || string.isEmpty() ? this.ftshow.isSelected("upcoming") : !FUNTIME_EVENTS.contains(string) || this.ftevents.isSelected(string);
    }

    public boolean isLootAllowed(String string) {
        if (string != null && !string.isEmpty()) {
            String string2;
            label53: {
                label52: {
                    label51: {
                        label50: {
                            label49: {
                                byte b = -1;
                                switch (string.hashCode()) {
                                    case -1769313434:
                                        if (string.equals("Богатый")) {
                                            break label50;
                                        }
                                        break;
                                    case -957954831:
                                        if (string.equals("Солидный")) {
                                            break label51;
                                        }
                                        break;
                                    case 828965756:
                                        if (string.equals("Обычный")) {
                                            break label52;
                                        }
                                        break;
                                    case 1048297000:
                                        if (string.equals("Легендарный")) {
                                            b = 4;
                                        }
                                        break;
                                    case 1525214739:
                                        if (string.equals("Элитный")) {
                                            break label49;
                                        }
                                }

                                switch (b) {
                                    case 0:
                                        break label52;
                                    case 1:
                                        break label51;
                                    case 2:
                                        break label50;
                                    case 3:
                                        break;
                                    case 4:
                                        string2 = "legendary";
                                        break label53;
                                    default:
                                        string2 = null;
                                        break label53;
                                }
                            }

                            string2 = "elite";
                            break label53;
                        }

                        string2 = "rich";
                        break label53;
                    }

                    string2 = "solid";
                    break label53;
                }

                string2 = "common";
            }

            String string3 = string2;
            return string3 == null || this.ftloot.isSelected(string3);
        } else {
            return true;
        }
    }

    public boolean showActive() {
        return this.ftshow.isSelected("active");
    }

    public boolean isSortByRarity() {
        return this.sort.is("rarity");
    }
}
