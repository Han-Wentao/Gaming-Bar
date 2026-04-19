package com.gamingbar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamingbar.entity.SmsCode;
import com.gamingbar.mapper.SmsCodeMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hamcrest.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "app.jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
})
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SmsCodeMapper smsCodeMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetTables() {
        jdbcTemplate.update("delete from t_message");
        jdbcTemplate.update("delete from t_room_user");
        jdbcTemplate.update("delete from t_room");
        jdbcTemplate.update("delete from t_sms_code");
        jdbcTemplate.update("delete from t_user");
    }

    @Test
    void shouldExposeRootEndpoint() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.service").value("GamingBar API"))
            .andExpect(jsonPath("$.data.status").value("running"))
            .andExpect(jsonPath("$.data.base_path").value("/api"));
    }

    @Test
    void shouldLoginUpdateProfileAndLoadGames() throws Exception {
        String token = login("13812345678");

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.phone").value("13812345678"))
            .andExpect(jsonPath("$.data.avatar").value(""));

        mockMvc.perform(put("/api/users/profile")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nickname": "新玩家",
                      "avatar": "https://cdn.example.com/avatar.png"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.nickname").value("新玩家"))
            .andExpect(jsonPath("$.data.avatar").value("https://cdn.example.com/avatar.png"));

        mockMvc.perform(get("/api/games").header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.length()").value(5))
            .andExpect(jsonPath("$.data[0].game_name").value("王者荣耀"));
    }

    @Test
    void shouldEnforceOneRoomRuleAndHandleJoinLeaveMessage() throws Exception {
        String ownerToken = login("13800000001");
        String memberToken = login("13800000002");

        long roomId = createRoom(ownerToken, 2);

        mockMvc.perform(post("/api/rooms/{roomId}/join", roomId).header("Authorization", bearer(memberToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.status").value("ready"))
            .andExpect(jsonPath("$.data.current_player").value(2));

        mockMvc.perform(post("/api/rooms")
                .header("Authorization", bearer(memberToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "game_id": 1,
                      "max_player": 4,
                      "type": "instant"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(409))
            .andExpect(jsonPath("$.message").value("您已在其他未关闭房间中"));

        mockMvc.perform(post("/api/rooms/{roomId}/messages", roomId)
                .header("Authorization", bearer(memberToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "大家好"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.content").value("大家好"));

        mockMvc.perform(get("/api/rooms/{roomId}/messages", roomId).header("Authorization", bearer(ownerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.messages.length()").value(1))
            .andExpect(jsonPath("$.data.messages[0].content").value("大家好"));

        mockMvc.perform(post("/api/rooms/{roomId}/leave", roomId).header("Authorization", bearer(memberToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.action").value("left"));

        mockMvc.perform(delete("/api/rooms/{roomId}", roomId).header("Authorization", bearer(ownerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").value(Matchers.nullValue()));

        mockMvc.perform(get("/api/rooms/{roomId}", roomId).header("Authorization", bearer(ownerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldCleanupExpiredRoomBeforeCreatingNewRoom() throws Exception {
        String token = login("13800000003");
        long expiredRoomId = createRoom(token, 4);
        jdbcTemplate.update("update t_room set create_time = dateadd('HOUR', -3, current_timestamp) where id = ?", expiredRoomId);

        mockMvc.perform(post("/api/rooms")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "game_id": 2,
                      "max_player": 4,
                      "type": "instant"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.game_id").value(2));

        mockMvc.perform(get("/api/rooms/{roomId}", expiredRoomId).header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldRejectRoomDetailForNonMembers() throws Exception {
        String ownerToken = login("13800000004");
        String outsiderToken = login("13800000005");

        long roomId = createRoom(ownerToken, 4);

        mockMvc.perform(get("/api/rooms/{roomId}", roomId).header("Authorization", bearer(outsiderToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(403));
    }

    private String login(String phone) throws Exception {
        mockMvc.perform(post("/api/auth/sms/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + phone + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        SmsCode smsCode = smsCodeMapper.selectLatestUnusedByPhone(phone);
        assertThat(smsCode).isNotNull();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"" + phone + "\",\"code\":\"" + smsCode.getSmsCode() + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();

        JsonNode root = readBody(result);
        return root.path("data").path("token").asText();
    }

    private long createRoom(String token, int maxPlayer) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/rooms")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "game_id": 1,
                      "max_player": %d,
                      "type": "instant"
                    }
                    """.formatted(maxPlayer)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andReturn();
        return readBody(result).path("data").path("id").asLong();
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
