package jp.ac.titech.itpro.sdl.sensorgraph;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private final static String TAG = "MainActivity";

    private TextView sensorTypeView;
    private TextView rateView, accuracyView;
    private GraphView xView, yView, zView;

    private SensorManager sensorManager;
    private Sensor sensor;

    private final static long GRAPH_REFRESH_PERIOD_MS = 20;

    private Handler handler;
    private Timer timer;

    private float vx, vy, vz;
    private int rate;
    private int accuracy;
    private long prev_ts;

    private int sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
    private int sensorType = Sensor.TYPE_ACCELEROMETER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        sensorTypeView = findViewById(R.id.sensor_type_view);
        rateView = findViewById(R.id.rate_view);
        accuracyView = findViewById(R.id.accuracy_view);
        xView = findViewById(R.id.x_view);
        yView = findViewById(R.id.y_view);
        zView = findViewById(R.id.z_view);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager == null) {
            Toast.makeText(this, R.string.toast_no_sensor_manager, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        sensor = sensorManager.getDefaultSensor(sensorType);
        if (sensor == null) {
            Toast.makeText(this, getString(R.string.toast_no_sensor_available,
                    sensorTypeName(sensorType)), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        handler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        timer = new Timer();
        timer.scheduleAtFixedRate(refreshTask, 0, GRAPH_REFRESH_PERIOD_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        sensorManager.unregisterListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "onPrepareOptionsMenu");
        MenuItem item_delay = menu.findItem(R.id.menu_delay);
        MenuItem item_accelerometer = menu.findItem(R.id.menu_accelerometer);
        MenuItem item_gyroscope = menu.findItem(R.id.menu_gyroscope);
        MenuItem item_magnetic_field = menu.findItem(R.id.menu_magnetic_field);
        switch (sensorDelay) {
        case SensorManager.SENSOR_DELAY_FASTEST:
            item_delay.setTitle(R.string.menu_delay_fastest_title);
            break;
        case SensorManager.SENSOR_DELAY_GAME:
            item_delay.setTitle(R.string.menu_delay_game_title);
            break;
        case SensorManager.SENSOR_DELAY_UI:
            item_delay.setTitle(R.string.menu_delay_ui_title);
            break;
        case SensorManager.SENSOR_DELAY_NORMAL:
            item_delay.setTitle(R.string.menu_delay_normal_title);
            break;
        }
        item_accelerometer.setEnabled(true);
        item_gyroscope.setEnabled(true);
        item_magnetic_field.setEnabled(true);
        switch (sensorType) {
        case Sensor.TYPE_ACCELEROMETER:
            item_accelerometer.setEnabled(false);
            break;
        case Sensor.TYPE_GYROSCOPE:
            item_gyroscope.setEnabled(false);
            break;
        case Sensor.TYPE_MAGNETIC_FIELD:
            item_magnetic_field.setEnabled(false);
            break;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
        case R.id.menu_delay:
            Log.d(TAG, "menu_delay");
            switch (sensorDelay) {
            case SensorManager.SENSOR_DELAY_NORMAL:
                sensorDelay = SensorManager.SENSOR_DELAY_UI;
                break;
            case SensorManager.SENSOR_DELAY_UI:
                sensorDelay = SensorManager.SENSOR_DELAY_GAME;
                break;
            case SensorManager.SENSOR_DELAY_GAME:
                sensorDelay = SensorManager.SENSOR_DELAY_FASTEST;
                break;
            case SensorManager.SENSOR_DELAY_FASTEST:
                sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
                break;
            }
            break;
        case R.id.menu_accelerometer:
            sensorType = Sensor.TYPE_ACCELEROMETER;
            sensorTypeView.setText(R.string.menu_accelerometer_title);
            break;
        case R.id.menu_gyroscope:
            sensorType = Sensor.TYPE_GYROSCOPE;
            sensorTypeView.setText(R.string.menu_gyroscope_title);
            break;
        case R.id.menu_magnetic_field:
            sensorType = Sensor.TYPE_MAGNETIC_FIELD;
            sensorTypeView.setText(R.string.menu_magnetic_field_title);
            break;
        }
        invalidateOptionsMenu();
        changeConfig();
        return super.onOptionsItemSelected(item);
    }

    private void changeConfig() {
        sensorManager.unregisterListener(this);
        Sensor sensor = sensorManager.getDefaultSensor(sensorType);
        sensorManager.registerListener(this, sensor, sensorDelay);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        vx = event.values[0];
        vy = event.values[1];
        vz = event.values[2];
        long ts = event.timestamp;
        rate = (int) (ts - prev_ts) / 1000;
        prev_ts = ts;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "onAccuracyChanged: ");
        this.accuracy = accuracy;
    }

    private final Runnable refreshRunner = new Runnable() {
        @Override
        public void run() {
            rateView.setText(getString(R.string.int_format, rate));
            accuracyView.setText(getString(R.string.int_format, accuracy));
            xView.addData(vx, true);
            yView.addData(vy, true);
            zView.addData(vz, true);
        }
    };

    private final TimerTask refreshTask = new TimerTask() {
        @Override
        public void run() {
            handler.post(refreshRunner);
        }
    };

    private String sensorTypeName(int sensorType) {
        try {
            Class klass = Sensor.class;
            for (Field field : klass.getFields()) {
                String fieldName = field.getName();
                if (fieldName.startsWith("TYPE_") && field.getInt(klass) == sensorType)
                    return fieldName;
            }
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
