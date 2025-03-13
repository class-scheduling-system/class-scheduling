package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.models.dto.BackAddStudentDTO;
import com.frontleaves.scheduling.models.dto.PrepareStudentExampleDTO;
import com.frontleaves.scheduling.models.vo.BatchAddStudentVO;
import com.frontleaves.scheduling.services.StudentService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ResultUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

    /**
     * 获取学生导入模板
     * 该方法返回一个包含示例数据的响应实体，客户端可以通过下载链接下载该文件
     *
     * @return 返回一个包含示例数据的响应实体
     */
    @RequestRole("教务")
    @GetMapping("/get-example")
    public ResponseEntity<byte[]> getExample(
            HttpServletRequest request
    ) {
        PrepareStudentExampleDTO prepareStudentExampleDTO = studentService.prepareDepartmentData(request);
        // 从studentService获取示例数据，以字节数组形式返回
        byte[] bytes = studentService.getExample(prepareStudentExampleDTO);
        // 创建HTTP头，设置为文件下载
        HttpHeaders headers = new HttpHeaders();
        // 设置内容类型为二进制流
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        // 使用UTF-8编码文件名，解决中文文件名问题
        String fileName = URLEncoder.encode("学生导入模板.xlsx", StandardCharsets.UTF_8)
                .replace("\\+", "%20");
        // 使用RFC 5987编码格式设置文件名
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName);
        // 设置内容长度
        headers.setContentLength(bytes.length);
        // 返回带有文件内容的ResponseEntity
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
            HttpServletRequest request
    ) {
        byte[] file = studentService.checkBatchAddStudentVO(batchAddStudentVO);
        String departmentUuid = studentService.getDepartmentUuid(request);
        // 执行批量导入学生的操作
        BackAddStudentDTO backAddStudentDTO;
        if (Boolean.TRUE.equals(batchAddStudentVO.getIgnoreError())) {
            backAddStudentDTO = studentService.batchImportIgnoreError(file, departmentUuid);
        } else {
            backAddStudentDTO = studentService.batchImportNoIgnoreError(file, departmentUuid);
        }

        if (backAddStudentDTO.getFailedCount() > 0) {
            // 如果有学生导入失败，返回带有错误信息的响应
            return ResultUtil.success("存在添加失败的学生情况", backAddStudentDTO);
        }
        // 返回批量添加学生成功的响应
        return ResultUtil.success("批量添加学生成功", backAddStudentDTO);
    }


}
