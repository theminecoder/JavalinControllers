package me.theminecoder.web.javalin;

public class JavalinControllerException extends RuntimeException {

    public JavalinControllerException(String message) {
        super(message);
    }

    public JavalinControllerException(String message, Throwable cause) {
        super(message, cause);
    }

    public JavalinControllerException(Throwable cause) {
        super(cause);
    }
}
