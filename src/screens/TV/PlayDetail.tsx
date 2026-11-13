import { useEffect } from 'react'
import { View, StyleSheet } from 'react-native'
import Text from '@/components/common/Text'
import Button from '@/components/common/Button'
import Image from '@/components/common/Image'
import { useTheme } from '@/store/theme/hook'
import { usePlayerMusicInfo } from '@/store/player/hook'
import { screenkeepAwake, screenUnkeepAwake } from '@/utils/nativeModules/utils'
import { Navigation } from 'react-native-navigation'
import Lyric from '@/screens/PlayDetail/Horizontal/Lyric'
import PlayerControls from './PlayerControls'

export default function TVPlayDetail({ componentId }: { componentId: string }) {
  const theme = useTheme()
  const music = usePlayerMusicInfo()
  useEffect(() => {
    screenkeepAwake()
    return () => { screenUnkeepAwake() }
  }, [])
  return <View style={[styles.screen, { backgroundColor: theme['c-content-background'] }]}>
    <View style={styles.header}>
      <Text size={20} style={{ flex: 1 }}>正在播放</Text>
      <Button style={styles.back} onPress={() => { void Navigation.pop(componentId) }}><Text>返回歌单</Text></Button>
    </View>
    <View style={styles.body}>
      <View style={styles.info}>
        {music.pic ? <Image url={music.pic} style={styles.cover} /> : <View style={[styles.cover, { backgroundColor: theme['c-primary-background-hover'] }]} />}
        <Text size={24} numberOfLines={2} style={styles.name}>{music.name || '暂无歌曲'}</Text>
        <Text size={17} color={theme['c-500']} numberOfLines={2}>{music.singer}</Text>
      </View>
      <View nativeID="tv-display-only" collapsable={false} style={styles.lyric}><Lyric /></View>
    </View>
    <PlayerControls />
  </View>
}
const styles = StyleSheet.create({
  screen: { flex: 1, padding: 18 },
  header: { flexDirection: 'row', alignItems: 'center' },
  back: { minHeight: 44, paddingHorizontal: 16, justifyContent: 'center' },
  body: { flex: 1, flexDirection: 'row', paddingVertical: 12 },
  info: { width: '35%', alignItems: 'center', justifyContent: 'center', paddingHorizontal: 16 },
  cover: { width: '65%', aspectRatio: 1, borderRadius: 10 },
  name: { textAlign: 'center', marginTop: 12, marginBottom: 8 },
  lyric: { flex: 1, marginLeft: 12 },
})
