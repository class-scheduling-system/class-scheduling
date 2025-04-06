package com.frontleaves.scheduling.models.dto;

import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.StudentDTO;
import com.frontleaves.scheduling.models.entity.AdministrativeClassDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class PageWithClassListDTO {

    /**
     * 学生分页信息
     */
    private PageDTO<StudentDTO> studentPage;

    /**
     * 班级列表
     */
    private List<AdministrativeClassDO> classList;
}
