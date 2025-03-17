package com.frontleaves.scheduling.models.dto;

import lombok.Data;

import java.util.List;

public class BackAddBuildingDTO {
    /**
     * 总记录数
     */
    private int totalCount;

    /**
     * 成功记录数
     */
    private int successCount;

    /**
     * 失败记录数
     */
    private int failedCount;

    /**
     * 失败详情列表
     */
    private List<BackAddStudentDTO.FailedDetail> failedDetails;

    /**
     * 失败详情内部类
     */
    @Data
    public static class FailedDetail {
        /**
         * 失败行号
         */
        private int row;

        /**
         * 失败原因
         */
        private String reason;
    }
}
