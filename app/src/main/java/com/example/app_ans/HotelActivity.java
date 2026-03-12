package com.example.app_ans;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.app_ans.databinding.ActivityHotelBinding;

public class HotelActivity extends AppCompatActivity {

    private ActivityHotelBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHotelBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        com.example.app_ans.auth.session.AuthSessionManager sessionManager =
                new com.example.app_ans.auth.session.AuthSessionManager(this);

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
    }
}