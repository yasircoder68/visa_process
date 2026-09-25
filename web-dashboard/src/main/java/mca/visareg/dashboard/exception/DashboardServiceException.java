package mca.visareg.dashboard.exception;

public class DashboardServiceException extends RuntimeException {
    public DashboardServiceException(String message) {
        super(message);
    }

    public DashboardServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
