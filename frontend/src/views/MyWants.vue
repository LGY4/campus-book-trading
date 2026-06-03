<template>
  <div class="my-wants">
    <el-card>
      <h2 slot="header">我想要的</h2>
      <div v-if="books.length" v-loading="loading" class="book-grid">
        <el-card
          v-for="book in books"
          :key="book.interactionId"
          shadow="hover"
          class="book-card"
          @click.native="$router.push('/book/' + book.id)"
        >
          <el-image :src="book.coverImage" fit="cover" class="book-cover">
            <div slot="error" class="image-slot">
              <i class="el-icon-picture-outline" />
            </div>
          </el-image>
          <div class="book-info">
            <h4 class="book-title" :title="book.title">{{ book.title }}</h4>
            <div class="book-meta">
              <span class="price">¥{{ book.sellingPrice }}</span>
              <span class="original-price">¥{{ book.originalPrice }}</span>
            </div>
            <div class="book-bottom">
              <div class="seller">
                <el-avatar :src="book.sellerAvatar" :size="20" icon="el-icon-user-solid" />
                <span class="seller-name">{{ book.sellerName }}</span>
              </div>
              <div class="card-actions">
                <el-tooltip content="立即下单" placement="top">
                  <el-button
                    type="text"
                    icon="el-icon-shopping-cart-2"
                    class="order-btn"
                    @click.stop="openOrderDialog(book)"
                  />
                </el-tooltip>
                <el-tooltip content="取消想要" placement="top">
                  <el-button
                    type="text"
                    icon="el-icon-shopping-bag-1"
                    class="unwant-btn"
                    @click.stop="handleUnwant(book)"
                  />
                </el-tooltip>
              </div>
            </div>
          </div>
        </el-card>
      </div>
      <el-empty v-else description="还没有想要的书籍" />
      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="pageSize"
          :current-page.sync="page"
          @current-change="fetchWants"
        />
      </div>
    </el-card>

    <el-dialog title="确认下单" :visible.sync="orderDialogVisible" width="500px">
      <div v-if="selectedBook" style="margin-bottom:16px;">
        <p style="margin:0 0 8px;font-weight:600;">{{ selectedBook.title }}</p>
        <p style="margin:0;color:#f56c6c;font-size:18px;font-weight:700;">¥{{ selectedBook.sellingPrice }}</p>
      </div>
      <div v-if="addresses.length">
        <p style="margin:0 0 12px;color:#909399;font-size:14px;">选择收货地址：</p>
        <el-radio-group v-model="selectedAddressId" style="width:100%;">
          <div v-for="addr in addresses" :key="addr.id" class="addr-radio-item">
            <el-radio :label="addr.id">
              <span class="addr-name">{{ addr.receiverName }}</span>
              <span class="addr-phone">{{ addr.phone }}</span>
              <el-tag v-if="addr.isDefault" type="danger" size="mini">默认</el-tag>
              <p class="addr-detail">{{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detail }}</p>
            </el-radio>
          </div>
        </el-radio-group>
      </div>
      <el-empty v-else description="暂无收货地址">
        <el-button type="primary" size="small" @click="$router.push('/profile')">去添加地址</el-button>
      </el-empty>
      <div slot="footer">
        <el-button @click="orderDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="orderLoading" :disabled="!selectedAddressId" @click="confirmOrder">确认下单</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getWants, toggleWant } from '@/api/interaction'
import { createOrder } from '@/api/order'
import { getAddressList } from '@/api/address'

export default {
  name: 'MyWants',
  data() {
    return {
      books: [],
      page: 1,
      pageSize: 16,
      total: 0,
      loading: false,
      orderDialogVisible: false,
      orderLoading: false,
      selectedBook: null,
      addresses: [],
      selectedAddressId: null
    }
  },
  created() {
    this.fetchWants()
  },
  watch: {
    '$route'(to) {
      if (to.path === '/my-wants') {
        this.page = 1
        this.fetchWants()
      }
    }
  },
  methods: {
    async fetchWants() {
      this.loading = true
      try {
        const res = await getWants({ page: this.page, size: this.pageSize })
        this.books = (res.data.records || []).filter(b => b != null)
        this.total = res.data.total || 0
      } catch (e) {
        this.$message.error('加载想要列表失败')
      } finally {
        this.loading = false
      }
    },
    async handleUnwant(book) {
      try {
        await toggleWant(book.id)
        this.books = this.books.filter(b => b.interactionId !== book.interactionId)
        this.total--
        this.$message.success('已取消想要')
      } catch (e) {
        this.$message.error('操作失败')
      }
    },
    async openOrderDialog(book) {
      this.selectedBook = book
      this.selectedAddressId = null
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
          bookId: this.selectedBook.id,
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
    }
  }
}
</script>

<style scoped>
.my-wants h2 { margin: 0; font-size: 20px; }
.book-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.book-card { cursor: pointer; transition: transform 0.2s; border-radius: 8px; overflow: hidden; }
.book-card:hover { transform: translateY(-4px); }
.book-card >>> .el-card__body { padding: 0; }
.book-cover { width: 100%; height: 200px; display: block; }
.image-slot { display: flex; align-items: center; justify-content: center; height: 100%; background: #f5f7fa; color: #c0c4cc; font-size: 32px; }
.book-info { padding: 12px; }
.book-title { font-size: 14px; color: #303133; margin: 0 0 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.book-meta { margin-bottom: 8px; }
.price { font-size: 18px; font-weight: 700; color: #f56c6c; }
.original-price { font-size: 13px; color: #c0c4cc; text-decoration: line-through; margin-left: 8px; }
.book-bottom { display: flex; align-items: center; justify-content: space-between; }
.seller { display: flex; align-items: center; gap: 4px; }
.seller-name { font-size: 12px; color: #909399; }
.unwant-btn { color: #409EFF; font-size: 18px; }
.card-actions { display: flex; gap: 8px; }
.order-btn { color: #67c23a; font-size: 18px; }
.addr-radio-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 8px;
  transition: border-color 0.2s;
}
.addr-radio-item:hover { border-color: #409EFF; }
.addr-name { font-weight: 600; color: #303133; }
.addr-phone { color: #606266; font-size: 14px; margin-left: 8px; }
.addr-detail { font-size: 13px; color: #909399; margin: 4px 0 0; }
.pagination-wrap { text-align: center; margin-top: 20px; }
</style>
