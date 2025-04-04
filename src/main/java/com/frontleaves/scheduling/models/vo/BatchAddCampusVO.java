package com.frontleaves.scheduling.models.vo;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 批量添加校区视图对象
 * <p>
 * 用于批量导入校区信息的请求体，包含Excel文件的Base64编码和是否忽略错误的配置
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BatchAddCampusVO {
    /**
     * Base64 编码的 Excel 文件
     * <p>
     * 包含校区信息的 Excel 文件，需以 Base64 编码形式传递。
     * </p>
     * <ul>
     *   <li>必填字段</li>
     *   <li>支持 .xlsx 和 .xls 格式</li>
     *   <li>文件大小建议限制在 10MB 以内</li>
     * </ul>
     *
     * @apiNote 文件必须是有效的 Base64 编码的 Excel 文件
     */
    @NotNull(message = "Excel文件不能为空")
    private String file;
    
    /**
     * 是否忽略错误
     * <p>
     * 如果为 true，则在导入过程中忽略错误继续执行；如果为 false，则在遇到错误时停止导入。
     * </p>
     */
    @NotNull
    private Boolean ignoreError;
} 