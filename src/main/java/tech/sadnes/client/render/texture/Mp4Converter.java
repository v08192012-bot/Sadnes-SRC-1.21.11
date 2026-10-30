package tech.sadnes.client.render.texture;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import tech.sadnes.client.Sadnes;

public final class Mp4Converter {
    private static final Set<String> IN_PROGRESS = ConcurrentHashMap.newKeySet();

    private Mp4Converter() {
    }

    public static Path getSpritesheet(Path path) {
        if (path != null && Files.exists(path)) {
            String string = path.toString();
            int i = string.lastIndexOf(46);
            if (i <= 0) {
                return null;
            }

            String string2 = string.substring(0, i);
            Path path2 = Path.of(string2 + "_sheet.png");
            if (Files.exists(path2)) {
                return path2;
            }

            if (IN_PROGRESS.add(string)) {
                Sadnes.getLogger().info("[Mp4Converter] Старт фоновой конвертации всех кадров MP4: {}", path);
                CompletableFuture.runAsync(() -> convert(path, path2)).thenRun(() -> {
                    IN_PROGRESS.remove(string);
                    Sadnes.getLogger().info("[Mp4Converter] Полная конвертация кадров завершена: {}", path2);
                });
                return null;
            }
        }

        return null;
    }

    private static void convert(Path path, Path path2) {
        try {
            String string = "Add-Type -ReferencedAssemblies PresentationCore, PresentationFramework, WindowsBase, System.Xaml -TypeDefinition @\"\nusing System;\nusing System.IO;\nusing System.Threading;\nusing System.Windows;\nusing System.Windows.Media;\nusing System.Windows.Media.Imaging;\nusing System.Windows.Threading;\nusing System.Globalization;\n\npublic class VideoConverter {\n    public static void ConvertToSpritesheet(string mp4Path, string outputPath) {\n        Uri uri = new Uri(mp4Path);\n        MediaPlayer player = new MediaPlayer();\n        player.ScrubbingEnabled = true;\n        player.Open(uri);\n\n        int timeout = 100;\n        while (!player.NaturalDuration.HasTimeSpan && timeout > 0) {\n            Thread.Sleep(50);\n            Dispatcher.CurrentDispatcher.Invoke(() => {}, DispatcherPriority.Background);\n            timeout--;\n        }\n        if (!player.NaturalDuration.HasTimeSpan) return;\n\n        double totalSeconds = player.NaturalDuration.TimeSpan.TotalSeconds;\n\n        // Вычисляем полное число кадров для 30 FPS видео\n        int targetFps = 30;\n        int totalFrames = (int)Math.Round(totalSeconds * targetFps);\n        if (totalFrames < 1) totalFrames = 1;\n        if (totalFrames > 256) totalFrames = 256; // Максимум 256 кадров на текстуру для GPU\n\n        int cols = (int)Math.Ceiling(Math.Sqrt(totalFrames));\n        int rows = (int)Math.Ceiling((double)totalFrames / cols);\n\n        int frameW = 256;\n        int frameH = 144;\n        if (player.NaturalVideoWidth > 0 && player.NaturalVideoHeight > 0) {\n            frameW = Math.Min(256, player.NaturalVideoWidth);\n            frameH = (int)(frameW * ((double)player.NaturalVideoHeight / player.NaturalVideoWidth));\n        }\n\n        RenderTargetBitmap sheet = new RenderTargetBitmap(frameW * cols, frameH * rows, 96, 96, PixelFormats.Pbgra32);\n\n        for (int i = 0; i < totalFrames; i++) {\n            double time = (i * totalSeconds) / totalFrames;\n            player.Position = TimeSpan.FromSeconds(time);\n\n            for (int w = 0; w < 4; w++) {\n                Thread.Sleep(25);\n                Dispatcher.CurrentDispatcher.Invoke(() => {}, DispatcherPriority.Render);\n            }\n\n            DrawingVisual tileVisual = new DrawingVisual();\n            using (DrawingContext dc = tileVisual.RenderOpen()) {\n                int col = i % cols;\n                int row = i / cols;\n                dc.DrawVideo(player, new Rect(col * frameW, row * frameH, frameW, frameH));\n            }\n            sheet.Render(tileVisual);\n        }\n\n        player.Close();\n\n        PngBitmapEncoder encoder = new PngBitmapEncoder();\n        encoder.Frames.Add(BitmapFrame.Create(sheet));\n        if (File.Exists(outputPath)) File.Delete(outputPath);\n        using (FileStream fs = File.Create(outputPath)) {\n            encoder.Save(fs);\n        }\n\n        string metaPath = Path.ChangeExtension(mp4Path, null) + \"_sheet.meta\";\n        double fps = totalSeconds > 0 ? (totalFrames / totalSeconds) : 30.0;\n        string metaContent = string.Format(CultureInfo.InvariantCulture, \"cols={0}\\nrows={1}\\nframes={2}\\nfps={3:F2}\\nduration={4:F2}\", cols, rows, totalFrames, fps, totalSeconds);\n        File.WriteAllText(metaPath, metaContent);\n    }\n}\n\"@\n[VideoConverter]::ConvertToSpritesheet('{MP4_PATH}', '{SHEET_PATH}')\n";
            String string2 = string.replace("{MP4_PATH}", path.toAbsolutePath().toString().replace("'", "''"))
                .replace("{SHEET_PATH}", path2.toAbsolutePath().toString().replace("'", "''"));
            File file = File.createTempFile("sadnes_vid_", ".ps1");
            file.deleteOnExit();
            Files.writeString(file.toPath(), string2);
            Process process = new ProcessBuilder("powershell", "-ExecutionPolicy", "Bypass", "-File", file.getAbsolutePath()).start();
            process.waitFor();
            file.delete();
        } catch (Exception exception) {
            Sadnes.getLogger().error("[Mp4Converter] Не удалось сконвертировать видео {}", path, exception);
        }
    }
}
