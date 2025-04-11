package com.frontleaves.scheduling.models.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
/**
 * 课程时间安排的VO类
 * @author flashlack
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClassTimeVO {
    /**
     * 星期几 (例如，1代表周一, 2代表周二)
     */
    private Integer dayOfWeek;

    /**
     * 开始节次
     */
    private Integer periodStart;

    /**
     * 结束节次
     */
    private Integer periodEnd;

    /**
     * 生效周列表
     */
    private List<Integer> weekNumbers;
}
