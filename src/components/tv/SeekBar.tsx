import { requireNativeComponent, type ViewProps, type NativeSyntheticEvent } from 'react-native'
interface NativeProps extends ViewProps {
  value: number
  duration: number
  onSeek: (event: NativeSyntheticEvent<{ value: number }>) => void
}
const NativeSeekBar = requireNativeComponent<NativeProps>('TVSeekBar')
export default function TVSeekBar({ value, duration, onSeek }: { value: number, duration: number, onSeek: (value: number) => void }) {
  return <NativeSeekBar style={{ height: 44, width: '100%' }} value={value} duration={duration} onSeek={event => { onSeek(event.nativeEvent.value) }} accessibilityLabel="播放进度" />
}
