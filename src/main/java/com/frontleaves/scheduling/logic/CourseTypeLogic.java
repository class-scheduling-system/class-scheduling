package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.daos.CourseTypeDAO;
import com.frontleaves.scheduling.models.dto.base.CourseTypeDTO;
import com.frontleaves.scheduling.models.entity.CourseTypeDO;
import com.frontleaves.scheduling.services.CourseTypeService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 课程类型逻辑
 *
 * @author FLASHLACK
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseTypeLogic implements CourseTypeService {
    private final CourseTypeDAO courseTypeDAO;

    /**
     * 根据UUID获取课程类型信息，如果课程类型不存在，则抛出业务异常
     * @param uuid 课程类型的唯一标识符
     * @return 课程类型的数据传输对象
     * @throws BusinessException 如果课程类型不存在，则抛出此异常，提示错误信息并返回错误码
     */
    @Override
    public CourseTypeDTO getCourseTypeByUuidWithError(String uuid) {
        CourseTypeDO courseTypeDO = courseTypeDAO.getCourseTypeByUuid(uuid);
        if (courseTypeDO == null) {
            throw new BusinessException("课程类型不存在", ErrorCode.BODY_ERROR);
        }
        return BeanUtil.toBean(courseTypeDO,CourseTypeDTO.class);
    }

    /**
     * 获取课程类型列表
     * 本方法通过调用DAO层的listCourseType方法获取课程类型数据对象列表，
     * 然后将这些数据对象转换为DTO（数据传输对象）列表，以供上层调用
     * @return 课程类型DTO列表
     */
    @Override
    public List<CourseTypeDTO> listCourseType() {
        // 从数据库中获取课程类型数据对象列表
        List<CourseTypeDO> courseTypeDOList = courseTypeDAO.listCourseType();

        // 将课程类型数据对象列表转换为课程类型数据传输对象列表
        return BeanUtil.copyToList(courseTypeDOList,CourseTypeDTO.class);
    }
}
