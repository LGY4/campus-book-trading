import request from './request'

export function getDashboard(days) {
  return request.get('/admin/dashboard/data', { params: { days } })
}

export function getAdminUsers(params) {
  return request.get('/admin/user/list', { params })
}

export function changeUserStatus(id, status) {
  return request.put('/admin/user/status/' + id, { status })
}

export function changeUserRole(id, role) {
  return request.put('/admin/user/role/' + id, { role })
}

export function getAdminBooks(params) {
  return request.get('/admin/book/list', { params })
}

export function changeBookStatus(id, status) {
  return request.put('/admin/book/status/' + id, { status })
}

export function getAdminOrders(params) {
  return request.get('/admin/order/list', { params })
}

export function getDisputes(params) {
  return request.get('/admin/order/disputes', { params })
}

export function resolveDispute(data) {
  return request.post('/admin/order/resolve-dispute', data)
}

export function getAdminComments(params) {
  return request.get('/admin/comment/list', { params })
}

export function getAdminMessages(params) {
  return request.get('/admin/message/list', { params })
}

export function getLogs(params) {
  return request.get('/admin/log/list', { params })
}

export function sendMessage(data) {
  return request.post('/admin/message/send', data)
}

export function broadcastMessage(data) {
  return request.post('/admin/message/broadcast', data)
}

export function deleteMessage(id) {
  return request.delete('/admin/message/' + id)
}

export function searchUsers(keyword) {
  return request.get('/admin/user/search', { params: { keyword } })
}

export function deleteComment(id) {
  return request.delete('/admin/comment/' + id)
}

export function approveBook(id) {
  return request.post('/admin/book/approve/' + id)
}

export function rejectBook(id, reason) {
  return request.post('/admin/book/reject/' + id, { reason })
}

function downloadCsv(url, filename, params) {
  return request.get(url, {
    params,
    responseType: 'blob'
  }).then(res => {
    if (res.data && res.data.type === 'application/json') {
      return res.data.text().then(text => {
        const json = JSON.parse(text)
        throw new Error(json.message || '导出失败')
      })
    }
    const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = filename
    a.click()
    URL.revokeObjectURL(a.href)
  })
}

export function exportBooks(params) {
  return downloadCsv('/admin/book/export', 'books.csv', params)
}

export function exportOrders(params) {
  return downloadCsv('/admin/order/export', 'orders.csv', params)
}

export function exportUsers(params) {
  return downloadCsv('/admin/user/export', 'users.csv', params)
}

export function getAdminBanners(params) {
  return request.get('/admin/banner/list', { params })
}

export function createBanner(data) {
  return request.post('/admin/banner/create', data)
}

export function updateBanner(data) {
  return request.put('/admin/banner/update', data)
}

export function deleteBanner(id) {
  return request.delete('/admin/banner/' + id)
}

export function changeBannerStatus(id, status) {
  return request.put('/admin/banner/status/' + id, { status })
}
