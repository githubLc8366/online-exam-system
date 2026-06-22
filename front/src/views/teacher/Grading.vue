<template>
  <div class="grading-management">
    <!-- 考试选择 -->
    <el-card class="mb-4" shadow="never">
      <el-form inline>
        <el-form-item label="选择考试">
          <el-select 
            v-model="selectedSessionId" 
            placeholder="请选择考试场次" 
            @change="loadGradeData"
            style="width: 300px;"
          >
            <el-option 
              v-for="s in sessionList" 
              :key="s.id" 
              :label="s.sessionName" 
              :value="s.id" 
            />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>
    <!-- 统计看板 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="4">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value text-primary">{{ stats.total }}</div>
          <div class="stat-label">参考人数</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="4">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value text-success">{{ stats.maxScore }}</div>
          <div class="stat-label">最高分</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="4">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value text-warning">{{ stats.minScore }}</div>
          <div class="stat-label">最低分</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="4">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value text-danger">{{ stats.avgScore }}</div>
          <div class="stat-label">平均分</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="4">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value text-info">{{ stats.passRate }}%</div>
          <div class="stat-label">及格率</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="4">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value">{{ stats.pending }}</div>
          <div class="stat-label">待阅卷</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表分析区 -->
    <el-row :gutter="20" class="mb-4">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header><span>班级成绩对比</span></template>
          <div ref="classChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header><span>知识点掌握度</span></template>
          <div ref="knowledgeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 试题质量分析 -->
    <el-row :gutter="20" class="mb-4">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>
            <div class="flex-between">
              <span>试题质量分析（难度系数 & 区分度）</span>
              <el-tooltip content="难度系数=平均分/满分；区分度=高分组答对率-低分组答对率">
                <el-icon><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>
          <el-table :data="questionQuality" stripe border size="small">
            <el-table-column type="index" label="题号" width="60" align="center" />
            <el-table-column prop="content" label="题目内容" min-width="200" show-overflow-tooltip />
            <el-table-column prop="type" label="题型" width="80" align="center" />
            <el-table-column prop="score" label="分值" width="70" align="center" />
            <el-table-column prop="difficulty" label="难度系数" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.difficulty > 0.7 ? 'success' : row.difficulty > 0.4 ? 'warning' : 'danger'">{{ row.difficulty.toFixed(2) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="discrimination" label="区分度" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.discrimination > 0.3 ? 'success' : row.discrimination > 0.1 ? 'warning' : 'danger'">{{ row.discrimination.toFixed(2) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="correctRate" label="正确率" width="80" align="center">
              <template #default="{ row }">{{ (row.correctRate * 100).toFixed(1) }}%</template>
            </el-table-column>
            <el-table-column label="评价" width="100" align="center">
              <template #default="{ row }">
                <span v-if="row.difficulty > 0.7 && row.discrimination > 0.3" class="text-success">优秀</span>
                <span v-else-if="row.difficulty < 0.2 || row.difficulty > 0.9" class="text-danger">需调整</span>
                <span v-else class="text-warning">一般</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作栏 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="flex-between">
          <span>成绩列表</span>
          <div class="flex gap-2">
            <el-button type="success" @click="handleExport"><el-icon><Download /></el-icon>导出成绩</el-button>
            <el-button type="primary" @click="openWrongAnalysis"><el-icon><DataAnalysis /></el-icon>错题分析</el-button>
          </div>
        </div>
      </template>

      <el-table :data="scoreList" v-loading="loading" stripe border :header-cell-style="{ background: '#f5f7fa' }">
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column prop="studentNo" label="学号" width="110" />
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column prop="objectiveScore" label="客观题" width="80" align="center" />
        <el-table-column prop="subjectiveScore" label="主观题" width="80" align="center" />
        <el-table-column prop="totalScore" label="总分" width="80" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.totalScore < stats.passLine, 'text-success': row.totalScore >= stats.passLine }">{{ row.totalScore }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'graded'" type="success" size="small">已阅卷</el-tag>
            <el-tag v-else type="warning" size="small">待阅卷</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="异常" width="70" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.abnormal" type="danger" size="small">可疑</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="交卷时间" width="150" />
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleGrade(row)"><el-icon><EditPen /></el-icon>阅卷</el-button>
            <el-button link type="primary" size="small" @click="handleView(row)"><el-icon><View /></el-icon>查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 阅卷弹窗 -->
    <el-dialog v-model="gradeVisible" :title="`阅卷 - ${gradeTarget.name}`" width="800px" :close-on-click-modal="false" destroy-on-close>
      <div class="grade-info mb-4">
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="学号">{{ gradeTarget.studentNo }}</el-descriptions-item>
          <el-descriptions-item label="班级">{{ gradeTarget.className }}</el-descriptions-item>
          <el-descriptions-item label="客观题">{{ gradeTarget.objectiveScore }} 分</el-descriptions-item>
          <el-descriptions-item label="当前总分">{{ gradeTarget.objectiveScore + subjectiveTotal }} 分</el-descriptions-item>
        </el-descriptions>
      </div>
      <div v-for="(q, idx) in subjectiveQuestions" :key="idx" class="grade-question mb-4">
        <div class="font-bold mb-2">{{ idx + 1 }}. 【{{ typeLabel(q.type) }}】{{ q.content }}（满分 {{ q.maxScore }} 分）</div>
        <div class="answer-box bg-gray p-3 rounded mb-2">
          <div class="text-gray text-sm mb-1">学生答案：</div>
          <div>{{ q.studentAnswer || '（未作答）' }}</div>
        </div>
        <div class="answer-box bg-light p-3 rounded mb-2">
          <div class="text-gray text-sm mb-1">参考答案：</div>
          <div>{{ q.answer }}</div>
        </div>
        <div class="flex items-center gap-2">
          <span>得分：</span>
          <el-input-number v-model="q.score" :min="0" :max="q.maxScore" size="small" />
          <span>/ {{ q.maxScore }} 分</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="gradeVisible = false">取消</el-button>
        <el-button type="primary" @click="submitGrade" :loading="gradeLoading">提交评分</el-button>
      </template>
    </el-dialog>

    <!-- 错题分析弹窗 -->
    <el-dialog v-model="analysisVisible" title="错题正确率分析" width="700px" :close-on-click-modal="false">
      <el-empty v-if="!wrongAnalysis.length" description="暂无可分析的试题数据（请先选择有成绩的考试场次）" />
      <div v-for="(item, idx) in wrongAnalysis" :key="idx" class="analysis-item mb-3">
        <div class="flex-between mb-1">
          <span class="text-sm">{{ idx + 1 }}. {{ item.content }}</span>
          <span class="text-sm font-bold" :class="item.correctRate < 50 ? 'text-danger' : 'text-success'">{{ item.correctRate }}%</span>
        </div>
        <el-progress :percentage="item.correctRate" :color="item.correctRate < 50 ? '#f56c6c' : item.correctRate < 80 ? '#e6a23c' : '#67c23a'" :stroke-width="16">
          <span>{{ item.wrongCount }}/{{ stats.total }} 人错</span>
        </el-progress>
      </div>
    </el-dialog>

    <!-- 查看详情弹窗 -->
    <el-dialog v-model="viewVisible" title="答题详情" width="800px" :close-on-click-modal="false">
      <el-descriptions :column="2" border size="small" class="mb-4">
        <el-descriptions-item label="姓名">{{ viewTarget.name }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ viewTarget.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="客观题得分">{{ viewTarget.objectiveScore }}</el-descriptions-item>
        <el-descriptions-item label="主观题得分">{{ viewTarget.subjectiveScore }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ viewTarget.totalScore }}</el-descriptions-item>
        <el-descriptions-item label="交卷时间">{{ viewTarget.submitTime }}</el-descriptions-item>
      </el-descriptions>
      
      <div v-for="(q, idx) in (viewTarget.questions || [])" :key="idx" class="mb-3 p-3" style="border:1px solid #ebeef5;border-radius:6px;">
        <div class="font-bold mb-2">{{ idx + 1 }}. {{ q.content }}</div>
        <div class="mb-1"><span class="text-gray">学生答案：</span>{{ q.studentAnswer || '（未作答）' }}</div>
        <div class="mb-1"><span class="text-gray">正确答案：</span>{{ q.answer }}</div>
        <div class="mb-1"><span class="text-gray">得分：</span>{{ q.gotScore || 0 }}/{{ q.score }} 分</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, DataAnalysis, EditPen, View, QuestionFilled } from '@element-plus/icons-vue'
import request from '@/api/request.js'

// 考试场次列表（用于选择查看哪场考试的成绩）
const sessionList = ref([])
const selectedSessionId = ref(null)

const stats = ref({ total: 0, maxScore: 0, minScore: 0, avgScore: 0, passRate: 0, pending: 0, passLine: 60 })
const scoreList = ref([])
const loading = ref(false)

// 图表引用
const classChartRef = ref(null)
const knowledgeChartRef = ref(null)

// 试题质量分析
const questionQuality = ref([])

// 阅卷
const gradeVisible = ref(false)
const gradeTarget = ref({})
const gradeLoading = ref(false)
const subjectiveQuestions = ref([])
const subjectiveTotal = computed(() => subjectiveQuestions.value.reduce((sum, q) => sum + (q.score || 0), 0))

// 查看
const viewVisible = ref(false)
const viewTarget = ref({})

// 错题分析
const analysisVisible = ref(false)
const wrongAnalysis = ref([])
let classChartInstance = null
let knowledgeChartInstance = null

// 加载考试场次列表
const loadSessions = async () => {
  try {
    const res = await request.get('/session/list', { params: { page: 1, size: 100 } })
    sessionList.value = res.records || []
    if (sessionList.value.length > 0) {
      selectedSessionId.value = sessionList.value[0].id
      loadGradeData()
    }
  } catch (e) {
    console.error('获取考试列表失败', e)
  }
}

// 加载成绩统计数据
const loadStats = async () => {
  if (!selectedSessionId.value) return
  try {
    const res = await request.get(`/grade/statistics/${selectedSessionId.value}`)
    if (res) {
      stats.value = {
        total: res.totalCount || 0,
        maxScore: res.maxScore || 0,
        minScore: res.minScore || 0,
        avgScore: res.avgScore || 0,
        passRate: parseFloat(res.passRate) || 0,  
        pending: res.pendingCount || 0,
        passLine: res.passLine || 60
      }
    }
  } catch (e) {
    console.error('获取统计数据失败', e)
  }
}

// 加载成绩列表
const loadScoreList = async () => {
  if (!selectedSessionId.value) return
  loading.value = true
  try {
    const res = await request.get('/grade/score-list/' + selectedSessionId.value)
    scoreList.value = (res || []).map(s => ({
      recordId: s.recordId,
      studentNo: s.studentNo,
      name: s.name,
      className: s.className || '未知班级',
      objectiveScore: s.objectiveScore || 0,
      subjectiveScore: s.subjectiveScore || 0,
      totalScore: s.totalScore || 0,
      status: s.status || 'pending',
      submitTime: s.submitTime ? s.submitTime.replace('T', ' ').substring(0, 16) : '',
      abnormal: s.abnormal || false
    }))
  } catch (e) {
    console.error('获取成绩列表失败', e)
    ElMessage.error('获取成绩列表失败')
  } finally {
    loading.value = false
  }
}
const loadQuestionQuality = async () => {
  if (!selectedSessionId.value) return
  try {
    const res = await request.get(`/grade/question-quality/${selectedSessionId.value}`)
    questionQuality.value = res || []
  } catch (e) {
    console.error('获取试题质量失败', e)
  }
}

// 打开错题正确率分析：基于已加载的试题质量数据生成（按正确率升序，最易错的排最前）
const openWrongAnalysis = () => {
  wrongAnalysis.value = (questionQuality.value || [])
    .map(q => {
      const rate = Number(q.correctRate) || 0          // 后端返回 0~1 的小数
      const correctRate = Math.round(rate * 100)        // 转成百分比整数供进度条使用
      const wrongCount = Math.max(0, Math.round(stats.value.total * (1 - rate)))
      return { content: q.content || '', correctRate, wrongCount }
    })
    .sort((a, b) => a.correctRate - b.correctRate)
  analysisVisible.value = true
}

// 加载所有数据
const loadGradeData = () => {
  loadStats()
  loadScoreList()
  loadClassComparison()      // 新增
  loadKnowledgeMastery()     // 新增
  loadQuestionQuality()
}

// 阅卷
const handleGrade = async (row) => {
  gradeTarget.value = { ...row }

  try {
    const res = await request.get(`/exam/result/${row.recordId}`)
    const questions = res.questions || []

    // 主观题：简答=5, 编程=6
    const allSubjective = questions.filter(q => (q.questionType === 5 || q.questionType === 6))

    if (allSubjective.length === 0) {
      ElMessage.info('该试卷没有主观题需要阅卷')
      return
    }

    // 只显示未批阅（status !== 'graded'）的主观题
    const pending = allSubjective.filter(q => q.status !== 'graded')
    if (pending.length === 0) {
      ElMessage.info('该试卷主观题已全部批阅完成')
      return
    }

    subjectiveQuestions.value = pending.map(q => ({
      questionId: q.questionId,
      type: q.questionType === 5 ? '简答' : '编程',
      content: q.content || '',
      maxScore: q.score || 0,
      score: 0,
      studentAnswer: q.studentAnswer || '（未作答）',
      answer: q.answer || '暂无参考答案',
      status: q.status
    }))

    gradeVisible.value = true
  } catch (e) {
    console.error('获取答卷失败', e)
    ElMessage.error('获取答卷失败')
  }
}
// 提交评分
const submitGrade = async () => {
  // 检测是否有未打分（得分框留空）的题目，提醒教师避免漏判
  const unscored = subjectiveQuestions.value.filter(
    q => q.score === null || q.score === undefined || q.score === ''
  )
  if (unscored.length > 0) {
    try {
      await ElMessageBox.confirm(
        `还有 ${unscored.length} 道主观题未打分，未打分的题目将按 0 分计。确定要提交吗？`,
        '存在未打分题目',
        { confirmButtonText: '确定提交', cancelButtonText: '继续打分', type: 'warning' }
      )
    } catch {
      return // 教师选择「继续打分」，取消提交
    }
  }

  gradeLoading.value = true
  try {
    for (const q of subjectiveQuestions.value) {
      // 得分输入框被清空时 q.score 为 null，直接发给后端会触发空指针（系统内部错误）。
      // 这里统一兜底为 0 分，并确保是有效数字。
      const raw = Number(q.score)
      const score = Number.isFinite(raw) ? raw : 0
      await request.post('/grade/mark', {
        recordId: gradeTarget.value.recordId,
        questionId: q.questionId,
        score,
        comment: ''
      })
    }
    ElMessage.success('评分提交成功')
    gradeVisible.value = false
    gradeLoading.value = false
    loadGradeData()
  } catch (e) {
    console.error('提交评分失败', e)
    ElMessage.error('提交评分失败')
    gradeLoading.value = false
  }
}

// 查看详情
const handleView = async (row) => {
  try {
    const res = await request.get(`/exam/result/${row.recordId}`)
    viewTarget.value = {
      name: row.name,
      studentNo: row.studentNo,
      objectiveScore: row.objectiveScore,
      subjectiveScore: row.subjectiveScore,
      totalScore: row.totalScore,
      submitTime: row.submitTime,
      questions: res.questions || []
    }
    viewVisible.value = true
  } catch (e) {
    console.error('获取详情失败', e)
    ElMessage.error('获取详情失败')
  }
}

// 导出成绩
const handleExport = () => {
  if (!scoreList.value.length) {
    ElMessage.warning('当前没有可导出的成绩')
    return
  }
  ElMessage.info('正在生成成绩导出文件...')
  setTimeout(() => {
    const headers = ['学号', '姓名', '班级', '总分', '客观题', '主观题', '状态']
    const rows = scoreList.value.map(r => [r.studentNo, r.name, r.className, r.totalScore, r.objectiveScore, r.subjectiveScore, r.status === 'graded' ? '已阅卷' : '待阅卷'])
    const csv = [headers.join(','), ...rows.map(r => r.map(f => `"${String(f).replace(/"/g, '""')}"`).join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    const sessionName = sessionList.value.find(s => s.id === selectedSessionId.value)?.sessionName || '成绩'
    link.download = `${sessionName}_成绩_${new Date().toLocaleDateString()}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)
    ElMessage.success('成绩导出成功')
  }, 800)
}

const typeMap = { single: '单选', multiple: '多选', judge: '判断', fill: '填空', short: '简答', program: '编程' }
const typeLabel = (t) => typeMap[t] || t

const formatTime = (seconds) => {
  if (seconds <= 0) return '00:00'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}
// 加载班级成绩对比图（每个班级一组：平均分 / 最高分 / 最低分）
const loadClassComparison = async () => {
  if (!selectedSessionId.value) return
  try {
    const res = await request.get(`/grade/class-comparison/${selectedSessionId.value}`)
    if (!classChartRef.value) return

    if (!classChartInstance) {
      classChartInstance = echarts.init(classChartRef.value)
    }

    const hasData = res && Array.isArray(res.classNames) && res.classNames.length > 0
    if (!hasData) {
      classChartInstance.clear()
      classChartInstance.setOption({
        title: {
          text: '暂无成绩数据',
          left: 'center',
          top: 'middle',
          textStyle: { color: '#909399', fontSize: 14, fontWeight: 'normal' }
        }
      })
      return
    }

    classChartInstance.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: ['平均分', '最高分', '最低分'], top: 0 },
      grid: { left: 40, right: 20, top: 36, bottom: 28, containLabel: true },
      xAxis: {
        type: 'category',
        data: res.classNames,
        axisLabel: { interval: 0, rotate: res.classNames.length > 4 ? 20 : 0 }
      },
      yAxis: { type: 'value', max: 100, name: '分数' },
      series: [
        {
          name: '平均分', type: 'bar', data: res.avgScores || [],
          itemStyle: { color: '#409eff' },
          label: { show: true, position: 'top', formatter: '{c}' }
        },
        {
          name: '最高分', type: 'bar', data: res.maxScores || [],
          itemStyle: { color: '#67c23a' },
          label: { show: true, position: 'top', formatter: '{c}' }
        },
        {
          name: '最低分', type: 'bar', data: res.minScores || [],
          itemStyle: { color: '#f56c6c' },
          label: { show: true, position: 'top', formatter: '{c}' }
        }
      ]
    }, true)
  } catch (e) {
    console.error('获取班级对比数据失败', e)
  }
}

// 加载知识点掌握度图
const loadKnowledgeMastery = async () => {
  if (!selectedSessionId.value) return
  try {
    const res = await request.get(`/grade/knowledge-mastery/${selectedSessionId.value}`)
    if (knowledgeChartRef.value && res && res.knowledge && res.knowledge.length > 0) {
      if (!knowledgeChartInstance) {
        knowledgeChartInstance = echarts.init(knowledgeChartRef.value)
      }
      knowledgeChartInstance.setOption({
        tooltip: { 
    trigger: 'axis',
    formatter: function(params) {
        const idx = params[0].dataIndex;
        const full = res.fullContents ? res.fullContents[idx] : '';
        return '<strong>' + full + '</strong><br/>掌握度：' + params[0].value + '%'
    }
},

        xAxis: { type: 'category', data: res.knowledge, axisLabel: { rotate: 30 } },
        yAxis: { type: 'value', max: 100, name: '掌握度(%)' },
        series: [{
          type: 'bar',
          data: res.mastery || [],
          itemStyle: { color: '#67c23a' },
          label: { show: true, position: 'top', formatter: '{c}%' }
        }]
      })
    }
  } catch (e) {
    console.error('获取知识点掌握度失败', e)
  }
}

onMounted(() => {
  loadSessions()
})
</script>

<style scoped>
.grading-management { width: 100%; }
.stats-row { margin-bottom: 20px; }
.stat-card { text-align: center; padding: 10px; margin-bottom: 16px; }
.stat-value { font-size: 24px; font-weight: bold; line-height: 1.2; }
.stat-label { font-size: 13px; color: #909399; margin-top: 6px; }
.table-card { margin-bottom: 16px; }
.text-primary { color: #409eff; }
.text-success { color: #67c23a; }
.text-warning { color: #e6a23c; }
.text-danger { color: #f56c6c; }
.text-info { color: #909399; }
.font-bold { font-weight: bold; }
.text-sm { font-size: 13px; }
.text-gray { color: #909399; }
.bg-gray { background-color: #f5f7fa; }
.bg-light { background-color: #f0f9ff; }
.p-3 { padding: 12px; }
.rounded { border-radius: 6px; }
.mb-1 { margin-bottom: 4px; }
.mb-2 { margin-bottom: 8px; }
.mb-3 { margin-bottom: 12px; }
.mb-4 { margin-bottom: 16px; }
.flex { display: flex; }
.items-center { align-items: center; }
.gap-2 { gap: 8px; }
.flex-between { display: flex; justify-content: space-between; align-items: center; }
.grade-question { border-bottom: 1px dashed #dcdfe6; padding-bottom: 12px; }
.analysis-item { padding: 8px 0; }
.chart-container { width: 100%; height: 280px; }
</style>
