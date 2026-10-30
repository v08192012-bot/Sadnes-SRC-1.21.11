package tech.sadnes.client.feature.impl.player;

import it.unimi.dsi.fastutil.ints.IntPredicate;
import java.util.stream.IntStream;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import tech.sadnes.api.mixin.accessor.ClientPlayerInteractionManagerAccessor;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.ui.notification.NotificationType;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.network.PacketSender;
import tech.sadnes.client.util.rotation.RotationUtil;

@FeatureInfo(
    name = "ClanUpgrade",
    description = "feature.impl.player.clanupgrade",
    displayName = "feature.impl.player.clanupgrade",
    category = Category.MISC,
    subcategory = Subcategory.MISC_SOCIAL
)
public final class ClanUpgrade extends Feature {
    int previousSlot = -1;

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        int i = mc.player.age;
        NotificationType notificationType;
        String string;
        ItemStack itemStack;
        String string2;
        if (ServerManager.getDimension().equals("lobby")) {
            int i2 = mc.player.age;
            int i3 = this.previousSlot;
            int i4 = i2 * -1030792151 + 85899344;
            int i5 = i4 >>> 1;
            int i6 = i4 << 31;
            if (Integer.compareUnsigned(i5 | i6, 85899344) > 0) {
                this.previousSlot = i3;
                return;
            }

            this.previousSlot = i3;
            notificationType = NotificationType.INFO;
            string = this.getDisplayName();
            int i7 = i & 1;
            if ((byte)(i & 0xFF & 1) != 0 ? i7 == 0 : i7 == 0) {
                Item item = Items.TORCH;
                itemStack = new ItemStack(item);
                string2 = Language.translate("clanupgrade.worldissue", "Р’ СЌС‚РѕРј РјРёСЂРµ РЅРµР»СЊР·СЏ РїСЂРѕРєР°С‡РёРІР°С‚СЊ РєР»Р°РЅ!");
            } else {
                Item item2 = Items.REDSTONE;
                itemStack = new ItemStack(item2);
                string2 = Language.translate("clanupgrade.worldissue", "Р’ СЌС‚РѕРј РјРёСЂРµ РЅРµР»СЊР·СЏ РїСЂРѕРєР°С‡РёРІР°С‚СЊ РєР»Р°РЅ!");
            }
        } else {
            IntPredicate intPredicate = i8 -> mc.player.getInventory().getStack(i8).getItem().equals(Items.TORCH)
                || mc.player.getInventory().getStack(i8).getItem().equals(Items.REDSTONE);
            int i9 = this.findHotbarSlot(intPredicate);
            if (i9 != -1) {
                ClientWorld clientWorld = mc.world;
                BlockPos blockPos = mc.player.getBlockPos().down();
                if (clientWorld.getBlockState(blockPos).isSolid()) {
                    Vec2f vec2f = RotationUtil.getRotationsTo(mc.player.getBlockPos().down().toCenterPos());
                    float f = vec2f.x;
                    float f2 = vec2f.y;
                    RotationComponent.rotate(new Rotation(f, f2), 999.0F, 999.0F, 999.0F, 999.0F, 2, 5, false);
                    int i10 = mc.player.getInventory().getSelectedSlot();
                    if (i9 != i10) {
                        int i11 = mc.player.getInventory().getSelectedSlot();
                        this.previousSlot = i11;
                        mc.player.getInventory().setSelectedSlot(i9);
                    }

                    PacketSender.sendSequenced(
                        i12 -> new PlayerInteractBlockC2SPacket(
                            Hand.MAIN_HAND, new BlockHitResult(mc.player.getEntityPos(), Direction.UP, mc.player.getBlockPos().down(), false), i12
                        )
                    );
                    PacketSender.sendSequenced(
                        i13 -> new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, mc.player.getBlockPos().down().up(), Direction.UP, i13)
                    );
                    return;
                }

                return;
            }

            int i14 = mc.player.age;
            int i15 = this.previousSlot;
            int i16 = i14 * -1030792151 + 85899344;
            int i17 = i16 >>> 1;
            int i18 = i16 << 31;
            if (Integer.compareUnsigned(i17 | i18, 85899344) > 0) {
                this.previousSlot = i15;
                return;
            }

            this.previousSlot = i15;
            notificationType = NotificationType.INFO;
            string = this.getDisplayName();
            int i19 = i & 1;
            if ((byte)(i & 0xFF & 1) != 0 ? i19 == 0 : i19 == 0) {
                Item item3 = Items.TORCH;
                itemStack = new ItemStack(item3);
                string2 = Language.translate("clanupgrade.noitem", "РџРѕР»РѕР¶Рё РІ С…РѕС‚Р±Р°СЂ С„Р°РєРµР» РёР»Рё СЂРµРґСЃС‚РѕСѓРЅРѕРІСѓСЋ РїС‹Р»СЊ!");
            } else {
                Item item4 = Items.REDSTONE;
                itemStack = new ItemStack(item4);
                string2 = Language.translate("clanupgrade.noitem", "РџРѕР»РѕР¶Рё РІ С…РѕС‚Р±Р°СЂ С„Р°РєРµР» РёР»Рё СЂРµРґСЃС‚РѕСѓРЅРѕРІСѓСЋ РїС‹Р»СЊ!");
            }
        }

        NotificationUtil.notify(notificationType, string, itemStack, string2);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        IntPredicate intPredicate = i -> mc.player.getInventory().getStack(i).getItem().equals(Items.TORCH)
            || mc.player.getInventory().getStack(i).getItem().equals(Items.REDSTONE);
        int i2 = this.findHotbarSlot(intPredicate);
        if (i2 != -1) {
            int i3 = mc.player.getInventory().getSelectedSlot();
            if (i2 != i3) {
                int i4 = mc.player.getInventory().getSelectedSlot();
                this.previousSlot = i4;
                mc.player.getInventory().setSelectedSlot(i2);
            }
        } else {
            NotificationType notificationType = NotificationType.INFO;
            String string = this.getDisplayName();
            ItemStack itemStack;
            if ((byte)(mc.player.age & 0xFF & 1) != 0) {
                Item item = Items.REDSTONE;
                itemStack = new ItemStack(item);
            } else {
                Item item2 = Items.TORCH;
                itemStack = new ItemStack(item2);
            }

            String string2 = Language.translate("clanupgrade.noitem", "РџРѕР»РѕР¶Рё РІ С…РѕС‚Р±Р°СЂ С„Р°РєРµР» РёР»Рё СЂРµРґСЃС‚РѕСѓРЅРѕРІСѓСЋ РїС‹Р»СЊ!");
            NotificationUtil.notify(notificationType, string, itemStack, string2);
            this.setEnabled(false);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (this.previousSlot != -1) {
            PlayerInventory playerInventory = mc.player.getInventory();
            int i = this.previousSlot;
            playerInventory.setSelectedSlot(i);
            ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).sadnes$invokeSyncSelectedSlot();
        }

        this.previousSlot = -1;
    }

    public int findHotbarSlot(IntPredicate intPredicate) {
        return IntStream.range(0, 9).filter(intPredicate).findFirst().orElse(-1);
    }
}
