<template>
  <div class="pay-result">
    <el-card v-loading="loading" class="result-card">
      <div v-if="success" class="result-content">
        <i class="el-icon-circle-check result-icon success" />
        <h2>支付成功</h2>
        <p class="result-desc">您的订单已支付成功，等待卖家发货</p>
        <el-descriptions :column="1" border class="order-info" v-if="orderInfo">
          <el-descriptions-item label="订单号">{{ orderInfo.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="书籍">{{ orderInfo.bookTitle }}</el-descriptions-item>
          <el-descriptions-item label="支付金额">
            <span class="price">¥{{ orderInfo.price }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ orderInfo.updateTime }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div v-else class="result-content">
        <i class="el-icon-circle-close result-icon fail" />
        <h2>支付失败</h2>
        <p class="result-desc">{{ errorMsg || '支付过程中出现问题，请重试' }}</p>
      </div>
      <div class="result-actions">
        <el-button type="primary" @click="$router.push('/my-orders')">查看我的订单</el-button>
        <el-button @click="$router.push('/home')">返回首页</el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import { getOrderDetail } from '@/api/order'

export default {
  name: 'PayResult',
  data() {
    return {
      success: false,
      loading: false,
      orderInfo: null,
      errorMsg: ''
    }
  },
  created() {
    const query = this.$route.query
    const orderId = query.orderId
    if (orderId) {
      this.verifyPayment(orderId)
    } else {
      this.success = false
      this.errorMsg = '缺少订单信息'
    }
  },
  methods: {
    async verifyPayment(orderId) {
      this.loading = true
      try {
        const res = await getOrderDetail(orderId)
        this.orderInfo = res.data
        // Verify payment by checking order status from server
        if (this.orderInfo && (this.orderInfo.status === 'PAID' || this.orderInfo.status === 'SHIPPED' || this.orderInfo.status === 'COMPLETED')) {
          this.success = true
        } else {
          this.success = false
          this.errorMsg = '订单状态异常，请查看订单详情'
        }
      } catch (e) {
        this.success = false
        this.errorMsg = '获取订单信息失败'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.pay-result {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}
.result-card {
  width: 520px;
  text-align: center;
  border-radius: 12px;
}
.result-content {
  padding: 20px 0;
}
.result-icon {
  font-size: 64px;
}
.result-icon.success {
  color: #67c23a;
}
.result-icon.fail {
  color: #f56c6c;
}
.result-content h2 {
  margin: 16px 0 8px;
  font-size: 22px;
  color: #303133;
}
.result-desc {
  font-size: 14px;
  color: #909399;
  margin-bottom: 24px;
}
.order-info {
  text-align: left;
  margin-bottom: 24px;
}
.price {
  color: #f56c6c;
  font-weight: 700;
  font-size: 16px;
}
.result-actions {
  padding-bottom: 8px;
}
</style>
