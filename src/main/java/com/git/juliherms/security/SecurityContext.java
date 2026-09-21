package com.git.juliherms.security;

/**
 * Classe de contexto de segurança para armazenar informações do usuário atual.
 * Utiliza ThreadLocal para manter o contexto de segurança por thread.
 */
public class SecurityContext {

    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();

    public static void setCurrentUser(String user) {
        currentUser.set(user);
    }

    public static String getCurrentUser() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }
}
