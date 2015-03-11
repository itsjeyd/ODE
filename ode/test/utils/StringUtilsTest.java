// StringUtilsTest.java --- Test utilities for building strings.

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

package utils;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;


public class StringUtilsTest {

    @Test
    public void joinTest() {
        List<String> stringsToJoin = new ArrayList<String>();
        String conjunction = " AND ";
        assertThat(StringUtils.join(stringsToJoin, conjunction))
            .isEqualTo("");
        stringsToJoin.add("foo");
        assertThat(StringUtils.join(stringsToJoin, conjunction))
            .isEqualTo("foo");
        stringsToJoin.add("bar");
        assertThat(StringUtils.join(stringsToJoin, conjunction))
            .isEqualTo("foo AND bar");
        stringsToJoin.add("baz");
        assertThat(StringUtils.join(stringsToJoin, conjunction))
            .isEqualTo("foo AND bar AND baz");
    }

}
