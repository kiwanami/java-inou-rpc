import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class RPCTestSuite extends TestCase {
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(EncoderTest.class));
        suite.addTest(new TestSuite(AddressAccepterTest.class));
        suite.addTest(new TestSuite(ClientServerTest.class));
        suite.addTest(new TestSuite(MultiBinServerTest.class));
        suite.addTest(new TestSuite(StreamTest.class));
        return suite;
    }
}
