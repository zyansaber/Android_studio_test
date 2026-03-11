package com.example.githubdemoapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String ADMIN_PASSWORD = "admin";
    private static final String PREFS_NAME = "login_prefs";
    private static final String KEY_REMEMBER = "remember_me";
    private static final String KEY_USERNAME = "saved_username";
    private static final String KEY_PASSWORD = "saved_password";

    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private MaterialButton scanButton;
    private CheckBox rememberMeCheckBox;
    private LinearLayout dashboardLayout;
    private View loginCard;

    private TextView welcomeText;
    private TextView statusText;
    private TextView selectedSectionText;
    private TextView barcodeText;
    private TextView barcodeTypeText;
    private EditText laserInput;
    private View predeliveryWarrantyFormLayout;
    private TextInputEditText predeliveryDetailInput;
    private MaterialButton predeliveryTakePhotoButton;

    private String currentUser = "";
    private String selectedSection = "";
    private String lastScannedBarcode = "";
    private DatabaseReference scansRef;
    private DatabaseReference predeliveryWarrantyRef;
    private StorageReference predeliveryStorageRef;

    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupResultLaunchers();
        bindViews();
        initFirebaseRealtimeDb();
        setupActions();
        loadRememberedCredentials();
    }

    private void setupResultLaunchers() {
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        statusText.setText("Status: Camera cancelled");
                        return;
                    }

                    Bundle extras = result.getData().getExtras();
                    if (extras == null || !(extras.get("data") instanceof Bitmap)) {
                        statusText.setText("Status: No photo captured");
                        return;
                    }

                    Bitmap bitmap = (Bitmap) extras.get("data");
                    uploadPredeliveryWarrantyPhoto(bitmap);
                }
        );
    }

    private void bindViews() {
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        scanButton = findViewById(R.id.scanButton);
        rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox);
        dashboardLayout = findViewById(R.id.dashboardLayout);
        loginCard = findViewById(R.id.loginCard);
        welcomeText = findViewById(R.id.welcomeText);
        statusText = findViewById(R.id.statusText);
        selectedSectionText = findViewById(R.id.selectedSectionText);
        barcodeText = findViewById(R.id.barcodeText);
        barcodeTypeText = findViewById(R.id.barcodeTypeText);
        laserInput = findViewById(R.id.laserInput);
        predeliveryWarrantyFormLayout = findViewById(R.id.predeliveryWarrantyFormLayout);
        predeliveryDetailInput = findViewById(R.id.predeliveryDetailInput);
        predeliveryTakePhotoButton = findViewById(R.id.predeliveryTakePhotoButton);

        bindSectionCard(R.id.cardPartsReceiving, "Parts Receiving");
        bindSectionCard(R.id.cardSemivanReceiving, "Semivan Receiving");
        bindSectionCard(R.id.cardHoist, "Hoist");
        bindSectionCard(R.id.cardRoofLine, "Roof Line");
        bindSectionCard(R.id.cardAssembleLine, "Assemble Line");
        bindSectionCard(R.id.cardQualityCenter, "Quality Center");
        bindSectionCard(R.id.cardDispatching, "Dispatching Area");
        bindSectionCard(R.id.cardGoodsIssuing, "Goods Issuing");
        bindSectionCard(R.id.cardStocktaking, "Stocktaking");

        MaterialCardView predeliveryCard = findViewById(R.id.cardPredeliveryWarranty);
        predeliveryCard.setOnClickListener(v -> {
            selectedSection = "Predelivery Warranty";
            selectedSectionText.setText("Current Area: " + selectedSection);
            statusText.setText("Status: Fill detail and take photo for scanned barcode.");
            predeliveryWarrantyFormLayout.setVisibility(View.VISIBLE);
            armLaserScannerInput();
        });
    }

    private void bindSectionCard(int cardId, String sectionName) {
        MaterialCardView card = findViewById(cardId);
        card.setOnClickListener(v -> {
            selectedSection = sectionName;
            selectedSectionText.setText("Current Area: " + selectedSection);
            statusText.setText("Status: " + selectedSection + " selected. Ready to scan.");
            predeliveryWarrantyFormLayout.setVisibility(View.GONE);
            Toast.makeText(this, sectionName + " selected", Toast.LENGTH_SHORT).show();
            armLaserScannerInput();
        });
    }

    private void setupActions() {
        loginButton.setOnClickListener(v -> doLogin());
        scanButton.setOnClickListener(v -> armLaserScannerInput());
        predeliveryTakePhotoButton.setOnClickListener(v -> validateAndStartCamera());

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

    private void validateAndStartCamera() {
        if (!"Predelivery Warranty".equals(selectedSection)) {
            Toast.makeText(this, "Please select Predelivery Warranty first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(lastScannedBarcode)) {
            Toast.makeText(this, "Please scan barcode first", Toast.LENGTH_SHORT).show();
            armLaserScannerInput();
            return;
        }

        if (TextUtils.isEmpty(getInputText(predeliveryDetailInput))) {
            Toast.makeText(this, "Please fill detail before taking photo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(cameraIntent);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void doLogin() {
        String username = getInputText(usernameInput);
        String password = getInputText(passwordInput);

        if (!ADMIN_PASSWORD.equals(password)) {
            Toast.makeText(this, "Invalid password. Use: admin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(username)) {
            username = "operator_" + System.currentTimeMillis() % 10000;
        }

        saveLoginPreference(username, password);

        currentUser = username;
        welcomeText.setText("Welcome, " + currentUser);
        statusText.setText("Status: Login successful. Select a work area.");

        loginCard.setVisibility(View.GONE);
        dashboardLayout.setVisibility(View.VISIBLE);
    }

    private void saveLoginPreference(String username, String password) {
        boolean shouldRemember = rememberMeCheckBox.isChecked();
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_REMEMBER, shouldRemember)
                .putString(KEY_USERNAME, shouldRemember ? username : "")
                .putString(KEY_PASSWORD, shouldRemember ? password : "")
                .apply();
    }

    private void loadRememberedCredentials() {
        boolean remember = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_REMEMBER, false);
        rememberMeCheckBox.setChecked(remember);
        if (!remember) {
            return;
        }

        String savedUsername = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_USERNAME, "");
        String savedPassword = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_PASSWORD, "");

        usernameInput.setText(savedUsername);
        passwordInput.setText(savedPassword);
    }

    private void armLaserScannerInput() {
        if (TextUtils.isEmpty(selectedSection)) {
            statusText.setText("Status: Select a work area before scanning.");
            Toast.makeText(this, "Please choose a work area first", Toast.LENGTH_SHORT).show();
            return;
        }

        statusText.setText("Status: Laser scanner armed for " + selectedSection + ". Press scanner key.");
        laserInput.setText("");
        laserInput.requestFocus();
    }

    private void handleLaserResult(String barcodeValue) {
        if (TextUtils.isEmpty(barcodeValue)) {
            return;
        }

        lastScannedBarcode = barcodeValue;
        barcodeText.setText(barcodeValue);
        barcodeTypeText.setText("Type: Laser/Wedge");
        statusText.setText("Status: Scan captured. Uploading to Firebase...");
        laserInput.setText("");

        saveBarcodeToFirebase(barcodeValue, "LASER_WEDGE");
    }

    private void saveBarcodeToFirebase(String barcodeValue, String barcodeFormat) {
        if (scansRef == null) {
            statusText.setText("Status: Firebase initialization failed");
            Toast.makeText(this, "Firebase initialization failed", Toast.LENGTH_SHORT).show();
            return;
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> payload = new HashMap<>();
        payload.put("username", currentUser);
        payload.put("workArea", selectedSection);
        payload.put("barcode", barcodeValue);
        payload.put("format", barcodeFormat);
        payload.put("timestamp", timestamp);

        scansRef.push().setValue(payload)
                .addOnSuccessListener(unused -> {
                    statusText.setText("Status: Uploaded successfully ✅");
                    Toast.makeText(this, "Uploaded", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    statusText.setText("Status: Upload failed - " + e.getMessage());
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void uploadPredeliveryWarrantyPhoto(Bitmap bitmap) {
        if (predeliveryStorageRef == null || predeliveryWarrantyRef == null) {
            statusText.setText("Status: Firebase storage not initialized");
            Toast.makeText(this, "Firebase storage not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        String detail = getInputText(predeliveryDetailInput);
        String safeBarcode = lastScannedBarcode.replaceAll("[^a-zA-Z0-9_-]", "_");
        String fileName = "PW_" + System.currentTimeMillis() + ".jpg";
        String storagePath = "predelivery_warranty/" + safeBarcode + "/" + fileName;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] imageBytes = baos.toByteArray();

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("barcode", lastScannedBarcode)
                .setCustomMetadata("detail", detail)
                .setCustomMetadata("username", currentUser)
                .build();

        statusText.setText("Status: Uploading Predelivery Warranty photo...");

        StorageReference photoRef = predeliveryStorageRef.child(storagePath);
        photoRef.putBytes(imageBytes, metadata)
                .addOnSuccessListener(taskSnapshot -> photoRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> savePredeliveryRecord(detail, storagePath, uri.toString()))
                        .addOnFailureListener(e -> savePredeliveryRecord(detail, storagePath, "")))
                .addOnFailureListener(e -> {
                    statusText.setText("Status: Predelivery upload failed - " + e.getMessage());
                    Toast.makeText(this, "Photo upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void savePredeliveryRecord(String detail, String storagePath, String downloadUrl) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> payload = new HashMap<>();
        payload.put("username", currentUser);
        payload.put("workArea", selectedSection);
        payload.put("barcode", lastScannedBarcode);
        payload.put("detail", detail);
        payload.put("photoPath", "gs://yardstock.firebasestorage.app/" + storagePath);
        payload.put("downloadUrl", downloadUrl);
        payload.put("timestamp", timestamp);

        predeliveryWarrantyRef.push().setValue(payload)
                .addOnSuccessListener(unused -> {
                    statusText.setText("Status: Predelivery Warranty uploaded ✅");
                    Toast.makeText(this, "Predelivery warranty saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    statusText.setText("Status: Record save failed - " + e.getMessage());
                    Toast.makeText(this, "Record save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
            predeliveryWarrantyRef = database.getReference("predelivery_warranty_records");
            predeliveryStorageRef = FirebaseStorage.getInstance().getReference();
        } catch (Exception e) {
            Toast.makeText(this, "Firebase init exception: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getInputText(TextInputEditText inputEditText) {
        if (inputEditText.getText() == null) {
            return "";
        }
        return inputEditText.getText().toString().trim();
    }
}
