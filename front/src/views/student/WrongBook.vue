<template>
  <div class="wrong-book">
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="科目">
          <el-select v-model="searchForm.subject" placeholder="全部" clearable class="w-32">
            <el-option v-for="s in subjects" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>查询</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="flex-between">
          <span><el-icon><CircleClose /></el-icon> 错题本（共 {{ wrongList.length }} 道）</span>
          <el-button type="primary" @click="startPractice"><el-icon><EditPen /></el-icon>重新练习</el-button>
        </div>
      </template>

      <el-empty v-if="!wrongList.length" description="暂无错题" />
      <div v-else>
        <el-collapse v-model="activeNames">
          <el-collapse-item v-for="(q, idx) in wrongList" :key="q.id" :name="String(q.id)">
            <template #title>
              <div class="collapse-title flex-between w-full pr-4">
                <span>{{ idx + 1 }}. 【{{ q.type }}】{{ q.content }}</span>
                <el-tag size="small" class="ml-2">{{ q.subject }}</el-tag>
              </div>
            </template>
            <div class="question-detail">
              <div class="mb-2"><span class="text-danger">你的答案：{{ q.studentAnswer || '（未作答）' }}</span></div>
              <div class="mb-2"><span class="text-success">正确答案：{{ q.correctAnswer }}</span></div>
              <div class="mb-2"><span class="text-gray">出自考试：{{ q.examName }}</span></div>
              <div class="mb-2"><span class="text-gray">错误时间：{{ q.wrongTime }}</span></div>
              <div class="bg-gray p-3 rounded">
                <div class="text-gray text-sm mb-1">解析：</div>
                <div>{{ q.analysis }}</div>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-card>

    <!-- 重新练习弹窗 -->
    <el-dialog v-model="practiceVisible" title="错题练习" width="700px" :close-on-click-modal="false" destroy-on-close>
      <div v-if="practiceList.length" class="practice-content">
        <div class="mb-4">第 {{ practiceIndex + 1 }} / {{ practiceList.length }} 题</div>
        <div class="font-bold mb-4">{{ currentPractice.content }}</div>
        <el-radio-group v-if="currentPractice.type === 'single'" v-model="practiceAnswer" class="question-options">
          <el-radio v-for="(opt, oidx) in currentPractice.options" :key="oidx" :label="String.fromCharCode(65+oidx)">
            {{ String.fromCharCode(65+oidx) }}. {{ opt }}
          </el-radio>
        </el-radio-group>
        <el-input v-else v-model="practiceAnswer" type="textarea" :rows="3" placeholder="请输入答案" />
        <div v-if="showResult" class="mt-4">
          <el-alert v-if="isCorrect" title="回答正确！" type="success" :closable="false" />
          <el-alert v-else :title="`回答错误，正确答案是：${currentPractice.correctAnswer}`" type="error" :closable="false" />
        </div>
      </div>
      <template #footer>
        <el-button @click="practiceVisible = false">关闭</el-button>
        <el-button v-if="!showResult" type="primary" @click="checkAnswer">提交答案</el-button>
        <el-button v-else type="primary" @click="nextPractice">下一题</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleClose, EditPen, Search, Refresh } from '@element-plus/icons-vue'
import request from '@/api/request.js'

const subjects = ref([])
const searchForm = ref({ subject: '' })
const activeNames = ref([])
const wrongList = ref([])
const loading = ref(false)

const typeMap = { 1: '单选', 2: '多选', 3: '判断', 4: '填空', 5: '简答', 6: '编程' }
const typeLabel = (t) => typeMap[t] || '未知'

// 格式化答案显示
// 支持：普通字符串("final")、JSON 对象({"answer":"true"})、JSON 数组(["a","b"])、
//       逗号分隔串("public,private,protected")、Java 数组 toString("[a, b, c]")
const formatAnswerDisplay = (answer) => {
  if (answer === null || answer === undefined || answer === '') return '（未作答）'

  // 数组：逗号连接
  if (Array.isArray(answer)) {
    return answer.map(a => String(a).trim()).filter(v => v !== '').join(', ') || '（未作答）'
  }

  // 对象：提取 answer 字段
  if (typeof answer === 'object') {
    if ('answer' in answer) return formatAnswerDisplay(answer.answer)
    const vals = Object.values(answer)
    return vals.length ? vals.map(v => String(v).trim()).join(', ') : '（未作答）'
  }

  if (typeof answer !== 'string') return String(answer)

  let s = answer.trim()
  if (s === '') return '（未作答）'

  // JSON 字符串：对象取 answer 字段，数组逗号连接
  if (s.startsWith('{') || s.startsWith('[')) {
    try {
      return formatAnswerDisplay(JSON.parse(s))
    } catch (e) {
      // 解析失败可能是 Java 数组 toString，如 [a, b, c]，去掉首尾方括号
      if (s.startsWith('[') && s.endsWith(']')) s = s.slice(1, -1)
    }
  }

  // 普通字符串：逗号分隔统一为 ", "
  return s.replace(/,\s*/g, ', ')
}

// 加载科目列表
const loadSubjects = async () => {
  try {
    const res = await request.get('/question/list', { params: { page: 1, size: 1000 } })
    const records = res.records || []
    const subjectSet = [...new Set(records.map(q => q.subject).filter(s => s))]
    subjects.value = subjectSet
  } catch (e) {
    console.error('获取科目列表失败', e)
  }
}

// 加载错题本
const loadWrongBook = async () => {
  loading.value = true
  try {
    const res = await request.get('/wrong-book/list', {
      params: {
        page: 1,
        size: 100,
        subject: searchForm.value.subject || null,
        isMastered: null
      }
    })
    const records = res.records || []
    
    wrongList.value = records.map(wb => ({
      id: wb.id,
      type: typeLabel(wb.questionType),
      content: wb.content || '题目加载失败',
      options: wb.options || [],
      studentAnswer: formatAnswerDisplay(wb.lastWrongAnswer),
      correctAnswer: formatAnswerDisplay(wb.correctAnswer),
      subject: wb.subject || '未知',
      examName: wb.examName || '', 
      wrongTime: wb.createTime ? wb.createTime.substring(0, 10) : '',
      analysis: wb.analysis || '暂无解析',
      isMastered: wb.isMastered
    }))
  } catch (e) {
    console.error('获取错题本失败', e)
    ElMessage.error('获取错题本失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => loadWrongBook()
const handleReset = () => { searchForm.value.subject = ''; loadWrongBook() }

// 练习功能（保持原有逻辑）
const practiceVisible = ref(false)
const practiceList = ref([])
const practiceIndex = ref(0)
const practiceAnswer = ref('')
const showResult = ref(false)

const currentPractice = computed(() => practiceList.value[practiceIndex.value] || {})
const isCorrect = computed(() => {
  const ca = String(currentPractice.value.correctAnswer || '').trim()
  const pa = Array.isArray(practiceAnswer.value) 
    ? practiceAnswer.value.sort().join(',') 
    : String(practiceAnswer.value).trim()
  return ca === pa
})

const startPractice = () => {
  practiceList.value = [...wrongList.value]
  practiceIndex.value = 0
  practiceAnswer.value = ''
  showResult.value = false
  practiceVisible.value = true
}

const checkAnswer = () => {
  if (!practiceAnswer.value || (Array.isArray(practiceAnswer.value) && !practiceAnswer.value.length)) {
    ElMessage.warning('请先输入答案')
    return
  }
  showResult.value = true
}

const nextPractice = () => {
  if (practiceIndex.value < practiceList.value.length - 1) {
    practiceIndex.value++
    practiceAnswer.value = ''
    showResult.value = false
  } else {
    ElMessage.success('练习完成')
    practiceVisible.value = false
  }
}

onMounted(() => {
  loadSubjects()
  loadWrongBook()
})
</script>

<style scoped>
.wrong-book { width: 100%; }
.search-card { margin-bottom: 16px; }
.flex-between { display: flex; justify-content: space-between; align-items: center; }
.w-full { width: 100%; }
.pr-4 { padding-right: 16px; }
.collapse-title { font-size: 14px; }
.question-detail { padding: 8px 12px; }
.mb-2 { margin-bottom: 8px; }
.mb-4 { margin-bottom: 16px; }
.mt-4 { margin-top: 16px; }
.font-bold { font-weight: bold; }
.text-gray { color: #909399; }
.text-success { color: #67c23a; }
.text-danger { color: #f56c6c; }
.bg-gray { background: #f5f7fa; }
.p-3 { padding: 12px; }
.rounded { border-radius: 6px; }
.text-sm { font-size: 13px; }
.question-options { display: flex; flex-direction: column; gap: 10px; }
.ml-2 { margin-left: 8px; }
.w-32 { width: 128px; }
</style>
