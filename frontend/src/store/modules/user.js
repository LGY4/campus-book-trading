import { getToken, setToken, removeToken, getUser, setUser } from '@/utils/auth'
import { getProfile } from '@/api/user'

export default {
  namespaced: true,
  state: {
    token: getToken() || '',
    user: getUser() || null
  },
  mutations: {
    SET_TOKEN(state, { token, remember = true }) {
      state.token = token
      setToken(token, remember)
    },
    SET_USER(state, { user, remember = true }) {
      state.user = user
      setUser(user, remember)
    },
    CLEAR_ALL(state) {
      state.token = ''
      state.user = null
      removeToken()
    }
  },
  actions: {
    login({ commit }, { token, user, remember = true }) {
      commit('SET_TOKEN', { token, remember })
      commit('SET_USER', { user, remember })
    },
    logout({ commit }) {
      commit('CLEAR_ALL')
    },
    async refreshUser({ commit }) {
      try {
        const res = await getProfile()
        commit('SET_USER', { user: res.data })
      } catch (e) { /* noop */ }
    }
  }
}
