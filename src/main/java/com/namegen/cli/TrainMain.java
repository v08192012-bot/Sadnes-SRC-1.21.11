package com.namegen.cli;

import com.namegen.CharMLP;
import com.namegen.Dataset;
import com.namegen.ModelIO;
import com.namegen.Trainer;
import com.namegen.Vocabulary;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class TrainMain {
    private static final String DEFAULT_OUT = "src/main/resources/com/namegen/namegen-model.bin";

    public static void main(String[] strings) throws Exception {
        if (strings.length < 1) {
            System.err.println("Usage: TrainMain <dbFile> [outFile] [maxNames]");
            System.exit(2);
        }

        Path path = Path.of(strings[0]);
        Path path2 = Path.of(strings.length >= 2 && !strings[1].contains("=") ? strings[1] : "src/main/resources/com/namegen/namegen-model.bin");
        Trainer.Config config = new Trainer.Config();
        int i = 150000;
        boolean flag = true;

        for (String string : strings) {
            int i2 = string.indexOf(61);
            if (i2 >= 0) {
                String string2;
                label92: {
                    label91: {
                        label90: {
                            label89: {
                                label88: {
                                    label87: {
                                        label86: {
                                            label85: {
                                                label84: {
                                                    String string3 = string.substring(0, i2);
                                                    string2 = string.substring(i2 + 1);
                                                    byte b = -1;
                                                    switch (string3.hashCode()) {
                                                        case -1296583414:
                                                            if (string3.equals("epochs")) {
                                                                break label88;
                                                            }
                                                            break;
                                                        case -1217487446:
                                                            if (string3.equals("hidden")) {
                                                                break label89;
                                                            }
                                                            break;
                                                        case 3462:
                                                            if (string3.equals("lr")) {
                                                                break label86;
                                                            }
                                                            break;
                                                        case 100538:
                                                            if (string3.equals("emb")) {
                                                                break label90;
                                                            }
                                                            break;
                                                        case 3526257:
                                                            if (string3.equals("seed")) {
                                                                break label84;
                                                            }
                                                            break;
                                                        case 93509434:
                                                            if (string3.equals("batch")) {
                                                                break label87;
                                                            }
                                                            break;
                                                        case 93832333:
                                                            if (string3.equals("block")) {
                                                                break label91;
                                                            }
                                                            break;
                                                        case 104585032:
                                                            if (string3.equals("names")) {
                                                                break label92;
                                                            }
                                                            break;
                                                        case 397756304:
                                                            if (string3.equals("lrFinal")) {
                                                                break label85;
                                                            }
                                                            break;
                                                        case 1840341186:
                                                            if (string3.equals("nameLike")) {
                                                                b = 9;
                                                            }
                                                    }

                                                    switch (b) {
                                                        case 0:
                                                            break label92;
                                                        case 1:
                                                            break label91;
                                                        case 2:
                                                            break label90;
                                                        case 3:
                                                            break label89;
                                                        case 4:
                                                            break label88;
                                                        case 5:
                                                            break label87;
                                                        case 6:
                                                            break label86;
                                                        case 7:
                                                            break label85;
                                                        case 8:
                                                            break;
                                                        case 9:
                                                            flag = Boolean.parseBoolean(string2);
                                                            continue;
                                                        default:
                                                            System.err.println("Unknown option: " + string3);
                                                            continue;
                                                    }
                                                }

                                                config.seed = Long.parseLong(string2);
                                                continue;
                                            }

                                            config.lrFinal = Double.parseDouble(string2);
                                            continue;
                                        }

                                        config.lr = Double.parseDouble(string2);
                                        continue;
                                    }

                                    config.batchSize = Integer.parseInt(string2);
                                    continue;
                                }

                                config.epochs = Integer.parseInt(string2);
                                continue;
                            }

                            config.hidden = Integer.parseInt(string2);
                            continue;
                        }

                        config.embDim = Integer.parseInt(string2);
                        continue;
                    }

                    config.blockSize = Integer.parseInt(string2);
                    continue;
                }

                i = Integer.parseInt(string2);
            }
        }

        long l = System.currentTimeMillis();
        System.out.printf("Loading logins from %s (subsampling up to %d)...%n", path, i);
        List list = Dataset.loadLogins(path, config.blockSize + 1, 20, i, flag, config.seed);
        System.out.printf("Loaded %d distinct logins in %.1fs%n", list.size(), (System.currentTimeMillis() - l) / 1000.0);
        Vocabulary vocabulary = Vocabulary.fromNames(list);
        Dataset dataset = Dataset.build(list, config.blockSize, vocabulary);
        long l2 = System.currentTimeMillis();
        CharMLP charMLP = Trainer.train(dataset, config, System.out::println);
        System.out.printf("Trained in %.1fs%n", (System.currentTimeMillis() - l2) / 1000.0);
        Files.createDirectories(path2.toAbsolutePath().getParent());
        ModelIO.save(charMLP, vocabulary, path2);
        System.out.printf("Saved model -> %s (%d bytes)%n", path2, Files.size(path2));
    }

    private TrainMain() {
    }
}
