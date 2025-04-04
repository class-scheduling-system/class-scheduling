package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.daos.TeachingClassDAO;
import com.frontleaves.scheduling.models.dto.base.TeachingClassDTO;
import com.frontleaves.scheduling.models.entity.base.TeachingClassDO;
import com.frontleaves.scheduling.services.TeachingClassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        return list.stream().map(teachingClassDO -> {
            return BeanUtil.toBean(teachingClassDO, TeachingClassDTO.class);
        }).toList();
    }
}
