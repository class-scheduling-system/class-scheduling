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

package com.frontleaves.scheduling.models.dto;

import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页数据传输对象
 * <p>
 * 用于返回分页数据相关信息，包含记录列表、总记录数、每页大小、当前页码和总页数。
 * 该类提供了将 JSON 格式的记录字符串转换为实际记录列表的方法，以及直接设置记录列表的方法。
 * </p>
 *
 * @param <T> 记录的类型
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
public class PageDTO<T> {
    private List<T> records;
    private Long total;
    private Long size;
    private Long current;

    public PageDTO() {
        this.total = 0L;
        this.size = 0L;
        this.current = 0L;
        this.records = new ArrayList<>();
    }

    public PageDTO(long total, long size) {
        this.total = total;
        this.size = size;
        this.records = new ArrayList<>();
    }

    public PageDTO<T> setRecords(String jsonRecords, Class<T> t) {
        this.records = JSONUtil.toList(jsonRecords, t);
        return this;
    }

    public PageDTO<T> setRecords(List<T> records) {
        this.records = records;
        return this;
    }
}
