package tech.sadnes.client.neuro;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class MixtureDensityNetwork {
    private static final int MAGIC = 1397640753;
    private static final float LEAKY_SLOPE = 0.01F;
    private static final float BETA1 = 0.9F;
    private static final float BETA2 = 0.999F;
    private static final float EPSILON = 1.0E-8F;
    private static final float GRADIENT_CLIP = 5.0F;
    private static final float LOG_SIGMA_MIN = -7.0F;
    private static final float LOG_SIGMA_MAX = 3.0F;
    private static final int THREAD_COUNT = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
    private static final ExecutorService EXECUTOR = createExecutor();
    public final int inputSize;
    public final int outputSize;
    public final int mixtureCount;
    public final int headSize;
    final int[] layerSizes;
    final int lastHiddenSize;
    final int maxLayerSize;
    private final float[][] weights;
    private final float[][] biases;
    private final float[] headWeights;
    private final float[] headBiases;
    private float[][] weightMoment1;
    private float[][] weightMoment2;
    private float[][] biasMoment1;
    private float[][] biasMoment2;
    private float[] headWeightMoment1;
    private float[] headWeightMoment2;
    private float[] headBiasMoment1;
    private float[] headBiasMoment2;
    private int step;
    private MixtureDensityNetwork.Workspace[] workspaces;
    private MixtureDensityNetwork.Workspace inferenceWorkspace;

    public MixtureDensityNetwork(int i, int i2, int i3, int... ints) {
        this.inputSize = i;
        this.outputSize = i2;
        this.mixtureCount = i3;
        this.headSize = i3 * (1 + 2 * i2);
        this.layerSizes = new int[ints.length + 1];
        this.layerSizes[0] = i;
        System.arraycopy(ints, 0, this.layerSizes, 1, ints.length);
        int i4 = this.layerSizes.length - 1;
        this.weights = new float[i4][];
        this.biases = new float[i4][];
        Random random = new Random(42L);

        for (int i5 = 0; i5 < i4; i5++) {
            int i6 = this.layerSizes[i5];
            int i7 = this.layerSizes[i5 + 1];
            this.weights[i5] = new float[i7 * i6];
            this.biases[i5] = new float[i7];
            float f = (float)Math.sqrt(2.0 / i6);
            int i8 = 0;

            for (int i9 = i7 * i6; i8 < i9; i8++) {
                this.weights[i5][i8] = (float)random.nextGaussian() * f;
            }
        }

        this.lastHiddenSize = this.layerSizes[this.layerSizes.length - 1];
        this.maxLayerSize = maxOf(this.layerSizes);
        this.headWeights = new float[this.headSize * this.lastHiddenSize];
        this.headBiases = new float[this.headSize];
        float f2 = (float)Math.sqrt(1.0 / this.lastHiddenSize);
        int i10 = 0;

        for (int i11 = this.headSize * this.lastHiddenSize; i10 < i11; i10++) {
            this.headWeights[i10] = (float)random.nextGaussian() * f2;
        }
    }

    private MixtureDensityNetwork(int i, int i2, int i3, int[] ints, float[][] floatses, float[][] floatses2, float[] floats, float[] floats2) {
        this.inputSize = i;
        this.outputSize = i2;
        this.mixtureCount = i3;
        this.headSize = i3 * (1 + 2 * i2);
        this.layerSizes = ints;
        this.lastHiddenSize = ints[ints.length - 1];
        this.maxLayerSize = maxOf(ints);
        this.weights = floatses;
        this.biases = floatses2;
        this.headWeights = floats;
        this.headBiases = floats2;
    }

    private static int maxOf(int[] ints) {
        int i = 0;

        for (int i2 : ints) {
            if (i2 > i) {
                i = i2;
            }
        }

        return i;
    }

    private static ExecutorService createExecutor() {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(THREAD_COUNT, THREAD_COUNT, 20L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), runnable -> {
            Thread thread = new Thread(runnable, "Neuro-Worker");
            thread.setDaemon(true);
            thread.setPriority(1);
            return thread;
        });
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        return threadPoolExecutor;
    }

    public static int getThreadCount() {
        return THREAD_COUNT;
    }

    private void ensureWorkspaces(int i) {
        if (this.workspaces == null || this.workspaces.length < i) {
            MixtureDensityNetwork.Workspace[] workspaces2 = new MixtureDensityNetwork.Workspace[i];
            int i2 = this.workspaces == null ? 0 : this.workspaces.length;

            for (int i3 = 0; i3 < i2; i3++) {
                workspaces2[i3] = this.workspaces[i3];
            }

            for (int i4 = i2; i4 < i; i4++) {
                workspaces2[i4] = new MixtureDensityNetwork.Workspace();
            }

            this.workspaces = workspaces2;
        }
    }

    private void initOptimizer() {
        if (this.weightMoment1 == null) {
            int i = this.layerSizes.length - 1;
            this.weightMoment1 = new float[i][];
            this.weightMoment2 = new float[i][];
            this.biasMoment1 = new float[i][];
            this.biasMoment2 = new float[i][];

            for (int i2 = 0; i2 < i; i2++) {
                this.weightMoment1[i2] = new float[this.weights[i2].length];
                this.weightMoment2[i2] = new float[this.weights[i2].length];
                this.biasMoment1[i2] = new float[this.biases[i2].length];
                this.biasMoment2[i2] = new float[this.biases[i2].length];
            }

            this.headWeightMoment1 = new float[this.headWeights.length];
            this.headWeightMoment2 = new float[this.headWeights.length];
            this.headBiasMoment1 = new float[this.headSize];
            this.headBiasMoment2 = new float[this.headSize];
            this.step = 0;
        }
    }

    private static float clipGradient(float f) {
        if (f < -5.0F) {
            return -5.0F;
        } else {
            return f > 5.0F ? 5.0F : f;
        }
    }

    private float[] forward(MixtureDensityNetwork.Workspace workspace, float[] floats) {
        int i = this.layerSizes.length - 1;
        workspace.activations[0] = floats;
        float[] floats2 = floats;

        for (int i2 = 0; i2 < i; i2++) {
            int i3 = this.layerSizes[i2];
            int i4 = this.layerSizes[i2 + 1];
            float[] floats3 = this.weights[i2];
            float[] floats4 = this.biases[i2];
            float[] floats5 = workspace.preActivations[i2];
            float[] floats6 = workspace.activations[i2 + 1];
            int i5 = 0;

            for (int i6 = 0; i5 < i4; i6 += i3) {
                float f = 0.0F;
                float f2 = 0.0F;
                float f3 = 0.0F;
                float f4 = 0.0F;
                int i7 = 0;

                for (int i8 = i3 - 3; i7 < i8; i7 += 4) {
                    f += floats3[i6 + i7] * floats2[i7];
                    f2 += floats3[i6 + i7 + 1] * floats2[i7 + 1];
                    f3 += floats3[i6 + i7 + 2] * floats2[i7 + 2];
                    f4 += floats3[i6 + i7 + 3] * floats2[i7 + 3];
                }

                while (i7 < i3) {
                    f += floats3[i6 + i7] * floats2[i7];
                    i7++;
                }

                float f5 = floats4[i5] + (f + f2) + (f3 + f4);
                floats5[i5] = f5;
                floats6[i5] = f5 >= 0.0F ? f5 : 0.01F * f5;
                i5++;
            }

            floats2 = floats6;
        }

        int i9 = 0;

        for (int i10 = 0; i9 < this.headSize; i10 += this.lastHiddenSize) {
            float f6 = 0.0F;
            float f7 = 0.0F;
            float f8 = 0.0F;
            float f9 = 0.0F;
            int i11 = 0;

            for (int i12 = this.lastHiddenSize - 3; i11 < i12; i11 += 4) {
                f6 += this.headWeights[i10 + i11] * floats2[i11];
                f7 += this.headWeights[i10 + i11 + 1] * floats2[i11 + 1];
                f8 += this.headWeights[i10 + i11 + 2] * floats2[i11 + 2];
                f9 += this.headWeights[i10 + i11 + 3] * floats2[i11 + 3];
            }

            while (i11 < this.lastHiddenSize) {
                f6 += this.headWeights[i10 + i11] * floats2[i11];
                i11++;
            }

            workspace.headOutput[i9] = this.headBiases[i9] + (f6 + f7) + (f8 + f9);
            i9++;
        }

        return floats2;
    }

    private void computeMixture(MixtureDensityNetwork.Workspace workspace) {
        float f = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < this.mixtureCount; i++) {
            if (workspace.headOutput[i] > f) {
                f = workspace.headOutput[i];
            }
        }

        float f2 = 0.0F;

        for (int i2 = 0; i2 < this.mixtureCount; i2++) {
            float f3 = (float)Math.exp(workspace.headOutput[i2] - f);
            workspace.mixWeights[i2] = f3;
            f2 += f3;
        }

        for (int i3 = 0; i3 < this.mixtureCount; i3++) {
            workspace.mixWeights[i3] = workspace.mixWeights[i3] / f2;
        }

        int i4 = this.mixtureCount;
        int i5 = i4 + i4 * this.outputSize;
        int i6 = 0;

        for (int i7 = i4 * this.outputSize; i6 < i7; i6++) {
            workspace.means[i6] = workspace.headOutput[i4 + i6];
            float f4 = workspace.headOutput[i5 + i6];
            float f5 = f4 < -7.0F ? -7.0F : (f4 > 3.0F ? 3.0F : f4);
            workspace.sigmas[i6] = (float)Math.exp(f5);
        }
    }

    private float logLikelihood(MixtureDensityNetwork.Workspace workspace, float[] floats) {
        float f = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < this.mixtureCount; i++) {
            float f2 = 0.0F;
            int i2 = i * this.outputSize;

            for (int i3 = 0; i3 < this.outputSize; i3++) {
                float f3 = (floats[i3] - workspace.means[i2 + i3]) / workspace.sigmas[i2 + i3];
                f2 += -0.5F * f3 * f3 - (float)Math.log(workspace.sigmas[i2 + i3]) - 0.9189385F;
            }

            float f4 = (float)Math.log(Math.max(workspace.mixWeights[i], 1.0E-12)) + f2;
            workspace.logComponents[i] = f4;
            if (f4 > f) {
                f = f4;
            }
        }

        float f5 = 0.0F;

        for (int i4 = 0; i4 < this.mixtureCount; i4++) {
            f5 += (float)Math.exp(workspace.logComponents[i4] - f);
        }

        return f + (float)Math.log(f5);
    }

    private void backward(MixtureDensityNetwork.Workspace workspace, float[] floats, float f, float f2, float[] floats2) {
        int i = this.mixtureCount;
        int i2 = i + i * this.outputSize;

        for (int i3 = 0; i3 < i; i3++) {
            float f3 = (float)Math.exp(workspace.logComponents[i3] - f2);
            workspace.headGrads[i3] = f * (workspace.mixWeights[i3] - f3);
            int i4 = i3 * this.outputSize;

            for (int i5 = 0; i5 < this.outputSize; i5++) {
                float f4 = (floats[i5] - workspace.means[i4 + i5]) / workspace.sigmas[i4 + i5];
                workspace.headGrads[i + i4 + i5] = f * (-f3 * f4 / workspace.sigmas[i4 + i5]);
                workspace.headGrads[i2 + i4 + i5] = f * (-f3 * (f4 * f4 - 1.0F));
            }
        }

        float[] floats3 = workspace.backpropBuffers[0];
        Arrays.fill(floats3, 0, this.lastHiddenSize, 0.0F);
        int i6 = 0;

        for (int i7 = 0; i6 < this.headSize; i7 += this.lastHiddenSize) {
            float f5 = clipGradient(workspace.headGrads[i6]);
            workspace.headBiasGrads[i6] = workspace.headBiasGrads[i6] + f5;

            for (int i8 = 0; i8 < this.lastHiddenSize; i8++) {
                workspace.headWeightGrads[i7 + i8] = workspace.headWeightGrads[i7 + i8] + f5 * floats2[i8];
                floats3[i8] += this.headWeights[i7 + i8] * f5;
            }

            i6++;
        }

        int i9 = 0;

        for (int i10 = this.layerSizes.length - 2; i10 >= 0; i10--) {
            int i11 = this.layerSizes[i10];
            int i12 = this.layerSizes[i10 + 1];
            float[] floats4 = workspace.backpropBuffers[i9];
            float[] floats5 = workspace.preActivations[i10];
            float[] floats6 = workspace.activations[i10];
            float[] floats7 = workspace.weightGrads[i10];
            float[] floats8 = workspace.biasGrads[i10];
            float[] floats9 = this.weights[i10];
            float[] floats10 = workspace.deltaBuffer;

            for (int i13 = 0; i13 < i12; i13++) {
                floats10[i13] = clipGradient(floats4[i13] * (floats5[i13] >= 0.0F ? 1.0F : 0.01F));
            }

            float[] floats11 = null;
            if (i10 > 0) {
                floats11 = workspace.backpropBuffers[1 - i9];
                Arrays.fill(floats11, 0, i11, 0.0F);
            }

            int i14 = 0;

            for (int i15 = 0; i14 < i12; i15 += i11) {
                float f6 = floats10[i14];
                floats8[i14] += f6;
                if (floats11 == null) {
                    for (int i16 = 0; i16 < i11; i16++) {
                        floats7[i15 + i16] = floats7[i15 + i16] + f6 * floats6[i16];
                    }
                } else {
                    for (int i17 = 0; i17 < i11; i17++) {
                        floats7[i15 + i17] = floats7[i15 + i17] + f6 * floats6[i17];
                        floats11[i17] += floats9[i15 + i17] * f6;
                    }
                }

                i14++;
            }

            i9 = 1 - i9;
        }
    }

    private void accumulateRange(MixtureDensityNetwork.Workspace workspace, float[][] floatses, float[][] floatses2, float[] floats, int i, int i2) {
        workspace.clearGradients();

        for (int i3 = i; i3 < i2; i3++) {
            float f = floats == null ? 1.0F : floats[i3];
            if (!(f <= 0.0F)) {
                workspace.weightSum += f;
                float[] floats2 = this.forward(workspace, floatses[i3]);
                this.computeMixture(workspace);
                float f2 = this.logLikelihood(workspace, floatses2[i3]);
                workspace.loss += f * -f2;
                this.backward(workspace, floatses2[i3], f, f2, floats2);
            }
        }
    }

    private void evaluateRange(MixtureDensityNetwork.Workspace workspace, float[][] floatses, float[][] floatses2, float[] floats, int i, int i2) {
        workspace.loss = 0.0F;
        workspace.weightSum = 0.0F;

        for (int i3 = i; i3 < i2; i3++) {
            float f = floats == null ? 1.0F : floats[i3];
            if (!(f <= 0.0F)) {
                workspace.weightSum += f;
                this.forward(workspace, floatses[i3]);
                this.computeMixture(workspace);
                workspace.loss = workspace.loss + f * -this.logLikelihood(workspace, floatses2[i3]);
            }
        }
    }

    public float trainBatch(float[][] floatses, float[][] floatses2, float[] floats, int i, float f) {
        this.initOptimizer();
        int i2 = chooseWorkerCount(i, 32);
        this.ensureWorkspaces(i2);
        if (i2 == 1) {
            this.accumulateRange(this.workspaces[0], floatses, floatses2, floats, 0, i);
        } else {
            int i3 = (i + i2 - 1) / i2;
            CountDownLatch countDownLatch = new CountDownLatch(i2 - 1);

            for (int i4 = 1; i4 < i2; i4++) {
                MixtureDensityNetwork.Workspace workspace = this.workspaces[i4];
                int i5 = i4 * i3;
                int i6 = Math.min(i, i5 + i3);
                EXECUTOR.execute(() -> {
                    try {
                        this.accumulateRange(workspace, floatses, floatses2, floats, i5, i6);
                    } finally {
                        countDownLatch.countDown();
                    }
                });
            }

            this.accumulateRange(this.workspaces[0], floatses, floatses2, floats, 0, Math.min(i, i3));
            if (!awaitLatch(countDownLatch)) {
                return 0.0F;
            }

            this.mergeGradients(this.workspaces[0], i2);
        }

        MixtureDensityNetwork.Workspace workspace2 = this.workspaces[0];
        this.applyAdam(workspace2, f);
        return workspace2.weightSum > 0.0F ? workspace2.loss / workspace2.weightSum : 0.0F;
    }

    public float evaluate(float[][] floatses, float[][] floatses2, float[] floats) {
        int i = floatses.length;
        int i2 = chooseWorkerCount(i, 128);
        this.ensureWorkspaces(i2);
        float f;
        float f2;
        int i3;
        if (i2 == 1) {
            this.evaluateRange(this.workspaces[0], floatses, floatses2, floats, 0, i);
            f = 0.0F;
            f2 = 0.0F;
            i3 = 0;
        } else {
            int i4 = (i + i2 - 1) / i2;
            CountDownLatch countDownLatch = new CountDownLatch(i2 - 1);

            for (int i5 = 1; i5 < i2; i5++) {
                MixtureDensityNetwork.Workspace workspace = this.workspaces[i5];
                int i6 = i5 * i4;
                int i7 = Math.min(i, i6 + i4);
                EXECUTOR.execute(() -> {
                    try {
                        this.evaluateRange(workspace, floatses, floatses2, floats, i6, i7);
                    } finally {
                        countDownLatch.countDown();
                    }
                });
            }

            this.evaluateRange(this.workspaces[0], floatses, floatses2, floats, 0, Math.min(i, i4));
            if (!awaitLatch(countDownLatch)) {
                return Float.MAX_VALUE;
            }

            f = 0.0F;
            f2 = 0.0F;
            i3 = 0;
        }

        while (i3 < i2) {
            f += this.workspaces[i3].loss;
            f2 += this.workspaces[i3].weightSum;
            i3++;
        }

        return f2 > 0.0F ? f / f2 : 0.0F;
    }

    private static int chooseWorkerCount(int i, int i2) {
        return THREAD_COUNT > 1 && i >= i2 * 2 ? Math.min(THREAD_COUNT, i / i2) : 1;
    }

    private static boolean awaitLatch(CountDownLatch countDownLatch) {
        try {
            countDownLatch.await();
            return true;
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void mergeGradients(MixtureDensityNetwork.Workspace workspace, int i) {
        int i2 = this.layerSizes.length - 1;

        for (int i3 = 1; i3 < i; i3++) {
            MixtureDensityNetwork.Workspace workspace2 = this.workspaces[i3];

            for (int i4 = 0; i4 < i2; i4++) {
                addInPlace(workspace.weightGrads[i4], workspace2.weightGrads[i4]);
                addInPlace(workspace.biasGrads[i4], workspace2.biasGrads[i4]);
            }

            addInPlace(workspace.headWeightGrads, workspace2.headWeightGrads);
            addInPlace(workspace.headBiasGrads, workspace2.headBiasGrads);
            workspace.loss = workspace.loss + workspace2.loss;
            workspace.weightSum = workspace.weightSum + workspace2.weightSum;
        }
    }

    private static void addInPlace(float[] floats, float[] floats2) {
        int i = 0;

        for (int i2 = floats.length; i < i2; i++) {
            floats[i] += floats2[i];
        }
    }

    private void applyAdam(MixtureDensityNetwork.Workspace workspace, float f) {
        if (!(workspace.weightSum <= 0.0F)) {
            float f2 = 1.0F / workspace.weightSum;
            this.step++;
            float f3 = 1.0F - (float)Math.pow(0.9F, this.step);
            float f4 = 1.0F - (float)Math.pow(0.999F, this.step);
            int i = 0;

            for (int i2 = this.layerSizes.length - 1; i < i2; i++) {
                adamUpdate(this.weights[i], workspace.weightGrads[i], this.weightMoment1[i], this.weightMoment2[i], f2, f, f3, f4);
                adamUpdate(this.biases[i], workspace.biasGrads[i], this.biasMoment1[i], this.biasMoment2[i], f2, f, f3, f4);
            }

            adamUpdate(this.headWeights, workspace.headWeightGrads, this.headWeightMoment1, this.headWeightMoment2, f2, f, f3, f4);
            adamUpdate(this.headBiases, workspace.headBiasGrads, this.headBiasMoment1, this.headBiasMoment2, f2, f, f3, f4);
        }
    }

    private static void adamUpdate(float[] floats, float[] floats2, float[] floats3, float[] floats4, float f, float f2, float f3, float f4) {
        int i = 0;

        for (int i2 = floats.length; i < i2; i++) {
            float f5 = clipGradient(floats2[i] * f);
            float f6 = 0.9F * floats3[i] + 0.100000024F * f5;
            float f7 = 0.999F * floats4[i] + 9.999871E-4F * f5 * f5;
            floats3[i] = f6;
            floats4[i] = f7;
            floats[i] -= f2 * (f6 / f3) / ((float)Math.sqrt(f7 / f4) + 1.0E-8F);
        }
    }

    public MixtureDensityNetwork.Prediction predict(float[] floats) {
        if (this.inferenceWorkspace == null) {
            this.inferenceWorkspace = new MixtureDensityNetwork.Workspace();
        }

        this.forward(this.inferenceWorkspace, floats);
        this.computeMixture(this.inferenceWorkspace);
        return new MixtureDensityNetwork.Prediction(
            (float[])this.inferenceWorkspace.mixWeights.clone(),
            (float[])this.inferenceWorkspace.means.clone(),
            (float[])this.inferenceWorkspace.sigmas.clone()
        );
    }

    public float[] mixtureMean(MixtureDensityNetwork.Prediction prediction) {
        float[] floats = new float[this.outputSize];

        for (int i = 0; i < this.mixtureCount; i++) {
            float f = prediction.weights[i];
            int i2 = i * this.outputSize;

            for (int i3 = 0; i3 < this.outputSize; i3++) {
                floats[i3] += f * prediction.means[i2 + i3];
            }
        }

        return floats;
    }

    public float[] dominantSigma(MixtureDensityNetwork.Prediction prediction) {
        int i = 0;

        for (int i2 = 1; i2 < this.mixtureCount; i2++) {
            if (prediction.weights[i2] > prediction.weights[i]) {
                i = i2;
            }
        }

        float[] floats = new float[this.outputSize];
        System.arraycopy(prediction.sigmas, i * this.outputSize, floats, 0, this.outputSize);
        return floats;
    }

    public MixtureDensityNetwork copy() {
        int i = this.layerSizes.length - 1;
        float[][] floatses = new float[i][];
        float[][] floatses2 = new float[i][];

        for (int i2 = 0; i2 < i; i2++) {
            floatses[i2] = (float[])this.weights[i2].clone();
            floatses2[i2] = (float[])this.biases[i2].clone();
        }

        return new MixtureDensityNetwork(
            this.inputSize,
            this.outputSize,
            this.mixtureCount,
            (int[])this.layerSizes.clone(),
            floatses,
            floatses2,
            (float[])this.headWeights.clone(),
            (float[])this.headBiases.clone()
        );
    }

    public void save(File file) throws IOException {
        File file2 = file.getParentFile();
        if (file2 != null) {
            file2.mkdirs();
        }

        try (DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            dataOutputStream.writeInt(1397640753);
            dataOutputStream.writeInt(this.inputSize);
            dataOutputStream.writeInt(this.outputSize);
            dataOutputStream.writeInt(this.mixtureCount);
            dataOutputStream.writeInt(this.layerSizes.length);
            int[] ints = this.layerSizes;

            for (int i : ints) {
                dataOutputStream.writeInt(i);
            }

            int i2 = 0;

            for (int i3 = ints.length - 1; i2 < i3; i2++) {
                for (float f : this.weights[i2]) {
                    dataOutputStream.writeFloat(f);
                }

                for (float f2 : this.biases[i2]) {
                    dataOutputStream.writeFloat(f2);
                }
            }

            for (float f3 : this.headWeights) {
                dataOutputStream.writeFloat(f3);
            }

            for (float f4 : this.headBiases) {
                dataOutputStream.writeFloat(f4);
            }
        }
    }

    public static MixtureDensityNetwork load(File file) throws IOException {
        return load(new FileInputStream(file));
    }

    public static MixtureDensityNetwork load(InputStream inputStream) throws IOException {
        try (DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(inputStream))) {
            if (dataInputStream.readInt() != 1397640753) {
                throw new IOException("model.incompatible");
            }

            int i = dataInputStream.readInt();
            int i2 = dataInputStream.readInt();
            int i3 = dataInputStream.readInt();
            int i4 = dataInputStream.readInt();
            if (i < 1 || i > 4096 || i2 < 1 || i2 > 16 || i3 < 1 || i3 > 64 || i4 < 2 || i4 > 16) {
                throw new IOException("model.incompatible");
            }

            int[] ints = new int[i4];

            for (int i5 = 0; i5 < i4; i5++) {
                ints[i5] = dataInputStream.readInt();
                if (ints[i5] < 1 || ints[i5] > 8192) {
                    throw new IOException("model.incompatible");
                }
            }

            if (ints[0] != i) {
                throw new IOException("model.incompatible");
            }

            int i6 = i4 - 1;
            float[][] floatses = new float[i6][];
            float[][] floatses2 = new float[i6][];

            for (int i7 = 0; i7 < i6; i7++) {
                floatses[i7] = new float[ints[i7 + 1] * ints[i7]];
                floatses2[i7] = new float[ints[i7 + 1]];
                int i8 = 0;

                for (int i9 = floatses[i7].length; i8 < i9; i8++) {
                    floatses[i7][i8] = dataInputStream.readFloat();
                }

                int i10 = 0;

                for (int i11 = floatses2[i7].length; i10 < i11; i10++) {
                    floatses2[i7][i10] = dataInputStream.readFloat();
                }
            }

            int i12 = i3 * (1 + 2 * i2);
            float[] floats = new float[i12 * ints[i4 - 1]];
            float[] floats2 = new float[i12];
            int i13 = 0;

            for (int i14 = floats.length; i13 < i14; i13++) {
                floats[i13] = dataInputStream.readFloat();
            }

            for (int i15 = 0; i15 < i12; i15++) {
                floats2[i15] = dataInputStream.readFloat();
            }

            return new MixtureDensityNetwork(i, i2, i3, ints, floatses, floatses2, floats, floats2);
        }
    }

    public static final class Prediction {
        public final float[] weights;
        public final float[] means;
        public final float[] sigmas;

        Prediction(float[] floats, float[] floats2, float[] floats3) {
            this.weights = floats;
            this.means = floats2;
            this.sigmas = floats3;
        }
    }

    final class Workspace {
        final float[][] activations = new float[MixtureDensityNetwork.this.layerSizes.length][];
        final float[][] preActivations = new float[MixtureDensityNetwork.this.layerSizes.length - 1][];
        final float[][] weightGrads = new float[MixtureDensityNetwork.this.layerSizes.length - 1][];
        final float[][] biasGrads = new float[MixtureDensityNetwork.this.layerSizes.length - 1][];
        final float[] headWeightGrads = new float[MixtureDensityNetwork.this.headSize * MixtureDensityNetwork.this.lastHiddenSize];
        final float[] headBiasGrads = new float[MixtureDensityNetwork.this.headSize];
        final float[] headOutput = new float[MixtureDensityNetwork.this.headSize];
        final float[] mixWeights = new float[MixtureDensityNetwork.this.mixtureCount];
        final float[] means = new float[MixtureDensityNetwork.this.mixtureCount * MixtureDensityNetwork.this.outputSize];
        final float[] sigmas = new float[MixtureDensityNetwork.this.mixtureCount * MixtureDensityNetwork.this.outputSize];
        final float[] logComponents = new float[MixtureDensityNetwork.this.mixtureCount];
        final float[] headGrads = new float[MixtureDensityNetwork.this.headSize];
        final float[][] backpropBuffers = new float[2][MixtureDensityNetwork.this.maxLayerSize];
        final float[] deltaBuffer = new float[MixtureDensityNetwork.this.maxLayerSize];
        float loss;
        float weightSum;

        Workspace() {
            int i = 0;

            for (int i2 = MixtureDensityNetwork.this.layerSizes.length - 1; i < i2; i++) {
                this.activations[i + 1] = new float[MixtureDensityNetwork.this.layerSizes[i + 1]];
                this.preActivations[i] = new float[MixtureDensityNetwork.this.layerSizes[i + 1]];
                this.weightGrads[i] = new float[MixtureDensityNetwork.this.layerSizes[i + 1] * MixtureDensityNetwork.this.layerSizes[i]];
                this.biasGrads[i] = new float[MixtureDensityNetwork.this.layerSizes[i + 1]];
            }
        }

        void clearGradients() {
            int i = 0;

            for (int i2 = MixtureDensityNetwork.this.layerSizes.length - 1; i < i2; i++) {
                Arrays.fill(this.weightGrads[i], 0.0F);
                Arrays.fill(this.biasGrads[i], 0.0F);
            }

            Arrays.fill(this.headWeightGrads, 0.0F);
            Arrays.fill(this.headBiasGrads, 0.0F);
            this.loss = 0.0F;
            this.weightSum = 0.0F;
        }
    }
}
