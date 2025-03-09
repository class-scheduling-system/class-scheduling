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

package com.frontleaves.scheduling.models.vo;

import com.frontleaves.scheduling.constants.StringConstant;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 教室信息视图对象
 * <p>
 * 该类用于封装教室相关的详细信息，包括教室编号、名称、所在校区及楼栋、楼层、类型、容量等属性。
 * 此外，还包含了一些教室的特殊配置如是否为考场、多媒体教室、空调配备情况以及教室的状态描述。
 * 通过此视图对象可以方便地进行数据传输和展示。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomVO {
    /**
     * 教室编号
     * <p>
     * 该字段用于存储教室的唯一编号标识。不能为空，可以自定义输入。
     * 该字段通过 {@code @NotBlank} 注解进行非空验证，确保提交的表单中必须包含有效的教室编号。
     * 教室编号通常由字母、数字或其组合构成，用于在系统中快速识别和查找教室。
     */
    @NotBlank(message = "教室编号不能为空")
    private String number;

    /**
     * 教室名称
     * <p>
     * 该字段用于存储教室的名称。不能为空，需提供可读性良好的描述性名称。
     * 该字段通过 {@code @NotBlank} 注解进行非空验证，确保提交的表单中必须包含有效的教室名称。
     * 教室名称应简洁明了，方便用户识别和理解教室的用途或位置。
     */
    @NotBlank(message = "教室名称不能为空")
    private String name;

    /**
     * 校区主键
     * <p>
     * 该字段用于标识教室所属的校区。不能为空，且必须符合UUID格式（不带短横线）。
     * 该字段通过 {@code @NotBlank} 注解进行非空验证，并通过 {@code @Pattern} 注解确保提供的UUID格式正确。
     * 例如，校区的UUID为3fa85f6457174562b3fc2c963f66afa6（不带短横线，小写）。
     */
    @NotBlank(message = "校区主键不能为空")
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION, message = "校区主键输入有误")
    private String campusUuid;

    /**
     * 楼栋主键
     * <p>
     * 该字段用于标识教室所属的楼栋。不能为空，且必须符合UUID格式（不带短横线）。
     * 该字段通过 {@code @NotBlank} 注解进行非空验证，并通过 {@code @Pattern} 注解确保提供的UUID格式正确。
     * 例如，楼栋的UUID为3fa85f6457174562b3fc2c963f66afa6（不带短横线，小写）。
     */
    @NotBlank(message = "楼栋主键不能为空")
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION, message = "楼栋主键输入有误")
    private String buildingUuid;

    /**
     * 楼层
     * <p>
     * 该字段用于标识教室所在的楼层。不能为空，且必须符合特定的格式规范。
     * 该字段通过 {@code @NotNull} 注解进行非空验证，并通过 {@code @Pattern} 注解确保符合格式规范。
     * 楼层表示可以是地下楼层（如B1、B2等），地面层（G），或数字表示的楼层。
     * 格式必须符合正则表达式 ^(B?\\d+|G){4}，表示以B开头的数字、纯数字或字母G，长度为4个字符。
     */
    @NotNull(message = "楼层不能为空")
    @Pattern(regexp = "^(B?\\d{1,3}|G)$", message = "楼层输入有误")
    private String floor;

    /**
     * 教室类型
     * <p>
     * 该字段用于标识教室的类型。不能为空，且必须符合UUID格式（不带短横线）。
     * 该字段通过 {@code @NotBlank} 注解进行非空验证，并通过 {@code @Pattern} 注解确保提供的UUID格式正确。
     * 教室类型可以是如：普通教室、多媒体教室、实验室等，通过UUID关联到教室类型表中的具体类型。
     * 例如，类型的UUID为3fa85f6457174562b3fc2c963f66afa6（不带短横线，小写）。
     */
    @NotBlank(message = "教室类型不能为空")
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION, message = "教室类型输入有误")
    private String type;

    /**
     * 教室标签列表
     * <p>
     * 该变量用于存储与教室相关的标签，每个标签必须符合 UUID 格式且不含中划线。
     * 具体格式要求见 {@code StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION}。
     * 如果输入的标签不符合指定的正则表达式，则会抛出相应的错误信息。
     */
    private List<@Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION, message = "教室标签输入有误") String> tag;

    /**
     * 教室容量
     * <p>
     * 该字段用于存储教室可容纳的最大人数。不能为空，且不能小于1。
     * 该字段通过 {@code @NotNull} 注解进行非空验证，并通过 {@code @Min} 注解确保容量至少为1人。
     * 教室容量是安排课程和活动时的重要参考指标，用于确保空间足够容纳预期人数。
     */
    @NotNull(message = "教室容量不能为空")
    @Min(value = 1, message = "教室容量不能低于 1 人")
    private Integer capacity;

    /**
     * 是否是考场
     * <p>
     * 该字段用于标识教室是否可以用作考场。不能为空，布尔值表示。
     * 该字段通过 {@code @NotNull} 注解进行非空验证，确保提交的表单中必须明确指定该教室是否为考场。
     * true表示教室可以用作考场，false表示教室不适合作为考场使用。
     * 考场标识对于安排考试和评估活动具有重要意义。
     */
    @NotNull(message = "是否是考场不能为空")
    private Boolean examinationRoom;

    /**
     * 考场容量
     * <p>
     * 该字段用于存储教室作为考场时可容纳的最大人数。可以为空，仅当教室被标识为考场时有意义。
     * 考场容量通常小于或等于普通教室容量，因为考试时需要确保座位之间有足���的间距，防止作弊行为。
     * 如果教室不是考场（examinationRoom为false），则此字段可不填。
     */
    private Integer examinationRoomCapacity;

    /**
     * 是否是多媒体教室
     * <p>
     * 该字段用于标识教室是否配备多媒体设备。不能为空，布尔值表示。
     * 该字段通过 {@code @NotNull} 注解进行非空验证，确保提交的表单中必须明确指定该教室是否为多媒体教室。
     * true表示教室配备有多媒体设备（如投影仪、电子白板等），false表示教室不具备多媒体设备。
     * 多媒体设备的存在对于选择适合特定教学方式的教室非常重要。
     */
    @NotNull(message = "是否是多媒体教室不能为空")
    private Boolean isMultimedia;

    /**
     * 是否有空调
     * <p>
     * 该字段用于标识教室是否配备空调设备。不能为空，布尔值表示。
     * 该字段通过 {@code @NotNull} 注解进行非空验证，确保提交的表单中必须明确指定该教室是否有空调。
     * true表示教室配备有空调设备，false表示教室不具备空调设备。
     * 空调设备的存在对于确保教学环境舒适度，特别是在极端天气条件下，具有重要意义。
     */
    @NotNull(message = "是否有空调不能为空")
    private Boolean isAirConditioned;

    /**
     * 教室状态
     * <p>
     * 该字段用于标识教室的使用状态。不能为空，布尔值表示。
     * 该字段通过 {@code @NotNull} 注解进行非空验证，确保提交的表单中必须明确指定该教室的使用状态。
     * true表示教室处于启用状态可正常使用，false表示教室处于禁用状态不可使用。
     * 状态标识对于教室资源的合理分配和管理至关重要。
     */
    @NotNull(message = "教室状态不能为空")
    private Boolean status;

    /**
     * 教室描述
     * <p>
     * 该字段用于存储对教室的详细描述信息。可以为空，提供额外的教室信息。
     * 描述可以包含教室的特点、用途、历史或其他用户可能关的信息。
     * 此字段有助于用户更全面地了解教室的情况，便于做出更合适的选择。
     */
    private String description;

    /**
     * 管理部门
     * <p>
     * 该字段用于标识教室所属的管理部门。可以为空，如果提供必须符合UUID格式（不带短横线）。
     * 例如，管理部门的UUID为3fa85f6457174562b3fc2c963f66afa6（不带短横线，小写）。
     * 该字段通过正则表达式 {@code StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION_ABLE_EMPTY} 进行验证。
     */
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION_ABLE_EMPTY, message = "管理部门输入有误")
    private String managementDepartment;

    /**
     * 教室面积
     * <p>
     * 该字段用于存储教室的面积大小，以平方米为单位。不能为空，必须是有效的数值。
     * 该字段通过 {@code @NotNull} 注解进行非空验证，确保提交的表单中必须包含有效的教室面积。
     * 教室面积是评估教室容量和适用性的重要参考指标，用于确保教室空间符合教学需求。
     * 面积使用BigDecimal类型，可以精确表示小数，避免浮点数计算误差。
     */
    @NotNull(message = "教室面积不能为空")
    private BigDecimal area;

    /**
     * 桌椅类型
     * <p>
     * 该字段用于标识教室内桌的类型。可以为空，如果提供必须符合UUID格式（不带短横线）。
     * 该字段通过正则表达式 {@code StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION_ABLE_EMPTY} 进行验证。
     * 桌椅类型可能包括：固定排座、可移动桌椅、会议式桌椅等，通过UUID关联到桌椅类型表中的具体类型。
     * 例如，类型的UUID为3fa85f6457174562b3fc2c963f66afa6（不带短横线，小写）。
     */
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION_ABLE_EMPTY, message = "桌椅类型输入有误")
    private String tablesChairsType;
}
