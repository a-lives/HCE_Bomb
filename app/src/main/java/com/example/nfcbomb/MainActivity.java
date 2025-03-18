package com.example.nfcbomb;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private boolean isServiceEnabled = false;
    private NfcAdapter adapter;
    private final List<String> aidList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        adapter = NfcAdapter.getDefaultAdapter(this);

        if (adapter == null) {
            Toast.makeText(this, "手机不支持nfc", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!adapter.isEnabled()) {
            Toast.makeText(this, "设置没开NFC", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch toggleButton = findViewById(R.id.switch1);
        toggleButton.setOnClickListener(view -> {
            if(isServiceEnabled){
                disableComponent(getPackageManager(), HASBomb.COMPONENT);
                Toast.makeText(MainActivity.this, "Disable NdefService", Toast.LENGTH_SHORT).show();
                toggleButton.setText("已关闭");
            }else {
                enableComponent(getPackageManager(), HASBomb.COMPONENT);
                Toast.makeText(MainActivity.this, "Enable NdefService", Toast.LENGTH_SHORT).show();
                toggleButton.setText("已开启");
            }
            isServiceEnabled = !isServiceEnabled;
        });
    }

    public static void enableComponent(PackageManager pm, ComponentName component) {
        pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }
    public static void disableComponent(PackageManager pm, ComponentName component) {
        pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }


}