<template>
  <div class="organization-management">
    <el-row :gutter="20">
      <!-- 左侧：院系树 -->
      <el-col :xs="24" :sm="24" :md="10" :lg="6">
        <el-card shadow="never">
          <template #header>
            <div class="flex-between">
              <span>院系管理</span>
              <el-button type="primary" size="small" @click="handleAddDepartment">
                <el-icon><Plus /></el-icon>新增院系
              </el-button>
            </div>
          </template>
          <el-tree
            :data="departmentTree"
            :props="{ label: 'label', children: 'children' }"
            node-key="id"
            highlight-current
            default-expand-all
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <div class="custom-tree-node flex-between w-full">
                <span>{{ data.label || data.deptName || data.className }}</span>
                <span class="tree-actions">
                  <template v-if="!data.isClass">
                    <el-button link type="primary" size="small" @click.stop="handleEditDepartment(data)">
                      <el-icon><Edit /></el-icon>
                    </el-button>
                    <el-button link type="danger" size="small" @click.stop="handleDeleteDepartment(data)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </template>
                  <template v-else>
                    <el-button link type="danger" size="small" @click.stop="handleDeleteClassNode(data)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </template>
                </span>
              </div>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <!-- 右侧：根据层级切换显示 -->
      <el-col :xs="24" :sm="24" :md="14" :lg="18">
        <el-card shadow="never">
          <template #header>
            <div class="flex-between">
              <span>
                <template v-if="activeView === 'dept'">系列表</template>
                <template v-else-if="activeView === 'class'">班级列表</template>
                <template v-else>学生列表</template>
                <el-tag v-if="currentDepartment" type="info" class="ml-2">{{ currentDepartment.label || currentDepartment.deptName }}</el-tag>
              </span>
              <el-button
                type="primary"
                size="small"
                @click="handleAddInRightPanel"
                :disabled="!currentDepartment"
              >
                <el-icon><Plus /></el-icon>{{
                  activeView === 'dept' ? '新增系'
                    : activeView === 'class' ? '新增班级'
                    : '添加学生'
                }}
              </el-button>
            </div>
          </template>

          <!-- 系列表 -->
          <el-table v-if="activeView === 'dept'" :data="subDepts" stripe border>
            <el-table-column prop="label" label="系名称" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleEditDepartment(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="handleDeleteDepartment(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 班级列表 -->
          <template v-else-if="activeView === 'class'">
            <el-table :data="classList" v-loading="classLoading" stripe border :header-cell-style="{ background: '#f5f7fa' }">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="className" label="班级名称" min-width="150" />
              <el-table-column prop="deptName" label="所属院系" min-width="150" />
              <el-table-column prop="enrollmentYear" label="入学年份" width="100" align="center" />
              <el-table-column prop="studentCount" label="学生人数" width="100" align="center" />
              <el-table-column label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-switch
                    :model-value="row.status === 1"
                    :loading="row._switchLoading"
                    @change="(val) => handleStatusChange(row, val)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150" fixed="right" align="center">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="handleEditClass(row)"><el-icon><Edit /></el-icon>编辑</el-button>
                  <el-button link type="danger" size="small" @click="handleDeleteClass(row)"><el-icon><Delete /></el-icon>删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="pagination-wrapper">
              <el-pagination v-model:current-page="classPagination.page" v-model:page-size="classPagination.pageSize" :page-sizes="[10, 20, 50]" :total="classPagination.total" layout="total, sizes, prev, pager, next, jumper" @size-change="handleClassSizeChange" @current-change="handleClassPageChange" />
            </div>
          </template>

          <!-- 学生列表 -->
          <el-table v-else :data="studentList" v-loading="studentLoading" stripe border>
            <el-table-column label="学号" prop="studentNo" />
            <el-table-column label="姓名" prop="name" />
            <el-table-column label="性别">
              <template #default="{ row }">
                {{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="状态">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 院系对话框 -->
    <el-dialog v-model="deptDialogVisible" :title="deptDialogTitle" width="500px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="deptFormRef" :model="deptForm" :rules="deptRules" label-width="100px">
        <el-form-item label="院系名称" prop="deptName">
          <el-input v-model="deptForm.deptName" placeholder="请输入院系名称" />
        </el-form-item>
        <el-form-item label="排序" prop="orderNum">
          <el-input-number v-model="deptForm.orderNum" :min="0" class="w-full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deptDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDeptSubmit" :loading="deptSubmitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 班级对话框 -->
    <el-dialog v-model="classDialogVisible" :title="classDialogTitle" width="500px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="classFormRef" :model="classForm" :rules="classRules" label-width="100px">
        <el-form-item label="所属院系" prop="deptId">
          <el-tree-select
            v-model="classForm.deptId"
            :data="deptTreeData"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            placeholder="请选择所属院系"
            check-strictly
            class="w-full"
            filterable
          />
        </el-form-item>
        <el-form-item label="班级名称" prop="className">
          <el-input v-model="classForm.className" placeholder="请输入班级名称，如：软件2101班" />
        </el-form-item>
        <el-form-item label="入学年份" prop="enrollmentYear">
          <el-input v-model="classForm.enrollmentYear" placeholder="请输入入学年份，如：2021" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleClassSubmit" :loading="classSubmitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import request from '@/api/request.js'
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { organizationApi } from '@/api/organization'

const router = useRouter()

// ================== 院系相关 ==================
const departmentTree = ref([])
const departmentList = ref([])
const currentDepartment = ref(null)
const deptTreeData = ref([])

// 院系对话框
const deptDialogVisible = ref(false)
const deptDialogTitle = ref('新增院系')
const deptFormRef = ref(null)
const deptSubmitLoading = ref(false)
const isEditDept = ref(false)
const currentDeptId = ref(null)

const activeView = ref('class') // 'dept' | 'class' | 'student'
const currentClassId = ref(null)
const studentList = ref([])
const studentLoading = ref(false)
const subDepts = ref([])
const deptForm = reactive({
  deptName: '',
  orderNum: 0,
  parentId: null
})
const loadSubDepts = () => {
  const dept = currentDepartment.value
  if (!dept || !dept.children) {
    subDepts.value = []
    return
  }
  subDepts.value = dept.children.filter(c => !c.isClass)
}
const loadStudentsByClass = async () => {
  studentLoading.value = true
  try {
    const res = await request.get('/admin/student/list', {
      params: { classId: currentClassId.value, page: 1, pageSize: 100 }
    })
    studentList.value = res.list || []
  } catch (e) {
    console.error('获取学生列表失败', e)
  } finally {
    studentLoading.value = false
  }
}
const deptRules = {
  deptName: [{ required: true, message: '请输入院系名称', trigger: 'blur' }]
}

// ================== 班级相关 ==================
const classList = ref([])
const classLoading = ref(false)

const classPagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 班级对话框
const classDialogVisible = ref(false)
const classDialogTitle = ref('新增班级')
const classFormRef = ref(null)
const classSubmitLoading = ref(false)
const isEditClass = ref(false)

const classForm = reactive({
  deptId: null,
  className: '',
  enrollmentYear: ''
})

const classRules = {
  deptId: [{ required: true, message: '请选择所属院系', trigger: 'change' }],
  className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
  enrollmentYear: [{ required: true, message: '请输入入学年份', trigger: 'blur' }]
}

// ================== 方法 ==================

// 获取院系数据
const getDepartmentTree = async () => {
  try {
    const res = await organizationApi.getTree()
    departmentTree.value = res || []
    departmentList.value = flattenTree(departmentTree.value)
  } catch (error) {
    ElMessage.error('获取组织架构失败')
  }
}

// 获取院系树（仅院系，供 el-tree-select 使用）
const getDeptTree = async () => {
  try {
    const res = await organizationApi.getDeptTree()
    deptTreeData.value = res || []
  } catch (error) {
    ElMessage.error('获取院系树失败')
  }
}

// 扁平化树数据
const flattenTree = (tree) => {
  const result = []
  const traverse = (nodes) => {
    nodes.forEach(node => {
      if (!node.isClass) {
        result.push({ id: node.id, name: node.label || node.deptName, orderNum: node.orderNum })
      }
      if (node.children) traverse(node.children)
    })
  }
  traverse(tree)
  return result
}

// 获取班级列表
const getClassList = async () => {
  if (!currentDepartment.value) return
  classLoading.value = true
  try {
    const params = {
      deptId: currentDepartment.value.id,
      page: classPagination.page,
      pageSize: classPagination.pageSize
    }
    const res = await organizationApi.getClasses(params)
    classList.value = res.records || res || []
    classPagination.total = res.total || classList.value.length
  } catch (error) {
    ElMessage.error('获取班级列表失败')
  } finally {
    classLoading.value = false
  }
}

// 点击树节点 - 联动更新搜索参数并触发表格刷新
const handleNodeClick = (data, node) => {
  currentDepartment.value = data

  const isClassNode = typeof data.id === 'string' && data.id.startsWith('class_')

  if (isClassNode) {
    // 点击班级 → 显示该班级的学生
    activeView.value = 'student'
    currentClassId.value = parseInt(data.id.replace('class_', ''))
    loadStudentsByClass()
  } else if (data.parentId == null) {
    // 顶层学院（parent_id 为空）→ 显示系列表，按钮"新增系"
    activeView.value = 'dept'
    loadSubDepts()
  } else {
    // 系（带父级）→ 显示班级列表，按钮"新增班级"
    activeView.value = 'class'
    classPagination.page = 1
    getClassList()
  }
}

// 状态开关
const handleStatusChange = async (row, val) => {
  const newStatus = val ? 1 : 0
  // 添加本地 loading 状态
  row._switchLoading = true
  try {
    await organizationApi.updateClass({ id: row.id, status: newStatus })
    row.status = newStatus
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
  } catch (error) {
    ElMessage.error('状态更新失败')
  } finally {
    row._switchLoading = false
  }
}

// 新增院系（顶层，parentId 为空）
const handleAddDepartment = () => {
  isEditDept.value = false
  deptDialogTitle.value = '新增院系'
  currentDeptId.value = null
  resetDeptForm()
  deptForm.parentId = null
  deptDialogVisible.value = true
}

// 新增系（作为当前选中学院的子院系）
const handleAddSubDept = () => {
  if (!currentDepartment.value) return
  isEditDept.value = false
  deptDialogTitle.value = '新增系'
  currentDeptId.value = null
  resetDeptForm()
  // 取数字 id，避免班级节点 id="class_X" 这种情况误入
  const parentId = typeof currentDepartment.value.id === 'string'
    ? null
    : currentDepartment.value.id
  deptForm.parentId = parentId
  deptDialogVisible.value = true
}

// 右侧面板"新增"按钮分发：dept→新增系，class→新增班级，student→跳学生管理添加
const handleAddInRightPanel = () => {
  if (activeView.value === 'dept') {
    handleAddSubDept()
  } else if (activeView.value === 'class') {
    handleAddClass()
  } else if (activeView.value === 'student') {
    handleAddStudent()
  }
}

// 添加学生：跳转到学生管理页，带上当前班级名，自动打开新增弹窗
const handleAddStudent = () => {
  if (!currentClassId.value) {
    ElMessage.warning('请先选择班级')
    return
  }
  const className = currentDepartment.value?.className || currentDepartment.value?.label || ''
  router.push({
    path: '/student',
    query: { action: 'add', className }
  })
}

// 编辑院系
const handleEditDepartment = (data) => {
  isEditDept.value = true
  deptDialogTitle.value = '编辑院系'
  currentDeptId.value = data.id
  Object.assign(deptForm, {
    deptName: data.deptName || data.label || '',
    orderNum: data.orderNum || 0,
    parentId: data.parentId ?? null
  })
  deptDialogVisible.value = true
}

// 删除院系
const handleDeleteDepartment = (data) => {
  const name = data.deptName || data.label || ''
  ElMessageBox.confirm(`确定要删除院系"${name}" 吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await organizationApi.deleteDepartment(data.id)
      ElMessage.success('删除成功')
      getDepartmentTree()
      getDeptTree()
      if (currentDepartment.value?.id === data.id) {
        currentDepartment.value = null
        classList.value = []
      }
    } catch (error) {
      // 后端真实错误信息已由 request.js 拦截器弹出，这里不再覆盖
    }
  }).catch(() => {})
}

// 从树上删除班级节点（兼容 id="class_X" 这种字符串 id）
const handleDeleteClassNode = (data) => {
  const name = data.className || data.label || ''
  const classId = typeof data.id === 'string' && data.id.startsWith('class_')
    ? parseInt(data.id.replace('class_', ''))
    : data.id
  ElMessageBox.confirm(`确定要删除班级"${name}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await organizationApi.deleteClass(classId)
      ElMessage.success('删除成功')
      getDepartmentTree()
      getDeptTree()
      if (activeView.value === 'class') {
        getClassList()
      }
    } catch (error) {
      // 后端错误信息已由 request.js 弹出
    }
  }).catch(() => {})
}

// 提交院系
const handleDeptSubmit = () => {
  deptFormRef.value?.validate(async (valid) => {
    if (!valid) return
    deptSubmitLoading.value = true
    try {
      const payload = {
        deptName: deptForm.deptName,
        orderNum: deptForm.orderNum,
        parentId: deptForm.parentId
      }
      if (isEditDept.value) {
        await organizationApi.updateDepartment({ ...payload, id: currentDeptId.value })
        ElMessage.success('编辑成功')
      } else {
        await organizationApi.addDepartment(payload)
        ElMessage.success('新增成功')
      }
      deptDialogVisible.value = false
      getDepartmentTree()
      getDeptTree()
    } catch (error) {
      ElMessage.error(isEditDept.value ? '编辑失败' : '新增失败')
    } finally {
      deptSubmitLoading.value = false
    }
  })
}

// 新增班级
const handleAddClass = () => {
  isEditClass.value = false
  classDialogTitle.value = '新增班级'
  currentClassId.value = null
  resetClassForm()
  if (currentDepartment.value) {
    classForm.deptId = currentDepartment.value.id
  }
  classDialogVisible.value = true
}

// 编辑班级
const handleEditClass = (row) => {
  isEditClass.value = true
  classDialogTitle.value = '编辑班级'
  currentClassId.value = row.id
  Object.assign(classForm, {
    deptId: row.deptId,
    className: row.className || '',
    enrollmentYear: row.enrollmentYear || ''
  })
  classDialogVisible.value = true
}

// 删除班级
const handleDeleteClass = (row) => {
  const name = row.className || ''
  ElMessageBox.confirm(`确定要删除班级"${name}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await organizationApi.deleteClass(row.id)
      ElMessage.success('删除成功')
      getClassList()
      getDepartmentTree()
    } catch (error) {
      // 后端错误信息已由 request.js 弹出
    }
  }).catch(() => {})
}

// 提交班级
const handleClassSubmit = () => {
  classFormRef.value?.validate(async (valid) => {
    if (!valid) return
    classSubmitLoading.value = true
    try {
      const payload = {
        deptId: classForm.deptId,
        className: classForm.className,
        enrollmentYear: classForm.enrollmentYear
      }
      if (isEditClass.value) {
        await organizationApi.updateClass({ ...payload, id: currentClassId.value })
        ElMessage.success('编辑成功')
      } else {
        await organizationApi.addClass(payload)
        ElMessage.success('新增成功')
      }
      classDialogVisible.value = false
      getClassList()
      getDepartmentTree()
      getDeptTree()
    } catch (error) {
      ElMessage.error(isEditClass.value ? '编辑失败' : '新增失败')
    } finally {
      classSubmitLoading.value = false
    }
  })
}

// 重置表单
const resetDeptForm = () => {
  Object.assign(deptForm, { deptName: '', orderNum: 0, parentId: null })
  deptFormRef.value?.resetFields()
}

const resetClassForm = () => {
  Object.assign(classForm, { deptId: null, className: '', enrollmentYear: '' })
  classFormRef.value?.resetFields()
}

// 分页
const handleClassSizeChange = (val) => {
  classPagination.pageSize = val
  classPagination.page = 1
  getClassList()
}

const handleClassPageChange = (val) => {
  classPagination.page = val
  getClassList()
}

onMounted(() => {
  getDepartmentTree()
  getDeptTree()
})
</script>

<style scoped>
.organization-management {
  width: 100%;
}

.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
}

.tree-actions {
  display: none;
}

.custom-tree-node:hover .tree-actions {
  display: inline-block;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.ml-2 {
  margin-left: 8px;
}

:deep(.el-tree-node__content) {
  height: 36px;
}
</style>