import request from './request'

export function createOrder(data) {
  return request.post('/order/create', data)
}

export function payOrder(orderId) {
  return request.post('/order/pay/' + orderId)
}

export function shipOrder(data) {
  return request.post('/order/ship', data)
}

export function confirmReceive(orderId) {
  return request.post('/order/confirm/' + orderId)
}

export function cancelOrder(orderId) {
  return request.post('/order/cancel/' + orderId)
}

export function requestRefund(data) {
  return request.post('/order/refund', data)
}

export function confirmRefund(orderId) {
  return request.post('/order/confirm-refund/' + orderId)
}

export function rejectRefund(orderId) {
  return request.post('/order/reject-refund/' + orderId)
}

export function submitReturnShipping(data) {
  return request.post('/order/return-shipping', data)
}

export function confirmReturnReceive(orderId) {
  return request.post('/order/confirm-return/' + orderId)
}

export function disputeOrder(data) {
  return request.post('/order/dispute', data)
}

export function getMyBuyOrders(params) {
  return request.get('/order/my-buy', { params })
}

export function getMySellOrders(params) {
  return request.get('/order/my-sell', { params })
}

export function getOrderDetail(id) {
  return request.get('/order/detail/' + id)
}

export function getCompletedOrdersByBook(bookId) {
  return request.get('/order/completed-by-book/' + bookId)
}
