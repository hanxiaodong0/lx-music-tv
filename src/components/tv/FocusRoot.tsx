import { requireNativeComponent, View, type ViewProps } from 'react-native'
import { isTV } from '@/utils/tv'

interface Props extends ViewProps {
  backFocusID?: string
}

const NativeRoot = isTV ? requireNativeComponent<Props>('TVFocusRoot') : View

export default function FocusRoot({ style, ...props }: Props) {
  return <NativeRoot {...props} style={[{ flex: 1 }, style]} collapsable={false} />
}

