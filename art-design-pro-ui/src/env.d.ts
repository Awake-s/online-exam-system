/// <reference types="vite/client" />

declare module 'nprogress'

declare module 'crypto-js'

declare module 'vue-img-cutter'

declare module 'file-saver'

// xgplayer: art-design-pro 模板自带视频播放器，无官方 @types 声明
// 本项目 `<ArtVideoPlayer>` 组件通过 unplugin-vue-components 被动注册
// 但业务代码未引用，此 shim 仅为保证 vue-tsc 通过；若未来真正使用请引入官方类型
// 导出为 class 以便 `ref<Player | null>()` 同时作为值（构造函数）和类型使用
declare module 'xgplayer' {
  export default class Player {
    constructor(options?: any)
    on(event: string, callback?: (...args: any[]) => void): void
    destroy(): void
    [key: string]: any
  }
}
declare module 'xgplayer/dist/index.min.css'

declare module 'qrcode.vue' {
  export type Level = 'L' | 'M' | 'Q' | 'H'
  export type RenderAs = 'canvas' | 'svg'
  export type GradientType = 'linear' | 'radial'
  export interface ImageSettings {
    src: string
    height: number
    width: number
    excavate: boolean
  }
  export interface QRCodeProps {
    value: string
    size?: number
    level?: Level
    background?: string
    foreground?: string
    renderAs?: RenderAs
  }
  const QrcodeVue: any
  export default QrcodeVue
}

// 全局变量声明
declare const __APP_VERSION__: string // 版本号
