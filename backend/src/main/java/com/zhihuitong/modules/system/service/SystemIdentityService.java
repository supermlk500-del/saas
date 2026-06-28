package com.zhihuitong.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.modules.system.entity.SysMenu;
import com.zhihuitong.modules.system.entity.SysRole;
import com.zhihuitong.modules.system.entity.SysRoleMenu;
import com.zhihuitong.modules.system.entity.SysUser;
import com.zhihuitong.modules.system.mapper.SysMenuMapper;
import com.zhihuitong.modules.system.mapper.SysRoleMapper;
import com.zhihuitong.modules.system.mapper.SysRoleMenuMapper;
import com.zhihuitong.modules.system.mapper.SysUserMapper;
import com.zhihuitong.security.model.LoginPrincipal;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class SystemIdentityService {
    public static final String SUPER_PERMISSION = "*:*:*";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    public SystemIdentityService(SysUserMapper userMapper, SysRoleMapper roleMapper,
                                 SysRoleMenuMapper roleMenuMapper, SysMenuMapper menuMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
    }

    public SysUser findByUsername(String username) {
        return userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUserName, username)
                .last("limit 1"));
    }

    public SysUser requireUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    public SysRole requireRole(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        return role;
    }

    public LoginPrincipal buildPrincipal(SysUser user) {
        SysRole role = requireRole(user.getRoleId());
        boolean enabled = "0".equals(user.getStatus()) && "0".equals(role.getStatus());
        Set<String> permissions = new LinkedHashSet<>();
        Set<String> menuKeys = new LinkedHashSet<>();
        if ("admin".equals(role.getRoleKey())) {
            permissions.add(SUPER_PERMISSION);
            menuKeys.add("*");
        } else {
            List<Long> menuIds = roleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery()
                            .eq(SysRoleMenu::getRoleId, role.getRoleId()))
                    .stream().map(SysRoleMenu::getMenuId).toList();
            if (!menuIds.isEmpty()) {
                menuMapper.selectBatchIds(menuIds).stream()
                        .filter(menu -> "0".equals(menu.getStatus()))
                        .forEach(menu -> collectMenuAccess(menu, permissions, menuKeys));
            }
        }
        return new LoginPrincipal(user.getUserId(), user.getDeptId(), user.getPostId(), user.getRoleId(),
                user.getUserName(), user.getNickName(), user.getPassword(), role.getRoleKey(), permissions,
                menuKeys, enabled);
    }

    private void collectMenuAccess(SysMenu menu, Set<String> permissions, Set<String> menuKeys) {
        if (menu.getPerms() != null && !menu.getPerms().isBlank()) {
            permissions.add(menu.getPerms().trim());
        }
        if (menu.getMenuKey() != null && !menu.getMenuKey().isBlank() && !"F".equals(menu.getMenuType())) {
            menuKeys.add(menu.getMenuKey());
        }
    }
}
