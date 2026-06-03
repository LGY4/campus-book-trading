<template>
  <div class="my-books">
    <el-card>
      <h2 slot="header">我的发布</h2>

      <div class="toolbar">
        <el-checkbox
          v-model="checkAll"
          :indeterminate="isIndeterminate"
          @change="handleCheckAll"
        >全选</el-checkbox>
        <el-button
          type="danger"
          size="small"
          icon="el-icon-delete"
          :disabled="!selectedIds.length"
          @click="handleBatchDelete"
        >批量删除 ({{ selectedIds.length }})</el-button>
      </div>

      <el-table
        :data="books"
        v-loading="loading"
        stripe
        style="width: 100%;"
        @selection-change="handleSelectionChange"
        ref="bookTable"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column label="封面" width="80">
          <template slot-scope="{ row }">
            <el-image :src="row.coverImage" fit="cover" style="width:50px;height:60px;border-radius:4px;">
              <div slot="error" class="image-slot">
                <i class="el-icon-picture-outline" style="font-size:20px;" />
              </div>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="书名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="sellingPrice" label="售价" width="100">
          <template slot-scope="{ row }">
            <span class="price">¥{{ row.sellingPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="库存" width="80">
          <template slot-scope="{ row }">
            <span :class="{ 'stock-low': row.quantity <= 1 }">{{ row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="bookCondition" label="品相" width="120">
          <template slot-scope="{ row }">
            <el-tag :type="conditionType(row.bookCondition)" size="small">{{ row.bookCondition }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template slot-scope="{ row }">
            <el-tooltip :disabled="row.status !== 'REJECTED' || !row.rejectReason" :content="'驳回原因：' + row.rejectReason" placement="top">
              <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
            </el-tooltip>
            <div v-if="row.status === 'REJECTED' && row.rejectReason" class="reject-reason">{{ row.rejectReason }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="small" @click="$router.push('/book/' + row.id)">查看</el-button>
            <el-button type="text" size="small" @click="$router.push('/publish?bookId=' + row.id)">编辑</el-button>
            <el-button
              v-if="row.status === 'ON_SALE'"
              type="text"
              size="small"
              class="warning-text"
              @click="handleDelist(row)"
            >下架</el-button>
            <el-button
              v-if="row.status === 'REJECTED'"
              type="text"
              size="small"
              class="warning-text"
              @click="handleResubmit(row)"
            >重新提交</el-button>
            <el-button type="text" size="small" class="danger-text" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="pageSize"
          :current-page.sync="page"
          @current-change="fetchBooks"
        />
      </div>
      <el-empty v-if="!books.length && loaded" description="还没有发布书籍" />
    </el-card>
  </div>
</template>

<script>
import { getMyBooks, deleteBook, delistBook, batchDeleteBooks, updateBook } from '@/api/book'

export default {
  name: 'MyBooks',
  data() {
    return {
      books: [],
      page: 1,
      pageSize: 10,
      total: 0,
      loaded: false,
      loading: false,
      selectedIds: [],
      checkAll: false,
      isIndeterminate: false
    }
  },
  created() {
    this.fetchBooks()
  },
  methods: {
    async fetchBooks() {
      this.loading = true
      try {
        const res = await getMyBooks({ page: this.page, size: this.pageSize })
        this.books = res.data.records || []
        this.total = res.data.total || 0
        this.selectedIds = []
        this.checkAll = false
        this.isIndeterminate = false
      } catch (e) { this.$message.error('加载书籍列表失败') } finally {
        this.loaded = true
        this.loading = false
      }
    },
    handleSelectionChange(rows) {
      this.selectedIds = rows.map(r => r.id)
      const total = this.books.length
      this.checkAll = total > 0 && this.selectedIds.length === total
      this.isIndeterminate = this.selectedIds.length > 0 && this.selectedIds.length < total
    },
    handleCheckAll(val) {
      if (val) {
        this.$refs.bookTable.clearSelection()
        this.books.forEach(row => this.$refs.bookTable.toggleRowSelection(row, true))
      } else {
        this.$refs.bookTable.clearSelection()
      }
      this.isIndeterminate = false
    },
    conditionType(val) {
      const map = { '全新': 'success', '九成新': 'success', '八成新': 'warning', '七成新及以下': 'info' }
      return map[val] || 'info'
    },
    statusType(val) {
      const map = { ON_SALE: 'success', SOLD: 'info', OFF_SHELF: 'warning', PENDING: 'warning', REJECTED: 'danger' }
      return map[val] || 'info'
    },
    statusText(val) {
      const map = { ON_SALE: '在售', SOLD: '已售', OFF_SHELF: '已下架', PENDING: '待审核', REJECTED: '已驳回' }
      return map[val] || val
    },
    handleDelist(row) {
      this.$confirm(`确定下架《${row.title}》？下架后买家将无法购买。`, '下架确认', { type: 'warning' }).then(async () => {
        try {
          await delistBook(row.id)
          this.$message.success('已下架')
          this.fetchBooks()
        } catch (e) { this.$message.error('下架失败') }
      }).catch(() => {})
    },
    handleDelete(row) {
      this.$confirm(`确定删除《${row.title}》？删除后不可恢复。`, '警告', {
        type: 'warning',
        confirmButtonText: '确定删除',
        confirmButtonClass: 'el-button--danger'
      }).then(async () => {
        try {
          await deleteBook(row.id)
          this.$message.success('已删除')
          this.fetchBooks()
        } catch (e) { this.$message.error('删除失败') }
      }).catch(() => {})
    },
    handleBatchDelete() {
      this.$confirm(`确定删除选中的 ${this.selectedIds.length} 本书籍？删除后不可恢复。`, '批量删除', {
        type: 'warning',
        confirmButtonText: '确定删除',
        confirmButtonClass: 'el-button--danger'
      }).then(async () => {
        try {
          await batchDeleteBooks(this.selectedIds)
          this.$message.success(`已删除 ${this.selectedIds.length} 本书籍`)
          this.fetchBooks()
        } catch (e) { this.$message.error('批量删除失败') }
      }).catch(() => {})
    },
    handleResubmit(row) {
      this.$confirm('重新提交审核？', '提示', { type: 'info' }).then(async () => {
        try {
          await updateBook({
            id: row.id,
            title: row.title,
            author: row.author,
            publisher: row.publisher,
            isbn: row.isbn,
            categoryId: row.categoryId,
            bookCondition: row.bookCondition,
            originalPrice: row.originalPrice,
            sellingPrice: row.sellingPrice,
            quantity: row.quantity,
            description: row.description,
            coverImage: row.coverImage,
            imagesJson: row.imagesJson
          })
          this.$message.success('已重新提交审核')
          this.fetchBooks()
        } catch (e) { this.$message.error('重新提交失败') }
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.my-books h2 {
  margin: 0;
  font-size: 20px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
}
.price {
  color: #f56c6c;
  font-weight: 600;
}
.stock-low {
  color: #e6a23c;
  font-weight: 600;
}
.warning-text {
  color: #e6a23c;
}
.danger-text {
  color: #f56c6c;
}
.reject-reason {
  font-size: 11px;
  color: #f56c6c;
  margin-top: 2px;
  line-height: 1.2;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pagination-wrap {
  text-align: center;
  margin-top: 20px;
}
.image-slot {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #c0c4cc;
}
</style>
