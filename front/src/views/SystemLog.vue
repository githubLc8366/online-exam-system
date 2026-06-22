<template>
  <div class="system-log-management">
    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            clearable
          />
        </el-form-item>
        <el-form-item label="操作人姓名">
          <el-input v-model="searchForm.operatorName" placeholder="请输入操作人姓名" clearable />
        </el-form-item>
        <el-form-item label="模块类型">
          <el-select v-model="searchForm.module" placeholder="全部" clearable class="w-40">
            <el-option
              v-for="item in moduleOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作与列表区域 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="flex-between">
          <span>系统日志</span>
          <div class="flex gap-2">
            <el-button type="success" @click="handleExport">
              <el-icon><Download /></el-icon>导出日志
            </el-button>
            <el-button type="danger" @click="handleBatchDelete" :disabled="!selectedIds.length">
              <el-icon><Delete /></el-icon>批量删除
            </el-button>
            <el-button type="danger" plain @click="handleClearAll">
              <el-icon><DeleteFilled /></el-icon>清空全部
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="logList"
        v-loading="loading"
        stripe
        border
        :header-cell-style="{ background: '#f5f7fa' }"
        @selection-change="handleSelectionChange"
        :default-sort="{ prop: 'operationTime', order: 'descending' }"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="account" label="账号" min-width="120" show-overflow-tooltip />
        <el-table-column prop="name" label="姓名" min-width="100" />
        <el-table-column prop="role" label="角色" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.role === '管理员'" type="danger" size="small">{{ row.role }}</el-tag>
            <el-tag v-else-if="row.role === '教师'" type="primary" size="small">{{ row.role }}</el-tag>
            <el-tag v-else type="success" size="small">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="操作模块" width="130" align="center">
          <template #default="{ row }">
            <el-tag effect="plain" size="small">{{ row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="操作描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP 地址" width="140" align="center" />
        <el-table-column prop="operationTime" label="操作时间" width="170" align="center" sortable />
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleViewDetail(row)">
              <el-icon><View /></el-icon>查看详情
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="日志详情"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-descriptions :column="1" border>
        <el-descriptions-item label="账号">{{ currentLog.account }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ currentLog.name }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag v-if="currentLog.role === '管理员'" type="danger">{{ currentLog.role }}</el-tag>
          <el-tag v-else-if="currentLog.role === '教师'" type="primary">{{ currentLog.role }}</el-tag>
          <el-tag v-else type="success">{{ currentLog.role }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作模块">{{ currentLog.module }}</el-descriptions-item>
        <el-descriptions-item label="操作描述">{{ currentLog.description }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ currentLog.method || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求路径">{{ currentLog.path || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP 地址">{{ currentLog.ip }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ currentLog.operationTime }}</el-descriptions-item>
        <el-descriptions-item label="异常信息" v-if="currentLog.errorMsg">
          <el-popover placement="top-start" width="400" trigger="hover">
            <template #reference>
              <span class="text-danger cursor-pointer">{{ currentLog.errorMsg }}</span>
            </template>
            <div style="max-height: 200px; overflow-y: auto;">
              <pre style="margin: 0; white-space: pre-wrap; word-break: break-all;">{{ currentLog.errorMsg }}</pre>
            </div>
          </el-popover>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Download, Delete, DeleteFilled, View } from '@element-plus/icons-vue'
import { systemLogApi } from '@/api/systemLog'

// 模块类型选项
const moduleOptions = [
  { label: '用户管理', value: '用户管理' },
  { label: '学生管理', value: '学生管理' },
  { label: '教师管理', value: '教师管理' },
  { label: '组织架构', value: '组织架构' },
  { label: '题库管理', value: '题库管理' },
  { label: '考试发布', value: '考试发布' },
  { label: '系统设置', value: '系统设置' },
  { label: '登录认证', value: '登录认证' }
]

// 搜索表单
const searchForm = reactive({
  dateRange: [],
  operatorName: '',
  module: ''
})

// 分页参数
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 列表数据
const logList = ref([])
const loading = ref(false)
const selectedIds = ref([])

// 详情弹窗
const detailVisible = ref(false)
const currentLog = ref({})

// 模拟日志数据
const mockLogList = [
  { id: 1, account: 'admin', name: '系统管理员', role: '管理员', module: '学生管理', description: '修改了学号为 2021001 的学生信息，将联系电话更新为 13800138001', ip: '192.168.1.101', operationTime: '2026-04-24 14:32:18', method: 'PUT', path: '/api/admin/student' },
  { id: 2, account: 'T2021001', name: '陈教授', role: '教师', module: '题库管理', description: '新增了题库 "Java程序设计期末复习"，共导入 50 道题目', ip: '192.168.1.205', operationTime: '2026-04-24 13:15:42', method: 'POST', path: '/api/teacher/question-bank' },
  { id: 3, account: '2021002', name: '李四', role: '学生', module: '登录认证', description: '学生账号登录系统成功', ip: '192.168.1.88', operationTime: '2026-04-24 12:08:55', method: 'POST', path: '/api/auth/login' },
  { id: 4, account: 'admin', name: '系统管理员', role: '管理员', module: '教师管理', description: '删除了工号为 T2021004 的教师账号', ip: '192.168.1.101', operationTime: '2026-04-24 10:45:30', method: 'DELETE', path: '/api/admin/teacher/4' },
  { id: 5, account: 'T2021003', name: '王老师', role: '教师', module: '考试发布', description: '发布了考试 "软件工程期中测试"，考试时长 120 分钟，参与班级：软件2101班、软件2102班', ip: '192.168.1.206', operationTime: '2026-04-24 09:22:11', method: 'POST', path: '/api/teacher/exam' },
  { id: 6, account: 'admin', name: '系统管理员', role: '管理员', module: '组织架构', description: '新增了院系 "人工智能学院"，院系代码 AI', ip: '192.168.1.101', operationTime: '2026-04-23 16:18:07', method: 'POST', path: '/api/admin/department' },
  { id: 7, account: '2021001', name: '张三', role: '学生', module: '考试发布', description: '提交了考试 "数据结构期末考" 的答卷', ip: '192.168.1.87', operationTime: '2026-04-23 14:55:33', method: 'POST', path: '/api/student/exam/submit' },
  { id: 8, account: 'T2021002', name: '刘老师', role: '教师', module: '用户管理', description: '批量导入了 35 名学生账号，班级：软件2101班', ip: '192.168.1.204', operationTime: '2026-04-23 11:30:00', method: 'POST', path: '/api/admin/student/import' },
  { id: 9, account: 'admin', name: '系统管理员', role: '管理员', module: '系统设置', description: '修改了系统参数：单次考试最大时长由 180 分钟调整为 240 分钟', ip: '192.168.1.101', operationTime: '2026-04-22 17:05:22', method: 'PUT', path: '/api/admin/settings' },
  { id: 10, account: '2022001', name: '赵六', role: '学生', module: '登录认证', description: '学生账号登录系统失败，密码错误次数已达 3 次', ip: '192.168.1.92', operationTime: '2026-04-22 08:12:45', method: 'POST', path: '/api/auth/login', errorMsg: 'AuthenticationException: 用户名或密码错误，剩余尝试次数 2 次' }
]

// 获取日志列表
const getLogList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      operatorName: searchForm.operatorName,
      module: searchForm.module,
      startDate: searchForm.dateRange?.[0] || '',
      endDate: searchForm.dateRange?.[1] || ''
    }
    const res = await systemLogApi.getList(params)
    logList.value = (res.records || []).map(item => ({
    id: item.id,
    account: item.userNo,
    name: item.userName,
    role: item.roleCode === 'admin' ? '管理员' : item.roleCode === 'teacher' ? '教师' : '学生',
    module: item.module,
    description: item.content,
    method: item.requestMethod,        
    path: item.requestUrl,         
    ip: item.ipAddress,
    operationTime: item.createTime
}))
    pagination.total = res.total || 0
  } catch (error) {
    ElMessage.error('获取日志列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  getLogList()
}

// 重置
const handleReset = () => {
  searchForm.dateRange = []
  searchForm.operatorName = ''
  searchForm.module = ''
  handleSearch()
}

// 多选变化
const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

// 查看详情
const handleViewDetail = (row) => {
  currentLog.value = { ...row }
  detailVisible.value = true
}

// 单条删除
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该条日志记录吗？', '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
  }).then(async () => {
    try {
      await systemLogApi.batchDelete([row.id])   
      ElMessage.success('删除成功')
      getLogList()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

// 批量删除
const handleBatchDelete = () => {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择要删除的日志')
    return
  }
  ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 条日志记录吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await systemLogApi.batchDelete(selectedIds.value)
      ElMessage.success('批量删除成功')
      selectedIds.value = []
      getLogList()
    } catch (error) {
      ElMessage.error('批量删除失败')
    }
  }).catch(() => {})
}

// 清空全部
const handleClearAll = () => {
  ElMessageBox.confirm('确定要清空全部系统日志吗？此操作不可恢复，仅限超级管理员执行。', '危险操作', {
    confirmButtonText: '确定清空',
    cancelButtonText: '取消',
    type: 'danger'
  }).then(async () => {
    try {
      await systemLogApi.clearAll()
      ElMessage.success('全部日志已清空')
      getLogList()
    } catch (error) {
      ElMessage.error('清空失败')
    }
  }).catch(() => {})
}

// 导出日志
const handleExport = () => {
  ElMessage.info('正在生成日志导出文件，请稍候...')
  setTimeout(() => {
    // 模拟导出：创建并下载一个 CSV 文件
    const headers = ['账号', '姓名', '角色', '操作模块', '操作描述', 'IP地址', '操作时间']
    const rows = logList.value.map(item => [
      item.account,
      item.name,
      item.role,
      item.module,
      item.description,
      item.ip,
      item.operationTime
    ])
    const csvContent = [headers.join(','), ...rows.map(r => r.map(field => `"${String(field).replace(/"/g, '""')}"`).join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `系统日志_${new Date().toLocaleDateString()}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)
    ElMessage.success('日志导出成功')
  }, 800)
}

// 分页事件
const handleSizeChange = (val) => {
  pagination.pageSize = val
  pagination.page = 1
  getLogList()
}

const handlePageChange = (val) => {
  pagination.page = val
  getLogList()
}

onMounted(() => {
  getLogList()
})
</script>

<style scoped>
.system-log-management {
  width: 100%;
}

.search-card {
  margin-bottom: 16px;
}

.table-card {
  margin-bottom: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.flex {
  display: flex;
}

.gap-2 {
  gap: 8px;
}

.w-40 {
  width: 160px;
}

.text-danger {
  color: #f56c6c;
}

.cursor-pointer {
  cursor: pointer;
}

@media (max-width: 768px) {
  .el-form-item {
    margin-bottom: 12px;
  }
}
</style>
