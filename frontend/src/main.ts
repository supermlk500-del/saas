import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import '@/assets/styles/global.css'

import App from './App.vue'
import router from './router'
import { installPermissionDirective } from '@/directives/permission'

const app = createApp(App)

app.use(createPinia())
app.use(Antd)
app.use(router)
installPermissionDirective(app)

app.mount('#app')
