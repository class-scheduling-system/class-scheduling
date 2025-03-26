package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.entity.SemesterDO;

/**
 * 学期服务
 * @author FLASHLACK
 */
public interface SemesterService {

    /**
     * 根据学期UUID获取学期，并且检查是否启用
     * @param semesterUuid 学期的唯一标识符
     * @return 返回学期信息对象，如果找不到则返回null，若不启用则报错
     */
    SemesterDO getSemesterByUuidCheckEnabled(
            String semesterUuid);
}
