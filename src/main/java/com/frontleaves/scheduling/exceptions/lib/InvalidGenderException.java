package com.frontleaves.scheduling.exceptions.lib;

/**
 * 无效性别异常
 * @author FLASHLACK
 */
public class InvalidGenderException extends RuntimeException{
    public InvalidGenderException(String message) {
        super(message);
    }
}
