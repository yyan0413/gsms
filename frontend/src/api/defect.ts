import request from './request'

/**
 * 缺陷创建请求
 */
export interface DefectCreateReq {
  parentTaskId: number
  description: string
  assigneeId?: number
  screenshotId?: number
  severity: 'FATAL' | 'SERIOUS' | 'NORMAL'
}

/**
 * 缺陷更新请求
 */
export interface DefectUpdateReq {
  id: number
  parentTaskId?: number
  description?: string
  assigneeId?: number
  screenshotId?: number
  severity?: string
  status?: string
}

/**
 * 缺陷查询请求
 */
export interface DefectQueryReq {
  projectId: number
  assigneeIds?: number[]
  createTimeStart?: string
  createTimeEnd?: string
  severities?: string[]
  parentTaskIds?: number[]
  statuses?: string[]
  pageNum: number
  pageSize: number
}

/**
 * 缺陷信息响应
 */
export interface DefectInfoResp {
  id: number
  projectId: number
  parentTaskId: number
  parentTaskTitle: string
  description: string
  assigneeId?: number
  assigneeName?: string
  severity: string
  severityName: string
  status: string
  statusName: string
  screenshotUrl?: string
  creatorId: number
  creatorName: string
  createTime: string
  updateTime: string
  canEdit: boolean
  canDelete: boolean
  canClose: boolean
}

/**
 * 创建缺陷
 */
export const createDefect = (projectId: number, data: DefectCreateReq) => {
  return request.post<any, DefectInfoResp>(`/projects/${projectId}/defects`, data)
}

/**
 * 更新缺陷
 */
export const updateDefect = (projectId: number, data: DefectUpdateReq) => {
  return request.put<any, DefectInfoResp>(`/projects/${projectId}/defects/${data.id}`, data)
}

/**
 * 删除缺陷
 */
export const deleteDefect = (projectId: number, id: number) => {
  return request.delete(`/projects/${projectId}/defects/${id}`)
}

/**
 * 办结缺陷
 */
export const closeDefect = (projectId: number, id: number) => {
  return request.post(`/projects/${projectId}/defects/${id}/close`)
}

/**
 * 获取缺陷详情
 */
export const getDefectDetail = (projectId: number, id: number) => {
  return request.get<any, DefectInfoResp>(`/projects/${projectId}/defects/${id}`)
}

/**
 * 查询缺陷列表
 */
export const getDefectList = (params: DefectQueryReq) => {
  return request.get<any, PageResult<DefectInfoResp>>(`/projects/${params.projectId}/defects`, { params })
}

/**
 * 导出缺陷列表
 */
export const exportDefects = (params: DefectQueryReq) => {
  return request.get(`/projects/${params.projectId}/defects/export`, {
    params,
    responseType: 'blob'
  })
}
