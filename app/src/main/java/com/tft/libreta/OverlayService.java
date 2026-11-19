package com.tft.libreta;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OverlayService extends Service {
    private WindowManager wm;
    private TextView bubble;
    private LinearLayout panel;
    private WebView web;
    private WindowManager.LayoutParams bp, pp;
    private boolean open = false;
    private int startX, startY;
    private float touchX, touchY;
    private boolean moved;

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public int onStartCommand(Intent i, int flags, int id) {
        if (i != null && "STOP".equals(i.getAction())) { stopSelf(); return START_NOT_STICKY; }
        startForegroundCompat();
        if (bubble == null) { wm = (WindowManager) getSystemService(WINDOW_SERVICE); buildBubble(); buildPanel(); }
        return START_STICKY;
    }

    private void startForegroundCompat() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("tft", "Libreta TFT", NotificationManager.IMPORTANCE_LOW));
        Intent stop = new Intent(this, OverlayService.class).setAction("STOP");
        PendingIntent pi = PendingIntent.getService(this, 0, stop, PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, "tft")
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setContentTitle("Libreta TFT activa")
                .setContentText("Toca aquí para quitar la burbuja")
                .setContentIntent(pi).build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(1, n);
    }

    private void buildBubble() {
        bubble = new TextView(this);
        bubble.setText("TFT");
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(13);
        bubble.setTypeface(null, Typeface.BOLD);
        bubble.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(0xE60F7F76);
        g.setStroke(dp(2), 0xFFE3B84F);
        bubble.setBackground(g);
        bp = new WindowManager.LayoutParams(dp(52), dp(52),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        bp.gravity = Gravity.TOP | Gravity.START;
        bp.x = 0;
        bp.y = dp(220);
        bubble.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = bp.x; startY = bp.y; touchX = e.getRawX(); touchY = e.getRawY(); moved = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int) (e.getRawX() - touchX), dy = (int) (e.getRawY() - touchY);
                    if (Math.abs(dx) > dp(6) || Math.abs(dy) > dp(6)) moved = true;
                    if (moved) { bp.x = startX + dx; bp.y = startY + dy; wm.updateViewLayout(bubble, bp); }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!moved) toggle();
                    return true;
            }
            return false;
        });
        wm.addView(bubble, bp);
    }

    private void buildPanel() {
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF0E1822);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(2), 0xFFE3B84F);
        panel.setBackground(bg);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("Libreta TFT");
        title.setTextColor(0xFFE3B84F);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(dp(14), 0, 0, 0);
        TextView close = new TextView(this);
        close.setText("✕");
        close.setTextColor(Color.WHITE);
        close.setTextSize(20);
        close.setPadding(dp(18), dp(8), dp(18), dp(8));
        close.setOnClickListener(v -> toggle());
        bar.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(close);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        web.setBackgroundColor(0xFF0E1822);
        web.setWebViewClient(new WebViewClient());
        web.loadUrl("file:///android_asset/libreta.html");

        panel.addView(bar);
        panel.addView(web, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        int w = (int) (getResources().getDisplayMetrics().widthPixels * 0.94f);
        int h = (int) (getResources().getDisplayMetrics().heightPixels * 0.6f);
        pp = new WindowManager.LayoutParams(w, h,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        pp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        pp.y = dp(40);
        pp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
    }

    private void toggle() {
        if (open) wm.removeView(panel); else wm.addView(panel, pp);
        open = !open;
    }

    @Override public void onDestroy() {
        try { if (open) wm.removeView(panel); } catch (Exception ignored) {}
        try { if (bubble != null) wm.removeView(bubble); } catch (Exception ignored) {}
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
