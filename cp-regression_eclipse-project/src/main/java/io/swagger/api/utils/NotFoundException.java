package io.swagger.api.utils;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-10-06T12:54:15.929Z")
public class NotFoundException extends ApiException {
	private static final long serialVersionUID = -8000903005649088467L;
	@SuppressWarnings("unused")
	private int code;
    public NotFoundException (int code, String msg) {
        super(code, msg);
        this.code = code;
    }
}
