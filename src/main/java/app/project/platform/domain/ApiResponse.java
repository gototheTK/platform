package app.project.platform.domain;

import app.project.platform.domain.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ApiResponse<T> {

    private final String status;

    private final String message;

    private final T data;

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>("fail", errorCode.getMessage(), null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, T data) {
        return new ApiResponse<>("fail", errorCode.getMessage(), data);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", null, data);
    }

}
