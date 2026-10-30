package tech.sadnes.client.feature.impl.player;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.EntityRaycastEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.impl.render.AuctionHelper;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.math.TimerUtil;

@FeatureInfo(
    name = "ContainerStealer",
    description = "feature.impl.misc.containerstealer",
    displayName = "feature.impl.misc.containerstealer",
    category = Category.PLAYER,
    subcategory = Subcategory.PLAYER_INVENTORY
)
public final class ContainerStealer extends Feature {
    TimerUtil stopWatch = new TimerUtil();
    Random random = new Random();
    private final ModeSetting technology = new ModeSetting("containerstealer.technology", new String[]{"from_start", "from_end", "random"});
    private final ModeSetting stealMode = new ModeSetting("containerstealer.stealMode", new String[]{"normal", "FunTime"});
    private final NumberSetting delay = new NumberSetting("containerstealer.delay", 67.0F, 0.0F, 200.0F, 1.0F)
        .setVisibleWhen(() -> !this.stealMode.is("FunTime") ? true : false);
    private final BooleanSetting closeIfEmpty = new BooleanSetting("containerstealer.closeIfEmpty", true);
    private final BooleanSetting ignoreEntity = new BooleanSetting("containerstealer.ignoreEntity", false);

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.currentScreen instanceof GenericContainerScreen genericContainerScreen) {
            TextContent textContent = genericContainerScreen.getTitle().getContent();
            boolean flag;
            if (textContent instanceof TranslatableTextContent) {
                flag = ((TranslatableTextContent)textContent).getKey().startsWith("container.");
            } else {
                flag = false;
            }

            if (flag) {
                if (AuctionHelper.isAuctionScreen(genericContainerScreen)) {
                    return;
                }

                GenericContainerScreenHandler genericContainerScreenHandler = genericContainerScreen.getScreenHandler();
                List list = IntStream.range(0, genericContainerScreenHandler.slots.size()).filter(i -> {
                    Slot slot = genericContainerScreenHandler.slots.get(i);
                    return slot != null && slot.hasStack() && slot.inventory != mc.player.getInventory();
                }).boxed().collect(Collectors.toList());
                if (!list.isEmpty()) {
                    label39: {
                        boolean flag2;
                        if (this.stealMode.is("FunTime")) {
                            if (!mc.player.getItemCooldownManager().isCoolingDown(Items.GUNPOWDER.getDefaultStack())) {
                                break label39;
                            }

                            flag2 = false;
                        } else {
                            flag2 = this.stopWatch.hasPassed(this.delay.getValue().longValue());
                        }

                        boolean flag3 = flag2;
                        if (!flag3) {
                            return;
                        }
                    }

                    int i2 = this.technology.is("random")
                        ? (Integer)list.get(this.random.nextInt(list.size()))
                        : (this.technology.is("from_end") ? (Integer)list.get(list.size() - 1) : (Integer)list.get(0));
                    mc.interactionManager.clickSlot(genericContainerScreenHandler.syncId, i2, 0, SlotActionType.QUICK_MOVE, mc.player);
                    this.stopWatch.reset();
                    return;
                }

                if (this.closeIfEmpty.getValue()) {
                    mc.player.closeHandledScreen();
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onEntityInteract(EntityRaycastEvent entityRaycastEvent) {
        if (entityRaycastEvent.getEntity() instanceof LivingEntity) {
            if (this.ignoreEntity.getValue() || mc.options.useKey.isPressed()) {
                entityRaycastEvent.cancel();
            }
        }
    }
}
