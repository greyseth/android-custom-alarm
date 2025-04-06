package com.example.customalarm;

import static android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Calendar;

import pl.droidsonroids.gif.GifImageView;

public class AlarmScreen extends AppCompatActivity {

    GifImageView gifView;
    ImageView imageView;
    TextView textView;
    Button dismissBtn;

    boolean confirmedUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_alarm_screen);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);

            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            keyguardManager.requestDismissKeyguard(this, null);
        }

        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

        gifView = findViewById(R.id.gifView);
        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        dismissBtn = findViewById(R.id.btn_dismiss);

        dismissBtn.setOnClickListener(v -> {
            textView.setText("Plug in charger to prove you're actually up");
            imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.areyousure));

            dismissBtn.setVisibility(View.GONE);

            confirmedUp = true;
        });

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("CHARGER_CONNECTED");
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (confirmedUp) {
                Intent ringtoneStopIntent = new Intent(context, RingtoneService.class);
                stopService(ringtoneStopIntent);

                imageView.setVisibility(View.GONE);
                gifView.setVisibility(View.VISIBLE);

                textView.setText("Now don't go back to bed and get to work");

//                Sets new alarm for the next day
                SharedPreferences sp = getSharedPreferences("alarmTime", MODE_PRIVATE);
                int hour = Integer.parseInt(sp.getString("hour", "5"));
                int minute = Integer.parseInt(sp.getString("minute", "0"));
                if (hour != 0) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(System.currentTimeMillis());
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);
                    calendar.set(Calendar.SECOND, 0);

                    calendar.add(Calendar.DAY_OF_YEAR, 1);

                    AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        Intent alarmIntent = new Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        startActivity(alarmIntent);
                    }

                    RegisterAlarm.register(context, calendar, alarmManager);
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }
}