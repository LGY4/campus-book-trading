<template>
  <div class="book-manage">
    <el-card shadow="never">
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索书名/作者"
          clearable
          style="width: 220px"
          @keyup.enter.native="handleSearch"
        >
          <el-button slot="append" icon="el-icon-search" @click="handleSearch" />
        </el-input>
        <el-select v-model="categoryId" placeholder="分类筛选" clearable style="width: 150px; margin-left: 10px" @change="handleSearch">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="status" placeholder="状态筛选" clearable style="width: 150px; margin-left: 10px" @change="handleSearch">
          <el-option label="待审核" value="PENDING" />
          <el-option label="在售" value="ON_SALE" />
          <el-option label="已下架" value="OFF_SHELF" />
          <el-option label="已售出" value="SOLD" />
          <el-option label="已拒绝" value="REJECTED" />
        </el-select>
        <el-button type="success" icon="el-icon-download" style="margin-left: 10px" @click="handleExport">导出</el-button>
      </div>

      <el-table :data="books" v-loading="loading" stripe style="width: 100%">
        <el-table-column label="封面" width="80">
          <template slot-scope="{ row }">
            <el-image
              :src="row.coverImage"
              style="width: 50px; height: 60px"
              fit="cover"
            >
              <div slot="error" class="image-placeholder">
                <i class="el-icon-picture-outline"></i>
              </div>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="书名" min-width="150" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" min-width="100" show-overflow-tooltip />
        <el-table-column label="价格" width="90">
          <template slot-scope="{ row }">
            <span style="color: #f56c6c; font-weight: bold">¥{{ row.sellingPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sellerName" label="卖家" min-width="100" />
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column label="状态" width="90">
          <template slot-scope="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="库存" width="70">
          <template slot-scope="{ row }">
            <span :style="{ color: row.quantity <= 0 ? '#f56c6c' : '#303133', fontWeight: row.quantity <= 0 ? 'bold' : 'normal' }">{{ row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览量" width="80" />
        <el-table-column label="操作" width="280" fixed="right">
          <template slot-scope="{ row }">
            <el-tooltip :disabled="row.quantity > 0" content="库存为0，无法上架" placement="top">
              <span>
                <el-button
                  v-if="row.status === 'PENDING'"
                  type="success" size="mini"
                  :disabled="row.quantity <= 0"
                  @click="handleApprove(row)"
                >通过</el-button>
              </span>
            </el-tooltip>
            <el-button
              v-if="row.status === 'PENDING'"
              type="warning" size="mini"
              @click="handleReject(row)"
            >拒绝</el-button>
            <el-button
              v-if="row.status !== 'OFF_SHELF' && row.status !== 'SOLD'"
              type="danger" size="mini"
              @click="handleRemove(row)"
            >下架</el-button>
            <el-tooltip :disabled="row.quantity > 0" content="库存为0，无法上架" placement="top">
              <span>
                <el-button
                  v-if="row.status === 'OFF_SHELF'"
                  type="success" size="mini"
                  :disabled="row.quantity <= 0"
                  @click="handlePutOn(row)"
                >上架</el-button>
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </el-card>
  </div>
</template>

<script>
import { getAdminBooks, changeBookStatus, approveBook, rejectBook, exportBooks } from '@/api/admin'
import { getCategories } from '@/api/category'

export default {
  name: 'BookManage',
  data() {
    return {
      keyword: '',
      categoryId: '',
      status: '',
      categories: [],
      books: [],
      loading: false,
      total: 0,
      currentPage: 1,
      pageSize: 10
    }
  },
  created() {
    this.fetchCategories()
    this.fetchBooks()
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
        const res = await getAdminBooks({
          keyword: this.keyword,
          categoryId: this.categoryId,
          status: this.status,
          page: this.currentPage,
          size: this.pageSize
        })
        const data = res.data || res
        this.books = data.records || data.list || []
        this.total = data.total || 0
      } catch {
        this.$message.error('获取书籍列表失败')
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.currentPage = 1
      this.fetchBooks()
    },
    handlePageChange(page) {
      this.currentPage = page
      this.fetchBooks()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.fetchBooks()
    },
    statusLabel(s) {
      const map = { PENDING: '待审核', ON_SALE: '在售', OFF_SHELF: '已下架', SOLD: '已售出', REJECTED: '已拒绝' }
      return map[s] || s
    },
    statusTagType(s) {
      const map = { PENDING: 'warning', ON_SALE: 'success', OFF_SHELF: 'info', SOLD: '', REJECTED: 'danger' }
      return map[s] || ''
    },
    async handleApprove(row) {
      try {
        await this.$confirm('确定通过该书籍审核？', '提示', { type: 'success' })
        await approveBook(row.id)
        this.$message.success('审核通过')
        this.fetchBooks()
      } catch (e) { if (e !== 'cancel') this.$message.error('审核操作失败') }
    },
    async handleReject(row) {
      try {
        const { value } = await this.$prompt('请输入拒绝原因', '拒绝书籍', {
          confirmButtonText: '确定拒绝',
          cancelButtonText: '取消',
          inputPattern: /\S+/,
          inputErrorMessage: '请输入拒绝原因',
          type: 'warning'
        })
        await rejectBook(row.id, value || '不符合发布规范')
        this.$message.success('已拒绝')
        this.fetchBooks()
      } catch (e) { if (e !== 'cancel') this.$message.error('拒绝操作失败') }
    },
    async handleExport() {
      try {
        await exportBooks({ keyword: this.keyword, status: this.status })
        this.$message.success('导出成功')
      } catch {
        this.$message.error('导出失败')
      }
    },
    async handleRemove(row) {
      try {
        await this.$confirm('确定下架该书籍？', '提示', { type: 'warning' })
        await changeBookStatus(row.id, 'OFF_SHELF')
        this.$message.success('已下架')
        this.fetchBooks()
      } catch (e) { if (e !== 'cancel') this.$message.error('下架操作失败') }
    },
    async handlePutOn(row) {
      try {
        await this.$confirm('确定重新上架该书籍？', '提示', { type: 'success' })
        await changeBookStatus(row.id, 'ON_SALE')
        this.$message.success('已上架')
        this.fetchBooks()
      } catch (e) { if (e !== 'cancel') this.$message.error('上架操作失败') }
    }
  }
}
</script>

<style scoped>
.search-bar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}
.pagination {
  margin-top: 16px;
  text-align: right;
}
.image-placeholder {
  width: 50px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 20px;
}
</style>
