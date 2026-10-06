package uz.fovrecorder;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class Recorder {
    private static final byte[] POISON = new byte[0];
    private static volatile boolean recording;
    private static Process proc;
    private static BlockingQueue<byte[]> queue;
    private static int width, height;
    private static long nextNs, frameNs;
    private static final AtomicInteger pending = new AtomicInteger();

    public static boolean isRecording() { return recording; }

    public static void toggle() {
        if (recording) stop(); else start();
    }

    private static void msg(String s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(Component.literal(s), false);
    }

    private static synchronized void start() {
        Minecraft mc = Minecraft.getInstance();
        try {
            RenderTarget t = mc.getMainRenderTarget();
            width = t.width;
            height = t.height;
            Path dir = Config.outputDir.isEmpty() ? mc.gameDirectory.toPath().resolve("fovrecordings") : Path.of(Config.outputDir);
            Files.createDirectories(dir);
            Path file = dir.resolve("rec_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".mp4");

            List<String> c = new ArrayList<>(List.of(Config.ffmpeg, "-y", "-loglevel", "error",
                    "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", width + "x" + height,
                    "-use_wallclock_as_timestamps", "1", "-i", "-",
                    "-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2,fps=" + Config.fps + ",format=yuv420p",
                    "-c:v", Config.encoder, "-preset", Config.preset));
            if (Config.encoder.contains("nvenc")) { c.add("-cq"); } else { c.add("-crf"); }
            c.add("" + Config.crf);
            c.addAll(List.of("-pix_fmt", "yuv420p", "-movflags", "+faststart", file.toString()));

            ProcessBuilder pb = new ProcessBuilder(c);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            proc = pb.start();
            queue = new ArrayBlockingQueue<>(4);
            frameNs = 1_000_000_000L / Config.fps;
            nextNs = 0;
            pending.set(0);

            final Process p = proc;
            final BlockingQueue<byte[]> q = queue;
            Thread w = new Thread(() -> {
                try (OutputStream out = p.getOutputStream()) {
                    while (true) {
                        byte[] f = q.take();
                        if (f == POISON) break;
                        out.write(f);
                    }
                } catch (Exception e) {
                    recording = false;
                }
                try { p.waitFor(); } catch (InterruptedException ignored) { }
            }, "fovrecorder-writer");
            w.setDaemon(false);
            w.start();

            recording = true;
            msg("Recording started: " + file.getFileName());
        } catch (Exception e) {
            recording = false;
            msg("Recording failed: " + e.getMessage() + " (is ffmpeg installed?)");
        }
    }

    public static synchronized void stop() {
        if (!recording && queue == null) return;
        recording = false;
        if (queue != null) {
            try { queue.put(POISON); } catch (InterruptedException ignored) { }
            queue = null;
        }
        msg("Recording stopped, saving...");
    }

    /** Called at the end of GameRenderer.render on the render thread. */
    public static void onFrameRendered() {
        if (!recording) return;
        long now = System.nanoTime();
        if (now < nextNs) return;
        BlockingQueue<byte[]> q = queue;
        if (q == null || q.remainingCapacity() == 0 || pending.get() >= 2) return;
        nextNs = now + frameNs;
        RenderTarget t = Minecraft.getInstance().getMainRenderTarget();
        if (t.width != width || t.height != height) return;
        pending.incrementAndGet();
        try {
            Screenshot.takeScreenshot(t, img -> {
                try {
                    int w = img.getWidth(), h = img.getHeight();
                    if (w != width || h != height) return;
                    byte[] buf = new byte[w * h * 3];
                    int i = 0;
                    boolean swap = Config.swapRB;
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            int p = img.getPixel(x, y); // assumed ARGB
                            byte r = (byte) (p >> 16), g = (byte) (p >> 8), b = (byte) p;
                            buf[i++] = swap ? b : r;
                            buf[i++] = g;
                            buf[i++] = swap ? r : b;
                        }
                    }
                    q.offer(buf); // drop frame if encoder is behind
                } finally {
                    pending.decrementAndGet();
                }
            });
        } catch (Throwable e) {
            pending.decrementAndGet();
            recording = false;
        }
    }
}
