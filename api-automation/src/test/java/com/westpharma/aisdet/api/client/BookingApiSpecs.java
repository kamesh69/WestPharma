package com.westpharma.aisdet.api.client;

import com.westpharma.aisdet.config.ConfigReader;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.hamcrest.Matchers;

/**
 * Shared RestAssured request/response specifications for Restful-Booker.
 */
public final class BookingApiSpecs {

    private static final RequestSpecification REQUEST_SPEC = new RequestSpecBuilder()
            .setBaseUri(ConfigReader.get("api.baseUrl", "https://restful-booker.herokuapp.com"))
            .setContentType(ContentType.JSON)
            .addHeader("Accept", "*/*")
            .addHeader("User-Agent", "Mozilla/5.0 (compatible; WestPharma-AISDET/1.0)")
            .addFilter(new RequestLoggingFilter())
            .addFilter(new ResponseLoggingFilter())
            .build();

    /** Successful JSON body responses (GET booking, PUT booking, auth-adjacent JSON). */
    private static final ResponseSpecification OK_JSON = new ResponseSpecBuilder()
            .expectStatusCode(200)
            .expectContentType(ContentType.JSON)
            .build();

    /** Create booking success on Restful-Booker returns 200 with bookingid. */
    private static final ResponseSpecification CREATED_BOOKING = new ResponseSpecBuilder()
            .expectStatusCode(200)
            .expectContentType(ContentType.JSON)
            .expectBody("bookingid", Matchers.greaterThan(0))
            .expectBody("booking.firstname", Matchers.notNullValue())
            .build();

    private static final ResponseSpecification NOT_FOUND = new ResponseSpecBuilder()
            .expectStatusCode(404)
            .build();

    /** Unauthorized mutate — Restful-Booker returns 403 (sometimes 401). */
    private static final ResponseSpecification UNAUTHORIZED = new ResponseSpecBuilder()
            .expectStatusCode(Matchers.anyOf(Matchers.is(401), Matchers.is(403)))
            .build();

    /** Delete success — Restful-Booker commonly returns 201 Created. */
    private static final ResponseSpecification DELETE_SUCCESS = new ResponseSpecBuilder()
            .expectStatusCode(Matchers.anyOf(Matchers.is(200), Matchers.is(201)))
            .build();

    private BookingApiSpecs() {
    }

    public static RequestSpecification request() {
        return REQUEST_SPEC;
    }

    public static ResponseSpecification okJson() {
        return OK_JSON;
    }

    public static ResponseSpecification createdBooking() {
        return CREATED_BOOKING;
    }

    public static ResponseSpecification notFound() {
        return NOT_FOUND;
    }

    public static ResponseSpecification unauthorized() {
        return UNAUTHORIZED;
    }

    public static ResponseSpecification deleteSuccess() {
        return DELETE_SUCCESS;
    }

    public static ResponseSpecification status(int expected) {
        return new ResponseSpecBuilder()
                .expectStatusCode(expected)
                .build();
    }
}
