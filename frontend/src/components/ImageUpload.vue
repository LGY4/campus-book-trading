<template>
  <div class="image-upload">
    <el-upload
      :action="uploadUrl"
      :headers="headers"
      :file-list="fileList"
      :multiple="multiple"
      :limit="limit"
      :on-success="handleSuccess"
      :on-remove="handleRemove"
      :on-exceed="handleExceed"
      :on-preview="handlePreview"
      :before-upload="beforeUpload"
      list-type="picture-card"
      :drag="drag"
      accept="image/*"
    >
      <i class="el-icon-plus"></i>
      <div slot="tip" class="el-upload__tip" v-if="showTip">
        只能上传图片文件，且不超过 {{ maxSize }}MB
        <template v-if="limit">，最多上传 {{ limit }} 张</template>
      </div>
    </el-upload>

    <el-dialog :visible.sync="previewVisible" append-to-body>
      <img width="100%" :src="previewUrl" alt="预览" />
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'ImageUpload',
  props: {
    value: { type: [String, Array], default: '' },
    multiple: { type: Boolean, default: false },
    limit: { type: Number, default: 5 },
    maxSize: { type: Number, default: 5 },
    drag: { type: Boolean, default: true },
    showTip: { type: Boolean, default: true }
  },
  data() {
    return {
      uploadUrl: '/api/file/upload',
      previewVisible: false,
      previewUrl: ''
    }
  },
  computed: {
    headers() {
      const token = this.$store.state.user && this.$store.state.user.token
      return token ? { Authorization: `Bearer ${token}` } : {}
    },
    fileList() {
      if (!this.value) return []
      if (typeof this.value === 'string' && this.value) {
        return [{ name: 'image', url: this.value }]
      }
      if (Array.isArray(this.value)) {
        return this.value.map((url, i) => ({ name: `image-${i}`, url }))
      }
      return []
    }
  },
  methods: {
    beforeUpload(file) {
      const isImage = file.type.startsWith('image/')
      const isLt = file.size / 1024 / 1024 < this.maxSize
      if (!isImage) {
        this.$message.error('只能上传图片文件')
        return false
      }
      if (!isLt) {
        this.$message.error(`图片大小不能超过 ${this.maxSize}MB`)
        return false
      }
      return true
    },
    handleSuccess(res, file, fileList) {
      const url = (res && (res.data || res.url)) || ''
      if (this.multiple) {
        const urls = fileList.map(f => f.response ? (f.response.data || f.response.url) : f.url).filter(Boolean)
        this.$emit('input', urls)
      } else {
        this.$emit('input', url)
      }
    },
    handleRemove(file, fileList) {
      if (this.multiple) {
        const urls = fileList.map(f => f.response ? (f.response.data || f.response.url) : f.url).filter(Boolean)
        this.$emit('input', urls)
      } else {
        this.$emit('input', '')
      }
    },
    handleExceed() {
      this.$message.warning(`最多上传 ${this.limit} 张图片`)
    },
    handlePreview(file) {
      this.previewUrl = file.url || (file.response && (file.response.data || file.response.url))
      this.previewVisible = true
    }
  }
}
</script>

<style scoped>
.image-upload >>> .el-upload--picture-card {
  width: 120px;
  height: 120px;
  line-height: 120px;
}
.image-upload >>> .el-upload-list--picture-card .el-upload-list__item {
  width: 120px;
  height: 120px;
}
</style>
