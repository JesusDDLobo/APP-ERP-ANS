package com.example.app_ans.hotels.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.app_ans.hotels.model.Hotel;
import com.example.app_ans.hotels.repository.HotelRepository;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HotelViewModel extends ViewModel {
    private final HotelRepository repository;
    private final Executor executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Hotel> hotelLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> infoLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>();

    public HotelViewModel(HotelRepository repository) {
        this.repository = repository;
    }

    public LiveData<Hotel> getHotel() { return hotelLiveData; }
    public LiveData<String> getError() { return errorLiveData; }
    public LiveData<String> getInfo() { return infoLiveData; }
    public LiveData<Boolean> getLoading() { return loadingLiveData; }

    public void loadHotelReservation() {
        loadingLiveData.postValue(true);
        errorLiveData.postValue(null);
        infoLiveData.postValue(null);
        executor.execute(() -> {
            try {
                Hotel hotel = repository.getHotelReservation();
                if (hotel == null || hotel.getReservation() == null) {
                    hotelLiveData.postValue(null);
                    infoLiveData.postValue("No hay reservas activas.");
                    return;
                }

                hotelLiveData.postValue(hotel);
            } catch (Exception e) {
                String message = e.getMessage();
                if (isNoActiveReservation(message)) {
                    hotelLiveData.postValue(null);
                    infoLiveData.postValue("No hay reservas activas.");
                } else {
                    errorLiveData.postValue(message);
                }
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    private boolean isNoActiveReservation(String message) {
        if (message == null) return false;
        String normalized = message.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("ticket no encontrado")
                || normalized.contains("no hay una reserva de hotel asociada al ticket")
                || normalized.contains("no hay reservas activas");
    }
}
