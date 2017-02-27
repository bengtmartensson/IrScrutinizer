package org.harctoolbox.jirc;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.harctoolbox.IrpMaster.DecodeIR;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSequence;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import org.harctoolbox.IrpMaster.IrpUtils;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;

/**
 * This class represents a remote in Lirc, with all its parameters.
 * It should preferably not be used outside of the package, and may be package private in a future version.
 */
final public class IrRemote {

    private static final int LOG_ERR = 1;
    private static final int LOG_WARNING = 2;
    private static IrRemote repeat_remote = null;

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

    private static final int PULSE_BIT = 0x01000000;

    private static final Map<String, Integer> all_flags = new HashMap<String, Integer>() {
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

    public static IrRemote getRepeat_remote() {
        return repeat_remote;
    }

    public static void setRepeat_remote(IrRemote remote) {
        repeat_remote = remote;
    }

    public static RemoteSet newRemoteSet(Collection<IrRemote> remotes, String configFilename,
            String creatingUser, boolean alternatingSigns, int debug) {
        if (remotes == null)
            return null;
        String decodeir_version = DecodeIR.getVersion();

        Map<String, Remote>girrRemotes = new LinkedHashMap<>(remotes.size());
        for (IrRemote irRemote : remotes) {
            Remote remote = irRemote.toRemote(alternatingSigns, debug);
            girrRemotes.put(remote.getName(), remote);
        }

        RemoteSet girr = new RemoteSet(creatingUser == null ? System.getProperty("user.name") : creatingUser, configFilename,
                (new Date()).toString(), Version.appName, Version.version,
                "DecodeIr", decodeir_version != null ? decodeir_version : "not found",
                null,
                girrRemotes);
        return girr;
    }

    static int bit_count(IrRemote remote) {
        return remote.pre_data_bits + remote.bits + remote.post_data_bits;
    }

    static int bits_set(long data) {
        return Long.bitCount(data);
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
    static boolean has_repeat(IrRemote remote) {
        return remote.prepeat > 0 && remote.srepeat > 0;
    }
    static void set_protocol(IrRemote remote, int protocol) {
        remote.flags &= ~(IR_PROTOCOL_MASK);
        remote.flags |= protocol;
    }
    public static boolean is_raw(IrRemote remote) {
        return remote.is_raw();
    }
    static boolean is_space_enc(IrRemote remote) {
        return remote.is_space_enc();
    }
    public static boolean is_space_first(IrRemote remote) {
        return remote.is_space_first();
    }
    public static boolean is_rc5(IrRemote remote) {
        return remote.is_rc5();
    }
    public static boolean is_rc6(IrRemote remote) {
        return remote.is_rc6();
    }
    static boolean is_biphase(IrRemote remote) {
        return remote.is_biphase();
    }
    public static boolean is_rcmm(IrRemote remote) {
        return remote.is_rcmm();
    }
    public static boolean is_goldstar(IrRemote remote) {
        return remote.is_goldstar();
    }
    public static boolean is_grundig(IrRemote remote) {
        return remote.is_grundig();
    }
    public static boolean is_bo(IrRemote remote) {
        return remote.is_bo();
    }
    public static boolean is_xmp(IrRemote remote) {
        return remote.is_xmp();
    }
    static boolean is_const(IrRemote remote) {
        return (remote.flags & CONST_LENGTH) != 0;
    }
    static boolean has_repeat_gap(IrRemote remote) {
        return remote.repeat_gap > 0;
    }
    static boolean has_pre(IrRemote remote) {
        return remote.pre_data_bits > 0;
    }
    static boolean has_post(IrRemote remote) {
        return remote.post_data_bits > 0;
    }
    public static boolean has_header(IrRemote remote) {
        return remote.has_header();
    }
    static boolean has_foot(IrRemote remote) {
        return remote.has_foot();
    }
    static boolean has_toggle_bit_mask(IrRemote remote) {
        return remote.toggle_bit_mask > 0;
    }
    static boolean has_ignore_mask(IrRemote remote) {
        return remote.ignore_mask > 0;
    }
    static boolean has_toggle_mask(IrRemote remote) {
        return remote.has_toggle_mask();
    }
    static int min_gap(IrRemote remote) {
        return (remote.gap2 != 0 && remote.gap2 < remote.gap) ? remote.gap2 : remote.gap;
    }
    static int max_gap(IrRemote remote) {
        return (remote.gap2 > remote.gap) ? remote.gap2 : remote.gap;
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
    private final int debug = 0;

    /**
     * name of remote control
     */
    private String name;
    private String driver;
    private boolean timingInfo; // ! lircCode
    private String source;
    private List<IrNCode> codes;

    /**
     * bits (length of code)
     */
    @SuppressWarnings("PackageVisibleField")
    int bits;
    /**
     * flags
     */
    @SuppressWarnings("PackageVisibleField")
    int flags;
    /**
     * eps (_relative_ tolerance)
     */
    @SuppressWarnings("PackageVisibleField")
    int eps;
    /**
     * detecing _very short_ pulses is difficult with relative tolerance for
     * some remotes, this is an _absolute_ tolerance to solve this problem
     * usually you can say 0 here
     */
    @SuppressWarnings("PackageVisibleField")
    int aeps;

    /**
     * pulse and space lengths of: header
     */
    @SuppressWarnings("PackageVisibleField")
    int phead;

    @SuppressWarnings("PackageVisibleField")
    int shead;

    /**
     * 3 (only used for RC-MM)
     */
    @SuppressWarnings("PackageVisibleField")
            int pthree;
    @SuppressWarnings("PackageVisibleField")
    int sthree;

    /**
     * 2 (only used for RC-MM)
     */
    @SuppressWarnings("PackageVisibleField")
    int ptwo;
    @SuppressWarnings("PackageVisibleField")
    int stwo;

    /**
     * 1
     */
    @SuppressWarnings("PackageVisibleField")
    int pone;
    @SuppressWarnings("PackageVisibleField")
    int sone;

    /**
     * 0
     */
    @SuppressWarnings("PackageVisibleField")
            int pzero;

    @SuppressWarnings("PackageVisibleField")
    int szero;
    /**
     * leading pulse
     */
    @SuppressWarnings("PackageVisibleField")
    int plead;
    /**
     * trailing pulse
     */
    @SuppressWarnings("PackageVisibleField")
    int ptrail;

    /**
     * foot
     */
    @SuppressWarnings("PackageVisibleField")
    int pfoot;

    @SuppressWarnings("PackageVisibleField")
    int sfoot;

    /**
     * indicate repeating
     */
    @SuppressWarnings("PackageVisibleField")
    int prepeat;

    @SuppressWarnings("PackageVisibleField")
    int srepeat;
    /**
     * length of pre_data
     */
    @SuppressWarnings("PackageVisibleField")
    int pre_data_bits;
    /**
     * data which the remote sends before actual keycode
     */
    @SuppressWarnings("PackageVisibleField")
    long pre_data;
    /**
     * length of post_data
     */
    @SuppressWarnings("PackageVisibleField")
    int post_data_bits;
    /**
     * data which the remote sends after actual keycode
     */
    @SuppressWarnings("PackageVisibleField")
    long post_data;

    /**
     * signal between pre_data and keycode
     */
    @SuppressWarnings("PackageVisibleField")
    int pre_p;

    @SuppressWarnings("PackageVisibleField")
    int pre_s;

    /**
     * signal between keycode and post_code
     */
    @SuppressWarnings("PackageVisibleField")
            int post_p;

    @SuppressWarnings("PackageVisibleField")
    int post_s;
    /**
     * time between signals in usecs
     */
    @SuppressWarnings("PackageVisibleField")
    int gap;
    /**
     * time between signals in usecs
     */
    @SuppressWarnings("PackageVisibleField")
    int gap2;
    /**
     * time between two repeat codes if different from gap
     */
    @SuppressWarnings("PackageVisibleField")
    int repeat_gap;
    /**
     * obsolete
     */
    @SuppressWarnings("PackageVisibleField")
    int toggle_bit;
    /**
     * previously only one bit called toggle_bit
     */
    @SuppressWarnings("PackageVisibleField")
    long toggle_bit_mask;
    /**
     * suppress unwanted repeats
     */
    @SuppressWarnings("PackageVisibleField")
    int suppress_repeat;
    /**
     * code is repeated at least x times code sent once . min_repeat=0
     */
    @SuppressWarnings("PackageVisibleField")
    int min_repeat;
    /**
     * meaningful only if remote sends a repeat code: in this case this value
     * indicates how often the real code is repeated before the repeat code is
     * being sent
     */
    @SuppressWarnings("PackageVisibleField")
    int min_code_repeat;
    /**
     * modulation frequency
     */
    @SuppressWarnings("PackageVisibleField")
    int freq = DEFAULT_FREQ;

    /**
     * 0<duty cycle<=100
     */
    @SuppressWarnings("PackageVisibleField")
    int duty_cycle;

    @SuppressWarnings("PackageVisibleField")
    int baud;

    /**
     * Sharp (?) error detection scheme
     */
    @SuppressWarnings("PackageVisibleField")
    long toggle_mask;
    /**
     * RC-6 doubles signal length of some bits
     */
    @SuppressWarnings("PackageVisibleField")
    long rc6_mask;
    /**
     * mask defines which bits can be ignored when matching a code
     */
    @SuppressWarnings("PackageVisibleField")
    long ignore_mask;
    // end of user editable values
    @SuppressWarnings("PackageVisibleField")
    long toggle_bit_mask_state;
    @SuppressWarnings("PackageVisibleField")
    int toggle_mask_state;
    @SuppressWarnings("PackageVisibleField")
    int repeat_countdown;
    /**
     * code received or sent last
     */
    @SuppressWarnings("PackageVisibleField")
    IrNCode last_code;
    /**
     * toggle code received or sent last
     */
    @SuppressWarnings("PackageVisibleField")
    IrNCode toggle_code;
    @SuppressWarnings("PackageVisibleField")
    int reps;
    /**
     * time last_code was received or sent
     */
    @SuppressWarnings("PackageVisibleField")
    Date last_send;
    /**
     * remember gap for CONST_LENGTH remotes
     */
    @SuppressWarnings("PackageVisibleField")
    int min_remaining_gap;
    /**
     * gap range
     */
    @SuppressWarnings("PackageVisibleField")
    int max_remaining_gap;
    /**
     * how long is the shortest signal including gap
     */
    @SuppressWarnings("PackageVisibleField")
    int min_total_signal_length;
    /**
     * how long is the longest signal including gap
     */
    private int max_total_signal_length;
    /**
     * how long is the shortest gap
     */
    @SuppressWarnings("PackageVisibleField")
    int min_gap_length;
    /**
     * how long is the longest gap
     */
    @SuppressWarnings("PackageVisibleField")
    int max_gap_length;
    @SuppressWarnings("PackageVisibleField")
    int min_pulse_length;
    @SuppressWarnings("PackageVisibleField")
    int max_pulse_length;
    @SuppressWarnings("PackageVisibleField")
    int min_space_length;
    @SuppressWarnings("PackageVisibleField")
    int max_space_length;
    /**
     * set by release generator
     */
    @SuppressWarnings("PackageVisibleField")
    boolean release_detected;
    @SuppressWarnings("PackageVisibleField")
    IrRemote next;

    private IrRemote() {
    }

    IrRemote(String name, String driver, List<String> flags, Map<String, Long> unaryParameters,
            Map<String, XY> binaryParameters, List<IrNCode> codes) {
        this.name = name;
        this.driver = driver;
        for (String flag  : flags)
            setNamedFlag(flag);
        for (Entry<String, Long> kvp : unaryParameters.entrySet())
            setNamedParameter(kvp.getKey(), kvp.getValue());

        for (Entry<String, XY> kvp : binaryParameters.entrySet())
            setNamedParameter(kvp.getKey(), kvp.getValue().x, kvp.getValue().y);

        this.codes = codes;
        this.timingInfo = hasSaneTimingInfo() || is_raw();
        sanityChecks();
    }

    private void logprintf(int level, String format, Object... args) {
        System.err.println(String.format(format, args));
    }

    public Map<String, String> getApplicationData() {
        Map<String, String> applicationData = new LinkedHashMap<>(32);
        if (driver != null)
            applicationData.put("driver", driver);
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
        applicationData.put("baud", Integer.toString(baud));	//
        applicationData.put("toggle_mask", Long.toString(toggle_mask)); // Sharp (?) error detection scheme
        applicationData.put("rc6_mask", Long.toString(rc6_mask)); // RC-6 doubles signal length of some bits
        applicationData.put("ignore_mask", Long.toString(ignore_mask)); // mask defines which bits can be ignored when matching a code

        return applicationData;
    }

    public Remote toRemote(boolean alternatingSigns, int debug) {
        Map<String, Map<String, String>> appDataMap = new LinkedHashMap<>(8);
        appDataMap.put("jirc", getApplicationData());

        //remote.last_code = null;

        Map<String, Command> commands = new LinkedHashMap<>(32);
        for (IrNCode c : getCodes()) {
            Command command = toCommand(c, alternatingSigns, debug);
            if (command != null)
                commands.put(command.getName(), command);
        }

        Remote.MetaData metaData = new Remote.MetaData(getName());
        return new Remote(metaData,
                null, // comment,
                null, // notes,
                commands,
                appDataMap);
    }

    Command toCommand(IrNCode code, boolean alternatingSigns, int debug) {
        return isTimingInfo()
                ? toTimedCommand(code, alternatingSigns, debug)
                : toLircCodeCommand(code);
    }

    Command toLircCodeCommand(IrNCode code) {
        Map<String, Long> parameters = new HashMap<>(1);
        parameters.put("lirc", code.getCode());
        try {
            return new Command(code.getName(), null, "lircdriver:" + driver, parameters);
        } catch (IrpMasterException ex) {
            // this cannot happen
            throw new InternalError();
        }
    }

    Command toTimedCommand(IrNCode code, boolean alternatingSigns, int debug) {
        IrSignal irSignal = toIrSignal(code, alternatingSigns, debug);
        return irSignal != null
                ? new Command(code.getName(), /*comment=*/null, irSignal)
                : null;
    }

    IrSignal toIrSignal(IrNCode code, boolean alternatingSigns, int debug) {
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

        return new IrSignal(freq,  /* duty cycle= */IrpUtils.invalid, intro, repeat,  /* ending= */ null);
    }

    private boolean setNamedFlag(String flagName) {
        if (!all_flags.containsKey(flagName))
            return false;

        flags |= all_flags.get(flagName);
        return false;
    }

    public String getName() {
        return name;
    }

    public boolean isTimingInfo() {
        return timingInfo;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    List<IrNCode> getCodes() {
        return codes;
    }

    public int getFreq() {
        return freq;
    }

    public int getDutyCycle() {
        return duty_cycle;
    }

    /*      $Id: ir_remote.h,v 5.44 2010/04/11 18:50:38 lirc Exp $      */

    public int bit_count() {
        return pre_data_bits + bits + post_data_bits;
    }

    public boolean has_repeat() {
        return prepeat > 0 && srepeat > 0;
    }

    public void set_protocol(int protocol) {
        flags &= ~(IR_PROTOCOL_MASK);
        flags |= protocol;
    }

    public boolean is_raw() {
        return (flags & IR_PROTOCOL_MASK) == RAW_CODES;
    }

    public boolean is_space_enc() {
        return (flags & IR_PROTOCOL_MASK) == SPACE_ENC;
    }

    public boolean is_space_first() {
        return (flags & IR_PROTOCOL_MASK) == SPACE_FIRST;
    }

    public boolean is_rc5() {
        return (flags & IR_PROTOCOL_MASK) == RC5;
    }

    public boolean is_rc6() {
        return (flags & IR_PROTOCOL_MASK) == RC6 || rc6_mask != 0;
    }

    boolean is_biphase() {
        return is_rc5() || is_rc6();
    }


    public boolean is_rcmm() {
        return (flags & IR_PROTOCOL_MASK) == RCMM;
    }

    public boolean is_goldstar() {
        return (flags & IR_PROTOCOL_MASK) == GOLDSTAR;
    }

    public boolean is_grundig() {
        return (flags & IR_PROTOCOL_MASK) == GRUNDIG;
    }

    public boolean is_bo() {
        return (flags & IR_PROTOCOL_MASK) == BO;
    }

    public boolean is_xmp() {
        return (flags & IR_PROTOCOL_MASK) == XMP;
    }

    public boolean is_const() {
        return (flags & CONST_LENGTH) != 0;
    }

    public boolean has_repeat_gap() {
        return repeat_gap > 0;
    }

    public boolean has_pre() {
        return pre_data_bits > 0;
    }

    public boolean has_post() {
        return post_data_bits > 0;
    }

    public boolean has_header() {
        return phead > 0 && shead > 0;
    }

    boolean has_foot() {
        return pfoot > 0 && sfoot > 0;
    }

    public boolean has_toggle_bit_mask() {
        return toggle_bit_mask > 0;
    }


    boolean has_toggle_mask() {
        return toggle_mask > 0;
    }

    public int min_gap() {
        return (gap2 != 0 && gap2 < gap) ? gap2 : gap;
    }

    public int max_gap() {
        return (gap2 > gap) ? gap2 : gap;
    }

    private boolean hasSaneTimingInfo() {
        return pzero != 0 && szero != 0 && pone != 0 && sone != 0;
    }

    // from config_file.c
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

        } else if (key.equalsIgnoreCase("baud")) {
            baud = (int) val;

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
        return new ModulatedIrSequence(array, freq);
    }

    public ModulatedIrSequence toSequence(List<Integer> signals, boolean alternatingSigns) throws IncompatibleArgumentException {
        int[] array = new int[signals.size() + signals.size() % 2];
        array[array.length - 1] = 0;
        int index = 0;
        for (Integer d : signals) {
            array[index] = (alternatingSigns && (index & 1) == 1) ? -d : d;
            index++;
        }
        return toSequence(array, alternatingSigns);
    }

    IrSequence render(IrNCode code, boolean useSignsInRawSequences, boolean repeat, int debug) {
        IrSequence seq = null;

        try {
            Transmit transmit;
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

    @SuppressWarnings("PackageVisibleInnerClass")
    static class XY {

        @SuppressWarnings("PackageVisibleField")
        long x;
        @SuppressWarnings("PackageVisibleField")
        long y;

        XY(long x, long y) {
            this.x = x;
            this.y = y;
        }
    }
}
