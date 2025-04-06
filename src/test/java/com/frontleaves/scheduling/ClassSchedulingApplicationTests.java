package com.frontleaves.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class ClassSchedulingApplicationTests {

    @Test
    void contextLoads() {
        log.debug("Test case executed successfully");
        Assertions.assertTrue(true);
    }

}
