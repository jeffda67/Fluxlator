package com.winlator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.winlator.core.AppUtils;
import com.winlator.core.DiagnosticLogger;

public class DiagnosticsFragment extends Fragment {
    private TextView logText;
    private boolean errorsOnly;

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater, @Nullable android.view.ViewGroup container, @Nullable Bundle state) {
        Context context = requireContext();
        int pad = dp(context, 16);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(context);
        title.setText(R.string.diagnostics_title);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(AppUtils.getThemeColor(context, R.attr.colorPrimaryText));
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView detail = new TextView(context);
        detail.setText(R.string.diagnostics_description);
        detail.setTextSize(14);
        detail.setTextColor(AppUtils.getThemeColor(context, R.attr.colorSecondaryText));
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(context, 6);
        detailParams.bottomMargin = dp(context, 12);
        root.addView(detail, detailParams);

        LinearLayout controls = new LinearLayout(context);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        addButton(context, controls, R.string.refresh, this::refreshLogs);
        addButton(context, controls, R.string.copy, this::copyLogs);
        addButton(context, controls, R.string.share, this::shareLogs);
        root.addView(controls, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        addButton(context, actions, R.string.all_logs, () -> { errorsOnly = false; refreshLogs(); });
        addButton(context, actions, R.string.errors_only, () -> { errorsOnly = true; refreshLogs(); });
        addButton(context, actions, R.string.clear, this::confirmClear);
        root.addView(actions, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        logText = new TextView(context);
        logText.setTextIsSelectable(true);
        logText.setTextSize(12);
        logText.setTypeface(Typeface.MONOSPACE);
        logText.setTextColor(AppUtils.getThemeColor(context, R.attr.colorPrimaryText));
        logText.setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 16));
        scroll.addView(logText, new ScrollView.LayoutParams(-1, -2));
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        scrollParams.topMargin = dp(context, 8);
        root.addView(scroll, scrollParams);

        refreshLogs();
        root.setAlpha(0f);
        root.setTranslationY(dp(context, 10));
        root.animate().alpha(1f).translationY(0).setDuration(220).start();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity)requireActivity()).getSupportActionBar().setTitle(R.string.diagnostics_title);
    }

    private void refreshLogs() {
        if (logText == null) return;
        String all = DiagnosticLogger.readAll();
        if (errorsOnly) {
            StringBuilder filtered = new StringBuilder();
            for (String line : all.split("\\n")) {
                if (line.contains("ERROR/") || line.contains("FATAL/") || line.contains("WARN/")) filtered.append(line).append('\n');
            }
            logText.setText(filtered.length() > 0 ? filtered.toString() : getString(R.string.no_errors_logged));
        }
        else logText.setText(all);
    }

    private String currentLogs() {
        if (errorsOnly) {
            StringBuilder filtered = new StringBuilder();
            for (String line : DiagnosticLogger.readAll().split("\\n")) {
                if (line.contains("ERROR/") || line.contains("FATAL/") || line.contains("WARN/")) filtered.append(line).append('\n');
            }
            return filtered.toString();
        }
        return DiagnosticLogger.readAll();
    }

    private void copyLogs() {
        ClipboardManager clipboard = (ClipboardManager)requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("Fluxlator diagnostics", currentLogs()));
        AppUtils.showToast(requireContext(), R.string.logs_copied);
    }

    private void shareLogs() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "Fluxlator diagnostics");
        send.putExtra(Intent.EXTRA_TEXT, currentLogs());
        startActivity(Intent.createChooser(send, getString(R.string.share_diagnostics)));
    }

    private void confirmClear() {
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.clear_diagnostics_title)
            .setMessage(R.string.clear_diagnostics_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.clear, (dialog, which) -> {
                DiagnosticLogger.clear();
                errorsOnly = false;
                refreshLogs();
            })
            .show();
    }

    private static void addButton(Context context, LinearLayout parent, int title, Runnable action) {
        Button button = new Button(context);
        button.setText(title);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setMinHeight(dp(context, 40));
        button.setPadding(dp(context, 8), 0, dp(context, 8), 0);
        button.setOnClickListener(view -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1f);
        params.setMargins(dp(context, 2), 0, dp(context, 2), 0);
        parent.addView(button, params);
    }

    private static int dp(Context context, float value) {
        return (int)(value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
