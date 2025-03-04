package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.CampusDTO;
import com.frontleaves.scheduling.models.entity.CampusDO;
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

    /**
     * 校验更新校区信息的视图对象
     * 此方法用于在更新校区信息之前，校验传入的校区信息是否符合业务规则和数据完整性要求
     *
     * @param campusUuid 要更新的校区唯一标识符
     * @param campusVO   包含要更新的校区信息的视图对象
     * @return 返回一个包含校区数据的对象，表示校验通过后的校区信息
     */
    CampusDO checkUpdateCampusVO(
            String campusUuid,
            CampusVO campusVO);


    /**
     * 更新校区信息
     * 此方法用于根据传入的校区视图对象和校区数据对象，更新指定校区的信息
     *
     * @param campusVO 包含要更新的校区信息的视图对象
     * @param campusOldDO 校区的数据对象，包含校区的当前信息
     * @return 返回一个包含更新后校区信息的数据传输对象（DTO）
     */
    CampusDTO updateCampus(
            CampusVO campusVO,
            CampusDO campusOldDO);

}
