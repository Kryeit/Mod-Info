package org.pipeman.mod_info;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class Utils {
    public static <T, R> List<R> map(List<T> list, Function<T, R> mappingFunction) {
        List<R> output = new ArrayList<>(list.size());
        for (T t : list) {
            output.add(mappingFunction.apply(t));
        }
        return output;
    }

    public static <T, R> List<R> map(T[] list, Function<T, R> mappingFunction) {
        List<R> output = new ArrayList<>(list.length);
        for (T t : list) {
            output.add(mappingFunction.apply(t));
        }
        return output;
    }

    public static <T> List<T> filter(List<T> list, Predicate<T> filterFunction) {
        List<T> output = new ArrayList<>();
        for (T t : list) {
            if (filterFunction.test(t)) {
                output.add(t);
            }
        }
        return output;
    }

    public static String hash(String s) {
        try {
            StringBuilder result = new StringBuilder();
            for (byte b : MessageDigest.getInstance("sha-1").digest(s.getBytes())) {
                result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
