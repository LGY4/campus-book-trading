<template>
  <div class="my-exchange">
    <el-card>
      <h2 slot="header">换书记录</h2>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="我发起的" name="sent" />
        <el-tab-pane label="我收到的" name="received" />
      </el-tabs>

      <el-table :data="exchanges" v-loading="loading" stripe style="width:100%;">
        <el-table-column label="对方" width="140">
          <template slot-scope="{ row }">
            <div class="user-cell">
              <el-avatar :src="activeTab === 'sent' ? row.targetUserAvatar : row.initiatorAvatar" :size="28" icon="el-icon-user-solid" />
              <span>{{ activeTab === 'sent' ? row.targetUserNickname : row.initiatorNickname }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="对方书籍" min-width="200">
          <template slot-scope="{ row }">
            <div class="book-cell">
              <el-image :src="activeTab === 'sent' ? row.targetBookCover : row.initiatorBookCover" fit="cover" class="book-thumb">
                <div slot="error" class="thumb-slot"><i class="el-icon-picture-outline" /></div>
              </el-image>
              <span>{{ activeTab === 'sent' ? row.targetBookTitle : row.initiatorBookTitle }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="我的书籍" min-width="200">
          <template slot-scope="{ row }">
            <div class="book-cell">
              <el-image :src="activeTab === 'sent' ? row.initiatorBookCover : row.targetBookCover" fit="cover" class="book-thumb">
                <div slot="error" class="thumb-slot"><i class="el-icon-picture-outline" /></div>
              </el-image>
              <span>{{ activeTab === 'sent' ? row.initiatorBookTitle : row.targetBookTitle }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="留言" min-width="150" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="110">
          <template slot-scope="{ row }">
            <el-tag :type="exchangeStatusType(row.status)" size="small">{{ exchangeStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="{ row }">
            <template v-if="activeTab === 'received' && row.status === 'PENDING'">
              <el-button type="text" size="small" @click="handleAccept(row)">接受</el-button>
              <el-button type="text" size="small" class="danger-text" @click="handleReject(row)">拒绝</el-button>
            </template>
            <template v-if="row.status === 'ACCEPTED'">
              <el-button type="text" size="small" @click="handleComplete(row)">确认完成</el-button>
            </template>
            <template v-if="activeTab === 'sent' && row.status === 'PENDING'">
              <el-button type="text" size="small" class="danger-text" @click="handleCancel(row)">撤回</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="pageSize"
          :current-page.sync="page"
          @current-change="fetchExchanges"
        />
      </div>
      <el-empty v-if="!exchanges.length && loaded" description="暂无换书记录" />
    </el-card>
  </div>
</template>

<script>
import { getMyExchanges, acceptExchange, rejectExchange, completeExchange, cancelExchange } from '@/api/exchange'

export default {
  name: 'MyExchange',
  data() {
    return {
      activeTab: 'sent',
      exchanges: [],
      page: 1,
      pageSize: 10,
      total: 0,
      loaded: false,
      loading: false
    }
  },
  created() {
    this.fetchExchanges()
  },
  watch: {
    '$route'() {
      this.page = 1
      this.fetchExchanges()
    },
    activeTab() {
      this.page = 1
      this.exchanges = []
      this.fetchExchanges()
    }
  },
  methods: {
    async fetchExchanges() {
      this.loading = true
      try {
        const res = await getMyExchanges({ page: this.page, size: this.pageSize, type: this.activeTab })
        this.exchanges = res.data.records || []
        this.total = res.data.total || 0
      } catch (e) {
        this.$message.error('加载换书记录失败')
      } finally {
        this.loaded = true
        this.loading = false
      }
    },
    exchangeStatusType(val) {
      const map = { PENDING: 'warning', ACCEPTED: '', COMPLETED: 'success', REJECTED: 'info', CANCELLED: 'info' }
      return map[val] || 'info'
    },
    exchangeStatusText(val) {
      const map = { PENDING: '待处理', ACCEPTED: '已接受', COMPLETED: '已完成', REJECTED: '已拒绝', CANCELLED: '已撤回' }
      return map[val] || val
    },
    async handleAccept(row) {
      try {
        await this.$confirm('接受此次换书请求？', '确认', { type: 'info' })
        await acceptExchange(row.id)
        this.$message.success('已接受')
        this.fetchExchanges()
      } catch (e) { if (e !== 'cancel') this.$message.error('接受失败') }
    },
    async handleReject(row) {
      try {
        await this.$confirm('拒绝此次换书请求？', '确认', { type: 'warning' })
        await rejectExchange(row.id)
        this.$message.success('已拒绝')
        this.fetchExchanges()
      } catch (e) { if (e !== 'cancel') this.$message.error('拒绝失败') }
    },
    async handleComplete(row) {
      try {
        await this.$confirm('确认换书已完成？', '确认', { type: 'info' })
        await completeExchange(row.id)
        this.$message.success('换书已完成')
        this.fetchExchanges()
      } catch (e) { if (e !== 'cancel') this.$message.error('操作失败') }
    },
    async handleCancel(row) {
      try {
        await this.$confirm('撤回此换书请求？', '确认', { type: 'warning' })
        await cancelExchange(row.id)
        this.$message.success('已撤回')
        this.fetchExchanges()
      } catch (e) { if (e !== 'cancel') this.$message.error('撤回失败') }
    }
  }
}
</script>

<style scoped>
.my-exchange h2 {
  margin: 0;
  font-size: 20px;
}
.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.danger-text {
  color: #f56c6c;
}
.pagination-wrap {
  text-align: center;
  margin-top: 20px;
}
.book-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.book-thumb {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  flex-shrink: 0;
}
.thumb-slot {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: #f5f7fa;
  color: #c0c4cc;
}
</style>
