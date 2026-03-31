package com.example.project3;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private final Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Get the date from calendar view.
        CalendarView calendarView = findViewById(R.id.calendarView);
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
        });

        // Set the spinner.
        Spinner spinner = findViewById(R.id.my_spinner);
        String[] options = {"Select Frequency", "5 minutes", "10 minutes", "15 minutes"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Create the SharedPreferences
        SharedPreferences sp = getSharedPreferences("AlarmPrefs", MODE_PRIVATE);
        if (sp.getBoolean("isSet", false)) {

            int savedHour = sp.getInt("hour", 12);
            int savedMinute = sp.getInt("minute", 0);
            int savedFreq = sp.getInt("frequency", 5);

            TimePicker timePicker = findViewById(R.id.timePicker);
            timePicker.setHour(savedHour);
            timePicker.setMinute(savedMinute);

            int spinnerPosition = 0;
            if (savedFreq == 5) spinnerPosition = 1;
            else if (savedFreq == 10) spinnerPosition = 2;
            else if (savedFreq == 15) spinnerPosition = 3;
            spinner.setSelection(spinnerPosition);

            long savedMillis = sp.getLong("saved_millis", System.currentTimeMillis());
            calendar.setTimeInMillis(savedMillis);

            calendarView.setDate(savedMillis);
        }

        // Set the button.
        Button btnSetUpdate = findViewById(R.id.btnSetUpdate);
        btnSetUpdate.setOnClickListener(v -> {
            // Check if the permission is granted.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    // If not, ask permission.
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
                    return;
                }
            }
            start_sensing_new();
        });

        Button btnRemove = findViewById(R.id.btnRemove);
        btnRemove.setOnClickListener(v -> {
            stop_sensing();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("MainActivity", "onRequestPermissionsResult");

        if (requestCode == 10) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;

            if (granted) {
                Log.d("NotifPermission", "POST_NOTIFICATIONS: GRANTED");
                start_sensing_new();
            } else {
                Log.d("NotifPermission", "POST_NOTIFICATIONS: DENIED");
            }
        }
    }

    public void start_sensing_new() {
        Log.d("MainActivity", "start_sensing_new");

        // Get date and time.
        TimePicker timePicker = findViewById(R.id.timePicker);
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);

        // get the chosen option from spinner.
        Spinner spinner = findViewById(R.id.my_spinner);
        if (spinner.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a frequency.", Toast.LENGTH_SHORT).show();
            return;
        }
        String selectedItem = spinner.getSelectedItem().toString();
        int frequency = Integer.parseInt(selectedItem.split(" ")[0]);

        // Create intent and set notification.
        Intent intent = new Intent(this, sendNotification.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), (long) frequency * 60 * 1000, pendingIntent);

        Toast.makeText(this, "Alarm created for " + hour + ":" + String.format("%02d", minute), Toast.LENGTH_LONG).show();

        // Save the chosen time in preferences.
        SharedPreferences sp = getSharedPreferences("AlarmPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong("saved_millis", calendar.getTimeInMillis());
        editor.putInt("hour", hour);
        editor.putInt("minute", minute);
        editor.putInt("frequency", frequency);
        editor.putBoolean("isSet", true);
        editor.apply();
    }

    public void stop_sensing() {
        Log.d("MainActivity", "stop_sensing");

        Intent intent = new Intent(this, sendNotification.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);

            pendingIntent.cancel();

            Log.d("MainActivity", "Alarm cancelled");
            Toast.makeText(this, "Alarm removed", Toast.LENGTH_SHORT).show();

            SharedPreferences pref = getSharedPreferences("AlarmPrefs", MODE_PRIVATE);
            pref.edit().clear().apply();
        }
    }
}