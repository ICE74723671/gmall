package com.atguigu.gmall.common.exception;

import com.atguigu.gmall.common.result.ResultCodeEnum;
import lombok.Data;

/**
 * description:
 *
 * @author Ice on 2021/2/2 in 19:33
 */
@Data
public class GuliException extends RuntimeException{
    private Integer code;
    /**
     * 接受状态码和消息
     * @param code
     * @param message
     */
    public GuliException(Integer code, String message){
        super(message);
        this.code=code;
    }

    /**
     * 接收枚举类型
     * @param resultCodeEnum
     */
    public GuliException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
    }

    @Override
    public String toString(){
        return "GuliException{" +
                "code=" + code +
                ", message=" + this.getMessage() +
                '}';
    }
}
