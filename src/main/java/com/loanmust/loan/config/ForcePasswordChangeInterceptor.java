package com.loanmust.loan.config;

import com.loanmust.loan.security.CustomUserDetails;
import com.loanmust.loan.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * A user created by an administrator lands on a temporary password. Until they change
 * it, every page redirects to the change-password screen.
 */
@Component
public class ForcePasswordChangeInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        CustomUserDetails user = SecurityUtils.currentUser();
        if (user == null || !user.isMustChangePassword()) {
            return true;
        }
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        String path = uri.substring(ctx.length());
        if (path.startsWith("/account/password") || path.startsWith("/logout")
                || path.startsWith("/css") || path.startsWith("/js") || path.startsWith("/images")) {
            return true;
        }
        response.sendRedirect(ctx + "/account/password?required");
        return false;
    }
}
