package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.daos.BuildingDAO;
import com.frontleaves.scheduling.daos.CampusDAO;
import com.frontleaves.scheduling.models.dto.CampusDTO;
import com.frontleaves.scheduling.models.entity.BuildingDO;
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
    private final BuildingDAO buildingDAO;

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
        if (campusVO.getCampusName().isEmpty()) {
            throw new BusinessException("校区名称不能为空", ErrorCode.BODY_ERROR);
        }
        if (campusVO.getCampusCode().isEmpty()) {
            throw new BusinessException("校区编码不能为空", ErrorCode.BODY_ERROR);
        }
        if (campusVO.getCampusDesc().isEmpty()) {
            throw new BusinessException("校区描述不能为空", ErrorCode.BODY_ERROR);
        }
        //为Integer类型的属性赋值时，需要判断是否为空
        if (campusVO.getCampusStatus() == null) {
            throw new BusinessException("校区状态不能为空", ErrorCode.BODY_ERROR);
        }
        if (campusVO.getCampusAddress().isEmpty()) {
            throw new BusinessException("校区地址不能为空", ErrorCode.BODY_ERROR);
        }
        if (campusDAO.getCampusByCode(campusVO.getCampusCode()) != null) {
            throw new BusinessException("校区编码已存在", ErrorCode.BODY_ERROR);
        }
        if (campusDAO.getCampusByName(campusVO.getCampusName()) != null) {
            throw new BusinessException("校区名称已存在", ErrorCode.BODY_ERROR);
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
        //检查是否存在校区的教学楼
        if (buildingDAO.lambdaQuery().eq(BuildingDO::getCampusUuid, campusDO.getCampusUuid()).count() > 0) {
            buildingDAO.deleteBuildingByCampusUuid(campusDO.getCampusUuid());
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

}
