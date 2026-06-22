import request from './request'

// 教师管理 API
export const teacherApi = {
  // 获取教师列表
  getList(params) {
    return request.get('/admin/teacher/list', { params })
  },

  // 新增教师
  add(data) {
    return request.post('/admin/teacher', data)
  },

  // 编辑教师
  update(data) {
    return request.put('/admin/teacher', data)
  },

  // 删除教师
  delete(id) {
    return request.delete(`/admin/teacher/${id}`)
  },

  // 修改教师状态
  updateStatus(id, status) {
    return request.put(`/admin/teacher/${id}/status`, { status })
  }
}