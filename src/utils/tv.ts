import { NativeModules } from 'react-native'

// A build flag also works on Xiaomi firmware that does not report UI_MODE_TYPE_TELEVISION.
export const isTV = NativeModules.TVNavigation?.enabled === true
export const appId: string = NativeModules.TVNavigation?.applicationId ?? 'cn.toside.music.mobile'

export function focusTV(nativeID: string) {
  if (isTV) NativeModules.TVNavigation.focus(nativeID)
}
