package com.common.schedule.admin.service;

import com.common.schedule.admin.model.TaskLog;
import com.common.schedule.admin.storage.TaskLogDao;
import com.common.schedule.admin.web.dto.CallbackRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 执行日志：接收执行器回调、按任务分页查询。
 */
@Service
public class LogService {

    private static final int MAX_LIMIT = 200;

    private final TaskLogDao taskLogDao;

    public LogService(TaskLogDao taskLogDao) {
        this.taskLogDao = taskLogDao;
    }

    public void callback(CallbackRequest request) {
        taskLogDao.updateCallback(request.getLogId(), request.isSuccess(), request.getCostMs(), request.getErrorMsg());
    }

    public List<TaskLog> query(String taskId, int limit, int offset) {
        int safeLimit = Math.min(Math.max(1, limit), MAX_LIMIT);
        int safeOffset = Math.max(0, offset);
        return taskLogDao.findByTask(taskId, safeLimit, safeOffset);
    }
}