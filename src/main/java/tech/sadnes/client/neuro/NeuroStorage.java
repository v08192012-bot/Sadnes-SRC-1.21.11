package tech.sadnes.client.neuro;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;

public final class NeuroStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger("NeuroAim");
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private final Object fileLock = new Object();
    private final ExecutorService autosaveExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Neuro-Autosave");
        thread.setDaemon(true);
        return thread;
    });

    private File getRootDir() {
        return MC.runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("neuro").toFile();
    }

    public File getModelsDir() {
        return new File(this.getRootDir(), "models");
    }

    public File getDatasetsDir() {
        return new File(this.getRootDir(), "datasets");
    }

    public File getDebugDir() {
        return new File(this.getRootDir(), "debug");
    }

    public File getStateFile() {
        return new File(this.getRootDir(), "state.json");
    }

    public String readState() {
        File file = this.getStateFile();
        if (!file.exists()) {
            return null;
        }

        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            return null;
        }
    }

    public void saveStateAsync(String string) {
        this.autosaveExecutor.execute(() -> {
            try {
                File file = this.getStateFile();
                File file2 = file.getParentFile();
                if (file2 != null) {
                    file2.mkdirs();
                }

                Files.writeString(file.toPath(), string, StandardCharsets.UTF_8);
            } catch (IOException ioException) {
                LOGGER.warn("Neuro state save failed: {}", ioException.getMessage());
            }
        });
    }

    public File getModelFile(String string) {
        return new File(this.getModelsDir(), string + ".smodel");
    }

    public File getDatasetFile(String string) {
        return new File(this.getDatasetsDir(), string + ".dataset");
    }

    public List<String> listModels() {
        return this.listNames(this.getModelsDir(), ".smodel");
    }

    public List<String> listDatasets() {
        return this.listNames(this.getDatasetsDir(), ".dataset");
    }

    private List<String> listNames(File file, String string) {
        ArrayList arrayList = new ArrayList();
        File[] files = file.listFiles((file2, string2) -> string2.endsWith(string));
        if (files == null) {
            return arrayList;
        }

        for (File file3 : files) {
            arrayList.add(file3.getName().substring(0, file3.getName().length() - string.length()));
        }

        return arrayList;
    }

    public boolean deleteModel(String string) {
        File file = this.getModelFile(string);
        return file.exists() && file.delete();
    }

    public boolean deleteDataset(String string) {
        File file = this.getDatasetFile(string);
        return file.exists() && file.delete();
    }

    public boolean renameModel(String string, String string2) {
        File file = this.getModelFile(string);
        File file2 = this.getModelFile(string2);
        return file.exists() && !file2.exists() && file.renameTo(file2);
    }

    public boolean renameDataset(String string, String string2) {
        File file = this.getDatasetFile(string);
        File file2 = this.getDatasetFile(string2);
        return file.exists() && !file2.exists() && file.renameTo(file2);
    }

    public NeuroDataset loadDataset(String string) {
        File file = this.getDatasetFile(string);
        if (file.exists()) {
            try {
                NeuroDataset neuroDataset;
                synchronized (this.fileLock) {
                    neuroDataset = NeuroDataset.load(file);
                }

                if (neuroDataset.featureSize == 11 && neuroDataset.labelSize == 2) {
                    return neuroDataset;
                }

                this.backupBrokenDataset(file);
            } catch (IOException ioException) {
                LOGGER.warn("Dataset load failed, starting fresh: {}", ioException.getMessage());
                this.backupBrokenDataset(file);
            }
        }

        return new NeuroDataset(string, 11, 2);
    }

    private void backupBrokenDataset(File file) {
        File file2 = new File(file.getParentFile(), file.getName() + ".bak");
        synchronized (this.fileLock) {
            if (file2.exists()) {
                file2.delete();
            }

            if (file.renameTo(file2)) {
                LOGGER.warn("Broken dataset moved to {}", file2.getName());
            }
        }
    }

    public int countDatasetSamples(String string) {
        File file = this.getDatasetFile(string);
        if (!file.exists()) {
            return 0;
        }

        try {
            synchronized (this.fileLock) {
                return NeuroDataset.countSamples(file);
            }
        } catch (IOException ioException) {
            return 0;
        }
    }

    public void saveDataset(NeuroDataset neuroDataset) {
        try {
            synchronized (this.fileLock) {
                File file = this.getDatasetFile(neuroDataset.getName());
                File file2 = new File(file.getParentFile(), neuroDataset.getName() + ".dataset.tmp");
                neuroDataset.save(file2);
                Files.move(file2.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ioException) {
            LOGGER.error("Dataset save failed", ioException);
        }
    }

    public void saveDatasetAsync(NeuroDataset neuroDataset) {
        this.autosaveExecutor.execute(() -> this.saveDataset(neuroDataset));
    }
}
