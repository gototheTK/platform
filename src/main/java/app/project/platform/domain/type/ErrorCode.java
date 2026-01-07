package app.project.platform.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // =================================================================
    // 1. Common (공통 에러) - C001 ~ C099
    // =================================================================
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 HTTP 메서드 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C004", "값의 타입이 올바르지 않습니다."),

    // =================================================================
    // 2. Member (회원 관련) - M001 ~ M099
    // =================================================================
    // 회원가입
    EMAIL_DUPLICATION(HttpStatus.BAD_REQUEST, "M001", "이미 가입된 이메일입니다."),
    NICKNAME_DUPLICATION(HttpStatus.BAD_REQUEST, "M002", "이미 사용 중인 닉네임입니다."),

    // 로그인 & 인증
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M003", "존재하지 않는 회원입니다."),
    LOGIN_FAILED(HttpStatus.BAD_REQUEST, "M004", "아이디 또는 비밀번호가 일치하지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "M005", "로그인이 필요한 서비스입니다."),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "M006", "올바른 이메일 형식이 아닙니다."),

    // =================================================================
    // 3. Content (게시글/컨텐츠 관련) - P001 ~ P099
    // =================================================================
    CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "해당 게시글을 찾을 수 없습니다."),
    CONTENT_WRITER_MISMATCH(HttpStatus.FORBIDDEN, "P002", "해당 게시글을 수정/삭제할 권한이 없습니다."),
    ALREADY_DELETED_CONTENT(HttpStatus.BAD_REQUEST, "P003", "이미 삭제된 게시글입니다.");

    // 필드 정의
    private final HttpStatus status; // HTTP 상태 코드 (200, 400, 404 등)
    private final String code;       // 우리가 정의한 고유 코드
    private final String message;    // 프론트엔드에 보여줄 메시지
}
