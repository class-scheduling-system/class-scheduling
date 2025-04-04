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
 * 本软件是“按原样”提供的，没有任何形式的明示或暗示的保证，包括但不限于
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

package com.frontleaves.scheduling.models.dto.scheduling;

import com.frontleaves.scheduling.models.dto.base.DepartmentDTO;
import com.frontleaves.scheduling.models.dto.base.SemesterDTO;
import com.frontleaves.scheduling.models.dto.merge.ClassroomAndTypeDTO;
import com.frontleaves.scheduling.models.dto.merge.CourseLibraryAndTeacherCourseQualificationListDTO;
import enums.StrategyEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 自动排课基础数据传输对象（DTO）
 * 该类用于封装自动排课所需的各项参数和配置信息。
 *
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class AutomaticClassSchedulingBaseDTO {
    /**
     * 学期实体
     * 包含学期的基本信息，如学期ID、开始日期、结束日期等。
     **/
    private SemesterDTO semester;

    /**
     * 院系实体
     * 包含院系的基本信息，如院系ID、名称等。
     */
    private DepartmentDTO department;

    /**
     * 排课策略
     * 可选值包括：
     * - optimal(最优): 尽可能满足所有约束条件并优化课程安排。
     * - balanced(平衡): 在满足基本约束的前提下，尽量使课程安排均匀分布。
     * - quick(快速): 快速生成一个初步的课程安排方案。
     */
    private StrategyEnum strategy;


    /**
     * 排课约束
     * 定义了一系列排课时需要遵守的规则和限制条件。
     */
    private Constraints constraints;

    /**
     * 算法参数
     * 提供了遗传算法或其他优化算法所需的关键参数。
     */
    private AlgorithmParams algorithmParams;

    /**
     * 时间偏好
     * 描述了在排课过程中需要考虑的时间方面的偏好。
     */
    private TimePreferences timePreferences;

    /**
     * 课程库和教师课程资格列表DTO
     * <p>
     * 包含以下内容：
     * <li>课程列表：每门课程的基本信息。</li>
     * <li>教师课程资格列表：每个教师可以教授哪些课程的信息。</li>
     * <li>课程优先级：指定某些课程的优先级，以便在排课时给予更多关注。</li>
     * <p>
     * 注意：一门课程可以由多个教师授课(但是一门课只能由一门老师教，只是有一个或多个老师有资格教)，
     * 在教室资格表中会有相应的教师DTO和教师时间偏好DTO。
     */
    private List<CourseLibraryAndTeacherCourseQualificationListDTO> courseList;


    /**
     * 教室和教室类型列表DTO
     * 包含教室及其类型的详细信息，用于辅助排课决策。
     */
    private List<ClassroomAndTypeDTO> classroomList;

    @Data
    @Accessors(chain = true)
    public static class Constraints {
        /**
         * 是否考虑教师时间偏好
         * 如果设置为true，则在排课时会尽量尊重教师的时间偏好。
         */
        private Boolean teacherPreference;

        /**
         * 是否优化教室资源分配
         * 如果设置为true，则会在排课过程中尝试优化教室的使用效率。
         */
        private Boolean roomOptimization;

        /**
         * 是否避免学生班级冲突
         * 如果设置为true，则会确保同一学生的不同课程不会在同一时间发生冲突。
         */
        private Boolean studentConflictAvoidance;

        /**
         * 是否优先安排连堂课
         * 如果设置为true，则会在排课时尽量将同一天内的相关课程连续安排。
         */
        private Boolean consecutiveCoursesPreferred;

        /**
         * 专业教室匹配
         * 如果设置为true，则会尽量将特定专业的实验课安排在对应的实验室或专业教室。
         */
        private Boolean specializationRoomMatching;
    }

    @Data
    @Accessors(chain = true)
    public static class AlgorithmParams {
        /**
         * 种群大小
         * 遗传算法中种群的数量，影响算法的搜索能力和收敛速度。
         */
        private Integer populationSize;

        /**
         * 最大迭代次数
         * 遗传算法的最大迭代次数，控制算法运行的时间长度。
         */
        private Integer maxIterations;

        /**
         * 交叉率
         * 遗传算法中的交叉概率，影响个体之间基因交换的程度。
         */
        private Double crossoverRate;

        /**
         * 变异率
         * 遗传算法中的变异概率，影响个体基因突变的可能性。
         */
        private Double mutationRate;
    }

    @Data
    @Accessors(chain = true)
    public static class TimePreferences {
        /**
         * 是否避免晚间课程安排
         * 如果设置为true，则会尽量减少晚上时段的课程数量。
         */
        private Boolean eveningCourses;

        /**
         * 是否平衡周内课程分布
         * 如果设置为true，则会尽量使得一周内的课程分布较为均衡。
         */
        private Boolean balanceWeekdayCourses;

        /**
         * 优先时间段
         * 列出了一些时间段，这些时间段被认为是更受欢迎或更适合上课的时间。
         */
        private List<PreferredTimeSlot> preferredTimeSlots;

        @Data
        @Accessors(chain = true)
        public static class PreferredTimeSlot {
            /**
             * 星期几（1-7，1表示周一）
             * 表示时间段所在的星期几。
             */
            private Integer day;
            /**
             * 开始节次
             * 表示时间段的开始节次。
             */
            private Integer periodStart;
            /**
             * 结束节次
             * 表示时间段的结束节次。
             */
            private Integer periodEnd;
        }
    }
}



