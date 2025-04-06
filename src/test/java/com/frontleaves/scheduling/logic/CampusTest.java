package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.CampusDAO;
import com.frontleaves.scheduling.models.dto.base.CampusDTO;
import com.frontleaves.scheduling.models.entity.base.CampusDO;
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

import java.util.Optional;
import java.util.stream.Stream;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
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
                                "好学校", true, "锡山区"), "校区名称不能为空"),
                Arguments.of(new CampusVO("无锡学院校区", "",
                        "好学校", true, "锡山区"), "校区编码不能为空"),
                Arguments.of(new CampusVO("无锡学院校区", "1456789",
                        "", true, "锡山区"), "校区描述不能为空"),
                Arguments.of(new CampusVO("无锡学院校区", "1456789",
                        "好学校", null, "锡山区"), "校区状态不能为空"),
                Arguments.of(new CampusVO("无锡学院校区", "1456789",
                        "好学校", true, ""), "校区地址不能为空")
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
                "好学校", true, "锡山区");
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
                .setCampusStatus(true)
                .setCampusDesc("好学校")
                .setCampusAddress("锡山区");
        campusDAO.save(campusDO);
        CampusDO oldCampusDO = campusDAO.lambdaQuery()
                .eq(CampusDO::getCampusUuid, campusDO.getCampusUuid()).one();
        CampusVO campusVO = new CampusVO(
                "东南大学校区", "123456",
                "坏学校", false, "惠山区");
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
                .setCampusStatus(true)
                .setCampusDesc("好学校")
                .setCampusAddress("锡山区");
        campusDAO.save(campusDO);
        CampusDO campusDO1 = new CampusDO();
        campusDO1.setCampusUuid(UuidUtil.generateUuidNoDash())
                .setCampusCode("123456")
                .setCampusName("东南大学校区")
                .setCampusStatus(false)
                .setCampusDesc("好学校")
                .setCampusAddress("锡山区");
        campusDAO.save(campusDO1);
        log.debug("测试校区名称重复");
        CampusVO campusVO = new CampusVO(
                "东南大学校区", "1456789",
                "好学校", false, "锡山区");
        String campusUuid = campusDO.getCampusUuid();
        Assertions.assertThrows(BusinessException.class, () ->
                campusService.checkUpdateCampusVO(campusUuid, campusVO)
        );
        log.debug("测试校区编码重复");
        CampusVO campusVO1 = new CampusVO(
                "无锡学院校区", "123456",
                "好学校", false, "锡山区");
        Assertions.assertThrows(BusinessException.class, () ->
                campusService.checkUpdateCampusVO(campusUuid, campusVO1)
        );
        campusDAO.removeById(campusDO);
        campusDAO.removeById(campusDO1);
    }

    @Test
    void testDeleteCampus (){
        CampusDO campusDO = campusDAO.lambdaQuery().list().get(0);
        campusService.deleteCampus(campusDO);
        Assertions.assertNull(
                campusDAO.lambdaQuery().eq(CampusDO::getCampusUuid,campusDO.getCampusUuid()).one());
        campusDAO.save(campusDO);
    }

    /**
     * 验证校区分页查询结果为空的情况
     * <p>
     * 该方法用于测试当查询条件无法匹配到任何校区时，返回的分页数据是否为空。
     * 具体来说，它执行两次调用 {@code campusService.getPageOfCampus} 方法的操作：
     * 第一次传入一个确定不存在的校区名称，预期返回的记录列表应为空；
     * 第二次则传入空值作为校区名称参数，预期能够返回非空的记录列表。
     * <p>
     * 此测试确保了系统在处理无效或缺失的查询条件时的行为符合预期。
     */
    @Test
    void testVerifyPageOfCampusHasEmpty() {
        Optional.ofNullable(campusService.getPageOfCampus(1, 20, true, "不存在的校区"))
                .ifPresent(pageDTO -> Assertions.assertTrue(pageDTO.getRecords().isEmpty()));
        Optional.ofNullable(campusService.getPageOfCampus(1, 20, true, null))
                .ifPresent(pageDTO -> Assertions.assertFalse(pageDTO.getRecords().isEmpty()));
    }

    /**
     * 测试获取校区列表
     * <p>
     * 该方法用于验证 {@code campusService.getCampusList()} 方法是否能够成功返回一个非空的校区列表。
     * 如果 {@code campusService.getCampusList()} 返回的列表为空，则测试失败。
     * <p>
     * 此测试确保了系统能够正确地从数据源中获取校区信息，并且这些信息在系统中是可用的。
     */
    @Test
    void testGetCampusList() {
        Assertions.assertFalse(campusService.getCampusList().isEmpty());
    }
}
