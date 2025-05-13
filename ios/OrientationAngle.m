// OrientationAngle.m
#import "OrientationAngle.h"
#import <CoreMotion/CoreMotion.h>
#import <React/RCTLog.h>

#define RAD_TO_DEG (180.0f / 3.141592653589793f)

@interface OrientationAngle ()

@property(nonatomic, strong) CMMotionManager *motionManager;
@property(nonatomic, assign) float alpha; // Alpha for filtering
@property(nonatomic, assign) float prevYaw;
@property(nonatomic, assign) float prevPitch;
@property(nonatomic, assign) float prevRoll;

@end

@implementation OrientationAngle {
  CMMotionManager *_motionManager;
}

// To export a module named OrientationAngle
RCT_EXPORT_MODULE();

- (instancetype)init {
  if (self = [super init]) {
    _motionManager = [[CMMotionManager alloc] init];
    _alpha = 0.8; // Default alpha value
    _prevYaw = -1000.0;
    _prevPitch = -1000.0;
    _prevRoll = -1000.0;
  }
  return self;
}

+ (BOOL)requiresMainQueueSetup {
  return NO;
}

- (NSArray<NSString *> *)supportedEvents {
  return @[ @"OrientationAngle" ];
}

// Will be called when this module's first listener is added.
- (void)startObserving {
  // Set up any upstream listeners or background tasks as necessary
  if (_motionManager.deviceMotionAvailable) {
    NSOperationQueue *queue = [[NSOperationQueue alloc] init];
    [_motionManager
        startDeviceMotionUpdatesToQueue:queue
                            withHandler:^(CMDeviceMotion *motion,
                                          NSError *error) {
                              // Get the attitude of the device
                              CMQuaternion quaternion =
                                  motion.attitude.quaternion;
                              NSArray *eulerAngles =
                                  [self quaternionToEuler:quaternion];

                              [self sendEventWithName:@"OrientationAngle"
                                                 body:@{
                                                   @"pitch" : eulerAngles[0],
                                                   @"roll" : eulerAngles[1],
                                                   @"yaw" : eulerAngles[2]
                                                 }];
                            }];

    NSLog(@"Device motion started");
  } else {
    NSLog(@"Device motion unavailable");
  }
}

// Will be called when this module's last listener is removed, or on dealloc.
- (void)stopObserving {
  NSLog(@"Device motion stop");

  // Remove upstream listeners, stop unnecessary background tasks
  [_motionManager stopDeviceMotionUpdates];
}

// Normalize angle to the range [-180, 180]
- (float)normalizeAngle:(float)angle {
  while (true) {
    if (angle > 180) {
      angle -= 360;
    } else if (angle < -180) {
      angle += 360;
    } else {
      break;
    }
  }
  return angle;
}

// Convert quaternion to Euler angles (pitch, roll, yaw)
- (NSArray *)quaternionToEuler:(CMQuaternion)quaternion {
  float ysqr = quaternion.y * quaternion.y;

  // Pre-calculate values for the Euler angles
  float t0 = -2.0 * (ysqr + quaternion.z * quaternion.z) + 1.0;
  float t1 = +2.0 * (quaternion.x * quaternion.y + quaternion.w * quaternion.z);
  float t2 = -2.0 * (quaternion.x * quaternion.z - quaternion.w * quaternion.y);
  float t3 = +2.0 * (quaternion.y * quaternion.z + quaternion.w * quaternion.x);
  float t4 = -2.0 * (quaternion.x * quaternion.x + ysqr) + 1.0;

  // Clamp t2 to avoid NaN in the arcsin function
  t2 = fminf(1.0, fmaxf(-1.0, t2));

  // Calculate pitch, roll, and yaw
  float pitch = atan2f(t3, t4) * RAD_TO_DEG;
  if (pitch < 0 && fabsf(pitch) > 90.0f) {
    pitch += 360.0f;
  }
  pitch -= 90.0f; // Adjust pitch by -90 to match original code

  float roll = asinf(t2) * RAD_TO_DEG;
  float yaw = -atan2f(t1, t0) * RAD_TO_DEG;

  // Initialize previous values if needed
  if (_prevPitch < -360.0f) {
    _prevPitch = pitch;
    _prevRoll = roll;
    _prevYaw = yaw;
  }

  // Apply exponential smoothing
  _prevPitch += _alpha * (pitch - _prevPitch);
  _prevRoll += _alpha * (roll - _prevRoll);
  _prevYaw += _alpha * [self normalizeAngle:(yaw - _prevYaw)];
  _prevYaw = [self normalizeAngle:_prevYaw];

  // Return values in [pitch, roll, yaw] order
  return @[ @(_prevPitch), @(_prevRoll), @(_prevYaw) ];
}

RCT_EXPORT_METHOD(setUpdateInterval : (float)newInterval) {
  float interval = newInterval / 1000; // millisecond to second

  NSLog(@"setUpdateInterval: %f", interval);

  [_motionManager setDeviceMotionUpdateInterval:interval];
}

RCT_EXPORT_METHOD(getUpdateInterval : (RCTResponseSenderBlock)callback) {
  float interval =
      _motionManager.deviceMotionUpdateInterval * 1000; // second to millisecond

  NSLog(@"getUpdateInterval: %f", interval);

  callback(@[ [NSNumber numberWithFloat:interval] ]);
}

RCT_EXPORT_METHOD(setAlpha : (float)newAlpha) { self.alpha = newAlpha; }

RCT_EXPORT_METHOD(getAlpha : (RCTResponseSenderBlock)callback) {
  callback(@[ @(self.alpha) ]);
}

@end
