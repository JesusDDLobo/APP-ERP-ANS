package com.example.app_ans.hotels.network;

import com.example.app_ans.hotels.model.Hotel;

import retrofit2.Call;
import retrofit2.http.GET;

public interface HotelApi {
    @GET("api/mobile/hotel-reservations")
    Call<Hotel> getHotelReservation();
}
