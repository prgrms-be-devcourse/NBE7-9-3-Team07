package com.back.pinco.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 공통 0000번대
    SUCCESS(200, HttpStatus.OK, "성공적으로 처리되었습니다."),
    INVALID_VALUE(400, HttpStatus.BAD_REQUEST, "입력값이 잘못되었습니다."),

    // PIN 도메인_1000번대
    INVALID_PIN_INPUT(1001, HttpStatus.BAD_REQUEST, "잘못된 핀 입력값입니다."),
    PIN_NOT_FOUND(1002, HttpStatus.NOT_FOUND, "존재하지 않는 핀입니다."),
    PINS_NOT_FOUND(1003, HttpStatus.NOT_FOUND, "해당 조건에 일치하는 핀이 없습니다."),
    PIN_CREATE_FAILED(1004, HttpStatus.INTERNAL_SERVER_ERROR, "핀 생성 중 오류가 발생했습니다."),
    INVALID_PIN_CONTENT(1005, HttpStatus.BAD_REQUEST, "내용을 입력해주세요."),
    INVALID_PIN_LATITUDE(1006, HttpStatus.BAD_REQUEST, "latitude를 입력해주세요."),
    INVALID_PIN_LONGITUDE(1007, HttpStatus.BAD_REQUEST, "longitude를 입력해주세요."),
    PIN_UPDATE_FAILED(1008, HttpStatus.INTERNAL_SERVER_ERROR, "핀 수정 중 오류가 발생했습니다."),
    PIN_DELETE_FAILED(1009, HttpStatus.INTERNAL_SERVER_ERROR, "핀 삭제 중 오류가 발생했습니다."),
    PIN_NO_PERMISSION(1010, HttpStatus.FORBIDDEN, "핀 생성,수정 권한이 없습니다."),

    // User 도메인_2000번대
    INVALID_EMAIL_FORMAT(2001, HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."),
    INVALID_PASSWORD_FORMAT(2002, HttpStatus.BAD_REQUEST, "비밀번호 형식을 만족하지 않습니다."),
    INVALID_USERNAME_FORMAT(2003, HttpStatus.BAD_REQUEST, "회원 이름 형식을 만족하지 않습니다."),
    EMAIL_ALREADY_EXISTS(2004, HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(2005, HttpStatus.CONFLICT, "이미 존재하는 회원이름입니다."),
    USER_NOT_FOUND(2006, HttpStatus.NOT_FOUND, "존재하지 않는 이메일입니다."),
    PASSWORD_NOT_MATCH(2007, HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    USER_INFO_NOT_FOUND(2008, HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."),
    CURRENT_PASSWORD_REQUIRED(2009, HttpStatus.BAD_REQUEST, "현재 비밀번호가 필요합니다."),
    CURRENT_PASSWORD_NOT_MATCH(2010, HttpStatus.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다."),
    NO_FIELDS_TO_UPDATE(2011, HttpStatus.BAD_REQUEST, "변경할 내용이 없습니다."),
    INVALID_API_KEY(2012, HttpStatus.UNAUTHORIZED, "API 키가 유효하지 않습니다."),
    INVALID_ACCESS_TOKEN(2013, HttpStatus.UNAUTHORIZED, "Access Token이 유효하지 않습니다."),
    AUTH_REQUIRED(2014, HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    TOKEN_EXPIRED(2015, HttpStatus.UNAUTHORIZED, "Access Token이 만료되었습니다."),
    ACCESS_DENIED(2016, HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // Tag 도메인_3000번대
    TAG_NOT_FOUND(3001, HttpStatus.NOT_FOUND, "존재하지 않는 태그입니다."),
    TAG_ALREADY_EXISTS(3002, HttpStatus.CONFLICT, "이미 존재하는 태그입니다."),
    TAG_LINK_NOT_FOUND(3003, HttpStatus.NOT_FOUND, "태그 연결이 존재하지 않습니다."),
    TAG_ALREADY_LINKED(3004, HttpStatus.CONFLICT, "이미 이 핀에 연결된 태그입니다."),
    TAG_CREATE_FAILED(3005, HttpStatus.INTERNAL_SERVER_ERROR, "태그 생성 중 오류가 발생했습니다."),
    TAG_PIN_NOT_FOUND(3006, HttpStatus.NOT_FOUND, "핀을 찾을 수 없습니다."),
    PIN_TAG_LIST_EMPTY(3007, HttpStatus.NOT_FOUND, "해당 핀에 연결된 태그가 없습니다."),
    PIN_TAG_DELETE_FAILED(3008, HttpStatus.INTERNAL_SERVER_ERROR, "태그 삭제 중 오류가 발생했습니다."),
    PIN_TAG_RESTORE_FAILED(3009, HttpStatus.INTERNAL_SERVER_ERROR, "태그 복구 중 오류가 발생했습니다."),
    INVALID_TAG_KEYWORD(3010, HttpStatus.BAD_REQUEST, "태그 키워드를 입력해주세요."),
    INVALID_TAG_INPUT(3011, HttpStatus.BAD_REQUEST, "잘못된 태그 입력값입니다."),
    TAG_POSTS_NOT_FOUND(3012, HttpStatus.NOT_FOUND, "해당 태그가 달린 게시물이 없습니다."),

    // Bookmark 도메인_4000번대
    BOOKMARK_NOT_FOUND(4001, HttpStatus.NOT_FOUND, "존재하지 않는 북마크입니다."),
    BOOKMARK_ALREADY_EXISTS(4002, HttpStatus.CONFLICT, "이미 북마크된 핀입니다."),
    BOOKMARKS_NOT_FOUND(4003, HttpStatus.NO_CONTENT, "조회된 북마크 목록이 없습니다."),
    BOOKMARK_CREATE_FAILED(4004, HttpStatus.INTERNAL_SERVER_ERROR, "북마크 생성 중 오류가 발생했습니다."),
    BOOKMARK_DELETE_FAILED(4005, HttpStatus.INTERNAL_SERVER_ERROR, "북마크 삭제 중 오류가 발생했습니다."),
    BOOKMARK_RESTORE_FAILED(4006, HttpStatus.INTERNAL_SERVER_ERROR, "북마크 복구 중 오류가 발생했습니다."),
    BOOKMARK_INVALID_USER_INPUT(4007, HttpStatus.NOT_FOUND, "잘못된 사용자 정보입니다."),

    // Likes 도메인_5000번대
    LIKES_INVALID_USER_INPUT(5001, HttpStatus.NOT_FOUND, "잘못된 사용자 정보입니다."),
    LIKES_INVALID_PIN_INPUT(5002, HttpStatus.NOT_FOUND, "잘못된 핀 정보입니다."),
    LIKES_CREATE_FAILED(5003, HttpStatus.INTERNAL_SERVER_ERROR, "좋아요 등록 중 오류가 발생했습니다."),
    LIKES_REVOKE_FAILED(5004, HttpStatus.INTERNAL_SERVER_ERROR, "좋아요 취소 중 오류가 발생했습니다."),
    LIKES_UPDATE_PIN_FAILED(5005, HttpStatus.NOT_FOUND, "좋아요 갱신 중 오류가 발생했습니다."),
    LIKES_NOT_FOUND(5006, HttpStatus.NOT_FOUND, "존재하지 않는 좋아요입니다."),
    ;



    private final int code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }


}