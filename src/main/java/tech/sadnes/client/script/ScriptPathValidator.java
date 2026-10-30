package tech.sadnes.client.script;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import tech.sadnes.libs.rhino.Context;

public final class ScriptPathValidator {
    private ScriptPathValidator() {
    }

    public static Path resolveSafe(Path path, String string) {
        String string2 = string == null ? "" : string;
        if (string2.indexOf(58) < 0 && !string2.startsWith("/") && !string2.startsWith("\\")) {
            Path path2 = path.toAbsolutePath().normalize();
            Path path3 = path2.resolve(string2.replace('\\', '/')).normalize();
            if (!path3.startsWith(path2)) {
                throw Context.reportRuntimeError("script path is outside its directory");
            }

            try {
                Path path4 = path2;
                checkNotLinked(path2);

                for (Path path5 : path2.relativize(path3)) {
                    path4 = path4.resolve(path5);
                    checkNotLinked(path4);
                }

                return path3;
            } catch (IOException ioException) {
                throw Context.reportRuntimeError("cannot validate script path: " + ioException.getMessage());
            }
        } else {
            throw Context.reportRuntimeError("script path must be relative");
        }
    }

    private static void checkNotLinked(Path path) throws IOException {
        if (Files.isSymbolicLink(path)) {
            throw new IOException("linked paths are not allowed");
        }

        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            Path path2 = path.getParent();
            if (path2 != null && !path.toRealPath().equals(path2.toRealPath().resolve(path.getFileName()))) {
                throw new IOException("redirected paths are not allowed");
            }
        }
    }
}
