<template>
  <div class="student-dashboard">
    <el-row :gutter="20">
      <el-col :xs="24" :md="16">
        <el-card shadow="never">
          <template #header>
            <div class="flex-between">
              <span><el-icon><AlarmClock /></el-icon> 待考中心</span>
              <el-tag v-if="isTesting" type="danger" effect="dark">您有一场考试正在进行中</el-tag>
            </div>
          </template>
          <el-empty v-if="!examList.length" description="暂无待考考试" />
          <div v-for="exam in examList" :key="exam.id" class="exam-item mb-4">
            <el-card shadow="hover" :body-style="{ padding: '16px' }">
              <div class="flex-between">
                <div class="flex-1">
                  <div class="flex items-center gap-2 mb-2">
                    <!-- 【修改】显示考试名称（sessionName），兜底用 paperName -->
                    <span class="font-bold text-base">{{ exam.sessionName || exam.paperName }}</span>
                    <el-tag size="small" :type="statusTagType(exam.status)">{{ statusText(exam.status) }}</el-tag>
                  </div>
                  <div class="text-gray text-sm">
                    <span class="mr-4"><el-icon><Timer /></el-icon> 时长：{{ exam.duration || 120 }} 分钟</span>
                    <span class="mr-4"><el-icon><Calendar /></el-icon> 状态：{{ statusText(exam.status) }}</span>
                  </div>
                </div>
                <div class="ml-4">
                  <el-button
                    v-if="exam.status === 0 || exam.status === 1"
                    type="primary"
                    size="large"
                    :disabled="isTesting"
                    @click="enterExam(exam)"
                  >
                    进入考试
                  </el-button>
                  <el-button
                    v-else
                    size="large"
                    @click="viewHistory(exam)"
                  >
                    查看记录
                  </el-button>
                </div>
              </div>
            </el-card>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="never">
          <template #header>
            <span><el-icon><User /></el-icon> 个人信息</span>
          </template>
          <div class="profile-preview">
            <div class="text-center mb-4">
              <el-avatar :size="64" :icon="UserFilled" />
              <div class="mt-2 font-bold">{{ studentInfo.nickname }}</div>
              <div class="text-gray text-sm">{{ studentInfo.userNo }}</div>
            </div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="班级">{{ studentInfo.className || '未设置' }}</el-descriptions-item>
              <el-descriptions-item label="专业">{{ studentInfo.deptName || '未设置' }}</el-descriptions-item>
              <el-descriptions-item label="已考次数">{{ stats.examCount }} 次</el-descriptions-item>
              <el-descriptions-item label="平均分">{{ stats.avgScore }} 分</el-descriptions-item>
            </el-descriptions>
            <div class="mt-4 text-center">
              <el-button type="primary" size="small" @click="$router.push('/student-portal/profile')">进入个人中心</el-button>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="mt-4">
          <template #header>
            <span><el-icon><Document /></el-icon> 快捷入口</span>
          </template>
          <div class="quick-links">
            <el-button class="w-full mb-2" @click="$router.push('/student-portal/history')">
              <el-icon><Tickets /></el-icon> 考试记录
            </el-button>
            <el-button class="w-full mb-2" @click="$router.push('/student-portal/wrongbook')">
              <el-icon><CircleClose /></el-icon> 错题本
            </el-button>
            <el-button class="w-full" @click="$router.push('/student-portal/profile')">
              <el-icon><Setting /></el-icon> 修改密码
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  AlarmClock, Timer, Calendar, User, UserFilled,
  Document, Tickets, CircleClose, Setting
} from '@element-plus/icons-vue'
import request from '@/api/request.js'

const router = useRouter()

const studentInfo = ref({})
const examList = ref([])
const isTesting = ref(false)
const stats = ref({ examCount: 0, avgScore: 0 })
let statusTimer = null

const statusText = (status) => {
  const map = { 0: '待考', 1: '考试中', 2: '已交卷', 3: '批阅中', 4: '已完成', 5: '缺考' }
  return map[status] || '未知'
}

const statusTagType = (status) => {
  const map = { 0: 'info', 1: 'success', 2: 'warning', 3: 'danger', 4: '' }
  return map[status] || ''
}

const checkTestingStatus = () => {
  isTesting.value = localStorage.getItem('is_testing') === 'true'
}

// 加载考试列表
const loadExams = async () => {
  try {
    const res = await request.get('/exam/my-exams')
    // 只显示待考(0)和考试中(1)的考试
    examList.value = (res || []).filter(exam => exam.status === 0 || exam.status === 1)
  } catch (e) {
    console.error('获取考试列表失败', e)
  }
}

// 加载用户信息
const loadUserInfo = async () => {
  try {
    const res = await request.get('/user/info')
    studentInfo.value = res || {}

  } catch (e) {
    console.error('获取用户信息失败', e)
  }
  


}
const loadStats = async () => {
  try {
    const res = await request.get('/exam/my-exams')
    const records = res || []
    const finished = records.filter(r => r.status === 4)
    stats.value.examCount = finished.length
    if (finished.length > 0) {
      const total = finished.reduce((sum, r) => sum + (r.totalScore || 0), 0)
      stats.value.avgScore = (total / finished.length).toFixed(1)
    }
  } catch (e) {
    console.error('获取统计数据失败', e)
  }
}
const enterExam = (exam) => {
  if (isTesting.value) {
    ElMessage.warning('您当前正有一场考试在进行中，请先完成当前考试。')
    return
  }
  localStorage.setItem('is_testing', 'true')
  router.push(`/student-portal/exam/${exam.sessionId}`)
}

const viewHistory = (exam) => {
  router.push('/student-portal/history')
}

onMounted(() => {
  checkTestingStatus()
  statusTimer = setInterval(checkTestingStatus, 3000)
  loadExams()
  loadUserInfo()
  loadStats()
})

onUnmounted(() => {
  if (statusTimer) clearInterval(statusTimer)
})
</script>

<style scoped>
.student-dashboard { width: 100%; }
.exam-item { transition: all 0.3s; }
.flex-between { display: flex; justify-content: space-between; align-items: center; }
.flex { display: flex; }
.items-center { align-items: center; }
.gap-2 { gap: 8px; }
.flex-1 { flex: 1; }
.ml-4 { margin-left: 16px; }
.mt-1 { margin-top: 4px; }
.mt-2 { margin-top: 8px; }
.mt-4 { margin-top: 16px; }
.mb-2 { margin-bottom: 8px; }
.mb-4 { margin-bottom: 16px; }
.text-center { text-align: center; }
.font-bold { font-weight: bold; }
.text-base { font-size: 16px; }
.text-sm { font-size: 13px; }
.text-gray { color: #909399; }
.text-primary { color: #409eff; }
.w-full { width: 100%; }
.quick-links { display: flex; flex-direction: column; }
:deep(.el-button+.el-button){
  margin-left: 0px;
}
</style>
