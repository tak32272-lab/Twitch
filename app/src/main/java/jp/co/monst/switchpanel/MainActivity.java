package jp.co.monst.switchpanel;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView status = findViewById(R.id.status);
        Button startBtn = findViewById(R.id.start_btn);
        Button stopBtn = findViewById(R.id.stop_btn);

        startBtn.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "オーバーレイ権限を許可してください", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return;
            }
            startService(new Intent(this, OverlayService.class));
            status.setText("起動中 — モンストに切り替えて⚙ボタンを操作");
            Toast.makeText(this, "Switch起動！モンストに戻ってください", Toast.LENGTH_SHORT).show();
        });

        stopBtn.setOnClickListener(v -> {
            stopService(new Intent(this, OverlayService.class));
            status.setText("停止中");
        });
    }
}
