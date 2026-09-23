package com.westpharma.aisdet.api.builder;

import com.westpharma.aisdet.api.dto.Booking;
import com.westpharma.aisdet.api.dto.BookingDates;
import com.westpharma.aisdet.data.DataFactory;

public class BookingPayloadBuilder {

    private final Booking booking = new Booking();

    public static BookingPayloadBuilder create() {
        return new BookingPayloadBuilder();
    }

    public static BookingPayloadBuilder dynamic() {
        String checkin = DataFactory.checkInDate();
        return create()
                .firstname(DataFactory.firstName())
                .lastname(DataFactory.lastName())
                .totalprice(DataFactory.totalPrice())
                .depositpaid(DataFactory.depositPaid())
                .bookingdates(checkin, DataFactory.checkOutDate(checkin))
                .additionalneeds(DataFactory.additionalNeeds());
    }

    public BookingPayloadBuilder firstname(String firstname) {
        booking.setFirstname(firstname);
        return this;
    }

    public BookingPayloadBuilder lastname(String lastname) {
        booking.setLastname(lastname);
        return this;
    }

    public BookingPayloadBuilder totalprice(int totalprice) {
        booking.setTotalprice(totalprice);
        return this;
    }

    public BookingPayloadBuilder depositpaid(boolean depositpaid) {
        booking.setDepositpaid(depositpaid);
        return this;
    }

    public BookingPayloadBuilder bookingdates(String checkin, String checkout) {
        booking.setBookingdates(new BookingDates(checkin, checkout));
        return this;
    }

    public BookingPayloadBuilder additionalneeds(String additionalneeds) {
        booking.setAdditionalneeds(additionalneeds);
        return this;
    }

    public Booking build() {
        return booking;
    }
}
