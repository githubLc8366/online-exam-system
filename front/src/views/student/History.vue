<template>
  <div class="exam-history">
    <el-card shadow="never">
      <template #header>
        <div class="flex-between">
          <span><el-icon><Tickets /></el-icon> 考试记录</span>
        </div>
      </template>

      <el-table :data="historyList" stripe border :header-cell-style="{ background: '#f5f7fa' }">
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column prop="name" label="考试名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="subject" label="科目" width="120" />
        <el-table-column prop="score" label="得分" width="80" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-success': row.score >= 60, 'text-danger': row.score < 60 }">{{ row.score }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" width="80" align="center" />
        <el-table-column prop="duration" label="用时" width="90" align="center" />
        <el-table-column prop="submitTime" label="交卷时间" width="150" />
        <el-table-column prop="rank" label="排名" width="80" align="center">
          <template #default="{ row }">{{ row.rank > 0 ? '第' + row.rank + '名' : '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)"><el-icon><View /></el-icon>查看答卷</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 答卷详情弹窗 -->
    <el-dialog v-model="detailVisible" title="答卷详情" width="800px" :close-on-click-modal="false">
    <el-descriptions :column="4" border size="small" class="mb-4">
      <el-descriptions-item label="考试">{{ detailExam.name }}</el-descriptions-item>
      <el-descriptions-item label="得分">{{ detailExam.score }}/{{ detailExam.totalScore }}</el-descriptions-item>
      <el-descriptions-item label="排名">第 {{ detailExam.rank || '--' }} 名</el-descriptions-item>
      <el-descriptions-item label="用时">{{ detailExam.duration || '--' }}</el-descriptions-item>
    </el-descriptions>
      <div v-for="(q, idx) in detailQuestions" :key="idx" class="detail-question mb-4">
        <div class="font-bold mb-2">{{ idx + 1 }}. 【{{ typeLabel(q.type) }}】{{ q.content }}</div>
        <div class="mb-1"><span class="text-gray">你的答案：</span><span :class="q.isCorrect ? 'text-success' : 'text-danger'">{{ q.studentAnswer || '（未作答）' }}</span></div>
        <div class="mb-1"><span class="text-gray">正确答案：</span><span class="text-success">{{ q.correctAnswer }}</span></div>
        <div><span class="text-gray">得分：</span>{{ q.gotScore }}/{{ q.score }} 分</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { ref, onMounted } from 'vue'
import { Tickets, View } from '@element-plus/icons-vue'
import request from '@/api/request.js'

const historyList = ref([])

const detailVisible = ref(false)
const detailExam = ref({})
const detailQuestions = ref([])

const typeMap = { 1: '单选', 2: '多选', 3: '判断', 4: '填空', 5: '简答', 6: '编程' }
const typeLabel = (t) => typeMap[t] || t

// 加载考试记录
const loadHistory = async () => {
  try {
    const res = await request.get('/exam/my-exams')
    const records = res || []
    
    historyList.value = records.map(record => {
      let duration = '--'
      if (record.startTime && record.submitTime) {
        const start = new Date(record.startTime)
        const submit = new Date(record.submitTime)
        const seconds = Math.round((submit - start) / 1000)
        duration = seconds < 60 ? seconds + '秒' : Math.round(seconds / 60) + '分钟'
      }
      
      return {
        id: record.id,
        name: record.sessionName || record.paperName,  // 【修改】优先用考试名称
        subject: record.paperName,
        score: record.totalScore || 0,
        totalScore: record.totalScore || 0,
        duration: duration,
        submitTime: record.submitTime ? record.submitTime.replace('T', ' ').substring(0, 16) : '',
        rank: record.rank || 0
      }
    })
  } catch (e) {
    console.error('获取考试记录失败', e)
    ElMessage.error('获取考试记录失败')
  }
}
const handleView = async (row) => {
  try {
    const res = await request.get(`/exam/result/${row.id}`)
    detailExam.value = {
      id: res.recordId,
      name: res.paperName,
      score: res.totalScore || 0,
      totalScore: res.totalScore || 0,
      rank: res.rank || 1,    // 【新增】
      duration: res.duration || '--'  // 【新增】
    }
    detailQuestions.value = (res.questions || []).map(q => ({
      type: q.questionType,
      content: q.content,
      studentAnswer: q.studentAnswer || '（未作答）',
      correctAnswer: q.answer,
      isCorrect: q.status === 'correct',
      score: q.score,
      gotScore: q.gotScore || 0
    }))
    detailVisible.value = true
  } catch (e) {
    console.error('获取答卷详情失败', e)
    ElMessage.error('获取答卷详情失败')
  }
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.exam-history { width: 100%; }
.flex-between { display: flex; justify-content: space-between; align-items: center; }
.mb-4 { margin-bottom: 16px; }
.mb-2 { margin-bottom: 8px; }
.mb-1 { margin-bottom: 4px; }
.font-bold { font-weight: bold; }
.text-gray { color: #909399; }
.text-success { color: #67c23a; }
.text-danger { color: #f56c6c; }
.detail-question { border-bottom: 1px dashed #dcdfe6; padding-bottom: 12px; }
</style>
