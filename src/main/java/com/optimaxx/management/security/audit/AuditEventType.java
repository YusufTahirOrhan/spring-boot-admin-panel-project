package com.optimaxx.management.security.audit;

public enum AuditEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGIN_BLOCKED,
    TOKEN_REFRESHED,
    LOGOUT,
    PASSWORD_CHANGED,
    USER_CREATED,
    USER_ROLE_UPDATED,
    USER_STATUS_UPDATED,
    USER_SOFT_DELETED
}
