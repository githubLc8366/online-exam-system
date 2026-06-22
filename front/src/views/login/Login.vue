<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <template #header>
        <div class="text-center">
          <el-icon :size="48" color="#409EFF"><Reading /></el-icon>
          <h2 class="mt-2">在线考试系统</h2>
          <p class="text-gray text-sm">Online Examination System</p>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" class="login-form">
        <el-form-item prop="account">
          <el-input
            v-model="form.account"
            placeholder="请输入账号"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item prop="role">
          <el-radio-group v-model="form.role" size="large" class="w-full flex justify-center">
            <el-radio-button label="admin">管理员</el-radio-button>
            <el-radio-button label="teacher">教师</el-radio-button>
            <el-radio-button label="student">学生</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" class="w-full" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Reading, User, Lock } from '@element-plus/icons-vue'
import request from '@/api/request.js'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  account: '',
  password: '',
  role: 'admin'
})

const rules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

// 从 URL 路径自动识别角色: /admin/login, /teacher/login, /student/login
onMounted(() => {
  const path = route.path
  if (path.startsWith('/admin')) form.role = 'admin'
  else if (path.startsWith('/teacher')) form.role = 'teacher'
  else if (path.startsWith('/student')) form.role = 'student'
})

const handleLogin = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    loading.value = true

    try {
      const res = await request.post('/login', {
        userNo: form.account,
        password: form.password,
        role: form.role
      })

      // 保存登录状态
      localStorage.setItem('token', res.token)
      localStorage.setItem('user_role', res.userInfo.role)
      localStorage.setItem('user_account', res.userInfo.userNo)
      localStorage.setItem('user_info', JSON.stringify(res.userInfo))
      localStorage.setItem('user_nickname', res.userInfo.nickname || '')

      ElMessage.success('登录成功')

      // 先设置好所有 localStorage 数据，再用 router.push 跳转
      // App.vue 的 router.afterEach 会检测到登录状态变化
      const role = res.userInfo.role
      if (role === 'admin') {
        router.push('/dashboard')
      } else if (role === 'teacher') {
        router.push('/teacher-portal/dashboard')
      } else {
        router.push('/student-portal/dashboard')
      }
    } catch (error) {
      // 错误已在响应拦截器中处理
      // 如果是"用户不存在"或"密码错误"，拦截器已显示对应的 msg
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #E9EEF6;
}
.login-card {
  width: 333px;
  border-radius: 12px;
}
.login-form {
  padding: 10px 0;
}
.text-center {
  text-align: center;
}
.mt-2 {
  margin-top: 8px;
}
.text-gray {
  color: #909399;
}
.text-sm {
  font-size: 13px;
}
.w-full {
  width: 100%;
}
.flex {
  display: flex;
}
.justify-center {
  justify-content: center;
}
</style>
