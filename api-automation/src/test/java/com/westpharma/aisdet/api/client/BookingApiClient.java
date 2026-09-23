package com.westpharma.aisdet.api.client;

import com.westpharma.aisdet.api.dto.AuthRequest;
import com.westpharma.aisdet.api.dto.Booking;
import com.westpharma.aisdet.config.ConfigReader;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.function.Supplier;

public class BookingApiClient {

    private final RequestSpecification spec;

    public BookingApiClient() {
        this.spec = BookingApiSpecs.request();
        // Warm-up ping reduces intermittent 418 responses from the public demo host
        try {
            RestAssured.given().spec(spec).when().get("/").then().extract().response();
        } catch (Exception ignored) {
        }
    }

    public String getAuthToken() {
        AuthRequest body = new AuthRequest(
                ConfigReader.get("api.auth.username", "admin"),
                ConfigReader.get("api.auth.password", "password123"));
        Response response = withRetry(() -> RestAssured.given().spec(spec)
                .body(body)
                .when()
                .post("/auth")
                .then()
                .extract()
                .response());
        // Apply response spec after retry so 418/429 can be retried first
        response.then().spec(BookingApiSpecs.okJson());
        return response.jsonPath().getString("token");
    }

    public Response createBooking(Booking booking) {
        return withRetry(() -> RestAssured.given().spec(spec)
                .body(booking)
                .when()
                .post("/booking")
                .then()
                .extract()
                .response());
    }

    public Response getBooking(int bookingId) {
        return withRetry(() -> RestAssured.given().spec(spec)
                .when()
                .get("/booking/{id}", bookingId)
                .then()
                .extract()
                .response());
    }

    public Response updateBooking(int bookingId, Booking booking, String token) {
        return withRetry(() -> RestAssured.given().spec(spec)
                .header("Cookie", "token=" + token)
                .body(booking)
                .when()
                .put("/booking/{id}", bookingId)
                .then()
                .extract()
                .response());
    }

    public Response updateBookingWithoutAuth(int bookingId, Booking booking) {
        return withRetry(() -> RestAssured.given().spec(spec)
                .body(booking)
                .when()
                .put("/booking/{id}", bookingId)
                .then()
                .extract()
                .response());
    }

    public Response deleteBooking(int bookingId, String token) {
        return withRetry(() -> RestAssured.given().spec(spec)
                .header("Cookie", "token=" + token)
                .when()
                .delete("/booking/{id}", bookingId)
                .then()
                .extract()
                .response());
    }

    public Response deleteBookingWithoutAuth(int bookingId) {
        return withRetry(() -> RestAssured.given().spec(spec)
                .when()
                .delete("/booking/{id}", bookingId)
                .then()
                .extract()
                .response());
    }

    /**
     * Restful-Booker occasionally returns HTTP 418 under load; retry with short backoff.
     * Keep total wait modest so shared demo data is less likely to be purged mid-suite.
     */
    private Response withRetry(Supplier<Response> call) {
        int attempts = 5;
        long baseSleepMs = 2000; // 2s, 4s, 6s, 8s
        Response last = null;
        for (int i = 0; i < attempts; i++) {
            last = call.get();
            int code = last.statusCode();
            if (code != 418 && code != 429 && code != 503) {
                return last;
            }
            if (i < attempts - 1) {
                try {
                    Thread.sleep(baseSleepMs * (i + 1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return last;
                }
            }
        }
        return last;
    }
}
