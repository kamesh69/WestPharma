package com.westpharma.aisdet.api.tests;

import com.westpharma.aisdet.BaseTest;
import com.westpharma.aisdet.api.builder.BookingPayloadBuilder;
import com.westpharma.aisdet.api.client.BookingApiClient;
import com.westpharma.aisdet.api.dto.Booking;
import com.westpharma.aisdet.reporting.ExtentTestListener;
import com.westpharma.aisdet.reporting.ExtentTestManager;
import com.westpharma.aisdet.api.client.BookingApiSpecs;
import com.westpharma.aisdet.api.validators.BookingResponseValidator;
import com.westpharma.aisdet.utils.AssertUtils;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(ExtentTestListener.class)
public class BookingNegativeTest extends BaseTest {

    private BookingApiClient client;
    private int existingBookingId;
    private Booking sample;

    @Override
    protected String moduleName() {
        return "api";
    }

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        client = new BookingApiClient();
        sample = BookingPayloadBuilder.dynamic().build();
        Response created = client.createBooking(sample);
        if (created.statusCode() != 200 || created.getContentType() == null
                || !created.getContentType().contains("json")) {
            throw new org.testng.SkipException(
                    "Unable to seed booking for negative tests (status=" + created.statusCode()
                            + "). Restful-Booker may be rate-limiting.");
        }
        existingBookingId = created.jsonPath().getInt("bookingid");
        AssertUtils.assertTrue(existingBookingId > 0, "Seed booking required for negative tests");
    }

    @Test(priority = 1, description = "Negative: invalid booking payload (missing firstname)")
    public void createBookingWithInvalidData() {
        // Restful-Booker is permissive; assert non-success or empty/invalid bookingid behavior.
        String invalidJson = """
                {"lastname":"OnlyLast","totalprice":100,"depositpaid":true,
                 "bookingdates":{"checkin":"2026-01-01","checkout":"2026-01-05"},
                 "additionalneeds":"None"}
                """;
        Response response = io.restassured.RestAssured.given()
                .spec(BookingApiSpecs.request())
                .body(invalidJson)
                .when()
                .post("/booking")
                .then()
                .extract()
                .response();

        ExtentTestManager.logInfo("Invalid create status=" + response.statusCode() + " body=" + response.asString());
        // Accept either hard failure or soft failure (null/missing firstname in response)
        boolean hardFail = response.statusCode() >= 400;
        boolean softFail = response.statusCode() == 200
                && (response.jsonPath().get("booking.firstname") == null
                || response.jsonPath().getString("booking.firstname").isBlank());
        AssertUtils.assertTrue(hardFail || softFail,
                "Invalid payload should not yield a fully valid booking");
        ExtentTestManager.logPass("Invalid data scenario covered");
    }

    @Test(priority = 2, description = "Negative: update without authorization token")
    public void updateWithoutAuthorization() {
        Booking updated = BookingPayloadBuilder.dynamic().build();
        Response response = client.updateBookingWithoutAuth(existingBookingId, updated);
        ExtentTestManager.logInfo("Unauthorized update status=" + response.statusCode());
        BookingResponseValidator.assertUnauthorized(response);
        ExtentTestManager.logPass("Unauthorized update rejected");
    }

    @Test(priority = 3, description = "Negative: delete without authorization token")
    public void deleteWithoutAuthorization() {
        Response response = client.deleteBookingWithoutAuth(existingBookingId);
        ExtentTestManager.logInfo("Unauthorized delete status=" + response.statusCode());
        BookingResponseValidator.assertUnauthorized(response);
        ExtentTestManager.logPass("Unauthorized delete rejected");
    }

    @Test(priority = 4, description = "Negative: get non-existent booking returns 404")
    public void getNonExistentBooking() {
        Response response = client.getBooking(Integer.MAX_VALUE - 7);
        BookingResponseValidator.assertNotFound(response);
        ExtentTestManager.logPass("Missing booking returns 404");
    }
}
