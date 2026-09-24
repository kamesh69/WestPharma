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
 * macOS Calculator + TextEdit automation via System Events (osascript).
 * Used when Appium Mac2 is unavailable; same use-case outcomes as Mac2/WinAppDriver.
 * <p>
 * When macOS returns Automation denied (-1743), callers should fall back to
 * {@link MacVisibleDesktop}.
 */
public final class MacOsascriptDesktop {

    /** Thrown when System Events / Accessibility Automation is blocked (-1743). */
    public static final class AutomationDeniedException extends IllegalStateException {
        public AutomationDeniedException(String message) {
            super(message);
        }
    }

    private MacOsascriptDesktop() {
    }

    public static boolean isAutomationDenied(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof AutomationDeniedException) {
                return true;
            }
            String msg = cur.getMessage();
            if (msg != null && (msg.contains("-1743")
                    || msg.toLowerCase().contains("not authorised")
                    || msg.toLowerCase().contains("not authorized")
                    || msg.toLowerCase().contains("blocked automation"))) {
                return true;
            }
        }
        return false;
    }

    public static Map<String, String> runThreeCalculations() throws Exception {
        runOsascript("""
                tell application "Calculator" to activate
                delay 0.8
                tell application "System Events"
                  tell process "Calculator"
                    set frontmost to true
                  end tell
                  keystroke "c" using {command down}
                  delay 0.2
                end tell
                """);

        Map<String, String> results = new LinkedHashMap<>();
        results.put("12 + 8", calculate("12", "+", "8"));
        results.put("9 × 7", calculate("9", "*", "7"));
        results.put("100 ÷ 4", calculate("100", "/", "4"));
        return results;
    }

    public static void typeSummaryIntoTextEdit(String content) throws Exception {
        // Escape for AppleScript string
        String escaped = content
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
        runOsascript("""
                tell application "TextEdit"
                  activate
                  make new document
                  set the text of the front document to "%s"
                end tell
                """.formatted(escaped));
    }

    /** Opens an existing Calc_Summary file in TextEdit (no System Events required). */
    public static void openSummaryFileInTextEdit(Path file) throws Exception {
        String posix = file.toAbsolutePath().normalize().toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        runOsascript("tell application \"TextEdit\" to open POSIX file \"" + posix + "\"");
    }

    private static String calculate(String left, String op, String right) throws Exception {
        runOsascript("""
                tell application "Calculator" to activate
                delay 0.3
                tell application "System Events"
                  keystroke "c" using {command down}
                  delay 0.15
                  keystroke "%s"
                  delay 0.1
                  keystroke "%s"
                  delay 0.1
                  keystroke "%s"
                  delay 0.1
                  keystroke "="
                  delay 0.35
                end tell
                """.formatted(left, op, right));
        String raw = readCalculatorDisplay();
        return normalizeNumber(raw);
    }

    private static String readCalculatorDisplay() throws Exception {
        String script = """
                tell application "System Events"
                  tell process "Calculator"
                    set frontmost to true
                    delay 0.2
                    try
                      return value of first static text of first group of window 1
                    end try
                    try
                      return value of text field 1 of window 1
                    end try
                    try
                      return name of first static text of window 1
                    end try
                    return ""
                  end tell
                end tell
                """;
        String value = runOsascript(script).trim();
        if (value.isBlank()) {
            // Fallback: copy display via Cmd+C is unreliable; use AX description dump
            value = runOsascript("""
                    tell application "System Events"
                      tell process "Calculator"
                        set vals to {}
                        repeat with e in (entire contents of window 1)
                          try
                            set end of vals to (value of e as text)
                          end try
                        end repeat
                        return vals as text
                      end tell
                    end tell
                    """);
        }
        return value;
    }

    private static String normalizeNumber(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.replace(",", "").replace(" ", "").trim();
        // Pull last integer/decimal token
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("-?\\d+(?:\\.\\d+)?")
                .matcher(cleaned);
        String last = "";
        while (m.find()) {
            last = m.group();
        }
        if (last.endsWith(".0")) {
            last = last.substring(0, last.length() - 2);
        }
        return last;
    }

    private static String runOsascript(String script) throws Exception {
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
            throw new IllegalStateException("osascript timed out");
        }
        if (p.exitValue() != 0) {
            if (out.contains("-1743") || out.toLowerCase().contains("not authorised")
                    || out.toLowerCase().contains("not authorized")) {
                throw new AutomationDeniedException("""
                        macOS blocked Automation (-1743). Falling back to visible-fallback when engine=auto.
                        To enable System Events later: System Settings → Privacy & Security → Automation
                        Allow Terminal / iTerm / Cursor / Java / osascript to control System Events and Calculator.
                        Optional: install full Xcode + Appium Mac2 on :4724.
                        """ + out);
            }
            throw new IllegalStateException("osascript failed: " + out);
        }
        return out;
    }
}
