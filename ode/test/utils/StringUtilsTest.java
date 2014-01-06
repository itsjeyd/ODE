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
