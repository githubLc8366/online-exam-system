import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useExamSessionStore = defineStore('examSession', () => {
  // 当前考试信息
  const examInfo = ref(null)

  // 题目列表
  const questions = ref([])

  // 当前题号
  const currentIndex = ref(0)

  // 学生答案 { questionId: answer }
  const answers = ref({})

  // 剩余时间（秒）
  const remainingTime = ref(0)

  // 切屏次数
  const switchCount = ref(0)

  // 是否已警告
  const hasWarned = ref(false)

  // 是否已自动交卷
  const autoSubmitted = ref(false)

  // 是否全屏
  const isFullscreen = ref(false)

  // 网络状态
  const isOnline = ref(true)

  // 已答题数（多选空数组、空串均视为未答）
  const answeredCount = computed(() => {
    return Object.values(answers.value).filter(a => {
      if (a === '' || a === null || a === undefined) return false
      if (Array.isArray(a)) return a.length > 0
      return true
    }).length
  })

  // 未答题数
  const unansweredCount = computed(() => questions.value.length - answeredCount.value)

  // 设置考试信息
  const setExamInfo = (info) => { examInfo.value = info }

  // 设置题目
  const setQuestions = (list) => { questions.value = list }

  // 设置当前题号
  const setCurrentIndex = (idx) => { currentIndex.value = idx }

  // 保存答案
  const saveAnswer = (questionId, answer) => {
    answers.value[questionId] = answer
  }

  // 设置剩余时间
  const setRemainingTime = (time) => { remainingTime.value = time }

  // 减少剩余时间
  const tick = () => {
    if (remainingTime.value > 0) remainingTime.value--
  }

  // 增加切屏次数
  const incrementSwitch = () => { switchCount.value++ }

  // 设置警告状态
  const setWarned = (val) => { hasWarned.value = val }

  // 设置自动交卷
  const setAutoSubmitted = (val) => { autoSubmitted.value = val }

  // 设置全屏
  const setFullscreen = (val) => { isFullscreen.value = val }

  // 设置网络状态
  const setOnline = (val) => { isOnline.value = val }

  // 从 localStorage 恢复状态
  const restoreFromStorage = () => {
    try {
      const saved = localStorage.getItem('exam_session')
      if (saved) {
        const data = JSON.parse(saved)
        answers.value = data.answers || {}
        remainingTime.value = data.remainingTime || 0
        currentIndex.value = data.currentIndex || 0
        switchCount.value = data.switchCount || 0
      }
    } catch (e) { }
  }

  // 保存到 localStorage
  const saveToStorage = () => {
    try {
      localStorage.setItem('exam_session', JSON.stringify({
        answers: answers.value,
        remainingTime: remainingTime.value,
        currentIndex: currentIndex.value,
        switchCount: switchCount.value,
        examId: examInfo.value?.id
      }))
    } catch (e) { }
  }

  // 清除存储
  const clearStorage = () => {
    localStorage.removeItem('exam_session')
  }

  // 重置
  const reset = () => {
    examInfo.value = null
    questions.value = []
    currentIndex.value = 0
    answers.value = {}
    remainingTime.value = 0
    switchCount.value = 0
    hasWarned.value = false
    autoSubmitted.value = false
    isFullscreen.value = false
    isOnline.value = true
    clearStorage()
  }

  return {
    examInfo, questions, currentIndex, answers, remainingTime,
    switchCount, hasWarned, autoSubmitted, isFullscreen, isOnline,
    answeredCount, unansweredCount,
    setExamInfo, setQuestions, setCurrentIndex, saveAnswer,
    setRemainingTime, tick, incrementSwitch, setWarned,
    setAutoSubmitted, setFullscreen, setOnline,
    restoreFromStorage, saveToStorage, clearStorage, reset
  }
})
