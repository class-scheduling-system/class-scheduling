package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.daos.MajorDAO;
import com.frontleaves.scheduling.daos.StudentDAO;
import com.frontleaves.scheduling.models.entity.MajorDO;
import com.frontleaves.scheduling.models.entity.StudentDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class UserTest {
    @Resource
    private StudentDAO studentDAO;
    @Resource
    private MajorDAO majorDAO;

    @Test
    void testUserLogin() {
        StudentDO studentDO = new StudentDO();
        MajorDO majorDO = new MajorDO();
        majorDO.setMajorName("软件工程")
                .setMajorCode("123456")
                .setMajorDescription("好专业")
                .setMajorStatus(1);
        majorDAO.save(majorDO);
        MajorDO majorDO1 = majorDAO.lambdaQuery().eq(MajorDO::getMajorCode,"123456").one();
        studentDO.setId("2022")
                .setName("小王")
                .setGender(1)
                .setGrade("12")
                .setDepartment("2aac32e33a0644b4898a77ddeee47231")
                .setMajor(majorDO1.getMajorUuid())
                .setClazz("123");
        studentDAO.save(studentDO);
    }

}
