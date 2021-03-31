package com.atguigu.gmall.common.exception;

/**
 * description:
 *
 * @author Ice on 2021/3/31 in 18:54
 */
public class OrderException extends RuntimeException {

    public OrderException() {
        super();
    }

    public OrderException(String message) {
        super(message);
    }
}
