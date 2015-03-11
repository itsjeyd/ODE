// IntegrationTest.java --- High-level tests for user-facing functionality.

// Copyright (C) 2013-2015  Tim Krones

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.

// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

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
