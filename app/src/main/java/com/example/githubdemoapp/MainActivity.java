package com.example.githubdemoapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String ADMIN_PASSWORD = "admin";

    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private MaterialButton scanButton;
    private LinearLayout dashboardLayout;
    private View loginCard;

    private TextView welcomeText;
    private TextView statusText;
    private TextView barcodeText;
    private TextView barcodeTypeText;
    private EditText laserInput;

    private String currentUser = "";
    private DatabaseReference scansRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        initFirebaseRealtimeDb();
        setupActions();
    }

    private void bindViews() {
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        scanButton = findViewById(R.id.scanButton);
        dashboardLayout = findViewById(R.id.dashboardLayout);
        loginCard = findViewById(R.id.loginCard);
        welcomeText = findViewById(R.id.welcomeText);
        statusText = findViewById(R.id.statusText);
        barcodeText = findViewById(R.id.barcodeText);
        barcodeTypeText = findViewById(R.id.barcodeTypeText);
        laserInput = findViewById(R.id.laserInput);
    }

    private void setupActions() {
        loginButton.setOnClickListener(v -> doLogin());
        scanButton.setOnClickListener(v -> armLaserScannerInput());

        laserInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                handleLaserResult(laserInput.getText().toString().trim());
                return true;
            }
            return false;
        });

        laserInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String value = editable.toString();
                if (value.endsWith("\n") || value.endsWith("\r")) {
                    handleLaserResult(value.trim());
                }
            }
        });
    }

    private void doLogin() {
        String username = getInputText(usernameInput);
        String password = getInputText(passwordInput);

        if (!ADMIN_PASSWORD.equals(password)) {
            Toast.makeText(this, "密码错误，请输入 admin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(username)) {
            username = "operator_" + System.currentTimeMillis() % 10000;
        }

        currentUser = username;
        welcomeText.setText("欢迎，" + currentUser);
        statusText.setText("状态：登录成功，请点击“准备激光扫码”");

        loginCard.setVisibility(View.GONE);
        dashboardLayout.setVisibility(View.VISIBLE);
        armLaserScannerInput();
    }

    private void armLaserScannerInput() {
        statusText.setText("状态：激光扫码已就绪，请按手持机扫描键");
        laserInput.setText("");
        laserInput.requestFocus();
        Toast.makeText(this, "请按设备侧键触发激光头扫码", Toast.LENGTH_SHORT).show();
    }

    private void handleLaserResult(String barcodeValue) {
        if (TextUtils.isEmpty(barcodeValue)) {
            return;
        }

        barcodeText.setText(barcodeValue);
        barcodeTypeText.setText("类型：Laser/Wedge");
        statusText.setText("状态：扫码成功，正在上传 Firebase...");
        laserInput.setText("");

        saveBarcodeToFirebase(barcodeValue, "LASER_WEDGE");
    }

    private void saveBarcodeToFirebase(String barcodeValue, String barcodeFormat) {
        if (scansRef == null) {
            statusText.setText("状态：Firebase 未初始化");
            Toast.makeText(this, "Firebase 初始化失败", Toast.LENGTH_SHORT).show();
            return;
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> payload = new HashMap<>();
        payload.put("username", currentUser);
        payload.put("barcode", barcodeValue);
        payload.put("format", barcodeFormat);
        payload.put("timestamp", timestamp);

        scansRef.push().setValue(payload)
                .addOnSuccessListener(unused -> {
                    statusText.setText("状态：已上传 Firebase ✅");
                    Toast.makeText(this, "上传成功", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    statusText.setText("状态：上传失败 - " + e.getMessage());
                    Toast.makeText(this, "上传失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void initFirebaseRealtimeDb() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setApiKey("AIzaSyANdJ-HWSA7KMihF8DbGYLTAsy-ZcjsCR8")
                        .setApplicationId("1:585682067847:android:232cb0bc87905a0f0ef322")
                        .setProjectId("yardstock")
                        .setDatabaseUrl("https://yardstock-default-rtdb.asia-southeast1.firebasedatabase.app")
                        .setStorageBucket("yardstock.firebasestorage.app")
                        .build();
                FirebaseApp.initializeApp(this, options);
            }

            FirebaseDatabase database = FirebaseDatabase.getInstance("https://yardstock-default-rtdb.asia-southeast1.firebasedatabase.app");
            scansRef = database.getReference("receiving_scans");
        } catch (Exception e) {
            Toast.makeText(this, "Firebase 初始化异常：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getInputText(TextInputEditText inputEditText) {
        if (inputEditText.getText() == null) {
            return "";
        }
        return inputEditText.getText().toString().trim();
    }
}
