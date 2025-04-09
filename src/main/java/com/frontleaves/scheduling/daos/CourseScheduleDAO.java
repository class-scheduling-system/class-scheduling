package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.schedule.CourseScheduleDTO;
import com.frontleaves.scheduling.models.entity.base.*;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 课程表数据访问对象
 * <p>
 * 该类提供了对课程表数据的操作方法，包括从Redis或数据库中获取课程表信息。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CourseScheduleDAO {

    private final RedissonClient redisson;
    private final ClassAssignmentDAO classAssignmentDAO;
    private final SemesterDAO semesterDAO;
    private final TeacherDAO teacherDAO;
    private final CourseLibraryDAO courseLibraryDAO;
    private final CampusDAO campusDAO;
    private final BuildingDAO buildingDAO;
    private final ClassroomDAO classroomDAO;
    private final TeachingClassDAO teachingClassDAO;
    private final CreditHourTypeDAO creditHourTypeDAO;
    private final StudentDAO studentDAO;
    
    /**
     * 获取当前学期
     * <p>
     * 该方法用于获取当前激活的学期信息。
     * 优先从Redis缓存中获取，如果不存在则查询数据库。
     * </p>
     *
     * @return 返回当前学期信息，如果没有当前学期则抛出异常
     */
    public SemesterDO getCurrentSemester() {
        // 尝试从Redis缓存获取当前学期
        RMap<String, String> rMap = redisson.getMap(StringConstant.Redis.CURRENT_SEMESTER);
        
        if (!rMap.isExists()) {
            // 如果缓存中不存在，从数据库查询
            SemesterDO currentSemester = semesterDAO.lambdaQuery()
                    .eq(SemesterDO::getIsCurrent, true)
                    .eq(SemesterDO::getIsEnabled, true)
                    .one();
            
            if (currentSemester == null) {
                throw new BusinessException("当前没有激活的学期", ErrorCode.NOT_EXIST);
            }
            
            // 将查询结果缓存到Redis
            Map<String, String> map = BeanUtil.beanToMap(currentSemester, false, true)
                    .entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString()));
            
            rMap.putAll(map);
            rMap.expire(Duration.ofHours(1));
            
            return currentSemester;
        } else {
            // 如果缓存中存在，转换成对象并返回
            return BeanUtil.toBean(rMap, SemesterDO.class);
        }
    }
    
    /**
     * 获取教师课程表
     * <p>
     * 该方法用于获取指定学期中特定教师的课程表信息。
     * 优先从Redis缓存中获取，如果不存在则查询数据库并组装数据。
     * </p>
     *
     * @param teacherUuid  教师UUID
     * @param semesterUuid 学期UUID，如果为null则获取当前学期
     * @return 返回教师课程表信息
     */
    public CourseScheduleDTO getTeacherCourseSchedule(String teacherUuid, String semesterUuid) {
        if (teacherUuid == null) {
            throw new BusinessException("教师UUID不能为空", ErrorCode.PARAMETER_INVALID);
        }
        
        // 如果未指定学期，使用当前学期
        if (semesterUuid == null) {
            SemesterDO currentSemester = getCurrentSemester();
            semesterUuid = currentSemester.getSemesterUuid();
        }
        
        // 构建缓存键
        String cacheKey = StringConstant.Redis.TEACHER_COURSE_SCHEDULE + teacherUuid + ":" + semesterUuid;
        RMap<String, String> rMap = redisson.getMap(cacheKey);
        
        if (!rMap.isExists()) {
            // 如果缓存不存在，从数据库查询并组装数据
            CourseScheduleDTO scheduleDTO = buildTeacherCourseSchedule(teacherUuid, semesterUuid);
            
            // 缓存结果
            String jsonString = JSONUtil.toJsonStr(scheduleDTO);
            rMap.put("data", jsonString);
            rMap.expire(Duration.ofHours(1));
            
            return scheduleDTO;
        } else {
            // 从缓存读取
            String jsonString = rMap.get("data");
            return JSONUtil.toBean(jsonString, CourseScheduleDTO.class);
        }
    }
    
    /**
     * 获取学生课程表
     * <p>
     * 该方法用于获取指定学期中特定学生的课程表信息。
     * 优先从Redis缓存中获取，如果不存在则查询数据库并组装数据。
     * </p>
     *
     * @param studentUuid  学生UUID
     * @param semesterUuid 学期UUID，如果为null则获取当前学期
     * @return 返回学生课程表信息
     */
    public CourseScheduleDTO getStudentCourseSchedule(String studentUuid, String semesterUuid) {
        if (studentUuid == null) {
            throw new BusinessException("学生UUID不能为空", ErrorCode.PARAMETER_INVALID);
        }
        
        // 如果未指定学期，使用当前学期
        if (semesterUuid == null) {
            SemesterDO currentSemester = getCurrentSemester();
            semesterUuid = currentSemester.getSemesterUuid();
        }
        
        // 构建缓存键
        String cacheKey = StringConstant.Redis.STUDENT_COURSE_SCHEDULE + studentUuid + ":" + semesterUuid;
        RMap<String, String> rMap = redisson.getMap(cacheKey);
        
        if (!rMap.isExists()) {
            // 如果缓存不存在，从数据库查询并组装数据
            CourseScheduleDTO scheduleDTO = buildStudentCourseSchedule(studentUuid, semesterUuid);
            
            // 缓存结果
            String jsonString = JSONUtil.toJsonStr(scheduleDTO);
            rMap.put("data", jsonString);
            rMap.expire(Duration.ofHours(1));
            
            return scheduleDTO;
        } else {
            // 从缓存读取
            String jsonString = rMap.get("data");
            return JSONUtil.toBean(jsonString, CourseScheduleDTO.class);
        }
    }
    
    /**
     * 构建教师课程表
     * <p>
     * 该私有方法用于从数据库中查询并组装教师课程表数据。
     * </p>
     *
     * @param teacherUuid  教师UUID
     * @param semesterUuid 学期UUID
     * @return 返回组装的课程表数据
     */
    private CourseScheduleDTO buildTeacherCourseSchedule(String teacherUuid, String semesterUuid) {
        CourseScheduleDTO scheduleDTO = new CourseScheduleDTO();
        
        // 获取学期信息
        SemesterDO semesterDO = semesterDAO.getById(semesterUuid);
        if (semesterDO == null) {
            throw new BusinessException("学期不存在", ErrorCode.NOT_EXIST);
        }
        
        // 设置学期信息
        CourseScheduleDTO.SemesterInfo semesterInfo = new CourseScheduleDTO.SemesterInfo()
                .setSemesterUuid(semesterDO.getSemesterUuid())
                .setSemesterName(semesterDO.getName())
                .setStartDate(semesterDO.getStartDate())
                .setEndDate(semesterDO.getEndDate());
        scheduleDTO.setSemester(semesterInfo);
        
        // 查询教师在该学期的所有排课信息
        List<ClassAssignmentDO> assignments = classAssignmentDAO.lambdaQuery()
                .eq(ClassAssignmentDO::getSemesterUuid, semesterUuid)
                .eq(ClassAssignmentDO::getTeacherUuid, teacherUuid)
                .list();
        
        // 收集所有需要的ID列表
        List<String> courseIds = assignments.stream()
                .map(ClassAssignmentDO::getCourseUuid)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        List<String> campusIds = assignments.stream()
                .map(ClassAssignmentDO::getCampusUuid)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        List<String> buildingIds = assignments.stream()
                .map(ClassAssignmentDO::getBuildingUuid)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        List<String> classroomIds = assignments.stream()
                .map(ClassAssignmentDO::getClassroomUuid)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        List<String> teachingClassIds = assignments.stream()
                .map(ClassAssignmentDO::getTeachingClassUuid)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        List<String> creditHourTypeIds = assignments.stream()
                .map(ClassAssignmentDO::getCreditHourType)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        // 批量查询所有相关数据
        Map<String, CourseLibraryDO> courseMap = courseLibraryDAO.listByIds(courseIds).stream()
                .collect(Collectors.toMap(CourseLibraryDO::getCourseLibraryUuid, course -> course));
        
        Map<String, CampusDO> campusMap = campusDAO.listByIds(campusIds).stream()
                .collect(Collectors.toMap(CampusDO::getCampusUuid, campus -> campus));
        
        Map<String, BuildingDO> buildingMap = buildingDAO.listByIds(buildingIds).stream()
                .collect(Collectors.toMap(BuildingDO::getBuildingUuid, building -> building));
        
        Map<String, ClassroomDO> classroomMap = classroomDAO.listByIds(classroomIds).stream()
                .collect(Collectors.toMap(ClassroomDO::getClassroomUuid, classroom -> classroom));
        
        Map<String, TeachingClassDO> teachingClassMap = teachingClassDAO.listByIds(teachingClassIds).stream()
                .collect(Collectors.toMap(TeachingClassDO::getTeachingClassUuid, teachingClass -> teachingClass));
        
        Map<String, CreditHourTypeDO> creditHourTypeMap = creditHourTypeDAO.listByIds(creditHourTypeIds).stream()
                .collect(Collectors.toMap(CreditHourTypeDO::getCreditHourTypeUuid, creditHourType -> creditHourType));
        
        // 获取教师信息
        TeacherDO teacherDO = teacherDAO.getById(teacherUuid);
        
        // 组装课程表项
        List<CourseScheduleDTO.ScheduleItem> scheduleItems = new ArrayList<>();
        for (ClassAssignmentDO assignment : assignments) {
            // 只处理有上课时间的课程安排
            if (assignment.getClassTime() == null || assignment.getClassTime().isEmpty()) {
                continue;
            }
            
            // 解析上课时间 - 支持两种格式：对象格式和数组格式
            try {
                String classTimeStr = assignment.getClassTime().trim();
                
                // 检查是否是数组格式 [{"day":1,"week":1,"period":9},...]
                if (classTimeStr.startsWith("[") && classTimeStr.endsWith("]")) {
                    processArrayTimeFormat(classTimeStr, assignment, scheduleItems, teacherDO, 
                            courseMap, teachingClassMap, campusMap, buildingMap, classroomMap, creditHourTypeMap);
                    continue;
                }
                
                // 处理对象格式 {"dayOfWeek":1,"startSlot":1,"endSlot":2}
                if (classTimeStr.startsWith("{") && classTimeStr.endsWith("}")) {
                    JSONObject classTimeJson = JSONUtil.parseObj(classTimeStr);
                    
                    Integer dayOfWeek = classTimeJson.getInt("dayOfWeek");
                    Integer startSlot = classTimeJson.getInt("startSlot");
                    Integer endSlot = classTimeJson.getInt("endSlot");
                    
                    if (dayOfWeek == null || startSlot == null || endSlot == null) {
                        log.warn("课程时间缺少必要字段: {}", classTimeStr);
                        continue;
                    }
                    
                    // 创建课程表项
                    CourseScheduleDTO.ScheduleItem item = createScheduleItem(assignment, dayOfWeek, startSlot, endSlot,
                            teacherUuid, teacherDO, courseMap, teachingClassMap, campusMap, buildingMap, classroomMap, creditHourTypeMap);
                    scheduleItems.add(item);
                    continue;
                }
                
                // 既不是对象也不是数组格式
                log.warn("无效的课程时间格式: {}", classTimeStr);
            } catch (Exception e) {
                log.error("解析课程时间异常: {}, 原始数据: {}", e.getMessage(), assignment.getClassTime());
            }
        }
        
        scheduleDTO.setScheduleItems(scheduleItems);
        return scheduleDTO;
    }
    
    /**
     * 构建学生课程表
     * <p>
     * 该私有方法用于从数据库中查询并组装学生课程表数据。
     * </p>
     *
     * @param studentUuid  学生UUID
     * @param semesterUuid 学期UUID
     * @return 返回组装的课程表数据
     */
    private CourseScheduleDTO buildStudentCourseSchedule(String studentUuid, String semesterUuid) {
        CourseScheduleDTO scheduleDTO = new CourseScheduleDTO();
        
        // 获取学期信息
        SemesterDO semesterDO = semesterDAO.getById(semesterUuid);
        if (semesterDO == null) {
            throw new BusinessException("学期不存在", ErrorCode.NOT_EXIST);
        }
        
        // 设置学期信息
        CourseScheduleDTO.SemesterInfo semesterInfo = new CourseScheduleDTO.SemesterInfo()
                .setSemesterUuid(semesterDO.getSemesterUuid())
                .setSemesterName(semesterDO.getName())
                .setStartDate(semesterDO.getStartDate())
                .setEndDate(semesterDO.getEndDate());
        scheduleDTO.setSemester(semesterInfo);
        
        // 获取学生所在的行政班级
        StudentDO student = this.getSudentByUuid(studentUuid);
        if (student == null) {
            throw new BusinessException("学生不存在", ErrorCode.NOT_EXIST);
        }
        
        // 获取学生的行政班级UUID
        String administrativeClassUuid = student.getClazz();
        if (administrativeClassUuid == null || administrativeClassUuid.isEmpty()) {
            log.warn("学生{}没有关联行政班级", studentUuid);
            scheduleDTO.setScheduleItems(new ArrayList<>());
            return scheduleDTO;
        }
        
        log.info("获取学生{}所在行政班级{}的课程表", studentUuid, administrativeClassUuid);
        
        try {
            // 获取学生所在行政班级关联的教学班
            // 由于administrativeClasses是JSON格式，使用like查询可能不够精确
            // 先获取学期内所有教学班
            List<TeachingClassDO> allTeachingClasses = teachingClassDAO.lambdaQuery()
                    .eq(TeachingClassDO::getSemesterUuid, semesterUuid)
                    .eq(TeachingClassDO::getIsEnabled, true)
                    .list();
            
            // 筛选出包含学生所在行政班级的教学班
            List<TeachingClassDO> teachingClasses = allTeachingClasses.stream()
                    .filter(teachingClass -> {
                        String administrativeClasses = teachingClass.getAdministrativeClasses();
                        // 仅当行政班级信息包含学生的行政班级UUID时才选中
                        return administrativeClasses != null && 
                               administrativeClasses.contains(administrativeClassUuid);
                    })
                    .collect(Collectors.toList());
            
            if (teachingClasses.isEmpty()) {
                log.warn("未找到学生{}所在行政班级{}相关的教学班", studentUuid, administrativeClassUuid);
                scheduleDTO.setScheduleItems(new ArrayList<>());
                return scheduleDTO;
            }
            
            log.info("找到学生{}所在行政班级{}相关的教学班{}个", studentUuid, administrativeClassUuid, teachingClasses.size());
            
            // 获取这些教学班对应的课程安排
            List<String> teachingClassIds = teachingClasses.stream()
                    .map(TeachingClassDO::getTeachingClassUuid)
                    .collect(Collectors.toList());
            
            // 查询所有相关的课程安排
            List<ClassAssignmentDO> assignments = classAssignmentDAO.lambdaQuery()
                    .eq(ClassAssignmentDO::getSemesterUuid, semesterUuid)
                    .in(ClassAssignmentDO::getTeachingClassUuid, teachingClassIds)
                    .list();
            
            if (assignments.isEmpty()) {
                log.warn("未找到学生{}所在教学班的课程安排", studentUuid);
                scheduleDTO.setScheduleItems(new ArrayList<>());
                return scheduleDTO;
            }
            
            log.info("找到学生{}的课程安排{}个", studentUuid, assignments.size());
            
            // 收集所有需要的ID列表
            List<String> courseIds = assignments.stream()
                    .map(ClassAssignmentDO::getCourseUuid)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            
            List<String> teacherIds = assignments.stream()
                    .map(ClassAssignmentDO::getTeacherUuid)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            
            List<String> campusIds = assignments.stream()
                    .map(ClassAssignmentDO::getCampusUuid)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            
            List<String> buildingIds = assignments.stream()
                    .map(ClassAssignmentDO::getBuildingUuid)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            
            List<String> classroomIds = assignments.stream()
                    .map(ClassAssignmentDO::getClassroomUuid)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            
            List<String> creditHourTypeIds = assignments.stream()
                    .map(ClassAssignmentDO::getCreditHourType)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            
            // 批量查询所有相关数据
            Map<String, CourseLibraryDO> courseMap = courseLibraryDAO.listByIds(courseIds).stream()
                    .collect(Collectors.toMap(CourseLibraryDO::getCourseLibraryUuid, course -> course));
            
            Map<String, TeacherDO> teacherMap = teacherDAO.listByIds(teacherIds).stream()
                    .collect(Collectors.toMap(TeacherDO::getTeacherUuid, teacher -> teacher));
            
            Map<String, CampusDO> campusMap = campusDAO.listByIds(campusIds).stream()
                    .collect(Collectors.toMap(CampusDO::getCampusUuid, campus -> campus));
            
            Map<String, BuildingDO> buildingMap = buildingDAO.listByIds(buildingIds).stream()
                    .collect(Collectors.toMap(BuildingDO::getBuildingUuid, building -> building));
            
            Map<String, ClassroomDO> classroomMap = classroomDAO.listByIds(classroomIds).stream()
                    .collect(Collectors.toMap(ClassroomDO::getClassroomUuid, classroom -> classroom));
            
            Map<String, TeachingClassDO> teachingClassMap = teachingClasses.stream()
                    .collect(Collectors.toMap(TeachingClassDO::getTeachingClassUuid, teachingClass -> teachingClass));
            
            Map<String, CreditHourTypeDO> creditHourTypeMap = creditHourTypeDAO.listByIds(creditHourTypeIds).stream()
                    .collect(Collectors.toMap(CreditHourTypeDO::getCreditHourTypeUuid, creditHourType -> creditHourType));
            
            // 组装课程表项
            List<CourseScheduleDTO.ScheduleItem> scheduleItems = new ArrayList<>();
            for (ClassAssignmentDO assignment : assignments) {
                // 只处理有上课时间的课程安排
                if (assignment.getClassTime() == null || assignment.getClassTime().isEmpty()) {
                    continue;
                }
                
                // 解析上课时间 - 支持两种格式：对象格式和数组格式
                try {
                    String classTimeStr = assignment.getClassTime().trim();
                    
                    // 检查是否是数组格式 [{"day":1,"week":1,"period":9},...]
                    if (classTimeStr.startsWith("[") && classTimeStr.endsWith("]")) {
                        String teacherUuid = assignment.getTeacherUuid();
                        TeacherDO teacher = teacherMap.get(teacherUuid);
                        processArrayTimeFormat(classTimeStr, assignment, scheduleItems, teacher, 
                                courseMap, teachingClassMap, campusMap, buildingMap, classroomMap, creditHourTypeMap);
                        continue;
                    }
                    
                    // 处理对象格式 {"dayOfWeek":1,"startSlot":1,"endSlot":2}
                    if (classTimeStr.startsWith("{") && classTimeStr.endsWith("}")) {
                        JSONObject classTimeJson = JSONUtil.parseObj(classTimeStr);
                        
                        Integer dayOfWeek = classTimeJson.getInt("dayOfWeek");
                        Integer startSlot = classTimeJson.getInt("startSlot");
                        Integer endSlot = classTimeJson.getInt("endSlot");
                        
                        if (dayOfWeek == null || startSlot == null || endSlot == null) {
                            log.warn("课程时间缺少必要字段: {}", classTimeStr);
                            continue;
                        }
                        
                        // 创建课程表项
                        String teacherUuid = assignment.getTeacherUuid();
                        TeacherDO teacher = teacherMap.get(teacherUuid);
                        CourseScheduleDTO.ScheduleItem item = createScheduleItem(assignment, dayOfWeek, startSlot, endSlot,
                                teacherUuid, teacher, courseMap, teachingClassMap, campusMap, buildingMap, classroomMap, creditHourTypeMap);
                        scheduleItems.add(item);
                        continue;
                    }
                    
                    // 既不是对象也不是数组格式
                    log.warn("无效的课程时间格式: {}", classTimeStr);
                } catch (Exception e) {
                    log.error("解析课程时间异常: {}, 原始数据: {}", e.getMessage(), assignment.getClassTime());
                }
            }
            
            scheduleDTO.setScheduleItems(scheduleItems);
            return scheduleDTO;
        } catch (Exception e) {
            log.error("构建学生课程表异常: {}", e.getMessage(), e);
            // 返回空课程表，避免前端显示错误
            scheduleDTO.setScheduleItems(new ArrayList<>());
            return scheduleDTO;
        }
    }
    
    /**
     * 处理数组格式的时间数据
     */
    private void processArrayTimeFormat(String classTimeStr, ClassAssignmentDO assignment, 
                                      List<CourseScheduleDTO.ScheduleItem> scheduleItems,
                                      TeacherDO teacher, Map<String, CourseLibraryDO> courseMap, 
                                      Map<String, TeachingClassDO> teachingClassMap,
                                      Map<String, CampusDO> campusMap, Map<String, BuildingDO> buildingMap,
                                      Map<String, ClassroomDO> classroomMap, Map<String, CreditHourTypeDO> creditHourTypeMap) {
        try {
            // 解析为数组
            List<JSONObject> timeSlots = JSONUtil.parseArray(classTimeStr).toList(JSONObject.class);
            
            // 按周次和星期分组
            Map<Integer, Map<Integer, List<Integer>>> weekDayPeriodMap = new HashMap<>();
            
            // 遍历所有时间槽，按周次和星期分组
            for (JSONObject slot : timeSlots) {
                Integer week = slot.getInt("week");
                Integer day = slot.getInt("day");
                Integer period = slot.getInt("period");
                
                if (week == null || day == null || period == null) {
                    continue;
                }
                
                // 获取或创建该周的映射
                Map<Integer, List<Integer>> dayPeriodMap = weekDayPeriodMap.computeIfAbsent(week, k -> new HashMap<>());
                
                // 获取或创建该星期的课时列表
                List<Integer> periods = dayPeriodMap.computeIfAbsent(day, k -> new ArrayList<>());
                
                // 添加课时
                periods.add(period);
            }
            
            // 为每周的每天创建课程项
            for (Map.Entry<Integer, Map<Integer, List<Integer>>> weekEntry : weekDayPeriodMap.entrySet()) {
                Integer week = weekEntry.getKey();
                Map<Integer, List<Integer>> dayPeriodMap = weekEntry.getValue();
                
                for (Map.Entry<Integer, List<Integer>> dayEntry : dayPeriodMap.entrySet()) {
                    Integer day = dayEntry.getKey();
                    List<Integer> periods = dayEntry.getValue();
                    
                    // 对课时排序
                    Collections.sort(periods);
                    
                    // 查找连续的课时段
                    List<List<Integer>> consecutivePeriods = findConsecutivePeriods(periods);
                    
                    // 为每个连续段创建一个课程项
                    for (List<Integer> consecutiveSegment : consecutivePeriods) {
                        if (consecutiveSegment.isEmpty()) continue;
                        
                        Integer startSlot = consecutiveSegment.get(0);
                        Integer endSlot = consecutiveSegment.get(consecutiveSegment.size() - 1);
                        
                        // 创建课程表项
                        String teacherUuid = assignment.getTeacherUuid();
                        CourseScheduleDTO.ScheduleItem item = createScheduleItem(assignment, day, startSlot, endSlot,
                                teacherUuid, teacher, courseMap, teachingClassMap, campusMap, buildingMap, classroomMap, creditHourTypeMap);
                        
                        // 添加周次信息
                        item.setWeek(week);
                        
                        scheduleItems.add(item);
                    }
                }
            }
        } catch (Exception e) {
            log.error("处理数组格式时间数据异常: {}", e.getMessage());
        }
    }
    
    /**
     * 查找连续的课时段
     */
    private List<List<Integer>> findConsecutivePeriods(List<Integer> periods) {
        List<List<Integer>> result = new ArrayList<>();
        if (periods.isEmpty()) return result;
        
        List<Integer> current = new ArrayList<>();
        current.add(periods.get(0));
        
        for (int i = 1; i < periods.size(); i++) {
            // 如果与前一个课时连续
            if (periods.get(i) == periods.get(i-1) + 1) {
                current.add(periods.get(i));
            } else {
                // 不连续，保存当前连续段并开始新段
                result.add(new ArrayList<>(current));
                current.clear();
                current.add(periods.get(i));
            }
        }
        
        // 添加最后一个连续段
        if (!current.isEmpty()) {
            result.add(current);
        }
        
        return result;
    }
    
    /**
     * 创建课程表项
     */
    private CourseScheduleDTO.ScheduleItem createScheduleItem(ClassAssignmentDO assignment, Integer dayOfWeek, 
                                                            Integer startSlot, Integer endSlot,
                                                            String teacherUuid, TeacherDO teacher,
                                                            Map<String, CourseLibraryDO> courseMap, 
                                                            Map<String, TeachingClassDO> teachingClassMap,
                                                            Map<String, CampusDO> campusMap, 
                                                            Map<String, BuildingDO> buildingMap,
                                                            Map<String, ClassroomDO> classroomMap, 
                                                            Map<String, CreditHourTypeDO> creditHourTypeMap) {
        CourseScheduleDTO.ScheduleItem item = new CourseScheduleDTO.ScheduleItem();
        item.setClassAssignmentUuid(assignment.getClassAssignmentUuid());
        item.setDayOfWeek(dayOfWeek);
        item.setStartSlot(startSlot);
        item.setEndSlot(endSlot);
        item.setConsecutiveSessions(assignment.getConsecutiveSessions());
        item.setTotalHours(assignment.getTotalHours());
        
        // 设置课程信息
        String courseUuid = assignment.getCourseUuid();
        CourseLibraryDO course = courseMap.get(courseUuid);
        if (course != null) {
            item.setCourseUuid(courseUuid);
            item.setCourseName(course.getName());
        }
        
        // 设置教师信息
        if (teacher != null) {
            item.setTeacherUuid(teacherUuid);
            item.setTeacherName(teacher.getName());
        }
        
        // 设置教学班信息
        String teachingClassUuid = assignment.getTeachingClassUuid();
        TeachingClassDO teachingClass = teachingClassMap.get(teachingClassUuid);
        if (teachingClass != null) {
            item.setTeachingClassUuid(teachingClassUuid);
            item.setTeachingClassName(teachingClass.getTeachingClassName());
        }
        
        // 设置校区信息
        String campusUuid = assignment.getCampusUuid();
        CampusDO campus = campusMap.get(campusUuid);
        if (campus != null) {
            item.setCampusUuid(campusUuid);
            item.setCampusName(campus.getCampusName());
        }
        
        // 设置教学楼信息
        String buildingUuid = assignment.getBuildingUuid();
        BuildingDO building = buildingMap.get(buildingUuid);
        if (building != null) {
            item.setBuildingUuid(buildingUuid);
            item.setBuildingName(building.getBuildingName());
        }
        
        // 设置教室信息
        String classroomUuid = assignment.getClassroomUuid();
        ClassroomDO classroom = classroomMap.get(classroomUuid);
        if (classroom != null) {
            item.setClassroomUuid(classroomUuid);
            item.setClassroomName(classroom.getName());
        }
        
        // 设置学时类型信息
        String creditHourTypeUuid = assignment.getCreditHourType();
        CreditHourTypeDO creditHourType = creditHourTypeMap.get(creditHourTypeUuid);
        if (creditHourType != null) {
            item.setCreditHourTypeUuid(creditHourTypeUuid);
            item.setCreditHourTypeName(creditHourType.getName());
        }
        
        return item;
    }
    
    /**
     * 根据UUID获取学生信息
     * <p>
     * 该方法用于获取学生的详细信息。
     * </p>
     *
     * @param studentUuid 学生UUID
     * @return 返回学生信息，如果不存在则返回null
     */
    private StudentDO getSudentByUuid(String studentUuid) {
        // 从 StudentDAO 获取学生信息
        return studentDAO.getById(studentUuid);
    }
} 
 