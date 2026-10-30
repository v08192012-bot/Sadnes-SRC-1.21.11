package tech.sadnes.client.config;

import java.nio.file.Path;
import java.util.List;

public interface ConfigStorage {
    boolean write(String string, String string2);

    String read(String string);

    boolean delete(String string);

    List<String> list();

    boolean exists(String string);

    Path getDirectory();
}
