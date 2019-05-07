package mounil.android.project.fitme;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class FitnessActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {
    public static final int NOTIFICATION_ID = 1234;
    private static final String PRIMARY_CHANNEL_ID = "primary_notification_channel";
    public TextView stepCount, welcomeUser, totalStepsWalked;
    public DbHelper dbHelper;
    Intent mServiceIntent;
    Sensor stepCountSensor;
    int totalStepCount;
    private Button btnLogout;
    private Switch officeModeSwitch;
    private SessionManager sessionManager;
    private NotificationManager mNotificationManager;
    private StepLoggerService service;
    private Intent intent;
    private PendingIntent notifyPendingIntent;
    private AlarmManager alarmManager;
    private SensorManager sensorManager;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fitness);
        intent = new Intent(this, StepLoggerService.class);
        dbHelper = new DbHelper(this);
        sessionManager = new SessionManager(this);
        if (!sessionManager.loggedIn()) {
            logout();
        }
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        totalStepCount = dbHelper.readTotalStepsEntries(sessionManager.getUserName());
        btnLogout = (Button) findViewById(R.id.fitnessActivity_logout_button);
        totalStepsWalked = (TextView) findViewById(R.id.fitnessActivity_totalDistance_textView);
        stepCount = (TextView) findViewById(R.id.fitnessActivity_dailyStepCount_textView);
        welcomeUser = (TextView) findViewById(R.id.fitnessActivity_userName_textView);
        officeModeSwitch = (Switch) findViewById(R.id.fitnessActivity_officeMode_switch);
        welcomeUser.setText("Hello " + sessionManager.getUserName() + "!");
        createNotificationChannel();
        btnLogout.setOnClickListener(this);
        officeModeSwitch.setChecked(sessionManager.getSwitchState());
        registerReceiver(broadcastReceiver, new IntentFilter(StepLoggerService.BROADCAST_ACTION));
        service = new StepLoggerService();
        mServiceIntent = new Intent(getApplicationContext(), service.getClass());
        if (!isMyServiceRunning(service.getClass())) {
            startService(mServiceIntent);
        }
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent notifyIntent = new Intent(this, NotificationReceiver.class);
        notifyPendingIntent = PendingIntent.getBroadcast(this, NOTIFICATION_ID, notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.fitnessActivity_logout_button:
                logout();
                break;
            default:
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startService(new Intent(this, StepLoggerService.class));
        stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepCountSensor != null) {
            sensorManager.registerListener(this, stepCountSensor, sensorManager.SENSOR_DELAY_FASTEST);
        } else {
            displayMessage("Counter Sensor Not Available!");
        }

        officeModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (officeModeSwitch.isChecked()) {
                    sessionManager.setSwitchState(true);
                    long triggerTime = SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HOUR;
                    if (alarmManager != null) {
                        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                        triggerTime, AlarmManager.INTERVAL_HOUR, notifyPendingIntent);
                        displayMessage("Office Mode On!");
                    }
                } else {
                    sessionManager.setSwitchState(false);
                    mNotificationManager.cancelAll();
                    if (alarmManager != null) {
                        alarmManager.cancel(notifyPendingIntent);
                    }
                    displayMessage("Office Mode Off");
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(this, StepLoggerService.class));
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void displayMessage(String message) {
        Toast.makeText(FitnessActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void createNotificationChannel() {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel
                    (PRIMARY_CHANNEL_ID, "Stand up notification",
                            NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setDescription("Notifies On Every 1000 Step Milestone");
            mNotificationManager.createNotificationChannel(notificationChannel);
        }
    }

    private void updateUI(Intent intent) {
        String dayStepCounter = intent.getStringExtra("dayStepCounter");
        String totalStepCounter = intent.getStringExtra("totalStepCounter");
        stepCount = (TextView) findViewById(R.id.fitnessActivity_dailyStepCount_textView);
        stepCount.setText(dayStepCounter);
        totalStepsWalked = (TextView) findViewById(R.id.fitnessActivity_totalDistance_textView);
        totalStepsWalked.setText("Total distance walked so far: " + totalStepCounter + " Mile(s)");
        System.out.println(dbHelper.readStepsEntries(sessionManager.getUserName()));
    }

    private void logout() {
        sessionManager.setLoggedIn(false);
        sessionManager.setSwitchState(false);
        alarmManager.cancel(notifyPendingIntent);
        sensorManager.unregisterListener(this, stepCountSensor);
        stopService(mServiceIntent);
        finish();
        startActivity(new Intent(FitnessActivity.this, LoginActivity.class));
    }
}