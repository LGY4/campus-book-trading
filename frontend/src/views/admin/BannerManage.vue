<template>
  <div class="banner-manage">
    <el-card shadow="never">
      <div style="margin-bottom: 16px;">
        <el-button type="primary" size="small" icon="el-icon-plus" @click="openDialog(null)">新增轮播</el-button>
      </div>
      <el-table :data="banners" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="图片" width="100">
          <template slot-scope="{ row }">
            <el-image :src="row.imageUrl" fit="cover" style="width: 60px; height: 60px; border-radius: 4px;">
              <div slot="error" class="image-slot"><i class="el-icon-picture-outline" /></div>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="展示标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="bookId" label="书籍ID" width="100" />
        <el-table-column prop="sortOrder" label="排序" width="100" />
        <el-table-column label="状态" width="100">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="small" @click="handleToggle(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="text" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button type="text" size="small" class="danger-text" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="total > pageSize" class="pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page.sync="currentPage"
          :page-sizes="[10, 20, 50]"
          @current-change="fetchBanners"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog :title="form.id ? '编辑轮播' : '新增轮播'" :visible.sync="dialogVisible" width="520px">
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="选择书籍" prop="bookId">
          <el-select
            v-model="form.bookId"
            filterable
            remote
            reserve-keyword
            :remote-method="searchBooks"
            :loading="bookSearching"
            placeholder="输入书名搜索"
            style="width: 100%"
            @change="onBookChange"
          >
            <el-option
              v-for="b in bookOptions"
              :key="b.id"
              :label="b.title"
              :value="b.id"
            >
              <span style="float: left">{{ b.title }}</span>
              <span style="float: right; color: #8492a6; font-size: 12px;">¥{{ b.sellingPrice }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="展示标题">
          <el-input v-model="form.title" placeholder="可选，默认使用书名" />
        </el-form-item>
        <el-form-item label="展示图片">
          <div style="display:flex;align-items:flex-start;gap:12px;">
            <el-upload
              action="/api/file/upload"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleImageSuccess"
              accept="image/*"
            >
              <el-button size="small" type="primary">上传图片</el-button>
            </el-upload>
            <el-input v-model="form.imageUrl" placeholder="或输入图片URL" style="flex:1;" />
          </div>
          <el-image v-if="form.imageUrl" :src="form.imageUrl" fit="cover" style="width:120px;height:80px;margin-top:8px;border-radius:4px;">
            <div slot="error" style="display:flex;align-items:center;justify-content:center;height:100%;background:#f5f7fa;color:#c0c4cc;">
              <i class="el-icon-picture-outline" />
            </div>
          </el-image>
        </el-form-item>
        <el-form-item label="排序值">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getAdminBanners, createBanner, updateBanner, deleteBanner, changeBannerStatus } from '@/api/admin'
import { getAdminBooks } from '@/api/admin'
import { getToken } from '@/utils/auth'

export default {
  name: 'BannerManage',
  data() {
    return {
      banners: [],
      loading: false,
      total: 0,
      currentPage: 1,
      pageSize: 10,
      dialogVisible: false,
      saving: false,
      form: { id: null, bookId: null, title: '', imageUrl: '', sortOrder: 0, status: 1 },
      rules: {
        bookId: [{ required: true, message: '请选择书籍', trigger: 'change' }]
      },
      bookOptions: [],
      bookSearching: false
    }
  },
  computed: {
    uploadHeaders() {
      return { Authorization: 'Bearer ' + getToken() }
    }
  },
  created() {
    this.fetchBanners()
    this.loadAllBooks()
  },
  methods: {
    async fetchBanners() {
      this.loading = true
      try {
        const res = await getAdminBanners({ page: this.currentPage, size: this.pageSize })
        this.banners = res.data.records || []
        this.total = res.data.total || 0
      } catch (e) {
        this.$message.error('获取轮播列表失败')
      } finally {
        this.loading = false
      }
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.fetchBanners()
    },
    async loadAllBooks() {
      try {
        const res = await getAdminBooks({ page: 1, size: 100 })
        this.bookOptions = res.data.records || []
      } catch (e) { /* ignore */ }
    },
    async searchBooks(query) {
      this.bookSearching = true
      try {
        const params = { page: 1, size: 20 }
        if (query) params.keyword = query
        const res = await getAdminBooks(params)
        this.bookOptions = res.data.records || []
      } catch (e) { this.$message.error('搜索书籍失败') } finally {
        this.bookSearching = false
      }
    },
    onBookChange(bookId) {
      const book = this.bookOptions.find(b => b.id === bookId)
      if (book && !this.form.title) {
        this.form.title = book.title
      }
      if (book && !this.form.imageUrl && book.coverImage) {
        this.form.imageUrl = book.coverImage
      }
    },
    handleImageSuccess(res) {
      const url = typeof res.data === 'string' ? res.data : (res.data?.url || res.data)
      if (url) this.form.imageUrl = url
    },
    openDialog(row) {
      if (row) {
        this.form = { ...row }
        if (row.bookId) {
          this.bookOptions = [{ id: row.bookId, title: row.title || '书籍#' + row.bookId, sellingPrice: '' }]
        }
      } else {
        this.form = { id: null, bookId: null, title: '', imageUrl: '', sortOrder: 0, status: 1 }
        this.loadAllBooks()
      }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleSave() {
      this.$refs.form.validate(async (valid) => {
        if (!valid) return
        this.saving = true
        try {
          if (this.form.id) {
            await updateBanner(this.form)
            this.$message.success('更新成功')
          } else {
            await createBanner(this.form)
            this.$message.success('创建成功')
          }
          this.dialogVisible = false
          this.fetchBanners()
        } catch (e) {
          this.$message.error('操作失败')
        } finally {
          this.saving = false
        }
      })
    },
    async handleToggle(row) {
      const newStatus = row.status === 1 ? 0 : 1
      try {
        await changeBannerStatus(row.id, newStatus)
        this.$message.success(newStatus === 1 ? '已启用' : '已禁用')
        this.fetchBanners()
      } catch (e) {
        this.$message.error('操作失败')
      }
    },
    handleDelete(row) {
      this.$confirm('确定删除此轮播？', '提示', { type: 'warning' }).then(async () => {
        try {
          await deleteBanner(row.id)
          this.$message.success('已删除')
          this.fetchBanners()
        } catch (e) {
          this.$message.error('删除失败')
        }
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.pagination { margin-top: 16px; text-align: right; }
.danger-text { color: #f56c6c; }
</style>
