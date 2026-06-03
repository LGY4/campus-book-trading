<template>
  <div class="publish-container">
    <el-card class="publish-card">
      <h2 slot="header">{{ isEdit ? '编辑书籍' : '发布书籍' }}</h2>
      <el-form ref="bookForm" :model="form" :rules="rules" label-width="100px" class="publish-form">
        <el-form-item label="书名" prop="title">
          <el-input v-model="form.title" placeholder="请输入书名" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="form.author" placeholder="请输入作者" />
        </el-form-item>
        <el-form-item label="出版社" prop="publisher">
          <el-input v-model="form.publisher" placeholder="请输入出版社" />
        </el-form-item>
        <el-form-item label="ISBN" prop="isbn">
          <el-input v-model="form.isbn" placeholder="请输入ISBN" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width:100%;">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="品相" prop="bookCondition">
          <el-select v-model="form.bookCondition" placeholder="请选择品相" style="width:100%;">
            <el-option label="全新" value="全新" />
            <el-option label="九成新" value="九成新" />
            <el-option label="八成新" value="八成新" />
            <el-option label="七成新及以下" value="七成新及以下" />
          </el-select>
        </el-form-item>
        <div class="price-row">
          <el-form-item label="原价" prop="originalPrice">
            <el-input-number v-model="form.originalPrice" :min="0" :precision="2" :step="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="售价" prop="sellingPrice">
            <el-input-number v-model="form.sellingPrice" :min="0" :precision="2" :step="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="数量" prop="quantity">
            <el-input-number v-model="form.quantity" :min="1" :max="999" controls-position="right" />
          </el-form-item>
        </div>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="5" placeholder="描述一下书籍的使用情况、笔记痕迹等..." maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="书籍图片">
          <el-upload
            action="/api/file/upload"
            :headers="uploadHeaders"
            :file-list="fileList"
            list-type="picture-card"
            :on-success="handleUploadSuccess"
            :on-remove="handleRemove"
            :before-upload="beforeUpload"
            :limit="5"
            accept="image/*"
          >
            <i class="el-icon-plus" />
          </el-upload>
          <div class="upload-tip">最多上传5张图片，第一张为封面</div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '发布书籍' }}
          </el-button>
          <el-button size="large" @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { getBookDetail, publishBook, updateBook } from '@/api/book'
import { getCategories } from '@/api/category'
import { getToken } from '@/utils/auth'

export default {
  name: 'PublishBook',
  data() {
    return {
      form: {
        title: '',
        author: '',
        publisher: '',
        isbn: '',
        categoryId: '',
        bookCondition: '',
        originalPrice: null,
        sellingPrice: null,
        quantity: 1,
        description: '',
        images: []
      },
      rules: {
        title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
        categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
        bookCondition: [{ required: true, message: '请选择品相', trigger: 'change' }],
        originalPrice: [{ required: true, message: '请输入原价', trigger: 'blur' }],
        sellingPrice: [{ required: true, message: '请输入售价', trigger: 'blur' }]
      },
      categories: [],
      fileList: [],
      loading: false,
      isEdit: false,
      bookId: null
    }
  },
  computed: {
    uploadHeaders() {
      return { Authorization: 'Bearer ' + getToken() }
    }
  },
  created() {
    this.fetchCategories()
    if (this.$route.query.bookId) {
      this.isEdit = true
      this.bookId = this.$route.query.bookId
      this.fetchBook()
    }
  },
  methods: {
    async fetchCategories() {
      try {
        const res = await getCategories()
        this.categories = res.data || []
      } catch (e) { this.$message.error('加载分类失败') }
    },
    async fetchBook() {
      try {
        const res = await getBookDetail(this.bookId)
        const d = res.data
        this.form = {
          title: d.title,
          author: d.author,
          publisher: d.publisher,
          isbn: d.isbn,
          categoryId: d.categoryId,
          bookCondition: d.bookCondition,
          originalPrice: d.originalPrice,
          sellingPrice: d.sellingPrice,
          quantity: d.quantity || 1,
          description: d.description,
          images: d.images || []
        }
        this.fileList = (d.images || []).map((url, i) => ({ name: 'image' + i, url }))
      } catch (e) { this.$message.error('加载书籍信息失败') }
    },
    beforeUpload(file) {
      const isImage = file.type.startsWith('image/')
      const isLt5M = file.size / 1024 / 1024 < 5
      if (!isImage) this.$message.error('只能上传图片文件')
      if (!isLt5M) this.$message.error('图片大小不能超过5MB')
      return isImage && isLt5M
    },
    handleUploadSuccess(res, file, fileList) {
      const url = typeof res.data === 'string' ? res.data : (res.data?.url || res.data)
      if (url) this.form.images.push(url)
    },
    handleRemove(file, fileList) {
      const url = file.url || (typeof file.response?.data === 'string' ? file.response.data : file.response?.data?.url)
      const idx = this.form.images.indexOf(url)
      if (idx > -1) this.form.images.splice(idx, 1)
    },
    handleSubmit() {
      this.$refs.bookForm.validate(async (valid) => {
        if (!valid) return
        this.loading = true
        try {
          const payload = {
            ...this.form,
            imagesJson: JSON.stringify(this.form.images || []),
            coverImage: (this.form.images && this.form.images.length) ? this.form.images[0] : ''
          }
          delete payload.images
          let res
          if (this.isEdit) {
            res = await updateBook({ id: this.bookId, ...payload })
          } else {
            res = await publishBook(payload)
          }
          console.log('[PublishBook] response:', JSON.stringify(res))
          const status = res.data?.status
          console.log('[PublishBook] status:', status)
          if (status === 'ON_SALE') {
            this.$message.success(this.isEdit ? '修改成功，已自动上架' : '发布成功，已自动上架')
          } else if (status === 'PENDING') {
            this.$message.success(this.isEdit ? '修改成功，等待管理员审核' : '发布成功，等待管理员审核')
          } else {
            this.$message.success(this.isEdit ? '修改成功' : '发布成功')
          }
          this.$router.push('/my-books')
        } catch (e) { this.$message.error(this.isEdit ? '修改失败' : '发布失败') } finally {
          this.loading = false
        }
      })
    }
  }
}
</script>

<style scoped>
.publish-container {
  max-width: 800px;
  margin: 0 auto;
}
.publish-card h2 {
  margin: 0;
  font-size: 20px;
}
.publish-form {
  padding-top: 10px;
}
.price-row {
  display: flex;
  gap: 20px;
}
.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
</style>
