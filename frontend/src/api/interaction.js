import request from './request'

export function toggleFavorite(bookId) {
  return request.post('/interaction/favorite/' + bookId)
}

export function getFavorites(params) {
  return request.get('/interaction/favorite/list', { params })
}

export function getFootprint(params) {
  return request.get('/interaction/footprint', { params })
}

export function deleteFootprint(id) {
  return request.delete('/interaction/footprint/' + id)
}

export function clearFootprint() {
  return request.delete('/interaction/footprint/clear')
}

export function toggleWant(bookId) {
  return request.post('/interaction/want/' + bookId)
}

export function getWants(params) {
  return request.get('/interaction/want/list', { params })
}
