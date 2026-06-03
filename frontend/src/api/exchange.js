import request from './request'

export function createExchange(data) {
  return request.post('/exchange/create', data)
}

export function acceptExchange(id) {
  return request.put('/exchange/accept/' + id)
}

export function rejectExchange(id) {
  return request.put('/exchange/reject/' + id)
}

export function completeExchange(id) {
  return request.put('/exchange/complete/' + id)
}

export function getMyExchanges(params) {
  return request.get('/exchange/my', { params })
}

export function cancelExchange(id) {
  return request.put('/exchange/cancel/' + id)
}
