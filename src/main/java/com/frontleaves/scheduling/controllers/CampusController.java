package com.frontleaves.scheduling.controllers;

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
 *     该类用于定义校区控制器;
 *     用于定义校区相关的控制器接口。
 *   </p>
 * @author FLASHLACK
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/campus")
public class CampusController {

    private final CampusService campusService;
    /**
     * 添加校区接口
     * 此接口用于接收客户端发送的校区信息，并将其添加到系统中
     *
     * @param campusVO 校区信息的视图对象，由客户端发送
     * @return 返回一个包含成功消息和新增校区信息的响应实体
     */
    @PostMapping("")
    public ResponseEntity<BaseResponse<CampusDTO>> addCampus(
            @RequestBody @Validated CampusVO campusVO
    ) {
        campusService.checkAddCampusVO(campusVO);
        CampusDTO campusDTO = campusService.addCampus(campusVO);
        return ResultUtil.success("添加校区成功",campusDTO);
    }

    /**
     * 更新校区信息的接口方法
     * 使用PUT请求，请求路径为/{campus_uuid}
     *
     * @param campusUuid 校区的唯一标识符，用于定位要更新的校区
     * @param campusVO 包含更新后的校区信息的请求体，经过验证确保数据的合法性
     * @return 返回一个包含更新后的校区信息的响应实体
     */
    @PutMapping("/{campus_uuid}")
    public ResponseEntity<BaseResponse<CampusDTO>> updateCampus(
            @PathVariable("campus_uuid") String campusUuid,
            @RequestBody @Validated CampusVO campusVO
    ) {
        CampusDO campusDO = campusService.checkUpdateCampusVO(campusUuid,campusVO);
        CampusDTO campusDTO = campusService.updateCampus(campusVO,campusDO);
        return ResultUtil.success("更新校区成功", campusDTO);
    }
}
