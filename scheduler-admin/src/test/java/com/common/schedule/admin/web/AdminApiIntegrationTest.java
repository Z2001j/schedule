package com.common.schedule.admin.web;

import com.common.schedule.admin.model.ScheduleType;
import com.common.schedule.admin.web.dto.CallbackRequest;
import com.common.schedule.admin.web.dto.RegisterRequest;
import com.common.schedule.admin.web.dto.TaskMeta;
import com.common.schedule.admin.web.dto.UpdateScheduleRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "scheduler.admin.access-token=testtoken")
@AutoConfigureMockMvc
class AdminApiIntegrationTest {

    private static final String TOKEN = "testtoken";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private void register(String taskId, String cron, boolean enabled) throws Exception {
        TaskMeta meta = new TaskMeta();
        meta.setTaskId(taskId);
        meta.setName("demo");
        meta.setType(ScheduleType.CRON);
        meta.setCron(cron);
        meta.setEnabled(enabled);
        RegisterRequest request = new RegisterRequest();
        request.setAppName("webapp");
        request.setAddress("http://127.0.0.1:59999");
        request.setTasks(List.of(meta));
        mockMvc.perform(post("/api/registry/register")
                        .header("X-Access-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void registerWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/registry/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"appName\":\"x\",\"address\":\"http://a:1\",\"tasks\":[]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void malformedJsonBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/registry/register")
                        .header("X-Access-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-valid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void registerWithTokenThenListShowsTask() throws Exception {
        String taskId = "webapp:listBean.run";
        register(taskId, "0 0 0 * * ?", false);
        mockMvc.perform(get("/api/tasks").header("X-Access-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.taskId=='" + taskId + "')]").exists());
    }

    @Test
    void managementApiWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
        mockMvc.perform(get("/api/executors"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void invalidCronUpdateReturnsBadRequest() throws Exception {
        String taskId = "webapp:badCronBean.run";
        register(taskId, "0 0 0 * * ?", false);
        UpdateScheduleRequest req = new UpdateScheduleRequest();
        req.setType(ScheduleType.CRON);
        req.setCron("not-a-cron");
        mockMvc.perform(put("/api/tasks/" + taskId + "/schedule")
                        .header("X-Access-Token", TOKEN)
                        .param("version", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void manualTriggerThenCallbackUpdatesLog() throws Exception {
        String taskId = "webapp:callbackBean.run";
        register(taskId, "0 0 0 * * ?", false);

        mockMvc.perform(post("/api/tasks/" + taskId + "/trigger").header("X-Access-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        MvcResult result = mockMvc.perform(get("/api/tasks/" + taskId + "/logs").header("X-Access-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").exists())
                .andReturn();
        long logId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").get(0).path("id").asLong();
        assertThat(logId).isPositive();

        CallbackRequest callback = new CallbackRequest();
        callback.setLogId(logId);
        callback.setSuccess(true);
        callback.setCostMs(42L);
        mockMvc.perform(post("/api/log/callback")
                        .header("X-Access-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callback)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks/" + taskId + "/logs").header("X-Access-Token", TOKEN))
                .andExpect(jsonPath("$.data[0].success").value(true))
                .andExpect(jsonPath("$.data[0].costMs").value(42));
    }
}