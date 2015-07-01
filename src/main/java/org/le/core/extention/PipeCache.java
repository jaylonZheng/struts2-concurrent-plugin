package org.le.core.extention;

import org.le.bean.PipeProxy;

/**
 * 模块缓存接口
 * 在渲染模块之前先通过回调此接口获取模块结果，
 * 如果为null，则继续执行渲染模块代码
 * 如果不为null，则直接使用接口返回作为模块渲染接口
 */
public interface PipeCache{

    Object getCachedPipe(PipeProxy pipe);
}
