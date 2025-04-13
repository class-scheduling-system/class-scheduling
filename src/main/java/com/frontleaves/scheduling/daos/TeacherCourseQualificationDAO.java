package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.HashUtil;
import cn.hutool.crypto.digest.MD5;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.TeacherCourseQualificationMapper;
import com.frontleaves.scheduling.models.entity.base.TeacherCourseQualificationDO;
import com.frontleaves.scheduling.models.entity.base.TeacherDO;
import com.frontleaves.scheduling.models.vo.TeacherCourseQualificationQueryVO;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.codec.digest.Md5Crypt;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

/**
 * 教师课程资格数据访问对象
 * <p>
 * 该类是 {@code TeacherCourseQualificationDO} 实体类的数据访问对象，通过继承 {@code ServiceImpl<TeacherCourseQualificationMapper, TeacherCourseQualificationDO>}，
 * 实现了对教师课程资格表的基本 CRUD 操作。此类提供了与数据库交互的方法，用于管理教师课程资格的相关信息。
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @see TeacherCourseQualificationDO
 * @see TeacherCourseQualificationMapper
 * @since v1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TeacherCourseQualificationDAO extends
        ServiceImpl<TeacherCourseQualificationMapper, TeacherCourseQualificationDO> {
    private final RedissonClient redisson;
    private final TeacherDAO teacherDAO;

    /**
     * 根据教师课程资格UUID获取教师课程资格信息
     * @param teacherCourseQualificationUuid 教师课程资格UUID
     * @return 教师课程资格信息
     */
    public TeacherCourseQualificationDO getTeacherCourseQualificationByUuid(
            String teacherCourseQualificationUuid) {
        RMap<String, String> rMap = redisson.getMap(
                StringConstant.Redis.TEACHER_COURSE_QUALIFICATION_UUID + teacherCourseQualificationUuid);
        if (!rMap.isExists()){
            TeacherCourseQualificationDO teacherCourseQualificationDO = this.getById(teacherCourseQualificationUuid);
            if (teacherCourseQualificationDO != null) {
                rMap.putAll(ConvertUtil.convertObjectToMapString(teacherCourseQualificationDO));
                rMap.expire(Duration.ofSeconds(3600));
                return teacherCourseQualificationDO;
            }
            return null;
        }
        return BeanUtil.toBean(rMap, TeacherCourseQualificationDO.class);
    }

    /**
     * 根据课程库UUID获取已通过审核教师课程资格信息,并将查询结果存入Redis缓存
     * <p>
     * 该方法首先尝试从Redis缓存中获取教师课程资格信息。如果缓存中不存在，则从数据库中查询，
     * 并将查询结果存入Redis缓存中。缓存的有效期为1小时。
     * </p>
     * @param courseLibraryUuid 课程库UUID，用于查询对应的教师课程资格信息
     * @return 教师课程资格信息，如果未找到则返回null
     * @see TeacherCourseQualificationDO
     */
    public List<TeacherCourseQualificationDO> getTeacherCourseQualificationStatusByCourseLibraryUuid(
            String courseLibraryUuid) {
        RList<TeacherCourseQualificationDO> rList = redisson.getList(
                StringConstant.Redis.TEACHER_COURSE_QUALIFICATION_COURSE_LIBRARY_UUID + courseLibraryUuid);
        if (!rList.isExists()){
            List<TeacherCourseQualificationDO> teacherCourseQualificationDOList = this.lambdaQuery()
                    .eq(TeacherCourseQualificationDO::getCourseUuid, courseLibraryUuid)
                    .eq(TeacherCourseQualificationDO::getStatus,1).list();
            if (teacherCourseQualificationDOList != null) {
                rList.addAll(teacherCourseQualificationDOList);
                rList.expire(Duration.ofSeconds(3600));
                return teacherCourseQualificationDOList;
            }
            return list();
        }
        return rList.readAll();
    }
    
    /**
     * 根据查询条件分页获取教师课程资格列表
     *
     * @param page 页码
     * @param size 每页大小
     * @param isDesc 是否降序排序
     * @param queryVO 查询条件
     * @return 分页结果
     */
    public IPage<TeacherCourseQualificationDO> getTeacherCourseQualificationPage(
            Integer page, Integer size, Boolean isDesc, TeacherCourseQualificationQueryVO queryVO) {
        LambdaQueryWrapper<TeacherCourseQualificationDO> wrapper = createQueryWrapper(queryVO);
        
        // 设置排序
        if (Boolean.TRUE.equals(isDesc)) {
            wrapper.orderByDesc(TeacherCourseQualificationDO::getUpdatedAt);
        } else {
            wrapper.orderByAsc(TeacherCourseQualificationDO::getUpdatedAt);
        }
        
        return this.page(new Page<>(page, size), wrapper);
    }
    
    /**
     * 根据查询条件获取教师课程资格列表（不分页）
     *
     * @param queryVO 查询条件
     * @return 教师课程资格列表
     */
    public List<TeacherCourseQualificationDO> getTeacherCourseQualificationList(
            TeacherCourseQualificationQueryVO queryVO) {
        LambdaQueryWrapper<TeacherCourseQualificationDO> wrapper = createQueryWrapper(queryVO);
        return this.list(wrapper);
    }
    
    /**
     * 根据教师UUID获取该教师的所有课程资格
     *
     * @param teacherUuid 教师UUID
     * @return 教师课程资格列表
     */
    public List<TeacherCourseQualificationDO> getTeacherCourseQualificationsByTeacherUuid(String teacherUuid) {
        return this.lambdaQuery()
                .eq(TeacherCourseQualificationDO::getTeacherUuid, teacherUuid)
                .list();
    }
    
    /**
     * 保存教师课程资格信息并清除相关缓存
     *
     * @param teacherCourseQualificationDO 教师课程资格信息
     * @return 是否保存成功
     */
    public boolean saveTeacherCourseQualification(TeacherCourseQualificationDO teacherCourseQualificationDO) {
        boolean result = this.save(teacherCourseQualificationDO);
        if (result) {
            // 清除课程相关缓存
            redisson.getList(StringConstant.Redis.TEACHER_COURSE_QUALIFICATION_COURSE_LIBRARY_UUID 
                    + teacherCourseQualificationDO.getCourseUuid()).delete();
        }
        return result;
    }
    
    /**
     * 更新教师课程资格信息并清除相关缓存
     *
     * @param teacherCourseQualificationDO 教师课程资格信息
     * @return 是否更新成功
     */
    public boolean updateTeacherCourseQualification(TeacherCourseQualificationDO teacherCourseQualificationDO) {
        boolean result = this.updateById(teacherCourseQualificationDO);
        if (result) {
            // 清除UUID缓存
            redisson.getMap(StringConstant.Redis.TEACHER_COURSE_QUALIFICATION_UUID 
                    + teacherCourseQualificationDO.getQualificationUuid()).delete();
            // 清除课程相关缓存
            redisson.getList(StringConstant.Redis.TEACHER_COURSE_QUALIFICATION_COURSE_LIBRARY_UUID 
                    + teacherCourseQualificationDO.getCourseUuid()).delete();
        }
        return result;
    }
    
    /**
     * 删除教师课程资格信息并清除相关缓存
     *
     * @param qualificationUuid 资格UUID
     * @return 是否删除成功
     */
    public boolean removeTeacherCourseQualification(String qualificationUuid) {
        // 先获取实体以便后续清除课程相关缓存
        TeacherCourseQualificationDO entity = this.getTeacherCourseQualificationByUuid(qualificationUuid);
        if (entity == null) {
            return false;
        }
        
        boolean result = this.removeById(qualificationUuid);
        if (result) {
            // 清除UUID缓存
            redisson.getMap(StringConstant.Redis.TEACHER_COURSE_QUALIFICATION_UUID + qualificationUuid).delete();
            // 清除课程相关缓存
            redisson.getList(StringConstant.Redis.TEACHER_COURSE_QUALIFICATION_COURSE_LIBRARY_UUID 
                    + entity.getCourseUuid()).delete();
        }
        return result;
    }
    
    /**
     * 创建查询条件包装器
     *
     * @param queryVO 查询条件
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<TeacherCourseQualificationDO> createQueryWrapper(
            TeacherCourseQualificationQueryVO queryVO) {
        LambdaQueryWrapper<TeacherCourseQualificationDO> wrapper = new LambdaQueryWrapper<>();
        
        if (queryVO != null) {
            // 添加教师UUID条件
            if (queryVO.getTeacherUuid() != null && !queryVO.getTeacherUuid().isBlank()) {
                wrapper.eq(TeacherCourseQualificationDO::getTeacherUuid, queryVO.getTeacherUuid());
            }
            
            // 添加课程UUID条件
            if (queryVO.getCourseUuid() != null && !queryVO.getCourseUuid().isBlank()) {
                wrapper.eq(TeacherCourseQualificationDO::getCourseUuid, queryVO.getCourseUuid());
            }
            
            // 添加资格等级条件
            if (queryVO.getQualificationLevel() != null) {
                wrapper.eq(TeacherCourseQualificationDO::getQualificationLevel, queryVO.getQualificationLevel());
            }
            
            // 添加是否主讲教师条件
            if (queryVO.getIsPrimary() != null) {
                wrapper.eq(TeacherCourseQualificationDO::getIsPrimary, queryVO.getIsPrimary());
            }
            
            // 添加状态条件
            if (queryVO.getStatus() != null) {
                wrapper.eq(TeacherCourseQualificationDO::getStatus, queryVO.getStatus());
            }
            
            // 按部门查询 - 通过部门UUID查找该部门下的所有教师，然后添加条件
            if (queryVO.getDepartmentUuid() != null && !queryVO.getDepartmentUuid().isBlank()) {
                List<TeacherDO> teacherList = teacherDAO.getTeacherLiteList(queryVO.getDepartmentUuid(), null);
                if (!teacherList.isEmpty()) {
                    List<String> teacherUuids = teacherList.stream()
                            .map(TeacherDO::getTeacherUuid)
                            .toList();
                    wrapper.in(TeacherCourseQualificationDO::getTeacherUuid, teacherUuids);
                } else {
                    // 如果部门下没有教师，则设置一个不可能满足的条件，保证查询结果为空
                    wrapper.eq(TeacherCourseQualificationDO::getTeacherUuid, "NO_TEACHER_IN_DEPARTMENT");
                }
            }
        }
        
        return wrapper;
    }

    /**
     * 根据教师UUID列表获取教师课程资格列表（不分页）
     *
     * @param teacherUuids 教师UUID列表
     * @return 教师课程资格列表
     */
    public List<TeacherCourseQualificationDO> getTeacherCourseQualificationLiteList(List<String> teacherUuids) {
        // 计算 teacherUuids 的 hash
        String newHash = teacherUuids == null ? "all" : Md5Crypt.md5Crypt(teacherUuids.toString().getBytes());
        RList<TeacherCourseQualificationDO> rList = redisson.getList(StringConstant.Redis.TEACHER_COURSE_QUALIFICATION_LITE_LIST + newHash);
        if (!rList.isExists()) {
            if (teacherUuids == null || teacherUuids.isEmpty()) {
                return this.list();
            } else {
                List<TeacherCourseQualificationDO> teacherCourseQualificationDOList = this.lambdaQuery()
                        .in(TeacherCourseQualificationDO::getTeacherUuid, teacherUuids)
                        .list();
                rList.addAll(teacherCourseQualificationDOList);
                rList.expire(Duration.ofSeconds(3600));
                return teacherCourseQualificationDOList;
            }
        }
        return rList.readAll();
    }
}
