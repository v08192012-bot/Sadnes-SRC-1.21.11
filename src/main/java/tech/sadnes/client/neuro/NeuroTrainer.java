package tech.sadnes.client.neuro;

import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NeuroTrainer {
    private static final Logger LOGGER = LoggerFactory.getLogger("NeuroAim");
    private final NeuroStorage storage;
    private volatile boolean training;
    private volatile boolean cancelled;
    private volatile float progress;
    private volatile float loss;
    private volatile long etaMillis;
    private volatile String error;

    public NeuroTrainer(NeuroStorage neuroStorage) {
        this.storage = neuroStorage;
    }

    public void cancel() {
        if (this.training) {
            this.cancelled = true;
        }
    }

    public void startTraining(String string, int i, String string2, Consumer<MixtureDensityNetwork> consumer) {
        if (!this.training) {
            this.training = true;
            this.cancelled = false;
            this.progress = 0.0F;
            this.loss = 0.0F;
            this.etaMillis = 0L;
            this.error = null;
            Thread thread = new Thread(
                () -> {
                    long l = System.currentTimeMillis();

                    try {
                        NeuroDataset neuroDataset = NeuroDataset.load(this.storage.getDatasetFile(string));
                        if (neuroDataset.featureSize != 11 || neuroDataset.labelSize != 2) {
                            this.error = "neuro.error.datasetFormat";
                            return;
                        }

                        if (neuroDataset.getSampleCount() < 200) {
                            this.error = "neuro.error.datasetSmall";
                            return;
                        }

                        int i2 = MathHelper.clamp(i, 10, 2000);
                        int i3 = neuroDataset.getSessionCount();
                        boolean flag = i3 >= 2;
                        boolean[] booleans = new boolean[i3];
                        boolean[] booleans2 = new boolean[i3];
                        Random random = new Random(2026L);
                        int i4 = 0;

                        for (int i5 = 0; i5 < i3; i5++) {
                            boolean flag2 = flag && random.nextFloat() < 0.12F;
                            booleans2[i5] = flag2;
                            booleans[i5] = !flag2;
                            if (flag2) {
                                i4++;
                            }
                        }

                        if (flag && i4 == 0) {
                            int i6 = random.nextInt(i3);
                            booleans2[i6] = true;
                            booleans[i6] = false;
                        }

                        NeuroDataset.Samples samples = neuroDataset.buildSamples(5, booleans);
                        if (samples.size() < 100) {
                            this.error = "neuro.error.datasetEmpty";
                            return;
                        }

                        NeuroDataset.Samples samples2 = subsample(flag ? neuroDataset.buildSamples(5, booleans2) : samples, 4096);
                        LOGGER.info(
                            "Neuro train: {} train / {} val samples ({} epochs, {} threads)",
                            samples.size(),
                            samples2.size(),
                            i2,
                            MixtureDensityNetwork.getThreadCount()
                        );
                        MixtureDensityNetwork mixtureDensityNetwork = this.train(new MixtureDensityNetwork(55, 2, 6, 80, 56), samples, samples2, i2, l);
                        if (!this.cancelled) {
                            mixtureDensityNetwork.save(this.storage.getModelFile(string2));
                            this.progress = 1.0F;
                            if (consumer != null) {
                                consumer.accept(mixtureDensityNetwork);
                            }

                            LOGGER.info("Neuro train done in {}ms, loss {}", System.currentTimeMillis() - l, this.loss);
                            return;
                        }
                    } catch (Exception exception) {
                        LOGGER.error("Training failed", exception);
                        this.error = "neuro.error.generic";
                        return;
                    } finally {
                        this.training = false;
                    }
                },
                "Neuro-Training"
            );
            thread.setDaemon(true);
            thread.start();
        }
    }

    private MixtureDensityNetwork train(MixtureDensityNetwork mixtureDensityNetwork, NeuroDataset.Samples samples, NeuroDataset.Samples samples2, int i, long l) {
        float[][] floatses = samples.inputs;
        float[][] floatses2 = scaleTargets(samples.targets);
        float[] floats = samples.weights;
        float[][] floatses3 = samples2.inputs;
        float[][] floatses4 = scaleTargets(samples2.targets);
        float[] floats2 = samples2.weights;
        int i2 = Math.max(25, i / 5);
        int i3 = floatses.length;
        int i4 = i3 * 2;
        float[][] floatses5 = new float[i4][];
        float[][] floatses6 = new float[i4][];
        float[] floats3 = new float[i4];

        for (int i5 = 0; i5 < i3; i5++) {
            float f = 1.0F + Math.min(Math.abs(floatses2[i5][1]) * 1.25F, 1.5F);
            float f2 = floatses[i5][47] * 6.0F;
            float f3 = 1.0F + Math.max(0.0F, 1.0F - f2 / 2.0F);
            floatses5[i5] = floatses[i5];
            floatses6[i5] = floatses2[i5];
            floats3[i5] = floats[i5] * f * f3;
            floatses5[i3 + i5] = NeuroFeatures.mirrorFeatures(floatses[i5]);
            floatses6[i3 + i5] = new float[]{-floatses2[i5][0], floatses2[i5][1]};
            floats3[i3 + i5] = floats[i5] * f * f3 * 0.5F;
        }

        int[] ints = new int[i4];
        int i6 = 0;

        while (i6 < i4) {
            ints[i6] = i6++;
        }

        float[][] floatses7 = new float[128][];
        float[][] floatses8 = new float[128][];
        float[] floats4 = new float[128];
        Random random = new Random(123L);
        MixtureDensityNetwork mixtureDensityNetwork2 = null;
        float f4 = Float.MAX_VALUE;
        int i7 = 0;

        for (int i8 = 0; i8 < i && !this.cancelled; i8++) {
            float f5 = 3.0E-4F + 0.5F * (0.003F - 3.0E-4F) * (1.0F + (float)Math.cos(Math.PI * i8 / i));

            for (int i9 = i4 - 1; i9 > 0; i9--) {
                int i10 = random.nextInt(i9 + 1);
                int i11 = ints[i9];
                ints[i9] = ints[i10];
                ints[i10] = i11;
            }

            for (short s = 0; s < i4; s += 128) {
                int i12 = Math.min(128, i4 - s);

                for (int i13 = 0; i13 < i12; i13++) {
                    int i14 = ints[s + i13];
                    floatses7[i13] = floatses5[i14];
                    floatses8[i13] = floatses6[i14];
                    floats4[i13] = floats3[i14];
                }

                mixtureDensityNetwork.trainBatch(floatses7, floatses8, floats4, i12, f5);
            }

            float f6 = mixtureDensityNetwork.evaluate(floatses3, floatses4, floats2);
            this.loss = f6;
            float f7 = (float)(i8 + 1) / i;
            this.progress = f7;
            this.etaMillis = (long)((float)(System.currentTimeMillis() - l) * (1.0F - f7) / f7);
            if (f6 < f4) {
                f4 = f6;
                mixtureDensityNetwork2 = mixtureDensityNetwork.copy();
                i7 = 0;
            } else if (++i7 >= i2) {
                break;
            }
        }

        return mixtureDensityNetwork2 != null ? mixtureDensityNetwork2 : mixtureDensityNetwork;
    }

    private static NeuroDataset.Samples subsample(NeuroDataset.Samples samples, int i) {
        int i2 = samples.size();
        if (i2 <= i) {
            return samples;
        }

        float[][] floatses = new float[i][];
        float[][] floatses2 = new float[i][];
        float[] floats = new float[i];

        for (int i3 = 0; i3 < i; i3++) {
            int i4 = (int)((long)i3 * i2 / i);
            floatses[i3] = samples.inputs[i4];
            floatses2[i3] = samples.targets[i4];
            floats[i3] = samples.weights[i4];
        }

        return new NeuroDataset.Samples(floatses, floatses2, floats);
    }

    private static float[][] scaleTargets(float[][] floatses) {
        float[][] floatses2 = new float[floatses.length][2];
        int i = 0;

        for (int i2 = floatses.length; i < i2; i++) {
            floatses2[i][0] = floatses[i][0] / 10.0F;
            floatses2[i][1] = floatses[i][1] / 10.0F;
        }

        return floatses2;
    }

    public NeuroStorage getStorage() {
        return this.storage;
    }

    public boolean isTraining() {
        return this.training;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public float getProgress() {
        return this.progress;
    }

    public float getLoss() {
        return this.loss;
    }

    public long getEtaMillis() {
        return this.etaMillis;
    }

    public String getError() {
        return this.error;
    }
}
