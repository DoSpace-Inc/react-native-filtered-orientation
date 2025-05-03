# React Native Orientation Angle

[![npm version](https://badge.fury.io/js/react-native-filtered-orientation.svg)](https://badge.fury.io/js/react-native-filtered-orientation)

A React Native module that provides device orientation angles (pitch, roll, yaw) using quaternion data with optional angle smoothing. Works for both iOS and Android.

## Features

- Provides pitch, roll, and yaw from device motion
- Uses Core Motion (iOS) and SensorManager (Android)
- Supports optional low-pass filtering using configurable `alpha` value
- Easy subscription-based API

## Installation

```bash
yarn add react-native-filtered-orientation

# For RN >= 0.60
cd ios && pod install
```

## Usage

### Subscribe to orientation updates

```js
import { orientationAngle } from 'react-native-filtered-orientation'

orientationAngle.subscribe((angles) => {
  console.log(angles) // { pitch: number, roll: number, yaw: number }
})
```

### Unsubscribe from updates

```js
orientationAngle.unsubscribe()
```

### Set update interval

Set how often orientation updates are emitted (in milliseconds).

```js
orientationAngle.setUpdateInterval(100) // 100ms
```

### Get current update interval

```js
orientationAngle.getUpdateInterval((ms) => {
  console.log('Current interval:', ms)
})
```

### Set alpha value (for filtering)

Set the smoothing factor (`alpha`) for pitch, roll, and yaw.  
A higher alpha means smoother but slower-to-react values.

```js
orientationAngle.setAlpha(0.8) // Range: 0.0 to 1.0, default is 0.8
```

### Get current alpha

```js
orientationAngle.getAlpha((alpha) => {
  console.log('Current alpha:', alpha)
})
```

## Hook Example

```js
import { useEffect } from 'react'
import { orientationAngle } from 'react-native-filtered-orientation'

export const useOrientationAngle = () => {
  useEffect(() => {
    orientationAngle.setUpdateInterval(300)
    orientationAngle.setAlpha(0.75)

    orientationAngle.subscribe((angles) => {
      console.log('Orientation:', angles)
    })

    return () => {
      orientationAngle.unsubscribe()
    }
  }, [])
}
```

## Output Format

The `angles` object returned from `subscribe()` looks like:

```ts
{
  pitch: number, // In degrees
  roll: number,  // In degrees
  yaw: number    // In degrees
}
```

## Platform Support

- ✅ iOS (Core Motion)
- ✅ Android (SensorManager)
- 🔲 Web (not supported)

## Notes

- Make sure motion permissions are handled on iOS 13+ (e.g. `NSMotionUsageDescription`)
- Filtering (`alpha`) is applied only if you use the native module directly. If you compute angles in JS, apply your own smoothing.

## License

[MIT](LICENSE.md)
