package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.dto.base.*;
import com.frontleaves.scheduling.models.dto.merge.ClassroomInfoDTO;
import com.frontleaves.scheduling.models.dto.merge.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.dto.scheduling.AutomaticClassSchedulingBaseDTO;
import com.frontleaves.scheduling.models.dto.scheduling.CreditHourTypeEnuDTO;
import com.frontleaves.scheduling.models.dto.scheduling.ScheduleResultDTO;
import com.frontleaves.scheduling.models.entity.base.*;
import com.frontleaves.scheduling.models.vo.ClassAssignmentVO;
import com.frontleaves.scheduling.services.*;
import com.frontleaves.scheduling.utils.CheckConflicts;
import com.frontleaves.scheduling.utils.ProjectOption;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 排课分配逻辑实现类
 * <p>
 * 该类用于实现排课分配相关的业务逻辑，包括添加、删除、更新和查询排课分配信息等功能。
 * 该类实现了排课分配服务接口，用于处理排课分配相关的业务逻辑。
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassAssignmentLogic implements ClassAssignmentService {

    private final ClassAssignmentDAO classAssignmentDAO;
    private final SemesterDAO semesterDAO;
    private final CourseLibraryDAO courseLibraryDAO;
    private final TeacherDAO teacherDAO;
    private final TeachingClassService teachingClassService;
    private final SemesterService semesterService;
    private final CourseLibraryService courseLibraryService;
    private final TeacherService teacherService;
    private final ClassroomService classroomService;
    private final CampusService campusService;
    private final CreditHourTypeService creditHourTypeService;
    private final AdministrativeClassService administrativeClassService;
    private final SchedulingConflictDAO schedulingConflictDAO;

    @Override
    public List<SchedulingConflictDTO> add(@NotNull ClassAssignmentVO vo) {
        // 验证学期是否存在
        SemesterDTO semester = semesterService.getSemesterByUuidCheckEnabled(vo.getSemesterUuid());
        // 验证课程是否存在
        CourseLibraryDTO course = courseLibraryService.getCourseLibraryByUuid(vo.getCourseUuid());
        // 验证教师是否存在
        TeacherDTO teacher = teacherService.getTeacher(vo.getTeacherUuid());
        //验证教学楼UUID是否存在
        ClassroomInfoDTO classroomInfo = classroomService.getClassroomByUuid(vo.getClassroomUuid());
        if (classroomInfo == null){
            throw new BusinessException("教室不存在", ErrorCode.NOT_EXIST);
        }
        //验证校区是否存在
        CampusDTO campus = campusService.getCampusByUuid(vo.getTeachingCampus());
        if (campus == null){
            throw new BusinessException("教学校区不存在", ErrorCode.NOT_EXIST);
        }
        //检测学时类型
        CreditHourTypeEnuDTO creditHourType = creditHourTypeService.getCreditHourTypeByUuid(
                vo.getCreditHourType());
        this.determineWhetherTheCreditHoursAreCorrect(vo,course);
        this.checkClass(vo.getAdministrativeClassUuids());
        //新建教学班级
        TeachingClassDO teachingClass = this.addTeachingClass(vo,course);
        //新建课程安排
        ClassAssignmentDO classAssignment = new ClassAssignmentDO();
        classAssignment
                .setClassAssignmentUuid(UuidUtil.generateUuidNoDash())
                .setSemesterUuid(semester.getSemesterUuid())
                .setCourseUuid(vo.getCourseUuid())
                .setTeacherUuid(teacher.getTeacherUuid())
                .setCampusUuid(classroomInfo.getCampus().getCampusUuid())
                .setBuildingUuid(classroomInfo.getBuilding().getBuildingUuid())
                .setClassroomUuid(classroomInfo.getClassroom().getClassroomUuid())
                .setTeachingClassUuid(teachingClass.getTeachingClassUuid())
                .setCourseOwnership(vo.getCourseOwnership())
                .setCreditHourType(creditHourType.getCreditHourTypeUuid())
                .setTeachingHours(vo.getTeachingHours())
                .setScheduledHours(vo.getScheduledHours())
                .setTotalHours(vo.getTotalHours())
                .setTeachingCampus(vo.getTeachingCampus())
                .setClassroomType(classroomInfo.getType().getClassTypeUuid())
                .setSchedulingPriority(vo.getSchedulingPriority())
                .setConsecutiveSessions(vo.getConsecutiveSessions())
                .setClassTime(JSONUtil.toJsonStr(ProjectUtil.ClassTimeToTimeSlot(vo.getClassTime())));
        classAssignmentDAO.save(classAssignment);
        ClassAssignmentDTO classAssignmentDTO = BeanUtil.toBean(classAssignment, ClassAssignmentDTO.class);
        List<ClassAssignmentDTO> allAssignments = this.getClassAssignmentListConflict(
                classAssignmentDTO);
        List<SchedulingConflictDTO> conflict =
                CheckConflicts.checkConflicts(allAssignments,classAssignmentDTO);
        schedulingConflictDAO.batchSaveConflicts(conflict, vo.getSemesterUuid());
        return  conflict;
    }

    private @NotNull TeachingClassDO addTeachingClass(@NotNull ClassAssignmentVO vo, @NotNull CourseLibraryDTO course) {
        TeachingClassDO teachingClass = new TeachingClassDO();
        teachingClass.
                setTeachingClassUuid(UuidUtil.generateUuidNoDash())
                .setSemesterUuid(vo.getSemesterUuid())
                .setCourseUuid(vo.getCourseUuid())
                .setTeachingClassCode(UuidUtil.generateUuidNoDash())
                .setTeachingClassName(vo.getTeachingClassName())
                .setActualStudentCount(vo.getStudentCount())
                .setCourseDepartmentUuid(course.getDepartment())
                .setIsEnabled(true);
        if (vo.getAdministrativeClassUuids() != null && !vo.getAdministrativeClassUuids().isEmpty()){
            teachingClass.setAdministrativeClasses(JSONUtil.toJsonStr(vo.getAdministrativeClassUuids()))
                    .setClassSize(vo.getAdministrativeClassUuids().size())
                    .setIsAdministrative(true);
        }else {
            teachingClass.setAdministrativeClasses(JSONUtil.toJsonStr(new ArrayList<>()))
                    .setClassSize(1)
                    .setIsAdministrative(false);
        }
        teachingClassService.save(teachingClass);
        return  teachingClass;
    }

    private void checkClass(List<String> administrativeClassUuids) {
        if (administrativeClassUuids != null && !administrativeClassUuids.isEmpty()){
            for (String uuid : administrativeClassUuids){
                administrativeClassService.getClassByUuid(uuid);
            }
        }
    }

    private void determineWhetherTheCreditHoursAreCorrect(
            @NotNull ClassAssignmentVO vo,
            @NotNull CourseLibraryDTO course) {
        // 学时不能小于课程总学时
        if (vo.getTotalHours().compareTo(course.getTotalHours()) > 0) {
            log.debug("总学时：{}", course.getTotalHours());
            log.debug("实际安排学时：{}", vo.getTotalHours());
            throw new BusinessException("实际安排学时不能大于课程要求总学时", ErrorCode.PARAMETER_ERROR);
        }
        if (vo.getTeachingHours().compareTo(course.getTotalHours()) > 0){
            log.debug("总学时：{}", course.getTotalHours());
            log.debug("实际教学学时：{}", vo.getTeachingHours());
            throw new BusinessException("实际安排学时不能大于课程要求总学时", ErrorCode.PARAMETER_ERROR);
        }
        if (vo.getScheduledHours().compareTo(course.getTheoryHours()) > 0){
            log.debug("理论学时：{}", course.getTheoryHours());
            log.debug("实际安排学时：{}", vo.getScheduledHours());
            throw new BusinessException("实际安排学时不能大于课程要求理论学时", ErrorCode.PARAMETER_ERROR);
        }
    }


    @Override
    public void delete(String classAssignmentUuid) {
        // 验证排课分配是否存在
        ClassAssignmentDO entity = classAssignmentDAO.getClassAssignmentByUuid(classAssignmentUuid);
        if (entity == null) {
            throw new BusinessException(StringConstant.ErrorMessage.CLASS_ASSIGNMENT_NOT_FOUND, ErrorCode.NOT_EXIST);
        }

        // 删除排课分配
        classAssignmentDAO.removeClassAssignment(classAssignmentUuid);
    }

    @Override
    public void update(String classAssignmentUuid, ClassAssignmentVO vo) {
        // 验证排课分配是否存在
        ClassAssignmentDO existingEntity = classAssignmentDAO.getClassAssignmentByUuid(classAssignmentUuid);
        if (existingEntity == null) {
            throw new BusinessException(StringConstant.ErrorMessage.CLASS_ASSIGNMENT_NOT_FOUND, ErrorCode.NOT_EXIST);
        }

        // 验证学期是否存在
        if (vo.getSemesterUuid() != null) {
            SemesterDO semester = semesterDAO.getSemesterByUuid(vo.getSemesterUuid());
            if (semester == null) {
                throw new BusinessException("学期不存在", ErrorCode.NOT_EXIST);
            }
        }

        // 验证课程是否存在
        if (vo.getCourseUuid() != null) {
            CourseLibraryDO course = courseLibraryDAO.getCourseLibraryByUuid(vo.getCourseUuid());
            if (course == null) {
                throw new BusinessException("课程不存在", ErrorCode.NOT_EXIST);
            }
        }

        // 验证教师是否存在
        if (vo.getTeacherUuid() != null) {
            TeacherDO teacher = teacherDAO.getTeacherByUuid(vo.getTeacherUuid());
            if (teacher == null) {
                throw new BusinessException("教师不存在", ErrorCode.NOT_EXIST);
            }
        }

        // 更新实体对象
        BeanUtil.copyProperties(vo, existingEntity, ProjectOption.stringBlankToNull());
        classAssignmentDAO.updateClassAssignment(existingEntity);
    }

    @Override
    public ClassAssignmentDTO getById(String classAssignmentUuid) {
        // 验证排课分配是否存在
        ClassAssignmentDO entity = classAssignmentDAO.getClassAssignmentByUuid(classAssignmentUuid);
        if (entity == null) {
            throw new BusinessException(StringConstant.ErrorMessage.CLASS_ASSIGNMENT_NOT_FOUND, ErrorCode.NOT_EXIST);
        }
        // 转换为 DTO 并返回
        return BeanUtil.toBean(entity, ClassAssignmentDTO.class);
    }

    @Override
    public PageDTO<ClassAssignmentDTO> page(Integer page, Integer size, String semesterUuid, String courseUuid, String teacherUuid) {
        // 验证 UUID 格式（如果提供）
        if (semesterUuid != null && !semesterUuid.isBlank() &&
                !semesterUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException(StringConstant.ErrorMessage.SEMESTER_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR);
        }
        if (courseUuid != null && !courseUuid.isBlank() &&
                !courseUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException(StringConstant.ErrorMessage.COURSE_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR);
        }
        if (teacherUuid != null && !teacherUuid.isBlank() &&
                !teacherUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException(StringConstant.ErrorMessage.TEACHER_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR);
        }

        // 获取分页数据
        Page<ClassAssignmentDO> pageResult = classAssignmentDAO.getClassAssignmentPage(page, size, semesterUuid, courseUuid, teacherUuid);
        if (pageResult == null || !pageResult.hasNext()) {
            throw new BusinessException(StringConstant.ErrorMessage.CLASS_ASSIGNMENT_NOT_FOUND, ErrorCode.NOT_EXIST);
        }

        // 转换为 DTO
        return ProjectUtil.convertPageToPageDTO(pageResult, ClassAssignmentDTO.class);
    }

    @Override
    public List<ClassAssignmentDTO> list(String semesterUuid, String courseUuid, String teacherUuid) {
        // 验证 UUID 格式（如果提供）
        if (semesterUuid != null && !semesterUuid.isBlank() &&
                !semesterUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException(StringConstant.ErrorMessage.SEMESTER_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR);
        }
        if (courseUuid != null && !courseUuid.isBlank() &&
                !courseUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException(StringConstant.ErrorMessage.COURSE_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR);
        }
        if (teacherUuid != null && !teacherUuid.isBlank() &&
                !teacherUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException(StringConstant.ErrorMessage.TEACHER_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR);
        }

        // 获取列表数据
        return Optional.ofNullable(classAssignmentDAO.list(semesterUuid, courseUuid, teacherUuid))
                .map(list -> list.stream()
                        .map(entity -> BeanUtil.toBean(entity, ClassAssignmentDTO.class))
                        .toList())
                .orElse(List.of());
    }

    @Override
    public void saveClassAssignment(@NotNull ScheduleResultDTO result) {
        // 获取排课结果
        List<ScheduleResultDTO.CourseTeachingClassDTO> classAssignments = result.getAssignments();
        if (classAssignments == null || classAssignments.isEmpty()) {
            throw new BusinessException("排课结果为空", ErrorCode.PARAMETER_ERROR);
        }
        // 遍历排课结果，保存到数据库
        for (ScheduleResultDTO.CourseTeachingClassDTO assignment : classAssignments) {
            ClassAssignmentDO assignmentDO = new ClassAssignmentDO();
            //交换数据
            assignmentDO.setSemesterUuid(result.getSemesterUuid())
                    .setCourseUuid(assignment.getCourse().getCourseLibraryUuid())
                    .setTeacherUuid(assignment.getTeacher().getTeacher().getTeacherUuid())
                    .setClassroomUuid(assignment.getClassroom().getClassroom().getClassroomUuid());
        }

    }
    /**
     * 根据条件获取排课分配列表
     * 根据教室、教师和行政班级的UUID进行筛选
     * @param automaticClassSchedulingBaseDTO 自动排课基础信息DTO
     * @return 符合条件的排课分配列表
     */
    @Override
    public List<ClassAssignmentDTO> getClassAssignmentListByLimit(
            @NotNull AutomaticClassSchedulingBaseDTO automaticClassSchedulingBaseDTO) {
        Map<String, Boolean> classroomUuidsMap = automaticClassSchedulingBaseDTO.getClassroomList().stream()
                .map(dto -> dto.getClassroom().getClassroomUuid())
                .collect(Collectors.toMap(
                        uuid -> uuid,
                        uuid -> Boolean.TRUE,
                        (existing, replacement) -> existing));
        Map<String, Boolean> teacherUuidsMap = automaticClassSchedulingBaseDTO.getCourseList().stream()
                .flatMap(course -> course.getTeacherList().stream())
                .map(teacherPref -> teacherPref.getTeacher().getTeacherUuid())
                .collect(Collectors.toMap(
                        uuid -> uuid,
                        uuid -> Boolean.TRUE,
                        (existing, replacement) -> existing));
        Map<String, Boolean> classUuidsMap = new HashMap<>();
        List<CourseLibraryAndTeacherCourseQualificationListDTO> courseList = automaticClassSchedulingBaseDTO.getCourseList();
        if (courseList != null) {
            classUuidsMap = courseList.stream()
                    .filter(Objects::nonNull)
                    .map(CourseLibraryAndTeacherCourseQualificationListDTO::getClassList)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .filter(Objects::nonNull)
                    .map(AdministrativeClassDTO::getAdministrativeClassUuid)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(
                            uuid -> uuid,
                            uuid -> Boolean.TRUE,
                            (existing, replacement) -> existing));
        }
        //获取教学班
        List<TeachingClassDTO> teachingClassDTOList = teachingClassService
                .getTeachingClassListBySemester(automaticClassSchedulingBaseDTO.getSemester().getSemesterUuid());
        // 获取本学期的所有排课分配
        List<ClassAssignmentDO> classAssignments = classAssignmentDAO
                .getClassAssignmentListBySemester(automaticClassSchedulingBaseDTO.getSemester().getSemesterUuid());
        // 遍历排课分配列表并筛选出 classroomUuid 匹配的项
        Map<String, ClassAssignmentDTO> classAssignmentMap = new HashMap<>();
        for (ClassAssignmentDO classAssignment : classAssignments) {
            this.processClassAssignment(classAssignment, classroomUuidsMap, teacherUuidsMap, classUuidsMap, classAssignmentMap, teachingClassDTOList);
        }
        return new ArrayList<>(classAssignmentMap.values());
    }

    @Override
    public void save(ClassAssignmentDO classAssignmentDO) {
        classAssignmentDAO.saveClassAssignment(classAssignmentDO);
    }

    @Override
    public List<ClassAssignmentDTO> getClassAssignmentListConflict(@NotNull ClassAssignmentDTO classAssignment) {
        // 获取老师UUID,教室UUID和教学班UUID
        List<ClassAssignmentDO> list = classAssignmentDAO.getClassAssignmentListBySemester(classAssignment.getSemesterUuid());
        List<ClassAssignmentDO> filteredList = list.stream()
                .filter(assignmentDO ->
                        // 检查当前 assignmentDO 的 UUID 是否与输入的任一 UUID 匹配
                        Objects.equals(assignmentDO.getTeacherUuid(), classAssignment.getTeacherUuid()) ||
                                Objects.equals(assignmentDO.getClassroomUuid(), classAssignment.getClassroomUuid()) ||
                                Objects.equals(assignmentDO.getTeachingClassUuid(), classAssignment.getTeachingClassUuid())
                )
                .toList();
        return BeanUtil.copyToList(filteredList, ClassAssignmentDTO.class);
    }

    /**
     * 处理排课分配对象
     */
    private void processClassAssignment(@NotNull ClassAssignmentDO classAssignment,
                                        @NotNull Map<String, Boolean> classroomUuidsMap,
                                        Map<String, Boolean> teacherUuidsMap,
                                        Map<String, Boolean> classUuidsMap,
                                        Map<String, ClassAssignmentDTO> classAssignmentMap,
                                        List<TeachingClassDTO> teachingClassDTOList) {
        // 检查教室匹配
        if (classroomUuidsMap.containsKey(classAssignment.getClassroomUuid())) {
            this.addToAssignmentMap(classAssignment, classAssignmentMap);
        }
        // 检查教师匹配
        if (teacherUuidsMap.containsKey(classAssignment.getTeacherUuid())) {
            this.addToAssignmentMap(classAssignment, classAssignmentMap);
        }
        // 只有当classUuidsMap不为空时才检查行政班级匹配
        if (classUuidsMap != null && !classUuidsMap.isEmpty()) {
            this.checkClassGroupMatch(classAssignment, classUuidsMap, classAssignmentMap, teachingClassDTOList);
        }
    }
    /**
     * 将排课分配对象添加到映射中
     * @param classAssignment 排课分配对象
     * @param classAssignmentMap 排课分配映射
     */
    private void addToAssignmentMap(ClassAssignmentDO classAssignment,
                                    @NotNull Map<String, ClassAssignmentDTO> classAssignmentMap) {
        ClassAssignmentDTO dto = BeanUtil.toBean(classAssignment, ClassAssignmentDTO.class);
        classAssignmentMap.putIfAbsent(classAssignment.getClassAssignmentUuid(), dto);
    }
    /**
     * 检查行政班级匹配
     */
    private void checkClassGroupMatch(@NotNull ClassAssignmentDO classAssignment,
                                      @NotNull Map<String, Boolean> classUuidsMap,
                                      Map<String, ClassAssignmentDTO> classAssignmentMap,
                                      @NotNull List<TeachingClassDTO> teachingClassDTOList) {
        // 在教学班列表中查找匹配的教学班
        TeachingClassDTO teachingClassDTO = teachingClassDTOList.stream()
                .filter(dto -> dto.getTeachingClassUuid().equals(classAssignment.getTeachingClassUuid()))
                .findFirst()
                .orElse(null);
        if (teachingClassDTO == null || teachingClassDTO.getAdministrativeClasses() == null) {
            return;
        }
        try {
            List<String> administrativeClassUuids = JSONUtil.toList(teachingClassDTO.getAdministrativeClasses(), String.class);
            for (String uuid : administrativeClassUuids) {
                if (classUuidsMap.containsKey(uuid)) {
                    addToAssignmentMap(classAssignment, classAssignmentMap);
                    break;
                }
            }
        } catch (Exception e) {
            log.error("解析教学班行政班级列表失败, teachingClassUuid: {}, administrativeClasses: {}",
                    teachingClassDTO.getTeachingClassUuid(),
                    teachingClassDTO.getAdministrativeClasses(),
                    e);
        }
    }
}
