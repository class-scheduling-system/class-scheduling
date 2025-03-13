package com.frontleaves.scheduling.exceptions.lib;

/**
 * 年级未找到异常
 * @author FLASHALCK
 */
public class GradeNotFoundException extends  RuntimeException{
    public GradeNotFoundException(String message) {
        super(message);
    }
}
