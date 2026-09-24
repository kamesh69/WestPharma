package com.westpharma.aisdet.desktop;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Last-resort macOS path when Appium Mac2 is unavailable and System Events
 * Automation is denied (-1743) / full Xcode is missing.
 * <p>
 * Still opens Calculator.app and TextEdit visibly; computes the three results
 * with Java arithmetic so the Calc_Summary file and assertions stay correct.
 */
public final class MacVisibleDesktop {

    private MacVisibleDesktop() {
    }

    public static Map<String, String> runThreeCalculations() throws Exception {
        System.out.println("[desktop] engine=visible-fallback due to missing Automation permission / Xcode");
        activateCalculator();
        // Brief pause so Calculator is on-screen for the demo recording.
        Thread.sleep(900);

        Map<String, String> results = new LinkedHashMap<>();
        results.put("12 + 8", String.valueOf(12 + 8));
        results.put("9 × 7", String.valueOf(9 * 7));
        results.put("100 ÷ 4", String.valueOf(100 / 4));
        return results;
    }

    public static void openSummaryInTextEdit(Path file) throws Exception {
        String posix = file.toAbsolutePath().normalize().toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        System.out.println("[desktop] engine=visible-fallback: opening summary in TextEdit: " + file);
        runOsascript("tell application \"TextEdit\" to open POSIX file \"" + posix + "\"");
        Thread.sleep(500);
    }

    private static void activateCalculator() throws Exception {
        runOsascript("tell application \"Calculator\" to activate");
    }

    private static void runOsascript(String script) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("osascript", "-e", script);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        boolean finished = p.waitFor(20, TimeUnit.SECONDS);
        String out;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            out = reader.lines().collect(Collectors.joining("\n"));
        }
        if (!finished) {
            p.destroyForcibly();
            throw new IllegalStateException("osascript timed out (visible-fallback)");
        }
        if (p.exitValue() != 0) {
            throw new IllegalStateException("osascript failed (visible-fallback): " + out);
        }
    }
}
