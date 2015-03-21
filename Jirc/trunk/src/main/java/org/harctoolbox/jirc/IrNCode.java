package org.harctoolbox.jirc;

import java.util.ArrayList;

/**
 * Code with name
 */
public class IrNCode {

    private String name;
    private long code;
    //public int length;
    private ArrayList<Integer> signals;// int[] signals ;
    private IrCodeNode next;
    private IrCodeNode current;
    private IrCodeNode transmit_state;

    public IrNCode(String name, long code, ArrayList<Integer> signals) {
        this.name = name;
        this.code = code;
        this.signals = signals;
        //length = signals.size();
    }

    /*public int[] intArray(int trailingSilence, boolean alternatingSigns) {
        int[] array = new int[signals.size() + signals.size() % 2];
        int index = 0;
        for (Integer d : signals) {
            array[index] = (alternatingSigns && index % 2 == 1) ? -d : d;
            index++;
        }

        if (signals.size() % 2 == 1)
            array[array.length - 1] = alternatingSigns ? -trailingSilence : trailingSilence;
        return array;
    }*/

    //public IrNCode(String name, String code) {
    //    this(name, Lirc.smartLongParse(code));
    //}

    //public IrNCode() {
    //    this(null, 0L);
    //}

    public IrNCode(String name, long code) {
        this(name, code, null);
    }

    public IrNCode(String name, ArrayList<Long> codelist) {
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
    public ArrayList<Integer> getSignals() {
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
