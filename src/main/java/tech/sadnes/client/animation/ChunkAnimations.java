package tech.sadnes.client.animation;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import tech.sadnes.client.feature.impl.render.Animations;
import tech.sadnes.client.util.math.TimerUtil;

public final class ChunkAnimations {
    public static final ChunkAnimations INSTANCE = new ChunkAnimations();
    private final Long2ObjectMap<ChunkAnimations.ChunkState> chunkStates = new Long2ObjectOpenHashMap<>();
    private final Queue<Long> pendingChunks = new ConcurrentLinkedQueue<>();
    private volatile boolean enabled;

    private ChunkAnimations() {
    }

    public void setEnabled(boolean flag) {
        this.enabled = flag;
        if (!flag) {
            this.clear();
        }
    }

    public void queueChunk(int i, int i2, int i3) {
        if (this.enabled) {
            this.pendingChunks.add(BlockPos.asLong(i, i2, i3));
        }
    }

    public void clear() {
        this.pendingChunks.clear();
        this.chunkStates.clear();
    }

    public Matrix4fc transform(Matrix4fc matrix4fc, int i, int i2, int i3) {
        Long longValue2;
        while ((longValue2 = this.pendingChunks.poll()) != null) {
            if (this.chunkStates.size() > 4096) {
                this.chunkStates.clear();
            }

            this.chunkStates.put(longValue2.longValue(), new ChunkAnimations.ChunkState());
        }

        if (this.chunkStates.isEmpty()) {
            return matrix4fc;
        }

        long l = BlockPos.asLong(i, i2, i3);
        ChunkAnimations.ChunkState chunkState = this.chunkStates.get(l);
        if (chunkState == null) {
            return matrix4fc;
        }

        Animations animations = Animations.getInstance();
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (animations != null && animations.isEnabled("chunks") && minecraftClient.world != null) {
            if (!chunkState.started) {
                if (chunkState.delayTimer.hasPassed(5000L)) {
                    this.chunkStates.remove(l);
                    return matrix4fc;
                }

                chunkState.started = true;
                chunkState.animationTimer.reset();
            }

            if (chunkState.animationTimer.hasPassed(800.0)) {
                this.chunkStates.remove(l);
                return matrix4fc;
            } else {
                float f = 1.0F - (float)Easings.SINE_OUT.ease((float)chunkState.animationTimer.getElapsed() / 800.0F);
                return f <= 0.0F ? matrix4fc : new Matrix4f(matrix4fc).translate(0.0F, -i2 * f, 0.0F);
            }
        } else {
            this.chunkStates.clear();
            return matrix4fc;
        }
    }

    static final class ChunkState {
        final TimerUtil delayTimer = new TimerUtil();
        final TimerUtil animationTimer = new TimerUtil();
        boolean started;
    }
}
