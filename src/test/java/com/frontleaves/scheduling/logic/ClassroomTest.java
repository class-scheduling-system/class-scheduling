package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.ClassroomDAO;
import com.frontleaves.scheduling.services.ClassroomService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
class ClassroomTest {
    @Resource
    private ClassroomService classroomService;

    @Resource
    private ClassroomDAO classroomDAO;


}
