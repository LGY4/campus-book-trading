<template>
  <div class="dashboard" v-loading="loading">
    <div class="dashboard-header">
      <el-radio-group v-model="days" size="small" @change="fetchData">
        <el-radio-button :label="7">近7天</el-radio-button>
        <el-radio-button :label="30">近30天</el-radio-button>
        <el-radio-button :label="90">近90天</el-radio-button>
      </el-radio-group>
    </div>

    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="12" :sm="4" v-for="item in statCards" :key="item.label">
        <el-card shadow="hover" class="stat-card" :body-style="{ padding: '20px' }">
          <div class="stat-card-inner">
            <div>
              <div class="stat-value">{{ item.value }}</div>
              <div class="stat-label">{{ item.label }}</div>
            </div>
            <i :class="item.icon" :style="{ color: item.color, fontSize: '36px' }"></i>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover">
          <div slot="header">交易趋势</div>
          <div ref="tradeChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="hover">
          <div slot="header">收入趋势</div>
          <div ref="revenueChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover">
          <div slot="header">书籍类别分布</div>
          <div ref="categoryChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="hover">
          <div slot="header">订单量趋势</div>
          <div ref="orderChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getDashboard } from '@/api/admin'

export default {
  name: 'AdminDashboard',
  data() {
    return {
      days: 7,
      statCards: [
        { label: '总用户数', value: 0, icon: 'el-icon-user', color: '#409EFF' },
        { label: '在售商品', value: 0, icon: 'el-icon-reading', color: '#67C23A' },
        { label: '总订单数', value: 0, icon: 'el-icon-tickets', color: '#E6A23C' },
        { label: '纠纷订单', value: 0, icon: 'el-icon-warning', color: '#F56C6C' },
        { label: '总交易额', value: '¥0', icon: 'el-icon-money', color: '#67C23A' }
      ],
      charts: [],
      loading: false
    }
  },
  mounted() {
    this.fetchData()
  },
  beforeDestroy() {
    this.charts.forEach(c => c.dispose())
  },
  methods: {
    async fetchData() {
      this.loading = true
      this.charts.forEach(c => c.dispose())
      this.charts = []
      try {
        const res = await getDashboard(this.days)
        const data = res.data || res
        this.statCards[0].value = data.userCount || 0
        this.statCards[1].value = data.bookCount || 0
        this.statCards[2].value = data.orderCount || 0
        this.statCards[3].value = data.disputeCount || 0
        this.statCards[4].value = '¥' + (data.totalRevenue || 0)
        const dailyStats = data.dailyStats || []
        const categoryStats = data.categoryStats || []
        const dailyRevenue = data.dailyRevenue || []
        this.$nextTick(() => {
          this.initTradeChart(dailyStats)
          this.initRevenueChart(dailyRevenue)
          this.initCategoryChart(categoryStats)
          this.initOrderChart(dailyStats)
        })
      } catch {
        this.$message.error('获取数据失败')
      } finally {
        this.loading = false
      }
    },
    initTradeChart(dailyStats) {
      const chart = echarts.init(this.$refs.tradeChart)
      this.charts.push(chart)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: dailyStats.map(i => i.date) },
        yAxis: { type: 'value', name: '新增数' },
        series: [
          {
            name: '新增用户',
            type: 'line',
            data: dailyStats.map(i => i.newUsers),
            smooth: true,
            itemStyle: { color: '#409EFF' }
          },
          {
            name: '新增书籍',
            type: 'line',
            data: dailyStats.map(i => i.newBooks),
            smooth: true,
            itemStyle: { color: '#67C23A' }
          }
        ],
        legend: { bottom: 0 },
        grid: { left: 60, right: 20, top: 30, bottom: 40 }
      })
    },
    initRevenueChart(dailyRevenue) {
      const chart = echarts.init(this.$refs.revenueChart)
      this.charts.push(chart)
      chart.setOption({
        tooltip: { trigger: 'axis', formatter: '{b}<br/>{a}: ¥{c}' },
        xAxis: { type: 'category', data: dailyRevenue.map(i => i.date) },
        yAxis: { type: 'value', name: '收入(元)' },
        series: [{
          name: '收入',
          type: 'line',
          data: dailyRevenue.map(i => i.revenue),
          smooth: true,
          areaStyle: { opacity: 0.15 },
          itemStyle: { color: '#67C23A' }
        }],
        grid: { left: 80, right: 20, top: 30, bottom: 30 }
      })
    },
    initCategoryChart(categoryStats) {
      const chart = echarts.init(this.$refs.categoryChart)
      this.charts.push(chart)
      chart.setOption({
        tooltip: { trigger: 'item' },
        legend: { bottom: 0 },
        series: [{
          type: 'pie',
          radius: ['40%', '65%'],
          data: categoryStats.map(i => ({ name: i.categoryName, value: i.bookCount })),
          label: { formatter: '{b}: {d}%' }
        }]
      })
    },
    initOrderChart(dailyStats) {
      const chart = echarts.init(this.$refs.orderChart)
      this.charts.push(chart)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: dailyStats.map(i => i.date) },
        yAxis: { type: 'value', name: '订单数' },
        series: [{
          type: 'bar',
          data: dailyStats.map(i => i.orderCount),
          itemStyle: { color: '#E6A23C', borderRadius: [4, 4, 0, 0] }
        }],
        grid: { left: 60, right: 20, top: 30, bottom: 30 }
      })
    }
  }
}
</script>

<style scoped>
.dashboard-header {
  margin-bottom: 16px;
  text-align: right;
}
.stat-cards {
  margin-bottom: 20px;
}
.stat-card-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}
.chart-row {
  margin-bottom: 20px;
}
.chart-container {
  width: 100%;
  height: 350px;
}
</style>
