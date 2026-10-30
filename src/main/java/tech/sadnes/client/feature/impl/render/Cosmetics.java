package tech.sadnes.client.feature.impl.render;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.entity.player.PlayerEntity;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.cosmetic.Cosmetic;
import tech.sadnes.client.cosmetic.CosmeticType;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.WorldTickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.impl.render.esp.EspTargetType;
import tech.sadnes.client.figura.FiguraAvatarManager;
import tech.sadnes.client.network.BackendService;
import tech.sadnes.client.network.ModelRepository;
import tech.sadnes.client.network.packet.PlayerLook;
import tech.sadnes.client.party.PartyManager;
import tech.sadnes.client.party.PartyMemberView;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.util.math.StopWatch;
import ua.mintantileak.profile.Profile;

@FeatureInfo(
    name = "Cosmetics",
    aliases = "PlayerModels",
    description = "feature.impl.render.cosmetics",
    displayName = "feature.impl.render.cosmetics",
    category = Category.VISUALS,
    subcategory = Subcategory.ENTITIES
)
public final class Cosmetics extends Feature {
    public static final RenderStateDataKey<Cosmetic[]> COSMETICS_KEY = RenderStateDataKey.create(() -> "sadnes:cosmetics");
    private static final CosmeticType[] COSMETIC_TYPES = CosmeticType.values();
    private static final EspTargetType[] MODEL_TARGETS = new EspTargetType[]{
        EspTargetType.SELF, EspTargetType.FRIENDS, EspTargetType.OTHERS, EspTargetType.NAKED, EspTargetType.INVISIBLE
    };
    private final Map<EspTargetType, Cosmetics.TargetSettings> targetSettings = new EnumMap<>(EspTargetType.class);
    private final List<Setting<?>> settings = new ArrayList<>();
    private final EnumSet<EspTargetType> activeModelTargets = EnumSet.noneOf(EspTargetType.class);
    private final Map<EspTargetType, String> lastModelNames = new EnumMap<>(EspTargetType.class);
    private final Map<EspTargetType, Path> modelPaths = new EnumMap<>(EspTargetType.class);
    private final Map<EspTargetType, Cosmetic[]> targetCosmetics = new EnumMap<>(EspTargetType.class);
    private final Cosmetic[] previewCosmetics = new Cosmetic[COSMETIC_TYPES.length];
    private final Map<String, Cosmetic[]> remoteCosmetics = new HashMap<>();
    private final Map<String, Path> remoteModelPaths = new HashMap<>();
    private final List<String> missingModels = new ArrayList<>();
    private final StopWatch downloadTimer = new StopWatch();
    private final StopWatch syncTimer = new StopWatch();

    public Cosmetics() {
        for (EspTargetType espTargetType : EspTargetType.values()) {
            if (espTargetType.isPlayerType()) {
                String string = espTargetType.name().toLowerCase(Locale.ROOT);
                BooleanSetting booleanSetting = new BooleanSetting("models." + string + ".enabled", false);
                TextSetting textSetting = new TextSetting("models." + string + ".model", "", "none");
                EnumMap enumMap = new EnumMap<>(CosmeticType.class);

                for (CosmeticType cosmeticType : COSMETIC_TYPES) {
                    enumMap.put(cosmeticType, new TextSetting("cosmetics." + string + "." + cosmeticType.getId(), "", "none"));
                }

                this.targetSettings.put(espTargetType, new Cosmetics.TargetSettings(booleanSetting, textSetting, enumMap));
                this.settings.add(booleanSetting);
                this.settings.add(textSetting);
                this.settings.addAll(enumMap.values());
                this.targetCosmetics.put(espTargetType, new Cosmetic[COSMETIC_TYPES.length]);
            }
        }
    }

    public static Cosmetics getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(Cosmetics.class);
    }

    public List<EspTargetType> getModelTargets() {
        ArrayList arrayList = new ArrayList(MODEL_TARGETS.length);

        for (EspTargetType espTargetType : MODEL_TARGETS) {
            if (this.targetSettings.containsKey(espTargetType)) {
                arrayList.add(espTargetType);
            }
        }

        return arrayList;
    }

    public String getModel(EspTargetType espTargetType) {
        Cosmetics.TargetSettings targetSettings2 = this.targetSettings.get(espTargetType);
        return targetSettings2 == null ? "" : targetSettings2.model().getValue();
    }

    public void setModel(EspTargetType espTargetType, String string) {
        Cosmetics.TargetSettings targetSettings2 = this.targetSettings.get(espTargetType);
        if (targetSettings2 != null) {
            TextSetting textSetting = targetSettings2.model();
            if (string == null) {
                textSetting.setValue("");
                return;
            }

            textSetting.setValue(string);
        }
    }

    public boolean isModelEnabled(EspTargetType espTargetType) {
        Cosmetics.TargetSettings targetSettings2 = this.targetSettings.get(espTargetType);
        return targetSettings2 != null && targetSettings2.enabled().getValue();
    }

    public void setModelEnabled(EspTargetType espTargetType, boolean flag) {
        Cosmetics.TargetSettings targetSettings2 = this.targetSettings.get(espTargetType);
        if (targetSettings2 != null) {
            targetSettings2.enabled().setValue(flag);
        }
    }

    public boolean isWearing(EspTargetType espTargetType, Cosmetic cosmetic) {
        return cosmetic.id().equals(this.targetSettings.get(espTargetType).wear().get(cosmetic.type()).getValue());
    }

    public void toggleWear(EspTargetType espTargetType, Cosmetic cosmetic) {
        TextSetting textSetting = this.targetSettings.get(espTargetType).wear().get(cosmetic.type());
        if (this.isWearing(espTargetType, cosmetic)) {
            textSetting.setValue("");
        } else {
            textSetting.setValue(cosmetic.id());
        }
    }

    public Cosmetic getFirstCosmetic(EspTargetType espTargetType) {
        for (Cosmetic cosmetic : this.resolveCosmetics(espTargetType, this.targetCosmetics.get(espTargetType))) {
            if (cosmetic != null) {
                return cosmetic;
            }
        }

        return null;
    }

    public Cosmetic getPreviewCosmetic(CosmeticType cosmeticType) {
        return this.previewCosmetics[cosmeticType.ordinal()];
    }

    public void setPreviewCosmetic(EspTargetType espTargetType, Cosmetic cosmetic) {
        this.resolveCosmetics(espTargetType, this.previewCosmetics);
        if (cosmetic != null) {
            this.previewCosmetics[cosmetic.type().ordinal()] = cosmetic;
        }
    }

    public static boolean hasBodywear(Cosmetic[] cosmetics) {
        return cosmetics != null && cosmetics[CosmeticType.BODYWEAR.ordinal()] != null;
    }

    public Cosmetic[] getCosmetics(PlayerEntity playerEntity) {
        if (playerEntity.getUuid().equals(FiguraAvatarManager.getPreviewUuid())) {
            return this.previewCosmetics;
        }

        if (!this.isEnabled()) {
            return null;
        }

        Cosmetic[] cosmetics = this.remoteCosmetics.get(playerEntity.getGameProfile().name());
        if (cosmetics != null) {
            return cosmetics;
        }

        EspTargetType espTargetType = Esp.getTargetType(playerEntity);
        return espTargetType == null ? null : this.resolveCosmetics(espTargetType, this.targetCosmetics.get(espTargetType));
    }

    private Cosmetic[] resolveCosmetics(EspTargetType espTargetType, Cosmetic[] cosmetics) {
        Map map = this.targetSettings.get(espTargetType).wear();

        for (CosmeticType cosmeticType : COSMETIC_TYPES) {
            cosmetics[cosmeticType.ordinal()] = Sadnes.getInstance().getCosmeticsManager().getCosmetic(cosmeticType, ((TextSetting)map.get(cosmeticType)).getValue());
        }

        return cosmetics;
    }

    private PartyManager getSession() {
        BackendService backendService = Sadnes.getInstance().getBackend();
        return backendService == null ? null : backendService.getPartyManager();
    }

    private PlayerLook buildOwnAppearance() {
        Cosmetics.TargetSettings targetSettings2 = this.targetSettings.get(EspTargetType.SELF);
        ArrayList arrayList = new ArrayList();

        for (CosmeticType cosmeticType : COSMETIC_TYPES) {
            String string = targetSettings2.wear().get(cosmeticType).getValue();
            if (!string.isEmpty()) {
                arrayList.add(cosmeticType.getId() + "/" + string);
            }
        }

        return new PlayerLook(targetSettings2.enabled().getValue() ? targetSettings2.model().getValue() : "", arrayList);
    }

    private void syncRemote(PartyManager partyManager) {
        partyManager.setLook(this.buildOwnAppearance());
        this.remoteCosmetics.clear();
        this.remoteModelPaths.clear();
        this.missingModels.clear();
        if (partyManager.inParty()) {
            String string = ServerManager.getServerName();

            for (PartyMemberView partyMemberView : partyManager.getMemberViews()) {
                PlayerLook playerLook = partyManager.getLook(partyMemberView.uid());
                boolean flag;
                if (partyMemberView.hasState()) {
                    flag = partyMemberView.state().getServer().equals(string);
                } else {
                    flag = false;
                }

                if (partyMemberView.uid() != Profile.getUid() && flag && !playerLook.equals(PlayerLook.NONE)) {
                    Cosmetic[] cosmetics2 = new Cosmetic[COSMETIC_TYPES.length];

                    for (String string2 : playerLook.cosmetics()) {
                        int i = string2.indexOf(47);
                        if (i >= 0) {
                            CosmeticType cosmeticType = CosmeticType.fromId(string2.substring(0, i));
                            if (cosmeticType != null) {
                                cosmetics2[cosmeticType.ordinal()] = Sadnes.getInstance().getCosmeticsManager().getCosmetic(cosmeticType, string2.substring(i + 1));
                            }
                        }
                    }

                    this.remoteCosmetics.put(partyMemberView.state().getUsername(), cosmetics2);
                    this.remoteModelPaths.put(partyMemberView.state().getUsername(), this.resolveModelPath(playerLook.model()));
                }
            }

            this.downloadMissingModels();
        }
    }

    private Path resolveModelPath(String string) {
        if (!string.isEmpty() && string.indexOf(47) < 0 && string.indexOf(92) < 0 && !string.contains("..")) {
            Path path = FiguraAvatarManager.resolveAvatar(string);
            if (path == null) {
                this.missingModels.add(string);
            }

            return path;
        } else {
            return null;
        }
    }

    private void downloadMissingModels() {
        if (!this.missingModels.isEmpty() && this.downloadTimer.hasPassed(30000.0)) {
            this.downloadTimer.reset();
            List<String> strings = List.copyOf(this.missingModels);
            ModelRepository.fetchCatalog().thenRun(() -> {
                for (ModelRepository.RemoteModel remoteModel : ModelRepository.getCatalog()) {
                    if (strings.stream().anyMatch(remoteModel.name()::equalsIgnoreCase)) {
                        ModelRepository.download(remoteModel);
                    }
                }
            });
        }
    }

    @EventHandler
    public void onTick(WorldTickEvent worldTickEvent) {
        if (mc.world != null) {
            PartyManager partyManager = this.getSession();
            if (partyManager != null && this.syncTimer.hasPassed(1000.0)) {
                this.syncTimer.reset();
                this.syncRemote(partyManager);
            }

            this.activeModelTargets.clear();

            for (Entry entry : this.targetSettings.entrySet()) {
                EspTargetType espTargetType = (EspTargetType)entry.getKey();
                Cosmetics.TargetSettings targetSettings2 = (Cosmetics.TargetSettings)entry.getValue();
                Path path = null;
                if (targetSettings2.enabled().getValue()) {
                    String string = targetSettings2.model().getValue();
                    if (!Objects.equals(string, this.lastModelNames.get(espTargetType))) {
                        this.lastModelNames.put(espTargetType, string);
                        this.modelPaths.put(espTargetType, FiguraAvatarManager.resolveAvatar(string));
                    }

                    path = this.modelPaths.get(espTargetType);
                }

                FiguraAvatarManager.setCategoryAvatar(espTargetType, path);
                if (path != null) {
                    this.activeModelTargets.add(espTargetType);
                }
            }

            FiguraAvatarManager.tick(mc, this.activeModelTargets, this.remoteModelPaths);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        FiguraAvatarManager.reset();
        this.remoteCosmetics.clear();
        this.remoteModelPaths.clear();
        PartyManager partyManager = this.getSession();
        if (partyManager != null) {
            partyManager.setLook(PlayerLook.NONE);
        }
    }

    record TargetSettings(BooleanSetting enabled, TextSetting model, Map<CosmeticType, TextSetting> wear) {
    }
}
