package jp.co.monst.switchpanel;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

public class OverlayService extends Service {

    private static final String CHANNEL_ID = "monst_switch";
    private static final int NOTIF_ID = 201;
    private WindowManager wm;
    private View miniBtn;
    private WebView panel;
    private WindowManager.LayoutParams miniParams, panelParams;
    private boolean panelShown = false;
    private boolean hidden = false;

    private final Map<String, Boolean> state = new LinkedHashMap<>();
    // v46: 実績ある4機能に戻した安定版（全てON）
    private static final String[][] FEATS = {
            {"onepunch", "👊", "ワンパン(×50)", "true"},
            {"invin", "🛡", "無敵", "true"},
            {"ss", "⚡", "SS無限", "true"},
            {"autoclear", "🤖", "自動クリア", "false"},
    };

    @Override
    public void onCreate() {
        super.onCreate();
        for (String[] f : FEATS) state.put(f[0], Boolean.parseBoolean(f[3]));
        startFg();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        createMiniButton();
        createPanel();
    }

    @Override public int onStartCommand(Intent i, int f, int id) { return START_STICKY; }
    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public void onDestroy() {
        try { if (miniBtn != null) wm.removeView(miniBtn); } catch (Exception ignored) {}
        try { if (panel != null) wm.removeView(panel); } catch (Exception ignored) {}
        super.onDestroy();
    }

    private void startFg() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "MonstSwitch", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
        Notification n = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("MonstSwitch 稼働中")
                .setContentText("モンスト画面に ⚙ ボタンを表示")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setOngoing(true).build();
        startForeground(NOTIF_ID, n);
    }

    private int windowType() {
        return Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private int dp(int d) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, d, getResources().getDisplayMetrics());
    }

    private void createMiniButton() {
        TextView btn = new TextView(this);
        btn.setText("⚙"); // ⚙
        btn.setTextSize(22);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor("#AA6c5ce7"));
        btn.setPadding(dp(14), dp(10), dp(14), dp(10));
        btn.setGravity(Gravity.CENTER);
        miniBtn = btn;

        miniParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                windowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        miniParams.gravity = Gravity.TOP | Gravity.START;
        miniParams.x = dp(8);
        miniParams.y = dp(100);

        final float[] touch = new float[4];
        final boolean[] moved = {false};

        btn.setOnTouchListener((v, ev) -> {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch[0] = ev.getRawX(); touch[1] = ev.getRawY();
                    touch[2] = miniParams.x; touch[3] = miniParams.y;
                    moved[0] = false;
                    return false;
                case MotionEvent.ACTION_MOVE:
                    float dx = ev.getRawX() - touch[0];
                    float dy = ev.getRawY() - touch[1];
                    if (Math.abs(dx) > dp(5) || Math.abs(dy) > dp(5)) {
                        miniParams.x = (int) (touch[2] + dx);
                        miniParams.y = (int) (touch[3] + dy);
                        try { wm.updateViewLayout(miniBtn, miniParams); } catch (Exception ignored) {}
                        moved[0] = true;
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                    if (!moved[0]) togglePanel();
                    return true;
            }
            return false;
        });

        try { wm.addView(miniBtn, miniParams); } catch (Exception ignored) {}
    }

    private void createPanel() {
        panel = new WebView(this);
        WebSettings ws = panel.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        panel.setBackgroundColor(Color.TRANSPARENT);
        panel.addJavascriptInterface(new JsBridge(), "MSB");
        panel.loadDataWithBaseURL(null, buildHtml(), "text/html", "UTF-8", null);

        panelParams = new WindowManager.LayoutParams(
                dp(300), dp(640),
                windowType(),
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.TOP | Gravity.START;
        panelParams.x = dp(16);
        panelParams.y = dp(80);

        panel.setVisibility(View.GONE);
        try { wm.addView(panel, panelParams); } catch (Exception ignored) {}
    }

    private void togglePanel() {
        if (panelShown) {
            panel.setVisibility(View.GONE);
            panelShown = false;
            miniBtn.setVisibility(View.VISIBLE);
        } else {
            panel.setVisibility(View.VISIBLE);
            panelShown = true;
            miniBtn.setVisibility(View.GONE);
        }
    }

    private void hideAll() {
        hidden = true;
        panel.setVisibility(View.GONE);
        miniBtn.setVisibility(View.GONE);
        panelShown = false;
    }

    private String buildHtml() {
        StringBuilder rows = new StringBuilder();
        for (String[] f : FEATS) {
            String checked = Boolean.TRUE.equals(state.get(f[0])) ? " checked" : "";
            rows.append("<div class='r'><span class='i'>").append(f[1]).append("</span>")
                    .append("<span class='l'>").append(f[2]).append("</span>")
                    .append("<label class='t'><input type='checkbox' data-k='").append(f[0]).append("'")
                    .append(checked).append(" onchange='tog(this)'><span class='s'></span></label></div>");
        }
        return "<!DOCTYPE html><html><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1,user-scalable=no'>"
                + "<style>"
                + "*{margin:0;padding:0;box-sizing:border-box;-webkit-tap-highlight-color:transparent}"
                + "body{background:transparent;font-family:sans-serif;color:#e0e0e8;overflow:hidden}"
                + ".w{background:rgba(10,10,18,0.96);border-radius:12px;border:1px solid rgba(108,92,231,0.4);overflow:hidden}"
                + ".h{display:flex;align-items:center;justify-content:space-between;padding:8px 12px;background:rgba(20,20,35,0.98)}"
                + ".h h1{font-size:13px;font-weight:700;letter-spacing:1px}"
                + ".h h1 b{color:#6c5ce7}"
                + ".hbtns{display:flex;gap:6px}"
                + ".hbtn{width:26px;height:26px;border-radius:50%;color:#fff;font-size:13px;font-weight:700;text-align:center;line-height:26px;border:none;cursor:pointer}"
                + ".hide{background:#888}"
                + ".cls{background:#ff4757}"
                + ".b{max-height:520px;overflow-y:auto;-webkit-overflow-scrolling:touch}"
                + ".b::-webkit-scrollbar{width:2px}.b::-webkit-scrollbar-thumb{background:#6c5ce7}"
                + ".r{display:flex;align-items:center;padding:8px 12px;border-bottom:1px solid rgba(255,255,255,0.04)}"
                + ".i{font-size:16px;width:24px;text-align:center;margin-right:8px;flex-shrink:0}"
                + ".l{font-size:11px;font-weight:500;flex:1}"
                + ".t{position:relative;width:40px;height:22px;flex-shrink:0}"
                + ".t input{opacity:0;width:0;height:0;position:absolute}"
                + ".t .s{position:absolute;inset:0;background:#444;border-radius:11px;transition:.2s}"
                + ".t .s::before{content:\"\";position:absolute;width:16px;height:16px;border-radius:50%;left:3px;bottom:3px;background:#fff;transition:.2s}"
                + ".t input:checked+.s{background:#00e676}"
                + ".t input:checked+.s::before{transform:translateX(18px)}"
                + ".bar{display:flex;gap:4px;padding:6px 8px;background:rgba(20,20,35,0.98)}"
                + ".btn{flex:1;padding:8px 0;border:none;border-radius:6px;font-size:10px;font-weight:600;cursor:pointer}"
                + ".b1{background:#6c5ce7;color:#fff}.b2{background:#ff4757;color:#fff}"
                + "</style></head><body><div class='w'>"
                + "<div class='h'><h1>Monst<b>Switch</b> v46</h1>"
                + "<div class='hbtns'>"
                + "<button class='hbtn hide' onclick='MSB.hide()' title='完全に隠す'>👁</button>"
                + "<button class='hbtn cls' onclick='MSB.close()' title='⚙に戻す'>−</button>"
                + "</div></div>"
                + "<div class='b'>" + rows.toString() + "</div>"
                + "<div class='bar'><button class='btn b1' onclick='allOn()'>全ON</button>"
                + "<button class='btn b2' onclick='allOff()'>全OFF</button></div>"
                + "</div><script>"
                + "function tog(e){MSB.setState(e.dataset.k,e.checked?'1':'0')}"
                + "function allOn(){document.querySelectorAll('input').forEach(function(e){e.checked=true;MSB.setState(e.dataset.k,'1')})}"
                + "function allOff(){document.querySelectorAll('input').forEach(function(e){e.checked=false;MSB.setState(e.dataset.k,'0')})}"
                + "</script></body></html>";
    }

    private void sendStateToHook() {
        new Thread(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Boolean> e : state.entrySet()) {
                    if (sb.length() > 0) sb.append(";");
                    sb.append(e.getKey()).append("=").append(e.getValue() ? "1" : "0");
                }
                Socket s = new Socket("127.0.0.1", 28900);
                s.setSoTimeout(1500);
                OutputStream os = s.getOutputStream();
                os.write(sb.toString().getBytes("UTF-8"));
                os.flush();
                s.close();
            } catch (Exception ignored) {}
        }).start();
    }

    private class JsBridge {
        @JavascriptInterface
        public void setState(String k, String v) {
            state.put(k, "1".equals(v));
            sendStateToHook();
        }
        @JavascriptInterface
        public void close() { panel.post(OverlayService.this::togglePanel); }
        @JavascriptInterface
        public void hide() { panel.post(OverlayService.this::hideAll); }
    }
}
