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

package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.entity.*;
import com.frontleaves.scheduling.models.vo.CourseLibraryVO;
import com.frontleaves.scheduling.services.CourseLibraryService;
import com.frontleaves.scheduling.utils.ProjectOption;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 课程库逻辑处理类，实现了 {@link CourseLibraryService} 接口，提供了课程库管理的具体实现。
 * <p>
 * 该类负责处理与课程库相关的业务逻辑，包括查询、添加、删除和更新课程库信息等。
 * 通过依赖注入的方式获取所需的其他服务或组件。
 * </p>
 *
 * @author Claude AI
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseLibraryLogic implements CourseLibraryService {

    private final CourseLibraryDAO courseLibraryDAO;
    private final CourseCategoryDAO courseCategoryDAO;
    private final CoursePropertyDAO coursePropertyDAO;
    private final CourseTypeDAO courseTypeDAO;
    private final CourseNatureDAO courseNatureDAO;
    private final DepartmentDAO departmentDAO;

    /**
     * 添加课程库
     *
     * 此方法用于将课程库信息添加到系统中在此之前，它会验证课程类别、课程属性、课程类型、课程性质和部门的存在性
     * 如果任何一方不存在，则抛出业务异常这确保了课程库信息的完整性和一致性
     *
     * @param courseLibraryVO 课程库的视图对象，包含课程库的相关信息，不能为空
     */
    @Override
    public void addCourseLibrary(@NotNull CourseLibraryVO courseLibraryVO) {
        // 根据UUID获取课程类别对象，验证课程类别是否存在
        CourseCategoryDO courseCategoryDO = courseCategoryDAO.getCourseCategoryByUuid(courseLibraryVO.getCategory());
        if (courseCategoryDO == null) {
            throw new BusinessException("课程类别不存在", ErrorCode.NOT_EXIST);
        }

        // 根据UUID获取课程属性对象，验证课程属性是否存在
        CoursePropertyDO coursePropertyDO = coursePropertyDAO.getCoursePropertyByUuid(courseLibraryVO.getProperty());
        if (coursePropertyDO == null) {
            throw new BusinessException("课程属性不存在", ErrorCode.NOT_EXIST);
        }

        // 根据UUID获取课程类型对象，验证课程类型是否存在
        CourseTypeDO courseTypeDO = courseTypeDAO.getCourseTypeByUuid(courseLibraryVO.getType());
        if (courseTypeDO == null) {
            throw new BusinessException("课程类型不存在", ErrorCode.NOT_EXIST);
        }

        // 根据UUID获取课程性质对象，验证课程性质是否存在
        CourseNatureDO courseNatureDO = courseNatureDAO.getCourseNatureByUuid(courseLibraryVO.getNature());
        if (courseNatureDO == null) {
            throw new BusinessException("课程性质不存在", ErrorCode.NOT_EXIST);
        }

        // 根据UUID获取部门对象，验证部门是否存在
        DepartmentDO departmentDO = departmentDAO.getDepartmentByUuid(courseLibraryVO.getDepartment());
        if (departmentDO == null) {
            throw new BusinessException("部门不存在", ErrorCode.NOT_EXIST);
        }

        // 创建一个新的课程库对象，并从视图对象中复制属性
        CourseLibraryDO courseLibraryDO = new CourseLibraryDO();
        BeanUtil.copyProperties(courseLibraryVO, courseLibraryDO, ProjectOption.stringBlankToNull());

        // 保存课程库对象到数据库
        courseLibraryDAO.save(courseLibraryDO);
    }
}