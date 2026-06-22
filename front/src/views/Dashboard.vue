<template>
  <div class="dashboard">
    <!-- 统计卡片区域 -->
    <el-row :gutter="20" class="statistics-row">
      <el-col :xs="24" :sm="8" :md="8" :lg="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #409eff;">
              <el-icon :size="32"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalUsers }}</div>
              <div class="stat-label">总用户数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8" :md="8" :lg="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #67c23a;">
              <el-icon :size="32"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalExams }}</div>
              <div class="stat-label">总考试场次</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8" :md="8" :lg="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #e6a23c;">
              <el-icon :size="32"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalParticipants }}</div>
              <div class="stat-label">累计参与人次</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>系统访问趋势（近7天）</span>
            </div>
          </template>
          <div ref="chartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷操作区域 -->
    <el-row :gutter="20" class="quick-actions">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <span>快捷操作</span>
          </template>
          <div class="action-buttons">
            <el-button type="primary" @click="$router.push('/student')">
              <el-icon><User /></el-icon>
              学生管理
            </el-button>
            <el-button type="success" @click="$router.push('/teacher')">
              <el-icon><UserFilled /></el-icon>
              教师管理
            </el-button>
            <el-button type="warning" @click="$router.push('/organization')">
              <el-icon><OfficeBuilding /></el-icon>
              组织架构
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { User, Document, TrendCharts, UserFilled, OfficeBuilding } from '@element-plus/icons-vue'
import { dashboardApi } from '@/api/dashboard'

// 统计数据
const statistics = ref({
  totalUsers: 0,
  totalExams: 0,
  totalParticipants: 0
})

// 图表引用
const chartRef = ref(null)
let chartInstance = null

// 加载统计数据
const loadStatistics = async () => {
  try {
    const data = await dashboardApi.getStatistics()
    // 拦截器已解包 Result.data，data = { userCount, sessionCount, recordCount }
    statistics.value = {
      totalUsers: data.userCount || 0,
      totalExams: data.sessionCount || 0,
      totalParticipants: data.recordCount || 0
    }
  } catch (e) {
    console.error('加载统计数据失败', e)
  }
}

// 加载趋势数据
const loadTrend = async () => {
  if (!chartRef.value) return
  
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
    window.addEventListener('resize', () => {
      chartInstance?.resize()
    })
  }
  
  try {
    const list = await dashboardApi.getTrend()
    // list = [{ dateStr: '2026-04-24', count: 5 }, ...]
    const dates = list.map(item => item.dateStr ? item.dateStr.substring(5) : '')
    const values = list.map(item => item.count || 0)
    
    const option = {
      tooltip: {
        trigger: 'axis'
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: dates
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          name: '新增记录',
          type: 'line',
          smooth: true,
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
              { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
            ])
          },
          itemStyle: {
            color: '#409eff'
          },
          data: values
        }
      ]
    }
    
    chartInstance.setOption(option)
  } catch (e) {
    console.error('加载趋势数据失败', e)
  }
}

onMounted(() => {
  loadStatistics()
  loadTrend()
})
</script>

<style scoped>
.dashboard {
  width: 100%;
}

.statistics-row {
  margin-bottom: 20px;
}

.stat-card {
  margin-bottom: 20px;
}

.stat-content {
  display: flex;
  align-items: center;
  padding: 10px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-right: 20px;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.chart-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  width: 100%;
  height: 350px;
}

.quick-actions {
  margin-bottom: 20px;
}

.action-buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .stat-content {
    flex-direction: column;
    text-align: center;
  }
  
  .stat-icon {
    margin-right: 0;
    margin-bottom: 10px;
  }
  
  .action-buttons {
    justify-content: center;
  }
}
</style>