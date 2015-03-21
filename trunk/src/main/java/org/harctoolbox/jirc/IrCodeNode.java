package org.harctoolbox.jirc;

//struct ir_code_node {

import java.util.ArrayList;

//	ir_code code;
//	struct ir_code_node *next;
//};
public class IrCodeNode {

    private long code;
    private IrCodeNode next;

    private static long parse(String str) {
        return
                str.startsWith(("0x")) ? Long.parseLong(str.substring(2), 16)
                : str.startsWith("0") ? Long.parseLong(str, 8)
                : Long.parseLong(str);
    }

    public IrCodeNode(long code, IrCodeNode next) {
        this.code = code;
        this.next = next;
    }

    public IrCodeNode(long code) {
        this(code, null);
    }

    public IrCodeNode(String code) {
        this(parse(code));
    }

    /**
     * @return the code
     */
    public long getCode() {
        return code;
    }

    public IrCodeNode(ArrayList<Long> list) {
        code = list.get(0);
        list.remove(0);
        next = list.isEmpty() ? null : new IrCodeNode(list);
    }

    /**
     * @return the next
     */
    public IrCodeNode getNext() {
        return next;
    }

    /**
     * @param code the code to set
     */
    public void setCode(long code) {
        this.code = code;
    }
}
