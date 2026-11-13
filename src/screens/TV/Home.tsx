import { useEffect } from 'react'
import { ScrollView, View, StyleSheet } from 'react-native'
import Text from '@/components/common/Text'
import Button from '@/components/common/Button'
import { useTheme } from '@/store/theme/hook'
import { useNavActiveId } from '@/store/common/hook'
import { setNavActiveId } from '@/core/common'
import type { NAV_ID_Type } from '@/config/constant'
import Main from '@/screens/Home/Horizontal/Main'
import PlayerControls from './PlayerControls'
import { focusTV } from '@/utils/tv'

const menus: Array<{ id: NAV_ID_Type, label: string }> = [
  { id: 'nav_love', label: '我的歌单' },
  { id: 'nav_search', label: '搜索音乐' },
  { id: 'nav_songlist', label: '发现歌单' },
  { id: 'nav_top', label: '排行榜' },
  { id: 'nav_setting', label: '设置' },
]

export default function TVHome() {
  const theme = useTheme()
  const active = useNavActiveId()
  useEffect(() => { setNavActiveId('nav_love') }, [])

  return <View style={[styles.screen, { backgroundColor: theme['c-content-background'] }]}>
    <View style={styles.body}>
      <View nativeID="tv-navigation" collapsable={false} style={[styles.navigation, { borderRightColor: theme['c-border-background'] }]}>
        <Text size={23} style={styles.brand}>LX Music</Text>
        <Text size={12} color={theme['c-500']} style={styles.subtitle}>TV · 遥控器测试版</Text>
        <ScrollView>
          {menus.map(menu => <Button
            key={menu.id}
            nativeID={menu.id === active ? 'tv-nav-active' : `tv-nav-${menu.id}`}
            accessibilityLabel={menu.label}
            style={[styles.menu, menu.id === active && { backgroundColor: theme['c-primary-background-hover'] }]}
            onPress={() => { setNavActiveId(menu.id) }}
          ><Text size={17} color={menu.id === active ? theme['c-primary-font'] : theme['c-font']}>{menu.label}</Text></Button>)}
          <Button style={styles.menu} onPress={() => { focusTV('tv-play-toggle') }}><Text size={17}>播放控制</Text></Button>
        </ScrollView>
        <Text size={12} color={theme['c-500']} style={styles.help}>上下选择 · 确认打开{'\n'}向右进入内容{'\n'}返回键回到导航</Text>
      </View>
      <View style={styles.content}><Main /></View>
    </View>
    <PlayerControls showDetails />
  </View>
}

const styles = StyleSheet.create({
  screen: { flex: 1, padding: 12 },
  body: { flex: 1, flexDirection: 'row' },
  navigation: { width: 146, borderRightWidth: 1, paddingRight: 10 },
  brand: { paddingLeft: 10, paddingTop: 8, fontWeight: '700' },
  subtitle: { paddingLeft: 10, paddingTop: 4, paddingBottom: 16 },
  menu: { minHeight: 48, paddingHorizontal: 12, justifyContent: 'center', borderRadius: 8, marginVertical: 3 },
  content: { flex: 1, paddingLeft: 12 },
  help: { padding: 8, lineHeight: 21 },
})
