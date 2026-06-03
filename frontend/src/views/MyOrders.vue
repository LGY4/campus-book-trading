<template>
  <div class="my-orders">
    <el-card>
      <h2 slot="header">我的订单</h2>
      <el-tabs v-model="activeTab" @tab-click="handleTabChange">
        <el-tab-pane label="我买到的" name="buy">
          <div class="status-filter">
            <el-radio-group v-model="statusFilter" size="small" @change="handleFilterChange">
              <el-radio-button label="">全部</el-radio-button>
              <el-radio-button label="PENDING">待付款</el-radio-button>
              <el-radio-button label="PAID">已付款</el-radio-button>
              <el-radio-button label="SHIPPED">已发货</el-radio-button>
              <el-radio-button label="COMPLETED">已完成</el-radio-button>
              <el-radio-button label="REFUNDING">退款中</el-radio-button>
              <el-radio-button label="RETURNING">退回中</el-radio-button>
              <el-radio-button label="DISPUTE">纠纷中</el-radio-button>
            </el-radio-group>
          </div>
          <el-table :data="orders" v-loading="loading" stripe style="width:100%;">
            <el-table-column prop="orderNo" label="订单号" width="200" show-overflow-tooltip />
            <el-table-column prop="bookTitle" label="书籍" min-width="180" show-overflow-tooltip />
            <el-table-column prop="price" label="金额" width="100">
              <template slot-scope="{ row }">
                <span class="price">¥{{ row.price }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120">
              <template slot-scope="{ row }">
                <el-tag :type="orderStatusType(row.status)" size="small">{{ orderStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="下单时间" width="170" />
            <el-table-column label="操作" width="240" fixed="right">
              <template slot-scope="{ row }">
                <el-button v-if="row.status === 'PENDING'" type="text" size="small" @click="handlePay(row)">去支付</el-button>
                <el-button v-if="row.status === 'SHIPPED'" type="text" size="small" @click="handleConfirm(row)">确认收货</el-button>
                <el-button v-if="row.status === 'PENDING' || row.status === 'PAID'" type="text" size="small" class="danger-text" @click="handleCancel(row)">取消</el-button>
                <el-button v-if="row.status === 'COMPLETED'" type="primary" size="mini" @click="handleComment(row)">评价</el-button>
                <el-button v-if="row.status === 'PAID' || row.status === 'SHIPPED' || row.status === 'COMPLETED'" type="text" size="small" @click="handleRefund(row)">申请退款</el-button>
                <el-button v-if="row.status === 'RETURNING' && !row.returnTrackingNumber" type="text" size="small" @click="handleReturnShipping(row)">填写退回物流</el-button>
                <el-button v-if="row.status === 'PAID' || row.status === 'SHIPPED' || row.status === 'REFUNDING' || row.status === 'RETURNING'" type="text" size="small" class="danger-text" @click="handleDispute(row)">申请纠纷</el-button>
                <el-button type="text" size="small" @click="viewDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="我卖出的" name="sell">
          <div class="status-filter">
            <el-radio-group v-model="statusFilter" size="small" @change="handleFilterChange">
              <el-radio-button label="">全部</el-radio-button>
              <el-radio-button label="PAID">已付款</el-radio-button>
              <el-radio-button label="SHIPPED">已发货</el-radio-button>
              <el-radio-button label="REFUNDING">退款中</el-radio-button>
              <el-radio-button label="RETURNING">退回中</el-radio-button>
              <el-radio-button label="DISPUTE">纠纷中</el-radio-button>
            </el-radio-group>
          </div>
          <el-table :data="orders" v-loading="loading" stripe style="width:100%;">
            <el-table-column prop="orderNo" label="订单号" width="200" show-overflow-tooltip />
            <el-table-column prop="bookTitle" label="书籍" min-width="180" show-overflow-tooltip />
            <el-table-column prop="price" label="金额" width="100">
              <template slot-scope="{ row }">
                <span class="price">¥{{ row.price }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="buyerName" label="买家" width="120" />
            <el-table-column prop="status" label="状态" width="120">
              <template slot-scope="{ row }">
                <el-tag :type="orderStatusType(row.status)" size="small">{{ orderStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="下单时间" width="170" />
            <el-table-column label="操作" width="260" fixed="right">
              <template slot-scope="{ row }">
                <el-button v-if="row.status === 'PAID'" type="text" size="small" @click="handleShip(row)">发货</el-button>
                <el-button v-if="row.status === 'REFUNDING'" type="text" size="small" @click="handleRefundApprove(row)">同意退款</el-button>
                <el-button v-if="row.status === 'REFUNDING'" type="text" size="small" class="danger-text" @click="handleRefundReject(row)">拒绝退款</el-button>
                <el-button v-if="row.status === 'RETURNING'" type="text" size="small" @click="handleConfirmReturn(row)">确认收到退回</el-button>
                <el-button v-if="row.status === 'PAID' || row.status === 'SHIPPED' || row.status === 'REFUNDING' || row.status === 'RETURNING'" type="text" size="small" class="danger-text" @click="handleDispute(row)">申请纠纷</el-button>
                <el-button type="text" size="small" @click="viewDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="pageSize"
          :current-page.sync="page"
          @current-change="fetchOrders"
        />
      </div>
      <el-empty v-if="!orders.length && loaded" description="暂无订单" />
    </el-card>

    <el-dialog title="发货" :visible.sync="shipDialogVisible" width="460px">
      <el-form ref="shipForm" :model="shipForm" label-width="80px">
        <el-form-item label="物流公司">
          <el-select v-model="shipForm.logisticsCompany" placeholder="请选择物流公司" style="width:100%">
            <el-option label="顺丰速运" value="顺丰速运" />
            <el-option label="圆通速递" value="圆通速递" />
            <el-option label="中通快递" value="中通快递" />
            <el-option label="韵达快递" value="韵达快递" />
            <el-option label="申通快递" value="申通快递" />
            <el-option label="百世快递" value="百世快递" />
            <el-option label="邮政快递" value="邮政快递" />
            <el-option label="京东物流" value="京东物流" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model="shipForm.trackingNumber" placeholder="请输入物流单号" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="shipForm.shippingInfo" type="textarea" :rows="2" placeholder="可选备注信息" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipLoading" @click="submitShip">确认发货</el-button>
      </div>
    </el-dialog>

    <el-dialog title="填写退回物流" :visible.sync="returnDialogVisible" width="460px">
      <el-form :model="returnForm" label-width="80px">
        <el-form-item label="物流公司">
          <el-select v-model="returnForm.logisticsCompany" placeholder="请选择物流公司" style="width:100%">
            <el-option label="顺丰速运" value="顺丰速运" />
            <el-option label="圆通速递" value="圆通速递" />
            <el-option label="中通快递" value="中通快递" />
            <el-option label="韵达快递" value="韵达快递" />
            <el-option label="申通快递" value="申通快递" />
            <el-option label="百世快递" value="百世快递" />
            <el-option label="邮政快递" value="邮政快递" />
            <el-option label="京东物流" value="京东物流" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model="returnForm.trackingNumber" placeholder="请输入退回物流单号" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="returnDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="returnLoading" @click="submitReturn">确认提交</el-button>
      </div>
    </el-dialog>

    <el-dialog title="申请纠纷" :visible.sync="disputeDialogVisible" width="500px">
      <el-form :model="disputeForm" label-width="60px">
        <el-form-item label="原因">
          <el-input v-model="disputeForm.reason" type="textarea" :rows="4" placeholder="请描述纠纷原因..." />
        </el-form-item>
        <el-form-item label="证据">
          <el-upload
            action="/api/file/upload"
            :headers="uploadHeaders"
            :on-success="handleDisputeImageSuccess"
            :on-remove="handleDisputeImageRemove"
            :file-list="disputeImageList"
            list-type="picture-card"
            :limit="5"
            accept="image/*"
          >
            <i class="el-icon-plus"></i>
          </el-upload>
          <div class="upload-tip">上传证据图片，最多5张</div>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="disputeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="disputeLoading" @click="submitDispute">提交纠纷</el-button>
      </div>
    </el-dialog>

    <el-dialog title="评价订单" :visible.sync="commentDialogVisible" width="500px">
      <el-form ref="commentForm" :model="commentForm" label-width="60px">
        <el-form-item label="评分">
          <el-rate v-model="commentForm.rating" show-text :texts="['很差','差','一般','好','很好']" />
        </el-form-item>
        <el-form-item label="评价">
          <el-input v-model="commentForm.content" type="textarea" :rows="4" placeholder="分享你的购买体验..." />
        </el-form-item>
        <el-form-item label="图片">
          <el-upload
            action="/api/file/upload"
            :headers="uploadHeaders"
            :on-success="handleCommentImageSuccess"
            :on-remove="handleCommentImageRemove"
            :file-list="commentImageList"
            list-type="picture-card"
            :limit="5"
            accept="image/*"
          >
            <i class="el-icon-plus"></i>
          </el-upload>
          <div class="upload-tip">最多上传5张图片</div>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="commentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="commentLoading" @click="submitComment">提交评价</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getMyBuyOrders,
  getMySellOrders,
  payOrder,
  confirmReceive,
  cancelOrder,
  shipOrder,
  requestRefund,
  confirmRefund,
  rejectRefund,
  disputeOrder,
  submitReturnShipping,
  confirmReturnReceive
} from '@/api/order'
import { createComment } from '@/api/comment'
import { getToken } from '@/utils/auth'

export default {
  name: 'MyOrders',
  data() {
    return {
      activeTab: 'buy',
      statusFilter: '',
      orders: [],
      page: 1,
      pageSize: 10,
      total: 0,
      loaded: false,
      loading: false,
      commentDialogVisible: false,
      commentForm: { orderId: '', rating: 5, content: '', images: [] },
      commentLoading: false,
      commentImageList: [],
      shipDialogVisible: false,
      shipForm: { orderId: null, logisticsCompany: '', trackingNumber: '', shippingInfo: '' },
      shipLoading: false,
      returnDialogVisible: false,
      returnForm: { orderId: null, logisticsCompany: '', trackingNumber: '' },
      returnLoading: false,
      disputeDialogVisible: false,
      disputeForm: { orderId: null, reason: '', images: [] },
      disputeLoading: false,
      disputeImageList: []
    }
  },
  created() {
    this.fetchOrders()
  },
  computed: {
    uploadHeaders() {
      return { Authorization: 'Bearer ' + getToken() }
    }
  },
  methods: {
    async fetchOrders() {
      this.loading = true
      try {
        const fn = this.activeTab === 'buy' ? getMyBuyOrders : getMySellOrders
        const params = { page: this.page, size: this.pageSize }
        if (this.statusFilter) params.status = this.statusFilter
        const res = await fn(params)
        this.orders = res.data.records || []
        this.total = res.data.total || 0
      } catch (e) { this.$message.error('加载订单失败') } finally {
        this.loaded = true
        this.loading = false
      }
    },
    handleTabChange() {
      this.page = 1
      this.statusFilter = ''
      this.orders = []
      this.fetchOrders()
    },
    handleFilterChange() {
      this.page = 1
      this.fetchOrders()
    },
    orderStatusType(val) {
      const map = {
        PENDING: 'warning',
        PAID: '',
        SHIPPED: 'primary',
        COMPLETED: 'success',
        CANCELLED: 'info',
        REFUNDING: 'danger',
        RETURNING: 'warning',
        REFUNDED: 'info',
        DISPUTE: 'danger'
      }
      return map[val] || 'info'
    },
    orderStatusText(val) {
      const map = {
        PENDING: '待付款',
        PAID: '已付款',
        SHIPPED: '已发货',
        COMPLETED: '已完成',
        CANCELLED: '已取消',
        REFUNDING: '退款中',
        RETURNING: '退回中',
        REFUNDED: '已退款',
        DISPUTE: '纠纷中'
      }
      return map[val] || val
    },
    async handlePay(row) {
      try {
        await this.$confirm('确认支付 ¥' + row.price + '？', '确认支付', { type: 'info' })
        const res = await payOrder(row.id)
        this.$message.success('支付成功')
        this.$router.push({ path: '/pay/result', query: { orderId: row.id } })
      } catch (e) { if (e !== 'cancel') this.$message.error('支付失败') }
    },
    async handleConfirm(row) {
      try {
        await this.$confirm('确认已收到书籍？', '确认收货', { type: 'info' })
        await confirmReceive(row.id)
        this.$message.success('已确认收货')
        this.fetchOrders()
      } catch (e) { if (e !== 'cancel') this.$message.error('确认收货失败') }
    },
    async handleCancel(row) {
      try {
        await this.$confirm('确定取消此订单？', '取消订单', { type: 'warning' })
        await cancelOrder(row.id)
        this.$message.success('订单已取消')
        this.fetchOrders()
      } catch (e) { if (e !== 'cancel') this.$message.error('取消订单失败') }
    },
    handleShip(row) {
      this.shipForm = { orderId: row.id, logisticsCompany: '', trackingNumber: '', shippingInfo: '' }
      this.shipDialogVisible = true
    },
    async submitShip() {
      if (!this.shipForm.logisticsCompany) {
        this.$message.warning('请选择物流公司')
        return
      }
      if (!this.shipForm.trackingNumber || !this.shipForm.trackingNumber.trim()) {
        this.$message.warning('请输入物流单号')
        return
      }
      this.shipLoading = true
      try {
        await shipOrder(this.shipForm)
        this.$message.success('已发货')
        this.shipDialogVisible = false
        this.fetchOrders()
      } catch (e) { this.$message.error('发货失败') } finally {
        this.shipLoading = false
      }
    },
    handleReturnShipping(row) {
      this.returnForm = { orderId: row.id, logisticsCompany: '', trackingNumber: '' }
      this.returnDialogVisible = true
    },
    async submitReturn() {
      if (!this.returnForm.logisticsCompany) {
        this.$message.warning('请选择物流公司')
        return
      }
      if (!this.returnForm.trackingNumber || !this.returnForm.trackingNumber.trim()) {
        this.$message.warning('请输入物流单号')
        return
      }
      this.returnLoading = true
      try {
        await submitReturnShipping(this.returnForm)
        this.$message.success('退回物流信息已提交')
        this.returnDialogVisible = false
        this.fetchOrders()
      } catch (e) { this.$message.error('提交失败') } finally {
        this.returnLoading = false
      }
    },
    async handleConfirmReturn(row) {
      try {
        await this.$confirm('确认已收到退回书籍？将自动完成退款。', '确认收货', { type: 'info' })
        await confirmReturnReceive(row.id)
        this.$message.success('已确认收到退回书籍，退款已完成')
        this.fetchOrders()
      } catch (e) { if (e !== 'cancel') this.$message.error('操作失败') }
    },
    handleComment(row) {
      this.commentForm = { orderId: row.id, bookId: row.bookId, rating: 5, content: '', images: [] }
      this.commentImageList = []
      this.commentDialogVisible = true
    },
    handleCommentImageSuccess(res, file) {
      const url = typeof res.data === 'string' ? res.data : (res.data?.url || res.data)
      if (url) this.commentForm.images.push(url)
    },
    handleCommentImageRemove(file) {
      const url = file.url || (typeof file.response?.data === 'string' ? file.response.data : file.response?.data?.url)
      const idx = this.commentForm.images.indexOf(url)
      if (idx > -1) this.commentForm.images.splice(idx, 1)
    },
    async submitComment() {
      if (!this.commentForm.content.trim()) {
        this.$message.warning('请输入评价内容')
        return
      }
      this.commentLoading = true
      try {
        const payload = { ...this.commentForm }
        if (payload.images && payload.images.length) {
          payload.images = JSON.stringify(payload.images)
        } else {
          delete payload.images
        }
        await createComment(payload)
        this.$message.success('评价成功')
        this.commentDialogVisible = false
        this.fetchOrders()
      } catch (e) { this.$message.error('评价失败') } finally {
        this.commentLoading = false
      }
    },
    async handleRefund(row) {
      try {
        const { value } = await this.$prompt('请输入退款原因', '申请退款', {
          inputType: 'textarea',
          inputValidator: (v) => !!v.trim() || '请输入原因'
        })
        await requestRefund({ orderId: row.id, reason: value })
        this.$message.success('退款申请已提交')
        this.fetchOrders()
      } catch (e) { if (e !== 'cancel') this.$message.error('退款申请失败') }
    },
    handleDispute(row) {
      this.disputeForm = { orderId: row.id, reason: '', images: [] }
      this.disputeImageList = []
      this.disputeDialogVisible = true
    },
    handleDisputeImageSuccess(res) {
      const url = typeof res.data === 'string' ? res.data : (res.data?.url || res.data)
      if (url) this.disputeForm.images.push(url)
    },
    handleDisputeImageRemove(file) {
      const url = file.url || (typeof file.response?.data === 'string' ? file.response.data : file.response?.data?.url)
      const idx = this.disputeForm.images.indexOf(url)
      if (idx > -1) this.disputeForm.images.splice(idx, 1)
    },
    async submitDispute() {
      if (!this.disputeForm.reason.trim()) {
        this.$message.warning('请描述纠纷原因')
        return
      }
      this.disputeLoading = true
      try {
        const payload = {
          orderId: this.disputeForm.orderId,
          reason: this.disputeForm.reason,
          disputeImages: this.disputeForm.images.length ? JSON.stringify(this.disputeForm.images) : undefined
        }
        await disputeOrder(payload)
        this.$message.success('纠纷已提交，等待管理员处理')
        this.disputeDialogVisible = false
        this.fetchOrders()
      } catch (e) { this.$message.error('申请纠纷失败') } finally {
        this.disputeLoading = false
      }
    },
    async handleRefundApprove(row) {
      try {
        await this.$confirm('同意退款？', '退款确认', { type: 'warning' })
        await confirmRefund(row.id)
        this.$message.success('已同意退款')
        this.fetchOrders()
      } catch (e) { if (e !== 'cancel') this.$message.error('退款操作失败') }
    },
    async handleRefundReject(row) {
      try {
        await this.$confirm('拒绝退款申请？买家可发起纠纷。', '拒绝退款', { type: 'warning' })
        await rejectRefund(row.id)
        this.$message.success('已拒绝退款')
        this.fetchOrders()
      } catch (e) { if (e !== 'cancel') this.$message.error('拒绝退款失败') }
    },
    viewDetail(row) {
      this.$router.push('/order/' + row.id)
    }
  }
}
</script>

<style scoped>
.my-orders h2 {
  margin: 0;
  font-size: 20px;
}
.status-filter {
  margin-bottom: 16px;
}
.price {
  color: #f56c6c;
  font-weight: 600;
}
.danger-text {
  color: #f56c6c;
}
.pagination-wrap {
  text-align: center;
  margin-top: 20px;
}
.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
