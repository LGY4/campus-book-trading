<template>
  <div class="order-detail" v-loading="loading">
    <el-card v-if="order">
      <h2 slot="header">
        <el-button type="text" icon="el-icon-arrow-left" @click="$router.back()">返回</el-button>
        订单详情
      </h2>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(order.status)">{{ statusText(order.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="书籍">{{ order.bookTitle }}</el-descriptions-item>
        <el-descriptions-item label="金额">
          <span class="price">¥{{ order.price }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="买家">{{ order.buyerName || order.buyerId }}</el-descriptions-item>
        <el-descriptions-item label="卖家">{{ order.sellerName || order.sellerId }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ order.receiverName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ order.phone }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">{{ order.address }}</el-descriptions-item>
        <el-descriptions-item v-if="order.logisticsCompany" label="物流公司">{{ order.logisticsCompany }}</el-descriptions-item>
        <el-descriptions-item v-if="order.trackingNumber" label="物流单号">{{ order.trackingNumber }}</el-descriptions-item>
        <el-descriptions-item v-if="order.shippingInfo" label="物流备注" :span="2">{{ order.shippingInfo }}</el-descriptions-item>
        <el-descriptions-item v-if="order.returnLogisticsCompany" label="退回物流公司">{{ order.returnLogisticsCompany }}</el-descriptions-item>
        <el-descriptions-item v-if="order.returnTrackingNumber" label="退回物流单号">{{ order.returnTrackingNumber }}</el-descriptions-item>
        <el-descriptions-item v-if="order.refundReason" label="退款原因" :span="2">{{ order.refundReason }}</el-descriptions-item>
        <el-descriptions-item v-if="order.disputeReason" label="纠纷原因" :span="2">{{ order.disputeReason }}</el-descriptions-item>
        <el-descriptions-item v-if="order.adminNote" label="管理员备注" :span="2">{{ order.adminNote }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ order.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ order.updateTime }}</el-descriptions-item>
      </el-descriptions>

      <div class="status-timeline">
        <h3>订单状态流转</h3>
        <el-steps :active="statusStep(order.status)" finish-status="success" align-center>
          <el-step title="下单" :description="order.createTime" />
          <el-step title="付款" />
          <el-step title="发货" />
          <el-step title="收货" />
          <el-step title="完成" />
        </el-steps>
      </div>
    </el-card>
    <el-empty v-else-if="!loading" description="订单不存在" />
  </div>
</template>

<script>
import { getOrderDetail } from '@/api/order'

export default {
  name: 'OrderDetail',
  data() {
    return {
      order: null,
      loading: false
    }
  },
  created() {
    this.fetchOrder()
  },
  methods: {
    async fetchOrder() {
      this.loading = true
      try {
        const res = await getOrderDetail(this.$route.params.id)
        this.order = res.data
      } catch (e) {
        this.$message.error('获取订单详情失败')
      } finally {
        this.loading = false
      }
    },
    statusText(s) {
      const map = { PENDING: '待付款', PAID: '已付款', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消', REFUNDING: '退款中', RETURNING: '退回中', REFUNDED: '已退款', DISPUTE: '纠纷中' }
      return map[s] || s
    },
    statusType(s) {
      const map = { PENDING: 'warning', PAID: 'primary', SHIPPED: '', COMPLETED: 'success', CANCELLED: 'info', REFUNDING: 'danger', RETURNING: 'warning', REFUNDED: 'info', DISPUTE: 'danger' }
      return map[s] || 'info'
    },
    statusStep(s) {
      const map = { PENDING: 1, PAID: 2, SHIPPED: 3, COMPLETED: 5, CANCELLED: 0, REFUNDING: 2, RETURNING: 3, REFUNDED: 2, DISPUTE: 2 }
      return map[s] || 0
    }
  }
}
</script>

<style scoped>
.order-detail { max-width: 900px; margin: 0 auto; }
.order-detail h2 { margin: 0; font-size: 20px; display: flex; align-items: center; gap: 8px; }
.price { font-size: 18px; font-weight: 700; color: #f56c6c; }
.status-timeline { margin-top: 32px; }
.status-timeline h3 { font-size: 16px; margin-bottom: 16px; }
</style>
