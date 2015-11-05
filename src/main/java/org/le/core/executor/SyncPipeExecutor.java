package org.le.core.executor;

import org.le.Exception.PipeParamInitException;
import org.le.bean.PipeProxy;
import org.le.core.DefaultFreemarkerRenderer;
import org.le.core.FreemarkerRenderer;
import org.le.core.PipeExecutor;
import org.le.core.extention.PipeBackup;
import org.le.core.extention.PipeCache;
import org.le.core.extention.PipeDowngrade;
import org.le.util.InjectUtils;
import org.le.util.YamlConfig;
import org.springframework.util.CollectionUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncPipeExecutor implements PipeExecutor {

    private static SyncPipeExecutor instance = new SyncPipeExecutor();
    private FreemarkerRenderer renderer = DefaultFreemarkerRenderer.newIntance();
   
    public final static String DOWNGRADE_CONFIG="downgrade";
    public final static String BACKUP_CONFIG="backup";
    public final static String CACHE_CONFIG="cache";
    
    private static PipeDowngrade downgrade;
    private static PipeBackup backup;
    private static PipeCache cache;
    private boolean devMode;
    
    public static boolean initFlag=false;
    /**
     * 由原来静态初识方法　改成加载函数
     * @param configPath
     */
    public static void loadConfig(String configPath){
    	if(initFlag)
    		return;
		try {
	        Map<String, String> extenInfo = new HashMap<String, String>();
	        List<String> extenKeys = Arrays.asList("downgrade", "cache", "backup");
	        //未进行初始化
	        if(YamlConfig.isInit()==false){
	        	YamlConfig.load(configPath);
	        }
	        if(YamlConfig.getConfig()!=null)
	            for(String key: extenKeys){
	            	String value=YamlConfig.getAsString(key,null);
	            	if(value!=null)
	            		extenInfo.put(key,value);
	            }
	       initExtentionParam(extenInfo);
	       initFlag=true;
	       
	    } catch (Exception e) {
	        e.printStackTrace();
	        throw new RuntimeException("");
	    }
		
    }
    

    private static void initExtentionParam(Map<String, String> classNames) {
        if (CollectionUtils.isEmpty(classNames)) {
            return;
        }
        for (String extenType : classNames.keySet()) {
            try {
                Class clazz = Class.forName(classNames.get(extenType));
                if (DOWNGRADE_CONFIG.equals(extenType)) {
                    downgrade = (PipeDowngrade) clazz.newInstance();
                } else if (CACHE_CONFIG.equals(extenType)) {
                    cache = (PipeCache) clazz.newInstance();
                } else if (BACKUP_CONFIG.equals(extenType)) {
                    backup = (PipeBackup) clazz.newInstance();
                }
            } catch (Exception e) {
            	e.printStackTrace();
                throw new PipeParamInitException("init pipe cache object error!");
            }
        }
    }

    private SyncPipeExecutor() {
    }


    public static SyncPipeExecutor newInstance() {
        return instance;
    }

    public Object execute(PipeProxy pipe) {
        //先从cache里获取
        Object backupRenderResult = getResultFromCache(pipe);
        if (backupRenderResult != null)
            return backupRenderResult;
        try {
            pipe.execute();
            Object renderResult = render(pipe);
            backup(pipe, renderResult);
            pipe.setRenderResult(renderResult);
            return renderResult;
        } catch (Exception e) {
            if (isDevMode()) {
                return generateExceptionToPrintStack(e);
            } else {
                if (downgrade != null) {
                    Object backupResult = downgrade.downgrade(pipe);
                    if (backupResult != null)
                        return backupResult;
                    else
                        return "";
                } else {
                    return "";
                }
            }
        }
    }

    public Map<String, Object> execute(List<PipeProxy> pipes) {
        Map<String, Object> result = new HashMap<String, Object>();
        for (PipeProxy pipe : pipes)
            result.put(pipe.getKey(), execute(pipe));
        return result;
    }

    private Object getResultFromCache(PipeProxy pipe) {
        if (cache != null)
            return cache.getCachedPipe(pipe);
        return null;
    }

    private void backup(PipeProxy pipe, Object renderResult) {
        if (backup != null)
            backup.backup(pipe, renderResult);
    }

    private Object render(PipeProxy pipe) {
        Map<String, Object> context = InjectUtils.getFieldValueForFreemarker(pipe.getPipe());
        String ftl = pipe.getFtl();
        return renderer.render(ftl, context);
    }

    private String generateExceptionToPrintStack(Exception e) {
        StringBuilder result = new StringBuilder();
        result.append("<div style=\"background-color: #eee;font-size:9px;font-family: " +
                "Consolas,Menlo,Monaco;height:250px;overflow:scroll\">");
        result.append("<font style=\"color:red\">")
                .append(e.toString())
                .append("</font></br>");
        for (StackTraceElement element : e.getStackTrace()) {
            result.append(element.toString() + "</br>");
        }
        result.append("</div>");
        return result.toString();
    }

    public PipeDowngrade getDowngrade() {
        return downgrade;
    }

    public void setDowngrade(PipeDowngrade downgrade) {
        this.downgrade = downgrade;
    }

    public boolean isDevMode() {
        return devMode;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public void setBackup(PipeBackup backup) {
        this.backup = backup;
    }

    public void setCache(PipeCache cache) {
        this.cache = cache;
    }
}
