package com.reactnativeorientationangle;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class OrientationAngleModule extends ReactContextBaseJavaModule implements SensorEventListener {
    private final ReactApplicationContext reactContext;
    private final SensorManager sensorManager;
    private final Sensor sensor;
    private double lastReading = (double) System.currentTimeMillis();
    private int interval = 100;
    private double alpha = 0.8;

    private double prevYaw = -1000.0;
    private double prevPitch = -1000.0;
    private double prevRoll = -1000.0;

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private final float[] quaternion = new float[4];

    private boolean isListening = false; // Guard flag

    public OrientationAngleModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.sensorManager = (SensorManager) reactContext.getSystemService(reactContext.SENSOR_SERVICE);
        this.sensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
    }

    @Override
    @NonNull
    public String getName() {
        return "OrientationAngle";
    }

    @ReactMethod
    public void setAlpha(double newAlpha) {
        this.alpha = newAlpha;
    }

    @ReactMethod
    public void getAlpha(Callback callback) {
        callback.invoke(this.alpha);
    }

    @ReactMethod
    public void setUpdateInterval(int newInterval) {
        this.interval = newInterval;
    }

    @ReactMethod
    public void getUpdateInterval(Callback callBack) {
        callBack.invoke(this.interval);
    }

    @ReactMethod
    public void startUpdates() {
        if (!isListening) {
            this.sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
            isListening = true;
            Log.d("OrientationAngle", "Sensor started");
        }
    }

    @ReactMethod
    public void stopUpdates() {
        if (isListening) {
            this.sensorManager.unregisterListener(this);
            isListening = false;
            Log.d("OrientationAngle", "Sensor stopped");
        }
    }

    // CRITICAL FIX: Add missing methods required by NativeEventEmitter
    @ReactMethod
    public void addListener(String eventName) {
        // Required by NativeEventEmitter but handled by React Native bridge
        // Just log for debugging
        Log.d("OrientationAngle", "addListener called for: " + eventName);
    }

    @ReactMethod
    public void removeListeners(Integer count) {
        // Required by NativeEventEmitter but handled by React Native bridge
        // Just log for debugging
        Log.d("OrientationAngle", "removeListeners called for count: " + count);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (!isListening) {
            return; // Ignore events if not listening
        }
        double currentTime = (double) System.currentTimeMillis();
        if (currentTime - this.lastReading >= this.interval) {
            this.lastReading = currentTime;
            SensorManager.getQuaternionFromVector(quaternion, sensorEvent.values);
            double[] eulerAngles = quaternionToEuler(quaternion);
            WritableMap params = Arguments.createMap();
            params.putDouble("pitch", eulerAngles[0]);
            params.putDouble("roll", eulerAngles[1]);
            params.putDouble("yaw", eulerAngles[2]);
            this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("OrientationAngle", params);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private double[] quaternionToEuler(float[] quaternion) {
        // Your existing conversion code
        double ysqr = quaternion[2] * quaternion[2];
        double t0 = -2.0 * (ysqr + quaternion[3] * quaternion[3]) + 1.0;
        double t1 = +2.0 * (quaternion[1] * quaternion[2] + quaternion[0] * quaternion[3]);
        double t2 = -2.0 * (quaternion[1] * quaternion[3] - quaternion[0] * quaternion[2]);
        double t3 = +2.0 * (quaternion[2] * quaternion[3] + quaternion[0] * quaternion[1]);
        double t4 = -2.0 * (quaternion[1] * quaternion[1] + ysqr) + 1.0;
        t2 = Math.max(-1.0, Math.min(1.0, t2));
        double pitch = Math.toDegrees(Math.atan2(t3, t4));
        if (pitch < 0 && Math.abs(pitch) > 90) {
            pitch += 360;
        }
        pitch -= 90;
        double roll = Math.toDegrees(Math.asin(t2));
        double yaw = -Math.toDegrees(Math.atan2(t1, t0));
        if (prevPitch < -360) {
            prevPitch = pitch;
            prevRoll = roll;
            prevYaw = yaw;
        }
        prevPitch += this.alpha * (pitch - prevPitch);
        prevRoll += this.alpha * (roll - prevRoll);
        prevYaw += this.alpha * normalizeAngle(yaw - prevYaw);
        prevYaw = normalizeAngle(prevYaw);
        return new double[] { prevPitch, prevRoll, prevYaw };
    }

    private double normalizeAngle(double angle) {
        while (true) {
            if (angle > 180)
                angle -= 360;
            else if (angle < -180)
                angle += 360;
            else
                break;
        }
        return angle;
    }
}