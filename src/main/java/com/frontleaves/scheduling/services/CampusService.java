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
 * 本软件是“按原样”提供的，没有任何形式的明示或暗示的保证，包括但不限于
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

package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.CampusDTO;
import com.frontleaves.scheduling.models.dto.ListOfCampusDTO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.entity.CampusDO;
import com.frontleaves.scheduling.models.vo.CampusVO;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * 校园服务接口，定义了与校园相关的操作。
 * <p>
 * 该接口提供了与校园管理相关的基础方法，包括但不限于查询校园信息、更新校园数据等。具体实现细节由实现类决定。
 * </p>
 *
 * @author xiao_lfeng | FLASHLACK
 * @version v1.0.0
 * @since v1.0.0
 */
public interface CampusService {

    /**
     * 添加校园信息。
     *
     * @param campusVO 校园的视图对象，包含需要添加的校园详细信息
     * @return CampusDTO 返回新添加校园的信息数据传输对象
     */
    CampusDTO addCampus(
            CampusVO campusVO
    );

    /**
     * 检查并验证待添加的校园视图对象。
     *
     * @param campusVO 待验证的校园视图对象，包含需要添加的校园详细信息
     */
    void checkAddCampusVO(
            CampusVO campusVO
    );

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
            CampusVO campusVO
    );


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
            CampusDO campusOldDO
    );

    /**
     * 检查删除校区信息
     * @param campusUuid 校区唯一标识符
     * @return 返回一个包含校区数据的对象，表示校验通过后的校区信息
     */
    CampusDO checkDeleteCampus(
            String campusUuid
    );

    /**
     * 删除校区信息
     * @param campusDO 校区数据对象
     */
    void deleteCampus(CampusDO campusDO);

    /**
     * 根据校区唯一标识符获取校区信息
     * <p>
     * 该方法通过提供的校区唯一标识符 {@code campusUuid} 查询对应的校区信息，并返回一个包含校区详细信息的 {@link CampusDTO} 对象。
     * 如果找不到与给定 {@code campusUuid} 匹配的校区，则返回 null。
     * </p>
     *
     * @param campusUuid 校区的唯一标识符
     * @return 返回与给定唯一标识符匹配的校区信息，如果未找到则返回 null
     */
    @Nullable
    CampusDTO getCampusByUuid(String campusUuid);

    /**
     * 获取校园信息分页数据
     * <p>
     * 该方法用于根据指定的分页参数和搜索关键字获取校园信息的分页数据。返回的数据包括符合条件的校园记录列表、总记录数、每页大小、当前页码和总页数。
     * </p>
     *
     * @param page 当前页码，从1开始
     * @param size 每页显示的记录数
     * @param isDesc 是否降序排列，默认为false表示升序
     * @param keyword 搜索关键字，可为空。如果提供，则在查询时会根据此关键字进行模糊匹配
     * @return 返回一个包含校园信息分页数据的 {@link PageDTO} 对象
     */
    PageDTO<CampusDTO> getPageOfCampus(int page, int size, boolean isDesc, @Nullable String keyword);

    /**
     * 获取校区列表
     * <p>
     * 该方法用于获取系统中所有校区的简要信息列表。返回的数据为 {@link ListOfCampusDTO} 对象的列表，每个对象包含校区的主键、名称和编码。
     * </p>
     *
     * @return 返回一个包含所有校区简要信息的 {@code List<ListOfCampusDTO>} 列表
     */
    List<ListOfCampusDTO> getCampusList();

}
