package com.frontleaves.scheduling.models.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 手动排课请求视图对象
 *
 * @author xiao_lfeng
 */
@Getter
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class ManualSchedulingRequestVO {
    /**
     * 用户输入
     */
    private String structuredData;

    /**
     * 当前学期 UUID
     */
    @NotBlank(message = "当前学期不能为空")
    private String currentSemesterUuid;

    /**
     * 对话内容
     */
    private String ask;

    /**
     * 是否是编辑模式
     */
    private Boolean edit;

    /**
     * 排课主键
     */
    private String scheduleUuid;
}
