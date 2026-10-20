package io.github.gangstagggg.verdant;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Kjører kl. 08:00. Sjekker hvilke planter som trenger vann og viser ett varsel. */
public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Reminders.prefs(context).getBoolean(Reminders.KEY_ON, false)) {
            return;
        }
        // Planlegg alltid neste dag først, så en feil under ikke stopper varslene.
        Reminders.schedule(context);

        List<String> lines = new ArrayList<>();
        String firstName = "";
        String firstDetail = "";
        try {
            JSONArray arr = new JSONArray(Reminders.prefs(context).getString(Reminders.KEY_LIST, "[]"));
            LocalDate today = LocalDate.now();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject p = arr.getJSONObject(i);
                LocalDate due = LocalDate.parse(p.getString("due"));
                if (due.isAfter(today)) {
                    continue;
                }
                long overdue = today.toEpochDay() - due.toEpochDay();
                String name = p.optString("name");
                String room = p.optString("room");
                String moisture = p.optString("moisture");
                String detail = room + (moisture.isEmpty() ? "" : " · " + moisture)
                        + (overdue > 0 ? " — overdue " + overdue + (overdue == 1 ? " day" : " days") : " — today");
                if (lines.isEmpty()) {
                    firstName = name;
                    firstDetail = detail;
                }
                lines.add(name + " (" + room + ")");
            }
        } catch (Exception e) {
            return;
        }
        if (lines.isEmpty()) {
            return;
        }

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        if (!nm.areNotificationsEnabled()) {
            return;
        }
        Reminders.ensureChannel(context);

        Intent open = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(context, 1, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title;
        String text;
        NotificationCompat.Builder b = new NotificationCompat.Builder(context, Reminders.CHANNEL_ID);
        if (lines.size() == 1) {
            title = "🌱 " + firstName + " needs water";
            text = firstDetail;
        } else {
            title = "🌱 " + lines.size() + " plants need water";
            text = String.join(", ", lines);
            NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
            for (String l : lines) {
                style.addLine(l);
            }
            b.setStyle(style);
        }
        b.setSmallIcon(R.drawable.ic_stat_leaf)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(content)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER);
        try {
            nm.notify(1, b.build());
        } catch (SecurityException ignored) {
            // Varsler er ikke tillatt.
        }
    }
}
