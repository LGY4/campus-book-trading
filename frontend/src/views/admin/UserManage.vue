<template>
  <div class="user-manage">
    <el-card shadow="never">
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索用户名/昵称/手机号"
          clearable
          style="width: 280px"
          @keyup.enter.native="handleSearch"
        >
          <el-button slot="append" icon="el-icon-search" @click="handleSearch" />
        </el-input>
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 120px; margin-left: 10px" @change="handleSearch">
          <el-option label="正常" value="ACTIVE" />
          <el-option label="禁用" value="BANNED" />
        </el-select>
        <el-select v-model="roleFilter" placeholder="角色筛选" clearable style="width: 120px; margin-left: 10px" @change="handleSearch">
          <el-option label="普通用户" value="STUDENT" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>
        <el-button type="success" icon="el-icon-download" style="margin-left: 10px" @click="handleExport">导出</el-button>
      </div>

      <el-table :data="users" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="username" label="用户名" min-width="100" />
        <el-table-column prop="nickname" label="昵称" min-width="100" />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column label="角色" width="100">
          <template slot-scope="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'" size="small">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reputationScore" label="信誉分" width="80" />
        <el-table-column prop="createTime" label="注册时间" min-width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="{ row }">
            <el-button
              :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
              size="mini"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
            <el-button type="primary" size="mini" @click="openRoleDialog(row)">
              修改角色
            </el-button>
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

    <el-dialog title="修改角色" :visible.sync="roleDialogVisible" width="400px">
      <el-form label-width="60px">
        <el-form-item label="角色">
          <el-select v-model="editRole" style="width: 100%">
            <el-option label="普通用户" value="STUDENT" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleLoading" @click="handleRoleChange">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getAdminUsers, changeUserStatus, changeUserRole, exportUsers } from '@/api/admin'

export default {
  name: 'UserManage',
  data() {
    return {
      keyword: '',
      statusFilter: '',
      roleFilter: '',
      users: [],
      loading: false,
      total: 0,
      currentPage: 1,
      pageSize: 10,
      roleDialogVisible: false,
      roleLoading: false,
      editRole: 'USER',
      editingUser: null
    }
  },
  created() {
    this.fetchUsers()
  },
  methods: {
    async fetchUsers() {
      this.loading = true
      try {
        const res = await getAdminUsers({ keyword: this.keyword, status: this.statusFilter, role: this.roleFilter, page: this.currentPage, size: this.pageSize })
        const data = res.data || res
        this.users = data.records || data.list || []
        this.total = data.total || 0
      } catch {
        this.$message.error('获取用户列表失败')
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.currentPage = 1
      this.fetchUsers()
    },
    handlePageChange(page) {
      this.currentPage = page
      this.fetchUsers()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.fetchUsers()
    },
    async handleToggleStatus(row) {
      const newStatus = row.status === 'ACTIVE' ? 'BANNED' : 'ACTIVE'
      const label = newStatus === 'ACTIVE' ? '启用' : '禁用'
      try {
        await this.$confirm(`确定${label}用户 ${row.username}？`, '提示', { type: 'warning' })
        await changeUserStatus(row.id, newStatus)
        this.$message.success(`${label}成功`)
        this.fetchUsers()
      } catch (e) { if (e !== 'cancel') this.$message.error('操作失败') }
    },
    openRoleDialog(row) {
      this.editingUser = row
      this.editRole = row.role
      this.roleDialogVisible = true
    },
    async handleRoleChange() {
      this.roleLoading = true
      try {
        await changeUserRole(this.editingUser.id, this.editRole)
        this.$message.success('角色修改成功')
        this.roleDialogVisible = false
        this.fetchUsers()
      } catch {
        this.$message.error('角色修改失败')
      } finally {
        this.roleLoading = false
      }
    },
    async handleExport() {
      try {
        await exportUsers({ keyword: this.keyword })
        this.$message.success('导出成功')
      } catch {
        this.$message.error('导出失败')
      }
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
