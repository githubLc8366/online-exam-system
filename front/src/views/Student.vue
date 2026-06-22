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
        <el-form-item label="专业">
          <el-select v-model="searchForm.major" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="d in deptOptions" :key="d.id" :label="d.deptName" :value="d.deptName" />
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

    <!-- 操作区域 -->
    <el-card class="table-card" shadow="never">
    <template #header>
      <div class="flex-between">
        <span>学生列表</span>
        <div class="flex gap-2">
          <el-button type="warning" @click="showBatchResetDialog" :disabled="!selectedIds.length">
            <el-icon><Lock /></el-icon>批量重置密码
          </el-button>
          <el-button type="info" @click="downloadTemplate">
            <el-icon><Download /></el-icon>下载模板
          </el-button>
          <el-upload ref="importUploadRef" action="#" :auto-upload="false" :on-change="handleImport" :show-file-list="false" accept=".xlsx,.xls" class="inline-block">
            <el-button type="warning" :disabled="importLoading">
              <el-icon><Upload /></el-icon>Excel批量导入
            </el-button>
          </el-upload>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增学生
          </el-button>
        </div>
      </div>
    </template>

      <el-table
        :data="studentList"
        v-loading="loading"
        stripe
        border
        @selection-change="handleSelectionChange"
        :header-cell-style="{ background: '#f5f7fa' }"
      >
        <el-table-column type="selection" width="55" align="center" />
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
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button link type="warning" size="small" @click="handleResetPassword(row)">
              <el-icon><Lock /></el-icon>重置密码
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>删除
            </el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        class="dialog-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学号" prop="studentNo">
              <el-input v-model="formData.studentNo" placeholder="请输入学号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="formData.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="formData.gender" placeholder="请选择性别" class="w-full">
                <el-option label="男" :value="1" />
                <el-option label="女" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班级" prop="className">
            <el-select v-model="formData.className" placeholder="请选择班级" clearable @change="onClassChange">
              <el-option v-for="c in classOptions" :key="c.id" :label="c.className" :value="c.className" />
            </el-select>
          </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="专业">
              <el-input :model-value="formData.major || '选择班级后自动关联'" readonly disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="formData.phone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="邮箱地址" prop="email">
              <el-input v-model="formData.email" placeholder="请输入邮箱地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入学年份" prop="enrollmentYear">
              <el-date-picker
                v-model="formData.enrollmentYear"
                type="year"
                placeholder="选择入学年份"
                value-format="YYYY"
                class="w-full"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="账号状态" prop="status">
              <el-radio-group v-model="formData.status">
                <el-radio :label="1">正常</el-radio>
                <el-radio :label="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>
    <!-- 批量重置密码弹窗 -->
    <el-dialog v-model="batchResetVisible" title="批量重置密码" width="500px" :close-on-click-modal="false">
      <p class="mb-4">已选择 <strong>{{ selectedIds.length }}</strong> 名学生</p>
      <el-form label-width="120px">
        <el-form-item label="前缀（选填）">
          <el-input v-model="batchPrefix" placeholder="如：hbnu" style="width: 200px;" />
          <div class="text-gray text-sm mt-1">例：hbnu + 学号后6位 = hbnu210001</div>
        </el-form-item>
        <el-form-item label="使用学号后6位">
          <el-switch v-model="batchUseSuffix" />
          <span class="text-gray text-sm ml-2">关闭则仅用前缀作为密码</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchResetVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBatchReset" :loading="batchResetLoading">确定重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { studentApi } from '@/api/student'
import request from '@/api/request.js'
import { Search, Refresh, Plus, Edit, Delete, Upload, Download, Lock } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'

const route = useRoute()
const router = useRouter()
const classOptions = ref([])
const deptOptions = ref([])
const selectedIds = ref([])
const batchResetVisible = ref(false)
const batchPrefix = ref('')
const batchUseSuffix = ref(true)
const batchResetLoading = ref(false)
const importUploadRef = ref(null)
const importLoading = ref(false)

const showBatchResetDialog = () => {
  batchPrefix.value = ''
  batchUseSuffix.value = true
  batchResetVisible.value = true
}

const handleBatchReset = async () => {
  batchResetLoading.value = true
  try {
    await request.post('/admin/student/batch-reset-password', {
      ids: selectedIds.value,
      prefix: batchPrefix.value,
      useSuffix: batchUseSuffix.value
    })
    ElMessage.success('批量重置成功')
    batchResetVisible.value = false
    selectedIds.value = []
    getStudentList()
  } catch (e) {
    ElMessage.error('批量重置失败')
  } finally {
    batchResetLoading.value = false
  }
}
const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}
const loadOptions = async () => {
  try {
    const classes = await request.get('/admin/class/list', { params: { pageSize: 1000 } })
    classOptions.value = classes.records || classes.list || []
    const depts = await request.get('/admin/department/list')
    deptOptions.value = depts || []
  } catch (e) {
    console.error('获取选项失败', e)
  }
}

const downloadTemplate = () => {
  // 创建工作簿
  const headers = ['学号', '姓名', '性别', '班级', '专业', '联系电话', '邮箱', '入学年份', '备注']
  const example = ['2024001', '张三', '男', '软件2401班', '软件工程', '13800138000', 'zhangsan@xxx.com', '2024', '']
  
  const ws = XLSX.utils.aoa_to_sheet([headers, example])
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '学生导入模板')
  XLSX.writeFile(wb, '学生导入模板.xlsx')
}
// 搜索表单
const searchForm = reactive({
  studentNo: '',
  name: '',
  className: '',
  major: '',
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

// 弹窗相关
const dialogVisible = ref(false)
const dialogTitle = ref('新增学生')
const formRef = ref(null)
const submitLoading = ref(false)
const isEdit = ref(false)
const currentId = ref(null)

const formData = reactive({
  studentNo: '',
  name: '',
  gender: 1,
  className: '',
  major: '',
  phone: '',
  email: '',
  enrollmentYear: '',
  status: 1,
  remark: ''
})

const formRules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  className: [{ required: true, message: '请选择班级', trigger: 'change' }],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

// 班级变更时自动关联专业（从班级的 deptId 查 deptOptions 得到 deptName）
const onClassChange = (className) => {
  if (!className) {
    formData.major = ''
    return
  }
  const clazz = classOptions.value.find(c => c.className === className)
  if (clazz && clazz.deptId) {
    const dept = deptOptions.value.find(d => d.id === clazz.deptId)
    formData.major = dept ? dept.deptName : ''
  } else {
    formData.major = ''
  }
}
const handleResetPassword = (row) => {
  ElMessageBox.prompt(`请输入学生 "${row.name}" 的新密码`, '重置密码', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputType: 'password',
    inputPlaceholder: '请输入新密码（至少6位）',
    inputValidator: (value) => {
      if (!value || value.trim().length < 6) {
        return '密码长度不能少于6位'
      }
      return true
    }
  }).then(async ({ value }) => {
    try {
      await request.put(`/admin/student/${row.id}/reset-password`, { password: value })
      ElMessage.success('密码重置成功')
    } catch (e) {
      ElMessage.error('重置失败')
    }
  }).catch(() => {})
}
// 获取列表
const getStudentList = async () => {
  loading.value = true
  try {
    const params = {
      ...searchForm,
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    const res = await studentApi.getList(params)
// 拦截器已解包 Result，res 直接是 data 对象
if (res) {
  studentList.value = res.list || []
  pagination.total = res.total || 0
} else {
      ElMessage.error(res.message || '获取学生列表失败')
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

// 新增
const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增学生'
  currentId.value = null
  resetForm()
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑学生'
  currentId.value = row.id
  Object.assign(formData, row)
  dialogVisible.value = true
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除学生 "${row.name}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await studentApi.delete(row.id)
      ElMessage.success('删除成功')
      getStudentList()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 状态变更
const handleStatusChange = async (row, val) => {
  try {
    await studentApi.updateStatus(row.id, val)
    ElMessage.success(val === 1 ? '账号已启用' : '账号已禁用')
  } catch (error) {
    row.status = val === 1 ? 0 : 1
    ElMessage.error('状态更新失败')
  }
}

// 提交
const handleSubmit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (isEdit.value) {
        await studentApi.update({ ...formData, id: currentId.value })
      } else {
        await studentApi.add(formData)
      }
      
      // 只要上面的 await 没有抛出异常，就说明拦截器认为成功了
      ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
      dialogVisible.value = false
      getStudentList()
      
    } catch (error) {
      // 拦截器在 code !== 200 时会抛出异常，由这里统一捕获
      console.error(error)
      // 如果拦截器没处理弹窗，这里可以手动弹
      // ElMessage.error(isEdit.value ? '编辑失败' : '新增失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// 重置表单
const resetForm = () => {
  Object.assign(formData, {
    studentNo: '',
    name: '',
    gender: 1,
    className: '',
    major: '',
    phone: '',
    email: '',
    enrollmentYear: '',
    status: 1,
    remark: ''
  })
  formRef.value?.resetFields()
}

// 批量导入
const handleImport = async (uploadFile) => {
  if (!uploadFile.raw) {
    ElMessage.warning('无法读取文件，请重试')
    return
  }
  importLoading.value = true
  try {
    const data = await uploadFile.raw.arrayBuffer()
    const workbook = XLSX.read(data, { type: 'array' })
    const firstSheet = workbook.Sheets[workbook.SheetNames[0]]
    const jsonData = XLSX.utils.sheet_to_json(firstSheet, { header: 1 })

    if (jsonData.length < 2) {
      ElMessage.warning('文件内容为空或格式不正确')
      return
    }

    const headers = jsonData[0]
    const rows = jsonData.slice(1)
    const students = rows.map(row => {
      const obj = {}
      headers.forEach((h, i) => { obj[h] = row[i] })
      return obj
    }).filter(r => r['学号'] || r['studentNo'])

    if (students.length === 0) {
      ElMessage.warning('未找到有效学生数据，请检查模板格式')
      return
    }

    // 调用后端批量导入接口
    const res = await studentApi.batchImport(students)
    const imported = res?.success ?? students.length
    const skipped = res?.skip ?? 0
    ElMessage.success(
      skipped > 0
        ? `成功导入 ${imported} 名学生，跳过 ${skipped} 条（学号为空或已存在）`
        : `成功导入 ${imported} 名学生`
    )
    getStudentList()
  } catch (error) {
    ElMessage.error('导入失败：' + error.message)
  } finally {
    // 清空上传队列，允许重新选择同一个文件
    importUploadRef.value?.clearFiles()
    importLoading.value = false
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

onMounted(async () => {
  await Promise.all([getStudentList(), loadOptions()])

  // 来自组织架构页的"添加学生"：?action=add&className=xxx
  if (route.query.action === 'add') {
    handleAdd()
    if (route.query.className) {
      formData.className = String(route.query.className)
    }
    // 清掉 query，避免刷新时再次自动弹出
    router.replace({ path: route.path, query: {} })
  }
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

.dialog-form {
  padding: 10px 20px 0;
}

:deep(.el-upload) {
  display: inline-block;
}
</style>
