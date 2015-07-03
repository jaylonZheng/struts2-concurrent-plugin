package org.le.util;

import ognl.Ognl;
import ognl.OgnlException;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.Reader;

/**
 * @author ltebean
 */
public class YamlConfig {
    private static Object config;

    static {
        try {
            Yaml yaml = new Yaml();
            config = yaml.load(YamlConfig.class.getClassLoader().getResourceAsStream("pipe.config"));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("init params error.please make sure has a config " +
                    "file named 【pipe.config】and put it at root resources dirctory");
        }
    }
    public static String getAsString(String expression){
        return get(expression, String.class);
    }

    public static <T> T get(String expression, Class<T> clazz) {
        try {
            final Object ognlTree = Ognl.parseExpression(expression);
            return (T) Ognl.getValue(ognlTree, config, clazz);
        } catch (OgnlException e) {
            throw new RuntimeException("falied to get config with expression: " + expression, e);
        }
    }

    public static <T> T get(String expression, T defaultValue, Class<T> clazz) {
        try {
            T value = get(expression, clazz);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

}
