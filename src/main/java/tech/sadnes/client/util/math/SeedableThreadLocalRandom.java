package tech.sadnes.client.util.math;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SeedableThreadLocalRandom extends Random {
    private final ThreadLocalRandom threadRandom = ThreadLocalRandom.current();
    private Random seededRandom = null;
    private volatile boolean seeded;
    private volatile boolean seedDirty;
    private volatile long seed;

    private void ensureSeeded() {
        if (this.seededRandom == null) {
            this.seededRandom = new Random(this.seed);
            this.seedDirty = false;
        } else {
            if (this.seedDirty) {
                this.seededRandom.setSeed(this.seed);
                this.seedDirty = false;
            }
        }
    }

    @Override
    public void setSeed(long l) {
        this.seed = l;
        this.seeded = true;
        this.seedDirty = true;
    }

    @Override
    public void nextBytes(byte[] bytes) {
        if (this.seeded) {
            this.ensureSeeded();
            this.seededRandom.nextBytes(bytes);
        } else {
            this.threadRandom.nextBytes(bytes);
        }
    }

    @Override
    public int nextInt() {
        if (this.seeded) {
            this.ensureSeeded();
            return this.seededRandom.nextInt();
        } else {
            return this.threadRandom.nextInt();
        }
    }

    @Override
    public int nextInt(int i) {
        if (this.seeded) {
            this.ensureSeeded();
            return this.seededRandom.nextInt(i);
        } else {
            return this.threadRandom.nextInt(i);
        }
    }

    @Override
    public long nextLong() {
        if (this.seeded) {
            this.ensureSeeded();
            return this.seededRandom.nextLong();
        } else {
            return this.threadRandom.nextLong();
        }
    }

    @Override
    public boolean nextBoolean() {
        if (this.seeded) {
            this.ensureSeeded();
            return this.seededRandom.nextBoolean();
        } else {
            return this.threadRandom.nextBoolean();
        }
    }

    @Override
    public float nextFloat() {
        if (this.seeded) {
            this.ensureSeeded();
            return this.seededRandom.nextFloat();
        } else {
            return this.threadRandom.nextFloat();
        }
    }

    @Override
    public double nextDouble() {
        if (this.seeded) {
            this.ensureSeeded();
            return this.seededRandom.nextDouble();
        } else {
            return this.threadRandom.nextDouble();
        }
    }

    @Override
    public double nextGaussian() {
        if (this.seeded) {
            this.ensureSeeded();
            return this.seededRandom.nextGaussian();
        } else {
            return this.threadRandom.nextGaussian();
        }
    }
}
