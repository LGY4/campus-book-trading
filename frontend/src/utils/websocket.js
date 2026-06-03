import { getToken } from './auth'

let ws = null
let messageHandler = null
let reconnectTimer = null
let reconnectAttempts = 0
const MAX_RECONNECT = 5

export function connectWebSocket(onMessage) {
  messageHandler = onMessage
  reconnectAttempts = 0
  doConnect()
}

function doConnect() {
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) return

  const token = getToken()
  if (!token) return

  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const host = window.location.host
  ws = new WebSocket(`${protocol}://${host}/ws/chat?token=${token}`)

  ws.onopen = () => {
    reconnectAttempts = 0
  }

  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)
      if (messageHandler) messageHandler(msg)
    } catch (e) { /* ignore non-JSON */ }
  }

  ws.onclose = () => {
    if (reconnectAttempts < MAX_RECONNECT) {
      reconnectTimer = setTimeout(() => {
        reconnectAttempts++
        doConnect()
      }, 3000)
    }
  }

  ws.onerror = () => {
    ws.close()
  }
}

export function sendMessage(receiverId, content, type) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    const msg = { receiverId, content }
    if (type) msg.type = type
    ws.send(JSON.stringify(msg))
  }
}

export function disconnectWebSocket() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  reconnectAttempts = MAX_RECONNECT
  if (ws) {
    ws.close()
    ws = null
  }
}
