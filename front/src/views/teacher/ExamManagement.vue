<template>
  <div class="exam-management">
    <!-- 考试列表 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="flex-between">
          <span>考试管理</span>
          <el-button type="primary" @click="handlePublish"><el-icon><Promotion /></el-icon>发布考试</el-button>
        </div>
      </template>

      <el-table :data="examList" v-loading="loading" stripe border :header-cell-style="{ background: '#f5f7fa' }">
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column prop="name" label="考试名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="paperName" label="使用试卷" min-width="150" show-overflow-tooltip />
        <el-table-column prop="startTime" label="开始时间" width="150" />
        <el-table-column prop="endTime" label="结束时间" width="150" />
        <el-table-column prop="duration" label="时长" width="80" align="center"><template #default="{ row }">{{ row.duration }}分钟</template></el-table-column>
        <el-table-column prop="participantCount" label="参考人数" width="90" align="center" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'ongoing'" type="success">进行中</el-tag>
            <el-tag v-else-if="row.status === 'upcoming'" type="warning">未开始</el-tag>
            <el-tag v-else type="info">已结束</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <!-- 进行中：显示监控 -->
            <el-button 
              v-if="row.status === 'ongoing'" 
              link type="primary" size="small" 
              @click="handleMonitor(row)">
              <el-icon><VideoPlay /></el-icon>监控
            </el-button>
            <!-- 所有状态：显示详情 -->
            <el-button link type="primary" size="small" @click="handleDetail(row)">
              <el-icon><View /></el-icon>详情
            </el-button>
            <!-- 非已结束：显示取消 -->
            <el-button 
              v-if="row.status !== 'ended'" 
              link type="danger" size="small" 
              @click="handleCancel(row)">
              <el-icon><CircleClose /></el-icon>取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 发布考试弹窗 -->
    <el-dialog v-model="publishVisible" title="发布考试" width="600px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="publishRef" :model="publishForm" :rules="publishRules" label-width="120px">
        <el-form-item label="考试名称" prop="name">
          <el-input v-model="publishForm.name" placeholder="请输入考试名称" />
        </el-form-item>
        <el-form-item label="选择试卷" prop="paperId">
          <el-select v-model="publishForm.paperId" placeholder="请选择试卷" class="w-full">
            <el-option v-for="p in paperOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="参考班级" prop="classIds">
          <el-select v-model="publishForm.classIds" multiple placeholder="请选择参考班级" class="w-full" @change="handleClassChange">
            <el-option v-for="c in classOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="指定个人（可选）" prop="studentIds">
          <el-select v-model="publishForm.studentIds" multiple filterable placeholder="不选则默认发布给所选班级全部学生" class="w-full" :loading="studentLoading">
            <el-option v-for="s in studentOptions" :key="s.id" :label="s.userNo ? `${s.name}（${s.userNo}）` : s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="publishForm.startTime" type="datetime" placeholder="选择开始时间" value-format="YYYY-MM-DD HH:mm:ss" :disabled-date="disabledStartDate" class="w-full" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-input :model-value="computedEndTime || '请先选择试卷与开始时间'" readonly class="w-full">
            <template #append>按试卷时长自动计算</template>
          </el-input>
        </el-form-item>
        <el-form-item label="快速交卷阈值">
          <el-input-number v-model="publishForm.fastSubmitThreshold" :min="5" :max="50" /> %
          <div class="text-gray text-sm">答题时间低于总时长该比例时标记为可疑答卷，默认20%</div>
        </el-form-item>
        <el-form-item label="题目乱序">
          <el-switch v-model="publishForm.shuffleQuestions" />
          <span class="text-gray text-sm" style="margin-left: 8px;">不同考生题目顺序随机打乱</span>
        </el-form-item>
        <el-form-item label="选项乱序">
          <el-switch v-model="publishForm.shuffleOptions" />
          <span class="text-gray text-sm" style="margin-left: 8px;">选择题选项顺序随机打乱</span>
        </el-form-item>
        <el-form-item label="迟到限制">
          <el-input-number v-model="publishForm.lateLimit" :min="0" :max="180" /> 分钟
          <div class="text-gray text-sm">开考超过该时间后禁止进入，0 表示不限制</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePublishSubmit" :loading="publishLoading">发布</el-button>
      </template>
    </el-dialog>

    <!-- 监控面板 -->
    <el-dialog v-model="monitorVisible" title="考试实时监控" width="900px" :close-on-click-modal="false" destroy-on-close @close="stopMonitor">
      <div class="monitor-header flex-between mb-4">
        <div>
          <span class="font-bold">{{ monitorStore.currentExam?.name }}</span>
          <el-tag type="success" class="ml-2">进行中</el-tag>
        </div>
        <div class="text-gray">参考人数：{{ monitorStore.studentStatusList.length }} | 已交卷：{{ submittedCount }} | 进行中：{{ ongoingCount }}</div>
      </div>

      <el-table :data="monitorStore.studentStatusList" stripe border size="small" :header-cell-style="{ background: '#f5f7fa' }">
        <el-table-column prop="studentNo" label="学号" width="110" />
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'not_started'" size="small">未开始</el-tag>
            <el-tag v-else-if="row.status === 'ongoing'" type="warning" size="small">进行中</el-tag>
            <el-tag v-else-if="row.status === 'submitted'" type="success" size="small">已交卷</el-tag>
            <el-tag v-else type="danger" size="small">异常</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="answeredCount" label="已答题" width="80" align="center">
          <template #default="{ row }">{{ row.answeredCount }}/{{ row.totalCount }}</template>
        </el-table-column>
        <el-table-column prop="remainingTime" label="剩余时间" width="100" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.remainingTime < 300 }">{{ formatTime(row.remainingTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="120" />
        <el-table-column label="异常标记" width="120" align="center">
          <template #default="{ row }">
            <template v-if="row.abnormal">
              <el-tag v-if="row.switchCount > 0" type="danger" size="small" style="margin-right: 4px;">切屏{{ row.switchCount }}次</el-tag>
              <el-tag v-if="row.hasFastSubmit" type="warning" size="small">快速交卷</el-tag>
            </template>
            <span v-else style="color: #67c23a;">无异常</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === 'ongoing'" link type="primary" size="small" @click="handleExtend(row)">延长时间</el-button>
            <el-button v-if="row.status === 'ongoing'" link type="danger" size="small" @click="handleForceSubmit(row)">强制交卷</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4">
        <el-progress :percentage="Math.round((submittedCount / Math.max(monitorStore.studentStatusList.length, 1)) * 100)" status="success">
          <span>交卷进度 {{ submittedCount }}/{{ monitorStore.studentStatusList.length }}</span>
        </el-progress>
      </div>
    </el-dialog>

    <!-- 延长时间弹窗 -->
    <el-dialog v-model="extendVisible" title="延长考试时长" width="400px" :close-on-click-modal="false">
      <p>考生：{{ extendTarget.name }}（{{ extendTarget.studentNo }}）</p>
      <p class="text-gray mb-4">当前剩余时间：{{ formatTime(extendTarget.remainingTime) }}</p>
      <el-form label-width="100px">
        <el-form-item label="延长分钟">
          <el-input-number v-model="extendMinutes" :min="1" :max="60" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="extendVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmExtend">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Promotion, VideoPlay, View, CircleClose } from '@element-plus/icons-vue'
import { useExamMonitorStore } from '@/stores/examMonitor'
import request from '@/api/request.js'

const monitorStore = useExamMonitorStore()

const examList = ref([])
const loading = ref(false)

// 发布考试
const publishVisible = ref(false)
const publishRef = ref(null)
const publishLoading = ref(false)
const publishForm = reactive({
  name: '',
  paperId: null,
  classIds: [],
  studentIds: [],
  startTime: '',
  endTime: '',
  fastSubmitThreshold: 20,
  shuffleQuestions: false,
  shuffleOptions: false,
  lateLimit: 0
})
const publishRules = {
  name: [{ required: true, message: '请输入考试名称', trigger: 'blur' }],
  paperId: [{ required: true, message: '请选择试卷', trigger: 'change' }],
  classIds: [{ type: 'array', required: true, message: '请至少选择一个班级', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }]
}
const paperOptions = ref([])
// 将硬编码的 classOptions 改为动态获取
const classOptions = ref([])
const studentOptions = ref([])
const studentLoading = ref(false)

// 选中试卷的时长（分钟）
const selectedPaperDuration = computed(() => {
  const p = paperOptions.value.find(p => p.id === publishForm.paperId)
  return p?.duration || 0
})

// 结束时间 = 开始时间 + 试卷时长（前端只读展示，后端最终以此规则为准）
const computedEndTime = computed(() => {
  if (!publishForm.startTime || !selectedPaperDuration.value) return ''
  const start = new Date(String(publishForm.startTime).replace(/-/g, '/'))
  if (isNaN(start.getTime())) return ''
  const end = new Date(start.getTime() + selectedPaperDuration.value * 60000)
  const pad = (n) => String(n).padStart(2, '0')
  return `${end.getFullYear()}-${pad(end.getMonth() + 1)}-${pad(end.getDate())} ${pad(end.getHours())}:${pad(end.getMinutes())}:${pad(end.getSeconds())}`
})

// 开始时间禁选今天之前的日期
const disabledStartDate = (time) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return time.getTime() < today.getTime()
}

// 监控
const monitorVisible = ref(false)
let pollTimer = null

const submittedCount = computed(() => monitorStore.studentStatusList.filter(s => s.status === 'submitted').length)
const ongoingCount = computed(() => monitorStore.studentStatusList.filter(s => s.status === 'ongoing').length)

// 延长
const extendVisible = ref(false)
const extendTarget = ref({})
const extendMinutes = ref(10)

const getList = async () => {
  loading.value = true
  try {
    const res = await request.get('/session/list', {
      params: { page: 1, size: 20 }
    })
    const records = res.records || []
    const now = new Date()
    
    // 收集所有paperId
    const paperIds = [...new Set(records.map(s => s.paperId).filter(Boolean))]
    const paperMap = {}
    
    // 批量获取试卷信息
    if (paperIds.length > 0) {
      try {
        const paperRes = await request.get('/paper/list', { 
          params: { page: 1, size: 100 } 
        })
        const papers = paperRes.records || []
        papers.forEach(p => {
          paperMap[p.id] = {
            name: p.paperName,
            duration: p.duration
          }
        })
      } catch (e) {
        console.error('获取试卷信息失败', e)
      }
    }
    
    examList.value = records.map(s => {
      let startTime = s.startTime
      let endTime = s.endTime
      if (startTime && startTime.includes('T')) {
        startTime = startTime.replace('T', ' ')
      }
      if (endTime && endTime.includes('T')) {
        endTime = endTime.replace('T', ' ')
      }
      
      const start = new Date(startTime)
      const end = new Date(endTime)
      
      let status = 'ended'
      if (s.publishStatus === 0) {
        status = 'unpublished'
      } else if (start > now) {
        status = 'upcoming'
      } else if (start <= now && end > now) {
        status = 'ongoing'
      } else {
        status = 'ended'
      }
      
      const paper = paperMap[s.paperId] || {}
      
      return {
        id: s.id,
        name: s.sessionName,
        paperName: paper.name || `试卷${s.paperId}`,
        paperId: s.paperId,
        duration: paper.duration || 0,
        startTime: startTime ? startTime.slice(0, 16) : '',
        endTime: endTime ? endTime.slice(0, 16) : '',
        participantCount: s.participantCount || 0,
        status: status
      }
    })
  } catch (e) {
    console.error('获取考试列表失败', e)
  } finally {
    loading.value = false
  }
}

const handlePublish = async () => {
  Object.assign(publishForm, {
    name: '',
    paperId: null,
    classIds: [],
    studentIds: [],
    startTime: '',
    endTime: '',
    fastSubmitThreshold: 20,
    shuffleQuestions: false,
    shuffleOptions: false,
    lateLimit: 0
  })
  studentOptions.value = []
  
  // 加载试卷列表
  try {
    const res = await request.get('/paper/list', { params: { page: 1, size: 100 } })
    const records = res.records || []
    paperOptions.value = records.map(p => ({ id: p.id, name: p.paperName, duration: p.duration }))
  } catch (e) {
    console.error('获取试卷列表失败', e)
  }
  
  publishVisible.value = true
}

// 班级变化时加载所有选中班级的学生
const handleClassChange = async (classIds) => {
  publishForm.studentIds = []
  studentOptions.value = []

  if (!classIds || classIds.length === 0) return

  studentLoading.value = true
  try {
    const allStudents = []
    const seen = new Set()
    for (const cid of classIds) {
      const res = await request.get('/user/list-by-class', {
        params: { classId: cid }
      })
      const students = Array.isArray(res) ? res : []
      const className = classOptions.value.find(c => c.id === cid)?.name || ''
      for (const s of students) {
        if (!seen.has(s.id)) {
          seen.add(s.id)
          allStudents.push({ ...s, className })
        }
      }
    }
    studentOptions.value = allStudents.map(u => ({ id: u.id, name: u.name, userNo: u.userNo, className: u.className }))
  } catch (e) {
    console.error('获取班级学生列表失败', e)
    ElMessage.error('获取班级学生列表失败')
  } finally {
    studentLoading.value = false
  }
}

const handlePublishSubmit = () => {
  publishRef.value?.validate(async (valid) => {
    if (!valid) return

    const startTime = publishForm.startTime
    if (!startTime) {
      ElMessage.error('请选择开始时间')
      return
    }

    // 开始时间不能早于当前时间（留 1 分钟容差）
    const startTimestamp = new Date(String(startTime).replace(/-/g, '/')).getTime()
    if (startTimestamp < Date.now() - 60 * 1000) {
      ElMessage.error('开始时间不能早于当前时间')
      return
    }

    // 结束时间由后端按 开始时间 + 试卷时长 计算，前端同步传递计算值
    const params = {
      sessionName: publishForm.name,
      paperId: publishForm.paperId,
      startTime: startTime,
      endTime: computedEndTime.value,
      targetType: 1,
      targetIds: publishForm.classIds,
      fastSubmitThreshold: publishForm.fastSubmitThreshold,
      shuffleQuestions: publishForm.shuffleQuestions,
      shuffleOptions: publishForm.shuffleOptions,
      lateLimit: publishForm.lateLimit
    }
    
    // 如果指定了个人学生，则添加到参数中
    if (publishForm.studentIds && publishForm.studentIds.length > 0) {
      params.specificStudentIds = publishForm.studentIds
    }
    
    publishLoading.value = true
    try {
      await request.post('/session/publish', params)
      ElMessage.success('考试发布成功')
      publishVisible.value = false
      getList()
    } catch (e) {
      console.error('发布考试失败', e)
      ElMessage.error(e.response?.data?.message || '发布考试失败')
    } finally {
      publishLoading.value = false
    }
  })
}

const handleMonitor = async (row) => {
  monitorStore.setCurrentExam(row)
  // 从后端获取考生状态
  await loadMonitorData(row.id)
  monitorVisible.value = true
  monitorStore.startPolling()
  startPolling()
}
// 加载监控数据
const loadMonitorData = async (sessionId) => {
  try {
    const res = await request.get(`/session/monitor/${sessionId}`)
    const students = res || []
    monitorStore.updateStudentStatus(students)
  } catch (e) {
    console.error('获取监控数据失败', e)
    ElMessage.error('获取监控数据失败')
  }
}

const startPolling = () => {
  if (pollTimer) clearInterval(pollTimer)
  pollTimer = setInterval(async () => {
    if (monitorStore.currentExam) {
      await loadMonitorData(monitorStore.currentExam.id)
    }
  }, 5000) // 每5秒刷新一次
}
const stopMonitor = () => {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
  monitorStore.stopPolling()
}

const handleDetail = async (row) => {
  try {
    // 获取场次基本信息
    const sessionRes = await request.get(`/session/detail/${row.id}`)
    const session = sessionRes
    
    if (!session) {
      ElMessage.error('获取考试信息失败')
      return
    }
    
    // 获取参与对象列表
    let targetText = '未关联任何对象'
    try {
      const targetsRes = await request.get(`/session/targets/${row.id}`)
      const targets = targetsRes || []
      const classTargets = targets.filter(t => t.targetType === 1)
      const studentTargets = targets.filter(t => t.targetType === 2)

      const parts = []
      if (classTargets.length > 0) {
        const names = classTargets.map(t => t.targetName).filter(Boolean)
        parts.push(`${classTargets.length}个班级（${names.join('、') || '未知'}）`)
      }
      if (studentTargets.length > 0) {
        const names = studentTargets.map(t => t.targetName).filter(Boolean)
        parts.push(`${studentTargets.length}个学生（${names.join('、') || '未知'}）`)
      }
      if (parts.length > 0) {
        targetText = parts.join(' + ')
      }
    } catch (e) {
      console.error('获取参与对象失败', e)
    }
    
    // 格式化时间
    const formatTime = (time) => {
      if (!time) return '-'
      return time.replace('T', ' ').substring(0, 16)
    }
    
    ElMessageBox.alert(`
      <div style="margin-bottom: 10px;"><strong>考试名称：</strong>${session.sessionName || row.name}</div>
      <div style="margin-bottom: 10px;"><strong>开始时间：</strong>${formatTime(session.startTime)}</div>
      <div style="margin-bottom: 10px;"><strong>结束时间：</strong>${formatTime(session.endTime)}</div>
      <div style="margin-bottom: 10px;"><strong>参考人数：</strong>${session.participantCount || row.participantCount || 0}人</div>
      <div style="margin-bottom: 10px;"><strong>发布对象：</strong>${targetText}</div>
    `, '考试详情', {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '关闭'
    })
  } catch (e) {
    console.error('获取考试详情失败', e)
    ElMessage.error('获取考试详情失败')
  }
}

const handleCancel = (row) => {
  ElMessageBox.confirm(`确定取消考试 "${row.name}" 吗？`, '提示', { type: 'warning' }).then(async () => {
    try {
      await request.delete(`/session/${row.id}`)
      ElMessage.success('考试已取消')
      getList()
    } catch (e) {
      console.error('取消考试失败', e)
      ElMessage.error('取消考试失败')
    }
  }).catch(() => {})
}

// 延长时间
const handleExtend = (row) => {
  console.log('延长时间 - 当前行数据:', row)  // 调试用
  extendTarget.value = {
    recordId: row.recordId,
    studentId: row.studentId,
    name: row.name,
    studentNo: row.studentNo,
    remainingTime: row.remainingTime
  }
  extendMinutes.value = 10
  extendVisible.value = true
}

// 强制交卷
const handleForceSubmit = (row) => {
  console.log('强制交卷 - 当前行数据:', row)  // 调试用
  ElMessageBox.confirm(`确定强制 ${row.name}（${row.studentNo}）交卷吗？`, '提示', { 
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await request.post(`/session/force-submit/${row.recordId}`)
      ElMessage.success('已强制交卷')
      await loadMonitorData(monitorStore.currentExam.id)
    } catch (e) {
      console.error('强制交卷失败', e)
      ElMessage.error('操作失败')
    }
  }).catch(() => {})
}

// 确认延长
const confirmExtend = async () => {
  if (!extendTarget.value.recordId) {
    ElMessage.error('无效的考试记录')
    return
  }
  try {
    const res = await request.put(`/session/extend/${extendTarget.value.recordId}`, {
      extraMinutes: extendMinutes.value
    })
    ElMessage.success(`已为 ${extendTarget.value.name} 延长 ${extendMinutes.value} 分钟`)
    extendVisible.value = false
    // 重新加载监控数据
    await loadMonitorData(monitorStore.currentExam.id)
  } catch (e) {
    console.error('延长时间失败', e)
    ElMessage.error('操作失败')
  }
}
const formatTime = (seconds) => {
  if (seconds === undefined || seconds === null || seconds <= 0) return '00:00'
  // 如果 seconds 是小数，取整
  seconds = Math.floor(seconds)
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}
// 新增：加载班级列表
const loadClassOptions = async () => {
  try {
    const res = await request.get('/admin/class/list', { 
      params: { page: 1, pageSize: 100 } 
    })
    const records = res.records || res.list || []
    classOptions.value = records.map(c => ({
      id: c.id,
      name: c.className
    }))
  } catch (e) {
    console.error('获取班级列表失败', e)
    // 兜底数据
    classOptions.value = [
      { id: 1, name: '软件2101班' },
      { id: 2, name: '软件2102班' }
    ]
  }
}
// 在 onMounted 中调用
onMounted(async () => {
  await loadClassOptions()
  await getList()
})
onUnmounted(() => stopMonitor())
</script>

<style scoped>
.exam-management { width: 100%; }
.table-card { margin-bottom: 16px; }
.monitor-header { padding: 0 4px; }
.text-danger { color: #f56c6c; }
.text-gray { color: #909399; }
.font-bold { font-weight: bold; }
.ml-2 { margin-left: 8px; }
.mb-4 { margin-bottom: 16px; }
.mt-4 { margin-top: 16px; }
.flex-between { display: flex; justify-content: space-between; align-items: center; }
</style>