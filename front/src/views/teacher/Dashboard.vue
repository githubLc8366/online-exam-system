<template>
  <div class="teacher-dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="statistics-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #409eff;">
              <el-icon :size="28"><Collection /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalQuestions ?? '-' }}</div>
              <div class="stat-label">题库题目数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #67c23a;">
              <el-icon :size="28"><DocumentChecked /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalPapers ?? '-' }}</div>
              <div class="stat-label">已组试卷</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #e6a23c;">
              <el-icon :size="28"><AlarmClock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.activeExams ?? '-' }}</div>
              <div class="stat-label">进行中考试</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #f56c6c;">
              <el-icon :size="28"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pendingGrading ?? '-' }}</div>
              <div class="stat-label">待阅卷数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-row :gutter="20" class="quick-actions">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <span>快捷操作</span>
          </template>
          <div class="action-buttons">
            <el-button type="primary" @click="$router.push('/teacher-portal/question-bank')">
              <el-icon><Plus /></el-icon>新增题目
            </el-button>
            <el-button type="success" @click="$router.push('/teacher-portal/exam-paper')">
              <el-icon><Document /></el-icon>组卷
            </el-button>
            <el-button type="warning" @click="$router.push('/teacher-portal/exam-management')">
              <el-icon><Promotion /></el-icon>发布考试
            </el-button>
            <el-button type="danger" @click="$router.push('/teacher-portal/grading')">
              <el-icon><EditPen /></el-icon>去阅卷
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近考试 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="flex-between">
              <span>最近考试</span>
              <el-button link type="primary" @click="$router.push('/teacher-portal/exam-management')">查看更多</el-button>
            </div>
          </template>


<el-table :data="recentExams" stripe v-loading="loading">
  <!-- 考试名称：使用 sessionName -->
  <el-table-column prop="sessionName" label="考试名称" min-width="180" />
  
  <!-- 试卷：显示 paperId -->
  <el-table-column label="试卷" min-width="150">
    <template #default="{ row }">试卷ID: {{ row.paperId }}</template>
  </el-table-column>
  
  <!-- 开始时间 -->
  <el-table-column label="开始时间" width="160">
    <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
  </el-table-column>
  
  <!-- 时长：可以计算或显示"--" -->
  <el-table-column prop="duration" label="时长" width="80" align="center">
  <template #default="{ row }">{{ row.duration }}分钟</template>
</el-table-column>
  
  <!-- 状态 -->
  <el-table-column label="状态" width="100" align="center">
    <template #default="{ row }">
      <el-tag v-if="isExamOngoing(row)" type="success">进行中</el-tag>
      <el-tag v-else-if="isExamUpcoming(row)" type="warning">未开始</el-tag>
      <el-tag v-else type="info">已结束</el-tag>
    </template>
  </el-table-column>
  
  <!-- 操作 -->
  <el-table-column label="操作" width="120" align="center">
    <template #default="{ row }">
      <el-button link type="primary" size="small" @click="goMonitor(row)">监控</el-button>
    </template>
  </el-table-column>
</el-table>



          <el-empty v-if="!loading && recentExams.length === 0" description="暂无考试记录" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Collection, DocumentChecked, AlarmClock, User,
  Plus, Document, Promotion, EditPen
} from '@element-plus/icons-vue'
import request from '@/api/request.js'

const router = useRouter()
const loading = ref(false)

const stats = ref({
  totalQuestions: 0,
  totalPapers: 0,
  activeExams: 0,
  pendingGrading: 0
})

const recentExams = ref([])

// 加载统计数据
const loadStats = async () => {
  try {
    const res = await request.get('/dashboard/teacher/stats')
    if (res) {
      stats.value = res
    }
  } catch (e) {
    console.error('获取统计数据失败', e)
  }
}

// 加载最近考试
const loadRecentExams = async () => {
  loading.value = true
  try {
    const res = await request.get('/dashboard/teacher/recent-exams')
    if (res) {
      recentExams.value = res
    }
  } catch (e) {
    console.error('获取最近考试失败', e)
  } finally {
    loading.value = false
  }
}

const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}

const isExamOngoing = (exam) => {
  const now = new Date()
  const start = new Date(exam.startTime)
  const end = new Date(exam.endTime)
  return now >= start && now <= end
}

const isExamUpcoming = (exam) => {
  const now = new Date()
  const start = new Date(exam.startTime)
  return now < start
}

const goMonitor = (row) => {
  router.push({ path: '/teacher-portal/exam-management', query: { examId: row.id } })
}

onMounted(() => {
  loadStats()
  loadRecentExams()
})
</script>

<style scoped>
.teacher-dashboard {
  width: 100%;
}

.statistics-row {
  margin-bottom: 20px;
}

.stat-card {
  margin-bottom: 16px;
}

.stat-content {
  display: flex;
  align-items: center;
  padding: 10px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-right: 16px;
  flex-shrink: 0;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.quick-actions {
  margin-bottom: 20px;
}

.action-buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.flex-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
