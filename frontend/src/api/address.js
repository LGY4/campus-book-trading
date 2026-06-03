import request from './request'

export function getAddressList() {
  return request.get('/user/address/list')
}

export function deleteAddress(id) {
  return request.delete(`/user/address/${id}`)
}

export function setDefaultAddress(id) {
  return request.put(`/user/address/default/${id}`)
}

export function saveAddress(data) {
  if (data.id) {
    return request.put('/user/address/update', data)
  }
  return request.post('/user/address/create', data)
}
