package com.arosbio.api.utils;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-10-06T12:54:15.929Z")
public class ApiException extends Exception{
	private static final long serialVersionUID = -2174637728997742359L;
	@SuppressWarnings("unused")
	private int code;
    public ApiException (int code, String msg) {
        super(msg);
        this.code = code;
    }
}
