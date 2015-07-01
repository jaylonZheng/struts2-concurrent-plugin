package org.le.core.extention;

import org.le.bean.PipeProxy;

/**
 * 模块备份接口
 *在一个模块渲染完之后，会回调实现这个接口的类，备份策略由实现类自行制定
 */
public interface PipeBackup {
    void backup(PipeProxy pipe, Object pipeResult);
}
