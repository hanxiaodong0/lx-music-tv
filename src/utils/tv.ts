import { DeviceEventEmitter, NativeModules } from 'react-native'

// A build flag also works on Xiaomi firmware that does not report UI_MODE_TYPE_TELEVISION.
export const isTV = NativeModules.TVNavigation?.enabled === true
export const appId: string = NativeModules.TVNavigation?.applicationId ?? 'cn.toside.music.mobile'

export function focusTV(nativeID: string) {
  if (isTV) NativeModules.TVNavigation.focus(nativeID)
}

export interface RemoteInputSession { url: string, qr: string }

export async function startRemoteInput(title: string, onText: (text: string) => void) {
  const subscription = DeviceEventEmitter.addListener('tvRemoteInput', onText)
  try {
    const session: RemoteInputSession = await NativeModules.TVRemoteInput.start(title)
    return { session, stop: () => { subscription.remove(); NativeModules.TVRemoteInput.stop() } }
  } catch (err) {
    subscription.remove()
    throw err
  }
}
