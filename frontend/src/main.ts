import { createApp } from 'vue'
import { pinia } from './stores'
import router from './router'
import App from './App.vue'
import { useUserStore } from './stores/user'

const app = createApp(App)
app.use(pinia)
app.use(router)

// 初始化用户状态
const userStore = useUserStore()
userStore.init()

app.mount('#app')
