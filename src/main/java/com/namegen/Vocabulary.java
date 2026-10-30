package com.namegen;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public final class Vocabulary {
    public static final char BOUNDARY = '.';
    private final char[] itos;
    private final int[] stoi;

    private Vocabulary(char[] chars) {
        this.itos = chars;
        this.stoi = new int[128];
        Arrays.fill(this.stoi, -1);
        int i = 0;

        while (i < chars.length) {
            this.stoi[chars[i]] = i++;
        }
    }

    public static Vocabulary fromNames(Iterable<String> strings) {
        TreeSet treeSet = new TreeSet();

        for (String string : strings) {
            for (int i = 0; i < string.length(); i++) {
                treeSet.add(string.charAt(i));
            }
        }

        return fromChars(treeSet);
    }

    public static Vocabulary fromChars(Set<Character> characters) {
        TreeSet<Character> characters2 = new TreeSet<>(characters);
        characters2.remove('.');
        char[] chars = new char[characters2.size() + 1];
        chars[0] = '.';
        int i = 1;

        for (char c : characters2) {
            chars[i++] = c;
        }

        return new Vocabulary(chars);
    }

    public static Vocabulary fromTable(char[] chars) {
        return new Vocabulary((char[])chars.clone());
    }

    public int size() {
        return this.itos.length;
    }

    public int boundaryIndex() {
        return 0;
    }

    public int indexOf(char c) {
        return c < 128 ? this.stoi[c] : -1;
    }

    public char charOf(int i) {
        return this.itos[i];
    }

    public char[] table() {
        return (char[])this.itos.clone();
    }
}
