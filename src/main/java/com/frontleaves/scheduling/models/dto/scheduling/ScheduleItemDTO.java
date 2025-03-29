package com.frontleaves.scheduling.models.dto.scheduling;

import com.frontleaves.scheduling.models.dto.ClassroomAndTypeDTO;
import com.frontleaves.scheduling.models.dto.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.TeacherCoursePreferencesDTO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 课程安排项 DTO
 */
@Data
@Accessors(chain = true)
public class ScheduleItemDTO {
    private final CourseLibraryDTO course;
    private final TeacherCoursePreferencesDTO teacher;
    private final ClassroomAndTypeDTO classroom;
    private final Short priority;
}