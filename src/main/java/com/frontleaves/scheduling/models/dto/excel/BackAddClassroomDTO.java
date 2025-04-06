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

package com.frontleaves.scheduling.models.dto.excel;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 批量添加教室返回DTO
 * <p>
 * 该类用于封装批量导入教室信息的结果，包含总记录数、成功数量、失败数量以及失败详情。
 * 当批量导入过程中出现错误时，failedDetails字段将包含详细的错误信息。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@Accessors(chain = true)
public class BackAddClassroomDTO {

    /**
     * 总记录数
     */
    private Integer totalCount;

    /**
     * 成功数量
     */
    private Integer successCount;

    /**
     * 失败数量
     */
    private Integer failedCount;

    /**
     * 失败详情列表
     */
    private List<FailedDetail> failedDetails;

    /**
     * 失败详情内部类
     * <p>
     * 用于记录每条失败记录的详细信息，包括行号和失败原因。
     * </p>
     */
    @Data
    @Accessors(chain = true)
    public static class FailedDetail {

        /**
         * 行号
         * <p>
         * 表示在Excel文件中的行号，便于用户定位问题。
         * </p>
         */
        private Integer row;

        /**
         * 失败原因
         * <p>
         * 详细描述导入失败的原因，如数据格式错误、必填字段缺失等。
         * </p>
         */
        private String reason;
    }
}
