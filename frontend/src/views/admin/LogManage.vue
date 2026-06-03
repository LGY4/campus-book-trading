<template>
  <div class="log-manage">
    <el-card shadow="never">
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索用户名/模块/操作"
          clearable
          style="width: 240px"
          @keyup.enter.native="handleSearch"
        >
          <el-button slot="append" icon="el-icon-search" @click="handleSearch" />
        </el-input>
        <el-select v-model="moduleFilter" placeholder="模块筛选" clearable style="width: 130px; margin-left: 10px" @change="handleSearch">
          <el-option label="认证" value="认证" />
          <el-option label="用户" value="用户" />
          <el-option label="书籍" value="书籍" />
          <el-option label="订单" value="订单" />
          <el-option label="评论" value="评论" />
          <el-option label="交换" value="交换" />
          <el-option label="支付" value="支付" />
          <el-option label="消息" value="消息" />
          <el-option label="类别" value="类别" />
          <el-option label="轮播" value="轮播" />
          <el-option label="用户管理" value="用户管理" />
          <el-option label="书籍管理" value="书籍管理" />
          <el-option label="订单管理" value="订单管理" />
          <el-option label="评论管理" value="评论管理" />
          <el-option label="消息管理" value="消息管理" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="yyyy-MM-dd"
          style="margin-left: 10px"
          @change="handleSearch"
        />
      </div>

      <el-table :data="logs" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="username" label="用户" min-width="100" />
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="action" label="操作" min-width="120" />
        <el-table-column label="详情" min-width="200">
          <template slot-scope="{ row }">
            <el-tooltip :content="row.detail" placement="top" :disabled="!row.detail">
              <span v-if="row.detail && row.detail.includes('[ERROR:')" class="detail-text">
                <el-tag type="danger" size="mini">失败</el-tag>
                <span style="color:#f56c6c;margin-left:4px;">{{ extractError(row.detail) }}</span>
              </span>
              <span v-else class="detail-text">{{ formatDetail(row.detail) }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="createTime" label="时间" min-width="160" />
      </el-table>

      <el-pagination
        class="pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        :page-sizes="[20, 50, 100]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </el-card>
  </div>
</template>

<script>
import { getLogs } from '@/api/admin'

export default {
  name: 'LogManage',
  data() {
    return {
      keyword: '',
      moduleFilter: '',
      dateRange: null,
      logs: [],
      loading: false,
      total: 0,
      currentPage: 1,
      pageSize: 20
    }
  },
  created() {
    this.fetchLogs()
  },
  methods: {
    async fetchLogs() {
      this.loading = true
      try {
        const params = { keyword: this.keyword, module: this.moduleFilter, page: this.currentPage, size: this.pageSize }
        if (this.dateRange && this.dateRange.length === 2) {
          params.startDate = this.dateRange[0]
          params.endDate = this.dateRange[1]
        }
        const res = await getLogs(params)
        const data = res.data || res
        this.logs = data.records || data.list || []
        this.total = data.total || 0
      } catch {
        this.$message.error('获取日志列表失败')
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.currentPage = 1
      this.fetchLogs()
    },
    handlePageChange(page) {
      this.currentPage = page
      this.fetchLogs()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.fetchLogs()
    },
    formatDetail(detail) {
      if (!detail) return '-'
      return detail
    },
    extractError(detail) {
      if (!detail) return ''
      const match = detail.match(/失败原因:\s*(.+?)$/)
      return match ? match[1] : detail
    }
  }
}
</script>

<style scoped>
.search-bar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}
.pagination {
  margin-top: 16px;
  text-align: right;
}
.detail-text {
  display: inline-block;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
