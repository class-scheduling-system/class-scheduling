package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.CampusDAO;
import com.frontleaves.scheduling.models.dto.CampusDTO;
import com.frontleaves.scheduling.models.entity.CampusDO;
import com.frontleaves.scheduling.models.vo.CampusVO;
import com.frontleaves.scheduling.services.CampusService;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.stream.Stream;

@SpringBootTest
@Slf4j
class CampusTest {

    @Autowired
    private CampusService campusService;

    @Autowired
    private CampusDAO campusDAO;

    /**
     * 提供用于测试的无效校区信息流
     * 该方法主要用于单元测试，通过返回一系列无效的校区信息参数，来测试系统对错误输入的处理能力
     * 每个参数对包含一个校区信息对象（CampusVO）和一个预期的错误消息，用于验证系统是否能正确识别并响应这些错误输入
     *
     * @return Stream<Arguments> 一个包含无效校区信息及其对应错误消息的参数流
     */
    private static Stream<Arguments> provideInvalidCampusVOs() {
        return Stream.of(
                Arguments.of(
                        new CampusVO("", "1456789",
                                "好学校", 1, "锡山区"), "校区名称不能为空"),
                Arguments.of(new CampusVO("无锡学院校区", "",
                        "好学校", 1, "锡山区"), "校区编码不能为空"),
                Arguments.of(new CampusVO("无锡学院校区", "1456789",
                        "", 1, "锡山区"), "校区描述不能为空"),
                Arguments.of(new CampusVO("无锡学院校区", "1456789",
                        "好学校", null, "锡山区"), "校区状态不能为空"),
                Arguments.of(new CampusVO("无锡学院校区", "1456789",
                        "好学校", 1, ""), "校区地址不能为空")
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidCampusVOs")
    void testCheckAddCampusVOWithInvalidData(CampusVO campusVO) {
        // 保存一个重复的校区记录
        CampusDO campusDO = new CampusDO();
        campusDO.setCampusUuid(UuidUtil.generateUuidNoDash())
                .setCampusCode(campusVO.getCampusCode())
                .setCampusName(campusVO.getCampusName())
                .setCampusStatus(campusVO.getCampusStatus())
                .setCampusDesc(campusVO.getCampusDesc())
                .setCampusAddress(campusVO.getCampusAddress());
        campusDAO.save(campusDO);
        // 校验是否抛出 BusinessException
        Assertions.assertThrows(BusinessException.class, () ->
                campusService.checkAddCampusVO(campusVO)
        );
        // 清理测试数据
        campusDAO.removeById(campusDO);
    }

    @Test
    void testAddCampus() {
        CampusVO campusVO = new CampusVO(
                "无锡学院校区", "1456789",
                "好学校", 1, "锡山区");
        CampusDTO campusDTO = campusService.addCampus(campusVO);
        Assertions.assertNotNull(campusDTO);
        CampusDO campusDO = campusDAO.lambdaQuery()
                .eq(CampusDO::getCampusUuid, campusDTO.getCampusUuid()).one();
        campusDAO.removeById(campusDO);
    }

    @Test
    void testUpdateCampus() {
        CampusDO campusDO = new CampusDO();
        campusDO.setCampusUuid(UuidUtil.generateUuidNoDash())
                .setCampusCode("1456789")
                .setCampusName("无锡学院校区")
                .setCampusStatus(1)
                .setCampusDesc("好学校")
                .setCampusAddress("锡山区");
        campusDAO.save(campusDO);
        CampusDO oldCampusDO = campusDAO.lambdaQuery()
                .eq(CampusDO::getCampusUuid, campusDO.getCampusUuid()).one();
        CampusVO campusVO = new CampusVO(
                "东南大学校区", "123456",
                "坏学校", 0, "惠山区");
        CampusDTO campusDTO = campusService.updateCampus(campusVO, oldCampusDO);
        Assertions.assertNotNull(campusDTO);
        Assertions.assertEquals(campusVO.getCampusName(), campusDTO.getCampusName());
        Assertions.assertEquals(campusVO.getCampusCode(), campusDTO.getCampusCode());
        Assertions.assertEquals(campusVO.getCampusDesc(), campusDTO.getCampusDesc());
        Assertions.assertEquals(campusVO.getCampusStatus(), campusDTO.getCampusStatus());
        Assertions.assertEquals(campusVO.getCampusAddress(), campusDTO.getCampusAddress());
        campusDAO.removeById(oldCampusDO);
    }

    @Test
    void testCheckUpdateCampusVO() {
        CampusDO campusDO = new CampusDO();
        campusDO.setCampusUuid(UuidUtil.generateUuidNoDash())
                .setCampusCode("1456789")
                .setCampusName("无锡学院校区")
                .setCampusStatus(1)
                .setCampusDesc("好学校")
                .setCampusAddress("锡山区");
        campusDAO.save(campusDO);
        CampusDO campusDO1 = new CampusDO();
        campusDO1.setCampusUuid(UuidUtil.generateUuidNoDash())
                .setCampusCode("123456")
                .setCampusName("东南大学校区")
                .setCampusStatus(0)
                .setCampusDesc("好学校")
                .setCampusAddress("锡山区");
        campusDAO.save(campusDO1);
        log.debug("测试校区名称重复");
        CampusVO campusVO = new CampusVO(
                "东南大学校区", "1456789",
                "好学校", 0, "锡山区");
        String campusUuid = campusDO.getCampusUuid();
        Assertions.assertThrows(BusinessException.class, () ->
                campusService.checkUpdateCampusVO(campusUuid, campusVO)
        );
        log.debug("测试校区编码重复");
        CampusVO campusVO1 = new CampusVO(
                "无锡学院校区", "123456",
                "好学校", 0, "锡山区");
        Assertions.assertThrows(BusinessException.class, () ->
                campusService.checkUpdateCampusVO(campusUuid, campusVO1)
        );
        campusDAO.removeById(campusDO);
        campusDAO.removeById(campusDO1);
    }
}
