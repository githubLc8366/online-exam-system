<template>
  <div class="exam-paper-management">
    <!-- 搜索区 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="试卷名称">
          <el-input v-model="searchForm.name" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="科目">
          <el-input v-model="searchForm.subject" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="组卷方式">
          <el-select v-model="searchForm.mode" placeholder="全部" clearable class="w-28">
            <el-option label="手动组卷" value="manual" />
            <el-option label="随机组卷" value="random" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>查询</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表区 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="flex-between">
          <span>试卷列表</span>
          <el-button type="primary" @click="handleCreate"><el-icon><Plus /></el-icon>新建试卷</el-button>
        </div>
      </template>

      <el-table :data="paperList" v-loading="loading" stripe border :header-cell-style="{ background: '#f5f7fa' }">
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column prop="name" label="试卷名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="subject" label="科目" width="120" />
        <el-table-column prop="mode" label="组卷方式" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.mode === 'manual' ? 'primary' : 'success'">{{ row.mode === 'manual' ? '手动' : '随机' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" width="80" align="center" />
        <el-table-column prop="questionCount" label="题数" width="70" align="center" />
        <el-table-column prop="duration" label="时长(分)" width="90" align="center" />
        <el-table-column prop="passScore" label="及格线" width="80" align="center" />
        <el-table-column prop="createdAt" label="创建时间" width="140" />
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handlePreview(row)"><el-icon><View /></el-icon>预览</el-button>
            <el-button link type="primary" size="small" @click="handleEdit(row)"><el-icon><Edit /></el-icon>编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)"><el-icon><Delete /></el-icon>删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.pageSize" :page-sizes="[10,20,50]" :total="pagination.total" layout="total, sizes, prev, pager, next, jumper" @size-change="handleSizeChange" @current-change="handlePageChange" />
      </div>
    </el-card>

    <!-- 组卷弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="900px" :close-on-click-modal="false" destroy-on-close>
      <el-steps :active="activeStep" finish-status="success" simple>
        <el-step title="基本信息" />
        <el-step title="配置题目" />
      </el-steps>

      <!-- 步骤1：基本信息 -->
      <div v-show="activeStep === 0" class="step-content">
        <el-form ref="step1Ref" :model="paperStore.currentPaper" :rules="step1Rules" label-width="100px">
          <el-form-item label="试卷名称" prop="name">
            <el-input v-model="paperStore.currentPaper.name" placeholder="如：软件工程期中A卷" />
          </el-form-item>
          <el-form-item label="科目" prop="subject">
            <el-select v-model="paperStore.currentPaper.subject" placeholder="请选择科目" clearable class="w-full">
              <el-option v-for="subject in subjectList" :key="subject" :label="subject" :value="subject" />
            </el-select>
          </el-form-item>
          <el-form-item label="组卷方式" prop="mode">
            <el-radio-group v-model="paperStore.currentPaper.mode">
              <el-radio label="manual">手动挑选</el-radio>
              <el-radio label="random">随机抽题</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="考试时长(分钟)" prop="duration">
            <el-input-number v-model="paperStore.currentPaper.duration" :min="1" :max="300" />
          </el-form-item>
        </el-form>
      </div>

      <!-- 步骤2：配置题目 -->
      <div v-show="activeStep === 1" class="step-content">
        <!-- 手动组卷 -->
        <template v-if="paperStore.currentPaper.mode === 'manual'">
          <div class="flex-between mb-4">
            <span>已选题目：{{ paperStore.questionCount }} 道，总分：{{ paperStore.totalScore }} 分</span>
            <el-button type="primary" size="small" @click="openQuestionSelector"><el-icon><Plus /></el-icon>从题库选题</el-button>
          </div>
          <el-table :data="paperStore.selectedQuestions" border size="small" max-height="300">
            <el-table-column type="index" label="序号" width="50" align="center" />
            <el-table-column prop="type" label="题型" width="80" align="center">
              <template #default="{ row }"><el-tag size="small">{{ typeLabel(row.type) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="content" label="题目内容" min-width="200" show-overflow-tooltip />
            <el-table-column prop="knowledge" label="知识点" width="120" />
            <el-table-column prop="difficulty" label="难度" width="70" align="center">
              <template #default="{ row }"><el-tag size="small" :type="difficultyColor(row.difficulty)">{{ diffMap[row.difficulty] }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="score" label="分值" width="80" align="center">
              <template #default="{ row }">
                <el-input-number v-model="row.score" :min="1" :max="100" size="small" style="width: 70px;" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="{ row }">
                <el-button link type="danger" size="small" @click="paperStore.removeQuestion(row.id)"><el-icon><Delete /></el-icon></el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 及格线设置 -->
          <el-divider />
          <el-form-item label="及格线(分)">
            <el-input-number 
              v-model="paperStore.currentPaper.passScore" 
              :min="0" 
              :max="paperStore.totalScore" 
              :precision="1" 
            />
            <span class="text-gray text-sm ml-2">
              建议：{{ (paperStore.totalScore * 0.6).toFixed(1) }} 分（总分的60%）
            </span>
          </el-form-item>
        </template>

        <!-- 随机组卷 -->
        <template v-else>
          <el-alert title="系统将按以下配置自动从题库中抽取题目" type="info" :closable="false" class="mb-4" />
          <!-- 添加：显示题库可用数量 -->
          <el-alert v-if="subjectStatsLoaded" type="success" :closable="false" class="mb-4">
            <template #title>
              <span>当前科目题库可用：</span>
              <span v-for="(stat, key) in questionStats" :key="key" class="mr-3">
                {{ statLabel(key) }}：<strong>{{ stat.count }}</strong>道
              </span>
            </template>
          </el-alert>
          <el-form label-width="120px">
            <el-row :gutter="20">
              <el-col :span="12" v-for="(cfg, key) in paperStore.randomConfig" :key="key">
                <template v-if="key !== 'difficultyRatio' && key !== 'knowledgeRatio'">
                  <el-form-item :label="randomLabel(key)">
                    <div class="flex gap-2">
                      <el-input-number v-model="cfg.count" :min="0" :max="50" size="small" />
                      <span class="text-gray text-sm">题 ×</span>
                      <el-input-number v-model="cfg.score" :min="1" :max="50" size="small" />
                      <span class="text-gray text-sm">分</span>
                    </div>
                  </el-form-item>
                </template>
              </el-col>
            </el-row>
            <el-form-item label="难度比例">
              <div class="flex gap-4">
                <div>易：<el-input-number v-model="paperStore.randomConfig.difficultyRatio.easy" :min="0" :max="100" size="small" />%</div>
                <div>中：<el-input-number v-model="paperStore.randomConfig.difficultyRatio.medium" :min="0" :max="100" size="small" />%</div>
                <div>难：<el-input-number v-model="paperStore.randomConfig.difficultyRatio.hard" :min="0" :max="100" size="small" />%</div>
              </div>
            </el-form-item>
            <el-form-item label="知识点比例（可选）">
              <div style="width: 100%;">
                <div class="text-gray text-sm mb-2">设置各知识点占比，留空则不按知识点筛选</div>
                <div v-for="(val, key, idx) in paperStore.randomConfig.knowledgeRatio" :key="idx" class="flex items-center gap-2 mb-2">
                  <span style="width: 100px;">{{ key }}：</span>
                  <el-input-number v-model="paperStore.randomConfig.knowledgeRatio[key]" :min="0" :max="100" size="small" />
                  <span>%</span>
                  <el-button link type="danger" size="small" @click="delete paperStore.randomConfig.knowledgeRatio[key]">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
                <div class="flex gap-2">
                  <el-select v-model="newKnowledgeName" placeholder="选择知识点" size="small" style="width: 140px;" clearable>
                    <el-option v-for="tag in knowledgeTags" :key="tag" :label="tag" :value="tag" />
                  </el-select>
                  <el-button link type="primary" size="small" @click="addKnowledgeItem">
                    <el-icon><Plus /></el-icon>添加
                  </el-button>
                </div>
              </div>
            </el-form-item>
            <el-form-item>
              <div class="text-primary font-bold">预计：{{ paperStore.questionCount }} 道题，总分 {{ paperStore.totalScore }} 分</div>
            </el-form-item>
          </el-form>
          <!-- 及格线设置 -->
          <el-divider />
          <el-form-item label="及格线(分)">
            <el-input-number 
              v-model="paperStore.currentPaper.passScore" 
              :min="0" 
              :max="paperStore.totalScore" 
              :precision="1" 
            />
            <span class="text-gray text-sm ml-2">
              建议：{{ (paperStore.totalScore * 0.6).toFixed(1) }} 分（总分的60%）
            </span>
          </el-form-item>
        </template>
      </div>

      <template #footer>
  <el-button v-if="activeStep > 0" @click="activeStep--">上一步</el-button>
  <el-button v-if="activeStep === 0" type="primary" @click="nextStep">下一步</el-button>
  <el-button v-else type="primary" @click="handleSave" :loading="submitLoading">保存试卷</el-button>
</template>
    </el-dialog>

    <!-- 选题弹窗 -->
    <el-dialog v-model="questionSelectorVisible" title="从题库选题" width="800px" :close-on-click-modal="false">
      <el-form :model="selectorForm" inline>
        <el-form-item label="题型">
          <el-select v-model="selectorForm.type" placeholder="全部" clearable class="w-28">
            <el-option label="单选" value="single" /><el-option label="多选" value="multiple" />
            <el-option label="判断" value="judge" /><el-option label="填空" value="fill" />
            <el-option label="简答" value="short" /><el-option label="编程" value="program" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-input v-model="selectorForm.knowledge" placeholder="关键词" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="small" @click="getSelectorList">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="selectorList" stripe border size="small" @selection-change="handleSelectorSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column prop="type" label="题型" width="80" align="center">
          <template #default="{ row }"><el-tag size="small">{{ typeLabel(row.type) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="content" label="题目内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="difficulty" label="难度" width="70" align="center">
          <template #default="{ row }"><el-tag size="small" :type="difficultyColor(row.difficulty)">{{ diffMap[row.difficulty] }}</el-tag></template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="questionSelectorVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSelectQuestions">确定添加</el-button>
      </template>
    </el-dialog>

    <!-- 预览弹窗 -->
    <el-dialog v-model="previewVisible" title="试卷预览" width="800px" :close-on-click-modal="false">
      <div class="preview-content">
        <h3 class="text-center text-xl font-bold mb-4">{{ previewPaper.name }}</h3>
        <div class="text-center text-gray mb-6">满分 {{ previewPaper.totalScore }} 分 | 时长 {{ previewPaper.duration }} 分钟 | 及格线 {{ previewPaper.passScore }} 分</div>
        <div v-for="(q, idx) in previewPaper.questions" :key="idx" class="preview-question mb-4">
          <div class="font-bold mb-2">
            第{{ idx + 1 }}题 【{{ typeFullLabel(q.type) }}】{{ q.content }}（{{ q.score }}分）
          </div>
          <div v-if="q.options && q.options.length" class="pl-4 mb-2">
            <div class="text-gray text-sm">选项：</div>
            <div v-for="(opt, oidx) in q.options" :key="oidx">
              {{ String.fromCharCode(65+oidx) }}. {{ opt }}
            </div>
          </div>
          <div class="pl-4 preview-meta">
            正确答案：<span class="text-success font-bold">{{ formatPreviewAnswer(q) || '—' }}</span>
          </div>
          <div v-if="formatPreviewTags(q)" class="pl-4 preview-meta">
            知识点：{{ formatPreviewTags(q) }}
          </div>
          <div v-if="q.analysis" class="pl-4 preview-meta">
            解析：{{ q.analysis }}
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, View } from '@element-plus/icons-vue'
import { useExamPaperStore } from '@/stores/examPaper'
import request from '@/api/request.js'

const paperStore = useExamPaperStore()

const searchForm = reactive({ name: '', subject: '', mode: '' })
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })
const paperList = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('新建试卷')
const activeStep = ref(0)
const step1Ref = ref(null)

const submitLoading = ref(false)
const isEdit = ref(false)

const step1Rules = {
  name: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }],
  subject: [{ required: true, message: '请输入科目', trigger: 'blur' }]
}


const typeMap = { single: '单选', multiple: '多选', judge: '判断', fill: '填空', short: '简答', program: '编程' }
const typeLabel = (t) => typeMap[t] || t
// 预览用完整题型名（带“题”）
const typeFullMap = { single: '单选题', multiple: '多选题', judge: '判断题', fill: '填空题', short: '简答题', program: '编程题' }
const typeFullLabel = (t) => typeFullMap[t] || t

// 预览：格式化正确答案。选择题把答案内容映射成选项字母(A/B/C…)，其余题型原样显示
const formatPreviewAnswer = (q) => {
  let ans = q.answer
  if (ans === null || ans === undefined || ans === '') return ''
  if (typeof ans === 'string') {
    const s = ans.trim()
    if (s.startsWith('[')) {
      try { ans = JSON.parse(s) } catch (e) { /* 保持原样 */ }
    }
  }
  const arr = Array.isArray(ans) ? ans : [ans]
  const isChoice = (q.type === 'single' || q.type === 'multiple') && Array.isArray(q.options) && q.options.length > 0
  return arr
    .map(a => {
      const val = String(a).trim()
      if (isChoice) {
        const idx = q.options.findIndex(o => String(o).trim() === val)
        if (idx >= 0) return String.fromCharCode(65 + idx)
      }
      return val
    })
    .filter(v => v !== '')
    .join(', ')
}

// 预览：格式化知识点标签（categoryTags，多个逗号分隔）
const formatPreviewTags = (q) => {
  let tags = q.categoryTags
  if (typeof tags === 'string') {
    try { tags = JSON.parse(tags) } catch (e) { tags = tags ? [tags] : [] }
  }
  return Array.isArray(tags) ? tags.filter(Boolean).join(', ') : ''
}
const diffMap = { easy: '易', medium: '中', hard: '难' }
const difficultyColor = (d) => ({ easy: 'success', medium: 'warning', hard: 'danger' }[d] || '')
const randomLabelMap = {
  singleChoice: '单选题', multiChoice: '多选题', judge: '判断题',
  fillBlank: '填空题', shortAnswer: '简答题', programming: '编程题'
}
const randomLabel = (key) => randomLabelMap[key] || key

// 题库筛选用映射
const backendTypeMap = { single: 1, multiple: 2, judge: 3, fill: 4, short: 5, program: 6 }
const frontendTypeMap = { 1: 'single', 2: 'multiple', 3: 'judge', 4: 'fill', 5: 'short', 6: 'program' }
const difficultyBackendMap = { easy: 1, medium: 3, hard: 5 }
const difficultyFrontendMap = { 1: 'easy', 2: 'easy', 3: 'medium', 4: 'medium', 5: 'hard' }

// 选题器
const questionSelectorVisible = ref(false)
const selectorForm = reactive({ type: '', knowledge: '' })
const selectorList = ref([])
const selectorSelection = ref([])

// 预览
const previewVisible = ref(false)
const previewPaper = ref({ name: '', totalScore: 0, duration: 0, passScore: 0, questions: [] })

// 科目列表
const subjectList = ref([])

// 题型可用数量
const questionStats = ref({})
const subjectStatsLoaded = ref(false)

const statLabelMap = {
  single: '单选', multiple: '多选', judge: '判断',
  fill: '填空', short: '简答', program: '编程'
}
const statLabel = (key) => statLabelMap[key] || key
const newKnowledgeName = ref('')
const knowledgeTags = ref([])

// 添加知识点比例项
const addKnowledgeItem = () => {
  if (newKnowledgeName.value && !paperStore.randomConfig.knowledgeRatio[newKnowledgeName.value]) {
    paperStore.randomConfig.knowledgeRatio[newKnowledgeName.value] = 0
    newKnowledgeName.value = ''
  }
}

// 加载知识点标签
const loadKnowledgeTags = (subject) => {
  // 从题库中获取该科目的所有知识点标签
  request.get('/question/list', { params: { subject, page: 1, size: 1000 } }).then(res => {
    const records = res.records || []
    const tags = new Set()
    records.forEach(q => {
      try {
        const tagList = JSON.parse(q.categoryTags || '[]')
        tagList.forEach(t => tags.add(t))
      } catch (e) {}
    })
    knowledgeTags.value = [...tags]
  })
}
// 获取题型可用数量
const loadQuestionStats = async (subject) => {
  try {
    const params = {}
    if (subject) params.subject = subject
    const res = await request.get('/paper/question-stats', { params })
    questionStats.value = res || {}
    subjectStatsLoaded.value = true
  } catch (e) {
    console.error('获取题型统计失败', e)
  }
}

// 监听科目变化（步骤1→步骤2时触发）
const nextStep = () => {
  if (activeStep.value === 0) {
    step1Ref.value?.validate((valid) => { 
      if (valid) {
        loadQuestionStats(paperStore.currentPaper.subject || '')
        loadKnowledgeTags(paperStore.currentPaper.subject || '')  // 【新增】
        activeStep.value++ 
      }
    })
  }
}
const getList = async () => {
  loading.value = true
  try {
    const res = await request.get('/paper/list', {
      params: {
        page: pagination.page,
        size: pagination.pageSize,
        subject: searchForm.subject || null,
        status: null
      }
    })
    const records = res.records || []
    paperList.value = records.map(p => ({
      id: p.id,
      name: p.paperName,
      subject: p.subject || '',
      mode: p.mode || 'manual',
      totalScore: p.totalScore,
      questionCount: p.questionCount || 0,
      duration: p.duration,
      passScore: p.passScore,
      createdAt: p.createTime ? p.createTime.slice(0, 10) : ''
    }))
    pagination.total = res.total || 0
  } catch (e) {
    console.error('获取试卷列表失败', e)
    ElMessage.error('获取试卷列表失败')
  } finally {
    loading.value = false
  }
}
const handleSearch = () => { pagination.page = 1; getList() }
const handleReset = () => { Object.keys(searchForm).forEach(k => searchForm[k] = ''); handleSearch() }

const handleCreate = () => {
  isEdit.value = false
  dialogTitle.value = '新建试卷'
  paperStore.resetPaper()
  activeStep.value = 0
  dialogVisible.value = true
}
const handleEdit = async (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑试卷'
  
  try {
    const res = await request.get(`/paper/detail/${row.id}`)
    const paper = res.paper || {}
    const questions = res.questions || []
    
    paperStore.setPaperInfo({
      id: paper.id,  // 确保设置ID
      name: paper.paperName || row.name,
      subject: paper.subject || '',
      mode: paper.mode || 'manual',
      totalScore: paper.totalScore,
      duration: paper.duration,
      passScore: paper.passScore
    })
    
    if (paper.mode === 'manual') {
      // 手动组卷：加载已有题目
      paperStore.clearQuestions()
      questions.forEach((q, idx) => {
        paperStore.addQuestion({
          id: q.questionId,
          type: frontendTypeMap[q.questionType] || 'short',
          content: q.content || '',
          score: q.score,
          difficulty: difficultyFrontendMap[q.difficulty] || 'medium'
        })
      })
    } else {
      // 随机组卷：加载题型数量统计
      loadQuestionStats(paper.subject || '')
    }
    
    activeStep.value = 0
    dialogVisible.value = true
  } catch (e) {
    console.error('获取试卷详情失败', e)
    ElMessage.error('获取试卷详情失败')
  }
}
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除试卷 "${row.name}" 吗？`, '提示', { type: 'warning' }).then(async () => {
    try {
      await request.delete(`/paper/${row.id}`)
      ElMessage.success('删除成功')
      getList()
    } catch (e) {
      console.error('删除失败', e)
    }
  }).catch(() => {})
}
const handleSave = async () => {
  if (paperStore.currentPaper.mode === 'manual' && paperStore.selectedQuestions.length === 0) {
    ElMessage.warning('请至少选择一道题目')
    return
  }
  
  submitLoading.value = true
  
const basePayload = {
    paperName: paperStore.currentPaper.name,
    subject: paperStore.currentPaper.subject,
    mode: paperStore.currentPaper.mode,
    duration: paperStore.currentPaper.duration,   
    passScore: paperStore.currentPaper.passScore    
}

  
  let payload = {}
  
  if (paperStore.currentPaper.mode === 'manual') {
    payload = {
      ...basePayload,
      questions: paperStore.selectedQuestions.map((q, idx) => ({
        questionId: q.id,
        questionOrder: idx + 1,
        score: q.score || 2
      }))
    }
  } else {
    const rc = paperStore.randomConfig
    payload = {
      ...basePayload,
      randomConfig: {
        singleCount: rc.singleChoice?.count || 0,
        multipleCount: rc.multiChoice?.count || 0,
        judgeCount: rc.judge?.count || 0,
        fillCount: rc.fillBlank?.count || 0,
        shortCount: rc.shortAnswer?.count || 0,
        programCount: rc.programming?.count || 0,
        singleScore: rc.singleChoice?.score || 2,
        multipleScore: rc.multiChoice?.score || 3,
        judgeScore: rc.judge?.score || 2,
        fillScore: rc.fillBlank?.score || 2,
        shortScore: rc.shortAnswer?.score || 5,
        programScore: rc.programming?.score || 10,
        difficultyRatio: rc.difficultyRatio || { easy: 30, medium: 50, hard: 20 },
        knowledgeRatio: rc.knowledgeRatio || {}  // 传递知识点比例
      }
    }
  }
  
try {
    if (isEdit.value) {
      payload.paperId = paperStore.currentPaper.id  // 确保传入paperId
      await request.put('/paper/update', payload)
      ElMessage.success('更新成功')
    } else {
      await request.post('/paper/create', payload)
      ElMessage.success('新建成功')
    }
    dialogVisible.value = false
    getList()
  } catch (e) {
    console.error('保存试卷失败', e)
    // 后端返回的错误信息（如题目不足）已在拦截器中弹出
    // 不需要额外处理，也不关闭弹窗，让教师继续调整
  } finally {
    submitLoading.value = false
  }
}


// 禁用开始日期：当前日期之前的日期不可选
const disabledStartDate = (time) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return time.getTime() < today.getTime()
}

// 禁用结束日期：当前日期之前的日期不可选，且早于开始时间的日期置灰
const disabledEndDate = (time) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  if (time.getTime() < today.getTime()) {
    return true
  }
  if (paperStore.currentPaper.startTime) {
    const start = new Date(paperStore.currentPaper.startTime)
    return time.getTime() < start.getTime()
  }
  return false
}

// 选题器 - 从题库API获取
const getSelectorList = async () => {
  try {
    const res = await request.get('/question/list', {
      params: {
        page: 1,
        size: 50,
        questionType: selectorForm.type ? backendTypeMap[selectorForm.type] : null,
        subject: selectorForm.subject || null
      }
    })
    const records = res.records || []
    selectorList.value = records.map(q => ({
      id: q.id,
      type: frontendTypeMap[q.questionType] || 'single',
      content: q.content,
      options: q.options ? JSON.parse(q.options) : [],
      difficulty: difficultyFrontendMap[q.difficulty] || 'medium',
      score: q.score || 2
    }))
  } catch (e) {
    console.error('获取题库列表失败', e)
  }
}
const handleSelectorSelectionChange = (sel) => { selectorSelection.value = sel }
const confirmSelectQuestions = () => {
  selectorSelection.value.forEach(q => paperStore.addQuestion(q))
  questionSelectorVisible.value = false
  ElMessage.success(`已添加 ${selectorSelection.value.length} 道题目`)
}

// 打开选题弹窗时，自动设置科目过滤
const openQuestionSelector = () => {
  // 设置当前试卷的科目到选题表单
  selectorForm.subject = paperStore.currentPaper.subject
  // 立即执行查询
  getSelectorList()
  questionSelectorVisible.value = true
}

// 预览
const handlePreview = async (row) => {
  try {
    const res = await request.get(`/paper/detail/${row.id}`)
    const paper = res.paper || {}
    const questions = res.questions || []
    
    previewPaper.value = {
      name: paper.paperName || row.name,
      totalScore: paper.totalScore || 0,
      duration: paper.duration || 120,
      passScore: paper.passScore || 0,
      questions: questions.map(q => {
        // 安全获取题型
        const qType = q.questionType || 5
        return {
          type: frontendTypeMap[qType] || 'short',
          content: q.content || '题目内容加载失败',
          score: q.score || 0,
          options: Array.isArray(q.options) ? q.options : [],
          answer: q.answer,
          categoryTags: q.categoryTags,
          analysis: q.analysis || ''
        }
      })
    }
    previewVisible.value = true
  } catch (e) {
    console.error('获取试卷详情失败', e)
    ElMessage.error('获取试卷详情失败')
  }
}

const handleSizeChange = (val) => { pagination.pageSize = val; pagination.page = 1; getList() }
const handlePageChange = (val) => { pagination.page = val; getList() }

// 获取科目列表
const getSubjectList = async () => {
  try {
    const res = await request.get('/question/list', {
      params: {
        page: 1,
        size: 1000 // 获取所有题目
      }
    })
    const records = res.records || []
    // 从题目中提取所有唯一的科目
    const subjects = [...new Set(records.map(q => q.subject).filter(s => s))]
    subjectList.value = subjects
  } catch (e) {
    console.error('获取科目列表失败', e)
  }
}

onMounted(() => {
  getList()
  getSubjectList()
})
</script>

<style scoped>
.exam-paper-management { width: 100%; }
.search-card { margin-bottom: 16px; }
.table-card { margin-bottom: 16px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
.step-content { padding: 20px 10px; }
.mb-4 { margin-bottom: 16px; }
.text-gray { color: #909399; }
.text-sm { font-size: 12px; }
.text-primary { color: #409eff; }
.font-bold { font-weight: bold; }
.text-center { text-align: center; }
.text-xl { font-size: 18px; }
.ml-2 { margin-left: 8px; }
.pl-4 { padding-left: 16px; }
.preview-content { max-height: 500px; overflow-y: auto; padding: 10px; }
.preview-question { border-bottom: 1px dashed #dcdfe6; padding-bottom: 10px; }
.preview-meta { font-size: 14px; color: #606266; margin-top: 4px; line-height: 1.6; }
.text-success { color: #67c23a; }
.flex { display: flex; }
.gap-2 { gap: 8px; }
.gap-4 { gap: 16px; }
.flex-between { display: flex; justify-content: space-between; align-items: center; }
.w-28 { width: 112px; }
</style>
