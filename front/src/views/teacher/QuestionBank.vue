<template>
  <div class="question-bank">
    <!-- 搜索区 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="题型">
          <el-select v-model="searchForm.type" placeholder="全部" clearable class="w-32">
            <el-option label="单选题" value="single" />
            <el-option label="多选题" value="multiple" />
            <el-option label="判断题" value="judge" />
            <el-option label="填空题" value="fill" />
            <el-option label="简答题" value="short" />
            <el-option label="编程题" value="program" />
          </el-select>
        </el-form-item>
        <el-form-item label="科目">
          <el-select v-model="searchForm.subject" placeholder="全部" clearable class="w-32">
            <el-option v-for="s in subjects" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-input v-model="searchForm.knowledge" placeholder="关键词" clearable />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="searchForm.difficulty" placeholder="全部" clearable class="w-28">
            <el-option label="易" value="easy" />
            <el-option label="中" value="medium" />
            <el-option label="难" value="hard" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>查询</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
        </el-form-item>
      </el-form>
      
    </el-card>
    <!-- 隐藏的Word上传 -->
      <el-upload ref="wordUploadRef" action="#" :auto-upload="false" :show-file-list="false" accept=".docx" :on-change="handleWordImport" style="display:none;">
      </el-upload>
      <!-- 隐藏的Excel上传 -->
      <el-upload ref="excelUploadRef" action="#" :auto-upload="false" :show-file-list="false" accept=".xlsx,.xls" :on-change="handleImport" style="display:none;">
      </el-upload>

    <!-- 操作区 -->
    <el-card class="table-card" shadow="never">
      <template #header>
      <div class="flex-between">
        <span>题目列表</span>
        <div class="flex gap-2">
          <el-dropdown @command="handleDownloadTemplate">
            <el-button type="info">
              <el-icon><Download /></el-icon>下载模板<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="excel">下载Excel模板</el-dropdown-item>
                <el-dropdown-item command="word">下载Word模板</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown @command="handleImportFile">
            <el-button type="warning">
              <el-icon><Upload /></el-icon>批量导入<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="excel">Excel导入</el-dropdown-item>
                <el-dropdown-item command="word">Word导入</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增题目
          </el-button>
        </div>
      </div>
    </template>

      <el-table :data="questionList" v-loading="loading" stripe border :header-cell-style="{ background: '#f5f7fa' }">
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column prop="type" label="题型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="typeColor(row.type)">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="题目内容" min-width="250" show-overflow-tooltip />
        <el-table-column prop="subject" label="科目" width="120" />
        <el-table-column prop="knowledge" label="知识点" width="140" show-overflow-tooltip />
        <el-table-column prop="difficulty" label="难度" width="70" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="difficultyColor(row.difficulty)">{{ difficultyLabel(row.difficulty) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分值" width="70" align="center" />
        <el-table-column prop="createdAt" label="创建时间" width="140" />
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)"><el-icon><Edit /></el-icon>编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)"><el-icon><Delete /></el-icon>删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page" v-model:page-size="pagination.pageSize"
          :page-sizes="[10,20,50]" :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange" @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 题目弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-form-item label="题型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择" class="w-full">
            <el-option label="单选题" value="single" />
            <el-option label="多选题" value="multiple" />
            <el-option label="判断题" value="judge" />
            <el-option label="填空题" value="fill" />
            <el-option label="简答题" value="short" />
            <el-option label="编程题" value="program" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目内容" prop="content">
          <el-input v-model="formData.content" type="textarea" :rows="3" placeholder="请输入题目内容" />
        </el-form-item>
        <!-- 单选/多选 -->
        <template v-if="['single','multiple'].includes(formData.type)">
          <el-form-item label="选项">
            <div v-for="(opt, idx) in formData.options" :key="idx" class="flex gap-2 mb-2">
              <el-input v-model="formData.options[idx]" :placeholder="'选项 ' + String.fromCharCode(65+idx)" class="flex-1" />
              <el-button v-if="formData.options.length > 2" link type="danger" @click="removeOption(idx)"><el-icon><Delete /></el-icon></el-button>
            </div>
            <el-button link type="primary" @click="addOption"><el-icon><Plus /></el-icon>添加选项</el-button>
          </el-form-item>
          <el-form-item label="正确答案" prop="answer">
            <el-select v-if="formData.type === 'single'" v-model="formData.answer" placeholder="选择答案" class="w-full">
              <el-option v-for="(opt, idx) in formData.options" :key="idx" :label="String.fromCharCode(65+idx) + '. ' + opt" :value="opt" />
            </el-select>
            <el-select v-else v-model="formData.answer" multiple placeholder="选择答案" class="w-full">
              <el-option v-for="(opt, idx) in formData.options" :key="idx" :label="String.fromCharCode(65+idx) + '. ' + opt" :value="opt" />
            </el-select>
          </el-form-item>
        </template>

        <!-- 判断题 -->
        <template v-if="formData.type === 'judge'">
          <el-form-item label="正确答案" prop="answer">
            <el-radio-group v-model="formData.answer">
              <el-radio label="true">正确</el-radio>
              <el-radio label="false">错误</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>

        <!-- 填空/简答/编程 -->
        <template v-if="['fill','short','program'].includes(formData.type)">
          <el-form-item label="参考答案" prop="answer">
            <el-input v-model="formData.answer" type="textarea" :rows="3" placeholder="请输入参考答案" />
          </el-form-item>
        </template>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="科目" prop="subject">
              <el-input v-model="formData.subject" placeholder="如：软件工程" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="知识点" prop="knowledge">
              <el-input v-model="formData.knowledge" placeholder="如：面向对象" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="难度" prop="difficulty">
              <el-select v-model="formData.difficulty" placeholder="请选择" class="w-full">
                <el-option label="易" value="easy" />
                <el-option label="中" value="medium" />
                <el-option label="难" value="hard" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分值" prop="score">
              <el-input-number v-model="formData.score" :min="1" :max="100" class="w-full" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="解析" prop="analysis">
          <el-input v-model="formData.analysis" type="textarea" :rows="2" placeholder="题目解析（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 题目导入预览弹窗（Word / Excel 共用） -->
    <el-dialog v-model="wordPreviewVisible" title="题目导入预览" width="920px" :close-on-click-modal="false" destroy-on-close @open="onWordPreviewOpen">
      <el-alert
        :closable="false" class="preview-summary"
        :type="wordSummary.error ? 'warning' : 'success'"
        :title="`共解析 ${wordSummary.total} 道题目：正常 ${wordSummary.ok}，警告 ${wordSummary.warn}，错误 ${wordSummary.error}（错误题目不可勾选导入）`" />
      <el-table
        ref="wordPreviewTableRef" :data="wordPreviewRows" border max-height="460"
        row-key="index" :row-class-name="wordRowClass" @selection-change="onWordSelectionChange">
        <el-table-column type="selection" width="44" :selectable="(row) => row.status !== 'error'" />
        <el-table-column prop="index" label="#" width="46" align="center" />
        <el-table-column label="题型" width="78" align="center">
          <template #default="{ row }"><el-tag size="small" :type="typeColor(row.type)">{{ typeLabel(row.type) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="content" label="题目内容" min-width="210" show-overflow-tooltip />
        <el-table-column label="选项" width="56" align="center">
          <template #default="{ row }">{{ row.options.length || '—' }}</template>
        </el-table-column>
        <el-table-column label="答案" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.answerDisplay || '—' }}</template>
        </el-table-column>
        <el-table-column label="难度" width="62" align="center">
          <template #default="{ row }"><el-tag size="small" :type="difficultyColor(row.difficulty)">{{ difficultyLabel(row.difficulty) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="score" label="分值" width="56" align="center" />
        <el-table-column label="状态" min-width="150">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'ok'" type="success" size="small">正常</el-tag>
            <el-tag v-else-if="row.status === 'warn'" type="warning" size="small">警告</el-tag>
            <el-tag v-else type="danger" size="small">错误</el-tag>
            <span v-if="row.reason" class="preview-reason">{{ row.reason }}</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="wordPreviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="wordImporting" :disabled="!wordPreviewSelected.length" @click="confirmWordImport">
          确认导入选中的 {{ wordPreviewSelected.length }} 道
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Upload, Download } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'
import request from '@/api/request.js'
import { ArrowDown } from '@element-plus/icons-vue'
import mammoth from 'mammoth'
const wordUploadRef = ref(null)
const excelUploadRef = ref(null)
const handleImportFile = (command) => {
  if (command === 'excel') {
    const el = excelUploadRef.value?.$el?.querySelector('input[type="file"]')
    if (el) el.click()
  } else if (command === 'word') {
    const el = wordUploadRef.value?.$el?.querySelector('input[type="file"]')
    if (el) el.click()
  }
}
import { Document, Packer, Paragraph, TextRun } from 'docx'
import { saveAs } from 'file-saver'

const handleDownloadTemplate = (command) => {
  if (command === 'excel') {
    const headers = ['题型', '题目内容', '选项', '答案', '科目', '知识点', '难度', '分值', '解析']
    // 覆盖六种题型的示例：选择题答案写选项字母，多选写多个字母；判断写 对/错；填空/简答/编程直接写答案
    const examples = [
      ['单选', 'Java 中 int 类型占几个字节？', '2,4,8,16', 'B', 'Java程序设计', '基本数据类型', '易', '3', 'int 固定占 4 个字节'],
      ['多选', '以下哪些属于 Java 的访问修饰符？', 'public,private,protected,static', 'ABC', 'Java程序设计', '面向对象', '中', '4', 'static 不是访问修饰符'],
      ['判断', 'Java 的类支持多继承。', '', '错', 'Java程序设计', '面向对象', '易', '2', 'Java 类只支持单继承'],
      ['填空', '在 Java 中使用 ____ 关键字定义常量。', '', 'final', 'Java程序设计', '基础语法', '易', '3', ''],
      ['简答', '请简述面向对象的三大基本特征。', '', '封装、继承、多态', 'Java程序设计', '面向对象', '中', '8', ''],
      ['编程', '编写方法判断整数 n 是否为素数。', '', 'public boolean isPrime(int n){ if(n<2) return false; for(int i=2;i*i<=n;i++) if(n%i==0) return false; return true; }', 'Java程序设计', '算法', '难', '15', '']
    ]
    const ws = XLSX.utils.aoa_to_sheet([headers, ...examples])
    ws['!cols'] = [{ wch: 6 }, { wch: 30 }, { wch: 26 }, { wch: 12 }, { wch: 14 }, { wch: 12 }, { wch: 6 }, { wch: 6 }, { wch: 22 }]

    // 填写说明工作表
    const help = [
      ['填写说明（本表不会被导入）'],
      ['1. 题型：单选 / 多选 / 判断 / 填空 / 简答 / 编程，共六种。'],
      ['2. 选项：选择题用英文逗号分隔，如 2,4,8,16；判断/填空/简答/编程留空。'],
      ['3. 答案：单选写选项字母（如 B）；多选写多个字母（如 ABC）；判断写 对/错；填空、简答、编程直接写答案内容。'],
      ['4. 知识点：可填多个，用逗号分隔，如 面向对象,封装。'],
      ['5. 难度：易 / 中 / 难；分值：数字；不填使用默认值。'],
      ['6. 导入时会弹出预览，逐题核对（含按行报错提示）后再确认入库。']
    ]
    const wsHelp = XLSX.utils.aoa_to_sheet(help)
    wsHelp['!cols'] = [{ wch: 80 }]

    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '题目')
    XLSX.utils.book_append_sheet(wb, wsHelp, '填写说明')
    XLSX.writeFile(wb, '题目导入模板.xlsx')
    ElMessage.info('模板已下载：第一个“题目”表填写题目，可参考“填写说明”表')
  } else if (command === 'word') {
    downloadWordDocx()
  }
}

const downloadWordDocx = async () => {
  // 段落工具：text + 可选 加粗/字号(半磅)/颜色/段后间距
  const P = (text, o = {}) => new Paragraph({
    children: [new TextRun({ text, bold: !!o.bold, size: o.size, color: o.color })],
    spacing: { before: o.before || 0, after: o.after != null ? o.after : 80 }
  })
  const SEC = (t) => P(t, { bold: true, size: 26, before: 160, after: 100 })

  const children = []
  // 标题
  children.push(P('题目批量导入模板', { bold: true, size: 32, after: 160 }))

  // 填写说明（导入时自动忽略）
  children.push(P('【填写说明】（导入时本说明会被自动忽略，可保留也可删除）', { bold: true }))
  children.push(P('1. 顶部“科目：xxx”为全文统一科目；个别题目想用其他科目，可在该题任意一行另写“科目：xxx”。'))
  children.push(P('2. 用“一、单选题”“二、多选题”“三、判断题”“四、填空题”“五、简答题”“六、编程题”分组，其下题目自动归入该题型。'))
  children.push(P('3. 每道题以题号开头（如“1、”），选项每行写一个，以“A.”“B.”开头。'))
  children.push(P('4. 答案：单选写选项字母（答案：B）；多选写多个字母（答案：ABD）；判断写“对”或“错”；填空/简答/编程直接写答案内容。'))
  children.push(P('5. 可选字段：难度（易/中/难）、分值、知识点、解析；不填则使用系统默认值。'))
  children.push(P('6. 题号可不连续；导入时会先弹出预览，逐题核对无误后再确认入库。'))

  // 分隔哨兵：此行以下才是题目
  children.push(P('══════════ 以下为题目（请在此横线下方编辑） ══════════', { bold: true, color: '909399', before: 120, after: 160 }))

  // 全文统一科目
  children.push(P('科目：Java程序设计', { after: 160 }))

  // 一、单选题
  children.push(SEC('一、单选题'))
  children.push(P('1、Java 中 int 类型占几个字节？'))
  children.push(P('A. 2')); children.push(P('B. 4')); children.push(P('C. 8')); children.push(P('D. 16'))
  children.push(P('答案：B'))
  children.push(P('难度：易'))
  children.push(P('知识点：基本数据类型'))
  children.push(P('解析：int 在 Java 中固定占 4 个字节。', { after: 160 }))

  // 二、多选题
  children.push(SEC('二、多选题'))
  children.push(P('2、以下哪些属于 Java 的访问修饰符？'))
  children.push(P('A. public')); children.push(P('B. private')); children.push(P('C. protected')); children.push(P('D. static'))
  children.push(P('答案：ABC'))
  children.push(P('难度：中'))
  children.push(P('解析：static 是非访问修饰符。', { after: 160 }))

  // 三、判断题
  children.push(SEC('三、判断题'))
  children.push(P('3、Java 的类支持多继承。'))
  children.push(P('答案：错'))
  children.push(P('解析：Java 类只支持单继承，多继承通过实现多个接口达成。', { after: 160 }))

  // 四、填空题
  children.push(SEC('四、填空题'))
  children.push(P('4、在 Java 中，使用 ______ 关键字来定义常量。'))
  children.push(P('答案：final', { after: 160 }))

  // 五、简答题
  children.push(SEC('五、简答题'))
  children.push(P('5、请简述面向对象的三大基本特征。'))
  children.push(P('答案：封装、继承、多态。'))
  children.push(P('分值：10', { after: 160 }))

  // 六、编程题
  children.push(SEC('六、编程题'))
  children.push(P('6、编写一个方法，判断给定整数是否为素数。'))
  children.push(P('答案：public boolean isPrime(int n){ if(n<2) return false; for(int i=2;i*i<=n;i++) if(n%i==0) return false; return true; }'))
  children.push(P('难度：难'))
  children.push(P('分值：15'))

  const doc = new Document({ sections: [{ properties: {}, children }] })
  const blob = await Packer.toBlob(doc)
  saveAs(blob, '题目导入模板.docx')
  ElMessage.info('模板已下载：按说明填写后，点击“批量导入 → Word导入”即可预览并入库')
}
const handleWordImport = async (file) => {
  try {
    const arrayBuffer = await file.raw.arrayBuffer()
    const result = await mammoth.extractRawText({ arrayBuffer })
    const text = result.value
    
    if (!text || !text.trim()) {
      ElMessage.warning('文件内容为空或格式不正确')
      return
    }

    const rows = parseExamText(text)
    if (!rows.length) {
      ElMessage.warning('未解析到任何题目，请确认题目写在“以下为题目”横线下方，并以题号（如“1、”）开头')
      return
    }
    wordPreviewRows.value = rows
    wordPreviewVisible.value = true
  } catch (error) {
    ElMessage.error('Word解析失败：' + (error?.message || error))
  } finally {
    // 清空已选文件，便于再次选择同一文件
    wordUploadRef.value?.clearFiles?.()
  }
}

/* ============== Word 题目导入：自然格式解析 + 预览入库 ============== */
// 预览弹窗状态
const wordPreviewVisible = ref(false)
const wordPreviewRows = ref([])
const wordPreviewSelected = ref([])
const wordImporting = ref(false)
const wordPreviewTableRef = ref(null)
const onWordSelectionChange = (sel) => { wordPreviewSelected.value = sel }
// 弹窗打开时默认勾选所有“非错误”行
const onWordPreviewOpen = () => {
  nextTick(() => {
    const tbl = wordPreviewTableRef.value
    if (tbl) wordPreviewRows.value.forEach(r => { if (r.status !== 'error') tbl.toggleRowSelection(r, true) })
  })
}
const wordRowClass = ({ row }) => row.status === 'error' ? 'row-error' : (row.status === 'warn' ? 'row-warn' : '')
const wordSummary = computed(() => {
  const rows = wordPreviewRows.value
  return {
    total: rows.length,
    ok: rows.filter(r => r.status === 'ok').length,
    warn: rows.filter(r => r.status === 'warn').length,
    error: rows.filter(r => r.status === 'error').length
  }
})

// 中文题型/难度 ↔ 前端键
const cnTypeToFront = { 单选: 'single', 多选: 'multiple', 判断: 'judge', 填空: 'fill', 简答: 'short', 编程: 'program' }
const cnDiffToFront = { 易: 'easy', 较易: 'easy', 中: 'medium', 较难: 'hard', 难: 'hard' }
// 将题型原始值（中文“单选”/“单选题”或前端键“single”）解析为前端键；无法识别返回 '' 交由自动判断
const resolveFrontType = (raw) => {
  const t = String(raw || '').trim().replace(/题$/, '')
  if (cnTypeToFront[t]) return cnTypeToFront[t]
  if (backendTypeMap[t]) return t
  return ''
}
const JUDGE_TRUE = ['对', '正确', '是', '√', '✓', 't', 'true', 'y', 'yes']
const JUDGE_FALSE = ['错', '错误', '否', '×', '✗', 'x', 'f', 'false', 'n', 'no']
// 行识别正则
const SECTION_RE = /^(?:[一二三四五六七八九十]+\s*[、.．]\s*)?(单选|多选|判断|填空|简答|编程)题\s*$/
const FIELD_RE = /^\s*(答案|难度|分值|知识点|科目|解析)\s*[：:]\s*(.*)$/
const OPTION_RE = /^\s*([A-Za-z])\s*[.．、)）:：]\s*(.+)$/
const QNUM_RE = /^\s*(\d+)\s*[、.．。)）]\s*(.*)$/
const TYPE_TAG_RE = /^\s*[【(\[（]\s*(单选|多选|判断|填空|简答|编程)题?\s*[)\]）】]\s*(.*)$/

const defaultScoreByType = (type) => ({ single: 3, multiple: 4, judge: 2, fill: 3, short: 8, program: 10 }[type] || 5)
const judgeValue = (s) => {
  const t = (s || '').trim().toLowerCase()
  if (!t) return null
  if (JUDGE_TRUE.includes(t)) return 'true'
  if (JUDGE_FALSE.includes(t)) return 'false'
  return null
}

// 答案字母 → 选项内容（库里存“选项内容”，与学生答题、判分逻辑保持一致）
const letterToContents = (ansRaw, options, multi) => {
  const letters = (ansRaw.match(/[A-Za-z]/g) || []).map(c => c.toUpperCase())
  const idxOf = (L) => L.charCodeAt(0) - 65
  if (multi) {
    if (!letters.length) {
      const parts = ansRaw.split(/[,，、\s]+/).map(s => s.trim()).filter(Boolean)
      return parts.length ? { contents: parts, warn: '答案非字母，已按文本处理' } : { contents: [], warn: '答案为空' }
    }
    const contents = []; const seen = new Set(); let bad = false
    letters.forEach(L => {
      const i = idxOf(L)
      if (i >= 0 && i < options.length) { if (!seen.has(i)) { contents.push(options[i]); seen.add(i) } }
      else bad = true
    })
    return { contents, warn: bad ? '部分答案字母超出选项范围' : '' }
  }
  // 单选
  if (!letters.length) {
    const t = ansRaw.trim()
    if (!t) return { contents: [], warn: '答案为空' }
    const m = options.find(o => o.trim().toLowerCase() === t.toLowerCase())
    return { contents: [m || t], warn: m ? '' : '答案与选项不匹配' }
  }
  const i = idxOf(letters[0])
  if (i >= 0 && i < options.length) return { contents: [options[i]], warn: letters.length > 1 ? '单选题答案含多个字母，已取第一个' : '' }
  const t = ansRaw.trim()
  const m = options.find(o => o.trim().toLowerCase() === t.toLowerCase())
  return { contents: [m || t], warn: '答案与选项不匹配' }
}

// 单题归一化 + 校验
const normalizeQuestion = (q, idx, docSubject) => {
  const reasons = []
  let status = 'ok'
  const bump = () => { if (status !== 'error') status = 'warn' }
  const content = q.stemLines.join('\n').trim()
  const ansRaw = (q.answerRaw || '').trim()

  // 题型：显式标签 > 小标题 > 自动判断
  let type = q.type || q.sectionType || ''
  if (!type) {
    if (q.options.length >= 2) {
      const n = (ansRaw.match(/[A-Za-z]/g) || []).length
      type = n >= 2 ? 'multiple' : 'single'
    } else if (judgeValue(ansRaw) !== null) {
      type = 'judge'
    } else if (/_{2,}|（\s*）|\(\s*\)/.test(content)) {
      type = 'fill'
    } else {
      type = 'short'
    }
  }

  // 答案 → 内容数组
  let answerArray = []
  let answerDisplay = ''
  if (type === 'single') {
    const r = letterToContents(ansRaw, q.options, false)
    answerArray = r.contents; if (r.warn) { reasons.push(r.warn); bump() }
    answerDisplay = answerArray.join('')
  } else if (type === 'multiple') {
    const r = letterToContents(ansRaw, q.options, true)
    answerArray = r.contents; if (r.warn) { reasons.push(r.warn); bump() }
    answerDisplay = answerArray.join('、')
    if ((ansRaw.match(/[A-Za-z]/g) || []).length === 1) { reasons.push('多选题答案只有一个选项'); bump() }
  } else if (type === 'judge') {
    const v = judgeValue(ansRaw)
    if (v === null) { reasons.push('判断题答案应为“对/错”'); bump(); answerArray = ansRaw ? [ansRaw] : [] }
    else answerArray = [v]
    answerDisplay = v === 'true' ? '正确' : (v === 'false' ? '错误' : ansRaw)
  } else {
    answerArray = ansRaw ? [ansRaw] : []
    answerDisplay = ansRaw
  }

  // 难度
  let difficulty = 'medium'
  if (q.difficultyRaw) {
    const d = cnDiffToFront[q.difficultyRaw.trim()]
    if (d) difficulty = d
    else { reasons.push('难度无法识别，已用“中”'); bump() }
  }

  // 分值
  let score = defaultScoreByType(type)
  if (q.scoreRaw) {
    const n = parseFloat(String(q.scoreRaw).replace(/[^\d.]/g, ''))
    if (!isNaN(n) && n > 0) score = n
  }

  const knowledge = (q.knowledgeRaw || '').trim()
  const subject = (q.subjectRaw || '').trim() || (docSubject || '').trim()

  // 校验
  if (!content) { reasons.push('题目内容为空'); status = 'error' }
  if ((type === 'single' || type === 'multiple') && q.options.length < 2) { reasons.push('选择题至少需要 2 个选项'); status = 'error' }
  if (answerArray.length === 0) {
    if (type === 'short' || type === 'program') { reasons.push('未填写参考答案'); bump() }
    else { reasons.push('缺少答案'); status = 'error' }
  }

  return {
    index: idx + 1, type, content, options: q.options,
    answerArray, answerDisplay, difficulty, score, knowledge, subject,
    analysis: (q.analysisRaw || '').trim(),
    status, reason: reasons.join('；')
  }
}

// 主解析：自然试卷格式 → 结构化题目
const parseExamText = (rawText) => {
  const text = (rawText || '').replace(/^﻿/, '').replace(/\r\n?/g, '\n')
  let lines = text.split('\n')
  // 跳过“填写说明”区：优先哨兵行，其次第一个题型小标题，否则全文
  const sentinel = lines.findIndex(l => l.includes('以下为题目'))
  if (sentinel >= 0) {
    lines = lines.slice(sentinel + 1)
  } else {
    const h = lines.findIndex(l => SECTION_RE.test(l.replace(/　/g, ' ').trim()))
    if (h > 0) lines = lines.slice(h)
  }

  const list = []
  let cur = null
  let currentType = ''
  let docSubject = ''
  let phase = 'stem'   // stem=收集题干, body=已出现选项/答案

  const startNew = (stem) => {
    cur = { type: currentType, sectionType: currentType, stemLines: [], options: [], answerRaw: '', difficultyRaw: '', scoreRaw: '', knowledgeRaw: '', subjectRaw: '', analysisRaw: '' }
    list.push(cur)
    phase = 'stem'
    let s = stem || ''
    const tag = s.match(TYPE_TAG_RE)   // 题干前的【单选】等显式标签
    if (tag) { cur.type = cnTypeToFront[tag[1]] || cur.type; s = tag[2] || '' }
    if (s.trim()) cur.stemLines.push(s.trim())
  }

  for (const raw of lines) {
    const line = raw.replace(/　/g, ' ').replace(/\s+$/, '')   // 全角空格→空格，去行尾空白
    if (line.trim() === '') continue   // 空行：忽略，不结束当前题

    const secTxt = line.trim()
    if (SECTION_RE.test(secTxt)) { currentType = cnTypeToFront[secTxt.match(SECTION_RE)[1]] || ''; continue }

    const fm = line.match(FIELD_RE)
    if (fm) {
      const key = fm[1]; const val = (fm[2] || '').trim()
      if (key === '科目' && !cur) { docSubject = val; continue }   // 题前的科目=全文科目
      if (!cur) continue
      if (key === '答案') { cur.answerRaw = val; phase = 'body' }
      else if (key === '难度') cur.difficultyRaw = val
      else if (key === '分值') cur.scoreRaw = val
      else if (key === '知识点') cur.knowledgeRaw = val
      else if (key === '科目') cur.subjectRaw = val
      else if (key === '解析') cur.analysisRaw = val
      continue
    }

    const om = line.match(OPTION_RE)
    if (om && cur) { const c = om[2].trim(); if (c) cur.options.push(c); phase = 'body'; continue }

    const qm = line.match(QNUM_RE)
    if (qm) { startNew(qm[2] || ''); continue }   // 题号行：开始新题

    // 普通文本行
    if (!cur) { startNew(line.trim()); continue }
    if (phase === 'stem') cur.stemLines.push(line.trim())   // 多行题干
    else startNew(line.trim())                              // 已有选项/答案后的普通行 → 新题（兼容未编号）
  }

  return list
    .filter(q => q.stemLines.length || q.options.length || q.answerRaw)
    .map((q, i) => normalizeQuestion(q, i, docSubject))
}

// 确认导入：仅导入选中的有效题目，复用 /question/batch-import（与 Excel 导入一致）
const confirmWordImport = async () => {
  const selected = wordPreviewSelected.value.filter(r => r.status !== 'error')
  if (!selected.length) { ElMessage.warning('请至少选择一道有效题目'); return }
  wordImporting.value = true
  try {
    const payload = selected.map(r => ({
      questionType: backendTypeMap[r.type] || 1,
      content: r.content,
      options: r.options.length ? JSON.stringify(r.options) : '[]',
      // 答案统一以 JSON 数组字符串发送：单选/判断/填空=1 个元素，多选=N 个元素 → 后端原样存储
      answer: JSON.stringify(r.answerArray.length ? r.answerArray : ['']),
      subject: r.subject || '',
      // 知识点支持逗号分隔多个标签
      categoryTags: r.knowledge ? JSON.stringify(r.knowledge.split(/[,，]/).map(s => s.trim()).filter(Boolean)) : '[]',
      difficulty: difficultyBackendMap[r.difficulty] || 3,
      score: Number(r.score) || 5,
      analysis: r.analysis || ''
    }))
    await request.post('/question/batch-import', payload)
    ElMessage.success(`成功导入 ${payload.length} 道题目`)
    wordPreviewVisible.value = false
    getList()
  } catch (e) {
    ElMessage.error('导入失败：' + (e?.message || e))
  } finally {
    wordImporting.value = false
  }
}
const subjects = ['软件工程', '数据结构', 'Java程序设计', '计算机网络', '数据库原理']

const searchForm = reactive({ type: '', subject: '', knowledge: '', difficulty: '' })
const pagination = reactive({ page: 1, pageSize: 10, total: 0 })
const questionList = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('新增题目')
const formRef = ref(null)
const submitLoading = ref(false)
const isEdit = ref(false)
const currentId = ref(null)

const formData = reactive({
  type: 'single',
  content: '',
  options: ['', ''],
  answer: '',
  subject: '',
  knowledge: '',
  difficulty: 'medium',
  score: 2,
  analysis: ''
})
const formRules = {
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  content: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
  answer: [{ required: true, message: '请输入答案', trigger: 'blur' }],
  subject: [{ required: true, message: '请输入科目', trigger: 'blur' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }]
}

const typeMap = { single: '单选', multiple: '多选', judge: '判断', fill: '填空', short: '简答', program: '编程' }
const typeColor = (t) => ({ single: 'primary', multiple: 'success', judge: 'warning', fill: 'info', short: '', program: 'danger' }[t] || '')
const typeLabel = (t) => typeMap[t] || t
const diffMap = { easy: '易', medium: '中', hard: '难' }
const difficultyColor = (d) => ({ easy: 'success', medium: 'warning', hard: 'danger' }[d] || '')
const difficultyLabel = (d) => diffMap[d] || d

// 题型映射：后端 1=单选, 2=多选, 3=判断, 4=填空, 5=简答, 6=编程
const backendTypeMap = { single: 1, multiple: 2, judge: 3, fill: 4, short: 5, program: 6 }
const frontendTypeMap = { 1: 'single', 2: 'multiple', 3: 'judge', 4: 'fill', 5: 'short', 6: 'program' }
const difficultyBackendMap = { easy: 1, medium: 3, hard: 5 }
const difficultyFrontendMap = { 1: 'easy', 2: 'easy', 3: 'medium', 4: 'medium', 5: 'hard' }

const getList = async () => {
  loading.value = true
  try {
    const res = await request.get('/question/list', {
      params: {
        page: pagination.page,
        size: pagination.pageSize,
        questionType: searchForm.type ? backendTypeMap[searchForm.type] : null,
        subject: searchForm.subject || null,
        difficulty: searchForm.difficulty ? difficultyBackendMap[searchForm.difficulty] : null
      }
    })
    const records = res.records || []
    questionList.value = records.map(q => {
      // 解析知识点
      let knowledge = ''
      try {
        const tags = JSON.parse(q.categoryTags || '[]')
        knowledge = Array.isArray(tags) ? tags.join(', ') : q.categoryTags
      } catch (e) {
        knowledge = q.categoryTags || ''
      }
      
      // 解析答案：多选回显为内容数组，其余题型为字符串
      const ftype = frontendTypeMap[q.questionType] || 'short'
      let answerArr = []
      try {
        const parsed = JSON.parse(q.answer || '[]')
        answerArr = Array.isArray(parsed) ? parsed.map(a => String(a)) : [String(parsed)]
      } catch (e) {
        answerArr = q.answer ? [String(q.answer)] : []
      }
      const answerVal = ftype === 'multiple' ? answerArr : (answerArr.length ? answerArr[0] : '')

      return {
        id: q.id,
        type: ftype,
        content: q.content,
        options: q.options ? JSON.parse(q.options) : [],
        answer: answerVal,
        subject: q.subject || '',
        knowledge: knowledge,
        analysis: q.analysis || '',
        difficulty: difficultyFrontendMap[q.difficulty] || 'medium',
        score: q.score || 2,
        createdAt: q.createTime ? q.createTime.slice(0, 10) : ''
      }
    })
    pagination.total = res.total || 0
  } catch (e) {
    console.error('获取题目列表失败', e)
  } finally {
    loading.value = false
  }
}
const handleSearch = () => { pagination.page = 1; getList() }
const handleReset = () => { Object.keys(searchForm).forEach(k => searchForm[k] = ''); handleSearch() }
const handleAdd = () => { isEdit.value = false; dialogTitle.value = '新增题目'; currentId.value = null; resetForm(); dialogVisible.value = true }


const handleEdit = (row) => { 
  isEdit.value = true
  dialogTitle.value = '编辑题目'
  currentId.value = row.id
  
  // 先完全重置，再赋值
  Object.assign(formData, {
    type: 'single',
    content: '',
    options: ['', ''],
    answer: '',
    subject: '',
    knowledge: '',
    difficulty: 'medium',
    score: 2,
    analysis: ''
  })
  
  // 再赋值行数据
  const rowData = JSON.parse(JSON.stringify(row))
  Object.assign(formData, rowData)
  
  dialogVisible.value = true 
}


const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除该题目吗？`, '提示', { type: 'warning' }).then(async () => {
    try {
      await request.delete(`/question/${row.id}`)
      ElMessage.success('删除成功')
      getList()
    } catch (e) {
      console.error('删除失败', e)
    }
  }).catch(() => {})
}
const addOption = () => { formData.options.push('') }
const removeOption = (idx) => { formData.options.splice(idx, 1) }
const handleSubmit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      // 答案：多选为内容数组；新增接口收数组、更新接口收 JSON 字符串（Question.answer 为 String）
      const isMulti = formData.type === 'multiple'
      const answerForUpdate = isMulti
        ? JSON.stringify(Array.isArray(formData.answer) ? formData.answer : [])
        : (formData.answer == null ? '' : String(formData.answer))

      const payload = {
        id: isEdit.value ? currentId.value : undefined,  // 编辑时传入ID
        questionType: backendTypeMap[formData.type] || 1,
        content: formData.content,
        options: formData.options ? JSON.stringify(formData.options) : '[]',
        answer: isEdit.value ? answerForUpdate : formData.answer,
        subject: formData.subject,
        // 知识点支持逗号分隔多个标签
        categoryTags: formData.knowledge ? JSON.stringify(String(formData.knowledge).split(/[,，]/).map(s => s.trim()).filter(Boolean)) : '[]',
        difficulty: difficultyBackendMap[formData.difficulty] || 3,
        score: formData.score,
        analysis: formData.analysis || ''
      }
      
      if (isEdit.value) {
        // 编辑模式：调用更新接口
        await request.put('/question/update', payload)
        ElMessage.success('更新成功')
      } else {
        // 新增模式：调用新增接口
        await request.post('/question/add', payload)
        ElMessage.success('新增成功')
      }
      
      dialogVisible.value = false
      getList()
    } catch (e) {
      console.error('保存失败', e)
      // 错误信息已在拦截器中处理
    } finally {
      submitLoading.value = false
    }
  })
}
const resetForm = () => {
  Object.assign(formData, { type: 'single', content: '', options: ['', ''], answer: '', subject: '', knowledge: '', difficulty: 'medium', score: 2, analysis: '' })
  formRef.value?.resetFields()
}
const handleImport = async (file) => {
  try {
    const data = await file.raw.arrayBuffer()
    const workbook = XLSX.read(data, { type: 'array' })
    const firstSheet = workbook.Sheets[workbook.SheetNames[0]]
    const jsonData = XLSX.utils.sheet_to_json(firstSheet, { header: 1 })

    if (jsonData.length < 2) {
      ElMessage.warning('文件内容为空或格式不正确')
      return
    }

    const headers = (jsonData[0] || []).map(h => String(h == null ? '' : h).trim())
    // 取某行中任一别名列的值
    const pick = (obj, ...names) => { for (const n of names) { if (obj[n] != null && String(obj[n]).trim() !== '') return obj[n] } return '' }

    // 逐行解析为与 Word 一致的结构，复用 normalizeQuestion 做归一化与校验
    const rows = []
    for (let i = 1; i < jsonData.length; i++) {
      const raw = jsonData[i] || []
      if (!raw.some(c => String(c == null ? '' : c).trim() !== '')) continue   // 整行空 → 跳过
      const obj = {}
      headers.forEach((h, j) => { obj[h] = raw[j] })

      const typeRaw = String(pick(obj, '题型', 'type'))
      const optStr = String(pick(obj, '选项', 'options'))
      const optionsArr = optStr ? optStr.split(/[,，]/).map(s => s.trim()).filter(Boolean) : []
      const content = String(pick(obj, '题目内容', '题目', 'content')).trim()

      const q = {
        type: resolveFrontType(typeRaw),
        sectionType: resolveFrontType(typeRaw),
        stemLines: content ? [content] : [],
        options: optionsArr,
        answerRaw: String(pick(obj, '答案', 'answer')),
        difficultyRaw: String(pick(obj, '难度', 'difficulty')),
        scoreRaw: String(pick(obj, '分值', 'score')),
        knowledgeRaw: String(pick(obj, '知识点', 'knowledge')),
        subjectRaw: String(pick(obj, '科目', 'subject')),
        analysisRaw: String(pick(obj, '解析', 'analysis'))
      }
      const row = normalizeQuestion(q, rows.length, '')
      row.excelRow = i + 1   // 原始 Excel 行号（含表头）
      if (row.reason) row.reason = `第 ${row.excelRow} 行：${row.reason}`
      rows.push(row)
    }

    if (!rows.length) {
      ElMessage.warning('未解析到任何题目，请检查表头与内容是否符合模板')
      return
    }
    // 复用与 Word 相同的预览弹窗：核对无误后再入库
    wordPreviewRows.value = rows
    wordPreviewVisible.value = true
  } catch (error) {
    ElMessage.error('Excel解析失败：' + (error?.message || error))
  } finally {
    excelUploadRef.value?.clearFiles?.()
  }
}

const handleSizeChange = (val) => { pagination.pageSize = val; pagination.page = 1; getList() }
const handlePageChange = (val) => { pagination.page = val; getList() }
onMounted(() => getList())
</script>

<style scoped>
.question-bank { width: 100%; }
.search-card { margin-bottom: 16px; }
.table-card { margin-bottom: 16px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
.flex { display: flex; }
.gap-2 { gap: 8px; }
.mb-2 { margin-bottom: 8px; }
.flex-1 { flex: 1; }
.w-32 { width: 128px; }
.w-28 { width: 112px; }
:deep(.el-upload) { display: inline-block; }
.preview-summary { margin-bottom: 12px; }
.preview-reason { margin-left: 6px; color: #e6a23c; font-size: 12px; }
:deep(.row-error) { background: #fef0f0; }
:deep(.row-warn) { background: #fdf6ec; }
</style>
