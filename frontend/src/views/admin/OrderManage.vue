<template>
  <div class="order-manage">
    <el-card shadow="never">
      <div class="search-bar">
        <el-input
          v-model="orderNo"
          placeholder="搜索订单号"
          clearable
          style="width: 220px"
          @keyup.enter.native="handleSearch"
        >
          <el-button slot="append" icon="el-icon-search" @click="handleSearch" />
        </el-input>
        <el-select v-model="status" placeholder="状态筛选" clearable style="width: 150px; margin-left: 10px" @change="handleSearch">
          <el-option label="待付款" value="PENDING" />
          <el-option label="已付款" value="PAID" />
          <el-option label="已发货" value="SHIPPED" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
          <el-option label="退款中" value="REFUNDING" />
          <el-option label="退回中" value="RETURNING" />
          <el-option label="已退款" value="REFUNDED" />
          <el-option label="纠纷中" value="DISPUTE" />
        </el-select>
        <el-button type="success" icon="el-icon-download" style="margin-left: 10px" @click="handleExport">导出</el-button>
      </div>

      <el-table :data="orders" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="bookTitle" label="书籍" min-width="150" show-overflow-tooltip />
        <el-table-column prop="buyerName" label="买家" min-width="100" />
        <el-table-column prop="sellerName" label="卖家" min-width="100" />
        <el-table-column label="金额" width="90">
          <template slot-scope="{ row }">
            <span style="color: #f56c6c; font-weight: bold">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template slot-scope="{ row }">
            <el-tag :type="orderStatusType(row.status)" size="small">{{ orderStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" min-width="160" />
        <el-table-column label="操作" width="100" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="primary" size="mini" @click="openDetail(row)">详情</el-button>
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

    <el-dialog title="订单详情" :visible.sync="detailVisible" width="600px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="orderStatusType(detail.status)" size="small">{{ orderStatusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="书籍">{{ detail.bookTitle }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ detail.price }}</el-descriptions-item>
        <el-descriptions-item label="买家">{{ detail.buyerName }}</el-descriptions-item>
        <el-descriptions-item label="卖家">{{ detail.sellerName }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updateTime }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">{{ detail.address || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.logisticsCompany" label="物流公司">{{ detail.logisticsCompany }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.trackingNumber" label="物流单号">{{ detail.trackingNumber }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.shippingInfo" label="物流备注" :span="2">{{ detail.shippingInfo }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.returnLogisticsCompany" label="退回物流公司">{{ detail.returnLogisticsCompany }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.returnTrackingNumber" label="退回物流单号">{{ detail.returnTrackingNumber }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.adminNote || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import { getAdminOrders, exportOrders } from '@/api/admin'

export default {
  name: 'OrderManage',
  data() {
    return {
      orderNo: '',
      status: '',
      orders: [],
      loading: false,
      total: 0,
      currentPage: 1,
      pageSize: 10,
      detailVisible: false,
      detail: null
    }
  },
  created() {
    this.fetchOrders()
  },
  methods: {
    async fetchOrders() {
      this.loading = true
      try {
        const res = await getAdminOrders({ keyword: this.orderNo, status: this.status, page: this.currentPage, size: this.pageSize })
        const data = res.data || res
        this.orders = data.records || data.list || []
        this.total = data.total || 0
      } catch {
        this.$message.error('获取订单列表失败')
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.currentPage = 1
      this.fetchOrders()
    },
    handlePageChange(page) {
      this.currentPage = page
      this.fetchOrders()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.fetchOrders()
    },
    orderStatusLabel(s) {
      const map = { PENDING: '待付款', PAID: '已付款', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消', REFUNDING: '退款中', RETURNING: '退回中', REFUNDED: '已退款', DISPUTE: '纠纷中' }
      return map[s] || s
    },
    orderStatusType(s) {
      const map = { PENDING: 'warning', PAID: 'primary', SHIPPED: '', COMPLETED: 'success', CANCELLED: 'info', REFUNDING: 'danger', RETURNING: 'warning', REFUNDED: 'info', DISPUTE: 'danger' }
      return map[s] || ''
    },
    openDetail(row) {
      this.detail = row
      this.detailVisible = true
    },
    async handleExport() {
      try {
        await exportOrders({ keyword: this.orderNo, status: this.status })
        this.$message.success('导出成功')
      } catch {
        this.$message.error('导出失败')
      }
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
</style>
