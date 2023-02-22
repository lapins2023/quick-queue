package io.github.lapins2023.quickqueue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileUtils {
    public static void clean(File dir) throws IOException {
        Files.delete(dir.toPath());
        dir.mkdirs();
    }
}
