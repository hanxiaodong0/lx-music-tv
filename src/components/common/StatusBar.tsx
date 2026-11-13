import { useTheme } from '@/store/theme/hook'
import { StatusBar as RNStatusBar } from 'react-native'
import { isTV } from '@/utils/tv'

const StatusBar = function() {
  const theme = useTheme()
  const statusBarStyle = theme.isDark ? 'light-content' : 'dark-content'
  return <RNStatusBar hidden={isTV} backgroundColor="rgba(0,0,0,0)" barStyle={statusBarStyle} translucent={true} />
}

StatusBar.currentHeight = RNStatusBar.currentHeight ?? 0
StatusBar.setBarStyle = RNStatusBar.setBarStyle

export default StatusBar
