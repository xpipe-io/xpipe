import io.xpipe.app.util.ThreadHelper;

public class Test {

    @org.junit.jupiter.api.Test
    public void test() {
        System.out.println("a");
        ThreadHelper.sleep(1000);
    }
}
