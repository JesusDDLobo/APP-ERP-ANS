package com.example.app_ans.auth.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.app_ans.R;

/** Simple dialog to capture a new password when must_change_password is true. */
public class PasswordSetupDialog extends DialogFragment {

    public interface Listener {
        void onPasswordConfirmed(String password);
    }

    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_password_setup, container, false);
        EditText passwordInput = view.findViewById(R.id.new_password_input);
        Button confirmButton = view.findViewById(R.id.confirm_button);
        Button cancelButton = view.findViewById(R.id.cancel_button);

        confirmButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPasswordConfirmed(passwordInput.getText().toString());
            }
            dismiss();
        });

        cancelButton.setOnClickListener(v -> dismiss());
        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_AppANS_Dialog);
        return super.onCreateDialog(savedInstanceState);
    }
}
