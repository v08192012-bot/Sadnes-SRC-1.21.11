package tech.sadnes.client.figura;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.UserData;
import org.figuramc.figura.avatar.local.LocalAvatarFetcher;
import org.figuramc.figura.avatar.local.LocalAvatarLoader;
import org.figuramc.figura.math.matrix.FiguraMat4;
import org.figuramc.figura.math.vector.FiguraVec4;
import org.figuramc.figura.model.FiguraModelPart;
import org.figuramc.figura.model.ParentType;
import org.figuramc.figura.model.rendering.Vertex;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.feature.impl.render.Esp;
import tech.sadnes.client.feature.impl.render.esp.EspTargetType;

public final class FiguraAvatarManager {
    private static final Map<EspTargetType, FiguraAvatarManager.AvatarSlot> CATEGORY_AVATARS = new EnumMap<>(EspTargetType.class);
    private static final Map<Path, FiguraAvatarManager.AvatarSlot> PATH_AVATARS = new HashMap<>();
    private static final Map<UUID, FiguraAvatarManager.AvatarSlot> APPLIED_AVATARS = new HashMap<>();
    private static final int CATEGORY_SWITCH_THRESHOLD = 15;
    private static final Map<UUID, EspTargetType> STABLE_CATEGORIES = new HashMap<>();
    private static final Map<UUID, EspTargetType> PENDING_CATEGORIES = new HashMap<>();
    private static final Map<UUID, Integer> PENDING_COUNTS = new HashMap<>();
    private static final Map<UUID, FiguraAvatarManager.AvatarSlot> TARGET_AVATARS = new HashMap<>();
    private static final Set<UUID> SEEN_PLAYERS = new HashSet<>();
    private static Path avatarsDirectory;
    private static final Map<Avatar, FiguraAvatarManager.HeightCache> HEIGHT_CACHE = new WeakHashMap<>();
    private static final UUID PREVIEW_UUID = UUID.nameUUIDFromBytes("sadnes:panel-preview".getBytes(StandardCharsets.UTF_8));
    private static final Map<EspTargetType, UUID> CATEGORY_PREVIEW_UUIDS = new EnumMap<>(EspTargetType.class);
    private static final FiguraAvatarManager.AvatarSlot PREVIEW_SLOT;

    private FiguraAvatarManager() {
    }

    public static Path resolveAvatar(String string) {
        if (string != null && !string.isBlank()) {
            Path path = ensureAvatarsDirectory().resolve(string.trim());
            return LocalAvatarFetcher.isAvatar(path) ? path : null;
        } else {
            return null;
        }
    }

    public static Path getAvatarsDirectory() {
        return ensureAvatarsDirectory();
    }

    private static Path ensureAvatarsDirectory() {
        if (avatarsDirectory == null) {
            Path path = MinecraftClient.getInstance().runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("avatars");

            try {
                Files.createDirectories(path);
            } catch (IOException ioException) {
            }

            avatarsDirectory = path;
        }

        return avatarsDirectory;
    }

    public static List<FiguraAvatarManager.AvatarEntry> listAvatars() {
        ArrayList arrayList = new ArrayList();

        try (Stream<Path> stream = Files.list(ensureAvatarsDirectory())) {
            stream.filter(path -> Files.isDirectory(path))
                .filter(LocalAvatarFetcher::isAvatar)
                .sorted(Comparator.comparing(path2 -> path2.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                .forEach(path3 -> arrayList.add(new FiguraAvatarManager.AvatarEntry(path3.getFileName().toString(), path3, path3.resolve("avatar.png"))));
        } catch (IOException ioException) {
        }

        return arrayList;
    }

    public static UUID getPreviewUuid() {
        return PREVIEW_UUID;
    }

    public static boolean updatePreview(Path path2) {
        if (!Objects.equals(PREVIEW_SLOT.path, path2)) {
            PREVIEW_SLOT.path = path2;
            PREVIEW_SLOT.nbt = null;
            if (PREVIEW_SLOT.userData != null) {
                PREVIEW_SLOT.userData.clear();
                PREVIEW_SLOT.userData = null;
            }

            AvatarManager.removeLocalAvatar(PREVIEW_UUID);
            if (path2 != null) {
                PREVIEW_SLOT.userData = new UserData(UUID.randomUUID());
                LocalAvatarLoader.loadAvatar(path2, PREVIEW_SLOT.userData);
            }
        }

        if (PREVIEW_SLOT.nbt == null && PREVIEW_SLOT.userData != null) {
            NbtCompound nbtCompound = AvatarManager.avatarNbt(PREVIEW_SLOT.userData);
            if (nbtCompound != null) {
                PREVIEW_SLOT.nbt = nbtCompound;
                PREVIEW_SLOT.userData.clear();
                PREVIEW_SLOT.userData = null;
            }
        }

        if (PREVIEW_SLOT.nbt != null && !isAvatarApplied(PREVIEW_UUID, PREVIEW_SLOT.nbt)) {
            AvatarManager.setLocalAvatar(PREVIEW_UUID, PREVIEW_SLOT.nbt);
        }

        return PREVIEW_SLOT.nbt != null;
    }

    public static void clearPreview() {
        AvatarManager.removeLocalAvatar(PREVIEW_UUID);
        PREVIEW_SLOT.path = null;
        PREVIEW_SLOT.nbt = null;
        if (PREVIEW_SLOT.userData != null) {
            PREVIEW_SLOT.userData.clear();
            PREVIEW_SLOT.userData = null;
        }
    }

    public static UUID getCategoryPreviewUuid(EspTargetType espTargetType) {
        FiguraAvatarManager.AvatarSlot avatarSlot = CATEGORY_AVATARS.get(espTargetType);
        if (avatarSlot != null && avatarSlot.nbt != null) {
            UUID uuid = CATEGORY_PREVIEW_UUIDS.get(espTargetType);
            if (!isAvatarApplied(uuid, avatarSlot.nbt)) {
                AvatarManager.setLocalAvatar(uuid, avatarSlot.nbt);
            }

            return uuid;
        } else {
            return null;
        }
    }

    public static void clearAllPreviews() {
        clearPreview();
        Iterator iterator2 = CATEGORY_PREVIEW_UUIDS.values().iterator();

        while (iterator2.hasNext()) {
            AvatarManager.removeLocalAvatar((UUID)iterator2.next());
        }
    }

    public static void setCategoryAvatar(EspTargetType espTargetType, Path path2) {
        FiguraAvatarManager.AvatarSlot avatarSlot = CATEGORY_AVATARS.computeIfAbsent(espTargetType, espTargetType2 -> new FiguraAvatarManager.AvatarSlot());
        if (!Objects.equals(avatarSlot.path, path2)) {
            loadSlot(avatarSlot, path2);
        }
    }

    private static FiguraAvatarManager.AvatarSlot loadSlot(FiguraAvatarManager.AvatarSlot avatarSlot, Path path2) {
        avatarSlot.path = path2;
        avatarSlot.nbt = null;
        if (avatarSlot.userData != null) {
            avatarSlot.userData.clear();
            avatarSlot.userData = null;
        }

        if (path2 != null) {
            avatarSlot.userData = new UserData(UUID.randomUUID());
            LocalAvatarLoader.loadAvatar(path2, avatarSlot.userData);
        }

        return avatarSlot;
    }

    private static void resolveSlotNbt(FiguraAvatarManager.AvatarSlot avatarSlot) {
        if (avatarSlot.nbt == null && avatarSlot.userData != null) {
            NbtCompound nbtCompound = AvatarManager.avatarNbt(avatarSlot.userData);
            if (nbtCompound != null) {
                avatarSlot.nbt = nbtCompound;
                avatarSlot.userData.clear();
                avatarSlot.userData = null;
            }
        }
    }

    public static void tick(MinecraftClient minecraftClient, Set<EspTargetType> espTargetTypes, Map<String, Path> map) {
        if (minecraftClient.world == null) {
            reset();
        } else {
            Iterator iterator2 = PATH_AVATARS.entrySet().iterator();

            while (iterator2.hasNext()) {
                Entry entry = (Entry)iterator2.next();
                if (!map.containsValue(entry.getKey())) {
                    loadSlot((FiguraAvatarManager.AvatarSlot)entry.getValue(), null);
                    iterator2.remove();
                }
            }

            for (Path path : map.values()) {
                if (path != null) {
                    PATH_AVATARS.computeIfAbsent(path, path2 -> loadSlot(new FiguraAvatarManager.AvatarSlot(), path2));
                }
            }

            CATEGORY_AVATARS.values().forEach(FiguraAvatarManager::resolveSlotNbt);
            PATH_AVATARS.values().forEach(FiguraAvatarManager::resolveSlotNbt);
            TARGET_AVATARS.clear();
            SEEN_PLAYERS.clear();

            for (PlayerEntity playerEntity : minecraftClient.world.getPlayers()) {
                UUID uuid = playerEntity.getUuid();
                SEEN_PLAYERS.add(uuid);
                String string = playerEntity.getGameProfile().name();
                FiguraAvatarManager.AvatarSlot avatarSlot;
                if (map.containsKey(string)) {
                    avatarSlot = PATH_AVATARS.get(map.get(string));
                } else {
                    EspTargetType espTargetType = Esp.getTargetType(playerEntity);
                    if (espTargetType == null) {
                        continue;
                    }

                    EspTargetType espTargetType2 = stabilizeCategory(uuid, espTargetType);
                    if (!espTargetTypes.contains(espTargetType2)) {
                        continue;
                    }

                    avatarSlot = CATEGORY_AVATARS.get(espTargetType2);
                }

                if (avatarSlot != null && avatarSlot.nbt != null) {
                    TARGET_AVATARS.put(uuid, avatarSlot);
                }
            }

            STABLE_CATEGORIES.keySet().retainAll(SEEN_PLAYERS);
            PENDING_CATEGORIES.keySet().retainAll(SEEN_PLAYERS);
            PENDING_COUNTS.keySet().retainAll(SEEN_PLAYERS);
            Iterator iterator3 = APPLIED_AVATARS.entrySet().iterator();

            while (iterator3.hasNext()) {
                Entry entry2 = (Entry)iterator3.next();
                if (TARGET_AVATARS.get(entry2.getKey()) != entry2.getValue()) {
                    AvatarManager.removeLocalAvatar((UUID)entry2.getKey());
                    iterator3.remove();
                }
            }

            for (Entry entry3 : TARGET_AVATARS.entrySet()) {
                UUID uuid2 = (UUID)entry3.getKey();
                FiguraAvatarManager.AvatarSlot avatarSlot2 = (FiguraAvatarManager.AvatarSlot)entry3.getValue();
                if (!isAvatarApplied(uuid2, avatarSlot2.nbt)) {
                    AvatarManager.setLocalAvatar(uuid2, avatarSlot2.nbt);
                }

                APPLIED_AVATARS.put(uuid2, avatarSlot2);
            }
        }
    }

    private static boolean isAvatarApplied(UUID uuid, NbtCompound nbtCompound) {
        Avatar avatar = AvatarManager.getLoadedAvatar(uuid);
        return avatar != null && avatar.nbt == nbtCompound;
    }

    public static boolean hasCustomAvatar(EntityRenderState entityRenderState) {
        if (APPLIED_AVATARS.isEmpty()) {
            return false;
        }

        Entity entity = AvatarManager.getEntity(entityRenderState);
        return entity != null && APPLIED_AVATARS.containsKey(entity.getUuid());
    }

    public static float getHeadHeight(EntityRenderState entityRenderState) {
        Avatar avatar = AvatarManager.getAvatar(entityRenderState);
        if (avatar != null && avatar.renderer != null && avatar.renderer.root != null) {
            long l = System.currentTimeMillis();
            FiguraAvatarManager.HeightCache heightCache = HEIGHT_CACHE.get(avatar);
            if (heightCache != null && l - heightCache.measuredAt() < 1000L) {
                return heightCache.height();
            }

            float f = computeMaxHeadY(avatar.renderer.root, FiguraMat4.of(), false);
            float f2 = f == -Float.MAX_VALUE ? Float.NaN : (f - 24.0F) / 16.0F;
            HEIGHT_CACHE.put(avatar, new FiguraAvatarManager.HeightCache(f2, l));
            return f2;
        } else {
            return Float.NaN;
        }
    }

    private static float computeMaxHeadY(FiguraModelPart figuraModelPart, FiguraMat4 figuraMat4, boolean flag) {
        if (!figuraModelPart.customization.visible) {
            return -Float.MAX_VALUE;
        }

        figuraModelPart.customization.recalculate();
        FiguraMat4 figuraMat42 = figuraMat4.copy().rightMultiply(figuraModelPart.customization.positionMatrix);
        boolean flag2 = flag || figuraModelPart.parentType == ParentType.Head;
        float f = -Float.MAX_VALUE;
        if (flag2 && figuraModelPart.vertices != null) {
            for (List<Vertex> vertexes : figuraModelPart.vertices.values()) {
                for (Vertex vertex : vertexes) {
                    f = Math.max(f, (float)figuraMat42.apply(FiguraVec4.of(vertex.x, vertex.y, vertex.z, 1.0)).y);
                }
            }
        }

        for (FiguraModelPart figuraModelPart2 : figuraModelPart.children) {
            f = Math.max(f, computeMaxHeadY(figuraModelPart2, figuraMat42, flag2));
        }

        return f;
    }

    private static EspTargetType stabilizeCategory(UUID uuid, EspTargetType espTargetType) {
        EspTargetType espTargetType2 = STABLE_CATEGORIES.get(uuid);
        if (espTargetType2 == null) {
            STABLE_CATEGORIES.put(uuid, espTargetType);
            PENDING_CATEGORIES.remove(uuid);
            PENDING_COUNTS.remove(uuid);
            return espTargetType;
        } else if (espTargetType == espTargetType2) {
            PENDING_CATEGORIES.remove(uuid);
            PENDING_COUNTS.remove(uuid);
            return espTargetType2;
        } else if (espTargetType != PENDING_CATEGORIES.get(uuid)) {
            PENDING_CATEGORIES.put(uuid, espTargetType);
            PENDING_COUNTS.put(uuid, 1);
            return espTargetType2;
        } else {
            int i = PENDING_COUNTS.getOrDefault(uuid, 0) + 1;
            if (i >= 15) {
                STABLE_CATEGORIES.put(uuid, espTargetType);
                PENDING_CATEGORIES.remove(uuid);
                PENDING_COUNTS.remove(uuid);
                return espTargetType;
            } else {
                PENDING_COUNTS.put(uuid, i);
                return espTargetType2;
            }
        }
    }

    public static void reset() {
        Iterator iterator2 = APPLIED_AVATARS.keySet().iterator();

        while (iterator2.hasNext()) {
            AvatarManager.removeLocalAvatar((UUID)iterator2.next());
        }

        APPLIED_AVATARS.clear();
        Iterator iterator3 = PATH_AVATARS.values().iterator();

        while (iterator3.hasNext()) {
            loadSlot((FiguraAvatarManager.AvatarSlot)iterator3.next(), null);
        }

        PATH_AVATARS.clear();
        STABLE_CATEGORIES.clear();
        PENDING_CATEGORIES.clear();
        PENDING_COUNTS.clear();
    }

    static {
        for (EspTargetType espTargetType : EspTargetType.values()) {
            CATEGORY_PREVIEW_UUIDS.put(espTargetType, UUID.nameUUIDFromBytes(("sadnes:category-preview:" + espTargetType.name()).getBytes(StandardCharsets.UTF_8)));
        }

        PREVIEW_SLOT = new FiguraAvatarManager.AvatarSlot();
    }

    public record AvatarEntry(String name, Path path, Path icon) {
    }

    static final class AvatarSlot {
        Path path;
        UserData userData;
        NbtCompound nbt;
    }

    record HeightCache(float height, long measuredAt) {
    }
}
