package com.namegen;

import java.util.Random;
import java.util.function.Consumer;

public final class Trainer {
    private Trainer() {
    }

    public static CharMLP train(Dataset dataset, Trainer.Config config, Consumer<String> consumer) {
        if (dataset.blockSize != config.blockSize) {
            throw new IllegalArgumentException("Dataset block size " + dataset.blockSize + " != config " + config.blockSize);
        }

        CharMLP charMLP = new CharMLP(dataset.vocab.size(), config.blockSize, config.embDim, config.hidden, config.seed);
        if (consumer != null) {
            consumer.accept(
                String.format(
                    "model: vocab=%d block=%d emb=%d hidden=%d params=%d | examples=%d",
                    dataset.vocab.size(),
                    config.blockSize,
                    config.embDim,
                    config.hidden,
                    charMLP.paramCount(),
                    dataset.count
                )
            );
        }

        int i = dataset.count;
        int[] ints = new int[i];
        int i2 = 0;

        while (i2 < i) {
            ints[i2] = i2++;
        }

        Random random = new Random(config.seed ^ -7046029254386353131L);
        int i3 = (i + config.batchSize - 1) / config.batchSize;
        long l = (long)i3 * config.epochs;
        long l2 = 0L;

        for (int i4 = 0; i4 < config.epochs; i4++) {
            shuffle(ints, random);
            double d = 0.0;
            int i5 = 0;

            for (int i6 = 0; i6 < i; i6 += config.batchSize) {
                int i7 = Math.min(i6 + config.batchSize, i);
                double d2 = config.lr + (config.lrFinal - config.lr) * ((double)l2 / Math.max(1L, l - 1L));
                d += charMLP.trainStep(dataset.x, dataset.y, ints, i6, i7, d2);
                i5++;
                l2++;
            }

            if (consumer != null) {
                consumer.accept(
                    String.format(
                        "epoch %d/%d  train_loss=%.4f  lr~%.4f",
                        i4 + 1,
                        config.epochs,
                        d / i5,
                        config.lr + (config.lrFinal - config.lr) * ((double)l2 / Math.max(1L, l - 1L))
                    )
                );
            }
        }

        return charMLP;
    }

    private static void shuffle(int[] ints, Random random) {
        for (int i = ints.length - 1; i > 0; i--) {
            int i2 = random.nextInt(i + 1);
            int i3 = ints[i];
            ints[i] = ints[i2];
            ints[i2] = i3;
        }
    }

    public static final class Config {
        public int blockSize = 3;
        public int embDim = 12;
        public int hidden = 96;
        public int epochs = 8;
        public int batchSize = 256;
        public double lr = 0.1;
        public double lrFinal = 0.01;
        public long seed = 1234L;
    }
}
