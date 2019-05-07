package mounil.android.project.fitme;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RestartServiceReciever extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        context.startService(new Intent(context, StepLoggerService.class));
    }
}