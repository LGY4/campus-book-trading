<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在</p>
    <el-button type="primary" @click="$router.push('/home')">返回首页</el-button>
  </div>
</template>

<script>
export default { name: 'NotFound' }
</script>

<style scoped>
.not-found { text-align: center; padding: 100px 20px; }
.not-found h1 { font-size: 80px; color: #409EFF; margin-bottom: 20px; }
.not-found p { font-size: 18px; color: #666; margin-bottom: 30px; }
</style>
