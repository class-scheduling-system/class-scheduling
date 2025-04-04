package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.base.TeachingClassDTO;

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
}
