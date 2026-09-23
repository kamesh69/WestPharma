package com.westpharma.aisdet.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.westpharma.aisdet.config.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Bonus gate: schema + business-rule validation before automation consumes AI data.
 */
public final class AiArtifactValidator {

    private static final Logger LOG = LoggerFactory.getLogger(AiArtifactValidator.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private AiArtifactValidator() {
    }

    public static Path validateLatestRawArtifact() throws IOException {
        Path rawDir = ConfigReader.resolveFromRoot(
                ConfigReader.get("ai.artifactsDir", "ai-helper/artifacts") + "/raw");
        Path validatedDir = ConfigReader.resolveFromRoot(
                ConfigReader.get("ai.validatedDir", "ai-helper/artifacts/validated"));
        Path schemaPath = ConfigReader.resolveFromRoot(
                ConfigReader.get("ai.schemaPath", "ai-helper/schemas/test_data.schema.json"));

        Files.createDirectories(validatedDir);
        Path latest = findLatestJson(rawDir);
        if (latest == null) {
            throw new IllegalStateException("No AI raw JSON found under " + rawDir);
        }
        return validateAndPromote(latest, validatedDir, schemaPath);
    }

    public static Path validateAndPromote(Path rawJson, Path validatedDir, Path schemaPath) throws IOException {
        JsonNode node = MAPPER.readTree(rawJson.toFile());
        if (!Files.exists(schemaPath)) {
            throw new IllegalStateException("Schema missing: " + schemaPath);
        }
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(Files.readString(schemaPath));
        Set<ValidationMessage> errors = schema.validate(node);
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("AI artifact failed schema validation:\n");
            errors.forEach(e -> sb.append(" - ").append(e.getMessage()).append('\n'));
            throw new IllegalStateException(sb.toString());
        }
        applyBusinessRules(node);

        Path target = validatedDir.resolve("validated_test_data.json");
        Files.copy(rawJson, target, StandardCopyOption.REPLACE_EXISTING);
        LOG.info("AI artifact validated and promoted: {} -> {}", rawJson, target);
        return target;
    }

    private static void applyBusinessRules(JsonNode root) {
        JsonNode employees = root.path("employees");
        if (employees.isArray()) {
            for (JsonNode emp : employees) {
                requireNonBlank(emp, "firstName");
                requireNonBlank(emp, "lastName");
            }
            if (employees.size() >= 2) {
                String a = employees.get(0).path("firstName").asText() + employees.get(0).path("lastName").asText();
                String b = employees.get(1).path("firstName").asText() + employees.get(1).path("lastName").asText();
                if (a.equalsIgnoreCase(b)) {
                    throw new IllegalStateException("Employee names must be unique");
                }
            }
        }

        JsonNode bookings = root.path("bookings");
        if (bookings.isArray()) {
            for (JsonNode booking : bookings) {
                requireNonBlank(booking, "firstname");
                requireNonBlank(booking, "lastname");
                int price = booking.path("totalprice").asInt(-1);
                if (price < 0) {
                    throw new IllegalStateException("totalprice must be >= 0");
                }
                JsonNode dates = booking.path("bookingdates");
                LocalDate checkin = LocalDate.parse(dates.path("checkin").asText());
                LocalDate checkout = LocalDate.parse(dates.path("checkout").asText());
                if (!checkout.isAfter(checkin)) {
                    throw new IllegalStateException("checkout must be after checkin");
                }
            }
        }
    }

    private static void requireNonBlank(JsonNode node, String field) {
        String value = node.path(field).asText("");
        if (value.isBlank()) {
            throw new IllegalStateException("Field '" + field + "' must be non-blank");
        }
    }

    private static Path findLatestJson(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return null;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .filter(p -> !p.getFileName().toString().endsWith(".meta.json"))
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                    .orElse(null);
        }
    }
}
