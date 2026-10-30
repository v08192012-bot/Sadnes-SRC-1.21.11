package tech.sadnes.client.server.donut;

import java.util.stream.IntStream;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import tech.sadnes.client.server.TradeBook;
import tech.sadnes.client.util.IMinecraft;

public final class OrderDeliveryHelper implements IMinecraft {
    private final GenericContainerScreenHandler handler;
    private final Item item;
    private final OrderDeliveryHelper.SlotClicker clicker;
    private final long[] previousPrices = new long[9];

    public static boolean isContainerEmpty(GenericContainerScreenHandler genericContainerScreenHandler) {
        return IntStream.range(0, (genericContainerScreenHandler.getRows() - 1) * 9).allMatch(i -> genericContainerScreenHandler.getSlot(i).getStack().isEmpty());
    }

    public static boolean isOrderScreen(GenericContainerScreenHandler genericContainerScreenHandler) {
        int i = (genericContainerScreenHandler.getRows() - 1) * 9;
        boolean flag = IntStream.range(i, i + 9).anyMatch(i2 -> !genericContainerScreenHandler.getSlot(i2).getStack().isEmpty());
        return i >= 9 && isContainerEmpty(genericContainerScreenHandler) && flag;
    }

    public void deliver(int i) {
        int i2 = (this.handler.getRows() - 1) * 9;

        for (int i3 = 0; i3 < 9; i3++) {
            this.previousPrices[i3] = DonutPriceUtil.getItemPrice(this.handler.getSlot(i2 + i3).getStack());
        }

        int i4 = i;

        while (i4 > 0) {
            Slot slot = null;

            for (Slot slot2 : this.handler.slots) {
                boolean flag = slot2.inventory == mc.player.getInventory()
                    && TradeBook.isPlainItem(slot2.getStack(), this.item)
                    && slot2.getStack().getCount() <= i4;
                if (flag && (slot == null || slot2.getStack().getCount() > slot.getStack().getCount())) {
                    slot = slot2;
                }
            }

            if (slot == null) {
                break;
            }

            i4 -= slot.getStack().getCount();
            this.clicker.click(slot.id, 0, SlotActionType.QUICK_MOVE);
        }

        if (i4 > 0) {
            this.deliverSplit(i4);
        }
    }

    private void deliverSplit(int i) {
        Slot slot = null;

        for (Slot slot2 : this.handler.slots) {
            boolean flag = slot2.inventory == mc.player.getInventory() && TradeBook.isPlainItem(slot2.getStack(), this.item) && slot2.getStack().getCount() > i;
            if (flag && (slot == null || slot2.getStack().getCount() < slot.getStack().getCount())) {
                slot = slot2;
            }
        }

        int i2 = (this.handler.getRows() - 1) * 9;
        int i3 = IntStream.range(0, i2).filter(i4 -> this.handler.getSlot(i4).getStack().isEmpty()).findFirst().orElse(-1);
        if (slot != null && i3 != -1) {
            int i5 = slot.getStack().getCount();
            boolean flag2 = i == (i5 + 1) / 2;
            this.clicker.click(slot.id, flag2 ? 1 : 0, SlotActionType.PICKUP);
            if (!flag2 && i <= i5 - i) {
                for (int i6 = 0; i6 < i; i6++) {
                    this.clicker.click(i3, 1, SlotActionType.PICKUP);
                }

                this.clicker.click(slot.id, 0, SlotActionType.PICKUP);
            } else {
                if (!flag2) {
                    int i7 = 0;

                    for (int i8 = i5 - i; i7 < i8; i7++) {
                        this.clicker.click(slot.id, 1, SlotActionType.PICKUP);
                    }
                }

                this.clicker.click(i3, 0, SlotActionType.PICKUP);
            }
        }
    }

    public int countDelivered() {
        int i = (this.handler.getRows() - 1) * 9;
        int i2 = 0;

        for (int i3 = 0; i3 < i; i3++) {
            ItemStack itemStack = this.handler.getSlot(i3).getStack();
            if (itemStack.isOf(this.item)) {
                i2 += itemStack.getCount();
            }
        }

        return i2;
    }

    public long findChangedPrice() {
        int i = (this.handler.getRows() - 1) * 9;
        long l = -1L;

        for (int i2 = 0; i2 < 9; i2++) {
            long l2 = DonutPriceUtil.getItemPrice(this.handler.getSlot(i + i2).getStack());
            boolean flag = l2 > 0L && l2 != this.previousPrices[i2];
            if (flag && (l < 0L || l2 < l)) {
                l = l2;
            }
        }

        return l;
    }

    public void takeBack(int i) {
        ItemStack itemStack = this.handler.getCursorStack();

        for (Slot slot : this.handler.slots) {
            if (itemStack.isEmpty()) {
                break;
            }

            ItemStack itemStack2 = slot.getStack();
            boolean flag = itemStack2.isEmpty() || ItemStack.areItemsAndComponentsEqual(itemStack2, itemStack) && itemStack2.getCount() < itemStack2.getMaxCount();
            if (slot.inventory == mc.player.getInventory() && flag) {
                this.clicker.click(slot.id, 0, SlotActionType.PICKUP);
                itemStack = this.handler.getCursorStack();
            }
        }

        int i2 = (this.handler.getRows() - 1) * 9;

        for (int i3 = 0; i3 < i2; i3++) {
            boolean flag2 = this.handler.getSlot(i3).getStack().isOf(this.item);
            if (flag2 || i > 0 && i3 < 9) {
                this.clicker.click(i3, 0, SlotActionType.QUICK_MOVE);
            }
        }
    }

    public OrderDeliveryHelper(GenericContainerScreenHandler genericContainerScreenHandler, Item item2, OrderDeliveryHelper.SlotClicker slotClicker) {
        this.handler = genericContainerScreenHandler;
        this.item = item2;
        this.clicker = slotClicker;
    }

    @FunctionalInterface
    public interface SlotClicker {
        void click(int i, int i2, SlotActionType slotActionType);
    }
}
