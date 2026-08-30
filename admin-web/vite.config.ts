import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Khi được Spring Boot phục vụ (đóng gói chung port 8080), app nằm ở /admin/.
  // Khi dev riêng (vite), vẫn truy cập qua http://localhost:5173/admin/.
  base: '/admin/',
})
