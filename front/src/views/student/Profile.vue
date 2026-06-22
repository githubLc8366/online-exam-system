<template>
  <div class="student-profile">
    <el-row :gutter="20">
      <!-- 左侧：成绩分析 -->
      <el-col :xs="24" :md="16">
        <el-card shadow="never" class="mb-4">
          <template #header>
            <span><el-icon><TrendCharts /></el-icon> 各科成绩趋势</span>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <span><el-icon><PieChart /></el-icon> 知识点掌握雷达图</span>
          </template>
          <div ref="radarChartRef" class="chart-container"></div>
        </el-card>
      </el-col>

      <!-- 右侧：个人信息与密码修改 -->
      <el-col :xs="24" :md="8">
        <el-card shadow="never">
          <template #header>
            <span><el-icon><User /></el-icon> 个人信息</span>
          </template>
          <div class="text-center mb-4">
            <el-avatar :size="80" :icon="UserFilled" />
            <div class="mt-2 font-bold text-lg">{{ profile.name }}</div>
            <div class="text-gray">{{ profile.studentNo }}</div>
          </div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="班级">{{ profile.className }}</el-descriptions-item>
            <el-descriptions-item label="专业">{{ profile.major }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ profile.phone }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ profile.email }}</el-descriptions-item>
            <el-descriptions-item label="入学年份">{{ profile.enrollmentYear }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never" class="mt-4">
          <template #header>
            <span><el-icon><Lock /></el-icon> 修改密码</span>
          </template>
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleChangePassword" :loading="pwdLoading">确认修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { TrendCharts, PieChart, User, UserFilled, Lock } from '@element-plus/icons-vue'
import request from '@/api/request.js'

const profile = ref({
  name: '',
  studentNo: '',
  className: '',
  major: '',
  phone: '',
  email: '',
  enrollmentYear: ''
})

// 密码修改
const pwdFormRef = ref(null)
const pwdLoading = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.value.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

// 加载用户信息
const loadUserInfo = async () => {
  try {
    const res = await request.get('/user/info')
    if (res) {
      profile.value = {
        name: res.nickname || '',
        studentNo: res.userNo || '',
        className: res.className || '未设置',
        major: res.deptName || '未设置',
        phone: res.phone || '未设置',
        email: res.email || '未设置',
        enrollmentYear: res.enrollmentYear || '未设置'
      }
    }
  } catch (e) {
    console.error('获取用户信息失败', e)
  }
}

// 修改密码
const handleChangePassword = async () => {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  
  pwdLoading.value = true
  try {
    await request.put('/user/password', {
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    // 清除登录状态
    localStorage.clear()
    sessionStorage.clear()
    window.location.href = '/login'
  } catch (e) {
    console.error('修改密码失败', e)
  } finally {
    pwdLoading.value = false
  }
}

// 图表引用
const trendChartRef = ref(null)
const radarChartRef = ref(null)

onMounted(async () => {
  loadUserInfo()

  // 加载成绩趋势
  try {
    const trendRes = await request.get('/grade/student-trend')
    if (trendChartRef.value && trendRes) {
      const chart = echarts.init(trendChartRef.value)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: trendRes.subjects || [] },
        yAxis: { type: 'value', max: 100 },
        series: [{
          name: '成绩',
          type: 'line',
          smooth: true,
          data: trendRes.scores || [],
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(64,158,255,0.3)' },
              { offset: 1, color: 'rgba(64,158,255,0.05)' }
            ])
          },
          itemStyle: { color: '#409eff' }
        }]
      })
    }
  } catch (e) {
    console.error('获取成绩趋势失败', e)
  }

  // 加载知识点雷达图（数字显示在顶点外侧，知识点名称显示在内侧，避免重叠）
  try {
    const radarRes = await request.get('/grade/knowledge-radar')
    if (radarChartRef.value && radarRes) {
      const chart = echarts.init(radarChartRef.value)
      const knowledge = radarRes.knowledge || []
      const mastery = radarRes.mastery || []

      // 没有数据时显示提示，不渲染雷达图
    if (knowledge.length === 0) {
      chart.setOption({
        title: {
          show: true,
          text: '暂无错题数据，参加考试后将自动生成雷达图',
          left: 'center',
          top: 'center',
          textStyle: { color: '#909399', fontSize: 14 }
        }
      })
      return
    }
      const indicators = knowledge.map(k => ({ name: k, max: 100 }))
      // 顶点名称 → 分值映射，供 axisName 在顶点外侧显示数字
      const nameToValue = {}
      knowledge.forEach((k, i) => { nameToValue[k] = mastery[i] })

      chart.setOption({
        tooltip: {
          trigger: 'item',
          confine: true,        // 限制提示框在图表容器内，避免被边缘裁剪
          appendToBody: true,   // 渲染到 body，逃出父级 overflow 截断
          formatter: () => knowledge.map((k, i) => `${k}：${mastery[i] ?? 0}`).join('<br/>')
        },
        radar: {
          indicator: indicators,
          center: ['50%', '54%'],
          radius: '60%',
          splitNumber: 4,
          // 顶点外侧（远离中心）：显示掌握度数字
          axisName: {
            color: '#67c23a',
            fontSize: 14,
            fontWeight: 'bold',
            formatter: (name) => {
              const v = nameToValue[name]
              return v != null ? `${v}` : ''
            }
          },
          axisLine: { lineStyle: { color: '#dcdfe6' } },
          splitLine: { lineStyle: { color: '#e4e7ed' } },
          splitArea: { areaStyle: { color: ['#ffffff', '#f7f9fc'] } }
        },
        series: [{
          type: 'radar',
          symbol: 'circle',
          symbolSize: 5,
          data: [{
            value: mastery,
            name: '掌握程度',
            areaStyle: { color: 'rgba(103,194,58,0.3)' },
            itemStyle: { color: '#67c23a' },
            lineStyle: { color: '#67c23a' },
            // 数据点内侧：显示知识点名称
            label: {
              show: true,
              color: '#606266',
              fontSize: 12,
              formatter: (params) => knowledge[params.dimensionIndex] ?? ''
            }
          }]
        }]
      })
    }
  } catch (e) {
    console.error('获取知识点掌握度失败', e)
  }
})
</script>

<style scoped>
.student-profile { width: 100%; }
.chart-container { width: 100%; height: 300px; }
.text-center { text-align: center; }
.font-bold { font-weight: bold; }
.text-lg { font-size: 18px; }
.text-gray { color: #909399; }
.mt-2 { margin-top: 8px; }
.mt-4 { margin-top: 16px; }
.mb-4 { margin-bottom: 16px; }
</style>
