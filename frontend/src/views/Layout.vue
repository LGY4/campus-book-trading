<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-left">
        <span class="logo" @click="$router.push('/home')">校园二手书</span>
        <el-menu
          :default-active="activeMenu"
          mode="horizontal"
          router
          class="header-nav"
          background-color="transparent"
          text-color="#303133"
          active-text-color="#409EFF"
        >
          <el-menu-item index="/home">首页</el-menu-item>
          <el-menu-item index="/publish">发布书籍</el-menu-item>
          <el-menu-item index="/chat">
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
              消息
            </el-badge>
          </el-menu-item>
        </el-menu>
      </div>
      <div class="header-right">
        <el-dropdown trigger="click" @command="handleCommand">
          <span class="user-dropdown">
            <el-avatar :src="userAvatar" :size="32" icon="el-icon-user-solid" />
            <span class="username">{{ nickname }}</span>
            <i class="el-icon-arrow-down" />
          </span>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="profile">
              <i class="el-icon-user" /> 个人中心
            </el-dropdown-item>
            <el-dropdown-item command="my-orders">
              <i class="el-icon-tickets" /> 我的订单
            </el-dropdown-item>
            <el-dropdown-item command="my-books">
              <i class="el-icon-notebook-2" /> 我的发布
            </el-dropdown-item>
            <el-dropdown-item command="my-favorites">
              <i class="el-icon-star-off" /> 我的收藏
            </el-dropdown-item>
            <el-dropdown-item command="my-wants">
              <i class="el-icon-shopping-cart-2" /> 我想要的
            </el-dropdown-item>
            <el-dropdown-item command="my-footprint">
              <i class="el-icon-time" /> 浏览足迹
            </el-dropdown-item>
            <el-dropdown-item command="my-exchange">
              <i class="el-icon-sort" /> 交换记录
            </el-dropdown-item>
            <el-dropdown-item command="messages">
              <i class="el-icon-message" /> 消息中心
            </el-dropdown-item>
            <el-dropdown-item v-if="isAdmin" command="admin" divided>
              <i class="el-icon-setting" /> 管理后台
            </el-dropdown-item>
            <el-dropdown-item command="logout" divided>
              <i class="el-icon-switch-button" /> 退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
    </el-header>
    <el-main class="layout-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script>
import { mapState } from 'vuex'
import { disconnectWebSocket } from '@/utils/websocket'
import { getUnreadCount } from '@/api/message'

export default {
  name: 'Layout',
  data() {
    return {
      unreadCount: 0
    }
  },
  computed: {
    ...mapState('user', ['user']),
    activeMenu() {
      return this.$route.path
    },
    nickname() {
      return this.user ? (this.user.nickname || this.user.username) : ''
    },
    userAvatar() {
      return this.user ? this.user.avatar : ''
    },
    isAdmin() {
      return this.user && this.user.role === 'ADMIN'
    }
  },
  created() {
    this.fetchUnreadCount()
    this._unreadTimer = setInterval(this.fetchUnreadCount, 30000)
  },
  beforeDestroy() {
    if (this._unreadTimer) {
      clearInterval(this._unreadTimer)
    }
  },
  methods: {
    async fetchUnreadCount() {
      try {
        const res = await getUnreadCount()
        this.unreadCount = res.data || 0
      } catch (e) { /* noop */ }
    },
    handleCommand(cmd) {
      switch (cmd) {
        case 'profile':
          this.$router.push('/profile')
          break
        case 'my-orders':
          this.$router.push('/my-orders')
          break
        case 'my-books':
          this.$router.push('/my-books')
          break
        case 'my-favorites':
          this.$router.push('/my-favorites')
          break
        case 'my-exchange':
          this.$router.push('/my-exchange')
          break
        case 'my-wants':
          this.$router.push('/my-wants')
          break
        case 'my-footprint':
          this.$router.push('/my-footprint')
          break
        case 'messages':
          this.$router.push('/messages')
          break
        case 'admin':
          this.$router.push('/admin')
          break
        case 'logout':
          this.$confirm('确定退出登录？', '提示', {
            type: 'warning'
          }).then(() => {
            disconnectWebSocket()
            this.$store.dispatch('user/logout')
            this.$router.push('/login')
          }).catch(() => {})
          break
      }
    }
  }
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  background: #f5f7fa;
}
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 100;
  height: 60px;
}
.header-left {
  display: flex;
  align-items: center;
}
.logo {
  font-size: 20px;
  font-weight: 700;
  color: #409EFF;
  cursor: pointer;
  margin-right: 40px;
  white-space: nowrap;
}
.header-nav {
  border-bottom: none;
}
.header-nav .el-menu-item {
  height: 60px;
  line-height: 60px;
  font-size: 15px;
}
.header-right {
  display: flex;
  align-items: center;
}
.user-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #303133;
}
.username {
  margin: 0 8px;
  font-size: 14px;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.layout-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  width: 100%;
}
</style>
