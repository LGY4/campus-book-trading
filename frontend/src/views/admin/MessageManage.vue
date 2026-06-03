<template>
  <div class="message-manage">
    <el-card shadow="never">
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索消息内容"
          clearable
          style="width: 200px"
          @keyup.enter.native="handleSearch"
        >
          <el-button slot="append" icon="el-icon-search" @click="handleSearch" />
        </el-input>
        <el-select v-model="msgType" placeholder="消息类型" clearable style="width: 150px; margin-left: 10px" @change="handleSearch">
          <el-option label="系统通知" value="SYSTEM" />
          <el-option label="交易消息" value="TRADE" />
          <el-option label="私信" value="CHAT" />
        </el-select>
        <el-button type="primary" icon="el-icon-edit" style="margin-left: 10px" @click="openSendDialog">发送消息</el-button>
        <el-button type="warning" icon="el-icon-bell" @click="openBroadcastDialog">广播通知</el-button>
      </div>

      <el-table :data="messages" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="senderName" label="发送者" min-width="100">
          <template slot-scope="{ row }">
            {{ row.senderName || '系统' }}
          </template>
        </el-table-column>
        <el-table-column prop="receiverName" label="接收者" min-width="100" />
        <el-table-column prop="content" label="内容" min-width="250" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template slot-scope="{ row }">
            <el-tag :type="msgTypeTag(row.type)" size="small">{{ msgTypeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="已读" width="80">
          <template slot-scope="{ row }">
            <el-tag :type="row.isRead > 0 ? 'success' : 'info'" size="small">{{ row.isRead > 0 ? '已读' : '未读' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发送时间" min-width="160" />
        <el-table-column label="操作" width="80" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="danger" size="mini" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </el-card>

    <el-dialog title="发送消息" :visible.sync="sendVisible" width="450px">
      <el-form label-width="80px">
        <el-form-item label="接收用户">
          <el-select
            v-model="sendForm.receiverId"
            filterable
            remote
            reserve-keyword
            placeholder="搜索用户名或昵称"
            :remote-method="searchUser"
            :loading="userSearching"
            style="width: 100%"
          >
            <el-option v-for="u in userOptions" :key="u.id" :label="u.label" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="消息内容">
          <el-input v-model="sendForm.content" type="textarea" :rows="4" placeholder="请输入消息内容" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="sendVisible = false">取消</el-button>
        <el-button type="primary" :loading="sendLoading" @click="handleSend">发送</el-button>
      </span>
    </el-dialog>

    <el-dialog title="广播通知" :visible.sync="broadcastVisible" width="450px">
      <el-alert title="将向所有注册用户发送系统通知" type="warning" :closable="false" style="margin-bottom: 16px" />
      <el-form label-width="80px">
        <el-form-item label="消息内容">
          <el-input v-model="broadcastContent" type="textarea" :rows="4" placeholder="请输入广播内容" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="broadcastVisible = false">取消</el-button>
        <el-button type="warning" :loading="broadcastLoading" @click="handleBroadcast">广播发送</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getAdminMessages, sendMessage, broadcastMessage, deleteMessage, searchUsers } from '@/api/admin'

export default {
  name: 'MessageManage',
  data() {
    return {
      msgType: '',
      keyword: '',
      messages: [],
      loading: false,
      total: 0,
      currentPage: 1,
      pageSize: 10,
      sendVisible: false,
      sendLoading: false,
      sendForm: { receiverId: null, content: '' },
      userOptions: [],
      userSearching: false,
      broadcastVisible: false,
      broadcastLoading: false,
      broadcastContent: ''
    }
  },
  created() {
    this.fetchMessages()
  },
  methods: {
    async fetchMessages() {
      this.loading = true
      try {
        const params = { page: this.currentPage, size: this.pageSize }
        if (this.msgType) params.type = this.msgType
        if (this.keyword) params.keyword = this.keyword
        const res = await getAdminMessages(params)
        const data = res.data || res
        this.messages = data.records || data.list || []
        this.total = data.total || 0
      } catch (e) {
        console.error('获取消息列表失败', e)
        this.$message.error('获取消息列表失败')
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.currentPage = 1
      this.fetchMessages()
    },
    handlePageChange(page) {
      this.currentPage = page
      this.fetchMessages()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.fetchMessages()
    },
    msgTypeLabel(t) {
      const map = { SYSTEM: '系统通知', TRADE: '交易消息', CHAT: '私信' }
      return map[t] || t
    },
    msgTypeTag(t) {
      const map = { SYSTEM: 'danger', TRADE: 'warning', CHAT: '' }
      return map[t] || ''
    },
    openSendDialog() {
      this.sendForm = { receiverId: null, content: '' }
      this.userOptions = []
      this.sendVisible = true
    },
    async searchUser(query) {
      if (!query || query.length < 1) return
      this.userSearching = true
      try {
        const res = await searchUsers(query)
        this.userOptions = res.data || []
      } catch { /* noop */ } finally {
        this.userSearching = false
      }
    },
    async handleSend() {
      if (!this.sendForm.receiverId || !this.sendForm.content.trim()) {
        this.$message.warning('请填写完整')
        return
      }
      this.sendLoading = true
      try {
        await sendMessage(this.sendForm)
        this.$message.success('发送成功')
        this.sendVisible = false
        this.fetchMessages()
      } catch {
        this.$message.error('发送失败')
      } finally {
        this.sendLoading = false
      }
    },
    openBroadcastDialog() {
      this.broadcastContent = ''
      this.broadcastVisible = true
    },
    async handleBroadcast() {
      if (!this.broadcastContent.trim()) {
        this.$message.warning('请输入广播内容')
        return
      }
      this.broadcastLoading = true
      try {
        await broadcastMessage({ content: this.broadcastContent })
        this.$message.success('广播发送成功')
        this.broadcastVisible = false
        this.fetchMessages()
      } catch {
        this.$message.error('广播失败')
      } finally {
        this.broadcastLoading = false
      }
    },
    async handleDelete(row) {
      try {
        await this.$confirm('确定删除该消息？', '提示', { type: 'warning' })
        await deleteMessage(row.id)
        this.$message.success('删除成功')
        this.fetchMessages()
      } catch (e) { if (e !== 'cancel') this.$message.error('删除失败') }
    }
  }
}
</script>

<style scoped>
.search-bar {
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
  text-align: right;
}
</style>
