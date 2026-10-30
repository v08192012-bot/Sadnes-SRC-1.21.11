package tech.sadnes.client.command.impl;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.math.BlockPos;
import tech.sadnes.client.command.Command;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.player.ClipUtil;

public final class TeleportCommand extends Command {
    public TeleportCommand() {
        super("tp");
    }

    @Override
    public void execute(String[] strings) {
        if (mc.player != null && mc.world != null) {
            if (strings.length >= 3) {
                double d = ClipUtil.parseDouble(strings[0]);
                double d2 = ClipUtil.parseDouble(strings[1]);
                double d3 = ClipUtil.parseDouble(strings[2]);
                ClipUtil.teleport(d, d2, d3);
                ChatUtil.print(String.format("Телепортация на координаты: %.1f, %.1f, %.1f", d, d2, d3));
            } else if (strings.length == 1) {
                PlayerEntity playerEntity = this.findPlayer(strings[0]);
                if (playerEntity == null) {
                    ChatUtil.printError("Игрок " + strings[0] + " не найден.");
                } else {
                    double d4 = this.findGroundY(playerEntity);
                    if (d4 == Double.MIN_VALUE) {
                        ChatUtil.printError("Не удалось найти твердый блок рядом с игроком " + playerEntity.getName().getString() + ".");
                    } else {
                        this.teleportWithPackets(playerEntity.getX(), d4, playerEntity.getZ(), mc.player.distanceTo(playerEntity));
                        ChatUtil.print("Телепортация к игроку " + playerEntity.getName().getString() + " выполнена.");
                        ChatUtil.print(String.format("Координаты: %.1f %.1f %.1f", playerEntity.getX(), d4, playerEntity.getZ()));
                    }
                }
            } else {
                ChatUtil.print("Используй: .tp NICK или .tp X Y Z");
            }
        }
    }

    private PlayerEntity findPlayer(String string) {
        for (PlayerEntity playerEntity : mc.world.getPlayers()) {
            if (playerEntity.getName().getString().equalsIgnoreCase(string)) {
                return playerEntity;
            }
        }

        return null;
    }

    private void teleportWithPackets(double d, double d2, double d3, double d4) {
        if (mc.player != null && mc.player.networkHandler != null) {
            int i = Math.max((int)(d4 / 1000.0), 3);
            boolean flag = mc.player.horizontalCollision;

            for (int i2 = 0; i2 < i; i2++) {
                mc.player.networkHandler.sendPacket(new OnGroundOnly(mc.player.isOnGround(), flag));
            }

            mc.player.networkHandler.sendPacket(new PositionAndOnGround(d, d2, d3, false, flag));
            mc.player.setPosition(d, d2, d3);
        }
    }

    private double findGroundY(PlayerEntity playerEntity) {
        if (mc.world == null) {
            return Double.MIN_VALUE;
        }

        BlockPos blockPos = playerEntity.getBlockPos();
        int i = mc.world.getBottomY();
        int i2 = mc.world.getTopYInclusive();

        for (int i3 = blockPos.getY() - 1; i3 >= i; i3--) {
            BlockPos blockPos2 = new BlockPos(blockPos.getX(), i3, blockPos.getZ());
            if (this.isSolid(blockPos2)) {
                return i3 + 0.25;
            }
        }

        for (int i4 = blockPos.getY() + 1; i4 <= i2; i4++) {
            BlockPos blockPos3 = new BlockPos(blockPos.getX(), i4, blockPos.getZ());
            if (this.isSolid(blockPos3)) {
                return i4 + 0.25;
            }
        }

        return Double.MIN_VALUE;
    }

    private boolean isSolid(BlockPos blockPos) {
        return mc.world == null ? false : !mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty();
    }

    @Override
    public List<String> complete(String[] strings) {
        return strings.length == 1 ? CommandManager.filterByPrefix(this.getTabPlayerNames(), strings[0]) : List.of();
    }

    private List<String> getTabPlayerNames() {
        ArrayList arrayList = new ArrayList();
        if (mc.getNetworkHandler() == null) {
            return arrayList;
        }

        for (PlayerListEntry playerListEntry : mc.getNetworkHandler().getListedPlayerListEntries()) {
            arrayList.add(playerListEntry.getProfile().name());
        }

        return arrayList;
    }
}
