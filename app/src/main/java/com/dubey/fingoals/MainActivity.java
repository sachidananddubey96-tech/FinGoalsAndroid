package com.dubey.fingoals;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private LinearLayout root;
    private LinearLayout content;
    private FinanceCore.Data data = new FinanceCore.Data();
    private FinanceCore.Summary summary = new FinanceCore.Summary();
    private String currentView = "Dashboard";

    private final int bg = Color.rgb(0, 0, 0);
    private final int surface = Color.rgb(28, 28, 30);
    private final int surface2 = Color.rgb(44, 44, 46);
    private final int text = Color.WHITE;
    private final int muted = Color.rgb(174, 174, 178);
    private final int accent = Color.rgb(10, 132, 255);
    private final int green = Color.rgb(48, 209, 88);
    private final int orange = Color.rgb(255, 159, 10);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showLogin();
    }

    private void showLogin() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setGravity(Gravity.CENTER);
        screen.setPadding(dp(24), dp(24), dp(24), dp(24));
        screen.setBackgroundColor(bg);

        TextView title = label("Dubey's FinGoals", 28, text, true);
        title.setGravity(Gravity.CENTER);
        TextView sub = label("Native finance dashboard", 14, muted, false);
        sub.setGravity(Gravity.CENTER);
        EditText pass = input("Password");
        pass.setInputType(0x00000081);
        Button open = button("Unlock");
        open.setOnClickListener(v -> {
            if (FinanceCore.APP_PASSWORD.equals(pass.getText().toString())) {
                hideKeyboard(pass);
                buildApp();
            } else {
                toast("Incorrect password");
            }
        });

        screen.addView(title);
        screen.addView(sub);
        screen.addView(spacer(20));
        screen.addView(pass);
        screen.addView(open);
        setContentView(screen);
    }

    private void buildApp() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);

        LinearLayout header = new LinearLayout(this);
        header.setPadding(dp(16), dp(16), dp(16), dp(10));
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(surface);

        TextView title = label("Dubey's FinGoals", 20, text, true);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button sync = smallButton("Sync");
        sync.setOnClickListener(v -> syncData());
        header.addView(sync);
        root.addView(header);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setPadding(dp(8), dp(8), dp(8), dp(8));
        nav.setBackgroundColor(surface);
        for (String item : new String[]{"Dashboard", "Add", "Records", "Cards"}) {
            Button b = smallButton(item);
            b.setOnClickListener(v -> {
                currentView = ((Button) v).getText().toString();
                render();
            });
            nav.addView(b, new LinearLayout.LayoutParams(0, dp(44), 1));
        }
        root.addView(nav);
        setContentView(root);

        String cached = FinanceCore.loadCache(this);
        if (cached.length() > 0) {
            data = FinanceCore.parse(cached);
            summary = FinanceCore.summarize(data);
            render();
        } else {
            renderLoading();
        }
        syncData();
    }

    private void syncData() {
        toast("Syncing...");
        io.execute(() -> {
            try {
                String json = FinanceCore.fetchJson();
                FinanceCore.saveCache(this, json);
                data = FinanceCore.parse(json);
                summary = FinanceCore.summarize(data);
                updateWidgets();
                runOnUiThread(() -> {
                    render();
                    toast("Synced");
                });
            } catch (Exception e) {
                runOnUiThread(() -> toast("Sync failed. Showing cached data."));
            }
        });
    }

    private void updateWidgets() {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        int[] ids = manager.getAppWidgetIds(new ComponentName(this, FinGoalsWidgetProvider.class));
        FinGoalsWidgetProvider.updateAll(this, manager, ids, data);
    }

    private void render() {
        content.removeAllViews();
        content.setPadding(dp(16), dp(16), dp(16), dp(16));
        if ("Add".equals(currentView)) renderAdd();
        else if ("Records".equals(currentView)) renderRecords();
        else if ("Cards".equals(currentView)) renderCards();
        else renderDashboard();
    }

    private void renderLoading() {
        content.removeAllViews();
        content.setPadding(dp(16), dp(16), dp(16), dp(16));
        content.addView(card("Loading", "Fetching your finance data...", muted));
    }

    private void renderDashboard() {
        content.addView(section("Dashboard"));
        content.addView(kpi("Total Investments", FinanceCore.inr(summary.totalInvestments), orange));
        content.addView(kpi("This Month Expenses", FinanceCore.inr(summary.monthlyExpenses), text));
        content.addView(kpi("Unbilled Credit Cards", FinanceCore.inr(summary.unbilledCards), green));

        LinearLayout inv = panel();
        inv.addView(label("Investment Targets", 17, text, true));
        inv.addView(row("Sachidanand monthly", FinanceCore.inr(summary.sachinMonthlyInvestment) + " / " + FinanceCore.inr(85000)));
        inv.addView(row("Asha monthly", FinanceCore.inr(summary.ashaMonthlyInvestment) + " / " + FinanceCore.inr(85000)));
        inv.addView(row("Combined yearly", FinanceCore.inr(summary.yearInvestments) + " / " + FinanceCore.inr(2000000)));
        content.addView(inv);

        LinearLayout upi = panel();
        upi.addView(label("UPI Spending - " + FinanceCore.monthYear(null), 17, text, true));
        upi.addView(row("Sachidanand", FinanceCore.inr(summary.sachinUpi)));
        upi.addView(row("Asha", FinanceCore.inr(summary.ashaUpi)));
        content.addView(upi);
    }

    private void renderAdd() {
        content.addView(section("Add Record"));
        content.addView(addOutflowForm());
        content.addView(addSalaryForm());
    }

    private View addOutflowForm() {
        LinearLayout box = panel();
        box.addView(label("Outflow", 17, text, true));
        Spinner owner = spinner(new String[]{"Sachidanand Dubey", "Asha Dubey"});
        EditText date = input("Date yyyy-mm-dd");
        date.setText(FinanceCore.today());
        Spinner category = spinner(withFallback(FinanceCore.categories(data), "Daily Expenses"));
        EditText sub = input("Sub category");
        EditText amount = input("Amount");
        amount.setInputType(0x00002002);
        Spinner payMode = spinner(new String[]{"UPI", "Credit Card", "Cash", "Bank Transfer", "Auto Debit"});
        Spinner card = spinner(withFallback(FinanceCore.cardIds(data, ""), "Select Card"));
        EditText notes = input("Notes");
        Button save = button("Save Outflow");
        save.setOnClickListener(v -> {
            try {
                JSONObject payload = FinanceCore.appendTransaction(
                        owner.getSelectedItem().toString(),
                        date.getText().toString(),
                        category.getSelectedItem().toString(),
                        sub.getText().toString(),
                        amount.getText().toString(),
                        payMode.getSelectedItem().toString(),
                        card.getSelectedItem().toString(),
                        notes.getText().toString());
                postAndRefresh(payload);
            } catch (Exception e) {
                toast("Could not save outflow");
            }
        });
        for (View v : new View[]{owner, date, category, sub, amount, payMode, card, notes, save}) box.addView(v);
        return box;
    }

    private View addSalaryForm() {
        LinearLayout box = panel();
        box.addView(label("Inflow", 17, text, true));
        Spinner owner = spinner(new String[]{"Sachidanand Dubey", "Asha Dubey"});
        EditText date = input("Date yyyy-mm-dd");
        date.setText(FinanceCore.today());
        EditText amount = input("Salary amount");
        amount.setInputType(0x00002002);
        Button save = button("Save Inflow");
        save.setOnClickListener(v -> {
            try {
                postAndRefresh(FinanceCore.appendSalary(owner.getSelectedItem().toString(), date.getText().toString(), amount.getText().toString()));
            } catch (Exception e) {
                toast("Could not save inflow");
            }
        });
        for (View v : new View[]{owner, date, amount, save}) box.addView(v);
        return box;
    }

    private void postAndRefresh(JSONObject payload) {
        toast("Saving...");
        io.execute(() -> {
            try {
                JSONObject response = new JSONObject(FinanceCore.postJson(payload));
                runOnUiThread(() -> toast(response.optString("status", "Saved")));
                syncData();
            } catch (Exception e) {
                runOnUiThread(() -> toast("Save failed"));
            }
        });
    }

    private void renderRecords() {
        content.addView(section("Recent Records"));
        List<Map<String, String>> rows = new ArrayList<>(data.transactions);
        Collections.sort(rows, (a, b) -> FinanceCore.val(b, "Date").compareTo(FinanceCore.val(a, "Date")));
        int max = Math.min(60, rows.size());
        for (int i = 0; i < max; i++) {
            Map<String, String> t = rows.get(i);
            content.addView(card(
                    FinanceCore.val(t, "Sub_Category").length() > 0 ? FinanceCore.val(t, "Sub_Category") : FinanceCore.val(t, "Category"),
                    FinanceCore.val(t, "Date") + " · " + ownerShort(FinanceCore.val(t, "Owner")) + " · " + FinanceCore.val(t, "Pay_Mode"),
                    FinanceCore.money(t.get("Amount")) > 0 ? orange : text,
                    FinanceCore.inr(FinanceCore.money(t.get("Amount")))));
        }
        if (max == 0) content.addView(card("No records", "Sync to load transactions.", muted));
    }

    private void renderCards() {
        content.addView(section("Credit Cards"));
        List<Map<String, String>> cards = new ArrayList<>(data.cards);
        Collections.sort(cards, Comparator.comparing(a -> FinanceCore.val(a, "Card_ID")));
        for (Map<String, String> c : cards) {
            double unbilled = FinanceCore.cardUnbilled(data.transactions, c);
            content.addView(card(
                    FinanceCore.val(c, "Card_Name") + " (" + FinanceCore.val(c, "Card_ID") + ")",
                    ownerShort(FinanceCore.val(c, "Card_Owner")) + " · Due day " + FinanceCore.val(c, "Due_Day"),
                    green,
                    FinanceCore.inr(unbilled)));
        }
        if (cards.isEmpty()) content.addView(card("No cards", "Sync to load card profiles.", muted));
    }

    private LinearLayout panel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        box.setLayoutParams(lp);
        GradientDrawable bgd = new GradientDrawable();
        bgd.setColor(surface);
        bgd.setCornerRadius(dp(20));
        bgd.setStroke(1, Color.rgb(58, 58, 60));
        box.setBackground(bgd);
        return box;
    }

    private View kpi(String title, String value, int color) {
        return card(title, "", color, value);
    }

    private View card(String title, String body, int color) {
        return card(title, body, color, "");
    }

    private View card(String title, String body, int color, String trailing) {
        LinearLayout box = panel();
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        TextView left = label(title, 15, text, true);
        line.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        if (trailing.length() > 0) line.addView(label(trailing, 20, color, true));
        box.addView(line);
        if (body.length() > 0) box.addView(label(body, 12, muted, false));
        return box;
    }

    private View row(String left, String right) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(4));
        row.addView(label(left, 14, muted, false), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(label(right, 14, text, true));
        return row;
    }

    private TextView section(String s) {
        TextView v = label(s, 22, text, true);
        v.setPadding(0, 0, 0, dp(12));
        return v;
    }

    private TextView label(String s, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(muted);
        e.setTextColor(text);
        e.setSingleLine(true);
        e.setPadding(dp(14), 0, dp(14), 0);
        e.setBackgroundColor(surface2);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        lp.setMargins(0, dp(8), 0, dp(8));
        e.setLayoutParams(lp);
        return e;
    }

    private Spinner spinner(String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values);
        Spinner s = new Spinner(this);
        s.setAdapter(adapter);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        lp.setMargins(0, dp(8), 0, dp(8));
        s.setLayoutParams(lp);
        return s;
    }

    private Button button(String text) {
        Button b = smallButton(text);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        lp.setMargins(0, dp(10), 0, dp(4));
        b.setLayoutParams(lp);
        return b;
    }

    private Button smallButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setBackgroundColor(accent);
        return b;
    }

    private View spacer(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h)));
        return v;
    }

    private String[] withFallback(List<String> values, String fallback) {
        if (values == null || values.isEmpty()) return new String[]{fallback};
        Collections.sort(values);
        return values.toArray(new String[0]);
    }

    private String ownerShort(String owner) {
        return "Asha Dubey".equals(owner) ? "Asha" : "Sachidanand Dubey".equals(owner) ? "Sachidanand" : owner;
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard(View view) {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }
}
