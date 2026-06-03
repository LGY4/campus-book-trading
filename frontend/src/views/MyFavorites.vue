<template>
  <div class="my-favorites">
    <el-card>
      <h2 slot="header">我的收藏</h2>
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
              <el-tooltip content="取消收藏" placement="top">
                <el-button
                  type="text"
                  icon="el-icon-star-on"
                  class="unfav-btn"
                  @click.stop="handleUnfavorite(book)"
                />
              </el-tooltip>
            </div>
          </div>
        </el-card>
      </div>
      <el-empty v-else description="还没有收藏书籍" />
      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="pageSize"
          :current-page.sync="page"
          @current-change="fetchFavorites"
        />
      </div>
    </el-card>
  </div>
</template>

<script>
import { getFavorites, toggleFavorite } from '@/api/interaction'

export default {
  name: 'MyFavorites',
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
    this.fetchFavorites()
  },
  watch: {
    '$route'(to) {
      if (to.path === '/my-favorites') {
        this.page = 1
        this.fetchFavorites()
      }
    }
  },
  methods: {
    async fetchFavorites() {
      this.loading = true
      try {
        const res = await getFavorites({ page: this.page, size: this.pageSize })
        this.books = (res.data.records || []).filter(b => b != null)
        this.total = res.data.total || 0
      } catch (e) {
        this.$message.error('加载收藏失败')
      } finally {
        this.loading = false
      }
    },
    async handleUnfavorite(book) {
      try {
        await toggleFavorite(book.id)
        this.books = this.books.filter(b => b.interactionId !== book.interactionId)
        this.total--
        this.$message.success('已取消收藏')
      } catch (e) {
        this.$message.error('操作失败')
      }
    }
  }
}
</script>

<style scoped>
.my-favorites h2 {
  margin: 0;
  font-size: 20px;
}
.book-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.book-card {
  cursor: pointer;
  transition: transform 0.2s;
  border-radius: 8px;
  overflow: hidden;
}
.book-card:hover {
  transform: translateY(-4px);
}
.book-card >>> .el-card__body {
  padding: 0;
}
.book-cover {
  width: 100%;
  height: 200px;
  display: block;
}
.image-slot {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 32px;
}
.book-info {
  padding: 12px;
}
.book-title {
  font-size: 14px;
  color: #303133;
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.book-meta {
  margin-bottom: 8px;
}
.price {
  font-size: 18px;
  font-weight: 700;
  color: #f56c6c;
}
.original-price {
  font-size: 13px;
  color: #c0c4cc;
  text-decoration: line-through;
  margin-left: 8px;
}
.book-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.seller {
  display: flex;
  align-items: center;
  gap: 4px;
}
.seller-name {
  font-size: 12px;
  color: #909399;
}
.unfav-btn {
  color: #e6a23c;
  font-size: 18px;
}
.pagination-wrap {
  text-align: center;
  margin-top: 20px;
}
</style>
