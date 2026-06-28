package com.zhihuitong.security.service;

import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.modules.system.entity.SysDept;
import com.zhihuitong.modules.system.entity.SysRole;
import com.zhihuitong.modules.system.entity.SysRoleDept;
import com.zhihuitong.modules.system.mapper.SysDeptMapper;
import com.zhihuitong.modules.system.mapper.SysRoleDeptMapper;
import com.zhihuitong.modules.system.mapper.SysRoleMapper;
import com.zhihuitong.security.model.LoginPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataScopeServiceTest {
    @Mock private PermissionService permissionService;
    @Mock private SysRoleMapper roleMapper;
    @Mock private SysRoleDeptMapper roleDeptMapper;
    @Mock private SysDeptMapper deptMapper;
    private DataScopeService service;

    @BeforeEach
    void setUp() {
        service = new DataScopeService(permissionService, roleMapper, roleDeptMapper, deptMapper);
    }

    @Test
    void adminCanAccessAnyOwnership() {
        when(permissionService.currentPrincipal()).thenReturn(principal(1L, 100L, 1L, "admin"));
        assertThatCode(() -> service.assertAccessible(999L, 999L)).doesNotThrowAnyException();
    }

    @Test
    void selfScopeOnlyAllowsCreator() {
        when(permissionService.currentPrincipal()).thenReturn(principal(6L, 115L, 6L, "qc_inspector"));
        when(roleMapper.selectById(6L)).thenReturn(role("SELF"));
        assertThatCode(() -> service.assertAccessible(999L, 6L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.assertAccessible(115L, 7L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void departmentScopeRejectsOtherDepartment() {
        when(permissionService.currentPrincipal()).thenReturn(principal(2L, 111L, 2L, "order_manager"));
        when(roleMapper.selectById(2L)).thenReturn(role("DEPT"));
        assertThatCode(() -> service.assertAccessible(111L, 8L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.assertAccessible(112L, 2L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void departmentAndChildrenScopeAllowsDescendantDepartment() {
        when(permissionService.currentPrincipal()).thenReturn(principal(5L, 110L, 5L, "scheduler"));
        when(roleMapper.selectById(5L)).thenReturn(role("DEPT_AND_CHILD"));
        SysDept child = new SysDept();
        child.setDeptId(114L);
        when(deptMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(java.util.List.of(child));

        assertThatCode(() -> service.assertAccessible(114L, 8L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.assertAccessible(115L, 8L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void customScopeUsesConfiguredDepartmentsOnly() {
        when(permissionService.currentPrincipal()).thenReturn(principal(7L, 115L, 7L, "qc_manager"));
        when(roleMapper.selectById(7L)).thenReturn(role("CUSTOM"));
        SysRoleDept relation = new SysRoleDept();
        relation.setRoleId(7L);
        relation.setDeptId(112L);
        when(roleDeptMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(java.util.List.of(relation));

        assertThatCode(() -> service.assertAccessible(112L, 8L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.assertAccessible(115L, 8L)).isInstanceOf(BusinessException.class);
    }

    private LoginPrincipal principal(Long userId, Long deptId, Long roleId, String roleKey) {
        return new LoginPrincipal(userId, deptId, 1L, roleId, "user", "User", null, roleKey, Set.of(), Set.of(), true);
    }

    private SysRole role(String scope) {
        SysRole role = new SysRole();
        role.setRoleId(2L);
        role.setStatus("0");
        role.setDataScope(scope);
        return role;
    }
}