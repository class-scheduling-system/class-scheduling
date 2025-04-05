package com.frontleaves.scheduling.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 批量添加校区返回数据传输对象
 * <p>
 * 用于返回批量导入校区的结果信息，包括总数、成功数、失败数以及失败详情
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class BackAddCampusDTO {

    /**
     * 总数
     */
    private Integer totalCount;

    /**
     * 成功数
     */
    private Integer successCount;

    /**
     * 失败数
     */
    private Integer failedCount;

    /**
     * 失败详情列表
     */
    private List<FailedDetail> failedDetails;

    /**
     * 失败详情内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class FailedDetail {
        /**
         * 行号（从1开始）
         */
        private Integer rowNumber;

        /**
         * 错误信息
         */
        private String errorMessage;
    }
} 