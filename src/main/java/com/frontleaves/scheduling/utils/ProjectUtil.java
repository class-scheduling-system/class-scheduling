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

package com.frontleaves.scheduling.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.sax.Excel07SaxReader;
import cn.hutool.poi.excel.sax.handler.RowHandler;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.UserDTO;
import com.frontleaves.scheduling.models.entity.UserDO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RMap;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 项目工具类
 * <p>
 * 提供了一系列的静态方法，用于在项目中进行一些通用的操作。该类中的方法主要用于对象之间的转换、数据处理等。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
public class ProjectUtil {

    private ProjectUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 将用户实体对象转换为用户数据传输对象
     * <p>
     * 该方法接收一个 {@code UserDO} 对象，并将其转换为对应的 {@code UserDTO} 对象。
     * 转换过程中，如果用户权限不为空，则会将权限从 JSON 格式字符串转换为字符串列表。
     * 如果权限为空，则在 {@code UserDTO} 中设置权限为 null。
     * </p>
     *
     * @param userDO 用户实体对象，不能为空
     * @return 转换后的用户数据传输对象
     */
    @NotNull
    public static UserDTO convertUserDoToUserDTO(@NotNull UserDO userDO) {
        UserDTO userDTO = BeanUtil.toBean(userDO, UserDTO.class);
        if (userDO.getPermission() != null && !userDO.getPermission().isEmpty()) {
            userDTO.setPermission(JSONUtil.toList(userDO.getPermission(), String.class));
        } else {
            userDTO.setPermission(null);
        }
        return userDTO;
    }

    /**
     * 将分页对象转换为分页数据传输对象
     * <p>
     * 该方法接收一个 {@code Page<T>} 对象和目标类型的类，将其转换为对应的 {@code PageDTO<E>} 对象。
     * 转换过程中，如果分页对象为空，则返回一个新的空的 {@code PageDTO} 对象。
     * 如果分页对象不为空且当前页码不为0，则创建一个新的 {@code PageDTO} 对象，并设置总记录数、每页大小、当前页码和记录列表。
     * 记录列表会从 JSON 格式的字符串转换为目标类型列表。
     * </p>
     *
     * @param page  分页对象，可以为空
     * @param clazz 目标类型的类
     * @param <T>   源分页对象中记录的泛型类型
     * @param <E>   目标分页数据传输对象中记录的泛型类型
     * @return 转换后的分页数据传输对象
     */
    @NotNull
    public static <T, E> PageDTO<E> convertPageToPageDTO(Page<T> page, Class<E> clazz) {
        if (page == null) {
            return new PageDTO<>();
        }
        if (page.getCurrent() != 0) {
            PageDTO<E> pageDTO = new PageDTO<>(page.getTotal(), page.getSize());
            pageDTO
                    .setRecords(JSONUtil.toJsonStr(page.getRecords()), clazz)
                    .setCurrent(page.getCurrent());
            return pageDTO;
        } else {
            return new PageDTO<>();
        }
    }

    /**
     * 根据传入的映射创建分页对象
     * <p>
     * 该方法接收一个包含分页信息的映射和记录类型的类，根据映射中的数据创建并返回一个分页对象。
     * 映射中应包含以下键值对："current" 表示当前页码，默认为1；"size" 表示每页大小，默认为20；
     * "records" 表示记录列表的 JSON 字符串，默认为空列表；"total" 表示总记录数，默认为0。
     * </p>
     *
     * @param map   包含分页信息的映射，不能为空
     * @param clazz 记录的类型
     * @param <T>   记录的泛型类型
     * @return 创建的分页对象
     */
    @NotNull
    public static <T> Page<T> convertMapToPage(@NotNull Map<String, String> map, Class<T> clazz) {
        Page<T> pageResult = new Page<>(
                Long.parseLong(map.getOrDefault("current", "1")),
                Long.parseLong(map.getOrDefault("size", "20"))
        );
        List<T> records = JSONUtil.toList(map.getOrDefault("records", "[]"), clazz);
        pageResult
                .setRecords(records)
                .setTotal(Long.parseLong(map.getOrDefault("total", "0")));
        return pageResult;
    }

    /**
     * 查询并缓存分页数据
     * <p>
     * 该方法用于根据给定的查询条件从数据库中获取分页数据，并将结果缓存到 Redis 中。如果查询成功，返回包含查询结果的分页对象。
     * 缓存的数据包括记录、当前页码、每页大小、总记录数和总页数。缓存的有效期为 1 小时。
     * </p>
     *
     * @param queryWrapper 查询条件包装器，用于构建查询条件
     * @param page         分页的页码
     * @param size         每页的大小
     * @param map          用于存储缓存数据的 Redis Map 对象
     * @return 返回包含查询结果的分页对象，如果查询失败则返回 null
     */
    @Nullable
    public static <T> Page<T> queryAndCache(@NotNull LambdaQueryChainWrapper<T> queryWrapper, int page, int size, RMap<String, String> map) {
        Page<T> buildingPage = queryWrapper.page(new Page<>(page, size));

        if (buildingPage.getCurrent() != 0) {
            map.put("records", JSONUtil.toJsonStr(buildingPage.getRecords()));
            map.put("current", String.valueOf(buildingPage.getCurrent()));
            map.put("size", String.valueOf(buildingPage.getSize()));
            map.put("total", String.valueOf(buildingPage.getTotal()));
            map.put("pages", String.valueOf(buildingPage.getPages()));
            map.expire(Duration.ofSeconds(3600));
            return buildingPage;
        }
        return null;
    }

    /**
     * 解析Excel文件，返回行数据列表
     *
     * @param excelBytes     Excel文件字节数组
     * @param startRow       开始读取的行号（从0开始计数）
     * @param columnsToCheck 需要检查的列数（检查前N列是否有值）
     * @return 行数据列表，每行为一个List<Object>
     */
    public static List<List<Object>> parseExcelToRowList(byte[] excelBytes, int startRow, int columnsToCheck) {
        try {
            // 存储解析结果
            List<List<Object>> resultList = new ArrayList<>();

            // 创建行处理器
            RowHandler rowHandler = createRowHandler(startRow, columnsToCheck, resultList);

            // 使用ByteArrayInputStream读取文件内容
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(excelBytes)) {
                // 判断Excel版本并选择合适的解析方式
                if (isExcel2007(excelBytes)) {
                    // 使用Excel07SaxReader处理xlsx格式
                    Excel07SaxReader reader = new Excel07SaxReader(rowHandler);
                    // 只读取第一个sheet
                    reader.read(byteArrayInputStream, 0);
                } else {
                    // 使用ExcelUtil.readBySax处理Excel文件（自动判断格式）
                    ExcelUtil.readBySax(byteArrayInputStream, 0, rowHandler);
                }
                return resultList;
            }
        } catch (Exception e) {
            // 捕获并处理解析过程中可能发生的异常
            throw new IllegalArgumentException("Excel解析失败：" + e.getMessage(), e);
        }
    }

    /**
     * 创建Excel行处理器
     *
     * @param startRow       开始读取的行号
     * @param columnsToCheck 需要检查的列数
     * @param resultList     结果列表
     * @return 行处理器
     */
    private static RowHandler createRowHandler(int startRow, int columnsToCheck, List<List<Object>> resultList) {
        return (sheetIndex, rowIndex, rowlist) -> {
            // 从指定行开始读取
            if (rowIndex >= startRow && rowlist != null && !rowlist.isEmpty()
                    && hasValidData(rowlist, columnsToCheck)) {
                    resultList.add(new ArrayList<>(rowlist));
                }

        };
    }

    /**
     * 检查行中是否有有效数据
     *
     * @param rowlist        行数据
     * @param columnsToCheck 需要检查的列数
     * @return 是否有有效数据
     */
    private static boolean hasValidData(List<Object> rowlist, int columnsToCheck) {
        int actualColumnsToCheck = Math.min(columnsToCheck, rowlist.size());

        for (int i = 0; i < actualColumnsToCheck; i++) {
            Object cellValue = rowlist.get(i);
            if (cellValue != null && !cellValue.toString().trim().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查给定的字节数组是否代表一个Excel 2007或更高版本的文件
     * Excel 2007+ 文件的前8个字节是固定的PK头部，用于识别文件格式
     *
     * @param bytes 文件的字节数组表示
     * @return 如果字节数组以Excel 2007+ 文件的PK头部开始，则返回true；否则返回false
     */
    private static boolean isExcel2007(byte[] bytes) {
        // Excel 2007+ 文件的前8个字节是固定的PK头部
        if (bytes.length >= 4) {
            // 检查前4个字节是否与Excel 2007+ 文件的PK头部匹配
            return bytes[0] == 'P' && bytes[1] == 'K' && bytes[2] == 0x03 && bytes[3] == 0x04;
        }
        // 如果字节数组长度不足4，不可能是Excel 2007+ 文件
        return false;
    }
}
