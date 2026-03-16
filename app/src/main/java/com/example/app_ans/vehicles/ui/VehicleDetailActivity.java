package com.example.app_ans.vehicles.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.app_ans.R;
import com.example.app_ans.auth.model.UserSession;
import com.example.app_ans.auth.session.AuthSessionManager;
import com.example.app_ans.auth.ui.AuthActivity;
import com.example.app_ans.core.network.NetworkModule;
import com.example.app_ans.core.ui.NavbarUtils;
import com.example.app_ans.core.utils.FileStorageUtils;
import com.example.app_ans.databinding.ActivityVehicleDetailBinding;
import com.example.app_ans.vehicles.model.Vehicle;
import com.example.app_ans.vehicles.repository.VehicleRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class VehicleDetailActivity extends AppCompatActivity {

    private ActivityVehicleDetailBinding binding;
    private VehicleViewModel viewModel;
    private Vehicle currentVehicle;
    private MaintenanceDialog activeMaintenanceDialog;
    private String pendingFieldId;
    private boolean isAdmin = false;

    private final ActivityResultLauncher<String[]> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(),
            uris -> {
                if (!isAdmin) return;

                if (uris != null && !uris.isEmpty() && pendingFieldId != null && activeMaintenanceDialog != null) {
                    Uri firstValidUri = null;
                    for (Uri uri : uris) {
                        if (FileStorageUtils.isValidFile(this, uri)) {
                            firstValidUri = uri;
                            break;
                        } else {
                            Toast.makeText(this, "Archivo no permitido: " + FileStorageUtils.getFileName(this, uri), Toast.LENGTH_SHORT).show();
                        }
                    }

                    if (firstValidUri != null) {
                        String fileName = getFileName(firstValidUri);
                        activeMaintenanceDialog.onImageSelected(pendingFieldId, firstValidUri, fileName);
                        pendingFieldId = null;
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityVehicleDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AuthSessionManager sessionManager = new AuthSessionManager(this);
        UserSession session = sessionManager.restoreSession();
        String role = session != null ? session.getRole() : null;
        isAdmin = isAdminRole(role);

        setupNavbar();
        setupViewModel();

        if (binding.backToHome != null) {
            binding.backToHome.setOnClickListener(v -> finish());
        }

        if (!isAdmin) {
            showNavbarOnly();
            return;
        }

        observeViewModel();
        viewModel.loadMyVehicle();
    }

    private boolean isAdminRole(String role) {
        if (role == null) return false;
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("administrador") || normalized.equals("admin");
    }

    private void showNavbarOnly() {
        if (binding.vehicleContent != null) {
            binding.vehicleContent.setVisibility(View.VISIBLE);
        }

        if (binding.backToHome != null) {
            binding.backToHome.setVisibility(View.VISIBLE);
            binding.backToHome.setOnClickListener(v -> finish());
        }

        if (binding.vehicleBodyContent != null) {
            binding.vehicleBodyContent.setVisibility(View.GONE);
        }

        if (binding.loadingProgress != null) {
            binding.loadingProgress.setVisibility(View.GONE);
        }

        if (binding.errorText != null) {
            binding.errorText.setVisibility(View.GONE);
        }
    }

    private void setupNavbar() {
        NavbarUtils.setupNavbar(
                this,
                null,
                () -> {
                    Intent intent = new Intent(this, AuthActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
        );

        binding.navbar.navbarTitle.setVisibility(View.GONE);
        binding.navbar.navbarBackButton.setVisibility(View.GONE);
    }

    private void setupViewModel() {
        AuthSessionManager sessionManager = new AuthSessionManager(this);
        VehicleRepository repository = new VehicleRepository(
                NetworkModule.provideVehicleApi(this, sessionManager)
        );
        viewModel = new ViewModelProvider(this, new VehicleViewModelFactory(repository))
                .get(VehicleViewModel.class);
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, loading ->
                binding.loadingProgress.setVisibility(loading ? View.VISIBLE : View.GONE)
        );

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                binding.errorText.setText(error);
                binding.errorText.setVisibility(View.VISIBLE);
                binding.vehicleContent.setVisibility(View.GONE);
            } else {
                binding.errorText.setVisibility(View.GONE);
            }
        });

        viewModel.getVehicle().observe(this, this::displayVehicle);

        viewModel.getMaintenanceConfig().observe(this, config -> {
            if (config != null && currentVehicle != null) {
                showMaintenanceDialog(config);
            }
        });

        viewModel.getMaintenanceSubmitted().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Mantenimiento reportado correctamente", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayVehicle(Vehicle vehicle) {
        if (vehicle == null) return;
        this.currentVehicle = vehicle;

        binding.vehicleContent.setVisibility(View.VISIBLE);
        binding.errorText.setVisibility(View.GONE);

        binding.vehiclePlate.setText(vehicle.getPlate());
        binding.vehicleBrandModel.setText(vehicle.getBrand() + " " + vehicle.getModel());
        binding.vehicleYear.setText(String.valueOf(vehicle.getYear()));
        binding.vehicleOdometer.setText(String.format("%.1f Km", vehicle.getOdometer()));

        if (vehicle.getType() != null) {
            binding.vehicleType.setText(vehicle.getType().getName());
        } else {
            binding.vehicleType.setText("No especificado");
        }

        if (vehicle.getProvider() != null) {
            binding.vehicleProvider.setText(vehicle.getProvider().getName());
        } else {
            binding.vehicleProvider.setText("ANS Comunicaciones S.A.S.");
        }

        if (vehicle.getMaintenanceStatus() != null) {
            binding.maintenanceInfoContainer.setVisibility(View.VISIBLE);
            Vehicle.MaintenanceStatus status = vehicle.getMaintenanceStatus();

            if (status.isDue()) {
                binding.maintenanceDueStatus.setText("¡Mantenimiento Vencido!");
                binding.maintenanceDueStatus.setTextColor(getResources().getColor(R.color.ans_secondary));
            } else {
                binding.maintenanceDueStatus.setText("Al día");
                binding.maintenanceDueStatus.setTextColor(getResources().getColor(R.color.ans_primary));
            }

            binding.maintenanceNextDue.setText(String.format("Próximo: %.1f Km", status.getNextDueKms()));
            binding.maintenanceRemaining.setText(String.format("Faltan: %.1f Km", status.getRemainingKms()));

            if (status.isCanFill()) {
                binding.maintenanceButton.setVisibility(View.VISIBLE);
                binding.maintenanceButton.setOnClickListener(v -> viewModel.loadMaintenanceConfig("periodic"));
            } else {
                binding.maintenanceButton.setVisibility(View.GONE);
            }
        } else {
            binding.maintenanceInfoContainer.setVisibility(View.GONE);
            binding.maintenanceButton.setVisibility(View.GONE);
        }

        if (vehicle.getActiveImage() != null && vehicle.getActiveImage().getPreviewUrl() != null) {
            String processedUrl = vehicle.getActiveImage().getPreviewUrl();

            if (processedUrl.startsWith("/")) {
                processedUrl = com.example.app_ans.BuildConfig.API_BASE_URL + processedUrl;
            }

            if (processedUrl.contains("localhost")) {
                processedUrl = processedUrl.replace("localhost", "10.0.2.2");
            } else if (processedUrl.contains("127.0.0.1")) {
                processedUrl = processedUrl.replace("127.0.0.1", "10.0.2.2");
            }

            final String imageUrl = processedUrl;

            Log.d("VehicleDetail", "Loading image: " + imageUrl);

            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.vehicle)
                    .error(R.drawable.vehicle)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e("VehicleDetail", "Error loading image: " + imageUrl, e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d("VehicleDetail", "Image loaded successfully");
                            return false;
                        }
                    })
                    .into(binding.vehicleImage);
        } else {
            binding.vehicleImage.setImageResource(R.drawable.vehicle);
        }
    }

    private void showMaintenanceDialog(Map<String, Object> config) {
        activeMaintenanceDialog = new MaintenanceDialog(this, config, (dataJson, images) -> {
            List<MultipartBody.Part> imageParts = new ArrayList<>();

            for (Map.Entry<String, Uri> entry : images.entrySet()) {
                try {
                    File file = FileStorageUtils.fromUri(this, entry.getValue());
                    if (file != null) {
                        File compressed = FileStorageUtils.getCompressedFile(this, file);
                        RequestBody requestFile = RequestBody.create(compressed, MediaType.parse("image/*"));
                        imageParts.add(MultipartBody.Part.createFormData(
                                "images[" + entry.getKey() + "]",
                                compressed.getName(),
                                requestFile
                        ));
                    }
                } catch (Exception e) {
                    Log.e("VehicleDetail", "Error processing image for field " + entry.getKey(), e);
                }
            }

            double odometer = currentVehicle != null ? currentVehicle.getOdometer() : 0.0;
            viewModel.submitMaintenance(currentVehicle.getId(), "periodic", odometer, dataJson, imageParts);
        });

        activeMaintenanceDialog.setImagePickerLauncher(fieldName -> {
            pendingFieldId = fieldName;
            String[] mimeTypes = {
                    "image/jpeg", "image/png", "application/pdf",
                    "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "application/x-rar-compressed", "application/vnd.rar", "application/octet-stream"
            };
            imagePickerLauncher.launch(mimeTypes);
        });

        activeMaintenanceDialog.show();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int colIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (colIndex != -1) {
                        result = cursor.getString(colIndex);
                    }
                }
            }
        }

        if (result == null) {
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : -1;
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }

        return result;
    }
}