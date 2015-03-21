/****************************************************************************
 ** hardware.h **************************************************************
 ****************************************************************************
 *
 * hardware.h - internal hardware interface
 *
 * Copyright (C) 1999 Christoph Bartelmus <lirc@bartelmus.de>
 *
 */
package org.harctoolbox.jirc;

public abstract class Hardware {

    /**
     * @return the hw
     */
    public static Hardware getHw() {
        return hw;
    }

    private String device; //char *device;
    private int fd;
    private int features;
    private int send_mode;
    private int rec_mode;
    private int code_length;
    private String name; //char *name;
    private int resolution;

    private static Hardware hw;

    public abstract int initFunc(); //int (*init_func) (void);

    public abstract int deinit_func(); //int (*deinit_func) (void);

    public abstract int send_func(IrRemote remote, IrNCode code); //int (*send_func) (struct ir_remote * remote, struct ir_ncode * code);

    public abstract String rec_func(IrRemote remotes); //char *(*rec_func) (struct ir_remote * remotes);

    public abstract int decode_func(IrRemote remote, Long prep, Long codep, Long postp,
            Boolean repeat_flag, Integer min_remaining_gapp, Integer max_remaining_gapp);
    //int (*decode_func) (struct ir_remote * remote, ir_code * prep, ir_code * codep, ir_code * postp,
    //int *repeat_flag, lirc_t * min_remaining_gapp, lirc_t * max_remaining_gapp);

    public abstract int ioctl_func(int cmd, Object... arg);//int (*ioctl_func) (unsigned int cmd, void *arg);

    public abstract int readdata(int timeout);//int lirc_t(*readdata) (lirc_t timeout);

    /**
     * @return the device
     */
    public String getDevice() {
        return device;
    }

    /**
     * @return the fd
     */
    public int getFd() {
        return fd;
    }

    /**
     * @return the features
     */
    public int getFeatures() {
        return features;
    }

    /**
     * @return the send_mode
     */
    public int getSend_mode() {
        return send_mode;
    }

    /**
     * @return the rec_mode
     */
    public int getRec_mode() {
        return rec_mode;
    }

    /**
     * @return the code_length
     */
    public int getCode_length() {
        return code_length;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the resolution
     */
    public int getResolution() {
        return resolution;
    }

}