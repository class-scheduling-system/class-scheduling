package com.frontleaves.scheduling.models.dto.scheduling;

import com.frontleaves.scheduling.models.dto.base.AdministrativeClassDTO;
import com.frontleaves.scheduling.models.dto.base.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherCoursePreferencesDTO;
import com.frontleaves.scheduling.models.dto.merge.ClassroomAndTypeDTO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 课程安排项数据传输对象
 * <p>
 * 该类表示排课系统中的单个课程安排项，包含了课程、教师、教室以及课程优先级等信息。
 * 用于在遗传算法排课过程中表示时间槽位上分配的具体资源组合，以及最终排课结果中的课程安排细节。
 * </p>
 * <p>
 * 课程安排项是排课系统的核心数据结构之一，它将课程库、教师资源和教室资源三者关联起来，
 * 构成了排课系统解决方案中的基本单元。
 * </p>
 *
 * @author frontleaves
 * @version 1.0
 */
@Data
@Accessors(chain = true)
public class CourseScheduleItemDTO {
    /**
     * 课程信息
     * <p>
     * 包含课程的基本信息，如课程名称、课程代码、学时数、学分、课程类型等。
     * 这些信息决定了课程的基本属性和对教室类型的需求。
     * </p>
     */
    private final CourseLibraryDTO course;

    /**
     * 教师信息
     * <p>
     * 包含教师的基本信息及其对该课程的教学偏好设置。
     * 教师偏好信息对排课质量有重要影响，如时间偏好、连续授课偏好等。
     * </p>
     */
    private final TeacherCoursePreferencesDTO teacher;

    /**
     * 教室信息
     * <p>
     * 包含教室的基本信息及其类型，如教室容量、位置、设备情况等。
     * 教室信息需要与课程要求相匹配，以满足教学需求。
     * </p>
     */
    private final ClassroomAndTypeDTO classroom;
    /**
     * 班级分组信息
     * <p>
     * 包含班级分组的基本信息，如班级名称、班级人数等。
     * 班级分组信息用于在排课过程中进行班级资源的合理分配。
     * </p>
     */
    private List<AdministrativeClassDTO> classGroup;

    /**
     * 课程优先级
     * <p>
     * 表示该课程在排课过程中的优先级，值越小优先级越高。
     * 高优先级的课程会在排课算法中优先安排，以确保重要课程能够获得合适的时间和资源。
     * </p>
     */
    private final Short priority;

    /**
     * 构造方法
     * <p>
     * 创建一个新的课程安排项实例，包含指定的课程、教师、教室和优先级信息。
     * </p>
     *
     * @param course    课程信息
     * @param teacher   教师信息
     * @param classroom 教室信息
     * @param priority  课程优先级
     */
    public CourseScheduleItemDTO(CourseLibraryDTO course, TeacherCoursePreferencesDTO teacher,
                                 ClassroomAndTypeDTO classroom, List<AdministrativeClassDTO> classGroup, Short priority) {
        this.course = course;
        this.teacher = teacher;
        this.classroom = classroom;
        this.classGroup = classGroup;
        this.priority = priority;
    }

    /**
     * 拷贝构造方法
     * <p>
     * 基于现有的课程安排项创建一个新的课程安排项实例，用于深拷贝操作。
     * 在遗传算法的交叉和变异操作中，需要创建课程安排的深拷贝以避免引用共享。
     * </p>
     *
     * @param other 原课程安排项
     */
    public CourseScheduleItemDTO(CourseScheduleItemDTO other) {
        this.course = other.course;
        this.teacher = other.teacher;
        this.classroom = other.classroom;
        this.priority = other.priority;
        this.classGroup = other.classGroup;
    }
}
