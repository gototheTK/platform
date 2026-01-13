package app.project.platform.exception;

import app.project.platform.domain.code.ErrorCode;
import lombok.Getter;

// 비지니스 로직 예외
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException (ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

}
