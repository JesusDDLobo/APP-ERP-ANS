package com.example.app_ans.tasks.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.app_ans.MainActivity;
import com.example.app_ans.R;
import com.example.app_ans.auth.repository.AuthRepository;
import com.example.app_ans.auth.session.AuthSessionManager;
import com.example.app_ans.auth.ui.AuthActivity;
import com.example.app_ans.auth.ui.AuthViewModel;
import com.example.app_ans.auth.ui.AuthViewModelFactory;
import com.example.app_ans.core.network.NetworkModule;
import com.example.app_ans.core.persistence.AppDatabase;
import com.example.app_ans.core.ui.NavbarUtils;
import com.example.app_ans.databinding.ActivityRenditionsBinding;
import com.example.app_ans.tasks.model.Rendition;
import com.example.app_ans.tasks.model.RenditionTaskGroup;
import com.example.app_ans.tasks.persistence.PendingRendition;
import com.example.app_ans.tasks.persistence.TaskEntity;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RenditionsActivity extends AppCompatActivity {

    private ActivityRenditionsBinding binding;
    private RenditionTaskGroupAdapter adapter;
    private final List<RenditionTaskGroup> originalList = new ArrayList<>();

    private AuthViewModel authViewModel;
    private AuthSessionManager sessionManager;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();

    private int openTaskId = -1;
    private String openRenditionJson = null;
    private long openCreatedAt = -1L;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRenditionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        readIncomingExtras();
        setupAuth();
        setupNavbar();
        setupHeaderActions();
        setupRecycler();
        setupSearch();
        setupBackStackListener();
        loadLocalRenditions();
    }

    private void readIncomingExtras() {
        Intent intent = getIntent();
        if (intent == null) return;

        openTaskId = intent.getIntExtra("open_task_id", -1);
        openRenditionJson = intent.getStringExtra("open_rendition_json");
        openCreatedAt = intent.getLongExtra("open_created_at", -1L);
    }

    private void setupAuth() {
        sessionManager = new AuthSessionManager(this);

        AuthRepository repository = new AuthRepository(
                NetworkModule.provideAuthApi(this, sessionManager),
                sessionManager
        );

        authViewModel = new ViewModelProvider(
                this,
                new AuthViewModelFactory(repository)
        ).get(AuthViewModel.class);

        authViewModel.getLogoutResult().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Intent intent = new Intent(this, AuthActivity.class);
                intent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                );
                startActivity(intent);
                finish();
            }
        });
    }

    private void setupNavbar() {
        NavbarUtils.setupNavbar(
                this,
                null,
                () -> authViewModel.logout(this)
        );

        View backButton = findViewById(R.id.navbar_back_button);
        if (backButton != null) {
            backButton.setVisibility(View.GONE);
        }
    }

    private void setupHeaderActions() {
        binding.backRow.setOnClickListener(v -> {
            if (binding.detailContainer.getVisibility() == View.VISIBLE) {
                getSupportFragmentManager().popBackStack();
                return;
            }

            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void setupRecycler() {
        adapter = new RenditionTaskGroupAdapter();
        binding.recyclerTaskGroups.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerTaskGroups.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterGroups(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void setupBackStackListener() {
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            boolean showingDetail = getSupportFragmentManager().getBackStackEntryCount() > 0;

            binding.detailContainer.setVisibility(showingDetail ? View.VISIBLE : View.GONE);
            binding.contentScroll.setVisibility(showingDetail ? View.GONE : View.VISIBLE);
        });
    }

    private void loadLocalRenditions() {
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());

                List<TaskEntity> taskEntities = db.taskDao().getAllTasks();
                List<PendingRendition> pendingRenditions = db.pendingRenditionDao().getAll();

                List<RenditionTaskGroup> groups = buildGroups(taskEntities, pendingRenditions);

                runOnUiThread(() -> {
                    originalList.clear();
                    originalList.addAll(groups);
                    adapter.submitList(new ArrayList<>(originalList));
                    updateEmptyState(originalList.isEmpty());

                    if (shouldOpenIncomingDetail()) {
                        binding.contentScroll.postDelayed(this::openIncomingDetail, 200);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    originalList.clear();
                    adapter.submitList(new ArrayList<>(originalList));
                    updateEmptyState(true);
                });
            }
        });
    }

    private boolean shouldOpenIncomingDetail() {
        return openTaskId != -1
                && openRenditionJson != null
                && !openRenditionJson.trim().isEmpty()
                && openCreatedAt > 0;
    }

    private void openIncomingDetail() {
        binding.detailContainer.setVisibility(View.VISIBLE);
        binding.contentScroll.setVisibility(View.GONE);

        TaskRenditionFragment fragment = TaskRenditionFragment.newDetailInstance(
                openTaskId,
                openRenditionJson,
                openCreatedAt
        );

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.detailContainer, fragment)
                .addToBackStack("rendition_detail")
                .commit();
    }

    private List<RenditionTaskGroup> buildGroups(List<TaskEntity> taskEntities, List<PendingRendition> pendingRenditions) {
        List<RenditionTaskGroup> groups = new ArrayList<>();

        if (taskEntities == null || taskEntities.isEmpty() || pendingRenditions == null || pendingRenditions.isEmpty()) {
            return groups;
        }

        for (TaskEntity taskEntity : taskEntities) {
            List<Rendition> renditionsForTask = new ArrayList<>();
            double totalAmount = 0.0;

            for (PendingRendition pending : pendingRenditions) {
                if (pending.taskId != taskEntity.id) continue;

                String taskCode = safeTaskCode(taskEntity);
                String taskTitle = safeTaskTitle(taskEntity);

                String date = extractDateFromRenditionJson(pending.renditionJson, pending.createdAt);
                double amountValue = extractAmountFromRenditionJson(pending.renditionJson);
                String amountFormatted = formatCurrency(amountValue);
                String status = extractStatusFromRenditionJson(pending.renditionJson);

                renditionsForTask.add(new Rendition(
                        pending.id,
                        taskEntity.id,
                        taskCode,
                        taskTitle,
                        date,
                        amountFormatted,
                        status
                ));

                totalAmount += amountValue;
            }

            if (!renditionsForTask.isEmpty()) {
                groups.add(new RenditionTaskGroup(
                        taskEntity.id,
                        safeTaskCode(taskEntity),
                        safeTaskTitle(taskEntity),
                        formatCurrency(totalAmount),
                        renditionsForTask.size(),
                        renditionsForTask,
                        false
                ));
            }
        }

        return groups;
    }

    private void filterGroups(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            adapter.submitList(new ArrayList<>(originalList));
            updateEmptyState(originalList.isEmpty());
            return;
        }

        List<RenditionTaskGroup> filtered = new ArrayList<>();

        for (RenditionTaskGroup group : originalList) {
            String title = group.getTaskTitle() != null
                    ? group.getTaskTitle().toLowerCase(Locale.ROOT)
                    : "";

            String code = group.getTaskCode() != null
                    ? group.getTaskCode().toLowerCase(Locale.ROOT)
                    : "";

            if (title.contains(normalized) || code.contains(normalized)) {
                filtered.add(group);
            }
        }

        adapter.submitList(filtered);
        updateEmptyState(filtered.isEmpty());
    }

    private void updateEmptyState(boolean isEmpty) {
        binding.recyclerTaskGroups.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private String safeTaskCode(TaskEntity entity) {
        if (entity.publicId != null && !entity.publicId.trim().isEmpty()) {
            return entity.publicId;
        }
        return "TK-" + entity.id;
    }

    private String safeTaskTitle(TaskEntity entity) {
        try {
            if (entity.contentJson != null && !entity.contentJson.trim().isEmpty() && !"null".equals(entity.contentJson)) {
                JsonObject content = gson.fromJson(entity.contentJson, JsonObject.class);
                if (content != null) {
                    String[] keys = {"title", "name", "subject", "description"};
                    for (String key : keys) {
                        if (content.has(key) && !content.get(key).isJsonNull()) {
                            String value = content.get(key).getAsString();
                            if (value != null && !value.trim().isEmpty()) {
                                return value.trim();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { }

        if (entity.publicId != null && !entity.publicId.trim().isEmpty()) {
            return entity.publicId;
        }

        return "Tarea " + entity.id;
    }

    private String extractDateFromRenditionJson(String renditionJson, long fallbackCreatedAt) {
        try {
            if (renditionJson != null && !renditionJson.trim().isEmpty()) {
                JsonObject json = gson.fromJson(renditionJson, JsonObject.class);
                if (json != null) {
                    String[] keys = {"date", "created_at", "createdAt", "rendition_date", "fecha"};
                    for (String key : keys) {
                        if (json.has(key) && !json.get(key).isJsonNull()) {
                            JsonElement element = json.get(key);

                            if (element.isJsonPrimitive()) {
                                if (element.getAsJsonPrimitive().isNumber()) {
                                    long timestamp = element.getAsLong();
                                    return formatDate(timestamp);
                                }

                                String raw = element.getAsString();
                                if (raw != null && !raw.trim().isEmpty()) {
                                    raw = raw.trim();

                                    if (raw.matches("^\\d{12,}$")) {
                                        long timestamp = Long.parseLong(raw);
                                        return formatDate(timestamp);
                                    }

                                    return raw;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { }

        return formatDate(fallbackCreatedAt);
    }

    private String formatDate(long timestamp) {
        return new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                .format(new Date(timestamp));
    }

    private String extractStatusFromRenditionJson(String renditionJson) {
        try {
            if (renditionJson != null && !renditionJson.trim().isEmpty()) {
                JsonObject json = gson.fromJson(renditionJson, JsonObject.class);
                if (json != null) {
                    String[] keys = {"status", "state", "estado"};
                    for (String key : keys) {
                        if (json.has(key) && !json.get(key).isJsonNull()) {
                            return json.get(key).getAsString();
                        }
                    }
                }
            }
        } catch (Exception ignored) { }

        return "pendiente";
    }

    private double extractAmountFromRenditionJson(String renditionJson) {
        try {
            if (renditionJson == null || renditionJson.trim().isEmpty()) {
                return 0.0;
            }

            JsonObject json = gson.fromJson(renditionJson, JsonObject.class);
            if (json == null) return 0.0;

            String[] directKeys = {
                    "amount", "total", "total_amount", "totalAmount", "approved_total",
                    "approvedTotal", "value", "valor", "monto", "total_reported"
            };

            for (String key : directKeys) {
                if (json.has(key) && !json.get(key).isJsonNull()) {
                    Double value = tryParseDouble(json.get(key));
                    if (value != null) return value;
                }
            }

            String[] arrayKeys = {"items", "concepts", "details", "rendition_items"};
            for (String key : arrayKeys) {
                if (json.has(key) && json.get(key).isJsonArray()) {
                    double sum = 0.0;
                    for (JsonElement el : json.getAsJsonArray(key)) {
                        if (el != null && el.isJsonObject()) {
                            JsonObject obj = el.getAsJsonObject();
                            String[] itemAmountKeys = {"amount", "value", "valor", "total", "monto"};
                            for (String itemKey : itemAmountKeys) {
                                if (obj.has(itemKey) && !obj.get(itemKey).isJsonNull()) {
                                    Double value = tryParseDouble(obj.get(itemKey));
                                    if (value != null) {
                                        sum += value;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (sum > 0) return sum;
                }
            }

        } catch (Exception ignored) { }

        return 0.0;
    }

    private Double tryParseDouble(JsonElement element) {
        try {
            if (element == null || element.isJsonNull()) return null;

            if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsDouble();
            }

            String raw = element.getAsString();
            if (raw == null) return null;

            raw = raw.replace("$", "")
                    .replace(".", "")
                    .replace(",", "")
                    .replace(" ", "")
                    .trim();

            if (raw.isEmpty()) return null;
            return Double.parseDouble(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatCurrency(double value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
        String result = format.format(value);

        if (result.contains(",00")) {
            result = result.replace(",00", "");
        }
        return result;
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}