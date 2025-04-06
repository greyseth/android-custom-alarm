package com.example.customalarm;

import static android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    EditText input_hour;
    EditText input_minute;
    Button btn_setAlarm;
    Button btn_setAlarmTest;
    Button btn_cancelAlarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notifChannel = new NotificationChannel("customalarm", "Custom Alarm Channel", NotificationManager.IMPORTANCE_HIGH);
            notifChannel.setDescription("Bazinga");

            NotificationManager notifManager = getSystemService(NotificationManager.class);
            notifManager.createNotificationChannel(notifChannel);
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Intent intent = new Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            startActivity(intent);
        }

        input_hour = findViewById(R.id.input_hour);
        input_minute = findViewById(R.id.input_minute);
        btn_setAlarm = findViewById(R.id.btn_setAlarm);
        btn_setAlarmTest = findViewById(R.id.btn_setAlarmTest);
        btn_cancelAlarm = findViewById(R.id.btn_cancelAlarm);

        SharedPreferences sp = getSharedPreferences("alarmTime", MODE_PRIVATE);
        SharedPreferences.Editor spEdit = sp.edit();

        String hour = sp.getString("hour", "");
        String minute = sp.getString("minute", "");
        if (!hour.isEmpty()) input_hour.setText(hour);
        if (!minute.isEmpty()) input_minute.setText(minute);

        btn_setAlarm.setOnClickListener(v -> {
            if (input_hour.getText().toString().isEmpty() || input_minute.getText().toString().isEmpty()) {
                Toast.makeText(this, "Hour and minute must be filled", Toast.LENGTH_SHORT).show();
                return;
            }

            spEdit.putString("hour", input_hour.getText().toString());
            spEdit.putString("minute", input_minute.getText().toString());
            spEdit.apply();

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(input_hour.getText().toString()));
            calendar.set(Calendar.MINUTE, Integer.parseInt(input_minute.getText().toString()));
            calendar.set(Calendar.SECOND, 0);

            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            RegisterAlarm.register(this, calendar, alarmManager);

            Toast.makeText(this, "Alarm set for "+input_hour.getText().toString()+":"+input_minute.getText().toString(), Toast.LENGTH_SHORT).show();
        });

        btn_setAlarmTest.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis() + 3000);

            Toast.makeText(this, "Test alarm will run in 3 seconds...", Toast.LENGTH_SHORT).show();

            RegisterAlarm.register(this, calendar, alarmManager);
        });

        btn_cancelAlarm.setOnClickListener(v -> {
            Intent alarmCancelIntent = new Intent(this, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmCancelIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            alarmManager.cancel(pendingIntent);

            spEdit.remove("hour");
            spEdit.remove("minute");
            spEdit.apply();

            Toast.makeText(this, "Next alarm stopped", Toast.LENGTH_SHORT).show();
        });
    }
}