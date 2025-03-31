package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.constants.SystemConstant;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.dto.base.TokenDTO;
import com.frontleaves.scheduling.models.entity.*;
import com.frontleaves.scheduling.models.vo.AutomaticClassSchedulingVO;
import com.frontleaves.scheduling.models.vo.SpecificCourseIdVO;
import com.frontleaves.scheduling.services.SchedulingService;
import enums.StrategyEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;
import java.util.List;

/**
 * 调度逻辑测试类
 *
 * @author FLASHLACK
 */
@Slf4j
@SpringBootTest
class SchedulingTest {

    @Resource
    private SchedulingService schedulingService;
    @Resource
    private SemesterDAO semesterDAO;
    @Resource
    private DepartmentDAO departmentDAO;
    @Resource
    private UserDAO userDAO;
    @Resource
    private CourseTypeDAO courseTypeDAO;
    @Resource
    private CourseLibraryDAO courseLibraryDAO;
    @Resource
    private ClassroomDAO classroomDAO;
    @Resource
    private AcademicAffairsPermissionDAO academicAffairsPermissionDAO;
    @Resource
    private TokenDAO tokenDAO;
    @Resource
    private TeacherCourseQualificationDAO teacherCourseQualificationDAO;

    private SemesterDO setUpSemester;
    private DepartmentDO setUpDepartment;
    private UserDO setUpUser;
    private CourseTypeDO setUpCourseType;
    private CourseLibraryDO setUpCourseLibrary;
    private ClassroomDO setUpClassroom;


    @BeforeEach
    void setUp() {
        log.debug("SchedulingLogic单元测试初始化");
        // 创建测试用户
        setUpUser = userDAO.lambdaQuery().eq(UserDO::getRoleUuid, SystemConstant.getRoleAcademic()).one();
        // 创建测试学期
        setUpSemester = semesterDAO.lambdaQuery().eq(SemesterDO::getIsEnabled, 1).list().get(0);
        // 创建教务权限
        AcademicAffairsPermissionDO setUpPermission = academicAffairsPermissionDAO.lambdaQuery()
                .eq(AcademicAffairsPermissionDO::getAuthorizedUser, setUpUser.getUserUuid()).one();
        //创建测试部门
        setUpDepartment = departmentDAO.lambdaQuery()
                .eq(DepartmentDO::getDepartmentUuid, setUpPermission.getDepartment()).one();
        // 创建测试课程类型
        setUpCourseType = courseTypeDAO.lambdaQuery().list().get(0);
        //获取有老师分配的课程库
        List<TeacherCourseQualificationDO> teacherCourseQualificationDOList = teacherCourseQualificationDAO
                .lambdaQuery().list();
        for (TeacherCourseQualificationDO teacherCourseQualificationDO : teacherCourseQualificationDOList) {
            CourseLibraryDO courseLibraryDO = courseLibraryDAO.lambdaQuery()
                    .eq(CourseLibraryDO::getDepartment, setUpDepartment.getDepartmentUuid())
                    .eq(CourseLibraryDO::getCourseLibraryUuid, teacherCourseQualificationDO.getCourseUuid())
                    .one();
            if (courseLibraryDO != null) {
                setUpCourseLibrary = courseLibraryDO;
                break;
            }
        }
        // 创建测试教室
        setUpClassroom = classroomDAO.lambdaQuery().list().get(0);
    }

    @Test
    void testGetAutoClassSchedulingBaseDTO() {

        long now = System.currentTimeMillis();
        //准备数据,添加头节点
        MockHttpServletRequest request = new MockHttpServletRequest();
        TokenDTO tokenDTO = tokenDAO.createToken(setUpUser);
        request.addHeader("Authorization", "Bearer " + tokenDTO.getToken());
        // 构建AutomaticClassSchedulingVO对象
        AutomaticClassSchedulingVO.Constraints constraints = new AutomaticClassSchedulingVO.Constraints(
                true,
                true,
                true,
                true,
                true
        );
        AutomaticClassSchedulingVO.AlgorithmParams algorithmParams = new AutomaticClassSchedulingVO.AlgorithmParams(
                50,
                100,
                0.7,
                0.3
        );
        AutomaticClassSchedulingVO.PrioritySettings.CourseTypePriority courseTypePriority = new AutomaticClassSchedulingVO.PrioritySettings.CourseTypePriority(
                setUpCourseType.getCourseTypeUuid(),
                (short) 1
        );
        AutomaticClassSchedulingVO.PrioritySettings prioritySettings = new AutomaticClassSchedulingVO.PrioritySettings(
                List.of(courseTypePriority)
        );
        AutomaticClassSchedulingVO.TimePreferences.PreferredTimeSlot preferredTimeSlot = new AutomaticClassSchedulingVO.TimePreferences.PreferredTimeSlot(
                (short) 1,
                (short) 2,
                (short) 4
        );
        AutomaticClassSchedulingVO.TimePreferences timePreferences = new AutomaticClassSchedulingVO.TimePreferences(
                false,
                true,
                Collections.singletonList(preferredTimeSlot)
        );
        List<SpecificCourseIdVO> list = Collections.singletonList(new SpecificCourseIdVO(
                setUpCourseLibrary.getCourseLibraryUuid(),
                null,
                50
        ));
        AutomaticClassSchedulingVO.ScopeSettings scopeSettings = new AutomaticClassSchedulingVO.ScopeSettings(

                list,
                Collections.singletonList(setUpClassroom.getBuildingUuid())
        );
        AutomaticClassSchedulingVO vo = new AutomaticClassSchedulingVO(
                setUpSemester.getSemesterUuid(),
                setUpDepartment.getDepartmentUuid(),
                StrategyEnum.OPTIMAL,
                16,
                constraints,
                algorithmParams,
                prioritySettings,
                timePreferences,
                scopeSettings
        );
        schedulingService.getAutoClassSchedulingBaseDTO(vo, request);

        log.debug("SchedulingLogic单元测试完成，耗时：{}ms", System.currentTimeMillis() - now);
    }
}
