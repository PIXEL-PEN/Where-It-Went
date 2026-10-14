package com.pixelpen.whereitwent;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Travel mode (v3.3), full screen: the trip's entries day by day, with the
 * entry card at the bottom. While the keyboard is open the card moves up
 * under the top bar, with the newest entries showing beneath it.
 *
 * The app opens here while travel mode is on, with the same trip, category
 * and date. The home button goes back to the main screen, which shows a
 * "✈ trip" button to come back.
 */
public class TravelActivity extends AppCompatActivity {

    // Same stores AddExpenseDialog uses for an account's own categories
    private static final String PREF_ACCOUNT_CATEGORIES = "account_categories_store";
    private static final String KEY_ACCOUNT_CATEGORIES = "account_categories";
    private static final List<String> TRAVEL_DEFAULTS = Arrays.asList(
            "Lodging", "Meals", "Transportation", "Activities", "Other");

    private AccountEntity trip;
    private String settingsSymbol;

    private LinearLayout body;
    private View listFrame, card;
    private RecyclerView list;
    private TextView emptyList, tripName, tripTotal, amountCurrency, conversion, dateView;
    private EditText editItem, editAmount, editNote;
    private Spinner spinnerCategory;
    private final List<String> categories = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;
    private final Calendar date = Calendar.getInstance();
    private boolean bindingCategories;
    private boolean cardOnTop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Draw edge to edge and place the keyboard and navigation bar ourselves
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_travel);

        body = findViewById(R.id.travel_body);
        listFrame = findViewById(R.id.travel_list_frame);
        card = findViewById(R.id.travel_card);
        list = findViewById(R.id.travel_list);
        emptyList = findViewById(R.id.travel_list_empty);
        tripName = findViewById(R.id.travel_trip_name);
        tripTotal = findViewById(R.id.travel_trip_total);
        amountCurrency = findViewById(R.id.travel_amount_currency);
        conversion = findViewById(R.id.travel_conversion);
        dateView = findViewById(R.id.travel_date);
        editItem = findViewById(R.id.travel_edit_item);
        editAmount = findViewById(R.id.travel_edit_amount);
        editNote = findViewById(R.id.travel_edit_note);
        spinnerCategory = findViewById(R.id.travel_spinner_category);

        list.setLayoutManager(new LinearLayoutManager(this));

        // Slightly stronger hints than the theme's default, for outdoor reading
        android.content.res.ColorStateList hint =
                ContextCompat.getColorStateList(this, R.color.travel_hint);
        editItem.setHintTextColor(hint);
        editAmount.setHintTextColor(hint);
        editNote.setHintTextColor(hint);

        categoryAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_selected, categories);
        categoryAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerCategory.setAdapter(categoryAdapter);

        findViewById(R.id.travel_btn_home).setOnClickListener(v -> finish());
        findViewById(R.id.travel_btn_end).setOnClickListener(v -> confirmEnd());
        findViewById(R.id.travel_header_text).setOnClickListener(v ->
                TravelMode.pickTrip(this, trip != null ? trip.id : -1, t -> {
                    editItem.setText("");
                    editAmount.setText("");
                    editNote.setText("");
                    bind();
                }));
        findViewById(R.id.travel_btn_add).setOnClickListener(v -> add());
        dateView.setOnClickListener(v -> pickDate());
        conversion.setOnClickListener(v -> changeRate());

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (bindingCategories || trip == null || pos < 0 || pos >= categories.size()) return;
                TravelMode.setCategory(TravelActivity.this, trip.id, categories.get(pos));
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });

        editAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { refreshConversion(); }
        });
        // Done on the note (the last field) adds the entry
        editNote.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) { add(); return true; }
            return false;
        });

        // Keyboard open: card up under the top bar. Closed: card back at the bottom.
        View root = findViewById(R.id.travel_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, 0, bars.right, Math.max(ime.bottom, bars.bottom));
            boolean typing = insets.isVisible(WindowInsetsCompat.Type.ime()) || ime.bottom > bars.bottom;
            v.post(() -> placeCard(typing));   // not while insets are being dispatched
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        settingsSymbol = AppPrefs.getCurrencySymbol(this);
        trip = TravelMode.activeTrip(this);
        if (trip == null) {   // ended, or the trip was deleted or archived
            finish();
            return;
        }
        bind();
    }

    /** After an entry was edited from this screen. */
    public void refresh() {
        trip = TravelMode.activeTrip(this);
        if (trip == null) { finish(); return; }
        bind();
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private void placeCard(boolean onTop) {
        if (onTop == cardOnTop) return;
        cardOnTop = onTop;
        // Move the list, never the card: taking the card out of the layout
        // would take focus (and the cursor) away from the field being typed in
        body.removeView(listFrame);
        body.addView(listFrame, onTop ? body.getChildCount() : 0);
        if (onTop) list.scrollToPosition(0);   // newest entries right under the card
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    private void bind() {
        if (trip == null) return;
        trip = TravelMode.activeTrip(this);
        if (trip == null) { finish(); return; }

        List<AccountItemEntity> all = ExpenseDatabase.getDatabase(this)
                .accountItemDao().getItemsForAccount(trip.id);   // newest first

        tripName.setText("✈  " + trip.name + "  ▾");
        tripTotal.setText(all.isEmpty() ? "No spending yet" : "Total " + total(all));

        list.setAdapter(new EntriesAdapter(rows(all)));
        emptyList.setVisibility(all.isEmpty() ? View.VISIBLE : View.GONE);

        if (trip.hasForeignCurrency()) {
            TripCurrency.Currency c = TripCurrency.byCode(trip.foreignCurrency);
            amountCurrency.setText(trip.foreignCurrency);
            editAmount.setHint("Amount");   // the currency code shows beside it
            editAmount.setInputType(c != null && c.decimals == 0
                    ? InputType.TYPE_CLASS_NUMBER
                    : InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        } else {
            amountCurrency.setText(settingsSymbol);
            editAmount.setHint("Amount");
            editAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        }
        refreshConversion();
        bindCategories();

        date.setTimeInMillis(TravelMode.date(this));
        showDate();
    }

    /** "1,955,000 ₫ · 2,500 ฿", or the home amount for a trip without a currency. */
    private String total(List<AccountItemEntity> items) {
        double home = 0;
        for (AccountItemEntity it : items) home += it.amount;
        Double foreign = AccountCurrency.foreignTotal(trip, items);
        if (foreign != null) {
            return AccountCurrency.foreign(trip, foreign) + "  ·  "
                    + AccountCurrency.homeWhole(trip, home, settingsSymbol);
        }
        return AccountCurrency.home(trip, home, settingsSymbol);
    }

    private void bindCategories() {
        bindingCategories = true;
        categories.clear();
        categories.addAll(TRAVEL_DEFAULTS);
        try {
            String raw = getSharedPreferences(PREF_ACCOUNT_CATEGORIES, Context.MODE_PRIVATE)
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

        String remembered = TravelMode.category(this, trip.id);
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
            conversion.setText("= " + AccountCurrency.homeWhole(trip, home, settingsSymbol)
                    + "  ·  " + rate);
        }
    }

    private void showDate() {
        String label = new SimpleDateFormat("dd MMM", Locale.ENGLISH).format(date.getTime());
        boolean today = !TravelDate.isPinned(date.getTimeInMillis(), System.currentTimeMillis());
        dateView.setText(today ? "Today · " + label : label);
    }

    // ------------------------------------------------------------------
    // Day-by-day list
    // ------------------------------------------------------------------

    /** A day header, or an entry. */
    private static final class Row {
        final String dayTitle, dayTotal;
        final AccountItemEntity item;
        Row(String title, String total) { dayTitle = title; dayTotal = total; item = null; }
        Row(AccountItemEntity it) { dayTitle = null; dayTotal = null; item = it; }
    }

    private List<Row> rows(List<AccountItemEntity> all) {
        Map<Long, List<AccountItemEntity>> byDay = new LinkedHashMap<>();
        for (AccountItemEntity it : all) {
            long day = it.dateMillis > 0 ? TravelDate.midnight(it.dateMillis) : 0;
            List<AccountItemEntity> l = byDay.get(day);
            if (l == null) { l = new ArrayList<>(); byDay.put(day, l); }
            l.add(it);
        }
        SimpleDateFormat fmt = new SimpleDateFormat("EEE dd MMM", Locale.ENGLISH);
        long today = TravelDate.midnight(System.currentTimeMillis());
        List<Row> rows = new ArrayList<>();
        for (Map.Entry<Long, List<AccountItemEntity>> e : byDay.entrySet()) {
            long day = e.getKey();
            String title = day == 0 ? e.getValue().get(0).date
                    : (day == today ? "Today · " : "") + fmt.format(day);
            rows.add(new Row(title, total(e.getValue())));
            for (AccountItemEntity it : e.getValue()) rows.add(new Row(it));
        }
        return rows;
    }

    private final class EntriesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<Row> rows;
        EntriesAdapter(List<Row> rows) { this.rows = rows; }

        @Override public int getItemCount() { return rows.size(); }
        @Override public int getItemViewType(int pos) { return rows.get(pos).item == null ? 0 : 1; }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View v = type == 0 ? dayHeaderView() : entryView();
            return new RecyclerView.ViewHolder(v) { };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
            Row r = rows.get(pos);
            ViewGroup v = (ViewGroup) h.itemView;
            if (r.item == null) {
                ((TextView) v.getChildAt(0)).setText(r.dayTitle);
                ((TextView) v.getChildAt(1)).setText(r.dayTotal);
                v.setOnClickListener(null);
                return;
            }
            AccountItemEntity it = r.item;
            ViewGroup left = (ViewGroup) v.getChildAt(0);
            ViewGroup right = (ViewGroup) v.getChildAt(1);
            ((TextView) left.getChildAt(0)).setText(it.item);
            String sub = it.category == null ? "" : it.category;
            if (it.note != null && !it.note.trim().isEmpty()) sub += " · " + it.note.trim();
            ((TextView) left.getChildAt(1)).setText(sub);

            TextView main = (TextView) right.getChildAt(0);
            TextView under = (TextView) right.getChildAt(1);
            if (it.hasForeignAmount() && trip.hasForeignCurrency()) {
                main.setText(AccountCurrency.foreign(trip, it.foreignAmount));
                String home = AccountCurrency.homeWhole(trip, it.amount, settingsSymbol);
                String note = AccountCurrency.rateNote(trip, it);
                under.setText(note.isEmpty() ? home : home + "  (" + note + ")");
                under.setVisibility(View.VISIBLE);
            } else {
                main.setText(AccountCurrency.home(trip, it.amount, settingsSymbol));
                under.setVisibility(View.GONE);
            }
            v.setOnClickListener(x -> edit(it.id));
        }
    }

    private View dayHeaderView() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(ContextCompat.getColor(this, R.color.header_bg_blue));
        row.setPadding(dp(12), dp(7), dp(12), dp(7));
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(10), dp(10), dp(10), dp(2));
        row.setLayoutParams(lp);

        int ink = ContextCompat.getColor(this, R.color.colorPrimary);
        TextView title = text(15, true, ink);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView total = text(14, true, ink);
        total.setPadding(dp(8), 0, 0, 0);
        row.addView(total);
        return row;
    }

    private View entryView() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(ContextCompat.getColor(this, R.color.settings_card_bg));
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(10), dp(1), dp(10), 0);
        row.setLayoutParams(lp);
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        row.setForeground(ContextCompat.getDrawable(this, tv.resourceId));

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(text(15, false, 0));
        TextView sub = text(12, false, 0);
        sub.setAlpha(0.78f);
        left.addView(sub);
        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END);
        right.setPadding(dp(8), 0, 0, 0);
        right.addView(text(15, true, 0));
        TextView under = text(12, false, 0);
        under.setAlpha(0.78f);
        right.addView(under);
        row.addView(right);
        return row;
    }

    /** A TextView; colour 0 = the travel screen's text colour. */
    private TextView text(int sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setTextSize(sp);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        if (bold) t.setTypeface(t.getTypeface(), Typeface.BOLD);
        t.setTextColor(color != 0 ? color : ContextCompat.getColor(this, R.color.travel_text));
        return t;
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void edit(long itemId) {
        EditAccountItemDialog d = new EditAccountItemDialog();
        Bundle args = new Bundle();
        args.putLong(EditAccountItemDialog.ARG_ACCOUNT_ITEM_ID, itemId);
        d.setArguments(args);
        d.show(getSupportFragmentManager(), "EDIT_TRAVEL_ITEM");
    }

    private void confirmEnd() {
        new AlertDialog.Builder(this)
                .setTitle("End travel mode?")
                .setMessage("The trip and its entries stay in Accounts. "
                        + "You can start travel mode again from the menu.")
                .setPositiveButton("End", (d, w) -> {
                    TravelMode.end(this);
                    Toast.makeText(this, "Travel mode ended", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void pickDate() {
        new DatePickerDialog(this, (v, y, m, d) -> {
            date.set(y, m, d);
            TravelMode.setDate(this, date.getTimeInMillis());
            showDate();
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void changeRate() {
        if (trip == null || !trip.hasForeignCurrency()) return;
        ExpenseDatabase db = ExpenseDatabase.getDatabase(this);
        boolean hasEntries = !db.accountItemDao().getItemsForAccount(trip.id).isEmpty();
        TripCurrencyDialog.show(this, trip.name + ": currency",
                trip.homeCurrency, trip.foreignCurrency, trip.rate,
                hasEntries, hasEntries,
                (home, foreign, rate) -> {
                    db.accountDao().setCurrency(trip.id, foreign, home, rate);
                    bind();
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

        String note = editNote.getText().toString().trim();

        AccountItemEntity entity = new AccountItemEntity(
                trip.id, dateText, dayMillis, item, category, amount, note);
        // On a trip with a currency the typed amount is in the trip currency
        AccountCurrency.applyForeign(trip, entity, amount);
        ExpenseDatabase.getDatabase(this).accountItemDao().insert(entity);

        TravelMode.setCategory(this, trip.id, category);

        editItem.setText("");
        editAmount.setText("");
        editNote.setText("");
        editItem.setError(null);
        editAmount.setError(null);
        hideKeyboard();   // the card drops back to the bottom, showing the new entry

        Toast.makeText(this, "Added: " + item, Toast.LENGTH_SHORT).show();
        bind();
        list.scrollToPosition(0);
    }

    @Override
    public void finish() {
        hideKeyboard();
        super.finish();
    }

    private void hideKeyboard() {
        View f = getCurrentFocus();
        if (f == null) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(f.getWindowToken(), 0);
        f.clearFocus();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
