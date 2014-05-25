package utils;

import java.nio.charset.Charset;
import java.util.UUID;


public class UUIDGenerator {

    public static String random() {
        return UUID.randomUUID().toString();
    }

    public static String from(String seed) {
        byte[] bytes = seed.getBytes(Charset.forName("UTF-8"));
        return UUID.nameUUIDFromBytes(bytes).toString();
    }

}
