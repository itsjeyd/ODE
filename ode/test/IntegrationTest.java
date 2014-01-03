import org.junit.*;

import play.test.*;
import play.libs.F.*;

import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;


public class IntegrationTest {

    /**
     * Add your integration tests here.
     * In this example we just check if the home page is being shown.
     */
    @Test
    public void test() {
        running(
            testServer(3333, fakeApplication(inMemoryDatabase())),
            HTMLUNIT, new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
                browser.goTo("http://localhost:3333");
                assertThat(
                    browser.pageSource()).contains("Hi! This is Ode.");
            }
        });
    }

}
