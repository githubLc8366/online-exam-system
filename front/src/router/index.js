import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '登录' }
  },
  // 角色专属登录路径 - 自动切换身份页签
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '管理员登录' }
  },
  {
    path: '/teacher/login',
    name: 'TeacherLogin',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '教师登录' }
  },
  {
    path: '/student/login',
    name: 'StudentLogin',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '学生登录' }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { title: '数据统计', roles: ['admin'] }
  },
  {
    path: '/student',
    name: 'Student',
    component: () => import('@/views/Student.vue'),
    meta: { title: '学生管理', roles: ['admin'] }
  },
  {
    path: '/teacher',
    name: 'Teacher',
    component: () => import('@/views/Teacher.vue'),
    meta: { title: '教师管理', roles: ['admin'] }
  },
  {
    path: '/organization',
    name: 'Organization',
    component: () => import('@/views/Organization.vue'),
    meta: { title: '组织架构', roles: ['admin'] }
  },
  {
    path: '/system-log',
    name: 'SystemLog',
    component: () => import('@/views/SystemLog.vue'),
    meta: { title: '系统日志', roles: ['admin'] }
  },
  // 教师端路由
  {
    path: '/teacher-portal',
    redirect: '/teacher-portal/dashboard'
  },
  {
    path: '/teacher-portal/dashboard',
    name: 'TeacherDashboard',
    component: () => import('@/views/teacher/Dashboard.vue'),
    meta: { title: '教师工作台', roles: ['teacher'] }
  },
  {
    path: '/teacher-portal/question-bank',
    name: 'QuestionBank',
    component: () => import('@/views/teacher/QuestionBank.vue'),
    meta: { title: '题库管理', roles: ['teacher'] }
  },
  {
    path: '/teacher-portal/exam-paper',
    name: 'ExamPaper',
    component: () => import('@/views/teacher/ExamPaper.vue'),
    meta: { title: '试卷管理', roles: ['teacher'] }
  },
  {
    path: '/teacher-portal/exam-management',
    name: 'ExamManagement',
    component: () => import('@/views/teacher/ExamManagement.vue'),
    meta: { title: '考试管理', roles: ['teacher'] }
  },
  {
    path: '/teacher-portal/grading',
    name: 'Grading',
    component: () => import('@/views/teacher/Grading.vue'),
    meta: { title: '成绩管理', roles: ['teacher'] }
  },
  // 学生端路由
  {
    path: '/student-portal',
    redirect: '/student-portal/dashboard'
  },
  {
    path: '/student-portal/dashboard',
    name: 'StudentDashboard',
    component: () => import('@/views/student/Dashboard.vue'),
    meta: { title: '待考中心', roles: ['student'] }
  },
  {
    path: '/student-portal/exam/:id',
    name: 'StudentExam',
    component: () => import('@/views/student/Exam.vue'),
    meta: { title: '在线考试', roles: ['student'] }
  },
  {
    path: '/student-portal/history',
    name: 'ExamHistory',
    component: () => import('@/views/student/History.vue'),
    meta: { title: '考试记录', roles: ['student'] }
  },
  {
    path: '/student-portal/wrongbook',
    name: 'WrongBook',
    component: () => import('@/views/student/WrongBook.vue'),
    meta: { title: '错题本', roles: ['student'] }
  },
  {
    path: '/student-portal/profile',
    name: 'StudentProfile',
    component: () => import('@/views/student/Profile.vue'),
    meta: { title: '个人中心', roles: ['student'] }
  },
  {
    path: '/teacher-portal/student',
    name: 'TeacherStudent',
    component: () => import('@/views/teacher/Student.vue'),
    meta: { title: '学生管理', roles: ['teacher'] }
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 判断是否为登录页面路径
const isLoginPage = (path) => {
  return path === '/login' ||
    path === '/admin/login' ||
    path === '/teacher/login' ||
    path === '/student/login'
}

// 路由守卫 - 登录认证 + 权限控制 + 动态标题
router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = `${to.meta.title} - 在线考试系统`
  }

  // 登录状态检查
  const token = localStorage.getItem('token')
  const userRole = localStorage.getItem('user_role')

  // 登录页面直接放行
  if (isLoginPage(to.path)) {
    // 已登录用户跳转到对应主页
    if (token) {
      if (userRole === 'admin') return next('/dashboard')
      if (userRole === 'teacher') return next('/teacher-portal/dashboard')
      return next('/student-portal/dashboard')
    }
    return next()
  }

  // 未登录强制跳转登录
  if (!token) {
    ElMessage.warning('请先登录')
    return next('/login')
  }

  // 权限检查（严格角色匹配）：读取路由 meta 中的 roles 配置
  const requiredRoles = to.meta.roles
  if (requiredRoles && requiredRoles.length > 0) {
    if (!requiredRoles.includes(userRole)) {
      ElMessage.error('权限不足，无法访问此页面')
      // 根据实际角色跳转对应主页
      if (userRole === 'admin') return next('/dashboard')
      if (userRole === 'teacher') return next('/teacher-portal/dashboard')
      if (userRole === 'student') return next('/student-portal/dashboard')
      return next('/login')
    }
  }

  next()
})

export default router
