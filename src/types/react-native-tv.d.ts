import 'react-native'

// These props are forwarded to Animated.View by RN 0.73's Touchable implementations,
// but are missing from the accompanying TouchableWithoutFeedback TS interface.
declare module 'react-native' {
  interface TouchableWithoutFeedbackProps {
    nativeID?: string
    focusable?: boolean
  }
}
