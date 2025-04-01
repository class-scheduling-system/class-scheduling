package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.ClassAssignmentDAO;
import com.frontleaves.scheduling.daos.CourseLibraryDAO;
import com.frontleaves.scheduling.daos.SemesterDAO;
import com.frontleaves.scheduling.daos.TeacherDAO;
import com.frontleaves.scheduling.models.dto.ClassAssignmentDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.scheduling.ScheduleResultDTO;
import com.frontleaves.scheduling.models.entity.ClassAssignmentDO;
import com.frontleaves.scheduling.models.entity.CourseLibraryDO;
import com.frontleaves.scheduling.models.entity.SemesterDO;
import com.frontleaves.scheduling.models.entity.TeacherDO;
import com.frontleaves.scheduling.models.vo.ClassAssignmentVO;
import com.frontleaves.scheduling.services.AdministrativeClassService;
import com.frontleaves.scheduling.services.ClassAssignmentService;
import com.frontleaves.scheduling.utils.ProjectOption;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 排课分配逻辑实现类
 * <p>
 * 该类用于实现排课分配相关的业务逻辑，包括添加、删除、更新和查询排课分配信息等功能。
 * 该类实现了排课分配服务接口，用于处理排课分配相关的业务逻辑。
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassAssignmentLogic implements ClassAssignmentService {

    private final ClassAssignmentDAO classAssignmentDAO;
    private final SemesterDAO semesterDAO;
    private final CourseLibraryDAO courseLibraryDAO;
    private final TeacherDAO teacherDAO;
    private final AdministrativeClassService administrativeClassService;

    @Override
    public void add(ClassAssignmentVO vo) {
        // 验证学期是否存在
        SemesterDO semester = semesterDAO.getSemesterByUuid(vo.getSemesterUuid());
        if (semester == null) {
            throw new BusinessException("学期不存在", ErrorCode.NOT_EXIST);
        }

        // 验证课程是否存在
        CourseLibraryDO course = courseLibraryDAO.getCourseByUuid(vo.getCourseUuid());
        if (course == null) {
            throw new BusinessException("课程不存在", ErrorCode.NOT_EXIST);
        }

        // 验证教师是否存在
        TeacherDO teacher = teacherDAO.getTeacherByUuid(vo.getTeacherUuid());
        if (teacher == null) {
            throw new BusinessException("教师不存在", ErrorCode.NOT_EXIST);
        }

        // 创建实体对象并保存
        ClassAssignmentDO entity = new ClassAssignmentDO();
        BeanUtil.copyProperties(vo, entity, ProjectOption.stringBlankToNull());
        classAssignmentDAO.save(entity);
    }

    @Override
    public void delete(String classAssignmentUuid) {
        // 验证排课分配是否存在
        ClassAssignmentDO entity = classAssignmentDAO.getClassAssignmentByUuid(classAssignmentUuid);
        if (entity == null) {
            throw new BusinessException(StringConstant.ErrorMessage.CLASS_ASSIGNMENT_NOT_FOUND, ErrorCode.NOT_EXIST);
        }

        // 删除排课分配
        classAssignmentDAO.removeClassAssignment(classAssignmentUuid);
    }

    @Override
    public void update(String classAssignmentUuid, ClassAssignmentVO vo) {
        // 验证排课分配是否存在
        ClassAssignmentDO existingEntity = classAssignmentDAO.getClassAssignmentByUuid(classAssignmentUuid);
        if (existingEntity == null) {
            throw new BusinessException(StringConstant.ErrorMessage.CLASS_ASSIGNMENT_NOT_FOUND, ErrorCode.NOT_EXIST);
        }

        // 验证学期是否存在
        if (vo.getSemesterUuid() != null) {
            SemesterDO semester = semesterDAO.getSemesterByUuid(vo.getSemesterUuid());
            if (semester == null) {
                throw new BusinessException("学期不存在", ErrorCode.NOT_EXIST);
            }
        }

        // 验证课程是否存在
        if (vo.getCourseUuid() != null) {
            CourseLibraryDO course = courseLibraryDAO.getCourseByUuid(vo.getCourseUuid());
            if (course == null) {
                throw new BusinessException("课程不存在", ErrorCode.NOT_EXIST);
            }
        }

        // 验证教师是否存在
        if (vo.getTeacherUuid() != null) {
            TeacherDO teacher = teacherDAO.getTeacherByUuid(vo.getTeacherUuid());
            if (teacher == null) {
                throw new BusinessException("教师不存在", ErrorCode.NOT_EXIST);
            }
        }

        // 更新实体对象
        BeanUtil.copyProperties(vo, existingEntity, ProjectOption.stringBlankToNull());
        classAssignmentDAO.updateClassAssignment(existingEntity);
    }

    @Override
    public ClassAssignmentDTO getById(String classAssignmentUuid) {
        // 验证排课分配是否存在
        ClassAssignmentDO entity = classAssignmentDAO.getClassAssignmentByUuid(classAssignmentUuid);
        if (entity == null) {
            throw new BusinessException(StringConstant.ErrorMessage.CLASS_ASSIGNMENT_NOT_FOUND, ErrorCode.NOT_EXIST);
        }

        // 转换为 DTO 并返回
        return BeanUtil.toBean(entity, ClassAssignmentDTO.class);
    }

    @Override
    public PageDTO<ClassAssignmentDTO> page(Integer page, Integer size, String semesterUuid, String courseUuid, String teacherUuid) {
        // 验证 UUID 格式（如果提供）
        if (semesterUuid != null && !semesterUuid.isBlank() &&
                !semesterUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException(StringConstant.ErrorMessage.SEMESTER_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR);
        }
        if (courseUuid != null && !courseUuid.isBlank() &&
                !courseUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException(StringConstant.ErrorMessage.COURSE_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR);
        }
        if (teacherUuid != null && !teacherUuid.isBlank() &&
                !teacherUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException(StringConstant.ErrorMessage.TEACHER_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR);
        }

        // 获取分页数据
        Page<ClassAssignmentDO> pageResult = classAssignmentDAO.getClassAssignmentPage(page, size, semesterUuid, courseUuid, teacherUuid);
        if (pageResult == null || !pageResult.hasNext()) {
            throw new BusinessException(StringConstant.ErrorMessage.CLASS_ASSIGNMENT_NOT_FOUND, ErrorCode.NOT_EXIST);
        }

        // 转换为 DTO
        return ProjectUtil.convertPageToPageDTO(pageResult, ClassAssignmentDTO.class);
    }

    @Override
    public List<ClassAssignmentDTO> list(String semesterUuid, String courseUuid, String teacherUuid) {
        // 验证 UUID 格式（如果提供）
        if (semesterUuid != null && !semesterUuid.isBlank() &&
                !semesterUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException(StringConstant.ErrorMessage.SEMESTER_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR);
        }
        if (courseUuid != null && !courseUuid.isBlank() &&
                !courseUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException(StringConstant.ErrorMessage.COURSE_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR);
        }
        if (teacherUuid != null && !teacherUuid.isBlank() &&
                !teacherUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException(StringConstant.ErrorMessage.TEACHER_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR);
        }

        // 获取列表数据
        return Optional.ofNullable(classAssignmentDAO.list(semesterUuid, courseUuid, teacherUuid))
                .map(list -> list.stream()
                        .map(entity -> BeanUtil.toBean(entity, ClassAssignmentDTO.class))
                        .toList())
                .orElse(List.of());
    }

    @Override
    public void saveClassAssignment(@NotNull ScheduleResultDTO result) {
        // 获取排课结果
        List<ScheduleResultDTO.ClassAssignmentDTO> classAssignments = result.getAssignments();
        if (classAssignments == null || classAssignments.isEmpty()) {
            throw new BusinessException("排课结果为空", ErrorCode.PARAMETER_ERROR);
        }

        // 遍历排课结果，保存到数据库
        for (ScheduleResultDTO.ClassAssignmentDTO assignment : classAssignments) {
            ClassAssignmentDO assignmentDO = new ClassAssignmentDO();
            // 查询班级（如果为Uuid格式则需查询为名字）
            List<String> className = administrativeClassService.getClassNameByGroup(assignment.getClassGroup());
            //交换数据
            assignmentDO.setSemesterUuid(result.getSemesterId())
                    .setCourseUuid(assignment.getCourse().getCourseLibraryUuid())
                    .setTeacherUuid(assignment.getTeacher().getTeacher().getTeacherUuid())
                    .setClassroomUuid(assignment.getClassroom().getClassroom().getClassroomUuid())
                    .setTeachingClassComposition(JSONUtil.toJsonStr(className));
        }

    }
}
