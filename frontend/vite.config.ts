import { fileURLToPath, URL } from 'node:url'
import fs from 'node:fs'
import type { IncomingMessage, ServerResponse } from 'node:http'
import path from 'node:path'

import { defineConfig } from 'vite'
import type { Plugin, ViteDevServer } from 'vite'
import vue from '@vitejs/plugin-vue'

const copyOrtRuntimeAssets = (): Plugin => ({
  name: 'copy-ort-runtime-assets',
  configureServer(server: ViteDevServer) {
    const sourceDir = path.resolve(__dirname, 'node_modules/onnxruntime-web/dist')
    server.middlewares.use('/ort', (request: IncomingMessage, response: ServerResponse, next: () => void) => {
      const rawUrl = request.url?.split('?')[0]?.replace(/^\/+/, '')
      if (!rawUrl) {
        next()
        return
      }
      const filePath = path.resolve(sourceDir, rawUrl)
      if (!filePath.startsWith(sourceDir) || !fs.existsSync(filePath)) {
        next()
        return
      }
      if (filePath.endsWith('.wasm')) {
        response.setHeader('Content-Type', 'application/wasm')
      } else if (filePath.endsWith('.mjs')) {
        response.setHeader('Content-Type', 'application/javascript')
      }
      fs.createReadStream(filePath).pipe(response)
    })
  },
  writeBundle() {
    const sourceDir = path.resolve(__dirname, 'node_modules/onnxruntime-web/dist')
    const targetDir = path.resolve(__dirname, 'dist/ort')
    fs.mkdirSync(targetDir, { recursive: true })
    for (const fileName of [
      'ort-wasm-simd-threaded.asyncify.mjs',
      'ort-wasm-simd-threaded.asyncify.wasm',
      'ort-wasm-simd-threaded.jsep.mjs',
      'ort-wasm-simd-threaded.jsep.wasm',
    ]) {
      fs.copyFileSync(path.join(sourceDir, fileName), path.join(targetDir, fileName))
    }
  },
})

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    copyOrtRuntimeAssets(),
  ],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return undefined
          }

          if (/[\\/]node_modules[\\/](vue|@vue|vue-router|pinia)[\\/]/.test(id)) {
            return 'vue-vendor'
          }

          if (/[\\/]node_modules[\\/](@ant-design|ant-design-vue)[\\/]/.test(id)) {
            return 'antd-vendor'
          }

          if (/[\\/]node_modules[\\/]axios[\\/]/.test(id)) {
            return 'http-vendor'
          }

          return 'vendor'
        },
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
    conditions: ['onnxruntime-web-use-extern-wasm'],
  },
  server: {
    headers: {
      'Cross-Origin-Opener-Policy': 'same-origin',
      'Cross-Origin-Embedder-Policy': 'require-corp',
      'Cross-Origin-Resource-Policy': 'same-origin',
    },
    proxy: {
      '/prod-api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
        rewrite: (path) => path.replace(/^\/prod-api/, ''),
      },
    },
  },
})
