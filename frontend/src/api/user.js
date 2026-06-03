import request from './request'

export function getProfile() {
  return request.get('/user/profile')
}

export const getUserInfo = getProfile

export function updateProfile(data) {
  return request.put('/user/profile', data)
}

export const updateUserInfo = updateProfile

export function changePassword(data) {
  return request.put('/user/password', data)
}
