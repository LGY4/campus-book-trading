<template>
  <div class="my-footprint">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>浏览足迹</h2>
          <el-button
            v-if="books.length"
            type="danger"
            size="small"
            plain
            icon="el-icon-delete"
            @click="handleClearAll"
          >清空足迹</el-button>
        </div>
      </template>
      <div v-if="books.length" v-loading="loading" class="book-grid">
        <div v-for="book in books" :key="book.interactionId" class="book-card-wrap">
          <el-card
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
                <span class="view-time">{{ formatTime(book.viewTime) }}</span>
              </div>
            </div>
          </el-card>
          <el-button
            class="delete-btn"
            type="danger"
            size="mini"
            icon="el-icon-delete"
            circle
            @click.stop="handleDelete(book)"
          />
        </div>
      </div>
      <el-empty v-else description="还没有浏览记录" />
      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="pageSize"
          :current-page.sync="page"
          @current-change="fetchFootprint"
        />
      </div>
    </el-card>
  </div>
</template>

<script>
import { getFootprint, deleteFootprint, clearFootprint } from '@/api/interaction'

export default {
  name: 'MyFootprint',
  data() {
    return {
      books: [],
      page: 1,
      pageSize: 16,
      total: 0,
      loading: false
    }
  },
  created() {
    this.fetchFootprint()
  },
  watch: {
    '$route'(to) {
      if (to.path === '/my-footprint') {
        this.page = 1
        this.fetchFootprint()
      }
    }
  },
  methods: {
    async fetchFootprint() {
      this.loading = true
      try {
        const res = await getFootprint({ page: this.page, size: this.pageSize })
        this.books = res.data.records || []
        this.total = res.data.total || 0
      } catch (e) {
        this.$message.error('加载足迹失败')
      } finally {
        this.loading = false
      }
    },
    handleDelete(book) {
      this.$confirm('删除此条浏览记录？', '提示', { type: 'warning' }).then(async () => {
        try {
          await deleteFootprint(book.interactionId)
          this.books = this.books.filter(b => b.interactionId !== book.interactionId)
          this.total--
          this.$message.success('已删除')
        } catch (e) {
          this.$message.error('删除失败')
        }
      }).catch(() => {})
    },
    handleClearAll() {
      this.$confirm('确定清空所有浏览足迹？此操作不可恢复。', '清空足迹', {
        type: 'warning',
        confirmButtonText: '确定清空',
        confirmButtonClass: 'el-button--danger'
      }).then(async () => {
        try {
          await clearFootprint()
          this.books = []
          this.total = 0
          this.$message.success('已清空')
        } catch (e) {
          this.$message.error('清空失败')
        }
      }).catch(() => {})
    },
    formatTime(val) {
      if (!val) return ''
      const d = new Date(val)
      const now = new Date()
      if (d.toDateString() === now.toDateString()) {
        return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      }
      return d.toLocaleDateString('zh-CN')
    }
  }
}
</script>

<style scoped>
.my-footprint h2 { margin: 0; font-size: 20px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.book-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.book-card-wrap { position: relative; }
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
.view-time { font-size: 12px; color: #c0c4cc; }
.delete-btn { position: absolute; top: 8px; right: 8px; opacity: 0; transition: opacity 0.2s; }
.book-card-wrap:hover .delete-btn { opacity: 1; }
.pagination-wrap { text-align: center; margin-top: 20px; }
</style>
