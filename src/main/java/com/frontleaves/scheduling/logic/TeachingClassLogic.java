package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.daos.TeachingClassDAO;
import com.frontleaves.scheduling.models.dto.base.TeachingClassDTO;
import com.frontleaves.scheduling.models.entity.base.TeachingClassDO;
import com.frontleaves.scheduling.services.TeachingClassService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 教学班服务实现类
 * @author FLASHLACK
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeachingClassLogic implements TeachingClassService {
    private final TeachingClassDAO teachingClassDAO;
    @Override
    public List<TeachingClassDTO> getTeachingClassListBySemester(String semesterUuid) {
        List<TeachingClassDO> list = teachingClassDAO.getTeachingClassBySemester(semesterUuid);
        return list.stream()
                .map(teachingClassDO -> BeanUtil.toBean(teachingClassDO, TeachingClassDTO.class))
                .toList();
    }

    @Override
    public @NotNull TeachingClassDTO getTeachingClassByUuid(String teachingClassUuid) {
        TeachingClassDO teachingClassDO = teachingClassDAO.getTeachingClassByUuid(teachingClassUuid);
        if (teachingClassDO == null) {
            throw new BusinessException("教学班不存在", ErrorCode.BODY_ERROR);
        }
        return BeanUtil.toBean(teachingClassDO, TeachingClassDTO.class);
    }

    @Override
    public void save(TeachingClassDO teachingClassDO) {
        teachingClassDAO.save(teachingClassDO);
    }
}
