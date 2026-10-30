package tech.sadnes.client.feature;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.config.ConfigManager;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.event.impl.WorldTickEvent;
import tech.sadnes.client.feature.impl.combat.AimAssist;
import tech.sadnes.client.feature.impl.combat.AimBot;
import tech.sadnes.client.feature.impl.combat.AntiBot;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.feature.impl.combat.AutoCart;
import tech.sadnes.client.feature.impl.combat.AutoCrystal;
import tech.sadnes.client.feature.impl.combat.AutoEC;
import tech.sadnes.client.feature.impl.combat.AutoExtinguish;
import tech.sadnes.client.feature.impl.combat.AutoGApple;
import tech.sadnes.client.feature.impl.combat.AutoMace;
import tech.sadnes.client.feature.impl.combat.AutoPotion;
import tech.sadnes.client.feature.impl.combat.AutoSwap;
import tech.sadnes.client.feature.impl.combat.AutoTotem;
import tech.sadnes.client.feature.impl.combat.AutoWeapon;
import tech.sadnes.client.feature.impl.combat.BackTrack;
import tech.sadnes.client.feature.impl.combat.CpvpHelper;
import tech.sadnes.client.feature.impl.combat.Criticals;
import tech.sadnes.client.feature.impl.combat.Hitbox;
import tech.sadnes.client.feature.impl.combat.ShieldBreaker;
import tech.sadnes.client.feature.impl.combat.TriggerBot;
import tech.sadnes.client.feature.impl.combat.VelocityControl;
import tech.sadnes.client.feature.impl.hud.Armor;
import tech.sadnes.client.feature.impl.hud.ClickGui;
import tech.sadnes.client.feature.impl.hud.Cooldowns;
import tech.sadnes.client.feature.impl.hud.Hotkeys;
import tech.sadnes.client.feature.impl.hud.Inventory;
import tech.sadnes.client.feature.impl.hud.Luna;
import tech.sadnes.client.feature.impl.hud.MediaPlayer;
import tech.sadnes.client.feature.impl.hud.NetworkTest;
import tech.sadnes.client.feature.impl.hud.Notifications;
import tech.sadnes.client.feature.impl.hud.Potions;
import tech.sadnes.client.feature.impl.hud.ServerBinds;
import tech.sadnes.client.feature.impl.hud.StaffList;
import tech.sadnes.client.feature.impl.hud.TargetCooldowns;
import tech.sadnes.client.feature.impl.hud.TargetHud;
import tech.sadnes.client.feature.impl.hud.Watermark;
import tech.sadnes.client.feature.impl.misc.AncientESP;
import tech.sadnes.client.feature.impl.misc.AntiAFK;
import tech.sadnes.client.feature.impl.misc.AutoAccept;
import tech.sadnes.client.feature.impl.misc.AutoAuth;
import tech.sadnes.client.feature.impl.misc.AutoBeneficialSell;
import tech.sadnes.client.feature.impl.misc.AutoBuild;
import tech.sadnes.client.feature.impl.misc.AutoClan;
import tech.sadnes.client.feature.impl.misc.AutoDonutSell;
import tech.sadnes.client.feature.impl.misc.AutoDuel;
import tech.sadnes.client.feature.impl.misc.AutoFish;
import tech.sadnes.client.feature.impl.misc.AutoGamble;
import tech.sadnes.client.feature.impl.misc.AutoLeave;
import tech.sadnes.client.feature.impl.misc.AutoSell;
import tech.sadnes.client.feature.impl.misc.AutoWarden;
import tech.sadnes.client.feature.impl.misc.ChatHelper;
import tech.sadnes.client.feature.impl.misc.ClientSpoof;
import tech.sadnes.client.feature.impl.misc.DiscordRPC;
import tech.sadnes.client.feature.impl.misc.Events;
import tech.sadnes.client.feature.impl.misc.FreeCam;
import tech.sadnes.client.feature.impl.misc.ItemHelper;
import tech.sadnes.client.feature.impl.misc.ItemScroller;
import tech.sadnes.client.feature.impl.misc.Joiner;
import tech.sadnes.client.feature.impl.misc.KillTalk;
import tech.sadnes.client.feature.impl.misc.LockSlot;
import tech.sadnes.client.feature.impl.misc.NoFriendDamage;
import tech.sadnes.client.feature.impl.misc.NoRotationBreak;
import tech.sadnes.client.feature.impl.misc.PartyPoint;
import tech.sadnes.client.feature.impl.misc.PvPSafe;
import tech.sadnes.client.feature.impl.misc.QuickInteract;
import tech.sadnes.client.feature.impl.misc.ServerAssist;
import tech.sadnes.client.feature.impl.misc.Sounds;
import tech.sadnes.client.feature.impl.misc.StreamerMode;
import tech.sadnes.client.feature.impl.misc.Test;
import tech.sadnes.client.feature.impl.misc.Tracker;
import tech.sadnes.client.feature.impl.misc.farmbase.FarmBase;
import tech.sadnes.client.feature.impl.movement.AbilitiesFly;
import tech.sadnes.client.feature.impl.movement.Airstuck;
import tech.sadnes.client.feature.impl.movement.Baritone;
import tech.sadnes.client.feature.impl.movement.BlockNormalizer;
import tech.sadnes.client.feature.impl.movement.ElytraRecast;
import tech.sadnes.client.feature.impl.movement.ElytraSwap;
import tech.sadnes.client.feature.impl.movement.Flight;
import tech.sadnes.client.feature.impl.movement.GuiWalk;
import tech.sadnes.client.feature.impl.movement.MovementHelper;
import tech.sadnes.client.feature.impl.movement.NoPush;
import tech.sadnes.client.feature.impl.movement.NoSlow;
import tech.sadnes.client.feature.impl.movement.NoWeb;
import tech.sadnes.client.feature.impl.movement.Speed;
import tech.sadnes.client.feature.impl.movement.Spider;
import tech.sadnes.client.feature.impl.movement.Sprint;
import tech.sadnes.client.feature.impl.movement.Strafe;
import tech.sadnes.client.feature.impl.movement.Timer;
import tech.sadnes.client.feature.impl.movement.WaterSpeed;
import tech.sadnes.client.feature.impl.player.AirPlace;
import tech.sadnes.client.feature.impl.player.AutoRelease;
import tech.sadnes.client.feature.impl.player.AutoRespawn;
import tech.sadnes.client.feature.impl.player.AutoTool;
import tech.sadnes.client.feature.impl.player.ClanUpgrade;
import tech.sadnes.client.feature.impl.player.ClickAction;
import tech.sadnes.client.feature.impl.player.ContainerStealer;
import tech.sadnes.client.feature.impl.player.FastBreak;
import tech.sadnes.client.feature.impl.player.Immortality;
import tech.sadnes.client.feature.impl.player.KTLeave;
import tech.sadnes.client.feature.impl.player.Luckboost;
import tech.sadnes.client.feature.impl.player.NoDelay;
import tech.sadnes.client.feature.impl.player.NoFall;
import tech.sadnes.client.feature.impl.player.NoInteract;
import tech.sadnes.client.feature.impl.player.Nuker;
import tech.sadnes.client.feature.impl.player.OpenWalls;
import tech.sadnes.client.feature.impl.player.PingSpike;
import tech.sadnes.client.feature.impl.player.SuperToss;
import tech.sadnes.client.feature.impl.player.TargetPearl;
import tech.sadnes.client.feature.impl.render.Animations;
import tech.sadnes.client.feature.impl.render.Arrows;
import tech.sadnes.client.feature.impl.render.AspectRatio;
import tech.sadnes.client.feature.impl.render.AuctionHelper;
import tech.sadnes.client.feature.impl.render.BoxEsp;
import tech.sadnes.client.feature.impl.render.CameraTweaks;
import tech.sadnes.client.feature.impl.render.ChinaHat;
import tech.sadnes.client.feature.impl.render.Cosmetics;
import tech.sadnes.client.feature.impl.render.Crosshair;
import tech.sadnes.client.feature.impl.render.Esp;
import tech.sadnes.client.feature.impl.render.FreeLook;
import tech.sadnes.client.feature.impl.render.FullBright;
import tech.sadnes.client.feature.impl.render.HitColor;
import tech.sadnes.client.feature.impl.render.HitMarker;
import tech.sadnes.client.feature.impl.render.ItemPhysics;
import tech.sadnes.client.feature.impl.render.JumpCircle;
import tech.sadnes.client.feature.impl.render.MotionBlur;
import tech.sadnes.client.feature.impl.render.Particles;
import tech.sadnes.client.feature.impl.render.Prediction;
import tech.sadnes.client.feature.impl.render.Previews;
import tech.sadnes.client.feature.impl.render.Removals;
import tech.sadnes.client.feature.impl.render.SeeInvisibles;
import tech.sadnes.client.feature.impl.render.TNTTimer;
import tech.sadnes.client.feature.impl.render.TestRender;
import tech.sadnes.client.feature.impl.render.TransparentPlayers;
import tech.sadnes.client.feature.impl.render.ambience.Ambience;
import tech.sadnes.client.feature.impl.render.hand.HandGlow;
import tech.sadnes.client.feature.impl.render.hand.HandOffset;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.script.ScriptFeature;
import tech.sadnes.client.script.ScriptManager;
import tech.sadnes.client.setting.DragSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.Setting;
import ua.mintantileak.profile.Profile;
import ua.mintantileak.profile.Role;

public final class FeatureManager extends LinkedHashMap<Class<? extends Feature>, Feature> {
    private final Map<Category, List<Feature>> byCategory = new EnumMap<>(Category.class);
    private final List<Feature> sortedFeatures = new ArrayList<>();
    private final List<Feature> hudFeatures = new ArrayList<>();
    private final List<FeatureManager.SearchEntry> searchEntries = new ArrayList<>();

    public void initialize() {
        Sadnes.getLogger().info("Initializing features");
        this.register(
            new Events(),
            new ClickGui(),
            new ChatHelper(),
            new BackTrack(),
            new Watermark(),
            new Joiner(),
            new AttackAura(),
            new AutoMace(),
            new AutoWeapon(),
            new AimAssist(),
            new TriggerBot(),
            new CpvpHelper(),
            new AimBot(),
            new Hitbox(),
            new AntiBot(),
            new VelocityControl(),
            new Sprint(),
            new NoPush(),
            new BlockNormalizer(),
            new Speed(),
            new Strafe(),
            new AutoSwap(),
            new AutoTotem(),
            new AutoCart(),
            new AutoEC(),
            new AutoCrystal(),
            new AutoExtinguish(),
            new AutoPotion(),
            new AutoGApple(),
            new GuiWalk(),
            new ServerAssist(),
            new ClickAction(),
            new Tracker(),
            new PartyPoint(),
            new FreeCam(),
            new ItemScroller(),
            new ItemHelper(),
            new AutoFish(),
            new AutoGamble(),
            new AutoBuild(),
            new AutoSell(),
            new AutoDonutSell(),
            new AutoBeneficialSell(),
            new TargetPearl(),
            new AncientESP(),
            new FarmBase(),
            new AutoTool(),
            new FastBreak(),
            new Nuker(),
            new NoDelay(),
            new NoFall(),
            new CameraTweaks(),
            new AspectRatio(),
            new MotionBlur(),
            new FreeLook(),
            new Removals(),
            new ItemPhysics(),
            new Animations(),
            new Crosshair(),
            new HitMarker(),
            new FullBright(),
            new Ambience(),
            new ElytraSwap(),
            new AuctionHelper(),
            new Esp(),
            new BoxEsp(),
            new Arrows(),
            new TNTTimer(),
            new Prediction(),
            new ChinaHat(),
            new JumpCircle(),
            new Previews(),
            new HitColor(),
            new Cosmetics(),
            new Particles(),
            new HandGlow(),
            new HandOffset(),
            new SeeInvisibles(),
            new TransparentPlayers(),
            new Hotkeys(),
            new Potions(),
            new Cooldowns(),
            new Armor(),
            new ServerBinds(),
            new StaffList(),
            new TargetHud(),
            new TargetCooldowns(),
            new Inventory(),
            new MediaPlayer(),
            new Notifications(),
            new NoSlow(),
            new NoWeb(),
            new Spider(),
            new MovementHelper(),
            new NoFriendDamage(),
            new NoRotationBreak(),
            new ContainerStealer(),
            new AutoDuel(),
            new QuickInteract(),
            new Sounds(),
            new LockSlot(),
            new AutoAccept(),
            new AutoAuth(),
            new OpenWalls(),
            new NoInteract(),
            new Immortality(),
            new AirPlace(),
            new SuperToss(),
            new Criticals(),
            new DiscordRPC(),
            new ClientSpoof(),
            new Timer(),
            new Airstuck(),
            new ElytraRecast(),
            new AutoLeave(),
            new PvPSafe(),
            new AntiAFK(),
            new Flight(),
            new AutoRespawn(),
            new AutoRelease(),
            new KillTalk(),
            new PingSpike(),
            new WaterSpeed(),
            new AutoClan(),
            new ClanUpgrade(),
            new ShieldBreaker(),
            new KTLeave(),
            new AbilitiesFly(),
            new Baritone()
        );
        if (Profile.getRole() != null && Profile.getRole().ordinal() == Role.ALPHA.ordinal()) {
            this.register(new AutoWarden());
        }

        if (Profile.getUid() == 19 || Profile.getUid() == 2291 || Profile.getRole().ordinal() >= Role.ADMIN.ordinal()) {
            this.register(new NetworkTest(), new TestRender(), new Test());
            this.register(new StreamerMode());
            this.register(new Luna());
        }

        if (Profile.getRole() != null && Profile.getRole().ordinal() >= Role.ADMIN.ordinal()) {
            this.register(new Luckboost());
        }

        this.values().forEach(Feature::initialize);
        this.values().stream().filter(Feature::shouldAutoEnable).forEach(feature2 -> feature2.setEnabled(true));
        this.rebuildCategories();
        this.rebuildSorted();
        this.rebuildHudFeatures();
        this.buildSearchIndex();
        Sadnes.getEventBus().subscribe(this);
    }

    public void shutdown() {
        Feature.setInitialized(false);
        Sadnes.getEventBus().unsubscribe(this);
        Collection<Feature> features = this.values();
        Consumer<Feature> consumer = feature2 -> {
            feature2.setEnabled(false, true);
            feature2.destroy();
        };
        features.forEach(consumer);
        this.clear();
        this.byCategory.clear();
        this.sortedFeatures.clear();
        this.hudFeatures.clear();
        this.searchEntries.clear();
    }

    public void register(Feature... features) {
        int i = Array.getLength(features);
        if (0 < i) {
            int i2 = 0;

            while (true) {
                Object object = Array.get(features, i2);
                Class clazz = object.getClass();
                this.put(clazz, (Feature)object);
                int i3 = i2 + 1;
                int i4 = i3 + i3 - i3;
                if (i4 >= i) {
                    return;
                }

                i2 = i4;
            }
        }
    }

    public void unregister(Feature... features) {
        int i = Array.getLength(features);
        if (0 < i) {
            int i2 = 0;

            while (true) {
                Class clazz = Array.get(features, i2).getClass();
                this.remove(clazz);
                int i3 = i2 + 1;
                int i4 = i2 & -2 ^ 618839735;
                int i5 = i4 * i4;
                int i6 = (2 - i5) * i5;
                int i7 = (2 - i6) * i6;
                int i8 = (2 - i7) * i7;
                int i9 = (2 - i8) * i8;
                int i10 = 2 - i9;
                int i11 = i9 * i3 * i10;
                if (i11 >= i) {
                    return;
                }

                i2 = i11;
            }
        }
    }

    public void rebuildCategories() {
        ArrayList<Feature> features = new ArrayList<>(this.values());
        features.sort(Comparator.comparing(Feature::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        this.clear();

        for (Feature feature : features) {
            this.put((Class<? extends Feature>)feature.getClass(), feature);
        }

        this.refreshSearchIndex();
    }

    public void rebuildSorted() {
        this.byCategory.clear();

        for (Feature feature2 : this.values()) {
            this.byCategory.computeIfAbsent(feature2.getFeatureCategory(), category -> new ArrayList<>()).add(feature2);
        }
    }

    public void rebuildHudFeatures() {
        this.sortedFeatures.clear();

        for (Feature feature : this.values()) {
            if (this.isFeatureEnabled(feature)) {
                this.sortedFeatures.add(feature);
            }
        }
    }

    public void buildSearchIndex() {
        this.hudFeatures.clear();

        for (Feature feature : this.values()) {
            if (this.isFeatureVisible(feature)) {
                this.hudFeatures.add(feature);
            }
        }
    }

    public boolean isFeatureEnabled(Feature feature) {
        return feature.isEnabled();
    }

    public boolean isFeatureVisible(Feature feature) {
        return !feature.getBinds().isEmpty() || feature.hasBinds();
    }

    @EventHandler
    public void onKey(WorldTickEvent worldTickEvent) {
        this.rebuildHudFeatures();
        this.buildSearchIndex();
        ConfigManager configManager = Sadnes.getInstance().getConfigManager();
        if (configManager != null) {
            configManager.tickAutoSave();
        }
    }

    @EventHandler
    public void onMouse(ScreenMouseClickEvent screenMouseClickEvent) {
        if (screenMouseClickEvent.isPress() || screenMouseClickEvent.isRelease()) {
            if (screenMouseClickEvent.getScreen() == null || !screenMouseClickEvent.isPress()) {
                this.values().forEach(feature2 -> feature2.onKey(screenMouseClickEvent.getButton(), screenMouseClickEvent.isPress()));
                this.rebuildHudFeatures();
            }
        }
    }

    @EventHandler
    public void onRelease(ScreenKeyEvent screenKeyEvent) {
        if (screenKeyEvent.isPress() || screenKeyEvent.isRelease()) {
            if (screenKeyEvent.getScreen() == null || !screenKeyEvent.isPress()) {
                this.values().forEach(feature2 -> feature2.onKey(screenKeyEvent.getKey(), screenKeyEvent.isPress()));
                this.rebuildHudFeatures();
            }
        }
    }

    public List<Feature> getByCategory(Category category) {
        return this.byCategory.getOrDefault(category, List.of());
    }

    public List<Feature> getSorted() {
        return this.sortedFeatures;
    }

    public List<Feature> getHudFeatures() {
        return this.hudFeatures;
    }

    public void refreshSearchIndex() {
        this.searchEntries.clear();

        for (Feature feature : this.values()) {
            this.indexFeature(feature);
        }

        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        if (scriptManager != null) {
            for (ScriptFeature scriptFeature : scriptManager.getActiveHudElements()) {
                this.indexFeature(scriptFeature);
            }
        }
    }

    private void indexFeature(Module module) {
        List list = module.getSettings();
        int i = list.size();
        ArrayList arrayList = new ArrayList(i);

        for (int i2 = 0; i2 < i; i2++) {
            Setting setting = (Setting)list.get(i2);
            if (module.isSettingVisible(setting) && !(setting instanceof DragSetting)) {
                String string = Language.translate(setting.getName());
                StringBuilder builder = new StringBuilder(this.normalize(string));
                List list2 = this.getSettingValues(setting);
                int i3 = list2.size();

                for (int i4 = 0; i4 < i3; i4++) {
                    String string2 = this.normalize((String)list2.get(i4));
                    if (!string2.isEmpty()) {
                        builder.append(' ').append(string2);
                    }
                }

                arrayList.add(new FeatureManager.SettingEntry(setting, string, list2, builder.toString()));
            }
        }

        this.searchEntries.add(new FeatureManager.SearchEntry(module, List.copyOf(arrayList)));
    }

    private List<String> getSettingValues(Setting<?> setting) {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        if (setting instanceof ModeSetting modeSetting) {
            for (String string : modeSetting.getModes()) {
                this.putSearchAlias(linkedHashMap, setting.getName(), string);
            }
        } else if (setting instanceof MultiSetting multiSetting) {
            for (MultiSetting.Entry entry : multiSetting.getEntries()) {
                this.putSearchAlias(linkedHashMap, setting.getName(), entry.value().getName());
            }
        }

        return linkedHashMap.isEmpty() ? List.of() : new ArrayList<>(linkedHashMap.values());
    }

    private void putSearchAlias(Map<String, String> map, String string, String string2) {
        String string3 = Language.translateOption(string, string2);
        String string4 = this.normalize(string3);
        if (!string4.isEmpty() && !map.containsKey(string4)) {
            map.put(string4, string3);
        }
    }

    public List<FeatureManager.SearchEntry> getSearchEntries() {
        return List.copyOf(this.searchEntries);
    }

    private String normalize(String string) {
        return string == null ? "" : string.trim().toLowerCase(Locale.ROOT);
    }

    public <T extends Feature> T getFeature(Class<T> clazz) {
        return (T)this.get(clazz);
    }

    public record SearchEntry(Module feature, List<FeatureManager.SettingEntry> settings) {
    }

    public record SettingEntry(Setting<?> setting, String displayName, List<String> values, String searchLine) {
    }
}
