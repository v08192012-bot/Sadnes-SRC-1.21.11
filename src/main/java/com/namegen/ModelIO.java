package com.namegen;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModelIO {
    private static final int MAGIC = 1313293646;
    private static final int VERSION = 1;

    private ModelIO() {
    }

    public static void save(CharMLP charMLP, Vocabulary vocabulary, Path path) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            save(charMLP, vocabulary, outputStream);
        }
    }

    public static void save(CharMLP charMLP, Vocabulary vocabulary, OutputStream outputStream) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(outputStream));
        dataOutputStream.writeInt(1313293646);
        dataOutputStream.writeInt(1);
        dataOutputStream.writeInt(charMLP.vocab);
        dataOutputStream.writeInt(charMLP.blockSize);
        dataOutputStream.writeInt(charMLP.embDim);
        dataOutputStream.writeInt(charMLP.hidden);
        char[] chars = vocabulary.table();
        dataOutputStream.writeInt(chars.length);

        for (char c2 : chars) {
            dataOutputStream.writeChar(c2);
        }

        writeArray(dataOutputStream, charMLP.c);
        writeArray(dataOutputStream, charMLP.w1);
        writeArray(dataOutputStream, charMLP.b1);
        writeArray(dataOutputStream, charMLP.w2);
        writeArray(dataOutputStream, charMLP.b2);
        dataOutputStream.flush();
    }

    public static ModelIO.Bundle load(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return load(inputStream);
        }
    }

    public static ModelIO.Bundle load(InputStream inputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(inputStream));
        int i = dataInputStream.readInt();
        if (i != 1313293646) {
            throw new IOException("Not a namegen model file (bad magic: 0x" + Integer.toHexString(i) + ")");
        }

        int i2 = dataInputStream.readInt();
        if (i2 != 1) {
            throw new IOException("Unsupported model version: " + i2);
        }

        int i3 = dataInputStream.readInt();
        int i4 = dataInputStream.readInt();
        int i5 = dataInputStream.readInt();
        int i6 = dataInputStream.readInt();
        int i7 = dataInputStream.readInt();
        char[] chars = new char[i7];

        for (int i8 = 0; i8 < i7; i8++) {
            chars[i8] = dataInputStream.readChar();
        }

        Vocabulary vocabulary = Vocabulary.fromTable(chars);
        double[] doubles = readArray(dataInputStream);
        double[] doubles2 = readArray(dataInputStream);
        double[] doubles3 = readArray(dataInputStream);
        double[] doubles4 = readArray(dataInputStream);
        double[] doubles5 = readArray(dataInputStream);
        CharMLP charMLP = new CharMLP(i3, i4, i5, i6, doubles, doubles2, doubles3, doubles4, doubles5);
        return new ModelIO.Bundle(charMLP, vocabulary);
    }

    private static void writeArray(DataOutputStream dataOutputStream, double[] doubles) throws IOException {
        dataOutputStream.writeInt(doubles.length);

        for (double d : doubles) {
            dataOutputStream.writeDouble(d);
        }
    }

    private static double[] readArray(DataInputStream dataInputStream) throws IOException {
        int i = dataInputStream.readInt();
        double[] doubles = new double[i];

        for (int i2 = 0; i2 < i; i2++) {
            doubles[i2] = dataInputStream.readDouble();
        }

        return doubles;
    }

    public record Bundle(CharMLP model, Vocabulary vocab) {
    }
}
