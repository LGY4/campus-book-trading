import request from './request'

export function createComment(data) {
  return request.post('/comment/create', data)
}

export function getBookComments(bookId, params) {
  return request.get('/comment/book/' + bookId, { params })
}
