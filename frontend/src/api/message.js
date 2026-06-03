import request from './request'

export function getMessages(params) {
  return request.get('/message/list', { params })
}

export function markAsRead(id) {
  return request.put('/message/read/' + id)
}

export function markAllAsRead() {
  return request.put('/message/read-all')
}

export function deleteMessage(id) {
  return request.delete('/message/' + id)
}

export function getUnreadCount() {
  return request.get('/message/unread-count')
}
