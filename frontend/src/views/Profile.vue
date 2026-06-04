<template>
  <div class="profile-page">
    <el-card class="quick-links-card">
      <div class="quick-links">
        <el-button icon="el-icon-notebook-2" @click="$router.push('/my-books')">我的发布</el-button>
        <el-button icon="el-icon-tickets" @click="$router.push('/my-orders')">我的订单</el-button>
        <el-button icon="el-icon-star-off" @click="$router.push('/my-favorites')">我的收藏</el-button>
        <el-button icon="el-icon-shopping-cart-2" @click="$router.push('/my-wants')">我想要的</el-button>
        <el-button icon="el-icon-time" @click="$router.push('/my-footprint')">浏览足迹</el-button>
        <el-button icon="el-icon-sort" @click="$router.push('/my-exchange')">交换记录</el-button>
        <el-button icon="el-icon-message" @click="$router.push('/messages')">消息中心</el-button>
      </div>
    </el-card>
    <el-card>
      <h2 slot="header">个人中心</h2>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="个人信息" name="info">
          <el-form ref="infoForm" :model="infoForm" :rules="infoRules" label-width="80px" class="profile-form">
            <el-form-item label="头像">
              <el-upload
                class="avatar-uploader"
                action="/api/user/avatar"
                :headers="uploadHeaders"
                :show-file-list="false"
                :on-success="handleAvatarSuccess"
                :before-upload="beforeAvatarUpload"
                accept="image/*"
              >
                <el-avatar v-if="infoForm.avatar" :src="infoForm.avatar" :size="80" shape="square" fit="cover" />
                <i v-else class="el-icon-plus avatar-uploader-icon" />
              </el-upload>
              <span class="upload-tip">点击更换头像</span>
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="infoForm.nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="infoForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="infoForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="信誉分">
              <el-rate :value="reputation" disabled show-score score-template="{value}" :allow-half="true" />
              <span class="rep-text">{{ reputation }} 分</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="infoLoading" @click="handleSaveInfo">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="修改密码" name="password">
          <el-form ref="pwdForm" :model="pwdForm" :rules="pwdRules" label-width="100px" class="profile-form">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" placeholder="请输入新密码" show-password />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="pwdLoading" @click="handleChangePwd">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="收货地址" name="address">
          <div class="address-actions">
            <el-button type="primary" size="small" icon="el-icon-plus" @click="openAddressDialog(null)">新增地址</el-button>
          </div>
          <div v-if="addresses.length" class="address-list">
            <div v-for="addr in addresses" :key="addr.id" class="address-item">
              <div class="address-info">
                <div class="address-top">
                  <span class="addr-name">{{ addr.receiverName }}</span>
                  <span class="addr-phone">{{ addr.phone }}</span>
                  <el-tag v-if="addr.isDefault" type="danger" size="mini">默认</el-tag>
                </div>
                <p class="addr-detail">{{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detail }}</p>
              </div>
              <div class="address-actions-cell">
                <el-button type="text" size="small" @click="openAddressDialog(addr)">编辑</el-button>
                <el-button v-if="!addr.isDefault" type="text" size="small" @click="setDefault(addr)">设为默认</el-button>
                <el-button type="text" size="small" class="danger-text" @click="deleteAddr(addr)">删除</el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无收货地址" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog :title="addrForm.id ? '编辑地址' : '新增地址'" :visible.sync="addrDialogVisible" width="520px">
      <el-form ref="addrForm" :model="addrForm" :rules="addrRules" label-width="80px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="addrForm.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addrForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="省" prop="province">
          <el-input v-model="addrForm.province" placeholder="省份" style="width:30%;margin-right:3%;" />
          <el-input v-model="addrForm.city" placeholder="城市" style="width:30%;margin-right:3%;" />
          <el-input v-model="addrForm.district" placeholder="区/县" style="width:30%;" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="addrForm.detail" type="textarea" :rows="2" placeholder="请输入详细地址" />
        </el-form-item>
        <el-form-item label="默认">
          <el-switch v-model="addrForm.isDefault" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="addrDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addrLoading" @click="saveAddress">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getUserInfo, updateUserInfo, changePassword } from '@/api/user'
import { getAddressList, saveAddress as saveAddressApi, deleteAddress, setDefaultAddress } from '@/api/address'
import { getToken } from '@/utils/auth'
import { mapState } from 'vuex'

export default {
  name: 'Profile',
  data() {
    const validatePhone = (rule, value, callback) => {
      if (value && !/^1[3-9]\d{9}$/.test(value)) {
        callback(new Error('请输入正确的手机号'))
      } else {
        callback()
      }
    }
    const validateConfirm = (rule, value, callback) => {
      if (value !== this.pwdForm.newPassword) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      activeTab: 'info',
      infoForm: { avatar: '', nickname: '', phone: '', email: '' },
      infoRules: {
        nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
        phone: [{ validator: validatePhone, trigger: 'blur' }],
        email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }]
      },
      infoLoading: false,
      reputation: 0,
      pwdForm: { oldPassword: '', newPassword: '', confirmPassword: '' },
      pwdRules: {
        oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, message: '密码长度不少于6位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请确认新密码', trigger: 'blur' },
          { validator: validateConfirm, trigger: 'blur' }
        ]
      },
      pwdLoading: false,
      addresses: [],
      addrDialogVisible: false,
      addrForm: { id: null, receiverName: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false },
      addrRules: {
        receiverName: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
        phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
        province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
        city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
        detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
      },
      addrLoading: false
    }
  },
  computed: {
    ...mapState('user', ['user']),
    uploadHeaders() {
      return { Authorization: 'Bearer ' + getToken() }
    }
  },
  created() {
    this.fetchInfo()
    this.fetchAddresses()
    if (this.$route.query.tab === 'address') {
      this.activeTab = 'address'
      if (this.$route.query.action === 'add') {
        this.$nextTick(() => this.openAddressDialog(null))
      }
    }
  },
  methods: {
    async fetchInfo() {
      try {
        const res = await getUserInfo()
        const d = res.data
        this.infoForm = { avatar: d.avatar, nickname: d.nickname, phone: d.phone, email: d.email }
        this.reputation = d.reputationScore || 0
      } catch (e) { this.$message.error('加载个人信息失败') }
    },
    beforeAvatarUpload(file) {
      const isImage = file.type.startsWith('image/')
      const isLt2M = file.size / 1024 / 1024 < 2
      if (!isImage) this.$message.error('只能上传图片')
      if (!isLt2M) this.$message.error('图片大小不能超过2MB')
      return isImage && isLt2M
    },
    handleAvatarSuccess(res) {
      if (res.code !== 200) {
        this.$message.error(res.message || '上传失败')
        return
      }
      this.infoForm.avatar = res.data
      this.$store.commit('user/SET_USER', { user: { ...this.user, avatar: res.data } })
    },
    handleSaveInfo() {
      this.$refs.infoForm.validate(async (valid) => {
        if (!valid) return
        this.infoLoading = true
        try {
          await updateUserInfo(this.infoForm)
          this.$store.commit('user/SET_USER', { user: { ...this.user, ...this.infoForm } })
          this.$message.success('保存成功')
        } catch (e) { this.$message.error('保存失败') } finally {
          this.infoLoading = false
        }
      })
    },
    handleChangePwd() {
      this.$refs.pwdForm.validate(async (valid) => {
        if (!valid) return
        this.pwdLoading = true
        try {
          await changePassword({
            oldPassword: this.pwdForm.oldPassword,
            newPassword: this.pwdForm.newPassword
          })
          this.$message.success('密码修改成功，请重新登录')
          this.$store.dispatch('user/logout')
          this.$router.push('/login')
        } catch (e) { this.$message.error('密码修改失败') } finally {
          this.pwdLoading = false
        }
      })
    },
    async fetchAddresses() {
      try {
        const res = await getAddressList()
        this.addresses = res.data || []
      } catch (e) { this.$message.error('加载地址失败') }
    },
    openAddressDialog(addr) {
      if (addr) {
        this.addrForm = { ...addr, isDefault: !!addr.isDefault }
      } else {
        this.addrForm = { id: null, receiverName: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false }
      }
      this.addrDialogVisible = true
      this.$nextTick(() => this.$refs.addrForm && this.$refs.addrForm.clearValidate())
    },
    saveAddress() {
      this.$refs.addrForm.validate(async (valid) => {
        if (!valid) return
        this.addrLoading = true
        try {
          const payload = { ...this.addrForm, isDefault: this.addrForm.isDefault ? 1 : 0 }
          await saveAddressApi(payload)
          this.$message.success(this.addrForm.id ? '地址已更新' : '地址已添加')
          this.addrDialogVisible = false
          this.fetchAddresses()
        } catch (e) { this.$message.error('保存地址失败') } finally {
          this.addrLoading = false
        }
      })
    },
    async setDefault(addr) {
      try {
        await setDefaultAddress(addr.id)
        this.$message.success('已设为默认')
        this.fetchAddresses()
      } catch (e) { this.$message.error('设置默认地址失败') }
    },
    deleteAddr(addr) {
      this.$confirm('确定删除该地址？', '提示', { type: 'warning' }).then(async () => {
        try {
          await deleteAddress(addr.id)
          this.$message.success('已删除')
          this.fetchAddresses()
        } catch (e) {
          this.$message.error('删除失败')
        }
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 800px;
  margin: 0 auto;
}
.quick-links-card {
  margin-bottom: 16px;
}
.quick-links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.profile-page h2 {
  margin: 0;
  font-size: 20px;
}
.profile-form {
  max-width: 480px;
  padding-top: 10px;
}
.avatar-uploader {
  display: inline-block;
  cursor: pointer;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 80px;
  height: 80px;
  line-height: 80px;
  text-align: center;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
}
.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 12px;
}
.rep-text {
  margin-left: 8px;
  font-size: 14px;
  color: #e6a23c;
  font-weight: 600;
}
.address-actions {
  margin-bottom: 16px;
}
.address-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.address-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  transition: border-color 0.2s;
}
.address-item:hover {
  border-color: #409EFF;
}
.address-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}
.addr-name {
  font-weight: 600;
  color: #303133;
}
.addr-phone {
  color: #606266;
  font-size: 14px;
}
.addr-detail {
  font-size: 14px;
  color: #909399;
  margin: 0;
}
.address-actions-cell {
  flex-shrink: 0;
}
.danger-text {
  color: #f56c6c;
}
</style>
