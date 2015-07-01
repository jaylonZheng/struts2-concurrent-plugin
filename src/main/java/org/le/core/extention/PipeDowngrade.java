package org.le.core.extention;

import org.le.bean.PipeProxy;

/**
 * 模块降级接口
 * 当模块渲染抛出异常时，会回调此接口
 */
public interface PipeDowngrade{

    Object downgrade(PipeProxy pipe);
}
