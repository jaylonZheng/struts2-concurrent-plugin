package org.le.util;

import java.io.File;

import org.le.core.executor.SyncPipeExecutor;
import org.yaml.snakeyaml.Yaml;

import ognl.Ognl;
import ognl.OgnlException;

/**
 * @author ltebean
 */
public class YamlConfig {
    private static Object config;
    //初始化标记
    private static boolean initFlag=false;
    /*static {
        try {
            Yaml yaml = new Yaml();
            config = yaml.load(YamlConfig.class.getClassLoader().getResourceAsStream("pipe.config"));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("init params error.please make sure has a config " +
                    "file named 【pipe.config】and put it at root resources dirctory");
        }
    }*/
    /**
     * 如果指定了配置文件位置则加载为文件．
     * 如果配置文件路径没有指定，则加载默认文件．
     * @param pipeConfigPath
     * @return
     */
    public static Object loadConfig(String pipeConfigPath){
        initFlag=true;
    	if(pipeConfigPath!=null)
    		return load(pipeConfigPath);
    	return config=load();
    
    }
    public static Object getConfig(){
    	return config;
    }
    /**
     *默认情况下调用　pipe.config
     * @return
     */
    public static Object load(){
    	return load("pipe.config");
    }    
    /**
     * 根据制定的配置文件路径，加载降级配置文件．
     * １.如果文件名为null,表示用户没有设置该内容.则看是否在类路径下存在pipe.config.
     * ２.如果文件名长度为０，抛出异常
     * ３．如果找不到文件路径，抛出异常
     * ４．加载成功后则验证文件内容，即要求　backup和cache要么同时出现要么都不出现．否则报错
     * 
     * @param pipeConfigPath
     * @return　null if can't find
     */
    public static  Object load(String pipeConfigPath){
    	
    	if(pipeConfigPath == null)
    	{//用户没有设置该值,则看是否在类路径下存在pipe.config
    		return load("pipe.config");
    	}
    	
    	if(pipeConfigPath.trim().length() == 0)
    		throw new RuntimeException("value of struts.concurrent.plugin.configPath　shouldn't be blank");    
    	
    	/*File temp=new File(pipeConfigPath);
    	if(!temp.exists())
    		throw new RuntimeException("could't find the file: "+pipeConfigPath);*/
    	
    	try {
            Yaml yaml = new Yaml();
            config = yaml.load(YamlConfig.class.getClassLoader().getResourceAsStream(pipeConfigPath));
    	} catch (Exception e) {
        	e.printStackTrace();
            throw new RuntimeException("load config error.please make sure has a config " +
                    "file named 【"+pipeConfigPath+"】");
        }
         
        if(validationOfContent(config,pipeConfigPath)){
        	return config;
        }else{
        	throw new RuntimeException("backup and cache must set together or not! ");
        }                   
    }
    public static boolean isInit(){
    	return initFlag;
    }

    private static boolean validationOfContent(Object config2,String pipeConfigPath) {
		String backup = getAsString("backup",null);
		String cache = getAsString("cache",null);
		
		if(backup != null && cache == null)
			throw new RuntimeException("cache could't be null when backup have value. please set value of cache in 【"+pipeConfigPath+"】");
		if(backup == null && cache != null)
			throw new RuntimeException("backup could't be null when cache have value. please set value of backup in 【"+pipeConfigPath+"】"); 
		return true;
	}

	public static String getAsString(String expression){
        return get(expression, String.class);
    }
	public static String getAsString(String expression,String defaultValue){
        return get(expression,defaultValue, String.class);
    }
    public static <T> T get(String expression, Class<T> clazz) {
    	if(config == null)
    		return null;
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
