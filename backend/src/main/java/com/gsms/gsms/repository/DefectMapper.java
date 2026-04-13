package com.gsms.gsms.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gsms.gsms.model.entity.Defect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 缺陷 Mapper
 */
@Mapper
public interface DefectMapper extends BaseMapper<Defect> {

    /**
     * 根据项目ID查询缺陷列表
     */
    @Select("SELECT * FROM gsms_defect WHERE project_id = #{projectId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<Defect> selectByProjectId(@Param("projectId") Long projectId);

    /**
     * 根据父任务ID查询缺陷列表
     */
    @Select("SELECT * FROM gsms_defect WHERE parent_task_id = #{parentTaskId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<Defect> selectByParentTaskId(@Param("parentTaskId") Long parentTaskId);
}
