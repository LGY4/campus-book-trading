<template>
  <div class="chat-window">
    <div class="chat-header">
      <span class="target-name">{{ targetUser.nickname || targetUser.username || '用户' }}</span>
    </div>

    <div class="message-list" ref="messageList">
      <div
        v-for="(msg, index) in messages"
        :key="index"
        class="message-item"
        :class="{ 'message-self': msg.isSelf }"
      >
        <el-avatar
          :size="36"
          :src="msg.isSelf ? selfAvatar : targetUser.avatar"
          icon="el-icon-user-solid"
          class="msg-avatar"
        />
        <div class="msg-bubble" :class="{ 'bubble-self': msg.isSelf, 'bubble-other': !msg.isSelf }">
          <div class="msg-content">{{ msg.content }}</div>
          <div class="msg-time">{{ formatTime(msg.createTime) }}</div>
        </div>
      </div>
      <div v-if="messages.length === 0" class="empty-chat">
        <span>暂无消息，开始聊天吧</span>
      </div>
    </div>

    <div class="chat-input">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="2"
        placeholder="输入消息..."
        resize="none"
        @keydown.enter.native.exact.prevent="sendMessage"
      />
      <el-button
        type="primary"
        size="small"
        :disabled="!inputText.trim()"
        @click="sendMessage"
      >发送</el-button>
    </div>
  </div>
</template>

<script>
import { getChatHistory } from '@/api/chat'

export default {
  name: 'ChatWindow',
  props: {
    sessionId: { type: [String, Number], required: true },
    targetUser: { type: Object, default: () => ({}) }
  },
  data() {
    return {
      messages: [],
      inputText: '',
      ws: null
    }
  },
  computed: {
    selfAvatar() {
      const user = this.$store.state.user && this.$store.state.user.user
      return user && user.avatar
    }
  },
  watch: {
    sessionId: {
      immediate: true,
      handler() {
        this.loadHistory()
        this.connectWs()
      }
    }
  },
  beforeDestroy() {
    this.closeWs()
  },
  methods: {
    async loadHistory() {
      this.messages = []
      try {
        const res = await getChatHistory(this.sessionId, { page: 1, size: 100 })
        const records = (res.data && res.data.records) || res.data || []
        const userId = this.$store.state.user.user && this.$store.state.user.user.id
        this.messages = records.map(msg => ({
          content: msg.content,
          isSelf: msg.senderId === userId,
          createTime: msg.createTime
        }))
        this.$nextTick(() => this.scrollToBottom())
      } catch (e) { this.$message && this.$message.error && this.$message.error('加载聊天记录失败') }
    },
    connectWs() {
      this.closeWs()
      const token = (this.$store.state.user && this.$store.state.user.token) || ''
      const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
      const host = window.location.host
      this.ws = new WebSocket(`${protocol}://${host}/ws/chat?token=${token}&sessionId=${this.sessionId}`)
      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          this.messages.push(data)
          this.$nextTick(() => this.scrollToBottom())
        } catch {}
      }
      this.ws.onclose = () => {
        // Reconnect after delay
        setTimeout(() => {
          if (this.sessionId) this.connectWs()
        }, 3000)
      }
    },
    closeWs() {
      if (this.ws) {
        this.ws.close()
        this.ws = null
      }
    },
    sendMessage() {
      const text = this.inputText.trim()
      if (!text) return
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({
          sessionId: this.sessionId,
          content: text,
          receiverId: this.targetUser.id
        }))
      }
      this.messages.push({
        content: text,
        isSelf: true,
        createTime: new Date().toISOString()
      })
      this.inputText = ''
      this.$nextTick(() => this.scrollToBottom())
    },
    scrollToBottom() {
      const el = this.$refs.messageList
      if (el) el.scrollTop = el.scrollHeight
    },
    formatTime(time) {
      if (!time) return ''
      const d = new Date(time)
      const pad = n => String(n).padStart(2, '0')
      return `${pad(d.getHours())}:${pad(d.getMinutes())}`
    }
  }
}
</script>

<style scoped>
.chat-window {
  display: flex;
  flex-direction: column;
  height: 100%;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.chat-header {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  background: #fafafa;
}
.target-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f5f5;
}
.message-item {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
.message-item.message-self {
  flex-direction: row-reverse;
}
.msg-avatar {
  flex-shrink: 0;
}
.msg-bubble {
  max-width: 60%;
  padding: 10px 14px;
  border-radius: 12px;
  position: relative;
}
.bubble-self {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.bubble-other {
  background: #fff;
  color: #303133;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}
.msg-content {
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}
.msg-time {
  font-size: 11px;
  margin-top: 4px;
  opacity: 0.7;
}
.bubble-self .msg-time {
  text-align: right;
}
.empty-chat {
  text-align: center;
  color: #c0c4cc;
  padding: 40px 0;
  font-size: 14px;
}
.chat-input {
  padding: 12px 16px;
  border-top: 1px solid #ebeef5;
  display: flex;
  gap: 10px;
  align-items: flex-end;
  background: #fafafa;
}
.chat-input .el-textarea {
  flex: 1;
}
</style>
