package org.le.core.executor;

import org.apache.log4j.Logger;
import org.le.bean.PipeProxy;
import org.le.core.PipeExecutor;
import org.le.core.concurrent.ConcurrentPipeWorker;
import org.le.core.factory.ThreadPoolExecutorFactoryImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class ConcurrentPipeExecutor implements PipeExecutor {
    Logger logger = Logger.getLogger("logger");
    private ThreadPoolExecutor executor = ThreadPoolExecutorFactoryImpl.
            newInstance().instanceOfDefaultConfig();

    private SyncPipeExecutor syncPipeExecutor = SyncPipeExecutor.newInstance();

    @Override
    public Object execute(PipeProxy pipe) {
        return syncPipeExecutor.execute(pipe);
    }

    @Override
    public Map<String, Object> execute(List<PipeProxy> pipes) {
        return execute(pipes, Integer.MAX_VALUE);
    }

    /**
     * 有超时时间限制的页面
     *
     * @param pipes
     * @param ms    超时时间 单位毫秒
     * @return
     */
    public Map<String, Object> execute(List<PipeProxy> pipes, int ms) {
        int pipesSize = pipes.size();
        Map<Future, String> futureStringMap = new HashMap<Future, String>();
        List<Future> renderResultFuture = new ArrayList<Future>(pipesSize);
        CountDownLatch latch = new CountDownLatch(pipesSize);
        //create pipe worker
        for (PipeProxy pipeProxy : pipes) {
            logger.info("create pipe task >> " + pipeProxy);
            Future future = executor.submit(new ConcurrentPipeWorker(pipeProxy, latch));
            renderResultFuture.add(future);
            futureStringMap.put(future, pipeProxy.getKey());
        }
        Map<String, Object> renderResult = new HashMap<String, Object>(pipesSize);
        try {
            //页面整体设置超时时间
            latch.await(ms, TimeUnit.MILLISECONDS);
            collectRenderResult(renderResultFuture, futureStringMap, renderResult);
        } catch (InterruptedException e) {
            logger.error(e);
        }
        return renderResult;
    }

    private void collectRenderResult(List<Future> renderResultFuture, Map<Future, String> futureStringMap,
                                     Map<String, Object> renderResult) {
        logger.info("start collect render result for " + renderResultFuture.size() + " pipes");
        for (Future future : renderResultFuture) {
            String pipeName = futureStringMap.get(future);
            try {
                //todo 超时加入降级功能
                //过了整个页面超时时间，则抛弃未执行完的模块
                Object result = future.get(0, TimeUnit.MILLISECONDS);
                renderResult.put(pipeName, result);
            } catch (Exception e) {
                logger.error(" pipe render timeout >>> " + pipeName, e);
            }
        }
    }
}
