package com.zhihuitong.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.system.dto.SystemPostQuery;
import com.zhihuitong.modules.system.entity.SysDept;
import com.zhihuitong.modules.system.entity.SysMenu;
import com.zhihuitong.modules.system.entity.SysPost;
import com.zhihuitong.modules.system.entity.SysRoleMenu;
import com.zhihuitong.modules.system.entity.SysUser;
import com.zhihuitong.modules.system.mapper.SysDeptMapper;
import com.zhihuitong.modules.system.mapper.SysMenuMapper;
import com.zhihuitong.modules.system.mapper.SysPostMapper;
import com.zhihuitong.modules.system.mapper.SysRoleMenuMapper;
import com.zhihuitong.modules.system.mapper.SysUserMapper;
import com.zhihuitong.security.service.LoginSessionService;
import com.zhihuitong.security.service.PermissionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SystemResourceService {
    private final SysMenuMapper menuMapper;
    private final SysDeptMapper deptMapper;
    private final SysPostMapper postMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserMapper userMapper;
    private final LoginSessionService sessionService;
    private final PermissionService permissionService;

    public SystemResourceService(SysMenuMapper menuMapper, SysDeptMapper deptMapper, SysPostMapper postMapper,
                                 SysRoleMenuMapper roleMenuMapper, SysUserMapper userMapper,
                                 LoginSessionService sessionService, PermissionService permissionService) {
        this.menuMapper = menuMapper; this.deptMapper = deptMapper; this.postMapper = postMapper;
        this.roleMenuMapper = roleMenuMapper; this.userMapper = userMapper;
        this.sessionService = sessionService; this.permissionService = permissionService;
    }

    public List<SysMenu> listMenus(String menuName, String status) {
        return menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .like(StringUtils.hasText(menuName), SysMenu::getMenuName, menuName)
                .eq(StringUtils.hasText(status), SysMenu::getStatus, status)
                .orderByAsc(SysMenu::getParentId, SysMenu::getOrderNum));
    }

    public SysMenu saveMenu(SysMenu request) {
        validateStatus(request.getStatus());
        if (request.getParentId() == null) request.setParentId(0L);
        if (request.getMenuId() != null && request.getMenuId().equals(request.getParentId())) throw new BusinessException(422, "菜单不能选择自己作为父节点");
        if (request.getParentId() != 0 && menuMapper.selectById(request.getParentId()) == null) throw new BusinessException(422, "父菜单不存在");
        if (!StringUtils.hasText(request.getMenuKey())) throw new BusinessException(400, "menuKey 不能为空");
        if (menuMapper.selectCount(Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getMenuKey, request.getMenuKey()).ne(request.getMenuId() != null, SysMenu::getMenuId, request.getMenuId())) > 0) throw new BusinessException(409, "菜单标识已存在");
        LocalDateTime now = LocalDateTime.now();
        if (request.getMenuId() == null) {
            request.setDeleted(0); request.setCreatedBy(permissionService.currentUserId()); request.setCreateTime(now); menuMapper.insert(request);
        } else {
            requireMenu(request.getMenuId()); request.setUpdatedBy(permissionService.currentUserId()); request.setUpdateTime(now); menuMapper.updateById(request);
        }
        sessionService.invalidateAllSessions();
        return request;
    }

    public void deleteMenu(Long menuId) {
        requireMenu(menuId);
        if (menuMapper.selectCount(Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getParentId, menuId)) > 0) throw new BusinessException(409, "菜单存在子节点，不能删除");
        if (roleMenuMapper.selectCount(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getMenuId, menuId)) > 0) throw new BusinessException(409, "菜单已分配给角色，请先解除授权");
        menuMapper.deleteById(menuId); sessionService.invalidateAllSessions();
    }

    public List<SysDept> listDepts(String deptName, String status) {
        return deptMapper.selectList(Wrappers.<SysDept>lambdaQuery().like(StringUtils.hasText(deptName), SysDept::getDeptName, deptName)
                .eq(StringUtils.hasText(status), SysDept::getStatus, status).orderByAsc(SysDept::getParentId, SysDept::getOrderNum));
    }

    public SysDept saveDept(SysDept request) {
        validateStatus(request.getStatus());
        if (request.getParentId() == null) request.setParentId(0L);
        if (request.getDeptId() != null && request.getDeptId().equals(request.getParentId())) throw new BusinessException(422, "部门不能选择自己作为父节点");
        SysDept parent = request.getParentId() == 0 ? null : deptMapper.selectById(request.getParentId());
        if (request.getParentId() != 0 && parent == null) throw new BusinessException(422, "父部门不存在");
        request.setAncestors(parent == null ? "0" : parent.getAncestors() + "," + parent.getDeptId());
        LocalDateTime now = LocalDateTime.now();
        if (request.getDeptId() == null) {
            request.setDeleted(0); request.setCreatedBy(permissionService.currentUserId()); request.setCreateTime(now); deptMapper.insert(request);
        } else {
            requireDept(request.getDeptId()); request.setUpdatedBy(permissionService.currentUserId()); request.setUpdateTime(now); deptMapper.updateById(request);
        }
        return request;
    }

    public void deleteDept(Long deptId) {
        requireDept(deptId);
        if (deptMapper.selectCount(Wrappers.<SysDept>lambdaQuery().eq(SysDept::getParentId, deptId)) > 0) throw new BusinessException(409, "部门存在子节点，不能删除");
        if (userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getDeptId, deptId)) > 0) throw new BusinessException(409, "部门仍有关联用户，不能删除");
        deptMapper.deleteById(deptId);
    }

    public TableDataInfo<SysPost> listPosts(SystemPostQuery query) {
        Page<SysPost> page = postMapper.selectPage(query.toPage(), Wrappers.<SysPost>lambdaQuery()
                .like(StringUtils.hasText(query.getPostCode()), SysPost::getPostCode, query.getPostCode())
                .like(StringUtils.hasText(query.getPostName()), SysPost::getPostName, query.getPostName())
                .eq(StringUtils.hasText(query.getStatus()), SysPost::getStatus, query.getStatus()).orderByAsc(SysPost::getPostSort));
        return TableDataInfoBuilder.build(page);
    }

    public SysPost savePost(SysPost request) {
        validateStatus(request.getStatus());
        if (!StringUtils.hasText(request.getPostCode()) || !StringUtils.hasText(request.getPostName())) throw new BusinessException(400, "岗位编码和名称不能为空");
        if (postMapper.selectCount(Wrappers.<SysPost>lambdaQuery().eq(SysPost::getPostCode, request.getPostCode()).ne(request.getPostId() != null, SysPost::getPostId, request.getPostId())) > 0) throw new BusinessException(409, "岗位编码已存在");
        LocalDateTime now = LocalDateTime.now();
        if (request.getPostId() == null) {
            request.setDeleted(0); request.setCreatedBy(permissionService.currentUserId()); request.setCreateTime(now); postMapper.insert(request);
        } else {
            requirePost(request.getPostId()); request.setUpdatedBy(permissionService.currentUserId()); request.setUpdateTime(now); postMapper.updateById(request);
        }
        return request;
    }

    public void deletePost(Long postId) {
        requirePost(postId);
        if (userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getPostId, postId)) > 0) throw new BusinessException(409, "岗位仍有关联用户，不能删除");
        postMapper.deleteById(postId);
    }

    private SysMenu requireMenu(Long id) { SysMenu item = menuMapper.selectById(id); if (item == null) throw new BusinessException(404, "菜单不存在"); return item; }
    private SysDept requireDept(Long id) { SysDept item = deptMapper.selectById(id); if (item == null) throw new BusinessException(404, "部门不存在"); return item; }
    private SysPost requirePost(Long id) { SysPost item = postMapper.selectById(id); if (item == null) throw new BusinessException(404, "岗位不存在"); return item; }
    private void validateStatus(String status) { if (status == null) return; if (!"0".equals(status) && !"1".equals(status)) throw new BusinessException(400, "状态不合法"); }
}
