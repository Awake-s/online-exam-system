// @ts-check
import { defineConfig } from 'astro/config'
import tailwindcss from '@tailwindcss/vite'

// ====================================================================
// 在线考试系统 · 项目落地页（Astro 5 + Tailwind 4）
// --------------------------------------------------------------------
// 部署说明：
//   - GitHub Pages 子路径：base 设为 '/online-exam-system/landing/'
//   - Vercel / Netlify 独立部署：site 改为你的域名，base 删除或设为 '/'
// ====================================================================
export default defineConfig({
  site: 'https://awake-s.github.io',
  base: '/online-exam-system/landing/',
  vite: {
    plugins: [tailwindcss()]
  }
})
