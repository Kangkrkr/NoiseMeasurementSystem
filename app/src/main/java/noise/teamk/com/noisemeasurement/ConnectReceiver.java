package noise.teamk.com.noisemeasurement;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ConnectReceiver extends BroadcastReceiver {

    public ConnectReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String message = intent.getExtras().getString("message");

        if (action.equals("com.action.message_received")) {
            Intent i = new Intent(context, MessageDialog.class);
            i.putExtra("message", message);
            PendingIntent pi = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_ONE_SHOT);
            try {
                pi.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }

    }
}
