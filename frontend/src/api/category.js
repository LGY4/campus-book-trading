import request from './request'

export function getCategories() {
  return request.get('/category/list')
}

export function createCategory(data) {
  return request.post('/category/create', data)
}

export function updateCategory(data) {
  return request.put('/category/update', data)
}

export function deleteCategory(id) {
  return request.delete('/category/' + id)
}
