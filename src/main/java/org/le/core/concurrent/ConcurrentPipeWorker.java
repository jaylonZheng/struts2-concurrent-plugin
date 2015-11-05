package org.le.core.concurrent;

import org.apache.log4j.Logger;
import org.le.Exception.FtlRenderException;
import org.le.Exception.PipeFtlReadExcption;
import org.le.bean.PipeProxy;
import org.le.core.executor.SyncPipeExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

public class ConcurrentPipeWorker implements Callable<Object> {
    private Logger logger = Logger.getLogger("logger");
    private SyncPipeExecutor syncPipeExecutor = SyncPipeExecutor.newInstance();
    private PipeProxy pipeProxy;
    private CountDownLatch latch;

    public ConcurrentPipeWorker(PipeProxy pipeProxy, CountDownLatch latch) {
        this.latch = latch;
        this.pipeProxy = pipeProxy;
    }

    @Override
    public Object call() throws Exception {
        try {
            return syncPipeExecutor.execute(pipeProxy);
        } catch (PipeFtlReadExcption pipeFtlReadExcption) {
            logger.error(pipeProxy, pipeFtlReadExcption);
            return null;
        } catch (Exception e) {
            logger.error(pipeProxy, e);
            return null;
        } finally {
            latch.countDown();
        }
    }
}
