<template>
  <div class="book-detail">
    <div class="breadcrumb-nav">
      <el-button type="text" icon="el-icon-arrow-left" @click="$router.push('/home')">返回首页</el-button>
    </div>
    <el-card v-if="book" v-loading="loading" class="detail-card">
      <div class="detail-top">
        <div class="detail-left">
          <el-carousel v-if="book.images && book.images.length" :interval="5000" height="400px">
            <el-carousel-item v-for="(img, idx) in book.images" :key="idx">
              <el-image :src="img" fit="contain" class="carousel-image" :preview-src-list="book.images">
                <div slot="error" class="image-slot">
                  <i class="el-icon-picture-outline" />
                </div>
              </el-image>
            </el-carousel-item>
          </el-carousel>
          <el-image v-else fit="contain" class="carousel-image" style="height:400px;">
            <div slot="error" class="image-slot">
              <i class="el-icon-picture-outline" />
            </div>
          </el-image>
        </div>
        <div class="detail-right">
          <h1 class="book-title">{{ book.title }}</h1>
          <el-descriptions :column="1" size="medium" border class="book-desc-table">
            <el-descriptions-item label="作者">{{ book.author || '-' }}</el-descriptions-item>
            <el-descriptions-item label="出版社">{{ book.publisher || '-' }}</el-descriptions-item>
            <el-descriptions-item label="ISBN">{{ book.isbn || '-' }}</el-descriptions-item>
            <el-descriptions-item label="品相">
              <el-tag :type="conditionType(book.bookCondition)" size="small">{{ book.bookCondition }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="原价">
              <span class="original-price">¥{{ book.originalPrice }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="售价">
              <span class="selling-price">¥{{ book.sellingPrice }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="库存">
              <span :class="{ 'stock-low': book.quantity <= 1 }">{{ book.quantity || 1 }} 本</span>
            </el-descriptions-item>
          </el-descriptions>

          <div class="seller-info">
            <el-avatar :src="book.sellerAvatar" :size="40" icon="el-icon-user-solid" />
            <div class="seller-meta">
              <span class="seller-name">{{ book.sellerName }}</span>
              <el-rate :value="book.sellerReputation" disabled show-score score-template="{value}" :allow-half="true" />
            </div>
          </div>

          <div v-if="isOwner" class="action-btns">
            <el-button type="primary" icon="el-icon-edit" @click="$router.push('/publish?bookId=' + book.id)">编辑</el-button>
            <el-button type="danger" icon="el-icon-delete" @click="handleDelete">删除</el-button>
          </div>
          <div v-else class="action-btns">
            <el-button type="danger" icon="el-icon-shopping-cart-2" @click="openOrderDialog">立即下单</el-button>
            <el-button :type="book.isWanted ? 'success' : 'default'" :icon="book.isWanted ? 'el-icon-check' : 'el-icon-shopping-bag-1'" @click="handleWant">
              {{ book.isWanted ? '已想要' : '我想要' }}
            </el-button>
            <el-button :type="book.isFavorited ? 'warning' : 'default'" icon="el-icon-star-off" @click="handleFavorite">
              {{ book.isFavorited ? '已收藏' : '收藏' }}
            </el-button>
            <el-button icon="el-icon-chat-dot-round" @click="handleChat">联系卖家</el-button>
            <el-button icon="el-icon-sort" @click="openExchangeDialog">换书</el-button>
          </div>
        </div>
      </div>

      <div class="detail-bottom">
        <h3>书籍描述</h3>
        <p class="description-text">{{ book.description || '暂无描述' }}</p>
      </div>
    </el-card>

    <el-card v-if="book" class="comment-section">
      <h3 slot="header">评价 ({{ commentTotal }})</h3>

      <div v-if="user && !isOwner" class="comment-form">
        <template v-if="completedOrders.length">
          <h4>发表评价</h4>
          <el-form ref="commentForm" :model="commentForm" label-width="60px">
            <el-form-item label="订单">
              <el-select v-model="commentForm.orderId" placeholder="请选择要评价的订单" style="width:100%;">
                <el-option v-for="o in completedOrders" :key="o.id" :label="'订单 ' + o.orderNo + '（' + o.createTime + '）'" :value="o.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="评分">
              <el-rate v-model="commentForm.rating" :allow-half="true" show-score />
            </el-form-item>
          <el-form-item label="内容">
            <el-input v-model="commentForm.content" type="textarea" :rows="3" placeholder="分享你的阅读体验..." />
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
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="commentLoading" @click="submitComment">提交评价</el-button>
          </el-form-item>
        </el-form>
        </template>
        <template v-else>
          <p class="no-order-tip">完成购买后即可评价此书籍，<router-link to="/my-orders">查看我的订单</router-link></p>
        </template>
      </div>

      <div v-if="comments.length">
        <div v-for="c in comments" :key="c.id" class="comment-item">
          <el-avatar :src="c.userAvatar" :size="36" icon="el-icon-user-solid" />
          <div class="comment-body">
            <div class="comment-header">
              <span class="comment-user">{{ c.userNickname }}</span>
              <el-rate :value="c.rating" disabled :allow-half="true" />
              <span class="comment-time">{{ c.createTime }}</span>
            </div>
            <p class="comment-content">{{ c.content }}</p>
            <div v-if="c.imagesJson" class="comment-images">
              <el-image
                v-for="(img, idx) in parseCommentImages(c.imagesJson)"
                :key="idx"
                :src="img"
                :preview-src-list="parseCommentImages(c.imagesJson)"
                fit="cover"
                class="comment-img"
              />
            </div>
            <el-button
              v-if="isCommentOwner(c)"
              type="text"
              size="mini"
              class="follow-up-btn"
              @click="openFollowUp(c)"
            >追评</el-button>
            <div v-if="c.followUps && c.followUps.length" class="follow-ups">
              <div v-for="fu in c.followUps" :key="fu.id" class="follow-up-item">
                <span class="follow-up-label">追评</span>
                <span class="follow-up-time">{{ fu.createTime }}</span>
                <p class="follow-up-content">{{ fu.content }}</p>
                <div v-if="fu.imagesJson" class="comment-images">
                  <el-image
                    v-for="(img, idx) in parseCommentImages(fu.imagesJson)"
                    :key="idx"
                    :src="img"
                    :preview-src-list="parseCommentImages(fu.imagesJson)"
                    fit="cover"
                    class="comment-img"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
        <el-pagination
          background
          layout="prev, pager, next"
          :total="commentTotal"
          :page-size="commentPageSize"
          :current-page.sync="commentPage"
          @current-change="fetchComments"
          style="text-align:center;margin-top:16px;"
        />
      </div>
      <el-empty v-else description="暂无评价" />
    </el-card>

    <el-dialog title="确认下单" :visible.sync="orderDialogVisible" width="520px" @open="onOrderDialogOpen">
      <template v-if="!showAddrForm">
        <div v-if="addresses.length">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
            <span style="color:#909399;font-size:14px;">选择收货地址：</span>
            <el-button type="text" size="small" icon="el-icon-plus" @click="openAddrForm(null)">新增地址</el-button>
          </div>
          <el-radio-group v-model="selectedAddressId" style="width:100%;">
            <div v-for="addr in addresses" :key="addr.id" class="addr-radio-item" style="display:flex;align-items:center;justify-content:space-between;">
              <el-radio :label="addr.id" style="flex:1;">
                <span class="addr-name">{{ addr.receiverName }}</span>
                <span class="addr-phone">{{ addr.phone }}</span>
                <el-tag v-if="addr.isDefault" type="danger" size="mini">默认</el-tag>
                <p class="addr-detail">{{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detail }}</p>
              </el-radio>
              <el-button type="text" size="mini" @click.stop="openAddrForm(addr)" style="flex-shrink:0;">编辑</el-button>
            </div>
          </el-radio-group>
        </div>
        <div v-else style="text-align:center;padding:20px 0;">
          <p style="color:#909399;margin-bottom:16px;">暂无收货地址，请先添加</p>
          <el-button type="primary" icon="el-icon-plus" @click="openAddrForm(null)">新增收货地址</el-button>
        </div>
      </template>
      <template v-else>
        <div style="margin-bottom:12px;">
          <el-button type="text" icon="el-icon-arrow-left" @click="showAddrForm = false">返回地址列表</el-button>
        </div>
        <el-form ref="addrFormInline" :model="addrFormInline" :rules="addrRulesInline" label-width="80px">
          <el-form-item label="收货人" prop="receiverName">
            <el-input v-model="addrFormInline.receiverName" placeholder="请输入收货人姓名" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="addrFormInline.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="省市区" required>
            <el-input v-model="addrFormInline.province" placeholder="省" style="width:30%;margin-right:3%;" />
            <el-input v-model="addrFormInline.city" placeholder="市" style="width:30%;margin-right:3%;" />
            <el-input v-model="addrFormInline.district" placeholder="区/县" style="width:30%;" />
          </el-form-item>
          <el-form-item label="详细地址" prop="detail">
            <el-input v-model="addrFormInline.detail" type="textarea" :rows="2" placeholder="请输入详细地址" />
          </el-form-item>
          <el-form-item label="默认">
            <el-switch v-model="addrFormInline.isDefault" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="addrSaving" @click="saveAddrInline">保存地址</el-button>
          </el-form-item>
        </el-form>
      </template>
      <div v-if="!showAddrForm" slot="footer">
        <el-button @click="orderDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="orderLoading" :disabled="!selectedAddressId" @click="confirmOrder">确认下单</el-button>
      </div>
    </el-dialog>

    <el-dialog title="发起换书" :visible.sync="exchangeDialogVisible" width="500px">
      <el-form ref="exchangeForm" :model="exchangeForm" :rules="exchangeRules" label-width="80px">
        <el-form-item label="我的书籍" prop="myBookId">
          <el-select v-if="myBooks.length" v-model="exchangeForm.myBookId" placeholder="选择你要交换的书籍" style="width:100%;">
            <el-option v-for="b in myBooks" :key="b.id" :label="b.title" :value="b.id" />
          </el-select>
          <div v-else>
            <el-alert title="你还没有可交换的书籍" type="info" :closable="false" show-icon />
            <el-button type="text" style="margin-top:8px;" @click="$router.push('/publish')">去发布书籍</el-button>
          </div>
        </el-form-item>
        <el-form-item label="留言" prop="message">
          <el-input v-model="exchangeForm.message" type="textarea" :rows="3" placeholder="给卖家留个言..." />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="exchangeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="exchangeLoading" @click="handleExchange">发送请求</el-button>
      </div>
    </el-dialog>

    <el-dialog title="追评" :visible.sync="followUpDialogVisible" width="500px">
      <el-form :model="followUpForm" label-width="60px">
        <el-form-item label="内容">
          <el-input v-model="followUpForm.content" type="textarea" :rows="4" placeholder="补充评价内容..." />
        </el-form-item>
        <el-form-item label="图片">
          <el-upload
            action="/api/file/upload"
            :headers="uploadHeaders"
            :on-success="handleFollowUpImageSuccess"
            :on-remove="handleFollowUpImageRemove"
            :file-list="followUpImageList"
            list-type="picture-card"
            :limit="5"
            accept="image/*"
          >
            <i class="el-icon-plus"></i>
          </el-upload>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="followUpDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="followUpLoading" @click="submitFollowUp">提交追评</el-button>
      </div>
    </el-dialog>

    <el-empty v-if="!book && loaded" description="书籍不存在" />
  </div>
</template>

<script>
import { getBookDetail, deleteBook, getMyBooks } from '@/api/book'
import { toggleFavorite, toggleWant } from '@/api/interaction'
import { createOrder, getCompletedOrdersByBook } from '@/api/order'
import { getBookComments, createComment } from '@/api/comment'
import { createExchange } from '@/api/exchange'
import { getAddressList, saveAddress as saveAddressApi } from '@/api/address'
import { mapState } from 'vuex'

export default {
  name: 'BookDetail',
  data() {
    return {
      book: null,
      loaded: false,
      loading: false,
      comments: [],
      commentPage: 1,
      commentPageSize: 10,
      commentTotal: 0,
      exchangeDialogVisible: false,
      exchangeForm: { myBookId: '', message: '' },
      exchangeRules: {
        myBookId: [{ required: true, message: '请选择你的书籍', trigger: 'change' }]
      },
      exchangeLoading: false,
      myBooks: [],
      orderDialogVisible: false,
      orderLoading: false,
      addresses: [],
      selectedAddressId: null,
      showAddrForm: false,
      addrFormInline: { id: null, receiverName: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false },
      addrRulesInline: {
        receiverName: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
        phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
        detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
      },
      addrSaving: false,
      followUpDialogVisible: false,
      followUpForm: { parentId: null, content: '', images: [] },
      followUpLoading: false,
      followUpImageList: [],
      commentForm: { orderId: '', rating: 5, content: '', images: [] },
      commentLoading: false,
      commentImageList: [],
      completedOrders: []
    }
  },
  computed: {
    ...mapState('user', ['user']),
    isOwner() {
      return this.user && this.book && this.user.id === this.book.sellerId
    },
    uploadHeaders() {
      const { getToken } = require('@/utils/auth')
      return { Authorization: 'Bearer ' + getToken() }
    }
  },
  created() {
    this.fetchDetail()
  },
  methods: {
    parseCommentImages(json) {
      try { return JSON.parse(json) } catch { return [] }
    },
    isCommentOwner(c) {
      return this.user && c.userId === this.user.id && !c.parentId
    },
    openFollowUp(comment) {
      this.followUpForm = { parentId: comment.id, content: '', images: [] }
      this.followUpImageList = []
      this.followUpDialogVisible = true
    },
    handleFollowUpImageSuccess(res) {
      const url = typeof res.data === 'string' ? res.data : (res.data?.url || res.data)
      if (url) this.followUpForm.images.push(url)
    },
    handleFollowUpImageRemove(file) {
      const url = file.url || (typeof file.response?.data === 'string' ? file.response.data : file.response?.data?.url)
      const idx = this.followUpForm.images.indexOf(url)
      if (idx > -1) this.followUpForm.images.splice(idx, 1)
    },
    async submitFollowUp() {
      if (!this.followUpForm.content.trim()) {
        this.$message.warning('请输入追评内容')
        return
      }
      this.followUpLoading = true
      try {
        const payload = { ...this.followUpForm }
        if (payload.images && payload.images.length) {
          payload.images = JSON.stringify(payload.images)
        } else {
          delete payload.images
        }
        await createComment(payload)
        this.$message.success('追评成功')
        this.followUpDialogVisible = false
        this.fetchComments()
      } catch (e) { this.$message.error('追评失败') } finally {
        this.followUpLoading = false
      }
    },
    async fetchDetail() {
      this.loading = true
      try {
        const res = await getBookDetail(this.$route.params.id)
        this.book = res.data
        this.loaded = true
        this.fetchComments()
        if (!this.isOwner) {
          this.fetchMyBooks()
          this.fetchCompletedOrders()
        }
      } catch (e) {
        this.$message.error('加载书籍详情失败')
        this.loaded = true
      } finally {
        this.loading = false
      }
    },
    async fetchComments() {
      try {
        const res = await getBookComments(this.$route.params.id, {
          bookId: this.$route.params.id,
          page: this.commentPage,
          size: this.commentPageSize
        })
        this.comments = res.data.records || []
        this.commentTotal = res.data.total || 0
      } catch (e) { this.$message.error('加载评价失败') }
    },
    async fetchMyBooks() {
      try {
        const res = await getMyBooks({ page: 1, size: 100 })
        this.myBooks = (res.data.records || []).filter(b => b.status === 'ON_SALE')
      } catch (e) { this.$message.error('加载我的书籍失败') }
    },
    async fetchCompletedOrders() {
      try {
        const res = await getCompletedOrdersByBook(this.$route.params.id)
        this.completedOrders = res.data || []
      } catch (e) { /* ignore */ }
    },
    conditionType(val) {
      const map = { '全新': 'success', '九成新': 'success', '八成新': 'warning', '七成新及以下': 'info' }
      return map[val] || 'info'
    },
    async openOrderDialog() {
      try {
        const addrRes = await getAddressList()
        this.addresses = addrRes.data || []
        const defaultAddr = this.addresses.find(a => a.isDefault)
        this.selectedAddressId = defaultAddr ? defaultAddr.id : (this.addresses.length ? this.addresses[0].id : null)
      } catch (e) {
        this.$message.error('加载地址失败')
      }
      this.orderDialogVisible = true
    },
    async handleWant() {
      try {
        const res = await toggleWant(this.book.id)
        const data = res.data || {}
        this.book.isWanted = !!data.wanted
        this.$message.success(data.message || '操作成功')
      } catch (e) {
        this.$message.error('操作失败')
      }
    },
    async confirmOrder() {
      if (!this.selectedAddressId) {
        this.$message.warning('请选择收货地址')
        return
      }
      this.orderLoading = true
      try {
        const addr = this.addresses.find(a => a.id === this.selectedAddressId)
        const addressParts = [addr.province, addr.city, addr.district, addr.detail].filter(Boolean)
        await createOrder({
          bookId: this.book.id,
          address: addressParts.join(''),
          phone: addr.phone,
          receiverName: addr.receiverName
        })
        this.$message.success('下单成功')
        this.orderDialogVisible = false
        this.$router.push('/my-orders')
      } catch (e) { this.$message.error('下单失败') } finally {
        this.orderLoading = false
      }
    },
    onOrderDialogOpen() {
      this.showAddrForm = false
      this.addrFormInline = { id: null, receiverName: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false }
    },
    openAddrForm(addr) {
      if (addr) {
        this.addrFormInline = { ...addr, isDefault: !!addr.isDefault }
      } else {
        this.addrFormInline = { id: null, receiverName: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false }
      }
      this.showAddrForm = true
    },
    async saveAddrInline() {
      try {
        await this.$refs.addrFormInline.validate()
      } catch { return }
      this.addrSaving = true
      try {
        const payload = { ...this.addrFormInline, isDefault: this.addrFormInline.isDefault ? 1 : 0 }
        const res = await saveAddressApi(payload)
        this.$message.success(this.addrFormInline.id ? '地址已更新' : '地址已添加')
        this.showAddrForm = false
        await this.reloadAddresses()
        // Auto-select the newly saved address
        if (res.data && res.data.id) {
          this.selectedAddressId = res.data.id
        } else if (!this.selectedAddressId && this.addresses.length) {
          const def = this.addresses.find(a => a.isDefault)
          this.selectedAddressId = def ? def.id : this.addresses[0].id
        }
      } catch (e) { this.$message.error('保存地址失败') } finally {
        this.addrSaving = false
      }
    },
    async reloadAddresses() {
      try {
        const addrRes = await getAddressList()
        this.addresses = addrRes.data || []
      } catch { /* ignore */ }
    },
    async handleFavorite() {
      try {
        const res = await toggleFavorite(this.book.id)
        const data = res.data || {}
        this.book.isFavorited = !!data.favorited
        this.$message.success(data.message || '操作成功')
      } catch (e) { this.$message.error('操作失败') }
    },
    async openExchangeDialog() {
      await this.fetchMyBooks()
      this.exchangeDialogVisible = true
    },
    handleChat() {
      this.$router.push({ path: '/chat', query: { userId: this.book.sellerId } })
    },
    handleDelete() {
      this.$confirm('确定删除此书？删除后不可恢复。', '警告', {
        type: 'warning',
        confirmButtonText: '确定删除',
        confirmButtonClass: 'el-button--danger'
      }).then(async () => {
        try {
          await deleteBook(this.book.id)
          this.$message.success('已删除')
          this.$router.push('/my-books')
        } catch (e) {
          this.$message.error('删除失败')
        }
      }).catch(() => {})
    },
    handleExchange() {
      this.$refs.exchangeForm.validate(async (valid) => {
        if (!valid) return
        this.exchangeLoading = true
        try {
          await createExchange({
            targetBookId: this.book.id,
            myBookId: this.exchangeForm.myBookId,
            message: this.exchangeForm.message
          })
          this.$message.success('换书请求已发送')
          this.exchangeDialogVisible = false
          this.exchangeForm = { myBookId: '', message: '' }
        } catch (e) { this.$message.error('发送换书请求失败') } finally {
          this.exchangeLoading = false
        }
      })
    },
    handleCommentImageSuccess(res) {
      const url = typeof res.data === 'string' ? res.data : (res.data?.url || res.data)
      if (url) this.commentForm.images.push(url)
    },
    handleCommentImageRemove(file) {
      const url = file.url || (typeof file.response?.data === 'string' ? file.response.data : file.response?.data?.url)
      const idx = this.commentForm.images.indexOf(url)
      if (idx > -1) this.commentForm.images.splice(idx, 1)
    },
    async submitComment() {
      if (!this.commentForm.orderId) {
        this.$message.warning('请选择要评价的订单')
        return
      }
      if (!this.commentForm.content.trim()) {
        this.$message.warning('请输入评价内容')
        return
      }
      this.commentLoading = true
      try {
        const payload = {
          orderId: this.commentForm.orderId,
          bookId: this.book.id,
          rating: this.commentForm.rating,
          content: this.commentForm.content
        }
        if (this.commentForm.images.length) {
          payload.images = JSON.stringify(this.commentForm.images)
        }
        await createComment(payload)
        this.$message.success('评价成功')
        this.commentForm = { orderId: '', rating: 5, content: '', images: [] }
        this.commentImageList = []
        this.fetchComments()
        this.fetchCompletedOrders()
      } catch (e) { this.$message.error('评价失败') } finally {
        this.commentLoading = false
      }
    }
  }
}
</script>

<style scoped>
.book-detail {
  padding-bottom: 20px;
}
.breadcrumb-nav {
  margin-bottom: 12px;
}
.addr-radio-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 8px;
  transition: border-color 0.2s;
}
.addr-radio-item:hover {
  border-color: #409EFF;
}
.addr-name {
  font-weight: 600;
  color: #303133;
}
.addr-phone {
  color: #606266;
  font-size: 14px;
  margin-left: 8px;
}
.addr-detail {
  font-size: 13px;
  color: #909399;
  margin: 4px 0 0;
}
.detail-card {
  margin-bottom: 20px;
}
.detail-top {
  display: flex;
  gap: 32px;
}
.detail-left {
  width: 420px;
  flex-shrink: 0;
}
.carousel-image {
  width: 100%;
  height: 400px;
  border-radius: 4px;
}
.image-slot {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 48px;
}
.detail-right {
  flex: 1;
  min-width: 0;
}
.book-title {
  font-size: 22px;
  color: #303133;
  margin: 0 0 16px;
}
.book-desc-table {
  margin-bottom: 20px;
}
.original-price {
  color: #c0c4cc;
  text-decoration: line-through;
}
.selling-price {
  font-size: 24px;
  font-weight: 700;
  color: #f56c6c;
}
.stock-low {
  color: #e6a23c;
  font-weight: 600;
}
.seller-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  margin-bottom: 16px;
  border-top: 1px solid #ebeef5;
}
.seller-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.seller-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}
.action-btns {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.detail-bottom {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
.detail-bottom h3 {
  font-size: 16px;
  margin-bottom: 12px;
}
.description-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
}
.comment-section {
  margin-bottom: 20px;
}
.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}
.comment-item:last-child {
  border-bottom: none;
}
.comment-body {
  flex: 1;
}
.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.comment-user {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
}
.comment-time {
  font-size: 12px;
  color: #c0c4cc;
}
.comment-content {
  font-size: 14px;
  color: #606266;
  margin: 0;
  line-height: 1.6;
}
.comment-images {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.comment-img {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  cursor: pointer;
}
.follow-up-btn {
  margin-top: 4px;
  padding: 0;
  font-size: 12px;
}
.follow-ups {
  margin-top: 8px;
  padding-left: 12px;
  border-left: 3px solid #e4e7ed;
}
.follow-up-item {
  padding: 8px 0;
}
.follow-up-item:not(:last-child) {
  border-bottom: 1px dashed #ebeef5;
}
.follow-up-label {
  font-size: 12px;
  color: #409eff;
  font-weight: 600;
  margin-right: 8px;
}
.follow-up-time {
  font-size: 12px;
  color: #909399;
}
.follow-up-content {
  font-size: 13px;
  color: #606266;
  margin: 4px 0 0;
  line-height: 1.5;
}
.comment-form {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 20px;
}
.comment-form h4 {
  margin: 0 0 16px;
  font-size: 15px;
  color: #303133;
}
.no-order-tip {
  font-size: 14px;
  color: #909399;
  margin: 0;
}
.no-order-tip a {
  color: #409eff;
  text-decoration: none;
}
.no-order-tip a:hover {
  text-decoration: underline;
}
</style>
