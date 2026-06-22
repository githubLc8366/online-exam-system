import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useExamPaperStore = defineStore('examPaper', () => {
  // 当前编辑的试卷
  const currentPaper = ref({
    id: null,
    name: '',
    subject: '',
    totalScore: 100,
    duration: 120,
    passScore: 0,    // 初始为0，保存时后端会自动计算
    questions: [],
    mode: 'manual'
  })

  // 组卷配置（随机组卷用）
  const randomConfig = ref({
    singleChoice: { count: 10, score: 2 },
    multiChoice: { count: 5, score: 3 },
    judge: { count: 5, score: 2 },
    fillBlank: { count: 5, score: 2 },
    shortAnswer: { count: 2, score: 10 },
    programming: { count: 1, score: 20 },
    difficultyRatio: { easy: 30, medium: 50, hard: 20 },
    knowledgeRatio: {}   // 【新增】知识点比例，如 {"基础语法": 50, "面向对象": 30, "算法": 20}
  })

  // 已选题目列表
  const selectedQuestions = ref([])

  // 试卷总分
  const totalScore = computed(() => {
    if (currentPaper.value.mode === 'random') {
      const c = randomConfig.value
      return c.singleChoice.count * c.singleChoice.score +
        c.multiChoice.count * c.multiChoice.score +
        c.judge.count * c.judge.score +
        c.fillBlank.count * c.fillBlank.score +
        c.shortAnswer.count * c.shortAnswer.score +
        c.programming.count * c.programming.score
    }
    return selectedQuestions.value.reduce((sum, q) => sum + (q.score || 0), 0)
  })

  // 题目数量
  const questionCount = computed(() => {
    if (currentPaper.value.mode === 'random') {
      const c = randomConfig.value
      return c.singleChoice.count + c.multiChoice.count + c.judge.count +
        c.fillBlank.count + c.shortAnswer.count + c.programming.count
    }
    return selectedQuestions.value.length
  })

  // 设置试卷基本信息
  const setPaperInfo = (info) => {
    Object.assign(currentPaper.value, info)
  }

  // 添加题目
  const addQuestion = (question) => {
    const exists = selectedQuestions.value.find(q => q.id === question.id)
    if (!exists) {
      selectedQuestions.value.push({ ...question, paperOrder: selectedQuestions.value.length + 1 })
    }
  }

  // 移除题目
  const removeQuestion = (questionId) => {
    selectedQuestions.value = selectedQuestions.value.filter(q => q.id !== questionId)
    // 重新排序
    selectedQuestions.value.forEach((q, idx) => { q.paperOrder = idx + 1 })
  }

  // 清空题目
  const clearQuestions = () => {
    selectedQuestions.value = []
  }

  // 设置组卷模式
  const setMode = (mode) => {
    currentPaper.value.mode = mode
  }

  // 重置试卷
  const resetPaper = () => {
    currentPaper.value = {
      id: null,
      name: '',
      subject: '',
      totalScore: 100,
      duration: 120,
      passScore: 0,
      questions: [],
      mode: 'manual'
    }
    selectedQuestions.value = []
  }

  return {
    currentPaper,
    randomConfig,
    selectedQuestions,
    totalScore,
    questionCount,
    setPaperInfo,
    addQuestion,
    removeQuestion,
    clearQuestions,
    setMode,
    resetPaper
  }
})