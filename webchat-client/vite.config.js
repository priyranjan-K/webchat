import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:9876',
        changeOrigin: true,
        // No path rewrite — /api/auth/signup → http://localhost:9876/api/auth/signup
      },
      '/ws': {
        target: 'ws://localhost:9876',
        ws: true,
        changeOrigin: true,
      }
    }
  }
})