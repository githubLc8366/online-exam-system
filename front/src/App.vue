<template>
  <div class="app-container">
    <!-- 未登录显示登录页面（无侧边栏） -->
    <router-view v-if="!isLoggedIn" />

    <!-- 已登录显示完整布局 -->
    <template v-else>
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="logo">
        <el-icon :size="24"><Reading /></el-icon>
        <span v-if="!isCollapsed" class="logo-text">考试管理系统</span>
      </div>

      <el-menu
        v-if="!isExamMode"
        :default-active="activeMenu"
        class="sidebar-menu"
        :collapse="isCollapsed"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <!-- 管理员端菜单 -->
        <template v-if="!isTeacherPortal && !isStudentPortal">
          <el-menu-item index="/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>数据统计</template>
          </el-menu-item>
          <el-menu-item index="/student">
            <el-icon><User /></el-icon>
            <template #title>学生管理</template>
          </el-menu-item>
          <el-menu-item index="/teacher">
            <el-icon><UserFilled /></el-icon>
            <template #title>教师管理</template>
          </el-menu-item>
          <el-menu-item index="/organization">
            <el-icon><OfficeBuilding /></el-icon>
            <template #title>组织架构</template>
          </el-menu-item>
          <el-menu-item index="/system-log">
            <el-icon><Document /></el-icon>
            <template #title>系统日志</template>
          </el-menu-item>
        </template>
        <!-- 教师端菜单 -->
        <template v-else-if="isTeacherPortal">
          <el-menu-item index="/teacher-portal/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>工作台</template>
          </el-menu-item>
          <el-menu-item index="/teacher-portal/question-bank">
            <el-icon><Collection /></el-icon>
            <template #title>题库管理</template>
          </el-menu-item>
          <el-menu-item index="/teacher-portal/exam-paper">
            <el-icon><DocumentChecked /></el-icon>
            <template #title>试卷管理</template>
          </el-menu-item>
          <el-menu-item index="/teacher-portal/exam-management">
            <el-icon><AlarmClock /></el-icon>
            <template #title>考试管理</template>
          </el-menu-item>
          <el-menu-item index="/teacher-portal/grading">
            <el-icon><EditPen /></el-icon>
            <template #title>成绩管理</template>
          </el-menu-item>
          <el-menu-item index="/teacher-portal/student">
          <el-icon><User /></el-icon>
              <template #title>学生管理</template>
          </el-menu-item>
        </template>
        <!-- 学生端菜单 -->
        <template v-else-if="isStudentPortal">
          <el-menu-item index="/student-portal/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>待考中心</template>
          </el-menu-item>
          <el-menu-item index="/student-portal/history">
            <el-icon><Tickets /></el-icon>
            <template #title>考试记录</template>
          </el-menu-item>
          <el-menu-item index="/student-portal/wrongbook">
            <el-icon><CircleClose /></el-icon>
            <template #title>错题本</template>
          </el-menu-item>
          <el-menu-item index="/student-portal/profile">
            <el-icon><Setting /></el-icon>
            <template #title>个人中心</template>
          </el-menu-item>
        </template>
      </el-menu>
    </aside>

    <!-- 主体区域 -->
    <div class="main-container">
      <!-- 顶部导航 -->
      <header class="header">
        <div class="header-left">
          <el-icon class="collapse-icon" @click="toggleSidebar">
            <Fold v-if="!isCollapsed" />
            <Expand v-else />
          </el-icon>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item v-if="currentRoute">{{ currentRoute }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right">
            <el-dropdown v-if="!isExamMode">
            <span class="user-info">
              <el-icon><Avatar /></el-icon>
              <span class="nickname">{{ userNickname }}</span>
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <span class="text-gray">{{ roleLabel }} · {{ userAccount }}</span>
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区域 -->
      <main class="content">
        <router-view />
      </main>
    </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Reading,
  DataAnalysis,
  User,
  UserFilled,
  OfficeBuilding,
  Document,
  Collection,
  DocumentChecked,
  AlarmClock,
  EditPen,
  Tickets,
  CircleClose,
  Setting,
  Fold,
  Expand,
  Avatar,
  ArrowDown
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const isCollapsed = ref(false)

// 登录状态：使用 ref + router.afterEach 动态更新
const isLoggedIn = ref(!!localStorage.getItem('token'))

// 用户信息
const userNickname = ref(localStorage.getItem('user_nickname') || '')
const userAccount = ref(localStorage.getItem('user_account') || '')

// 组件挂载时检查登录状态
onMounted(() => {
  refreshUserState()
})

// 每次路由变化后刷新状态
router.afterEach(() => {
  refreshUserState()
})

function refreshUserState() {
  const token = localStorage.getItem('token')
  isLoggedIn.value = !!token
  if (token) {
    userNickname.value = localStorage.getItem('user_nickname') || localStorage.getItem('user_account') || '用户'
    userAccount.value = localStorage.getItem('user_account') || ''
  }
}

// 角色标签
const roleLabel = computed(() => {
  const role = localStorage.getItem('user_role')
  if (role === 'admin') return '管理员'
  if (role === 'teacher') return '教师'
  if (role === 'student') return '学生'
  return '未知用户'
})

const isTeacherPortal = computed(() => route.path.startsWith('/teacher-portal'))
const isStudentPortal = computed(() => route.path.startsWith('/student-portal'))
const isExamMode = computed(() => route.path.startsWith('/student-portal/exam/'))

const activeMenu = computed(() => route.path)

const currentRoute = computed(() => {
  return route.meta?.title || ''
})

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

const handleLogout = () => {
  // 完整清除所有存储
  localStorage.clear()
  sessionStorage.clear()

  // 清除所有cookie
  document.cookie.split(';').forEach(cookie => {
    document.cookie = cookie.replace(/^ +/, '').replace(/=.*/, `=;expires=${new Date(0).toUTCString()};path=/`)
  })

  // 清除Pinia状态（如果存在）
  if (window.__PINIA__) {
    try {
      Object.values(window.__PINIA__.s).forEach(store => store.$reset())
    } catch (e) {}
  }

  // 强制跳转，彻底卸载当前页面上下文
  window.location.href = '/login'
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
}

.app-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* 侧边栏样式 */
.sidebar {
  width: 200px;
  background-color: #E9EEF6;
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
  flex-shrink: 0;
}

.sidebar.collapsed {
  width: 64px;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #303133;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #D0D8E6;
  background-color: #E9EEF6;
}

.logo-text {
  white-space: nowrap;
}

.sidebar-menu {
  border-right: none;
  flex: 1;
  background-color: #E9EEF6 !important;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 200px;
}

.sidebar-menu .el-menu-item {
  color: #606266;
}

.sidebar-menu .el-menu-item:hover {
  background-color: #DDE3ED;
  color: #303133;
}

.sidebar-menu .el-menu-item.is-active {
  background-color: #DCE8F9;
  color: #409EFF;
  font-weight: bold;
}

.sidebar-menu.el-menu--collapse {
  background-color: #E9EEF6 !important;
}

.sidebar-menu.el-menu--collapse .el-menu-item {
  background-color: #E9EEF6 !important;
  color: #606266;
}

.sidebar-menu.el-menu--collapse .el-menu-item:hover {
  background-color: #DDE3ED !important;
  color: #303133;
}

.sidebar-menu.el-menu--collapse .el-menu-item.is-active {
  background-color: #DCE8F9 !important;
  color: #409EFF;
}

.sidebar-menu .el-sub-menu__title {
  color: #606266;
}

.sidebar-menu .el-sub-menu__title:hover {
  background-color: #DDE3ED !important;
  color: #303133;
}

/* 主体区域样式 */
.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: #f0f2f5;
}

/* 顶部导航样式 */
.header {
  height: 60px;
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.collapse-icon {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
}

.collapse-icon:hover {
  color: #409eff;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #606266;
}

.user-info:hover {
  color: #409eff;
}

.nickname {
  font-size: 14px;
  font-weight: 500;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.text-gray {
  color: #909399;
}

/* 内容区域样式 */
.content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    z-index: 100;
    height: 100%;
  }

  .sidebar.collapsed {
    transform: translateX(-100%);
  }
}
</style>
