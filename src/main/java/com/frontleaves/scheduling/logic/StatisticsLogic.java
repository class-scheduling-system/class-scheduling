package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.dto.base.RequestLogDTO;
import com.frontleaves.scheduling.models.dto.statistics.AcademicDashboardDTO;
import com.frontleaves.scheduling.models.dto.statistics.AdminDashboardDTO;
import com.frontleaves.scheduling.models.dto.statistics.TeacherDashboardDTO;
import com.frontleaves.scheduling.models.entity.base.*;
import com.frontleaves.scheduling.services.StatisticsService;
import com.frontleaves.scheduling.services.UserService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计服务实现类
 * <p>
 * 该类实现了 {@link StatisticsService} 接口，提供系统各类统计数据的获取方法
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Service
@RequiredArgsConstructor
public class StatisticsLogic implements StatisticsService {

    // 学时类型代码到中文名称的映射
    private static final Map<String, String> CREDIT_HOUR_TYPE_MAP = new HashMap<>();

    static {
        CREDIT_HOUR_TYPE_MAP.put("theory", "理论课");
        CREDIT_HOUR_TYPE_MAP.put("practice", "实践课");
        CREDIT_HOUR_TYPE_MAP.put("experiment", "实验课");
        CREDIT_HOUR_TYPE_MAP.put("computer", "上机课");
        CREDIT_HOUR_TYPE_MAP.put("design", "设计课");
        CREDIT_HOUR_TYPE_MAP.put("internship", "实习课");
        CREDIT_HOUR_TYPE_MAP.put("project", "项目课");
        CREDIT_HOUR_TYPE_MAP.put("other", "其他");
    }

    private final UserDAO userDAO;
    private final BuildingDAO buildingDAO;
    private final TeacherDAO teacherDAO;
    private final StudentDAO studentDAO;
    private final CampusDAO campusDAO;
    private final RequestLogDAO requestLogDAO;
    private final UserService userService;
    private final AdministrativeClassDAO administrativeClassDAO;
    private final TeachingClassDAO teachingClassDAO;
    private final CourseLibraryDAO courseLibraryDAO;
    private final AcademicAffairsPermissionDAO academicAffairsPermissionDAO;
    private final MajorDAO majorDAO;
    private final ClassAssignmentDAO classAssignmentDAO;
    private final CreditHourTypeDAO creditHourTypeDAO;

    @Override
    public AdminDashboardDTO getAdminDashboardStatistics() {
        AdminDashboardDTO statistics = new AdminDashboardDTO();

        // 设置基础统计数据
        statistics.setUserCount(userDAO.count());
        statistics.setBuildingCount(buildingDAO.count());
        statistics.setTeacherCount(teacherDAO.count());
        statistics.setStudentCount(studentDAO.count());
        statistics.setCampusCount(campusDAO.count());

        // 获取最近100条请求日志
        LambdaQueryWrapper<RequestLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(RequestLogDO::getRequestTime)
                .last("LIMIT 100");
        List<RequestLogDO> logs = requestLogDAO.list(wrapper);

        // 转换为DTO
        List<RequestLogDTO> logDTOs = logs.stream()
                .map(log -> BeanUtil.copyProperties(log, RequestLogDTO.class))
                .toList();
        statistics.setRequestLogs(logDTOs);

        return statistics;
    }

    @Override
    public AcademicDashboardDTO getAcademicDashboardStatistics(HttpServletRequest request) {
        AcademicDashboardDTO statistics = new AcademicDashboardDTO();

        // 获取当前用户所属部门
        UserDO user = userService.getUserByRequest(request);
        AcademicAffairsPermissionDO academicAffairsPermission = academicAffairsPermissionDAO.getAcademicAffairsPermissionByUserUuid(user.getUserUuid());

        // 设置院系相关统计数据
        statistics
            .setTeacherCount(teacherDAO.lambdaQuery().eq(TeacherDO::getUnitUuid, academicAffairsPermission.getDepartment()).count())
            .setStudentCount(studentDAO.lambdaQuery().eq(StudentDO::getDepartment, academicAffairsPermission.getDepartment()).count())
            .setAdministrativeClassCount(administrativeClassDAO.lambdaQuery().eq(AdministrativeClassDO::getDepartmentUuid, academicAffairsPermission.getDepartment()).count())
            .setTeachingClassCount(teachingClassDAO.lambdaQuery().eq(TeachingClassDO::getCourseDepartmentUuid, academicAffairsPermission.getDepartment()).count())
            .setCourseLibraryCount(courseLibraryDAO.lambdaQuery().eq(CourseLibraryDO::getDepartment, academicAffairsPermission.getDepartment()).count());

        // 设置专业相关统计数据
        statistics.setMajorStudentCounts(new ArrayList<>());
        // 获取(本学院)所有专业
        List<MajorDO> majors = majorDAO.lambdaQuery().eq(MajorDO::getDepartmentUuid, academicAffairsPermission.getDepartment()).list();
        // 统计每个专业的学生人数
        for (MajorDO major : majors) {
            long count = studentDAO.lambdaQuery().eq(StudentDO::getMajor, major.getMajorUuid()).count();
            statistics.getMajorStudentCounts().add(
                new AcademicDashboardDTO.MajorStudentCount()
                    .setMajorUuid(major.getMajorUuid())
                    .setMajorName(major.getMajorName())
                    .setCount(count)
            );
        }

        return statistics;
    }

    @Override
    public TeacherDashboardDTO getTeacherDashboardStatistics(HttpServletRequest request) {
        TeacherDashboardDTO statistics = new TeacherDashboardDTO();

        // 获取当前用户信息
        UserDO user = userService.getUserByRequest(request);

        // 获取当前用户对应的教师信息
        TeacherDO teacher = teacherDAO.lambdaQuery()
                .eq(TeacherDO::getUserUuid, user.getUserUuid())
                .one();

        if (teacher == null) {
            throw new BusinessException("用户不是教师", ErrorCode.BODY_ERROR);
        }

        String teacherUuid = teacher.getTeacherUuid();

        // 1. 获取该教师的所有课程安排
        List<ClassAssignmentDO> assignments = classAssignmentDAO.lambdaQuery()
                .eq(ClassAssignmentDO::getTeacherUuid, teacherUuid)
                .list();

        if (assignments.isEmpty()) {
            // 没有课程安排，返回空统计
            return statistics.setCourseCount(0L)
                    .setStudentCount(0L)
                    .setClassCount(0L)
                    .setTotalHours(0L)
                    .setClassDetails(new ArrayList<>());
        }

        // 从课程安排中提取教学班和课程ID列表（去重）
        List<String> teachingClassIds = assignments.stream()
                .map(ClassAssignmentDO::getTeachingClassUuid)
                .distinct()
                .collect(Collectors.toList());

        List<String> courseIds = assignments.stream()
                .map(ClassAssignmentDO::getCourseUuid)
                .distinct()
                .collect(Collectors.toList());

        // 收集所有学时类型UUID
        List<String> creditHourTypeIds = assignments.stream()
                .map(ClassAssignmentDO::getCreditHourType)
                .distinct()
                .collect(Collectors.toList());

        // 2. 一次性查询所有需要的数据
        // 查询所有相关教学班
        List<TeachingClassDO> teachingClasses = teachingClassDAO.lambdaQuery()
                .in(TeachingClassDO::getTeachingClassUuid, teachingClassIds)
                .list();

        // 查询所有相关课程
        List<CourseLibraryDO> courses = courseLibraryDAO.lambdaQuery()
                .in(CourseLibraryDO::getCourseLibraryUuid, courseIds)
                .list();

        // 查询所有学时类型
        List<CreditHourTypeDO> creditHourTypes = creditHourTypeDAO.list();

        // 3. 建立映射，方便后续使用
        Map<String, TeachingClassDO> teachingClassMap = teachingClasses.stream()
                .collect(Collectors.toMap(TeachingClassDO::getTeachingClassUuid, tc -> tc));

        Map<String, CourseLibraryDO> courseMap = courses.stream()
                .collect(Collectors.toMap(CourseLibraryDO::getCourseLibraryUuid, c -> c));

        Map<String, List<ClassAssignmentDO>> assignmentsByClassMap = assignments.stream()
                .collect(Collectors.groupingBy(ClassAssignmentDO::getTeachingClassUuid));

        // 建立学时类型UUID到名称的映射
        Map<String, String> creditHourTypeNameMap = creditHourTypes.stream()
                .collect(Collectors.toMap(CreditHourTypeDO::getCreditHourTypeUuid, CreditHourTypeDO::getName));

        // 4. 统计数据
        // 统计不同课程的数量
        long courseCount = courseIds.size();

        // 班级数量
        long classCount = teachingClassIds.size();

        long studentCount = 0;
        long totalHours = 0;
        List<TeacherDashboardDTO.ClassDetail> classDetails = new ArrayList<>();

        // 5. 处理每个班级的详细信息
        for (String classId : teachingClassIds) {
            TeachingClassDO teachingClass = teachingClassMap.get(classId);
            if (teachingClass == null) {
                continue;
            }

            // 获取该班级对应的课程安排
            List<ClassAssignmentDO> classAssignments = assignmentsByClassMap.get(classId);
            if (classAssignments == null || classAssignments.isEmpty()) {
                continue;
            }

            // 创建班级详情对象
            TeacherDashboardDTO.ClassDetail detail = new TeacherDashboardDTO.ClassDetail();
            detail.setTeachingClassUuid(classId);
            detail.setTeachingClassName(teachingClass.getTeachingClassName());

            // 设置学生人数
            Integer actualStudentCount = teachingClass.getActualStudentCount();
            detail.setStudentCount(actualStudentCount != null ? actualStudentCount : 0);

            // 累加总学生人数
            studentCount += (actualStudentCount != null ? actualStudentCount : 0);

            // 获取第一个课程安排，通常一个班级只对应一个课程
            ClassAssignmentDO firstAssignment = classAssignments.get(0);
            String courseUuid = firstAssignment.getCourseUuid();

            // 设置课程名称
            CourseLibraryDO course = courseMap.get(courseUuid);
            detail.setCourseName(course != null ? course.getName() : "未知课程");

            // 设置学时类型（将UUID转换为名称）
            String creditHourTypeUuid = firstAssignment.getCreditHourType();
            // 优先从数据库获取的映射中查找，如果找不到则使用静态映射或原值
            String creditHourTypeName = creditHourTypeNameMap.get(creditHourTypeUuid);
            if (creditHourTypeName == null) {
                creditHourTypeName = CREDIT_HOUR_TYPE_MAP.getOrDefault(creditHourTypeUuid, creditHourTypeUuid);
            }
            detail.setCreditHourType(creditHourTypeName);

            // 计算该班级的总学时
            detail.setTotalHours(firstAssignment.getTotalHours());

            // 累加总课时数
            if (firstAssignment.getTotalHours() != null) {
                totalHours += firstAssignment.getTotalHours().longValue();
            }

            // 添加到班级详情列表
            classDetails.add(detail);
        }

        // 6. 设置统计结果
        statistics.setCourseCount(courseCount)
                .setStudentCount(studentCount)
                .setClassCount(classCount)
                .setTotalHours(totalHours)
                .setClassDetails(classDetails);

        return statistics;
    }
}
