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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.dto.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.CourseLiteDTO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.entity.*;
import com.frontleaves.scheduling.models.vo.CourseLibraryVO;
import com.frontleaves.scheduling.services.CourseLibraryService;
import com.frontleaves.scheduling.services.UserService;
import com.frontleaves.scheduling.utils.ProjectOption;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final UserService userService;

    /**
     * 添加课程库
     * <p>
     * 此方法用于将课程库信息添加到系统中在此之前，它会验证课程类别、课程属性、课程类型、课程性质和部门的存在性
     * 如果任何一方不存在，则抛出业务异常这确保了课程库信息的完整性和一致性
     *
     * @param courseLibraryVO 课程库的视图对象，包含课程库的相关信息，不能为空
     */
    @Override
    public void addCourseLibrary(@NotNull CourseLibraryVO courseLibraryVO, HttpServletRequest request) {
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

        UserDO getUser = userService.getUserByRequest(request);

        // 创建一个新的课程库对象，并从视图对象中复制属性
        CourseLibraryDO courseLibraryDO = new CourseLibraryDO();
        BeanUtil.copyProperties(courseLibraryVO, courseLibraryDO, ProjectOption.stringBlankToNull());
        courseLibraryDO.setEditUser(getUser.getUserUuid());
        // 保存课程库对象到数据库
        courseLibraryDAO.save(courseLibraryDO);
    }

    /**
     * 更新课程库信息
     *
     * 本方法通过课程UUID获取课程库对象，并根据传入的课程库视图对象（CourseLibraryVO）信息进行更新
     * 如果课程库视图对象中的类别、属性、类型、性质或部门不存在，则抛出业务异常
     *
     * @param courseUuid 课程库的唯一标识符
     * @param courseLibraryVO 包含更新信息的课程库视图对象
     */
    @Override
    public void updateCourseLibrary(String courseUuid, CourseLibraryVO courseLibraryVO) {
        // 根据课程UUID获取课程库对象
        CourseLibraryDO courseLibraryDO = courseLibraryDAO.getCourseLibraryByUuid(courseUuid);
        
        if (courseLibraryDO != null) {
            // 验证课程类别是否存在
            Optional.ofNullable(courseLibraryVO.getCategory())
                    .map(courseCategoryDAO::getCourseCategoryByUuid)
                    .orElseThrow(() -> new BusinessException("课程类别不存在", ErrorCode.NOT_EXIST));
            
            // 验证课程属性是否存在
            Optional.ofNullable(courseLibraryVO.getProperty())
                    .map(coursePropertyDAO::getCoursePropertyByUuid)
                    .orElseThrow(() -> new BusinessException("课程属性不存在", ErrorCode.NOT_EXIST));
            
            // 验证课程类型是否存在
            Optional.ofNullable(courseLibraryVO.getType())
                    .map(courseTypeDAO::getCourseTypeByUuid)
                    .orElseThrow(() -> new BusinessException("课程类型不存在", ErrorCode.NOT_EXIST));
            
            // 验证课程性质是否存在
            Optional.ofNullable(courseLibraryVO.getNature())
                    .map(courseNatureDAO::getCourseNatureByUuid)
                    .orElseThrow(() -> new BusinessException("课程性质不存在", ErrorCode.NOT_EXIST));
            
            // 验证部门是否存在
            Optional.ofNullable(courseLibraryVO.getDepartment())
                    .map(departmentDAO::getDepartmentByUuid)
                    .orElseThrow(() -> new BusinessException("部门不存在", ErrorCode.NOT_EXIST));

            // 将课程库视图对象的属性复制到课程库对象中
            BeanUtil.copyProperties(courseLibraryVO, courseLibraryDO, ProjectOption.stringBlankToNull());

            // 更新课程库信息
            courseLibraryDAO.updateCourseLibrary(courseLibraryDO);
        }
    }

    /**
     * 根据课程UUID删除课程库
     *
     * 此方法首先通过课程UUID获取课程库对象，如果找不到对应的课程库，
     * 则抛出一个表示课程库不存在的业务异常如果课程库存在，则调用DAO层方法将其删除
     *
     * @param courseUuid 课程库的唯一UUID，用于标识要删除的课程库
     * @throws BusinessException 当课程库不存在时抛出的业务异常
     */
    @Override
    public void deleteCourseLibrary(String courseUuid) {
        // 根据UUID获取课程库对象
        CourseLibraryDO courseLibraryDO = courseLibraryDAO.getCourseLibraryByUuid(courseUuid);
        // 检查课程库是否存在，如果不存在则抛出异常
        if (courseLibraryDO == null) {
            throw new BusinessException("课程库不存在", ErrorCode.NOT_EXIST);
        }
        // 删除课程库
        courseLibraryDAO.deleteCourseLibrary(courseLibraryDO);
    }

    /**
     * 获取课程库分页信息
     *
     * @param page 页码，从1开始
     * @param size 每页记录数
     * @param name 课程库名称，用于模糊查询
     * @return 包含课程库信息的分页数据传输对象
     */
    @Override
    public PageDTO<CourseLibraryDTO> getCourseLibrary(Integer page, Integer size, String name) {
        // 获取课程库分页数据
        Page<CourseLibraryDO> courseLibraryList = courseLibraryDAO.getCourseLibraryPage(page, size, name);

        // 如果查询结果为空，则直接返回一个空的PageDTO对象
        if (courseLibraryList.getTotal() == 0) {
            return new PageDTO<>();
        } else {
            // 初始化PageDTO对象，设置总记录数和每页大小
            PageDTO<CourseLibraryDTO> pageDTO = new PageDTO<>(courseLibraryList.getTotal(), courseLibraryList.getSize());
            // 设置当前页码
            pageDTO.setCurrent(courseLibraryList.getCurrent());
            // 将查询结果转换为CourseLibraryDTO对象列表，并设置到PageDTO中
            pageDTO.setRecords(
                    courseLibraryList.getRecords().stream()
                            .map(courseLibraryDO -> {
                                CourseLibraryDTO courseLibraryDTO = new CourseLibraryDTO();
                                // 使用BeanUtil工具类复制属性
                                BeanUtil.copyProperties(courseLibraryDO, courseLibraryDTO);
                                return courseLibraryDTO;
                            }).toList()
            );
            // 返回填充了数据的PageDTO对象
            return pageDTO;
        }
    }

    @Override
    public List<CourseLiteDTO> getCourseLibraryList(String courseCategoryUuid, String coursePropertyUuid, String courseTypeUuid, String courseNatureUuid, String courseDepartmentUuid) {
        List<CourseLibraryDO> courseLibraryList = courseLibraryDAO.getCourseLibraryList(courseCategoryUuid, coursePropertyUuid, courseTypeUuid, courseNatureUuid, courseDepartmentUuid);
        if (courseLibraryList.isEmpty()) {
            return new ArrayList<>();
        }

        List<CourseCategoryDO> getCourseCategoryList = courseCategoryDAO.getCourseCategoryList();
        List<CoursePropertyDO> getCoursePropertyList = coursePropertyDAO.getCoursePropertyList();
        List<CourseTypeDO> getCourseTypeList = courseTypeDAO.getCourseTypeList();
        List<CourseNatureDO> getCourseNatureList = courseNatureDAO.getCourseNatureList();
        List<DepartmentDO> getDepartmentList = departmentDAO.getDepartmentList();

        // 转换为 CourseLiteDTO
        return courseLibraryList.stream().map(courseLibrary -> new CourseLiteDTO()
                    .setCourseLibraryUuid(courseLibrary.getCourseLibraryUuid())
                    .setName(courseLibrary.getName())
                    .setCategory(getCourseCategoryList.stream()
                            .filter(courseCategory -> courseCategory.getCourseCategoryUuid().equals(courseLibrary.getCategory()))
                            .findFirst()
                            .map(CourseCategoryDO::getName)
                            .orElse(null))
                    .setProperty(getCoursePropertyList.stream()
                            .filter(courseProperty -> courseProperty.getCoursePropertyUuid().equals(courseLibrary.getProperty()))
                            .findFirst()
                            .map(CoursePropertyDO::getName)
                            .orElse(null))
                    .setType(getCourseTypeList.stream()
                            .filter(courseType -> courseType.getCourseTypeUuid().equals(courseLibrary.getType()))
                            .findFirst()
                            .map(CourseTypeDO::getName)
                            .orElse(null))
                    .setNature(getCourseNatureList.stream()
                            .filter(courseNature -> courseNature.getCourseNatureUuid().equals(courseLibrary.getNature()))
                            .findFirst()
                            .map(CourseNatureDO::getName)
                            .orElse(null))
                    .setDepartment(getDepartmentList.stream()
                            .filter(department -> department.getDepartmentUuid().equals(courseLibrary.getDepartment()))
                            .findFirst()
                            .map(DepartmentDO::getDepartmentName)
                            .orElse(null))
        ).toList();
    }

}