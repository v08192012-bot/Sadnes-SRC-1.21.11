package tech.sadnes.client.neuro;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.Render2DEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.ui.hud.HudPanelUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class NeuroAim {
    private static final Logger LOGGER = LoggerFactory.getLogger("NeuroAim");
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Gson GSON = new Gson();
    public static final NeuroAim INSTANCE = new NeuroAim();
    private final NeuroStorage storage = new NeuroStorage();
    private final NeuroTrainer trainer = new NeuroTrainer(this.storage);
    private final NeuroDebug debug = new NeuroDebug(this.storage);
    private final NeuroAimController aimController = new NeuroAimController(this.debug);
    private final NeuroAimController bundledController = new NeuroAimController(this.debug);
    private final NeuroAimController boxController = new NeuroAimController(this.debug);
    private final NeuroRecorder recorder = new NeuroRecorder(this.storage, this.debug);
    private final MixtureDensityNetwork bundledModel = this.loadBundledModel();
    private volatile MixtureDensityNetwork model;
    private volatile String modelName;
    private volatile float strength = 1.0F;
    private String lastSavedState;
    private int tickCounter;
    private final Animation recordingAnimation = new Animation();
    private float displayedSampleCount;
    private float indicatorWidth;

    private NeuroAim() {
        Sadnes.getEventBus().subscribe(this);
        this.loadState();
    }

    private void loadState() {
        String string = this.storage.readState();
        if (string != null) {
            try {
                NeuroAim.State state = GSON.fromJson(string, NeuroAim.State.class);
                if (state == null) {
                    return;
                }

                if (state.showAimPoint != null) {
                    this.debug.setShowAimPoint(state.showAimPoint);
                }

                if (state.showTrail != null) {
                    this.debug.setShowTrail(state.showTrail);
                }

                if (state.showHitbox != null) {
                    this.debug.setShowHitbox(state.showHitbox);
                }

                if (state.showDebugHud != null) {
                    this.debug.setShowDebugHud(state.showDebugHud);
                }

                if (state.csvLogging != null) {
                    this.debug.setCsvLogging(state.csvLogging);
                }

                if (state.debugHudX != null && state.debugHudY != null) {
                    this.debug.getHud().setPosition(state.debugHudX, state.debugHudY);
                }

                if (state.modelName != null) {
                    this.loadModel(state.modelName);
                }

                this.lastSavedState = this.serializeState();
            } catch (Exception exception) {
                LOGGER.warn("Neuro state load failed: {}", exception.getMessage());
            }
        }
    }

    private String serializeState() {
        NeuroAim.State state = new NeuroAim.State();
        state.modelName = this.modelName;
        state.debugHudX = this.debug.getHud().position2.x;
        state.debugHudY = this.debug.getHud().position2.y;
        state.showAimPoint = this.debug.isShowAimPoint();
        state.showTrail = this.debug.isShowTrail();
        state.showHitbox = this.debug.isShowHitbox();
        state.showDebugHud = this.debug.isShowDebugHud();
        state.csvLogging = this.debug.isCsvLogging();
        return GSON.toJson(state);
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.player != null) {
            this.aimController.tick();
            this.bundledController.tick();
            this.boxController.tick();
            this.recorder.tick();
            if (++this.tickCounter % 10 == 0) {
                String string = this.serializeState();
                if (!string.equals(this.lastSavedState)) {
                    this.lastSavedState = string;
                    this.storage.saveStateAsync(string);
                }
            }
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent render2DEvent) {
        if (mc.player != null) {
            boolean flag = this.recorder.isRecording();
            this.recordingAnimation.update();
            this.recordingAnimation.animate(flag ? 1.0 : 0.0, 0.25, flag ? Easings.QUAD_OUT : Easings.QUAD_IN, true);
            float f = this.recordingAnimation.get();
            if (f <= 0.01F) {
                this.displayedSampleCount = 0.0F;
                this.indicatorWidth = 0.0F;
            } else {
                DrawContext drawContext = render2DEvent.getContext();
                int i = this.recorder.getDataset() != null ? this.recorder.getDataset().getSampleCount() : 0;
                this.displayedSampleCount = this.displayedSampleCount + (i - this.displayedSampleCount) * 0.15F;
                boolean flag2 = this.recorder.isCapturing();
                MsdfFont msdfFont = Fonts.INTER_SEMIBOLD;
                String string = String.valueOf(Math.round(this.displayedSampleCount));
                float f2 = 9.0F + 4.0F + 6.0F + msdfFont.getWidth(string, 6.5F) + 9.0F;
                this.indicatorWidth = this.indicatorWidth <= 0.0F ? f2 : this.indicatorWidth + (f2 - this.indicatorWidth) * 0.2F;
                float f3 = this.indicatorWidth;
                float f4 = (mc.getWindow().getScaledWidth() - f3) / 2.0F;
                float f5 = 4.0F + this.getTopOffset() - (1.0F - f) * 8.0F;
                RenderUtil renderUtil = HudPanelUtil.drawPanel(drawContext, f4, f5, f3, 17.0F, f, 17.0F / 2.4F);
                float f6 = flag2 ? 0.55F + 0.45F * (float)Math.sin(System.currentTimeMillis() / 140.0) : 0.7F;
                int i2 = flag2 ? ColorUtil.rgb(235, 80, 80) : Theme.getSecondaryText();
                new RenderUtil(drawContext, f4 + 9.0F, f5 + 17.0F / 2.0F - 4.0F / 2.0F - 1.0F, 4.0F, 4.0F)
                    .rect(ColorUtil.multiplyAlpha(i2, f6 * f), Radius.all(4.0F / 2.0F));
                renderUtil.textMiddleLeft(msdfFont, string, 9.0F + 4.0F + 6.0F, -1.0F, Theme.withAlpha(flag2 ? Theme.getTextColor() : Theme.getSecondaryText(), f), 6.5F);
                float f7 = f4 + 9.0F;
                float f8 = f3 - 9.0F * 2.0F;
                float f9 = f5 + 17.0F - 4.0F;
                new RenderUtil(drawContext, f7, f9, f8, 1.5F).rect(Theme.withAlpha(Theme.getMutedText(), 0.45F * f), Radius.all(1.5F / 2.0F));
                float f10 = MathHelper.clamp(i / 15000.0F, 0.0F, 1.0F);
                if (f10 > 0.01F) {
                    new RenderUtil(drawContext, f7, f9, f8 * f10, 1.5F).rect(Theme.withAlpha(Theme.getPrimary(), f), Radius.all(1.5F / 2.0F));
                }
            }
        }
    }

    private float getTopOffset() {
        return HudPanelUtil.getTopOffset();
    }

    public float getIndicatorHeight() {
        return this.recordingAnimation.get() <= 0.01F ? 0.0F : 4.0F + this.getTopOffset() + 17.0F;
    }

    public float[] computeRotation(LivingEntity livingEntity, float f) {
        return this.recorder.isRecording() ? null : this.aimController.computeRotation(this.model, livingEntity, this.strength);
    }

    public float[] computeBundledRotation(LivingEntity livingEntity) {
        this.bundledController.tick();
        return this.bundledController.computeRotation(this.bundledModel, livingEntity, 1.0F);
    }

    public float[] computeBoxRotation(Box box, float f, float f2) {
        return this.model != null
            ? this.boxController.computeRotationForBox(this.model, box, f, f2)
            : this.boxController.computeRotationForBox(this.bundledModel, box, f, f2);
    }

    private MixtureDensityNetwork loadBundledModel() {
        String string = "/assets/sadnes/neuro/ZOV.smodel";

        try {
            InputStream inputStream = NeuroAim.class.getResourceAsStream(string);

            MixtureDensityNetwork mixtureDensityNetwork;
            try {
                if (inputStream == null) {
                    throw new IOException("Missing resource " + string);
                }

                mixtureDensityNetwork = MixtureDensityNetwork.load(inputStream);
                if (mixtureDensityNetwork.inputSize != 55 || mixtureDensityNetwork.outputSize != 2) {
                    throw new IOException("Incompatible ZOV model dimensions");
                }

                LOGGER.info("Bundled Funtime model loaded: ZOV");
            } catch (Throwable throwable) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }

                throw throwable;
            }

            inputStream.close();
            return mixtureDensityNetwork;
        } catch (IOException ioException) {
            LOGGER.error("Bundled Funtime model load failed", ioException);
            return null;
        }
    }

    public void setStrength(float f) {
        this.strength = f;
    }

    public boolean loadModel(String string) {
        try {
            MixtureDensityNetwork mixtureDensityNetwork = MixtureDensityNetwork.load(this.storage.getModelFile(string));
            if (mixtureDensityNetwork.inputSize == 55 && mixtureDensityNetwork.outputSize == 2) {
                if (this.recorder.isRecording()) {
                    this.recorder.stopRecording();
                }

                this.model = mixtureDensityNetwork;
                this.modelName = string;
                this.aimController.reset();
                LOGGER.info("Neuro model loaded: {}", string);
                return true;
            } else {
                return false;
            }
        } catch (Exception exception) {
            LOGGER.warn("Neuro model load failed: {}", exception.getMessage());
            return false;
        }
    }

    public void setModel(MixtureDensityNetwork mixtureDensityNetwork, String string) {
        mc.execute(() -> {
            if (this.recorder.isRecording()) {
                this.recorder.stopRecording();
            }

            this.model = mixtureDensityNetwork;
            this.modelName = string;
            this.aimController.reset();
        });
    }

    public void unloadModel() {
        this.model = null;
        this.modelName = null;
    }

    public boolean deleteModel(String string) {
        boolean flag = this.storage.deleteModel(string);
        if (flag && string.equals(this.modelName)) {
            this.unloadModel();
        }

        return flag;
    }

    public boolean renameModel(String string, String string2) {
        if (!this.storage.renameModel(string, string2)) {
            return false;
        }

        if (string.equals(this.modelName)) {
            this.modelName = string2;
        }

        return true;
    }

    public NeuroStorage getStorage() {
        return this.storage;
    }

    public NeuroTrainer getTrainer() {
        return this.trainer;
    }

    public NeuroDebug getDebug() {
        return this.debug;
    }

    public NeuroAimController getAimController() {
        return this.aimController;
    }

    public NeuroAimController getBundledController() {
        return this.bundledController;
    }

    public NeuroAimController getBoxController() {
        return this.boxController;
    }

    public NeuroRecorder getRecorder() {
        return this.recorder;
    }

    public MixtureDensityNetwork getBundledModel() {
        return this.bundledModel;
    }

    public MixtureDensityNetwork getModel() {
        return this.model;
    }

    public String getModelName() {
        return this.modelName;
    }

    public float getStrength() {
        return this.strength;
    }

    public String getLastSavedState() {
        return this.lastSavedState;
    }

    public int getTickCounter() {
        return this.tickCounter;
    }

    public Animation getRecordingAnimation() {
        return this.recordingAnimation;
    }

    public float getDisplayedSampleCount() {
        return this.displayedSampleCount;
    }

    public float getIndicatorWidth() {
        return this.indicatorWidth;
    }

    static final class State {
        String modelName;
        Float debugHudX;
        Float debugHudY;
        Boolean showAimPoint;
        Boolean showTrail;
        Boolean showHitbox;
        Boolean showDebugHud;
        Boolean csvLogging;
    }
}
