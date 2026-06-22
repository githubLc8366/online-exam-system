import request from './request'

// 系统日志 API
export const systemLogApi = {
  // 获取日志列表
  getList(params) {
    return request.get('/admin/system-log/list', { params })
  },

  // 查看日志详情
  getDetail(id) {
    return request.get(`/admin/system-log/${id}`)
  },

  // 批量删除日志
  batchDelete(ids) {
    return request.delete('/admin/system-log/batch', { data: ids })
  },

  // 清空全部日志（仅限超级管理员）
  clearAll() {
    return request.delete('/admin/system-log/clear')
  },

  // 导出日志
  exportLog(params) {
    return request.get('/admin/system-log/export', {
      params,
      responseType: 'blob'
    })
  }
}
