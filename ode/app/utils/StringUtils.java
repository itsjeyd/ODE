package utils;

import java.util.List;


public class StringUtils {

    public static String join(List<String> strings, String conjunction) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String string : strings) {
            if (first) {
                first = false;
            } else {
                sb.append(conjunction);
            }
            sb.append(string);
        }
        return sb.toString();
    }

}
