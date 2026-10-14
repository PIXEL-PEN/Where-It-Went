package com.pixelpen.whereitwent;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Trip currency settings for a Travel account (v3.3): home currency, trip
 * currency and the exchange rate, with a live example. Uses the layout
 * dialog_foreign_currency.xml.
 */
public final class TripCurrencyDialog {

    public interface OnSave {
        void onSave(String homeCode, String foreignCode, double rate);
    }

    private TripCurrencyDialog() { }

    /**
     * @param lockHome  true when the trip already has entries: their amounts are
     *                  stored in the home currency, so it can't change
     * @param lockTrip  true when entries were also made in the trip currency
     * @param onCancel       may be null
     * @param onTurnOff      may be null; when set, a "Turn off" button is shown
     */
    public static void show(Context ctx, String title,
                            String homeCode, String foreignCode, double rate,
                            boolean lockHome, boolean lockTrip,
                            OnSave onSave, Runnable onCancel, Runnable onTurnOff) {

        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_foreign_currency, null);

        TextView titleView = v.findViewById(R.id.text_title);
        Spinner spinnerHome = v.findViewById(R.id.spinner_domestic);
        Spinner spinnerTrip = v.findViewById(R.id.spinner_foreign);
        TextView rateLeft = v.findViewById(R.id.text_rate_left);
        EditText editRate = v.findViewById(R.id.edit_rate);
        TextView rateRight = v.findViewById(R.id.text_rate_right);
        TextView preview = v.findViewById(R.id.text_preview);

        // The layout's own buttons are replaced by the dialog's buttons
        v.findViewById(R.id.btn_ok).setVisibility(View.GONE);
        v.findViewById(R.id.btn_cancel).setVisibility(View.GONE);

        titleView.setText(title);
        // Whole numbers only (the rate is quoted so it never needs decimals)
        editRate.setInputType(InputType.TYPE_CLASS_NUMBER);
        editRate.setHint("");

        List<String> labels = new ArrayList<>();
        for (TripCurrency.Currency c : TripCurrency.ALL) labels.add(c.label());

        spinnerHome.setAdapter(adapter(ctx, labels));
        spinnerTrip.setAdapter(adapter(ctx, labels));

        int homeIdx = Math.max(0, TripCurrency.indexOf(homeCode));
        int tripIdx = TripCurrency.indexOf(foreignCode);
        if (tripIdx < 0) tripIdx = homeIdx == 0 ? 1 : 0;   // anything but the home currency
        spinnerHome.setSelection(homeIdx, false);
        spinnerTrip.setSelection(tripIdx, false);

        // ---- Rate, quoted the way banks do and in whole numbers (v3.3) ----
        // The row reads "<qty> <base> = [value] <other>", e.g. "1 THB = [772] VND"
        // or "10 CNY = [50] THB": the stronger currency is the base, so the
        // value is never a small decimal. The stored rate is converted from it.
        final TripCurrency.Quote[] shape = new TripCurrency.Quote[1];
        final boolean[] auto = {false};          // value is the built-in suggestion
        final boolean[] settingText = {false};   // we are setting the field ourselves
        final String origHome = homeCode, origTrip = foreignCode;
        final String[] origText = {null};        // the existing rate as first shown

        Runnable suggest = () -> {
            String home = code(spinnerHome), trip = code(spinnerTrip);
            Double sug = ApproxRates.suggest(home, trip);
            shape[0] = TripCurrency.quote(home, sug != null ? sug : 1, trip);
            settingText[0] = true;
            editRate.setText(sug == null ? "" : String.valueOf(Math.round(shape[0].value)));
            editRate.setSelection(editRate.getText().length());
            settingText[0] = false;
            auto[0] = sug != null;
        };
        if (rate > 0) {
            shape[0] = TripCurrency.quote(homeCode, rate, foreignCode);
            origText[0] = String.valueOf(Math.round(shape[0].value));
            editRate.setText(origText[0]);
        } else {
            suggest.run();
        }

        spinnerHome.setEnabled(!lockHome);
        spinnerTrip.setEnabled(!lockTrip);

        // The stored rate (trip units per 1 home unit) for what is on screen, or null
        final java.util.concurrent.Callable<Double> currentRate = () -> {
            Double value = TripCurrency.parseRate(editRate.getText().toString());
            if (value == null || shape[0] == null) return null;
            String home = code(spinnerHome), trip = code(spinnerTrip);
            // Unchanged existing rate: keep it exactly (the field shows it rounded)
            if (rate > 0 && editRate.getText().toString().trim().equals(origText[0])
                    && home.equals(origHome) && trip.equals(origTrip)) {
                return rate;
            }
            return TripCurrency.rateFromQuote(home, shape[0], value);
        };

        Runnable refresh = () -> {
            String home = code(spinnerHome);
            String trip = code(spinnerTrip);
            if (shape[0] != null) {
                rateLeft.setText(String.format(java.util.Locale.US, "%,d", shape[0].qty)
                        + " " + shape[0].base + " = ");
                rateRight.setText(" " + shape[0].other);
            }

            StringBuilder p = new StringBuilder();
            Double r = null;
            try { r = currentRate.call(); } catch (Exception ignored) { }
            boolean codes = TripCurrency.useCodes(home, trip);
            if (home.equals(trip)) {
                p.append("Choose a trip currency different from the home currency.");
            } else if (r == null) {
                p.append("Enter the rate in whole numbers, e.g. from your bank or exchange booth.");
            } else {
                double example = roundToOneFigure(50 * r);
                p.append("Example: ")
                        .append(TripCurrency.formatWhole(example, trip, codes))
                        .append(" = ")
                        .append(TripCurrency.formatWhole(TripCurrency.toHome(example, r, home), home, codes));
            }
            if (auto[0]) {
                p.append("\n\nSuggested rate: approximate market rate, ")
                        .append(ApproxRates.AS_OF_LABEL)
                        .append(". Your bank's or exchange booth's rate will differ, so change it if you know it.");
            }
            if (lockHome && lockTrip) {
                p.append("\n\nThe currencies are fixed once the trip has entries. "
                        + "A new rate applies to new entries only.");
            } else if (lockHome) {
                p.append("\n\nThe home currency is fixed because the trip already has "
                        + "entries. Earlier entries stay as they are.");
            } else {
                p.append("\n\nEach entry keeps the rate it was entered at.");
            }
            preview.setText(p.toString());
        };

        AdapterView.OnItemSelectedListener onPick = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String home = code(spinnerHome), trip = code(spinnerTrip);
                // A different currency pair needs a fresh rate (and quote direction)
                boolean pairChanged = !(home.equals(origHome) && trip.equals(origTrip));
                if (auto[0] || pairChanged || editRate.getText().toString().trim().isEmpty()) {
                    suggest.run();
                }
                refresh.run();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };
        spinnerHome.setOnItemSelectedListener(onPick);
        spinnerTrip.setOnItemSelectedListener(onPick);
        editRate.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                if (!settingText[0]) auto[0] = false;   // the user typed their own
                refresh.run();
            }
        });
        refresh.run();

        AlertDialog.Builder b = new AlertDialog.Builder(ctx)
                .setView(v)
                .setPositiveButton("Save", null)   // set below, so a bad rate doesn't close it
                .setNegativeButton("Cancel", (d, w) -> {
                    if (onCancel != null) onCancel.run();
                })
                .setOnCancelListener(d -> {
                    if (onCancel != null) onCancel.run();
                });
        if (onTurnOff != null) {
            b.setNeutralButton("Turn off", (d, w) -> onTurnOff.run());
        }

        AlertDialog dialog = b.create();
        dialog.setOnShowListener(d -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setOnClickListener(x -> {
                String home = code(spinnerHome);
                String trip = code(spinnerTrip);
                Double r = null;
                try { r = currentRate.call(); } catch (Exception ignored) { }
                if (home.equals(trip)) {
                    editRate.setError("Trip and home currency must differ");
                    return;
                }
                if (r == null) {
                    editRate.setError("Enter a rate above 0");
                    return;
                }
                dialog.dismiss();
                onSave.onSave(home, trip, r);
            });
        });
        dialog.show();
    }

    private static ArrayAdapter<String> adapter(Context ctx, List<String> labels) {
        ArrayAdapter<String> a = new ArrayAdapter<>(ctx, R.layout.spinner_item_selected, labels);
        a.setDropDownViewResource(R.layout.spinner_item_dropdown);
        return a;
    }

    private static String code(Spinner s) {
        int i = s.getSelectedItemPosition();
        if (i < 0 || i >= TripCurrency.ALL.size()) i = 0;
        return TripCurrency.ALL.get(i).code;
    }

    /** 1,272,500 -> 1,000,000; 355 -> 400; keeps the example a round number. */
    private static double roundToOneFigure(double x) {
        if (x <= 0) return 0;
        double mag = Math.pow(10, Math.floor(Math.log10(x)));
        return Math.max(1, Math.round(x / mag)) * mag;
    }
}
