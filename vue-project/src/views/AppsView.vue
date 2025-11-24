<script setup>
import { ref, onMounted } from 'vue'
import AppCard from '@/components/AppCard.vue'
import { getApps } from '@/data/getApps'

const list = ref([])
const loading = ref(false)
const error = ref('')
const sortBy = ref('')
const sortOrder = ref('DESC')

// 排序选项
const sortOptions = [
  { label: '默认排序', value: '' },
  { label: '评分', value: 'rating' },
  { label: '下载量', value: 'downloads' }
]

const sortOrderOptions = [
  { label: '从高到低', value: 'DESC' },
  { label: '从低到高', value: 'ASC' }
]

// 加载数据
async function loadApps() {
  loading.value = true
  error.value = ''
  try {
    list.value = await getApps(sortBy.value || null, sortOrder.value)
  } catch (e) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

// 排序改变时重新加载
function handleSortChange() {
  loadApps()
}

onMounted(() => {
  loadApps()
})
</script>

<template>
  <section class="min-h-dvh bg-slate-50 text-slate-800 antialiased">
    <div class="mx-auto max-w-screen-xl px-4 py-6 space-y-5">
      <header class="space-y-3">
        <div>
          <h2 class="text-lg font-medium text-slate-900">智能体应用列表</h2>
          <p class="text-sm text-slate-500">轻量卡片栅格，移动优先，自适应多端。</p>
        </div>
        
        <!-- 排序控件 -->
        <div class="flex flex-wrap items-center gap-3">
          <div class="flex items-center gap-2">
            <span class="text-sm text-slate-600">排序方式：</span>
            <el-select 
              v-model="sortBy" 
              placeholder="选择排序" 
              size="default"
              style="width: 140px"
              @change="handleSortChange"
            >
              <el-option
                v-for="item in sortOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>
          
          <div v-if="sortBy" class="flex items-center gap-2">
            <span class="text-sm text-slate-600">排序顺序：</span>
            <el-select 
              v-model="sortOrder" 
              size="default"
              style="width: 120px"
              @change="handleSortChange"
            >
              <el-option
                v-for="item in sortOrderOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>
        </div>
      </header>

      <!-- 加载态 -->
      <el-skeleton v-if="loading" animated :count="6">
        <template #template>
          <div class="grid gap-5 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3">
            <div class="rounded-2xl border border-slate-200 bg-white p-5">
              <el-skeleton-item variant="image" style="width:56px;height:56px;border-radius:12px" />
              <el-skeleton-item variant="h1" style="width:60%;margin-top:12px" />
              <el-skeleton-item variant="text" />
              <el-skeleton-item variant="text" style="width:80%" />
            </div>
          </div>
        </template>
      </el-skeleton>

      <!-- 错误态 -->
      <el-alert v-else-if="error" type="error" :title="error" show-icon />

      <!-- 空态 -->
      <el-empty v-else-if="!list.length" description="暂无应用" />

      <!-- 数据态 -->
      <div v-else class="grid gap-5 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3">
        <AppCard v-for="item in list" :key="item.id" :item="item" />
      </div>
    </div>
  </section>
</template>
