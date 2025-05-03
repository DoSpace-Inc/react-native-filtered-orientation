export type EventData = {
  readonly pitch: number
  readonly roll: number
  readonly yaw: number
}

export interface IOrientationAngle {
  subscribe: (callback: (angle: EventData) => void) => void
  unsubscribe: () => void
  setUpdateInterval: (interval: number) => void
  getUpdateInterval: (callback: (interval: number) => void) => void
}
