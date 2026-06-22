<template>
  <div class="student-management">
    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="学号">
          <el-input v-model="searchForm.studentNo" placeholder="请输入学号" clearable />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="searchForm.className" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="c in classOptions" :key="c.id" :label="c.className" :value="c.className" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
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

    <!-- 数据表格 -->
    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="flex-between">
          <span>学生列表（我所教班级）</span>
        </div>
      </template>

      <el-table
        :data="studentList"
        v-loading="loading"
        stripe
        border
        :header-cell-style="{ background: '#f5f7fa' }"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="studentNo" label="学号" min-width="120" show-overflow-tooltip />
        <el-table-column prop="name" label="姓名" min-width="100" />
        <el-table-column prop="gender" label="性别" width="70" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.gender === 1" size="small">男</el-tag>
            <el-tag v-else-if="row.gender === 2" type="danger" size="small">女</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="className" label="班级" min-width="120" show-overflow-tooltip />
        <el-table-column prop="major" label="专业" min-width="140" show-overflow-tooltip />
        <el-table-column prop="phone" label="联系电话" min-width="130" />
        <el-table-column prop="email" label="邮箱地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="enrollmentYear" label="入学年份" width="100" align="center" />
        <el-table-column prop="status" label="账号状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="(val) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import request from '@/api/request.js'

const classOptions = ref([])

const loadClassOptions = async () => {
  try {
    const res = await request.get('/admin/class/list', { params: { pageSize: 100 } })
    classOptions.value = res.records || res.list || []
  } catch (e) {
    console.error('获取班级列表失败', e)
  }
}

// 搜索表单
const searchForm = reactive({
  studentNo: '',
  name: '',
  className: '',
  status: undefined
})

// 分页参数
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 列表数据
const studentList = ref([])
const loading = ref(false)

// 获取学生列表
const getStudentList = async () => {
  loading.value = true
  try {
    const params = {
      ...searchForm,
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    const res = await request.get('/teacher/student/list', { params })
    if (res) {
      studentList.value = res.list || []
      pagination.total = res.total || 0
    }
  } catch (error) {
    ElMessage.error('获取学生列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  getStudentList()
}

// 重置
const handleReset = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = key === 'status' ? undefined : ''
  })
  handleSearch()
}

// 状态变更
const handleStatusChange = async (row, val) => {
  try {
    await request.put(`/admin/student/${row.id}/status`, { status: val })
    ElMessage.success(val === 1 ? '账号已启用' : '账号已禁用')
  } catch (error) {
    row.status = val === 1 ? 0 : 1
    ElMessage.error('状态更新失败')
  }
}

// 分页事件
const handleSizeChange = (val) => {
  pagination.pageSize = val
  pagination.page = 1
  getStudentList()
}

const handlePageChange = (val) => {
  pagination.page = val
  getStudentList()
}

onMounted(() => {
  loadClassOptions()
  getStudentList()
})
</script>

<style scoped>
.student-management {
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
</style>