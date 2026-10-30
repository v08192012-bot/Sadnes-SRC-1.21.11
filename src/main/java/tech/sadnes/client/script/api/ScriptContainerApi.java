package tech.sadnes.client.script.api;

import java.util.ArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.ScriptableObject;

public final class ScriptContainerApi {
    private ScriptContainerApi() {
    }

    public static void install(ScriptableObject scriptableObject) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject, "open", objects -> {
            ClientPlayerEntity clientPlayerEntity = MinecraftClient.getInstance().player;
            return clientPlayerEntity != null && clientPlayerEntity.currentScreenHandler != clientPlayerEntity.playerScreenHandler ? true : false;
        });
        ScriptValues.putFunction(nativeObject, "title", objects2 -> {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            return minecraftClient.currentScreen == null ? "" : minecraftClient.currentScreen.getTitle().getString();
        });
        ScriptValues.putFunction(nativeObject, "syncId", objects3 -> {
            ClientPlayerEntity clientPlayerEntity2 = MinecraftClient.getInstance().player;
            return clientPlayerEntity2 == null ? -1 : clientPlayerEntity2.currentScreenHandler.syncId;
        });
        ScriptValues.putFunction(nativeObject, "size", objects4 -> {
            ClientPlayerEntity clientPlayerEntity3 = MinecraftClient.getInstance().player;
            if (clientPlayerEntity3 == null) {
                return 0;
            }

            int i = 0;

            for (Slot slot : clientPlayerEntity3.currentScreenHandler.slots) {
                if (!(slot.inventory instanceof PlayerInventory)) {
                    i++;
                }
            }

            return i;
        });
        ScriptValues.putFunction(nativeObject, "slots", objects5 -> {
            ArrayList arrayList = new ArrayList();
            ClientPlayerEntity clientPlayerEntity4 = MinecraftClient.getInstance().player;
            if (clientPlayerEntity4 == null) {
                return ScriptValues.toArray(arrayList);
            }

            for (Slot slot2 : clientPlayerEntity4.currentScreenHandler.slots) {
                NativeObject nativeObject2 = ScriptConvertUtil.wrapStack(slot2.getStack());
                ScriptValues.putValue(nativeObject2, "slot", slot2.id);
                ScriptValues.putValue(nativeObject2, "playerInventory", slot2.inventory instanceof PlayerInventory);
                arrayList.add(nativeObject2);
            }

            return ScriptValues.toArray(arrayList);
        });
        ScriptValues.putFunction(
            nativeObject,
            "slot",
            objects6 -> {
                ClientPlayerEntity clientPlayerEntity5 = MinecraftClient.getInstance().player;
                int i2 = ScriptValues.argInt(objects6, 0, -1);
                return clientPlayerEntity5 != null && i2 >= 0 && i2 < clientPlayerEntity5.currentScreenHandler.slots.size()
                    ? ScriptConvertUtil.wrapStack(clientPlayerEntity5.currentScreenHandler.getSlot(i2).getStack())
                    : null;
            }
        );
        scriptableObject.put("container", scriptableObject, nativeObject);
    }
}
