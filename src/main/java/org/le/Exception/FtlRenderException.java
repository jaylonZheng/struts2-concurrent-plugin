package org.le.Exception;

public class FtlRenderException extends Exception{
    public FtlRenderException(String message){
        super(message);
    }

    public FtlRenderException(Exception e){
        super(e);
    }
}
