package com.zhihuitong.modules.system.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.system.dto.SystemPostQuery;
import com.zhihuitong.modules.system.entity.SysPost;
import com.zhihuitong.modules.system.service.SystemResourceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/post")
public class SystemPostController {
    private final SystemResourceService service;
    public SystemPostController(SystemResourceService service) { this.service = service; }
    @PreAuthorize("@auth.hasPermission('system:post:list')") @GetMapping("/list") public TableDataInfo<SysPost> list(@Valid @ModelAttribute SystemPostQuery query) { return service.listPosts(query); }
    @PreAuthorize("@auth.hasPermission('system:post:manage')") @PostMapping public AjaxResult create(@RequestBody SysPost request) { request.setPostId(null); return AjaxResult.success(service.savePost(request)); }
    @PreAuthorize("@auth.hasPermission('system:post:manage')") @PutMapping public AjaxResult update(@RequestBody SysPost request) { return AjaxResult.success(service.savePost(request)); }
    @PreAuthorize("@auth.hasPermission('system:post:manage')") @DeleteMapping("/{postId}") public AjaxResult delete(@PathVariable Long postId) { service.deletePost(postId); return AjaxResult.success(); }
}
