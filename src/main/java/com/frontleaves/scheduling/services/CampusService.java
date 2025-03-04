package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.CampusDTO;
import com.frontleaves.scheduling.models.vo.CampusVO;

/**
 * 校园服务接口，用于定义与校园相关的服务操作
 * 该接口目前尚未定义任何方法，但可以扩展以包括例如：
 * - 校园信息查询
 * - 校园活动管理
 * - 校园用户服务等
 *
 * @author FLASHLACK
 */
public interface CampusService {

    /**
     * 添加校园信息。
     *
     * @param campusVO 校园的视图对象，包含需要添加的校园详细信息
     * @return CampusDTO 返回新添加校园的信息数据传输对象
     */
    CampusDTO addCampus(
            CampusVO campusVO);

    /**
     * 检查并验证待添加的校园视图对象。
     *
     * @param campusVO 待验证的校园视图对象，包含需要添加的校园详细信息
     */
    void checkAddCampusVO(
            CampusVO campusVO);

}
