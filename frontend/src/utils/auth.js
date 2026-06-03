const TOKEN_KEY = 'book_trading_token'
const USER_KEY = 'book_trading_user'

function getStorage() {
  return sessionStorage.getItem(TOKEN_KEY) !== null ? sessionStorage : localStorage
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY)
}

export function setToken(token, remember = true) {
  const storage = remember ? localStorage : sessionStorage
  storage.setItem(TOKEN_KEY, token)
  // Clear the other storage to avoid conflicts
  const other = remember ? sessionStorage : localStorage
  other.removeItem(TOKEN_KEY)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
}

export function getUser() {
  const user = localStorage.getItem(USER_KEY) || sessionStorage.getItem(USER_KEY)
  return user ? JSON.parse(user) : null
}

export function setUser(user, remember = true) {
  const storage = remember ? localStorage : sessionStorage
  storage.setItem(USER_KEY, JSON.stringify(user))
  const other = remember ? sessionStorage : localStorage
  other.removeItem(USER_KEY)
}
