// 菜单相关 API
import { get } from '../utils/request'

export interface Menu {
  id: number
  title: string
  path: string
  children?: Menu[]
}

// 获取菜单列表
export const getMenus = async (): Promise<Menu[]> => {
  return get<Menu[]>('/v1/menus')
}

