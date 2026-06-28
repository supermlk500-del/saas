package com.zhihuitong.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.system.dto.RoleStatusRequest;
import com.zhihuitong.modules.system.dto.SystemRoleQuery;
import com.zhihuitong.modules.system.dto.SystemRoleSaveRequest;
import com.zhihuitong.modules.system.entity.SysRole;
import com.zhihuitong.modules.system.entity.SysRoleDept;
import com.zhihuitong.modules.system.entity.SysRoleMenu;
import com.zhihuitong.modules.system.entity.SysUser;
import com.zhihuitong.modules.system.mapper.SysDeptMapper;
import com.zhihuitong.modules.system.mapper.SysMenuMapper;
import com.zhihuitong.modules.system.mapper.SysRoleDeptMapper;
import com.zhihuitong.modules.system.mapper.SysRoleMapper;
import com.zhihuitong.modules.system.mapper.SysRoleMenuMapper;
import com.zhihuitong.modules.system.mapper.SysUserMapper;
import com.zhihuitong.security.service.LoginSessionService;
import com.zhihuitong.security.service.PermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class SystemRoleService {
    private static final Long SUPER_ADMIN_ROLE_ID = 1L;
    private static final Set<String> DATA_SCOPES = Set.of("ALL", "CUSTOM", "DEPT", "DEPT_AND_CHILD", "SELF");

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysMenuMapper menuMapper;
    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;
    private final LoginSessionService sessionService;
    private final PermissionService permissionService;

    public SystemRoleService(SysRoleMapper roleMapper, SysRoleMenuMapper roleMenuMapper,
                             SysRoleDeptMapper roleDeptMapper, SysMenuMapper menuMapper,
                             SysDeptMapper deptMapper, SysUserMapper userMapper,
                             LoginSessionService sessionService, PermissionService permissionService) {
        this.roleMapper = roleMapper; this.roleMenuMapper = roleMenuMapper; this.roleDeptMapper = roleDeptMapper;
        this.menuMapper = menuMapper; this.deptMapper = deptMapper; this.userMapper = userMapper;
        this.sessionService = sessionService; this.permissionService = permissionService;
    }

    public TableDataInfo<SysRole> list(SystemRoleQuery query) {
        Page<SysRole> page = roleMapper.selectPage(query.toPage(), Wrappers.<SysRole>lambdaQuery()
                .like(StringUtils.hasText(query.getRoleName()), SysRole::getRoleName, query.getRoleName())
                .like(StringUtils.hasText(query.getRoleKey()), SysRole::getRoleKey, query.getRoleKey())
                .eq(StringUtils.hasText(query.getStatus()), SysRole::getStatus, query.getStatus())
                .orderByAsc(SysRole::getRoleSort));
        return TableDataInfoBuilder.build(page);
    }

    public SysRole detail(Long roleId) { return requireRole(roleId); }

    public List<Long> menuIds(Long roleId) {
        requireRole(roleId);
        return roleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }

    public List<Long> deptIds(Long roleId) {
        requireRole(roleId);
        return roleDeptMapper.selectList(Wrappers.<SysRoleDept>lambdaQuery().eq(SysRoleDept::getRoleId, roleId))
                .stream().map(SysRoleDept::getDeptId).toList();
    }

    @Transactional
    public SysRole create(SystemRoleSaveRequest request) {
        if ("admin".equalsIgnoreCase(request.getRoleKey())) throw new BusinessException(403, "超级管理员角色编码受保护");
        validate(request, null);
        SysRole role = new SysRole();
        copy(request, role);
        role.setStatus(defaultStatus(request.getStatus())); role.setDeleted(0);
        role.setCreatedBy(permissionService.currentUserId()); role.setCreateTime(LocalDateTime.now());
        roleMapper.insert(role);
        replaceRelations(role.getRoleId(), request.getMenuIds(), request.getDeptIds(), request.getDataScope());
        return role;
    }

    @Transactional
    public SysRole update(SystemRoleSaveRequest request) {
        if (request.getRoleId() == null) throw new BusinessException(400, "roleId 不能为空");
        assertNotProtected(request.getRoleId());
        SysRole role = requireRole(request.getRoleId());
        validate(request, role.getRoleId());
        copy(request, role);
        role.setUpdatedBy(permissionService.currentUserId()); role.setUpdateTime(LocalDateTime.now());
        roleMapper.updateById(role);
        replaceRelations(role.getRoleId(), request.getMenuIds(), request.getDeptIds(), request.getDataScope());
        invalidateRoleUsers(role.getRoleId());
        return role;
    }

    public void changeStatus(RoleStatusRequest request) {
        assertNotProtected(request.getRoleId()); assertStatus(request.getStatus());
        SysRole role = requireRole(request.getRoleId()); role.setStatus(request.getStatus());
        role.setUpdatedBy(permissionService.currentUserId()); role.setUpdateTime(LocalDateTime.now());
        roleMapper.updateById(role); invalidateRoleUsers(role.getRoleId());
    }

    @Transactional
    public void delete(Long roleId) {
        assertNotProtected(roleId); requireRole(roleId);
        if (userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getRoleId, roleId)) > 0)
            throw new BusinessException(409, "角色仍有关联用户，请先解除关联");
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, roleId));
        roleDeptMapper.delete(Wrappers.<SysRoleDept>lambdaQuery().eq(SysRoleDept::getRoleId, roleId));
        roleMapper.deleteById(roleId);
    }

    private void validate(SystemRoleSaveRequest request, Long excludeId) {
        if (!DATA_SCOPES.contains(request.getDataScope())) throw new BusinessException(400, "数据范围不合法");
        if (roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleName, request.getRoleName()).ne(excludeId != null, SysRole::getRoleId, excludeId)) > 0)
            throw new BusinessException(409, "角色名称已存在");
        if (roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleKey, request.getRoleKey()).ne(excludeId != null, SysRole::getRoleId, excludeId)) > 0)
            throw new BusinessException(409, "角色编码已存在");
        if (request.getMenuIds() != null && !request.getMenuIds().isEmpty() && menuMapper.selectBatchIds(request.getMenuIds()).size() != request.getMenuIds().stream().distinct().count())
            throw new BusinessException(422, "菜单授权包含不存在的菜单");
        if ("CUSTOM".equals(request.getDataScope()) && (request.getDeptIds() == null || request.getDeptIds().isEmpty()))
            throw new BusinessException(422, "自定义数据范围必须选择部门");
        if (request.getDeptIds() != null && !request.getDeptIds().isEmpty() && deptMapper.selectBatchIds(request.getDeptIds()).size() != request.getDeptIds().stream().distinct().count())
            throw new BusinessException(422, "数据范围包含不存在的部门");
    }

    private void replaceRelations(Long roleId, List<Long> menuIds, List<Long> deptIds, String dataScope) {
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, roleId));
        roleDeptMapper.delete(Wrappers.<SysRoleDept>lambdaQuery().eq(SysRoleDept::getRoleId, roleId));
        if (menuIds != null) menuIds.stream().distinct().forEach(menuId -> { SysRoleMenu item = new SysRoleMenu(); item.setRoleId(roleId); item.setMenuId(menuId); roleMenuMapper.insert(item); });
        if ("CUSTOM".equals(dataScope) && deptIds != null) deptIds.stream().distinct().forEach(deptId -> { SysRoleDept item = new SysRoleDept(); item.setRoleId(roleId); item.setDeptId(deptId); roleDeptMapper.insert(item); });
    }

    private void invalidateRoleUsers(Long roleId) {
        userMapper.selectList(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getRoleId, roleId))
                .forEach(user -> sessionService.invalidateUserSessions(user.getUserId()));
    }

    private SysRole requireRole(Long roleId) { SysRole role = roleMapper.selectById(roleId); if (role == null) throw new BusinessException(404, "角色不存在"); return role; }
    private void copy(SystemRoleSaveRequest request, SysRole role) { role.setRoleName(request.getRoleName().trim()); role.setRoleKey(request.getRoleKey().trim()); role.setRoleSort(request.getRoleSort()); role.setDataScope(request.getDataScope()); role.setRemark(request.getRemark()); if (StringUtils.hasText(request.getStatus())) { assertStatus(request.getStatus()); role.setStatus(request.getStatus()); } }
    private String defaultStatus(String status) { return StringUtils.hasText(status) ? status : "0"; }
    private void assertNotProtected(Long roleId) { if (SUPER_ADMIN_ROLE_ID.equals(roleId)) throw new BusinessException(403, "超级管理员角色受保护"); }
    private void assertStatus(String status) { if (!"0".equals(status) && !"1".equals(status)) throw new BusinessException(400, "角色状态不合法"); }
}
