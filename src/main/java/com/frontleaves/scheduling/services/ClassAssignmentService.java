package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.ClassAssignmentDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.scheduling.AutomaticClassSchedulingBaseDTO;
import com.frontleaves.scheduling.models.dto.scheduling.ScheduleResultDTO;
import com.frontleaves.scheduling.models.vo.ClassAssignmentVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 排课分配服务接口
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Service
public interface ClassAssignmentService {

    /**
     * 新增排课分配
     *
     * @param vo 排课分配请求对象
     */
    void add(ClassAssignmentVO vo);

    /**
     * 删除排课分配
     *
     * @param classAssignmentUuid 排课分配主键
     */
    void delete(String classAssignmentUuid);

    /**
     * 更新排课分配
     *
     * @param classAssignmentUuid 排课分配主键
     * @param vo                  排课分配请求对象
     */
    void update(String classAssignmentUuid, ClassAssignmentVO vo);

    /**
     * 根据主键查询排课分配
     *
     * @param classAssignmentUuid 排课分配主键
     * @return 排课分配数据传输对象
     */
    ClassAssignmentDTO getById(String classAssignmentUuid);

    /**
     * 分页查询排课分配
     *
     * @param page         页码
     * @param size         每页大小
     * @param semesterUuid 学期主键（可选）
     * @param courseUuid   课程主键（可选）
     * @param teacherUuid  教师主键（可选）
     * @return 分页结果
     */
    PageDTO<ClassAssignmentDTO> page(Integer page, Integer size, String semesterUuid, String courseUuid, String teacherUuid);

    /**
     * 查询排课分配列表
     *
     * @param semesterUuid 学期主键（可选）
     * @param courseUuid   课程主键（可选）
     * @param teacherUuid  教师主键（可选）
     * @return 排课分配列表
     */
    List<ClassAssignmentDTO> list(String semesterUuid, String courseUuid, String teacherUuid);

    /**
     * 保存排课分配
     *
     * @param result 排课结果数据传输对象
     */
    void saveClassAssignment(ScheduleResultDTO result);

    /**
     * 获取排课分配列表 根据限制
     * @param automaticClassSchedulingBaseDTO 自动排课基础DTO
     * @return 排课分配列表
     */
    List<ClassAssignmentDTO> getClassAssignmentListByLimit(
            AutomaticClassSchedulingBaseDTO automaticClassSchedulingBaseDTO);

}
