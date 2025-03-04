package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.CampusDAO;
import com.frontleaves.scheduling.models.dto.CampusDTO;
import com.frontleaves.scheduling.models.entity.CampusDO;
import com.frontleaves.scheduling.models.vo.CampusVO;
import com.frontleaves.scheduling.services.CampusService;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
class CampusTest {

    @Resource
    private CampusService campusService;
    @Autowired
    private CampusDAO campusDAO;

    @Test
    void testCheckAddCampusVO() {
        CampusVO campusVO = new CampusVO(
                "无锡学院校区", "1456789",
                "好学校", 1, "锡山区");
        CampusDO campusDO = new CampusDO();
        campusDO.setCampusUuid(UuidUtil.generateUuidNoDash())
                .setCampusCode("1456789")
                .setCampusName("无锡学院校区")
                .setCampusStatus(1)
                .setCampusDesc("好学校")
                .setCampusAddress("锡山区");
        campusDAO.save(campusDO);
        //校区编码重复拦截报错
        Assertions.assertThrows(BusinessException.class, () ->
                campusService.checkAddCampusVO(campusVO)
        );
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

}
