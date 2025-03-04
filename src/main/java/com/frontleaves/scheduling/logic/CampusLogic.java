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
        if (campusVO.getCampusName().isEmpty()){
            throw new BusinessException("校区名称不能为空", ErrorCode.BODY_ERROR);
        }
        if (campusVO.getCampusCode().isEmpty()){
            throw new BusinessException("校区编码不能为空", ErrorCode.BODY_ERROR);
        }
        if (campusVO.getCampusDesc().isEmpty()){
            throw new BusinessException("校区描述不能为空", ErrorCode.BODY_ERROR);
        }
        if (campusVO.getCampusStatus() == null){
            throw new BusinessException("校区状态不能为空", ErrorCode.BODY_ERROR);
        }
        if (campusVO.getCampusAddress().isEmpty()){
            throw new BusinessException("校区地址不能为空", ErrorCode.BODY_ERROR);
        }
        //检查唯一键是否重复
        if (campusDAO.getCampusByCode(campusVO.getCampusCode()) != null) {
            throw  new BusinessException("校区编码已存在", ErrorCode.BODY_ERROR);
        }
        //检查校区名称是否重复
        if (campusDAO.getCampusByName(campusVO.getCampusName()) != null) {
            throw  new BusinessException("校区名称已存在", ErrorCode.BODY_ERROR);
        }
    }
}
