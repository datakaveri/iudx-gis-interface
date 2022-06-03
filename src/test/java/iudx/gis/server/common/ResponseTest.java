package iudx.gis.server.common;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class ResponseTest {
    private String type;
    private int status;
    private String title;
    private String detail;
    @Mock
    Throwable failureHandler;


    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
        type = ResponseUrn.INVALID_TOKEN_URN.getUrn();
        status = HttpStatus.SC_NOT_FOUND;
        title = ResponseUrn.INVALID_TOKEN_URN.getMessage();
        detail = failureHandler.getLocalizedMessage();
        vertxTestContext.completeNow();
    }

    @DisplayName("Test withUrn in Builder")
    @Test
    public void testWithUrn(VertxTestContext vertxTestContext) {

        Response response = new Response.Builder()
                .withUrn(type)
                .withTitle(title)
                .withStatus(status)
                .withDetail(detail).build();

        JsonObject expected = new JsonObject()
                .put("type", response.getType())
                .put("status", response.getStatus())
                .put("title", response.getTitle())
                .put("detail", response.getDetail());

        String actual = response.toString();
        Assertions.assertEquals(expected.encode(), actual);
        vertxTestContext.completeNow();
    }

}
