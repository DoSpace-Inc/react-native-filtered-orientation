package com.reactnativeorientationangle;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
	private double alpha = 0.8; // Default value for alpha (you can adjust this)

	private double prevYaw = -1000.0;
	private double prevPitch = -1000.0;
	private double prevRoll = -1000.0;

	private final float[] rotationMatrix = new float[9];
	private final float[] orientationAngles = new float[3];

	private float[] quaternion = new float[4];

	public OrientationAngleModule(ReactApplicationContext reactContext) {
		super(reactContext);

		this.reactContext = reactContext;
		this.sensorManager = (SensorManager) reactContext.getSystemService(reactContext.SENSOR_SERVICE);
		this.sensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
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

	// Method to get current alpha value
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
		this.sensorManager.registerListener(this, sensor, this.sensorManager.SENSOR_DELAY_FASTEST);
	}

	@ReactMethod
	public void stopUpdates() {
		this.sensorManager.unregisterListener(this);
	}

	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		double currentTime = (double) System.currentTimeMillis();
		if (currentTime - this.lastReading >= this.interval) {
			this.lastReading = currentTime;

			// Get quaternion from sensor
			SensorManager.getQuaternionFromVector(quaternion, sensorEvent.values);

			// Convert quaternion to Euler angles
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

	// Convert quaternion to Euler angles
	private double[] quaternionToEuler(float[] quaternion) {
		double ysqr = quaternion[2] * quaternion[2];

		// Pre-calculate values for the Euler angles
		double t0 = -2.0 * (ysqr + quaternion[3] * quaternion[3]) + 1.0;
		double t1 = +2.0 * (quaternion[1] * quaternion[2] + quaternion[0] * quaternion[3]);
		double t2 = -2.0 * (quaternion[1] * quaternion[3] - quaternion[0] * quaternion[2]);
		double t3 = +2.0 * (quaternion[2] * quaternion[3] + quaternion[0] * quaternion[1]);
		double t4 = -2.0 * (quaternion[1] * quaternion[1] + ysqr) + 1.0;

		// Clamp t2 to avoid NaN (not-a-number) in the arcsin function
		t2 = Math.max(-1.0, Math.min(1.0, t2));

		// Calculate pitch, roll, and yaw
		double pitch = Math.toDegrees(Math.atan2(t3, t4)) - 90;
		double roll = Math.toDegrees(Math.asin(t2));
		double yaw = -Math.toDegrees(Math.atan2(t1, t0));
		if (prevPitch < -360) {
			prevPitch = pitch;
			prevRoll = roll;
			prevYaw = yaw;
		}
		prevPitch = filterAngle(prevPitch, pitch);
		prevRoll = filterAngle(prevRoll, roll);
		prevYaw = filterAngle(prevYaw, yaw);

		// Return in the order of [pitch, roll, yaw]
		return new double[] { prevPitch, prevRoll, prevYaw }; // Adjust pitch by -90 to match original code
	}

	// Normalize angle to the range [-180, 180]
	private double normalizeAngle(double angle) {
		return (angle + 180) % 360 - 180;
	}

	// Method to calculate the angle difference between two angles
	private double angleDifference(double prev, double current) {
		return normalizeAngle(normalizeAngle(prev), normalizeAngle(current));
	}

	// Method to filter yaw (you can add similar methods for pitch and roll)
	private double filterAngle(double prev, double current) {
		double delta = angleDifference(prev, current);
		double adjusted_current = prev + delta;
		double filtered_angle = this.alpha * prev + (1 - this.alpha) * adjusted_current;
		return normalizeAngle(filtered_angle);
	}

}
