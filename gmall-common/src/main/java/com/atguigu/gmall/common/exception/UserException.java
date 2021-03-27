package com.atguigu.gmall.common.exception;

/**
 * description:
 *
 * @author Ice on 2021/3/27 in 14:13
 */
public class UserException extends RuntimeException {
    public UserException() {
        super();
    }

    public UserException(String message) {
        super(message);
    }
}
