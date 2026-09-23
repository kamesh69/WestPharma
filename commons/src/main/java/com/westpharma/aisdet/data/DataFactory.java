package com.westpharma.aisdet.data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Dynamic test-data factory (unique names/IDs per run).
 */
public final class DataFactory {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String[] FIRST = {"Ava", "Noah", "Mia", "Liam", "Zoe", "Ethan", "Ivy", "Owen"};
    private static final String[] LAST = {"Patel", "Nguyen", "Garcia", "Kim", "Singh", "Miller", "Rossi", "Chen"};

    private DataFactory() {
    }

    public static String uniqueToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    public static String firstName() {
        return pick(FIRST) + uniqueToken().substring(0, 4);
    }

    public static String lastName() {
        return pick(LAST);
    }

    public static String employeeFirstName(String suffix) {
        return "Auto" + uniqueToken() + suffix;
    }

    public static String employeeLastName(String suffix) {
        return "Emp" + suffix + uniqueToken().substring(0, 4);
    }

    public static String middleName() {
        return "M" + uniqueToken().substring(0, 3);
    }

    public static int totalPrice() {
        return ThreadLocalRandom.current().nextInt(50, 900);
    }

    public static boolean depositPaid() {
        return ThreadLocalRandom.current().nextBoolean();
    }

    public static String checkInDate() {
        return LocalDate.now().plusDays(ThreadLocalRandom.current().nextInt(1, 10)).format(ISO);
    }

    public static String checkOutDate(String checkIn) {
        LocalDate in = LocalDate.parse(checkIn, ISO);
        return in.plusDays(ThreadLocalRandom.current().nextInt(1, 7)).format(ISO);
    }

    public static String additionalNeeds() {
        return "Breakfast-" + uniqueToken().substring(0, 4);
    }

    private static String pick(String[] values) {
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }

    public static String timestampFileName(String prefix, String extension) {
        String ts = java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.ENGLISH));
        return prefix + "_" + ts + "." + extension;
    }
}
