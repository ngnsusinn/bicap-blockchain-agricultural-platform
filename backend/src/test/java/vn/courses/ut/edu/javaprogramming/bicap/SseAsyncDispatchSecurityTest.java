package vn.courses.ut.edu.javaprogramming.bicap;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.DispatcherType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Regression cho lỗi log lặp trong console khi chạy backend:
 *
 * <pre>
 * ERROR ... Servlet.service() for servlet [dispatcherServlet] threw exception
 * org.springframework.security.access.AccessDeniedException: Access Denied
 *     at ...AuthorizationFilter.doFilter(AuthorizationFilter.java:98)
 * ERROR ... threw exception [Unable to handle the Spring Security Exception because the
 *              response is already committed.] with root cause
 * org.springframework.security.access.AccessDeniedException: Access Denied
 * </pre>
 *
 * <p>Nguyên nhân: {@code /api/notifications/stream} trả {@code SseEmitter} nên request chạy
 * async; container dispatch lại request với {@link DispatcherType#ASYNC}. Ở lần dispatch
 * này {@code JwtAuthenticationFilter} (OncePerRequestFilter) KHÔNG chạy → SecurityContext
 * rỗng → {@code anyRequest().authenticated()} từ chối dù response đã commit.
 *
 * <p>Sau fix: {@code dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()} cho lần
 * dispatch async đi qua (request gốc đã được xác thực ở lần dispatch REQUEST).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SseAsyncDispatchSecurityTest {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("Dispatch ASYNC không bị AuthorizationFilter chặn (đại diện cho SSE stream)")
    void asyncDispatch_isNotRejectedBySecurityChain() throws Exception {
        // Không có SecurityContext ở lần dispatch async (giống SSE) nên controller trả 401
        // "chưa đăng nhập" — điều quan trọng là KHÔNG còn 403 AccessDeniedException từ
        // AuthorizationFilter (nguyên nhân gây log "response is already committed").
        mvc.perform(get("/api/notifications/unread-count")
                        .with(request -> {
                            request.setDispatcherType(DispatcherType.ASYNC);
                            return request;
                        }))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Dispatch ERROR tới /error không bị chặn (nguyên nhân log 'response is already committed')")
    void errorDispatch_toErrorPage_isNotRejectedBySecurityChain() throws Exception {
        // Khi async request kết thúc ở trạng thái lỗi (ví dụ emitter SSE ghi vào socket đã
        // chết), Tomcat dispatch tiếp trang lỗi /error với DispatcherType.ERROR. Lần dispatch
        // này KHÔNG có SecurityContext nên trước đây bị AuthorizationFilter từ chối → sinh
        // "AccessDeniedException: Access Denied" + "Unable to handle the Spring Security
        // Exception because the response is already committed" lặp lại mỗi nhịp heartbeat.
        int status = mvc.perform(get("/error")
                        .with(request -> {
                            request.setDispatcherType(DispatcherType.ERROR);
                            return request;
                        }))
                .andReturn().getResponse().getStatus();

        assertNotEquals(401, status, "trang lỗi của container không được đòi xác thực");
        assertNotEquals(403, status, "trang lỗi của container không được trả AccessDenied");
    }

    @Test
    @DisplayName("Dispatch REQUEST vẫn yêu cầu xác thực như trước (không nới lỏng bảo mật)")
    void requestDispatch_stillRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isForbidden());

        // SSE vẫn phải có token hợp lệ ở lần gọi đầu tiên.
        mvc.perform(get("/api/notifications/stream"))
                .andExpect(status().isForbidden());
    }
}
