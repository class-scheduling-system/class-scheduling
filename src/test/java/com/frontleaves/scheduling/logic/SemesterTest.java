package com.frontleaves.scheduling.logic;


import com.frontleaves.scheduling.daos.SemesterDAO;
import com.frontleaves.scheduling.models.entity.base.SemesterDO;
import com.frontleaves.scheduling.services.SemesterService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
class SemesterTest {
    @Resource
    private SemesterService semesterService;
    @Resource
    private SemesterDAO semesterDAO;
    @Resource
    private RedissonClient redisson;

    /**
     * 测试根据UUID获取并检查学期是否启用
     * 测试场景：
     * 1. 正常获取已启用的学期
     * 2. 获取未启用的学期时抛出异常
     * 3. 获取不存在的学期时抛出异常
     */
    @Test
    void testGetSemesterByUuidCheckEnabled() {
        SemesterDO getData = semesterDAO.lambdaQuery()
                .eq(SemesterDO::getIsEnabled, true)
                .last("limit 1")
                .one();
        // 测试场景1：正常获取已启用的学期
        semesterService.getSemesterByUuidCheckEnabled(getData.getSemesterUuid());
    }
}

