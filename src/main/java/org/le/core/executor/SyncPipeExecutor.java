package org.le.core.executor;

import org.apache.commons.lang3.StringUtils;
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncPipeExecutor implements PipeExecutor {

    private static SyncPipeExecutor instance = new SyncPipeExecutor();
    private FreemarkerRenderer renderer = DefaultFreemarkerRenderer.newIntance();
    private static PipeDowngrade downgrade;
    private static PipeBackup backup;
    private static PipeCache cache;
    private boolean devMode;

    //初始化参数
    static {
        try {
            initDowngradeParam(YamlConfig.getAsString("downgrade"));
            initCacheParam(YamlConfig.getAsString("cache"));
            initBackupParam(YamlConfig.getAsString("backup"));
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("init params error.please make sure has a config " +
                    "file named 【pipe.config】and put it at root resources dirctory");
        }
    }
    private static void initDowngradeParam(String downgradeClassName) {
        if (StringUtils.isNotEmpty(downgradeClassName)) {
            try {
                Class pipeDowngradeClass = Class.forName(downgradeClassName);
                downgrade = (PipeDowngrade) pipeDowngradeClass.newInstance();
            } catch (Exception e) {
                throw new PipeParamInitException("init pipe downgrade object error!");
            }
        }
    }

    private static void initBackupParam(String backupClassName) {
        if (StringUtils.isNotEmpty(backupClassName)) {
            try {
                Class clazz = Class.forName(backupClassName);
                backup = (PipeBackup) clazz.newInstance();
            } catch (Exception e) {
                throw new PipeParamInitException("init pipe backup object error!");
            }
        }
    }

    private static void initCacheParam(String cacheClassName) {
        if (StringUtils.isNotEmpty(cacheClassName)) {
            try {
                Class clazz = Class.forName(cacheClassName);
                cache = (PipeCache) clazz.newInstance();
            } catch (Exception e) {
                throw new PipeParamInitException("init pipe cache object error!");
            }
        }
    }

    private SyncPipeExecutor() {
    }


    public static SyncPipeExecutor newInstance() {
        return instance;
    }

    @Override
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

    @Override
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

    private Object render(PipeProxy pipe){
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
