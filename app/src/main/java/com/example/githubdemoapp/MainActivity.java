package com.example.githubdemoapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
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
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

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
    }

    private void setupActions() {
        loginButton.setOnClickListener(v -> doLogin());
        scanButton.setOnClickListener(v -> launchScanner());
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
        statusText.setText("状态：登录成功，准备扫码");

        loginCard.setVisibility(View.GONE);
        dashboardLayout.setVisibility(View.VISIBLE);
    }

    private void launchScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setPrompt("请将条码放入框内进行扫描");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                statusText.setText("状态：已取消扫码");
                Toast.makeText(this, "已取消扫描", Toast.LENGTH_SHORT).show();
            } else {
                String barcodeValue = result.getContents();
                String barcodeFormat = result.getFormatName();

                barcodeText.setText(barcodeValue);
                barcodeTypeText.setText("类型：" + barcodeFormat);
                statusText.setText("状态：扫码成功，正在上传 Firebase...");

                saveBarcodeToFirebase(barcodeValue, barcodeFormat);
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
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
