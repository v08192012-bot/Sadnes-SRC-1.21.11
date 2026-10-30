package tech.sadnes.client.feature;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.function.Consumer;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventBus;
import tech.sadnes.client.feature.component.AutoReconnectComponent;
import tech.sadnes.client.feature.component.FreeLookComponent;
import tech.sadnes.client.feature.component.GuiMoveComponent;
import tech.sadnes.client.feature.component.HudSettingsPopupComponent;
import tech.sadnes.client.feature.component.InventoryLayoutComponent;
import tech.sadnes.client.feature.component.ItemCooldownTrackerComponent;
import tech.sadnes.client.feature.component.KillDeathTrackerComponent;
import tech.sadnes.client.feature.component.MovementPauseComponent;
import tech.sadnes.client.feature.component.NetworkStatsComponent;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.component.TargetComponent;
import tech.sadnes.client.feature.component.ThrowComponent;
import tech.sadnes.client.feature.component.VanishDetectorComponent;
import tech.sadnes.client.feature.component.WindowClickTrackerComponent;
import tech.sadnes.client.feature.impl.combat.aura.AfkModeExitComponent;
import tech.sadnes.client.feature.impl.combat.aura.TargetEspRenderer;
import tech.sadnes.client.server.HealingPotionCooldown;
import tech.sadnes.client.server.autobuy.AutoBuyChecker;
import tech.sadnes.client.server.autobuy.AutoBuyController;
import tech.sadnes.client.server.autobuy.AutoBuyPriceScanner;
import tech.sadnes.client.server.autobuy.ItemPurchaser;
import tech.sadnes.client.server.autobuy.KitCollector;
import tech.sadnes.client.server.autobuy.ShulkerStash;
import tech.sadnes.client.server.donut.CommandTraceComponent;
import tech.sadnes.client.server.donut.DonutMenuCommandFallback;
import tech.sadnes.client.ui.drag.DragComponent;
import tech.sadnes.client.util.rotation.RotationAnimator;

public final class ComponentManager extends LinkedHashMap<Class<? extends FeatureComponent>, FeatureComponent> {
    public void init() {
        FeatureComponent[] featureComponents = new FeatureComponent[27];
        FreeLookComponent freeLookComponent = new FreeLookComponent();
        Array.set(featureComponents, 0, freeLookComponent);
        RotationComponent rotationComponent = new RotationComponent();
        Array.set(featureComponents, 1, rotationComponent);
        ThrowComponent throwComponent = new ThrowComponent();
        Array.set(featureComponents, 2, throwComponent);
        DragComponent dragComponent = new DragComponent();
        Array.set(featureComponents, 3, dragComponent);
        HudSettingsPopupComponent hudSettingsPopupComponent = new HudSettingsPopupComponent();
        Array.set(featureComponents, 4, hudSettingsPopupComponent);
        TargetComponent targetComponent = new TargetComponent();
        Array.set(featureComponents, 5, targetComponent);
        TargetEspRenderer targetEspRenderer = new TargetEspRenderer();
        Array.set(featureComponents, 6, targetEspRenderer);
        GuiMoveComponent guiMoveComponent = new GuiMoveComponent();
        Array.set(featureComponents, 7, guiMoveComponent);
        MovementPauseComponent movementPauseComponent = new MovementPauseComponent();
        Array.set(featureComponents, 8, movementPauseComponent);
        WindowClickTrackerComponent windowClickTrackerComponent = new WindowClickTrackerComponent();
        Array.set(featureComponents, 9, windowClickTrackerComponent);
        InventoryLayoutComponent inventoryLayoutComponent = new InventoryLayoutComponent();
        Array.set(featureComponents, 10, inventoryLayoutComponent);
        NetworkStatsComponent networkStatsComponent = new NetworkStatsComponent();
        Array.set(featureComponents, 11, networkStatsComponent);
        AutoReconnectComponent autoReconnectComponent = new AutoReconnectComponent();
        Array.set(featureComponents, 12, autoReconnectComponent);
        DonutMenuCommandFallback donutMenuCommandFallback = new DonutMenuCommandFallback();
        Array.set(featureComponents, 13, donutMenuCommandFallback);
        CommandTraceComponent commandTraceComponent = new CommandTraceComponent();
        Array.set(featureComponents, 14, commandTraceComponent);
        VanishDetectorComponent vanishDetectorComponent = new VanishDetectorComponent();
        Array.set(featureComponents, 15, vanishDetectorComponent);
        KillDeathTrackerComponent killDeathTrackerComponent = new KillDeathTrackerComponent();
        Array.set(featureComponents, 16, killDeathTrackerComponent);
        AutoBuyController autoBuyController = new AutoBuyController();
        Array.set(featureComponents, 17, autoBuyController);
        AutoBuyPriceScanner autoBuyPriceScanner = new AutoBuyPriceScanner();
        Array.set(featureComponents, 18, autoBuyPriceScanner);
        AutoBuyChecker autoBuyChecker = new AutoBuyChecker();
        Array.set(featureComponents, 19, autoBuyChecker);
        ItemPurchaser itemPurchaser = new ItemPurchaser();
        Array.set(featureComponents, 20, itemPurchaser);
        KitCollector kitCollector = new KitCollector();
        Array.set(featureComponents, 21, kitCollector);
        ShulkerStash shulkerStash = new ShulkerStash();
        Array.set(featureComponents, 22, shulkerStash);
        AfkModeExitComponent afkModeExitComponent = new AfkModeExitComponent();
        Array.set(featureComponents, 23, afkModeExitComponent);
        RotationAnimator rotationAnimator = new RotationAnimator();
        Array.set(featureComponents, 24, rotationAnimator);
        ItemCooldownTrackerComponent itemCooldownTrackerComponent = new ItemCooldownTrackerComponent();
        Array.set(featureComponents, 25, itemCooldownTrackerComponent);
        HealingPotionCooldown healingPotionCooldown = new HealingPotionCooldown();
        Array.set(featureComponents, 26, healingPotionCooldown);
        this.register(featureComponents);
        Collection collection = this.values();
        Consumer consumer = object -> Sadnes.getEventBus().subscribe(object);
        collection.forEach(consumer);
        EventBus eventBus = Sadnes.getEventBus();
        String string = "tech.sadnes.client.util.player.CriticalUtil";

        Class clazz;
        try {
            clazz = Class.forName(string, false, ComponentManager.class.getClassLoader());
        } catch (ClassNotFoundException classNotFoundException) {
            throw new ExceptionInInitializerError(classNotFoundException);
        }

        Class clazz2 = clazz;
        eventBus.subscribeStatic(clazz2);
    }

    public void shutdown() {
        Collection collection = this.values();
        Consumer consumer = object -> Sadnes.getEventBus().unsubscribe(object);
        collection.forEach(consumer);
        EventBus eventBus = Sadnes.getEventBus();
        String string = "tech.sadnes.client.util.player.CriticalUtil";

        Class clazz;
        try {
            clazz = Class.forName(string, false, ComponentManager.class.getClassLoader());
        } catch (ClassNotFoundException classNotFoundException) {
            throw new ExceptionInInitializerError(classNotFoundException);
        }

        Class clazz2 = clazz;
        eventBus.unsubscribeStatic(clazz2);
        this.clear();
    }

    public void register(FeatureComponent... featureComponents) {
        int i = Array.getLength(featureComponents);
        if (0 < i) {
            int i2 = 0;

            while (true) {
                Object object = Array.get(featureComponents, i2);
                Class clazz = object.getClass();
                this.put(clazz, (FeatureComponent)object);
                int i3 = i2 + 1;
                int i4 = i2 & -2 ^ 1195106197;
                int i5 = i4 * i4;
                int i6 = (2 - i5) * i5;
                int i7 = (2 - i6) * i6;
                int i8 = (2 - i7) * i7;
                int i9 = (2 - i8) * i8;
                int i10 = (2 - i9) * i9 * i3;
                if (i10 >= i) {
                    return;
                }

                i2 = i10;
            }
        }
    }

    public void unregister(FeatureComponent... featureComponents) {
        int i = Array.getLength(featureComponents);
        if (0 < i) {
            int i2 = 0;

            while (true) {
                Object object = Array.get(featureComponents, i2);
                Sadnes.getEventBus().unsubscribe(object);
                Class clazz = object.getClass();
                this.remove(clazz);
                int i3 = i2 + 1;
                if (i3 >= i) {
                    return;
                }

                i2 = i3;
            }
        }
    }

    public <T extends FeatureComponent> T getComponent(Class<T> clazz) {
        return (T)super.get(clazz);
    }
}
