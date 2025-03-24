package com.frontleaves.scheduling.models.vo;

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
    private String semesterId;
    /**
     * 院系UUID，对应cs_department表的department_uuid，可选，限定排课范围
     */
    private String departmentId;
    /**
     * 排课策略，可选: optimal(最优), balanced(平衡), quick(快速)
     */
    private String strategy;
    /**
     * 排课约束
     */
    private Constraints constraints;
    /**
     * 算法参数
     */
    private AlgorithmParams algorithmParams;
    /**
     * 优先级设置
     */
    private PrioritySettings prioritySettings;
    /**
     * 时间偏好
     */
    private TimePreferences timePreferences;
    /**
     * 排课范围设置
     */
    private ScopeSettings scopeSettings;

    @Data
    private static class Constraints {
        /**
         * 是否考虑教师时间偏好
         */
        Boolean teacherPreference;
        /**
         * 是否优化教室资源分配
         */
        Boolean roomOptimization;
        /**
         * 是否避免学生班级冲突
         */
        Boolean studentConflictAvoidance;
        /**
         * 是否优先安排连堂课
         */
        Boolean consecutiveCoursesPreferred;
        /**
         * 专业教室匹配(如实验课安排在实验室)
         */
        Boolean specializationRoomMatching;
    }

    @Data
    public static class AlgorithmParams {
        /**
         * 种群大小
         */
        private Integer populationSize;
        /**
         * 最大迭代次数
         */
        private Integer maxIterations;
        /**
         * 交叉率
         */
        private Double crossoverRate;
        /**
         * 变异率
         */
        private Double mutationRate;
    }

    @Data
    public static class PrioritySettings {
        /**
         * 课程类型优先级设置
         */
        private List<CourseTypePriority> courseTypes;

        @Data
        public static class CourseTypePriority {
            /**
             * 课程类型ID，对应cs_course_type表的course_type_uuid
             */
            private String typeId;
            /**
             * 优先级
             */
            private Integer priority;
        }
    }

    @Data
    public static class TimePreferences {
        /**
         * 是否避免晚间课程安排
         */
        private Boolean avoidEveningCourses;
        /**
         * 是否平衡周内课程分布
         */
        private Boolean balanceWeekdayCourses;
        /**
         * 优先时间段
         */
        private List<PreferredTimeSlot> preferredTimeSlots;

        @Data
        public static class PreferredTimeSlot {
            /**
             * 星期几（1-7，1表示周一）
             */
            private Integer day;
            /**
             * 开始节次
             */
            private Integer periodStart;
            /**
             * 结束节次
             */
            private Integer periodEnd;
        }
    }

    @Data
    public static class ScopeSettings {
        /**
         * 是否包含所有学期课程
         */
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
