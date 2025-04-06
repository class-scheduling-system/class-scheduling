package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.models.dto.BackAddCampusDTO;
import com.frontleaves.scheduling.models.dto.FileDTO;
import com.frontleaves.scheduling.models.dto.ListOfCampusDTO;
import com.frontleaves.scheduling.models.dto.base.CampusDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.entity.CampusDO;
import com.frontleaves.scheduling.models.vo.BatchAddCampusVO;
import com.frontleaves.scheduling.models.vo.CampusVO;
import com.frontleaves.scheduling.services.CampusService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * 校区控制器
 * <p>
 * 该类用于定义校区控制器;
 * 用于定义校区相关的控制器接口。
 * </p>
 *
 * @author FLASHLACK
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/campus")
public class CampusController {

    private final CampusService campusService;

    /**
     * 添加校区
     * <p>
     * 该方法用于添加新的校区信息。管理员可以通过此接口提交校区信息，系统会验证数据的合法性后进行添加操作。
     * </p>
     *
     * @param campusVO 包含要添加的校区信息的请求体，经过验证确保数据的合法性
     * @return 返回一个包含成功消息和新添加校区信息的响应实体
     */
    @RequestRole({"管理员"})
    @PostMapping("")
    public ResponseEntity<BaseResponse<CampusDTO>> addCampus(
            @RequestBody @Validated CampusVO campusVO
    ) {
        campusService.checkAddCampusVO(campusVO);
        CampusDTO campusDTO = campusService.addCampus(campusVO);
        return ResultUtil.success("添加校区成功", campusDTO);
    }

    /**
     * 更新校区
     * <p>
     * 该方法用于更新现有的校区信息。管理员可以通过此接口提交新的校区信息，系统会验证数据的合法性后进行更新操作。
     * </p>
     *
     * @param campusUuid 校区的唯一标识符，用于定位要更新的校区
     * @param campusVO   包含要更新的校区信息的请求体，经过验证确保数据的合法性
     * @return 返回一个包含成功消息和更新后的校区信息的响应实体
     */
    @RequestRole({"管理员"})
    @PutMapping("/{campus_uuid}")
    public ResponseEntity<BaseResponse<CampusDTO>> updateCampus(
            @PathVariable("campus_uuid") String campusUuid,
            @RequestBody @Validated CampusVO campusVO
    ) {
        CampusDO campusDO = campusService.checkUpdateCampusVO(campusUuid, campusVO);
        CampusDTO campusDTO = campusService.updateCampus(campusVO, campusDO);
        return ResultUtil.success("更新校区成功", campusDTO);
    }

    /**
     * 删除校区
     * <p>
     * 该方法用于删除指定的校区信息。管理员可以通过此接口提供校区的唯一标识符，系统会验证数据的合法性后进行删除操作。
     * </p>
     *
     * @param campusUuid 校区的唯一标识符，用于定位要删除的校区
     * @return 返回一个包含成功消息和被删除校区唯一标识符的响应实体
     */
    @RequestRole({"管理员"})
    @DeleteMapping("/{campus_uuid}")
    public ResponseEntity<BaseResponse<String>> deleteCampus(
            @PathVariable("campus_uuid") String campusUuid
    ) {
        CampusDO campusDO = campusService.checkDeleteCampus(campusUuid);
        campusService.deleteCampus(campusDO);
        return ResultUtil.success("删除校区成功", campusUuid);
    }

    /**
     * 获取校区分页数据
     * <p>
     * 该方法用于根据提供的分页参数、关键词以及排序方式，从系统中检索校区信息，并以分页的形式返回。
     * 支持通过关键词进行模糊搜索，同时允许用户指定结果是否按照降序排列。此接口仅限拥有"管理员"角色的用户访问。
     *
     * @param page    请求的数据页码，默认值为 {@code 1}
     * @param size    每页显示的记录数，默认值为 {@code 20}
     * @param keyword 用于过滤校区名称或其他属性的关键词，可选参数
     * @param isDesc  结果排序方式，如果设置为 {@code true} 则表示按降序排列，默认值为 {@code true}
     * @return 包含了请求状态和校区数据列表的响应实体，其中校区数据被封装在 {@code CampusDTO} 对象中
     */
    @RequestRole({"管理员"})
    @GetMapping("/page")
    public ResponseEntity<BaseResponse<PageDTO<CampusDTO>>> getCampusPage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "is_desc", defaultValue = "true") Boolean isDesc
    ) {
        Optional.ofNullable(size)
                .filter(s -> s > 0)
                .orElseThrow(() -> new BusinessException("每页显示数量错误", ErrorCode.PARAMETER_INVALID));
        Optional.of(size)
                .filter(s -> s <= 200)
                .orElseThrow(() -> new BusinessException("单页查询不允许超过 200", ErrorCode.PARAMETER_INVALID));
        Optional.ofNullable(page)
                .filter(p -> p > 0)
                .orElseThrow(() -> new BusinessException("页码参数错误", ErrorCode.PARAMETER_INVALID));
        keyword = Optional.ofNullable(keyword)
                .filter(key -> !key.isBlank())
                .orElse(null);
        PageDTO<CampusDTO> pageOfCampus = campusService.getPageOfCampus(page, size, isDesc, keyword);
        return ResultUtil.success("获取校区分页数据成功", pageOfCampus);
    }

    /**
     * 获取校区列表
     * <p>
     * 该方法用于从系统中获取所有校区的列表，并将其封装为 {@code List<ListOfCampusDTO>} 对象返回。此操作需要管理员权限。
     *
     * @return 包含校区列表的 {@code ResponseEntity<BaseResponse<List<ListOfCampusDTO>>>} 对象，其中 {@code BaseResponse} 封装了操作结果和消息
     */
    @RequestRole({"管理员"})
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<ListOfCampusDTO>>> getCampusList() {
        List<ListOfCampusDTO> campusList = campusService.getCampusList();
        return ResultUtil.success("获取校区列表成功", campusList);
    }

    /**
     * 获取校区导入模板
     * <p>
     * 该方法用于提供校区批量导入的Excel模板文件，以Base64编码的形式返回。
     * 此接口仅限拥有"管理员"角色的用户访问。
     * </p>
     *
     * @return 包含Excel模板文件Base64编码的响应实体
     */
    @RequestRole({"管理员"})
    @GetMapping("/get-template")
    public ResponseEntity<BaseResponse<FileDTO>> getCampusImportTemplate() {
        // 获取校区导入模板的字节数组
        byte[] bytes = campusService.getCampusImportTemplate();

        // 将字节数组转换为Base64编码字符串
        FileDTO fileDTO = new FileDTO(
                "校区导入模板.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "data:application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;base64," + Base64.getEncoder().encodeToString(bytes)
        );

        return ResultUtil.success("获取校区导入模板成功", fileDTO);
    }

    /**
     * 批量导入校区信息
     * <p>
     * 该方法用于批量导入校区信息，支持忽略错误的选项。
     * 只有具有"管理员"角色的用户才能访问此方法。
     * </p>
     *
     * @param batchAddCampusVO 批量添加校区的信息对象
     * @return 返回一个包含操作结果的响应实体对象
     */
    @RequestRole({"管理员"})
    @PostMapping("/batch-import")
    public ResponseEntity<BaseResponse<BackAddCampusDTO>> batchImportCampus(
            @RequestBody @Validated BatchAddCampusVO batchAddCampusVO
    ) {
        // 验证批量导入校区数据并返回处理后的文件
        byte[] file = campusService.verifyCampusBatchAndBackFile(batchAddCampusVO);

        // 根据是否忽略错误选择相应的导入方法
        BackAddCampusDTO backAddCampusDTO = Optional.ofNullable(batchAddCampusVO.getIgnoreError())
                .filter(Boolean.TRUE::equals)
                .map(ignoreError -> campusService.batchImportIgnoreError(file))
                .orElseGet(() -> campusService.batchImportNoIgnoreError(file));

        // 检查是否有校区导入失败
        if (backAddCampusDTO.getFailedCount() > 0) {
            // 如果有校区导入失败，返回带有错误信息的响应
            return ResultUtil.success("存在添加失败的校区", backAddCampusDTO);
        }

        // 如果所有校区都成功导入，返回批量添加校区成功的响应
        return ResultUtil.success("批量添加校区成功", backAddCampusDTO);
    }
}
