package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.models.dto.BackAddStudentDTO;
import com.frontleaves.scheduling.models.dto.PrepareStudentExampleDTO;
import com.frontleaves.scheduling.models.vo.BatchAddStudentVO;
import com.frontleaves.scheduling.services.BuildingService;
import com.frontleaves.scheduling.services.StudentService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 学生控制器
 *
 * @author FLASHLACK
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final BuildingService buildingService;

    /**
     * 获取学生导入模板
     * 该方法返回一个包含示例数据的响应实体，客户端可以通过下载链接下载该文件
     *
     * @return 返回一个包含示例数据的响应实体
     */
    @RequestRole("教务")
    @GetMapping("/get-example")
    public ResponseEntity<byte[]> getExample(
            @NotNull HttpServletRequest request
    ) {
        PrepareStudentExampleDTO prepareStudentExampleDTO = studentService.prepareDepartmentData(request);
        byte[] bytes = studentService.getExample(prepareStudentExampleDTO);

        HttpHeaders headers = Optional.of(new HttpHeaders())
                .map(header -> {
                    header.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                    header.setContentLength(bytes.length);
                    String fileName = URLEncoder.encode("学生导入模板.xlsx", StandardCharsets.UTF_8)
                            .replace("+", "%20");
                    header.add(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName);
                    return header;
                })
                .orElseThrow(() -> new BusinessException("获取响应头失败", ErrorCode.SERVER_INTERNAL_ERROR));

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    /**
     * 处理批量导入学生的请求
     * 该方法首先验证和检查批量添加学生的数据，然后调用服务层方法进行导入
     *
     * @param batchAddStudentVO 包含批量添加学生信息的请求体
     * @return 返回一个包含添加结果的响应实体
     */
    @PostMapping("/batch-import")
    public ResponseEntity<BaseResponse<BackAddStudentDTO>> batchImport(
            @RequestBody @Validated BatchAddStudentVO batchAddStudentVO,
            @NotNull HttpServletRequest request
    ) {
        byte[] file = studentService.verifyStudentBatchAndBackFile(batchAddStudentVO);
        String departmentUuid = studentService.getDepartmentUuid(request);
        // 执行批量导入学生的操作
        BackAddStudentDTO backAddStudentDTO = Optional.ofNullable(batchAddStudentVO.getIgnoreError())
                .filter(Boolean.TRUE::equals)
                .map(ignoreError -> studentService.batchImportIgnoreError(file, departmentUuid))
                .orElseGet(() -> studentService.batchImportNoIgnoreError(file, departmentUuid));

        if (backAddStudentDTO.getFailedCount() > 0) {
            // 如果有学生导入失败，返回带有错误信息的响应
            return ResultUtil.success("存在添加失败的学生情况", backAddStudentDTO);
        }
        // 返回批量添加学生成功的响应
        return ResultUtil.success("批量添加学生成功", backAddStudentDTO);
    }


}
