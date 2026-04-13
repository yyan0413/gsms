<template>
  <div class="defect-list">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">缺陷管理</h2>
        <el-button
          type="primary"
          :icon="Plus"
          @click="handleCreate"
          v-if="canCreateDefect"
        >
          登记缺陷
        </el-button>
      </div>
      <div class="header-right">
        <el-button :icon="Download" @click="handleExport">导出</el-button>
      </div>
    </div>

    <!-- 筛选条件 -->
    <div class="filter-section">
      <el-form :model="searchForm" inline>
        <el-form-item label="责任人">
          <el-select
            v-model="searchForm.assigneeIds"
            multiple
            placeholder="请选择责任人"
            clearable
            style="width: 200px"
          >
            <el-option
              v-for="user in projectMembers"
              :key="user.id"
              :label="user.username"
              :value="user.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="创建时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            @change="handleDateChange"
          />
        </el-form-item>

        <el-form-item label="缺陷等级">
          <el-select
            v-model="searchForm.severities"
            multiple
            placeholder="请选择缺陷等级"
            clearable
            style="width: 200px"
          >
            <el-option label="致命" value="FATAL" />
            <el-option label="严重" value="SERIOUS" />
            <el-option label="一般" value="NORMAL" />
          </el-select>
        </el-form-item>

        <el-form-item label="需求">
          <el-select
            v-model="searchForm.parentTaskIds"
            multiple
            placeholder="请选择需求"
            clearable
            style="width: 200px"
          >
            <el-option
              v-for="task in parentTasks"
              :key="task.id"
              :label="task.assigneeName ? `${task.title} - ${task.assigneeName}` : task.title"
              :value="task.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select
            v-model="searchForm.statuses"
            multiple
            placeholder="请选择状态"
            clearable
            style="width: 200px"
          >
            <el-option label="未整改" value="PENDING" />
            <el-option label="已整改" value="FIXED" />
            <el-option label="已办结" value="CLOSED" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 缺陷列表 -->
    <el-table :data="defectList" stripe v-loading="loading">
      <el-table-column prop="id" label="缺陷ID" width="100" />
      <el-table-column prop="parentTaskTitle" label="需求" width="200" show-overflow-tooltip />
      <el-table-column prop="description" label="缺陷描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="severityName" label="缺陷等级" width="100">
        <template #default="{ row }">
          <el-tag :type="getSeverityType(row.severity)">
            {{ row.severityName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="assigneeName" label="责任人" width="120" />
      <el-table-column prop="statusName" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">
            {{ row.statusName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="screenshotUrl" label="截图" width="100">
        <template #default="{ row }">
          <el-image
            v-if="row.screenshotUrl"
            :src="row.screenshotUrl"
            :preview-src-list="[row.screenshotUrl]"
            fit="cover"
            style="width: 60px; height: 60px; border-radius: 4px;"
          />
          <span v-else style="color: #909399;">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column prop="creatorName" label="创建人" width="120" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button
            type="primary"
            size="small"
            @click="handleEdit(row)"
            :disabled="!row.canEdit"
          >
            编辑
          </el-button>
          <el-button
            type="danger"
            size="small"
            @click="handleDelete(row)"
            :disabled="!row.canDelete"
          >
            删除
          </el-button>
          <el-button
            type="success"
            size="small"
            @click="handleClose(row)"
            :disabled="!row.canClose || row.status === 'CLOSED'"
          >
            办结
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="searchForm.pageNum"
        v-model:page-size="searchForm.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </div>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="需求" prop="parentTaskId">
          <el-select
            v-model="formData.parentTaskId"
            placeholder="请选择需求"
            style="width: 100%"
          >
            <el-option
              v-for="task in parentTasks"
              :key="task.id"
              :label="task.assigneeName ? `${task.title} - ${task.assigneeName}` : task.title"
              :value="task.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="缺陷描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="4"
            placeholder="请输入缺陷描述"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="责任人">
          <el-select
            v-model="formData.assigneeId"
            placeholder="请选择责任人"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="user in projectMembers"
              :key="user.userId"
              :label="user.nickname || `用户${user.userId}`"
              :value="user.userId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="缺陷等级" prop="severity">
          <el-radio-group v-model="formData.severity">
            <el-radio label="FATAL">致命</el-radio>
            <el-radio label="SERIOUS">严重</el-radio>
            <el-radio label="NORMAL">一般</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import type { PageResult } from '@/types'
import {
  getDefectList,
  createDefect,
  updateDefect,
  deleteDefect,
  closeDefect,
  exportDefects,
  type DefectInfoResp,
  type DefectCreateReq,
  type DefectUpdateReq
} from '@/api/defect'
import { getProjectMembers } from '@/api/project'
import { getParentTasks } from '@/api/task'
import type { UserInfo } from '@/api/user'

// Props
const props = defineProps<{
  projectId: number
}>()

const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const currentUserId = computed(() => authStore.getCurrentUserId())

// 数据定义
const defectList = ref<DefectInfoResp[]>([])
const total = ref(0)
const projectMembers = ref<UserInfo[]>([])
const parentTasks = ref<any[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('登记缺陷')
const dateRange = ref<[Date, Date]>()
const loading = ref(false)
const submitting = ref(false)

// 搜索表单
const searchForm = reactive({
  assigneeIds: [] as number[],
  createTimeStart: '',
  createTimeEnd: '',
  severities: [] as string[],
  parentTaskIds: [] as number[],
  statuses: [] as string[],
  pageNum: 1,
  pageSize: 10
})

// 表单数据
const formData = reactive<DefectCreateReq & { id?: number }>({
  parentTaskId: undefined as unknown as number,
  description: '',
  assigneeId: undefined,
  severity: 'NORMAL'
})

// 表单验证规则
const formRules: FormRules = {
  parentTaskId: [{ required: true, message: '请选择需求', trigger: 'change' }],
  description: [{ required: true, message: '请输入缺陷描述', trigger: 'blur' }],
  severity: [{ required: true, message: '请选择缺陷等级', trigger: 'change' }]
}

// 权限检查
const canCreateDefect = computed(() => {
  // TODO: 从权限系统获取
  return true
})

// 生命周期
onMounted(() => {
  fetchProjectMembers()
  fetchParentTasks()
  fetchData()
})

// 方法
const fetchData = async () => {
  try {
    loading.value = true
    const res = await getDefectList({
      projectId: props.projectId,
      ...searchForm
    })
    defectList.value = res.data || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取缺陷列表失败:', error)
    ElMessage.error('获取缺陷列表失败')
  } finally {
    loading.value = false
  }
}

const fetchProjectMembers = async () => {
  try {
    const res = await getProjectMembers(props.projectId)
    projectMembers.value = res || []
  } catch (error) {
    console.error('获取项目成员失败:', error)
  }
}

const fetchParentTasks = async () => {
  try {
    const res = await getParentTasks(props.projectId)
    // 后端返回 PageResult 格式，需要取 list 字段
    parentTasks.value = res?.list || []
  } catch (error) {
    console.error('获取需求列表失败:', error)
  }
}

const handleDateChange = (dates: [Date, Date] | null) => {
  if (dates) {
    searchForm.createTimeStart = dates[0].toISOString()
    searchForm.createTimeEnd = dates[1].toISOString()
  } else {
    searchForm.createTimeStart = ''
    searchForm.createTimeEnd = ''
  }
}

const handleSearch = () => {
  searchForm.pageNum = 1
  fetchData()
}

const handleReset = () => {
  searchForm.assigneeIds = []
  searchForm.createTimeStart = ''
  searchForm.createTimeEnd = ''
  searchForm.severities = []
  searchForm.parentTaskIds = []
  searchForm.statuses = []
  dateRange.value = undefined
  handleSearch()
}

const handleCreate = () => {
  dialogTitle.value = '登记缺陷'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: DefectInfoResp) => {
  dialogTitle.value = '编辑缺陷'
  Object.assign(formData, {
    id: row.id,
    parentTaskId: row.parentTaskId,
    description: row.description,
    assigneeId: row.assigneeId,
    severity: row.severity
  })

  dialogVisible.value = true
}

const handleDelete = (row: DefectInfoResp) => {
  ElMessageBox.confirm('确定要删除该缺陷吗？', '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await deleteDefect(props.projectId, row.id)
      ElMessage.success('删除成功')
      fetchData()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const handleClose = (row: DefectInfoResp) => {
  ElMessageBox.confirm('确定要办结该缺陷吗？', '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await closeDefect(props.projectId, row.id)
      ElMessage.success('办结成功')
      fetchData()
    } catch (error) {
      ElMessage.error('办结失败')
    }
  }).catch(() => {})
}

const handleExport = async () => {
  try {
    const blob = await exportDefects({
      projectId: props.projectId,
      ...searchForm
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `缺陷列表_${new Date().getTime()}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      submitting.value = true
      if (formData.id) {
        await updateDefect(props.projectId, formData as DefectUpdateReq)
        ElMessage.success('更新成功')
      } else {
        await createDefect(props.projectId, formData as DefectCreateReq)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      fetchData()
    } catch (error: any) {
      ElMessage.error(error.message || (formData.id ? '更新失败' : '创建失败'))
    } finally {
      submitting.value = false
    }
  })
}

const resetForm = () => {
  Object.assign(formData, {
    id: undefined,
    parentTaskId: undefined as unknown as number,
    description: '',
    assigneeId: undefined,
    severity: 'NORMAL'
  })
  formRef.value?.resetFields()
}

const getSeverityType = (severity: string) => {
  const map: Record<string, string> = {
    FATAL: 'danger',
    SERIOUS: 'warning',
    NORMAL: 'info'
  }
  return map[severity] || ''
}

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    PENDING: 'warning',
    FIXED: 'success',
    CLOSED: 'info'
  }
  return map[status] || ''
}
</script>

<style scoped>
.defect-list {
  min-height: calc(100vh - 160px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
  color: #333;
}

.filter-section {
  margin-bottom: 24px;
  padding: 20px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 20px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}
</style>
