import request from './request'

// 学生管理 API
export const studentApi = {
  // 获取学生列表
  getList(params) {
    return request.get('/admin/student/list', { params })
  },

  // 新增学生
  add(data) {
    return request.post('/admin/student', data)
  },

  // 编辑学生
  update(data) {
    return request.put('/admin/student', data)
  },

  // 删除学生
  delete(id) {
    return request.delete(`/admin/student/${id}`)
  },

  // 批量导入学生
  batchImport(students) {
    return request.post('/admin/student/import', students)
  },

  // 修改学生状态
  updateStatus(id, status) {
    return request.put(`/admin/student/${id}/status`, { status })
  }

}