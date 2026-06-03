<template>
  <div class="dispute-manage">
    <el-card shadow="never">
      <el-table :data="disputes" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="bookTitle" label="书籍" min-width="150" show-overflow-tooltip />
        <el-table-column prop="buyerName" label="买家" min-width="100" />
        <el-table-column prop="sellerName" label="卖家" min-width="100" />
        <el-table-column prop="disputeReason" label="纠纷原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 'DISPUTE' ? 'danger' : 'warning'" size="small">
              {{ row.status === 'DISPUTE' ? '纠纷中' : '退款中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="primary" size="mini" @click="openResolve(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
      />
    </el-card>

    <el-dialog title="处理纠纷" :visible.sync="resolveVisible" width="500px">
      <el-descriptions :column="2" border v-if="resolveItem" style="margin-bottom: 16px">
        <el-descriptions-item label="订单号" :span="2">{{ resolveItem.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="书籍">{{ resolveItem.bookTitle }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ resolveItem.price }}</el-descriptions-item>
        <el-descriptions-item label="买家">{{ resolveItem.buyerName }}</el-descriptions-item>
        <el-descriptions-item label="卖家">{{ resolveItem.sellerName }}</el-descriptions-item>
        <el-descriptions-item label="下单时间" :span="2">{{ resolveItem.createTime }}</el-descriptions-item>
        <el-descriptions-item label="纠纷原因" :span="2">{{ resolveItem.disputeReason || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="resolveItem.disputeImages" label="证据图片" :span="2">
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            <el-image
              v-for="(img, idx) in parseImages(resolveItem.disputeImages)"
              :key="idx"
              :src="img"
              :preview-src-list="parseImages(resolveItem.disputeImages)"
              fit="cover"
              style="width:80px;height:80px;border-radius:4px;"
            />
          </div>
        </el-descriptions-item>
      </el-descriptions>
      <el-form>
        <el-form-item label="处理意见">
          <el-input
            v-model="adminNote"
            type="textarea"
            :rows="4"
            placeholder="请输入处理意见"
          />
        </el-form-item>
        <el-form-item label="部分退款">
          <el-input-number v-model="partialRefundAmount" :min="0" :max="resolveItem ? Number(resolveItem.price) : 0" :precision="2" :step="1" style="width:200px;" />
          <span style="margin-left:8px;color:#909399;font-size:12px;">元（订单金额 ¥{{ resolveItem ? resolveItem.price : 0 }}）</span>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="resolveVisible = false">取消</el-button>
        <el-button type="warning" :loading="resolveLoading" @click="handleResolve('partial_refund')">部分退款</el-button>
        <el-button type="primary" :loading="resolveLoading" @click="handleResolve('refund')">全额退款给买家</el-button>
        <el-button type="success" :loading="resolveLoading" @click="handleResolve('release')">放款给卖家</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getDisputes, resolveDispute } from '@/api/admin'

export default {
  name: 'DisputeManage',
  data() {
    return {
      disputes: [],
      loading: false,
      total: 0,
      currentPage: 1,
      pageSize: 10,
      resolveVisible: false,
      resolveLoading: false,
      resolveItem: null,
      adminNote: '',
      partialRefundAmount: 0
    }
  },
  created() {
    this.fetchDisputes()
  },
  methods: {
    async fetchDisputes() {
      this.loading = true
      try {
        const res = await getDisputes({ page: this.currentPage, size: this.pageSize })
        const data = res.data || res
        this.disputes = data.records || data.list || []
        this.total = data.total || 0
      } catch {
        this.$message.error('获取纠纷列表失败')
      } finally {
        this.loading = false
      }
    },
    handlePageChange(page) {
      this.currentPage = page
      this.fetchDisputes()
    },
    parseImages(json) {
      try { return JSON.parse(json) } catch { return [] }
    },
    openResolve(row) {
      this.resolveItem = row
      this.adminNote = ''
      this.partialRefundAmount = 0
      this.resolveVisible = true
    },
    async handleResolve(action) {
      if (!this.adminNote.trim()) {
        this.$message.warning('请输入处理意见')
        return
      }
      this.resolveLoading = true
      try {
        await resolveDispute({
          orderId: this.resolveItem.id,
          action,
          adminNote: this.adminNote,
          partialRefundAmount: action === 'partial_refund' ? this.partialRefundAmount : undefined
        })
        this.$message.success('处理成功')
        this.resolveVisible = false
        this.fetchDisputes()
      } catch {
        this.$message.error('处理失败')
      } finally {
        this.resolveLoading = false
      }
    }
  }
}
</script>

<style scoped>
.pagination {
  margin-top: 16px;
  text-align: right;
}
</style>
