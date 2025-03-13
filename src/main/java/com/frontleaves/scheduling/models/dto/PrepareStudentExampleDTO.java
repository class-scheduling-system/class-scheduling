package com.frontleaves.scheduling.models.dto;

import com.frontleaves.scheduling.models.entity.GradeDO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 准备学生示例数据传输对象
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class PrepareStudentExampleDTO {
    private String departmentName;

    private List<ClassInfo> classInfoList;
    private List<GradeDO> gradeList;
    @Data
    @Accessors(chain = true)
    public static class ClassInfo {
        private String majorName;
        private String className;
    }
}
