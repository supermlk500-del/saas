package com.zhihuitong.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.system.dto.ResetPasswordRequest;
import com.zhihuitong.modules.system.dto.SystemUserQuery;
import com.zhihuitong.modules.system.dto.SystemUserSaveRequest;
import com.zhihuitong.modules.system.dto.UserStatusRequest;
import com.zhihuitong.modules.system.entity.SysDept;
import com.zhihuitong.modules.system.entity.SysPost;
import com.zhihuitong.modules.system.entity.SysRole;
import com.zhihuitong.modules.system.entity.SysUser;
import com.zhihuitong.modules.system.mapper.SysDeptMapper;
import com.zhihuitong.modules.system.mapper.SysPostMapper;
import com.zhihuitong.modules.system.mapper.SysRoleMapper;
import com.zhihuitong.modules.system.mapper.SysUserMapper;
import com.zhihuitong.modules.system.vo.SystemUserVo;
import com.zhihuitong.security.service.DataScopeService;
import com.zhihuitong.security.service.LoginSessionService;
import com.zhihuitong.security.service.PermissionService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SystemUserService {
    private static final Long SUPER_ADMIN_USER_ID = 1L;
    private static final Long SUPER_ADMIN_ROLE_ID = 1L;

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;
    private final SysPostMapper postMapper;
    private final PasswordEncoder passwordEncoder;
    private final LoginSessionService sessionService;
    private final PermissionService permissionService;
    private final DataScopeService dataScopeService;

    public SystemUserService(SysUserMapper userMapper, SysRoleMapper roleMapper, SysDeptMapper deptMapper,
                             SysPostMapper postMapper, PasswordEncoder passwordEncoder,
                             LoginSessionService sessionService, PermissionService permissionService, DataScopeService dataScopeService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.deptMapper = deptMapper;
        this.postMapper = postMapper;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.permissionService = permissionService;
        this.dataScopeService = dataScopeService;
    }

    public TableDataInfo<SystemUserVo> list(SystemUserQuery query) {
        var wrapper = dataScopeService.applyUserScope(Wrappers.<SysUser>lambdaQuery())
                .like(StringUtils.hasText(query.getUserName()), SysUser::getUserName, query.getUserName())
                .like(StringUtils.hasText(query.getPhonenumber()), SysUser::getPhonenumber, query.getPhonenumber())
                .eq(StringUtils.hasText(query.getStatus()), SysUser::getStatus, query.getStatus())
                .eq(query.getDeptId() != null, SysUser::getDeptId, query.getDeptId())
                .orderByDesc(SysUser::getCreateTime);
        Page<SysUser> page = userMapper.selectPage(query.toPage(), wrapper);
        return TableDataInfoBuilder.build(enrich(page).getRecords(), page.getTotal());
    }

    public SystemUserVo detail(Long userId) {
        Page<SysUser> page = Page.of(1, 1);
        page.setRecords(java.util.List.of(requireUser(userId)));
        return enrich(page).getRecords().get(0);
    }

    @Transactional
    public SystemUserVo create(SystemUserSaveRequest request) {
        if (!StringUtils.hasText(request.getPassword()) || request.getPassword().length() < 8) {
            throw new BusinessException(400, "初始密码至少 8 位");
        }
        if (SUPER_ADMIN_ROLE_ID.equals(request.getRoleId())) {
            throw new BusinessException(403, "不允许创建额外的超级管理员");
        }
        dataScopeService.assertDepartmentAssignable(request.getDeptId());
        validateReferences(request.getDeptId(), request.getPostId(), request.getRoleId());
        checkUnique(request, null);
        SysUser user = new SysUser();
        copy(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(defaultStatus(request.getStatus()));
        user.setDeleted(0);
        user.setPwdUpdateTime(LocalDateTime.now());
        user.setCreatedBy(permissionService.currentUserId());
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return detail(user.getUserId());
    }

    @Transactional
    public SystemUserVo update(SystemUserSaveRequest request) {
        if (request.getUserId() == null) throw new BusinessException(400, "userId 不能为空");
        SysUser existing = requireUser(request.getUserId());
        assertNotProtected(existing.getUserId());
        if (SUPER_ADMIN_ROLE_ID.equals(request.getRoleId())) {
            throw new BusinessException(403, "不允许分配超级管理员角色");
        }
        dataScopeService.assertDepartmentAssignable(request.getDeptId());
        validateReferences(request.getDeptId(), request.getPostId(), request.getRoleId());
        checkUnique(request, request.getUserId());
        copy(request, existing);
        existing.setUpdatedBy(permissionService.currentUserId());
        existing.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(existing);
        sessionService.invalidateUserSessions(existing.getUserId());
        return detail(existing.getUserId());
    }

    public void changeStatus(UserStatusRequest request) {
        assertStatus(request.getStatus());
        assertNotProtected(request.getUserId());
        SysUser user = requireUser(request.getUserId());
        user.setStatus(request.getStatus());
        user.setUpdatedBy(permissionService.currentUserId());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        sessionService.invalidateUserSessions(user.getUserId());
    }

    public void resetPassword(ResetPasswordRequest request) {
        assertNotProtected(request.getUserId());
        SysUser user = requireUser(request.getUserId());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPwdUpdateTime(LocalDateTime.now());
        user.setUpdatedBy(permissionService.currentUserId());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        sessionService.invalidateUserSessions(user.getUserId());
    }

    public void delete(Long userId) {
        assertNotProtected(userId);
        if (userId.equals(permissionService.currentUserId())) {
            throw new BusinessException(422, "不能删除当前登录用户");
        }
        requireUser(userId);
        userMapper.deleteById(userId);
        sessionService.invalidateUserSessions(userId);
    }

    private Page<SystemUserVo> enrich(Page<SysUser> page) {
        Map<Long, SysRole> roles = roleMapper.selectBatchIds(page.getRecords().stream().map(SysUser::getRoleId).distinct().toList())
                .stream().collect(Collectors.toMap(SysRole::getRoleId, Function.identity()));
        Map<Long, SysDept> depts = deptMapper.selectBatchIds(page.getRecords().stream().map(SysUser::getDeptId).distinct().toList())
                .stream().collect(Collectors.toMap(SysDept::getDeptId, Function.identity()));
        Map<Long, SysPost> posts = postMapper.selectBatchIds(page.getRecords().stream().map(SysUser::getPostId).distinct().toList())
                .stream().collect(Collectors.toMap(SysPost::getPostId, Function.identity()));
        Page<SystemUserVo> result = Page.of(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(user -> toVo(user, roles.get(user.getRoleId()), depts.get(user.getDeptId()), posts.get(user.getPostId()))).toList());
        return result;
    }

    private SystemUserVo toVo(SysUser user, SysRole role, SysDept dept, SysPost post) {
        SystemUserVo vo = new SystemUserVo();
        vo.setUserId(user.getUserId()); vo.setDeptId(user.getDeptId()); vo.setPostId(user.getPostId()); vo.setRoleId(user.getRoleId());
        vo.setUserName(user.getUserName()); vo.setNickName(user.getNickName()); vo.setEmail(user.getEmail());
        vo.setPhonenumber(user.getPhonenumber()); vo.setAvatar(user.getAvatar()); vo.setStatus(user.getStatus());
        vo.setLoginIp(user.getLoginIp()); vo.setLoginDate(user.getLoginDate()); vo.setCreateTime(user.getCreateTime()); vo.setRemark(user.getRemark());
        if (role != null) { vo.setRoleName(role.getRoleName()); vo.setRoleKey(role.getRoleKey()); }
        if (dept != null) vo.setDeptName(dept.getDeptName());
        if (post != null) vo.setPostName(post.getPostName());
        return vo;
    }

    private SysUser requireUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        return user;
    }

    private void validateReferences(Long deptId, Long postId, Long roleId) {
        SysDept dept = deptMapper.selectById(deptId); SysPost post = postMapper.selectById(postId); SysRole role = roleMapper.selectById(roleId);
        if (dept == null || !"0".equals(dept.getStatus())) throw new BusinessException(422, "部门不存在或已停用");
        if (post == null || !"0".equals(post.getStatus())) throw new BusinessException(422, "岗位不存在或已停用");
        if (role == null || !"0".equals(role.getStatus())) throw new BusinessException(422, "角色不存在或已停用");
    }

    private void checkUnique(SystemUserSaveRequest request, Long excludeId) {
        long usernameCount = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUserName, request.getUserName())
                .ne(excludeId != null, SysUser::getUserId, excludeId));
        if (usernameCount > 0) throw new BusinessException(409, "登录账号已存在");
        if (StringUtils.hasText(request.getEmail()) && userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getEmail, request.getEmail()).ne(excludeId != null, SysUser::getUserId, excludeId)) > 0)
            throw new BusinessException(409, "邮箱已存在");
        if (StringUtils.hasText(request.getPhonenumber()) && userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getPhonenumber, request.getPhonenumber()).ne(excludeId != null, SysUser::getUserId, excludeId)) > 0)
            throw new BusinessException(409, "手机号码已存在");
    }

    private void copy(SystemUserSaveRequest request, SysUser user) {
        user.setUserName(request.getUserName().trim()); user.setNickName(request.getNickName().trim()); user.setEmail(request.getEmail());
        user.setPhonenumber(request.getPhonenumber()); user.setAvatar(request.getAvatar()); user.setDeptId(request.getDeptId());
        user.setPostId(request.getPostId()); user.setRoleId(request.getRoleId()); user.setRemark(request.getRemark());
        if (StringUtils.hasText(request.getStatus())) { assertStatus(request.getStatus()); user.setStatus(request.getStatus()); }
    }

    private void assertNotProtected(Long userId) {
        if (SUPER_ADMIN_USER_ID.equals(userId)) throw new BusinessException(403, "超级管理员账号受保护");
    }
    private String defaultStatus(String status) { return StringUtils.hasText(status) ? status : "0"; }
    private void assertStatus(String status) { if (!"0".equals(status) && !"1".equals(status)) throw new BusinessException(400, "用户状态不合法"); }
}
