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

package com.frontleaves.scheduling.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.frontleaves.scheduling.models.entity.base.StudentDO;
import com.frontleaves.scheduling.models.entity.multiple.UserAndStudentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 学生Mapper
 * @author FLASHLACK
 */
@Mapper
public interface StudentMapper extends BaseMapper<StudentDO> {

    // 连表查询:查询已注册学生并根据cs_user.status筛选,降序排序
    List<UserAndStudentDO> getStudentAndUserQueryDesc(String clazz, Byte status, String name, Integer page, Integer size, Boolean isGraduated, String id, String departmentUuid);

    // 升序排序
    List<UserAndStudentDO> getStudentAndUserQueryAsc(String clazz, Byte status, String name, Integer page, Integer size, Boolean isGraduated, String id, String departmentUuid);

    // 查询未注册学生,降序排序
    List<StudentDO> getStudentNoRegisterUserQueryDesc(String clazz, Byte status, String name, Integer page, Integer size, Boolean isGraduated, String id, String departmentUuid);

    // 升序排序
    List<StudentDO> getStudentNoRegisterUserQueryAsc(String clazz, Byte status, String name, Integer page, Integer size, Boolean isGraduated, String id, String departmentUuid);

    // 根据 user_uuid 查询对应用户状态
    Byte getUserStatusByUuid(String userUuid);
}
