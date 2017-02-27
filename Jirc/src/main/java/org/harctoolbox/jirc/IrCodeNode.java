package org.harctoolbox.jirc;

//struct ir_code_node {

import java.util.List;

//	ir_code code;
//	struct ir_code_node *next;
//};

/**
 * This class mirrors ir_code_node from Lirc 0.9.0. It consists of an long "code", with
 * packing to be put in a linked list, the way programming was made the previous century :-).
 */
final class IrCodeNode {

    private long code;
    private IrCodeNode next;

    IrCodeNode(long code, IrCodeNode next) {
        this.code = code;
        this.next = next;
    }

    IrCodeNode(long code) {
        this(code, null);
    }

    IrCodeNode(String code) {
        this(IrNCode.parseLircNumber(code));
    }

    IrCodeNode(List<Long> list) {
        code = list.get(0);
        list.remove(0);
        next = list.isEmpty() ? null : new IrCodeNode(list);
    }

    /**
     * @return the code
     */
    public long getCode() {
        return code;
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
