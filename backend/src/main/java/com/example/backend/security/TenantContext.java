package com.example.backend.security;

public final class TenantContext {

    private static final ThreadLocal<CurrentUser> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(CurrentUser principal) {
        CURRENT.set(principal);
    }

    public static CurrentUser get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
