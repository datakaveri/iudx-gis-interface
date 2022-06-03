package iudx.gis.server.authenticate.authorization;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.gis.server.authenticator.authorization.IudxAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(VertxExtension.class)
public class IudxAccessTest {
    @ParameterizedTest
    @EnumSource
    public void test(IudxAccess iudxAccess, VertxTestContext vertxTestContext){
        assertNotNull(iudxAccess);
        vertxTestContext.completeNow();
    }
    @Test
    public void test2(VertxTestContext vertxTestContext){
        assertEquals("api",IudxAccess.API.getAccess());
        assertEquals(IudxAccess.API, IudxAccess.fromAccess("api"));
        vertxTestContext.completeNow();
    }

}
