package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.TeacherDAO;
import com.frontleaves.scheduling.daos.UserDAO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.TeacherDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.vo.TeacherVO;
import com.frontleaves.scheduling.services.TeacherService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeacherLogic implements TeacherService {
    private final DepartmentDAO departmentDAO;
    private final UserDAO userDAO;
    private final TeacherDAO teacherDAO;
    /**
     * 添加教师信息
     *
     * 本方法首先根据传入的部门UUID和用户UUID验证部门和用户的存在性，
     * 如果不存在则抛出业务异常接着将TeacherVO对象的属性复制到TeacherDO对象中，
     * 生成一个新的UUID作为教师UUID，并保存到数据库中
     *
     * @param teacherVO 教师视图对象，包含要添加的教师的相关信息
     * @throws BusinessException 如果部门或用户不存在，则抛出此异常
     */
    @Override
    public void addTeacher(TeacherVO teacherVO) {
        // 根据部门UUID获取部门信息
        DepartmentDO departmentDO = departmentDAO.getDepartmentByUuid(teacherVO.getUnitUuid());
        // 如果部门不存在，抛出业务异常
        if (departmentDO == null) {
            throw new BusinessException("部门不存在", ErrorCode.NOT_EXIST);
        }

        // 根据用户UUID获取用户信息
        UserDO userDO = userDAO.getUserByUuid(teacherVO.getUserUuid());
        // 如果用户不存在，抛出业务异常
        if (userDO == null) {
            throw new BusinessException("用户不存在", ErrorCode.NOT_EXIST);
        }

        // 创建一个新的TeacherDO对象
        TeacherDO teacherDO = new TeacherDO();
        // 将TeacherVO对象的属性复制到TeacherDO对象中
        BeanUtils.copyProperties(teacherVO, teacherDO);
        // 生成一个新的UUID作为教师UUID，并设置到TeacherDO对象中
        teacherDO.setTeacherUuid(UuidUtil.generateUuidNoDash());

        // 保存TeacherDO对象到数据库中
        teacherDAO.save(teacherDO);
    }


}
