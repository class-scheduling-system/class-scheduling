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

package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.daos.CampusDAO;
import com.frontleaves.scheduling.models.dto.CampusDTO;
import com.frontleaves.scheduling.models.dto.ListOfCampusDTO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.entity.CampusDO;
import com.frontleaves.scheduling.models.vo.CampusVO;
import com.frontleaves.scheduling.services.CampusService;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 校园逻辑处理类
 * <p>
 * 该类通过依赖注入的方式获取 {@link CampusDAO} 实例，并利用其实现对校区数据的访问和操作。主要功能包括根据校区唯一标识符查询校区信息等。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampusLogic implements CampusService {
    private final CampusDAO campusDAO;

    /**
     * 添加校园
     *
     * @param campusVO 校园的视图对象，包含需要添加的校园详细信息
     * @return 返回添加成功的校园信息
     */
    @Override
    public CampusDTO addCampus(CampusVO campusVO) {
        //数据交换
        CampusDO campusDO = BeanUtil.copyProperties(campusVO, CampusDO.class)
                .setCampusUuid(UuidUtil.generateUuidNoDash());
        campusDAO.save(campusDO);
        CampusDO newCampusDO = campusDAO.getCampusByUuid(campusDO.getCampusUuid());
        return BeanUtil.copyProperties(newCampusDO, CampusDTO.class);
    }

    /**
     * 校验添加校区的输入信息是否合法
     * 此方法主要通过检查校区信息的各个字段是否为空或重复来确保数据的合法性
     * 验证不通过将抛出异常，提示相应的错误信息
     *
     * @param campusVO 校区视图对象，包含了校区的相关信息
     * @throws BusinessException 当校区信息不合法时抛出此异常，包含错误信息和错误代码
     */
    @Override
    public void checkAddCampusVO(CampusVO campusVO) {
        if (campusVO == null) {
            throw new BusinessException("校区信息不能为空", ErrorCode.BODY_ERROR);
        }

        checkFieldNotEmpty(campusVO.getCampusName(), "校区名称");
        checkFieldNotEmpty(campusVO.getCampusCode(), "校区编码");
        checkFieldNotEmpty(campusVO.getCampusDesc(), "校区描述");
        checkFieldNotNull(campusVO.getCampusStatus());
        checkFieldNotEmpty(campusVO.getCampusAddress(), "校区地址");
        if (campusDAO.getCampusByCode(campusVO.getCampusCode()) != null) {
            throw new BusinessException("校区编码已存在", ErrorCode.BODY_ERROR);
        }
        if (campusDAO.getCampusByName(campusVO.getCampusName()) != null) {
            throw new BusinessException("校区名称已存在", ErrorCode.BODY_ERROR);
        }
    }

    /**
     * 检查字段是否为空
     *
     * @param field     字段
     * @param fieldName 字段名称
     */
    private void checkFieldNotEmpty(String field, String fieldName) {
        if (field == null || field.isEmpty()) {
            throw new BusinessException(fieldName + "不能为空", ErrorCode.BODY_ERROR);
        }
    }

    /**
     * 检查字段是否为空
     *
     * @param field 字段
     */
    private void checkFieldNotNull(Object field) {
        if (field == null) {
            throw new BusinessException("校区状态" + "不能为空", ErrorCode.BODY_ERROR);
        }
    }


    /**
     * 检查并更新校区信息
     * 此方法首先验证校区的唯一性标识（UUID），然后检查校区名称和编码的唯一性
     * 如果校区不存在，或名称、编码重复，将抛出异常
     *
     * @param campusUuid 校区的唯一性标识（UUID）
     * @param campusVO   待更新的校区信息对象
     * @return 返回更新前的校区信息对象
     * @throws BusinessException 如果校区不存在或校区名称、编码重复
     */
    @Override
    public CampusDO checkUpdateCampusVO(String campusUuid, CampusVO campusVO) {
        //检查校区是否存在
        CampusDO campusDO = campusDAO.getCampusByUuid(campusUuid);
        if (campusDO == null) {
            throw new BusinessException("校区不存在", ErrorCode.OPERATION_FAILED);
        }

        //检查校区名称是否重复
        if (campusVO.getCampusName() != null
                && !campusVO.getCampusName().equals(campusDO.getCampusName())
                && campusDAO.getCampusByName(campusVO.getCampusName()) != null) {
            throw new BusinessException("校区名称已存在", ErrorCode.BODY_ERROR);
        }

        //检查校区编码是否重复
        if (campusVO.getCampusCode() != null
                && !campusVO.getCampusCode().equals(campusDO.getCampusCode())
                && campusDAO.getCampusByCode(campusVO.getCampusCode()) != null) {
            throw new BusinessException("校区编码已存在", ErrorCode.BODY_ERROR);
        }

        return campusDO;
    }

    /**
     * 更新校区信息
     *
     * @param campusVO    包含要更新的校区信息的视图对象
     * @param campusOldDO 校区的数据对象，包含校区的当前信息
     * @return 返回更新后的校区信息
     */
    @Override
    public CampusDTO updateCampus(CampusVO campusVO, CampusDO campusOldDO) {
        //数据交换
        CampusDO campusDO = exchangeCampus(campusVO, campusOldDO);
        CampusDO newCampusDO = campusDAO.updateCampus(campusDO);
        return BeanUtil.copyProperties(newCampusDO, CampusDTO.class);
    }

    @Override
    public CampusDO checkDeleteCampus(String campusUuid) {
        CampusDO campusDO = campusDAO.getCampusByUuid(campusUuid);
        if (campusDO == null) {
            throw new BusinessException("校区不存在", ErrorCode.OPERATION_FAILED);
        }
        return campusDO;
    }

    @Override
    public void deleteCampus(CampusDO campusDO) {
        if (campusDO == null) {
            throw new BusinessException("删除校区时，校区不存在", ErrorCode.OPERATION_FAILED);
        }
        campusDAO.deleteCampus(campusDO);
    }

    /**
     * 交换校区信息
     * 此方法用于将校区视图对象（VO）中的非空字段值复制到校区数据对象（DO）中，
     * 同时保留原有校区的唯一标识符（UUID）。如果视图对象中的字段为空，则保留数据对象中原有的值。
     *
     * @param campusVO    包含要更新的校区信息的视图对象，不能为空
     * @param campusOldDO 原有的校区数据对象，作为默认值来源，不能为空
     * @return 返回一个包含更新后校区信息的数据对象（DO），该对象一定不为空
     */
    private @NotNull CampusDO exchangeCampus(
            @NotNull CampusVO campusVO, @NotNull CampusDO campusOldDO) {
        CampusDO campusDO = new CampusDO();
        // 保留原有校区的唯一标识符
        campusDO.setCampusUuid(campusOldDO.getCampusUuid());
        // 根据视图对象中的非空字段，更新数据对象中的相应字段
        if (campusVO.getCampusName() != null) {
            campusDO.setCampusName(campusVO.getCampusName());
        }
        if (campusVO.getCampusCode() != null) {
            campusDO.setCampusCode(campusVO.getCampusCode());
        }
        if (campusVO.getCampusDesc() != null) {
            campusDO.setCampusDesc(campusVO.getCampusDesc());
        }
        if (campusVO.getCampusStatus() != null) {
            campusDO.setCampusStatus(campusVO.getCampusStatus());
        }
        if (campusVO.getCampusAddress() != null) {
            campusDO.setCampusAddress(campusVO.getCampusAddress());
        }
        return campusDO;
    }

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
    @Override
    @Nullable
    public CampusDTO getCampusByUuid(String campusUuid) {
        CampusDO campusDO = campusDAO.getCampusByUuid(campusUuid);
        if (campusDO == null) {
            return null;
        }
        return BeanUtil.toBean(campusDO, CampusDTO.class);
    }

    /**
     * 获取校园信息分页数据
     * <p>
     * 该方法用于根据指定的分页参数和搜索关键字获取校园信息的分页数据。返回的数据包括符合条件的校园记录列表、总记录数、每页大小、当前页码和总页数。
     * </p>
     *
     * @param page    当前页码，从1开始
     * @param size    每页显示的记录数
     * @param isDesc  是否降序排列，默认为false表示升序
     * @param keyword 搜索关键字，可为空。如果提供，则在查询时会根据此关键字进行模糊匹配
     * @return 返回一个包含校园信息分页数据的 {@link PageDTO} 对象
     */
    @Override
    public PageDTO<CampusDTO> getPageOfCampus(int page, int size, boolean isDesc, @Nullable String keyword) {
        Page<CampusDO> campusPage = campusDAO.getPageOfCampus(page, size, isDesc, keyword);
        if (campusPage == null) {
            return new PageDTO<>();
        }
        return ProjectUtil.convertPageToPageDTO(campusPage, CampusDTO.class);
    }

    /**
     * 获取校区列表
     * <p>
     * 该方法用于获取系统中所有校区的简要信息列表。返回的数据为 {@link ListOfCampusDTO} 对象的列表，每个对象包含校区的主键、名称和编码。
     * </p>
     *
     * @return 返回一个包含所有校区简要信息的 {@code List<ListOfCampusDTO>} 列表
     */
    @Override
    public List<ListOfCampusDTO> getCampusList() {
        return campusDAO.getCampusList();
    }


}
