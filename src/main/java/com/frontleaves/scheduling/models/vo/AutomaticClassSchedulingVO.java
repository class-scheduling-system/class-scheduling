package com.frontleaves.scheduling.models.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 自动排课视图对象
 *
 * @author FLASHLACK
 */
@Getter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class AutomaticClassSchedulingVO {
    /**
     * 学期UUID，对应cs_semester表的semester_uuid
     **/
    @NotBlank(message = "学期ID不能为空")
    private String semesterId;
    /**
     * 院系UUID，对应cs_department表的department_uuid，可选，限定排课范围
     */
    private String departmentId;
    /**
     * 排课策略，可选: optimal(最优), balanced(平衡), quick(快速)
     */
    @NotBlank(message = "排课策略不能为空")
    private String strategy;
    /**
     * 排课约束
     */
    @NotNull(message = "排课约束不能为空")
    private Constraints constraints;
    /**
     * 算法参数
     */
    @NotNull(message = "算法参数不能为空")
    private AlgorithmParams algorithmParams;
    /**
     * 优先级设置
     */
    @NotNull(message = "优先级设置不能为空")
    private PrioritySettings prioritySettings;
    /**
     * 时间偏好
     */
    @NotNull(message = "时间偏好不能为空")
    private TimePreferences timePreferences;
    /**
     * 排课范围设置
     */
    @NotNull(message = "排课范围设置不能为空")
    private ScopeSettings scopeSettings;

    @Data
    private static class Constraints {
        /**
         * 是否考虑教师时间偏好
         */
        @NotNull(message = "是否考虑教师时间偏好不能为空")
        Boolean teacherPreference;
        /**
         * 是否优化教室资源分配
         */
        @NotNull(message = "是否优化教室资源分配不能为空")
        Boolean roomOptimization;
        /**
         * 是否避免学生班级冲突
         */
        @NotNull(message = "是否避免学生班级冲突不能为空")
        Boolean studentConflictAvoidance;
        /**
         * 是否优先安排连堂课
         */
        @NotNull(message = "是否优先安排连堂课不能为空")
        Boolean consecutiveCoursesPreferred;
        /**
         * 专业教室匹配(如实验课安排在实验室)
         */
        @NotNull(message = "专业教室匹配不能为空")
        Boolean specializationRoomMatching;
    }

    @Data
    public static class AlgorithmParams {
        /**
         * 种群大小
         */
        @NotNull(message = "种群大小不能为空")
        @Min(value = 1, message = "种群大小必须大于 0")
        private Integer populationSize;
        /**
         * 最大迭代次数
         */
        @NotNull(message = "最大迭代次数不能为空")
        @Min(value = 1, message = "最大迭代次数必须大于 0")
        private Integer maxIterations;
        /**
         * 交叉率
         */
        @NotNull(message = "交叉率不能为空")
        @DecimalMin(value = "0.0", message = "交叉率必须大于等于 0")
        @DecimalMax(value = "1.0", message = "交叉率必须小于等于 1")
        private Double crossoverRate;
        /**
         * 变异率
         */
        @NotNull(message = "变异率不能为空")
        @DecimalMin(value = "0.0", message = "变异率必须大于等于 0")
        @DecimalMax(value = "1.0", message = "变异率必须小于等于 1")
        private Double mutationRate;
    }

    @Data
    public static class PrioritySettings {
        /**
         * 课程类型优先级设置
         */
        @NotNull(message = "课程类型优先级设置不能为空")
        @Size(min = 1, message = "至少设置一个课程类型优先级")
        @Valid
        private List<CourseTypePriority> courseTypes;

        @Data
        public static class CourseTypePriority {
            /**
             * 课程类型ID，对应cs_course_type表的course_type_uuid
             */
            @NotBlank(message = "课程类型ID不能为空")
            private String typeId;
            /**
             * 优先级
             */
            @NotNull(message = "优先级不能为空")
            @Min(value = 1, message = "优先级必须大于等于 1")
            private Integer priority;
        }
    }

    @Data
    public static class TimePreferences {
        /**
         * 是否避免晚间课程安排
         */
        @NotNull(message = "是否避免晚间课程安排不能为空")
        private Boolean avoidEveningCourses;
        /**
         * 是否平衡周内课程分布
         */
        @NotNull(message = "是否平衡周内课程分布不能为空")
        private Boolean balanceWeekdayCourses;
        /**
         * 优先时间段
         */
        @NotNull(message = "优先时间段不能为空")
        @Size(min = 1, message = "至少设置一个优先时间段")
        @Valid
        private List<PreferredTimeSlot> preferredTimeSlots;

        @Data
        public static class PreferredTimeSlot {
            /**
             * 星期几（1-7，1表示周一）
             */
            @NotNull(message = "星期几不能为空")
            @Min(value = 1, message = "星期几必须大于等于 1")
            @Max(value = 7, message = "星期几必须小于等于 7")
            private Integer day;
            /**
             * 开始节次
             */
            @NotNull(message = "开始节次不能为空")
            @Min(value = 1, message = "开始节次必须大于等于 1")
            private Integer periodStart;
            /**
             * 结束节次
             */
            @NotNull(message = "结束节次不能为空")
            @Min(value = 1, message = "结束节次必须大于等于 1")
            private Integer periodEnd;
        }
    }

    @Data
    public static class ScopeSettings {
        /**
         * 是否包含所有学期课程
         */
        @NotNull(message = "是否包含所有学期课程不能为空")
        private Boolean includeAllSemesterCourses;
        /**
         * 指定课程ID列表，当includeAllSemesterCourses为false时使用
         */
        private List<String> specificCourseIds;
        /**
         * 排除的课程ID列表
         */
        private List<String> excludeCourseIds;
    }
}
