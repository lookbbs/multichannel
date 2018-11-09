package com.multichannel;

/**
 * @author yuandongfei
 * @date 2018/11/8
 */
public class ApkChannelExcception extends RuntimeException {

    public ApkChannelExcception(String message) {
        super(message);
    }

    public ApkChannelExcception(Throwable cause) {
        super(cause);
    }
}
