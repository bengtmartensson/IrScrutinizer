/*
 Copyright (C) 2013 Bengt Martensson.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 3 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License along with
 this program. If not, see http://www.gnu.org/licenses/.
 */
package org.harctoolbox.irscrutinizer;

/**
 * This class just bundles the help texts together, just for keeping them out of
 * the main source.
 */
class HelpTexts {

    // Do not use "final" on the public strings, even though it would be logical. See
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6447475

    // Findbugs sez:
    // Huge string constants is duplicated across multiple class files
    // A large String constant is duplicated across multiple class files.
    // This is likely because a final field is initialized to a String constant,
    // and the Java language mandates that all references to a final field from other
    // classes be inlined into that classfile. See JDK bug 6447475 for a description
    // of an occurrence of this bug in the JDK and how resolving it reduced the size of the JDK by 1 megabyte.

    private static final String treeHelpReaderImporterIntro
            = "When pressing one of the \"Load\", \"Load File/URL\", or \"Load from clipboard\" buttons, ";

    private static final String treeHelpFileImporterIntro
            = "When pressing the \"Load File\" button, ";

    private static final String treeHelp
            = "the selected information is downloaded, and presented in the format of "
            + "an expandable tree. By placing the mouse cursor over a command, additional information, like decode, is presented. A single "
            + "command can be selected by mouse click, a sequence of adjacent commands by "
            + "shift-click, a subset of not necessarily adjacent commands be selected by "
            + "Ctrl-click, as usual from most GUIs. A single selected command can be transferred to the "
            + "\"Scrutinize signal\" pane by pressing \"Import signal\". The \"Import all\" (\"Import selection\") button "
            + "transfers all commands (the selected commands) to the \"Scrutinize remote\" pane, "
            + "sub-pane \"Parametric remote\" (without overwriting already present commands), while the buttons \"Import all/raw\" and \"Import "
            + "selected/raw\" transfer to the sub-pane \"Raw remote\"."
            + "The key \"Transmit selected\" transmits the (single) selected signal to the selected sending hardware."
            + "\n\n"
            + "The \"Import all\" and \"Import all/raw\" buttons are only enabled if there are at most 10 remotes in the tree.";

    private static final String fileUrlImportHelp
            = "There is a text field, named File/URL. Either a local file name or an URL can be "
            + "entered, for subsequent import without downloading to a local disc. By pressing "
            + "the \"...\"-Button, a file selector allows the selection of a local file. For "
            + "files and URLs, the \"Edit/Browse\" button allows to examine the selected "
            + "file/URL with the operating system's standard command."
            + "\n\n"
            + "It is also possible to import a file by \"dropping\" it on the window.";

    private static final String fileImportHelp
            = "There is a text field, named File, where a local file name can be "
            + "entered. By pressing "
            + "the \"...\"-Button, a file selector allows the selection of a local file. "
            + "The \"Edit/Browse\" button allows to examine the selected "
            + "file with the operating system's standard command."
            + "\n\n"
            + "It is also possible to import a file by \"dropping\" it on the window.";

    private static final String importCharsetHelp
            = "The character set used for the import can be selected as Options -> Import options -> Character set.";

    @SuppressWarnings("PackageVisibleField")
    public static String scrutinizeSignalHelp = "<html><div>This panel is devoted to the analysis of a single IR signal. "
            + "The signal is either read from hardware using the \"Capture\" button (requires that "
            + "the capturing hardware has been set on the \"Capturing Hardware\" pane), imported "
            + "from a file (using the context menu in the data window, or through File -> "
            + "Import -> Import as single sequence, or pasted from the clipboard. For text "
            + "import, the signal can be in either Pronto CCF format, raw format (indicated by "
            + "a leading \"+\"-sign), or in the UEI learned format. The signal is printed in the "
            + "data window, in the preferred text format, which can be selected from the "
            + "options menu. The text representation may be edited (assuming sufficient "
            + "knowledge!), after which the edited signal is analyzed and plotted again by "
            + "pressing the \"Scrutinize\" button. The signal may be sent to the sending "
            + "hardware by pressing the \"Transmit\" button.</div><br/>"
            + "<div>Using context menus, the result can be sent to the clipboard or saved to a file.</div><br/>"
            + "<div>The plot can be zoomed by pressing the left mouse button and dragging.</div><br/>"
            + "<div>In rare cases, transforming the signal between different formats may introduce "
            + "some rounding errors causing decoding to fail.</div></html>";

    @SuppressWarnings("PackageVisibleField")
    public static String scrutinizeRemoteHelp
            = "This panel is devoted to the capturing/import/editing of a collection of IR signals, called \"a remote\". The panel contains two sub-panels: for parametric signals and for non-parametric, \"raw\", signals.\n\n"
            + "A \"parametric\" signal is determined by its protocol name, and the values of the protocol's parameters. A \"raw\" signal is determined by its timing pattern, and its modulation frequency. It may have one or many decodes, or none. Nevertheless, by definition, a raw signal is determined by its timing, not the decodes.\n\n"
            + "In both cases, the sub panes consists of tables with a number of columns. Every signal takes up a row in the table. The content of the individual cells (with the exception of its number and date) can be individually edited, like in a spreadsheet program.\n\n"
            + "In both tables, the right mouse button opens a context menu containing a number of ways to manipulate the table, its view, or the data contained therein. By enabling the row selector, the rows can be sorted along any of the present columns.\n\n"
            + "To capture a number of IR signals, first configure the hardware using the capturing hardware pane. Next press the Capture button. The program will now run a separate capturing thread, so the user just have to press the buttons of the remote. The signals will be received, interpreted, decoded, and entered on subsequent lines in the selected table (raw or parameterized). The capture thread will continue until the captured button is pressed again. The user may mix captures with other activities, like entering information (name, comments,...) in the table.\n\n"
            + "The export button exports the content of the currently selected table (raw or parameterized) according to the currently selected export format.\n\n"
            + "The menu entry Actions -> Enter test signal (or its accelerator, the F9 key) enters a test signal, either as parametric signal, or as a raw signal.\n\n"
            + "Key bindings:\n"
            + "* Ctrl-Up: Move selection up\n"
            + "* Ctrl-Down: Move selection down\n"
            + "* Ctrl-Delete: Delete selection\n"
            + "* Ctrl-Shift-Delete: Delete all\n"
            + "* Escape: Inhibit selection.";

    @SuppressWarnings("PackageVisibleField")
    public static String generateHelp
            = "In the upper part of this pane, an IR protocol is selected, identified by name, and the parameters D (\"device\", in almost all protocols), S (\"sub-device\", not in all protocols), F (\"function\", also called command number or OBC, present in almost all protocols), as well as T, \"toggle\" (in general 0 or 1, only in a few protocols). These number can be entered as decimal numbers, or, by prepending \"0x\", as hexadecimal numbers.\n\n"
            + "By pressing \"Generate\", the signal is computed, and the middle window is filled with its textual representation, in the form selected by Options -> Output Text Format.\n\n"
            + "The Export button initiates an export to a file format selected by the Export pane. The three lower buttons transfer the signal(s) to the scrutinize signal panel, the raw remote table, or the parameterized panel.\n\n"
            + "For the export and the transfer to the \"scrutinize remote\" tables, not only a single parameter value can be selected, but whole sets. The complete syntax and semantics is given in the IrpMaster documentation. Here we just mention that e.g. 12:34 means all numbers between 12 and 34, and * denotes all possible values.";

    @SuppressWarnings("PackageVisibleField")
    public static String importControlTowerHelp
            = "<html>The support for the Control Tower Data base is limited. It can be used for browsing the data base "
            + "for devices and the contained commands, but it cannot retreive the very codes of the commands. "
            + "For this, <a href=\"http://irdb.globalcache.com\">login there</a>, and have the codes emailed. "
            + "The received email can be parsed by Import -> Raw; use Name col. = 1 and Raw signal col. = 3 (or 2), Field separator \", (comma)\"."
            + "<p>Please observe the <a href=\"https://www.globalcache.com/files/docs/CT_tos_ver1.pdf\">Terms of service</a>.</p></html>";

    @SuppressWarnings("PackageVisibleField")
    public static String remoteLocatorHelp
            = "FIXME: To use the IRDB data base, select, in order, a manufacturer, a device type, and a protocol/parameter combination, the latter possibly by trial-and-error.\n\n"
            + "Pressing the \"Load all\" button transfers all present protocol/parameters combinations to the tree.\n\n"
            + "When pressing the \"Load\" button, " + treeHelp;

    @SuppressWarnings("PackageVisibleField")
    public static String importGirrHelp
            = "Girr is the native format of IrScrutinizer.\n\n"
            + fileUrlImportHelp + "\n\n"
            + treeHelpReaderImporterIntro
            + treeHelp;

    @SuppressWarnings("PackageVisibleField")
    public static String importIrTransHelp
            = "IrTrans' configuration files can be imported here.\n\n"
            + fileUrlImportHelp + "\n\n"
            + treeHelpReaderImporterIntro
            + treeHelp;
    
    @SuppressWarnings("PackageVisibleField")
    public static String importFlipperHelp
            = "Flipper IR files can be imported here.\n\n"
            + fileUrlImportHelp + "\n\n"
            + treeHelpReaderImporterIntro
            + treeHelp;

    @SuppressWarnings("PackageVisibleField")
    public static String importLircHelp
            = "LIRC files can be imported here, even directory hierarchies of LIRC files.\n\n"
            + importCharsetHelp + "\n\n"
            + fileUrlImportHelp + "\n\n"
            + treeHelpReaderImporterIntro
            + treeHelp;

    @SuppressWarnings("PackageVisibleField")
    public static String importProntoClassicHelp
            = "Many Pronto CCF files are available in Internet, in particular by RemoteCentral. "
            + "IrScrutinizer can read in these files to its import tree, even "
            + "preserving the Pronto \"devices\" as nodes in the tree.\n\n"
            + fileImportHelp + "\n\n"
            + treeHelpFileImporterIntro + treeHelp;

    @SuppressWarnings("PackageVisibleField")
    public static String importCmlHelp
            = "Many CML files are available in Internet, in particular by RemoteCentral. "
            + "Particularly noteworthy is the \"megalist\", http://files.remotecentral.com/collection/66-1/index.html. "
            + "IrScrutinizer can import these files to its import tree, "
            + "making every remote a nodes in the tree.\n\n"
            + fileImportHelp + "\n\n"
            + treeHelpFileImporterIntro + treeHelp;

    @SuppressWarnings("PackageVisibleField")
    public static String importCommandFusionHelp
            = "The native format for CommandFusion equipment (for example, their freely available IR Learner) "
            + "is a Json based text format, having the file extension \".cfir\". "
            + "IrScrutinizer can read in these files to its import tree, "
            + "making every remote a nodes in the tree.\n\n"
            + treeHelpReaderImporterIntro
            + treeHelp;

    @SuppressWarnings("PackageVisibleField")
    public static String importIctHelp
            = "The ICT format, introduced by Kevin Timmerman's  IrScope, contains one or many IR signals, "
            + "optionally with names.\n\n"
            + fileUrlImportHelp + "\n\n"
            + treeHelpReaderImporterIntro
            + treeHelp;

    @SuppressWarnings("PackageVisibleField")
    public static String importMode2Help
            = "The mode2 \"format\" just consists of interleaved mark and space lines. It is output  by the Lirc \"mode2\" program."
            + "\n\n"
            + fileUrlImportHelp + "\n\n"
            + treeHelpReaderImporterIntro
            + treeHelp;

    @SuppressWarnings("PackageVisibleField")
    public static String importProntoProfessionalHelp
            = "Pronto Professional XCF files are found for example at RemoteCentral. "
            + "IrScrutinizer can read in these files to its import tree, even "
            + "preserving the Pronto \"devices\" as nodes in the tree.\n\n"
            + fileImportHelp + "\n\n"
            + treeHelpFileImporterIntro + treeHelp;

    @SuppressWarnings("PackageVisibleField")
    public static String importTextRawHelp
            = "The sub-pane allows for the parsing of text files separated by a certain "
            + "characters, like commas, semicolons, or tabs. The separating characters is "
            + "selected in the \"Field separator\" combo box. The column to be used as name is "
            + "entered in the \"Name col.\" combo box, while the data to be interpreted either as "
            + "raw data or CCF format, is entered  in the \"Raw signal col.\". If the \"... and "
            + "subsequent columns\" is selected, all subsequent columns are added to the data." + "\n\n"
            + importCharsetHelp + "\n\n"
            + fileImportHelp + "\n\n"
            + treeHelpFileImporterIntro + treeHelp;

    @SuppressWarnings("PackageVisibleField")
    public static String importTextRawLineBasedHelp
            = "This pane tries to interpret a line-based file as a number of named IR commands, using heuristics.\n\n"
            + importCharsetHelp + "\n\n"
            + fileUrlImportHelp + "\n\n"
            + treeHelpReaderImporterIntro
            + treeHelp;

    @SuppressWarnings("PackageVisibleField")
    public static String importTextParametrizedHelp
            = "The sub-pane allows for the parsing of text files separated by a certain "
            + "characters, like commas, semicolons, or tabs. The separating characters is "
            + "selected in the \"Field separator\" combo box. The column to be used as name is "
            + "entered in the \"Name col.\" combo box, while protocol name and the parameters D, "
            + "S, and F are entered in their respective combo boxes. They are parsed in the "
            + "number base selected.\n\n"
            + importCharsetHelp + "\n\n"
            + fileImportHelp + "\n\n"
            + treeHelpFileImporterIntro + treeHelp;

    @SuppressWarnings("PackageVisibleField")
    public static String importWaveHelp
            = "The pane imports and analyzes wave files, considered to represent IR "
            + "signals. The outcome of the analysis (sample frequency, sample size, the number "
            + "of channels, and in the case of two channels, the number of sample times the "
            + "left and right channels are in phase or anti-phase) is printed to the console.\n\n"
            + fileImportHelp + "\n\n"
            + treeHelpFileImporterIntro + treeHelp;

    @SuppressWarnings("PackageVisibleField")
    public static String exportHelp
            = "<html>Using this pane, export files can be generated,  allowing "
            + "other programs to use the computed results. Single signals (from the "
            + "\"Scrutinize signal\" pane), collections of signals (from the \"Scrutinize remote\" "
            + "pane), or generated signals can be exported. Exports can be generated in a "
            + "number of different formats. Some (Girr (=XML) and text) can contain both "
            + "the Pronto format and the \"raw\" format (timings in "
            + "microseconds, positive for pulses, negative for gaps), as well as other "
            + "formats. These formats, together with Wave and Pronto Classic, are "
            + "built-in in the program. However, it is possible to define new "
            + "export formats by extending a configuration file, see the documentation."
            + "The formats are, at the time of this writing:\n\n"
            + "<div>"
            + "Girr:  "
            + "The program's native format, based on XML. Very flexible and "
            + "extensible. Can contain information like the raw format, Pronto Hex format "
            + "and the Global Caché sendir format.</div>\n"
            + "Text:  "
            + "The text format is essentially the Girr format stripped of the XML markup information.\n"
            + "Wave:  "
            + "IR Sequences packed in wave files.\n "
            + "Lirc Raw:   "
            + "The LIRC-exports are in lirc.conf-format using the raw LIRC format. They can be "
            + "concatenated together and used as the LIRC server data base. Can also be used with WinLirc.\n"
            + "Pronto Classic:   "
            + "This format generates a CCF configuration file to be downloaded in a Pronto Classoc, or opened by a ProntoEdit program.\n"
            + "IrTrans:   "
            + "This export format generates .rem files for the IrTrans system, using its CCF format.\n "
            + "Lintronic:   "
            + "Simple text protocol for describing a single IrSequence.\n "
            + "Spreadsheet:   "
            + "Simple tab separated value export format for importing in a spreadsheet program.\n "
            + "RM Functions:   "
            + "Variant of the Spreadsheet format, this format is intended to be pasted "
            + "directly into the \"Functions\" table of RemoteMaster.\n "
            + "C:   "
            + "Intended mostly as an example  of generating C code.\n "
            + "TV-B-Gone:   "
            + "Variant of the C format, this format generates C code for the TV-B-Gone.\n\n"
            + "Export file names are either user selected from a file  "
            + "selector, or, if \"Automatic file names\" has been selected, "
            + "automatically generated.\n\n"
            + "The export is performed by pressing the \"Export\" button. The \"...\"-marked "
            + "button allows for manually selecting the export directory. It is recommended to "
            + "create a new, empty directory for the exports. The just-created export file can "
            + "be immediately inspected by pressing the \"Open last file\"-button, which will open "
            + "it in the \"standard way\" of the used operating system. (Also available on the "
            + "actions menu.) The \"Open\" button "
            + "similarly opens the operating systems standard directory browser on the export directory.\n\n"
            + "Some export formats (presently Wave and Lintronic)  export an IR sequence rather than an "
            + "IR signal (consisting of an intro sequence, an repetition sequence (to be "
            + "included 0 or more times), and an (most often empty) ending "
            + "sequence). Using these formats, the number of repetition sequences to include "
            + "can be selected."
            + "\n\n"
            + "The character set used by the exports can be selected by Options -> Export options -> Character set..."
            + "However, some export formats ignore this."
            + "\n\n"
            + "Some export formats have some more parameters, see the help in these sub panes.</html>";

    @SuppressWarnings("PackageVisibleField")
    public static String exportGirrHelp
            = "\"Fat form raw\" can be selected; this means that the raw signals are not given "
            + " as a text string of alternating positive and negative numbers, but the "
            + " individual flashes and gaps are enclosed into own XML elements. This can be "
            + " advantageous if generating XML mainly for the purpose of transforming to other formats.";

    @SuppressWarnings("PackageVisibleField")
    public static String exportWaveHelp
            = "Parameters for the generated Wave export (except for the number of repeats) can be selected here.";

    @SuppressWarnings("PackageVisibleField")
    public static String exportProntoHelp
            = "A Pronto Classic export consists of a CCF file with the exported signals "
            + "associated to dummy buttons. The Pronto (Classic) model for which the export is "
            + "designed is entered in the combo box. Screen size of the Pronto is normally "
            + "inferred from the model, but can be changed here. The button size of the "
            + "generated buttons is also entered here.";

    @SuppressWarnings("PackageVisibleField")
    public static String capturingHardwareHelp
            = "The sub-panes of this pane allow for the configuration of capturing hardware. Selecting a sub-pane also selects the associated hardware, if possible.\n\n"
            + "Unfortunately, by e.g. selecting non-existing hardware or such, there is a possibility to \"hang\" the program.\n\n"
            + "After configuring and opening the capturing hardware, the \"Test\" button can be used for testing the configuration without switching pane.\n\n"
            + "Selected ports are stored in the properties, thereby remembered between sessions. So, for future sessions, only opening the preferred device is necessary.\n\n"
            + "Note that selecting a sub-pane selects the associated capturing hardware, to the extent possible.";

    @SuppressWarnings("PackageVisibleField")
    public static String irWidgetHardwareHelp
            = "Connect the IrWidget to the computer using a free USB-port. "
            + "Check that the operating system has assigned a port to it, and note which one it is. "
            + "On Windows: open the device manager, and check that there is one \"USB Serial Port\" under Ports. Note the port number (e.g. COM8). "
            + "On a Linux system, it likely shows up as a device like /dev/ttyUSB0. If the port does not show up, a suitable driver needs to be installed. "
            + "If the correct port is already visible in the combo box, just press \"Open\". "
            + "Otherwise, press \"Refresh\", which makes the program determine the available serial ports. Select the correct one. "
            + "Press \"Open\". which should now remain \"pressed\". "
            + "The port can be closed again by a repeated press, but there is not much reason to do so, "
            + "unless another capturing hardware should be used, or the IrWidget should be used from another program.";

    @SuppressWarnings("PackageVisibleField")
    public static String sendingHardwareHelp
            = "The sub-panes of this pane allows for the selection and configuration of the employed IR sending hardware. Note that selecting a sub-pane selects the associated hardware for sending, to the extent possible";

    @SuppressWarnings("PackageVisibleField")
    public static String globalCacheHelp
            = "IrScrutinizer automatically detects alive Global Caché units in the local area "
            + "network, using the AMX Beacon. However, this may take "
            + "up to 60 seconds, and is not implemented in very old firmware. Using the \"Add\""
            + "button, the IP address/name of older units can be entered manually.\n\n"
            + "The \"Browse\" button points the browser to the selected unit.\n\n"
            + "The reported type and firmware version serves to verify that the communication is working.\n\n"
            + "\"Stop IR\"-Button allows the interruption of ongoing transmission, "
            + "possibly initiated from another source.\n\n"
            + "The user can select one of the thus available Global Caché units, together with "
            + "IR-module and IR-port.";

    @SuppressWarnings("PackageVisibleField")
    public static String sendingAudioHelp
            = "IrScrutinizer can generate IR signals as wave files, which can be"
            + "exported from the export pane, or sent to the local computers sound card. There"
            + "are some settings available: Sample frequency (44100, 48000, 96000, 192000Hz),"
            + "sample size (8 or 16 bits) can be selected. Also \"stereo\" files can be"
            + "generated by selecting the number of channels to be 2. The use of this feature"
            + "is somewhat limited: it just generates another channel in opposite phase to the"
            + "first one, for hooking up the IR LEDs to the difference signal between the"
            + "left and the right channel. This will buy you double amplitude (6 dB) at the"
            + "cost of doubling the file sizes. If the possibility exists, it is better to"
            + "turn up the volume instead.\n\n"
            + "Most of \"our\" IR sequences ends with a period of silence almost for the half"
            + "of the total duration. By selecting the \"Omit trailing gap\"-option, this"
            + "trailing gap is left out of the generated data -- it is just silence"
            + "anyhow. This is probably a good choice (almost) always.\n\n"
            + "When listening to music, higher sample rates, wider sample sizes,"
            + "and more channels sound better (in general). However, generating \"audio\" for"
            + "IR-LEDs is a completely different use case. The recommended settings are:"
            + "48000kHz, 8bit, 1 channel, omit trailing gap.";

    @SuppressWarnings("PackageVisibleField")
    public static String sendingGirsHelp = "TODO";

    @SuppressWarnings("PackageVisibleField")
    public static String sendingCommandFusionHelp = "With this pane, a CommandFusion Learner can be used for transmitting IR signals.";

    @SuppressWarnings("PackageVisibleField")
    public static String devLircHelp = "Using this pane, the Lirc /dev/lirc device can be used to send and capture IR signals. "
            + "This requires a so-called mode2 device connected, capable of sending.";

    private HelpTexts() {
    }
}
