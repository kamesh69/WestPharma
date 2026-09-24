package com.westpharma.aisdet.desktop;

import com.westpharma.aisdet.config.ConfigReader;

/**
 * Resolves which desktop driver stack to use for Q3.
 * Same use case on both platforms: Calculator ops → summary file → text editor.
 * Windows = WinAppDriver; macOS = Appium Mac2.
 */
public final class DesktopOsSupport {

    public enum Platform {
        WINDOWS,
        MAC
    }

    private DesktopOsSupport() {
    }

    public static Platform resolve() {
        String configured = ConfigReader.get("desktop.platform", "auto").trim().toLowerCase();
        return switch (configured) {
            case "windows", "win" -> Platform.WINDOWS;
            case "mac", "macos", "osx" -> Platform.MAC;
            default -> detectHost();
        };
    }

    public static Platform detectHost() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return Platform.WINDOWS;
        }
        if (os.contains("mac")) {
            return Platform.MAC;
        }
        throw new UnsupportedOperationException(
                "Desktop Q3 supports Windows (WinAppDriver) or macOS (Appium Mac2). Current OS="
                        + System.getProperty("os.name"));
    }

    public static boolean isWindows() {
        return resolve() == Platform.WINDOWS;
    }

    public static boolean isMac() {
        return resolve() == Platform.MAC;
    }

    public static String framingNote() {
        return "Same use case; WinAppDriver on Windows, Appium Mac2 on macOS.";
    }
}
