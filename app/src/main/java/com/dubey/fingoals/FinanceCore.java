package com.dubey.fingoals;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class FinanceCore {
    static final String SCRIPT_URL = "https://script.google.com/macros/s/AKfycbzi3eFSziXdGjvT-agL61hrczanV4mcHjxaTB33eYtGMGflhVCSoRqXEMUuSw9YDnzpqQ/exec";
    static final String APP_PASSWORD = "Dubey@123";
    private static final String PREFS = "fin_goals_native";
    private static final String CACHE_JSON = "cache_json";

    private FinanceCore() {}

    static String fetchJson() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(SCRIPT_URL).openConnection();
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(20000);
        conn.setRequestMethod("GET");
        return read(conn);
    }

    static String postJson(JSONObject payload) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(SCRIPT_URL).openConnection();
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(20000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/plain;charset=utf-8");
        conn.setDoOutput(true);
        try (OutputStream out = conn.getOutputStream()) {
            out.write(payload.toString().getBytes("UTF-8"));
        }
        return read(conn);
    }

    private static String read(HttpURLConnection conn) throws Exception {
        StringBuilder out = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) out.append(line);
        }
        return out.toString();
    }

    static void saveCache(Context context, String json) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CACHE_JSON, json).apply();
    }

    static String loadCache(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(CACHE_JSON, "");
    }

    static Data parse(String json) {
        Data data = new Data();
        try {
            JSONObject root = new JSONObject(json == null ? "{}" : json);
            data.transactions = rows(root.optJSONArray("transactions"));
            data.salary = rows(root.optJSONArray("salary"));
            data.cards = rows(root.optJSONArray("cards"));
            data.bills = rows(root.optJSONArray("bills"));
            data.categories = rows(root.optJSONArray("categories"));
            data.longTerm = rows(root.optJSONArray("longTerm"));
            data.pfNps = rows(root.optJSONArray("pfNps"));
            data.loans = rows(root.optJSONArray("loans"));
        } catch (Exception ignored) {}
        return data;
    }

    private static List<Map<String, String>> rows(JSONArray arr) throws Exception {
        List<Map<String, String>> rows = new ArrayList<>();
        if (arr == null) return rows;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            Map<String, String> row = new HashMap<>();
            JSONArray names = o.names();
            if (names != null) {
                for (int n = 0; n < names.length(); n++) {
                    String key = names.getString(n);
                    row.put(key, o.optString(key, ""));
                }
            }
            rows.add(row);
        }
        return rows;
    }

    static Summary summarize(Data data) {
        Summary s = new Summary();
        String currentMonth = monthYear(new Date());
        Calendar now = Calendar.getInstance();
        String year = String.valueOf(now.get(Calendar.YEAR));

        for (Map<String, String> t : data.transactions) {
            double amount = money(t.get("Amount"));
            String category = val(t, "Category");
            String owner = val(t, "Owner");
            String payMode = val(t, "Pay_Mode");
            String month = monthOf(t);

            if ("Investment".equals(category)) {
                s.totalInvestments += amount;
                if (month.endsWith(year)) s.yearInvestments += amount;
                if (currentMonth.equals(month)) {
                    if ("Sachidanand Dubey".equals(owner)) s.sachinMonthlyInvestment += amount;
                    if ("Asha Dubey".equals(owner)) s.ashaMonthlyInvestment += amount;
                }
            }
            if ("Daily Expenses".equals(category) && currentMonth.equals(month)) s.monthlyExpenses += amount;
            if ("UPI".equals(payMode) && currentMonth.equals(month)) {
                if ("Sachidanand Dubey".equals(owner)) s.sachinUpi += amount;
                if ("Asha Dubey".equals(owner)) s.ashaUpi += amount;
            }
        }

        for (Map<String, String> card : data.cards) s.unbilledCards += cardUnbilled(data.transactions, card);

        s.cardCount = data.cards.size();
        s.recentTransactions = data.transactions;
        return s;
    }

    static double cardUnbilled(List<Map<String, String>> transactions, Map<String, String> card) {
        Calendar today = Calendar.getInstance();
        int billGenDay = integer(card.get("Bill_Generation_Day"));
        int startDay = integer(card.get("Cycle_Start_Day"));
        int endDay = integer(card.get("Cycle_End_Day"));
        String cardId = val(card, "Card_ID").trim();
        if (startDay <= 0 || endDay <= 0 || cardId.length() == 0) return 0;

        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        if (billGenDay > 0 && today.get(Calendar.DAY_OF_MONTH) >= billGenDay) {
            start.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), startDay, 0, 0, 0);
            if (startDay <= endDay) {
                if (start.get(Calendar.DAY_OF_MONTH) < billGenDay) start.add(Calendar.MONTH, 1);
                end.set(start.get(Calendar.YEAR), start.get(Calendar.MONTH), endDay, 23, 59, 59);
            } else {
                end.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, endDay, 23, 59, 59);
            }
        } else {
            if (startDay <= endDay) {
                start.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), startDay, 0, 0, 0);
                end.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), endDay, 23, 59, 59);
                if (today.after(end)) {
                    start.add(Calendar.MONTH, 1);
                    end.add(Calendar.MONTH, 1);
                }
            } else {
                Calendar prevStart = Calendar.getInstance();
                Calendar thisEnd = Calendar.getInstance();
                prevStart.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH) - 1, startDay, 0, 0, 0);
                thisEnd.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), endDay, 23, 59, 59);
                if (!today.before(prevStart) && !today.after(thisEnd)) {
                    start = prevStart;
                    end = thisEnd;
                } else {
                    start.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), startDay, 0, 0, 0);
                    end.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, endDay, 23, 59, 59);
                }
            }
        }

        double total = 0;
        for (Map<String, String> t : transactions) {
            if (!cardId.equals(val(t, "Card_Tag").trim())) continue;
            Date d = parseDate(t.get("Date"));
            if (d != null && !d.before(start.getTime()) && !d.after(end.getTime())) total += money(t.get("Amount"));
        }
        return total;
    }

    static JSONObject appendTransaction(String owner, String date, String category, String subCategory, String amount, String payMode, String cardTag, String notes) throws Exception {
        JSONObject d = new JSONObject();
        d.put("Owner", owner);
        d.put("Date", date);
        d.put("Month_Year", monthYear(parseDate(date)));
        d.put("Category", category);
        d.put("Sub_Category", subCategory);
        d.put("Amount", amount);
        d.put("Pay_Mode", payMode);
        d.put("Card_Tag", "Credit Card".equals(payMode) ? cardTag : "");
        d.put("Notes", notes);
        JSONObject payload = new JSONObject();
        payload.put("action", "append");
        payload.put("sheet", "Transactions");
        payload.put("data", d);
        return payload;
    }

    static JSONObject appendSalary(String owner, String date, String amount) throws Exception {
        JSONObject d = new JSONObject();
        d.put("Sr No", String.valueOf(System.currentTimeMillis()).substring(5));
        d.put("Owner", owner);
        d.put("Date", date);
        d.put("Month-Year", monthYear(parseDate(date)));
        d.put("In_Hand_Salary", amount);
        JSONObject payload = new JSONObject();
        payload.put("action", "append");
        payload.put("sheet", "Salary");
        payload.put("data", d);
        return payload;
    }

    static List<String> categories(Data data) {
        Set<String> set = new HashSet<>();
        for (Map<String, String> r : data.categories) if (val(r, "Category").length() > 0) set.add(val(r, "Category"));
        return new ArrayList<>(set);
    }

    static List<String> cardIds(Data data, String owner) {
        List<String> ids = new ArrayList<>();
        for (Map<String, String> r : data.cards) {
            if (owner == null || owner.length() == 0 || owner.equals(val(r, "Card_Owner"))) ids.add(val(r, "Card_ID"));
        }
        return ids;
    }

    static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    static String monthOf(Map<String, String> row) {
        String month = val(row, "Month_Year");
        if (month.length() == 0) month = val(row, "Month-Year");
        if (month.length() == 0) month = monthYear(parseDate(row.get("Date")));
        return month;
    }

    static String monthYear(Date date) {
        if (date == null) date = new Date();
        return new SimpleDateFormat("MMM-yyyy", Locale.US).format(date);
    }

    static Date parseDate(String raw) {
        if (raw == null || raw.length() == 0) return null;
        try { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw); } catch (Exception ignored) {}
        try { return new Date(raw); } catch (Exception ignored) {}
        return null;
    }

    static String inr(double value) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        nf.setMaximumFractionDigits(0);
        return nf.format(value);
    }

    static String val(Map<String, String> row, String key) {
        String value = row == null ? "" : row.get(key);
        return value == null ? "" : value.trim();
    }

    static double money(String raw) {
        try { return Double.parseDouble((raw == null ? "0" : raw).replace(",", "").trim()); } catch (Exception e) { return 0; }
    }

    static int integer(String raw) {
        try { return Integer.parseInt((raw == null ? "0" : raw).trim()); } catch (Exception e) { return 0; }
    }

    static final class Data {
        List<Map<String, String>> transactions = new ArrayList<>();
        List<Map<String, String>> salary = new ArrayList<>();
        List<Map<String, String>> cards = new ArrayList<>();
        List<Map<String, String>> bills = new ArrayList<>();
        List<Map<String, String>> categories = new ArrayList<>();
        List<Map<String, String>> longTerm = new ArrayList<>();
        List<Map<String, String>> pfNps = new ArrayList<>();
        List<Map<String, String>> loans = new ArrayList<>();
    }

    static final class Summary {
        double totalInvestments;
        double yearInvestments;
        double sachinMonthlyInvestment;
        double ashaMonthlyInvestment;
        double monthlyExpenses;
        double sachinUpi;
        double ashaUpi;
        double unbilledCards;
        int cardCount;
        List<Map<String, String>> recentTransactions = new ArrayList<>();
    }
}
