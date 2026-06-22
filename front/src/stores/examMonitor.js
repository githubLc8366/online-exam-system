import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useExamMonitorStore = defineStore('examMonitor', () => {
  // 当前监控的考试
  const currentExam = ref(null)

  // 考生状态列表
  const studentStatusList = ref([])

  // 是否正在轮询
  const isPolling = ref(false)

  // 设置当前考试
  const setCurrentExam = (exam) => {
    currentExam.value = exam
  }

  // 更新考生状态
  const updateStudentStatus = (list) => {
    studentStatusList.value = list
  }

  // 更新单个考生状态
  const updateSingleStatus = (studentId, status, extra = {}) => {
    const idx = studentStatusList.value.findIndex(s => s.studentId === studentId)
    if (idx !== -1) {
      studentStatusList.value[idx] = { ...studentStatusList.value[idx], ...status, ...extra }
    }
  }

  // 延长考生时长
  const extendDuration = (studentId, extraMinutes) => {
    const idx = studentStatusList.value.findIndex(s => s.studentId === studentId)
    if (idx !== -1) {
      studentStatusList.value[idx].remainingTime += extraMinutes * 60
      studentStatusList.value[idx].extended = true
    }
  }

  // 强制交卷
  const forceSubmit = (studentId) => {
    updateSingleStatus(studentId, { status: 'submitted' })
  }

  // 开始轮询
  const startPolling = () => { isPolling.value = true }
  const stopPolling = () => { isPolling.value = false }

  return {
    currentExam,
    studentStatusList,
    isPolling,
    setCurrentExam,
    updateStudentStatus,
    updateSingleStatus,
    extendDuration,
    forceSubmit,
    startPolling,
    stopPolling
  }
})
