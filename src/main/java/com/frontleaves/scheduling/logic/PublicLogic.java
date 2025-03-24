/*
 * --------------------------------------------------------------------------------
 * Copyright (c) 2022-NOW(至今) 锋楪技术团队
 * Author: 锋楪技术团队 (https://www.frontleaves.com)
 *
 * 本文件包含锋楪技术团队项目的源代码，项目的所有源代码均遵循 MIT 开源许可证协议。
 * --------------------------------------------------------------------------------
 * 许可证声明：
 *
 * 版权所有 (c) 2022-2025 锋楪技术团队。保留所有权利。
 *
 * 本软件是"按原样"提供的，没有任何形式的明示或暗示的保证，包括但不限于
 * 对适销性、特定用途的适用性和非侵权性的暗示保证。在任何情况下，
 * 作者或版权持有人均不承担因软件或软件的使用或其他交易而产生的、
 * 由此引起的或以任何方式与此软件有关的任何索赔、损害或其他责任。
 *
 * 使用本软件即表示您了解此声明并同意其条款。
 *
 * 有关 MIT 许可证的更多信息，请查看项目根目录下的 LICENSE 文件或访问：
 * https://opensource.org/licenses/MIT
 * --------------------------------------------------------------------------------
 * 免责声明：
 *
 * 使用本软件的风险由用户自担。作者或版权持有人在法律允许的最大范围内，
 * 对因使用本软件内容而导致的任何直接或间接的损失不承担任何责任。
 * --------------------------------------------------------------------------------
 */

package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.SystemDAO;
import com.frontleaves.scheduling.models.dto.JvmStackDTO;
import com.frontleaves.scheduling.models.dto.SiteDTO;
import com.frontleaves.scheduling.models.dto.SystemDTO;
import com.frontleaves.scheduling.services.PublicService;
import com.sun.management.OperatingSystemMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.management.*;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.text.DecimalFormat;
import java.util.*;

/**
 * 提供公共逻辑处理的类，实现了 {@code PublicService} 接口。
 * <p>
 * 该类主要用于处理与系统相关的公共逻辑，例如获取网站的基本信息。通过依赖注入的方式，
 * 使用 {@code SystemDAO} 来从数据库中获取所需的数据，并将其封装到相应的数据传输对象（DTO）中返回。
 * <p>
 * 该类使用了 Spring 的 {@code @Service} 注解来标识其为一个服务层组件，并且使用了 Lombok 的
 * {@code @Slf4j} 和 {@code @RequiredArgsConstructor} 注解来简化日志记录和构造函数的编写。
 *
 * @author fanfan187
 * @version v1.0.0
 * @see PublicService
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicLogic implements PublicService {

    private final SystemDAO systemDAO;

    /**
     * 获取网站的基本信息。
     * <p>
     * 该方法从系统数据库中获取网站的详细信息，并将其封装到一个 {@code SiteDTO} 对象中返回。
     * 返回的信息包括网站名称、标题、副标题、描述、关键词、图标URL、Logo URL、ICP备案号、
     * ICP备案链接、公安备案号、公安备案链接、版权状态、开源许可证、联系邮箱、联系电话、办公地址、
     * 微博URL、微信公众号、所有者、创始人、上线日期和技术栈等。
     *
     * @return 包含网站详细信息的 {@code SiteDTO} 对象
     */
    @Override
    public SiteDTO getSiteInfo() {
        SiteDTO siteDTO = new SiteDTO();
        Map<String, String> systemInfoList = systemDAO.getSystemInfoList();
        siteDTO.setName(systemInfoList.get("web_name"))
                .setTitle(systemInfoList.get("web_title"))
                .setSubTitle(systemInfoList.get("web_subtitle"))
                .setDescription(systemInfoList.get("web_description"))
                .setKeywords(systemInfoList.get("web_keywords"))
                .setIconUrl(systemInfoList.get("web_icon_url"))
                .setLogoUrl(systemInfoList.get("web_logo"))
                .setIcpNumber(systemInfoList.get("web_icp"))
                .setIcpLink(systemInfoList.get("web_icp_link"))
                .setSecurityRecord(systemInfoList.get("web_security_record"))
                .setSecurityRecordLink(systemInfoList.get("web_security_record_link"))
                .setCopyrightStatus(systemInfoList.get("web_copyright_status"))
                .setOpenSourceLicense(systemInfoList.get("web_open_source_license"))
                .setContactEmail(systemInfoList.get("web_contact_email"))
                .setContactPhone(systemInfoList.get("web_contact_phone"))
                .setOfficeAddress(systemInfoList.get("web_office_address"))
                .setWeiboUrl(systemInfoList.get("web_weibo_url"))
                .setWechatOfficeAccount(systemInfoList.get("web_wechat_office_account"))
                .setOwner(systemInfoList.get("web_owner"))
                .setFounder(systemInfoList.get("web_founder"))
                .setLaunchDate(systemInfoList.get("web_launch_date"))
                .setTechnologyStack(systemInfoList.get("web_technology_stack"));
        return siteDTO;
    }

    /**
     * 获取系统信息
     * <p>
     * 该方法用于获取当前系统的详细信息，包括 CPU、内存、磁盘和操作系统的信息。
     * 返回的 {@code SystemDTO} 对象包含了所有这些信息。
     *
     * @return 包含系统信息的 {@code SystemDTO} 对象
     */
    @Override
    public SystemDTO getSystemInfo() {
        SystemDTO systemDTO = new SystemDTO();

        this.setCpuInfo(systemDTO);
        this.setMemoryInfo(systemDTO);
        this.setDiskInfo(systemDTO);
        this.setOsInfo(systemDTO);

        return systemDTO;
    }

    /**
     * 获取当前 JVM 的堆栈信息
     * <p>
     * 该方法收集当前 JVM 的运行时信息，包括内存使用情况、系统属性、线程状态等，
     * 并将这些信息封装到一个 {@code JvmStackDTO} 对象中返回。
     *
     * @return 包含 JVM 堆栈详细信息的 {@code JvmStackDTO} 对象
     */
    @Override
    public JvmStackDTO getJvmStackInfo() {
        JvmStackDTO jvmStackDTO = new JvmStackDTO();

        // 设置内存信息
        Runtime runtime = Runtime.getRuntime();
        jvmStackDTO.setTotalMemory(runtime.totalMemory());
        jvmStackDTO.setMaxMemory(runtime.maxMemory());
        jvmStackDTO.setFreeMemory(runtime.freeMemory());
        jvmStackDTO.setUsedMemory(runtime.totalMemory() - runtime.freeMemory());

        // 设置系统属性
        Properties properties = System.getProperties();
        Map<String, String> systemProperties = new HashMap<>();
        properties.forEach((key, value) -> systemProperties.put(key.toString(), value.toString()));
        jvmStackDTO.setSystemProperties(systemProperties);

        // 获取线程信息
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMxBean.dumpAllThreads(true, true);
        jvmStackDTO.setActiveThreadCount(threadMxBean.getThreadCount());

        // 处理线程堆栈信息
        List<JvmStackDTO.ThreadInfo> threadInfoList = Arrays.stream(threadInfos)
                .map(this::convertThreadInfo)
                .toList();
        jvmStackDTO.setThreadInfos(threadInfoList);

        return jvmStackDTO;
    }

    /**
     * 将 Java 的 ThreadInfo 对象转换为自定义的 ThreadInfo 对象
     *
     * @param threadInfo Java 的线程信息对象
     * @return 自定义的线程信息对象
     */
    private JvmStackDTO.@NotNull ThreadInfo convertThreadInfo(ThreadInfo threadInfo) {
        JvmStackDTO.ThreadInfo info = new JvmStackDTO.ThreadInfo();
        info.setThreadName(threadInfo.getThreadName());
        info.setThreadState(threadInfo.getThreadState().name());
        info.setPriority(Thread.currentThread().getPriority());
        info.setIsDaemon(Thread.currentThread().isDaemon());

        // 获取堆栈跟踪
        List<String> stackTrace = Arrays.stream(threadInfo.getStackTrace())
                .map(StackTraceElement::toString)
                .toList();
        info.setStackTrace(stackTrace);

        return info;
    }

    /**
     * 设置系统 CPU 信息
     * <p>
     * 该方法用于从系统环境变量和运行时信息中获取 CPU 的相关信息，并将这些信息设置到给定的 {@code SystemDTO} 对象中。
     * 具体来说，它会获取并设置 CPU 名称、CPU 核心数以及当前的 CPU 使用率。
     *
     * @param systemDTO 系统信息数据传输对象，用于存储获取到的 CPU 信息
     */
    private void setCpuInfo(@NotNull SystemDTO systemDTO) {
        // 获取 CPU 名称
        String cpuName = System.getenv("PROCESSOR_IDENTIFIER");
        systemDTO.setCpuName(cpuName != null ? cpuName : "未能获取");

        // 获取 CPU 核心数
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        systemDTO.setCpuCores(availableProcessors);

        // 获取系统 CPU 使用率
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double cpuLoad = osBean.getCpuLoad() * 100;
        systemDTO.setCpuUsage(cpuLoad);
    }

    /**
     * 设置内存信息
     * <p>
     * 该方法用于获取系统和JVM的内存使用情况，并将这些信息设置到给定的 {@code SystemDTO} 对象中。
     * 具体来说，该方法会获取系统的物理内存总量和空闲内存大小，以及JVM堆内存的初始值、已使用量和最大值。
     * 获取到的信息会被格式化为可读的字符串形式，然后通过相应的 setter 方法设置到 {@code SystemDTO} 对象中。
     *
     * @param systemDTO 系统信息的数据传输对象，用于存储内存信息
     */
    private void setMemoryInfo(@NotNull SystemDTO systemDTO) {
        // 获取物理内存信息
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long totalPhysicalMemorySize = osBean.getTotalMemorySize();
        long freePhysicalMemorySize = osBean.getFreeMemorySize();

        // 获取 JVM 堆内存使用情况
        MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMxBean.getHeapMemoryUsage();

        systemDTO.setTotalMemory(formatSize(totalPhysicalMemorySize));
        systemDTO.setFreeMemory(formatSize(freePhysicalMemorySize));
        systemDTO.setHeapMemoryInit(formatSize(heapMemoryUsage.getInit()));
        systemDTO.setHeapMemoryUsed(formatSize(heapMemoryUsage.getUsed()));
        systemDTO.setHeapMemoryMax(formatSize(heapMemoryUsage.getMax()));
    }

    /**
     * 设置系统磁盘信息
     * <p>
     * 该方法用于获取当前系统的磁盘信息，并将其设置到传入的 {@code SystemDTO} 对象中。
     * 它通过遍历文件存储（FileStore）来获取每个磁盘的总空间和可用空间，然后将这些信息格式化后设置到
     * {@code SystemDTO} 的相应字段中。如果在获取磁盘信息的过程中发生异常，则会将磁盘信息设置为0。
     *
     * @param systemDTO 用于存储系统磁盘信息的对象
     */
    private void setDiskInfo(@NotNull SystemDTO systemDTO) {
        try {
            // 获取文件系统的文件存储（FileStore）信息
            FileSystem fs = FileSystems.getDefault();
            Iterable<FileStore> fileStores = fs.getFileStores();
            for (FileStore fileStore : fileStores) {
                // 获取磁盘的总空间和可用空间
                long totalSpace = fileStore.getTotalSpace();
                long usableSpace = fileStore.getUsableSpace();

                // 设置系统DTO中的磁盘信息
                systemDTO.setTotalDiskSpace(formatSize(totalSpace));
                systemDTO.setFreeDiskSpace(formatSize(usableSpace));
            }
        } catch (IOException e) {
            // 错误处理
            systemDTO.setTotalDiskSpace(0);
            systemDTO.setFreeDiskSpace(0);
        }
    }

    /**
     * 设置操作系统信息
     * <p>
     * 该方法用于获取当前操作系统的名称、版本和架构，并将这些信息设置到给定的 {@code SystemDTO} 对象中。
     * 操作系统信息通过调用 {@code System.getProperty} 方法获取，包括 "os.name"、"os.version" 和 "os.arch" 属性。
     *
     * @param systemDTO 用于存储操作系统信息的目标对象，不能为空
     */
    private void setOsInfo(@NotNull SystemDTO systemDTO) {
        // 获取操作系统信息
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");

        systemDTO.setOsName(osName);
        systemDTO.setOsVersion(osVersion);
        systemDTO.setOsArchitecture(osArch);
    }

    /**
     * 格式化文件大小为 MB 单位
     * <p>
     * 该方法接收一个以字节为单位的文件大小，并将其转换为以兆字节（MB）为单位的大小。转换后的结果保留两位小数。
     *
     * @param size 文件大小，以字节为单位
     * @return 转换后的文件大小，以兆字节（MB）为单位，保留两位小数
     */
    // 格式化只返回 mb 大小
    private double formatSize(long size) {
        double mb = size / 1024.0 / 1024.0;
        DecimalFormat df = new DecimalFormat("0.00");
        return Double.parseDouble(df.format(mb));
    }
}
