package com.yayfolk.backend.dto;

import lombok.Data;

@Data
public class ResponseDto {
    private int code;
    private String message;
    private Object data;

    public static ResponseDto success(Object data) {
        ResponseDto response = new ResponseDto();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    public static ResponseDto error(int code, String message) {
        ResponseDto response = new ResponseDto();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
}
