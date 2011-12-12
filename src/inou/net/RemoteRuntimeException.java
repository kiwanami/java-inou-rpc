package inou.net;


public class RemoteRuntimeException extends RuntimeException {

	private String klass;
	private String customMessage;
	private String detail;

	public RemoteRuntimeException(String klass,String message,String detail) {
		super("class="+klass+"  message="+message);
		this.klass = klass;
		this.customMessage = message;
		this.detail = detail;
	}

	public String getExceptionClass() {
		return klass;
	}

	public String getDetail() {
		return detail;
	}
    
    public String getCustomMessage() {
        return customMessage;
    }
    
}