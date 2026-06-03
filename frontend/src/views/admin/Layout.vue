<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="admin-aside">
      <div class="admin-logo">
        <span>管理后台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item index="/admin/dashboard">
          <i class="el-icon-data-line"></i>
          <span>数据大盘</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <i class="el-icon-user"></i>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/books">
          <i class="el-icon-reading"></i>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <i class="el-icon-tickets"></i>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/disputes">
          <i class="el-icon-warning-outline"></i>
          <span>纠纷处理</span>
        </el-menu-item>
        <el-menu-item index="/admin/comments">
          <i class="el-icon-chat-dot-round"></i>
          <span>评论管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/categories">
          <i class="el-icon-menu"></i>
          <span>分类管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/banners">
          <i class="el-icon-picture-outline"></i>
          <span>轮播管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/messages">
          <i class="el-icon-message"></i>
          <span>消息管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/logs">
          <i class="el-icon-document"></i>
          <span>操作日志</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-header">
        <div class="header-left">
          <h2>管理后台</h2>
        </div>
        <div class="header-right">
          <router-link to="/" class="home-link">
            <i class="el-icon-s-home"></i> 首页
          </router-link>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <i class="el-icon-user-solid"></i>
              {{ username }}
              <i class="el-icon-arrow-down"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
export default {
  name: 'AdminLayout',
  computed: {
    activeMenu() {
      return this.$route.path
    },
    username() {
      try {
        const user = this.$store.state.user.user
        return (user && user.nickname) || (user && user.username) || '管理员'
      } catch (e) {
        return '管理员'
      }
    }
  },
  methods: {
    handleCommand(cmd) {
      if (cmd === 'logout') {
        this.$confirm('确定退出登录？', '提示', { type: 'warning' }).then(() => {
          this.$store.dispatch('user/logout').then(() => {
            this.$router.push('/login')
          })
        }).catch(() => {})
      }
    }
  }
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}
.admin-aside {
  background-color: #304156;
  overflow-y: auto;
}
.admin-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}
.admin-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e6e6e6;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}
.header-left h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}
.home-link {
  color: #606266;
  text-decoration: none;
  font-size: 14px;
}
.home-link:hover {
  color: #409eff;
}
.user-info {
  cursor: pointer;
  color: #606266;
  font-size: 14px;
}
.admin-main {
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
