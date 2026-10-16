import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import packageJson from './package.json'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

// Automatically update sitemap.xml <lastmod> date on each build / deployment
function autoUpdateSitemap() {
  const updateFile = (filePath) => {
    if (fs.existsSync(filePath)) {
      const today = new Date().toISOString().split('T')[0]
      const content = fs.readFileSync(filePath, 'utf-8')
      const updated = content.replace(/<lastmod>.*?<\/lastmod>/g, `<lastmod>${today}</lastmod>`)
      fs.writeFileSync(filePath, updated, 'utf-8')
    }
  }

  return {
    name: 'auto-update-sitemap',
    buildStart() {
      updateFile(path.resolve(__dirname, 'public/sitemap.xml'))
    },
    closeBundle() {
      updateFile(path.resolve(__dirname, 'dist/sitemap.xml'))
    }
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    autoUpdateSitemap(),
    {
      name: 'html-transform-version',
      transformIndexHtml(html) {
        return html.replace(/__APP_VERSION__/g, packageJson.version)
      }
    }
  ],
  build: {
    target: 'esnext',
    cssTarget: ['chrome90', 'safari15', 'firefox90', 'edge90'],
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/@firebase/firestore') || id.includes('node_modules/firebase/firestore')) {
            return 'vendor-firestore'
          }
          if (id.includes('node_modules/@firebase/auth') || id.includes('node_modules/firebase/auth')) {
            return 'vendor-auth'
          }
          if (id.includes('node_modules/@firebase/') || id.includes('node_modules/firebase/')) {
            return 'vendor-firebase-core'
          }
          if (id.includes('node_modules/react/') || id.includes('node_modules/react-dom/')) {
            return 'vendor-react'
          }
        }
      }
    }
  },
  server: {
    host: true,
    allowedHosts: 'all'
  }
})
