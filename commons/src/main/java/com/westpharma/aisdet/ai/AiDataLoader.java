package com.westpharma.aisdet.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.westpharma.aisdet.config.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads schema-validated AI artifacts only (never raw).
 */
public final class AiDataLoader {

    private static final Logger LOG = LoggerFactory.getLogger(AiDataLoader.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private AiDataLoader() {
    }

    public static JsonNode loadValidatedRoot() throws IOException {
        Path validated = ConfigReader.resolveFromRoot(
                ConfigReader.get("ai.validatedDir", "ai-helper/artifacts/validated") + "/validated_test_data.json");
        if (!Files.exists(validated)) {
            throw new IllegalStateException(
                    "Validated AI data missing at " + validated + ". Run ai-helper then AiArtifactValidator.");
        }
        LOG.info("Loading validated AI data from {}", validated);
        return MAPPER.readTree(validated.toFile());
    }

    public static List<EmployeeData> loadEmployees() throws IOException {
        JsonNode root = loadValidatedRoot();
        List<EmployeeData> list = new ArrayList<>();
        for (JsonNode n : root.path("employees")) {
            list.add(new EmployeeData(
                    n.path("firstName").asText(),
                    n.path("middleName").asText(""),
                    n.path("lastName").asText()));
        }
        return list;
    }

    public static List<BookingData> loadBookings() throws IOException {
        JsonNode root = loadValidatedRoot();
        List<BookingData> list = new ArrayList<>();
        for (JsonNode n : root.path("bookings")) {
            list.add(new BookingData(
                    n.path("firstname").asText(),
                    n.path("lastname").asText(),
                    n.path("totalprice").asInt(),
                    n.path("depositpaid").asBoolean(),
                    n.path("bookingdates").path("checkin").asText(),
                    n.path("bookingdates").path("checkout").asText(),
                    n.path("additionalneeds").asText("")));
        }
        return list;
    }

    public record EmployeeData(String firstName, String middleName, String lastName) {
    }

    public record BookingData(
            String firstname,
            String lastname,
            int totalprice,
            boolean depositpaid,
            String checkin,
            String checkout,
            String additionalneeds) {
    }
}
