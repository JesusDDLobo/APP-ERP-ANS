package com.example.app_ans;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.app_ans.auth.model.UserSession;
import com.example.app_ans.auth.session.AuthSessionManager;
import com.example.app_ans.core.network.NetworkModule;
import com.example.app_ans.databinding.ActivityHotelBinding;
import com.example.app_ans.hotels.model.Hotel;
import com.example.app_ans.hotels.repository.HotelRepository;
import com.example.app_ans.hotels.ui.HotelViewModel;
import com.example.app_ans.hotels.ui.HotelViewModelFactory;

import java.util.Locale;

public class HotelActivity extends AppCompatActivity {

    private ActivityHotelBinding binding;
    private HotelViewModel viewModel;
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHotelBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        AuthSessionManager sessionManager = new AuthSessionManager(this);

        UserSession session = sessionManager.restoreSession();
        String role = session != null ? session.getRole() : null;
        isAdmin = isAdminRole(role);

        com.example.app_ans.auth.repository.AuthRepository repository =
                new com.example.app_ans.auth.repository.AuthRepository(
                        com.example.app_ans.core.network.NetworkModule.provideAuthApi(this, sessionManager),
                        sessionManager
                );

        com.example.app_ans.auth.ui.AuthViewModel authViewModel =
                new ViewModelProvider(
                        this,
                        new com.example.app_ans.auth.ui.AuthViewModelFactory(repository)
                ).get(com.example.app_ans.auth.ui.AuthViewModel.class);

        com.example.app_ans.core.ui.NavbarUtils.setupNavbar(
                this,
                null,
                () -> authViewModel.logout(this)
        );

        binding.navbar.navbarBackButton.setVisibility(View.GONE);
        binding.navbar.navbarTitle.setVisibility(View.GONE);

        if (binding.backToHome != null) {
            binding.backToHome.setOnClickListener(v -> finish());
        }

        authViewModel.getLogoutResult().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Intent intent = new Intent(this, com.example.app_ans.auth.ui.AuthActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        if (!isAdmin) {
            showNavbarOnly();
            return;
        }

        binding.invoiceCard.setVisibility(View.GONE);

        binding.downloadArrow.setOnClickListener(v -> {
            if (binding.invoiceCard.getVisibility() == View.VISIBLE) {
                binding.invoiceCard.setVisibility(View.GONE);
                binding.downloadArrow.animate().rotation(0f).setDuration(200).start();
            } else {
                binding.invoiceCard.setVisibility(View.VISIBLE);
                binding.downloadArrow.animate().rotation(180f).setDuration(200).start();
            }
        });

        setupHotelsViewModel(sessionManager);
        observeHotelsViewModel();
        viewModel.loadHotelReservation();
    }

    private boolean isAdminRole(String role) {
        if (role == null) return false;
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("administrador") || normalized.equals("admin");
    }

    private void showNavbarOnly() {
        if (binding.hotelContent != null) {
            binding.hotelContent.setVisibility(View.VISIBLE);
        }

        if (binding.backToHome != null) {
            binding.backToHome.setVisibility(View.VISIBLE);
            binding.backToHome.setOnClickListener(v -> finish());
        }

        if (binding.hotelBodyContent != null) {
            binding.hotelBodyContent.setVisibility(View.GONE);
        }

        if (binding.loadingProgress != null) {
            binding.loadingProgress.setVisibility(View.GONE);
        }

        if (binding.errorText != null) {
            binding.errorText.setVisibility(View.GONE);
        }
    }

    private void setupHotelsViewModel(AuthSessionManager sessionManager) {
        HotelRepository repository = new HotelRepository(NetworkModule.provideHotelApi(this, sessionManager));
        viewModel = new ViewModelProvider(this, new HotelViewModelFactory(repository)).get(HotelViewModel.class);
    }

    private void observeHotelsViewModel() {
        viewModel.getLoading().observe(this, loading -> {
            binding.loadingProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                showMessage(error, true);
            } else {
                if (viewModel.getInfo().getValue() == null) {
                    binding.errorText.setVisibility(View.GONE);
                }
            }
        });

        viewModel.getInfo().observe(this, info -> {
            if (info != null) {
                showMessage(info, false);
            } else {
                if (viewModel.getError().getValue() == null) {
                    binding.errorText.setVisibility(View.GONE);
                }
            }
        });

        viewModel.getHotel().observe(this, this::displayHotel);
    }

    private void displayHotel(Hotel hotel) {
        if (hotel == null) return;
        if (hotel.getReservation() == null) {
            showMessage("No hay reservas activas.", false);
            return;
        }

        binding.hotelContent.setVisibility(View.VISIBLE);
        binding.errorText.setVisibility(View.GONE);

        binding.hotelStatus.setText(valueOrFallback(hotel.getReservationStatus(), "No especificado"));
        binding.hotelName.setText(valueOrFallback(hotel.getName(), "No especificado"));
        binding.hotelAddress.setText(valueOrFallback(hotel.getAddress(), "No especificado"));
        binding.hotelPhone.setText(valueOrFallback(hotel.getPhone(), "No especificado"));
        binding.hotelStartDate.setText(valueOrFallback(hotel.getStartDate(), "No especificado"));
        binding.hotelEndDate.setText(valueOrFallback(hotel.getEndDate(), "No especificado"));
        binding.hotelNotes.setText(valueOrFallback(hotel.getNotes(), "Sin observaciones"));
    }

    private String valueOrFallback(String value, String fallback) {
        if (value == null) return fallback;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private void showMessage(String text, boolean isError) {
        binding.errorText.setText(text);
        binding.errorText.setVisibility(View.VISIBLE);
        binding.hotelContent.setVisibility(View.GONE);

        int color = ContextCompat.getColor(
                this,
                isError ? R.color.ans_secondary : R.color.ans_primary_dark
        );
        binding.errorText.setTextColor(color);
    }
}