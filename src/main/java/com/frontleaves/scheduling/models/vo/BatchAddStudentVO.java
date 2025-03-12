package com.frontleaves.scheduling.models.vo;

import com.frontleaves.scheduling.constants.StringConstant;
import io.micrometer.common.util.StringUtils;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;

/**
 * 批量添加学生信息的值对象（VO）
 * <p>
 * 用于接收批量添加学生信息的请求数据。
 * </p>
 *
 * @author FLASHLACK
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BatchAddStudentVO {

    /**
     * Base64 编码的 Excel 文件
     * <p>
     * 包含学生信息的 Excel 文件，需以 Base64 编码形式传递。
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
    @Size(max = 10 * 1024 * 1024, message = "Excel文件大小不能超过10MB")
    private String file;
    /**
     * 是否忽略错误
     * <p>
     * 如果为 true，则在导入过程中忽略错误继续执行；如果为 false，则在遇到错误时停止导入。
     * </p>
     */
    @NotNull
    private Boolean ignoreError;
    /**
     * 学院 UUID
     * <p>
     * 关联学院的唯一标识符，必须为不带短横线的 UUID 格式。
     * </p>
     */
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)
    @NotNull
    private String departmentUuid;
    /**
     * 专业 UUID
     * <p>
     * 关联专业的唯一标识符，必须为不带短横线的 UUID 格式。
     * </p>
     */
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)
    @NotNull
    private String majorUuid;
    /**
     * 行政班 UUID
     * <p>
     * 关联行政班的唯一标识符，必须为不带短横线的 UUID 格式。
     * </p>
     */
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)
    @NotNull
    private String administrativeClassUuid;
    /**
     * 年级
     * <p>
     * 学生所在年级，必须为不带短横线的 UUID 格式。
     * </p>
     */
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)
    @NotNull
    private String grade;

    /**
     * 校验上传的Excel文件格式
     *
     * @throws IllegalArgumentException 当文件格式不支持时
     */
    public void validateFile() {
        // 非空校验
        if (StringUtils.isBlank(file)) {
            throw new IllegalArgumentException("Excel文件不能为空");
        }
        // 校验文件格式
        validateFileFormat(file);
    }

    /**
     * 验证文件格式
     *
     * @param base64File Base64编码的文件
     * @throws IllegalArgumentException 当文件格式不支持时
     */
    private void validateFileFormat(String base64File) {
        // 支持的Excel文件MIME类型前缀
        String[] supportedPrefixes = {
                // .xlsx
                "data:application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;base64,",
                // .xls
                "data:application/vnd.ms-excel;base64,",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;base64,",
                "application/vnd.ms-excel;base64,"
        };
        // 检查是否以支持的前缀开头
        boolean isValidFormat = Arrays.stream(supportedPrefixes)
                .anyMatch(base64File::startsWith);
        if (!isValidFormat) {
            throw new IllegalArgumentException("Excel文件格式不支持，仅支持 .xlsx 和 .xls 格式");
        }
    }
}