package vn.courses.ut.edu.javaprogramming.bicap;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ErrorResponse;
import vn.courses.ut.edu.javaprogramming.bicap.exception.GlobalExceptionHandler;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression cho bug log production: khi client ngắt kết nối giữa lúc server đang stream SSE
 * (người dùng reload / đóng tab, đúng tình huống trong báo cáo lỗi), lần ghi socket kế tiếp
 * ném IOException. Response lúc đó đã commit với Content-Type {@code text/event-stream} nên
 * <b>không thể</b> ghi ErrorResponse — code cũ vẫn log ERROR kèm stack trace và còn sinh thêm
 * {@code HttpMessageNotWritableException}, lặp lại mỗi nhịp heartbeat 25 giây.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachLogCapture() {
        logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachLogCapture() {
        logger.detachAppender(appender);
    }

    private WebRequest requestWith(MockHttpServletResponse response) {
        return new ServletWebRequest(
                new MockHttpServletRequest("GET", "/api/notifications/stream"), response);
    }

    private boolean errorLogged() {
        return appender.list.stream().anyMatch(event -> event.getLevel() == Level.ERROR);
    }

    @Test
    void unexpectedExceptionOnNormalResponse_stillReturns500AndLogsError() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<ErrorResponse> result = handler.handleAllExceptions(
                new IllegalStateException("boom"), requestWith(response), response);

        assertNotNull(result, "lỗi thật vẫn phải trả về ErrorResponse 500");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertEquals("An unexpected error occurred", result.getBody().getMessage());
        assertTrue(errorLogged(), "lỗi thật vẫn phải được log ERROR để còn chẩn đoán");
    }

    @Test
    void clientAbortOnCommittedSseResponse_returnsNoBodyAndLogsNoError() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCommitted(true);

        // Đúng thông điệp đã thấy trong log production trên Windows:
        // java.io.IOException: An established connection was aborted by the software in your host machine
        IOException aborted = new IOException(
                "An established connection was aborted by the software in your host machine");

        ResponseEntity<ErrorResponse> result = handler.handleAllExceptions(aborted, requestWith(response), response);

        assertNull(result, "response đã commit thì không được cố ghi ErrorResponse");
        assertFalse(errorLogged(), "client ngắt kết nối là chuyện bình thường, không được log ERROR");
    }

    @Test
    void connectionResetMessage_isTreatedAsClientDisconnect() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);

        ResponseEntity<ErrorResponse> result = handler.handleAllExceptions(
                new IOException("Connection reset by peer"), requestWith(response), response);

        assertNull(result);
        assertFalse(errorLogged());
    }

    @Test
    void asyncRequestNotUsable_isTreatedAsClientDisconnect() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);

        ResponseEntity<ErrorResponse> result = handler.handleAllExceptions(
                new AsyncRequestNotUsableException("Servlet container error"), requestWith(response), response);

        assertNull(result, "Spring 6.1 bọc client-abort thành AsyncRequestNotUsableException");
        assertFalse(errorLogged());
    }

    @Test
    void tomcatClientAbortException_isTreatedAsClientDisconnect() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);

        ResponseEntity<ErrorResponse> result = handler.handleAllExceptions(
                new ClientAbortException(new IOException("Broken pipe")), requestWith(response), response);

        assertNull(result);
        assertFalse(errorLogged());
    }

    @Test
    void anyExceptionOnStreamingResponse_returnsNoBodyBecauseErrorResponseCannotBeWritten() {
        // Chưa commit nhưng Content-Type đã là text/event-stream: ghi ErrorResponse sẽ ném
        // HttpMessageNotWritableException ("No converter for ErrorResponse") như trong log cũ.
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);

        ResponseEntity<ErrorResponse> result = handler.handleAllExceptions(
                new RuntimeException("không ghi được frame SSE"), requestWith(response), response);

        assertNull(result);
        assertFalse(errorLogged());
    }

    @Test
    void committedNonStreamingResponse_alsoSkipsErrorBody() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCommitted(true);

        ResponseEntity<ErrorResponse> result = handler.handleAllExceptions(
                new RuntimeException("hỏng giữa lúc ghi file"), requestWith(response), response);

        assertNull(result, "response đã commit thì mọi nỗ lực ghi body đều vô nghĩa");
        assertFalse(errorLogged());
    }
}
