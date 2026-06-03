<template>
  <div class="chat-room">
    <el-card class="chat-card" :body-style="{ padding: '0', display: 'flex', height: '600px' }">
      <div class="chat-sidebar">
        <div class="sidebar-header">
          <el-input
            v-model="searchKey"
            prefix-icon="el-icon-search"
            placeholder="搜索联系人"
            size="small"
            clearable
          />
        </div>
        <div class="session-list" v-loading="sessionLoading">
          <div
            v-for="s in filteredSessions"
            :key="s.userId"
            class="session-item"
            :class="{ active: activeSession === s.userId }"
            @click="selectSession(s)"
          >
            <el-badge :value="s.unread" :hidden="!s.unread" :max="99">
              <el-avatar :src="s.avatar" :size="42" icon="el-icon-user-solid" />
            </el-badge>
            <div class="session-info">
              <div class="session-top">
                <span class="session-name">{{ s.nickname }}</span>
                <span class="session-time">{{ formatTime(s.lastTime) }}</span>
              </div>
              <p class="session-msg">{{ s.lastMessage }}</p>
            </div>
          </div>
          <el-empty v-if="!filteredSessions.length" description="暂无会话" :image-size="60" />
        </div>
      </div>

      <div class="chat-main">
        <template v-if="activeSession">
          <div class="chat-header">
            <span>{{ activeSessionInfo.nickname }}</span>
          </div>
          <div ref="messageList" v-loading="messageLoading" class="message-list" @scroll="handleScroll">
            <div v-for="msg in messages" :key="msg.id" class="message-item" :class="{ 'self': msg.senderId === currentUserId }">
              <el-avatar :src="msg.senderId === currentUserId ? userAvatar : activeSessionInfo.avatar" :size="36" icon="el-icon-user-solid" />
              <div class="message-body">
                <div v-if="msg.type === 'IMAGE'" class="message-image">
                  <el-image :src="msg.content" fit="contain" :preview-src-list="[msg.content]" style="max-width:200px;max-height:200px;" />
                </div>
                <div v-else class="message-content">{{ msg.content }}</div>
                <span class="message-time">{{ formatTime(msg.createTime) }}</span>
              </div>
            </div>
          </div>
          <div class="chat-input">
            <el-input
              v-model="inputMsg"
              type="textarea"
              :rows="3"
              placeholder="输入消息..."
              resize="none"
              @keydown.enter.native.exact.prevent="sendMsg"
            />
            <div class="input-actions">
              <div>
                <el-upload
                  action="/api/file/upload"
                  :headers="uploadHeaders"
                  :show-file-list="false"
                  :on-success="handleImageUpload"
                  accept="image/*"
                  style="display:inline-block;margin-right:8px;"
                >
                  <el-button size="small" icon="el-icon-picture-outline" circle />
                </el-upload>
                <span class="tip">按 Enter 发送</span>
              </div>
              <el-button type="primary" size="small" :disabled="!inputMsg.trim()" @click="sendMsg">发送</el-button>
            </div>
          </div>
        </template>
        <div v-else class="chat-empty">
          <i class="el-icon-chat-dot-round" style="font-size:48px;color:#c0c4cc;" />
          <p>选择一个会话开始聊天</p>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { getChatSessions, getChatHistoryByUser } from '@/api/chat'
import request from '@/api/request'
import { connectWebSocket, sendMessage, disconnectWebSocket } from '@/utils/websocket'
import { mapState } from 'vuex'

export default {
  name: 'ChatRoom',
  data() {
    return {
      sessions: [],
      sessionLoading: false,
      messageLoading: false,
      activeSession: null,
      activeSessionInfo: {},
      messages: [],
      inputMsg: '',
      searchKey: ''
    }
  },
  computed: {
    ...mapState('user', ['user']),
    currentUserId() {
      return this.user ? this.user.id : null
    },
    userAvatar() {
      return this.user ? this.user.avatar : ''
    },
    uploadHeaders() {
      const { getToken } = require('@/utils/auth')
      return { Authorization: 'Bearer ' + getToken() }
    },
    filteredSessions() {
      if (!this.searchKey) return this.sessions
      const k = this.searchKey.toLowerCase()
      return this.sessions.filter(s => s.nickname.toLowerCase().includes(k))
    }
  },
  created() {
    this.fetchSessions()
    this.initWebSocket()
    if (this.$route.query.userId) {
      this.activeSession = Number(this.$route.query.userId)
    }
  },
  beforeDestroy() {
    disconnectWebSocket()
  },
  methods: {
    async fetchSessions() {
      this.sessionLoading = true
      try {
        const res = await getChatSessions()
        this.sessions = res.data || []
        if (this.activeSession && !this.sessions.find(s => s.userId === this.activeSession)) {
          this.sessions.unshift({
            sessionId: null,
            userId: this.activeSession,
            nickname: '用户' + this.activeSession,
            avatar: '',
            lastMessage: '',
            lastTime: null,
            unread: 0
          })
        }
        if (this.activeSession) {
          this.activeSessionInfo = this.sessions.find(s => s.userId === this.activeSession) || {}
          this.fetchMessages()
        }
      } catch (e) {
        this.$message.error('加载会话列表失败')
      } finally {
        this.sessionLoading = false
      }
    },
    async fetchMessages() {
      if (!this.activeSession) return
      this.messageLoading = true
      try {
        const res = await getChatHistoryByUser(this.activeSession)
        this.messages = (res.data && res.data.records) ? res.data.records : (res.data || [])
        this.$nextTick(() => this.scrollToBottom())
      } catch (e) {
        this.$message.error('加载聊天记录失败')
      } finally {
        this.messageLoading = false
      }
    },
    selectSession(s) {
      this.activeSession = s.userId
      this.activeSessionInfo = s
      s.unread = 0
      this.fetchMessages()
      // Mark messages from this sender as read
      request.post('/chat/read/' + s.userId).catch(() => {})
    },
    initWebSocket() {
      connectWebSocket((msg) => {
        if (msg.senderId === this.activeSession) {
          this.messages.push(msg)
          this.$nextTick(() => this.scrollToBottom())
        }
        const session = this.sessions.find(s => s.userId === msg.senderId)
        if (session) {
          session.lastMessage = msg.content
          session.lastTime = msg.createTime
          if (msg.senderId !== this.activeSession) {
            session.unread = (session.unread || 0) + 1
          }
        }
      })
    },
    handleImageUpload(res) {
      const url = typeof res.data === 'string' ? res.data : (res.data?.url || res.data)
      if (!url || !this.activeSession) return
      sendMessage(this.activeSession, url, 'IMAGE')
      this.messages.push({
        id: Date.now(),
        senderId: this.currentUserId,
        content: url,
        type: 'IMAGE',
        createTime: new Date().toISOString()
      })
      const session = this.sessions.find(s => s.userId === this.activeSession)
      if (session) {
        session.lastMessage = '[图片]'
        session.lastTime = new Date().toISOString()
      }
      this.$nextTick(() => this.scrollToBottom())
    },
    sendMsg() {
      if (!this.inputMsg.trim() || !this.activeSession) return
      sendMessage(this.activeSession, this.inputMsg.trim())
      this.messages.push({
        id: Date.now(),
        senderId: this.currentUserId,
        content: this.inputMsg.trim(),
        createTime: new Date().toISOString()
      })
      const session = this.sessions.find(s => s.userId === this.activeSession)
      if (session) {
        session.lastMessage = this.inputMsg.trim()
        session.lastTime = new Date().toISOString()
      }
      this.inputMsg = ''
      this.$nextTick(() => this.scrollToBottom())
    },
    scrollToBottom() {
      const el = this.$refs.messageList
      if (el) el.scrollTop = el.scrollHeight
    },
    handleScroll() {
      // optional: load history
    },
    formatTime(val) {
      if (!val) return ''
      const d = new Date(val)
      const now = new Date()
      if (d.toDateString() === now.toDateString()) {
        return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      }
      return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    }
  }
}
</script>

<style scoped>
.chat-room {
  padding-bottom: 20px;
}
.chat-card {
  border-radius: 8px;
  overflow: hidden;
}
.chat-sidebar {
  width: 280px;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.sidebar-header {
  padding: 12px;
  border-bottom: 1px solid #ebeef5;
}
.session-list {
  flex: 1;
  overflow-y: auto;
}
.session-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  cursor: pointer;
  transition: background 0.2s;
}
.session-item:hover {
  background: #f5f7fa;
}
.session-item.active {
  background: #ecf5ff;
}
.session-info {
  flex: 1;
  min-width: 0;
}
.session-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.session-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}
.session-time {
  font-size: 11px;
  color: #c0c4cc;
  flex-shrink: 0;
}
.session-msg {
  font-size: 12px;
  color: #909399;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.chat-header {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #fafafa;
}
.message-item {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
.message-item.self {
  flex-direction: row-reverse;
}
.message-body {
  max-width: 60%;
}
.message-content {
  background: #fff;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
  word-break: break-word;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.self .message-content {
  background: #409EFF;
  color: #fff;
}
.message-image {
  background: #fff;
  padding: 4px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.self .message-image {
  background: #ecf5ff;
}
.message-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 4px;
  display: block;
}
.self .message-time {
  text-align: right;
}
.chat-input {
  border-top: 1px solid #ebeef5;
  padding: 8px 16px 12px;
}
.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}
.tip {
  font-size: 12px;
  color: #c0c4cc;
}
.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}
.chat-empty p {
  margin-top: 12px;
  font-size: 14px;
}
</style>
