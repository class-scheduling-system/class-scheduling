package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.base.TeachingClassDTO;
import com.frontleaves.scheduling.models.entity.base.TeachingClassDO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 教学班服务接口
 * @author FLASHLACK
 */
public interface TeachingClassService {
    /**
     * 根据学期主键和查询教学班
     * @param semesterUuid 学期UUID
     * @return 教学班数据传输对象
     */

    List<TeachingClassDTO> getTeachingClassListBySemester(
           String semesterUuid);

    /**
     * 根据UUID查询教学班
     * @param teachingClassUuid 教学班UUID
     * @return 教学班数据传输对象
     */
    @NotNull
    TeachingClassDTO getTeachingClassByUuid(
            String teachingClassUuid);

    /**
     * 保存教学班
     * @param teachingClassDO
     */
    void save(TeachingClassDO teachingClassDO);

    /**
     * 根据UUID查询教学班 - 为空
     * @param teachingClassUuid 教学班UUID
     * @return 教学班数据传输对象
     */
    @Nullable
    TeachingClassDTO getTeachingClassByUuidNoError(String teachingClassUuid);
}
