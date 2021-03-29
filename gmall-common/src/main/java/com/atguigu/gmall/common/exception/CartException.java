package com.atguigu.gmall.common.exception;

/**
 * description:
 *
 * @author Ice on 2021/3/29 in 0:09
 */
public class CartException extends RuntimeException {

    public CartException() {
        super();
    }

    public CartException(String message) {
        super(message);
    }
}
