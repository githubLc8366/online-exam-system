import request from './request'

// 组织架构管理 API
export const organizationApi = {
  // 获取组织架构树
  getTree() {
    return request.get('/admin/organization/tree')
  },

  // 获取院系列表
  getDepartments() {
    return request.get('/admin/department/list')
  },

  // 获取院系树（仅院系，供树选择器使用）
  getDeptTree() {
    return request.get('/admin/department/tree')
  },

  // 新增院系
  addDepartment(data) {
    return request.post('/admin/department', data)
  },

  // 编辑院系
  updateDepartment(data) {
    return request.put('/admin/department', data)
  },

  // 删除院系
  deleteDepartment(id) {
    return request.delete(`/admin/department/${id}`)
  },

  // 获取班级列表
  getClasses(params) {
    return request.get('/admin/class/list', { params })
  },

  // 新增班级
  addClass(data) {
    return request.post('/admin/class', data)
  },

  // 编辑班级
  updateClass(data) {
    return request.put('/admin/class', data)
  },

  // 删除班级
  deleteClass(id) {
    return request.delete(`/admin/class/${id}`)
  }
}