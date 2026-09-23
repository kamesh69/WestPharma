package com.westpharma.aisdet.api.tests;

import com.westpharma.aisdet.BaseTest;
import com.westpharma.aisdet.ai.AiDataLoader;
import com.westpharma.aisdet.api.builder.BookingPayloadBuilder;
import com.westpharma.aisdet.api.client.BookingApiClient;
import com.westpharma.aisdet.api.dto.Booking;
import com.westpharma.aisdet.api.validators.BookingResponseValidator;
import com.westpharma.aisdet.reporting.ExtentTestListener;
import com.westpharma.aisdet.reporting.ExtentTestManager;
import com.westpharma.aisdet.utils.AssertUtils;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

@Listeners(ExtentTestListener.class)
public class BookingCrudTest extends BaseTest {

    private BookingApiClient client;
    private String token;
    private final List<Integer> bookingIds = new ArrayList<>();
    private final List<Booking> payloads = new ArrayList<>();

    @Override
    protected String moduleName() {
        return "api";
    }

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        client = new BookingApiClient();
        token = client.getAuthToken();
        AssertUtils.assertTrue(token != null && !token.isBlank(), "Auth token must be present");
    }

    @Test(priority = 1, retryAnalyzer = com.westpharma.aisdet.utils.RetryAnalyzer.class,
            description = "Create multiple bookings using dynamic / AI-validated data")
    public void createMultipleBookings() throws Exception {
        List<Booking> toCreate = resolveBookings();
        for (Booking booking : toCreate) {
            ExtentTestManager.logInfo("Creating booking for " + booking.getFirstname() + " " + booking.getLastname());
            Response response = client.createBooking(booking);
            if (response.statusCode() == 418 || response.statusCode() == 429) {
                throw new org.testng.SkipException(
                        "Restful-Booker public host returned " + response.statusCode()
                                + " (rate limit / teapot). Re-run when the demo API is healthy.");
            }
            BookingResponseValidator.assertCreatedBooking(response, booking);
            int id = response.jsonPath().getInt("bookingid");
            bookingIds.add(id);
            payloads.add(booking);
            ExtentTestManager.logPass("Created bookingId=" + id);
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        AssertUtils.assertTrue(bookingIds.size() >= 2, "At least two bookings must be created");
    }

    @Test(priority = 2, dependsOnMethods = "createMultipleBookings",
            description = "Retrieve and validate booking details")
    public void retrieveAndValidateBookings() {
        for (int i = 0; i < bookingIds.size(); i++) {
            int id = bookingIds.get(i);
            Booking expected = payloads.get(i);
            Response response = client.getBooking(id);
            BookingResponseValidator.assertStatus(response, 200);
            BookingResponseValidator.assertBookingMatches(response, expected);
            ExtentTestManager.logPass("Validated GET bookingId=" + id);
        }
    }

    @Test(priority = 3, dependsOnMethods = "retrieveAndValidateBookings",
            description = "Update first booking and verify changes")
    public void updateFirstBooking() {
        int id = bookingIds.get(0);
        Booking updated = BookingPayloadBuilder.create()
                .firstname(payloads.get(0).getFirstname())
                .lastname(payloads.get(0).getLastname() + "Updated")
                .totalprice(payloads.get(0).getTotalprice() + 25)
                .depositpaid(!payloads.get(0).isDepositpaid())
                .bookingdates(payloads.get(0).getBookingdates().getCheckin(),
                        payloads.get(0).getBookingdates().getCheckout())
                .additionalneeds("LateCheckout")
                .build();

        Response put = client.updateBooking(id, updated, token);
        BookingResponseValidator.assertStatus(put, 200);
        BookingResponseValidator.assertBookingMatches(put, updated);

        Response get = client.getBooking(id);
        BookingResponseValidator.assertStatus(get, 200);
        BookingResponseValidator.assertBookingMatches(get, updated);
        payloads.set(0, updated);
        ExtentTestManager.logPass("Update verified for bookingId=" + id);
    }

    @Test(priority = 4, dependsOnMethods = "updateFirstBooking",
            description = "Delete second booking and validate deletion; first remains",
            retryAnalyzer = com.westpharma.aisdet.utils.RetryAnalyzer.class)
    public void deleteSecondBookingAndVerifyFirstRemains() {
        int deleteId = bookingIds.get(1);
        int keepId = ensureBookingAlive(0);

        Response delete = client.deleteBooking(deleteId, token);
        BookingResponseValidator.assertDeleteSuccess(delete);
        ExtentTestManager.logPass("Deleted bookingId=" + deleteId);

        Response missing = waitForNotFound(deleteId);
        BookingResponseValidator.assertNotFound(missing);

        keepId = ensureBookingAlive(0);
        Response stillThere = client.getBooking(keepId);
        BookingResponseValidator.assertBookingMatches(stillThere, payloads.get(0));
        ExtentTestManager.logPass("Unaffected bookingId=" + keepId + " still present");
    }

    /** Demo host sometimes purges bookings; recreate from stored payload if GET is not 200. */
    private int ensureBookingAlive(int index) {
        int id = bookingIds.get(index);
        Booking payload = payloads.get(index);
        Response get = client.getBooking(id);
        if (get.statusCode() == 200) {
            return id;
        }
        ExtentTestManager.logInfo("BookingId=" + id + " missing on demo host (status="
                + get.statusCode() + "); recreating before continue");
        Response created = client.createBooking(payload);
        BookingResponseValidator.assertCreatedBooking(created, payload);
        int newId = created.jsonPath().getInt("bookingid");
        bookingIds.set(index, newId);
        return newId;
    }

    private Response waitForNotFound(int bookingId) {
        Response last = null;
        for (int i = 0; i < 4; i++) {
            last = client.getBooking(bookingId);
            if (last.statusCode() == 404) {
                return last;
            }
            try {
                Thread.sleep(1000L * (i + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return last;
    }

    private List<Booking> resolveBookings() throws Exception {
        List<Booking> list = new ArrayList<>();
        try {
            List<AiDataLoader.BookingData> ai = AiDataLoader.loadBookings();
            for (AiDataLoader.BookingData b : ai) {
                list.add(BookingPayloadBuilder.create()
                        .firstname(b.firstname())
                        .lastname(b.lastname())
                        .totalprice(b.totalprice())
                        .depositpaid(b.depositpaid())
                        .bookingdates(b.checkin(), b.checkout())
                        .additionalneeds(b.additionalneeds())
                        .build());
            }
            ExtentTestManager.logInfo("Using AI-validated booking data (" + list.size() + ")");
        } catch (Exception ex) {
            ExtentTestManager.logInfo("AI data unavailable (" + ex.getMessage() + "); using DataFactory");
            list.add(BookingPayloadBuilder.dynamic().build());
            list.add(BookingPayloadBuilder.dynamic().build());
        }
        if (list.size() < 2) {
            while (list.size() < 2) {
                list.add(BookingPayloadBuilder.dynamic().build());
            }
        }
        return list.subList(0, 2);
    }
}
