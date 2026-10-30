package tech.sadnes.client.script.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.state.property.Property;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.libs.rhino.NativeObject;

public final class ScriptConvertUtil {
    private static final Object STACK_KEY = new Object();

    private ScriptConvertUtil() {
    }

    public static NativeObject vec(double d, double d2, double d3) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "x", d);
        ScriptValues.putValue(nativeObject, "y", d2);
        ScriptValues.putValue(nativeObject, "z", d3);
        return nativeObject;
    }

    public static NativeObject wrapVec(Vec3d vec3d) {
        return vec3d == null ? null : vec(vec3d.x, vec3d.y, vec3d.z);
    }

    public static NativeObject wrapBox(Box box) {
        if (box == null) {
            return null;
        }

        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "x1", box.minX);
        ScriptValues.putValue(nativeObject, "y1", box.minY);
        ScriptValues.putValue(nativeObject, "z1", box.minZ);
        ScriptValues.putValue(nativeObject, "x2", box.maxX);
        ScriptValues.putValue(nativeObject, "y2", box.maxY);
        ScriptValues.putValue(nativeObject, "z2", box.maxZ);
        ScriptValues.putValue(nativeObject, "width", box.getLengthX());
        ScriptValues.putValue(nativeObject, "height", box.getLengthY());
        ScriptValues.putValue(nativeObject, "depth", box.getLengthZ());
        return nativeObject;
    }

    public static NativeObject wrapStack(ItemStack itemStack) {
        NativeObject nativeObject = ScriptValues.newObject();
        boolean flag;
        if (itemStack != null && !itemStack.isEmpty()) {
            flag = false;
            nativeObject.associateValue(STACK_KEY, itemStack);
        } else {
            flag = true;
        }

        ScriptValues.putValue(nativeObject, "empty", flag);
        if (flag) {
            ScriptValues.putValue(nativeObject, "id", "minecraft:air");
            ScriptValues.putValue(nativeObject, "name", "");
            ScriptValues.putValue(nativeObject, "count", 0);
            return nativeObject;
        }

        ScriptValues.putValue(nativeObject, "id", Registries.ITEM.getId(itemStack.getItem()).toString());
        ScriptValues.putValue(nativeObject, "name", itemStack.getName().getString());
        ScriptValues.putValue(nativeObject, "count", itemStack.getCount());
        ScriptValues.putValue(nativeObject, "maxCount", itemStack.getMaxCount());
        ScriptValues.putValue(nativeObject, "damage", itemStack.getDamage());
        ScriptValues.putValue(nativeObject, "maxDamage", itemStack.getMaxDamage());
        ScriptValues.putValue(nativeObject, "durability", itemStack.getMaxDamage() <= 0 ? 1.0F : 1.0F - (float)itemStack.getDamage() / itemStack.getMaxDamage());
        ScriptValues.putValue(nativeObject, "damageable", itemStack.isDamageable());
        ScriptValues.putValue(nativeObject, "stackable", itemStack.isStackable());
        FoodComponent foodComponent = itemStack.get(DataComponentTypes.FOOD);
        ScriptValues.putValue(nativeObject, "food", foodComponent != null);
        if (foodComponent != null) {
            ScriptValues.putValue(nativeObject, "nutrition", foodComponent.nutrition());
            ScriptValues.putValue(nativeObject, "saturation", foodComponent.saturation());
        }

        PotionContentsComponent potionContentsComponent = itemStack.get(DataComponentTypes.POTION_CONTENTS);
        if (potionContentsComponent != null) {
            ArrayList arrayList = new ArrayList();

            for (StatusEffectInstance statusEffectInstance : potionContentsComponent.getEffects()) {
                arrayList.add(wrapEffect(statusEffectInstance));
            }

            ScriptValues.putValue(nativeObject, "effects", ScriptValues.toArray(arrayList));
        }

        ScriptValues.putValue(nativeObject, "block", itemStack.getItem() instanceof BlockItem);
        ScriptValues.putValue(nativeObject, "customName", itemStack.contains(DataComponentTypes.CUSTOM_NAME));
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        ScriptValues.putValue(nativeObject, "onCooldown", minecraftClient.player != null && minecraftClient.player.getItemCooldownManager().isCoolingDown(itemStack));
        ArrayList arrayList2 = new ArrayList();
        collectEnchants(itemStack, DataComponentTypes.ENCHANTMENTS, arrayList2);
        collectEnchants(itemStack, DataComponentTypes.STORED_ENCHANTMENTS, arrayList2);
        ScriptValues.putValue(nativeObject, "enchants", ScriptValues.toArray(arrayList2));
        ScriptValues.putFunction(nativeObject, "enchant", objects -> getEnchantLevel(itemStack, ScriptValues.argString(objects, 0)));
        ScriptValues.putFunction(nativeObject, "hasEnchant", objects2 -> getEnchantLevel(itemStack, ScriptValues.argString(objects2, 0)) > 0 ? true : false);
        ScriptValues.putFunction(nativeObject, "is", objects3 -> Registries.ITEM.getId(itemStack.getItem()).toString().equals(ScriptValues.argString(objects3, 0)));
        ArrayList arrayList3 = new ArrayList();
        LoreComponent loreComponent = itemStack.get(DataComponentTypes.LORE);
        if (loreComponent != null) {
            loreComponent.lines().forEach(text2 -> arrayList3.add(text2.getString()));
        }

        ScriptValues.putValue(nativeObject, "lore", ScriptValues.toArray(arrayList3));
        return nativeObject;
    }

    public static ItemStack unwrapStack(Object object) {
        Object object2 = object instanceof NativeObject ? ((NativeObject)object).getAssociatedValue(STACK_KEY) : null;
        return object2 instanceof ItemStack ? (ItemStack)object2 : null;
    }

    private static void collectEnchants(ItemStack itemStack, ComponentType<ItemEnchantmentsComponent> componentType, List<Object> objects) {
        ItemEnchantmentsComponent itemEnchantmentsComponent = itemStack.getOrDefault(componentType, ItemEnchantmentsComponent.DEFAULT);

        for (RegistryEntry<net.minecraft.enchantment.Enchantment> registryEntry : itemEnchantmentsComponent.getEnchantments()) {
            NativeObject nativeObject = ScriptValues.newObject();
            ScriptValues.putValue(nativeObject, "id", registryEntry.getKey().map(registryKey -> registryKey.getValue().toString()).orElse("unknown"));
            ScriptValues.putValue(nativeObject, "level", itemEnchantmentsComponent.getLevel(registryEntry));
            objects.add(nativeObject);
        }
    }

    private static int getEnchantLevel(ItemStack itemStack, String string) {
        int i = findEnchantLevel(itemStack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT), string);
        return i > 0 ? i : findEnchantLevel(itemStack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT), string);
    }

    private static int findEnchantLevel(ItemEnchantmentsComponent itemEnchantmentsComponent, String string) {
        for (RegistryEntry<net.minecraft.enchantment.Enchantment> registryEntry : itemEnchantmentsComponent.getEnchantments()) {
            String string2 = registryEntry.getKey().map(registryKey -> registryKey.getValue().toString()).orElse("");
            if (string2.equals(string) || string2.endsWith(":" + string)) {
                return itemEnchantmentsComponent.getLevel(registryEntry);
            }
        }

        return 0;
    }

    public static NativeObject wrapBlock(BlockPos blockPos, BlockState blockState) {
        NativeObject nativeObject = ScriptValues.newObject();
        if (blockPos != null) {
            ScriptValues.putValue(nativeObject, "x", blockPos.getX());
            ScriptValues.putValue(nativeObject, "y", blockPos.getY());
            ScriptValues.putValue(nativeObject, "z", blockPos.getZ());
        }

        if (blockState == null) {
            ScriptValues.putValue(nativeObject, "id", "minecraft:air");
            ScriptValues.putValue(nativeObject, "air", true);
            return nativeObject;
        }

        ScriptValues.putValue(nativeObject, "id", Registries.BLOCK.getId(blockState.getBlock()).toString());
        ScriptValues.putValue(nativeObject, "air", blockState.isAir());
        ScriptValues.putValue(nativeObject, "liquid", !blockState.getFluidState().isEmpty());
        ScriptValues.putValue(nativeObject, "solid", blockState.isSolidBlock(MinecraftClient.getInstance().world, blockPos == null ? BlockPos.ORIGIN : blockPos));
        ScriptValues.putValue(nativeObject, "replaceable", blockState.isReplaceable());
        ScriptValues.putValue(nativeObject, "luminance", blockState.getLuminance());
        ClientWorld clientWorld = MinecraftClient.getInstance().world;
        if (clientWorld != null && blockPos != null) {
            VoxelShape voxelShape = blockState.getOutlineShape(clientWorld, blockPos);
            ScriptValues.putValue(nativeObject, "box", voxelShape.isEmpty() ? null : wrapBox(voxelShape.getBoundingBox().offset(blockPos)));
            ScriptValues.putValue(nativeObject, "full", blockState.isFullCube(clientWorld, blockPos));
        }

        ScriptValues.putFunction(nativeObject, "is", objects -> Registries.BLOCK.getId(blockState.getBlock()).toString().equals(ScriptValues.argString(objects, 0)));
        NativeObject nativeObject2 = ScriptValues.newObject();
        blockState.getProperties()
            .forEach(property2 -> ScriptValues.putValue(nativeObject2, property2.getName(), String.valueOf(blockState.get((Property<?>)property2)).toLowerCase(Locale.ROOT)));
        ScriptValues.putValue(nativeObject, "properties", nativeObject2);
        return nativeObject;
    }

    public static NativeObject wrapEffect(StatusEffectInstance statusEffectInstance) {
        if (statusEffectInstance == null) {
            return null;
        }

        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "id", Registries.STATUS_EFFECT.getId(statusEffectInstance.getEffectType().value()).toString());
        ScriptValues.putValue(nativeObject, "level", statusEffectInstance.getAmplifier() + 1);
        ScriptValues.putValue(nativeObject, "duration", statusEffectInstance.getDuration());
        ScriptValues.putValue(nativeObject, "ambient", statusEffectInstance.isAmbient());
        return nativeObject;
    }

    public static NativeObject wrapHitResult(HitResult hitResult) {
        if (hitResult == null) {
            return null;
        }

        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "type", hitResult.getType().name().toLowerCase(Locale.ROOT));
        ScriptValues.putValue(nativeObject, "pos", wrapVec(hitResult.getPos()));
        if (hitResult instanceof BlockHitResult blockHitResult) {
            ScriptValues.putValue(nativeObject, "side", blockHitResult.getSide().name().toLowerCase(Locale.ROOT));
            BlockPos blockPos = blockHitResult.getBlockPos();
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (minecraftClient.world == null) {
                ScriptValues.putValue(nativeObject, "block", wrapBlock(blockPos, null));
                return nativeObject;
            } else {
                ScriptValues.putValue(nativeObject, "block", wrapBlock(blockPos, minecraftClient.world.getBlockState(blockPos)));
                return nativeObject;
            }
        } else {
            if (hitResult instanceof EntityHitResult entityHitResult) {
                ScriptValues.putValue(nativeObject, "entity", ScriptPlayerApi.wrapEntity(entityHitResult.getEntity()));
            }

            return nativeObject;
        }
    }

    public static String handName(Hand hand) {
        return hand == Hand.OFF_HAND ? "off" : "main";
    }

    public static Hand parseHand(String string) {
        return "off".equalsIgnoreCase(string) ? Hand.OFF_HAND : Hand.MAIN_HAND;
    }

    public static NativeObject wrapEquipment(LivingEntity livingEntity) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "mainHand", wrapStack(livingEntity.getMainHandStack()));
        ScriptValues.putValue(nativeObject, "offHand", wrapStack(livingEntity.getOffHandStack()));
        ArrayList arrayList = new ArrayList();

        for (EquipmentSlot equipmentSlot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            arrayList.add(wrapStack(livingEntity.getEquippedStack(equipmentSlot)));
        }

        ScriptValues.putValue(nativeObject, "armor", ScriptValues.toArray(arrayList));
        return nativeObject;
    }

    public static Object wrapEffects(LivingEntity livingEntity) {
        ArrayList arrayList = new ArrayList();

        for (StatusEffectInstance statusEffectInstance : livingEntity.getStatusEffects()) {
            arrayList.add(wrapEffect(statusEffectInstance));
        }

        return ScriptValues.toArray(arrayList);
    }

    public static String getScreenName(Screen screen) {
        if (screen == null) {
            return "";
        } else if (screen instanceof InventoryScreen) {
            return "InventoryScreen";
        } else if (screen instanceof CreativeInventoryScreen) {
            return "CreativeInventoryScreen";
        } else if (screen instanceof ChatScreen) {
            return "ChatScreen";
        } else if (screen instanceof GameMenuScreen) {
            return "GameMenuScreen";
        } else if (screen instanceof DeathScreen) {
            return "DeathScreen";
        } else {
            return screen instanceof HandledScreen ? "HandledScreen" : screen.getClass().getSimpleName();
        }
    }

    public static String getEntityTypeId(Entity entity) {
        return Registries.ENTITY_TYPE.getId(entity.getType()).toString();
    }
}
