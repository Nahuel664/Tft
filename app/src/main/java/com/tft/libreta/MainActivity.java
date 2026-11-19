package com.tft.libreta;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        l.setPadding(64, 64, 64, 64);
        TextView t = new TextView(this);
        t.setText("Libreta TFT\n\nActiva la burbuja, abre TFT y toca la burbuja para ver tus notas encima del juego.");
        t.setTextSize(18);
        t.setGravity(Gravity.CENTER);
        Button on = new Button(this);
        on.setText("Activar burbuja");
        on.setOnClickListener(v -> start());
        Button off = new Button(this);
        off.setText("Quitar burbuja");
        off.setOnClickListener(v -> stopService(new Intent(this, OverlayService.class)));
        l.addView(t);
        l.addView(on);
        l.addView(off);
        setContentView(l);
    }

    private void start() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Permite «Mostrar sobre otras apps» y vuelve a pulsar Activar", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
            return;
        }
        startForegroundService(new Intent(this, OverlayService.class));
        Toast.makeText(this, "Burbuja activada. Abre TFT.", Toast.LENGTH_SHORT).show();
    }
}
