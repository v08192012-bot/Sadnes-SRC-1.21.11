package tech.sadnes.client.feature.impl.render;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.network.BackendService;
import tech.sadnes.client.network.packet.PlayerState;
import tech.sadnes.client.party.PartyManager;
import tech.sadnes.client.party.PartyMemberView;
import tech.sadnes.client.party.WorldIdentity;
import tech.sadnes.client.util.IMinecraft;
import ua.mintantileak.profile.Profile;

public final class RemotePlayerTracker implements IMinecraft {
    private static final double TELEPORT_DIST_SQ = 1024.0;
    private static final long INTERP_DELAY_MS = 150L;
    private static final long HISTORY_MS = 600L;
    private final Map<String, RemotePlayerTracker.RemotePlayer> players = new HashMap<>();
    private final List<Entity> entities = new ArrayList<>();
    private ClientWorld lastWorld;

    public List<Entity> update(float f) {
        this.entities.clear();
        if (mc.player != null && mc.world != null) {
            BackendService backendService = Sadnes.getInstance().getBackend();
            PartyManager partyManager = backendService != null ? backendService.getPartyManager() : null;
            if (partyManager != null) {
                if (!partyManager.inParty()) {
                    this.players.clear();
                    return this.entities;
                }

                if (this.lastWorld != mc.world) {
                    this.players.clear();
                    this.lastWorld = mc.world;
                }

                String string = WorldIdentity.currentWorldId();
                int i = Profile.getUid();
                long l = System.currentTimeMillis();
                HashSet hashSet = new HashSet();

                for (PartyMemberView partyMemberView : partyManager.getMemberViews()) {
                    if (partyMemberView.uid() != i) {
                        PlayerState playerState = partyMemberView.state();
                        if (playerState != null && WorldIdentity.sameWorld(string, playerState.getWorld()) && !this.isLoaded(playerState.getUsername())) {
                            String string2 = partyMemberView.username();
                            hashSet.add(string2);
                            RemotePlayerTracker.RemotePlayer remotePlayer = this.players.computeIfAbsent(string2, string3 -> this.createPlayer(playerState, l));
                            this.updatePlayer(remotePlayer, playerState, l);
                            this.entities.add(remotePlayer.entity);
                        }
                    }
                }

                this.players.keySet().removeIf(string4 -> !hashSet.contains(string4));
                return this.entities;
            }
        }

        this.players.clear();
        return this.entities;
    }

    private RemotePlayerTracker.RemotePlayer createPlayer(PlayerState playerState, long l) {
        OtherClientPlayerEntity otherClientPlayerEntity = new OtherClientPlayerEntity(mc.world, this.resolveProfile(playerState.getUsername()));
        otherClientPlayerEntity.setYaw(0.0F);
        otherClientPlayerEntity.setHeadYaw(0.0F);
        otherClientPlayerEntity.setPitch(0.0F);
        otherClientPlayerEntity.lastYaw = 0.0F;
        otherClientPlayerEntity.bodyYaw = 0.0F;
        otherClientPlayerEntity.lastBodyYaw = 0.0F;
        return new RemotePlayerTracker.RemotePlayer(otherClientPlayerEntity, playerState, l);
    }

    private GameProfile resolveProfile(String string) {
        if (mc.getNetworkHandler() != null) {
            PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(string);
            if (playerListEntry != null) {
                return playerListEntry.getProfile();
            }
        }

        return new GameProfile(UUID.nameUUIDFromBytes(("OfflinePlayer:" + string).getBytes(StandardCharsets.UTF_8)), string);
    }

    private void updatePlayer(RemotePlayerTracker.RemotePlayer remotePlayer, PlayerState playerState, long l) {
        remotePlayer.addSample(playerState, l);
        remotePlayer.interpolate(l);
        this.updateEquipment(remotePlayer, playerState);
    }

    private void updateEquipment(RemotePlayerTracker.RemotePlayer remotePlayer, PlayerState playerState) {
        List list = playerState.getArmor();
        String string = playerState.getMainHand() + "|" + playerState.getOffHand() + "|" + String.join(",", list);
        if (!string.equals(remotePlayer.equipmentKey)) {
            remotePlayer.equipmentKey = string;
            OtherClientPlayerEntity otherClientPlayerEntity = remotePlayer.entity;
            otherClientPlayerEntity.equipStack(EquipmentSlot.MAINHAND, parseItem(playerState.getMainHand()));
            otherClientPlayerEntity.equipStack(EquipmentSlot.OFFHAND, parseItem(playerState.getOffHand()));
            if (list.size() >= 4) {
                otherClientPlayerEntity.equipStack(EquipmentSlot.HEAD, parseItem((String)list.get(0)));
                otherClientPlayerEntity.equipStack(EquipmentSlot.CHEST, parseItem((String)list.get(1)));
                otherClientPlayerEntity.equipStack(EquipmentSlot.LEGS, parseItem((String)list.get(2)));
                otherClientPlayerEntity.equipStack(EquipmentSlot.FEET, parseItem((String)list.get(3)));
            }
        }
    }

    private static ItemStack parseItem(String string) {
        if (string != null && !string.isEmpty()) {
            Identifier identifier = Identifier.tryParse(string);
            if (identifier == null) {
                return ItemStack.EMPTY;
            }

            Item item = Registries.ITEM.get(identifier);
            return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        } else {
            return ItemStack.EMPTY;
        }
    }

    private boolean isLoaded(String string) {
        for (AbstractClientPlayerEntity abstractClientPlayerEntity : mc.world.getPlayers()) {
            if (abstractClientPlayerEntity != mc.player && string.equalsIgnoreCase(abstractClientPlayerEntity.getGameProfile().name())) {
                return true;
            }
        }

        return false;
    }

    record PositionSample(double x, double y, double z, long at) {
    }

    static final class RemotePlayer {
        final OtherClientPlayerEntity entity;
        private final ArrayDeque<RemotePlayerTracker.PositionSample> history = new ArrayDeque<>();
        String equipmentKey = "";

        RemotePlayer(OtherClientPlayerEntity otherClientPlayerEntity, PlayerState playerState, long l) {
            this.entity = otherClientPlayerEntity;
            this.history.addLast(new RemotePlayerTracker.PositionSample(playerState.getX(), playerState.getY(), playerState.getZ(), l));
            this.setPosition(playerState.getX(), playerState.getY(), playerState.getZ());
        }

        void addSample(PlayerState playerState, long l) {
            RemotePlayerTracker.PositionSample positionSample = this.history.peekLast();
            double d = playerState.getX() - positionSample.x();
            double d2 = playerState.getY() - positionSample.y();
            double d3 = playerState.getZ() - positionSample.z();
            double d4 = d * d + d2 * d2 + d3 * d3;
            if (d4 != 0.0) {
                if (d4 > 1024.0) {
                    this.history.clear();
                }

                this.history.addLast(new RemotePlayerTracker.PositionSample(playerState.getX(), playerState.getY(), playerState.getZ(), l));

                while (this.history.size() > 2 && l - this.history.peekFirst().at() > 600L) {
                    this.history.pollFirst();
                }
            }
        }

        void interpolate(long l) {
            long l2 = l - 150L;
            RemotePlayerTracker.PositionSample positionSample = this.history.peekFirst();
            if (l2 <= positionSample.at()) {
                this.setPosition(positionSample.x(), positionSample.y(), positionSample.z());
            } else {
                for (RemotePlayerTracker.PositionSample positionSample2 : this.history) {
                    if (positionSample2.at() > l2) {
                        double d = (double)(l2 - positionSample.at()) / (positionSample2.at() - positionSample.at());
                        this.setPosition(
                            MathHelper.lerp(d, positionSample.x(), positionSample2.x()), MathHelper.lerp(d, positionSample.y(), positionSample2.y()), MathHelper.lerp(d, positionSample.z(), positionSample2.z())
                        );
                        return;
                    }

                    positionSample = positionSample2;
                }

                this.setPosition(positionSample.x(), positionSample.y(), positionSample.z());
            }
        }

        private void setPosition(double d, double d2, double d3) {
            this.entity.setPosition(d, d2, d3);
            this.entity.lastRenderX = d;
            this.entity.lastRenderY = d2;
            this.entity.lastRenderZ = d3;
        }
    }
}
