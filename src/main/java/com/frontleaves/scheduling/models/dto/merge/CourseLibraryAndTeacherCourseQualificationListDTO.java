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

package com.frontleaves.scheduling.models.dto.merge;

import com.frontleaves.scheduling.models.dto.base.AdministrativeClassDTO;
import com.frontleaves.scheduling.models.dto.base.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherCoursePreferencesDTO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 课程库和教师课程资格列表DTO
 * 内有课程库和班级DTO
 * 内有教师课程资格列表
 * 内有优先级
 *
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class CourseLibraryAndTeacherCourseQualificationListDTO {

    /**
     * 课程库
     */
    private CourseLibraryDTO course;
    /**
     * 行政班级列表（可为空）
     */
    private List<AdministrativeClassDTO> classList;
    /**
     * 需排课人数
     */
    private Integer number;
    /**
     * 对应教师课程资格
     */
    private List<TeacherCoursePreferencesDTO> teacherList;
    /**
     * 课程优先级
     */
    private Short priority;
}
