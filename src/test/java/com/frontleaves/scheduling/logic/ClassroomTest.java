package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.ClassroomDAO;
import com.frontleaves.scheduling.models.dto.ClassroomAndTypeDTO;
import com.frontleaves.scheduling.models.entity.ClassroomDO;
import com.frontleaves.scheduling.services.ClassroomService;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
class ClassroomTest {
    @Resource
    private ClassroomService classroomService;

    @Resource
    private ClassroomDAO classroomDAO;


    @Test
    void testGetClassroomAndTypeByUuidWihError() {
        //准备
        ClassroomDO classroomDO = classroomDAO.lambdaQuery().list().get(0);
        //执行
        ClassroomAndTypeDTO classroomAndType =
                classroomService.getClassroomAndTypeByUuidWihError(classroomDO.getClassroomUuid());
        //断言
        Assertions.assertNotNull(classroomAndType);
        String uuid = UuidUtil.generateUuidNoDash();
        Assertions.assertThrows(BusinessException.class, () ->
                classroomService.getClassroomAndTypeByUuidWihError(uuid)
        );
    }
}
