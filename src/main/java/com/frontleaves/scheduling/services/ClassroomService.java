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

package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.*;
import com.frontleaves.scheduling.models.vo.BatchAddClassroomVO;
import com.frontleaves.scheduling.models.vo.ClassroomVO;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * 教室服务接口，定义了教室相关的操作。
 * <p>
 * 该接口提供了教室管理相关的基础方法，包括添加教室、删除教室、查询教室信息等。具体实现细节由实现类决定。
 * </p>
 *
 * @author xiao_lfeng
 * @since v1.0.0
 * @since v1.0.0
 */
public interface ClassroomService {

    /**
     * 列出所有教室标签
     * <p>
     * 该方法用于获取系统中所有的教室标签信息。每个教室标签由 {@code ClassroomTagDTO} 对象表示，包含标签的主键、名称、描述以及创建和更新时间。
     * </p>
     *
     * @return 返回一个包含所有教室标签的列表
     */
    List<ClassroomTagDTO> listClassroomTags();

    /**
     * 列出所有教室类型
     * <p>
     * 该方法用于获取系统中所有的教室类型信息。每个教室类型由 {@code ClassroomTypeDTO} 对象表示，包含教室类型的主键、名称、描述以及创建和更新时间。
     * </p>
     *
     * @return 返回一个包含所有教室类型的列表
     */
    List<ClassroomTypeDTO> listClassroomTypes();


    /**
     * 获取教室分页数据
     * <p>
     * 该方法用于根据指定的分页参数、排序方式以及搜索条件获取教室信息的分页结果。返回的结果包含当前页的数据记录、总记录数等信息。
     * </p>
     *
     * @param page    当前页码，从1开始
     * @param size    每页显示的记录数
     * @param isDesc  是否降序排列，如果为 {@code true} 则按降序排列，否则按升序排列
     * @param keyword 搜索关键词，用于在教室名称或编号中进行模糊搜索
     * @param tag     教室标签，用于筛选具有特定标签的教室
     * @param type    教室类型，用于筛选特定类型的教室
     * @return 返回一个包含教室分页数据的 {@code PageDTO<ClassroomDTO>} 对象
     */
    PageDTO<ClassroomInfoDTO> getClassroomPage(
            int page,
            int size,
            boolean isDesc,
            @Nullable String keyword,
            @Nullable String tag,
            @Nullable String type
    );

    /**
     * 根据 UUID 获取教室类型
     * <p>
     * 该方法用于根据给定的 UUID 获取对应的教室类型信息。如果找到匹配的记录，则返回一个 {@code ClassroomTypeDTO} 对象，否则返回 {@code null}。
     * </p>
     *
     * @param uuid 教室类型的唯一标识符
     * @return 返回与给定 UUID 匹配的教室类型数据传输对象，如果没有找到匹配的记录则返回 {@code null}
     */
    @Nullable
    ClassroomTypeDTO getClassroomTypeByUuid(String uuid);

    /**
     * 根据 UUID 获取教室标签
     * <p>
     * 该方法用于根据给定的 UUID 获取对应的教室标签信息。如果找到匹配的记录，则返回一个 {@code ClassroomTagDTO} 对象，否则返回 {@code null}。
     * </p>
     *
     * @param uuid 教室标签的唯一标识符
     * @return 返回与指定 UUID 匹配的教室标签信息，如果没有找到匹配的记录则返回 {@code null}
     */
    @Nullable
    ClassroomTagDTO getClassroomTagByUuid(String uuid);

    /**
     * 添加新教室
     * <p>
     * 该方法用于根据传入的 {@code ClassroomVO} 对象添加一个新的教室记录。首先，将 {@code ClassroomVO} 转换为
     * {@code ClassroomDO} 对象并保存到数据库中。接着，通过新创建的教室 UUID 从数据库中获取刚刚添加的教室记录。
     * 如果未能成功获取教室记录，则抛出一个 {@code ServerInternalErrorException} 异常。最后，构建一个包含教室信息、标签、类型、所属校区及所在楼宇等详细信息的
     * {@code ClassroomInfoDTO} 对象并返回。
     *
     * @param classroomVO 用于表示要添加的新教室的基本信息
     * @return 包含新增教室详细信息的 {@code ClassroomInfoDTO} 对象
     */
    ClassroomInfoDTO addClassroom(ClassroomVO classroomVO);

    /**
     * 根据教室编号获取教室信息
     * <p>
     * 该方法用于根据给定的教室编号获取对应的教室信息。如果找到匹配的记录，则返回一个 {@code ClassroomDTO} 对象，否则返回 {@code null}。
     * </p>
     *
     * @param number 教室编号
     * @return 返回与给定教室编号匹配的教室数据传输对象，如果没有找到匹配的记录则返回 {@code null}
     */
    ClassroomDTO getClassroomByNumber(String number);

    /**
     * 根据教室 UUID 获取教室信息
     * <p>
     * 该方法用于根据给定的教室 UUID 获取对应的教室信息。如果找到匹配的记录，则返回一个 {@code ClassroomInfoDTO} 对象，否则返回 {@code null}。
     * 返回的对象包含教室的基本信息、标签、类型、所属校区及所在楼宇等详细信息。
     * </p>
     *
     * @param classroomUuid 教室的唯一标识符
     * @return 返回与给定教室 UUID 匹配的教室信息，如果没有找到匹配的记录则返回 {@code null}
     */
    @Nullable
    ClassroomInfoDTO getClassroomByUuid(String classroomUuid);

    /**
     * 编辑教室
     * <p>
     * 该方法用于根据传入的 {@code ClassroomVO} 对象编辑指定的教室。在编辑过程中，会进行一系列数据可用性检查，确保关联的教学楼、校区、教室类型、管理部门以及桌椅类型均存在。
     * 如果任何一项数据不存在，则抛出 {@code BusinessException} 异常，并附带相应的错误码。如果所有数据验证通过，则调用服务层的方法将新的教室信息保存到数据库中，并返回包含成功信息及新教室详情的响应。
     * </p>
     *
     * @param classroomUuid 教室的唯一标识符
     * @param classroomVO   包含待编辑教室详细信息的视图对象
     * @return 响应实体，包含操作结果和新创建的教室信息
     */
    ClassroomInfoDTO editClassroom(String classroomUuid, ClassroomVO classroomVO);

    /**
     * 删除教室
     * <p>
     * 根据给定的教室唯一标识符 {@code classroomUuid}，从系统中删除对应的教室记录。
     * 该操作不可逆，请谨慎使用。删除后，与该教室相关的所有数据将被清除。
     *
     * @param classroomUuid 教室的唯一标识符，用于定位需要删除的具体教室
     */
    void deleteClassroom(String classroomUuid);

    /**
     * 获取教室简单列表
     * <p>
     * 该方法用于获取所有教室的简化信息列表。每个教室由 {@code ClassroomLiteDTO} 对象表示，
     * 仅包含教室的基本信息，如UUID、编号、名称、容量和状态。主要用于下拉框等简单展示场景。
     * </p>
     *
     * @param keyword 搜索关键词，用于在教室名称或编号中进行模糊搜索
     * @return 返回一个包含所有教室简化信息的列表
     */
    List<ClassroomLiteDTO> listClassroomLite(String keyword);

    /**
     * 获取教室导入模板的字节数组
     * <p>
     * 该方法生成用于批量导入教室信息的Excel模板，返回包含模板数据的字节数组。
     * 模板包含必填字段、可选字段的说明以及示例数据。
     * </p>
     *
     * @return 包含教室导入模板的字节数组
     */
    byte[] getClassroomImportTemplate();

    /**
     * 验证批量导入教室数据并返回处理后的文件
     * <p>
     * 该方法用于验证通过Base64编码传入的Excel文件，确保其格式正确并且可以用于教室信息的批量导入。
     * </p>
     *
     * @param batchAddClassroomVO 包含Excel文件的Base64编码和导入设置的对象
     * @return 处理后的Excel文件字节数组
     */
    byte[] verifyClassroomBatchAndBackFile(BatchAddClassroomVO batchAddClassroomVO);

    /**
     * 批量导入教室信息，不忽略错误
     * <p>
     * 该方法用于批量导入教室信息，当遇到任何数据错误时会立即停止导入并抛出异常。
     * </p>
     *
     * @param file Excel文件的字节数组
     * @return 包含导入结果统计的对象
     */
    BackAddClassroomDTO batchImportNoIgnoreError(byte[] file);

    /**
     * 批量导入教室信息，忽略错误
     * <p>
     * 该方法用于批量导入教室信息，当遇到数据错误时会继续处理其他数据，并记录错误信息。
     * </p>
     *
     * @param file Excel文件的字节数组
     * @return 包含导入结果统计和错误详情的对象
     */
    BackAddClassroomDTO batchImportIgnoreError(byte[] file);
}
