package com.frontleaves.scheduling.exceptions.lib;

/**
 * 无效的 ID 异常
 * @author FLASHLAKC
 */
public class InvalidIdException extends RuntimeException {
    public InvalidIdException(String message) {
        super(message);
    }
}
