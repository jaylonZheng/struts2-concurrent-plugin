package org.le.util;

import ognl.Ognl;
import ognl.OgnlException;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.Reader;


public class YamlConfig {
    private static Object config;
    private static boolean hasConfigFile;

    static {
        try {
            Yaml yaml = new Yaml();
            InputStream configFile = YamlConfig.class.getClassLoader().getResourceAsStream("pipe.config");
            if (config != null){
                hasConfigFile = true;
                config = yaml.load(configFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("init params error.please make sure has a config " +
                    "file named 【pipe.config】and put it at root resources dirctory");
        }
    }

    public static boolean hasConfigFile(){
        return hasConfigFile;
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
