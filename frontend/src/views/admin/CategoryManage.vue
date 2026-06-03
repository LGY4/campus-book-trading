<template>
  <div class="category-manage">
    <el-card shadow="never">
      <div style="margin-bottom: 16px;">
        <el-button type="primary" size="small" icon="el-icon-plus" @click="openDialog(null)">新增分类</el-button>
      </div>
      <el-table :data="categories" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" min-width="150" />
        <el-table-column prop="icon" label="图标" width="100">
          <template slot-scope="{ row }">
            <i :class="row.icon" style="font-size: 20px;" />
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="100" />
        <el-table-column label="状态" width="100">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button type="text" size="small" class="danger-text" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="form.id ? '编辑分类' : '新增分类'" :visible.sync="dialogVisible" width="420px">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="如 el-icon-notebook-2" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
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
import { getCategories, createCategory, updateCategory, deleteCategory } from '@/api/category'

export default {
  name: 'CategoryManage',
  data() {
    return {
      categories: [],
      loading: false,
      dialogVisible: false,
      saving: false,
      form: { id: null, name: '', icon: '', sortOrder: 0, status: 1 },
      rules: {
        name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.fetchCategories()
  },
  methods: {
    async fetchCategories() {
      this.loading = true
      try {
        const res = await getCategories()
        this.categories = res.data || []
      } catch (e) {
        this.$message.error('获取分类列表失败')
      } finally {
        this.loading = false
      }
    },
    openDialog(row) {
      if (row) {
        this.form = { ...row }
      } else {
        this.form = { id: null, name: '', icon: '', sortOrder: 0, status: 1 }
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
            await updateCategory(this.form)
            this.$message.success('更新成功')
          } else {
            await createCategory(this.form)
            this.$message.success('创建成功')
          }
          this.dialogVisible = false
          this.fetchCategories()
        } catch (e) {
          this.$message.error('操作失败')
        } finally {
          this.saving = false
        }
      })
    },
    handleDelete(row) {
      this.$confirm(`确定删除分类"${row.name}"？`, '提示', { type: 'warning' }).then(async () => {
        try {
          await deleteCategory(row.id)
          this.$message.success('已删除')
          this.fetchCategories()
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
