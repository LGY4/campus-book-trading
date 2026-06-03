<template>
  <div class="comment-manage">
    <el-card shadow="never">
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索评论内容"
          clearable
          style="width: 200px"
          @keyup.enter.native="handleSearch"
        >
          <el-button slot="append" icon="el-icon-search" @click="handleSearch" />
        </el-input>
        <el-select v-model="ratingFilter" placeholder="评分筛选" clearable style="width: 130px; margin-left: 10px" @change="handleSearch">
          <el-option label="1星" :value="1" />
          <el-option label="2星" :value="2" />
          <el-option label="3星" :value="3" />
          <el-option label="4星" :value="4" />
          <el-option label="5星" :value="5" />
        </el-select>
        <el-input
          v-model="bookIdFilter"
          placeholder="书籍ID"
          clearable
          style="width: 120px; margin-left: 10px"
          @keyup.enter.native="handleSearch"
        />
        <el-input
          v-model="userIdFilter"
          placeholder="用户ID"
          clearable
          style="width: 120px; margin-left: 10px"
          @keyup.enter.native="handleSearch"
        />
      </div>
      <el-table :data="comments" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="bookTitle" label="书籍" min-width="120" show-overflow-tooltip />
        <el-table-column prop="userNickname" label="用户" width="100" />
        <el-table-column prop="content" label="评论内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="评分" width="160">
          <template slot-scope="{ row }">
            <el-rate :value="row.rating" disabled show-score score-template="{value}" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="评论时间" min-width="160" />
        <el-table-column label="操作" width="100" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="danger" size="mini" @click="handleDelete(row)">删除</el-button>
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
import { getAdminComments, deleteComment } from '@/api/admin'

export default {
  name: 'CommentManage',
  data() {
    return {
      comments: [],
      loading: false,
      total: 0,
      currentPage: 1,
      pageSize: 10,
      keyword: '',
      ratingFilter: null,
      bookIdFilter: '',
      userIdFilter: ''
    }
  },
  created() {
    this.fetchComments()
  },
  methods: {
    async fetchComments() {
      this.loading = true
      try {
        const params = { page: this.currentPage, size: this.pageSize }
        if (this.keyword) params.keyword = this.keyword
        if (this.ratingFilter) {
          params.minRating = this.ratingFilter
          params.maxRating = this.ratingFilter
        }
        if (this.bookIdFilter) params.bookId = this.bookIdFilter
        if (this.userIdFilter) params.userId = this.userIdFilter
        const res = await getAdminComments(params)
        const data = res.data || res
        this.comments = data.records || data.list || []
        this.total = data.total || 0
      } catch (e) {
        console.error('获取评论列表失败', e)
        this.$message.error('获取评论列表失败')
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.currentPage = 1
      this.fetchComments()
    },
    handlePageChange(page) {
      this.currentPage = page
      this.fetchComments()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.fetchComments()
    },
    async handleDelete(row) {
      try {
        await this.$confirm('确定删除该评论？', '提示', { type: 'warning' })
        await deleteComment(row.id)
        this.$message.success('删除成功')
        this.fetchComments()
      } catch (e) { if (e !== 'cancel') this.$message.error('删除失败') }
    }
  }
}
</script>

<style scoped>
.search-bar {
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
  text-align: right;
}
</style>
