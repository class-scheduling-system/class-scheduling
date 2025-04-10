package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherCourseQualificationDTO;
import com.frontleaves.scheduling.models.dto.merge.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.vo.TeacherCourseQualificationQueryVO;
import com.frontleaves.scheduling.models.vo.TeacherCourseQualificationVO;

import java.util.List;

/**
 * 教师课程资格服务接口
 * <p>
 * 该接口定义了处理教师课程资格相关业务的方法。
 * </p>
 *
 * @author FLASHLACK | xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
public interface TeacherCourseQualificationService {
    /**
     * 获取课程库和教师课程资格的所有关联信息
     *
     * @param courseLibraryDOList 课程库数据对象列表
     * @param isTeacherPreferences 是否包含教师偏好
     * @return 包含课程库和教师课程资格信息的DTO列表
     */
    List<CourseLibraryAndTeacherCourseQualificationListDTO>
    getCourseLibraryAndTeacherCourseQualificationList(
            List<CourseLibraryAndTeacherCourseQualificationListDTO> courseLibraryDOList,
            Boolean isTeacherPreferences
    );
    
    /**
     * 分页获取教师课程资格列表
     *
     * @param page 页码
     * @param size 每页大小
     * @param isDesc 是否降序排序
     * @param queryVO 查询条件
     * @return 分页结果
     */
    PageDTO<TeacherCourseQualificationDTO> getTeacherCourseQualificationList(
            Integer page, Integer size, Boolean isDesc, TeacherCourseQualificationQueryVO queryVO
    );
    
    /**
     * 根据条件获取教师课程资格列表（不分页）
     *
     * @param queryVO 查询条件
     * @return 教师课程资格列表
     */
    List<TeacherCourseQualificationDTO> getTeacherCourseQualificationSimpleList(
            TeacherCourseQualificationQueryVO queryVO
    );
    
    /**
     * 根据资格UUID获取教师课程资格详情
     *
     * @param qualificationUuid 资格UUID
     * @return 教师课程资格详情
     */
    TeacherCourseQualificationDTO getTeacherCourseQualification(String qualificationUuid);
    
    /**
     * 添加教师课程资格
     *
     * @param vo 教师课程资格信息
     * @return 添加成功的教师课程资格UUID
     */
    String addTeacherCourseQualification(TeacherCourseQualificationVO vo);
    
    /**
     * 更新教师课程资格
     *
     * @param qualificationUuid 资格UUID
     * @param vo 教师课程资格信息
     */
    void updateTeacherCourseQualification(String qualificationUuid, TeacherCourseQualificationVO vo);
    
    /**
     * 删除教师课程资格
     *
     * @param qualificationUuid 资格UUID
     */
    void deleteTeacherCourseQualification(String qualificationUuid);
    
    /**
     * 审核教师课程资格
     *
     * @param qualificationUuid 资格UUID
     * @param status 审核状态（1:通过 2:驳回）
     * @param remarks 审核备注
     * @param approvedBy 审核人
     */
    void approveTeacherCourseQualification(
            String qualificationUuid, Integer status, String remarks, String approvedBy
    );
    
    /**
     * 申请教师课程资格
     * <p>
     * 与添加教师课程资格不同，申请的资格状态为待审核(0)
     * </p>
     *
     * @param vo 教师课程资格信息
     * @return 申请成功的教师课程资格UUID
     */
    String applyTeacherCourseQualification(TeacherCourseQualificationVO vo);
}
