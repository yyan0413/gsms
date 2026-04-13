# 缺陷登记功能设计方案

## 📋 需求概述

### 功能位置
项目管理 → 项目详情 → 缺陷

### 核心功能
1. **缺陷登记**（权限可控）
2. **缺陷列表展示和查询**
3. **缺陷操作**（删除、编辑、办结）
4. **缺陷导出**

---

## 🎯 业务需求

### 1. 缺陷登记

#### 权限控制
- 登记缺陷按钮需要权限控制
- 权限可配置（通过 RBAC 系统）
- 建议权限标识：`defect:create`

#### 登记表单字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 选择需求 | 下拉选择 | ✅ 必填 | 只选择父任务（需求） |
| 缺陷描述 | 文本域 | ✅ 必填 | 详细描述缺陷内容 |
| 责任人 | 下拉选择 | ❌ 非必填 | 从项目成员中选择 |
| 截图 | 文件上传 | ❌ 非必填 | 支持图片上传 |
| 缺陷等级 | 单选 | ✅ 必填 | 致命/严重/一般 |

#### 缺陷等级定义

| 等级 | 说明 | 示例 |
|------|------|------|
| 致命 | 系统崩溃、数据丢失、无法使用 | 应用启动失败、数据库连接断开 |
| 严重 | 主要功能异常、影响使用 | 支付失败、保存失败 |
| 一般 | 次要功能异常、体验问题 | 样式错误、提示信息不准确 |

---

### 2. 缺陷状态

#### 状态定义

| 状态 | 代码 | 说明 |
|------|------|------|
| 未整改 | PENDING | 缺陷刚创建，待处理 |
| 已整改 | FIXED | 缺陷已修复，待验证 |
| 已办结 | CLOSED | 缺陷已验证并关闭 |

#### 状态流转

```
创建 → 未整改（PENDING）
  ↓
整改 → 已整改（FIXED）
  ↓
验证 → 已办结（CLOSED）
```

---

### 3. 缺陷列表

#### 列表字段

| 字段 | 说明 |
|------|------|
| 缺陷ID | 唯一标识 |
| 需求标题 | 关联的父任务 |
| 缺陷描述 | 缺陷详细描述 |
| 缺陷等级 | 致命/严重/一般（带颜色标识） |
| 责任人 | 负责处理的人员 |
| 状态 | 未整改/已整改/已办结 |
| 截图 | 缺陷截图缩略图 |
| 创建时间 | 创建时间 |
| 创建人 | 创建人姓名 |
| 操作 | 编辑/删除/办结按钮 |

#### 筛选条件

| 筛选项 | 类型 | 说明 |
|--------|------|------|
| 责任人 | 下拉选择 | 多选 |
| 创建时间 | 日期范围 | 开始日期-结束日期 |
| 缺陷等级 | 下拉选择 | 多选 |
| 需求 | 下拉选择 | 多选 |
| 状态 | 下拉选择 | 多选 |

#### 列表操作

| 操作 | 权限 | 说明 |
|------|------|------|
| 编辑 | 项目经理 | 修改缺陷信息 |
| 删除 | 项目经理 | 软删除缺陷 |
| 办结 | 项目经理 | 将状态改为"已办结" |

**权限说明**：
- 项目经理：可操作（按钮可点击）
- 其他角色：按钮置灰（disabled）

---

### 4. 缺陷导出

#### 导出功能
- 支持按筛选条件导出
- 导出格式：Excel (.xlsx)
- 导出字段：
  - 缺陷ID
  - 需求标题
  - 缺陷描述
  - 缺陷等级
  - 责任人
  - 状态
  - 创建时间
  - 创建人

---

## 🗄️ 数据库设计

### 缺陷表（gsms_defect）

```sql
CREATE TABLE gsms_defect (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '缺陷ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    parent_task_id BIGINT NOT NULL COMMENT '父任务ID（需求）',
    description TEXT NOT NULL COMMENT '缺陷描述',
    assignee_id BIGINT COMMENT '责任人ID',
    severity VARCHAR(20) NOT NULL COMMENT '缺陷等级：FATAL(致命)、SERIOUS(严重)、NORMAL(一般)',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING(未整改)、FIXED(已整改)、CLOSED(已办结)',
    screenshot_url VARCHAR(500) COMMENT '截图URL',
    creator_id BIGINT NOT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    update_user_id BIGINT COMMENT '更新人ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除：0-有效，1-已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_parent_task_id (parent_task_id),
    INDEX idx_assignee_id (assignee_id),
    INDEX idx_severity (severity),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time),
    INDEX idx_creator_id (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺陷表';
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| project_id | BIGINT | 项目ID，关联 gsms_project |
| parent_task_id | BIGINT | 父任务ID（需求），关联 gsms_task |
| description | TEXT | 缺陷描述，必填 |
| assignee_id | BIGINT | 责任人ID，关联 sys_user |
| severity | VARCHAR(20) | 缺陷等级，枚举值 |
| status | VARCHAR(20) | 状态，枚举值 |
| screenshot_url | VARCHAR(500) | 截图URL，关联 gsms_attachment |
| creator_id | BIGINT | 创建人ID，关联 sys_user |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| update_user_id | BIGINT | 更新人ID |
| is_deleted | TINYINT | 软删除标记 |

---

## 🔧 技术实现

### 后端实现

#### 1. 实体类（Entity）

**Defect.java**
```java
package com.gsms.gsms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("gsms_defect")
public class Defect {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long parentTaskId;
    private String description;
    private Long assigneeId;
    private String severity;  // FATAL, SERIOUS, NORMAL
    private String status;    // PENDING, FIXED, CLOSED
    private String screenshotUrl;
    private Long creatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long updateUserId;
    private Integer isDeleted;
}
```

#### 2. 枚举类

**DefectSeverity.java**（缺陷等级）
```java
package com.gsms.gsms.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DefectSeverity {
    FATAL(1, "致命"),
    SERIOUS(2, "严重"),
    NORMAL(3, "一般");

    @EnumValue
    private final Integer code;
    private final String name;

    DefectSeverity(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @JsonValue
    public String toString() {
        return name();
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
```

**DefectStatus.java**（缺陷状态）
```java
package com.gsms.gsms.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DefectStatus {
    PENDING("PENDING", "未整改"),
    FIXED("FIXED", "已整改"),
    CLOSED("CLOSED", "已办结");

    @EnumValue
    private final String code;
    private final String name;

    DefectStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    @JsonValue
    public String toString() {
        return name();
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
```

#### 3. DTO 类

**DefectCreateReq.java**
```java
package com.gsms.gsms.dto.defect;

import lombok.Data;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;

@Data
public class DefectCreateReq {
    @NotNull(message = "需求ID不能为空")
    private Long parentTaskId;

    @NotBlank(message = "缺陷描述不能为空")
    private String description;

    private Long assigneeId;  // 非必填

    private Long screenshotId;  // 非必填

    @NotNull(message = "缺陷等级不能为空")
    private String severity;  // FATAL, SERIOUS, NORMAL
}
```

**DefectUpdateReq.java**
```java
package com.gsms.gsms.dto.defect;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class DefectUpdateReq {
    @NotNull(message = "缺陷ID不能为空")
    private Long id;

    private Long parentTaskId;
    private String description;
    private Long assigneeId;
    private Long screenshotId;
    private String severity;
    private String status;  // 用于办结操作
}
```

**DefectQueryReq.java**
```java
package com.gsms.gsms.dto.defect;

import com.gsms.gsms.dto.common.BasePageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DefectQueryReq extends BasePageQuery {
    private Long projectId;  // 必填
    private List<Long> assigneeIds;  // 责任人（多选）
    private LocalDateTime createTimeStart;  // 创建时间开始
    private LocalDateTime createTimeEnd;  // 创建时间结束
    private List<String> severities;  // 缺陷等级（多选）
    private List<Long> parentTaskIds;  // 需求（多选）
    private List<String> statuses;  // 状态（多选）
}
```

**DefectInfoResp.java**
```java
package com.gsms.gsms.dto.defect;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DefectInfoResp {
    private Long id;
    private Long projectId;
    private Long parentTaskId;
    private String parentTaskTitle;  // 需求标题
    private String description;
    private Long assigneeId;
    private String assigneeName;  // 责任人姓名
    private String severity;  // FATAL, SERIOUS, NORMAL
    private String severityName;  // 致命, 严重, 一般
    private String status;  // PENDING, FIXED, CLOSED
    private String statusName;  // 未整改, 已整改, 已办结
    private String screenshotUrl;
    private Long creatorId;
    private String creatorName;  // 创建人姓名
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 权限相关
    private Boolean canEdit;  // 是否可编辑（项目经理）
    private Boolean canDelete;  // 是否可删除（项目经理）
    private Boolean canClose;  // 是否可办结（项目经理）
}
```

#### 4. Mapper 接口

**DefectMapper.java**
```java
package com.gsms.gsms.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gsms.gsms.model.entity.Defect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DefectMapper extends BaseMapper<Defect> {

    @Select("SELECT * FROM gsms_defect WHERE project_id = #{projectId} AND is_deleted = 0")
    List<Defect> selectByProjectId(@Param("projectId") Long projectId);

    @Select("SELECT * FROM gsms_defect WHERE parent_task_id = #{parentTaskId} AND is_deleted = 0")
    List<Defect> selectByParentTaskId(@Param("parentTaskId") Long parentTaskId);
}
```

#### 5. Service 接口和实现

**DefectService.java**
```java
package com.gsms.gsms.service;

import com.gsms.gsms.dto.defect.DefectCreateReq;
import com.gsms.gsms.dto.defect.DefectInfoResp;
import com.gsms.gsms.dto.defect.DefectQueryReq;
import com.gsms.gsms.dto.defect.DefectUpdateReq;
import com.gsms.gsms.infra.common.PageResult;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface DefectService {
    /**
     * 创建缺陷
     */
    DefectInfoResp create(DefectCreateReq req, Long currentUserId);

    /**
     * 更新缺陷
     */
    DefectInfoResp update(DefectUpdateReq req, Long currentUserId);

    /**
     * 删除缺陷（软删除）
     */
    void delete(Long id, Long currentUserId);

    /**
     * 办结缺陷
     */
    void close(Long id, Long currentUserId);

    /**
     * 获取缺陷详情
     */
    DefectInfoResp getDetail(Long id, Long currentUserId);

    /**
     * 分页查询缺陷列表
     */
    PageResult<DefectInfoResp> list(DefectQueryReq req, Long currentUserId);

    /**
     * 导出缺陷列表
     */
    void export(DefectQueryReq req, Long currentUserId, HttpServletResponse response);
}
```

#### 6. Controller

**DefectController.java**
```java
package com.gsms.gsms.controller;

import com.gsms.gsms.dto.defect.DefectCreateReq;
import com.gsms.gsms.dto.defect.DefectInfoResp;
import com.gsms.gsms.dto.defect.DefectQueryReq;
import com.gsms.gsms.dto.defect.DefectUpdateReq;
import com.gsms.gsms.infra.common.PageResult;
import com.gsms.gsms.infra.common.Result;
import com.gsms.gsms.service.DefectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/projects/{projectId}/defects")
@Api(tags = "缺陷管理")
public class DefectController {

    @Autowired
    private Defect defectService;

    @PostMapping
    @ApiOperation("创建缺陷")
    public Result<DefectInfoResp> create(
            @PathVariable Long projectId,
            @Valid @RequestBody DefectCreateReq req,
            @RequestHeader("X-User-Id") Long currentUserId) {
        req.setProjectId(projectId);
        DefectInfoResp defect = defectService.create(req, currentUserId);
        return Result.success(defect);
    }

    @PutMapping("/{id}")
    @ApiOperation("更新缺陷")
    public Result<DefectInfoResp> update(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @Valid @RequestBody DefectUpdateReq req,
            @RequestHeader("X-User-Id") Long currentUserId) {
        req.setId(id);
        DefectInfoResp defect = defectService.update(req, currentUserId);
        return Result.success(defect);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除缺陷")
    public Result<Void> delete(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long currentUserId) {
        defectService.delete(id, currentUserId);
        return Result.success();
    }

    @PostMapping("/{id}/close")
    @ApiOperation("办结缺陷")
    public Result<Void> close(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long currentUserId) {
        defectService.close(id, currentUserId);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("获取缺陷详情")
    public Result<DefectInfoResp> getDetail(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long currentUserId) {
        DefectInfoResp defect = defectService.getDetail(id, currentUserId);
        return Result.success(defect);
    }

    @GetMapping
    @ApiOperation("查询缺陷列表")
    public Result<PageResult<DefectInfoResp>> list(
            @PathVariable Long projectId,
            DefectQueryReq req,
            @RequestHeader("X-User-Id") Long currentUserId) {
        req.setProjectId(projectId);
        PageResult<DefectInfoResp> result = defectService.list(req, currentUserId);
        return Result.success(result);
    }

    @GetMapping("/export")
    @ApiOperation("导出缺陷列表")
    public void export(
            @PathVariable Long projectId,
            DefectQueryReq req,
            @RequestHeader("X-User-Id") Long currentUserId,
            HttpServletResponse response) {
        req.setProjectId(projectId);
        defectService.export(req, currentUserId, response);
    }
}
```

---

### 前端实现

#### 1. 路由配置

**router/index.ts**
```typescript
{
  path: '/projects/:projectId',
  component: () => import('@/views/projects/ProjectDetail.vue'),
  children: [
    // ...
    {
      path: 'defects',
      name: 'ProjectDefects',
      component: () => import('@/views/defect/DefectList.vue'),
      meta: { title: '缺陷管理', requiresAuth: true }
    }
  ]
}
```

#### 2. API 封装

**api/defect.ts**
```typescript
import request from './request'

export interface DefectCreateReq {
  parentTaskId: number
  description: string
  assigneeId?: number
  screenshotId?: number
  severity: 'FATAL' | 'SERIOUS' | 'NORMAL'
}

export interface DefectUpdateReq {
  id: number
  parentTaskId?: number
  description?: string
  assigneeId?: number
  screenshotId?: number
  severity?: string
  status?: string
}

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

// 创建缺陷
export const createDefect = (projectId: number, data: DefectCreateReq) => {
  return request.post<any, DefectInfoResp>(`/api/projects/${projectId}/defects`, data)
}

// 更新缺陷
export const updateDefect = (projectId: number, data: DefectUpdateReq) => {
  return request.put<any, DefectInfoResp>(`/api/projects/${projectId}/defects/${data.id}`, data)
}

// 删除缺陷
export const deleteDefect = (projectId: number, id: number) => {
  return request.delete(`/api/projects/${projectId}/defects/${id}`)
}

// 办结缺陷
export const closeDefect = (projectId: number, id: number) => {
  return request.post(`/api/projects/${projectId}/defects/${id}/close`)
}

// 获取缺陷详情
export const getDefectDetail = (projectId: number, id: number) => {
  return request.get<any, DefectInfoResp>(`/api/projects/${projectId}/defects/${id}`)
}

// 查询缺陷列表
export const getDefectList = (params: DefectQueryReq) => {
  return request.get<any, PageResult<DefectInfoResp>>('/api/projects/' + params.projectId + '/defects', { params })
}

// 导出缺陷列表
export const exportDefects = (params: DefectQueryReq) => {
  return request.get('/api/projects/' + params.projectId + '/defects/export', {
    params,
    responseType: 'blob'
  })
}
```

#### 3. 页面组件

**DefectList.vue**（主页面）
```vue
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
              :label="task.title"
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
    <el-table :data="defectList" stripe>
      <el-table-column prop="id" label="缺陷ID" width="100" />
      <el-table-column prop="parentTaskTitle" label="需求" width="200" />
      <el-table-column prop="description" label="缺陷描述" show-overflow-tooltip />
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
            style="width: 60px; height: 60px"
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column prop="creatorName" label="创建人" width="120" />
      <el-table-column label="操作" width="200" fixed="right">
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
    <div class="pagination">
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
              :label="task.title"
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
              :key="user.id"
              :label="user.username"
              :value="user.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="截图">
          <el-upload
            :action="uploadUrl"
            :headers="uploadHeaders"
            :on-success="handleUploadSuccess"
            :file-list="fileList"
            list-type="picture-card"
            :limit="1"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
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
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadUserFile } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import {
  getDefectList,
  createDefect,
  updateDefect,
  deleteDefect,
  closeDefect,
  exportDefects,
  type DefectInfoResp
} from '@/api/defect'
import { getProjectMembers } from '@/api/project'
import { getParentTasks } from '@/api/task'
import { uploadAttachment } from '@/api/attachment'

const route = useRoute()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()

const projectId = computed(() => parseInt(route.params.projectId as string))
const currentUserId = computed(() => authStore.getCurrentUserId())

// 数据定义
const defectList = ref<DefectInfoResp[]>([])
const total = ref(0)
const projectMembers = ref<any[]>([])
const parentTasks = ref<any[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('登记缺陷')
const dateRange = ref<[Date, Date]>()
const fileList = ref<UploadUserFile[]>([])

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
const formData = reactive({
  id: undefined as number | undefined,
  parentTaskId: undefined as number | undefined,
  description: '',
  assigneeId: undefined as number | undefined,
  screenshotId: undefined as number | undefined,
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

// 上传配置
const uploadUrl = computed(() => `/api/projects/${projectId.value}/attachments/upload`)
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${authStore.token}`
}))

// 生命周期
onMounted(() => {
  fetchProjectMembers()
  fetchParentTasks()
  fetchData()
})

// 方法
const fetchData = async () => {
  try {
    const res = await getDefectList({
      projectId: projectId.value,
      ...searchForm
    })
    defectList.value = res.list || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取缺陷列表失败:', error)
    ElMessage.error('获取缺陷列表失败')
  }
}

const fetchProjectMembers = async () => {
  try {
    const res = await getProjectMembers(projectId.value)
    projectMembers.value = res || []
  } catch (error) {
    console.error('获取项目成员失败:', error)
  }
}

const fetchParentTasks = async () => {
  try {
    const res = await getParentTasks(projectId.value)
    parentTasks.value = res || []
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
    screenshotId: undefined,
    severity: row.severity
  })
  dialogVisible.value = true
}

const handleDelete = (row: DefectInfoResp) => {
  ElMessageBox.confirm('确定要删除该缺陷吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await deleteDefect(projectId.value, row.id)
      ElMessage.success('删除成功')
      fetchData()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

const handleClose = (row: DefectInfoResp) => {
  ElMessageBox.confirm('确定要办结该缺陷吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await closeDefect(projectId.value, row.id)
      ElMessage.success('办结成功')
      fetchData()
    } catch (error) {
      ElMessage.error('办结失败')
    }
  })
}

const handleExport = async () => {
  try {
    const blob = await exportDefects({
      projectId: projectId.value,
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

const handleUploadSuccess = (response: any) => {
  if (response.code === 200) {
    formData.screenshotId = response.data.id
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      if (formData.id) {
        await updateDefect(projectId.value, formData)
        ElMessage.success('更新成功')
      } else {
        await createDefect(projectId.value, formData)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      fetchData()
    } catch (error) {
      ElMessage.error(formData.id ? '更新失败' : '创建失败')
    }
  })
}

const resetForm = () => {
  Object.assign(formData, {
    id: undefined,
    parentTaskId: undefined,
    description: '',
    assigneeId: undefined,
    screenshotId: undefined,
    severity: 'NORMAL'
  })
  fileList.value = []
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
```

---

## 🔐 权限控制

### 1. 功能权限

#### 权限定义

| 权限标识 | 权限名称 | 说明 |
|---------|---------|------|
| `defect:create` | 创建缺陷 | 登记缺陷按钮可见性 |
| `defect:edit` | 编辑缺陷 | 编辑按钮可见性和可用性 |
| `defect:delete` | 删除缺陷 | 删除按钮可见性和可用性 |
| `defect:close` | 办结缺陷 | 办结按钮可见性和可用性 |
| `defect:view` | 查看缺陷 | 缺陷列表访问权限 |
| `defect:export` | 导出缺陷 | 导出按钮可见性和可用性 |

#### 权限分配建议

| 角色 | 权限 |
|------|------|
| **项目经理** | defect:create, defect:edit, defect:delete, defect:close, defect:view, defect:export |
| **开发人员** | defect:create, defect:view, defect:export |
| **访客** | defect:view |

### 2. 数据权限

- 只能查看所属项目的缺陷
- 只能选择当前项目的成员作为责任人
- 只能选择当前项目的需求作为父任务

---

## 📝 数据库迁移脚本

**V2.3__create_defect_table.sql**
```sql
-- 创建缺陷表
CREATE TABLE IF NOT EXISTS gsms_defect (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '缺陷ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    parent_task_id BIGINT NOT NULL COMMENT '父任务ID（需求）',
    description TEXT NOT NULL COMMENT '缺陷描述',
    assignee_id BIGINT COMMENT '责任人ID',
    severity VARCHAR(20) NOT NULL COMMENT '缺陷等级：FATAL(致命)、SERIOUS(严重)、NORMAL(一般)',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING(未整改)、FIXED(已整改)、CLOSED(已办结)',
    screenshot_url VARCHAR(500) COMMENT '截图URL',
    creator_id BIGINT NOT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    update_user_id BIGINT COMMENT '更新人ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除：0-有效，1-已删除',
    INDEX idx_project_id (project_id),
    INDEX idx_parent_task_id (parent_task_id),
    INDEX idx_assignee_id (assigneeId),
    INDEX idx_severity (severity),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time),
    INDEX idx_creator_id (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺陷表';

-- 插入权限数据
INSERT INTO permission (permission_name, permission_code, resource_type, description, create_time, update_time, is_deleted)
VALUES
('创建缺陷', 'defect:create', 'defect', '创建缺陷权限', NOW(), NOW(), 0),
('查看缺陷', 'defect:view', 'defect', '查看缺陷权限', NOW(), NOW(), 0),
('编辑缺陷', 'defect:edit', 'defect', '编辑缺陷权限', NOW(), NOW(), 0),
('删除缺陷', 'defect:delete', 'defect', '删除缺陷权限', NOW(), NOW(), 0),
('办结缺陷', 'defect:close', 'defect', '办结缺陷权限', NOW(), NOW(), 0),
('导出缺陷', 'defect:export', 'defect', '导出缺陷权限', NOW(), NOW(), 0);
```

---

## 📊 实现步骤

### 第一阶段：数据库和基础功能
1. ✅ 创建数据库表
2. ✅ 创建实体类和枚举
3. ✅ 创建 DTO 和 Converter
4. ✅ 创建 Mapper 接口
5. ✅ 实现 Service 基础 CRUD

### 第二阶段：业务逻辑
1. ✅ 实现缺陷创建逻辑
2. ✅ 实现缺陷更新逻辑
3. ✅ 实现缺陷删除逻辑
4. ✅ 实现缺陷办结逻辑
5. ✅ 实现权限检查

### 第三阶段：查询和导出
1. ✅ 实现分页查询
2. ✅ 实现多条件筛选
3. ✅ 实现导出功能

### 第四阶段：前端实现
1. ✅ 创建路由和页面
2. ✅ 实现 API 封装
3. ✅ 实现列表展示
4. ✅ 实现创建/编辑表单
5. ✅ 实现导出功能

### 第五阶段：测试和优化
1. ✅ 单元测试
2. ✅ 集成测试
3. ✅ 权限测试
4. ✅ 性能优化

---

## 🎯 UI/UX 设计建议

### 1. 缺陷等级颜色标识

| 等级 | 颜色 | Tag 类型 |
|------|------|---------|
| 致命 | 红色 | danger |
| 严重 | 橙色 | warning |
| 一般 | 灰色 | info |

### 2. 状态颜色标识

| 状态 | 颜色 | Tag 类型 |
|------|------|---------|
| 未整改 | 橙色 | warning |
| 已整改 | 绿色 | success |
| 已办结 | 灰色 | info |

### 3. 列表展示优化
- 缺陷描述支持全文预览（tooltip）
- 截图支持点击放大查看
- 创建时间支持相对时间显示（如"2小时前"）

### 4. 操作按钮权限控制
- 项目经理：按钮可点击
- 其他角色：按钮置灰并显示 tooltip 提示"无权限"

---

## 📚 相关文档

- **RBAC 权限系统**：`docs/RBAC_IMPLEMENTATION.md`
- **项目管理功能**：`docs/PROJECT_MANAGEMENT.md`
- **任务管理功能**：`docs/TASK_MANAGEMENT.md`

---

**文档版本**：v1.0.0
**创建日期**：2026-03-25
**最后更新**：2026-03-25
**状态**：待实现
