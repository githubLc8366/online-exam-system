<template>
  <div class="p-6">
    <div class="bg-white rounded-lg shadow-sm p-6">
      <!-- 顶部操作栏 -->
      <div class="flex justify-between items-center mb-4">
        <h2 class="text-xl font-bold text-gray-800">教师管理</h2>
        <div class="flex items-center space-x-3">
          <el-button type="primary" @click="showAddDialog">
            <i class="el-icon-plus"></i>
            新增教师
          </el-button>
        </div>
      </div>

      <!-- 搜索区域 -->
      <div class="mb-4 p-4 bg-gray-50 rounded-lg">
        <el-form :inline="true" :model="searchForm" size="small">
          <el-form-item label="工号">
            <el-input v-model="searchForm.teacherNo" placeholder="请输入工号" clearable />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
          </el-form-item>
          <el-form-item label="所属院系">
            <el-select v-model="searchForm.department" placeholder="请选择院系" clearable style="width: 160px">
              <el-option v-for="dept in departments" :key="dept.id" :label="dept.deptName" :value="dept.deptName" />
            </el-select>
          </el-form-item>
          <el-form-item label="职称">
            <el-select v-model="searchForm.title" placeholder="请选择职称" clearable style="width: 140px">
              <el-option label="教授" value="教授" />
              <el-option label="副教授" value="副教授" />
              <el-option label="讲师" value="讲师" />
              <el-option label="助教" value="助教" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 100px">
              <el-option label="正常" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="search">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="teacherNo" label="工号" width="140" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="department" label="所属院系" min-width="150" />
        <el-table-column prop="title" label="职称" width="100" />
        <el-table-column prop="hireDate" label="入职日期" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="text" size="small" @click="showEditDialog(row)">编辑</el-button>
            <el-button
              type="text"
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="text" size="small" style="color: #f56c6c" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="flex justify-end mt-4">
        <el-pagination
          v-model:current-page="pagination.current"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 新增 / 编辑对话框 -->
    <el-dialog
      :title="dialogType === 'add' ? '新增教师' : '编辑教师'"
      v-model="dialogVisible"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px" size="small">
        <el-form-item label="工号" prop="teacherNo">
          <el-input v-model="form.teacherNo" placeholder="请输入工号" :disabled="dialogType === 'edit'" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="所属院系" prop="department">
          <el-select v-model="form.department" placeholder="请选择院系" style="width: 100%">
            <el-option v-for="dept in departments" :key="dept.id" :label="dept.deptName" :value="dept.deptName" />
          </el-select>
        </el-form-item>
        <el-form-item label="职称" prop="title">
          <el-select v-model="form.title" placeholder="请选择职称" style="width: 100%">
            <el-option label="教授" value="教授" />
            <el-option label="副教授" value="副教授" />
            <el-option label="讲师" value="讲师" />
            <el-option label="助教" value="助教" />
          </el-select>
        </el-form-item>
        <el-form-item label="入职日期" prop="hireDate">
          <el-date-picker
            v-model="form.hireDate"
            type="date"
            placeholder="选择入职日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status" v-if="dialogType === 'add'">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">正常</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false" size="small">取消</el-button>
        <el-button type="primary" @click="submitForm" size="small" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

// 数据
const tableData = ref([])
const loading = ref(false)
const departments = ref([])

// 分页
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

// 搜索表单
const searchForm = reactive({
  teacherNo: '',
  name: '',
  department: '',
  title: '',
  status: ''
})

// 对话框
const dialogVisible = ref(false)
const dialogType = ref('add') // 'add' | 'edit'
const submitLoading = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  teacherNo: '',
  name: '',
  department: '',
  title: '',
  hireDate: '',
  status: 1
})

// 表单验证
const formRules = {
  teacherNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  department: [{ required: true, message: '请选择所属院系', trigger: 'change' }],
  title: [{ required: true, message: '请选择职称', trigger: 'change' }],
  hireDate: [{ required: true, message: '请选择入职日期', trigger: 'change' }]
}

// 获取院系列表
async function fetchDepartments() {
  try {
    const res = await request.get('/admin/department/list')
    departments.value = res.list || res
  } catch (e) {
    console.error('获取院系列表失败', e)
  }
}

// 获取教师列表
async function fetchData() {
  loading.value = true
  try {
    const params = {
      page: pagination.current,
      pageSize: pagination.pageSize
    }
    // 搜索条件
    if (searchForm.teacherNo) params.teacherNo = searchForm.teacherNo
    if (searchForm.name) params.name = searchForm.name
    if (searchForm.department) params.department = searchForm.department
    if (searchForm.title) params.title = searchForm.title
    if (searchForm.status !== '' && searchForm.status !== null) params.status = searchForm.status

    const res = await request.get('/admin/teacher/list', { params })
    tableData.value = res.list || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('获取教师列表失败', e)
  } finally {
    loading.value = false
  }
}

// 搜索
function search() {
  pagination.current = 1
  fetchData()
}

// 重置搜索
function resetSearch() {
  searchForm.teacherNo = ''
  searchForm.name = ''
  searchForm.department = ''
  searchForm.title = ''
  searchForm.status = ''
  pagination.current = 1
  fetchData()
}

// 翻页
function handlePageChange(page) {
  pagination.current = page
  fetchData()
}

// 新增
function showAddDialog() {
  dialogType.value = 'add'
  form.id = null
  form.teacherNo = ''
  form.name = ''
  form.department = ''
  form.title = ''
  form.hireDate = ''
  form.status = 1
  dialogVisible.value = true
}

// 编辑
function showEditDialog(row) {
  dialogType.value = 'edit'
  form.id = row.id
  form.teacherNo = row.teacherNo
  form.name = row.name
  form.department = row.department
  form.title = row.title
  form.hireDate = row.hireDate
  form.status = row.status
  dialogVisible.value = true
}

// 提交表单
async function submitForm() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (dialogType.value === 'add') {
      await request.post('/admin/teacher', form)
      ElMessage.success('新增成功')
    } else {
      await request.put('/admin/teacher', form)
      ElMessage.success('编辑成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error('操作失败', e)
  } finally {
    submitLoading.value = false
  }
}

// 切换状态
async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const actionText = newStatus === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定${actionText}该教师吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.put(`/admin/teacher/${row.id}/status`, { status: newStatus })
    ElMessage.success(`${actionText}成功`)
    fetchData()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('操作失败', e)
    }
  }
}

// 删除
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该教师吗？此操作不可恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.delete(`/admin/teacher/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除失败', e)
    }
  }
}

// 初始化
onMounted(() => {
  fetchDepartments()
  fetchData()
})
</script>