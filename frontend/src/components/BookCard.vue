<template>
  <div class="book-card" @click="goDetail">
    <div class="book-cover">
      <el-image :src="book.coverImage" fit="cover" lazy>
        <div slot="error" class="cover-placeholder">
          <i class="el-icon-picture-outline"></i>
        </div>
        <div slot="placeholder" class="cover-placeholder">
          <i class="el-icon-loading"></i>
        </div>
      </el-image>
      <el-tag
        v-if="book.bookCondition"
        class="condition-tag"
        size="mini"
        :type="conditionType(book.bookCondition)"
      >{{ book.bookCondition }}</el-tag>
    </div>
    <div class="book-info">
      <h3 class="book-title" :title="book.title">{{ book.title }}</h3>
      <div class="book-price">¥{{ book.sellingPrice }}</div>
      <div class="book-seller">
        <el-avatar :size="20" :src="book.sellerAvatar" icon="el-icon-user-solid" />
        <span class="seller-name">{{ book.sellerName }}</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'BookCard',
  props: {
    book: { type: Object, required: true }
  },
  methods: {
    goDetail() {
      this.$router.push(`/book/${this.book.id}`)
    },
    conditionType(c) {
      const map = { '全新': 'success', '九成新': 'success', '八成新': 'warning', '七成新及以下': 'danger' }
      return map[c] || 'info'
    }
  }
}
</script>

<style scoped>
.book-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #ebeef5;
}
.book-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}
.book-cover {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
}
.book-cover .el-image {
  width: 100%;
  height: 100%;
}
.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 32px;
}
.condition-tag {
  position: absolute;
  top: 8px;
  right: 8px;
}
.book-info {
  padding: 12px;
}
.book-title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.book-price {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
  margin-bottom: 8px;
}
.book-seller {
  display: flex;
  align-items: center;
  gap: 6px;
}
.seller-name {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
