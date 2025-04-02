package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class PrepareCourseDTO {
    /**
     * 课程类别和属性信息列表
     */
    private List<CourseInfo> courseInfoList;

    /**
     * 课程类别列表
     */
    private List<CategoryInfo> categoryList;

    /**
     * 课程属性列表
     */
    private List<PropertyInfo> propertyList;

    /**
     * 课程类型列表
     */
    private List<TypeInfo> typeList;

    /**
     * 课程性质列表
     */
    private List<NatureInfo> natureList;

    /**
     * 部门列表
     */
    private List<DepartmentInfo> departmentList;

    /**
     * 教室类型列表
     */
    private List<ClassroomTypeInfo> classroomTypeList;

    @Data
    @Accessors(chain = true)
    public static class CourseInfo {
        /**
         * 课程类别名称
         */
        private String categoryName;

        /**
         * 课程属性名称
         */
        private String propertyName;
    }

    @Data
    @Accessors(chain = true)
    public static class CategoryInfo {
        /**
         * 课程类别UUID
         */
        private String uuid;

        /**
         * 课程类别名称
         */
        private String name;
    }

    @Data
    @Accessors(chain = true)
    public static class PropertyInfo {
        /**
         * 课程属性UUID
         */
        private String uuid;

        /**
         * 课程属性名称
         */
        private String name;
    }

    @Data
    @Accessors(chain = true)
    public static class TypeInfo {
        /**
         * 课程类型UUID
         */
        private String uuid;

        /**
         * 课程类型名称
         */
        private String name;
    }

    @Data
    @Accessors(chain = true)
    public static class NatureInfo {
        /**
         * 课程性质UUID
         */
        private String uuid;

        /**
         * 课程性质名称
         */
        private String name;
    }

    @Data
    @Accessors(chain = true)
    public static class DepartmentInfo {
        /**
         * 部门UUID
         */
        private String uuid;

        /**
         * 部门名称
         */
        private String name;
    }

    @Data
    @Accessors(chain = true)
    public static class ClassroomTypeInfo {
        /**
         * 教室类型UUID
         */
        private String uuid;

        /**
         * 教室类型名称
         */
        private String name;
    }
}
