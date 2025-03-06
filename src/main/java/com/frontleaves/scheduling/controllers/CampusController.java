package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.models.dto.CampusDTO;
import com.frontleaves.scheduling.models.entity.CampusDO;
import com.frontleaves.scheduling.models.vo.CampusVO;
import com.frontleaves.scheduling.services.CampusService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
}
