package com.frontleaves.scheduling.models.vo;

/**
 * @author fanfan187
 */

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseWrapperVO<T> {
    private String output;
    private Integer code;
    private String message;
    private T data;
}


