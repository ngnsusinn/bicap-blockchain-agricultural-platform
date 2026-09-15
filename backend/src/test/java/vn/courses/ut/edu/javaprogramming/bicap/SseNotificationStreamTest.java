package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.courses.ut.edu.javaprogramming.bicap.controller.NotificationController;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.exception.GlobalExceptionHandler;
import vn.courses.ut.edu.javaprogramming.bicap.repository.NotificationRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.impl.NotificationServiceImpl;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SSE stream thông báo (/api/notifications/stream) với {@link NotificationServiceImpl} thật:
 *
 * <ul>
 *   <li>Frame mở đầu {@code connected} được gửi ngay khi mở stream — trước đây trình duyệt chỉ
 *       nhận header sau nhịp heartbeat 25 giây, nên rất khó phân biệt "đang kết nối" và "treo".</li>
 *   <li>Emitter đã chết (client reload/đóng tab) phải được {@code complete()} và xoá khỏi
 *       registry ngay tại nhịp heartbeat lỗi đầu tiên, không để mỗi nhịp 25 giây lại ghi vào
 *       socket đã đóng và ném IOException ra tầng async của servlet container.</li>
 * </ul>
 */
class SseNotificationStreamTest {

    private NotificationServiceImpl notificationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(
                mock(NotificationRepository.class), mock(UserRepository.class));
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificationController(notificationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        Role farmRole = Role.builder().id(4L).name("FARM_MANAGER").permissions(Set.of()).build();
        User farmOwner = User.builder()
                .id(10L).email("farmer@bicap.com").password("x")
                .fullName("Chủ Trang Trại").status(UserStatus.ACTIVE).roles(Set.of(farmRole))
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(farmOwner, null, farmOwner.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void stream_sendsConnectedFrameImmediately() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/notifications/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("event:" + NotificationServiceImpl.CONNECTED_EVENT),
                "frame đầu tiên phải là 'connected', thực tế: [" + body + "]");
        assertTrue(body.contains("data:ok"), "frame connected phải có payload, thực tế: [" + body + "]");
        assertEquals(MediaType.TEXT_EVENT_STREAM_VALUE, result.getResponse().getContentType());
    }

    @Test
    void heartbeat_whenClientIsGone_completesEmitterAndDropsItFromRegistry() throws Exception {
        AtomicBoolean completed = new AtomicBoolean(false);
        SseEmitter deadEmitter = new SseEmitter(0L) {
            @Override
            public void send(SseEventBuilder builder) throws IOException {
                // Đúng lỗi Tomcat ném ra khi client đã ngắt kết nối.
                throw new IOException("An established connection was aborted by the software in your host machine");
            }

            @Override
            public void complete() {
                completed.set(true);
            }
        };
        emittersOf(notificationService).computeIfAbsent(10L, key -> new CopyOnWriteArrayList<>()).add(deadEmitter);

        notificationService.heartbeat(); // không được ném IOException ra ngoài

        assertTrue(completed.get(), "emitter chết phải được complete() để Spring đóng hẳn");
        assertTrue(emittersOf(notificationService).isEmpty(),
                "emitter chết phải bị xoá khỏi registry ngay nhịp heartbeat đầu tiên");
    }

    @Test
    void subscribe_registersEmitterAndKeepsItForLaterNotifications() throws Exception {
        SseEmitter emitter = notificationService.subscribe(10L);

        assertNotNull(emitter);
        assertEquals(1, emittersOf(notificationService).get(10L).size());

        // Emitter còn sống: heartbeat không được complete() nó.
        notificationService.heartbeat();
        assertEquals(1, emittersOf(notificationService).get(10L).size());
    }

    @SuppressWarnings("unchecked")
    private static Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersOf(NotificationServiceImpl service)
            throws Exception {
        Field field = NotificationServiceImpl.class.getDeclaredField("emitters");
        field.setAccessible(true);
        return (Map<Long, CopyOnWriteArrayList<SseEmitter>>) field.get(service);
    }
}
