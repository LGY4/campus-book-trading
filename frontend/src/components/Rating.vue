<template>
  <div class="rating-wrapper">
    <el-rate
      :value="value"
      :disabled="disabled"
      :show-score="false"
      @change="handleChange"
    />
    <span v-if="showText" class="rating-text" :style="{ color: textColor }">{{ ratingText }}</span>
  </div>
</template>

<script>
export default {
  name: 'Rating',
  props: {
    value: { type: Number, default: 0 },
    disabled: { type: Boolean, default: false },
    showText: { type: Boolean, default: false }
  },
  computed: {
    ratingText() {
      if (this.value >= 4.5) return '非常好'
      if (this.value >= 3.5) return '好'
      if (this.value >= 2.5) return '一般'
      if (this.value >= 1.5) return '差'
      return '很差'
    },
    textColor() {
      if (this.value >= 4.5) return '#67c23a'
      if (this.value >= 3.5) return '#409eff'
      if (this.value >= 2.5) return '#e6a23c'
      return '#f56c6c'
    }
  },
  methods: {
    handleChange(val) {
      this.$emit('input', val)
    }
  }
}
</script>

<style scoped>
.rating-wrapper {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.rating-text {
  font-size: 14px;
  font-weight: 500;
}
</style>
