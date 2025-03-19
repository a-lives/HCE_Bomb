package com.example.nfcbomb;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
import android.os.Bundle;
import android.util.Log;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private boolean isServiceEnabled = false;

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

        FloatingActionButton fab2setting = findViewById(R.id.floatingActionButton2setting);

        fab2setting.setOnClickListener((view)-> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);

        if (adapter == null) {
            Toast.makeText(this, "不支持NFC", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!adapter.isEnabled()) {
            Toast.makeText(this, "NFC未启用", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.i("MainActivity","OnCreated");

        CardEmulation cardEmulation = CardEmulation.getInstance(adapter);
        cardEmulation.setPreferredService(this,HASBomb.COMPONENT);

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch toggleButton = findViewById(R.id.switch1);
        toggleButton.setOnClickListener(view -> {
            if(isServiceEnabled){
                disableComponent(getPackageManager(), HASBomb.COMPONENT);
//                Toast.makeText(MainActivity.this, "Disable NdefService", Toast.LENGTH_SHORT).show();
                toggleButton.setText("已关闭");
            }else {
                enableComponent(getPackageManager(), HASBomb.COMPONENT);
//                Toast.makeText(MainActivity.this, "Enable NdefService", Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onDestroy() {
        disableComponent(getPackageManager(), HASBomb.COMPONENT);
        super.onDestroy();
    }
}