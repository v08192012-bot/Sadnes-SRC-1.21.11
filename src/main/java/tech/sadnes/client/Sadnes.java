package tech.sadnes.client;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.account.AccountManager;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.config.ConfigManager;
import tech.sadnes.client.config.LocalConfigStorage;
import tech.sadnes.client.config.ThemePrefs;
import tech.sadnes.client.cosmetic.CosmeticManager;
import tech.sadnes.client.event.EventBus;
import tech.sadnes.client.event.LambdaListener;
import tech.sadnes.client.feature.ComponentManager;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.friend.FriendManager;
import tech.sadnes.client.friend.PartyFriendSync;
import tech.sadnes.client.handler.ServerPacketFilter;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.macro.MacroManager;
import tech.sadnes.client.network.BackendService;
import tech.sadnes.client.network.SadnesSocketClient;
import tech.sadnes.client.render.BezierCurveRenderer;
import tech.sadnes.client.render.GooRenderer;
import tech.sadnes.client.render.LogoGlowRenderer;
import tech.sadnes.client.render.MotionBlurRenderer;
import tech.sadnes.client.render.RenderHooks;
import tech.sadnes.client.render.RopeRenderer;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.shader.AmbiencePostUniformStorage;
import tech.sadnes.client.render.shader.AmbienceRenderer;
import tech.sadnes.client.render.shader.BlurRectUniformStorage;
import tech.sadnes.client.render.shader.ChamsUniformStorage;
import tech.sadnes.client.render.shader.DischargeRenderer;
import tech.sadnes.client.render.shader.FlameUniformStorage;
import tech.sadnes.client.render.shader.FogUniformStorage;
import tech.sadnes.client.render.shader.FrameBufferPool;
import tech.sadnes.client.render.shader.GridBgUniformStorage;
import tech.sadnes.client.render.shader.MotionBlurUniformStorage;
import tech.sadnes.client.render.shader.Pipelines;
import tech.sadnes.client.render.shader.RoundedUniformStorage;
import tech.sadnes.client.render.shader.ScriptFxUniformStorage;
import tech.sadnes.client.render.shader.ShaderSkyRenderer;
import tech.sadnes.client.render.shader.ShapeBlur;
import tech.sadnes.client.render.shader.SkyUniformStorage;
import tech.sadnes.client.render.shader.WeatherUniformStorage;
import tech.sadnes.client.render.texture.AnimatedIconManager;
import tech.sadnes.client.render.texture.ChamsNoiseTexture;
import tech.sadnes.client.render.texture.LogoTexture;
import tech.sadnes.client.render.texture.MenuBackgroundTexture;
import tech.sadnes.client.render.texture.PlainLogoTexture;
import tech.sadnes.client.render.texture.SvgIconCache;
import tech.sadnes.client.render.world.FoliageSwayManager;
import tech.sadnes.client.render.world.OptifineSkyManager;
import tech.sadnes.client.script.ScriptManager;
import tech.sadnes.client.server.AutoBuyManager;
import tech.sadnes.client.server.autobuy.CollectorManager;
import tech.sadnes.client.staff.StaffManager;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.hud.GpsIndicator;
import tech.sadnes.client.ui.hud.WaypointRenderer;
import tech.sadnes.client.ui.menu.MainMenuScreen;
import tech.sadnes.client.util.SoundPlayer;

public final class Sadnes {
    private static final Logger log = LogManager.getLogger(Sadnes.class);
    public static final String AUTO_CONFIG_NAME = "autoCfg";
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ClientInfo.MOD_ID);
    private static final Sadnes INSTANCE = new Sadnes();
    private static final EventBus EVENT_BUS = EventBus.createConcurrent();
    private FeatureManager featureManager;
    private CommandManager commandManager;
    private ComponentManager componentManager;
    private ScriptManager scriptManager;
    private ConfigManager configManager;
    private FriendManager friendManager;
    private MacroManager macroManager;
    private AutoBuyManager autoBuyManager;
    private CollectorManager collectorManager;
    private PartyFriendSync configSync;
    private StaffManager staffManager;
    private AccountManager accountManager;
    private BackendService backend;
    private CosmeticManager cosmeticsManager;
    private ClickGuiScreen clickGui;
    private MainMenuScreen mainMenu;
    private boolean initialized;
    private boolean shuttingDown;
    public static float guiScale = 1.0F;
    public static boolean panic = false;

    public void setupEventBus() {
        EventBus eventBus = EVENT_BUS;
        LambdaListener.LambdaFactory lambdaFactory = (method, clazz) -> (Lookup)method.invoke(null, clazz, MethodHandles.lookup());
        eventBus.setLambdaFactory(lambdaFactory);
    }

    public void initialize() {
        boolean flag = this.initialized;
        if (!flag) {
            this.setupEventBus();
            RenderHooks.init();
            Pipelines.init();
            this.initManagers();
            this.initScreens();
            this.registerShutdownHook();
            this.registerReloadListener();
            this.initialized = true;
            Feature.setInitialized(true);
            LOGGER.info("{} initialized", "Sadnes");
        }
    }

    private void registerShutdownHook() {
        Event event = ClientLifecycleEvents.CLIENT_STOPPING;
        ClientStopping clientStopping = minecraftClient -> this.shutdown();
        event.register(clientStopping);
    }

    private void registerReloadListener() {
        ResourceManagerHelper resourceManagerHelper = ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES);
        SimpleSynchronousResourceReloadListener simpleSynchronousResourceReloadListener = new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Identifier.of(ClientInfo.MOD_ID, "graphics_reloader");
            }

            @Override
            public void reload(ResourceManager resourceManager) {
                Sadnes.this.reloadGraphics();
                OptifineSkyManager.reload();
            }
        };
        resourceManagerHelper.registerReloadListener(simpleSynchronousResourceReloadListener);
    }

    public void initManagers() {
        FriendManager friendManager2 = new FriendManager();
        this.friendManager = friendManager2;
        friendManager2.load();
        MacroManager macroManager2 = new MacroManager();
        this.macroManager = macroManager2;
        macroManager2.load();
        MacroManager macroManager3 = this.macroManager;
        macroManager3.register();
        AutoBuyManager autoBuyManager2 = new AutoBuyManager();
        this.autoBuyManager = autoBuyManager2;
        autoBuyManager2.load();
        AutoBuyManager autoBuyManager3 = this.autoBuyManager;
        CollectorManager collectorManager2 = new CollectorManager(autoBuyManager3);
        this.collectorManager = collectorManager2;
        collectorManager2.load();
        StaffManager staffManager2 = new StaffManager();
        this.staffManager = staffManager2;
        staffManager2.load();
        AccountManager accountManager2 = new AccountManager();
        this.accountManager = accountManager2;
        accountManager2.load();
        BackendService backendService = new BackendService();
        this.backend = backendService;
        backendService.start();
        FriendManager friendManager3 = this.friendManager;
        BackendService backendService2 = this.backend;
        PartyFriendSync partyFriendSync = new PartyFriendSync(backendService2, friendManager3);
        this.configSync = partyFriendSync;
        partyFriendSync.register();
        AutoBuyManager autoBuyManager4 = this.autoBuyManager;
        BackendService backendService3 = this.backend;
        SadnesSocketClient sadnesSocketClient = backendService3.getSocketClient();
        autoBuyManager4.initCloudSync(sadnesSocketClient);
        CollectorManager collectorManager3 = this.collectorManager;
        BackendService backendService4 = this.backend;
        SadnesSocketClient sadnesSocketClient2 = backendService4.getSocketClient();
        collectorManager3.initCloudConfig(sadnesSocketClient2);
        LocalConfigStorage localConfigStorage = LocalConfigStorage.createDefault();
        BackendService backendService5 = this.backend;
        SadnesSocketClient sadnesSocketClient3 = backendService5.getSocketClient();
        ConfigManager configManager2 = new ConfigManager(localConfigStorage, sadnesSocketClient3);
        this.configManager = configManager2;
        CosmeticManager cosmeticManager = new CosmeticManager();
        this.cosmeticsManager = cosmeticManager;
        cosmeticManager.loadPack();
        FeatureManager featureManager2 = new FeatureManager();
        this.featureManager = featureManager2;
        featureManager2.initialize();
        CommandManager commandManager2 = new CommandManager();
        this.commandManager = commandManager2;
        commandManager2.registerDefaults();
        WaypointRenderer.register();
        GpsIndicator.register();
        EventBus eventBus = EVENT_BUS;
        ServerPacketFilter serverPacketFilter = new ServerPacketFilter();
        eventBus.subscribe(serverPacketFilter);
        ComponentManager componentManager2 = new ComponentManager();
        this.componentManager = componentManager2;
        componentManager2.init();
        ScriptManager scriptManager2 = new ScriptManager();
        this.scriptManager = scriptManager2;
        scriptManager2.init();
        ConfigManager configManager3 = this.configManager;
        configManager3.captureDefaults();
        ConfigManager configManager4 = this.configManager;
        configManager4.loadLastActive();
    }

    public void initScreens() {
        ClickGuiScreen clickGuiScreen = new ClickGuiScreen();
        this.clickGui = clickGuiScreen;
        clickGuiScreen.ensureRows();
        MainMenuScreen mainMenuScreen = new MainMenuScreen();
        this.mainMenu = mainMenuScreen;
    }

    public void shutdown() {
        boolean flag = this.initialized;
        ConfigManager configManager2 = this.configManager;
        FriendManager friendManager2 = this.friendManager;
        MacroManager macroManager2 = this.macroManager;
        StaffManager staffManager2 = this.staffManager;
        AccountManager accountManager2 = this.accountManager;
        BackendService backendService = this.backend;
        FeatureManager featureManager2 = this.featureManager;
        if (flag) {
            MacroManager macroManager3 = macroManager2;
            BackendService backendService2 = backendService;
            AccountManager accountManager3 = accountManager2;
            FriendManager friendManager3 = friendManager2;
            StaffManager staffManager3 = staffManager2;
            FeatureManager featureManager3 = featureManager2;
            if (configManager2 != null) {
                configManager2.saveActive();
                ConfigManager configManager3 = this.configManager;
                configManager3.shutdown();
                FriendManager friendManager4 = this.friendManager;
                MacroManager macroManager4 = this.macroManager;
                StaffManager staffManager4 = this.staffManager;
                AccountManager accountManager4 = this.accountManager;
                BackendService backendService3 = this.backend;
                FeatureManager featureManager4 = this.featureManager;
                macroManager3 = macroManager4;
                backendService2 = backendService3;
                accountManager3 = accountManager4;
                friendManager3 = friendManager4;
                staffManager3 = staffManager4;
                featureManager3 = featureManager4;
            }

            if (friendManager3 != null) {
                friendManager3.save();
                MacroManager macroManager5 = this.macroManager;
                StaffManager staffManager5 = this.staffManager;
                AccountManager accountManager5 = this.accountManager;
                BackendService backendService4 = this.backend;
                FeatureManager featureManager5 = this.featureManager;
                macroManager3 = macroManager5;
                backendService2 = backendService4;
                accountManager3 = accountManager5;
                staffManager3 = staffManager5;
                featureManager3 = featureManager5;
            }

            if (macroManager3 != null) {
                macroManager3.unregister();
                MacroManager macroManager6 = this.macroManager;
                macroManager6.save();
                StaffManager staffManager6 = this.staffManager;
                AccountManager accountManager6 = this.accountManager;
                BackendService backendService5 = this.backend;
                FeatureManager featureManager6 = this.featureManager;
                macroManager3 = null;
                backendService2 = backendService5;
                accountManager3 = accountManager6;
                staffManager3 = staffManager6;
                featureManager3 = featureManager6;
            }

            if (staffManager3 != null) {
                this.macroManager = macroManager3;
                staffManager3.save();
                MacroManager macroManager7 = this.macroManager;
                AccountManager accountManager7 = this.accountManager;
                BackendService backendService6 = this.backend;
                FeatureManager featureManager7 = this.featureManager;
                macroManager3 = macroManager7;
                backendService2 = backendService6;
                accountManager3 = accountManager7;
                featureManager3 = featureManager7;
            }

            if (accountManager3 != null) {
                this.macroManager = macroManager3;
                accountManager3.save();
                MacroManager macroManager8 = this.macroManager;
                BackendService backendService7 = this.backend;
                FeatureManager featureManager8 = this.featureManager;
                macroManager3 = macroManager8;
                backendService2 = backendService7;
                featureManager3 = featureManager8;
            }

            if (backendService2 != null) {
                this.macroManager = macroManager3;
                backendService2.stop();
                MacroManager macroManager9 = this.macroManager;
                FeatureManager featureManager9 = this.featureManager;
                featureManager3 = featureManager9;
                macroManager3 = macroManager9;
                backendService2 = null;
            }

            this.macroManager = macroManager3;
            this.backend = backendService2;
            featureManager3.shutdown();
            SoundPlayer.shutdown();
            ScriptManager scriptManager2 = this.scriptManager;
            ComponentManager componentManager2 = this.componentManager;
            CommandManager commandManager2;
            ScriptManager scriptManager3;
            ComponentManager componentManager3;
            if (scriptManager2 != null) {
                this.commandManager = null;
                scriptManager2.shutdown();
                CommandManager commandManager3 = this.commandManager;
                ComponentManager componentManager4 = this.componentManager;
                commandManager2 = commandManager3;
                scriptManager3 = null;
                componentManager3 = componentManager4;
            } else {
                commandManager2 = null;
                scriptManager3 = scriptManager2;
                componentManager3 = componentManager2;
            }

            if (componentManager3 != null) {
                this.commandManager = commandManager2;
                this.scriptManager = scriptManager3;
                componentManager3.shutdown();
                CommandManager commandManager4 = this.commandManager;
                ScriptManager scriptManager4 = this.scriptManager;
                commandManager2 = commandManager4;
                scriptManager3 = scriptManager4;
                componentManager3 = null;
            }

            this.configManager = null;
            this.commandManager = commandManager2;
            this.scriptManager = scriptManager3;
            this.componentManager = componentManager3;
            this.cleanup();
            org.slf4j.Logger logger = LOGGER;
            this.initialized = false;
            logger.info("{} shutdown", "Sadnes");
        }
    }

    public void reloadGraphics() {
        if (this.shuttingDown) {
            this.cleanup();
            this.saveAll();
        }
    }

    public void saveAll() {
        boolean flag = this.shuttingDown;
        if (!flag) {
            GpuDevice gpuDevice = RenderSystem.tryGetDevice();
            if (gpuDevice != null) {
                Fonts.load();
                Language.load();
                ThemePrefs.load();
                FeatureManager featureManager2 = this.featureManager;
                featureManager2.refreshSearchIndex();
                RoundedUniformStorage.init();
                ChamsUniformStorage.init();
                FlameUniformStorage.init();
                ScriptFxUniformStorage.init();
                MotionBlurUniformStorage.init();
                AmbiencePostUniformStorage.init();
                WeatherUniformStorage.init();
                FogUniformStorage.init();
                BlurRectUniformStorage.init();
                GridBgUniformStorage.init();
                SkyUniformStorage.init();
                org.slf4j.Logger logger = LOGGER;
                this.shuttingDown = true;
                logger.info("{} graphics initialized", "Sadnes");
                SvgIconCache.warmup();
                LogoTexture.warmup();
                ChamsNoiseTexture.preload();
                return;
            }
        }
    }

    private void cleanup() {
        RenderHooks.reset();
        if (this.shuttingDown) {
            RoundedUniformStorage.close();
            ChamsUniformStorage.close();
            FlameUniformStorage.close();
            ScriptFxUniformStorage.close();
            MotionBlurUniformStorage.close();
            AmbiencePostUniformStorage.close();
            WeatherUniformStorage.close();
            FogUniformStorage.close();
            BlurRectUniformStorage.close();
            GridBgUniformStorage.close();
            SkyUniformStorage.close();
            FrameBufferPool.closeAll();
            ShapeBlur.INSTANCE.reset();
            MotionBlurRenderer.INSTANCE.reset();
            AmbienceRenderer.INSTANCE.reset();
            ShaderSkyRenderer.release();
            FoliageSwayManager.INSTANCE.close();
            GooRenderer.INSTANCE.reset();
            DischargeRenderer.INSTANCE.reset();
            SvgIconCache.clear();
            LogoTexture.release();
            PlainLogoTexture.release();
            MenuBackgroundTexture.unload();
            RopeRenderer.close();
            LogoGlowRenderer.close();
            BezierCurveRenderer.close();
            AnimatedIconManager.clear();
            Fonts.closeAll();
            this.shuttingDown = false;
        }
    }

    public FeatureManager getFeatureManager() {
        return this.featureManager;
    }

    public CommandManager getCommandManager() {
        return this.commandManager;
    }

    public ComponentManager getComponentManager() {
        return this.componentManager;
    }

    public ScriptManager getScriptManager() {
        return this.scriptManager;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public FriendManager getFriendManager() {
        return this.friendManager;
    }

    public MacroManager getMacroManager() {
        return this.macroManager;
    }

    public AutoBuyManager getAutoBuyManager() {
        return this.autoBuyManager;
    }

    public CollectorManager getCollectorManager() {
        return this.collectorManager;
    }

    public PartyFriendSync getConfigSync() {
        return this.configSync;
    }

    public StaffManager getStaffManager() {
        return this.staffManager;
    }

    public AccountManager getAccountManager() {
        return this.accountManager;
    }

    public BackendService getBackend() {
        return this.backend;
    }

    public CosmeticManager getCosmeticsManager() {
        return this.cosmeticsManager;
    }

    public ClickGuiScreen getClickGui() {
        return this.clickGui;
    }

    public MainMenuScreen getMainMenu() {
        return this.mainMenu;
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public boolean isShuttingDown() {
        return this.shuttingDown;
    }

    public static org.slf4j.Logger getLogger() {
        return LOGGER;
    }

    public static Sadnes getInstance() {
        return INSTANCE;
    }

    public static EventBus getEventBus() {
        return EVENT_BUS;
    }
}
