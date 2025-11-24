import { apps as mock } from '@/mock/apps'
import { http, USE_MOCK } from '@/utils/http'

function normalize(app) {
  return {
    ...app,
    price: Number(app.price ?? 0),
    rating: Number(app.rating ?? 0),
    downloads: Number(app.downloads ?? 0),
    reviews: Number(app.reviews ?? 0),
    // publishedAt 后端为 "YYYY-MM-DD"，目前仅展示，保留字符串即可
  }
}

export async function getApps(sortBy = null, sortOrder = 'DESC') {
  if (USE_MOCK) {
    let result = [...mock]
    // Mock 数据也支持排序
    if (sortBy === 'rating' || sortBy === 'downloads') {
      result.sort((a, b) => {
        const valA = a[sortBy] ?? 0
        const valB = b[sortBy] ?? 0
        return sortOrder === 'DESC' ? valB - valA : valA - valB
      })
    }
    return result.map(normalize)
  }

  // 构建查询参数
  const params = {}
  if (sortBy) params.sortBy = sortBy
  if (sortOrder) params.sortOrder = sortOrder

  // 获取数据
  const response = await http.get('/apps', { params })
  const list = response.data.data
  return (Array.isArray(list) ? list : []).map(normalize)
}
