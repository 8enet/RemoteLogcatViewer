package com.zzzmode.android.server;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zl on 16/1/30.
 */
class EncodeUtils {

    public static final String DEFAULT_CHARSET = "UTF-8";

    /**
     * sha1
     * @param decript
     * @return
     */
    public static byte[] sha1(String decript) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(decript.getBytes(DEFAULT_CHARSET));
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] sha1(String... strs) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            for (String str : strs) {
                digest.update(str.getBytes(DEFAULT_CHARSET));
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 提权get 请求参数
     *
     * @param s 类似 aa=1&bb=2&cc=3
     * @return [key, value]
     */
    //TODO // FIXME: 16/1/31 这里为了简单，没有完全遵守http规范，只取了query对应value的第一个值
    public static Map<String, String> parseUrlQueryString(String s) {
        if (s == null) return new HashMap<String, String>(0);

        HashMap<String, String> map1 = new HashMap<String, String>();
        int p = 0;
        while (p < s.length()) {
            int p0 = p;
            while (p < s.length() && s.charAt(p) != '=' && s.charAt(p) != '&') p++;
            String name = urlDecode(s.substring(p0, p));
            if (p < s.length() && s.charAt(p) == '=') p++;
            p0 = p;
            while (p < s.length() && s.charAt(p) != '&') p++;
            String value = urlDecode(s.substring(p0, p));
            if (p < s.length() && s.charAt(p) == '&') p++;
            map1.put(name, value);
        }
        return map1;
    }


//    public static Map<String, String[]> parseUrlQueryString(String s) {
//        if (s == null) return new HashMap<String, String[]>(0);
//        // In map1 we use strings and ArrayLists to collect the parameter values.
//        HashMap<String, Object> map1 = new HashMap<String, Object>();
//        int p = 0;
//        while (p < s.length()) {
//            int p0 = p;
//            while (p < s.length() && s.charAt(p) != '=' && s.charAt(p) != '&') p++;
//            String name = urlDecode(s.substring(p0, p));
//            if (p < s.length() && s.charAt(p) == '=') p++;
//            p0 = p;
//            while (p < s.length() && s.charAt(p) != '&') p++;
//            String value = urlDecode(s.substring(p0, p));
//            if (p < s.length() && s.charAt(p) == '&') p++;
//            Object x = map1.get(name);
//            if (x == null) {
//                // The first value of each name is added directly as a string to the map.
//                map1.put(name, value);
//            } else if (x instanceof String) {
//                // For multiple values, we use an ArrayList.
//                ArrayList<String> a = new ArrayList<String>();
//                a.add((String) x);
//                a.add(value);
//                map1.put(name, a);
//            } else {
//                @SuppressWarnings("unchecked")
//                ArrayList<String> a = (ArrayList<String>) x;
//                a.add(value);
//            }
//        }
//        // Copy map1 to map2. Map2 uses string arrays to store the parameter values.
//        HashMap<String, String[]> map2 = new HashMap<String, String[]>(map1.size());
//        for (Map.Entry<String, Object> e : map1.entrySet()) {
//            String name = e.getKey();
//            Object x = e.getValue();
//            String[] v;
//            if (x instanceof String) {
//                v = new String[]{(String) x};
//            } else {
//                @SuppressWarnings("unchecked")
//                ArrayList<String> a = (ArrayList<String>) x;
//                v = new String[a.size()];
//                v = a.toArray(v);
//            }
//            map2.put(name, v);
//        }
//        return map2;
//    }

    private static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error in urlDecode.", e);
        }
    }

}
