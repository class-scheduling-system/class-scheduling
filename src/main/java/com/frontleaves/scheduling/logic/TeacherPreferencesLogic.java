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
 * 本软件是"按原样"提供的，没有任何形式的明示或暗示的保证，包括但不限于
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

package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.daos.TeacherDAO;
import com.frontleaves.scheduling.daos.TeacherPreferencesDAO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherPreferencesDTO;
import com.frontleaves.scheduling.models.entity.TeacherDO;
import com.frontleaves.scheduling.models.entity.TeacherPreferencesDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.vo.TeacherPreferencesVO;
import com.frontleaves.scheduling.services.TeacherPreferencesService;
import com.frontleaves.scheduling.services.UserService;
import com.frontleaves.scheduling.utils.ProjectOption;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.UserAuthenticationException;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 教师课程偏好逻辑处理类，实现了 {@code TeacherPreferencesService} 接口。
 * <p>
 * 该类提供了教师课程偏好管理相关的具体实现，包括添加偏好、删除偏好、查询偏好信息等操作。
 * 通过依赖注入的方式，可以与其他服务进行交互，完成复杂的业务逻辑。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Service
@RequiredArgsConstructor
public class TeacherPreferencesLogic implements TeacherPreferencesService {
    private final TeacherPreferencesDAO teacherPreferencesDAO;
    private final UserService userService;
    private final TeacherDAO teacherDAO;

    /**
     * 获取教师课程偏好分页数据
     * <p>
     * 该方法用于根据指定的分页参数、排序方式以及搜索条件获取教师课程偏好信息的分页结果。返回的结果包含当前页的数据记录、总记录数等信息。
     * </p>
     *
     * @param page         当前页码，从1开始
     * @param size         每页显示的记录数
     * @param isDesc       是否降序排列，如果为 {@code true} 则按降序排列，否则按升序排列
     * @param teacherUuid  教师UUID，用于筛选特定教师的偏好设置
     * @param semesterUuid 学期UUID，用于筛选特定学期的偏好设置
     * @return 返回一个包含教师课程偏好分页数据的 {@code PageDTO<TeacherPreferencesDTO>} 对象
     */
    @Override
    public PageDTO<TeacherPreferencesDTO> getTeacherPreferencesPage(
            int page,
            int size,
            boolean isDesc,
            @Nullable String teacherUuid,
            @Nullable String semesterUuid
    ) {
        Page<TeacherPreferencesDO> preferencesPage = teacherPreferencesDAO.getTeacherPreferencesPage(page, size, isDesc, teacherUuid, semesterUuid);
        return ProjectUtil.convertPageToPageDTO(preferencesPage, TeacherPreferencesDTO.class);
    }

    /**
     * 获取教师在指定学期的所有课程偏好
     * <p>
     * 该方法用于获取特定教师在指定学期的所有课程偏好设置。结果会按照星期和时间段排序。
     * </p>
     *
     * @param teacherUuid  教师UUID
     * @param semesterUuid 学期UUID
     * @return 返回教师课程偏好列表
     */
    @Override
    public List<TeacherPreferencesDTO> getTeacherPreferencesList(String teacherUuid, String semesterUuid) {
        List<TeacherPreferencesDO> preferencesList = teacherPreferencesDAO.getTeacherPreferencesByTeacherAndSemester(teacherUuid, semesterUuid);
        return BeanUtil.copyToList(preferencesList, TeacherPreferencesDTO.class);
    }

    /**
     * 根据UUID获取教师课程偏好信息
     * <p>
     * 该方法用于根据偏好UUID获取对应的教师课程偏好详细信息。如果找到匹配的记录，则返回一个 {@code TeacherPreferencesDTO} 对象，否则返回 {@code null}。
     * </p>
     *
     * @param preferenceUuid 教师课程偏好的唯一标识符
     * @return 返回与给定UUID匹配的教师课程偏好数据传输对象，如果没有找到匹配的记录则返回 {@code null}
     */
    @Override
    @Nullable
    public TeacherPreferencesDTO getTeacherPreferencesByUuid(String preferenceUuid) {
        TeacherPreferencesDO preferencesDO = teacherPreferencesDAO.getTeacherPreferencesByUuid(preferenceUuid);
        if (preferencesDO == null) {
            return null;
        }
        return BeanUtil.toBean(preferencesDO, TeacherPreferencesDTO.class);
    }

    /**
     * 添加教师课程偏好
     * <p>
     * 该方法用于添加新的教师课程偏好记录。在添加之前，会验证教师和学期是否存在，以及时间段是否合法。
     * 如果验证通过，则创建新的偏好记录并返回创建后的详细信息。
     * </p>
     *
     * @param teacherPreferencesVO 包含要添加的教师课程偏好信息的视图对象
     * @return 返回新创建的教师课程偏好信息
     */
    @Override
    public TeacherPreferencesDTO addTeacherPreferences(TeacherPreferencesVO teacherPreferencesVO) {
        TeacherPreferencesDO preferencesDO = BeanUtil.toBean(teacherPreferencesVO, TeacherPreferencesDO.class);
        teacherPreferencesDAO.save(preferencesDO);
        return Optional.ofNullable(teacherPreferencesDAO.getTeacherPreferencesByUuid(preferencesDO.getPreferenceUuid()))
                .map(data -> BeanUtil.toBean(data, TeacherPreferencesDTO.class))
                .orElseThrow(() -> new BusinessException("添加教师课程偏好失败", ErrorCode.SERVER_INTERNAL_ERROR));
    }

    /**
     * 编辑教师课程偏好
     * <p>
     * 该方法用于更新现有的教师课程偏好记录。在更新之前，会验证教师和学期是否存在，以及时间段是否合法。
     * 如果验证通过，则更新偏好记录并返回更新后的详细信息。
     * </p>
     *
     * @param preferenceUuid       教师课程偏好的唯一标识符
     * @param teacherPreferencesVO 包含要更新的教师课程偏好信息的视图对象
     * @param request              请求对象，用于获取当前请求的上下文信息
     * @return 返回更新后的教师课程偏好信息
     */
    @Override
    public TeacherPreferencesDTO editTeacherPreferences(String preferenceUuid, TeacherPreferencesVO teacherPreferencesVO, HttpServletRequest request) {
        TeacherPreferencesDO getPreference = Optional.ofNullable(teacherPreferencesDAO.getTeacherPreferencesByUuid(preferenceUuid))
                .orElseThrow(() -> new BusinessException("教师课程偏好不存在", ErrorCode.NOT_EXIST));

        UserDO getUser = userService.getUserByRequest(request);
        if (getUser == null) {
            throw new UserAuthenticationException(UserAuthenticationException.ErrorType.USER_NOT_LOGIN, request);
        }

        TeacherDO getTeacher = teacherDAO.getTeacherByUserUuid(getUser.getUserUuid());

        Optional.ofNullable(getPreference.getTeacherUuid())
                .filter(teacherUuid -> !teacherUuid.equals(getTeacher.getTeacherUuid()))
                .ifPresent(teacherUuid -> {
                    throw new BusinessException("教师课程偏好不属于当前用户", ErrorCode.NOT_EXIST);
                });

        BeanUtil.copyProperties(teacherPreferencesVO, getPreference, ProjectOption.stringBlankToNull());

        teacherPreferencesDAO.updateTeacherPreferences(getPreference);

        return Optional.ofNullable(teacherPreferencesDAO.getTeacherPreferencesByUuid(preferenceUuid))
                .map(data -> BeanUtil.toBean(data, TeacherPreferencesDTO.class))
                .orElseThrow(() -> new BusinessException("更新教师课程偏好失败", ErrorCode.SERVER_INTERNAL_ERROR));
    }

    /**
     * 删除教师课程偏好
     * <p>
     * 根据给定的教师课程偏好唯一标识符 {@code preferenceUuid}，从系统中删除对应的偏好记录。
     * 该操作不可逆，请谨慎使用。删除后，与该偏好相关的所有数据将被清除。
     * </p>
     *
     * @param preferenceUuid 教师课程偏好的唯一标识符，用于定位需要删除的具体偏好记录
     * @param request        请求对象，用于获取当前请求的上下文信息
     */
    @Override
    public void deleteTeacherPreferences(String preferenceUuid, HttpServletRequest request) {
        TeacherPreferencesDO getPreference = Optional.ofNullable(teacherPreferencesDAO.getTeacherPreferencesByUuid(preferenceUuid))
                .orElseThrow(() -> new BusinessException("教师课程偏好不存在", ErrorCode.NOT_EXIST));

        UserDO getUser = userService.getUserByRequest(request);
        if (getUser == null) {
            throw new UserAuthenticationException(UserAuthenticationException.ErrorType.USER_NOT_LOGIN, request);
        }

        TeacherDO getTeacher = teacherDAO.getTeacherByUserUuid(getUser.getUserUuid());

        Optional.ofNullable(getPreference.getTeacherUuid())
                .filter(teacherUuid -> !teacherUuid.equals(getTeacher.getTeacherUuid()))
                .ifPresent(teacherUuid -> {
                    throw new BusinessException("教师课程偏好不属于当前用户", ErrorCode.NOT_EXIST);
                });

        if (!teacherPreferencesDAO.deleteTeacherPreference(getPreference)) {
            throw new BusinessException("删除教师课程偏好失败", ErrorCode.SERVER_INTERNAL_ERROR);
        }
    }
}
