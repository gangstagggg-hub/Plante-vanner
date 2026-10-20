package io.github.gangstagggg.verdant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Setter varselet på nytt etter omstart av telefonen og etter app-oppdatering. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Reminders.prefs(context).getBoolean(Reminders.KEY_ON, false)) {
            Reminders.schedule(context);
        }
    }
}
