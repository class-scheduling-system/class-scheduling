package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.annotations.RequestLogin;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.RoleDTO;
import com.frontleaves.scheduling.models.dto.RoleLiteDTO;
import com.frontleaves.scheduling.services.RoleService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色控制器
 * <p>
 * 该类提供了处理角色相关请求的 RESTful API，包括获取角色信息等功能。
 *
 * @author qiyu
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/role")
public class RoleController {

    private final RoleService roleService;

    /**
     * 获取角色信息
     * <p>
     * 该方法用于获取指定角色的详细信息。通过调用 {@code roleService.getRole()} 方法从系统数据库中获取指定角色的详细信息，并将其封装到一个 {@code RoleDTO} 对象中返回。
     * 返回的信息包括角色ID、角色名称、角色描述、角色权限等。
     *
     * @param role 角色ID或角色名称
     * @return 包含角色详细信息的响应实体，其中数据部分为 {@code BaseResponse<RoleDTO>} 类型
     */
    @RequestLogin
    @GetMapping("")
    public ResponseEntity<BaseResponse<RoleDTO>> getRole(
            @RequestParam("role_uuid") String role
    ) {
        if (role == null || role.isBlank()) {
            throw new BusinessException("角色ID/名称不能为空", ErrorCode.PARAMETER_ERROR);
        }
        RoleDTO roleDTO = roleService.getRole(role);
        return ResultUtil.success("角色信息获取成功", roleDTO);
    }

    /**
     * 获取角色分页列表
     * <p>
     * 该方法用于获取系统中所有角色的分页列表信息。通过调用 {@code roleService.getRolePage()} 方法从系统数据库中获取所有角色的分页列表信息，
     * 并将其封装到一个 {@code PageDTO<RoleDTO>} 对象中返回。
     * 返回的信息包括角色ID、角色名称、角色描述、角色权限等。
     *
     * @param page   当前页数
     * @param size   每页显示数量
     * @param isDesc 是否降序
     * @param keyword 搜索关键字
     * @return 包含角色列表信息的响应实体，其中数据部分为 {@code BaseResponse<PageDTO<RoleDTO>>} 类型
     */
    @RequestLogin
    @GetMapping("/page")
    public ResponseEntity<BaseResponse<PageDTO<RoleDTO>>> getRolePage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "is_desc", defaultValue = "false") Boolean isDesc,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        roleService.checkPageAndSize(page, size);
        PageDTO<RoleDTO> rolePage = roleService.getRolePage(page, size, isDesc, keyword);
        return ResultUtil.success("角色列表获取成功", rolePage);
    }

    /**
     * 获取角色列表（不分页）
     * <p>
     * 该方法用于获取系统中所有有效角色的列表信息（不分页）。通过调用 {@code roleService.getRoleList()} 方法从系统数据库中获取所有状态为有效的角色列表，
     * 并将其封装到一个 {@code List<RoleLiteDTO>} 对象中返回。
     * 返回的信息仅包含角色ID和角色名称，适用于下拉菜单等简单场景。
     *
     * @return 包含角色精简列表信息的响应实体，其中数据部分为 {@code BaseResponse<List<RoleLiteDTO>>} 类型
     */
    @RequestLogin
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<RoleLiteDTO>>> getRoleList() {
        List<RoleLiteDTO> roleList = roleService.getRoleList();
        return ResultUtil.success("角色列表获取成功", roleList);
    }
}
