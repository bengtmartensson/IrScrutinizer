package org.harctoolbox.jirc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import org.harctoolbox.IrpMaster.DecodeIR;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSequence;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;

/**
 * IrRemote defines the encoding of a remote control
 */
public class IrRemote {

    static class XY {

        public long x;
        public long y;

        XY(long x, long y) {
            this.x = x;
            this.y = y;
        }
    }

    private final int debug = 0;

    private static final int LOG_ERR = 1;
    private static final int LOG_WARNING = 2;

    private void logprintf(int level, String format, Object... args) {
        System.err.println(String.format(format, args));
    }

    //private void LOGPRINTF(int level, String format, Object... args) {
    //    if (debug > 0)
    //        System.err.println(String.format(format, args));
    //}

    // Some cruft, old globals
    //public static IrRemote remotes = null;
    private static IrRemote repeat_remote = null;

    public static IrRemote getRepeat_remote() {
        return repeat_remote;
    }

    public static void setRepeat_remote(IrRemote remote) {
        repeat_remote = remote;
    }

    /**
     * definitions for flags
     */
    final static int IR_PROTOCOL_MASK = 0x07ff;
    // protocols: must not be combined
    // Do not forget to take a look at config_file.h when adding new flags
    /**
     * for internal use only
     */
    final static int RAW_CODES = 0x0001;

    /* IR data follows RC5 protocol */
    final static int RC5 = 0x0002;
    /**
     * IR data is shift encoded (name obsolete) Hm, RC6 protocols seem to have
     * changed the biphase semantics so that lircd will calculate the bit-wise
     * complement of the codes. But this is only a guess as I did not have a
     * datasheet...
     */
    final static int SHIFT_ENC = RC5;
    /**
     * IR data follows RC6 protocol
     */
    final static int RC6 = 0x0004;
    /**
     * IR data follows RC-MM protocol
     */
    final static int RCMM = 0x0008;
    /**
     * IR data is space encoded
     */
    final static int SPACE_ENC = 0x0010;
    /**
     * bits are encoded as space+pulse
     */
    final static int SPACE_FIRST = 0x0020;
    /**
     * encoding found on Goldstar remote
     */
    final static int GOLDSTAR = 0x0040;
    /**
     * encoding found on Grundig remote
     */
    final static int GRUNDIG = 0x0080;
    /**
     * encoding found on Bang & Olufsen remote
     */
    final static int BO = 0x0100;
    /**
     * serial protocol
     */
    //public final static int SERIAL = 0x0200;
    /**
     * XMP protocol
     */
    final static int XMP = 0x0400;
    final static int REVERSE = 0x0800;
    /**
     * additional flags: can be or-red together with protocol flag
     */
    final static int NO_HEAD_REP = 0x1000;
    /**
     * no header for key repeats
     */
    /**
     * no foot for key repeats
     */
    final static int NO_FOOT_REP = 0x2000;
    /**
     * signal length+gap is always constant
     */
    final static int CONST_LENGTH = 0x4000;
    /**
     * header is also sent before repeat code
     */
    final static int REPEAT_HEADER = 0x8000;
    /**
     * compatibility mode for REVERSE flag
     */
    final static int COMPAT_REVERSE = 0x00010000;
    /**
     * stop repeating after 600 signals (approx. 1 minute). update
     * technical.html when changing this value
     */
    final static int REPEAT_MAX_DEFAULT = 600;
    final static int DEFAULT_FREQ = 38000;
    //public final static int IR_PARITY_NONE = 0;
    //public final static int IR_PARITY_EVEN = 1;
    //public final static int IR_PARITY_ODD = 2;

    private static final int PULSE_BIT = 0x01000000;

    private static final HashMap<String, Integer> all_flags = new HashMap<String, Integer>() {
        {
            put("RAW_CODES", IrRemote.RAW_CODES);
            put("RC5", IrRemote.RC5);
            put("SHIFT_ENC", IrRemote.SHIFT_ENC);	// obsolete
            put("RC6", IrRemote.RC6);
            put("RCMM", IrRemote.RCMM);
            put("SPACE_ENC", IrRemote.SPACE_ENC);
            put("SPACE_FIRST", IrRemote.SPACE_FIRST);
            put("GOLDSTAR", IrRemote.GOLDSTAR);
            put("GRUNDIG", IrRemote.GRUNDIG);
            put("BO", IrRemote.BO);
            //put("SERIAL", IrRemote.SERIAL);
            put("XMP", IrRemote.XMP);

            put("REVERSE", IrRemote.REVERSE);
            put("NO_HEAD_REP", IrRemote.NO_HEAD_REP);
            put("NO_FOOT_REP", IrRemote.NO_FOOT_REP);
            put("CONST_LENGTH", IrRemote.CONST_LENGTH);	// remember to adapt warning message when changing this
            put("REPEAT_HEADER", IrRemote.REPEAT_HEADER);
        }
    };

    public HashMap<String, String> getApplicationData() {
        HashMap<String, String> applicationData = new LinkedHashMap<String, String>();

        applicationData.put("type", lircProtocolType());
        applicationData.put("bits", Integer.toString(bits)); // bits (length of code)
        applicationData.put("flags", Integer.toString(flags)); // flags
        applicationData.put("eps", Integer.toString(eps)); // eps (_relative_ tolerance)
        applicationData.put("aeps", Integer.toString(aeps)); // aeps (_absolute_ tolerance)
        applicationData.put("pthree", Integer.toString(pthree)); // 3 (only used for RC-MM)
        applicationData.put("sthree", Integer.toString(sthree)); // 3 (only used for RC-MM)
        applicationData.put("ptwo", Integer.toString(ptwo)); // 2 (only used for RC-MM)
        applicationData.put("stwo", Integer.toString(stwo)); // 2 (only used for RC-MM)
        applicationData.put("pone", Integer.toString(pone)); // 1
        applicationData.put("sone", Integer.toString(sone)); // 1
        applicationData.put("pzero", Integer.toString(pzero)); // 0
        applicationData.put("szero", Integer.toString(szero)); // 0
        applicationData.put("plead", Integer.toString(plead)); // leading pulse
        applicationData.put("ptrail", Integer.toString(ptrail));	// trailing pulse
        applicationData.put("pfoot", Integer.toString(pfoot)); // foot
        applicationData.put("sfoot", Integer.toString(sfoot)); // foot
        applicationData.put("prepeat", Integer.toString(prepeat)); // indicate repeating
        applicationData.put("srepeat", Integer.toString(srepeat)); // indicate repeating
        applicationData.put("pre_data_bits", Integer.toString(pre_data_bits)); // length of pre_data
        applicationData.put("pre_data", Long.toString(pre_data)); // data which the remote sends before actual keycode
        applicationData.put("post_data_bits", Integer.toString(post_data_bits)); // length of post_data
        applicationData.put("post_data", Long.toString(post_data)); // data which the remote sends after actual keycode
        applicationData.put("pre_p", Integer.toString(pre_p)); // signal between pre_data and keycode
        applicationData.put("pre_s", Integer.toString(pre_s)); // signal between pre_data and keycode
        applicationData.put("post_p", Integer.toString(post_p)); // signal between keycode and post_code
        applicationData.put("post_s", Integer.toString(post_s)); // signal between keycode and post_code
        applicationData.put("gap", Integer.toString(gap)); // time between signals in usecs
        applicationData.put("gap2", Integer.toString(gap2)); // time between signals in usecs
        applicationData.put("repeat_gap", Integer.toString(repeat_gap));	// time between two repeat codes if different from gap
        applicationData.put("toggle_bit", Integer.toString(toggle_bit));	// obsolete
        applicationData.put("toggle_bit_mask", Long.toString(toggle_bit_mask)); // previously only one bit called toggle_bit
        applicationData.put("min_repeat", Integer.toString(min_repeat));	// code is repeated at least x times code sent once -> min_repeat=0
        applicationData.put("min_code_repeat", Integer.toString(min_code_repeat)); //meaningful only if remote sends a repeat code: in this case this value indicates how often the real code is repeated before the repeat code is being sent
        applicationData.put("freq", Integer.toString(freq != -1 ? freq : IrRemote.DEFAULT_FREQ)); // modulation frequency
        applicationData.put("duty_cycle", Integer.toString(duty_cycle));	// 0<duty cycle<=100
        applicationData.put("toggle_mask", Long.toString(toggle_mask)); // Sharp (?) error detection scheme
        applicationData.put("rc6_mask", Long.toString(rc6_mask)); // RC-6 doubles signal length of some bits
        applicationData.put("ignore_mask", Long.toString(ignore_mask)); // mask defines which bits can be ignored when matching a code

        return applicationData;
    }

    public Remote toRemote(boolean invokeDecodeIr, boolean alternatingSigns, int debug) {
        HashMap<String, HashMap<String, String>> appDataMap = new LinkedHashMap<String, HashMap<String, String>>();
        appDataMap.put("jirc", getApplicationData());

        //remote.last_code = null;

        LinkedHashMap<String, Command> commands = new LinkedHashMap<String, Command>();
        for (IrNCode c : getCodes()) {
            Command irCommand = toCommand(c, invokeDecodeIr, alternatingSigns, debug);
            if (irCommand != null)
                commands.put(irCommand.getName(), irCommand);
        }

        //if (commands.isEmpty()) {
        //    System.err.println("No useful commands found in " + getName() + ", rejected.");
        //    return null;
        //}

        Remote rem = new Remote(getName(),
                null, // manufacturer,
                null, // model,
                null, // deviceClass,
                null, // remoteName,
                null, // comment,
                null, // notes,
                commands,
                appDataMap);

        return rem;
    }

    // TODO: only generate CCF and decodes if necessary
    public Command toCommand(IrNCode code, boolean invokeDecodeIr, boolean alternatingSigns, int debug) {
        IrSignal irSignal = toIrSignal(code, alternatingSigns, debug);
        return irSignal != null
                ? new Command(code.getName(), /*comment=*/null, irSignal, /*genereteCcf=*/true, /*decode=*/true)
                : null;
    }

    public IrSignal toIrSignal(IrNCode code, boolean alternatingSigns, int debug) {
        IrSequence intro = new IrSequence();
        do {
            IrSequence seq = render(code, alternatingSigns, /* repeat= */ false, debug);
            if (seq != null && !seq.isEmpty())
                intro = intro.append(seq);
        } while (code.getTransmit_state() != null);
        if ((code.getSignals() == null || code.getSignals().isEmpty())
                && (intro == null || intro.isEmpty()))
            return null;

        IrSequence repeat;
        if (code.getNext() == null) {
            repeat = render(code, alternatingSigns, /* repeat= */ true, debug);
        } else {
            // It is quite unclear to me what the multiple codes in IrNCode are to be handled,
            // this is an educated guess.
            repeat = intro;
            intro = null;
        }

        return new IrSignal((double) freq,  /* duty cycle= */IrpUtils.invalid, intro, repeat,  /* ending= */ null);
    }

    public static RemoteSet newRemoteSet(HashMap<String, IrRemote> remotes, String configFilename,
            boolean invokeDecodeIr, String creatingUser, boolean alternatingSigns, int debug) {
        if (remotes == null || remotes.isEmpty())
            return null;
        String decodeir_version = invokeDecodeIr ? DecodeIR.getVersion() : "none";

        LinkedHashMap<String, Remote>girrRemotes = new LinkedHashMap<String, Remote>();
        for (IrRemote irRemote : remotes.values()) {
            Remote remote = irRemote.toRemote(invokeDecodeIr, alternatingSigns, debug);
            if (remote != null)
                girrRemotes.put(remote.getName(), remote);
        }

        RemoteSet girr = new RemoteSet(creatingUser == null ? System.getProperty("user.name") : creatingUser, configFilename,
                (new Date()).toString(), Version.appName, Version.version,
                "DecodeIr", decodeir_version != null ? decodeir_version : "not found",
                null,
                girrRemotes);
        return girr;
    }

    public static RemoteSet newRemoteSet(File configFile, boolean invokeDecodeIr, String creatingUser, int debug) throws IOException {
        return newRemoteSet(ConfigFile.readConfig(configFile, 0), configFile.getPath(),
           invokeDecodeIr, creatingUser, true, debug);
    }

    private boolean setNamedFlag(String flagName) {
        if (!all_flags.containsKey(flagName))
            return false;

        flags |= all_flags.get(flagName);
        return false;
    }

    /**
     * name of remote control
     */
    private String name;

    public String getName() {
        return name;
    }

    private String source;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    private ArrayList<IrNCode> codes;

    public ArrayList<IrNCode> getCodes() {
        return codes;
    }

    /**
     * bits (length of code)
     */
    int bits;
    /**
     * flags
     */
    int flags;
    /**
     * eps (_relative_ tolerance)
     */
    int eps;
    /**
     * detecing _very short_ pulses is difficult with relative tolerance for
     * some remotes, this is an _absolute_ tolerance to solve this problem
     * usually you can say 0 here
     */
    int aeps;
    /**
     * pulse and space lengths of: header
     */
    int phead, shead;
    /**
     * 3 (only used for RC-MM)
     */
    int pthree, sthree;
    /**
     * 2 (only used for RC-MM)
     */
    int ptwo, stwo;
    /**
     * 1
     */
    int pone, sone;
    /**
     * 0
     */
    int pzero, szero;
    /**
     * leading pulse
     */
    int plead;
    /**
     * trailing pulse
     */
    int ptrail;
    /**
     * foot
     */
    int pfoot, sfoot;
    /**
     * indicate repeating
     */
    int prepeat, srepeat;
    /**
     * length of pre_data
     */
    int pre_data_bits;
    /**
     * data which the remote sends before actual keycode
     */
    long pre_data;
    /**
     * length of post_data
     */
    int post_data_bits;
    /**
     * data which the remote sends after actual keycode
     */
    long post_data;
    /**
     * signal between pre_data and keycode
     */
    int pre_p, pre_s;
    /**
     * signal between keycode and post_code
     */
    int post_p, post_s;
    /**
     * time between signals in usecs
     */
    int gap;
    /**
     * time between signals in usecs
     */
    int gap2;
    /**
     * time between two repeat codes if different from gap
     */
    int repeat_gap;
    /**
     * obsolete
     */
    int toggle_bit;
    /**
     * previously only one bit called toggle_bit
     */
    long toggle_bit_mask;
    /**
     * suppress unwanted repeats
     */
    int suppress_repeat;
    /**
     * code is repeated at least x times code sent once . min_repeat=0
     */
    int min_repeat;
    /**
     * meaningful only if remote sends a repeat code: in this case this value
     * indicates how often the real code is repeated before the repeat code is
     * being sent
     */
    int min_code_repeat;
    /**
     * modulation frequency
     */
    int freq = DEFAULT_FREQ;

    public int getFreq() {
        return freq;
    }

    /**
     * 0<duty cycle<=100
     */
    int duty_cycle;

    public int getDutyCycle() {
        return duty_cycle;
    }

    /**
     * Sharp (?) error detection scheme
     */
    long toggle_mask;
    /**
     * RC-6 doubles signal length of some bits
     */
    long rc6_mask;
    /**
     * mask defines which bits can be ignored when matching a code
     */
    long ignore_mask;
    // end of user editable values
    long toggle_bit_mask_state;
    int toggle_mask_state;
    int repeat_countdown;
    /**
     * code received or sent last
     */
    IrNCode last_code;
    /**
     * toggle code received or sent last
     */
    IrNCode toggle_code;
    int reps;
    /**
     * time last_code was received or sent
     */
    Date last_send;
    /**
     * remember gap for CONST_LENGTH remotes
     */
    int min_remaining_gap;
    /**
     * gap range
     */
    int max_remaining_gap;
    /**
     * how long is the shortest signal including gap
     */
    int min_total_signal_length;
    /**
     * how long is the longest signal including gap
     */
    private int max_total_signal_length;
    /**
     * how long is the shortest gap
     */
    int min_gap_length;
    /**
     * how long is the longest gap
     */
    int max_gap_length;
    int min_pulse_length, max_pulse_length;
    int min_space_length, max_space_length;
    /**
     * set by release generator
     */
    boolean release_detected;
    IrRemote next;

    private IrRemote() {
    }

    public IrRemote(String name, ArrayList<String> flags, HashMap<String, Long> unaryParameters,
            HashMap<String, XY> binaryParameters, ArrayList<IrNCode> codes) {
        this.name = name;
        for (String flag  : flags)
            setNamedFlag(flag);
        for (Entry<String, Long> kvp : unaryParameters.entrySet())
            setNamedParameter(kvp.getKey(), kvp.getValue());

        for (Entry<String, XY> kvp : binaryParameters.entrySet())
            setNamedParameter(kvp.getKey(), kvp.getValue().x, kvp.getValue().y);

        this.codes = codes;

        sanityChecks();
    }

    /*      $Id: ir_remote.h,v 5.44 2010/04/11 18:50:38 lirc Exp $      */
//#include "drivers/lirc.h"
//#include "hardware.h"
//extern struct hardware Hardware.hw;
    //static Hardware hw;

    static long get_ir_code(IrNCode ncode, IrCodeNode node) {
        if (ncode.getNext() != null && node != null)
            return node.getCode();
        return ncode.getCode();
    }

    static IrCodeNode get_next_ir_code_node(IrNCode ncode, IrCodeNode node) {
        if (node == null)
            return ncode.getNext();
        return node.getNext();
    }

    public int bit_count() {
        return pre_data_bits + bits + post_data_bits;
    }

    static int bit_count(IrRemote remote) {
        return remote.pre_data_bits + remote.bits + remote.post_data_bits;
    }

    static int bits_set(long data) {
        int ret = 0;
        while (data != 0) {
            if ((data & 1) != 0)
                ret++;
            data >>= 1;
        }
        return ret;
    }

    static long reverse(long data, int bits) {
        int i;
        long c;

        c = 0;
        for (i = 0; i < bits; i++) {
            c |= (long) (((data & (((long) 1) << i)) != 0 ? 1 : 0))
                    << (bits - 1 - i);
        }
        return (c);
    }

    static boolean is_pulse(int data) {
        return (data & PULSE_BIT) != 0;
    }

    static boolean is_space(int data) {
        return (!is_pulse(data));
    }

    public boolean has_repeat() {
        return prepeat > 0 && srepeat > 0;
    }

    static boolean has_repeat(IrRemote remote) {
        return remote.prepeat > 0 && remote.srepeat > 0;
    }

    static void set_protocol(IrRemote remote, int protocol) {
        remote.flags &= ~(IR_PROTOCOL_MASK);
        remote.flags |= protocol;
    }

    public void set_protocol(int protocol) {
        flags &= ~(IR_PROTOCOL_MASK);
        flags |= protocol;
    }

    public boolean is_raw() {
        return (flags & IR_PROTOCOL_MASK) == RAW_CODES;
    }

    public static boolean is_raw(IrRemote remote) {
        return remote.is_raw();
    }

    public boolean is_space_enc() {
        return (flags & IR_PROTOCOL_MASK) == SPACE_ENC;
    }

    static boolean is_space_enc(IrRemote remote) {
        return remote.is_space_enc();
    }

    public boolean is_space_first() {
        return (flags & IR_PROTOCOL_MASK) == SPACE_FIRST;
    }

    public static boolean is_space_first(IrRemote remote) {
        return remote.is_space_first();
    }

    public boolean is_rc5() {
        return (flags & IR_PROTOCOL_MASK) == RC5;
    }

    public static boolean is_rc5(IrRemote remote) {
        return remote.is_rc5();
    }

    public boolean is_rc6() {
        return (flags & IR_PROTOCOL_MASK) == RC6 || rc6_mask != 0;
    }

    public static boolean is_rc6(IrRemote remote) {
        return remote.is_rc6();
    }

    boolean is_biphase() {
        return is_rc5() || is_rc6();
    }

    static boolean is_biphase(IrRemote remote) {
        return remote.is_biphase();
    }

    public boolean is_rcmm() {
        return (flags & IR_PROTOCOL_MASK) == RCMM;
    }

    public static boolean is_rcmm(IrRemote remote) {
        return remote.is_rcmm();
    }

    public boolean is_goldstar() {
        return (flags & IR_PROTOCOL_MASK) == GOLDSTAR;
    }

    public static boolean is_goldstar(IrRemote remote) {
        return remote.is_goldstar();
    }

    public boolean is_grundig() {
        return (flags & IR_PROTOCOL_MASK) == GRUNDIG;
    }

    public static boolean is_grundig(IrRemote remote) {
        return remote.is_grundig();
    }

    public boolean is_bo() {
        return (flags & IR_PROTOCOL_MASK) == BO;
    }

    public static boolean is_bo(IrRemote remote) {
        return remote.is_bo();
    }

    public boolean is_xmp() {
        return (flags & IR_PROTOCOL_MASK) == XMP;
    }

    public static boolean is_xmp(IrRemote remote) {
        return remote.is_xmp();
    }

    public boolean is_const() {
        return (flags & CONST_LENGTH) != 0;
    }

    static boolean is_const(IrRemote remote) {
        return (remote.flags & CONST_LENGTH) != 0;
    }

    public boolean has_repeat_gap() {
        return repeat_gap > 0;
    }

    static boolean has_repeat_gap(IrRemote remote) {
        return remote.repeat_gap > 0;
    }

    public boolean has_pre() {
        return pre_data_bits > 0;
    }

    static boolean has_pre(IrRemote remote) {
        return remote.pre_data_bits > 0;
    }

    public boolean has_post() {
        return post_data_bits > 0;
    }

    static boolean has_post(IrRemote remote) {
        return remote.post_data_bits > 0;
    }

    public boolean has_header() {
        return phead > 0 && shead > 0;
    }

    public static boolean has_header(IrRemote remote) {
        return remote.has_header();
    }

    boolean has_foot() {
        return pfoot > 0 && sfoot > 0;
    }

    static boolean has_foot(IrRemote remote) {
        return remote.has_foot();
    }

    public boolean has_toggle_bit_mask() {
        return toggle_bit_mask > 0;
    }

    static boolean has_toggle_bit_mask(IrRemote remote) {
        return remote.toggle_bit_mask > 0;
    }

    static boolean has_ignore_mask(IrRemote remote) {
        return remote.ignore_mask > 0;
    }

    boolean has_toggle_mask() {
        return toggle_mask > 0;
    }

    static boolean has_toggle_mask(IrRemote remote) {
        return remote.has_toggle_mask();
    }

    public int min_gap() {
        return (gap2 != 0 && gap2 < gap) ? gap2 : gap;
    }

    static int min_gap(IrRemote remote) {
        return (remote.gap2 != 0 && remote.gap2 < remote.gap) ? remote.gap2 : remote.gap;
    }

    public int max_gap() {
        return (gap2 > gap) ? gap2 : gap;
    }

    static int max_gap(IrRemote remote) {
        return (remote.gap2 > remote.gap) ? remote.gap2 : remote.gap;
    }

    /** check if delta is inside exdelta +/- exdelta*eps/100 */
    static int expect(IrRemote remote, int delta, int exdelta) {
        int aeps = Hardware.getHw().getResolution() > remote.aeps ? Hardware.getHw().getResolution() : remote.aeps;

        if (Math.abs(exdelta - delta) <= exdelta * remote.eps / 100 || Math.abs(exdelta - delta) <= aeps)
            return 1;
        return 0;
    }

    static int expect_at_least(IrRemote remote, int delta, int exdelta) {
        int aeps = Hardware.getHw().getResolution() > remote.aeps ? Hardware.getHw().getResolution() : remote.aeps;

        if (delta + exdelta * remote.eps / 100 >= exdelta || delta + aeps >= exdelta) {
            return 1;
        }
        return 0;
    }

    static boolean expect_at_most(IrRemote remote, int delta, int exdelta) {
        int aeps = Hardware.getHw().getResolution() > remote.aeps ? Hardware.getHw().getResolution() : remote.aeps;

        //if (delta <= exdelta + exdelta * remote.eps / 100 || delta <= exdelta + aeps) {
        //    return 1;
        //}
        //return 0;
        return delta <= exdelta + exdelta * remote.eps / 100 || delta <= exdelta + aeps;
    }

    static int upper_limit(IrRemote remote, int val) {
        int aeps = Hardware.getHw().getResolution() > remote.aeps ? Hardware.getHw().getResolution() : remote.aeps;
        int eps_val = val * (100 + remote.eps) / 100;
        int aeps_val = val + aeps;
        return eps_val > aeps_val ? eps_val : aeps_val;
    }

    static int lower_limit(IrRemote remote, int val) {
        int aeps = Hardware.getHw().getResolution() > remote.aeps ? Hardware.getHw().getResolution() : remote.aeps;
        int eps_val = val * (100 - remote.eps) / 100;
        int aeps_val = val - aeps;

        if (eps_val <= 0)
            eps_val = 1;
        if (aeps_val <= 0)
            aeps_val = 1;

        return eps_val < aeps_val ? eps_val : aeps_val;
    }

    /* only works if last <= current */
    static int time_elapsed(Date last, Date current) {
        //long secs, diff;

        //secs = current.tv_sec - last.tv_sec;

        //diff = 1000000 * secs + current.tv_usec - last.tv_usec;

        //return (diff);
        return (int) (current.getTime() - last.getTime());
    }

    static long gen_mask(int bits) {
        int i;
        long mask;

        mask = 0;
        for (i = 0; i < bits; i++) {
            mask <<= 1;
            mask |= 1;
        }
        return (mask);
    }

    static long gen_ir_code(IrRemote remote, long pre, long code, long post) {
        long all;

        all = (pre & gen_mask(remote.pre_data_bits));
        all <<= remote.bits;
        all |= is_raw(remote) ? code : (code & gen_mask(remote.bits));
        all <<= remote.post_data_bits;
        all |= post & gen_mask(remote.post_data_bits);

        return all;
    }
    /*
     void get_frequency_range(IrRemote remotes, unsigned int *min_freq, unsigned int *max_freq);
     void get_filter_parameters(IrRemote remotes, int * max_gap_lengthp, int * min_pulse_lengthp,
     int * min_space_lengthp, int * max_pulse_lengthp, int * max_space_lengthp);
     IrRemote is_in_remotes(IrRemote remotes, IrRemote remote);
     IrRemote get_ir_remote(IrRemote remotes, char *name);
     int map_code(IrRemote remote, long * prep, long * codep, long * postp, int pre_bits, long pre,
     int bits, long code, int post_bits, long post);
     void map_gap(IrRemote remote, timeval *start, timeval *last, int signal_length,
     int *repeat_flagp, int * min_remaining_gapp, int * max_remaining_gapp);
     IrNCode get_code_by_name(IrRemote remote, char *name);
     IrNCode get_code(IrRemote remote, long pre, long code, long post,
     long * toggle_bit_mask_state);
     __u64 set_code(IrRemote remote, IrNCode found, long toggle_bit_mask_state, int repeat_flag,
     int min_remaining_gap, int max_remaining_gap);
     int write_message(char *buffer, size_t size, const char *remote_name, const char *button_name,
     const char *button_suffix, long code, int reps);
     char *decode_all(IrRemote remotes);
     int send_ir_ncode(IrRemote remote, IrNCode code);
     */


    /*      $Id: ir_remote.c,v 5.49 2010/05/13 16:24:29 lirc Exp $      */

/****************************************************************************
 ** ir_remote.c *************************************************************
 ****************************************************************************
 *
 * ir_remote.c - sends and decodes the signals from IR remotes
 *
 * Copyright (C) 1996,97 Ralph Metzler (rjkm@thp.uni-koeln.de)
 * Copyright (C) 1998 Christoph Bartelmus (lirc@bartelmus.de)
 *
 */

    static IrRemote decoding = null;

//struct ir_remote *last_remote = null;
    static IrRemote last_remote = null;
//struct ir_remote *repeat_remote = null;
//struct ir_ncode *repeat_code;

//extern struct hardware hw;

    /**
     *
     * @param current
     * @param last
     * @param gap
     * @return
     */
    public static int time_left(Date current, Date last, int gap) {
        //unsigned long secs, diff;

        //secs = current.tv_sec - last.tv_sec;

        //diff = 1000000 * secs + current.tv_usec - last.tv_usec;
        long diff = current.getTime() - last.getTime();

        //return ((lirc_t) (diff < gap ? gap - diff : 0));
        return (int) Math.max(diff, 0);
    }

    public static boolean match_ir_code(IrRemote remote, long a, long b) {
        return (remote.ignore_mask | a) == (remote.ignore_mask | b)
                || (remote.ignore_mask | a) == (remote.ignore_mask | (b ^ remote.toggle_bit_mask));
    }

    public static void get_frequency_range(IrRemote remotes, Integer min_freq, Integer max_freq) {
        IrRemote scan;

        /* use remotes carefully, it may be changed on SIGHUP */
        scan = remotes;
        if (scan == null) {
            min_freq = 0;
            max_freq = 0;
        } else {
            min_freq = scan.freq;
            max_freq = scan.freq;
            scan = scan.next;
        }
        while (scan != null) {
            if (scan.freq != 0) {
                if (scan.freq >  max_freq) {
                     max_freq = scan.freq;
                } else if (scan.freq <  min_freq) {
                     min_freq = scan.freq;
                }
            }
            scan = scan.next;
        }
    }

    public static void get_filter_parameters(IrRemote remotes, Integer max_gap_lengthp, Integer min_pulse_lengthp,
            Integer min_space_lengthp, Integer max_pulse_lengthp, Integer max_space_lengthp) {
        IrRemote scan = remotes;
        int max_gap_length = 0;
        int min_pulse_length = 0, min_space_length = 0;
        int max_pulse_length = 0, max_space_length = 0;

        while (scan != null) {
            int val;
            val = upper_limit(scan, scan.max_gap_length);
            if (val > max_gap_length) {
                max_gap_length = val;
            }
            val = lower_limit(scan, scan.min_pulse_length);
            if (min_pulse_length == 0 || val < min_pulse_length) {
                min_pulse_length = val;
            }
            val = lower_limit(scan, scan.min_space_length);
            if (min_space_length == 0 || val > min_space_length) {
                min_space_length = val;
            }
            val = upper_limit(scan, scan.max_pulse_length);
            if (val > max_pulse_length) {
                max_pulse_length = val;
            }
            val = upper_limit(scan, scan.max_space_length);
            if (val > max_space_length) {
                max_space_length = val;
            }
            scan = scan.next;
        }
        max_gap_lengthp = max_gap_length;
        min_pulse_lengthp = min_pulse_length;
        min_space_lengthp = min_space_length;
        max_pulse_lengthp = max_pulse_length;
        max_space_lengthp = max_space_length;
    }

    public static IrRemote is_in_remotes(IrRemote remotes, IrRemote remote) {
        while (remotes != null) {
            if (remotes == remote) {
                return remote;
            }
            remotes = remotes.next;
        }
        return null;
    }

    public static IrRemote get_ir_remote(IrRemote remotes, String name) {
        IrRemote all;

        /* use remotes carefully, it may be changed on SIGHUP */
        all = remotes;
        while (all != null) {
            if (all.name.equals(name)) {
                return all;
            }
            all = all.next;
        }
        return null;
    }

    public static int map_code(IrRemote remote, Long prep, Long codep, Long postp, int pre_bits, long pre,
            int bits, long code, int post_bits, long post) {
        long all;

        if (pre_bits + bits + post_bits != remote.pre_data_bits + remote.bits + remote.post_data_bits) {
            return (0);
        }
        all = (pre & gen_mask(pre_bits));
        all <<= bits;
        all |= (code & gen_mask(bits));
        all <<= post_bits;
        all |= (post & gen_mask(post_bits));

        postp = (all & gen_mask(remote.post_data_bits));
        all >>= remote.post_data_bits;
        codep = (all & gen_mask(remote.bits));
        all >>= remote.bits;
        prep = (all & gen_mask(remote.pre_data_bits));

        //LOGPRINTF(1, "pre: %llx", (__u64) * prep);
        //LOGPRINTF(1, "code: %llx", (__u64) * codep);
        //LOGPRINTF(1, "post: %llx", (__u64) * postp);
        //LOGPRINTF(1, "code:                   %016llx\n", code);

        return (1);
    }

    void map_gap(IrRemote remote, Date start, Date last, int signal_length,
            Boolean repeat_flagp, Integer min_remaining_gapp, Integer max_remaining_gapp) {
        // Time gap (us) between a keypress on the remote control and
        // the next one.
        int gap;

        // Check the time gap between the last keypress and this one.
        //if (start.tv_sec - last.tv_sec >= 2) {
        if ((start.getTime() - last.getTime()) / 1000 >= 2) {
            // Gap of 2 or more seconds: this is not a repeated keypress.
            repeat_flagp = false;
            gap = 0;
        } else {
            // Calculate the time gap in microseconds.
            gap = time_elapsed(last, start);
            //if (expect_at_most(remote, gap, remote.max_remaining_gap)) {
            // The gap is shorter than a standard gap
            // (with relative or aboslute tolerance): this
            // is a repeated keypress.
            //	*repeat_flagp = 1;
            //} else {
            // Standard gap: this is a new keypress.
            //	*repeat_flagp = 0;
            //}
            repeat_flagp = expect_at_most(remote, gap, remote.max_remaining_gap);
        }

        // Calculate extimated time gap remaining for the next code.
        if (is_const(remote)) {
            // The sum (signal_length + gap) is always constant
            // so the gap is shorter when the code is longer.
            if (min_gap(remote) > signal_length) {
                min_remaining_gapp = min_gap(remote) - signal_length;
                max_remaining_gapp = max_gap(remote) - signal_length;
            } else {
                min_remaining_gapp = 0;
                if (max_gap(remote) > signal_length) {
                    max_remaining_gapp = max_gap(remote) - signal_length;
                } else {
                    max_remaining_gapp = 0;
                }
            }
        } else {
            // The gap after the signal is always constant.
            // This is the case of Kanam Accent serial remote.
            min_remaining_gapp = min_gap(remote);
            max_remaining_gapp = max_gap(remote);
        }

        //LOGPRINTF(1, "repeat_flagp:           %d", *repeat_flagp);
        //LOGPRINTF(1, "is_const(remote):       %d", is_const(remote));
        //LOGPRINTF(1, "remote.gap range:      %lu %lu", (__u32) min_gap(remote), (__u32) max_gap(remote));
        //LOGPRINTF(1, "remote.remaining_gap:  %lu %lu", (__u32) remote.min_remaining_gap,
        //	  (__u32) remote.max_remaining_gap);
        //LOGPRINTF(1, "signal length:          %lu", (__u32) signal_length);
        //LOGPRINTF(1, "gap:                    %lu", (__u32) gap);
        //LOGPRINTF(1, "extim. remaining_gap:   %lu %lu", (__u32) * min_remaining_gapp, (__u32) * max_remaining_gapp);

    }

    public static IrNCode get_code_by_name(IrRemote remote, String name) {
        //IrNCode all;

        //all = remote.codes;
        //while (all.name != null) {
        //    if (all.name.equals(name)) {
        //        return all;
        //    }
        //    all++;
        //}
        //return (0);
        for (IrNCode all : remote.codes) {
            if (all.getName().equals(name))
                return all;
        }
        return null;
    }

    public static IrNCode get_code(IrRemote remote, long pre, long code, long post,
            Long toggle_bit_mask_statep) {
        long pre_mask, code_mask, post_mask, toggle_bit_mask_state, all;
        boolean found_code, have_code;
        //IrNCode codes;
        IrNCode found;

        pre_mask = code_mask = post_mask = 0;

        if (has_toggle_bit_mask(remote)) {
            pre_mask = remote.toggle_bit_mask >> (remote.bits + remote.post_data_bits);
            post_mask = remote.toggle_bit_mask & gen_mask(remote.post_data_bits);
        }
        if (has_ignore_mask(remote)) {
            pre_mask |= remote.ignore_mask >> (remote.bits + remote.post_data_bits);
            post_mask |= remote.ignore_mask & gen_mask(remote.post_data_bits);
        }
        if (has_toggle_mask(remote) && remote.toggle_mask_state % 2 == 0) {
            Long affected, mask, mask_bit;
            int bit, current_bit;

            affected = /*&*/ post;
            mask = remote.toggle_mask;
            for (bit = current_bit = 0; bit < bit_count(remote); bit++, current_bit++) {
                if (bit == remote.post_data_bits) {
                    affected = /*&*/ code;
                    current_bit = 0;
                }
                if (bit == remote.post_data_bits + remote.bits) {
                    affected = /*&*/ pre;
                    current_bit = 0;
                }
                mask_bit = mask & 1;
                (affected) ^= (mask_bit << current_bit);
                mask >>= 1;
            }
        }
        if (has_pre(remote)) {
            if ((pre | pre_mask) != (remote.pre_data | pre_mask)) {
                //LOGPRINTF(1, "bad pre data");
                //LOGPRINTF(2, "%llx %llx", pre, remote.pre_data);
                return null;//(0);
            }
            //LOGPRINTF(1, "pre");
        }

        if (has_post(remote)) {
            if ((post | post_mask) != (remote.post_data | post_mask)) {
                //LOGPRINTF(1, "bad post data");
                //LOGPRINTF(2, "%llx %llx", post, remote.post_data);
                return null;
            }
            //LOGPRINTF(1, "post");
        }

        all = gen_ir_code(remote, pre, code, post);

        toggle_bit_mask_state = all & remote.toggle_bit_mask;

        found = null;
        found_code = false;
        have_code = false;
        //codes = remote.codes;
        //if (codes != null) {
        for (IrNCode codes : remote.codes) {
            while (codes.getName() != null) {
                long next_all;

                next_all = gen_ir_code(remote, remote.pre_data, get_ir_code(codes, codes.getCurrent()),
                        remote.post_data);
                if (match_ir_code(remote, next_all, all)) {
                    found_code = true;
                    if (codes.getNext() != null) {
                        if (codes.getCurrent() == null) {
                            codes.setCurrent(codes.getNext());
                        } else {
                            codes.setCurrent(codes.getCurrent().getNext());
                        }
                    }
                    if (!have_code) {
                        found = codes;
                        if (codes.getCurrent() == null) {
                            have_code = true;
                        }
                    }
                } else {
                    /* find longest matching sequence */
                    IrCodeNode search;

                    search = codes.getNext();
                    if (search == null || (codes.getNext() != null && codes.getCurrent() == null)) {
                        codes.setCurrent(null);
                    } else {
                        boolean sequence_match = false;

                        while (search != codes.getCurrent().getNext()) {
                            IrCodeNode prev, next;
                            int flag = 1;

                            prev = null;	/* means codes.code */
                            next = search;
                            while (next != codes.getCurrent()) {
                                if (get_ir_code(codes, prev) != get_ir_code(codes, next)) {
                                    flag = 0;
                                    break;
                                }
                                prev = get_next_ir_code_node(codes, prev);
                                next = get_next_ir_code_node(codes, next);
                            }
                            if (flag == 1) {
                                next_all =
                                        gen_ir_code(remote, remote.pre_data,
                                        get_ir_code(codes, prev), remote.post_data);
                                if (match_ir_code(remote, next_all, all)) {
                                    codes.setCurrent(get_next_ir_code_node(codes, prev));
                                    sequence_match = true;
                                    found_code = true;
                                    if (!have_code) {
                                        found = codes;
                                    }
                                    break;
                                }
                            }
                            search = search.getNext();
                        }
                        if (!sequence_match)
                            codes.setCurrent(null);
                    }
                }
                //codes++;
            }
        }
        /*#       ifdef DYNCODES
         if (!found_code) {
         if (remote.dyncodes[remote.dyncode].code != code) {
         remote.dyncode++;
         remote.dyncode %= 2;
         }
         remote.dyncodes[remote.dyncode].code = code;
         found = &(remote.dyncodes[remote.dyncode]);
         found_code = 1;
         }
         #       endif*/
        if (found_code && found != null && has_toggle_mask(remote)) {
            if (!(remote.toggle_mask_state % 2 == 0)) {
                remote.toggle_code = found;
                //LOGPRINTF(1, "toggle_mask_start");
            } else {
                if (found != remote.toggle_code) {
                    remote.toggle_code = null;
                    return null;
                }
                remote.toggle_code = null;
            }
        }
        toggle_bit_mask_statep = toggle_bit_mask_state;
        return found;
    }

    private static IrRemote last_decoded = null;

    public static long set_code(IrRemote remote, IrNCode found, long toggle_bit_mask_state, boolean repeat_flag,
            int min_remaining_gap, int max_remaining_gap) {
        long code;
        Date current;
        //static IrRemote last_decoded = null;

        //LOGPRINTF(1, "found: %s", found.name);

        //gettimeofday(&current, null);
        current = new Date();
        //LOGPRINTF(1, "%lx %lx %lx %d %d %d %d %d %d %d",
        //	  remote, last_remote, last_decoded,
        //	  remote == last_decoded,
        //	  found == remote.last_code, found.next != null, found.current != null, repeat_flag,
        //	  time_elapsed(&remote.last_send, &current) < 1000000, (!has_toggle_bit_mask(remote)
        //								 || toggle_bit_mask_state ==
        //								 remote.toggle_bit_mask_state));

        if (remote.release_detected) {
            remote.release_detected = false;
            //if (repeat_flag) {
                //LOGPRINTF(0, "repeat indicated although release was detected before");
            //}
            repeat_flag = false;
        }
        if (remote == last_decoded
                && (found == remote.last_code || (found.getNext() != null && found.getCurrent() != null))
                && repeat_flag && time_elapsed(remote.last_send, current) < 1000000 && (!has_toggle_bit_mask(remote)
                || toggle_bit_mask_state
                == remote.toggle_bit_mask_state)) {
            if (has_toggle_mask(remote)) {
                remote.toggle_mask_state++;
                if (remote.toggle_mask_state == 4) {
                    remote.reps++;
                    remote.toggle_mask_state = 2;
                }
            } else if (found.getCurrent() == null) {
                remote.reps++;
            }
        } else {
            if (found.getNext() != null && found.getCurrent() == null) {
                remote.reps = 1;
            } else {
                remote.reps = 0;
            }
            if (has_toggle_mask(remote)) {
                remote.toggle_mask_state = 1;
                remote.toggle_code = found;
            }
            if (has_toggle_bit_mask(remote)) {
                remote.toggle_bit_mask_state = toggle_bit_mask_state;
            }
        }
        last_remote = remote;
        last_decoded = remote;
        if (    found.getCurrent() == null)
            remote.last_code = found;
        remote.last_send = current;
        remote.min_remaining_gap = min_remaining_gap;
        remote.max_remaining_gap = max_remaining_gap;

        code = 0;
        if (has_pre(remote)) {
            code |= remote.pre_data;
            code = code << remote.bits;
        }
        code |= found.getCode();
        if (has_post(remote)) {
            code = code << remote.post_data_bits;
            code |= remote.post_data;
        }
        if ((remote.flags & COMPAT_REVERSE) != 0) {
            /* actually this is wrong: pre, code and post should
             be rotated separately but we have to stay
             compatible with older software
             */
            code = reverse(code, bit_count(remote));
        }
        return (code);
    }

    private static String write_message(String remote_name, String button_name,
            String button_suffix, long code, int reps) {
        //int len;

        //len = snprintf(buffer, size, "%016llx %02x %s%s %s\n", code, reps, button_name, button_suffix, remote_name);
        String buffer = String.format("%016x %02x %s%s %s", code, reps, button_name, button_suffix, remote_name);
        //return len;
        return buffer;
    }

    public static String decode_all(IrRemote remotes) {
        IrRemote remote;
        //static char message[PACKET_SIZE + 1];
        Long pre = 0L, code = 0L, post = 0L;
        IrNCode ncode;
        Boolean repeat_flag = false;
        Long toggle_bit_mask_state = 0L;
        Integer min_remaining_gap = 0, max_remaining_gap = 0;
        IrRemote scan;
        //IrNCode scan_ncode;

        /* use remotes carefully, it may be changed on SIGHUP */
        decoding = remote = remotes;
        while (remote != null) {
            //LOGPRINTF(1, "trying \"%s\" remote", remote.name);

            if (Hardware.getHw().decode_func(remote, pre, code, post, repeat_flag, min_remaining_gap, max_remaining_gap) != 0
                    && ((ncode = get_code(remote, pre, code, post, toggle_bit_mask_state)) != null)) {
                int len;
                int reps;

                code = set_code(remote, ncode, toggle_bit_mask_state, repeat_flag, min_remaining_gap,
                        max_remaining_gap);
                if ((has_toggle_mask(remote) && remote.toggle_mask_state % 2 != 0) || ncode.getCurrent() != null) {
                    decoding = null;
                    return null;
                }

                for (scan = decoding; scan != null; scan = scan.next) {
                    for (IrNCode scan_ncode : scan.codes) {//; scan_ncode.name != null; scan_ncode++) {
                        scan_ncode.setCurrent(null);
                    }
                }
                if (is_xmp(remote)) {
                    remote.last_code.setCurrent(remote.last_code.getNext());
                }
                reps = remote.reps - (ncode.getNext() != null ? 1 : 0);
                if (reps > 0) {
                    if (reps <= remote.suppress_repeat) {
                        decoding = null;
                        return null;
                    } else {
                        reps -= remote.suppress_repeat;
                    }
                }
                // Do we need this cruft?
                //register_button_press(remote, remote.last_code, code, reps);

                String message = write_message(remote.name, remote.last_code.getName(), "", code, reps);
                decoding = null;
                //if (len >= PACKET_SIZE + 1) {
                //	logprintf(LOG_ERR, "message buffer overflow");
                //	return (null);
                //} else {
                return message;
                //}
            } else {
                //LOGPRINTF(1, "failed \"%s\" remote", remote.name);
            }
            remote.toggle_mask_state = 0;
            remote = remote.next;
        }
        decoding = null;
        last_remote = null;
        //LOGPRINTF(1, "decoding failed for all remotes");
        return null;
    }

    int send_ir_ncode(IrNCode code) {
	int ret;

        ret = Hardware.getHw().send_func(this, code);

        if (ret != 0) {
            //gettimeofday( & remote.last_send, null);
            last_send = new Date();
            last_code = code;
        }

        return ret;
    }
    /////////////////////////// end of ir_remote.c

    // moved here from config_file.c

    private boolean sanityChecks() {
        if (this.name == null) {
            logprintf(LOG_ERR, "you must specify a remote name");
            return false;
        }
        if (this.gap == 0) {
            logprintf(LOG_WARNING, "you should specify a valid gap value");
        }
        if (this.has_repeat_gap() && this.is_const()) {
            logprintf(LOG_WARNING, "repeat_gap will be ignored if CONST_LENGTH flag is set");
        }

        if (this.is_raw())
            return true;

        if ((this.pre_data & IrRemote.gen_mask(this.pre_data_bits)) != this.pre_data) {
            logprintf(LOG_WARNING, "invalid pre_data found for %s", this.name);
            this.pre_data &= IrRemote.gen_mask(this.pre_data_bits);
        }

        if ((this.post_data & IrRemote.gen_mask(this.post_data_bits)) != this.post_data) {
            logprintf(LOG_WARNING, "invalid post_data found for %s", this.name);
            this.post_data &= IrRemote.gen_mask(this.post_data_bits);
        }

        for (IrNCode codes : this.codes) { //[0]; codes.name != null; codes++) {
            if ((codes.getCode() & IrRemote.gen_mask(this.bits)) != codes.getCode()) {
                logprintf(LOG_WARNING, "invalid code found for %s: %s", this.name, codes.getName());
                codes.setCode(codes.getCode() & IrRemote.gen_mask(this.bits));
            }
            for (IrCodeNode node = codes.getNext(); node != null; node = node.getNext()) {
                if ((node.getCode() & IrRemote.gen_mask(this.bits)) != node.getCode()) {
                    logprintf(LOG_WARNING, "invalid code found for %s: %s", this.name, codes.getName());
                    //node.code &= IrRemote.gen_mask(this.bits);
                    node.setCode(node.getCode() & IrRemote.gen_mask(this.bits));
                }
            }
        }

        return true;
    }


    void calculate_signal_lengths() {
        /*
        if (remote.is_const()) {
            remote.min_total_signal_length = IrRemote.min_gap(remote);
            remote.max_total_signal_length = IrRemote.max_gap(remote);
        } else {
            remote.min_gap_length = remote.min_gap();
            remote.max_gap_length = remote.max_gap();
        }

        int min_signal_length = 0, max_signal_length = 0;
        int max_pulse = 0, max_space = 0;
        int first_sum = 1;
	//IrNCode[] c = remote.codes;
	int i;

        //while (c.name != null) {
        for (IrNCode code : remote.codes) {
            //IrNCode code = c;
            IrCodeNode next = code.next;
            int first = 1;
            //int repeat = 0;
            do {
                if (first != 0) {
                    first = 0;
                } else {
                    code.code = next.code;
                    next = next.next;
                }
                for (int repeat = 0; repeat < 2; repeat++) {
                    if (init_sim(remote, code, repeat)) {
                        int sum = send_buffer.sum;

                        if (sum != 0) {
                            if (first_sum != 0|| sum < min_signal_length) {
                                min_signal_length = sum;
                            }
                            if (first_sum != 0|| sum > max_signal_length) {
                                max_signal_length = sum;
                            }
                            first_sum = 0;
                        }
                        for (i = 0; i < send_buffer.wptr; i++) {
                            if ((i & 1) != 0) {	// space
                                if (send_buffer.data[i] > max_space) {
                                    max_space = send_buffer.data[i];
                                }
                            } else {	// pulse
                                if (send_buffer.data[i] > max_pulse) {
                                    max_pulse = send_buffer.data[i];
                                }
                            }
                        }
                    }
                }
            } while (next != null);
            //c++;
        }
        if (first_sum != 0) {
            //* no timing data, so assume gap is the actual total length
            remote.min_total_signal_length = remote.min_gap();
            remote.max_total_signal_length = remote.max_gap();
            remote.min_gap_length = remote.min_gap();
            remote.max_gap_length = remote.max_gap();
        } else if (remote.is_const()) {
            if (remote.min_total_signal_length > max_signal_length) {
                remote.min_gap_length = remote.min_total_signal_length - max_signal_length;
            } else {
                logprintf(LOG_WARNING, "min_gap_length is 0 for '%s' remote", remote.name);
                remote.min_gap_length = 0;
            }
            if (remote.max_total_signal_length > min_signal_length) {
                remote.max_gap_length = remote.max_total_signal_length - min_signal_length;
            } else {
                logprintf(LOG_WARNING, "max_gap_length is 0 for '%s' remote", remote.name);
                remote.max_gap_length = 0;
            }
        } else {
            remote.min_total_signal_length = min_signal_length + remote.min_gap_length;
            remote.max_total_signal_length = max_signal_length + remote.max_gap_length;
        }
        LOGPRINTF(1, "lengths: %lu %lu %lu %lu", remote.min_total_signal_length, remote.max_total_signal_length,
                remote.min_gap_length, remote.max_gap_length);*/
    }


    // This is based on defineRemote from config_file.h
    // defineRemote(String key, String val, String val2, IrRemote rem) {
    private boolean setNamedParameter(String key, long val) {
        if (key.equalsIgnoreCase("bits")) {
            bits = (int) val;
        } else if (key.equalsIgnoreCase("eps")) {
            eps = (int) val;

        } else if (key.equalsIgnoreCase("aeps")) {
            aeps = (int) val;

        } else if (key.equalsIgnoreCase("plead")) {
            plead = (int) val;

        } else if (key.equalsIgnoreCase("ptrail")) {
            ptrail = (int) val;

        } else if (key.equalsIgnoreCase("pre_data_bits")) {
            pre_data_bits = (int) val;

        } else if (key.equalsIgnoreCase("pre_data")) {
            pre_data = val;

        } else if (key.equalsIgnoreCase("post_data_bits")) {
            post_data_bits = (int) val;

        } else if (key.equalsIgnoreCase("post_data")) {
            post_data = val;

        } else if (key.equalsIgnoreCase("gap")) {
            gap = (int) val;
        } else if (key.equalsIgnoreCase("repeat_gap")) {
            repeat_gap = (int) val;

        } // obsolete: use toggle_bit_mask instead
        else if (key.equalsIgnoreCase("toggle_bit")) {
            toggle_bit = (int) val;

        } else if (key.equalsIgnoreCase("toggle_bit_mask")) {
            toggle_bit_mask = val;

        } else if (key.equalsIgnoreCase("toggle_mask")) {
            toggle_mask = val;

        } else if (key.equalsIgnoreCase("rc6_mask")) {
            rc6_mask = val;

        } else if (key.equalsIgnoreCase("ignore_mask")) {
            ignore_mask = val;

        } // obsolete name
        else if (key.equalsIgnoreCase("repeat_bit")) {
            toggle_bit = (int) val;

        } else if (key.equalsIgnoreCase("suppress_repeat")) {
            suppress_repeat = (int) val;

        } else if (key.equalsIgnoreCase("min_repeat")) {
            min_repeat = (int) val;

        } else if (key.equalsIgnoreCase("min_code_repeat")) {
            min_code_repeat = (int) val;

        } else if (key.equalsIgnoreCase("frequency")) {
            freq = (int) val;

        } else if (key.equalsIgnoreCase("duty_cycle")) {
            duty_cycle = (int) val;

        } else {
            //logprintf(LOG_ERR, "error in configfile line %d:");
            logprintf(LOG_ERR, "unknown definiton or too few arguments: \"%s %s\"", key, val);
            return false;
        }
        return true;
    }

    private boolean setNamedParameter(String key, long x, long y) {
        if (key.equalsIgnoreCase("gap")) {
            gap = (int) x;
            gap2 = (int) y;
        } else if (key.equalsIgnoreCase("header")) {
            phead = (int) x;
            shead = (int) y;
        } else if (key.equalsIgnoreCase("three")) {
            pthree = (int) x;
            sthree = (int) y;
        } else if (key.equalsIgnoreCase("two")) {
            ptwo = (int) x;
            stwo = (int) y;
        } else if (key.equalsIgnoreCase("one")) {
            pone = (int) x;
            sone = (int) y;
        } else if (key.equalsIgnoreCase("zero")) {
            pzero = (int) x;
            szero = (int) y;
        } else if (key.equalsIgnoreCase("foot")) {
            pfoot = (int) x;
            sfoot = (int) y;
        } else if (key.equalsIgnoreCase("repeat")) {
            prepeat = (int) x;
            srepeat = (int) y;
        } else if (key.equalsIgnoreCase("pre")) {
            pre_p = (int) x;
            pre_s = (int) y;
        } else if (key.equalsIgnoreCase("post")) {
            post_p = (int) x;
            post_s = (int) y;
        } else {
            //logprintf(LOG_ERR, "error in configfile line %d:", line);
            logprintf(LOG_ERR, "unknown definiton: \"%s %s %s\"", key, x, y);
            return false;
        }
        return true;
    }


    // mine
    public String lircProtocolType() {
        return is_raw() ? "RAW_CODES"
                : is_space_enc() ? "SPACE_ENC"
                : is_space_first() ? "SPACE_FIRST"
                : is_rc5() ? "RC5"
                : is_rc6() ? "RC6"
                : is_rcmm() ? "RCMM"
                : is_goldstar() ? "GOLDSTAR"
                : is_grundig() ? "GRUNDIG"
                : is_bo() ? "BO"
                : is_xmp() ? "XMP"
                : "??";
    }

    //mine
    public ModulatedIrSequence toSequence(int[] array, boolean alternatingSigns) throws IncompatibleArgumentException {
        if (array[array.length - 1] == 0)
            array[array.length - 1] = alternatingSigns ? -gap : gap;
        return new ModulatedIrSequence(array, (double) freq);
    }

    public ModulatedIrSequence toSequence(ArrayList<Integer> signals, boolean alternatingSigns) throws IncompatibleArgumentException {
        int[] array = new int[signals.size() + signals.size() % 2];
        array[array.length - 1] = 0;
        int index = 0;
        for (Integer d : signals) {
            array[index] = (alternatingSigns && (index & 1) == 1) ? -d : d;
            index++;
        }
        return toSequence(array, alternatingSigns);
    }

    public IrSequence render(IrNCode code, boolean useSignsInRawSequences, boolean repeat, int debug) {
        IrSequence seq = null;

        try {
            Transmit transmit = null;
            if (code.getSignals() != null && !code.getSignals().isEmpty()) {
                seq = repeat ? this.toSequence(code.getSignals(), useSignsInRawSequences) : null;
            } else {
                transmit = new Transmit(this, code, debug, repeat);
                boolean success = transmit.getValid();
                //transmit.send_repeat(r);
                if (success) {
                    int[] arr = transmit.getData(this.min_remaining_gap);
                    seq = this.toSequence(arr, useSignsInRawSequences);
                }
            }
        } catch (IncompatibleArgumentException ex) {
            System.err.println(ex.getMessage()); // FIXME
        }
        return seq;
    }
}
