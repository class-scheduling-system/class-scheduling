package com.frontleaves.scheduling.dao;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.constants.SystemConstant;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.entity.*;
import com.frontleaves.scheduling.models.vo.StudentVO;
import com.frontleaves.scheduling.models.dto.BackAddStudentDTO;
import com.frontleaves.scheduling.models.entity.StudentDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.ConvertUtil;
import com.xlf.utility.util.PasswordUtil;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;

@Slf4j
@SpringBootTest
class StudentTest {
    @Resource
    private StudentDAO studentDAO;

    @Resource
    private UserDAO userDAO;
    @Resource
    private DepartmentDAO departmentDAO;
    @Resource
    private MajorDAO majorDAO;
    @Resource
    private RedissonClient redisson;
    @Resource
    private AdministrativeClassDAO administrativeClassDAO;
    @Resource
    private GradeDAO gradeDAO;
    private StudentDO setUpStudent;
    private UserDO setUpUser;

    /**
     * 通过部门 名称获取部门数据
     *
     * @return 部门数据
     */
    private DepartmentDO getDepartmentByName() {
        DepartmentDO departmentDO = departmentDAO.lambdaQuery().list().get(0);
        if (departmentDO == null) {
            throw new BusinessException("[dao.StudentTest]单元测试通过找不到部门数据",
                    ErrorCode.OPERATION_ERROR);
        }
        return departmentDO;
    }

    /**
     * 通过专业名称获取专业数据
     *
     * @return 专业数据
     */
    private MajorDO getMajorByName() {
        MajorDO majorDO = majorDAO.lambdaQuery().list().get(0);
        if (majorDO == null) {
            throw new BusinessException("[dao.StudentTest]单元测试通过找不到专业数据",
                    ErrorCode.OPERATION_ERROR);
        }
        return majorDO;
    }


    @BeforeEach
    void setUp() {
        log.debug("StudentDAO单元测试初始化");
        setUpUser = new UserDO();
        setUpUser.setUserUuid(UuidUtil.generateUuidNoDash())
                .setName("studentDAOTest")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("studentDAOTest@qwer.com")
                .setPhone("14452873800")
                .setStatus((byte) 1)
                .setBan(false)
                .setRoleUuid(SystemConstant.getRoleStudent())
                .setPermission("[\"user:role:edit\"]");
        if (userDAO.lambdaQuery().eq(UserDO::getName, setUpUser.getName()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, setUpUser.getName()).remove();
        }
        userDAO.save(setUpUser);
        setUpStudent = new StudentDO();
        log.debug("初始化学生信息");
        setUpStudent.setStudentUuid(UuidUtil.generateUuidNoDash())
                .setId("1")
                .setName("ZhangSan1314")
                .setGender(true)
                .setGradeUuid(gradeDAO.lambdaQuery().list().get(0).getGradeUuid())
                .setDepartment(getDepartmentByName().getDepartmentUuid())
                .setMajor(getMajorByName().getMajorUuid())
                .setClazz(administrativeClassDAO.lambdaQuery().list().get(0).getAdministrativeClassUuid())
                .setGraduated(false)
                .setUserUuid(setUpUser.getUserUuid());
        if (studentDAO.lambdaQuery().eq(StudentDO::getName, setUpStudent.getName()).one() != null) {
            studentDAO.lambdaUpdate().eq(StudentDO::getName, setUpStudent.getName()).remove();
        }
        log.debug("保存数据");
        studentDAO.save(setUpStudent);
        //添加缓存
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.STUDENT_UUID + setUpStudent.getStudentUuid());
        map.putAll(ConvertUtil.convertObjectToMapString(setUpStudent));
        map.expire(Duration.ofSeconds(86400));
        RBucket<String> bucketId = redisson.getBucket(StringConstant.Redis.STUDENT_ID + setUpStudent.getId());
        bucketId.set(setUpStudent.getStudentUuid());
        bucketId.expire(Duration.ofSeconds(86400));
        RBucket<String> bucketUserUuid = redisson.getBucket(StringConstant.Redis.STUDENT_USER_UUID +
                setUpStudent.getUserUuid());
        bucketUserUuid.set(setUpStudent.getStudentUuid());
        bucketUserUuid.expire(Duration.ofSeconds(86400));
    }

    @AfterEach
    void tearDown() {
        log.debug("学生信息清理");
        if (studentDAO.lambdaQuery().eq(StudentDO::getStudentUuid, setUpStudent.getStudentUuid()).one() != null) {
            studentDAO.lambdaUpdate().eq(StudentDO::getStudentUuid, setUpStudent.getStudentUuid()).remove();
        }
        if (userDAO.lambdaQuery().eq(UserDO::getUserUuid, setUpUser.getUserUuid()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getUserUuid, setUpUser.getUserUuid()).remove();
        }
        redisson.getMap(StringConstant.Redis.STUDENT_UUID + setUpStudent.getStudentUuid()).delete();
        redisson.getBucket(StringConstant.Redis.STUDENT_ID + setUpStudent.getId()).delete();
        redisson.getBucket(StringConstant.Redis.STUDENT_USER_UUID + setUpStudent.getUserUuid()).delete();
    }


    @Test
    void testGetStudentById() {
        log.debug("测试通过学生 ID 获取学生信息");
        StudentDO studentById = studentDAO.getStudentById(setUpStudent.getId());
        Assertions.assertNotNull(studentById);
        redisson.getBucket(StringConstant.Redis.STUDENT_ID + setUpStudent.getId()).delete();
        StudentDO studentById1 = studentDAO.getStudentById(setUpStudent.getId());
        Assertions.assertNotNull(studentById1);
    }

    @Test
    void testGetStudentByUuid() {
        log.debug("测试通过学生 UUID 获取学生信息");
        StudentDO studentByUuid = studentDAO.getStudentByUuid(setUpStudent.getStudentUuid());
        Assertions.assertNotNull(studentByUuid);
        redisson.getMap(StringConstant.Redis.STUDENT_UUID + setUpStudent.getStudentUuid()).delete();
        StudentDO studentByUuid1 = studentDAO.getStudentByUuid(setUpStudent.getStudentUuid());
        Assertions.assertNotNull(studentByUuid1);
    }

    @Test
    void testUpdateUserUuid() {
        log.debug("测试更新学生信息中的用户 UUID");
        //创建一个新的教师用户
        UserDO newTestUserDO = new UserDO();
        newTestUserDO.setUserUuid(UuidUtil.generateUuidNoDash())
                .setName("studentDAONewTestUser")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("studentDAONewTestUser@qwer.com")
                .setPhone("15859273800")
                .setStatus((byte) 1)
                .setBan(false)
                .setRoleUuid(SystemConstant.getRoleStudent())
                .setPermission("[\"user:role:edit\"]");
        if (userDAO.lambdaQuery().eq(UserDO::getName, newTestUserDO.getName()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, newTestUserDO.getName()).remove();
        }
        userDAO.save(newTestUserDO);
        studentDAO.updateUserUuid(newTestUserDO.getUserUuid(), setUpStudent.getId());
        StudentDO studentDO1 = studentDAO.lambdaQuery().eq(StudentDO::getId, setUpStudent.getId()).one();
        Assertions.assertEquals(newTestUserDO.getUserUuid(), studentDO1.getUserUuid());
        //删除学生和新增用户数据
        if (studentDAO.lambdaQuery().eq(StudentDO::getStudentUuid, studentDO1.getStudentUuid()).one() != null) {
            studentDAO.lambdaUpdate().eq(StudentDO::getStudentUuid, studentDO1.getStudentUuid()).remove();
        }
        if (userDAO.lambdaQuery().eq(UserDO::getUserUuid, newTestUserDO.getUserUuid()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getUserUuid, newTestUserDO.getUserUuid()).remove();
        }
    }

    @Test
    void testDeleteStudent() {
        log.debug("测试删除学生信息");
        studentDAO.deleteStudent(setUpStudent);
        StudentDO studentDO1 = studentDAO.lambdaQuery().eq(StudentDO::getId, setUpStudent.getId()).one();
        log.debug("检查缓存是否删除");
        RMap<String, String> getMapById = redisson.getMap(StringConstant.Redis.STUDENT_ID + setUpStudent.getId());
        RMap<String, String> getMapByUuid = redisson.getMap(StringConstant.Redis.STUDENT_UUID + setUpStudent.getStudentUuid());
        RMap<String, String> getMapByUserUuid = redisson.getMap(StringConstant.Redis.STUDENT_USER_UUID
                + setUpStudent.getUserUuid());
        Assertions.assertNull(studentDO1);
        Assertions.assertFalse(getMapById.isExists());
        Assertions.assertFalse(getMapByUuid.isExists());
        Assertions.assertFalse(getMapByUserUuid.isExists());
    }

    @Test
    void testGetStudentByUserUuid() {
        log.debug("测试通过用户 UUID 获取学生信息");
        StudentDO studentByUserUuid = studentDAO.getStudentByUserUuid(setUpStudent.getUserUuid());
        Assertions.assertNotNull(studentByUserUuid);
        redisson.getBucket(StringConstant.Redis.STUDENT_USER_UUID + setUpStudent.getUserUuid()).delete();
        StudentDO studentByUserUuid1 = studentDAO.getStudentByUserUuid(setUpStudent.getUserUuid());
        Assertions.assertNotNull(studentByUserUuid1);
    }

    /**
     * 测试获取学生列表
     */
    @Test
    void testListStudents() {
        log.debug("测试获取学生列表");
        // 清空 Redis 缓存, 以防影响测试
        redisson.getMapCache(StringConstant.Redis.STUDENT_LIST).clear();

        // 从行政班级中获取一个班级 UUID
        List<AdministrativeClassDO> administrativeClassList = administrativeClassDAO.lambdaQuery().list();
        Assertions.assertFalse(administrativeClassList.isEmpty(), "行政班级列表不能为空");
        String clazzUuid = administrativeClassList.get(0).getAdministrativeClassUuid();
        Assertions.assertNotNull(clazzUuid, "班级 UUID 不能为空");

        // 获取班级映射
        AdministrativeClassDO classMapping = administrativeClassDAO.getAdministrativeClassMappingByClazz(clazzUuid);
        Assertions.assertNotNull(classMapping, "班级映射信息不能为空");
        Assertions.assertNotNull(classMapping.getGradeUuid(), "年级 UUID 不能为空");
        Assertions.assertNotNull(classMapping.getDepartmentUuid(), "学院 UUID 不能为空");
        Assertions.assertNotNull(classMapping.getMajorUuid(), "专业 UUID 不能为空");

        // 获取有效用户数据
        List<UserDO> userList = userDAO.lambdaQuery().list();
        Assertions.assertFalse(userList.isEmpty(), "用户列表不能为空");
        UserDO userDO = userList.get(1);

        String studentUuid = UuidUtil.generateUuidNoDash();
        String studentId = "233336";
        // 如果存在相同学号的学生记录，先删除
        studentDAO.lambdaUpdate().eq(StudentDO::getId, studentId).remove();

        StudentDO testStudent = new StudentDO()
                .setStudentUuid(studentUuid)
                .setId(studentId)
                .setName("王五")
                .setClazz(clazzUuid)
                .setGraduated(false)
                .setGender(false)
                .setGradeUuid(classMapping.getGradeUuid())
                .setDepartment(classMapping.getDepartmentUuid())
                .setMajor(classMapping.getMajorUuid())
                .setUserUuid(userDO.getUserUuid());
        boolean saveResult = studentDAO.save(testStudent);
        Assertions.assertTrue(saveResult, "学生数据保存失败");
        Assertions.assertNotNull(studentDAO.getById(studentUuid), "学生数据未成功插入");

        // 模拟查询数据库
        Page<StudentDO> pageResult = studentDAO.listStudents(
                1, 10, true,
                clazzUuid, false,
                "王五", studentId);
        Assertions.assertNotNull(pageResult, "listStudents返回值不应为null");
        Assertions.assertFalse(pageResult.getRecords().isEmpty(), "列表查询应至少返回一条记录");
        Assertions.assertEquals(1, pageResult.getRecords().size(), "应返回1条记录");

        // 模拟 Redis 缓存命中
        Page<StudentDO> cachedPage = studentDAO.listStudents(
                1, 10, true,
                clazzUuid, false,
                "王五", studentId);
        Assertions.assertNotNull(cachedPage, "listStudents返回值不应为null");
        Assertions.assertFalse(cachedPage.getRecords().isEmpty(), "缓存命中时列表查询应返回记录");
        Assertions.assertEquals(pageResult.getRecords().size(), cachedPage.getRecords().size(), "缓存结果应与数据库查询结果一致");

        // 当所有选填字段为空时, 返回所有学生
        Page<StudentDO> allStudentPage = studentDAO.listStudents(
                1, 10, null,
                null, null, null, null
        );
        Assertions.assertNotNull(allStudentPage, "listStudents返回值不应为null");
        Assertions.assertFalse(allStudentPage.getRecords().isEmpty(), "全空查询应至少返回1条记录");
    }

    /**
     * 测试编辑学生
     */
    @Test
    void testEditStudent() {
        log.debug("测试编辑学生信息");

        String studentUuid = UuidUtil.generateUuidNoDash();
        String studentId = "A951753";

        // 1. 如果存在相同学号的学生记录，先删除
        studentDAO.lambdaUpdate().eq(StudentDO::getId, studentId).remove();

        // 2. 构造初始学生数据并保存
        String clazzUuid = administrativeClassDAO.lambdaQuery().list().get(1).getAdministrativeClassUuid();
        Assertions.assertNotNull(clazzUuid, "班级 UUID 不能为空");

        AdministrativeClassDO classMapping = administrativeClassDAO.getAdministrativeClassMappingByClazz(clazzUuid);
        Assertions.assertNotNull(classMapping, "班级映射信息不能为空");
        Assertions.assertNotNull(classMapping.getGradeUuid(), "年级 UUID 不能为空");
        Assertions.assertNotNull(classMapping.getDepartmentUuid(), "学院 UUID 不能为空");
        Assertions.assertNotNull(classMapping.getMajorUuid(), "专业 UUID 不能为空");

        StudentDO initialStudent = new StudentDO();
        UserDO userDO = userDAO.lambdaQuery().list().get(1);
        initialStudent.setStudentUuid(studentUuid)
                .setId(studentId)
                .setName("OriginalName")
                .setGender(false)
                .setClazz(clazzUuid)
                .setGraduated(false)
                .setGradeUuid(classMapping.getGradeUuid())
                .setDepartment(classMapping.getDepartmentUuid())
                .setMajor(classMapping.getMajorUuid())
                .setUserUuid(userDO.getUserUuid());
        studentDAO.save(initialStudent);

        // 3. 确保初始数据写入缓存
        RMap<String, String> initialStudentCache = redisson.getMap(StringConstant.Redis.STUDENT_UUID + studentUuid);
        initialStudentCache.putAll(ConvertUtil.convertObjectToMapString(initialStudent));
        initialStudentCache.expire(Duration.ofSeconds(86400));

        // 4. 构造更新用的 VO 对象
        StudentVO studentTestVO = new StudentVO();
        studentTestVO.setId(studentId)
                .setName("LiSi456")
                .setGender(false)
                .setClazz(clazzUuid)
                .setGraduated(true);

        // 5. 执行更新操作
        StudentDO editStudent = null;
        try {
            editStudent = studentDAO.editStudent(studentUuid, studentTestVO);
        } catch (BusinessException e) {
            Assertions.fail("editStudent 异常：" + e.getMessage());
        }

        // 6. 校验更新结果
        Assertions.assertNotNull(editStudent, "更新后的学生不应为null");
        Assertions.assertEquals("A951753", editStudent.getId(), "学号应该被更新");
        Assertions.assertEquals("LiSi456", editStudent.getName(), "姓名应该被更新");
        Assertions.assertTrue(editStudent.getGraduated(), "毕业状态应更新为 true");

        // 7. 验证数据库中数据是否更新
        StudentDO dbStudent = studentDAO.lambdaQuery().eq(StudentDO::getStudentUuid, studentUuid).one();
        Assertions.assertNotNull(dbStudent, "数据库中应存在学生记录");
        Assertions.assertEquals(editStudent.getName(), dbStudent.getName(), "数据库中的姓名应该被更新");
        Assertions.assertTrue(dbStudent.getGraduated(), "数据库中的毕业状态应更新为 true");

        // 8. 验证缓存是否更新
        RMap<String, String> studentCache = redisson.getMap(StringConstant.Redis.STUDENT_UUID + studentUuid);
        Assertions.assertTrue(studentCache.isExists(), "学生缓存应存在");
        String cachedName = studentCache.get("name");
        Assertions.assertEquals(editStudent.getName(), cachedName, "缓存中的姓名应该被更新");
    }

    @Test
    void testSaveStudentBackError (){
        StudentDO studentDO =new StudentDO();
        studentDO.setStudentUuid(UuidUtil.generateUuidNoDash())
                .setId("11564564564")
                .setName("ZhangSan1314")
                .setGender(true)
                .setGradeUuid(gradeDAO.lambdaQuery().list().get(0).getGradeUuid())
                .setDepartment(getDepartmentByName().getDepartmentUuid())
                .setMajor(getMajorByName().getMajorUuid())
                .setClazz(administrativeClassDAO.lambdaQuery().list().get(0).getAdministrativeClassUuid())
                .setGraduated(false);
        studentDAO.saveStudentBackError(studentDO,1);
        StudentDO studentDO1 = studentDAO.lambdaQuery()
                .eq(StudentDO::getStudentUuid, studentDO.getStudentUuid()).one();
        Assertions.assertNotNull(studentDO1);
        studentDAO.deleteStudent(studentDO);
    }

    @Test
    void testSaveStudentBackErrorWithError (){
        StudentDO studentDO =new StudentDO();
        studentDO.setStudentUuid(UuidUtil.generateUuidNoDash())
                .setId("1")
                .setName("ZhangSan1314")
                .setGender(true)
                .setGradeUuid(gradeDAO.lambdaQuery().list().get(0).getGradeUuid())
                .setDepartment(getDepartmentByName().getDepartmentUuid())
                .setMajor(getMajorByName().getMajorUuid())
                .setClazz(administrativeClassDAO.lambdaQuery().list().get(0).getAdministrativeClassUuid())
                .setGraduated(false);
        Assertions.assertThrows(BusinessException.class,()->studentDAO.saveStudentBackError(studentDO,1));
        studentDO.setStudentUuid(UuidUtil.generateUuidNoDash())
                .setId("123456789")
                .setName(null)
                .setGender(true)
                .setGradeUuid(gradeDAO.lambdaQuery().list().get(0).getGradeUuid())
                .setDepartment(getDepartmentByName().getDepartmentUuid())
                .setMajor(getMajorByName().getMajorUuid())
                .setClazz(administrativeClassDAO.lambdaQuery().list().get(0).getAdministrativeClassUuid())
                .setGraduated(false);
        Assertions.assertThrows(BusinessException.class,()->studentDAO.saveStudentBackError(studentDO,1));
        studentDO.setStudentUuid(UuidUtil.generateUuidNoDash())
                .setId("123456789")
                .setName("ZhangSan1314")
                .setGender(true)
                .setGradeUuid(UuidUtil.generateUuidNoDash())
                .setDepartment(getDepartmentByName().getDepartmentUuid())
                .setMajor(getMajorByName().getMajorUuid())
                .setClazz(administrativeClassDAO.lambdaQuery().list().get(0).getAdministrativeClassUuid())
                .setGraduated(false);
        Assertions.assertThrows(BusinessException.class,()->studentDAO.saveStudentBackError(studentDO,1));
        studentDO.setStudentUuid(UuidUtil.generateUuidNoDash())
                .setId("123243232")
                .setName("ZhangSan1314")
                .setGender(true)
                .setGradeUuid(gradeDAO.lambdaQuery().list().get(0).getGradeUuid())
                .setDepartment(UuidUtil.generateUuidNoDash())
                .setMajor(getMajorByName().getMajorUuid())
                .setClazz(administrativeClassDAO.lambdaQuery().list().get(0).getAdministrativeClassUuid())
                .setGraduated(false);
        Assertions.assertThrows(BusinessException.class,()->studentDAO.saveStudentBackError(studentDO,1));
        studentDO.setStudentUuid(UuidUtil.generateUuidNoDash())
                .setId("12123312312321")
                .setName("ZhangSan1314")
                .setGender(true)
                .setGradeUuid(gradeDAO.lambdaQuery().list().get(0).getGradeUuid())
                .setDepartment(getDepartmentByName().getDepartmentUuid())
                .setMajor(UuidUtil.generateUuidNoDash())
                .setClazz(administrativeClassDAO.lambdaQuery().list().get(0).getAdministrativeClassUuid())
                .setGraduated(false);
        Assertions.assertThrows(BusinessException.class,()->studentDAO.saveStudentBackError(studentDO,1));
        studentDO.setStudentUuid(UuidUtil.generateUuidNoDash())
                .setId("1312321321321")
                .setName("ZhangSan1314")
                .setGender(true)
                .setGradeUuid(gradeDAO.lambdaQuery().list().get(0).getGradeUuid())
                .setDepartment(getDepartmentByName().getDepartmentUuid())
                .setMajor(getMajorByName().getMajorUuid())
                .setClazz(UuidUtil.generateUuidNoDash())
                .setGraduated(false);
        Assertions.assertThrows(BusinessException.class,()->studentDAO.saveStudentBackError(studentDO,1));
    }


    @Test
    void testSaveStudentIgnoreError (){
        StudentDO studentDO =new StudentDO();
        studentDO.setStudentUuid(UuidUtil.generateUuidNoDash())
                .setId("12312312312312")
                .setName("ZhangSan1314")
                .setGender(true)
                .setGradeUuid(gradeDAO.lambdaQuery().list().get(0).getGradeUuid())
                .setDepartment(getDepartmentByName().getDepartmentUuid())
                .setMajor(getMajorByName().getMajorUuid())
                .setClazz(administrativeClassDAO.lambdaQuery().list().get(0).getAdministrativeClassUuid())
                .setGraduated(false);
        List<BackAddStudentDTO.FailedDetail>  failedDetailList = studentDAO.saveStudentIgnoreError(studentDO,1);
        StudentDO studentDO1 = studentDAO.lambdaQuery()
                .eq(StudentDO::getStudentUuid, studentDO.getStudentUuid()).one();
        Assertions.assertTrue(failedDetailList.isEmpty());
        Assertions.assertNotNull(studentDO1);
        studentDAO.deleteStudent(studentDO);
    }
    @Test
    void testSaveStudentIgnoreErrorWithFail (){
        StudentDO studentDO =new StudentDO();
        studentDO.setStudentUuid(UuidUtil.generateUuidNoDash())
                .setId("1")
                .setName("ZhangSan1314")
                .setGender(true)
                .setGradeUuid(gradeDAO.lambdaQuery().list().get(0).getGradeUuid())
                .setDepartment(getDepartmentByName().getDepartmentUuid())
                .setMajor(getMajorByName().getMajorUuid())
                .setClazz(administrativeClassDAO.lambdaQuery().list().get(0).getAdministrativeClassUuid())
                .setGraduated(false);
        List<BackAddStudentDTO.FailedDetail>  failedDetailList = studentDAO.saveStudentIgnoreError(studentDO,1);
        StudentDO studentDO1 = studentDAO.lambdaQuery()
                .eq(StudentDO::getStudentUuid, studentDO.getStudentUuid()).one();
        Assertions.assertFalse(failedDetailList.isEmpty());
        Assertions.assertNull(studentDO1);
    }
}