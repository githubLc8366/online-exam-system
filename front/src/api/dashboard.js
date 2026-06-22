import request from './request'

// 首页/数据统计 API
export const dashboardApi = {
  // 获取统计数据（总用户、总场次、累计人次）
  getStatistics() {
    return request.get('/dashboard/statistics')
  },

  // 获取最近7天系统活跃趋势
  getTrend() {
    return request.get('/dashboard/trend')
  }
}
