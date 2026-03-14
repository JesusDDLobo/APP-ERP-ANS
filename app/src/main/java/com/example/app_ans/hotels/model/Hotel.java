package com.example.app_ans.hotels.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Hotel {
    @SerializedName("reservation_status")
    private String reservationStatus;

    @SerializedName("reservation")
    private Reservation reservation;

    public String getReservationStatus() { return reservationStatus; }
    public Reservation getReservation() { return reservation; }

    public int getId() { return reservation != null ? reservation.id : 0; }
    public String getName() { return reservation != null ? reservation.hotelName : null; }
    public String getAddress() { return reservation != null ? reservation.hotelAddress : null; }
    public String getPhone() { return reservation != null ? reservation.hotelPhone : null; }
    public String getEmail() { return reservation != null && reservation.reservedBy != null ? reservation.reservedBy.email : null; }
    public String getNotes() { return reservation != null ? reservation.hotelObservations : null; }

    public String getTicketPublicId() { return reservation != null ? reservation.ticketPublicId : null; }
    public String getReservationCode() { return reservation != null ? reservation.reservationCode : null; }
    public String getStartDate() { return reservation != null ? reservation.startDate : null; }
    public String getEndDate() { return reservation != null ? reservation.endDate : null; }
    public String getTotalAmount() { return reservation != null ? reservation.totalAmount : null; }
    public List<Room> getRooms() { return reservation != null ? reservation.rooms : null; }

    public static class Reservation {
        private int id;

        @SerializedName("ticket_id")
        private String ticketId;

        @SerializedName("ticket_public_id")
        private String ticketPublicId;

        @SerializedName("hotel_id")
        private String hotelId;

        @SerializedName("hotel_name")
        private String hotelName;

        @SerializedName("hotel_address")
        private String hotelAddress;

        @SerializedName("hotel_phone")
        private String hotelPhone;

        @SerializedName("start_date")
        private String startDate;

        @SerializedName("end_date")
        private String endDate;

        @SerializedName("total_amount")
        private String totalAmount;

        @SerializedName("reservation_code")
        private String reservationCode;

        @SerializedName(value = "observations", alternate = {"hotel_observations"})
        private String hotelObservations;

        private List<Room> rooms;

        @SerializedName("reserved_at")
        private String reservedAt;

        @SerializedName("reserved_by")
        private ReservedBy reservedBy;

        public int getId() { return id; }
        public String getTicketId() { return ticketId; }
        public String getTicketPublicId() { return ticketPublicId; }
        public String getHotelId() { return hotelId; }
        public String getHotelName() { return hotelName; }
        public String getHotelAddress() { return hotelAddress; }
        public String getHotelPhone() { return hotelPhone; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
        public String getTotalAmount() { return totalAmount; }
        public String getReservationCode() { return reservationCode; }
        public String getHotelObservations() { return hotelObservations; }
        public List<Room> getRooms() { return rooms; }
        public String getReservedAt() { return reservedAt; }
        public ReservedBy getReservedBy() { return reservedBy; }
    }

    public static class Room {
        @SerializedName("room_number")
        private String roomNumber;

        @SerializedName("room_type")
        private String roomType;

        @SerializedName("final_price_per_night")
        private double finalPricePerNight;

        public String getRoomNumber() { return roomNumber; }
        public String getRoomType() { return roomType; }
        public double getFinalPricePerNight() { return finalPricePerNight; }
    }

    public static class ReservedBy {
        private String id;
        private String name;
        private String email;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
    }
}
