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

package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.daos.ClassroomDAO;
import com.frontleaves.scheduling.daos.ClassroomTagDAO;
import com.frontleaves.scheduling.daos.ClassroomTypeDAO;
import com.frontleaves.scheduling.models.dto.ClassroomTagDTO;
import com.frontleaves.scheduling.models.dto.ClassroomTypeDTO;
import com.frontleaves.scheduling.models.entity.ClassroomTagDO;
import com.frontleaves.scheduling.models.entity.ClassroomTypeDO;
import com.frontleaves.scheduling.services.ClassroomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 教室逻辑处理类，实现了 {@code ClassroomService} 接口。
 * <p>
 * 该类提供了教室管理相关的具体实现，包括添加教室、删除教室、查询教室信息等操作。通过依赖注入的方式，可以与其他服务进行交互，完成复杂的业务逻辑。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Service
@RequiredArgsConstructor
public class ClassroomLogic implements ClassroomService {
    private final ClassroomTagDAO classroomTagDAO;
    private final ClassroomTypeDAO classroomTypeDAO;
    private final ClassroomDAO classroomDAO;

    /**
     * 获取教室标签列表
     * <p>
     * 该方法用于从数据库中获取所有教室标签，并将其转换为 {@code ClassroomTagDTO} 对象的列表返回。
     * 通过调用 {@code classroomTagDAO.getTags()} 方法获取数据，
     * 然后使用 Hutool 的 {@code BeanUtil.copyToList} 方法进行对象转换。
     * </p>
     *
     * @return 返回包含所有教室标签的 {@code List<ClassroomTagDTO>} 对象
     */
    @Override
    public List<ClassroomTagDTO> listClassroomTags() {
        List<ClassroomTagDO> tags = classroomTagDAO.getTags();
        return BeanUtil.copyToList(tags, ClassroomTagDTO.class);
    }

    /**
     * 获取教室类型列表
     * <p>
     * 该方法用于从数据库中获取所有教室类型，并将其转换为 {@code ClassroomTypeDTO} 对象的列表返回。
     * 通过调用 {@code classroomTypeDAO.getTypes()} 方法获取数据，
     * 然后使用 Hutool 的 {@code BeanUtil.copyToList} 方法进行对象转换。
     * </p>
     *
     * @return 返回包含所有教室类型的 {@code List<ClassroomTypeDTO>} 对象
     */
    @Override
    public List<ClassroomTypeDTO> listClassroomTypes() {
        List<ClassroomTypeDO> types = classroomTypeDAO.getTypes();
        return BeanUtil.copyToList(types, ClassroomTypeDTO.class);
    }
}
