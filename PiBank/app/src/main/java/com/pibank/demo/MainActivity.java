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
    private String activeTab = "Главная";
    private boolean showCardBack = false;
    private final java.security.SecureRandom secureRandom = new java.security.SecureRandom();

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
        setup("Ваши финансы под контролем");
        root.addView(text(user.getEmail() == null ? "Пользователь PiBank" : user.getEmail(), 13, MUTED, false));
        LinearLayout balance = new LinearLayout(this);
        balance.setOrientation(LinearLayout.VERTICAL); balance.setPadding(20, 20, 20, 20);
        balance.setBackground(shape(PANEL, 22));
        balance.addView(text("ОБЩИЙ БАЛАНС", 12, MUTED, true));
        TextView amount = text("Загрузка…", 30, WHITE, true); balance.addView(amount);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2); bp.topMargin = 18; bp.bottomMargin = 18; root.addView(balance, bp);
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
        userRef.child("cards").addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            public void onDataChange(com.google.firebase.database.DataSnapshot snap) {
                double total = 0; for (com.google.firebase.database.DataSnapshot c : snap.getChildren()) {
                    Double v = c.child("balance").getValue(Double.class); if (v != null) total += v;
                }
                amount.setText(String.format(java.util.Locale.getDefault(), "₽ %,.2f", total));
            }
            public void onCancelled(com.google.firebase.database.DatabaseError e) { amount.setText("—"); }
        });
        addNavigation();
        if (activeTab.equals("Главная")) showCards(user);
        else if (activeTab.equals("Крипта")) { section("Крипта"); root.addView(text("Ваш криптопортфель", 14, MUTED, false)); }
        else if (activeTab.equals("Новости")) showNews();
        else showHistory(user);
        section("Аккаунт");
        button("Выйти из аккаунта", () -> { auth.signOut(); showAuth(); }, false);
    }

    private void addNavigation() {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        String[] tabs = {"Главная", "Крипта", "Новости", "История"};
        for (String tab : tabs) {
            TextView b = text(tab, 12, WHITE, true); b.setGravity(Gravity.CENTER); b.setPadding(5, 13, 5, 13);
            b.setBackground(shape(tab.equals(activeTab) ? PURPLE : PANEL, 14));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1); lp.setMargins(3, 4, 3, 8); row.addView(b, lp);
            b.setOnClickListener(v -> { activeTab = tab; showDashboard(); });
        }
        root.addView(row);
    }

    private void showCards(FirebaseUser user) {
        section("Мои карты");
        DatabaseReference cards = FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("cards");
        cards.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            public void onDataChange(com.google.firebase.database.DataSnapshot snap) {
                for (com.google.firebase.database.DataSnapshot c : snap.getChildren()) drawCard(c);
                if (snap.getChildrenCount() < 3) button("＋ Добавить карту", () -> chooseCard(user), true);
                else button("Доступно не более трёх карт", () -> toast("Лимит карт достигнут"), false);
            }
            public void onCancelled(com.google.firebase.database.DatabaseError e) { toast("Не удалось загрузить карты"); }
        });
    }

    private void drawCard(com.google.firebase.database.DataSnapshot c) {
        String number = c.child("number").getValue(String.class);
        String type = c.child("type").getValue(String.class);
        String status = c.child("status").getValue(String.class);
        Double bal = c.child("balance").getValue(Double.class);
        FirebaseUser current = auth.getCurrentUser();
        String owner = current == null || current.getEmail() == null ? "ВЛАДЕЛЕЦ КАРТЫ" : current.getEmail().toUpperCase(java.util.Locale.ROOT);
        String fullNumber = number == null ? "•••• •••• •••• ••••" : number.replaceAll("(.{4})(?!$)", "$1 ").trim();
        String tail = number == null ? "••••" : number.substring(Math.max(0, number.length() - 4));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(22, 20, 22, 20);
        GradientDrawable face = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                showCardBack ? new int[]{Color.rgb(30, 25, 52), Color.rgb(16, 17, 34)}
                             : new int[]{Color.rgb(104, 75, 190), Color.rgb(35, 23, 72), Color.rgb(12, 13, 29)});
        face.setCornerRadius(26);
        face.setStroke(1, Color.rgb(120, 95, 210));
        card.setBackground(face);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("✓ vida", 22, WHITE, true);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(text(showCardBack ? "ИНФОРМАЦИЯ" : "VIRTUAL", 10, Color.rgb(220, 210, 255), true));
        card.addView(top);

        if (!showCardBack) {
            TextView balanceLabel = text("БАЛАНС", 10, Color.rgb(220, 210, 255), true);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2); blp.topMargin = 18;
            card.addView(balanceLabel, blp);
            card.addView(text("₽ " + String.format(java.util.Locale.getDefault(), "%,.2f", bal == null ? 0 : bal), 27, WHITE, true));
            TextView digits = text(fullNumber, 17, WHITE, true);
            LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2); np.topMargin = 18;
            card.addView(digits, np);
            LinearLayout bottom = new LinearLayout(this); bottom.setGravity(Gravity.CENTER_VERTICAL);
            TextView holder = text(owner, 10, Color.rgb(225, 220, 245), true);
            bottom.addView(holder, new LinearLayout.LayoutParams(0, -2, 1));
            bottom.addView(text("vida", 21, WHITE, true));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2); bp.topMargin = 14;
            card.addView(bottom, bp);
            card.addView(text(type == null ? "Карта" : type, 10, Color.rgb(220, 210, 255), false));
        } else {
            TextView stripe = text("", 1, WHITE, false);
            stripe.setBackgroundColor(Color.rgb(8, 8, 15));
            LinearLayout.LayoutParams stripeParams = new LinearLayout.LayoutParams(-1, 28); stripeParams.topMargin = 18; stripeParams.bottomMargin = 18;
            card.addView(stripe, stripeParams);
            card.addView(text("ВЛАДЕЛЕЦ", 10, MUTED, true));
            card.addView(text(owner, 14, WHITE, true));
            card.addView(text("НОМЕР КАРТЫ", 10, MUTED, true));
            card.addView(text("•••• •••• •••• " + tail, 16, WHITE, true));
            card.addView(text("СТАТУС: " + (status == null ? "Активна" : status), 12, Color.rgb(210, 195, 255), true));
        }
        card.setOnClickListener(v -> { showCardBack = !showCardBack; showDashboard(); });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2); cp.bottomMargin = 12; root.addView(card, cp);
        root.addView(text("Нажми на карту, чтобы перевернуть", 11, MUTED, false));
    }

    private void chooseCard(FirebaseUser user) {
        String[] types = {"Дебетовая", "Кредитная", "Детская"};
        new android.app.AlertDialog.Builder(this).setTitle("Выберите тип карты").setItems(types, (d, which) -> createCard(user, types[which])).show();
    }

    private void createCard(FirebaseUser user, String type) {
        DatabaseReference cards = FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("cards");
        cards.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            public void onDataChange(com.google.firebase.database.DataSnapshot snap) {
                if (snap.getChildrenCount() >= 3) { toast("Можно создать не более трёх карт"); return; }
                reserveCardNumber(user, type, 0);
            }
            public void onCancelled(com.google.firebase.database.DatabaseError e) { toast("Ошибка загрузки карт"); }
        });
    }

    private void reserveCardNumber(FirebaseUser user, String type, int attempt) {
        if (attempt > 12) { toast("Не удалось создать номер. Попробуйте ещё раз"); return; }
        StringBuilder sb = new StringBuilder(); for (int i = 0; i < 16; i++) sb.append(secureRandom.nextInt(10));
        String number = sb.toString();
        DatabaseReference unique = FirebaseDatabase.getInstance().getReference("cardNumbers").child(number);
        unique.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            public com.google.firebase.database.Transaction.Result doTransaction(com.google.firebase.database.MutableData data) {
                if (data.getValue() != null) return com.google.firebase.database.Transaction.abort();
                data.setValue(user.getUid()); return com.google.firebase.database.Transaction.success(data);
            }
            public void onComplete(com.google.firebase.database.DatabaseError error, boolean committed, com.google.firebase.database.DataSnapshot snap) {
                if (error != null) { toast("Ошибка создания карты"); return; }
                if (!committed) { reserveCardNumber(user, type, attempt + 1); return; }
                DatabaseReference cards = FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("cards");
                String key = cards.push().getKey(); Map<String,Object> data = new HashMap<>();
                data.put("number", number); data.put("type", type); data.put("balance", 0); data.put("status", "Активна");
                data.put("createdAt", System.currentTimeMillis()); data.put("ownerUid", user.getUid());
                cards.child(key).setValue(data).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String,Object> event = new HashMap<>(); event.put("type", "Создание карты"); event.put("cardNumber", number); event.put("timestamp", System.currentTimeMillis());
                        FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("history").push().setValue(event);
                        toast("Карта создана"); showDashboard();
                    } else toast("Не удалось сохранить карту");
                });
            }
        });
    }

    private void showNews() {
        section("Новости PiBank");
        FirebaseDatabase.getInstance().getReference("news").limitToLast(30).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            public void onDataChange(com.google.firebase.database.DataSnapshot snap) {
                for (com.google.firebase.database.DataSnapshot n : snap.getChildren()) {
                    String title = n.child("title").getValue(String.class), desc = n.child("description").getValue(String.class);
                    LinearLayout item = new LinearLayout(MainActivity.this); item.setOrientation(LinearLayout.VERTICAL); item.setPadding(15,14,15,14); item.setBackground(shape(PANEL,16));
                    item.addView(text(title == null ? "Новость" : title, 16, WHITE, true)); item.addView(text(desc == null ? "" : desc, 13, MUTED, false));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=10; root.addView(item,lp);
                }
                if (!snap.exists()) root.addView(text("Новых новостей пока нет", 13, MUTED, false));
            }
            public void onCancelled(com.google.firebase.database.DatabaseError e) { toast("Не удалось загрузить новости"); }
        });
    }

    private void showHistory(FirebaseUser user) {
        section("История операций");
        FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("history").limitToLast(50).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            public void onDataChange(com.google.firebase.database.DataSnapshot snap) {
                for (com.google.firebase.database.DataSnapshot h : snap.getChildren()) {
                    String type=h.child("type").getValue(String.class), num=h.child("cardNumber").getValue(String.class);
                    TextView item=text((type==null?"Операция":type)+(num==null?"":" • "+num),14,WHITE,false); item.setPadding(12,13,12,13); item.setBackground(shape(PANEL,14));
                    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=7; root.addView(item,lp);
                }
                if (!snap.exists()) root.addView(text("Операций пока нет",13,MUTED,false));
            }
            public void onCancelled(com.google.firebase.database.DatabaseError e) { toast("Не удалось загрузить историю"); }
        });
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
