package com.frontleaves.scheduling.utils;

import com.frontleaves.scheduling.models.dto.base.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.merge.ClassroomInfoDTO;
import com.frontleaves.scheduling.models.dto.merge.CourseLibraryAndTeacherCourseQualificationListDTO;
import enums.CourseEnuType;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 教室选择工具类
 *
 * @author FLASHLACK
 */
@Slf4j
public final class ClassroomSelectionUtil {

    // 私有构造函数防止实例化
    private ClassroomSelectionUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }


    /**
     * 按类型和容量要求筛选教室
     */
    public static List<ClassroomInfoDTO> findClassroomsByTypeAndCapacity(
            List<ClassroomInfoDTO> classrooms,
            String classroomType,
            int studentCount) {
        if (classrooms == null || classroomType == null) {
            return Collections.emptyList();
        }
        return classrooms.stream()
                .filter(classroom -> classroom.getType().getClassTypeUuid().equals(classroomType))
                .filter(classroom -> classroom.getClassroom().getCapacity() >= studentCount)
                .toList();
    }

    /**
     * 按容量差值排序
     */
    public static List<ClassroomInfoDTO> sortByCapacityDifference(
            List<ClassroomInfoDTO> classrooms,
            int studentCount) {
        if (classrooms == null) {
            return Collections.emptyList();
        }
        return classrooms.stream()
                .sorted((c1, c2) -> {
                    int diff1 = Math.abs(c1.getClassroom().getCapacity() - studentCount);
                    int diff2 = Math.abs(c2.getClassroom().getCapacity() - studentCount);
                    return Integer.compare(diff1, diff2);
                })
                .toList();
    }

    /**
     * 选择随机教室
     */
    public static ClassroomInfoDTO selectRandomClassroom(List<ClassroomInfoDTO> matchingClassrooms) {
        Random random = new SecureRandom();
        random.setSeed(System.currentTimeMillis());
        if (matchingClassrooms == null || matchingClassrooms.isEmpty()) {
            return null;
        }
        log.debug("选择随机教室，匹配教室数：{}", matchingClassrooms.size());
        // 使用整个列表的大小作为范围
        int randomIndex = random.nextInt(matchingClassrooms.size());
        return matchingClassrooms.get(randomIndex);
    }

    /**
     * 获取所需的教室类型
     */
    public static String getRequiredClassroomType(
            CourseLibraryDTO courseLibrary,
            CourseEnuType courseType) {
        if (courseLibrary == null || courseType == null) {
            return null;
        }

        return switch (courseType) {
            case EXPERIMENT -> courseLibrary.getExperimentClassroomType();
            case PRACTICE -> courseLibrary.getPracticeClassroomType();
            case COMPUTER -> courseLibrary.getComputerClassroomType();
            case OTHER, MIXED, THEORY -> courseLibrary.getTheoryClassroomType();
        };
    }

    /**
     * 查找满足最低要求的教室（只考虑容纳学生数）
     */
    public static List<ClassroomInfoDTO> findMinimumRequirementClassrooms(
            List<ClassroomInfoDTO> classrooms,
            CourseLibraryAndTeacherCourseQualificationListDTO course) {
        if (classrooms == null || course == null) {
            return Collections.emptyList();
        }
        String requiredType = getRequiredClassroomType(course.getCourse(), course.getCourseEnuType());
        int studentCount = course.getNumber();
        // 先尝试专业教室
        List<ClassroomInfoDTO> specializedClassrooms = classrooms.stream()
                .filter(classroom -> classroom.getType().getClassTypeUuid().equals(requiredType))
                .filter(classroom -> classroom.getClassroom().getCapacity() >= studentCount)
                .toList();
        // 如果没有合适的专业教室，尝试理论教室
        if (specializedClassrooms.isEmpty()) {
            return classrooms.stream()
                    .filter(classroom -> classroom.getType().getClassTypeUuid()
                            .equals(course.getCourse().getTheoryClassroomType()))
                    .filter(classroom -> classroom.getClassroom().getCapacity() >= studentCount)
                    .toList();
        }

        return specializedClassrooms;
    }

    /**
     * 查找容量最接近的教室
     */
    public static List<ClassroomInfoDTO> findClosestCapacityClassrooms(
            List<ClassroomInfoDTO> classrooms,
            CourseLibraryAndTeacherCourseQualificationListDTO course) {
        if (classrooms == null || course == null) {
            return Collections.emptyList();
        }
        String requiredType = getRequiredClassroomType(course.getCourse(), course.getCourseEnuType());
        int studentCount = course.getNumber();
        // 先尝试在专业教室中找最接近的
        List<ClassroomInfoDTO> closestSpecializedClassrooms = sortByCapacityDifference(
                classrooms.stream()
                        .filter(classroom -> classroom.getType().getClassTypeUuid().equals(requiredType))
                        .toList(), studentCount);
        if (!closestSpecializedClassrooms.isEmpty()) {
            return closestSpecializedClassrooms;
        }
        // 如果没有专业教室，在所有教室中找容量最接近的
        return sortByCapacityDifference(classrooms, studentCount);
    }

    /**
     * 查找合适的教室
     */
    public static List<ClassroomInfoDTO> findSuitableClassrooms(
            List<ClassroomInfoDTO> classrooms,
            CourseLibraryAndTeacherCourseQualificationListDTO course) {
        if (classrooms == null || course == null) {
            return Collections.emptyList();
        }
        // 查找最优教室
        List<ClassroomInfoDTO> matchingClassrooms = findOptimalClassrooms(classrooms, course);
        // 如果没有找到，查找满足最低要求的教室
        if (matchingClassrooms.isEmpty()) {
            matchingClassrooms = findMinimumRequirementClassrooms(classrooms, course);
        }
        // 如果仍未找到，查找容量最接近的教室
        if (matchingClassrooms.isEmpty()) {
            matchingClassrooms = findClosestCapacityClassrooms(classrooms, course);
        }
        return matchingClassrooms;
    }

    /**
     * 查找最优教室（符合专业类型和容量要求）
     */
    public static List<ClassroomInfoDTO> findOptimalClassrooms(
            List<ClassroomInfoDTO> classrooms,
            CourseLibraryAndTeacherCourseQualificationListDTO course) {
        if (classrooms == null || course == null) {
            return Collections.emptyList();
        }
        // 首先尝试专业教室
        List<ClassroomInfoDTO> specializedClassrooms = findClassroomsByTypeAndCapacity(
                classrooms,
                getRequiredClassroomType(course.getCourse(), course.getCourseEnuType()),
                course.getNumber());
        // 如果没有合适的专业教室，尝试理论教室
        if (specializedClassrooms.isEmpty()) {
            return findClassroomsByTypeAndCapacity(
                    classrooms,
                    course.getCourse().getTheoryClassroomType(),
                    course.getNumber());
        }
        return specializedClassrooms;
    }
}