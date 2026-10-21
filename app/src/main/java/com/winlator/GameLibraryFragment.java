package com.winlator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;

import com.winlator.box64.Box64Preset;
import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.container.Shortcut;
import com.winlator.core.AppUtils;
import com.winlator.core.DiagnosticLogger;

import java.util.ArrayList;
import java.util.List;

/** Fluxlator's game-first library and session dashboard. */
public class GameLibraryFragment extends Fragment {
    private static final String PREF_DEFAULT_PROFILE = "default_gaming_profile";
    private static final String[] PROFILE_IDS = {"performance", "balanced", "compatibility"};
    private static final String[] PROFILE_NAMES = {"Performance", "Balanced", "Compatibility"};
    private static final int[] CARD_COLORS = {0xff6f3dcc, 0xff3f63b2, 0xffb34d86, 0xff258b85, 0xff9c5a32};
    private LinearLayout content;
    private LinearLayout gameRow;
    private TextView gameCount;
    private TextView noSearchResults;
    private ArrayList<Shortcut> games;

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(context, 18);
        content.setPadding(pad, pad, pad, dp(context, 28));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        renderLibrary();
        content.setAlpha(0f);
        content.setTranslationY(dp(context, 12));
        content.animate().alpha(1f).translationY(0).setDuration(300).start();
        return scroll;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity)requireActivity()).getSupportActionBar().setTitle(R.string.game_library);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (content != null) renderLibrary();
    }

    private void renderLibrary() {
        if (content == null || !isAdded()) return;
        Context context = requireContext();
        content.removeAllViews();
        gameRow = null;
        gameCount = null;
        noSearchResults = null;
        ContainerManager manager = new ContainerManager(context);
        ArrayList<Container> containers = manager.getContainers();
        ArrayList<Shortcut> shortcuts = manager.loadShortcuts(null);
        games = new ArrayList<>();
        for (Shortcut shortcut : shortcuts) if (!shortcut.file.isDirectory()) games.add(shortcut);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String recentName = prefs.getString("recent_game_name", "");
        String recentContainer = prefs.getString("recent_game_container", "");
        for (int i = 0; i < games.size(); i++) {
            Shortcut candidate = games.get(i);
            if (candidate.name.equals(recentName) && candidate.container.getName().equals(recentContainer)) {
                games.remove(i);
                games.add(0, candidate);
                break;
            }
        }

        content.addView(buildHero(context), matchWrap());
        LinearLayout libraryHeader = new LinearLayout(context);
        libraryHeader.setGravity(Gravity.CENTER_VERTICAL);
        libraryHeader.addView(sectionLabel(context, R.string.your_library, R.string.launch_and_manage_shortcuts), new LinearLayout.LayoutParams(0, -2, 1f));
        TextView browse = textButton(context, R.string.browse_all);
        browse.setOnClickListener(v -> navigate(R.id.menu_item_shortcuts));
        libraryHeader.addView(browse);
        LinearLayout.LayoutParams headerParams = matchWrap();
        headerParams.topMargin = dp(context, 4);
        content.addView(libraryHeader, headerParams);

        EditText search = new EditText(context);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setHint(R.string.search_games);
        search.setTextColor(color(R.attr.colorPrimaryText));
        search.setHintTextColor(color(R.attr.colorSecondaryText));
        search.setPadding(dp(context, 14), 0, dp(context, 14), 0);
        search.setBackground(roundRect(color(R.attr.colorSecondarySurface), 16, 0));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(context, 48));
        searchParams.topMargin = dp(context, 5);
        content.addView(search, searchParams);

        gameCount = label(context, "", 12, color(R.attr.colorSecondaryText), false);
        gameCount.setText(getString(R.string.games_ready_count, games.size(), games.size()));
        LinearLayout.LayoutParams countParams = matchWrap();
        countParams.topMargin = dp(context, 7);
        content.addView(gameCount, countParams);
        if (games.isEmpty()) {
            content.addView(buildEmptyGames(context), matchWrap());
        }
        else {
            HorizontalScrollView gameScroller = (HorizontalScrollView)buildGameScroller(context, games);
            LinearLayout.LayoutParams scrollerParams = matchWrap();
            scrollerParams.topMargin = dp(context, 3);
            content.addView(gameScroller, scrollerParams);
            noSearchResults = label(context, R.string.no_search_results, 13, color(R.attr.colorSecondaryText), false);
            noSearchResults.setVisibility(View.GONE);
            content.addView(noSearchResults, matchWrap());
            search.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterGames(s.toString()); }
                @Override public void afterTextChanged(Editable s) {}
            });
            updateGameFilter("");
        }

        LinearLayout stats = new LinearLayout(context);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(buildStat(context, String.valueOf(games.size()), getString(R.string.games_ready)), new LinearLayout.LayoutParams(0, dp(context, 82), 1f));
        View spacer = new View(context);
        stats.addView(spacer, new LinearLayout.LayoutParams(dp(context, 10), 1));
        stats.addView(buildStat(context, String.valueOf(containers.size()), getString(R.string.containers)), new LinearLayout.LayoutParams(0, dp(context, 82), 1f));
        LinearLayout.LayoutParams statsParams = matchWrap();
        statsParams.topMargin = dp(context, 14);
        content.addView(stats, statsParams);

        content.addView(sectionLabel(context, R.string.new_container_profile, R.string.applies_to_new_containers));
        content.addView(buildProfiles(context), matchWrap());

        LinearLayout containerHeader = new LinearLayout(context);
        containerHeader.setGravity(Gravity.CENTER_VERTICAL);
        containerHeader.addView(sectionLabel(context, R.string.play_containers, R.string.launch_windows_sessions), new LinearLayout.LayoutParams(0, -2, 1f));
        TextView manage = textButton(context, R.string.manage);
        manage.setOnClickListener(v -> navigate(R.id.menu_item_containers));
        containerHeader.addView(manage);
        LinearLayout.LayoutParams containerHeaderParams = matchWrap();
        containerHeaderParams.topMargin = dp(context, 8);
        content.addView(containerHeader, containerHeaderParams);
        if (containers.isEmpty()) content.addView(buildEmptyContainers(context), matchWrap());
        else content.addView(buildContainerScroller(context, containers), matchWrap());

        TextView diagnostics = textButton(context, R.string.open_diagnostics);
        diagnostics.setGravity(Gravity.CENTER);
        diagnostics.setBackground(roundRect(color(R.attr.colorSecondarySurface), 18, 0));
        diagnostics.setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12));
        diagnostics.setOnClickListener(v -> navigate(R.id.menu_item_diagnostics));
        LinearLayout.LayoutParams diagParams = matchWrap();
        diagParams.topMargin = dp(context, 18);
        content.addView(diagnostics, diagParams);
    }

    private View buildHero(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 22), dp(context, 22), dp(context, 22), dp(context, 20));
        GradientDrawable background = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{0xff713bd1, 0xff46218d, 0xff201238});
        background.setCornerRadius(dp(context, 26));
        card.setBackground(background);
        card.setElevation(dp(context, 3));

        TextView overline = new TextView(context);
        overline.setText(R.string.fluxlator_game_deck);
        overline.setTextColor(0xffe1caff);
        overline.setTextSize(12);
        overline.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(overline);

        TextView title = new TextView(context);
        title.setText(R.string.library_hero_title);
        title.setTextColor(Color.WHITE);
        title.setTextSize(27);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = matchWrap();
        titleParams.topMargin = dp(context, 9);
        card.addView(title, titleParams);

        TextView message = new TextView(context);
        message.setText(R.string.library_hero_subtitle);
        message.setTextColor(0xffe4dcf3);
        message.setTextSize(14);
        LinearLayout.LayoutParams messageParams = matchWrap();
        messageParams.topMargin = dp(context, 7);
        card.addView(message, messageParams);

        TextView open = textButton(context, R.string.open_game_library);
        open.setTextColor(0xff38205f);
        open.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        open.setGravity(Gravity.CENTER);
        open.setBackground(roundRect(0xfff1e8ff, 50, 0));
        open.setPadding(dp(context, 18), dp(context, 11), dp(context, 18), dp(context, 11));
        open.setOnClickListener(v -> navigate(R.id.menu_item_shortcuts));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-2, -2);
        buttonParams.topMargin = dp(context, 16);
        card.addView(open, buttonParams);
        animateIn(card, 0);
        return card;
    }

    private View buildStat(Context context, String count, String label) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(context, 15), 0, dp(context, 15), 0);
        card.setBackground(roundRect(color(R.attr.colorSecondarySurface), 18, 0));
        TextView value = new TextView(context);
        value.setText(count);
        value.setTextColor(color(R.attr.colorPrimaryVariant));
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        value.setTextSize(22);
        card.addView(value);
        TextView caption = new TextView(context);
        caption.setText(label);
        caption.setTextColor(color(R.attr.colorSecondaryText));
        caption.setTextSize(12);
        card.addView(caption);
        animateIn(card, 1);
        return card;
    }

    private View sectionLabel(Context context, int titleRes, int subtitleRes) {
        LinearLayout label = new LinearLayout(context);
        label.setOrientation(LinearLayout.VERTICAL);
        label.setPadding(0, dp(context, 15), 0, dp(context, 7));
        TextView title = new TextView(context);
        title.setText(titleRes);
        title.setTextColor(color(R.attr.colorPrimaryText));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextSize(18);
        label.addView(title);
        TextView subtitle = new TextView(context);
        subtitle.setText(subtitleRes);
        subtitle.setTextColor(color(R.attr.colorSecondaryText));
        subtitle.setTextSize(12);
        LinearLayout.LayoutParams subParams = matchWrap();
        subParams.topMargin = dp(context, 2);
        label.addView(subtitle, subParams);
        return label;
    }

    private View buildProfiles(Context context) {
        LinearLayout group = new LinearLayout(context);
        group.setOrientation(LinearLayout.HORIZONTAL);
        String selected = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_DEFAULT_PROFILE, "balanced");
        for (int i = 0; i < PROFILE_IDS.length; i++) {
            final String profile = PROFILE_IDS[i];
            TextView chip = new TextView(context);
            chip.setText(PROFILE_NAMES[i]);
            chip.setTextSize(12);
            chip.setGravity(Gravity.CENTER);
            chip.setSingleLine(true);
            chip.setPadding(dp(context, 9), dp(context, 11), dp(context, 9), dp(context, 11));
            boolean active = profile.equals(selected);
            chip.setTextColor(active ? Color.WHITE : color(R.attr.colorSecondaryText));
            chip.setBackground(roundRect(active ? 0xff7441c4 : color(R.attr.colorSecondarySurface), 16, active ? 0xffa47cdb : 0));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1f);
            params.setMargins(dp(context, 2), 0, dp(context, 2), 0);
            group.addView(chip, params);
            chip.setOnClickListener(v -> {
                PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PREF_DEFAULT_PROFILE, profile).apply();
                DiagnosticLogger.info("Profile", "Default new-container profile set to " + profile);
                renderLibrary();
                View root = getView();
                if (root != null) root.announceForAccessibility(getString(R.string.profile_saved));
            });
            animateIn(chip, i + 2);
        }
        return group;
    }

    private View buildGameScroller(Context context, List<Shortcut> games) {
        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setHorizontalScrollBarEnabled(false);
        gameRow = new LinearLayout(context);
        gameRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < games.size(); i++) {
            View card = buildGameCard(context, games.get(i), i);
            card.setTag(games.get(i).name + "\n" + games.get(i).container.getName());
            gameRow.addView(card, new LinearLayout.LayoutParams(dp(context, 210), dp(context, 178)));
        }
        scroll.addView(gameRow, new HorizontalScrollView.LayoutParams(-2, -2));
        return scroll;
    }

    private void filterGames(String query) {
        updateGameFilter(query);
    }

    private void updateGameFilter(String query) {
        if (gameRow == null || gameCount == null) return;
        String filter = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        int matches = 0;
        for (int i = 0; i < gameRow.getChildCount(); i++) {
            View card = gameRow.getChildAt(i);
            boolean visible = String.valueOf(card.getTag()).toLowerCase(java.util.Locale.ROOT).contains(filter);
            card.setVisibility(visible ? View.VISIBLE : View.GONE);
            if (visible) matches++;
        }
        gameCount.setText(getString(R.string.games_ready_count, matches, games.size()));
        if (noSearchResults != null) noSearchResults.setVisibility(matches == 0 ? View.VISIBLE : View.GONE);
    }

    private View buildGameCard(Context context, Shortcut game, int index) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 14), dp(context, 13), dp(context, 14), dp(context, 12));
        card.setBackground(roundRect(CARD_COLORS[index % CARD_COLORS.length], 20, 0));
        card.setOnClickListener(v -> launchShortcut(game));

        LinearLayout top = new LinearLayout(context);
        top.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(context);
        int iconSize = dp(context, 42);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        if (game.icon != null) icon.setImageBitmap(game.icon);
        else icon.setImageResource(R.drawable.icon_shortcut);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setBackground(roundRect(0x55ffffff, 13, 0));
        icon.setPadding(dp(context, 5), dp(context, 5), dp(context, 5), dp(context, 5));
        top.addView(icon, iconParams);
        TextView tag = new TextView(context);
        tag.setText(R.string.pc_game);
        tag.setTextColor(0xfff4eaff);
        tag.setTextSize(10);
        tag.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tag.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        top.addView(tag, new LinearLayout.LayoutParams(0, -1, 1f));
        card.addView(top);

        TextView title = new TextView(context);
        title.setText(game.name);
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams = matchWrap();
        titleParams.topMargin = dp(context, 12);
        card.addView(title, titleParams);

        TextView container = new TextView(context);
        container.setText(game.container.getName() + "  ·  " + getProfileName(game.container.getBox64Preset()));
        container.setTextColor(0xffe6d7ff);
        container.setTextSize(11);
        LinearLayout.LayoutParams containerParams = matchWrap();
        containerParams.topMargin = dp(context, 2);
        card.addView(container, containerParams);

        TextView play = textButton(context, R.string.play_now);
        play.setTextColor(Color.WHITE);
        play.setGravity(Gravity.CENTER);
        play.setBackground(roundRect(0x35ffffff, 50, 0));
        LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(-1, dp(context, 34));
        playParams.topMargin = dp(context, 9);
        card.addView(play, playParams);
        play.setOnClickListener(v -> launchShortcut(game));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -1);
        params.setMargins(dp(context, 3), dp(context, 2), dp(context, 9), dp(context, 5));
        card.setLayoutParams(params);
        animateIn(card, index + 4);
        return card;
    }

    private View buildEmptyGames(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(context, 18), dp(context, 18), dp(context, 18), dp(context, 18));
        card.setBackground(roundRect(color(R.attr.colorSecondarySurface), 20, 0));
        TextView message = new TextView(context);
        message.setText(R.string.no_games_yet);
        message.setTextColor(color(R.attr.colorPrimaryText));
        message.setTextSize(15);
        message.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(message);
        TextView detail = new TextView(context);
        detail.setText(R.string.add_game_shortcut_hint);
        detail.setTextColor(color(R.attr.colorSecondaryText));
        detail.setTextSize(13);
        LinearLayout.LayoutParams detailParams = matchWrap();
        detailParams.topMargin = dp(context, 4);
        card.addView(detail, detailParams);
        TextView action = textButton(context, R.string.browse_all);
        action.setOnClickListener(v -> navigate(R.id.menu_item_shortcuts));
        LinearLayout.LayoutParams actionParams = matchWrap();
        actionParams.topMargin = dp(context, 8);
        card.addView(action, actionParams);
        return card;
    }

    private View buildContainerScroller(Context context, List<Container> containers) {
        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < Math.min(containers.size(), 8); i++) row.addView(buildContainerCard(context, containers.get(i), i), new LinearLayout.LayoutParams(dp(context, 220), dp(context, 100)));
        scroll.addView(row, new HorizontalScrollView.LayoutParams(-2, -2));
        return scroll;
    }

    private View buildContainerCard(Context context, Container container, int index) {
        LinearLayout card = new LinearLayout(context);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(context, 14), dp(context, 12), dp(context, 12), dp(context, 12));
        card.setBackground(roundRect(color(R.attr.colorSecondarySurface), 18, 0));
        LinearLayout textGroup = new LinearLayout(context);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        textGroup.addView(label(context, R.string.container, 11, color(R.attr.colorSecondaryText), false));
        textGroup.addView(label(context, container.getName(), 15, color(R.attr.colorPrimaryText), true));
        TextView run = textButton(context, R.string.start);
        run.setTextColor(Color.WHITE);
        run.setGravity(Gravity.CENTER);
        run.setBackground(roundRect(0xff7044bb, 14, 0));
        run.setPadding(dp(context, 11), dp(context, 8), dp(context, 11), dp(context, 8));
        LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(0, -2, 1f);
        card.addView(textGroup, groupParams);
        card.addView(run);
        View.OnClickListener launch = v -> launchContainer(container);
        card.setOnClickListener(launch);
        run.setOnClickListener(launch);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -1);
        params.setMargins(dp(context, 3), dp(context, 2), dp(context, 9), dp(context, 5));
        card.setLayoutParams(params);
        animateIn(card, index + 5);
        return card;
    }

    private View buildEmptyContainers(Context context) {
        TextView card = new TextView(context);
        card.setText(R.string.no_containers_yet);
        card.setTextColor(color(R.attr.colorSecondaryText));
        card.setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16));
        card.setBackground(roundRect(color(R.attr.colorSecondarySurface), 16, 0));
        card.setOnClickListener(v -> navigate(R.id.menu_item_containers));
        return card;
    }

    private TextView label(Context context, int text, int size, int color, boolean bold) {
        return label(context, context.getString(text), size, color, bold);
    }

    private TextView label(Context context, String text, int size, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView textButton(Context context, int text) {
        TextView view = label(context, text, 13, color(R.attr.colorPrimaryVariant), true);
        view.setGravity(Gravity.CENTER);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private void launchShortcut(Shortcut shortcut) {
        if (shortcut.file.isDirectory()) {
            navigate(R.id.menu_item_shortcuts);
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
            .putString("recent_game_name", shortcut.name)
            .putString("recent_game_container", shortcut.container.getName())
            .apply();
        DiagnosticLogger.info("GameLibrary", "Launching shortcut " + shortcut.name);
        Intent intent = new Intent(requireActivity(), XServerDisplayActivity.class);
        intent.putExtra("container_id", shortcut.container.id);
        intent.putExtra("shortcut_path", shortcut.file.getPath());
        startActivity(intent);
    }

    private void launchContainer(Container container) {
        DiagnosticLogger.info("GameLibrary", "Starting container session " + container.getName());
        Intent intent = new Intent(requireActivity(), XServerDisplayActivity.class);
        intent.putExtra("container_id", container.id);
        startActivity(intent);
    }

    private void navigate(int itemId) {
        MainActivity activity = (MainActivity)requireActivity();
        androidx.appcompat.widget.Toolbar toolbar = activity.findViewById(R.id.Toolbar);
        androidx.drawerlayout.widget.DrawerLayout drawer = activity.findViewById(R.id.DrawerLayout);
        // Navigation is routed through MainActivity to keep the drawer and selection state in sync.
        android.view.View nav = activity.findViewById(R.id.NavigationView);
        if (nav instanceof com.google.android.material.navigation.NavigationView) {
            com.google.android.material.navigation.NavigationView navigationView = (com.google.android.material.navigation.NavigationView)nav;
            navigationView.setCheckedItem(itemId);
            activity.onNavigationItemSelected(navigationView.getMenu().findItem(itemId));
        }
        if (toolbar == null || drawer == null) DiagnosticLogger.warn("Navigation", "Dashboard navigation used before activity view was ready");
    }

    private GradientDrawable roundRect(int fill, int radiusDp, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(requireContext(), radiusDp));
        if (stroke != 0) drawable.setStroke(dp(requireContext(), 1), stroke);
        return drawable;
    }

    private String getProfileName(String preset) {
        if (Box64Preset.PERFORMANCE.equals(preset)) return getString(R.string.performance);
        if (Box64Preset.INTERMEDIATE.equals(preset)) return getString(R.string.balanced_mode);
        if (Box64Preset.STABILITY.equals(preset)) return getString(R.string.compatibility_mode);
        return getString(R.string.custom);
    }

    private int color(int attr) {
        return AppUtils.getThemeColor(requireContext(), attr);
    }

    private static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static int dp(Context context, float value) {
        return (int)(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()) + 0.5f);
    }

    private void animateIn(View view, int order) {
        view.setAlpha(0f);
        view.setTranslationY(dp(requireContext(), 9));
        view.animate().alpha(1f).translationY(0).setStartDelay(Math.min(order, 8) * 28L).setDuration(230).start();
    }
}
