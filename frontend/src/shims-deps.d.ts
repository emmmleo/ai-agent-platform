// 仅用于本地IDE在未安装node_modules时的类型兜底（不影响Docker镜像内真实依赖构建）

declare module 'vue' {
  export type Ref<T = any> = any
  export function createApp(...args: any[]): any
  export function ref<T = any>(value?: T): Ref<T>
  export function reactive<T extends object>(target: T): T
  export function computed<T = any>(getter: any): any
  export function onMounted(cb: any): void
  export function nextTick(cb?: any): Promise<void>
  export function watch(...args: any[]): any
  export function defineComponent(options: any): any
}

declare module 'vue-router' {
  export type RouteRecordRaw = any
  export type Router = any
  export type RouteLocationNormalizedLoaded = any
  export function createRouter(options: any): Router
  export function createWebHistory(base?: string): any
  export function useRouter(): any
  export function useRoute(): RouteLocationNormalizedLoaded
}

declare module 'pinia' {
  export function defineStore(id: string, options: any): any
  export function createPinia(): any
}
