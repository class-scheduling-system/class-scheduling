package enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * 课程类型枚举
 * <p>
 * 该枚举类定义了四种课程类型：理论、实践、上机和其他。
 * </p>
 *
 * @author 26473
 * @version v1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum CourseEnuType {
    /**
     * 理论课
     */
    THEORY("theory", "理论课"),
    /**
     * 实践课
     */
    PRACTICE("practice", "实践课"),
    /**
     * 上机课
     */
    COMPUTER("computer", "上机课"),
    /**
     * 混排课程（理论+实践混合）
     */
    MIXED("mixed", "混排课程"),
    /**
     * 其他类型课程
     */
    OTHER("other", "其他类型");

    /**
     * 英文类型名称
     */
    private final String englishName;
    /**
     * 中文类型名称
     */
    private final String chineseName;

    /**
     * 根据英文名称获取对应的课程类型枚举
     *
     * @param englishName 英文名称
     * @return 课程类型枚举
     * @throws IllegalArgumentException 如果找不到对应的枚举值
     */
    public static @NotNull CourseEnuType fromEnglishName(String englishName) {
        for (CourseEnuType type : CourseEnuType.values()) {
            if (type.getEnglishName().equalsIgnoreCase(englishName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown english name: " + englishName);
    }

    /**
     * 根据中文名称获取对应的课程类型枚举
     *
     * @param chineseName 中文名称
     * @return 课程类型枚举
     * @throws IllegalArgumentException 如果找不到对应的枚举值
     */
    public static @NotNull CourseEnuType fromChineseName(String chineseName) {
        for (CourseEnuType type : CourseEnuType.values()) {
            if (type.getChineseName().equals(chineseName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown chinese name: " + chineseName);
    }

    /**
     * 检查英文名称是否有效
     *
     * @param englishName 英文名称
     * @return 是否有效
     */
    public static boolean isValidEnglishName(String englishName) {
        for (CourseEnuType type : CourseEnuType.values()) {
            if (type.getEnglishName().equalsIgnoreCase(englishName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查中文名称是否有效
     *
     * @param chineseName 中文名称
     * @return 是否有效
     */
    public static boolean isValidChineseName(String chineseName) {
        for (CourseEnuType type : CourseEnuType.values()) {
            if (type.getChineseName().equals(chineseName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取默认课程类型（理论课）
     *
     * @return 默认课程类型的英文名称
     */
    public static String getDefaultEnglishName() {
        return THEORY.getEnglishName();
    }

    /**
     * 获取指定枚举值的英文名称
     *
     * @param type 课程类型枚举
     * @return 英文名称
     */
    @Contract(pure = true)
    public static String getEnglishName(@NotNull CourseEnuType type) {
        return type.getEnglishName();
    }

    /**
     * 根据英文名称获取对应的英文名称（验证用）
     *
     * @param englishName 英文名称
     * @return 英文名称
     */
    public static String getEnglishName(String englishName) {
        return fromEnglishName(englishName).getEnglishName();
    }
}