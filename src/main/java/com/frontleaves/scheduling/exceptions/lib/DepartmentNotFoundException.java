package com.frontleaves.scheduling.exceptions.lib;

/**
 * 学院未找到异常
 * @author FLASHLACK
 */
public class DepartmentNotFoundException extends RuntimeException {
    public DepartmentNotFoundException(String message) {
        super(message);
    }
}