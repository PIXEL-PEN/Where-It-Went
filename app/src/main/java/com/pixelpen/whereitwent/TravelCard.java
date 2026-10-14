package com.pixelpen.whereitwent;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Travel mode (v3.3): a floating quick-entry card on the main screen for one
 * Travel account. While travel mode is on, the app opens on the card with the
 * trip, category and date where they were left. Minimising it shows a small
 * "✈ trip" pill, so switching between home and the trip is one tap.
 */
public final class TravelCard {

    private static final String PREFS = "travel_mode";
    private static final String K_ACTIVE = "active";
    private static final String K_TRIP = "trip_id";
    private static final String K_EXPANDED = "expanded";
    private static final String K_DATE = "date_millis";
    private static final String K_DATE_PINNED = "date_pinned";
    private static final String K_CATEGORY = "category_";   // + trip id

    // Same stores AddExpenseDialog uses for an account's own categories
    private static final String PREF_ACCOUNT_CATEGORIES = "account_categories_store";
    private static final String KEY_ACCOUNT_CATEGORIES = "account_categories";
    private static final List<String> TRAVEL_DEFAULTS = Arrays.asList(
            "Lodging", "Meals", "Transportation", "Activities", "Other");

    private static final int RECENT_COUNT = 3;

    private final MainScreen activity;
    private final SharedPreferences prefs;

    private final View scrim, card, pill;
    private final TextView tripName, tripTotal, amountCurrency, conversion, dateView, emptyRecent;
    private final LinearLayout recent;
    private final android.widget.EditText editItem, editAmount;
    private final Spinner spinnerCategory;
    private final List<String> categories = new ArrayList<>();
    private final ArrayAdapter<String> categoryAdapter;

    private AccountEntity trip;
    private final Calendar date = Calendar.getInstance();
    private boolean bindingCategories;

    private final OnBackPressedCallback backMinimises = new OnBackPressedCallback(false) {
        @Override public void handleOnBackPressed() { minimise(); }
    };

    public TravelCard(MainScreen activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        scrim = activity.findViewById(R.id.travel_scrim);
        card = activity.findViewById(R.id.travel_card);
        pill = activity.findViewById(R.id.travel_pill);
        tripName = activity.findViewById(R.id.travel_trip_name);
        tripTotal = activity.findViewById(R.id.travel_trip_total);
        amountCurrency = activity.findViewById(R.id.travel_amount_currency);
        conversion = activity.findViewById(R.id.travel_conversion);
        dateView = activity.findViewById(R.id.travel_date);
        emptyRecent = activity.findViewById(R.id.travel_recent_empty);
        recent = activity.findViewById(R.id.travel_recent);
        editItem = activity.findViewById(R.id.travel_edit_item);
        editAmount = activity.findViewById(R.id.travel_edit_amount);
        spinnerCategory = activity.findViewById(R.id.travel_spinner_category);

        categoryAdapter = new ArrayAdapter<>(activity, R.layout.spinner_item_selected, categories);
        categoryAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerCategory.setAdapter(categoryAdapter);

        activity.getOnBackPressedDispatcher().addCallback(activity, backMinimises);

        scrim.setOnClickListener(v -> minimise());
        pill.setOnClickListener(v -> expand());
        activity.findViewById(R.id.travel_btn_minimize).setOnClickListener(v -> minimise());
        activity.findViewById(R.id.travel_btn_end).setOnClickListener(v -> confirmEnd());
        activity.findViewById(R.id.travel_header_text).setOnClickListener(v -> pickTrip());
        activity.findViewById(R.id.travel_btn_add).setOnClickListener(v -> add());
        dateView.setOnClickListener(v -> pickDate());
        conversion.setOnClickListener(v -> changeRate());

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (bindingCategories || trip == null || pos < 0 || pos >= categories.size()) return;
                prefs.edit().putString(K_CATEGORY + trip.id, categories.get(pos)).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });

        editAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { refreshConversion(); }
        });
        editAmount.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) { add(); return true; }
            return false;
        });

        // Edge-to-edge: keep the card above the keyboard and the navigation bar
        final int baseMargin = dp(24);
        ViewCompat.setOnApplyWindowInsetsListener(card, (v, insets) -> {
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.bottomMargin = Math.max(baseMargin, Math.max(ime.bottom, bars.bottom) + dp(8));
            v.setLayoutParams(lp);
            return insets;
        });
    }

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    /** Called from onCreate: reopens the trip where it was left. */
    public void restore() {
        if (!prefs.getBoolean(K_ACTIVE, false)) { showNothing(); return; }
        if (!loadTrip(prefs.getLong(K_TRIP, -1))) { end(false); return; }
        // Travel mode on: the app opens on the card
        show(true);
    }

    /** Drawer link: start travel mode, or bring the card back if it is on. */
    public void startFromDrawer() {
        if (prefs.getBoolean(K_ACTIVE, false) && loadTrip(prefs.getLong(K_TRIP, -1))) {
            show(true);
        } else {
            pickTrip();
        }
    }

    /** After an entry is edited elsewhere (or the trip changed): redraw. */
    public void refresh() {
        if (!prefs.getBoolean(K_ACTIVE, false)) return;
        if (!loadTrip(prefs.getLong(K_TRIP, -1))) { end(false); return; }
        bind();
    }

    private boolean loadTrip(long id) {
        if (id < 0) return false;
        AccountEntity a = ExpenseDatabase.getDatabase(activity).accountDao().getById(id);
        if (a == null || a.archived || !"travel".equals(a.type)) return false;
        trip = a;
        return true;
    }

    private void show(boolean expanded) {
        prefs.edit().putBoolean(K_EXPANDED, expanded).apply();
        bind();
        card.setVisibility(expanded ? View.VISIBLE : View.GONE);
        scrim.setVisibility(expanded ? View.VISIBLE : View.GONE);
        pill.setVisibility(expanded ? View.GONE : View.VISIBLE);
        backMinimises.setEnabled(expanded);
        if (!expanded) hideKeyboard();
    }

    private void showNothing() {
        card.setVisibility(View.GONE);
        scrim.setVisibility(View.GONE);
        pill.setVisibility(View.GONE);
        backMinimises.setEnabled(false);
    }

    private void expand() { show(true); }

    private void minimise() { show(false); }

    private void begin(AccountEntity a) {
        trip = a;
        prefs.edit().putBoolean(K_ACTIVE, true).putLong(K_TRIP, a.id).apply();
        editItem.setText("");
        editAmount.setText("");
        show(true);
    }

    private void end(boolean say) {
        prefs.edit().putBoolean(K_ACTIVE, false).apply();
        trip = null;
        hideKeyboard();
        showNothing();
        if (say) Toast.makeText(activity, "Travel mode ended", Toast.LENGTH_SHORT).show();
    }

    private void confirmEnd() {
        new AlertDialog.Builder(activity)
                .setTitle("End travel mode?")
                .setMessage("The trip and its entries stay in Accounts. "
                        + "You can start travel mode again from the menu.")
                .setPositiveButton("End", (d, w) -> end(true))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    private void bind() {
        if (trip == null) return;
        String sym = AppPrefs.getCurrencySymbol(activity);
        AccountItemDao items = ExpenseDatabase.getDatabase(activity).accountItemDao();
        List<AccountItemEntity> all = items.getItemsForAccount(trip.id);

        tripName.setText("✈  " + trip.name + "  ▾");
        ((TextView) pill).setText("✈  " + trip.name);

        // Total: trip currency first, then the home value in whole numbers
        double homeTotal = 0;
        for (AccountItemEntity it : all) homeTotal += it.amount;
        Double foreignTotal = AccountCurrency.foreignTotal(trip, all);
        String total;
        if (all.isEmpty()) {
            total = "No spending yet";
        } else if (foreignTotal != null) {
            total = AccountCurrency.foreign(trip, foreignTotal)
                    + "  ·  " + AccountCurrency.homeWhole(trip, homeTotal, sym);
        } else {
            total = AccountCurrency.home(trip, homeTotal, sym);
        }
        tripTotal.setText("Total " + total);

        // Recent entries (newest first); tap one to edit it
        recent.removeAllViews();
        SimpleDateFormat inFmt = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        SimpleDateFormat outFmt = new SimpleDateFormat("dd MMM", Locale.ENGLISH);
        for (int i = 0; i < Math.min(RECENT_COUNT, all.size()); i++) {
            AccountItemEntity it = all.get(i);
            String day = it.date;
            try { day = outFmt.format(inFmt.parse(it.date)); } catch (Exception ignored) { }
            String amt = it.hasForeignAmount()
                    ? AccountCurrency.foreign(trip, it.foreignAmount)
                    : AccountCurrency.home(trip, it.amount, sym);
            recent.addView(recentRow(day + "   " + it.item, amt, it.id));
        }
        emptyRecent.setVisibility(all.isEmpty() ? View.VISIBLE : View.GONE);

        // Amount field in the trip currency
        if (trip.hasForeignCurrency()) {
            TripCurrency.Currency c = TripCurrency.byCode(trip.foreignCurrency);
            amountCurrency.setText(trip.foreignCurrency);
            editAmount.setHint("Amount in " + (c != null ? c.name : trip.foreignCurrency));
            editAmount.setInputType(c != null && c.decimals == 0
                    ? InputType.TYPE_CLASS_NUMBER
                    : InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        } else {
            amountCurrency.setText(sym);
            editAmount.setHint("Amount");
            editAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        }
        refreshConversion();

        bindCategories();

        long resolved = TravelDate.resolve(prefs.getLong(K_DATE, 0),
                prefs.getBoolean(K_DATE_PINNED, false), System.currentTimeMillis());
        date.setTimeInMillis(resolved);
        showDate();
    }

    private View recentRow(String left, String right, long itemId) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(3), 0, dp(3));
        android.util.TypedValue tv = new android.util.TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        row.setBackgroundResource(tv.resourceId);

        TextView l = new TextView(activity);
        l.setText(left);
        l.setTextSize(14);
        l.setSingleLine(true);
        l.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView r = new TextView(activity);
        r.setText(right);
        r.setTextSize(14);
        r.setPadding(dp(8), 0, 0, 0);
        r.setTypeface(r.getTypeface(), android.graphics.Typeface.BOLD);
        row.addView(r);

        row.setOnClickListener(v -> {
            EditAccountItemDialog d = new EditAccountItemDialog();
            Bundle args = new Bundle();
            args.putLong(EditAccountItemDialog.ARG_ACCOUNT_ITEM_ID, itemId);
            d.setArguments(args);
            d.show(activity.getSupportFragmentManager(), "EDIT_TRAVEL_ITEM");
        });
        return row;
    }

    private void bindCategories() {
        bindingCategories = true;
        categories.clear();
        categories.addAll(TRAVEL_DEFAULTS);
        try {
            String raw = activity.getSharedPreferences(PREF_ACCOUNT_CATEGORIES, Context.MODE_PRIVATE)
                    .getString(KEY_ACCOUNT_CATEGORIES, null);
            if (raw != null) {
                JSONObject root = new JSONObject(raw);
                if (root.has(trip.name)) {
                    JSONArray arr = root.getJSONArray(trip.name);
                    for (int i = 0; i < arr.length(); i++) {
                        String c = arr.getString(i);
                        if (!categories.contains(c)) categories.add(c);
                    }
                }
            }
        } catch (Exception ignored) { }
        categoryAdapter.notifyDataSetChanged();

        String remembered = prefs.getString(K_CATEGORY + trip.id, null);
        int idx = remembered == null ? -1 : categories.indexOf(remembered);
        spinnerCategory.setSelection(Math.max(0, idx), false);
        spinnerCategory.post(() -> bindingCategories = false);
    }

    private void refreshConversion() {
        if (trip == null || !trip.hasForeignCurrency()) {
            conversion.setVisibility(View.GONE);
            return;
        }
        conversion.setVisibility(View.VISIBLE);
        String rate = AccountCurrency.rateLabel(trip);
        Double typed = parseAmount();
        if (typed == null) {
            conversion.setText(rate + "   (tap to change)");
        } else {
            double home = TripCurrency.toHome(typed, trip.rate, trip.homeCurrency);
            conversion.setText("= " + AccountCurrency.homeWhole(trip, home,
                    AppPrefs.getCurrencySymbol(activity)) + "  ·  " + rate);
        }
    }

    private void showDate() {
        String label = new SimpleDateFormat("dd MMM", Locale.ENGLISH).format(date.getTime());
        boolean today = !TravelDate.isPinned(date.getTimeInMillis(), System.currentTimeMillis());
        dateView.setText(today ? "Today · " + label : label);
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void pickTrip() {
        List<AccountEntity> trips = new ArrayList<>();
        for (AccountEntity a : ExpenseDatabase.getDatabase(activity).accountDao().getActiveAccounts()) {
            if ("travel".equals(a.type)) trips.add(a);
        }
        if (trips.isEmpty()) {
            new AlertDialog.Builder(activity)
                    .setTitle("Travel mode")
                    .setMessage("Travel mode works with a Travel account. Create one first: "
                            + "tap +, choose Accounts, and add a new account of type Travel.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        String[] names = new String[trips.size()];
        int checked = -1;
        for (int i = 0; i < trips.size(); i++) {
            AccountEntity a = trips.get(i);
            names[i] = a.hasForeignCurrency() ? a.name + "  (" + a.foreignCurrency + ")" : a.name;
            if (trip != null && a.id == trip.id) checked = i;
        }
        new AlertDialog.Builder(activity)
                .setTitle("Travel mode: which trip?")
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    d.dismiss();
                    begin(trips.get(which));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void pickDate() {
        new DatePickerDialog(activity, (v, y, m, d) -> {
            date.set(y, m, d);
            long chosen = TravelDate.midnight(date.getTimeInMillis());
            prefs.edit()
                    .putLong(K_DATE, chosen)
                    .putBoolean(K_DATE_PINNED, TravelDate.isPinned(chosen, System.currentTimeMillis()))
                    .apply();
            showDate();
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void changeRate() {
        if (trip == null || !trip.hasForeignCurrency()) return;
        ExpenseDatabase db = ExpenseDatabase.getDatabase(activity);
        boolean hasEntries = !db.accountItemDao().getItemsForAccount(trip.id).isEmpty();
        TripCurrencyDialog.show(activity, trip.name + ": currency",
                trip.homeCurrency, trip.foreignCurrency, trip.rate,
                hasEntries, hasEntries,
                (home, foreign, rate) -> {
                    db.accountDao().setCurrency(trip.id, foreign, home, rate);
                    refresh();
                },
                null, null);
    }

    private Double parseAmount() {
        String s = editAmount.getText().toString().replace(",", "").trim();
        if (s.isEmpty()) return null;
        try {
            double v = Double.parseDouble(s);
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void add() {
        if (trip == null) return;
        String item = editItem.getText().toString().trim();
        if (item.isEmpty()) {
            editItem.setError("What was it?");
            editItem.requestFocus();
            return;
        }
        Double amount = parseAmount();
        if (amount == null) {
            editAmount.setError("Enter an amount");
            editAmount.requestFocus();
            return;
        }
        int pos = spinnerCategory.getSelectedItemPosition();
        String category = pos >= 0 && pos < categories.size() ? categories.get(pos) : "Other";

        long dayMillis = TravelDate.midnight(date.getTimeInMillis());
        String dateText = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(dayMillis);

        AccountItemEntity entity = new AccountItemEntity(
                trip.id, dateText, dayMillis, item, category, amount, "");
        // On a trip with a currency the typed amount is in the trip currency
        AccountCurrency.applyForeign(trip, entity, amount);
        ExpenseDatabase.getDatabase(activity).accountItemDao().insert(entity);

        prefs.edit().putString(K_CATEGORY + trip.id, category).apply();

        editItem.setText("");
        editAmount.setText("");
        editItem.setError(null);
        editAmount.setError(null);
        editItem.requestFocus();

        Toast.makeText(activity, "Added: " + item, Toast.LENGTH_SHORT).show();
        activity.refreshAfterAdd();   // also redraws this card
    }

    // ------------------------------------------------------------------

    private void hideKeyboard() {
        View f = activity.getCurrentFocus();
        if (f == null) return;
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(f.getWindowToken(), 0);
        f.clearFocus();
    }

    private int dp(int v) {
        return Math.round(v * activity.getResources().getDisplayMetrics().density);
    }
}
