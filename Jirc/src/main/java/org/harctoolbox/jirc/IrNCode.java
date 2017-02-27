package org.harctoolbox.jirc;

import java.math.BigInteger;
import java.util.List;
import java.util.Locale;

/**
 * This class consists of an IR code with a name. It mirrors ir_ncode from Lirc 0.9.0.
 */
final class IrNCode {

    public static long parseLircNumber(String s) {
        return s.toLowerCase(Locale.US).startsWith("0x") ? parseUnsignedLongHex(s.substring(2))
                : s.startsWith("0") ? Long.parseLong(s, 8)
                : Long.parseLong(s);
    }
    private static long parseUnsignedLongHex(String s) {
        if (s.length() == 16) {
            long value = new BigInteger(s, 16).longValue();
            return value;
        }
        return Long.parseLong(s, 16);
    }

    private String name;
    private long code;
    //public int length;
    private List<Integer> signals;// int[] signals ;
    private IrCodeNode next;
    private IrCodeNode current;
    private IrCodeNode transmit_state;

    IrNCode(String name, long code, List<Integer> signals) {
        this.name = name;
        this.code = code;
        this.signals = signals;
        //length = signals.size();
    }

    IrNCode(String name, long code) {
        this(name, code, null);
    }

    IrNCode(String name, List<Long> codelist) {
        this(name, codelist.get(0), null);
        codelist.remove(0);
        next = codelist.isEmpty() ? null : new IrCodeNode(codelist);
    }


    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the code
     */
    public long getCode() {
        return code;
    }

    /**
     * @return the signals
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<Integer> getSignals() {
        return signals;
    }

    /**
     * @return the next
     */
    public IrCodeNode getNext() {
        return next;
    }

    /**
     * @return the current
     */
    public IrCodeNode getCurrent() {
        return current;
    }

    /**
     * @return the transmit_state
     */
    public IrCodeNode getTransmit_state() {
        return transmit_state;
    }

    /**
     * @param current the current to set
     */
    public void setCurrent(IrCodeNode current) {
        this.current = current;
    }

    /**
     * @param transmit_state the transmit_state to set
     */
    public void setTransmit_state(IrCodeNode transmit_state) {
        this.transmit_state = transmit_state;
    }

    /**
     * @param code the code to set
     */
    public void setCode(long code) {
        this.code = code;
    }
}
