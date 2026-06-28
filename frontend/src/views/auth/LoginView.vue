<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { LockOutlined, SafetyCertificateOutlined, UserOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { getCaptcha, login } from '@/api/system/auth'
import { useAuthStore } from '@/stores/auth'
import { useRoute, useRouter } from 'vue-router'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const captchaImage = ref('')
const captchaEnabled = ref(true)
const form = reactive({ username: '', password: '', captchaUuid: '', captchaCode: '' })

const refreshCaptcha = async () => {
  try {
    const response = await getCaptcha()
    captchaEnabled.value = response.data.enabled
    captchaImage.value = response.data.image
    form.captchaUuid = response.data.uuid
    form.captchaCode = ''
  } catch {
    captchaImage.value = ''
  }
}

const submit = async () => {
  if (loading.value) return
  if (!form.username.trim()) {
    message.warning('请输入用户名')
    return
  }
  if (!form.password) {
    message.warning('请输入密码')
    return
  }
  if (captchaEnabled.value && !form.captchaCode.trim()) {
    message.warning('请输入验证码')
    return
  }

  loading.value = true
  try {
    const response = await login({ ...form, username: form.username.trim(), captchaCode: form.captchaCode.trim() })
    auth.setToken(response.data.token)
    await auth.initialize()
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } catch {
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(refreshCaptcha)
</script>

<template>
  <main class="login-page">
    <section class="login-context" aria-label="系统说明">
      <img src="/images/branding/logo.png" alt="织慧通" class="brand-logo" />
      <div>
        <h1>织慧通</h1>
        <p>AI胚布排产质检系统</p>
      </div>
      <div class="process-line" aria-hidden="true">
        <span>来料</span><i></i><span>工艺</span><i></i><span>排产</span><i></i><span>质检</span>
      </div>
    </section>

    <section class="login-panel">
      <div class="panel-heading">
        <SafetyCertificateOutlined />
        <div><h2>账号登录</h2><p>仅限管理员创建的内部账号</p></div>
      </div>
      <a-form layout="vertical" @keydown.enter.prevent="submit">
        <a-form-item label="用户名" required>
          <a-input v-model:value="form.username" size="large" autocomplete="username" placeholder="请输入用户名">
            <template #prefix><UserOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item label="密码" required>
          <a-input-password v-model:value="form.password" size="large" autocomplete="current-password" placeholder="请输入密码">
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>
        <a-form-item v-if="captchaEnabled" label="验证码" required>
          <div class="captcha-row">
            <a-input v-model:value="form.captchaCode" size="large" :maxlength="4" placeholder="验证码" />
            <button class="captcha-image" type="button" title="刷新验证码" @click="refreshCaptcha">
              <img v-if="captchaImage" :src="captchaImage" alt="验证码，点击刷新" />
              <span v-else>刷新</span>
            </button>
          </div>
        </a-form-item>
        <a-button type="primary" html-type="button" size="large" block :loading="loading" @click="submit">登录系统</a-button>
      </a-form>
      <p class="security-note">登录状态由 Redis 会话校验，权限变更后立即生效。</p>
    </section>
  </main>
</template>

<style scoped>
.login-page { min-height: 100vh; display: grid; grid-template-columns: minmax(320px, 1fr) minmax(360px, 460px); align-items: stretch; background: #f4f6f8; color: #253043; }
.login-context { position: relative; display: flex; flex-direction: column; justify-content: center; padding: clamp(48px, 8vw, 120px); overflow: hidden; background: #172235; color: #fff; }
.login-context::after { content: ''; position: absolute; inset: 0; opacity: .17; background: repeating-linear-gradient(135deg, transparent 0 32px, #fff 33px 34px); }
.brand-logo { position: relative; z-index: 1; width: 112px; height: 112px; object-fit: contain; margin-bottom: 24px; }
h1, h2, p { margin: 0; }
h1 { position: relative; z-index: 1; font-size: clamp(42px, 5vw, 72px); line-height: 1; font-weight: 800; }
.login-context p { position: relative; z-index: 1; margin-top: 12px; font-size: 20px; color: #c8d2df; }
.process-line { position: relative; z-index: 1; display: flex; align-items: center; gap: 12px; margin-top: 64px; color: #f3a45d; font-weight: 600; }
.process-line i { width: 42px; height: 1px; background: #64748b; }
.login-panel { align-self: center; margin: 42px; padding: 40px; background: #fff; border: 1px solid #dfe4ea; border-radius: 8px; box-shadow: 0 20px 50px rgba(35, 48, 67, .1); }
.panel-heading { display: flex; gap: 14px; align-items: center; margin-bottom: 30px; }
.panel-heading > span { font-size: 30px; color: #d66f22; }
.panel-heading h2 { font-size: 26px; }
.panel-heading p { margin-top: 4px; color: #778294; }
.captcha-row { display: grid; grid-template-columns: 1fr 126px; gap: 10px; }
.captcha-image { height: 40px; padding: 0; overflow: hidden; background: #eef2f5; border: 1px solid #d9dfe7; border-radius: 6px; cursor: pointer; }
.captcha-image img { width: 100%; height: 100%; object-fit: cover; }
.security-note { margin-top: 22px; color: #8a94a3; font-size: 12px; text-align: center; }
:deep(.ant-btn-primary) { background: #d66f22; }
@media (max-width: 760px) { .login-page { grid-template-columns: 1fr; } .login-context { min-height: 220px; padding: 34px; justify-content: flex-start; } .brand-logo { width: 64px; height: 64px; margin-bottom: 12px; } h1 { font-size: 34px; } .login-context p { font-size: 15px; } .process-line { margin-top: 28px; } .login-panel { margin: 22px; padding: 28px; } }
@media (prefers-reduced-motion: reduce) { * { scroll-behavior: auto !important; transition: none !important; } }
</style>