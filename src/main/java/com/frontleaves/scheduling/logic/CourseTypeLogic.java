package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.daos.CourseTypeDAO;
import com.frontleaves.scheduling.models.dto.CourseTypeDTO;
import com.frontleaves.scheduling.models.entity.CourseTypeDO;
import com.frontleaves.scheduling.services.CourseTypeService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

/**
 * 课程类型逻辑
 *
 * @author FLASHLACK
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Repository
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
}
