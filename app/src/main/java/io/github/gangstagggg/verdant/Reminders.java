package io.github.gangstagggg.verdant;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.time.ZonedDateTime;

/** Lagrer plantelisten fra webappen og planlegger det daglige vanningsvarselet. */
final class Reminders {
    static final String CHANNEL_ID = "watering";
    static final int HOUR = 8;          // klokkeslett for varselet (lokal tid)
    private static final String PREFS = "reminders";
    static final String KEY_LIST = "list";
    static final String KEY_ON = "on";

    private Reminders() {}

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static void save(Context c, String json, boolean on) {
        prefs(c).edit().putString(KEY_LIST, json).putBoolean(KEY_ON, on).apply();
        if (on) {
            schedule(c);
        } else {
            cancel(c);
        }
    }

    private static PendingIntent pending(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class);
        return PendingIntent.getBroadcast(c, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Neste varsel kl. 08:00. Settes på nytt hver gang varselet har gått. */
    static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next = now.withHour(HOUR).withMinute(0).withSecond(0).withNano(0);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), pending(c));
    }

    static void cancel(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(pending(c));
        }
    }

    static void ensureChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                    c.getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription(c.getString(R.string.channel_description));
            nm.createNotificationChannel(ch);
        }
    }
}
