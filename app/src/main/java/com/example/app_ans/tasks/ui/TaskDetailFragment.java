package com.example.app_ans.tasks.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.app_ans.R;
import com.example.app_ans.core.persistence.AppDatabase;
import com.example.app_ans.core.utils.DateUtils;
import com.example.app_ans.databinding.FragmentTaskDetailBinding;
import com.example.app_ans.tasks.model.AssignedUser;
import com.example.app_ans.tasks.model.Task;
import com.example.app_ans.tasks.model.TaskAdvance;
import com.example.app_ans.tasks.persistence.PendingRendition;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskDetailFragment extends Fragment {
    private static final String ARG_TASK_DATA = "TASK_DATA";
    private static final String ARG_TASK_ID = "TASK_ID";
    private static final String ARG_COORDINATOR_MODE = "COORDINATOR_MODE";

    private FragmentTaskDetailBinding binding;
    private TaskViewModel viewModel;
    private com.example.app_ans.vehicles.ui.VehicleViewModel vehicleViewModel;
    private Task task;

    private boolean coordinatorMode = false;

    private TaskRenditionAdapter renditionAdapter;
    private TaskAdvanceAdapter advanceAdapter;
    private TaskAdvanceDialog currentAdvanceDialog;
    private TaskCompleteDialog currentCompleteDialog;
    private int pendingQuestionId;
    private String currentDraftType = "";
    private com.example.app_ans.vehicles.ui.MaintenanceDialog activeMaintenanceDialog;
    private String pendingFieldId;

    private FusedLocationProviderClient fusedLocationClient;
    private Marker userMarker;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private long timerBaseTime = 0;

    private final ExecutorService renditionValidationExecutor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean fineLocation = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocation = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if ((fineLocation != null && fineLocation) || (coarseLocation != null && coarseLocation)) {
                    fetchCurrentLocation();
                } else {
                    Toast.makeText(getContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<String[]> advanceFilePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(),
            uris -> {
                if (uris != null && !uris.isEmpty() && currentAdvanceDialog != null) {
                    List<Uri> validUris = new ArrayList<>();
                    for (Uri uri : uris) {
                        if (com.example.app_ans.core.utils.FileStorageUtils.isValidFile(getContext(), uri)) {
                            validUris.add(uri);
                        } else {
                            Toast.makeText(
                                    getContext(),
                                    "Archivo no permitido: " + com.example.app_ans.core.utils.FileStorageUtils.getFileName(getContext(), uri),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                    if (!validUris.isEmpty()) {
                        currentAdvanceDialog.addFiles(validUris);
                    }
                }
            }
    );

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && currentCompleteDialog != null) {
                    if (com.example.app_ans.core.utils.FileStorageUtils.isValidFile(getContext(), uri)) {
                        currentCompleteDialog.onFileSelected(pendingQuestionId, uri, "Archivo seleccionado");
                    } else {
                        Toast.makeText(getContext(), "Tipo de archivo no permitido", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private final ActivityResultLauncher<String[]> maintenanceImagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(),
            uris -> {
                if (uris != null && !uris.isEmpty() && pendingFieldId != null && activeMaintenanceDialog != null) {
                    Uri firstValidUri = null;
                    for (Uri uri : uris) {
                        if (com.example.app_ans.core.utils.FileStorageUtils.isValidFile(getContext(), uri)) {
                            firstValidUri = uri;
                            break;
                        }
                    }

                    if (firstValidUri != null) {
                        String fileName = com.example.app_ans.core.utils.FileStorageUtils.getFileName(getContext(), firstValidUri);
                        activeMaintenanceDialog.onImageSelected(pendingFieldId, firstValidUri, fileName);
                        pendingFieldId = null;
                    }
                }
            }
    );

    public static TaskDetailFragment newInstance(Task task) {
        return newInstance(task, false);
    }

    public static TaskDetailFragment newInstance(Task task, boolean coordinatorMode) {
        TaskDetailFragment fragment = new TaskDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TASK_DATA, task);
        args.putBoolean(ARG_COORDINATOR_MODE, coordinatorMode);
        fragment.setArguments(args);
        return fragment;
    }

    public static TaskDetailFragment newInstance(int taskId) {
        TaskDetailFragment fragment = new TaskDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TASK_ID, taskId);
        args.putBoolean(ARG_COORDINATOR_MODE, false);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTaskDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Configuration.getInstance().load(
                getContext(),
                android.preference.PreferenceManager.getDefaultSharedPreferences(getContext())
        );

        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            coordinatorMode = getArguments().getBoolean(ARG_COORDINATOR_MODE, false);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        viewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        setupVehicleViewModel();
        setupBackNavigation();
        setupRecyclerView();
        setupListeners();
        setupObservers();

        viewModel.loadTasks();

        if (getArguments() != null) {
            if (getArguments().containsKey(ARG_TASK_DATA)) {
                task = (Task) getArguments().getSerializable(ARG_TASK_DATA);
                setupUI();

                if (!coordinatorMode) {
                    viewModel.loadTaskDetail(task.getId());
                    viewModel.loadPendingAdvances(task.getId());
                    viewModel.loadPendingRenditions(task.getId());
                    viewModel.observeUploadStatus(task.getId(), getViewLifecycleOwner());
                } else {
                    updateUIWithDetails(task);
                    updateTimeLogsHistory();
                    updateTimingUI();
                }
            } else if (getArguments().containsKey(ARG_TASK_ID)) {
                int taskId = getArguments().getInt(ARG_TASK_ID);

                viewModel.loadTaskDetail(taskId);
                viewModel.loadPendingAdvances(taskId);
                viewModel.loadPendingRenditions(taskId);
                viewModel.observeUploadStatus(taskId, getViewLifecycleOwner());
            }
        }

        applyCoordinatorMode();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (binding != null) {
            binding.mapView.onResume();
        }

        if (!coordinatorMode) {
            viewModel.loadTasks();
        }

        if (task != null) {
            if (task.isRunning()) {
                calculateTimerBaseAndStart();
            }

            if (coordinatorMode) {
                setupUI();
                updateUIWithDetails(task);
                updateTimeLogsHistory();
                updateTimingUI();
            } else {
                viewModel.loadTaskDetail(task.getId());
                viewModel.loadPendingRenditions(task.getId());
            }
        }
    }

    @Override
    public void onPause() {
        if (binding != null) {
            binding.mapView.onPause();
        }
        stopTimer();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        stopTimer();
        renditionValidationExecutor.shutdown();

        View activityBack = requireActivity().findViewById(R.id.back_to_home);
        if (activityBack != null) {
            activityBack.setVisibility(View.VISIBLE);
        }

        super.onDestroyView();
        binding = null;
    }

    private void setupBackNavigation() {
        View activityBack = requireActivity().findViewById(R.id.back_to_home);
        if (activityBack != null) {
            activityBack.setVisibility(coordinatorMode ? View.GONE : View.VISIBLE);
        }

        if (coordinatorMode) {
            binding.backToTasks.setVisibility(View.VISIBLE);
            binding.backToTasks.setText("←  Volver a subórdenes");
        } else {
            binding.backToTasks.setVisibility(View.VISIBLE);
            binding.backToTasks.setText("←  Volver a tareas");
        }

        binding.backToTasks.setOnClickListener(v -> {
            if (coordinatorMode) {
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack();
            } else {
                requireActivity().finish();
            }
        });
    }

    private void setupUI() {
        renderHeader();
        renderDescription();
        renderLocationSection();
        renderVehicleSection();
        renderPreviousTicketSection();
        applyCoordinatorMode();
    }

    private void renderHeader() {
        if (task == null) return;

        binding.detailPublicId.setText(task.getPublicId());
        binding.detailStatus.setText(task.getStatus());
        com.example.app_ans.core.ui.StatusUtils.applyStatusColor(binding.detailStatus, task.getStatus());

        binding.detailTitle.setText(task.getName() != null ? task.getName() : "");

        String startTime = DateUtils.formatDateTime(task.getStartTime());
        String endTime = DateUtils.formatDateTime(task.getEndTime());
        binding.detailDateRange.setText(startTime + " - " + endTime);
    }

    private void renderDescription() {
        if (task == null) return;

        String description = task.getDescription();
        if (description != null && !description.trim().isEmpty()) {
            binding.detailsLabel.setVisibility(View.VISIBLE);
            binding.detailDescription.setVisibility(View.VISIBLE);
            binding.detailDescription.setText(description);
        } else {
            binding.detailsLabel.setVisibility(View.GONE);
            binding.detailDescription.setVisibility(View.GONE);
        }
    }

    private void renderLocationSection() {
        if (task == null) return;

        if (task.getLatitude() != null && task.getLongitude() != null) {
            binding.locationContainer.setVisibility(View.VISIBLE);

            double lat = task.getLatitude();
            double lon = task.getLongitude();

            binding.detailLocation.setText("Lat: " + lat + ", Lon: " + lon);
            setupMap(lat, lon);
        } else {
            binding.locationContainer.setVisibility(View.GONE);
        }
    }

    private void renderVehicleSection() {
        if (task == null) return;

        if (task.getVehicle() != null) {
            binding.vehicleContainer.setVisibility(View.VISIBLE);
            binding.detailVehiclePlate.setText(task.getVehicle().getPlate());

            String vehicleInfo = task.getVehicle().getBrand() + " "
                    + task.getVehicle().getModel() + " ("
                    + task.getVehicle().getYear() + ")";
            binding.detailVehicleInfo.setText(vehicleInfo);

            String typeName = task.getVehicle().getType() != null
                    ? task.getVehicle().getType().getName()
                    : "Vehículo";
            String stateName = task.getVehicle().getState() != null
                    ? task.getVehicle().getState().getName()
                    : "";

            String extraInfo = typeName + " | " + String.format("%.1f Km", task.getVehicle().getOdometer());
            if (!stateName.isEmpty()) {
                extraInfo += " | " + stateName;
            }
            binding.detailVehicleExtra.setText(extraInfo);

            if (task.getVehicle().getProvider() != null) {
                binding.detailVehicleProvider.setText(task.getVehicle().getProvider().getName());
            } else {
                binding.detailVehicleProvider.setText("ANS Comunicaciones S.A.S.");
            }

            if (!coordinatorMode
                    && task.getVehicle().getMaintenanceStatus() != null
                    && task.getVehicle().getMaintenanceStatus().isCanFill()) {
                binding.btnFillMaintenance.setVisibility(View.VISIBLE);
                binding.btnFillMaintenance.setOnClickListener(v -> showMaintenanceForm(task.getVehicle()));
            } else {
                binding.btnFillMaintenance.setVisibility(View.GONE);
            }
        } else if (task.getVehiclePlate() != null && !task.getVehiclePlate().isEmpty()) {
            binding.vehicleContainer.setVisibility(View.VISIBLE);
            binding.detailVehiclePlate.setText(task.getVehiclePlate());
            binding.detailVehicleInfo.setText("Cargando detalles del vehículo...");
            binding.btnFillMaintenance.setVisibility(View.GONE);
        } else {
            binding.vehicleContainer.setVisibility(View.GONE);
        }
    }

    private void renderPreviousTicketSection() {
        if (task == null) return;

        if (task.getPreviousTicket() != null) {
            binding.previousTicketContainer.setVisibility(View.VISIBLE);
            binding.previousTicketInfo.setText(task.getPreviousTicket().getPublicId());
        } else {
            binding.previousTicketContainer.setVisibility(View.GONE);
        }
    }

    private void calculateTimerBaseAndStart() {
        if (task == null || !task.isRunning() || coordinatorMode) return;
        if (timerRunnable != null) return;

        long totalElapsedSecondsCalculated = DateUtils.getTotalRunningSeconds(task);
        timerBaseTime = SystemClock.elapsedRealtime() - (totalElapsedSecondsCalculated * 1000);

        startTimer();
        updateTimingUI();
    }

    private void startTimer() {
        if (task == null || !task.isRunning() || coordinatorMode) return;
        if (timerRunnable != null) return;

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (binding == null || task == null || !task.isRunning()) {
                    stopTimer();
                    return;
                }

                long totalCurrentSeconds = (SystemClock.elapsedRealtime() - timerBaseTime) / 1000;
                if (totalCurrentSeconds < 0) totalCurrentSeconds = 0;

                String formatted = DateUtils.formatSecondsToDuration(totalCurrentSeconds);
                binding.tvTotalTime.setText(formatted);
                task.setTotalTimeSpent(formatted);

                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    private void setupRecyclerView() {
        advanceAdapter = new TaskAdvanceAdapter();
        advanceAdapter.setOnAdvanceActionListener(new TaskAdvanceAdapter.OnAdvanceActionListener() {
            @Override
            public void onEdit(TaskAdvance advance) {
                if (coordinatorMode) return;

                TaskAdvanceDialog editDialog = new TaskAdvanceDialog(getContext(), new TaskAdvanceDialog.OnAdvanceSubmitListener() {
                    @Override
                    public void onSubmit(String content, List<Uri> files) {
                        viewModel.updateAdvance(task.getId(), advance.getId(), content);
                    }

                    @Override
                    public void onPickFiles() {
                    }
                });
                editDialog.setInitialContent(advance.getComment());
                editDialog.show();
            }

            @Override
            public void onDelete(TaskAdvance advance) {
                if (coordinatorMode) return;

                new AlertDialog.Builder(getContext())
                        .setTitle("Eliminar Avance")
                        .setMessage("¿Estás seguro de que deseas eliminar este avance?")
                        .setPositiveButton("Eliminar", (dialog, which) ->
                                viewModel.deleteAdvance(task.getId(), advance.getId()))
                        .setNegativeButton("Cancelar", null)
                        .show();
            }

            @Override
            public void onFileClick(String pathOrUrl, String mimeType) {
                if (pathOrUrl == null) return;

                boolean isDriveWebLink = pathOrUrl.contains("drive.google.com") && pathOrUrl.contains("/view");

                if (mimeType != null && mimeType.startsWith("image/") && !isDriveWebLink) {
                    showFullScreenImage(pathOrUrl);
                } else {
                    try {
                        Uri uri = Uri.parse(pathOrUrl);
                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, uri);

                        if (mimeType != null && !mimeType.isEmpty() && !isDriveWebLink) {
                            intent.setDataAndType(uri, mimeType);
                        } else {
                            intent.setData(uri);
                        }

                        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    } catch (Exception e) {
                        try {
                            startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(pathOrUrl)));
                        } catch (Exception e2) {
                            Toast.makeText(getContext(), "No se puede abrir el archivo", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });

        binding.advancesRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.advancesRecycler.setAdapter(advanceAdapter);

        renditionAdapter = new TaskRenditionAdapter();
        binding.renditionsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.renditionsRecycler.setAdapter(renditionAdapter);
    }

    private void showFullScreenImage(String imageUrl) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_image_viewer, null);
        ImageView imageView = dialogView.findViewById(R.id.full_screen_image);
        View btnClose = dialogView.findViewById(R.id.btn_close_viewer);

        com.bumptech.glide.Glide.with(this)
                .load(imageUrl)
                .into(imageView);

        AlertDialog dialog = new AlertDialog.Builder(
                getContext(),
                android.R.style.Theme_Black_NoTitleBar_Fullscreen
        ).setView(dialogView).create();

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setupListeners() {
        binding.btnMyLocation.setOnClickListener(v -> fetchCurrentLocation());
        binding.btnCheckLocationTop.setOnClickListener(v -> fetchCurrentLocation());

        if (getActivity() instanceof com.example.app_ans.TasksActivity) {
            binding.btnOpenFull.setVisibility(View.VISIBLE);
            binding.btnOpenFull.setOnClickListener(v -> {
                if (task != null) {
                    android.content.Intent intent = new android.content.Intent(
                            getContext(),
                            com.example.app_ans.tasks.ui.TaskDetailActivity.class
                    );
                    intent.putExtra("TASK_DATA", task);
                    startActivity(intent);
                }
            });
        } else {
            binding.btnOpenFull.setVisibility(View.GONE);
        }

        binding.btnTimer.setOnClickListener(v -> {
            if (!coordinatorMode) {
                handleTimerClick();
            }
        });

        binding.btnHistoryDropdown.setOnClickListener(v -> {
            boolean isVisible = binding.historyContainer.getVisibility() == View.VISIBLE;
            binding.historyContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            binding.ivHistoryArrow.setRotation(isVisible ? 0 : 180);
        });

        binding.btnCompleteTask.setOnClickListener(v -> {
            if (coordinatorMode) return;
            currentDraftType = "COMPLETION";
            viewModel.loadDraft(currentDraftType, task.getId());
        });

        binding.btnAddAdvance.setOnClickListener(v -> {
            if (coordinatorMode) return;
            currentDraftType = "ADVANCE";
            viewModel.loadDraft(currentDraftType, task.getId());
        });

        binding.btnAddRendition.setOnClickListener(v -> {
            if (!coordinatorMode) {
                openTaskRenditionFragment();
            }
        });

        View searchBarRoot = binding.getRoot().findViewById(R.id.advance_search_bar);
        if (searchBarRoot != null) {
            androidx.appcompat.widget.SearchView searchView = searchBarRoot.findViewById(R.id.search_view);
            if (searchView != null) {
                searchView.setQueryHint("Buscar por texto, técnico o fecha/hora...");
                searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        if (advanceAdapter != null) {
                            advanceAdapter.filter(query);
                        }
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        if (advanceAdapter != null) {
                            advanceAdapter.filter(newText);
                        }
                        return true;
                    }
                });
            }
        }
    }

    private void openTaskRenditionFragment() {
        if (task == null) return;

        renditionValidationExecutor.execute(() -> {
            try {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                long startOfDay = calendar.getTimeInMillis();

                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                long endOfDay = calendar.getTimeInMillis();

                AppDatabase db = AppDatabase.getInstance(requireContext().getApplicationContext());
                int count = db.pendingRenditionDao().countByTaskIdAndDay(task.getId(), startOfDay, endOfDay);

                requireActivity().runOnUiThread(() -> {
                    if (count > 0) {
                        showRenditionBlockedDialog();
                    } else {
                        int containerId = ((ViewGroup) requireView().getParent()).getId();

                        requireActivity()
                                .getSupportFragmentManager()
                                .beginTransaction()
                                .replace(containerId, TaskRenditionFragment.newInstance(task.getId()))
                                .addToBackStack(null)
                                .commit();
                    }
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "No se pudo validar la rendición del día", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private boolean hasAnotherRunningTask() {
        if (viewModel == null || task == null) return false;

        List<Task> tasks = viewModel.getTasks().getValue();
        if (tasks == null || tasks.isEmpty()) return false;

        for (Task item : tasks) {
            if (item != null && item.getId() != task.getId() && item.isRunning()) {
                return true;
            }
        }

        return false;
    }

    private void showTaskBlockedDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_task_blocked, null);

        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        TextView btnOk = dialogView.findViewById(R.id.btnDialogOk);

        tvMessage.setText("Ya tienes otra tarea en curso. Debes finalizarla o pausarla antes de iniciar una nueva.");

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnOk.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showRenditionBlockedDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_task_blocked, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        TextView btnOk = dialogView.findViewById(R.id.btnDialogOk);

        if (tvTitle != null) {
            tvTitle.setText("No puedes crear otra rendición hoy");
        }

        tvMessage.setText("Ya existe una rendición registrada para esta tarea el día de hoy. Solo puedes crear una rendición por día para esta tarea.");

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnOk.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void handleTimerClick() {
        if (task == null) return;

        if (task.isRunning()) {
            viewModel.toggleTimer(task.getId(), binding.tvTotalTime.getText().toString());
            return;
        }

        if (hasAnotherRunningTask()) {
            showTaskBlockedDialog();
            return;
        }

        validateLocationAndStartTask();
    }

    private void validateLocationAndStartTask() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }

        binding.btnTimer.setEnabled(false);
        Toast.makeText(getContext(), "Validando ubicación para iniciar...", Toast.LENGTH_SHORT).show();

        CancellationTokenSource cts = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(location -> {
                    binding.btnTimer.setEnabled(true);
                    if (location != null) {
                        boolean hasTaskLocation = task.getLatitude() != null
                                && task.getLongitude() != null;

                        if (hasTaskLocation) {
                            float[] results = new float[1];
                            Location.distanceBetween(
                                    location.getLatitude(), location.getLongitude(),
                                    task.getLatitude(), task.getLongitude(),
                                    results
                            );
                            float distance = results[0];

                            if (distance <= 100) {
                                proceedWithStartTask(location);
                            } else {
                                showFarDistanceDialog(distance);
                            }
                        } else {
                            proceedWithStartTask(location);
                        }
                    } else {
                        Toast.makeText(
                                getContext(),
                                "No se pudo obtener la ubicación actual. Por favor verifica tu GPS.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.btnTimer.setEnabled(true);
                    Toast.makeText(getContext(), "Error al validar ubicación: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showFarDistanceDialog(float distance) {
        String msg;
        if (distance < 1000) {
            msg = String.format(
                    "Te encuentras a %.0f metros de la tarea. Debes estar a menos de 100 metros para poder iniciarla.",
                    distance
            );
        } else {
            msg = String.format(
                    "Te encuentras a %.2f km de la tarea. Debes estar a menos de 100 metros para poder iniciarla.",
                    distance / 1000f
            );
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Fuera de Rango")
                .setMessage(msg)
                .setPositiveButton("Entendido", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void proceedWithStartTask(Location location) {
        viewModel.toggleTimer(task.getId(), binding.tvTotalTime.getText().toString());
        viewModel.recordTechnicianLocation(task.getId(), location.getLatitude(), location.getLongitude());
        updateMapWithUserLocation(location.getLatitude(), location.getLongitude());

        if (task.getLatitude() != null && task.getLongitude() != null) {
            float[] results = new float[1];
            Location.distanceBetween(
                    location.getLatitude(), location.getLongitude(),
                    task.getLatitude(), task.getLongitude(),
                    results
            );
            updateDistanceUI(results[0]);
        }
    }

    private void showCompleteDialog(List<com.example.app_ans.tasks.model.TaskQuestion> questions, String draft) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            currentCompleteDialog = new TaskCompleteDialog(getContext(), questions, (answers, files) -> {
                viewModel.completeTask(task.getId(), answers, new ArrayList<>(files.values()));
            });

            if (draft != null) {
                currentCompleteDialog.setInitialDraft(draft);
            }

            currentCompleteDialog.setOnDraftChangedListener(newDraft ->
                    viewModel.saveDraft("COMPLETION", task.getId(), newDraft));

            currentCompleteDialog.setOnDismissListener(() -> currentCompleteDialog = null);

            currentCompleteDialog.setFilePickerLauncher(questionId -> {
                pendingQuestionId = questionId;
                filePickerLauncher.launch("*/*");
            });

            currentCompleteDialog.show();
        });
    }

    private void showAdvanceDialog(String recoveredDraft) {
        currentAdvanceDialog = new TaskAdvanceDialog(getContext(), new TaskAdvanceDialog.OnAdvanceSubmitListener() {
            @Override
            public void onSubmit(String content, List<Uri> files) {
                viewModel.saveAdvanceOffline(task.getId(), content, files);
                Toast.makeText(getContext(), "Avance guardado localmente", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPickFiles() {
                String[] mimeTypes = {
                        "image/jpeg", "image/png", "application/pdf",
                        "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "application/x-rar-compressed", "application/vnd.rar", "application/octet-stream"
                };
                advanceFilePickerLauncher.launch(mimeTypes);
            }
        });

        if (recoveredDraft != null) {
            currentAdvanceDialog.setInitialContent(recoveredDraft);
        }

        currentAdvanceDialog.setOnDraftChangedListener(content ->
                viewModel.saveDraft("ADVANCE", task.getId(), content));

        currentAdvanceDialog.setOnDismissListener(() -> currentAdvanceDialog = null);
        currentAdvanceDialog.show();
    }

    private void setupObservers() {
        viewModel.getDraft().observe(getViewLifecycleOwner(), draft -> {
            if (task == null || coordinatorMode) return;

            if ("ADVANCE".equals(currentDraftType) && currentAdvanceDialog == null) {
                showAdvanceDialog(draft);
            } else if ("COMPLETION".equals(currentDraftType) && currentCompleteDialog == null) {
                viewModel.loadQuestions(task.getId(), questions -> showCompleteDialog(questions, draft));
            }
        });

        viewModel.getTaskDetail().observe(getViewLifecycleOwner(), detailedTask -> {
            if (detailedTask == null) {
                binding.detailTechnicians.setText("Cargando...");
                binding.techniciansListContainer.removeAllViews();
                advanceAdapter.setAdvances(new ArrayList<>());
                return;
            }

            if (this.task == null || detailedTask.getId() == this.task.getId()) {
                boolean wasRunning = this.task != null && this.task.isRunning();
                boolean isNowRunning = detailedTask.isRunning();
                boolean statusToggled = wasRunning != isNowRunning;

                this.task = detailedTask;
                setupUI();
                updateUIWithDetails(detailedTask);
                updateTimeLogsHistory();

                if (!coordinatorMode) {
                    if (isNowRunning) {
                        binding.btnTimer.setText("Pausar");
                        binding.btnTimer.setIconResource(R.drawable.ic_pause_24);
                        int color = androidx.core.content.ContextCompat.getColor(getContext(), R.color.ans_secondary);
                        binding.btnTimer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));

                        if (statusToggled || timerRunnable == null) {
                            calculateTimerBaseAndStart();
                        }
                    } else {
                        stopTimer();
                        binding.btnTimer.setText("Iniciar");
                        binding.btnTimer.setIconResource(R.drawable.ic_play_arrow_24);
                        int color = androidx.core.content.ContextCompat.getColor(getContext(), R.color.ans_primary);
                        binding.btnTimer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
                        binding.tvCurrentStartTime.setVisibility(View.GONE);
                        binding.tvTotalTime.setText(detailedTask.getTotalTimeSpent());
                    }
                } else {
                    stopTimer();
                    binding.tvCurrentStartTime.setVisibility(View.GONE);
                    binding.tvTotalTime.setText(detailedTask.getTotalTimeSpent());
                }

                updateTimingUI();
            }
        });

        viewModel.getPendingRenditions().observe(getViewLifecycleOwner(), pendingRenditions -> {
            if (renditionAdapter != null) {
                renditionAdapter.setItems(pendingRenditions);
            }
            checkEmptyRenditions(pendingRenditions);
        });
    }

    private void updateUIWithDetails(Task task) {
        updateTechniciansDropdown(task);

        advanceAdapter.setAdvances(task.getAdvances() != null
                ? task.getAdvances()
                : new ArrayList<>());

        checkEmptyAdvances();
    }

    private void updateTechniciansDropdown(Task task) {
        if (task == null || binding == null) return;

        binding.techniciansListContainer.removeAllViews();

        List<AssignedUser> users = task.getAssignedUsers();

        if (users == null || users.isEmpty()) {
            binding.detailTechnicians.setText("No asignados");
            binding.techniciansArrow.setVisibility(View.GONE);
            binding.techniciansHeader.setOnClickListener(null);
            binding.techniciansListContainer.setVisibility(View.GONE);
            return;
        }

        int count = users.size();
        binding.detailTechnicians.setText(
                count == 1 ? "1 técnico asignado" : count + " técnicos asignados"
        );

        binding.techniciansArrow.setVisibility(View.VISIBLE);
        binding.techniciansArrow.setRotation(0f);
        binding.techniciansListContainer.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (AssignedUser user : users) {
            View technicianView = inflater.inflate(
                    R.layout.item_assigned_technician,
                    binding.techniciansListContainer,
                    false
            );

            TextView technicianName = technicianView.findViewById(R.id.technician_name);
            technicianName.setText(user.getName());

            binding.techniciansListContainer.addView(technicianView);
        }

        binding.techniciansHeader.setOnClickListener(v -> {
            boolean isVisible = binding.techniciansListContainer.getVisibility() == View.VISIBLE;
            binding.techniciansListContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            binding.techniciansArrow.setRotation(isVisible ? 0f : 90f);
        });
    }

    private void updateTimingUI() {
        if (task == null || getContext() == null) return;

        binding.tvTotalTime.setText(task.getTotalTimeSpent() != null ? task.getTotalTimeSpent() : "00:00:00");

        if (!coordinatorMode && task.isRunning()) {
            binding.btnTimer.setText("Pausar");
            binding.btnTimer.setIconResource(R.drawable.ic_pause_24);
            int color = androidx.core.content.ContextCompat.getColor(getContext(), R.color.ans_secondary);
            binding.btnTimer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));

            if (task.getTimeLogs() != null && !task.getTimeLogs().isEmpty()) {
                Task.TaskTimeLog lastLog = task.getTimeLogs().get(0);
                if (lastLog.getEndTime() == null || lastLog.getEndTime().isEmpty()) {
                    binding.tvCurrentStartTime.setVisibility(View.VISIBLE);
                    binding.tvCurrentStartTime.setText("Inició: " + DateUtils.formatTimeOnly(lastLog.getStartTime()));
                } else {
                    binding.tvCurrentStartTime.setVisibility(View.GONE);
                }
            } else {
                binding.tvCurrentStartTime.setVisibility(View.GONE);
            }
        } else {
            binding.btnTimer.setText("Iniciar");
            binding.btnTimer.setIconResource(R.drawable.ic_play_arrow_24);
            int color = androidx.core.content.ContextCompat.getColor(getContext(), R.color.ans_primary);
            binding.btnTimer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
            binding.tvCurrentStartTime.setVisibility(View.GONE);
        }

        updateTimeLogsHistory();
    }

    private void updateTimeLogsHistory() {
        if (binding == null || task == null) return;

        binding.historyContainer.removeAllViews();

        if (task.getTimeLogs() == null || task.getTimeLogs().isEmpty()) {
            TextView emptyText = new TextView(getContext());
            emptyText.setText("No hay ciclos registrados");
            emptyText.setTextSize(12);
            emptyText.setPadding(0, 8, 0, 8);
            emptyText.setTextColor(getResources().getColor(R.color.ans_gray_3));
            binding.historyContainer.addView(emptyText);
            return;
        }

        if (!coordinatorMode) {
            renderSimpleTimeLogs(task.getTimeLogs());
            return;
        }

        Map<String, Map<String, List<Task.TaskTimeLog>>> groupedByTechnician = new LinkedHashMap<>();

        for (Task.TaskTimeLog log : task.getTimeLogs()) {
            String technicianName = "Técnico no disponible";
            if (log.getUser() != null
                    && log.getUser().getName() != null
                    && !log.getUser().getName().trim().isEmpty()) {
                technicianName = log.getUser().getName();
            }

            String dateKey = formatLogDate(log.getStartTime());

            if (!groupedByTechnician.containsKey(technicianName)) {
                groupedByTechnician.put(technicianName, new LinkedHashMap<>());
            }

            Map<String, List<Task.TaskTimeLog>> technicianDates = groupedByTechnician.get(technicianName);

            if (!technicianDates.containsKey(dateKey)) {
                technicianDates.put(dateKey, new ArrayList<>());
            }

            technicianDates.get(dateKey).add(log);
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (Map.Entry<String, Map<String, List<Task.TaskTimeLog>>> technicianEntry : groupedByTechnician.entrySet()) {
            String technicianName = technicianEntry.getKey();
            Map<String, List<Task.TaskTimeLog>> datesMap = technicianEntry.getValue();

            android.widget.LinearLayout technicianWrapper = new android.widget.LinearLayout(getContext());
            technicianWrapper.setOrientation(android.widget.LinearLayout.VERTICAL);
            technicianWrapper.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            android.widget.LinearLayout headerLayout = new android.widget.LinearLayout(getContext());
            headerLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            headerLayout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            headerLayout.setPadding(0, 16, 0, 8);
            headerLayout.setClickable(true);
            headerLayout.setFocusable(true);

            TextView technicianHeader = new TextView(getContext());
            technicianHeader.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
            ));
            technicianHeader.setText(technicianName);
            technicianHeader.setTextSize(14);
            technicianHeader.setTypeface(null, Typeface.BOLD);
            technicianHeader.setTextColor(getResources().getColor(R.color.ans_primary));

            TextView arrowView = new TextView(getContext());
            arrowView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            arrowView.setText("▶");
            arrowView.setTextSize(14);
            arrowView.setTypeface(null, Typeface.BOLD);
            arrowView.setTextColor(getResources().getColor(R.color.ans_primary));

            headerLayout.addView(technicianHeader);
            headerLayout.addView(arrowView);

            android.widget.LinearLayout contentLayout = new android.widget.LinearLayout(getContext());
            contentLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
            contentLayout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            contentLayout.setVisibility(View.GONE);

            for (Map.Entry<String, List<Task.TaskTimeLog>> dateEntry : datesMap.entrySet()) {
                View dayView = inflater.inflate(
                        R.layout.item_time_log,
                        contentLayout,
                        false
                );

                TextView tvDate = dayView.findViewById(R.id.tv_log_date);
                android.widget.LinearLayout entriesContainer = dayView.findViewById(R.id.layout_log_entries);

                tvDate.setText(dateEntry.getKey());
                entriesContainer.removeAllViews();

                for (Task.TaskTimeLog log : dateEntry.getValue()) {
                    View entryView = inflater.inflate(
                            R.layout.item_time_log_entry,
                            entriesContainer,
                            false
                    );

                    TextView tvTimeRange = entryView.findViewById(R.id.tv_log_time_range);
                    TextView tvDuration = entryView.findViewById(R.id.tv_log_duration);

                    String startRaw = log.getStartTime();
                    String endRaw = log.getEndTime();

                    String formattedStartTime = formatLogTime(startRaw);
                    String formattedEndTime = (endRaw != null && !endRaw.isEmpty())
                            ? formatLogTime(endRaw)
                            : "En progreso...";

                    tvTimeRange.setText(formattedStartTime + " - " + formattedEndTime);
                    tvDuration.setText("Duración: " + (log.getDuration() != null ? log.getDuration() : "---"));

                    entriesContainer.addView(entryView);
                }

                contentLayout.addView(dayView);
            }

            headerLayout.setOnClickListener(v -> {
                boolean isVisible = contentLayout.getVisibility() == View.VISIBLE;
                contentLayout.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                arrowView.setText(isVisible ? "▶" : "▼");
            });

            technicianWrapper.addView(headerLayout);
            technicianWrapper.addView(contentLayout);

            binding.historyContainer.addView(technicianWrapper);
        }
    }

    private void renderSimpleTimeLogs(List<Task.TaskTimeLog> logs) {
        Map<String, List<Task.TaskTimeLog>> groupedLogs = new LinkedHashMap<>();

        for (Task.TaskTimeLog log : logs) {
            String dateKey = formatLogDate(log.getStartTime());

            if (!groupedLogs.containsKey(dateKey)) {
                groupedLogs.put(dateKey, new ArrayList<>());
            }

            groupedLogs.get(dateKey).add(log);
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (Map.Entry<String, List<Task.TaskTimeLog>> entry : groupedLogs.entrySet()) {
            View dayView = inflater.inflate(
                    R.layout.item_time_log,
                    binding.historyContainer,
                    false
            );

            TextView tvDate = dayView.findViewById(R.id.tv_log_date);
            android.widget.LinearLayout entriesContainer = dayView.findViewById(R.id.layout_log_entries);

            tvDate.setText(entry.getKey());
            entriesContainer.removeAllViews();

            for (Task.TaskTimeLog log : entry.getValue()) {
                View entryView = inflater.inflate(
                        R.layout.item_time_log_entry,
                        entriesContainer,
                        false
                );

                TextView tvTimeRange = entryView.findViewById(R.id.tv_log_time_range);
                TextView tvDuration = entryView.findViewById(R.id.tv_log_duration);

                String startRaw = log.getStartTime();
                String endRaw = log.getEndTime();

                String formattedStartTime = formatLogTime(startRaw);
                String formattedEndTime = (endRaw != null && !endRaw.isEmpty())
                        ? formatLogTime(endRaw)
                        : "En progreso...";

                tvTimeRange.setText(formattedStartTime + " - " + formattedEndTime);
                tvDuration.setText("Duración: " + (log.getDuration() != null ? log.getDuration() : "---"));

                entriesContainer.addView(entryView);
            }

            binding.historyContainer.addView(dayView);
        }
    }

    private String formatLogDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "---";

        try {
            java.time.OffsetDateTime dateTime = java.time.OffsetDateTime.parse(raw);
            java.time.format.DateTimeFormatter formatter =
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return dateTime.format(formatter);
        } catch (Exception e) {
            return raw;
        }
    }

    private String formatLogTime(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "---";

        try {
            java.time.OffsetDateTime dateTime = java.time.OffsetDateTime.parse(raw);
            java.time.format.DateTimeFormatter formatter =
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
            return dateTime.format(formatter);
        } catch (Exception e) {
            return raw;
        }
    }

    private void fetchCurrentLocation() {
        if (coordinatorMode) return;

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }

        CancellationTokenSource cts = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        updateMapWithUserLocation(location.getLatitude(), location.getLongitude());

                        if (task != null
                                && task.getLatitude() != null
                                && task.getLongitude() != null) {

                            float[] results = new float[1];
                            Location.distanceBetween(
                                    location.getLatitude(), location.getLongitude(),
                                    task.getLatitude(), task.getLongitude(),
                                    results
                            );
                            updateDistanceUI(results[0]);
                        }

                        if (task != null) {
                            viewModel.recordTechnicianLocation(task.getId(), location.getLatitude(), location.getLongitude());
                        }
                    } else {
                        Toast.makeText(getContext(), "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error al obtener ubicación: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateDistanceUI(float distanceInMeters) {
        if (binding == null) return;

        String distanceText;
        if (distanceInMeters < 1000) {
            distanceText = String.format("Estás a %.0f metros de la tarea", distanceInMeters);
        } else {
            distanceText = String.format("Estás a %.2f km de la tarea", distanceInMeters / 1000f);
        }

        binding.tvDistanceInfo.setText(distanceText);
        binding.tvDistanceInfo.setVisibility(View.VISIBLE);
    }

    private void updateMapWithUserLocation(double lat, double lon) {
        if (binding == null || binding.mapView == null) return;

        GeoPoint userPoint = new GeoPoint(lat, lon);
        binding.mapView.getController().animateTo(userPoint);
        binding.mapView.getController().setZoom(18.0);

        if (userMarker == null) {
            userMarker = new Marker(binding.mapView);
            userMarker.setTitle("Mi ubicación");
            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            userMarker.setIcon(getResources().getDrawable(org.osmdroid.library.R.drawable.marker_default_focused_base));
        }

        userMarker.setPosition(userPoint);

        if (!binding.mapView.getOverlays().contains(userMarker)) {
            binding.mapView.getOverlays().add(userMarker);
        }

        binding.mapView.invalidate();
    }

    private void checkEmptyAdvances() {
        if (binding == null || task == null) return;

        boolean hasServer = task.getAdvances() != null && !task.getAdvances().isEmpty();

        List<?> pendingAdvances = viewModel.getPendingAdvances().getValue();
        boolean hasPending = pendingAdvances != null && !pendingAdvances.isEmpty();

        if (hasServer || (!coordinatorMode && hasPending)) {
            binding.advancesRecycler.setVisibility(View.VISIBLE);
            binding.emptyAdvances.setVisibility(View.GONE);
        } else {
            binding.advancesRecycler.setVisibility(View.GONE);
            binding.emptyAdvances.setVisibility(View.VISIBLE);
        }
    }

    private void checkEmptyRenditions(List<PendingRendition> pendingRenditions) {
        if (binding == null) return;

        boolean hasPending = pendingRenditions != null && !pendingRenditions.isEmpty();

        if (hasPending) {
            binding.renditionsRecycler.setVisibility(View.VISIBLE);
            binding.emptyRenditions.setVisibility(View.GONE);
        } else {
            binding.renditionsRecycler.setVisibility(View.GONE);
            binding.emptyRenditions.setVisibility(View.VISIBLE);
        }
    }

    private void setupVehicleViewModel() {
        com.example.app_ans.auth.session.AuthSessionManager sessionManager =
                new com.example.app_ans.auth.session.AuthSessionManager(getContext());

        com.example.app_ans.vehicles.repository.VehicleRepository repository =
                new com.example.app_ans.vehicles.repository.VehicleRepository(
                        com.example.app_ans.core.network.NetworkModule.provideVehicleApi(getContext(), sessionManager)
                );

        vehicleViewModel = new ViewModelProvider(
                this,
                new com.example.app_ans.vehicles.ui.VehicleViewModelFactory(repository)
        ).get(com.example.app_ans.vehicles.ui.VehicleViewModel.class);

        vehicleViewModel.getMaintenanceConfig().observe(getViewLifecycleOwner(), config -> {
            if (config != null && task != null && task.getVehicle() != null && !coordinatorMode) {
                showMaintenanceDialog(config);
            }
        });

        vehicleViewModel.getMaintenanceSubmitted().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(getContext(), "Mantenimiento reportado correctamente", Toast.LENGTH_SHORT).show();
                viewModel.loadTaskDetail(task.getId());
            }
        });

        vehicleViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupMap(double lat, double lon) {
        if (binding == null || binding.mapView == null) return;

        binding.mapView.setTileSource(TileSourceFactory.MAPNIK);
        binding.mapView.setMultiTouchControls(true);
        binding.mapView.getController().setZoom(17.0);

        GeoPoint startPoint = new GeoPoint(lat, lon);
        binding.mapView.getController().setCenter(startPoint);

        Marker marker = new Marker(binding.mapView);
        marker.setPosition(startPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("Ubicación de la Tarea");

        binding.mapView.getOverlays().clear();
        binding.mapView.getOverlays().add(marker);

        if (userMarker != null) {
            binding.mapView.getOverlays().add(userMarker);
        }

        binding.mapView.invalidate();
    }

    private void showMaintenanceForm(com.example.app_ans.vehicles.model.Vehicle vehicle) {
        vehicleViewModel.loadMaintenanceConfig("periodic");
    }

    private void showMaintenanceDialog(Map<String, Object> config) {
        activeMaintenanceDialog = new com.example.app_ans.vehicles.ui.MaintenanceDialog(
                getContext(),
                config,
                (dataJson, images) -> submitMaintenance(dataJson, images)
        );

        activeMaintenanceDialog.setImagePickerLauncher(fieldName -> {
            pendingFieldId = fieldName;
            maintenanceImagePickerLauncher.launch(new String[]{"image/*"});
        });

        activeMaintenanceDialog.show();
    }

    private void submitMaintenance(String dataJson, Map<String, Uri> images) {
        if (task == null || task.getVehicle() == null) return;

        List<okhttp3.MultipartBody.Part> imageParts = new ArrayList<>();
        for (Map.Entry<String, Uri> entry : images.entrySet()) {
            try {
                java.io.File file = com.example.app_ans.core.utils.FileStorageUtils.fromUri(getContext(), entry.getValue());
                if (file != null) {
                    java.io.File compressed = com.example.app_ans.core.utils.FileStorageUtils.getCompressedFile(getContext(), file);
                    okhttp3.RequestBody requestFile = okhttp3.RequestBody.create(
                            compressed,
                            okhttp3.MediaType.parse("image/jpeg")
                    );
                    imageParts.add(okhttp3.MultipartBody.Part.createFormData(
                            "images[" + entry.getKey() + "]",
                            compressed.getName(),
                            requestFile
                    ));
                }
            } catch (Exception e) {
                android.util.Log.e("TaskDetail", "Error processing image for field: " + entry.getKey(), e);
            }
        }

        vehicleViewModel.submitMaintenance(
                task.getVehicle().getId(),
                "periodic",
                task.getVehicle().getOdometer(),
                dataJson,
                imageParts
        );
    }

    private void applyCoordinatorMode() {
        if (binding == null || !coordinatorMode) return;

        stopTimer();

        binding.locationContainer.setVisibility(View.GONE);
        hideClosestCard(binding.locationContainer);

        binding.previousTicketContainer.setVisibility(View.GONE);
        hideClosestCard(binding.previousTicketContainer);

        binding.btnTimer.setVisibility(View.GONE);
        binding.tvCurrentStartTime.setVisibility(View.GONE);

        binding.btnAddAdvance.setVisibility(View.GONE);
        binding.btnAddRendition.setVisibility(View.GONE);
        binding.btnCompleteTask.setVisibility(View.GONE);
        binding.btnOpenFull.setVisibility(View.GONE);

        if (binding.btnMyLocation != null) {
            binding.btnMyLocation.setVisibility(View.GONE);
        }

        if (binding.btnCheckLocationTop != null) {
            binding.btnCheckLocationTop.setVisibility(View.GONE);
        }

        if (binding.btnFillMaintenance != null) {
            binding.btnFillMaintenance.setVisibility(View.GONE);
        }
    }

    private void hideClosestCard(View target) {
        if (target == null) return;

        View current = target;
        while (current != null && current != binding.getRoot()) {
            if (current instanceof CardView) {
                current.setVisibility(View.GONE);
                return;
            }

            Object parent = current.getParent();
            if (parent instanceof View) {
                current = (View) parent;
            } else {
                current = null;
            }
        }
    }
}