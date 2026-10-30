package tech.sadnes.client.util.media;

import com.sun.jna.Native;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import tech.sadnes.client.util.crypto.AssetCrypto;

public final class OptMediaNative {
    private static final String DLL_PATH = "/assets/sadnes/natives/OptMedia.dll";
    private static boolean loaded;
    public static OptMediaLibrary library;

    private OptMediaNative() {
    }

    public static synchronized void load() {
        if (!loaded) {
            try {
                InputStream inputStream = AssetCrypto.wrap(OptMediaNative.class.getResourceAsStream("/assets/sadnes/natives/OptMedia.dll"));

                try {
                    if (inputStream == null) {
                        throw new IllegalStateException("OptMedia.dll not found at /assets/sadnes/natives/OptMedia.dll");
                    }

                    Path path = Files.createTempFile("OptMedia", ".dll");
                    path.toFile().deleteOnExit();
                    Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
                    HashMap hashMap = new HashMap();
                    hashMap.put("allow-objects", Boolean.TRUE);
                    library = Native.load(path.toAbsolutePath().toString(), OptMediaLibrary.class, hashMap);
                    loaded = true;
                } catch (Throwable throwable) {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }

                    throw throwable;
                }

                inputStream.close();
            } catch (IOException ioException) {
                throw new RuntimeException("Failed to load native OptMedia.dll", ioException);
            }
        }
    }
}
