public class APIException extends RuntimeException {

	String url;
	int code;
	Object response;

	public APIException(String url, int code, Object res) {
		this.url = url;
		this.code = code;
		this.response = res;
	}
	
	public String toString() {
		return "APIException: " + code + " " + response + " on " + url;
	}

}
