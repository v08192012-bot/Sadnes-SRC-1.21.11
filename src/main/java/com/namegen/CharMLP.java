package com.namegen;

import java.util.Arrays;
import java.util.Random;

public final class CharMLP {
    final int vocab;
    final int blockSize;
    final int embDim;
    final int hidden;
    final int inDim;
    final double[] c;
    final double[] w1;
    final double[] b1;
    final double[] w2;
    final double[] b2;
    private final double[] gc;
    private final double[] gw1;
    private final double[] gb1;
    private final double[] gw2;
    private final double[] gb2;

    public CharMLP(int i, int i2, int i3, int i4, long l) {
        this.vocab = i;
        this.blockSize = i2;
        this.embDim = i3;
        this.hidden = i4;
        this.inDim = i2 * i3;
        this.c = new double[i * i3];
        this.w1 = new double[this.inDim * i4];
        this.b1 = new double[i4];
        this.w2 = new double[i4 * i];
        this.b2 = new double[i];
        this.gc = new double[this.c.length];
        this.gw1 = new double[this.w1.length];
        this.gb1 = new double[this.b1.length];
        this.gw2 = new double[this.w2.length];
        this.gb2 = new double[this.b2.length];
        this.initWeights(l);
    }

    CharMLP(int i, int i2, int i3, int i4, double[] doubles, double[] doubles2, double[] doubles3, double[] doubles4, double[] doubles5) {
        this.vocab = i;
        this.blockSize = i2;
        this.embDim = i3;
        this.hidden = i4;
        this.inDim = i2 * i3;
        this.c = doubles;
        this.w1 = doubles2;
        this.b1 = doubles3;
        this.w2 = doubles4;
        this.b2 = doubles5;
        this.gc = new double[doubles.length];
        this.gw1 = new double[doubles2.length];
        this.gb1 = new double[doubles3.length];
        this.gw2 = new double[doubles4.length];
        this.gb2 = new double[doubles5.length];
    }

    private void initWeights(long l) {
        Random random = new Random(l);
        double d = Math.sqrt(2.0 / this.inDim);

        for (int i = 0; i < this.c.length; i++) {
            this.c[i] = random.nextGaussian() * 0.1;
        }

        for (int i2 = 0; i2 < this.w1.length; i2++) {
            this.w1[i2] = random.nextGaussian() * d;
        }

        for (int i3 = 0; i3 < this.w2.length; i3++) {
            this.w2[i3] = random.nextGaussian() * 0.01;
        }
    }

    double trainStep(int[] ints, int[] ints2, int[] ints3, int i, int i2, double d) {
        double d2 = this.computeGrads(ints, ints2, ints3, i, i2);
        this.applyGrads(d / (i2 - i));
        return d2;
    }

    double computeGrads(int[] ints, int[] ints2, int[] ints3, int i, int i2) {
        zero(this.gc);
        zero(this.gw1);
        zero(this.gb1);
        zero(this.gw2);
        zero(this.gb2);
        int i3 = i2 - i;
        double d = 0.0;
        double[] doubles = new double[this.inDim];
        double[] doubles2 = new double[this.hidden];
        double[] doubles3 = new double[this.hidden];
        double[] doubles4 = new double[this.vocab];
        double[] doubles5 = new double[this.vocab];
        double[] doubles6 = new double[this.hidden];
        double[] doubles7 = new double[this.inDim];

        for (int i4 = i; i4 < i2; i4++) {
            int i5 = ints3[i4];
            int i6 = i5 * this.blockSize;

            for (int i7 = 0; i7 < this.blockSize; i7++) {
                int i8 = ints[i6 + i7] * this.embDim;
                System.arraycopy(this.c, i8, doubles, i7 * this.embDim, this.embDim);
            }

            for (int i9 = 0; i9 < this.hidden; i9++) {
                double d2 = this.b1[i9];

                for (int i10 = 0; i10 < this.inDim; i10++) {
                    d2 += doubles[i10] * this.w1[i10 * this.hidden + i9];
                }

                doubles2[i9] = d2;
                doubles3[i9] = Math.tanh(d2);
            }

            double d3 = Double.NEGATIVE_INFINITY;

            for (int i11 = 0; i11 < this.vocab; i11++) {
                double d4 = this.b2[i11];

                for (int i12 = 0; i12 < this.hidden; i12++) {
                    d4 += doubles3[i12] * this.w2[i12 * this.vocab + i11];
                }

                doubles4[i11] = d4;
                if (d4 > d3) {
                    d3 = d4;
                }
            }

            double d5 = 0.0;

            for (int i13 = 0; i13 < this.vocab; i13++) {
                double d6 = Math.exp(doubles4[i13] - d3);
                doubles5[i13] = d6;
                d5 += d6;
            }

            double d7 = 1.0 / d5;

            for (int i14 = 0; i14 < this.vocab; i14++) {
                doubles5[i14] *= d7;
            }

            int i15 = ints2[i5];
            d += -Math.log(Math.max(doubles5[i15], 1.0E-12));
            Arrays.fill(doubles6, 0.0);

            for (int i16 = 0; i16 < this.vocab; i16++) {
                double d8 = doubles5[i16];
                if (i16 == i15) {
                    d8--;
                }

                this.gb2[i16] = this.gb2[i16] + d8;

                for (int i17 = 0; i17 < this.hidden; i17++) {
                    this.gw2[i17 * this.vocab + i16] = this.gw2[i17 * this.vocab + i16] + doubles3[i17] * d8;
                    doubles6[i17] += d8 * this.w2[i17 * this.vocab + i16];
                }
            }

            Arrays.fill(doubles7, 0.0);

            for (int i18 = 0; i18 < this.hidden; i18++) {
                double d9 = doubles6[i18] * (1.0 - doubles3[i18] * doubles3[i18]);
                this.gb1[i18] = this.gb1[i18] + d9;

                for (int i19 = 0; i19 < this.inDim; i19++) {
                    this.gw1[i19 * this.hidden + i18] = this.gw1[i19 * this.hidden + i18] + doubles[i19] * d9;
                    doubles7[i19] += d9 * this.w1[i19 * this.hidden + i18];
                }
            }

            for (int i20 = 0; i20 < this.blockSize; i20++) {
                int i21 = ints[i6 + i20] * this.embDim;
                int i22 = i20 * this.embDim;

                for (int i23 = 0; i23 < this.embDim; i23++) {
                    this.gc[i21 + i23] = this.gc[i21 + i23] + doubles7[i22 + i23];
                }
            }
        }

        return d / i3;
    }

    void applyGrads(double d) {
        for (int i = 0; i < this.c.length; i++) {
            this.c[i] = this.c[i] - d * this.gc[i];
        }

        for (int i2 = 0; i2 < this.w1.length; i2++) {
            this.w1[i2] = this.w1[i2] - d * this.gw1[i2];
        }

        for (int i3 = 0; i3 < this.b1.length; i3++) {
            this.b1[i3] = this.b1[i3] - d * this.gb1[i3];
        }

        for (int i4 = 0; i4 < this.w2.length; i4++) {
            this.w2[i4] = this.w2[i4] - d * this.gw2[i4];
        }

        for (int i5 = 0; i5 < this.b2.length; i5++) {
            this.b2[i5] = this.b2[i5] - d * this.gb2[i5];
        }
    }

    void nextDistribution(int[] ints, double d, double[] doubles) {
        double[] doubles2 = new double[this.inDim];

        for (int i = 0; i < this.blockSize; i++) {
            int i2 = ints[i] * this.embDim;
            System.arraycopy(this.c, i2, doubles2, i * this.embDim, this.embDim);
        }

        double[] doubles3 = new double[this.hidden];

        for (int i3 = 0; i3 < this.hidden; i3++) {
            double d2 = this.b1[i3];

            for (int i4 = 0; i4 < this.inDim; i4++) {
                d2 += doubles2[i4] * this.w1[i4 * this.hidden + i3];
            }

            doubles3[i3] = Math.tanh(d2);
        }

        double d3 = 1.0 / Math.max(d, 1.0E-6);
        double d4 = Double.NEGATIVE_INFINITY;

        for (int i5 = 0; i5 < this.vocab; i5++) {
            double d5 = this.b2[i5];

            for (int i6 = 0; i6 < this.hidden; i6++) {
                d5 += doubles3[i6] * this.w2[i6 * this.vocab + i5];
            }

            double d6 = d5 * d3;
            doubles[i5] = d6;
            if (d6 > d4) {
                d4 = d6;
            }
        }

        double d7 = 0.0;

        for (int i7 = 0; i7 < this.vocab; i7++) {
            double d8 = Math.exp(doubles[i7] - d4);
            doubles[i7] = d8;
            d7 += d8;
        }

        double d9 = 1.0 / d7;

        for (int i8 = 0; i8 < this.vocab; i8++) {
            doubles[i8] *= d9;
        }
    }

    double evaluate(int[] ints, int[] ints2, int i) {
        double[] doubles = new double[this.vocab];
        int[] ints3 = new int[this.blockSize];
        double d = 0.0;
        int i2 = Math.min(i, ints2.length);

        for (int i3 = 0; i3 < i2; i3++) {
            int i4 = i3 * this.blockSize;

            for (int i5 = 0; i5 < this.blockSize; i5++) {
                ints3[i5] = ints[i4 + i5];
            }

            this.nextDistribution(ints3, 1.0, doubles);
            d += -Math.log(Math.max(doubles[ints2[i3]], 1.0E-12));
        }

        return d / i2;
    }

    long paramCount() {
        return (long)this.c.length + this.w1.length + this.b1.length + this.w2.length + this.b2.length;
    }

    double[] gradSnapshot(int i) {
        switch (i) {
            case 0:
                return (double[])this.gc.clone();
            case 1:
                return (double[])this.gw1.clone();
            case 2:
                return (double[])this.gb1.clone();
            case 3:
                return (double[])this.gw2.clone();
            case 4:
                return (double[])this.gb2.clone();
            default:
                throw new IllegalArgumentException("bad buffer id: " + i);
        }
    }

    private static void zero(double[] doubles) {
        Arrays.fill(doubles, 0.0);
    }
}
