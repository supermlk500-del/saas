package com.zhihuitong.security.service;

import com.zhihuitong.modules.system.entity.SysUser;
import com.zhihuitong.modules.system.service.SystemIdentityService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final SystemIdentityService identityService;

    public DatabaseUserDetailsService(SystemIdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = identityService.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        return identityService.buildPrincipal(user);
    }
}
