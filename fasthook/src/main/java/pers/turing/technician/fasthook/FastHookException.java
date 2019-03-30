package pers.turing.technician.fasthook;

public class FastHookException extends Exception {
    public FastHookException() {
        super();
    }

    public FastHookException(String message) {
        super(message);
    }

    public FastHookException(String message, Throwable cause) {
        super(message, cause);
    }

    public FastHookException(Throwable cause) {
        super(cause);
    }
}
