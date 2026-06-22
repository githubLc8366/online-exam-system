<template>
  <div class="exam-page" v-if="sessionStore.examInfo">
    <!-- 顶部栏 -->
    <header class="exam-header flex-between">
      <div class="flex items-center gap-4">
        <span class="font-bold text-lg">{{ sessionStore.examInfo.name }}</span>
        <el-tag v-if="!sessionStore.isOnline" type="danger" size="small">网络已断开</el-tag>
        <el-tag v-if="sessionStore.switchCount > 0" type="warning" size="small">切屏 {{ sessionStore.switchCount }} 次</el-tag>
        <!-- 保存状态 -->
        <el-tag
          v-if="saveStatus === 'saving'"
          type="warning"
          size="small"
          class="save-status"
        >
          <el-icon class="is-loading"><Loading /></el-icon> 正在保存...
        </el-tag>
        <el-tag
          v-else-if="saveStatus === 'saved' && lastSaveTime"
          type="success"
          size="small"
          class="save-status"
        >
          <el-icon><CircleCheck /></el-icon> 已保存 {{ lastSaveTime }}
        </el-tag>
        <el-tag
          v-else-if="saveStatus === 'error'"
          type="danger"
          size="small"
          class="save-status"
        >
          <el-icon><CircleClose /></el-icon> 保存失败
        </el-tag>
      </div>
      <div class="flex items-center gap-4">
        <div class="countdown" :class="{ 'text-danger': sessionStore.remainingTime < 300 }">
          <el-icon><Timer /></el-icon>
          剩余时间：{{ formatTime(sessionStore.remainingTime) }}
        </div>
        <el-button type="primary" size="small" @click="handleSubmit">
          <el-icon><Check /></el-icon> 交卷
        </el-button>
      </div>
    </header>

    <!-- 主体区域 -->
    <div class="exam-body">
      <!-- 左侧导航 -->
      <aside class="exam-nav">
        <div class="nav-title">答题卡</div>
        <div class="nav-stats mb-2">
          <span class="nav-dot answered"></span> 已答 {{ sessionStore.answeredCount }}
          <span class="nav-dot unanswered ml-2"></span> 未答 {{ sessionStore.unansweredCount }}
        </div>
        <div class="nav-grid">
          <div
            v-for="(q, idx) in sessionStore.questions"
            :key="q.id"
            class="nav-item"
            :class="{ current: idx === sessionStore.currentIndex, answered: !!sessionStore.answers[q.id] }"
            @click="goToQuestion(idx)"
          >
            {{ idx + 1 }}
          </div>
        </div>
        <el-divider />
        <div class="nav-info text-sm text-gray">
          <div>已答：{{ sessionStore.answeredCount }}/{{ sessionStore.questions.length }}</div>
          <div class="mt-1">剩余：{{ formatTime(sessionStore.remainingTime) }}</div>
        </div>
      </aside>

      <!-- 右侧题目 -->
      <main class="exam-content">
        <div v-if="currentQuestion" class="question-card">
          <div class="question-header mb-4">
            <span class="font-bold">第 {{ sessionStore.currentIndex + 1 }} 题</span>
            <el-tag size="small" class="ml-2">{{ typeLabel(currentQuestion.type) }}</el-tag>
            <span class="text-gray text-sm ml-2">（{{ currentQuestion.score }}分）</span>
          </div>
          <div class="question-content mb-6">{{ currentQuestion.content }}</div>

          <!-- 单选 -->
          <el-radio-group v-if="currentQuestion.type === 'single'" v-model="currentAnswer">
              <el-radio v-for="(opt, idx) in currentQuestion.options" :key="idx" :label="opt">
                  {{ String.fromCharCode(65+idx) }}. {{ opt }}
              </el-radio>
          </el-radio-group>

          <!-- 多选 -->
          <el-checkbox-group v-else-if="currentQuestion.type === 'multiple'" v-model="currentAnswerMultiple">
              <el-checkbox v-for="(opt, idx) in currentQuestion.options" :key="idx" :label="opt">
                  {{ String.fromCharCode(65+idx) }}. {{ opt }}
              </el-checkbox>
          </el-checkbox-group>

          <!-- 判断 -->
          <el-radio-group v-else-if="currentQuestion.type === 'judge'" v-model="currentAnswer" class="question-options">
            <el-radio label="true">正确</el-radio>
            <el-radio label="false">错误</el-radio>
          </el-radio-group>

          <!-- 填空/简答/编程（带防抖） -->
          <el-input
            v-else
            v-model="currentAnswerText"
            type="textarea"
            :rows="currentQuestion.type === 'program' ? 8 : 4"
            :placeholder="currentQuestion.type === 'fill' ? '请输入答案' : currentQuestion.type === 'program' ? '请编写代码' : '请输入答案'"
          />
        </div>

        <!-- 底部操作 -->
        <div class="exam-actions flex-between mt-6">
          <el-button :disabled="sessionStore.currentIndex === 0" @click="prevQuestion">
            <el-icon><ArrowLeft /></el-icon> 上一题
          </el-button>
          <el-button type="primary" @click="nextQuestion">
            {{ sessionStore.currentIndex === sessionStore.questions.length - 1 ? '最后一题' : '下一题' }}
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </main>
    </div>

    <!-- 全屏提示 -->
    <el-dialog v-model="fullscreenTipVisible" title="考试提示" width="400px" :close-on-click-modal="false" :show-close="false">
      <p>考试即将开始，建议进入全屏模式以获得最佳考试体验并增强防作弊效果。</p>
      <template #footer>
        <el-button @click="fullscreenTipVisible = false">取消</el-button>
        <el-button type="primary" @click="enterFullscreen">进入全屏</el-button>
      </template>
    </el-dialog>

    <!-- 切屏警告 -->
    <el-dialog v-model="warnVisible" title="警告" width="400px" :close-on-click-modal="false" :show-close="false">
      <p class="text-danger font-bold">检测到您已切屏 {{ sessionStore.switchCount }} 次！</p>
      <p>考试期间频繁切屏将被视为作弊行为。超过 {{ maxSwitch }} 次将自动交卷。</p>
      <template #footer>
        <el-button type="primary" @click="warnVisible = false">我知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { Timer, Check, ArrowLeft, ArrowRight, Loading, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { useExamSessionStore } from '@/stores/examSession'
import request from '@/api/request.js'

const route = useRoute()
const router = useRouter()
const sessionStore = useExamSessionStore()

const maxSwitch = 3
const fullscreenTipVisible = ref(false)
const warnVisible = ref(false)

// 保存状态
const saveStatus = ref('saved') // 'saved' | 'saving' | 'error'
const lastSaveTime = ref('')

// 交卷防重入（手动/自动/超时只允许一次提交，并用于放行离开守卫）
const submitting = ref(false)

let countdownTimer = null
let autoSaveTimer = null
let debounceTimer = null

const currentQuestion = computed(() => sessionStore.questions[sessionStore.currentIndex] || null)
// 在 setup 中添加
let heartbeatTimer = null
let syncTimer = null
// 【新增】启动心跳 - 每30秒发送一次
const startHeartbeat = () => {
  heartbeatTimer = setInterval(async () => {
    try {
      const recordId = sessionStore.examInfo?.recordId || route.params.recordId
      if (recordId) {
        const res = await request.post(`/exam/heartbeat/${recordId}`)
        // 如果返回强制交卷的提示
        if (res && res.msg && res.msg.includes('自动交卷')) {
          handleAutoSubmit('你已被教师强制交卷')
        }
      }
    } catch (e) {
      // 如果返回403或其他错误，可能是被强制交卷了
      if (e.response?.data?.msg?.includes('交卷') || e.response?.status === 403) {
        handleAutoSubmit('你已被教师强制交卷')
      }
      console.error('心跳发送失败', e)
    }
  }, 30000) // 每 30 秒一次心跳，后端更新 last_heartbeat_time 供监控判断在线
}
// 【修改】定时同步考试时间（2秒检查一次延长/强制交卷）
const startSyncTimer = () => {
  syncTimer = setInterval(async () => {
    try {
      const sessionId = route.params.id
      const recordId = sessionStore.examInfo?.recordId
      
      if (!recordId) return
      
      // 【检测强制交卷】通过记录状态接口
      try {
        const statusRes = await request.get(`/exam/record-status/${recordId}`)
        if (statusRes && statusRes.status >= 2 && sessionStore.remainingTime > 0) {
          // 状态变为已交卷，是被强制交卷了
          submitting.value = true // 置位防重入并放行离开守卫，避免与路由守卫弹窗冲突
          clearInterval(countdownTimer)
          clearInterval(autoSaveTimer)
          clearTimeout(debounceTimer)
          clearInterval(heartbeatTimer)
          clearInterval(syncTimer)
          sessionStore.clearStorage()
          localStorage.removeItem('is_testing')
          
          const reason = statusRes.submitType === 3 ? '你已被教师强制交卷' : '考试已结束'
          ElMessageBox.alert(reason, '提示', {
            confirmButtonText: '确定',
            callback: () => {
              router.push('/student-portal/history')
            }
          })
          return
        }
      } catch (e) {
        // 忽略
      }
      
      // 【检测延长时间】
      const res = await request.get(`/session/detail/${sessionId}`)
      if (res && res.endTime) {
        let endTimeStr = res.endTime
        if (typeof endTimeStr === 'string') {
          endTimeStr = endTimeStr.replace('T', ' ').replace(/-/g, '/')
        }
        const endTime = new Date(endTimeStr)
        const now = new Date()
        const newRemaining = Math.floor((endTime - now) / 1000)
        
        if (sessionStore.remainingTime > 0 && 
            newRemaining > 0 && 
            newRemaining > sessionStore.remainingTime + 30) {
          sessionStore.setRemainingTime(newRemaining)
          ElNotification({
            title: '考试时间已延长',
            message: `剩余时间已更新`,
            type: 'success',
            duration: 5000
          })
        }
        // 【新增】正常同步剩余时间（小幅调整不弹通知）
        if (sessionStore.remainingTime > 0 && 
            newRemaining > 0 && 
            Math.abs(newRemaining - sessionStore.remainingTime) > 5) {
          sessionStore.setRemainingTime(newRemaining)
        }
      }
    } catch (e) {
      // 静默失败
    }
  }, 2000)
}
// 【新增】上报切屏事件
const reportSwitchEvent = async () => {
  try {
    const recordId = sessionStore.examInfo?.recordId || route.params.recordId
    if (recordId) {
      await request.post(`/exam/switch-report/${recordId}`)
      sessionStore.saveToStorage()
    }
  } catch (e) {
    console.error('切屏上报失败', e)
    // 如果是强制交卷，处理自动提交
    if (e.response?.data?.msg?.includes('自动交卷')) {
      handleAutoSubmit('切屏次数超过限制，系统自动交卷')
    }
  }
}
// 防抖函数
const debounce = (fn, delay) => {
  return (...args) => {
    clearTimeout(debounceTimer)
    debounceTimer = setTimeout(() => fn(...args), delay)
  }
}

// 保存答案到后端（自动保存，静默失败不打扰；本地已另存 localStorage 兜底）
const saveAnswersToServer = async () => {
  const recordId = sessionStore.examInfo?.recordId
  if (!recordId || submitting.value) { saveStatus.value = 'saved'; return }
  saveStatus.value = 'saving'
  try {
    const answers = JSON.parse(JSON.stringify(sessionStore.answers))
    await request.post(`/exam/save-progress/${recordId}`, { answers }, { silent: true })
    saveStatus.value = 'saved'
    const now = new Date()
    lastSaveTime.value = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
  } catch (error) {
    saveStatus.value = 'error'
    console.error('保存失败', error)
  }
}

// 保存到本地 + 后端
const saveAnswers = async () => {
  sessionStore.saveToStorage()
  await saveAnswersToServer()
}

// 防抖保存（用于简答题/编程题输入）
const debouncedSave = debounce(async () => {
  sessionStore.saveToStorage()
  await saveAnswersToServer()
}, 2000)

// 单选/判断答案
const currentAnswer = computed({
  get: () => sessionStore.answers[currentQuestion.value?.id] || '',
  set: (val) => {
    if (currentQuestion.value) {
      sessionStore.saveAnswer(currentQuestion.value.id, val)
      sessionStore.saveToStorage()
      saveAnswersToServer()
    }
  }
})

// 多选答案
const currentAnswerMultiple = computed({
  get: () => sessionStore.answers[currentQuestion.value?.id] || [],
  set: (val) => {
    if (currentQuestion.value) {
      sessionStore.saveAnswer(currentQuestion.value.id, val)
      sessionStore.saveToStorage()
      saveAnswersToServer()
    }
  }
})

// 文本答案（填空/简答/编程）- 带防抖
const currentAnswerText = computed({
  get: () => sessionStore.answers[currentQuestion.value?.id] || '',
  set: (val) => {
    if (currentQuestion.value) {
      sessionStore.saveAnswer(currentQuestion.value.id, val)
      sessionStore.saveToStorage()
      debouncedSave()
    }
  }
})

const typeMap = { single: '单选题', multiple: '多选题', judge: '判断题', fill: '填空题', short: '简答题', program: '编程题' }
const typeLabel = (t) => typeMap[t] || t

// 题型映射：后端 1=单选, 2=多选, 3=判断, 4=填空, 5=简答, 6=编程
const backendTypeToFrontend = (type) => {
  const map = { 1: 'single', 2: 'multiple', 3: 'judge', 4: 'fill', 5: 'short', 6: 'program' }
  return map[type] || 'short'
}

// 从后端获取题目并初始化考试
// 【修改】开始考试时获取 recordId
const initExam = async () => {
  try {
    const res = await request.get(`/exam/start/${route.params.id}`)
    const backendQuestions = res.questions || []

    const questions = backendQuestions.map(q => {
      let options = []
      try {
        if (q.options) options = JSON.parse(q.options)
      } catch (e) {}
      return {
        id: q.questionId,
        type: backendTypeToFrontend(q.questionType),
        content: q.content,
        options: options,
        score: q.score
      }
    })

    const durationMinutes = res.duration || 120

    // 【修改】保存 recordId
    sessionStore.setExamInfo({
      id: route.params.id,
      recordId: res.recordId,
      name: res.paperName || '考试',
      duration: durationMinutes,
      fastSubmitThreshold: res.fastSubmitThreshold || 20,
      startTime: Date.now()
    })
    sessionStore.setQuestions(questions)
    sessionStore.setRemainingTime(durationMinutes * 60)

    sessionStore.restoreFromStorage()

    const saved = localStorage.getItem('exam_session')
    if (!saved || JSON.parse(saved).examId !== route.params.id) {
        sessionStore.answers = {}
        sessionStore.remainingTime = durationMinutes * 60 
        sessionStore.switchCount = 0
    }

    localStorage.setItem('is_testing', 'true')
    fullscreenTipVisible.value = true
  } catch (error) {
    // 后端已通过拦截器弹出具体原因（如“考试尚未开始/已结束/已结束”），此处退出考试页
    console.error(error)
    localStorage.removeItem('is_testing')
    submitting.value = true // 放行离开守卫
    router.replace('/student-portal/dashboard')
  }
}

// 倒计时
const startCountdown = () => {
  countdownTimer = setInterval(() => {
    sessionStore.tick()
    sessionStore.saveToStorage()
    // 倒计时归零自动交卷（submitting 防止边界处重复触发）
    if (sessionStore.remainingTime <= 0 && !submitting.value) {
      handleAutoSubmit('考试时间已结束，系统自动交卷')
    }
  }, 1000)
}

// 定时自动保存
const startAutoSave = () => {
  autoSaveTimer = setInterval(() => {
    saveAnswers()
  }, 10000)  // 改为10秒
}

const goToQuestion = async (idx) => {
  await saveAnswers()
  sessionStore.setCurrentIndex(idx)
}

const prevQuestion = async () => {
  if (sessionStore.currentIndex > 0) {
    await saveAnswers()
    sessionStore.setCurrentIndex(sessionStore.currentIndex - 1)
  }
}

const nextQuestion = async () => {
  await saveAnswers()
  if (sessionStore.currentIndex < sessionStore.questions.length - 1) {
    sessionStore.setCurrentIndex(sessionStore.currentIndex + 1)
  }
}

const handleSubmit = () => {
  console.log('handleSubmit 被调用')
  const unanswer = sessionStore.unansweredCount
  let msg = '确定要交卷吗？'
  if (unanswer > 0) msg += `您还有 ${unanswer} 道题未作答。`
  ElMessageBox.confirm(msg, '交卷确认', { confirmButtonText: '确定交卷', cancelButtonText: '继续答题', type: 'warning' }).then(() => {
    // 答题时间异常检测：快速交卷二次确认
    const abnormal = checkAbnormalDuration()
    if (abnormal) {
      ElMessageBox.confirm(
        `您的答题时间仅 ${abnormal.actualMinutes} 分钟，不到考试总时长的 ${abnormal.thresholdPercent}%，可能被标记为可疑答卷。确定要交卷吗？`,
        '答题时间异常提醒',
        { confirmButtonText: '仍要交卷', cancelButtonText: '继续答题', type: 'error' }
      ).then(() => {
        doSubmit()
      }).catch(() => {})
    } else {
      doSubmit()
    }
  }).catch(() => {})
}

// 答题时间异常检测：返回异常详情或 null
const checkAbnormalDuration = () => {
  const durationMinutes = sessionStore.examInfo?.duration
  if (!durationMinutes) return null

  // 已用时长 = 总时长 - 剩余时间。
  // 不能用 (Date.now() - startTime)：startTime 在每次进入考试页时被重置且未持久化，
  // 刷新/重新挂载后会偏小；而 remainingTime 已持久化并与后端同步，反推更可靠。
  const elapsedSeconds = durationMinutes * 60 - sessionStore.remainingTime
  const actualMinutes = Math.floor(elapsedSeconds / 60)
  const thresholdPercent = sessionStore.examInfo?.fastSubmitThreshold || 20
  const minMinutes = Math.ceil(durationMinutes * thresholdPercent / 100)

  if (actualMinutes < minMinutes && actualMinutes >= 0) {
    return { actualMinutes, minMinutes, thresholdPercent }
  }
  return null
}

// 统一清理所有定时器
const clearAllTimers = () => {
  clearInterval(countdownTimer)
  clearInterval(autoSaveTimer)
  clearInterval(heartbeatTimer)
  clearInterval(syncTimer)
  clearTimeout(debounceTimer)
}

const doSubmit = async () => {
  // 防重入：交卷流程进行中直接返回
  if (submitting.value) return
  submitting.value = true

  // 先落地本地答案（saveAnswersToServer 在 submitting 期间会自动跳过，避免与交卷竞争）
  await saveAnswers()

  try {
    // 深拷贝答案，避免 Proxy 对象
    const answers = JSON.parse(JSON.stringify(sessionStore.answers))
    await request.post('/exam/submit', {
      sessionId: parseInt(route.params.id),   // 确保是数字
      answers: answers
    })
    // 提交成功后再清理定时器并跳转
    clearAllTimers()
    ElMessage.success('交卷成功')
    sessionStore.clearStorage()
    localStorage.removeItem('is_testing')
    router.push('/student-portal/history')
  } catch (error) {
    console.error('提交失败详情:', error)
    ElMessage.error(error.response?.data?.msg || '交卷失败，请稍后重试')
    submitting.value = false // 失败允许重试，定时器保持运行
  }
}

const handleAutoSubmit = async (reason) => {
  // 防重入：与手动交卷/超时/强制交卷共用 submitting 标志
  if (submitting.value) return
  submitting.value = true
  clearAllTimers()

  // 先保存答案再提交
  await saveAnswers()

  try {
    const answers = JSON.parse(JSON.stringify(sessionStore.answers))
    await request.post('/exam/submit', {
      sessionId: parseInt(route.params.id),
      answers: answers
    })
  } catch (error) {
    console.error('自动提交失败', error)
  }

  sessionStore.clearStorage()
  localStorage.removeItem('is_testing')
  ElMessageBox.alert(reason, '提示', {
    confirmButtonText: '确定',
    callback: () => {
      router.push('/student-portal/history')
    }
  })
}

// 全屏
const enterFullscreen = () => {
  const elem = document.documentElement
  if (elem.requestFullscreen) elem.requestFullscreen()
  else if (elem.webkitRequestFullscreen) elem.webkitRequestFullscreen()
  sessionStore.setFullscreen(true)
  fullscreenTipVisible.value = false
}

// 防作弊监听
const handleVisibilityChange = () => {
  if (document.hidden) {
    sessionStore.incrementSwitch()
    sessionStore.saveToStorage()
    reportSwitchEvent()  // 【新增】上报切屏
    
    if (sessionStore.switchCount >= maxSwitch) {
      handleAutoSubmit(`切屏次数超过 ${maxSwitch} 次，系统自动交卷`)
    } else if (sessionStore.switchCount >= 2 && !sessionStore.hasWarned) {
      sessionStore.setWarned(true)
      warnVisible.value = true
    }
  }
}


const handleBlur = () => {
  handleVisibilityChange()
}

// 网络监测
const handleOnline = () => {
  sessionStore.setOnline(true)
  ElNotification({ title: '网络已恢复', message: '网络连接已恢复，请继续答题', type: 'success' })
}
const handleOffline = () => {
  sessionStore.setOnline(false)
  ElNotification({ title: '网络已断开', message: '请检查网络连接，答案已本地保存', type: 'error', duration: 0 })
}

const formatTime = (seconds) => {
  if (seconds <= 0) return '00:00:00'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

// 刷新/关闭浏览器提示（交卷流程中放行）
const handleBeforeUnload = (e) => {
  if (submitting.value) return
  e.preventDefault()
  e.returnValue = '考试进行中，确定要离开吗？答案已自动保存。'
  return e.returnValue
}

// SPA 内部跳转拦截：非交卷流程的离开视为切屏并上报
onBeforeRouteLeave((to, from, next) => {
  if (submitting.value) {
    next()
    return
  }
  ElMessageBox.confirm(
    '离开考试页面将被记录为一次切屏行为，确定要离开吗？',
    '离开确认',
    { confirmButtonText: '确定离开', cancelButtonText: '继续考试', type: 'warning' }
  ).then(() => {
    reportSwitchEvent()
    next()
  }).catch(() => next(false))
})

// 在 onMounted 中启动心跳
onMounted(() => {
  initExam().then(() => {
    startCountdown()
    startAutoSave()
    startHeartbeat()
    startSyncTimer()  // 【新增】启动时间同步
  })
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('blur', handleBlur)
  window.addEventListener('online', handleOnline)
  window.addEventListener('offline', handleOffline)
  window.addEventListener('beforeunload', handleBeforeUnload)
})
// 在 onUnmounted 中清理
onUnmounted(() => {
  clearAllTimers()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('blur', handleBlur)
  window.removeEventListener('online', handleOnline)
  window.removeEventListener('offline', handleOffline)
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<style scoped>
.exam-page { display: flex; flex-direction: column; height: 100vh; background: #f5f7fa; }
.exam-header { height: 56px; background: #fff; padding: 0 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); display: flex; align-items: center; z-index: 10; }
.countdown { font-size: 16px; font-weight: bold; color: #409eff; }
.text-danger { color: #f56c6c; }
.exam-body { display: flex; flex: 1; overflow: hidden; }
.exam-nav { width: 220px; background: #fff; padding: 16px; border-right: 1px solid #e4e7ed; overflow-y: auto; }
.nav-title { font-size: 16px; font-weight: bold; margin-bottom: 12px; }
.nav-stats { font-size: 12px; color: #606266; margin-bottom: 8px; }
.nav-dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 4px; }
.nav-dot.answered { background: #67c23a; }
.nav-dot.unanswered { background: #dcdfe6; }
.nav-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; }
.nav-item { height: 36px; display: flex; align-items: center; justify-content: center; border-radius: 4px; background: #f5f7fa; cursor: pointer; font-size: 13px; border: 2px solid transparent; }
.nav-item.answered { background: #67c23a; color: #fff; }
.nav-item.current { border-color: #409eff; background: #ecf5ff; font-weight: bold; }
.exam-content { flex: 1; padding: 20px; overflow-y: auto; }
.question-card { background: #fff; padding: 24px; border-radius: 8px; min-height: 400px; }
.question-content { font-size: 16px; line-height: 1.8; }
.question-options { display: flex; flex-direction: column; gap: 12px; }
.exam-actions { padding: 0 10px; }
.save-status { transition: all 0.3s; }
.is-loading { animation: rotating 2s linear infinite; }
@keyframes rotating { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.flex-between { display: flex; justify-content: space-between; align-items: center; }
.flex { display: flex; }
.items-center { align-items: center; }
.gap-4 { gap: 16px; }
.ml-2 { margin-left: 8px; }
.mt-1 { margin-top: 4px; }
.mt-6 { margin-top: 24px; }
.mb-2 { margin-bottom: 8px; }
.mb-4 { margin-bottom: 16px; }
.mb-6 { margin-bottom: 24px; }
.font-bold { font-weight: bold; }
.text-lg { font-size: 18px; }
.text-sm { font-size: 13px; }
.text-gray { color: #909399; }
</style>
