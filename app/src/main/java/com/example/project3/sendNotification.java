package com.example.project3;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.app.NotificationCompat;

public class sendNotification extends BroadcastReceiver {
    public static final String NOTIFICATION_CHANNEL_ID = "10001";

    public void onReceive(Context context , Intent intent) {

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context. NOTIFICATION_SERVICE ) ;

        Notification notification = getNotification(context,"Time to exercise!!");

        if (android.os.Build.VERSION. SDK_INT >= android.os.Build.VERSION_CODES. O ) {
            int importance = NotificationManager. IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID , "NOTIFICATION_CHANNEL_NAME" , importance);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        notificationManager.notify(0 , notification);

        SharedPreferences pref = context.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE);
        if (pref.getBoolean("isSet", false)) {
            long currentSavedMillis = pref.getLong("saved_millis", 0);
            int frequency = pref.getInt("frequency", 5); // 5, 10, 15

            long nextAlarmMillis = currentSavedMillis + (frequency * 60 * 1000);

            pref.edit().putLong("saved_millis", nextAlarmMillis).apply();
        }
    }

    private Notification getNotification(Context context,String content){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID );
        builder.setContentTitle( "Scheduled Notification" );
        builder.setContentText(content);
        builder.setSmallIcon(R.drawable. ic_launcher_foreground );
        builder.setChannelId( NOTIFICATION_CHANNEL_ID );
        return builder.build();
    }
}
