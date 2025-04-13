package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.ClassAssignmentDAO;
import com.frontleaves.scheduling.daos.SchedulingConflictDAO;
import com.frontleaves.scheduling.daos.TeachingClassDAO;
import com.frontleaves.scheduling.models.dto.base.*;
import com.frontleaves.scheduling.models.dto.merge.ClassroomInfoDTO;
import com.frontleaves.scheduling.models.dto.merge.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.dto.scheduling.*;
import com.frontleaves.scheduling.models.entity.base.ClassAssignmentDO;
import com.frontleaves.scheduling.models.entity.base.TeachingClassDO;
import com.frontleaves.scheduling.models.entity.base.UserDO;
import com.frontleaves.scheduling.models.vo.*;
import com.frontleaves.scheduling.services.*;
import com.frontleaves.scheduling.utils.CheckConflicts;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
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
    private final TeachingClassService teachingClassService;
    private final SemesterService semesterService;
    private final CourseLibraryService courseLibraryService;
    private final TeacherService teacherService;
    private final ClassroomService classroomService;
    private final CampusService campusService;
    private final CreditHourTypeService creditHourTypeService;
    private final AdministrativeClassService administrativeClassService;
    private final SchedulingConflictDAO schedulingConflictDAO;
    private final SchedulingConflictService schedulingConflictService;
    private final UserService userService;
    private final TeachingClassDAO teachingClassDAO;
    private final AcademicAffairsPermissionService academicAffairsPermissionService;

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
        if (classroomInfo == null) {
            throw new BusinessException("教室不存在", ErrorCode.NOT_EXIST);
        }
        //验证校区是否存在
        CampusDTO campus = campusService.getCampusByUuid(vo.getTeachingCampus());
        if (campus == null) {
            throw new BusinessException("教学校区不存在", ErrorCode.NOT_EXIST);
        }
        //检测学时类型
        CreditHourTypeEnuDTO creditHourType = creditHourTypeService.getCreditHourTypeByUuid(
                vo.getCreditHourType());
        this.determineWhetherTheCreditHoursAreCorrect(vo, course);
        this.checkClass(vo.getAdministrativeClassUuids());
        //新建教学班级
        TeachingClassDO teachingClass = this.addTeachingClass(vo, course);
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
        classAssignmentDAO.saveClassAssignment(classAssignment);
        ClassAssignmentDTO classAssignmentDTO = BeanUtil.toBean(classAssignment, ClassAssignmentDTO.class);
        List<ClassAssignmentDTO> allAssignments = this.getClassAssignmentListConflict(
                classAssignmentDTO);
        List<SchedulingConflictDTO> conflict =
                CheckConflicts.checkConflicts(allAssignments, classAssignmentDTO);
        schedulingConflictDAO.batchSaveConflicts(conflict, vo.getSemesterUuid());
        return conflict;
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
        if (vo.getAdministrativeClassUuids() != null && !vo.getAdministrativeClassUuids().isEmpty()) {
            teachingClass.setAdministrativeClasses(JSONUtil.toJsonStr(vo.getAdministrativeClassUuids()))
                    .setClassSize(vo.getAdministrativeClassUuids().size())
                    .setIsAdministrative(true);
        } else {
            teachingClass.setAdministrativeClasses(JSONUtil.toJsonStr(new ArrayList<>()))
                    .setClassSize(1)
                    .setIsAdministrative(false);
        }
        teachingClassService.save(teachingClass);
        return teachingClass;
    }

    private void checkClass(List<String> administrativeClassUuids) {
        if (administrativeClassUuids != null && !administrativeClassUuids.isEmpty()) {
            for (String uuid : administrativeClassUuids) {
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
        if (vo.getTeachingHours().compareTo(course.getTotalHours()) > 0) {
            log.debug("总学时：{}", course.getTotalHours());
            log.debug("实际教学学时：{}", vo.getTeachingHours());
            throw new BusinessException("实际安排学时不能大于课程要求总学时", ErrorCode.PARAMETER_ERROR);
        }
        if (vo.getScheduledHours().compareTo(course.getTheoryHours()) > 0) {
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
        // 删除教学班
        teachingClassDAO.deleteTeachingClass(entity.getTeachingClassUuid());
    }

    @Override
    public BackAdjustCourseScheduleDTO update(@NotNull AdjustmentsVO vo,
                                              HttpServletRequest request) {
        //获取排课分配
        ClassAssignmentDTO classAssignment = this.getById(vo.getAssignmentId());
        ClassAssignmentDTO classAssignment1 = BeanUtil.toBean(classAssignment, ClassAssignmentDTO.class);
        AdjustmentDetailsVO details = vo.getAdjustments();
        this.exchangeClassroom(classAssignment1, details);
        this.exchangeTeacher(classAssignment1, details);
        this.exchangeTimeSlot(classAssignment1, details);
        this.exchangeOtherDetails(classAssignment1, details);
        List<SchedulingConflictDTO> conflict =
                this.detectConflicts(classAssignment1, vo.getIgnoreConflicts());
        //更新排课安排
        classAssignmentDAO.updateClassAssignment(BeanUtil.toBean(classAssignment1, ClassAssignmentDO.class));
        //更新教学班
        TeachingClassDO teachingClass
                = this.exchangeTeachingClass(vo.getAdjustTeachingClass(), classAssignment);
        teachingClassDAO.updateTeachingClass(teachingClass);
        return this.createdBackDate(classAssignment,
                classAssignment1, vo, conflict, request);
    }

    private TeachingClassDO exchangeTeachingClass(@NotNull AdjustTeachingClassVO vo,
                                                  @NotNull ClassAssignmentDTO classAssignment) {
        TeachingClassDTO clazz = teachingClassService.getTeachingClassByUuid(vo.getTeachingClassUuid());
        //检查是否更新的是此课程的教学班
        if (!classAssignment.getTeachingClassUuid().equals(vo.getTeachingClassUuid())) {
            throw new BusinessException("教学班不属于此课程", ErrorCode.PARAMETER_ERROR);
        }
        if (vo.getTeachingClassName() != null && !vo.getTeachingClassName().isEmpty()) {
            clazz.setTeachingClassName(vo.getTeachingClassName());
        }
        if (vo.getTeachingClassCode() != null && !vo.getTeachingClassCode().isEmpty()) {
            clazz.setTeachingClassCode(vo.getTeachingClassCode());
        }
        if (vo.getDescription() != null && !vo.getDescription().isEmpty()) {
            clazz.setDescription(vo.getDescription());
        }
        if (vo.getActualStudentCount() != null) {
            clazz.setActualStudentCount(vo.getActualStudentCount());
        }
        if (vo.getAdministrativeClassUuids() != null && !vo.getAdministrativeClassUuids().isEmpty()) {
            //检查原先是否为选修班级
            if (Boolean.FALSE.equals(clazz.getIsAdministrative())) {
                throw new BusinessException("选修课程禁止添加行政班级", ErrorCode.BODY_ERROR);
            }
            for (String uuid : vo.getAdministrativeClassUuids()) {
                administrativeClassService.getClassByUuid(uuid);
            }
            clazz.setAdministrativeClasses(JSONUtil.toJsonStr(vo.getAdministrativeClassUuids()));
        }
        return BeanUtil.toBean(clazz, TeachingClassDO.class);
    }

    private BackAdjustCourseScheduleDTO createdBackDate(
            @NotNull ClassAssignmentDTO classAssignment,
            @NotNull ClassAssignmentDTO classAssignment1,
            @NotNull AdjustmentsVO adjustmentsVO,
            List<SchedulingConflictDTO> conflict,
            HttpServletRequest request) {
        //获取课程信息
        CourseLibraryDTO courseLibraryDTO = courseLibraryService.getCourseLibraryByUuid(classAssignment.getCourseUuid());
        //获取教室信息
        ClassroomInfoDTO classroom = classroomService.getClassroomByUuid(classAssignment.getClassroomUuid());
        //获取教室信息
        ClassroomInfoDTO classroom1 = classroomService.getClassroomByUuid(classAssignment1.getClassroomUuid());
        //获取教师信息
        TeacherDTO teacher = teacherService.getTeacher(classAssignment.getTeacherUuid());
        TeacherDTO teacher1 = teacherService.getTeacher(classAssignment1.getTeacherUuid());
        // 将时间槽转换为classTime
        List<ClassTimeDTO> classTime = ProjectUtil.convertToClassTimeDTOList(JSONUtil.toList(
                classAssignment.getClassTime(), TimeSlotDTO.class));
        List<ClassTimeDTO> classTime1 = ProjectUtil.convertToClassTimeDTOList(JSONUtil.toList(
                classAssignment1.getClassTime(), TimeSlotDTO.class));
        //或许当前更改用户数据
        UserDO getUser = userService.getUserByRequest(request);
        //创建返回值
        if (classroom1 != null && classroom != null) {
            return new BackAdjustCourseScheduleDTO()
                    .setAssignmentId(classAssignment.getClassAssignmentUuid())
                    .setCourseCode(courseLibraryDTO.getId())
                    .setCourseName(courseLibraryDTO.getName())
                    .setBefore(new CourseDetailsDTO()
                            .setClassroomId(classroom.getClassroom().getClassroomUuid())
                            .setClassroomName(classroom.getClassroom().getName())
                            .setClassTime(classTime)
                            .setTeacherId(teacher.getTeacherUuid())
                            .setTeacherName(teacher.getName())
                            .setSchedulingPriority(classAssignment.getSchedulingPriority())
                            .setConsecutiveSessions(classAssignment.getConsecutiveSessions())
                    )
                    .setAfter(new CourseDetailsDTO()
                            .setClassroomId(classroom1.getClassroom().getClassroomUuid())
                            .setClassroomName(classroom1.getClassroom().getName())
                            .setClassTime(classTime1)
                            .setTeacherId(teacher1.getTeacherUuid())
                            .setTeacherName(teacher1.getName())
                            .setSchedulingPriority(classAssignment1.getSchedulingPriority())
                            .setConsecutiveSessions(classAssignment1.getConsecutiveSessions()))
                    .setAdjustedAt(new Timestamp(System.currentTimeMillis()))
                    .setAdjustedBy(getUser.getUserUuid())
                    .setAdjustedByName(getUser.getName())
                    .setNewConflicts(conflict)
                    .setReason(adjustmentsVO.getReason());
        }
        throw new BusinessException("教室不存在", ErrorCode.BODY_ERROR);
    }


    private List<SchedulingConflictDTO> detectConflicts(
            ClassAssignmentDTO classAssignment1,
            Boolean ignoreConflicts) {
        schedulingConflictService.checkForConflictResolution(classAssignment1);
        List<SchedulingConflictDTO> conflicts = new ArrayList<>();
        if (Boolean.FALSE.equals(ignoreConflicts)) {
            // 检查冲突
            conflicts = this.findConflicts(classAssignment1);
            //批量保存
            schedulingConflictDAO.batchSaveConflicts(conflicts, classAssignment1.getSemesterUuid());
        }
        return conflicts;
    }

    private @NotNull List<SchedulingConflictDTO> findConflicts(ClassAssignmentDTO classAssignment1) {
        // 获取有可能与之相关的所有课程安排
        List<ClassAssignmentDTO> allAssignments = this.getClassAssignmentListConflict(classAssignment1);
        //检查冲突
        return CheckConflicts.checkConflicts(allAssignments, classAssignment1);
    }

    private void exchangeOtherDetails(ClassAssignmentDTO classAssignment1, @NotNull AdjustmentDetailsVO details) {
        // 交换其他细节
        if (details.getConsecutiveSessions() != null) {
            classAssignment1.setConsecutiveSessions(details.getConsecutiveSessions());
        }
        if (details.getSchedulingPriority() != null) {
            classAssignment1.setSchedulingPriority(details.getSchedulingPriority());
        }
    }

    private void exchangeTimeSlot(ClassAssignmentDTO classAssignment1, @NotNull AdjustmentDetailsVO details) {
        // 创建一个列表来存储所有解析出来的目标时间槽 DTO
        List<TimeSlotDTO> targetTimeSlots = new ArrayList<>();
        if (details.getClassTime() != null && !details.getClassTime().isEmpty()) {
            // 遍历 AdjustmentDetailsVO 中的每个 ClassTimeVO 对象
            for (ClassTimeVO classTime : details.getClassTime()) {
                Integer day = classTime.getDayOfWeek();
                Integer startPeriod = classTime.getPeriodStart();
                Integer endPeriod = classTime.getPeriodEnd();
                List<Integer> weeks = classTime.getWeekNumbers();
                // 校验当前 ClassTimeVO 条目是否包含所有必需的信息，并且节次是否有效
                if (day != null
                        && startPeriod != null
                        && endPeriod != null
                        && weeks != null
                        && !weeks.isEmpty()
                        && startPeriod <= endPeriod) {
                    // 外层循环：遍历所有指定的周
                    for (Integer week : weeks) {
                        // 内层循环：遍历从开始节次到结束节次的所有节次（包含）
                        for (int period = startPeriod; period <= endPeriod; period++) {
                            // 创建一个新的 TimeSlotDTO 实例
                            TimeSlotDTO timeSlot = new TimeSlotDTO()
                                    .setWeek(week)
                                    .setDay(day)
                                    .setPeriod(period);
                            // 将创建的 TimeSlotDTO 添加到结果列表中
                            targetTimeSlots.add(timeSlot);
                        }
                    }
                } else {
                    throw new BusinessException("调整时间信息不完整或无效", ErrorCode.BODY_ERROR);
                }
            }
            classAssignment1.setClassTime(JSONUtil.toJsonStr(targetTimeSlots));
        }
    }

    private void exchangeTeacher(ClassAssignmentDTO classAssignment, @NotNull AdjustmentDetailsVO details) {
        if (details.getTeacherId() != null
                && !details.getTeacherId().isEmpty()) {
            //获取教师
            TeacherDTO teacher = teacherService.getTeacher(details.getTeacherId());
            classAssignment.setTeacherUuid(teacher.getTeacherUuid());
        }
    }

    private void exchangeClassroom(ClassAssignmentDTO classAssignment,
                                   @NotNull AdjustmentDetailsVO details) {
        //检测是否为更换新教室
        if (details.getClassroomId() != null
                && !details.getClassroomId().isEmpty()) {
            //获取教室
            ClassroomInfoDTO classroomInfoDTO = classroomService.getClassroomByUuid(details.getClassroomId());
            if (classroomInfoDTO == null) {
                throw new BusinessException("教室不存在", ErrorCode.BODY_ERROR);
            }
            classAssignment
                    .setClassroomType(classroomInfoDTO.getType().getClassTypeUuid())
                    .setBuildingUuid(classAssignment.getBuildingUuid())
                    .setClassroomUuid(classAssignment.getClassroomUuid())
                    .setTeachingCampus(classroomInfoDTO.getCampus().getCampusUuid())
                    .setTeachingCampus(classroomInfoDTO.getCampus().getCampusUuid());
        }
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
    public PageDTO<BackClassAssignmentDTO> page(
            Integer page,
            Integer size,
            String semesterUuid,
            String courseUuid,
            String teacherUuid,
            HttpServletRequest request) {

        if (courseUuid != null && !courseUuid.isBlank()) {
            courseLibraryService.getCourseLibraryByUuid(courseUuid);
        }
        if (teacherUuid != null && !teacherUuid.isBlank()) {
            teacherService.getTeacher(teacherUuid);
        }
        //获取教务所开设的教学班
        String uuid = this.getDepartment(request);
        List<TeachingClassDTO> allTeachingClassList = teachingClassService
                .getTeachingClassListBySemester(semesterUuid);
        List<TeachingClassDTO> teachingClassList = allTeachingClassList.stream()
                .filter(teachingClass -> Objects.equals(teachingClass.getCourseDepartmentUuid(), uuid))
                .toList();
        // 从筛选后的教学班列表中只提取UUID字符串列表
        List<String> teachingClassUuidList = teachingClassList.stream()
                .map(TeachingClassDTO::getTeachingClassUuid)
                .toList();
        // 获取分页数据
        Page<ClassAssignmentDO> pageResult = classAssignmentDAO.getClassAssignmentPage(
                page, size, semesterUuid, courseUuid, teacherUuid, teachingClassUuidList);
        if (pageResult == null) {
            throw new BusinessException(StringConstant.ErrorMessage.CLASS_ASSIGNMENT_NOT_FOUND, ErrorCode.NOT_EXIST);
        }
        // 转换为 DTO
        PageDTO<ClassAssignmentDTO> pageDTO =
                ProjectUtil.convertPageToPageDTO(pageResult, ClassAssignmentDTO.class);
        //转成BackDTO
        return this.exchangeBackPage(pageDTO);
    }

    private @NotNull PageDTO<BackClassAssignmentDTO> exchangeBackPage(@NotNull PageDTO<ClassAssignmentDTO> pageDTO) {
        PageDTO<BackClassAssignmentDTO> backPage = new PageDTO<>();
        backPage.setCurrent(pageDTO.getCurrent());
        backPage.setSize(pageDTO.getSize());
        backPage.setTotal(pageDTO.getTotal());
        List<BackClassAssignmentDTO> list = pageDTO.getRecords().stream()
                .map(dto -> {
                    BackClassAssignmentDTO back = new BackClassAssignmentDTO();
                    BeanUtil.copyProperties(dto, back);
                    back.setClassTimeDTO(ProjectUtil.convertToClassTimeDTOList(
                            JSONUtil.toList(dto.getClassTime(), TimeSlotDTO.class)
                    ));
                    return back;
                }).toList();
        backPage.setRecords(list);
        return backPage;
    }

    private String getDepartment(HttpServletRequest request) {
        //获取当前用户
        UserDO user = userService.getUserByRequest(request);
        AcademicAffairsPermissionDTO dto =
                academicAffairsPermissionService.getAcademicPermissionByUserUuid(user.getUserUuid());
        if (dto == null) {
            throw new BusinessException("教务所权限不存在", ErrorCode.NOT_EXIST);
        }
        return dto.getDepartment();
    }

    @Override
    public List<BackDetailedAssignmentDTO> list(
            String semesterUuid,
            String courseUuid,
            String teacherUuid,
            HttpServletRequest request) {
        // 验证 UUID 格式（如果提供）
        SemesterDTO semesterDTO = semesterService.getSemesterByUuidCheckEnabled(semesterUuid);
        if (courseUuid != null && !courseUuid.isBlank()) {
            courseLibraryService.getCourseLibraryByUuid(courseUuid);

        }
        if (teacherUuid != null && !teacherUuid.isBlank()) {
            teacherService.getTeacher(teacherUuid);
        }
        //获取教务所开设的教学班
        String uuid = this.getDepartment(request);
        List<TeachingClassDTO> allTeachingClassList = teachingClassService
                .getTeachingClassListBySemester(semesterUuid);
        List<TeachingClassDTO> teachingClassList = allTeachingClassList.stream()
                .filter(teachingClass -> Objects.equals(teachingClass.getCourseDepartmentUuid(), uuid))
                .toList();
        // 从筛选后的教学班列表中只提取UUID字符串列表
        List<String> teachingClassUuidList = teachingClassList.stream()
                .map(TeachingClassDTO::getTeachingClassUuid)
                .toList();
        // 获取所有排课分配
        List<ClassAssignmentDO> classAssignments = classAssignmentDAO
                .getList(semesterUuid, courseUuid, teacherUuid, teachingClassUuidList);
        List<BackDetailedAssignmentDTO> backList = new ArrayList<>();
        for (ClassAssignmentDO assignment : classAssignments) {
            BackDetailedAssignmentDTO back = new BackDetailedAssignmentDTO();
            BeanUtil.copyProperties(assignment, back);
            //获取课程信息
            CourseLibraryDTO courseLibraryDTO = courseLibraryService.getCourseLibraryByUuid(assignment.getCourseUuid());
            //获取教室信息
            ClassroomInfoDTO classroom = classroomService.getClassroomByUuid(assignment.getClassroomUuid());
            //获取教师信息
            TeacherDTO teacher = teacherService.getTeacher(assignment.getTeacherUuid());
            //获取教学班信息
            TeachingClassDTO teachingClass = teachingClassService.getTeachingClassByUuid(assignment.getTeachingClassUuid());
            //获取校区信息
            CampusDTO teachingCampus = campusService.getCampusByUuid(assignment.getTeachingCampus());
            CampusDTO campus = campusService.getCampusByUuid(assignment.getCampusUuid());
            //学时类型
            CreditHourTypeEnuDTO creditHourType = creditHourTypeService.getCreditHourTypeByUuid(assignment.getCreditHourType());
            if (campus != null && classroom != null && teachingCampus != null) {
                back.setSemesterName(semesterDTO.getName())
                        .setCourseName(courseLibraryDTO.getName())
                        .setTeacherName(teacher.getName())
                        .setCampusName(campus.getCampusName())
                        .setBuildingName(classroom.getBuilding().getBuildingName())
                        .setClassroomName(classroom.getClassroom().getName())
                        .setTeachingClassName(teachingClass.getTeachingClassName())
                        .setCreditHourTypeName(creditHourType.getName())
                        .setTeachingCampusName(teachingCampus.getCampusName())
                        .setClassroomTypeName(classroom.getType().getName());
            }
            backList.add(back);
        }
        return backList;
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
     *
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

    @Override
    public BackClassAssignmentDTO exchange(ClassAssignmentDTO dto) {
        BackClassAssignmentDTO back = BeanUtil.toBean(dto, BackClassAssignmentDTO.class);
        back.setClassTimeDTO(ProjectUtil.convertToClassTimeDTOList(JSONUtil.toList(
                dto.getClassTime(), TimeSlotDTO.class)));
        return back;
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
     *
     * @param classAssignment    排课分配对象
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
