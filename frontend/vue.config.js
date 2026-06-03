const fs = require('fs')
const path = require('path')

function getBackendPort() {
  try {
    const portFile = path.resolve(__dirname, '../backend/target/app.port')
    const port = fs.readFileSync(portFile, 'utf-8').trim()
    return parseInt(port, 10) || 8088
  } catch (e) {
    return 8088
  }
}

const backendPort = getBackendPort()
const backendTarget = `http://localhost:${backendPort}`
const wsTarget = `ws://localhost:${backendPort}`
const frontendPort = parseInt(process.env.FRONTEND_PORT, 10) || 8080

console.log(`[vue.config.js] Backend proxy target: ${backendTarget}`)
console.log(`[vue.config.js] Frontend port: ${frontendPort}`)

module.exports = {
  configureWebpack: {
    resolve: {
      alias: {
        'stompjs': 'stompjs/lib/stomp.js'
      }
    }
  },
  devServer: {
    port: frontendPort,
    proxy: {
      '/api': {
        target: backendTarget,
        changeOrigin: true
      },
      '/uploads': {
        target: backendTarget,
        changeOrigin: true
      },
      '/ws': {
        target: wsTarget,
        ws: true,
        changeOrigin: true
      }
    }
  }
}
