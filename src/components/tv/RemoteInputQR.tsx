import { useEffect, useRef, useState } from 'react'
import { Image, StyleSheet, View } from 'react-native'
import Text from '@/components/common/Text'
import { useTheme } from '@/store/theme/hook'
import { startRemoteInput, type RemoteInputSession } from '@/utils/tv'

export default function RemoteInputQR({ title, onText }: { title: string, onText: (text: string) => void }) {
  const theme = useTheme()
  const [session, setSession] = useState<RemoteInputSession | null>(null)
  const [error, setError] = useState('')
  const onTextRef = useRef(onText)
  onTextRef.current = onText

  useEffect(() => {
    let stop: (() => void) | null = null
    let cancelled = false
    startRemoteInput(title, text => { onTextRef.current(text) }).then(result => {
      if (cancelled) { result.stop(); return }
      stop = result.stop
      setSession(result.session)
    }).catch((err: Error) => { if (!cancelled) setError(err.message || '扫码输入启动失败') })
    return () => { cancelled = true; stop?.() }
  }, [title])

  return <View style={styles.box}>
    {session
      ? <Image source={{ uri: session.qr }} style={styles.qr} />
      : <View style={[styles.qr, { backgroundColor: theme['c-primary-background-hover'] }]} />}
    <View style={styles.tip}>
      <Text size={14}>手机扫码输入</Text>
      <Text size={12} color={theme['c-600']} style={styles.line}>{error || '手机连接同一 Wi-Fi，扫码后粘贴链接并发送，电视会自动填入并打开'}</Text>
      {session ? <Text size={11} color={theme['c-500']} style={styles.line} selectable>{session.url}</Text> : null}
    </View>
  </View>
}

const styles = StyleSheet.create({
  box: { flexDirection: 'row', alignItems: 'center', marginTop: 15 },
  qr: { width: 150, height: 150, borderRadius: 4 },
  tip: { flex: 1, paddingLeft: 15 },
  line: { marginTop: 6 },
})
