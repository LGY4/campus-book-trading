import request from './request'

export function getBooks(params) {
  return request.get('/book/list', { params })
}

export function getBookDetail(id) {
  return request.get('/book/detail/' + id)
}

export function publishBook(data) {
  return request.post('/book/publish', data)
}

export function updateBook(data) {
  return request.put('/book/update', data)
}

export function deleteBook(id) {
  return request.delete('/book/' + id)
}

export function getMyBooks(params) {
  return request.get('/book/my', { params })
}

export function delistBook(id) {
  return request.put('/book/delist/' + id)
}

export function batchDeleteBooks(ids) {
  return request.post('/book/batch-delete', ids)
}

export function searchBooks(params) {
  return request.get('/book/search', { params })
}

export function getRecommendations(limit) {
  return request.get('/book/recommend', { params: { limit } })
}

export function getBanners() {
  return request.get('/banner/list')
}
