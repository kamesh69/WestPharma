package com.westpharma.aisdet.api.validators;

import com.westpharma.aisdet.api.client.BookingApiSpecs;
import com.westpharma.aisdet.api.dto.Booking;
import com.westpharma.aisdet.reporting.ExtentTestManager;
import com.westpharma.aisdet.utils.AssertUtils;
import io.restassured.response.Response;

public final class BookingResponseValidator {

    private BookingResponseValidator() {
    }

    public static void assertStatus(Response response, int expected) {
        response.then().spec(BookingApiSpecs.status(expected));
        ExtentTestManager.logPass("Status code is " + expected);
    }

    public static void assertOkJson(Response response) {
        response.then().spec(BookingApiSpecs.okJson());
        ExtentTestManager.logPass("Response is HTTP 200 with JSON content type");
    }

    public static void assertNotFound(Response response) {
        response.then().spec(BookingApiSpecs.notFound());
        ExtentTestManager.logPass("Status code is 404");
    }

    public static void assertUnauthorized(Response response) {
        response.then().spec(BookingApiSpecs.unauthorized());
        ExtentTestManager.logPass("Status code is 401/403 (unauthorized)");
    }

    public static void assertDeleteSuccess(Response response) {
        response.then().spec(BookingApiSpecs.deleteSuccess());
        ExtentTestManager.logPass("Delete succeeded (200/201)");
    }

    public static void assertBookingMatches(Response response, Booking expected) {
        if (response.statusCode() != 200) {
            throw new AssertionError("Expected HTTP 200 JSON booking body but got "
                    + response.statusCode() + " contentType=" + response.getContentType()
                    + " body=" + response.asString());
        }
        response.then().spec(BookingApiSpecs.okJson());
        AssertUtils.assertEquals(response.jsonPath().getString("firstname"), expected.getFirstname(), "firstname");
        AssertUtils.assertEquals(response.jsonPath().getString("lastname"), expected.getLastname(), "lastname");
        AssertUtils.assertEquals(response.jsonPath().getInt("totalprice"), expected.getTotalprice(), "totalprice");
        AssertUtils.assertEquals(response.jsonPath().getBoolean("depositpaid"), expected.isDepositpaid(), "depositpaid");
        AssertUtils.assertEquals(response.jsonPath().getString("bookingdates.checkin"),
                expected.getBookingdates().getCheckin(), "checkin");
        AssertUtils.assertEquals(response.jsonPath().getString("bookingdates.checkout"),
                expected.getBookingdates().getCheckout(), "checkout");
        AssertUtils.assertEquals(response.jsonPath().getString("additionalneeds"),
                expected.getAdditionalneeds(), "additionalneeds");
        ExtentTestManager.logPass("Booking payload fields match expected values");
    }

    public static void assertCreatedBooking(Response response, Booking expected) {
        response.then().spec(BookingApiSpecs.createdBooking());
        Booking nested = new Booking();
        nested.setFirstname(response.jsonPath().getString("booking.firstname"));
        nested.setLastname(response.jsonPath().getString("booking.lastname"));
        nested.setTotalprice(response.jsonPath().getInt("booking.totalprice"));
        nested.setDepositpaid(response.jsonPath().getBoolean("booking.depositpaid"));
        nested.setAdditionalneeds(response.jsonPath().getString("booking.additionalneeds"));
        AssertUtils.assertEquals(nested.getFirstname(), expected.getFirstname(), "created firstname");
        AssertUtils.assertEquals(nested.getLastname(), expected.getLastname(), "created lastname");
        ExtentTestManager.logPass("Create booking response validated via ResponseSpec");
    }
}
