<template>
  <div class="message-center">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>消息中心</h2>
          <div class="header-actions">
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
              <el-button size="small" :disabled="unreadCount === 0" @click="handleMarkAllRead">全部已读</el-button>
            </el-badge>
          </div>
        </div>
      </template>

      <div class="filter-bar">
        <el-radio-group v-model="typeFilter" size="small" @change="handleFilterChange">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="SYSTEM">系统</el-radio-button>
          <el-radio-button label="TRADE">交易</el-radio-button>
        </el-radio-group>
      </div>

      <div v-if="messages.length" v-loading="loading" class="message-list">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-item"
          :class="{ unread: !msg.isRead }"
          @click="handleRead(msg)"
        >
          <div class="msg-header">
            <div class="msg-header-left">
              <el-tag :type="msgTypeTag(msg.type)" size="mini">{{ msgTypeText(msg.type) }}</el-tag>
              <span class="msg-time">{{ msg.createTime }}</span>
            </div>
            <el-button
              type="text"
              size="small"
              class="delete-btn"
              icon="el-icon-delete"
              @click.stop="handleDelete(msg)"
            />
          </div>
          <p class="msg-content">{{ msg.content }}</p>
        </div>
      </div>
      <el-empty v-else description="暂无消息" />
      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="pageSize"
          :current-page.sync="page"
          @current-change="fetchMessages"
        />
      </div>
    </el-card>
  </div>
</template>

<script>
import { getMessages, markAsRead, markAllAsRead, deleteMessage, getUnreadCount } from '@/api/message'

export default {
  name: 'MessageCenter',
  data() {
    return {
      messages: [],
      loading: false,
      page: 1,
      pageSize: 20,
      total: 0,
      unreadCount: 0,
      typeFilter: ''
    }
  },
  created() {
    this.fetchMessages()
    this.fetchUnreadCount()
  },
  methods: {
    async fetchMessages() {
      this.loading = true
      try {
        const params = { page: this.page, size: this.pageSize }
        if (this.typeFilter) params.type = this.typeFilter
        const res = await getMessages(params)
        this.messages = res.data.records || []
        this.total = res.data.total || 0
      } catch (e) {
        this.$message.error('加载消息失败')
      } finally {
        this.loading = false
      }
    },
    async fetchUnreadCount() {
      try {
        const res = await getUnreadCount()
        this.unreadCount = res.data || 0
      } catch (e) { /* noop */ }
    },
    async handleRead(msg) {
      if (!msg.isRead) {
        try {
          await markAsRead(msg.id)
          msg.isRead = 1
          this.unreadCount = Math.max(0, this.unreadCount - 1)
        } catch (e) {
          this.$message.error('标记已读失败')
        }
      }
    },
    async handleMarkAllRead() {
      try {
        await markAllAsRead()
        this.messages.forEach(m => { m.isRead = 1 })
        this.unreadCount = 0
        this.$message.success('已全部标记为已读')
      } catch (e) {
        this.$message.error('操作失败')
      }
    },
    handleDelete(msg) {
      this.$confirm('确定删除此消息？', '提示', { type: 'warning' }).then(async () => {
        try {
          await deleteMessage(msg.id)
          this.messages = this.messages.filter(m => m.id !== msg.id)
          this.total--
          if (!msg.isRead) this.unreadCount = Math.max(0, this.unreadCount - 1)
          this.$message.success('已删除')
        } catch (e) {
          this.$message.error('删除失败')
        }
      }).catch(() => {})
    },
    handleFilterChange() {
      this.page = 1
      this.fetchMessages()
    },
    msgTypeText(t) {
      const map = { SYSTEM: '系统', TRADE: '交易', CHAT: '聊天' }
      return map[t] || t
    },
    msgTypeTag(t) {
      const map = { SYSTEM: 'danger', TRADE: 'warning', CHAT: '' }
      return map[t] || 'info'
    }
  }
}
</script>

<style scoped>
.message-center { max-width: 800px; margin: 0 auto; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.card-header h2 { margin: 0; font-size: 20px; }
.filter-bar { margin-bottom: 16px; }
.message-list { display: flex; flex-direction: column; gap: 8px; }
.message-item { padding: 16px; border: 1px solid #ebeef5; border-radius: 8px; cursor: pointer; transition: border-color 0.2s; }
.message-item:hover { border-color: #409EFF; }
.message-item.unread { background: #f0f9ff; border-color: #b3d8ff; }
.msg-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.msg-header-left { display: flex; align-items: center; gap: 8px; }
.msg-time { font-size: 12px; color: #c0c4cc; }
.msg-content { margin: 0; font-size: 14px; color: #303133; line-height: 1.6; }
.delete-btn { color: #c0c4cc; padding: 0; }
.delete-btn:hover { color: #f56c6c; }
.pagination-wrap { text-align: center; margin-top: 20px; }
</style>
