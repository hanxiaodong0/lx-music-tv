import { useState } from 'react'
import { FlatList, Modal, StyleSheet, View } from 'react-native'
import Text from '@/components/common/Text'
import Button from '@/components/common/Button'
import FocusRoot from '@/components/tv/FocusRoot'
import TVSeekBar from '@/components/tv/SeekBar'
import { useTheme } from '@/store/theme/hook'
import { useIsPlay, usePlayerMusicInfo, useProgress, useStatusText } from '@/store/player/hook'
import { playListById, playNext, playPrev, togglePlay } from '@/core/player/player'
import { getListMusicSync } from '@/utils/listManage'
import playerState from '@/store/player/state'
import commonState from '@/store/common/state'
import { navigations } from '@/navigation'
import { toast } from '@/utils/tools'
import { useSettingValue } from '@/store/setting/hook'
import { updateSetting } from '@/core/common'

const playModes = [
  { id: 'listLoop', label: '列表循环' },
  { id: 'list', label: '顺序播放' },
  { id: 'random', label: '随机播放' },
  { id: 'singleLoop', label: '单曲循环' },
] as const

export default function PlayerControls({ showDetails = false }: { showDetails?: boolean }) {
  const theme = useTheme()
  const music = usePlayerMusicInfo()
  const playing = useIsPlay()
  const progress = useProgress()
  const status = useStatusText()
  const [queue, setQueue] = useState<{ id: string, songs: LX.Music.MusicInfo[] } | null>(null)
  const openQueue = () => {
    const id = playerState.playInfo.playerListId ?? playerState.playMusicInfo.listId
    if (!id) { toast('请先选择一首歌曲'); return }
    setQueue({ id, songs: [...getListMusicSync(id)] })
  }
  const seek = (value: number) => { global.app_event.setProgress(value) }
  const playMode = useSettingValue('player.togglePlayMethod')
  const playModeIndex = playModes.findIndex(mode => mode.id == playMode)
  const togglePlayMode = () => {
    const next = playModes[(playModeIndex + 1) % playModes.length]
    updateSetting({ 'player.togglePlayMethod': next.id })
    toast(next.label)
  }

  return <View style={[styles.panel, { borderTopColor: theme['c-border-background'] }]}>
    <View style={styles.summary}>
      <Text numberOfLines={1} style={styles.title} size={16}>{music.id ? `${music.name}  ·  ${music.singer}` : '选择一首歌曲，开始播放'}</Text>
      <Text numberOfLines={1} size={12} color={theme['c-500']} style={styles.status}>{status}</Text>
      <Text size={12}>{progress.nowPlayTimeStr} / {progress.maxPlayTimeStr}</Text>
    </View>
    <View style={styles.controls}>
      <Button accessibilityLabel="上一首" style={styles.button} onPress={() => { void playPrev() }}><Text>上一首</Text></Button>
      <Button nativeID="tv-play-toggle" accessibilityLabel={playing ? '暂停' : '播放'} style={styles.button} onPress={togglePlay}><Text>{playing ? '暂停' : '播放'}</Text></Button>
      <Button accessibilityLabel="下一首" style={styles.button} onPress={() => { void playNext() }}><Text>下一首</Text></Button>
      <View style={styles.seek}>
        <TVSeekBar value={progress.nowPlayTime} duration={progress.maxPlayTime} onSeek={seek} />
      </View>
      {showDetails && <Button style={styles.button} onPress={() => {
        if (!music.id) { toast('请先选择一首歌曲'); return }
        navigations.pushPlayDetailScreen(commonState.componentIds.home!, true)
      }}><Text>歌词</Text></Button>}
      <Button nativeID="tv-play-mode" accessibilityLabel="切换播放模式" style={styles.button} onPress={togglePlayMode}><Text>{playModes[playModeIndex]?.label ?? '列表循环'}</Text></Button>
      <Button style={styles.button} onPress={openQueue}><Text>播放列表</Text></Button>
    </View>
    <Modal visible={queue != null} transparent onRequestClose={() => { setQueue(null) }}>
      <FocusRoot style={styles.modal}>
        <View style={[styles.dialog, { backgroundColor: theme['c-content-background'] }]}>
          <View style={styles.summary}>
            <Text size={21} style={styles.title}>当前播放列表</Text>
            <Button style={styles.button} onPress={() => { setQueue(null) }}><Text>关闭</Text></Button>
          </View>
          <FlatList
            data={queue?.songs ?? []}
            keyExtractor={item => item.id}
            initialNumToRender={12}
            windowSize={7}
            removeClippedSubviews={false}
            getItemLayout={(_, index) => ({ length: 60, offset: index * 60, index })}
            renderItem={({ item }) => <Button nativeID={`tv-queue-${item.id}`} style={styles.song} onPress={() => {
              if (queue) void playListById(queue.id, item.id)
            }}><Text numberOfLines={1}>{item.name}</Text><Text numberOfLines={1} size={12} color={theme['c-500']}>{item.singer}</Text></Button>}
            ListEmptyComponent={<Text>当前列表为空</Text>}
          />
        </View>
      </FocusRoot>
    </Modal>
  </View>
}

const styles = StyleSheet.create({
  panel: { borderTopWidth: 1, paddingTop: 8, marginTop: 6 },
  summary: { flexDirection: 'row', alignItems: 'center', minHeight: 26, gap: 10 },
  title: { flex: 1 },
  status: { maxWidth: '25%' },
  controls: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  button: { minHeight: 44, minWidth: 64, paddingHorizontal: 12, justifyContent: 'center', alignItems: 'center', borderRadius: 6 },
  seek: { flex: 1, minWidth: 60, paddingHorizontal: 8 },
  modal: { backgroundColor: '#0009', justifyContent: 'center', alignItems: 'center' },
  dialog: { width: '80%', height: '80%', borderRadius: 12, padding: 18 },
  song: { height: 60, justifyContent: 'center', paddingHorizontal: 12, borderRadius: 6 },
})
