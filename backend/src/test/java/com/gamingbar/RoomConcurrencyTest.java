package com.gamingbar;

import com.gamingbar.dto.room.CreateRoomRequest;
import com.gamingbar.entity.Room;
import com.gamingbar.entity.User;
import com.gamingbar.mapper.RoomMapper;
import com.gamingbar.mapper.RoomUserMapper;
import com.gamingbar.mapper.UserMapper;
import com.gamingbar.service.RoomService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "app.jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
})
@AutoConfigureMockMvc
class RoomConcurrencyTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private RoomUserMapper roomUserMapper;

    @Autowired
    private UserMapper userMapper;

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
    void shouldKeepRoomCapacitySafeUnderHundredConcurrentJoins() throws Exception {
        Long ownerId = createUser("13811110000");
        CreateRoomRequest request = new CreateRoomRequest();
        request.setGameId(1);
        request.setMaxPlayer(10);
        request.setType("instant");
        Long roomId = roomService.createRoom(ownerId, request).getId();

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        for (int index = 0; index < threadCount; index++) {
            Long userId = createUser("1382222" + String.format("%04d", index));
            executorService.submit(() -> {
                try {
                    startGate.await();
                    roomService.joinRoom(userId, roomId);
                    successCount.incrementAndGet();
                } catch (Exception exception) {
                    failureCount.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertThat(doneGate.await(30, TimeUnit.SECONDS)).isTrue();
        executorService.shutdownNow();

        Room room = roomMapper.selectById(roomId);
        assertThat(successCount.get()).isEqualTo(9);
        assertThat(failureCount.get()).isEqualTo(91);
        assertThat(room.getCurrentPlayer()).isEqualTo(10);
        assertThat(roomUserMapper.countByRoomId(roomId)).isEqualTo(10);
    }

    private Long createUser(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickname("user-" + phone.substring(phone.length() - 4));
        user.setAvatar("");
        user.setCreditScore(100);
        userMapper.insert(user);
        return user.getId();
    }
}
