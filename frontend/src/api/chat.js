import request from './request'

export function getChatSessions() {
  return request.get('/chat/sessions')
}

export function getChatHistory(sessionId, params) {
  return request.get('/chat/history/' + sessionId, { params })
}

export function getChatHistoryByUser(userId, params) {
  return request.get('/chat/history/user/' + userId, { params })
}
