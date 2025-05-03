import { NativeModules, NativeEventEmitter, Platform } from 'react-native'
import type { EmitterSubscription } from 'react-native'

import type { IOrientationAngle } from './types'

const isAndroid = Platform.OS === 'android'

const { OrientationAngle } = NativeModules

const eventEmitter = new NativeEventEmitter(OrientationAngle)

let subscription: EmitterSubscription | null = null

const orientationAngle: IOrientationAngle = {
  subscribe(callback) {
    if (!subscription) {
      if (isAndroid) OrientationAngle.startUpdates()

        subscription = eventEmitter.addListener('OrientationAngle', callback)

    } else {
      console.warn('Already subscribed')
    }
  },

  unsubscribe() {
    if (subscription) {
      if (isAndroid) OrientationAngle.stopUpdates()

      subscription.remove()
      subscription = null
    } else {
      console.warn('Already unsubscribed')
    }
  },

  setUpdateInterval(interval: number) {
    OrientationAngle.setUpdateInterval(interval)
  },

  getUpdateInterval(callback) {
    OrientationAngle.getUpdateInterval(callback)
  },
}

export { orientationAngle }
