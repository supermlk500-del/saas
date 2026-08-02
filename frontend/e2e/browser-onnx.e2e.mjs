import fs from 'node:fs'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'
import { chromium } from 'playwright-core'

const currentDir = path.dirname(fileURLToPath(import.meta.url))
const projectDir = path.resolve(currentDir, '..')
const baseUrl = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:5173'
const username = process.env.E2E_USERNAME ?? 'admin'
const password = process.env.E2E_PASSWORD
const imagePath = path.resolve(
  projectDir,
  process.env.E2E_IMAGE_PATH ?? '../photo/upload/20260524/source_20260524133631281_6eddfd90.jpg',
)
const disableWebGpu = process.env.E2E_DISABLE_WEBGPU === '1'
const operationTimeout = Number(process.env.E2E_OPERATION_TIMEOUT_MS ?? 180_000)

if (!password) {
  throw new Error('E2E_PASSWORD is required')
}
if (!fs.existsSync(imagePath)) {
  throw new Error(`E2E image does not exist: ${imagePath}`)
}

const chromeCandidates = [
  process.env.E2E_CHROME_PATH,
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
  `${process.env.LOCALAPPDATA ?? ''}/Google/Chrome/Application/chrome.exe`,
].filter(Boolean)
const executablePath = chromeCandidates.find((candidate) => fs.existsSync(candidate))
if (!executablePath) {
  throw new Error('Chrome executable was not found; set E2E_CHROME_PATH')
}

const browser = await chromium.launch({
  executablePath,
  headless: true,
  args: [
    '--use-fake-ui-for-media-stream',
    '--use-fake-device-for-media-stream',
    ...(disableWebGpu ? ['--disable-webgpu', '--disable-features=WebGPU'] : []),
  ],
})

const context = await browser.newContext({
  viewport: { width: 1600, height: 1000 },
})
if (disableWebGpu) {
  await context.addInitScript(() => {
    localStorage.setItem('zhihuitong.browserInference.forceWasm', 'true')
  })
}
await context.grantPermissions(['camera'], { origin: new URL(baseUrl).origin })
const page = await context.newPage()
page.setDefaultTimeout(30_000)

const pageErrors = []
const consoleErrors = []
const consoleMessages = []
const modelResponses = []
const apiRequests = []
const apiResponses = []
const runtimeResponses = []
const requestStartedAt = new Map()
let clientEventRequests = 0
page.on('pageerror', (error) => pageErrors.push(error.message))
page.on('console', (message) => {
  consoleMessages.push({ type: message.type(), text: message.text() })
  if (message.type() === 'error') {
    consoleErrors.push(message.text())
  }
})
page.on('response', (response) => {
  const url = response.url()
  if (url.includes('/ort/')) {
    runtimeResponses.push({
      url,
      status: response.status(),
      durationMs: Date.now() - (requestStartedAt.get(response.request()) ?? Date.now()),
    })
  }
  if (url.includes('/prod-api/')) {
    apiResponses.push({ method: response.request().method(), url, status: response.status() })
  }
  if (url.includes('/browser-inference/model')) {
    modelResponses.push(response.status())
  }
})
page.on('request', (request) => {
  requestStartedAt.set(request, Date.now())
  if (request.url().includes('/prod-api/')) {
    apiRequests.push({ method: request.method(), url: request.url() })
  }
  if (request.url().includes('/client-events')) {
    clientEventRequests += 1
  }
})

const readDescriptionValue = async (label) => {
  const labelCell = page.locator('.ant-descriptions-item-label').filter({ hasText: label }).last()
  await labelCell.waitFor()
  return (await labelCell.locator('xpath=following-sibling::*[1]').textContent())?.trim() ?? ''
}

const openSettings = async () => {
  const dialog = page.locator('.ant-modal').filter({ hasText: '实时质检设置' })
  if (!(await dialog.isVisible().catch(() => false))) {
    await page.getByRole('button', { name: '设置' }).click()
    await dialog.waitFor()
  }
}

const closeSettings = async () => {
  const dialog = page.locator('.ant-modal').filter({ hasText: '实时质检设置' })
  if (await dialog.isVisible().catch(() => false)) {
    await dialog.locator('.ant-modal-close').click()
    await dialog.waitFor({ state: 'hidden' })
  }
}

const runOfflineDetection = async () => {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/api/qc-records/client-detect-results')
      && response.request().method() === 'POST',
    { timeout: operationTimeout },
  )
  await page.getByRole('button', { name: '上传并检测' }).click()
  const response = await responsePromise
  if (response.status() !== 200) {
    throw new Error(`Offline result persistence failed with HTTP ${response.status()}: ${await response.text()}`)
  }
  await page.getByText(/浏览器质检完成/).waitFor({ timeout: operationTimeout })
}

try {
  await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' })
  await page.locator('input[autocomplete="username"]').fill(username)
  await page.locator('input[autocomplete="current-password"]').fill(password)
  await Promise.all([
    page.waitForURL((url) => !url.pathname.startsWith('/login'), { timeout: 30_000 }),
    page.getByRole('button', { name: '登录系统' }).click(),
  ])

  await page.goto(`${baseUrl}/quality/realtime`, { waitUntil: 'networkidle' })
  await page.getByText('单张图片质检', { exact: true }).waitFor()
  const isolation = await page.evaluate(() => ({
    secureContext: window.isSecureContext,
    crossOriginIsolated: window.crossOriginIsolated,
  }))
  if (!isolation.secureContext || !isolation.crossOriginIsolated) {
    throw new Error(`Browser isolation is not active: ${JSON.stringify(isolation)}`)
  }

  await openSettings()
  await page.locator('.ant-select-selection-item').filter({ hasText: 'AI 布面瑕疵检测' }).waitFor()
  await closeSettings()

  await page.locator('input[type="file"]').setInputFiles(imagePath)
  const selectedFileState = await page.locator('input[type="file"]').evaluate((input) => {
    const file = input.files?.[0]
    return file ? { name: file.name, type: file.type, size: file.size } : null
  })
  if (!selectedFileState || selectedFileState.name !== path.basename(imagePath)) {
    throw new Error(`Image selection did not reach the page: ${JSON.stringify(selectedFileState)}`)
  }
  await page.getByText(selectedFileState.name, { exact: true }).waitFor()
  await runOfflineDetection()
  if (!modelResponses.includes(200)) {
    throw new Error(`Model was not downloaded successfully: ${JSON.stringify(modelResponses)}`)
  }

  await openSettings()
  const provider = await readDescriptionValue('实际 Provider')
  const modelVersion = await readDescriptionValue('模型版本')
  if (!['WebGPU', 'WASM'].includes(provider)) {
    throw new Error(`Unexpected provider: ${provider}`)
  }
  if (disableWebGpu && provider !== 'WASM') {
    throw new Error(`WASM fallback was requested but provider is ${provider}`)
  }
  if (!/^[a-f0-9]{12}$/i.test(modelVersion)) {
    throw new Error(`Unexpected model version: ${modelVersion}`)
  }
  await closeSettings()

  const sessionResponsePromise = page.waitForResponse(
    (response) =>
      /\/api\/qc-stream-sessions$/.test(new URL(response.url()).pathname)
      && response.request().method() === 'POST',
  )
  await page.getByRole('button', { name: '开始检测' }).click()
  const sessionResponse = await sessionResponsePromise
  if (sessionResponse.status() !== 200) {
    throw new Error(`Realtime session creation failed with HTTP ${sessionResponse.status()}`)
  }
  await page.waitForFunction(() => {
    const video = document.querySelector('video')
    return Boolean(video?.srcObject && video.readyState >= HTMLMediaElement.HAVE_CURRENT_DATA)
  })

  await openSettings()
  await page.waitForFunction(() => {
    const labels = Array.from(document.querySelectorAll('.ant-descriptions-item-label'))
    const label = labels.find((item) => item.textContent?.trim() === '实时消息数')
    const value = label?.nextElementSibling?.textContent?.trim()
    return Number(value) > 0
  }, null, { timeout: 180_000 })
  const messagesBeforeConcurrentImage = Number(await readDescriptionValue('实时消息数'))
  await closeSettings()

  await runOfflineDetection()
  await page.waitForFunction(
    (previousCount) => {
      const labels = Array.from(document.querySelectorAll('.ant-descriptions-item-label'))
      const label = labels.find((item) => item.textContent?.trim() === '实时消息数')
      const value = label?.nextElementSibling?.textContent?.trim()
      return Number(value) > previousCount
    },
    messagesBeforeConcurrentImage,
    { timeout: 180_000 },
  ).catch(async () => {
    await openSettings()
    await page.waitForFunction(
      (previousCount) => {
        const labels = Array.from(document.querySelectorAll('.ant-descriptions-item-label'))
        const label = labels.find((item) => item.textContent?.trim() === '实时消息数')
        const value = label?.nextElementSibling?.textContent?.trim()
        return Number(value) > previousCount
      },
      messagesBeforeConcurrentImage,
      { timeout: 180_000 },
    )
  })
  await closeSettings()

  const closeResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/api/qc-stream-sessions/')
      && response.url().endsWith('/close')
      && response.request().method() === 'POST',
  )
  await page.getByRole('button', { name: '停止检测' }).click()
  const closeResponse = await closeResponsePromise
  if (closeResponse.status() !== 200) {
    throw new Error(`Realtime session close failed with HTTP ${closeResponse.status()}`)
  }
  await page.waitForFunction(() => {
    const video = document.querySelector('video')
    return !video?.srcObject
  })

  if (pageErrors.length || consoleErrors.length) {
    throw new Error(`Browser errors detected: ${JSON.stringify({ pageErrors, consoleErrors })}`)
  }

  console.log(JSON.stringify({
    status: 'passed',
    provider,
    modelVersion,
    isolation,
    modelResponses,
    clientEventRequests,
    concurrentImageWhileRealtime: true,
    cameraReleasedAfterStop: true,
  }, null, 2))
} catch (error) {
  const artifactDir = path.resolve(projectDir, 'test-results')
  fs.mkdirSync(artifactDir, { recursive: true })
  await openSettings().catch(() => undefined)
  await page.screenshot({
    path: path.join(artifactDir, 'browser-onnx-failure.png'),
    fullPage: true,
  }).catch(() => undefined)
  const visibleText = await page.locator('body').innerText().catch(() => '')
  const localInferenceState = await page.locator('.ant-descriptions-item')
    .filter({ hasText: '本地推理状态' })
    .last()
    .textContent()
    .catch(() => '')
  console.error(JSON.stringify({
    url: page.url(),
    error: error instanceof Error ? error.message : String(error),
    pageErrors,
    consoleErrors,
    apiRequests: apiRequests.slice(-30),
    apiResponses: apiResponses.slice(-30),
    runtimeResponses,
    consoleMessages: consoleMessages.slice(-50),
    localInferenceState,
    visibleText: visibleText.slice(-3000),
  }, null, 2))
  throw error
} finally {
  await context.close()
  await browser.close()
}
