package com.frontleaves.scheduling.exceptions.lib;

/**
 * 专业未找到异常
 * @author FLASHLACK
 */
public class MajorNotFoundException extends  RuntimeException{
    public MajorNotFoundException(String message) {
        super(message);
    }
}
