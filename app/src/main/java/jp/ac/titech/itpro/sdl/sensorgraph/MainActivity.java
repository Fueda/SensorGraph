package jp.ac.titech.itpro.sdl.sensorgraph;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private final static String TAG = "MainActivity";

    private TextView rateView, accuracyView;
    private GraphView xView, yView, zView;

    private SensorManager sensorMgr;
    private Sensor sensor;

    private final static long GRAPH_REFRESH_PERIOD_MS = 20;

    private Handler handler;
    private Timer timer;

    private float vx, vy, vz;
    private float rate;
    private int accuracy;
    private long prevts;

    private final static float alpha = 0.75F;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        rateView = findViewById(R.id.rate_view);
        accuracyView = findViewById(R.id.accuracy_view);
        xView = findViewById(R.id.x_view);
        yView = findViewById(R.id.y_view);
        zView = findViewById(R.id.z_view);

        sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorMgr == null) {
            Toast.makeText(this, R.string.toast_no_sensor_manager, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        sensor = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (sensor == null) {
            Toast.makeText(this, R.string.toast_no_sensor_available, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        handler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        sensorMgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        timer = new Timer();
        timer.scheduleAtFixedRate(refreshTask, 0, GRAPH_REFRESH_PERIOD_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        sensorMgr.unregisterListener(this);
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        vx = alpha * vx + (1 - alpha) * event.values[0];
        vy = alpha * vy + (1 - alpha) * event.values[1];
        vz = alpha * vz + (1 - alpha) * event.values[2];
        rate = ((float) (event.timestamp - prevts)) / (1000 * 1000);
        prevts = event.timestamp;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "onAccuracyChanged: ");
        this.accuracy = accuracy;
    }

    private Runnable refreshRunner = new Runnable() {
        @Override
        public void run() {
            rateView.setText(getString(R.string.float_format, rate));
            accuracyView.setText(getString(R.string.int_format, accuracy));
            xView.addData(vx, true);
            yView.addData(vy, true);
            zView.addData(vz, true);
        }
    };

    private TimerTask refreshTask = new TimerTask() {
        @Override
        public void run() {
            handler.post(refreshRunner);
        }
    };
}
