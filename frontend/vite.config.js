import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    watch: {
      // 🔥 db.json 파일이 바뀌어도 개발 서버가 새로고침을 하지 않도록 제외합니다.
      ignored: ['**/db.json']
    },
    // 로컬 개발: API 상대경로 요청을 백엔드(localhost:8080)로 전달
    // (배포 시엔 nginx가 동일 경로를 backend-service로 프록시)
    proxy: {
      '/books': 'http://localhost:8080',
      '/reviews': 'http://localhost:8080',
      '/auth': 'http://localhost:8080',
      '/admin': 'http://localhost:8080'
    }
  }
})
