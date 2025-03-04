package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.daos.CampusDAO;
import com.frontleaves.scheduling.models.dto.CampusDTO;
import com.frontleaves.scheduling.models.entity.CampusDO;
import com.frontleaves.scheduling.models.vo.CampusVO;
import com.frontleaves.scheduling.services.CampusService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

/**
 * 校园逻辑类，用于处理与校园相关的业务逻辑。
 *
 * <p>该类通过Spring的@Service注解标记为服务层组件，通常用于封装核心业务逻辑。</p>
 *
 * <p>使用@Slf4j注解自动生成日志对象，便于记录运行时信息。</p>
 *
 * <p>通过@RequiredArgsConstructor注解自动生成构造函数，用于依赖注入。</p>
 *
 * @author FLASHLACK
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampusLogic implements CampusService {
    private final CampusDAO campusDAO;

    @Override
    public CampusDTO addCampus(CampusVO campusVO) {
        //数据交换
        CampusDO campusDO = BeanUtil.copyProperties(campusVO, CampusDO.class)
                .setCampusUuid(UuidUtil.generateUuidNoDash());
        campusDAO.save(campusDO);
        CampusDO newCampusDO = campusDAO.getCampusByUuid(campusDO.getCampusUuid());
        return BeanUtil.copyProperties(newCampusDO, CampusDTO.class);
    }

    @Override
    public void checkAddCampusVO(CampusVO campusVO) {
        if (campusVO.getCampusName().isEmpty()) {
            throw new BusinessException("校区名称不能为空", ErrorCode.BODY_ERROR);
        }
        if (campusVO.getCampusCode().isEmpty()) {
            throw new BusinessException("校区编码不能为空", ErrorCode.BODY_ERROR);
        }
        if (campusVO.getCampusDesc().isEmpty()) {
            throw new BusinessException("校区描述不能为空", ErrorCode.BODY_ERROR);
        }
        if (campusVO.getCampusStatus() == null) {
            throw new BusinessException("校区状态不能为空", ErrorCode.BODY_ERROR);
        }
        if (campusVO.getCampusAddress().isEmpty()) {
            throw new BusinessException("校区地址不能为空", ErrorCode.BODY_ERROR);
        }
        //检查唯一键是否重复
        if (campusDAO.getCampusByCode(campusVO.getCampusCode()) != null) {
            throw new BusinessException("校区编码已存在", ErrorCode.BODY_ERROR);
        }
        //检查校区名称是否重复
        if (campusDAO.getCampusByName(campusVO.getCampusName()) != null) {
            throw new BusinessException("校区名称已存在", ErrorCode.BODY_ERROR);
        }
    }

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

    @Override
    public CampusDTO updateCampus(CampusVO campusVO, CampusDO campusOldDO) {
        //数据交换
        CampusDO campusDO = exchangeCampus(campusVO, campusOldDO);
        CampusDO newCampusDO = campusDAO.updateCampus(campusDO);
        return BeanUtil.copyProperties(newCampusDO, CampusDTO.class);
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

}
