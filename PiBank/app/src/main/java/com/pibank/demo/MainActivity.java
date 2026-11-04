package com.pibank.demo;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(9, 9, 20);
    private final int PANEL = Color.rgb(23, 23, 42);
    private final int PURPLE = Color.rgb(139, 92, 246);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(167, 167, 190);
    private FirebaseAuth auth;
    private EditText emailInput, passwordInput;
    private LinearLayout root;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        auth = FirebaseAuth.getInstance();
        showCurrentUser();
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void setup(String title) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 28, 24, 24);
        root.setBackgroundColor(BG);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);
        setContentView(scroll);
        TextView brand = text("PiBank", 30, WHITE, true);
        root.addView(brand);
        TextView sub = text(title, 15, MUTED, false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = 8;
        sp.bottomMargin = 28;
        root.addView(sub, sp);
    }

    private EditText field(String hint, int inputType) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setTextColor(WHITE);
        e.setHintTextColor(MUTED);
        e.setHint(hint);
        e.setInputType(inputType);
        e.setPadding(16, 12, 16, 12);
        e.setBackground(shape(PANEL, 18));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = 14;
        root.addView(e, p);
        return e;
    }

    private void button(String label, Runnable action, boolean primary) {
        TextView b = text(label, 16, WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(16, 15, 16, 15);
        b.setBackground(shape(primary ? PURPLE : PANEL, 18));
        b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = 12;
        root.addView(b, p);
    }

    private void showAuth() {
        setup("Твой цифровой банк");
        TextView note = text("Войди или создай аккаунт PiBank", 14, MUTED, false);
        root.addView(note);
        emailInput = field("Email", 33);
        passwordInput = field("Пароль (минимум 6 символов)", 129);
        button("Войти", () -> signIn(), true);
        button("Создать аккаунт", () -> register(), false);
        TextView demo = text("PiBank — демонстрационный банк. Реальных платежей нет.", 12, MUTED, false);
        demo.setGravity(Gravity.CENTER);
        root.addView(demo);
    }

    private void register() {
        String email = emailInput.getText().toString().trim();
        String pass = passwordInput.getText().toString();
        if (email.isEmpty() || pass.length() < 6) {
            toast("Введите Email и пароль от 6 символов");
            return;
        }
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) createUserRecord(user);
                    toast("Аккаунт PiBank создан!");
                    showDashboard();
                } else toast(firebaseMessage(task.getException()));
            });
    }

    private void signIn() {
        String email = emailInput.getText().toString().trim();
        String pass = passwordInput.getText().toString();
        if (email.isEmpty() || pass.isEmpty()) {
            toast("Введите Email и пароль");
            return;
        }
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    toast("Добро пожаловать в PiBank!");
                    showDashboard();
                } else toast(firebaseMessage(task.getException()));
            });
    }

    private String firebaseMessage(Exception e) {
        if (e == null || e.getMessage() == null) return "Не удалось выполнить запрос";
        String m = e.getMessage();
        if (m.contains("email address is already in use")) return "Этот Email уже зарегистрирован";
        if (m.contains("password is invalid") || m.contains("no user record")) return "Неверный Email или пароль";
        if (m.contains("badly formatted")) return "Проверь формат Email";
        return m;
    }

    private void createUserRecord(FirebaseUser user) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
        Map<String, Object> data = new HashMap<>();
        data.put("email", user.getEmail());
        data.put("balance", 10000);
        data.put("createdAt", System.currentTimeMillis());
        ref.updateChildren(data);
    }

    private void showCurrentUser() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) showAuth(); else showDashboard();
    }

    private void showDashboard() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) { showAuth(); return; }
        setup("Добро пожаловать!");
        TextView greeting = text(user.getEmail() == null ? "Пользователь PiBank" : user.getEmail(), 14, MUTED, false);
        root.addView(greeting);
        LinearLayout balance = new LinearLayout(this);
        balance.setOrientation(LinearLayout.VERTICAL);
        balance.setPadding(20, 22, 20, 22);
        balance.setBackground(shape(PANEL, 22));
        TextView label = text("ДЕМО-БАЛАНС", 12, MUTED, true);
        TextView amount = text("₽ 10 000", 32, WHITE, true);
        balance.addView(label);
        balance.addView(amount);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.topMargin = 22; bp.bottomMargin = 22;
        root.addView(balance, bp);
        section("Мои карты");
        TextView card = text("PiBank •••• 2048\nВиртуальная демонстрационная карта", 17, WHITE, true);
        card.setPadding(20, 22, 20, 22);
        card.setBackground(shape(PURPLE, 22));
        root.addView(card);
        section("Магазин");
        button("Roblox", () -> toast("Раздел Roblox скоро появится"), false);
        button("Steam", () -> toast("Раздел Steam скоро появится"), false);
        button("Minecraft", () -> toast("Раздел Minecraft скоро появится"), false);
        button("ChatGPT", () -> toast("Раздел ChatGPT скоро появится"), false);
        section("Аккаунт");
        button("История операций", () -> toast("История пока пуста"), false);
        button("Выйти из аккаунта", () -> {
            auth.signOut();
            showAuth();
        }, true);
    }

    private void section(String title) {
        TextView t = text(title, 20, WHITE, true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = 12; p.bottomMargin = 12;
        root.addView(t, p);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
