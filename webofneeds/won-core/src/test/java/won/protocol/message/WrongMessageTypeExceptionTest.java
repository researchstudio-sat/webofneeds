package won.protocol.message;

import org.junit.Assert;
import org.junit.Test;

public class WrongMessageTypeExceptionTest {
    @Test
    public void testExceptionMessage() {
        WrongMessageTypeException ex = new WrongMessageTypeException(WonMessageType.ACTIVATE,
                        WonMessageType.DEACTIVATE);
        Assert.assertEquals("Message has the wrong message type, expected: ACTIVATE, actual: DEACTIVATE",
                        ex.getMessage());
    }
}
