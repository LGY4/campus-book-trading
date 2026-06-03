<template>
  <div class="book-list">
    <div v-if="loading" class="book-grid">
      <div v-for="n in 8" :key="n" class="skeleton-card">
        <el-skeleton :loading="true" animated>
          <template slot="template">
            <el-skeleton-item variant="image" style="width: 100%; height: 200px" />
            <div style="padding: 12px">
              <el-skeleton-item variant="h3" style="width: 80%; height: 16px; margin-bottom: 8px" />
              <el-skeleton-item variant="text" style="width: 40%; height: 20px; margin-bottom: 8px" />
              <el-skeleton-item variant="text" style="width: 50%; height: 14px" />
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <div v-else-if="books.length === 0" class="empty-state">
      <el-empty description="暂无书籍" />
    </div>

    <div v-else class="book-grid">
      <BookCard v-for="book in books" :key="book.id" :book="book" />
    </div>
  </div>
</template>

<script>
import BookCard from './BookCard.vue'

export default {
  name: 'BookList',
  components: { BookCard },
  props: {
    books: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false }
  }
}
</script>

<style scoped>
.book-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.skeleton-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #ebeef5;
}
.empty-state {
  padding: 60px 0;
}
@media (max-width: 1200px) {
  .book-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 900px) {
  .book-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 600px) {
  .book-grid { grid-template-columns: 1fr; }
}
</style>
