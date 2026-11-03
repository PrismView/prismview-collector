package com.prismview.collector.okx;

/**
 * OKX가 업무 오류(code != "0")를 돌려줬을 때.
 * HTTP 200이어도 발생할 수 있다.
 */
public class OkxApiException extends RuntimeException {

    private final String code;

    public OkxApiException(String code, String msg) {
        super("OKX API 오류: code=" + code + ", msg=" + msg);
        this.code = code;
    }

    public String code() {
        return code;
    }
}