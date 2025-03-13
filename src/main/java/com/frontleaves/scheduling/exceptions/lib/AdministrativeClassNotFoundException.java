package com.frontleaves.scheduling.exceptions.lib;

/**
 * 行政班未找到异常
 * @author FLASHLACK
 */
public class AdministrativeClassNotFoundException extends RuntimeException{
    public AdministrativeClassNotFoundException(String message) {
        super(message);
    }
}
