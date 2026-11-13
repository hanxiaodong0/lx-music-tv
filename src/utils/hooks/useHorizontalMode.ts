import { useWindowSize } from '@/utils/hooks'
import { isHorizontalMode } from '../tools'
import { isTV } from '../tv'


export default () => {
  const windowSize = useWindowSize()

  return isTV || isHorizontalMode(windowSize.width, windowSize.height)
}
