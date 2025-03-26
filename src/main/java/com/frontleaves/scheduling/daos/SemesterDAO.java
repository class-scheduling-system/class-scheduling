package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.SemesterMapper;
import com.frontleaves.scheduling.models.entity.SemesterDO;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 学期数据访问对象
 * @author FLASHLACK
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SemesterDAO  extends ServiceImpl<SemesterMapper, SemesterDO> {
    private final RedissonClient redisson;


        /**
         * 根据学期的UUID获取学期信息
         * 首先尝试从Redis中获取学期信息，如果不存在，则从数据库中获取，并将其存入Redis中以供下次快速访问
         * @param semesterUuid 学期的唯一标识符
         * @return 返回学期信息对象，如果找不到则返回null
         */
        public SemesterDO getSemesterByUuid(String semesterUuid) {
            // 从Redis中获取学期信息
            RMap<String, String> rMap = redisson.getMap(StringConstant.Redis.SEMESTER_UUID + semesterUuid);
            if (!rMap.isExists()) {
                // 如果Redis中不存在该学期信息，则从数据库中获取
                SemesterDO semesterDO = this.getById(semesterUuid);
                if (semesterDO != null) {
                    // 将获取到的学期信息存入Redis，并设置过期时间
                    rMap.putAll(ConvertUtil.convertObjectToMapString(semesterDO));
                    rMap.expire(Duration.ofSeconds(86400));
                    return semesterDO;
                }
            } else {
                // 如果Redis中存在该学期信息，则直接转换并返回学期信息对象
                return BeanUtil.toBean(rMap, SemesterDO.class);
            }
            return null;
        }
}
