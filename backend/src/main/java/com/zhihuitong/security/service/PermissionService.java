package com.zhihuitong.security.service;

import com.zhihuitong.security.model.LoginPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("auth")
public class PermissionService {
    public boolean hasPermission(String permission) {
        LoginPrincipal principal = currentPrincipal();
        return principal != null && (principal.getPermissions().contains("*:*:*")
                || principal.getPermissions().contains(permission));
    }

    public boolean hasAnyPermission(String... permissions) {
        for (String permission : permissions) {
            if (hasPermission(permission)) return true;
        }
        return false;
    }

    public boolean hasRole(String roleKey) {
        LoginPrincipal principal = currentPrincipal();
        return principal != null && ("admin".equals(principal.getRoleKey()) || roleKey.equals(principal.getRoleKey()));
    }

    public LoginPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof LoginPrincipal principal ? principal : null;
    }

    public Long currentUserId() {
        LoginPrincipal principal = currentPrincipal();
        return principal == null ? null : principal.getUserId();
    }
}
