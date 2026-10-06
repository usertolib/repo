package uz.fovrecorder;

import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.Path;
import java.util.Properties;

public final class Config {
    public static String ffmpeg = "ffmpeg";
    public static int fps = 30;
    public static int crf = 23;
    public static String encoder = "libx264";
    public static String preset = "veryfast";
    public static String outputDir = "";
    public static boolean showRec = true;
    /** Set to true if video colors look wrong (red/blue swapped). */
    public static boolean swapRB = false;

    public static void load() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("fovrecorder.properties");
        Properties pr = new Properties();
        try {
            if (p.toFile().exists()) try (InputStream in = new FileInputStream(p.toFile())) { pr.load(in); }
            ffmpeg = pr.getProperty("ffmpeg", ffmpeg);
            fps = Math.max(1, Math.min(120, Integer.parseInt(pr.getProperty("fps", "" + fps).trim())));
            crf = Integer.parseInt(pr.getProperty("crf", "" + crf).trim());
            encoder = pr.getProperty("encoder", encoder).trim();
            preset = pr.getProperty("preset", preset).trim();
            outputDir = pr.getProperty("outputDir", outputDir).trim();
            showRec = Boolean.parseBoolean(pr.getProperty("showRec", "" + showRec).trim());
            swapRB = Boolean.parseBoolean(pr.getProperty("swapRB", "" + swapRB).trim());
            Properties o = new Properties();
            o.setProperty("ffmpeg", ffmpeg); o.setProperty("fps", "" + fps); o.setProperty("crf", "" + crf);
            o.setProperty("encoder", encoder); o.setProperty("preset", preset); o.setProperty("outputDir", outputDir);
            o.setProperty("showRec", "" + showRec); o.setProperty("swapRB", "" + swapRB);
            try (OutputStream out = new FileOutputStream(p.toFile())) { o.store(out, "FOV Recorder"); }
        } catch (Exception e) {
            System.err.println("[fovrecorder] config error: " + e);
        }
    }
}
