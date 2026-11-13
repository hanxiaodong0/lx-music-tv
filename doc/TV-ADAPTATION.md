# LX Music 小米电视适配方案

分析日期：2026-09-14。初次分析基于上游 `v1.8.4`（`d2956044533150057d65bfeaf2ae0fb5179433f5`）；按用户希望使用最新代码的要求，已对比并将 `lx-music-mobile/` 切换到本次获取的最新 `master`：`v1.9.0`，提交 `cd37a979a5845f1220b306b374285f5d38329e8d`（2026-09-12），本地开发分支 `feat/tv`。

v1.9.0 与 v1.8.4 相比，Android 构建脚本、Manifest、minSdk 21、compileSdk 36、targetSdk 29、ABI 配置和 RN 0.73.11 均未改变。部分依赖和业务代码已更新，因此目前只能确认源码配置未提高安装门槛，仍需检查构建后的合并 Manifest、原生库及真机运行。下文的架构与焦点问题分析仍适用。工具链正确解决编译问题；实际安装还取决于设备 API、ABI、APK 签名和安装策略，运行与遥控器体验另行验收。

本文保留静态源码分析与实施建议。现已在 `feat/tv` 分支实现第一版 TV 入口、横屏界面、原生焦点管理和播放操作，已完成签名测试 APK，位于 `artifacts/lx-music-tv-v1.9.0-tv.1-armeabi-v7a.apk`；具体操作见 `lx-music-mobile/TV-TESTING.md`。尚未连接小米电视测试。设备型号、Android 版本、实际播放能力待确认；已知用户可以安装并操作 v1.8.4 armeabi-v7a APK。

## 推荐路线

基于移动版复用搜索、歌单、自定义源、同步、播放和歌词业务，增加专用 TV 界面及焦点基础设施。先保持 React Native 0.73.11、Android API 21 最低版本、armeabi-v7a 和现有播放器依赖，做真机兼容验证，再决定是否引入 react-native-tvos。不要将升级整个 React Native 体系作为第一步。

同一仓库维护共享业务与手机/电视两套视图，电视构建使用独立 applicationId、名称和签名，便于与原版并存和回退。包名可以通过 Gradle product flavor 配置，不必整体重命名 Java 包。检查原生代码、Provider、深链接及更新逻辑中对包名的假设。电视版应使用自己的更新入口或关闭原版更新安装入口，避免安装回手机界面。数据通过明确的备份导入或同步迁移，不假设两个包共享私有数据。

## 源码发现

| 位置 | 发现及意义 |
| --- | --- |
| `package.json` | RN 0.73.11、React 18.2.0、react-native-navigation 7.39.2；多个原生依赖来自作者 fork，迁移 RN TV 需要兼容验证。 |
| `android/build.gradle`、`android/gradle.properties` | minSdk 21；支持 armeabi-v7a；Hermes 开启，新架构关闭。已安装 32 位 APK 不等于电视仅支持 32 位，但首版应保留该 ABI。 |
| `android/app/src/main/AndroidManifest.xml` | 没有 TV launcher、leanback feature 和 touchscreen 非必需声明。 |
| `android/app/src/main/java/cn/toside/music/mobile/MainActivity.java` | 继承 NavigationActivity，没有项目自定义遥控器按键处理。新增处理必须保留现有导航基类及默认事件分发。 |
| `src/screens/Home/index.tsx`、`src/screens/PlayDetail/index.tsx` | 已有横屏/竖屏分支，适合作为 TV 路由接入点；横屏判断不能代替 TV 判断。 |
| `src/screens/Home/Horizontal/Aside.tsx` | 当前栏目渲染为普通 View，其他栏目为 TouchableOpacity；焦点目标可能在选中后消失，需要保持同一可聚焦节点。图标旁文字被注释。 |
| `src/components/common/Button.tsx` | 封装 Pressable，仅处理点击涟漪和禁用透明度，没有统一焦点样式；ref 只暴露 measure。 |
| `src/components/OnlineList/ListItem.tsx`、`src/screens/Home/Views/Mylist/MusicList/ListItem.tsx` | 直接使用 TouchableOpacity，含长按和更多操作；选择、播放状态不能代替遥控器焦点。只改通用 Button 覆盖不到这些位置。 |
| `src/screens/Home/Views/Mylist/MusicList/List.tsx` | 虚拟列表启用 removeClippedSubviews，需验证滚动和卸载后的焦点连续性。 |
| `src/components/common/Modal.tsx`、`Menu.tsx`、`Dialog.tsx` | 有返回关闭和触摸背景关闭逻辑，缺少明确的初始焦点、弹窗焦点范围和关闭恢复策略。还需覆盖 Navigation overlay 弹窗。 |
| `src/navigation/components/PactModal.tsx` | 首次启动有协议滚动和倒计时按钮，必须纳入遥控器流程，不能只测已初始化的主页。 |
| `src/screens/PlayDetail/Horizontal/Player/ControlBtn.tsx` | 已调用 core 中的 playPrev、playNext、togglePlay，TV 控件可复用。 |
| `src/plugins/player/service.ts` | 已处理 RemotePlay、RemotePause、RemoteNext、RemotePrevious、RemoteSeek 等事件，应复用并避免与新增按键处理重复执行。 |
| `src/components/common/Slider.tsx` | 使用社区 Slider，未封装 TV 操作契约；不能单凭依赖推断实际遥控器体验。 |
| `src/utils/pixelRatio.ts` | 尺寸按手机基准缩放，TV 需要单独定义字体、行高和间距。 |

## 遥控器操作契约

主界面建议采用左侧带文字的导航、右侧内容、底部播放条。主页先以“我的歌单”为入口，提供搜索、播放队列和设置；更多功能按阶段加入。歌曲尽量用单列大行呈现，减少方向键判断负担。

| 区域 | 上下 | 左右 | 确认 | 返回 |
| --- | --- | --- | --- | --- |
| 左侧导航 | 移动栏目焦点 | 右进入当前栏目内容；左停留 | 切换栏目，节点保持可聚焦 | 根级交还系统退出 |
| 歌曲列表 | 移动上一/下一行并自动滚动 | 左回导航；右到本行“更多” | 播放歌曲 | 返回上一页；栏目根页回导航 |
| 本行更多按钮 | 移到相邻行的更多按钮 | 左回本行歌曲；右边界停留 | 打开操作菜单 | 回本行歌曲 |
| 播放控制条 | 上回内容最后焦点 | 移动上首、播放/暂停、下首、歌词、队列等按钮 | 执行当前按钮 | 回内容区 |
| 进度条 | 离开到上下相邻区域 | 小步前后定位，例如每次 10 秒 | 可选确认定位，需全程一致 | 取消未提交定位或离开 |
| 弹窗/操作菜单 | 移动菜单项 | 仅在弹窗内部按布局移动 | 执行操作 | 关闭并恢复触发它的焦点 |
| 搜索输入 | 遵循输入法导航 | 遵循输入法导航 | 输入/提交 | 先关闭输入法，再返回页面 |

内容底部提供可见的“播放控制”入口，保证长列表中无需滚到最后一首才能控制播放。焦点规则按可见布局确定；不要将所有页面的左右键全局绑定为切歌或快进。

焦点高亮应明显、稳定且不依赖播放状态：建议亮色描边、背景变化，必要时轻微缩放。播放中的歌曲另用播放图标标识。所有必要操作都必须能通过方向键、确认和返回完成；长按、菜单键、语音和媒体专用键只作附加入口。

## 焦点技术方案

1. 先制作兼容验证页：几个按钮、200 条虚拟列表、一个弹窗、一个输入框。测试焦点事件、确认键、长按连发、滚动、返回恢复和媒体按键。
2. RN 0.73.11 Android 源码已有 focusable、hasTVPreferredFocus、nextFocus* 基础能力，但不可假设所有普通组件都拥有最新版 RN TV 的事件和焦点 API。先验证实际版本。
3. 若原版 RN 缺少可靠焦点事件，可实现窄范围 Android 原生 TVFocusView：转发原生焦点变化、请求焦点命令及必要的方向键处理，由 TVFocusable 统一包装。普通方向导航优先交给 Android 原生焦点系统。
4. 另一方案是使用经过验证的 react-native-tvos 版本，利用 TVFocusGuideView 等机制。必须核对具体版本的 Android 最低版本、32 位原生库、React/RN 版本、NavigationActivity、播放器 fork 和其他原生模块兼容性；不要直接安装最新版并假设兼容。
5. 每个页面/区域记录语义焦点键，如 screen/listId/musicId/action；不以列表 index 或过期 native tag 作为长期标识。页面重新出现后，等目标完成布局再恢复；若歌曲被删除，回到合理邻项或栏目入口。
6. 虚拟列表需要先滚动、等待目标渲染，再请求焦点；处理 scrollToIndex 失败和列表数据更新。按需调整裁剪和渲染窗口，不能为解决焦点问题一次性渲染全部歌曲。
7. 弹窗打开时限定焦点范围、禁用背景交互；关闭后恢复触发项。隐藏页面和歌词文本默认不能抢焦点。对长按重复事件做有界处理，避免一次确认执行两次。
8. 媒体键沿用现有播放服务；仅在需要的控件上处理特殊方向键。不要同时由 MainActivity、全局 JS 监听和按钮 onPress 执行同一动作。

## TV 安装与设备适配

- 添加 LEANBACK_LAUNCHER 入口、TV banner、leanback 声明和 touchscreen required=false。保留普通 LAUNCHER 以兼容可能使用定制桌面的小米设备；实际桌面显示必须真机验证。
- TV 构建显式进入横屏 TV 界面。通用构建可结合系统 TV 类型识别与手动 TV 模式兜底，不以分辨率或横屏状态识别电视。
- 第一版输出 armeabi-v7a APK；核对电视 Android 版本及所有 .so 的 ABI。安装成功只能证明当前包可安装，播放、焦点和系统键仍需测试。
- 设置页包括自定义源导入/选择、同步连接、备份导入和基本播放设置。复用现有业务，但重新处理文件选择、路径输入和错误弹窗的焦点。
- 可以先提供 U 盘文件导入和适配电视输入法的输入框。手机扫码配置属于后续新增能力，不能当成现有同步服务已具备；也不假设歌单同步会自动同步自定义源脚本。
- 搜索先提供历史记录和系统输入法。语音功能依赖具体电视输入法/系统能力，不把语音设为必选前提。
- 字体、行高、图标和边距使用 TV 专用尺寸，在实际观看距离检验；兼顾 720p/1080p/4K 的逻辑尺寸、边缘裁切和低内存表现。

## 交付阶段与验收

第一阶段：设备兼容验证。原版构建复现；新增 TV 入口和焦点验证页；确认不提高这台电视的系统/ABI 门槛。

第二阶段：日常可用闭环。首次启动、源配置、歌单导入/同步、搜索、选歌、播放暂停、上下首、歌词、播放队列与返回，全程只用遥控器。

第三阶段：完善功能。榜单、在线歌单、收藏管理、批量操作、更完整设置和可选手机辅助输入。每个新增页面必须遵守同一焦点契约。

必须在小米真机完成：

- 冷启动始终有可见焦点，协议和初始化弹窗不阻塞操作。
- 连续按/长按方向键经过多屏列表，焦点不丢、不意外跳出，不重复播放。
- 搜索键盘关闭、菜单关闭、歌词返回后恢复合理焦点和滚动位置。
- 所有必需操作都有无需长按、鼠标或触摸的入口。
- 媒体键一次只执行一次，音量键仍由系统控制。
- 返回逐层退出，不反复在导航和内容之间循环。
- 首页切换、应用进入后台/恢复、电视待机/唤醒后行为符合设定；播放是否继续需明确验证。
- 安装新电视包与原版共存，正常播放，源错误/断网时仍能通过遥控器重试或返回。

测试 APK 已构建完成，并通过签名、架构、TV 入口和字节码静态检查，目前仍没有真机验证结果。模拟器和键盘方向键只能辅助验证，不能替代目标小米电视遥控器验收。

## 参考资料

- Android TV 应用入口和设备声明：https://developer.android.com/training/tv/get-started/create
- Android TV 导航、焦点与返回：https://developer.android.com/training/tv/get-started/navigation
- RN TV 能力说明（具体 API 随版本变化）：https://github.com/react-native-tvos/react-native-tvos
- 原版 RN 0.73.11 Android View 实现：https://github.com/facebook/react-native/blob/v0.73.11/packages/react-native/ReactAndroid/src/main/java/com/facebook/react/views/view/ReactViewManager.java
