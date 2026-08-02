import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'ant-design-vue/dist/reset.css'
import '@/assets/styles/global.css'

import App from './App.vue'
import router from './router'
import { installPermissionDirective } from '@/directives/permission'
import { installAntDesignComponents } from '@/plugins/ant-design'

const app = createApp(App)

app.use(createPinia())
installAntDesignComponents(app)
app.use(router)
installPermissionDirective(app)

app.mount('#app')
