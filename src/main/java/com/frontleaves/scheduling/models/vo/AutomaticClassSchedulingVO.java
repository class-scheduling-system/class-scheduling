/*
 * --------------------------------------------------------------------------------
 * Copyright (c) 2022-NOW(至今) 锋楪技术团队
 * Author: 锋楪技术团队 (https://www.frontleaves.com)
 *
 * 本文件包含锋楪技术团队项目的源代码，项目的所有源代码均遵循 MIT 开源许可证协议。
 * --------------------------------------------------------------------------------
 * 许可证声明：
 *
 * 版权所有 (c) 2022-2025 锋楪技术团队。保留所有权利。
 *
 * 本软件是"按原样"提供的，没有任何形式的明示或暗示的保证，包括但不限于
 * 对适销性、特定用途的适用性和非侵权性的暗示保证。在任何情况下，
 * 作者或版权持有人均不承担因软件或软件的使用或其他交易而产生的、
 * 由此引起的或以任何方式与此软件有关的任何索赔、损害或其他责任。
 *
 * 使用本软件即表示您了解此声明并同意其条款。
 *
 * 有关 MIT 许可证的更多信息，请查看项目根目录下的 LICENSE 文件或访问：
 * https://opensource.org/licenses/MIT
 * --------------------------------------------------------------------------------
 * 免责声明：
 *
 * 使用本软件的风险由用户自担。作者或版权持有人在法律允许的最大范围内，
 * 对因使用本软件内容而导致的任何直接或间接的损失不承担任何责任。
 * --------------------------------------------------------------------------------
 */

package com.frontleaves.scheduling.models.vo;

import enums.StrategyEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotBlank(message = "学期主键不能为空")
    private String semesterUuid;
    /**
     * 院系UUID，对应cs_department表的department_uuid，限定排课范围
     */
    @NotBlank(message = "部门主键不能为空")
    private String departmentUuid;
    /**
     * 排课策略，可选: optimal(最优), balanced(平衡), quick(快速)
     * 将根据策略自动设置算法参数
     */
    private StrategyEnum strategy;
    /**
     * 排课约束
     */
    @NotNull(message = "排课约束不能为空")
    private Constraints constraints;
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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Constraints {
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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrioritySettings {
        /**
         * 课程类型优先级设置
         */
        private List<CourseTypePriority> courseTypes;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
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
            private Short priority;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
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
        private List<PreferredTimeSlot> preferredTimeSlots;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PreferredTimeSlot {
            /**
             * 星期几（1-7，1表示周一）
             */
            @NotNull(message = "星期几不能为空")
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
            @Max(value = 12, message = "结束节次必须小于等于 12")
            private Integer periodEnd;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScopeSettings {
        /**
         * 指定课程ID列表，当includeAllSemesterCourses为false时使用
         */
        private List<SpecificCourseIdVO> specificCourseIds;
        /**
         * 允许的教学楼ID列表
         */
        private List<String> allowedBuildingIds;
    }
}
