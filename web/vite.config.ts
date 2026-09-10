/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Single web app served on one dev server / one bundled build:
//   - "/"        → Farm / Retailer / Shipping / Guest portal
//   - "/admin/*" → Admin dashboard
// In production Spring Boot serves the built assets from the same origin.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
    css: false,
  },
})
