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

package enums;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * 排课策略枚举
 * <p>
 * 该枚举类定义了三种排课策略：最优、平衡和快速。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 */
@RequiredArgsConstructor
public enum StrategyEnum {
    /**
     * 排课策略，可选: optimal(最优), balanced(平衡), quick(快速)
     */

    OPTIMAL("optimal"),
    BALANCED("balanced"),
    QUICK("quick");

    private final String value;

    /**
     * 根据值获取对应的策略枚举
     *
     * @param value 策略值
     * @return 策略枚举
     */
    public static StrategyEnum fromValue(String value) {
        for (StrategyEnum strategy : StrategyEnum.values()) {
            if (strategy.getValue().equalsIgnoreCase(value)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }

    /**
     * 检查值是否有效
     *
     * @param value 策略值
     * @return 是否有效
     */
    public static boolean isValidValue(String value) {
        for (StrategyEnum strategy : StrategyEnum.values()) {
            if (strategy.getValue().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    public static String getDefaultValue() {
        return OPTIMAL.getValue();
    }

    @Contract(pure = true)
    public static String getDefaultValue(@NotNull StrategyEnum strategy) {
        return strategy.getValue();
    }

    public static String getDefaultValue(String value) {
        return fromValue(value).getValue();
    }

    /**
     * 获取策略值
     *
     * @return 策略值
     */
    public String getValue() {
        return value;
    }

}
