package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.MajorDO;
import com.frontleaves.scheduling.models.entity.StudentDO;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.ConvertUtil;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 专业相关功能测试
 * <p>
 * 该类用于对专业相关的功能进行单元测试，确保各项操作符合预期。
 * 测试内容包括缓存数据的读取、数据库查询、以及数据一致性验证。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@SpringBootTest
class MajorTest {
    @Resource
    private MajorDAO majorDAO;
    @Resource
    private RedissonClient redisson;
    @Autowired
    private DepartmentDAO departmentDAO;
    @Autowired
    private StudentDAO studentDAO;
    @Autowired
    private GradeDAO gradeDAO;
    @Autowired
    private AdministrativeClassDAO administrativeClassDAO;

    @BeforeEach
    void setUp() {
        // 清理 Redis 和数据库中的测试数据,确保环境干净
        String majorUuid = "test_major_uuid";
        redisson.getMap(StringConstant.Redis.MAJOR_UUID + majorUuid).delete();
        majorDAO.removeById(majorUuid);
    }

    /**
     * 测试通过UUID获取专业信息,
     * 对比使用Redis缓存和不使用Redis缓存的情况
     */
    @Test
    void testMajorByUuid() {
        MajorDO majorDO = majorDAO.lambdaQuery().list().get(0);
        redisson.getKeys().delete(StringConstant.Redis.MAJOR_UUID + majorDO.getMajorUuid());

        long noRedisNowTime = System.currentTimeMillis();
        MajorDO majorNoRedis = majorDAO.getMajorByUuid(majorDO.getMajorUuid());
        log.info("[MajorUUID] No Redis Time: {}ms", System.currentTimeMillis() - noRedisNowTime);

        long redisNowTime = System.currentTimeMillis();
        MajorDO majorHasRedis = majorDAO.getMajorByUuid(majorDO.getMajorUuid());
        log.info("[MajorUUID] Redis Time: {}ms", System.currentTimeMillis() - redisNowTime);

        Assertions.assertEquals(majorNoRedis, majorHasRedis);
    }

    /**
     * 测试在 Redis 有数据的情况下,
     * 通过 UUID 获取专业信息的方法
     */
    @Test
    void testGetMajorByUuid_WhenRedisHasData() {
        // 1. 先在 Redis 中插入测试数据
        String majorUuid = "test_major_uuid";
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.MAJOR_UUID + majorUuid);
        MajorDO majorDO = new MajorDO();
        majorDO.setMajorUuid(majorUuid);
        majorDO.setMajorName("测试专业");
        // 转换对象为 Map 并存入 Redis
        map.putAll(ConvertUtil.convertObjectToMapString(majorDO));
        map.expire(Duration.ofSeconds(86400));

        // 2. 调用方法
        MajorDO result = majorDAO.getMajorByUuid(majorUuid);

        // 3. 断言 Redis 数据能正确返回
        assertNotNull(result);
        assertEquals("测试专业", result.getMajorName());
    }

    /**
     * 测试在 Redis 和数据库都没有数据时，
     * 通过 UUID 获取专业信息的行为
     */
    @Test
    void testGetMajorByUuid_WhenRedisAndDatabaseHaveNoData() {
        // 1. 确保 Redis 和数据库都没有数据
        String majorUuid = "test_major_uuid";
        redisson.getMap(StringConstant.Redis.MAJOR_UUID + majorUuid).delete();
        majorDAO.removeById(majorUuid);

        // 2. 调用方法
        MajorDO result = majorDAO.getMajorByUuid(majorUuid);

        // 3. 断言返回 null
        assertNull(result);
    }


    /**
     * 测试当专业不存在时,验证isReferenced方法是否抛出正确的异常信息;
     * 此测试用例的目的是确保当查询一个不存在的专业时,系统能够给出明确的错误提示
     */
    @Test
    void testIsReferenced_MajorNotFound() {
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> majorDAO.isReferenced("nonexistent-uuid"));

        Assertions.assertEquals("该专业不存在", exception.getMessage());
        Assertions.assertEquals(ErrorCode.BODY_ERROR, exception.getErrorCode());
    }

    /**
     * 测试删除专业(仅在未被引用的情况下)
     * <p>
     * 该测试方法验证了删除专业功能是否正常工作。具体步骤如下：
     * <ol>
     *     <li>创建并插入一个专业信息到数据库。</li>
     *     <li>调用 {@code deleteMajor} 方法删除指定的专业。</li>
     *     <li>验证数据库中是否已成功删除该专业记录。</li>
     * </ol>
     * <p>
     * 如果所有检查都通过，则表示删除专业功能正常。
     * </p>
     */
    @Test
    void testDeleteMajor() {
        // 创建并插入测试数据到数据库
        String majorUuid = UuidUtil.generateUuidNoDash();
        // 这个是外键,获取一个部门的UUID
        DepartmentDO departmentDO = departmentDAO.lambdaQuery().list().get(0);
        MajorDO majorDO = new MajorDO()
                .setMajorUuid(majorUuid)
                .setMajorName("汽车")
                .setMajorDescription("这是一个测试专业")
                .setMajorCode("22001")
                .setMajorStatus(true)
                .setEducationYears((short) 4)
                .setTrainingLevel("Undergraduate")
                .setDepartmentUuid(departmentDO.getDepartmentUuid());
        if (majorDAO.lambdaQuery().eq(MajorDO::getMajorName, "汽车").exists()) {
            majorDAO.lambdaUpdate().eq(MajorDO::getMajorName, "汽车").remove();
        }
        majorDAO.save(majorDO);

        // 假设使用Redis存储了专业信息
        redisson.getBucket(StringConstant.Redis.MAJOR_UUID + majorDO.getMajorUuid()).set(majorDO);

        // 确保Redis中存储了该专业
        assertTrue(redisson.getBucket(StringConstant.Redis.MAJOR_UUID + majorDO.getMajorUuid()).isExists());

        // 删除专业
        majorDAO.deleteMajor(majorDO.getMajorUuid());

        // 确保Redis缓存已经被清除
        assertFalse(redisson.getBucket(StringConstant.Redis.MAJOR_UUID + majorDO.getMajorUuid()).isExists());

        // 确保数据库中该专业已经被删除
        assertNull(majorDAO.getMajorByUuid(majorDO.getMajorUuid()));
    }


    /**
     * 测试删除专业时，专业已被引用，验证是否抛出正确的异常信息
     * <p>
     * 该测试方法验证了当专业已被引用时，删除专业功能是否抛出 {@code BusinessException} 异常。
     * </p>
     */
    @Test
    void testDeleteMajor_WhenReferenced_ShouldThrowBusinessException() {
        // 创建并插入测试数据到数据库
        String newMajorUuid;
        MajorDO getMajor = majorDAO.lambdaQuery().eq(MajorDO::getMajorName, "测试专业").one();
        DepartmentDO getDepartment = departmentDAO.lambdaQuery().list().get(0);
        if (getMajor == null) {
            newMajorUuid = UuidUtil.generateUuidNoDash();
            MajorDO majorDO = new MajorDO()
                    .setDepartmentUuid(getDepartment.getDepartmentUuid())
                    .setMajorUuid(newMajorUuid)
                    .setMajorName("测试专业")
                    .setMajorDescription("这是一个测试专业")
                    .setMajorCode("TM001")
                    .setMajorStatus(true)
                    .setEducationYears((short) 4)
                    .setTrainingLevel("Undergraduate");
            majorDAO.save(majorDO);
        } else {
            newMajorUuid = getMajor.getMajorUuid();
        }

        // 添加一个学生绑定当前专业
        String newStudentUuid;
        StudentDO getStudent = studentDAO.lambdaQuery().eq(StudentDO::getName, "ZhangSan1314").one();
        if (getStudent != null) {
            studentDAO.removeById(getStudent);
        }
        newStudentUuid = UuidUtil.generateUuidNoDash();
        StudentDO studentDO = new StudentDO()
                .setStudentUuid(newStudentUuid)
                .setId("1")
                .setName("ZhangSan1314")
                .setGender(true)
                .setGradeUuid(gradeDAO.lambdaQuery().list().get(0).getGradeUuid())
                .setDepartment(getDepartment.getDepartmentUuid())
                .setClazz(administrativeClassDAO.lambdaQuery().list().get(0).getAdministrativeClassUuid())
                .setMajor(newMajorUuid);
        studentDAO.save(studentDO);

        // 模拟 isReferenced 返回 true
        Assertions.assertThrows(BusinessException.class, () -> majorDAO.isReferenced(newMajorUuid));

        // 检查数据库数据是否存在
        Assertions.assertNotNull(majorDAO.lambdaQuery().eq(MajorDO::getMajorUuid, newMajorUuid).one());
        Assertions.assertNotNull(studentDAO.lambdaQuery().eq(StudentDO::getMajor, newMajorUuid).one());

        // 清理数据
        studentDAO.lambdaUpdate().eq(StudentDO::getMajor, newMajorUuid).remove();
        majorDAO.lambdaUpdate().eq(MajorDO::getMajorUuid, newMajorUuid).remove();
    }

    /**
     * 测试删除专业时，专业不存在，验证是否抛出正确的异常信息
     * <p>
     * 该测试方法验证了当专业不存在时，删除专业功能是否抛出 {@code BusinessException} 异常。
     * </p>
     */
    @Test
    void testDeleteMajor_WhenMajorDoesNotExist_ShouldThrowBusinessException() {
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> majorDAO.deleteMajor("nonexistent-uuid"));

        Assertions.assertEquals("该专业不存在", exception.getMessage());
        Assertions.assertEquals(ErrorCode.BODY_ERROR, exception.getErrorCode());
    }


    /**
     * 测试当学院不存在时，查询专业列表抛出异常
     * <p>
     * 该测试方法验证了当学院不存在时，查询专业列表是否抛出 {@code BusinessException} 异常。
     * </p>
     */
    @Test
    void testListMajors_WhenDepartmentDoesNotExist_ShouldThrowBusinessException() {
        Assertions.assertThrows(BusinessException.class,
                () -> majorDAO.listMajors(1, 10, null, "NonExistentDepartment", "Computer Science"), "Expected BusinessException to be thrown");
    }


    @Test
    void testGetMajorList() {
        // 先清除Redis中的专业列表缓存
        redisson.getKeys().delete(StringConstant.Redis.MAJOR_LIST);

        // 第一次调用getMajorList方法，应该从数据库获取数据并缓存到Redis
        List<MajorDO> majorList1 = majorDAO.getMajorList();

        // 断言从数据库获取的专业列表不为空
        Assertions.assertNotNull(majorList1);
        Assertions.assertFalse(majorList1.isEmpty());

        // 验证Redis中是否已缓存专业列表
        RList<MajorDO> redisCache = redisson.getList(StringConstant.Redis.MAJOR_LIST);
        Assertions.assertTrue(redisCache.isExists());

        // 记录第一次查询结果的大小
        int firstResultSize = majorList1.size();

        // 第二次调用getMajorList方法，应该从Redis缓存中获取数据
        List<MajorDO> majorList2 = majorDAO.getMajorList();

        // 断言第二次获取的结果与第一次结果大小相同
        Assertions.assertEquals(firstResultSize, majorList2.size());
    }
    @Test
    void testGetMajorListByDepartmentUuidForUpdate (){
        String departmentUuid = majorDAO.lambdaQuery().list().get(0).getDepartmentUuid();
        // 先清除Redis中的专业列表缓存
        redisson.getList(
                StringConstant.Redis.MAJOR_LIST_BY_DEPARTMENT_UUID + departmentUuid).delete();
        List<MajorDO> majorDOList = majorDAO.getMajorListByDepartmentUuidForUpdate(departmentUuid);
        Assertions.assertFalse(majorDOList.isEmpty());
        RList<MajorDO> rList = redisson.getList(
                StringConstant.Redis.MAJOR_LIST_BY_DEPARTMENT_UUID + departmentUuid);
        Assertions.assertTrue(rList.isExists());
    }
}
