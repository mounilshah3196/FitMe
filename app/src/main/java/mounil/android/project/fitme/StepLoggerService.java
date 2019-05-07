package mounil.android.project.fitme;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.widget.Toast;
import java.text.DecimalFormat;
import static android.os.SystemClock.elapsedRealtime;

public class StepLoggerService extends Service implements SensorEventListener {
    public static final String BROADCAST_ACTION = "mounil.android.project.fitme.displayevent";
    private static final String PRIMARY_CHANNEL_ID = "primary_notification_channel";
    private static final int NOTIFICATION_ID = 1234;
    private final Handler handler = new Handler();
    public int dayStepCounter = 0, totalStepCounter = 0;
    DbHelper dbHelper;
    AlarmManager myAlarmService, alarmManager;
    Intent updateUIIntent, notifyMilestoneIntent;
    PendingIntent notifyMilestonePendingIntent;
    int totalStepCount;
    DecimalFormat df = new DecimalFormat("#.#");
    private SessionManager sessionManager;
    private SensorManager sensorManager = null;
    private Sensor stepCountSensor = null;
    private NotificationManager mNotificationManager;
    private DbHelper mStepsDBHelper;
    private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {
            getDataForList();
            handler.postDelayed(this, 1000);
        }
    };

    public StepLoggerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        updateUIIntent = new Intent(BROADCAST_ACTION);
        sessionManager = new SessionManager(getApplicationContext());
        dbHelper = new DbHelper(this);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (sessionManager.loggedIn()) {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (stepCountSensor != null) {
                sensorManager.registerListener(this, stepCountSensor, sensorManager.SENSOR_DELAY_FASTEST);
                createNotificationChannel();
                alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                mStepsDBHelper = new DbHelper(getApplicationContext());
                notifyMilestoneIntent = new Intent(this, AlarmManager.class);
                notifyMilestonePendingIntent = PendingIntent.getBroadcast(
                        getApplicationContext(), NOTIFICATION_ID, notifyMilestoneIntent, PendingIntent.FLAG_ONE_SHOT);
                totalStepCount = dbHelper.readTotalStepsEntries(sessionManager.getUserName());
                handler.removeCallbacks(sendUpdatesToUI);
                handler.postDelayed(sendUpdatesToUI, 100);
                return START_STICKY;
            }
            Toast.makeText(getApplicationContext(), "No Sensor Found To Monitor Step Count", Toast.LENGTH_SHORT);
        }
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (sessionManager.loggedIn()) {
            Intent restartServicTask = new Intent(getApplicationContext(), StepLoggerService.class);
            restartServicTask.setPackage(getPackageName());
            PendingIntent restartPendingIntent = PendingIntent.getService(getApplicationContext(),
                    1, restartServicTask, PendingIntent.FLAG_ONE_SHOT);
            myAlarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            System.out.println("Executed onTaskRemoved");
            myAlarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), restartPendingIntent);
            Intent intent = new Intent(this, RestartServiceReciever.class);
            intent.putExtra("yourvalue", "torestore");
            sendBroadcast(intent);
        }
    }

    @Override
    public void onDestroy() {
        if (sessionManager.loggedIn()) {
            Intent intent = new Intent(this, RestartServiceReciever.class);
            intent.putExtra("yourvalue", "torestore");
            sendBroadcast(intent);
        }
        handler.removeCallbacks(sendUpdatesToUI);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        mStepsDBHelper.createStepsEntry(sessionManager.getUserName());
        dayStepCounter = mStepsDBHelper.readStepsEntries(sessionManager.getUserName());
        if (dayStepCounter % 1000 == 0 && dayStepCounter != 0) {
            Intent notifyIntent = new Intent(getApplicationContext(), AlarmReciever.class);
            PendingIntent notifyPendingIntent = PendingIntent.getBroadcast(
                    getApplicationContext(), NOTIFICATION_ID, notifyIntent, PendingIntent.FLAG_ONE_SHOT);
            if (alarmManager != null) {
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        elapsedRealtime(), notifyPendingIntent);
            }
        }
        int rawCount = Math.round(sensorEvent.values[0]);
        totalStepCounter = rawCount - (rawCount - totalStepCount);
        /*1 MILE DISTANCE IS ASSUMED TO BE COVERED IN 2160 REGULAR STEPS FOR A PERSON 5'11'' TALL*/
        /*REFERENCE SOURCE WWW.OPENFIT.COM*/
        String distance = df.format(totalStepCount / 2160);
        updateUIIntent.putExtra("totalStepCounter", distance);
        updateUIIntent.putExtra("dayStepCounter", String.valueOf(dayStepCounter));
        handler.removeCallbacks(sendUpdatesToUI);
        handler.postDelayed(sendUpdatesToUI, 100);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void createNotificationChannel() {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel
                    (PRIMARY_CHANNEL_ID, "Stand up notification",
                            NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setDescription
                    ("Notifies every 15 minutes to stand up and walk");
            mNotificationManager.createNotificationChannel(notificationChannel);
        }
    }

    private void getDataForList() {
        sendBroadcast(updateUIIntent);
    }

}