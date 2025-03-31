/*
 * --------------------------------------------------------------------------------
 * Copyright (c) 2022-NOW(至今) 锋楪技术团队
 * Author: 锋楪技术团队 (https://www.frontleaves.com)
 *
 * 本文件包含锋楪技术团队项目的源代码，项目的所有源代码均遵循 MIT 开源许可证协议。
 * --------------------------------------------------------------------------------
 * 许可证声明：
 *
 * 版权所有 (c) 2022-2025 锋楪技术团队。保留所有权利。
 *
 * 本软件是“按原样”提供的，没有任何形式的明示或暗示的保证，包括但不限于
 * 对适销性、特定用途的适用性和非侵权性的暗示保证。在任何情况下，
 * 作者或版权持有人均不承担因软件或软件的使用或其他交易而产生的、
 * 由此引起的或以任何方式与此软件有关的任何索赔、损害或其他责任。
 *
 * 使用本软件即表示您了解此声明并同意其条款。
 *
 * 有关 MIT 许可证的更多信息，请查看项目根目录下的 LICENSE 文件或访问：
 * https://opensource.org/licenses/MIT
 * --------------------------------------------------------------------------------
 * 免责声明：
 *
 * 使用本软件的风险由用户自担。作者或版权持有人在法律允许的最大范围内，
 * 对因使用本软件内容而导致的任何直接或间接的损失不承担任何责任。
 * --------------------------------------------------------------------------------
 */

package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.LogConstant;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.StudentMapper;
import com.frontleaves.scheduling.models.dto.BackAddStudentDTO;
import com.frontleaves.scheduling.models.entity.AdministrativeClassDO;
import com.frontleaves.scheduling.models.entity.StudentDO;
import com.frontleaves.scheduling.models.entity.multiple.UserAndStudentDO;
import com.frontleaves.scheduling.models.vo.StudentVO;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 学生数据访问对象类
 * <p>
 * 该类继承自ServiceImpl，专门用于实现对学生表(StudentDO)的数据访问操作。
 * 利用MyBatis-Plus框架简化了数据操作，实现了IService接口以提供更便捷的数据处理方法。
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @see StudentMapper
 * @see StudentDO
 * @since v1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class StudentDAO extends ServiceImpl<StudentMapper, StudentDO> implements IService<StudentDO> {
    private final RedissonClient redisson;
    private final AdministrativeClassDAO administrativeClassDAO;

    /**
     * 根据学生ID获取学生信息
     * <p>
     * 该方法首先尝试从Redis缓存中获取学生信息。如果缓存中不存在，则从数据库查询，并将查询结果存储到Redis缓存中，设置过期时间为24小时。
     * 如果在数据库中也未找到对应的学生信息，则返回null。
     *
     * @param id 学生的唯一标识符 {@code String}
     * @return 返回与给定ID匹配的学生信息 {@code StudentDO}，如果没有找到则返回null
     */
    @Nullable
    public StudentDO getStudentById(String id) {
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            RBucket<String> tryGetStudentId = transaction.getBucket(StringConstant.Redis.STUDENT_ID + id);
            if (!tryGetStudentId.isExists()) {
                StudentDO studentDO = this.lambdaQuery().eq(StudentDO::getId, id).one();
                if (studentDO != null) {
                    tryGetStudentId.set(studentDO.getStudentUuid());
                    tryGetStudentId.expire(Duration.ofSeconds(86400));
                    RMap<String, String> studentMap = transaction.getMap(
                            StringConstant.Redis.STUDENT_UUID + studentDO.getStudentUuid());
                    studentMap.putAll(ConvertUtil.convertObjectToMapString(studentDO));
                    studentMap.expire(Duration.ofSeconds(86400));
                    transaction.commit();
                    return studentDO;
                }
            } else {
                return this.getStudentByUuid(tryGetStudentId.get());
            }
            return null;
        } catch (Exception e) {
            transaction.rollback();
            log.error(LogConstant.DAO + "根据学生ID获取学生信息失败", e);
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 通过UUID获取学生信息
     * <p>该方法首先尝试从Redis缓存中根据给定的UUID查找学生信息。如果在Redis中找不到，则会尝试从数据库中查询。
     * 如果从数据库中成功查找到学生信息，会将该信息存入Redis，并设置过期时间为一天（86400秒），然后返回该学生信息。
     * 如果既在Redis中也未在数据库中找到学生信息，则返回null。
     *
     * @param studentUuid 学生的唯一标识符 {@code String}
     * @return 返回与给定UUID对应的学生信息对象 {@code StudentDO}，若未找到则返回null
     */
    @Nullable
    public StudentDO getStudentByUuid(String studentUuid) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.STUDENT_UUID + studentUuid);
        if (!map.isExists()) {
            StudentDO studentDO = this.getById(studentUuid);
            if (studentDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(studentDO));
                map.expire(Duration.ofSeconds(86400));
                return studentDO;
            }
        } else {
            return BeanUtil.toBean(map, StudentDO.class);
        }
        return null;
    }

    /**
     * 更新学生信息中的用户 UUID
     * <p>
     * 该方法用于根据给定的学生 ID 更新其对应的用户 UUID。更新操作包括在 Redis 中删除与旧 UUID 相关的键，并在数据库中更新新的 UUID。
     * 如果找不到对应的学生信息，将抛出业务异常。如果在执行过程中发生任何其他错误，将抛出服务器内部错误异常。
     *
     * @param userUuid  新的用户 UUID
     * @param studentId 学生 ID
     * @throws ServerInternalErrorException 如果在更新过程中发生服务器内部错误
     * @throws BusinessException            如果未找到对应的学生信息
     */
    public void updateUserUuid(String userUuid, String studentId) throws BusinessException, ServerInternalErrorException {
        StudentDO studentDO = this.getStudentById(studentId);
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            if (studentDO != null) {
                transaction.getBucket(StringConstant.Redis.STUDENT_ID + studentDO.getId()).delete();
                transaction.getMap(StringConstant.Redis.STUDENT_UUID + studentDO.getStudentUuid()).delete();
                this.lambdaUpdate()
                        .eq(StudentDO::getStudentUuid, studentDO.getStudentUuid())
                        .set(StudentDO::getUserUuid, userUuid)
                        .update();
                transaction.commit();
            } else {
                transaction.rollback();
                throw new BusinessException("未找到对应的教师信息", ErrorCode.NOT_EXIST);
            }
        } catch (Exception e) {
            transaction.rollback();
            log.error(LogConstant.DAO + "更新学生信息中的用户 UUID 失败", e);
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 删除学生并删除token
     * <p>
     * 该方法用于删除学生信息，首先通过学生 UUID 获取学生信息，然后删除学生信息。
     * 如果学生信息存在，则删除 Redis 中与学生相关的所有数据。
     * 如果学生信息不存在或者删除失败，则抛出 {@code ServerInternalErrorException} 异常。
     * </p>
     *
     * @param studentDO 学生实体
     * @throws ServerInternalErrorException 如果删除过程中发生服务器内部错误
     */
    public void deleteStudent(StudentDO studentDO) throws ServerInternalErrorException {
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            this.lambdaUpdate().eq(StudentDO::getStudentUuid, studentDO.getStudentUuid()).remove();
            transaction.getMap(StringConstant.Redis.STUDENT_UUID + studentDO.getStudentUuid()).delete();
            transaction.getBucket(StringConstant.Redis.STUDENT_ID + studentDO.getId()).delete();
            transaction.getBucket(StringConstant.Redis.STUDENT_USER_UUID + studentDO.getUserUuid()).delete();
            transaction.commit();
        } catch (Exception e) {
            log.debug("删除学生信息失败", e);
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 通过用户 UUID 获取学生信息
     * <p>
     * 该方法根据提供的用户 UUID 从系统中检索对应的学生信息。首先尝试从缓存（如 Redis）中获取数据，如果缓存中没有找到，则从数据库中查询，并将结果存入缓存以提高后续访问速度。
     * 如果在缓存和数据库中均未找到与给定 UUID 对应的学生信息，则返回 null。
     * </p>
     *
     * @param userUuid 用户的唯一标识符
     * @return 返回与指定 UUID 关联的学生信息，若无匹配项则返回 {@code null}
     * @throws ServerInternalErrorException 当执行数据库查询或缓存操作时遇到错误
     */
    public StudentDO getStudentByUserUuid(String userUuid) {
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            RBucket<String> tryGetStudentByUserUuid = transaction.getBucket(
                    StringConstant.Redis.STUDENT_USER_UUID + userUuid);
            if (!tryGetStudentByUserUuid.isExists()) {
                StudentDO studentDO = this.lambdaQuery().eq(StudentDO::getUserUuid, userUuid).one();
                if (studentDO != null) {
                    tryGetStudentByUserUuid.set(studentDO.getStudentUuid());
                    tryGetStudentByUserUuid.expire(Duration.ofSeconds(86400));
                    RMap<String, String> studentMap = transaction.getMap(
                            StringConstant.Redis.STUDENT_UUID + studentDO.getStudentUuid());
                    studentMap.putAll(ConvertUtil.convertObjectToMapString(studentDO));
                    studentMap.expire(Duration.ofSeconds(86400));
                    transaction.commit();
                    return studentDO;
                }
            } else {
                return this.getStudentByUuid(tryGetStudentByUserUuid.get());
            }
            transaction.rollback();
            return null;
        } catch (Exception e) {
            transaction.rollback();
            log.error(LogConstant.DAO + "通过用户 UUID 获取学生信息失败", e);
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 根据专业唯一标识获取学生列表
     * <p>
     * 此方法通过专业唯一标识（majorUuid）查询数据库，获取所有该专业的学生列表
     * 如果没有找到任何学生，即返回一个空列表，以避免返回null值导致的空指针异常
     * </p>
     *
     * @param majorUuid 专业唯一标识，用于查询学生记录
     * @return 包含StudentDO对象的列表，表示所有该专业的学生如果没有找到学生，则返回空列表
     */
    @NotNull
    public List<StudentDO> getStudentByMajorUuid(String majorUuid) {
        List<StudentDO> getList = this.lambdaQuery().eq(StudentDO::getMajor, majorUuid).list();
        if (getList == null || getList.isEmpty()) {
            return List.of();
        }
        return getList;
    }

    /**
     * 保存学生信息
     *
     * @param studentDO 学生实体
     * @param i 行号索引
     */
    public void saveStudentBackError(StudentDO studentDO, int i) {
        try {
            this.save(studentDO);
        } catch (DuplicateKeyException e) {
            // 学号重复异常
            log.error("学生学号重复", e);
            throw new BusinessException("第" + (i + 3) + "行学生学号重复，请检查", ErrorCode.BODY_ERROR);
        } catch (DataIntegrityViolationException e) {
            // 分析数据完整性异常的具体原因
            String errorMessage = e.getMessage();
            String detailedReason = analyzeDataIntegrityBuError(errorMessage);
            throw new BusinessException("第" + (i + 3) + "行" + detailedReason, ErrorCode.BODY_ERROR);
        } catch (Exception e) {
            // 其他未预期的异常
            throw new BusinessException("第" + (i + 3) + "行保存失败：" + e.getMessage(), ErrorCode.BODY_ERROR);
        }
    }
    /**
     * 分析数据完整性错误的详细原因
     *
     * @param errorMessage 错误信息
     * @return 具体的错误原因
     */
    private String analyzeDataIntegrityBuError(String errorMessage) {
        // 外键错误映射
        Map<String, String> foreignKeyErrors = Map.of(
                "fk_cs_student_cs_grade", "年级信息错误",
                "fk_cs_student_cs_department", "学院信息错误",
                "fk_cs_student_cs_major", "专业信息错误",
                "fk_cs_administrative_class_cs_student", "班级信息错误"
        );
        // 检查外键错误
        for (Map.Entry<String, String> entry : foreignKeyErrors.entrySet()) {
            if (errorMessage.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        // 长度错误检查
        if (errorMessage.contains("Data too long")) {
            if (errorMessage.contains("id")) {
                return "学号长度超出限制，最大32个字符";
            }
            if (errorMessage.contains("name")) {
                return "姓名长度超出限制，最大32个字符";
            }
            return "数据长度超出限制";
        }
        // 默认错误信息
        return "数据错误：可能包含错误的值（意料之外的报错）";
    }

    /**
     * 保存学生信息，忽略错误并返回失败详情
     *
     * @param studentDO 学生实体
     * @param i         当前处理的行索引
     * @return 失败详情列表，如果成功则返回空列表
     */
    public List<BackAddStudentDTO.FailedDetail> saveStudentIgnoreError(StudentDO studentDO, int i) {
        try {
            this.save(studentDO);
            // 成功时返回空列表
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.singletonList(createFailedDetail(e, i));
        }
    }

    /**
     * 根据异常创建失败详情
     */
    private BackAddStudentDTO.FailedDetail createFailedDetail(Exception e, int i) {
        BackAddStudentDTO.FailedDetail failedDetail = new BackAddStudentDTO.FailedDetail();
        failedDetail.setRow(i + 3);

        if (e instanceof DuplicateKeyException) {
            failedDetail.setReason("学号重复");
        } else if (e instanceof DataIntegrityViolationException) {
            String errorMessage = e.getMessage();
            failedDetail.setReason(analyzeDataIntegrityError(errorMessage));
        } else {
            failedDetail.setReason("保存失败：" + e.getMessage());
        }

        return failedDetail;
    }

    /**
     * 分析数据完整性错误
     */
    private String analyzeDataIntegrityError(String errorMessage) {
        // 外键错误映射
        Map<String, String> foreignKeyErrors = Map.of(
                "fk_cs_student_cs_grade", "年级信息错误",
                "fk_cs_student_cs_department", "学院信息错误",
                "fk_cs_student_cs_major", "专业信息错误",
                "fk_cs_administrative_class_cs_student", "班级信息错误"
        );
        // 检查外键错误
        String foreignKeyMatch = foreignKeyErrors.entrySet().stream()
                .filter(entry -> errorMessage.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
        if (foreignKeyMatch != null) {
            return foreignKeyMatch;
        }
        // 长度错误检查
        if (errorMessage.contains("Data too long")) {
            Map<String, String> lengthErrors = Map.of(
                    "id", "学号长度超出限制，最大32个字符",
                    "name", "姓名长度超出限制，最大32个字符"
            );
            return lengthErrors.entrySet().stream()
                    .filter(entry -> errorMessage.contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse("数据长度超出限制");
        }
        // 默认错误信息
        return "数据错误：可能包含空值、超出长度限制或不符合外键约束";
    }

    /**
     * 列出学生列表
     *
     * @param page        当前页码
     * @param size        每页大小
     * @param isDesc      是否降序排序
     * @param clazz       班级名称，可为空
     * @param isGraduated 是否毕业，可为空
     * @param name        学生姓名，可为空
     * @param id          学生学号，可为空
     * @param status      学生状态，可为空
     * @return 返回包含学生信息的页面对象
     */
    public @Nullable Page<StudentDO> listStudents(int page, int size, Boolean isDesc,
                                                  @Nullable String clazz, @Nullable Boolean isGraduated,
                                                  @Nullable String name, @Nullable String id, @Nullable Byte status
    ) {
        // 构建唯一缓存 key 并获取缓存数据
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.STUDENT_LIST
                + page + ":" + size + ":" + isDesc + ":" + clazz + ":" + isGraduated + ":" + name + ":" + id + ":" + status);

        // 若缓存中不存在数据,则执行数据库查询,并缓存结果
        if (!map.isExists()) {
            LambdaQueryChainWrapper<StudentDO> queryWrapper = this.lambdaQuery();
            // 查看班级是否为空
            if (CharSequenceUtil.isNotBlank(clazz)) {
                queryWrapper.eq(StudentDO::getClazz, clazz);
            }
            // 查看学生是否毕业
            if (isGraduated != null) {
                queryWrapper.eq(StudentDO::getGraduated, isGraduated);
            }
            // 查看姓名是否为空
            if (CharSequenceUtil.isNotBlank(name)) {
                queryWrapper.like(StudentDO::getName, name);
            }
            // 查看学号是否为空
            if (CharSequenceUtil.isNotBlank(id)) {
                queryWrapper.like(StudentDO::getId, id);
            }
            // 根据 isDesc 进行排序
            if (Boolean.TRUE.equals(isDesc)) {
                queryWrapper.orderByDesc(StudentDO::getCreatedAt);
            } else {
                queryWrapper.orderByAsc(StudentDO::getCreatedAt);
            }

            // 调用 ProjectUtil 方法查询并缓存数据
            return ProjectUtil.queryAndCache(queryWrapper, page, size, map);
        } else {
            return ProjectUtil.convertMapToPage(map, StudentDO.class);
        }
    }

    /**
     * 编辑学生信息
     *
     * @param studentUuid 学生的唯一标识符
     * @param studentVO 包含学生新信息的视图对象
     * @return 更新后的学生数据对象
     * @throws BusinessException 当学生信息不存在时抛出的业务异常
     * @throws ServerInternalErrorException 当学生信息更新失败时抛出的服务器内部错误异常
     */
    public StudentDO editStudent(String studentUuid, StudentVO studentVO) {
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            // 先查询学生,如果不存在则抛出异常
            StudentDO studentDO = this.getStudentByUuid(studentUuid);
            if (studentDO == null) {
                throw new BusinessException("未找到该学生信息", ErrorCode.NOT_EXIST);
            }

            // 通过班级 UUID 获取班级映射信息
            AdministrativeClassDO classMapping = administrativeClassDAO.getAdministrativeClassMappingByClazz(studentVO.getClazz());

            // 复制非空属性到 studentDO
            BeanUtil.copyProperties(studentVO, studentDO, CopyOptions.create().ignoreNullValue());

            // 设置外键字段
            studentDO.setGradeUuid(classMapping.getGradeUuid())
                    .setDepartment(classMapping.getDepartmentUuid())
                    .setMajor(classMapping.getMajorUuid());

            // 更新数据库
            boolean success = this.updateById(studentDO);
            if (!success) {
                transaction.rollback();
                throw new ServerInternalErrorException("学生信息更新失败");
            }

            // 更新缓存
            RMap<String, String> studentMap = transaction.getMap(StringConstant.Redis.STUDENT_UUID + studentUuid);
            studentMap.putAll(ConvertUtil.convertObjectToMapString(studentDO));
            studentMap.expire(Duration.ofSeconds(86400));

            transaction.commit();
            return studentDO;
        } catch (Exception e) {
            transaction.rollback();
            log.error(LogConstant.DAO + "编辑学生信息失败", e);
            throw new ServerInternalErrorException("学生信息更新失败");
        }
    }


    /**
     * 获取未在用户系统中注册的学生列表
     *
     * @param page        页码
     * @param size        每页大小
     * @param isDesc      是否降序查询
     * @param clazz       学生班级，可为空
     * @param isGraduated 是否毕业，可为空
     * @param name        学生姓名，可为空
     * @param id          学生ID，可为空
     * @param status      学生状态，可为空
     * @return 返回一个包含UserAndStudentDO对象的列表，每个对象代表一个学生及其相关信息
     */
    public List<StudentDO> getStudentNoRegisterUserList(int page, int size, Boolean isDesc,
                                                        @Nullable String clazz, @Nullable Boolean isGraduated,
                                                        @Nullable String name, @Nullable String id, @Nullable Byte status) {
        int offset = (page - 1) * size;
        if (Boolean.TRUE.equals(isDesc)) {
            return this.baseMapper.getStudentNoRegisterUserQueryDesc(clazz, status, name, offset, size, isGraduated, id);
        } else {
            return this.baseMapper.getStudentNoRegisterUserQueryAsc(clazz, status, name, offset, size, isGraduated, id);
        }
    }

    /**
     * 根据条件获取学生列表，包括用户信息
     *
     * @param page 页码
     * @param size 每页大小
     * @param isDesc 是否降序排序的标志
     * @param clazz 班级名称，可为空
     * @param isGraduated 是否毕业的标志，可为空
     * @param name 学生姓名，可为空
     * @param id 学生ID，可为空
     * @param status 学生状态，可为空
     * @return 返回一个包含用户和学生信息的列表
     */
    public List<UserAndStudentDO> getStudentListWithUser(int page, int size, Boolean isDesc,
                                                         @Nullable String clazz, @Nullable Boolean isGraduated,
                                                         @Nullable String name, @Nullable String id, @Nullable String status) {
        int offset = (page - 1) * size;

        Byte parsedStatus = (status != null && !status.isBlank()) ? Byte.parseByte(status) : null;

        if (Boolean.TRUE.equals(isDesc)) {
            return this.baseMapper.getStudentAndUserQueryDesc(clazz, parsedStatus, name, offset, size, isGraduated, id);
        } else {
            return this.baseMapper.getStudentAndUserQueryAsc(clazz, parsedStatus, name, offset, size, isGraduated, id);
        }
    }

    /**
     * 根据用户UUID获取用户状态
     *
     * @param userUuid 用户的唯一标识符（UUID）
     * @return 用户的状态，以Byte形式返回
     */
    public Byte getUserStatusByUuid(String userUuid) {
        return this.baseMapper.getUserStatusByUuid(userUuid);
    }
}

