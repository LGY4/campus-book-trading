<template>
  <div class="home-container">
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索书名、作者、ISBN..."
        size="large"
        clearable
        @keyup.enter.native="handleSearch"
        class="search-input"
      >
        <el-select slot="prepend" v-model="categoryId" placeholder="全部分类" clearable style="width: 140px;">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-button slot="append" icon="el-icon-search" @click="handleSearch" />
      </el-input>
      <div class="price-filter">
        <el-input v-model="minPrice" placeholder="最低价" clearable style="width: 120px" size="small" type="number" />
        <span style="margin: 0 4px; color: #909399">-</span>
        <el-input v-model="maxPrice" placeholder="最高价" clearable style="width: 120px" size="small" type="number" />
        <el-button size="small" type="primary" style="margin-left: 8px" @click="handleSearch">筛选</el-button>
      </div>
    </div>

    <div class="section">
      <h3 class="section-title">{{ bannerMode ? '精选推荐' : '推荐书籍' }}</h3>
      <el-carousel v-if="recommended.length" :interval="4000" type="card" height="220px" @change="onCarouselChange">
        <el-carousel-item v-for="book in recommended" :key="book.id">
          <div class="carousel-card" @click="$router.push('/book/' + book.id)">
            <el-image :src="book.coverImage" fit="cover" class="carousel-img">
              <div slot="error" class="image-slot">
                <i class="el-icon-picture-outline" />
              </div>
            </el-image>
            <div class="carousel-info">
              <h4>{{ book.title }}</h4>
              <p class="price">¥{{ book.sellingPrice }}</p>
              <p v-if="book.sellerName" class="seller">卖家: {{ book.sellerName }}</p>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
      <div v-else class="carousel-empty">
        <i class="el-icon-picture-outline" style="font-size: 48px; color: #c0c4cc;"></i>
        <p style="color: #909399; margin-top: 8px;">暂无推荐</p>
      </div>
    </div>

    <div class="section">
      <h3 class="section-title">{{ keyword ? '搜索结果' : '最新发布' }}</h3>
      <div v-if="books.length" v-loading="loading" class="book-grid">
        <el-card
          v-for="book in books"
          :key="book.id"
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
              <el-tag :type="conditionType(book.bookCondition)" size="mini">{{ book.bookCondition }}</el-tag>
            </div>
          </div>
        </el-card>
      </div>
      <el-empty v-else description="暂无书籍" />
      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page.sync="page"
          @current-change="fetchBooks"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { getBooks, searchBooks, getRecommendations, getBanners } from '@/api/book'
import { getCategories } from '@/api/category'

export default {
  name: 'Home',
  data() {
    return {
      keyword: '',
      categoryId: '',
      minPrice: '',
      maxPrice: '',
      categories: [],
      books: [],
      recommended: [],
      bannerMode: false,
      page: 1,
      pageSize: 16,
      total: 0,
      loading: false
    }
  },
  created() {
    this.fetchCategories()
    this.fetchBooks()
    this.fetchRecommend()
  },
  methods: {
    async fetchCategories() {
      try {
        const res = await getCategories()
        this.categories = res.data || []
      } catch (e) { this.$message.error('加载分类失败') }
    },
    async fetchBooks() {
      this.loading = true
      try {
        let res
        if (this.keyword && !this.categoryId && !this.minPrice && !this.maxPrice) {
          // Pure keyword search: use FULLTEXT endpoint for better relevance
          res = await searchBooks({ keyword: this.keyword, page: this.page, size: this.pageSize })
        } else {
          // Filtered browsing: use list endpoint
          res = await getBooks({
            keyword: this.keyword,
            categoryId: this.categoryId,
            minPrice: this.minPrice || undefined,
            maxPrice: this.maxPrice || undefined,
            page: this.page,
            pageSize: this.pageSize
          })
        }
        this.books = res.data.records || []
        this.total = res.data.total || 0
      } catch (e) { this.$message.error('加载书籍失败') } finally {
        this.loading = false
      }
    },
    async fetchRecommend() {
      // 1. Try admin-configured banners
      try {
        const bannerRes = await getBanners()
        if (bannerRes.data && bannerRes.data.length) {
          this.recommended = bannerRes.data.map(b => ({
            id: b.bookId,
            title: b.title || b.bookTitle,
            coverImage: b.imageUrl || b.coverImage,
            sellingPrice: b.sellingPrice,
            sellerName: ''
          }))
          this.bannerMode = true
          return
        }
      } catch (e) { /* fallback */ }
      // 2. Try algorithm recommendations
      try {
        const res = await getRecommendations(5)
        if (res.data && res.data.length) {
          this.recommended = res.data
          this.bannerMode = false
          return
        }
      } catch (e) { /* fallback */ }
      // 3. Fallback: use latest books
      try {
        const res = await getBooks({ page: 1, size: 8 })
        this.recommended = (res.data && res.data.records) || []
        this.bannerMode = false
      } catch (e) { /* noop */ }
    },
    onCarouselChange() {},
    handleSearch() {
      this.page = 1
      this.fetchBooks()
    },
    conditionType(val) {
      const map = { '全新': 'success', '九成新': 'success', '八成新': 'warning', '七成新及以下': 'danger' }
      return map[val] || 'info'
    }
  }
}
</script>

<style scoped>
.home-container {
  padding-bottom: 20px;
}
.search-bar {
  margin-bottom: 24px;
}
.search-input {
  max-width: 700px;
}
.price-filter {
  margin-top: 10px;
  display: flex;
  align-items: center;
}
.section {
  margin-bottom: 32px;
}
.section-title {
  font-size: 20px;
  color: #303133;
  margin-bottom: 16px;
  font-weight: 600;
}
.carousel-card {
  height: 100%;
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
}
.carousel-img {
  width: 140px;
  height: 100%;
  flex-shrink: 0;
}
.carousel-info {
  padding: 16px;
}
.carousel-info h4 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #303133;
}
.carousel-info .seller {
  margin: 4px 0 0;
  font-size: 12px;
  color: #909399;
}
.carousel-empty {
  height: 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 8px;
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
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pagination-wrap {
  text-align: center;
  margin-top: 24px;
}
</style>
