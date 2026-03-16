package com.example.app_ans.hotels.repository;

import com.example.app_ans.hotels.model.Hotel;
import com.example.app_ans.hotels.network.HotelApi;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import retrofit2.Response;

public class HotelRepository {
    private final HotelApi api;

    public HotelRepository(HotelApi api) {
        this.api = api;
    }

    public Hotel getHotelReservation() throws IOException {
        Response<Hotel> response = api.getHotelReservation().execute();
        if (response.isSuccessful() && response.body() != null) {
            return response.body();
        }

        String backendMessage = null;
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                if (raw != null && !raw.trim().isEmpty()) {
                    JsonElement parsed = JsonParser.parseString(raw);
                    if (parsed != null && parsed.isJsonObject()) {
                        JsonObject obj = parsed.getAsJsonObject();
                        JsonElement msg = obj.get("message");
                        if (msg != null && !msg.isJsonNull()) {
                            String asString = msg.getAsString();
                            if (asString != null && !asString.trim().isEmpty()) {
                                backendMessage = asString.trim();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (backendMessage != null) {
            throw new IOException(backendMessage);
        }
        if (response.code() == 404) {
            throw new IOException("No hay una reserva de hotel asociada al ticket.");
        }
        throw new IOException("Error al obtener información del hotel: " + response.code());
    }
}
