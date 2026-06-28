package com.zhihuitong.security.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

public class LoginPrincipal implements UserDetails {
    private final Long userId;
    private final Long deptId;
    private final Long postId;
    private final Long roleId;
    private final String username;
    private final String nickname;
    private final String password;
    private final String roleKey;
    private final Set<String> permissions;
    private final Set<String> menuKeys;
    private final boolean enabled;

    public LoginPrincipal(Long userId, Long deptId, Long postId, Long roleId, String username, String nickname,
                          String password, String roleKey, Set<String> permissions, Set<String> menuKeys, boolean enabled) {
        this.userId = userId;
        this.deptId = deptId;
        this.postId = postId;
        this.roleId = roleId;
        this.username = username;
        this.nickname = nickname;
        this.password = password;
        this.roleKey = roleKey;
        this.permissions = permissions;
        this.menuKeys = menuKeys;
        this.enabled = enabled;
    }

    public static LoginPrincipal fromSession(LoginSession session) {
        return new LoginPrincipal(session.getUserId(), session.getDeptId(), session.getPostId(), session.getRoleId(),
                session.getUsername(), session.getNickname(), null, session.getRoleKey(), session.getPermissions(),
                session.getMenuKeys(), true);
    }

    public Long getUserId() { return userId; }
    public Long getDeptId() { return deptId; }
    public Long getPostId() { return postId; }
    public Long getRoleId() { return roleId; }
    public String getNickname() { return nickname; }
    public String getRoleKey() { return roleKey; }
    public Set<String> getPermissions() { return permissions; }
    public Set<String> getMenuKeys() { return menuKeys; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions.stream().map(SimpleGrantedAuthority::new).toList();
    }

    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }
}
