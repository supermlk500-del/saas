package com.zhihuitong.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.modules.system.entity.SysDept;
import com.zhihuitong.modules.system.entity.SysRole;
import com.zhihuitong.modules.system.entity.SysRoleDept;
import com.zhihuitong.modules.system.entity.SysUser;
import com.zhihuitong.modules.system.mapper.SysDeptMapper;
import com.zhihuitong.modules.system.mapper.SysRoleDeptMapper;
import com.zhihuitong.modules.system.mapper.SysRoleMapper;
import com.zhihuitong.security.model.LoginPrincipal;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class DataScopeService {
    private final PermissionService permissionService;
    private final SysRoleMapper roleMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysDeptMapper deptMapper;

    public DataScopeService(PermissionService permissionService, SysRoleMapper roleMapper,
                            SysRoleDeptMapper roleDeptMapper, SysDeptMapper deptMapper) {
        this.permissionService = permissionService;
        this.roleMapper = roleMapper;
        this.roleDeptMapper = roleDeptMapper;
        this.deptMapper = deptMapper;
    }

    public <T> LambdaQueryWrapper<T> apply(LambdaQueryWrapper<T> wrapper,
                                            SFunction<T, Long> deptColumn,
                                            SFunction<T, Long> createdByColumn) {
        Scope scope = currentScope();
        if (scope.unrestricted()) return wrapper;
        if (scope.selfOnly()) return wrapper.eq(createdByColumn, scope.userId());
        if (scope.departmentIds().isEmpty()) return wrapper.apply("1 = 0");
        return wrapper.in(deptColumn, scope.departmentIds());
    }

    public LambdaQueryWrapper<SysUser> applyUserScope(LambdaQueryWrapper<SysUser> wrapper) {
        Scope scope = currentScope();
        if (scope.unrestricted()) return wrapper;
        if (scope.selfOnly()) return wrapper.eq(SysUser::getUserId, scope.userId());
        if (scope.departmentIds().isEmpty()) return wrapper.apply("1 = 0");
        return wrapper.in(SysUser::getDeptId, scope.departmentIds());
    }

    public void assertUserAccessible(Long userId, Long deptId) {
        Scope scope = currentScope();
        if (scope.unrestricted()) return;
        boolean allowed = scope.selfOnly()
                ? Objects.equals(userId, scope.userId())
                : scope.departmentIds().contains(deptId);
        if (!allowed) throw new BusinessException(403, "无权访问该用户");
    }

    public void assertDepartmentAssignable(Long deptId) {
        Scope scope = currentScope();
        if (scope.unrestricted()) return;
        if (scope.selfOnly() || !scope.departmentIds().contains(deptId)) {
            throw new BusinessException(403, "无权在该部门创建或移动用户");
        }
    }
    public void assertAccessible(Long deptId, Long createdBy) {
        Scope scope = currentScope();
        if (scope.unrestricted()) return;
        boolean allowed = scope.selfOnly()
                ? Objects.equals(createdBy, scope.userId())
                : scope.departmentIds().contains(deptId);
        if (!allowed) throw new BusinessException(403, "无权访问该数据");
    }

    public Long currentUserId() {
        LoginPrincipal principal = requirePrincipal();
        return principal.getUserId();
    }

    public Long currentDeptId() {
        LoginPrincipal principal = requirePrincipal();
        return principal.getDeptId();
    }

    private Scope currentScope() {
        LoginPrincipal principal = requirePrincipal();
        if ("admin".equals(principal.getRoleKey())) {
            return new Scope(principal.getUserId(), true, false, Set.of());
        }
        SysRole role = roleMapper.selectById(principal.getRoleId());
        if (role == null || !"0".equals(role.getStatus())) throw new BusinessException(403, "当前角色不可用");
        return switch (role.getDataScope()) {
            case "ALL" -> new Scope(principal.getUserId(), true, false, Set.of());
            case "SELF" -> new Scope(principal.getUserId(), false, true, Set.of());
            case "CUSTOM" -> new Scope(principal.getUserId(), false, false, customDepartments(role.getRoleId()));
            case "DEPT_AND_CHILD" -> new Scope(principal.getUserId(), false, false, departmentAndChildren(principal.getDeptId()));
            case "DEPT" -> new Scope(principal.getUserId(), false, false, Set.of(principal.getDeptId()));
            default -> throw new BusinessException(403, "角色数据范围配置无效");
        };
    }

    private Set<Long> customDepartments(Long roleId) {
        List<SysRoleDept> relations = roleDeptMapper.selectList(Wrappers.<SysRoleDept>lambdaQuery().eq(SysRoleDept::getRoleId, roleId));
        return relations.stream().map(SysRoleDept::getDeptId).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> departmentAndChildren(Long deptId) {
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(deptId);
        deptMapper.selectList(Wrappers.<SysDept>lambdaQuery().apply("FIND_IN_SET({0}, ancestors)", deptId))
                .forEach(dept -> ids.add(dept.getDeptId()));
        return ids;
    }

    private LoginPrincipal requirePrincipal() {
        LoginPrincipal principal = permissionService.currentPrincipal();
        if (principal == null) throw new BusinessException(401, "未登录");
        return principal;
    }

    private record Scope(Long userId, boolean unrestricted, boolean selfOnly, Set<Long> departmentIds) {}
}