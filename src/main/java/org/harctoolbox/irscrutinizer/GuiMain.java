/*
Copyright (C) 2013, 2014, 2015, 2017, 2018, 2019, 2021 Bengt Martensson.

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

import com.neuron.app.tonto.ProntoModel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import javax.comm.DriverGenUnix;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPopupMenu.Separator;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.TransferHandler;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.harctoolbox.analyze.Analyzer;
import org.harctoolbox.analyze.Burst;
import org.harctoolbox.analyze.Cleaner;
import org.harctoolbox.analyze.NoDecoderMatchException;
import org.harctoolbox.analyze.RepeatFinder;
import org.harctoolbox.devslashlirc.LircHardware;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.CommandSet;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.guicomponents.*;
import org.harctoolbox.guicomponents.HardwareBean.CannotCaptureException;
import org.harctoolbox.guicomponents.HardwareBean.CannotSendException;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.TimeoutException;
import org.harctoolbox.harchardware.comm.LocalSerialPort;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.Pronto;
import org.harctoolbox.ircore.ThisCannotHappenException;
import org.harctoolbox.irp.BitDirection;
import org.harctoolbox.irp.Decoder;
import org.harctoolbox.irp.ElementaryDecode;
import org.harctoolbox.irp.FiniteBitField;
import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.irp.IrpParseException;
import org.harctoolbox.irp.IrpUtils;
import org.harctoolbox.irp.Protocol;
import org.harctoolbox.irp.UnknownProtocolException;
import static org.harctoolbox.irscrutinizer.TestSignal.testSignal;
import org.harctoolbox.irscrutinizer.exporter.*;
import org.harctoolbox.irscrutinizer.importer.*;
import org.harctoolbox.remotelocator.Girrable;
import org.harctoolbox.remotelocator.NotFoundException;
import org.xml.sax.SAXException;

public final class GuiMain extends javax.swing.JFrame {

    private final static String ISSUES_URL = "https://github.com/bengtmartensson/IrScrutinizer/issues";
    private final static String DISCUSSIONS_URL = "https://github.com/bengtmartensson/IrScrutinizer/discussions";
    private final static String GIT_URL = "https://github.com/bengtmartensson/IrScrutinizer/";
    private final static String DOWNLOADS_URL = "https://github.com/bengtmartensson/IrScrutinizer/releases";
    private final static String UNIQUE_SEPARATOR = "#";

    // Stuff that may turn properties in the future
    private final static int importSequenceAskThreshold = 3;
    private final static int maxCharsInGuiMessages = 150;
    private final static int transmitSignalMouseButton = 2;
    public final static double chopThreshold = 100.0; // TODO should problably live somewhere else

    /**
     * Maximal endingTimeout that is accepted.
     */
    public final static int MAX_ENDING_TIMEOUT = 500;

    // Preferences for the analyzer. Possibly these should be made properties?
    private final static boolean analyzerEliminateConstantVars = false;
    private final static double analyzerMaxRoundingError = Burst.Preferences.DEFAULT_MAX_ROUNDING_ERROR;
    private final static double analyzerMaxUnits = Burst.Preferences.DEFAULT_MAX_UNITS;
    private final static double analyzerMaxMicroSeconds = Burst.Preferences.DEFAULT_MAX_MICROSECONDS;
    private final static String analyzerTimeBase = null;
    private final static boolean analyzerLsb = false;
    private final static boolean analyzerExtent = false;
    private final static List<Integer> analyzerParameterWidths = new ArrayList<>(0);
    private final static int analyzerMaxParameterWidth = FiniteBitField.MAXWIDTH;
    private final static boolean analyzerInvert = false;

    // ... and some more preferences for the decoding, that should probably be
    // properties too. (See IrpTransmogrifier for the semantics.)
    private final static boolean decodeStrict = false;
    private final static boolean decodeRecursive = false;
    private final static boolean decodeOverride = false;
    private final static boolean rejectLircCodeImports = true;

    // Locale used for transforming command names.
    // One day this may be made user selectable...
    private final static Locale namesLocale = Locale.getDefault();

    private static File libraryFileName(String appHome, String libraryName) {
        String subFolderName = (System.getProperty("os.name").startsWith("Windows")
                ? "Windows"
                : System.getProperty("os.name"))
                + '-' + System.getProperty("os.arch").toLowerCase(Locale.US);
        String stem = System.getProperty("harctoolbox.jniLibsHome") != null
                ? System.getProperty("harctoolbox.jniLibsHome")
                : appHome;
        String mappedName = System.mapLibraryName(libraryName);
        return new File(new File(stem, subFolderName), mappedName);
    }

    private static int csvNumberbaseIndex2numberbase(int index) {
        return (new int[] { 2, 8, 10, 16 })[index];
    }

    private Props properties;
    private final String applicationHome;
    private IrpDatabase irpDatabase;
    private Decoder decoder;
    private Decoder.DecoderParameters decoderParameters;
    private ExportFormatManager exportFormatManager;
    private HardwareManager hardwareManager;
    private Remote.MetaData metaData;
    private int dynamicExportFormatsMenuPosition;
    private TableUtils tableUtils;
    private CaptureThread captureThread = null;
    private LookAndFeelManager lookAndFeelManager;
    private GuiUtils guiUtils;
    private Component lastPane;
    private Component currentPane;
    private AboutPopup aboutBox;

    private ControlTowerIrDatabase controlTowerIrDatabase;
    private Map<String, String> controlTowerCodesetTable;
    private RemoteLocatorImporter remoteLocatorImporter;

    private CcfImporter ccfImporter;
    private XcfImporter xcfImporter;
    private CmlImporter cmlImporter;
    private CommandFusionImporter commandFusionImporter;
    private CsvRawImporter csvRawImporter;
    private CsvParametrizedImporter csvParametrizedImporter;
    private IctImporter ictImporter;
    private GirrImporter girrImporter;
    private LircImporter lircImporter;
    private IrTransImporter irTransImporter;
    private WaveImporter waveImporter;

    private RawIrSignal.RawTableColumnModel rawTableColumnModel;
    private ParametrizedIrSignal.ParameterIrSignalTableColumnModel parameterTableColumnModel;
    private RawIrSignal.RawTableModel rawTableModel;
    private ParametrizedIrSignal.ParameterIrSignalTableModel parameterTableModel;

    /**
     * Constructor for the GUI. Throws exceptions if configuration files cannot be found or on similar errors.
     * It may of course be questioned if this is necessary, however, it is faster for the user to
     * install the files than for me to write code for semi-working in semi-broken situations.
     *
     * @param applicationHome
     * @param propsfilename Name of properties file. Null for system default.
     * @param verbose Verbose execution of some commands, dependent on invoked programs.
     * @param arguments
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws org.harctoolbox.irp.IrpParseException
     */
    public GuiMain(String applicationHome, String propsfilename, boolean verbose, List<String> arguments) throws IOException, ParserConfigurationException, SAXException, IrpParseException {
        this.applicationHome = applicationHome;
        setupProperties(propsfilename, verbose);
        setupFrame();
        loadLibraries();
        setupGuiUtils();
        setupTables();
        setupIrpDatabase(); // must come before initComponents
        setupImporters();
        setupDecoder();
        loadExportFormats(); // must come before initComponents
        initComponents();
        if (!LircHardware.isLibraryLoaded())
            sendingHardwareTabbedPane.remove(devLircPanel);
        girrImporter.setIrpRendererBean(irpMasterBean);
        tweakTables();
        tweakFrame();
        setupRepeatFinder();
        setupHardware();
        setupRendering();
        initializePlot();
        updateOutputFormat(properties.getOutputFormatIndex());
        setupLAF();
        selectFormat();
        dummyLoadTontoLibrary();
        setupConsole(); // redefines stdout/stderr
        guiUtils.setUsePopupsForErrors(properties.getUsePopupsForErrors());
        processArguments(arguments);
    }

    private void setupProperties(String propsfilename, boolean verbose) throws MalformedURLException {
        properties = new Props(propsfilename, this.applicationHome);
        if (verbose)
            properties.setVerbose(true);
        Importer.setProperties(properties);
        HelpPopup.setBaseUrl(new URL("file", null, properties.mkPathAbsolute(properties.getProtocolDocfilePath())).toString());
        SelectFile.restoreFromString(properties.getFileselectordirs());
        Exporter.setCreatingUser(properties.getCreatingUser());
        Exporter.setEncoding(properties.getExportCharsetName());
    }

    private void setupFrame() {
        String title = Version.versionString;
        if (System.getenv("APPIMAGE") != null)
            title += " AppImage";
        super.setTitle(title);

        Rectangle bounds = properties.getBounds();
        if (bounds != null)
            super.setBounds(bounds);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                cleanupForShutdown();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                boolean leave = checkUnsavedStuff();
                if (leave) {
                    killRunningCapture();
                    System.exit(IrpUtils.EXIT_SUCCESS);
                }
            }
        });
        super.setIconImage((new ImageIcon(GuiMain.class.getResource("/icons/Crystal-Clear/64x64/apps/babelfish.png"))).getImage());
    }

    private void loadLibraries() throws IOException {
        System.setProperty("harctoolbox.jniLibsHome", applicationHome);
        // First try to load library from absolute path,
        try {
            LircHardware.loadLibrary(libraryFileName(applicationHome, LircHardware.libraryName));
        } catch (UnsatisfiedLinkError ex) {
        }

        // ... then try system path.
        try {
            LircHardware.loadLibrary();
        } catch (UnsatisfiedLinkError e) {
        }

        LocalSerialPort.setLibraryDir(org.harctoolbox.harchardware.Utils.libraryDir(applicationHome));
    }

    private void setupGuiUtils() {
        guiUtils = new GuiUtils(this);
        guiUtils.setProgramName(Version.appName);
        guiUtils.setMaxGuiMessageLength(maxCharsInGuiMessages);
        guiUtils.setUsePopupsForErrors(false); // during initialization, popup errors might not work
        guiUtils.setUsePopupsForHelp(properties.getUsePopupsForHelp());
        guiUtils.setOfferStackTrace(properties.getOfferStackTrace());
        guiUtils.setVerbose(properties.getVerbose());
    }

    private void setupTables() {
        parameterTableModel = new ParametrizedIrSignal.ParameterIrSignalTableModel();
        rawTableModel = new RawIrSignal.RawTableModel();
        parameterTableColumnModel = new ParametrizedIrSignal.ParameterIrSignalTableColumnModel();
        rawTableColumnModel = new RawIrSignal.RawTableColumnModel();
        tableUtils = new TableUtils(guiUtils);
    }

    private void setupIrpDatabase() throws IOException, IrpParseException, SAXException {
        List<File> configFiles = new ArrayList<>(4);
        configFiles.add(new File(properties.mkPathAbsolute(properties.getIrpProtocolsPath())));
        String secondary = properties.getSecondaryIrpProtocolsPath();
        if (!secondary.isEmpty())
            configFiles.add(new File(secondary));

        irpDatabase = new IrpDatabase(configFiles);
        Command.setIrpDatabase(irpDatabase);
    }

    private void setupImporters() throws MalformedURLException, IrpParseException {
        setupLircImporter();
        setupIrTransImporter();
        setupCmlImporter();
        setupCommandFusionImporter();
        setupProntoClassicImporter();
        setupXcfImporter();
        setupRawCsvImporter();
        setupParametrizedCsvImporter();
        setupGirrImporter();
        setupWaveImporter();
        setupIctImporter();
    }

    private void setupLircImporter() {
        lircImporter = new LircImporter();
        lircImporter.setRejectLircCode(rejectLircCodeImports);
    }

    private void setupIrTransImporter() {
        irTransImporter = new IrTransImporter();
    }

    private void setupCmlImporter() {
        cmlImporter = new CmlImporter();
    }

    private void setupCommandFusionImporter() {
        commandFusionImporter = new CommandFusionImporter();
    }

    private void setupProntoClassicImporter() {
        ccfImporter = new CcfImporter();
        ccfImporter.setTranslateProntoFont(properties.getTranslateProntoFont());
    }

    private void setupXcfImporter() {
        xcfImporter = new XcfImporter();
        xcfImporter.setTranslateProntoFont(properties.getTranslateProntoFont());
        properties.addTranslateProntoFontChangeListener((String name1, Object oldValue, Object newValue) -> {
            ccfImporter.setTranslateProntoFont((Boolean) newValue);
            xcfImporter.setTranslateProntoFont((Boolean) newValue);
        });
    }

    private void setupRawCsvImporter() {
        csvRawImporter = new CsvRawImporter(properties.getRawSeparatorIndex(), properties.getRawNameColumn(),
                properties.getRawNameMultiColumn(), properties.getCodeColumn(),
                properties.getIncludeTail());

        properties.addRawSeparatorIndexChangeListener((String name1, Object oldValue, Object newValue) -> {
            csvRawImporter.setSeparatorIndex((Integer)newValue);
        });
        properties.addRawNameColumnChangeListener((String name1, Object oldValue, Object newValue) -> {
            csvRawImporter.setNameColumn((Integer)newValue);
        });
        properties.addRawNameMultiColumnChangeListener((String name1, Object oldValue, Object newValue) -> {
            csvRawImporter.setNameMultiColumn((Boolean)newValue);
        });
        properties.addCodeColumnChangeListener((String name1, Object oldValue, Object newValue) -> {
            csvRawImporter.setCodeColumn((Integer)newValue);
        });
        properties.addIncludeTailChangeListener((String name1, Object oldValue, Object newValue) -> {
            csvRawImporter.setIncludeTail((Boolean)newValue);
        });
    }

    private void setupParametrizedCsvImporter() {
        csvParametrizedImporter = new CsvParametrizedImporter(
                CsvImporter.getSeparator(properties.getParametricSeparatorIndex()),
                properties.getParametricNameColumn(),
                properties.getParametrizedNameMultiColumn(),
                properties.getVerbose(),
                csvNumberbaseIndex2numberbase(properties.getParametricNumberBaseIndex()),
                properties.getFColumn(),
                properties.getDColumn(),
                properties.getSColumn(),
                properties.getProtocolColumn(),
                properties.getMiscParametersColumn());

        properties.addParametricSeparatorIndexChangeListener((String name1, Object oldValue, Object newValue) -> {
            csvParametrizedImporter.setSeparatorIndex((Integer) newValue);
        });

        properties.addParametricNameColumnChangeListener((String name1, Object oldValue, Object newValue) -> {
            csvParametrizedImporter.setNameColumn((Integer) newValue);
        });

        properties.addParametricNumberBaseIndexChangeListener((String name1, Object oldValue, Object newValue) -> {
            csvParametrizedImporter.setNumberBase(csvNumberbaseIndex2numberbase((Integer) newValue));
        });

        properties.addFColumnChangeListener((String name1, Object oldValue, Object newValue) -> {
            csvParametrizedImporter.setFColumn((Integer) newValue);
        });

        properties.addDColumnChangeListener((String name1, Object oldValue, Object newValue) -> {
            csvParametrizedImporter.setDColumn((Integer) newValue);
        });

        properties.addSColumnChangeListener((String name1, Object oldValue, Object newValue) -> {
            csvParametrizedImporter.setSColumn((Integer) newValue);
        });

        properties.addProtocolColumnChangeListener((String name1, Object oldValue, Object newValue) -> {
            csvParametrizedImporter.setProtocolColumn((Integer) newValue);
        });

        properties.addParametrizedNameMultiColumnChangeListener((String name1, Object oldValue, Object newValue) -> {
            csvParametrizedImporter.setNameMultiColumn((Boolean) newValue);
        });
    }

    private void setupIctImporter() {
        ictImporter = new IctImporter();
        ictImporter.setChop(properties.getChopIctImports());
    }

    private void setupGirrImporter() throws MalformedURLException {
        Command.setAcceptEmptyCommands(properties.getAllowEmptyGirrCommands());
        girrImporter = new GirrImporter(properties.getGirrValidate(), new URL(properties.getGirrSchemaLocation()), irpDatabase);
        properties.addGirrSchemaLocationChangeListener((String name1, Object oldValue, Object newValue) -> {
            try {
                girrImporter.setUrl(new URL((String)newValue));
            } catch (MalformedURLException ex) {
                guiUtils.error(ex);
            }
        });

        properties.addGirrValidateChangeListener((String name1, Object oldValue, Object newValue) -> {
            girrImporter.setValidate((Boolean) newValue);
        });
    }

    private void setupWaveImporter() {
        waveImporter = new WaveImporter(properties.getImportWaveDivideCarrier());
        properties.addImportWaveDivideCarrierChangeListener((String name1, Object oldValue, Object newValue) -> {
            waveImporter.setDivideCarrier((Boolean) newValue);
        });
    }

    private void loadExportFormats() throws ParserConfigurationException, SAXException, IOException {
        exportFormatManager = new ExportFormatManager((String name1) -> {
            selectFormat(name1);
        });
        exportFormatManager.add("Girr", () -> newGirrExporter());
        exportFormatManager.add("Wave", () -> exportAudioParametersBean.newWaveExporter());
        exportFormatManager.add("Text", () -> newTextExporter());
        exportFormatManager.add("ProntoClassic", () -> newProntoClassicExporter());

        addDynamicExportFormats(properties.getExportFormatFilePath());
        addDynamicExportFormats(properties.getSecondaryExportFormatFilePath());
    }

    private void addDynamicExportFormats(String file) throws ParserConfigurationException, SAXException, IOException {
        if (!file.isEmpty())
            exportFormatManager.addDynamicFormats(guiUtils, new File(properties.mkPathAbsolute(file)));
    }

    private void setupDecoder() throws IrpParseException {
        RepeatFinder.setDefaultAbsoluteTolerance(properties.getAbsoluteTolerance());
        RepeatFinder.setDefaultRelativeTolerance(properties.getRelativeTolerance());

        decoder = new Decoder(irpDatabase);
        decoderParameters = new Decoder.DecoderParameters(decodeStrict,
                properties.getPrintAlternativeDecodes(),
                properties.getRemoveDefaultedParameters(),
                decodeRecursive,
                properties.getFrequencyTolerance(),
                properties.getAbsoluteTolerance(),
                properties.getRelativeTolerance(),
                properties.getMinLeadOut(),
                decodeOverride,
                properties.getIgnoreLeadingGarbage()
        );
        Command.setDecoderParameters(decoderParameters);
        RawIrSignal.setDecoderParameters(decoderParameters);
        ParametrizedIrSignal.setDecoderParameters(decoderParameters);
        properties.addFrequencyToleranceChangeListener((String name1, Object oldValue, Object newValue) -> {
            decoderParameters.setFrequencyTolerance((Double) newValue);
        });
        properties.addAbsoluteToleranceChangeListener((String name1, Object oldValue, Object newValue) -> {
            decoderParameters.setAbsoluteTolerance((Double) newValue);
        });
        properties.addRelativeToleranceChangeListener((String name1, Object oldValue, Object newValue) -> {
            decoderParameters.setRelativeTolerance((Double) newValue);
        });
        properties.addMinLeadOutChangeListener((String name1, Object oldValue, Object newValue) -> {
            decoderParameters.setMinimumLeadout((Double) newValue);
        });
        properties.addRemoveDefaultedParametersChangeListener((String name1, Object oldValue, Object newValue) -> {
            decoderParameters.setRemoveDefaultedParameters((Boolean) newValue);
        });
        properties.addPrintAlternativeDecodesChangeListener((String name1, Object oldValue, Object newValue) -> {
            decoderParameters.setAllDecodes((Boolean) newValue);
        });
    }

    private void tweakTables() {
        tableUtils.fixKeyMappings(parameterTable);
        tableUtils.fixKeyMappings(rawTable);
        enableSorter(rawTable, properties.getSorterOnRawTable());
        enableSorter(parameterTable, properties.getSorterOnParametrizedTable());

        // Scroll the tables to the last line
        parameterTable.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (parameterTableModel.getAndResetScrollRequest())
                    parameterTable.scrollRectToVisible(parameterTable.getCellRect(parameterTable.getRowCount() - 1, 0, true));
            }
        });

        rawTable.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (rawTableModel.getAndResetScrollRequest())
                    rawTable.scrollRectToVisible(parameterTable.getCellRect(parameterTable.getRowCount() - 1, 0, true));
            }
        });

        ParametrizedIrSignal.setDecoder(decoder);

        RawIrSignal.setDecoder(decoder);
        RawIrSignal.setInvokeAnalyzer(properties.getInvokeAnalyzer());
        RawIrSignal.setInvokeDecoder(properties.getInvokeDecoder());
        RawIrSignal.setAbsoluteTolerance(properties.getAbsoluteTolerance());
        RawIrSignal.setRelativeTolerance(properties.getRelativeTolerance());

        properties.addInvokeAnalyzerChangeListener((String name1, Object oldValue, Object newValue) -> {
            RawIrSignal.setInvokeAnalyzer((Boolean) newValue);
        });
        properties.addInvokeDecoderChangeListener((String name1, Object oldValue, Object newValue) -> {
            RawIrSignal.setInvokeDecoder((Boolean) newValue);
        });
        properties.addAbsoluteToleranceChangeListener((String name1, Object oldValue, Object newValue) -> {
            RawIrSignal.setAbsoluteTolerance((Double) newValue);
            RepeatFinder.setDefaultAbsoluteTolerance((Double) newValue);
        });
        properties.addRelativeToleranceChangeListener((String name1, Object oldValue, Object newValue) -> {
            RawIrSignal.setRelativeTolerance((Double) newValue);
            RepeatFinder.setDefaultRelativeTolerance((Double) newValue);
        });

    }

    private void tweakFrame() {
        currentPane = topLevelTabbedPane.getSelectedComponent();
        cookedPanel.setTransferHandler(new GirrImporterBeanTransferHandler(false));
        rawPanel.setTransferHandler(new GirrImporterBeanTransferHandler(true));

        capturedDataTextArea.setTransferHandler(new SignalScrutinizerTransferHandler(capturedDataTextArea.getTransferHandler()));

        // Cannot do this in initComponents, since then it will be called therein
        importTabbedPane.addChangeListener((javax.swing.event.ChangeEvent evt) -> {
            int index = importTabbedPane.getSelectedIndex();
            properties.setImportPaneSelectedIndex(index);
        });
        parametrizedRawTabbedPane.addChangeListener((javax.swing.event.ChangeEvent evt) -> {
            int index = parametrizedRawTabbedPane.getSelectedIndex();
            properties.setParametrizedRawTabbedPaneSelectedIndex(index);
        });
        dynamicExportFormatsMenuPosition = optionsMenu.getItemCount();
        optionsMenu.add(exportFormatManager.getMenu(properties.getExportFormatName()));
        optionsMenu.add(new Separator());
    }

    private void setupRepeatFinder() {
        RepeatFinder.setDefaultAbsoluteTolerance(properties.getAbsoluteTolerance());
        RepeatFinder.setDefaultRelativeTolerance(properties.getRelativeTolerance());
        // propertychangelistener?
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private void setupHardware() throws IOException {
        setupHardwareManager();
        setupGlobalCache(); // Keep the same order as they appear in the pane
        setupDevLirc();
        setupIrAudioPort();
        setupGirsClient();
        setupCommandFusion();
        setupIrWidget();

        try {
            hardwareManager.select(properties.getSelectedHardware());
        } catch (HardwareUnavailableException ex) {
            // FIXME
            String replacement = hardwareManager.selectFallback();
            System.err.println("Selected hardware " + properties.getSelectedHardware() + " not found, falling back to " + replacement);
        }
        hardwareManager.setVerbose(properties.getVerbose());
        optionsMenu.add(hardwareManager.getMenu());
    }

    private void setupHardwareManager() {
        hardwareManager = new HardwareManager(guiUtils);
        properties.addVerboseChangeListener((String name1, Object oldValue, Object newValue) -> {
            hardwareManager.setVerbose((Boolean)newValue);
            guiUtils.setVerbose((Boolean)newValue);
        });
        hardwareManager.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            boolean canSend;
            switch (evt.getPropertyName()) {
                case HardwareManager.PROP_SELECTED_HARDWARE:
                    String name = (String) evt.getNewValue();
                    properties.setSelectedHardware(name);
                    Container bean = hardwareManager.getBean(name).getParent();
                    sendingHardwareTabbedPane.setSelectedComponent(bean);
                    captureTestButton.setEnabled(hardwareManager.canCapture());
                    canSend = hardwareManager.canSend();
                    transmitScrutinizedButton.setEnabled(canSend);
                    transmitGenerateButton2.setEnabled(canSend);
                    break;
                case HardwareBean.PROP_ISOPEN:
                    captureTestButton.setEnabled(hardwareManager.canCapture());
                    canSend = hardwareManager.canSend();
                    transmitScrutinizedButton.setEnabled(canSend);
                    transmitGenerateButton2.setEnabled(canSend);
                    break;
                default:
                    // throw new ThisCannotHappenException("Unhandled property: " + evt.getPropertyName());
            }
        });

//        properties.addCaptureBeginTimeoutChangeListener((String name1, Object oldValue, Object newValue) -> {
//            try {
//                capturingHardwareManager.getCapturer().setBeginTimeout((Integer) newValue);
//            } catch (IOException | HarcHardwareException ex) {
//                guiUtils.error(ex);
//            }
//        });
//
//        properties.addCaptureMaxSizeChangeListener((String name1, Object oldValue, Object newValue) -> {
//            try {
//                capturingHardwareManager.getCapturer().setCaptureMaxSize((Integer) newValue);
//            } catch (IOException | HarcHardwareException ex) {
//                guiUtils.error(ex);
//            }
//        });
//
//        properties.addCaptureEndingTimeoutChangeListener((String name1, Object oldValue, Object newValue) -> {
//            try {
//                capturingHardwareManager.getCapturer().setEndingTimeout((Integer) newValue);
//            } catch (IOException | HarcHardwareException ex) {
//                guiUtils.error(ex);
//            }
//        });
    }

    private void setupGlobalCache() throws IOException {
        // globalCacheIrSenderSelector was constructed in initComponents
        globalCacheIrSenderSelector.setGlobalCache(properties.getGlobalCacheIpName());
        globalCacheIrSenderSelector.setModule(properties.getGlobalCacheModule());
        globalCacheIrSenderSelector.setPort(properties.getGlobalCachePort());
        globalCacheIrSenderSelector.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            switch (evt.getPropertyName()) {
                case HardwareBean.PROP_IPNAME:
                    properties.setGlobalCacheIpName((String) evt.getNewValue());
                    break;
                case HardwareBean.PROP_MODULE:
                    properties.setGlobalCacheModule((Integer) evt.getNewValue());
                    break;
                case HardwareBean.PROP_PORT:
                    properties.setGlobalCachePort((Integer) evt.getNewValue());
                    break;
                case HardwareBean.PROP_ISOPEN:
                    //guiUtils.message("PROP_ISOPEN received, now " + ((Boolean) evt.getNewValue() ? "open" : "closed"));
                    break;
                default:
                    throw new ThisCannotHappenException("Unhandled property: " + evt.getPropertyName());
            }
        });
        hardwareManager.add(globalCacheIrSenderSelector);
    }

    private void setupIrAudioPort() {
        // transmitAudioParametersBean was constructed and setup in initComponents()
        transmitAudioBean.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            switch (evt.getPropertyName()) {
                case AudioTransmitterBean.PROP_SAMPLEFREQUENCY:
                    properties.setWaveSampleFrequency((Integer) evt.getNewValue());
                    break;
                case AudioTransmitterBean.PROP_NOCHANNELS:
                    properties.setWaveChannels((Integer) evt.getNewValue());
                    break;
                case AudioTransmitterBean.PROP_SAMPLESIZE:
                    properties.setWaveSampleSize((Integer) evt.getNewValue());
                    break;
                case AudioTransmitterBean.PROP_OMITTRAILINGGAP:
                    properties.setWaveOmitTrailingGap((Boolean) evt.getNewValue());
                    break;
                default:
                    throw new ThisCannotHappenException("Unhandled property: " + evt.getPropertyName());
            }
        });
        hardwareManager.add(transmitAudioBean);
    }

    private void setupDevLirc() {
        if (!LircHardware.isLibraryLoaded())
            return;

        // devLircBean was constructed and setup in initComponents()
        devLircBean.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            switch (evt.getPropertyName()) {
                case HardwareBean.PROP_PORTNAME:
                    properties.setDevLircName((String) evt.getNewValue());
                    break;
                case HardwareBean.PROP_PROPS:
                    break;
                case HardwareBean.PROP_ISOPEN:
                    guiUtils.message("PROP_ISOPEN received, now " + ((Boolean) evt.getNewValue() ? "open" : "closed"));
                    break;
                default:
                    throw new ThisCannotHappenException("Unhandled property: " + evt.getPropertyName());
            }
        });
        hardwareManager.add(devLircBean);
    }

    private void setupGirsClient() {
        // girsTcpSerialComboBean was constructed in initComponents()
        girsTcpSerialComboBean.setTimeout(properties.getGirsClientTimeout());
        girsTcpSerialComboBean.setPortName(properties.getGirsClientSerialPortName());
        girsTcpSerialComboBean.setBaud(properties.getGirsClientSerialPortBaudRate());
        girsTcpSerialComboBean.setIpName(properties.getGirsClientIPName());
        girsTcpSerialComboBean.setPort(properties.getGirsClientPortNumber());
        girsTcpSerialComboBean.setType(properties.getGirsClientType());

        girsTcpSerialComboBean.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            switch (evt.getPropertyName()) {

                case HardwareBean.PROP_BAUD:
                    properties.setGirsClientSerialPortBaudRate((int) evt.getNewValue());
                    break;
                case HardwareBean.PROP_PORTNAME:
                    properties.setGirsClientSerialPortName((String) evt.getNewValue());
                    break;
                case HardwareBean.PROP_IPNAME:
                    properties.setGirsClientIPName((String) evt.getNewValue());
                    break;
                case HardwareBean.PROP_PORT:
                    properties.setGirsClientPortNumber((int) evt.getNewValue());
                    break;
                case HardwareBean.PROP_TYPE:
                    properties.setGirsClientType(evt.getNewValue().toString());
                    break;
                case HardwareBean.PROP_VERSION:
                case HardwareBean.PROP_MODULE:
                case HardwareBean.PROP_PROPS:
                case HardwareBean.PROP_ISOPEN:
                    break;
                default:
                    throw new ThisCannotHappenException("Unhandled property: " + evt.getPropertyName());
            }
        });
        hardwareManager.add(girsTcpSerialComboBean);
    }

    private void setupCommandFusion() {
        // commandFusionBean was constructed and setup in initComponents()
        commandFusionBean.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            switch (evt.getPropertyName()) {

                case HardwareBean.PROP_PORTNAME:
                    properties.setCommandFusionPortName((String) evt.getNewValue());
                    break;
                case HardwareBean.PROP_ISOPEN:
                    guiUtils.message("PROP_ISOPEN received, now " + ((Boolean) evt.getNewValue() ? "open" : "closed"));
                    break;
                default:
                    throw new ThisCannotHappenException("Unhandled property: " + evt.getPropertyName());
            }
        });

        hardwareManager.add(commandFusionBean);
    }

    private void setupIrWidget() {
        //irWidgetBean was constructed and setup in initComponents()
        irWidgetBean.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            switch (evt.getPropertyName()) {

                case HardwareBean.PROP_PORTNAME:
                    properties.setIrWidgetPortName((String) evt.getNewValue());
                    break;
                case HardwareBean.PROP_ISOPEN:
                    break;
                case HardwareBean.PROP_LOWER_DTR_RTS:
                    properties.setIrWidgetLowerDtrRts((Boolean) evt.getNewValue());
                    break;
                default:
                    throw new ThisCannotHappenException("Unhandled property: " + evt.getPropertyName());
            }
        });

        properties.addCaptureBeginTimeoutChangeListener((String name1, Object oldValue, Object newValue) -> {
            irWidgetBean.setBeginTimeout((Integer)newValue);
        });

        properties.addCaptureMaxSizeChangeListener((String name1, Object oldValue, Object newValue) -> {
            irWidgetBean.setCaptureMaxSize((Integer)newValue);
        });

        properties.addCaptureEndingTimeoutChangeListener((String name1, Object oldValue, Object newValue) -> {
            irWidgetBean.setEndingTimeout((Integer)newValue);
        });

        hardwareManager.add(irWidgetBean);
    }

    private void setupRendering() {
        irpMasterBean.addPropertyChangeListener((java.beans.PropertyChangeEvent evt) -> {
            switch (evt.getPropertyName()) {
                case IrpRenderBean.PROP_PROTOCOL_NAME:
                    properties.setIrpMasterCurrentProtocol((String) evt.getNewValue());
                    break;
                case IrpRenderBean.PROP_D:
                    properties.setIrpMasterCurrentD((String) evt.getNewValue());
                    break;
                case IrpRenderBean.PROP_S:
                    properties.setIrpMasterCurrentS((String) evt.getNewValue());
                    break;
                case IrpRenderBean.PROP_F:
                    properties.setIrpMasterCurrentF((String) evt.getNewValue());
                    break;
                case IrpRenderBean.PROP_T:
                    properties.setIrpMasterCurrentT((String) evt.getNewValue());
                    break;
                case IrpRenderBean.PROP_ADDITIONAL_PARAMS:
                    properties.setIrpMasterCurrentAdditionalParameters((String) evt.getNewValue());
                    break;
                default:
                    guiUtils.error("Programming error detected: " + evt.getPropertyName());
                    break;
            }
        });
    }

    private void setupLAF() {
        LookAndFeelManager.ILookAndFeelManagerCaller caller = new ScrutinizeIrCaller();
        lookAndFeelManager = new LookAndFeelManager(this, lafMenu, caller);
        lookAndFeelManager.setLAF(properties.getLookAndFeel());
        lookAndFeelManager.updateLAF();
        lafMenu.setVisible(true);
    }

    // When the Tonto stuff loads, it tries to load a library called jnijcomm,
    // see javax.comm.DriverGenUnix. This will fail, but that is no concern for us.
    // However, it writes an ugly stacktrace on stderr, which is scaring the user.
    // Therefore, redirect stderr to nirvana, and make that call now.
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    private void dummyLoadTontoLibrary() {
        PrintStream nullPrintStream = new PrintStream(new ByteArrayOutputStream());
        System.setErr(nullPrintStream);
        new DriverGenUnix();
    }

    private void setupConsole() {
        console.setErrorFunction(
                new org.harctoolbox.guicomponents.Console.IErrorFunction() {
            @Override
            public void err(Exception ex, String message) {
                if (ex == null)
                    guiUtils.error(message);
                else
                    guiUtils.error(ex, message);
            }

            @Override
            public void err(String str) {
                guiUtils.error(str);
            }
        });

        console.setStdErr();
        console.setStdOut();
    }

    private void processArguments(List<String> arguments) {
        int sum = 0;
        for (String str : arguments) {
            try {
                sum += importGirr(new File(str), false);
            } catch (FileNotFoundException ex) {
                guiUtils.error("File \"" + str + "\" not found, ignoring.");
            } catch (IOException | ParseException | InvalidArgumentException ex) {
                guiUtils.error(ex);
            }
        }
        if (sum > 0) {
            selectImportPane(ImportType.parametricRemote);
            parameterTableModel.clearUnsavedChanges();
        }
    }

    private boolean killRunningCapture() {
        if (captureThreadRunning()) {
            captureThread.terminate();
            try {
                if (captureThread != null)
                    captureThread.join(1000L);
            } catch (InterruptedException ex) {
            }
            return true;
        }
        return false;
    }

    private boolean captureThreadRunning() {
        return captureThread != null && captureThread.isAlive();
    }

    private boolean checkUnsavedStuff() {
        AckWithMemoryDialog.PropertyFlip parameterFlip = new AckWithMemoryDialog.PropertyFlip() {
            @Override
            public boolean getProperty() {
                return properties.getDontInquire4UnsavedParametricRemotes();
            }

            @Override
            public void setProperty(boolean value) {
                properties.setDontInquire4UnsavedParametricRemotes(value);
            }
        };

        AckWithMemoryDialog.PropertyFlip rawFlip = new AckWithMemoryDialog.PropertyFlip() {
            @Override
            public boolean getProperty() {
                return properties.getDontInquire4UnsavedRawRemotes();
            }

            @Override
            public void setProperty(boolean value) {
                properties.setDontInquire4UnsavedRawRemotes(value);
            }
        };

        boolean exitOk = !parameterTableModel.hasUnsavedChanges()
                || AckWithMemoryDialog.ackWithMemoryDialog("There is unsaved data in \"Parametetric Remote\".", "Really exit?", parameterFlip, this);
        exitOk = exitOk && (!rawTableModel.hasUnsavedChanges()
                || AckWithMemoryDialog.ackWithMemoryDialog("There is unsaved data in \"Raw Remote\".", "Really exit?", rawFlip, this));

        return exitOk;
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private void cleanupForShutdown() {
        try {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, "US-ASCII"));
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, "US-ASCII"));
        } catch (UnsupportedEncodingException ex) {
            throw new InternalError("This cannot happen");
        }
//        capturingHardwareManager.close();
        hardwareManager.close();
        properties.setFileselectordirs(SelectFile.saveString());
        try {
            if (!properties.getWasReset())
                properties.setBounds(getBounds());
            properties.save();
        } catch (IOException e) {
            System.err.println("Problems saving properties; " + e.getMessage());
        }
    }

    private int importGirr(File file, boolean raw) throws ParseException, InvalidArgumentException, IOException {
        return importCommands(girrImporter.getRemoteSet(file), girrImporter.getMetaData(), raw);
    }

//    private void selectSenderHardware(HardwareBean panel) {
//        lastPane = topLevelTabbedPane.getSelectedComponent();
//        topLevelTabbedPane.setSelectedComponent(sendingPanel);
//        sendingHardwareTabbedPane.setSelectedComponent(panel);
//    }

    private Command.CommandTextFormat[] setupExtraTextFormats() {
        List<Command.CommandTextFormat> formats = new ArrayList<>(8);
        if (properties.getExportGenerateShortCcf())
            formats.add(new ShortCcfFormatter());
        if (properties.getExportGenerateSendIr())
            formats.add(new SendIrFormatter());
        if (properties.getExportGenerateBroadlinkHex())
            formats.add(new BroadlinkHexFormatter());
        if (properties.getExportGenerateBroadlinkBase64())
            formats.add(new BroadlinkBase64Formatter());
        return formats.toArray(new Command.CommandTextFormat[0]);
    }

    private GirrExporter newGirrExporter() {
        return new GirrExporter(properties.getGirrFatRaw(),
                properties.getExportGenerateRaw(),
                properties.getExportGenerateCcf(),
                properties.getExportGenerateParameters(),
                setupExtraTextFormats());
    }

    private TextExporter newTextExporter() {
        return new TextExporter(properties.getExportGenerateRaw(),
                properties.getExportGenerateCcf(),
                properties.getExportGenerateParameters(),
                setupExtraTextFormats());
    }

    private ProntoClassicExporter newProntoClassicExporter() {
        ProntoModel prontomodel = ProntoModel.getModelByName((String) prontoModelComboBox.getSelectedItem());
        int buttonwidth = Integer.parseInt(prontoExportButtonWidthTextField.getText());
        int buttonheight = Integer.parseInt(prontoExportButtonHeightTextField.getText());
        int screenwidth = Integer.parseInt(prontoExportScreenWidthTextField.getText());
        int screenheight = Integer.parseInt(prontoExportScreenHeightTextField.getText());
        return new ProntoClassicExporter(/*new File(properties.getExportDir()),*/ prontomodel,
                buttonwidth, buttonheight, screenwidth, screenheight);
    }

    private Exporter newExporter() {
        return newExporter((String) exportFormatComboBox.getSelectedItem());
    }

    private Exporter newExporter(String formatName) {
        return exportFormatManager.get(formatName).newExporter();
    }

    private IrSignal getCapturedIrSignal() throws InvalidArgumentException {
        String str = capturedDataTextArea.getText();
        if (str.trim().isEmpty())
            return null;

        return InterpretString.interpretString(str, null, properties.getDummyGap(),
                properties.getInvokeRepeatFinder(), properties.getInvokeCleaner(),
                properties.getAbsoluteTolerance(), properties.getRelativeTolerance(), properties.getMinRepeatLastGap());
    }

    private void clearSignal() {
        capturedDataTextArea.setText(null);
        irPlotter.clear();
        decodeIRTextField.setText(null);
        analyzerTextField.setText(null);
        frequencyLabel.setText(null);
        introLengthLabel.setText(null);
        repLengthLabel.setText(null);
        noRepsLabel.setText(null);
        endingLengthLabel.setText(null);
    }

    private ModulatedIrSequence captureIrSequence() throws IOException, HarcHardwareException, InvalidArgumentException, HardwareUnavailableException, CannotCaptureException {
        try {
            return hardwareManager.capture();
        } catch (TimeoutException ex) {
            return null;
        }
    }

    private void initializePlot() {
        irPlotter.setIgnoreLast(properties.getIgnoreEndingSilence());
    }

    private void clonePlot() {
        if (irPlotter.isEmpty()) {
            guiUtils.error("No plot to clone.");
            return;
        }
        IrPlotter dolly = new IrPlotter(irPlotter, false);
        dolly.setPreferredSize(irPlotter.getSize());
        HarcletFrame.newHarcletFrame(this, dolly, false, this.lookAndFeelManager.getCurrentLAFClassName());
    }

    private void setRepeatParameters(int introLength, int repeatLength, int nRepetitions, int endingLength) {
        introLengthLabel.setText(Integer.toString(introLength));
        repLengthLabel.setText(Integer.toString(repeatLength));
        endingLengthLabel.setText(Integer.toString(endingLength));
        noRepsLabel.setText(Integer.toString(nRepetitions));
    }

    private void setRepeatParameters(RepeatFinder repeatFinder) {
        setRepeatParameters(repeatFinder.getRepeatFinderData().getBeginLength(),
                repeatFinder.getRepeatFinderData().getRepeatLength(),
                repeatFinder.getRepeatFinderData().getNumberRepeats(),
                repeatFinder.getRepeatFinderData().getEndingLength());
    }

    private void setFrequencyParameter(Double frequency) {
        frequencyLabel.setText(frequency != null ? Long.toString(Math.round(frequency)) : "?");
    }

    private void setFrequencyParameter(IrSignal irSignal) {
        setFrequencyParameter(irSignal.getFrequency());
    }

    private String formatIrSignal(IrSignal irSignal, int formatIndex) {
        return OutputTextFormat.newOutputTextFormat(formatIndex).formatIrSignal(irSignal);
    }

    private String formatIrSignal(IrSignal irSignal) {
        return formatIrSignal(irSignal, properties.getOutputFormatIndex());
    }

    private void setCaptureWindow(IrSignal irSignal) {
        try {
            capturedDataTextArea.setText(formatIrSignal(irSignal, properties.getOutputFormatIndex()));
        } catch (Exception ex) {
            guiUtils.error(ex);
            capturedDataTextArea.setText(null);
        }
    }

    private void setDecodeIrParameters(IrSignal irSignal) {
        Decoder.AbstractDecodesCollection<? extends ElementaryDecode> decodes = decoder.decodeLoose(irSignal, decoderParameters);
        setDecodeResult(decodes);
    }

//    private Collection<Decoder.Decode> decodeIrSequence(ModulatedIrSequence irSequence) {
//        Decoder.DecodeTree decodeTree = decoder.decode(irSequence, decoderParameters);
//        List<Decoder.Decode> decodes = new ArrayList<>(4);
//        for (Decoder.TrunkDecodeTree decode : decodeTree)
//            decodes.add(decode.getTrunk());
//        return decodes;
//    }

//    private Iterable<Decoder.Decode> decodeIrSignal(IrSignal irSignal) {
//        Decoder.SimpleDecodesSet decodes = decoder.decodeIrSignal(irSignal, decoderParameters);
//        return decodes;
//    }

    private void setDecodeResult(Decoder.AbstractDecodesCollection<? extends ElementaryDecode> decodes) {
        if (decodes.isEmpty()) {
            decodeIRTextField.setText("");
            return;
        }

        StringBuilder decodeString = new StringBuilder(40);
        ElementaryDecode preferedDecode = decodes.getPreferred();
        decodeString.append(preferedDecode);

        if (decodes.size() == 2)
            decodeString.append(" + one more decode");
        else if (decodes.size() > 2)
            decodeString.append(" + ").append(Integer.toString(decodes.size() - 1)).append(" more decodes");

        decodeIRTextField.setText(decodeString.toString());
        if (properties.getPrintDecodesToConsole())
            guiUtils.message(preferedDecode.toString());

        if (properties.getPrintAlternativeDecodes())
            for (ElementaryDecode decode : decodes)
                if (decode != preferedDecode)
                    guiUtils.message("Alternative decode: " + decode.toString());
    }

    private void clearAnalyzeParameters() {
        analyzerTextField.setText(null);
    }

    private void setAnalyzeParameters(String line) {
        analyzerTextField.setText(line);
    }

    private void setAnalyzeParameters(Protocol protocol) {
        if (protocol == null)
            clearAnalyzeParameters();
        else {
            Protocol actualProtocol = analyzerEliminateConstantVars ? protocol.substituteConstantVariables() : protocol;
            String line = actualProtocol.toIrpString(properties.getAnalyzerBase());
            setAnalyzeParameters(line);
            if (properties.getPrintAnalyzerIRPsToConsole())
                guiUtils.message(line);
        }
    }

    private void setAnalyzeParameters(Analyzer analyzer) {
        Burst.Preferences burstPrefs = new Burst.Preferences(analyzerMaxRoundingError, analyzerMaxUnits, analyzerMaxMicroSeconds);
        Analyzer.AnalyzerParams params = new Analyzer.AnalyzerParams(analyzer.getFrequency(), analyzerTimeBase,
                analyzerLsb ? BitDirection.lsb : BitDirection.msb,
                analyzerExtent, analyzerParameterWidths, analyzerMaxParameterWidth, analyzerInvert, burstPrefs, new ArrayList<>(0));
        List<Protocol> protocols = null;
        try {
            protocols = analyzer.searchBestProtocol(params);
        } catch (NoDecoderMatchException ex) {
            guiUtils.error(ex);
        }
        if (protocols == null || protocols.isEmpty())
            clearAnalyzeParameters();
        else {
            setAnalyzeParameters(protocols.get(0));
        }
    }

    private void setAnalyzeParameters(ModulatedIrSequence irSequence) throws InvalidArgumentException {
        Analyzer analyzer = new Analyzer(irSequence, irSequence.getFrequency(), properties.getInvokeRepeatFinder(),
                properties.getAbsoluteTolerance(), properties.getRelativeTolerance());
        setAnalyzeParameters(analyzer);
    }

    private void setAnalyzeParameters(IrSignal irSignal) throws InvalidArgumentException {
        // Ignore repeatfinder request if irSignal already has repeat.
        if (properties.getInvokeRepeatFinder() && irSignal.getRepeatLength() == 0) {
            ModulatedIrSequence irSequence = irSignal.toModulatedIrSequence();
            setAnalyzeParameters(irSequence);
        } else {
            Analyzer analyzer = new Analyzer(irSignal, properties.getAbsoluteTolerance(), properties.getRelativeTolerance());
            setAnalyzeParameters(analyzer);
        }
    }

    public void scrutinizeIrSignal(IrSignal irSignal) throws InvalidArgumentException {
        if (irSignal == null || irSignal.isEmpty()) {
            guiUtils.error("Not scrutinizing empty signal.");
            return;
        }

        while (irSignal.containsZeros()) {
            String answer = guiUtils.getInput(
                    "The signal contains one or more duration of zero length, and is thus a meaningless signal.\n"
                    + "Enter a replacement value for the zero durations, if desired.\n"
                    + "If four characters, will be interpreted as Pronto hex, otherwise as microseconds.",
                    "Enter zero replacement", "0001");
            if (answer == null) {
                // user pressed cancel
                return;
            }
            answer = answer.trim();
            try {
                if (answer.length() == Pronto.CHARS_IN_DIGIT) {
                    irSignal.replaceZeros(Integer.parseInt(answer, 16));
                } else {
                    irSignal.replaceZeros(Double.parseDouble(answer));
                }
            } catch (NumberFormatException ex) {
                guiUtils.error(ex);
            }
        }

        setRepeatParameters(irSignal.getIntroLength(), irSignal.getRepeatLength(), irSignal.getRepeatLength() >  0 ? 1 : 0, irSignal.getEndingLength());

        if (properties.getInvokeAnalyzer())
            setAnalyzeParameters(irSignal);
        else
            clearAnalyzeParameters();
        displaySignal(irSignal);
    }

    private void scrutinizeIrSignal(JTable table) throws ErroneousSelectionException, GirrException, IrpException, IrCoreException {
        Command command = tableUtils.commandTableSelectedRow(table);
        scrutinizeIrSignal(command.toIrSignal());
    }

    private void displaySignal(IrSignal irSignal) {
        setFrequencyParameter(irSignal);
        setCaptureWindow(irSignal);
        irPlotter.plot(irSignal);
        setDecodeIrParameters(irSignal);
    }

    private void processIr(ModulatedIrSequence modulatedIrSequence) throws InvalidArgumentException {
        if (modulatedIrSequence == null || modulatedIrSequence.isEmpty()) {
            guiUtils.error("Not showing empty sequence.");
            return;
        }

        ModulatedIrSequence possiblyCleaned = properties.getInvokeCleaner()
                ? Cleaner.clean(modulatedIrSequence, properties.getAbsoluteTolerance(), properties.getRelativeTolerance())
                : modulatedIrSequence;

        if (properties.getInvokeAnalyzer())
            setAnalyzeParameters(modulatedIrSequence);
        else
            clearAnalyzeParameters();

        IrSignal irSignal;
        if (properties.getInvokeRepeatFinder()) {
            RepeatFinder repeatFinder = new RepeatFinder(modulatedIrSequence);
            setRepeatParameters(repeatFinder);
            irSignal = repeatFinder.getRepeatFinderData().chopIrSequence(possiblyCleaned);
        } else
            irSignal = new IrSignal(possiblyCleaned);
        displaySignal(irSignal);
    }

    private String mkTitle(JTable table) {
        return Version.appName + ((table == parameterTable) ? " parametric export" : " raw export");
    }

    private void saveSelectedCommands(JTable table) throws GirrException, IOException, TransformerException, IrCoreException, IrpException {
        saveCommands(table, tableUtils.commandTableSelected(table));
    }

    private void saveAllCommands(JTable table) throws IOException, TransformerException, GirrException, IrpException, IrCoreException {
        saveAllCommands(table, newExporter());
    }

    private void saveAllCommands(JTable table, Exporter exporter) throws IOException, TransformerException, IrCoreException, IrpException, GirrException {
        saveCommands(table, ((NamedIrSignal.LearnedIrSignalTableModel) table.getModel()).getCommandsWithSanityCheck(guiUtils), exporter);
    }

    private void saveCommands(JTable table, Map<String, Command> commands) throws IOException, TransformerException, IrCoreException, IrpException, GirrException {
        saveCommands(table, commands, newExporter());
    }

    private void saveCommands(JTable table, Map<String, Command> commands, Exporter exporter) throws IOException, TransformerException, IrCoreException, IrpException, GirrException {
        File file = saveCommands(commands, mkTitle(table), exporter);
        if (file != null)
            ((NamedIrSignal.LearnedIrSignalTableModel) table.getModel()).clearUnsavedChanges();
    }

    private File saveCommands(Map<String, Command> commands, String title) throws IOException, TransformerException, IrCoreException, IrpException, GirrException {
        return saveCommands(commands, title, newExporter());
    }

    private File saveCommands(Map<String, Command> commands, String title, Exporter exporter) throws IOException, TransformerException, IrCoreException, IrpException, GirrException {
        if (exporter == null) {
            guiUtils.error("No exporter");
            return null;
        }

        if (commands.isEmpty()) {
            guiUtils.error("Nothing to export");
            return null;
        }

        File savedFile;
        if (commands.size() == 1) {
            savedFile = saveSignalWrite(commands.values().iterator().next(), title, exporter);
        } else {
            if (! RemoteSetExporter.class.isInstance(exporter)) {
                guiUtils.error("Trying to export more than one signal, but the current export format only supports a single command");
                return null;
            }
            savedFile = saveCommandsWrite(commands, title, (RemoteSetExporter) exporter);
        }

        if (savedFile != null) {
            guiUtils.message("File " + savedFile + " was successfully written with " + commands.size() + (commands.size() == 1 ? " command." :  " commands."));
            if (properties.getAutoOpenExports())
                guiUtils.open(savedFile);
        }
        return savedFile;
    }

    private File saveCommandsWrite(Map<String, Command> commands, String title, RemoteSetExporter exporter) throws IOException, TransformerException, GirrException, IrpException, IrCoreException {
        if (properties.getExportAutomaticFilenames())
            if (!checkChangeExportDirectory(new File(exportDirectoryTextField.getText())))
                return null;

        if (properties.getExportInquireDeviceData() && exporter.supportsMetaData()) {
            Remote.MetaData newMetaData = MetaDataDialog.inquireMetaData(metaData, this);
            if (newMetaData == null) // user bailed out
                return null;

            metaData = newMetaData;
        } else
            metaData = new Remote.MetaData();

        return exporter.export(commands, null, title, metaData,
                properties.getExportAutomaticFilenames(), this,
                new File(properties.getExportDir()), properties.getExportCharsetName());
    }

    private void saveSignal(IrSignal irSignal) throws IOException, TransformerException, IrCoreException, IrpException, GirrException {
        saveSignal(irSignal, newExporter());
    }

    private void saveSignal(IrSignal irSignal, Exporter exporter) throws IOException, TransformerException, IrCoreException, IrpException, GirrException {
        if (irSignal == null) {
            guiUtils.error("Not exporting empty signal.");
            return;
        }
        Command command = new Command("IrScrutinizer scrutinized signal", null, irSignal);
        File savedFile = saveSignalWrite(command, "IrScrutinizer scrutinized signal", exporter);
        if (savedFile != null) {
            guiUtils.message("File " + savedFile.getPath() + " successfully writtten");
            if (properties.getAutoOpenExports())
                guiUtils.open(savedFile);
        }
    }

    private File saveSignalWrite(Command command, String title, Exporter exporter) throws IOException, TransformerException, IrCoreException, IrpException, GirrException {
        if (!checkChangeExportDirectory(new File(exportDirectoryTextField.getText())))
            return null;

        int numberRepeats = exportRepeatComboBox.isVisible() ? properties.getExportNoRepeats() : 1;
        return exporter.export(command, "IrScrutinizer captured signal", title,
                numberRepeats, properties.getExportAutomaticFilenames(), this,
                new File(properties.getExportDir()), properties.getExportCharsetName());
    }

    private void reAnalyze() {
        try {
            IrSignal irSignal = getCapturedIrSignal();
            if (irSignal != null)
                scrutinizeIrSignal(irSignal);
            else
                // Do not include the input text in the error message; it will make a popup unreadable.
                guiUtils.error("Could not parse the text in capture data window as IR signal.");
        } catch (InvalidArgumentException ex) {
            guiUtils.error(ex);
        } catch (RuntimeException ex) {
            // ??? Can be many different causes
            guiUtils.error("Unspecified error: \"" + ex.getMessage() + "\", please report.");
        }
    }

    private void saveCapturedDataAsText() {
        if (capturedDataTextArea.getText().trim().isEmpty()) {
            guiUtils.error("Nothing to save.");
            return;
        }

        TextExporter exporter = newTextExporter();
        try {
            File file = exporter.export(capturedDataTextArea.getText().trim(),
                    properties.getExportAutomaticFilenames(), this, new File(properties.getExportDir()),
                    properties.getExportCharsetName());
            guiUtils.message("File " + file.getPath() + " successfully written.");
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
    }

    GuiUtils getGuiUtils() {
        return guiUtils;
    }

    IrpDatabase getIrpDatabase() {
        return this.irpDatabase;
    }

    private void updateOutputFormat(OutputTextFormat format) {
        updateOutputFormat(format.ordinal());
    }

    private void updateOutputFormat(int format) {
        for (int i = 0; i < outputFormatMenu.getItemCount(); i++)
            outputFormatMenu.getItem(i).setSelected(i == format);
        properties.setOutputFormatIndex(format);
    }

    private String analysisString() {
        String linefeed = IrCoreUtils.LINE_SEPARATOR;
        StringBuilder str = new StringBuilder(256);
        str.append("Decode: ");
        str.append(decodeIRTextField.getText());
        str.append(linefeed);
        str.append("Analyze: ");
        str.append(analyzerTextField.getText());
        str.append(linefeed);
        str.append("Frequency: ");
        str.append(frequencyLabel.getText());
        str.append(linefeed);
        str.append(String.format("Introbursts: %s, Repeatbursts: %s, # repeats: %s, Endbursts: %s",
                introLengthLabel.getText(), repLengthLabel.getText(),
                noRepsLabel.getText(), endingLengthLabel.getText()));
        return str.toString();
    }

    private void importSequence(ICommandImporter importer) throws IrpException, IrCoreException {
        Collection<Command> commands = importer.getCommands();
        if (commands.isEmpty()) {
            guiUtils.error("Import does not contain any signals; aborting.");
            return;
        }
        if (commands.size() >= importSequenceAskThreshold
                && ! guiUtils.confirm("There are " + commands.size() + " commands. Proceed?"))
            return;

        processIr(Command.concatenateAsSequence(commands));
    }

    public int importCommands(RemoteSet remoteSet, Remote.MetaData metaData, boolean raw) {
        int sum = 0;
        for (Remote remote : remoteSet)
            sum += importCommands(remote, metaData, raw);
        return sum;
    }

    public int importCommands(Remote remote, Remote.MetaData metaData, boolean raw) {
        int sum = 0;
        for (CommandSet commandSet : remote)
            sum += importCommands(commandSet, metaData, raw);
        return sum;
    }

    public int importCommands(CommandSet commandSet, Remote.MetaData metaData, boolean raw) {
        return importCommands(commandSet.getCommands(), metaData, raw);
    }

    private int importCommands(Collection<Command> commands, Remote.MetaData metaData, boolean raw) {
        if (metaData != null && !metaData.isEmpty())
            this.metaData = metaData;
        boolean observeErrors = true;
        int count = 0;
        for (Command command : commands) {
            try {
                importCommand(command, raw);
                count++;
            } catch (IrpException | IrCoreException ex) {
                if (observeErrors) {
                    guiUtils.error("Erroneous signal: " + ex.getMessage());
                    boolean ans = guiUtils.confirm("Continue import and ignore further erroneous signals?");
                    if (ans)
                        observeErrors = false;
                    else
                        return -count;
                }
            }
        }
        return count;
    }

    public int importCommand(Command command, boolean raw) throws IrpException, IrCoreException {
        if (command == null)
            return 0;

        if (raw)
            registerRawCommand(command);
        else
            registerParameterCommand(command);
        return 1;
    }

    private <T extends IFileImporter & ICommandImporter> void importConcatenatedCommandsByFileSelector(T importer) {
        try {
            boolean status = importer.loadFileSelector(this, "Select file for signal import",
                    properties.getDefaultImportDir(), properties.getImportCharsetName());
            if (status)
                importSequence(importer);
        } catch (IOException | ParseException | IrCoreException | IrpException ex) {
            guiUtils.error(ex);
        }
    }

    private <T extends IFileImporter & IModulatedIrSequenceImporter> void importModulatedIrSequenceByFileSelector(T importer) {
    try {
            boolean status = importer.loadFileSelector(this, "Select file for signal import",
                    properties.getDefaultImportDir(), properties.getImportCharsetName());
            if (status)
                processIr(importer.getModulatedIrSequence());
        } catch (IOException | ParseException | InvalidArgumentException ex) {
            guiUtils.error(ex);
        }
    }

    private <T extends IFileImporter & ICommandImporter> void importRemoteByFileSelector(T importer, boolean raw) {
        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
            repaint();
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
            }
            boolean status = importer.loadFileSelector(this, "Select file for signal import",
                    properties.getDefaultImportDir(), properties.getImportCharsetName());
            if (status)
                importCommands(importer.getCommands(), importer.getMetaData(), raw);
        } catch (IOException | ParseException | InvalidArgumentException ex) {
            guiUtils.error(ex);
        } finally {
            busyWindow.unBusy();
        }
    }

    private <T extends IFileImporter & IModulatedIrSequenceImporter> ModulatedIrSequence importSequence(File file, T importer) {
        try {
            importer.load(file, properties.getImportCharsetName());
            return importer.getModulatedIrSequence();
        } catch (IOException | ParseException | InvalidArgumentException ex) {
        }
        return null;
    }

    private <T extends IFileImporter & ICommandImporter> ModulatedIrSequence importCommands(File file, T importer) {
        try {
            importer.load(file, properties.getImportCharsetName());
            return importer.getConcatenatedCommands();
        } catch (IOException | ParseException | IrCoreException | IrpException ex) {
        }
        return null;
    }

    private void importModulatedIrSequenceFile(File file) throws InvalidArgumentException {
        ModulatedIrSequence sequence = importCommands(file, girrImporter);
        if (sequence == null || sequence.isEmpty())
            sequence = importCommands(file, ictImporter);

        if (sequence == null || sequence.isEmpty())
            sequence = importSequence(file, new Mode2Importer());

        if (sequence == null || sequence.isEmpty())
            sequence = importSequence(file, waveImporter);

        if (sequence == null || sequence.isEmpty())
            guiUtils.error("File not recoginzed, ignored");
        else
            processIr(sequence);
    }

    private void registerRawCommands(Collection<Command> commands) throws IrpException, IrCoreException {
        for (Command command : commands)
            registerRawCommand(new RawIrSignal(command));
    }

    private void registerRawCommand(Command command) throws IrpException, IrCoreException {
        registerRawCommand(new RawIrSignal(command));
    }

    private void registerParameterCommand(Command command) throws IrpException, IrCoreException {
        registerParameterSignal(new ParametrizedIrSignal(command));
    }

    private void registerRawSignal(IrSignal irSignal, String name, String comment) {
        if (irSignal != null) {
            RawIrSignal cis = new RawIrSignal(irSignal, name, comment);
            registerRawCommand(cis);
        }
    }

    private void registerRawSignal(ModulatedIrSequence irSequence, String name, String comment) {
        if (irSequence != null) {
            RawIrSignal cis = new RawIrSignal(irSequence, name, comment);
            registerRawCommand(cis);
        }
    }

    private void registerRawCommand(RawIrSignal cir) {
        rawTableModel.addSignal(cir);
    }

    private void registerParameterSignal(ModulatedIrSequence irSequence) {
        if (irSequence != null) {
            try {
                ParametrizedIrSignal pir = new ParametrizedIrSignal(irSequence, properties.getParametrizedLearnIgnoreT());
                registerParameterSignal(pir);
            } catch (NoDecodeException ex) {
                guiUtils.message("Undecodable signal, ignored");
            }
        }
    }

    private void registerParameterSignal(IrSignal irSignal) {
        if (irSignal != null) {
            try {
                ParametrizedIrSignal pir = new ParametrizedIrSignal(irSignal, properties.getParametrizedLearnIgnoreT());
                registerParameterSignal(pir);
            } catch (NoDecodeException ex) {
                guiUtils.message("Undecodable signal, ignored");
            }
        }
    }

    private void registerParameterSignal(Collection<Command> commands) throws IrpException, IrCoreException {
        for (Command command : commands) {
            registerParameterSignal(new ParametrizedIrSignal(command));
        }
    }

    private void registerParameterSignal(ParametrizedIrSignal signal) {
        parameterTableModel.addSignal(signal);
    }

    @SuppressWarnings("unchecked")
    private <T extends TableModel> void enableSorter(JTable table, boolean state) {
        TableRowSorter<T> tableRowSorter = state ? new TableRowSorter<>((T) table.getModel()) : null;
        table.setRowSorter(tableRowSorter);
    }

    private <T extends TableModel> void copyTableToClipboard(JTable table, boolean useSelection) {
        StringBuilder str = new StringBuilder(1024);
        for (int i = 0; i < table.getRowCount(); i++) {
            if (!useSelection || table.isRowSelected(i)) {
                if (str.length() > 0)
                    str.append('\n');
                for (int j = 0; j < table.getColumnCount(); j++) {
                    if (j > 0)
                        str.append('\t');
                    Object obj = table.getValueAt(i, j);
                    if (obj != null)
                        str.append(obj.toString());
                }
            }
        }
        (new CopyClipboardText(null)).toClipboard(str.toString());
    }

    private void enableRawCaptureOnly(boolean value) {
        // FXIME: what is more to do? Disable the popup menu?
        rawCookedTabbedPane.setEnabledAt(0, !value);
        for (int i = 0; i < this.topLevelTabbedPane.getTabCount(); i++) {
            this.topLevelTabbedPane.setEnabledAt(i, !value || i == this.topLevelTabbedPane.getSelectedIndex());
        }
    }

    private void parameterTableAddMissingF() throws UnknownProtocolException, ErroneousSelectionException, GirrException, IrpException, IrCoreException {
        Command command = tableUtils.commandTableSelectedRow(parameterTable);
        if (command == null)
            throw new IllegalArgumentException("No command selected.");
        ArrayList<Long> presentFs = parameterTableModel.listF(command);
        String protocolName = command.getProtocolName();
        Protocol protocol = irpDatabase.getProtocol(protocolName);
        for (Long F = protocol.getParameterMin("F"); F <= protocol.getParameterMax("F"); F++) {
            if (!presentFs.contains(F)) {
                @SuppressWarnings("unchecked")
                Map<String, Long> params = new HashMap<>(command.getParameters());
                params.put("F", F);
                registerParameterCommand(new Command(DefaultSignalNameFormatter.formatName(command.getProtocolName(), params),
                        "", protocolName, params));
            }
        }
    }

    private void setParameter(String name) {
        List<Integer> rows = tableUtils.modelLinesSelected(parameterTable);
        if (rows.isEmpty()) {
            guiUtils.error("Nothing selected");
            return;
        }
        Long answer = guiUtils.getLongInput("Enter new value for " + name, 0);
        if (answer != null)
            parameterTableModel.setParameter(name, answer, rows);
    }

    private boolean transmit(IrSignal irSignal) throws IOException, HardwareUnavailableException, HarcHardwareException, NoSuchTransmitterException, InvalidArgumentException, CannotSendException {
        return hardwareManager.sendIr(irSignal, Integer.parseInt((String)noTransmitsComboBox.getSelectedItem()));
    }

    public boolean transmit(Command command) throws IrpException, IrCoreException, IOException, HardwareUnavailableException, HarcHardwareException, NoSuchTransmitterException, InvalidArgumentException, CannotSendException {
        return transmit(command.toIrSignal());
    }

    private boolean transmit(JTable table) throws ErroneousSelectionException, GirrException, IrpException, IrCoreException, IOException, HardwareUnavailableException, HarcHardwareException, NoSuchTransmitterException, InvalidArgumentException, CannotSendException {
        Command command = tableUtils.commandTableSelectedRow(table);
        return transmit(command);
    }

    public void selectImportPane(ImportType type) {
        lastPane = topLevelTabbedPane.getSelectedComponent();
        topLevelTabbedPane.setSelectedComponent(type == ImportType.signal ? signalScrutinizerPanel : remoteScrutinizerPanel);
        if (type != ImportType.signal)
            rawCookedTabbedPane.setSelectedComponent(type == ImportType.parametricRemote ? cookedPanel : rawPanel);
    }

    private void selectLastPane() {
        if (lastPane != null) {
            Component present = topLevelTabbedPane.getSelectedComponent();
            topLevelTabbedPane.setSelectedComponent(lastPane);
            lastPane = present;
        }
    }

    private void enableSubFormats(boolean select) {
        subformatsPanel.setVisible(select);
//        exportGenerateParametersCheckBox.setEnabled(select);
//        exportGenerateRawCheckBox.setEnabled(select);
//        exportGenerateCcfCheckBox.setEnabled(select);
//        exportGenerateShortCcfCheckBox.setEnabled(select);
//        exportGenerateSendIrCheckBox.setEnabled(select);
    }

    private void selectFormat() {
        selectFormat(properties.getExportFormatName());
    }

    private void selectFormat(String formatNm) {
        String formatName = formatNm;
        IExporterFactory format = exportFormatManager.get(formatName);
        if (format == null) {
            guiUtils.warning("Export format " + formatName + " not found, selecting Girr.");
            formatName = "Girr";
            format = exportFormatManager.get(formatName);
        }
        properties.setExportFormatName(formatName);

        Exporter exporter = format.newExporter();
        boolean supportsEmbedded = RemoteSetExporter.class.isInstance(exporter) && ((RemoteSetExporter) exporter).supportsEmbeddedFormats();
        enableSubFormats(supportsEmbedded);
        boolean enableRepetitions = supportsEmbedded && exportGenerateSendIrCheckBox.isSelected() || exporter.considersRepetitions();
        exportRepeatComboBox.setVisible(enableRepetitions);
        exportRepeatComboBox.setEnabled(enableRepetitions);
        exportNumberRepeatsLabel.setVisible(enableRepetitions);
        if (!formatName.equals(exportFormatComboBox.getSelectedItem()))
            exportFormatComboBox.setSelectedItem(formatName);
        exportFormatManager.setMenuSelection(formatName);
        exportFormatDocumentationButton.setEnabled(exporter.getDocumentation() != null);
        enableProtocolParameters(formatName);
    }

    private void enableProtocolParameters(String formatName) {
        exportFormatParametersPane.setVisible(formatName.equals("Girr") || formatName.equals("Wave") || formatName.equals("ProntoClassic"));
        girrExportOptionsPanel.setVisible(formatName.equals("Girr"));
        waveExportOptionsPanel.setVisible(formatName.equals("Wave"));
        prontoClassicExportOptionsPanel.setVisible(formatName.equals("ProntoClassic"));
    }

    private void setCapturedDataTextAreaFromClipboard() {
        String clip = (new CopyClipboardText(null)).fromClipboard();
        if (clip == null)
            clip = "";
        capturedDataTextArea.setText(clip.replace('\n', ' '));
    }

    // There is almost certainly a more eleganat way of doing this ;-).
    private void insertCapturedDataTextAreaFromClipboard() {
        String clip = (new CopyClipboardText(null)).fromClipboard();
        if (clip == null)
            clip = "";
        String old = capturedDataTextArea.getText();
        if (old == null)
            old = "";
        int index = capturedDataTextArea.getCaretPosition();
        String newContent = (old.substring(0, index) + clip + old.substring(index)).replace('\n', ' ');
        capturedDataTextArea.setText(newContent);
        capturedDataTextArea.setCaretPosition(index + clip.length());
    }

    private void loadExportFormatsGuiRefresh() {
        try {
            loadExportFormats();
            exportFormatComboBox.setModel(new DefaultComboBoxModel<>(exportFormatManager.toArray()));
            exportFormatComboBox.setSelectedItem(properties.getExportFormatName());
            optionsMenu.remove(dynamicExportFormatsMenuPosition);
            optionsMenu.add(exportFormatManager.getMenu(properties.getExportFormatName()), dynamicExportFormatsMenuPosition);
        } catch (IOException | ParserConfigurationException | SAXException ex) {
            guiUtils.error(ex);
        }
    }

    private boolean checkChangeExportDirectory(File dirName) {
        // Don't care for checking writeability of dirName
        File dir;
        try {
            dir = dirName.getCanonicalFile();
        } catch (IOException ex) {
            // really unlikely
            guiUtils.error(ex);
            return false;
        }
        if (dir.isDirectory()) {
            properties.setExportDir(exportDirectoryTextField.getText());
            return true;
        }

        boolean create = guiUtils.confirm(exportDirectoryTextField.getText() + " does not exist. Create it?");
        if (!create)
            return false;

        boolean status = dir.mkdirs();
        if (!status) {
            guiUtils.error("Failed to create " + dir);
            return false;
        }

        properties.setExportDir(exportDirectoryTextField.getText());
        return true;
    }

    private void protocolDocuPopup(Command command) throws IrpException, IrCoreException {
        protocolDocuPopup(command.getProtocolName());
    }

    private void protocolDocuPopup(String protocolName) throws IrpException {
        if (protocolName == null)
            throw new IrpException("No decode.");

        String docu = irpDatabase.getDocumentation(protocolName); // throws UnknownProtocolException
        StringBuilder str = new StringBuilder(irpDatabase.getIrp(protocolName)).append("\n\n");
        if (docu != null)
            str.append(docu);
        HelpPopup.newHelpPopup(this, str.toString(), protocolName);
    }

    private void pasteColumnInTable(JTable rawTable, NamedIrSignal.LearnedIrSignalTableModel tableModel) throws NoSelectionException {
        String clip = (new CopyClipboardText(null)).fromClipboard();
        String[] arr = clip.split("\r?\n");
        int row = rawTable.getSelectedRow();
        int column = rawTable.convertColumnIndexToModel(rawTable.getSelectedColumn());
        if (row < 0 || column < 0)
            throw new NoSelectionException("No target cell selected");
        tableModel.replaceColumnSubset(row, column, arr);
    }

    private static interface CaptureThreadClient {

        JToggleButton getButton();
        void processSequence(ModulatedIrSequence modulatedIrSequence);
    }

    private class CaptureThread extends Thread {

        private final CaptureThreadClient client;
        private boolean terminate;

        CaptureThread(CaptureThreadClient client) {
            this.client = client;
            terminate = false;
        }

        @Override
        public void run() {
            startButton.setEnabled(false);
            while (!terminate && client.getButton().isSelected()) {
                try {
                    ModulatedIrSequence sequence = captureIrSequence();
                    if (sequence != null)
                        client.processSequence(sequence);

                    if (!hardwareManager.isReady()) {
                        guiUtils.error("Selected capture device is no longer ready");
                        client.getButton().setSelected(false);
                    }
                } catch (HardwareUnavailableException | IOException | HarcHardwareException | InvalidArgumentException ex) {
                    guiUtils.error(ex);
                    client.getButton().setSelected(false);
                }
            }
            captureThread = null; // thread suicide
            startButton.setEnabled(true);
            enableRawCaptureOnly(false);
            topLevelTabbedPane.setEnabled(true);
            jumpToLastPanelMenuItem.setEnabled(true);
        }

        private void terminate() {
            terminate = true;
        }
    }

    private class ScrutinizeIrCaller implements LookAndFeelManager.ILookAndFeelManagerCaller {
        @Override
        public void err(Exception ex, String str) {
            guiUtils.error(ex, str);
        }

        @Override
        public void setLAFProperty(int index) {
            properties.setLookAndFeel(index);
        }
    }

    private class GirrImporterBeanTransferHandler extends TransferHandler {
        private final boolean raw;

        private GirrImporterBeanTransferHandler(boolean raw) {
            super();
            this.raw = raw;
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
                return false;

            boolean copySupported = (COPY & support.getSourceDropActions()) == COPY;
            if (!copySupported)
                return false;

            support.setDropAction(COPY);
            return true;
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            if (!canImport(support))
                return false;

            Transferable transferable = support.getTransferable();
            try {
                @SuppressWarnings("unchecked")
                List<File> list = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                list.forEach((file) -> {
                    try {
                        importGirr(file, raw);
                    } catch (IOException | ParseException | InvalidArgumentException ex) {
                        guiUtils.error(ex);
                    }
                });
            } catch (UnsupportedFlavorException | IOException e) {
                return false;
            }
            return true;
        }
    };

    private class SignalScrutinizerTransferHandler extends TransferHandler {

        TransferHandler oldhandler;
        private Position p0 = null;
        private Position p1 = null;

        private SignalScrutinizerTransferHandler(TransferHandler oldhandler) {
            super();
            this.oldhandler = oldhandler;
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            if (support.isDrop()) {
                if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
                    return false;

                boolean copySupported = (COPY & support.getSourceDropActions()) == COPY;
                if (!copySupported)
                    return false;

                support.setDropAction(COPY);
            }
            return true;
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            if (!canImport(support))
                return false;

            try {
                Transferable transferable = support.getTransferable();
                if (support.isDrop()) {
                    @SuppressWarnings("unchecked")
                    List<File> list = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                    if (list.size() > 1) {
                        guiUtils.error("Only one file can be dropped");
                        return false;
                    }

                    importModulatedIrSequenceFile(list.get(0));
                    return true;
                } else {
                    return oldhandler.importData(support);
                }
            } catch (UnsupportedFlavorException | IOException | InvalidArgumentException ex) {
                guiUtils.error(ex);
                return false;
            }
        }

        @Override
        protected Transferable createTransferable(JComponent component) {
            JTextComponent textComponent = (JTextComponent) component;
            int start = textComponent.getSelectionStart();
            int end = textComponent.getSelectionEnd();

            if (start == end)
                return null;

            try {
                Document doc = textComponent.getDocument();
                p0 = doc.createPosition(start);
                p1 = doc.createPosition(end);
            } catch (BadLocationException e) {
                guiUtils.error("Can't create position.");
            }
            return new StringSelection(textComponent.getSelectedText());
        }

        @Override
        public int getSourceActions(JComponent c) {
            return oldhandler.getSourceActions(c);
        }

        @Override
        protected void exportDone(JComponent component, Transferable data, int action) {
            if (action == MOVE && (p0 != null) && (p1 != null) && (p0.getOffset() != p1.getOffset())) {
                try {
                    ((JTextComponent) component).getDocument().remove(p0.getOffset(), p1.getOffset() - p0.getOffset());
                } catch (BadLocationException e) {
                    guiUtils.error("Can't remove text from source.");
                }
            }
        }
    }

    private boolean checkRowsSelected(javax.swing.JTable table) {
        if (table.getSelectionModel().isSelectionEmpty()) {
            guiUtils.error("Nothing selected");
            return false;
        }
        return true;
    }

    private void transformNameActionPerformed(javax.swing.JTable table, NamedIrSignal.LearnedIrSignalTableModel model, Function<String, String> transformation) {
        List<Integer> rows = tableUtils.modelLinesSelected(table);
        if (rows.isEmpty()) {
            guiUtils.error("Nothing selected");
            return;
        }
        model.namesTransform(transformation, rows);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        analyzerBaseButtonGroup = new javax.swing.ButtonGroup();
        CCFCodePopupMenu = new javax.swing.JPopupMenu();
        rawCodeClearMenuItem = new javax.swing.JMenuItem();
        signalClearMenuItem = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JPopupMenu.Separator();
        rawCodeCopyMenuItem = new javax.swing.JMenuItem();
        rawCodeCopyAllMenuItem = new javax.swing.JMenuItem();
        rawCodePasteReplacingMenuItem = new javax.swing.JMenuItem();
        rawCodePasteMenuItem = new javax.swing.JMenuItem();
        rawCodePasteAnalyzeMenuItem = new javax.swing.JMenuItem();
        rawCodeSelectAllMenuItem = new javax.swing.JMenuItem();
        jSeparator23 = new javax.swing.JPopupMenu.Separator();
        rawAddTestSignalMenuItem = new javax.swing.JMenuItem();
        jSeparator36 = new javax.swing.JPopupMenu.Separator();
        rawCodeAnalyzeMenuItem = new javax.swing.JMenuItem();
        scrutinizeSignalProtocolDocuMenuItem = new javax.swing.JMenuItem();
        jSeparator9 = new javax.swing.JPopupMenu.Separator();
        undoDataMenuItem = new javax.swing.JMenuItem();
        jSeparator30 = new javax.swing.JPopupMenu.Separator();
        rawCodeSaveMenuItem = new javax.swing.JMenuItem();
        importCaptureMenu = new javax.swing.JMenu();
        importSignalAsGirrMenuItem1 = new javax.swing.JMenuItem();
        importSignalAsIctMenuItem1 = new javax.swing.JMenuItem();
        importSignalAsWaveMenuItem1 = new javax.swing.JMenuItem();
        importSignalAsMode2MenuItem1 = new javax.swing.JMenuItem();
        exportCapturedMenu = new javax.swing.JMenu();
        exportSignalGirrMenuItem1 = new javax.swing.JMenuItem();
        exportSignalIctMenuItem1 = new javax.swing.JMenuItem();
        signalSignalTextMenuItem1 = new javax.swing.JMenuItem();
        exportSignalWaveMenuItem1 = new javax.swing.JMenuItem();
        parameterTablePopupMenu = new javax.swing.JPopupMenu();
        parametricSorterCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator24 = new javax.swing.JPopupMenu.Separator();
        moveUpMenuItem1 = new javax.swing.JMenuItem();
        moveDownMenuItem1 = new javax.swing.JMenuItem();
        jSeparator17 = new javax.swing.JPopupMenu.Separator();
        addEmptyParametrizedSignalMenuItem = new javax.swing.JMenuItem();
        addParametrizedTestSignalMenuItem = new javax.swing.JMenuItem();
        jSeparator13 = new javax.swing.JPopupMenu.Separator();
        scrutinizeParametricMenuItem = new javax.swing.JMenuItem();
        transmitMenuItem = new javax.swing.JMenuItem();
        protocolDocuMenuItem = new javax.swing.JMenuItem();
        jSeparator37 = new javax.swing.JPopupMenu.Separator();
        duplicateParametricMenuItem = new javax.swing.JMenuItem();
        deleteMenuItem1 = new javax.swing.JMenuItem();
        exportSelectedCookedMenuItem = new javax.swing.JMenuItem();
        printTableRowMenuItem = new javax.swing.JMenuItem();
        clearSelectionParametricMenuItem = new javax.swing.JMenuItem();
        jSeparator14 = new javax.swing.JPopupMenu.Separator();
        deleteAllMenuItem = new javax.swing.JMenuItem();
        checkParametrizedSignalsMenuItem = new javax.swing.JMenuItem();
        parameterUniquefyMenuItem = new javax.swing.JMenuItem();
        exportCookedMenuItem = new javax.swing.JMenuItem();
        jSeparator19 = new javax.swing.JPopupMenu.Separator();
        parametrizedCopyAllMenuItem = new javax.swing.JMenuItem();
        parametrizedCopySelectionMenuItem = new javax.swing.JMenuItem();
        jSeparator27 = new javax.swing.JPopupMenu.Separator();
        searchParametrizedMenuItem = new javax.swing.JMenuItem();
        jSeparator43 = new javax.swing.JPopupMenu.Separator();
        parametrizedPasteColumnMenuItem = new javax.swing.JMenuItem();
        jSeparator40 = new javax.swing.JPopupMenu.Separator();
        hideColumnMenuItem1 = new javax.swing.JMenuItem();
        resetRawTableColumnsMenuItem1 = new javax.swing.JMenuItem();
        hideUnusedMenuItem = new javax.swing.JMenuItem();
        hideUninterestingColumnsMenuItem = new javax.swing.JMenuItem();
        deleteParametrizedCommentMenuItem = new javax.swing.JMenuItem();
        parametrizedAdvancedMenu = new javax.swing.JMenu();
        setProtocolMenuItem = new javax.swing.JMenuItem();
        setDMenuItem = new javax.swing.JMenuItem();
        setSMenuItem = new javax.swing.JMenuItem();
        setFMenuItem = new javax.swing.JMenuItem();
        setTMenuItem = new javax.swing.JMenuItem();
        unsetTMenuItem = new javax.swing.JMenuItem();
        setMiscParamsMenuItem = new javax.swing.JMenuItem();
        transformNamesMenu = new javax.swing.JMenu();
        lowercaseNameMenuItem = new javax.swing.JMenuItem();
        upperCaseMenuItem = new javax.swing.JMenuItem();
        captitalizeNameMenuItem = new javax.swing.JMenuItem();
        jSeparator42 = new javax.swing.JPopupMenu.Separator();
        addNamePrefixMenuItem = new javax.swing.JMenuItem();
        transformNameMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        addMissingFsMenuItem = new javax.swing.JMenuItem();
        addMissingNamesMenuItem = new javax.swing.JMenuItem();
        deleteDefaultedSignalsMenuItem = new javax.swing.JMenuItem();
        deleteEmptyParametrizedSignalsMenuItem = new javax.swing.JMenuItem();
        rawTablePopupMenu = new javax.swing.JPopupMenu();
        rawSorterCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator25 = new javax.swing.JPopupMenu.Separator();
        moveUpMenuItem = new javax.swing.JMenuItem();
        moveDownMenuItem = new javax.swing.JMenuItem();
        jSeparator11 = new javax.swing.JPopupMenu.Separator();
        addEmptyRawMenuItem = new javax.swing.JMenuItem();
        addRawTestSignalMenuItem = new javax.swing.JMenuItem();
        rawFromClipboardMenuItem = new javax.swing.JMenuItem();
        jSeparator18 = new javax.swing.JPopupMenu.Separator();
        rawChopMenuItem = new javax.swing.JMenuItem();
        scrutinizeMenuItem = new javax.swing.JMenuItem();
        sendMenuItem = new javax.swing.JMenuItem();
        rawProtocolDocuMenuItem = new javax.swing.JMenuItem();
        jSeparator38 = new javax.swing.JPopupMenu.Separator();
        duplicateRawMenuItem = new javax.swing.JMenuItem();
        deleteMenuItem = new javax.swing.JMenuItem();
        saveSelectedRawTableRowMenuItem = new javax.swing.JMenuItem();
        printRawTableRowMenuItem = new javax.swing.JMenuItem();
        copyToParametrizedMenuItem = new javax.swing.JMenuItem();
        clearSelectionRawMenuItem = new javax.swing.JMenuItem();
        jSeparator12 = new javax.swing.JPopupMenu.Separator();
        deleteAllRawsMenuItem = new javax.swing.JMenuItem();
        checkRawCommandsMenuItem = new javax.swing.JMenuItem();
        rawUniquefyMenuItem = new javax.swing.JMenuItem();
        saveRawMenuItem = new javax.swing.JMenuItem();
        jSeparator28 = new javax.swing.JPopupMenu.Separator();
        rawCopyAllMenuItem = new javax.swing.JMenuItem();
        rawCopySelectionMenuItem = new javax.swing.JMenuItem();
        jSeparator44 = new javax.swing.JPopupMenu.Separator();
        searchRawMenuItem = new javax.swing.JMenuItem();
        jSeparator39 = new javax.swing.JPopupMenu.Separator();
        rawPasteColumnMenuItem = new javax.swing.JMenuItem();
        jSeparator29 = new javax.swing.JPopupMenu.Separator();
        hideColumnMenuItem = new javax.swing.JMenuItem();
        resetRawTableColumnsMenuItem = new javax.swing.JMenuItem();
        removeUnusedMenuItem1 = new javax.swing.JMenuItem();
        hideUninterestingColumnsMenuItem1 = new javax.swing.JMenuItem();
        deleteRawCommentMenuItem = new javax.swing.JMenuItem();
        deleteEmptyRawSignalsMenuItem = new javax.swing.JMenuItem();
        copyPastePopupMenu = new org.harctoolbox.guicomponents.CopyPastePopupMenu(true);
        copyPopupMenu = new org.harctoolbox.guicomponents.CopyPastePopupMenu();
        topLevelSplitPane = new javax.swing.JSplitPane();
        console = new org.harctoolbox.guicomponents.Console();
        topLevelTabbedPane = new javax.swing.JTabbedPane();
        signalScrutinizerPanel = new javax.swing.JPanel();
        capturedDataScrollPane = new javax.swing.JScrollPane();
        capturedDataTextArea = new UndoableJTextArea();
        infoPanel = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        introLengthLabel = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        repLengthLabel = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        noRepsLabel = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        endingLengthLabel = new javax.swing.JLabel();
        startButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        frequencyLabel = new javax.swing.JLabel();
        signalExportButton = new javax.swing.JButton();
        analyzerTextField = new javax.swing.JTextField();
        decodeIRTextField = new javax.swing.JTextField();
        transmitSignalButton = new javax.swing.JButton();
        scrutinizeSignalHelpButton = new javax.swing.JButton();
        transmitSignalButton1 = new javax.swing.JButton();
        pasteAnalyzeButton = new javax.swing.JButton();
        continuousCaptureButton = new javax.swing.JToggleButton();
        plotScrollPane = new javax.swing.JScrollPane();
        irPlotter = new org.harctoolbox.guicomponents.IrPlotter();
        remoteScrutinizerPanel = new javax.swing.JPanel();
        rawCookedTabbedPane = new javax.swing.JTabbedPane();
        cookedPanel = new javax.swing.JPanel();
        parameterTableScrollPane = new javax.swing.JScrollPane();
        parameterTable = new javax.swing.JTable();
        rawPanel = new javax.swing.JPanel();
        rawTableScrollPane = new javax.swing.JScrollPane();
        rawTable = new javax.swing.JTable();
        startStopToggleButton = new javax.swing.JToggleButton();
        editingTextField = new javax.swing.JTextField();
        parametricOrRawExportButton = new javax.swing.JButton();
        scrutinizeRemoteHelpButton = new javax.swing.JButton();
        generatePanel = new javax.swing.JPanel();
        toScrutinizeButton = new javax.swing.JButton();
        transferToRawRemoteButton = new javax.swing.JButton();
        transmitGenerateButton = new javax.swing.JButton();
        generateExportButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        generateTextArea = new javax.swing.JTextArea();
        irpMasterBean = new org.harctoolbox.guicomponents.IrpRenderBean(this, guiUtils, irpDatabase, properties.getIrpMasterCurrentProtocol(), properties.getIrpMasterCurrentD(), properties.getIrpMasterCurrentS(), properties.getIrpMasterCurrentF(), properties.getIrpMasterCurrentT(), properties.getIrpMasterCurrentAdditionalParameters());
        generateButton = new javax.swing.JButton();
        generateHelpButton = new javax.swing.JButton();
        transferToParametricRemoteButton = new javax.swing.JButton();
        openLastFileButton1 = new javax.swing.JButton();
        importPanel = new javax.swing.JPanel();
        importTabbedPane = new javax.swing.JTabbedPane();
        controlTowerPanel = new javax.swing.JPanel();
        controlTowerImportButton = new javax.swing.JButton();
        controlTowerBrowseButton = new javax.swing.JButton();
        controlTowerManufacturerComboBox = new javax.swing.JComboBox<>();
        controlTowerDeviceTypeComboBox = new javax.swing.JComboBox<>();
        controlTowerCodeSetComboBox = new javax.swing.JComboBox<>();
        jLabel50 = new javax.swing.JLabel();
        jLabel51 = new javax.swing.JLabel();
        jLabel52 = new javax.swing.JLabel();
        controlTowerTreeImporter = new org.harctoolbox.irscrutinizer.importer.TreeImporter(this.guiUtils, true);
        importControlTowerHelpButton = new javax.swing.JButton();
        remoteLocatorPanel = new javax.swing.JPanel();
        remoteLocatorBrowseButton = new javax.swing.JButton();
        remoteLocatorImportButton = new javax.swing.JButton();
        remoteLocatorManufacturerComboBox = new javax.swing.JComboBox<>();
        remoteLocatorDeviceTypeComboBox = new javax.swing.JComboBox<>();
        remoteLocatorRemoteComboBox = new javax.swing.JComboBox<>();
        jLabel45 = new javax.swing.JLabel();
        jLabel46 = new javax.swing.JLabel();
        jLabel49 = new javax.swing.JLabel();
        remoteLocatorTreeImporter = new org.harctoolbox.irscrutinizer.importer.TreeImporter(this.guiUtils);
        remoteLocatorBrowseRemoteButton = new javax.swing.JButton();
        remoteLocatorHelpButton = new javax.swing.JButton();
        remoteLocatorImporterKindTextField = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        girrImportPanel = new javax.swing.JPanel();
        girrWebSiteButton = new javax.swing.JButton();
        girrFileImporterBean = new org.harctoolbox.irscrutinizer.importer.FileImporterBean<>(guiUtils, properties, girrImporter);
        importGirrSignalHelpButton = new javax.swing.JButton();
        lircImportPanel = new javax.swing.JPanel();
        jButton20 = new javax.swing.JButton();
        lircFileImporterBean = new org.harctoolbox.irscrutinizer.importer.FileImporterBean<>(guiUtils, properties, lircImporter);
        importLircHelpButton = new javax.swing.JButton();
        irtransImportPanel = new javax.swing.JPanel();
        irTransWebButton = new javax.swing.JButton();
        irTransFileImporterBean = new org.harctoolbox.irscrutinizer.importer.FileImporterBean<>(guiUtils, properties, irTransImporter);
        importIrTransHelpButton = new javax.swing.JButton();
        ccfImportPanel = new javax.swing.JPanel();
        ccfFileImporterBean = new org.harctoolbox.irscrutinizer.importer.FileImporterBean<>(guiUtils, properties, ccfImporter);
        importProntoClassicHelpButton = new javax.swing.JButton();
        xcfImportPanel = new javax.swing.JPanel();
        xcfFileImporterBean = new org.harctoolbox.irscrutinizer.importer.FileImporterBean<>(guiUtils, properties, xcfImporter);
        importProntoProfessionalHelpButton = new javax.swing.JButton();
        ictImportPanel = new javax.swing.JPanel();
        ictFileImporterBean = new org.harctoolbox.irscrutinizer.importer.FileImporterBean<>(guiUtils, properties, ictImporter);
        importIctHelpButton = new javax.swing.JButton();
        chopIctImportCheckBox = new javax.swing.JCheckBox();
        mode2ImportPanel = new javax.swing.JPanel();
        mode2FileImporterBean = new org.harctoolbox.irscrutinizer.importer.FileImporterBean<>(guiUtils, properties, new Mode2Importer());
        jLabel17 = new javax.swing.JLabel();
        importMode2HelpButton = new javax.swing.JButton();
        csvImportPanel = new javax.swing.JPanel();
        parametrizedRawTabbedPane = new javax.swing.JTabbedPane();
        csvRawImportPanel = new javax.swing.JPanel();
        rawNameColumnComboBox = new javax.swing.JComboBox<>();
        rawCodeColumnComboBox1 = new javax.swing.JComboBox<>();
        includeSubsequenctColumnsCheckBox1 = new javax.swing.JCheckBox();
        jLabel25 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        csvRawSeparatorComboBox = new javax.swing.JComboBox<>();
        jLabel30 = new javax.swing.JLabel();
        csvRawFileImporterBean = new org.harctoolbox.irscrutinizer.importer.FileImporterBean<>(guiUtils, properties, csvRawImporter);
        importTextRawHelpButton = new javax.swing.JButton();
        rawMultiColumnNameCheckBox = new javax.swing.JCheckBox();
        rawLineCsvImportPanel = new javax.swing.JPanel();
        rawLineCsvFileImporterBean = new org.harctoolbox.irscrutinizer.importer.FileImporterBean<>(guiUtils, properties, new RawLineImporter());
        importTextRawLineBasedHelpButton = new javax.swing.JButton();
        parametrizedCsvImportPanel = new javax.swing.JPanel();
        parametrizedNameColumnComboBox = new javax.swing.JComboBox<>();
        protocolColumnComboBox = new javax.swing.JComboBox<>();
        dColumnComboBox = new javax.swing.JComboBox<>();
        sColumnComboBox = new javax.swing.JComboBox<>();
        fColumnComboBox = new javax.swing.JComboBox<>();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        parametrizedCsvSeparatorComboBox = new javax.swing.JComboBox<>();
        csvParametrizedFileImporterBean = new org.harctoolbox.irscrutinizer.importer.FileImporterBean<>(guiUtils, properties, csvParametrizedImporter);
        jLabel14 = new javax.swing.JLabel();
        parametrizedBaseComboBox = new javax.swing.JComboBox<>();
        importTextParametrizedHelpButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        parametrizedMultiColumnNameCheckBox = new javax.swing.JCheckBox();
        miscParametersColumnComboBox = new javax.swing.JComboBox<>();
        jLabel29 = new javax.swing.JLabel();
        waveImportPanel = new javax.swing.JPanel();
        waveFileImporterBean = new org.harctoolbox.irscrutinizer.importer.FileImporterBean<>(guiUtils, properties, waveImporter);
        importWaveDivideCarrierCheckBox1 = new javax.swing.JCheckBox();
        importWaveHelpButton = new javax.swing.JButton();
        cmlImportPanel = new javax.swing.JPanel();
        cmlFileImporterBean = new org.harctoolbox.irscrutinizer.importer.FileImporterBean<>(guiUtils, properties, cmlImporter);
        importCmlHelpButton = new javax.swing.JButton();
        commandFusionImportPanel = new javax.swing.JPanel();
        commandFusionFileImporterBean = new org.harctoolbox.irscrutinizer.importer.FileImporterBean<>(guiUtils, properties, commandFusionImporter);
        importCommandFusionHelpButton = new javax.swing.JButton();
        exportPanel = new javax.swing.JPanel();
        exportFormatComboBox = new javax.swing.JComboBox<>();
        jLabel6 = new javax.swing.JLabel();
        exportDirectoryTextField = new javax.swing.JTextField();
        exportDirSelectButton = new javax.swing.JButton();
        exportDirOpenButton = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        exportSignalButton = new javax.swing.JButton();
        automaticExportFilenamesCheckBox = new javax.swing.JCheckBox();
        openLastFileButton = new javax.swing.JButton();
        exportRawRemoteButton = new javax.swing.JButton();
        exportParametricRemoteButton = new javax.swing.JButton();
        exportRepeatComboBox = new javax.swing.JComboBox<>();
        exportNumberRepeatsLabel = new javax.swing.JLabel();
        exportHelpButton = new javax.swing.JButton();
        exportRawRemoteButton1 = new javax.swing.JButton();
        autoOpenExportsCheckBox = new javax.swing.JCheckBox();
        exportFormatDocumentationButton = new javax.swing.JButton();
        exportFormatParametersPane = new javax.swing.JLayeredPane();
        girrExportOptionsPanel = new javax.swing.JPanel();
        exportGirrHelpButton = new javax.swing.JButton();
        girrFatRawCheckBox = new javax.swing.JCheckBox();
        prontoClassicExportOptionsPanel = new javax.swing.JPanel();
        prontoModelComboBox = new javax.swing.JComboBox<>();
        jLabel22 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        prontoExportScreenWidthTextField = new javax.swing.JTextField();
        prontoExportScreenHeightTextField = new javax.swing.JTextField();
        jLabel31 = new javax.swing.JLabel();
        prontoExportButtonWidthTextField = new javax.swing.JTextField();
        prontoExportButtonHeightTextField = new javax.swing.JTextField();
        exportProntoHelpButton = new javax.swing.JButton();
        waveExportOptionsPanel = new javax.swing.JPanel();
        exportWaveHelpButton = new javax.swing.JButton();
        exportAudioParametersBean = new org.harctoolbox.guicomponents.AudioParametersBean(properties);
        subformatsPanel = new javax.swing.JPanel();
        exportGenerateParametersCheckBox = new javax.swing.JCheckBox();
        exportGenerateShortCcfCheckBox = new javax.swing.JCheckBox();
        exportGenerateRawCheckBox = new javax.swing.JCheckBox();
        exportGenerateBroadlinkHexCheckBox = new javax.swing.JCheckBox();
        exportGenerateCcfCheckBox = new javax.swing.JCheckBox();
        exportGenerateBroadlinkBase64CheckBox = new javax.swing.JCheckBox();
        exportGenerateSendIrCheckBox = new javax.swing.JCheckBox();
        sendingPanel = new javax.swing.JPanel();
        sendingHardwareTabbedPane = new javax.swing.JTabbedPane();
        globalCachePanel = new javax.swing.JPanel();
        globalCacheIrSenderSelector = new org.harctoolbox.guicomponents.GlobalCacheBean(guiUtils, properties.getVerbose(), properties.getGlobalCacheTimeout());
        sendingGlobalCacheHelpButton = new javax.swing.JButton();
        devLircPanel = new javax.swing.JPanel();
        devLircBean = new org.harctoolbox.guicomponents.DevLircBean(guiUtils, properties.getVerbose(), properties.getDevLircTimeout(), properties.getDevLircName());
        sendingDevLircHardwareHelpButton = new javax.swing.JButton();
        audioPanel = new javax.swing.JPanel();
        transmitAudioBean = new org.harctoolbox.guicomponents.AudioTransmitterBean(guiUtils, properties.getVerbose(), properties.getWaveSampleFrequency(), properties.getWaveChannels(), properties.getWaveSampleSize(), properties.getWaveOmitTrailingGap());
        transmitAudioHelpButton = new javax.swing.JButton();
        girsClientPanel = new javax.swing.JPanel();
        sendingGirsClientHelpButton = new javax.swing.JButton();
        girsTcpSerialComboBean = new org.harctoolbox.guicomponents.GirsClientBean(guiUtils, properties.getVerbose());
        commandFusionSendPanel = new javax.swing.JPanel();
        commandFusionBean = new org.harctoolbox.guicomponents.CommandFusionBean(guiUtils, properties.getVerbose(), properties.getCommandFusionPortName());
        sendingCommandFusionHelpButton = new javax.swing.JButton();
        captureIrWidgetPanel = new javax.swing.JPanel();
        irWidgetBean = new org.harctoolbox.guicomponents.IrWidgetBean(guiUtils, properties.getVerbose(), properties.getCaptureBeginTimeout(), properties.getCaptureMaxSize(), properties.getCaptureEndingTimeout(), properties.getIrWidgetPortName(), properties.getIrWidgetLowerDtrRts());
        irWidgetHelpButton = new javax.swing.JButton();
        noTransmitsComboBox = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        transmitScrutinizedButton = new javax.swing.JButton();
        transmitGenerateButton2 = new javax.swing.JButton();
        sendingHardwareHelpButton = new javax.swing.JButton();
        captureTestButton = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        saveMenu = new javax.swing.JMenu();
        saveCapturedMenu = new javax.swing.JMenu();
        exportSignalGirrMenuItem = new javax.swing.JMenuItem();
        exportSignalIctMenuItem = new javax.swing.JMenuItem();
        signalSignalTextMenuItem = new javax.swing.JMenuItem();
        exportSignalWaveMenuItem = new javax.swing.JMenuItem();
        saveParametrizedMenu = new javax.swing.JMenu();
        exportParametricAsGirrMenuItem = new javax.swing.JMenuItem();
        exportParametricAsTextMenuItem = new javax.swing.JMenuItem();
        saveRawMenu = new javax.swing.JMenu();
        exportRawAsGirrMenuItem = new javax.swing.JMenuItem();
        exportRawAsTextMenuItem = new javax.swing.JMenuItem();
        loadMenu = new javax.swing.JMenu();
        importSignalMenu = new javax.swing.JMenu();
        importSignalAsGirrMenuItem = new javax.swing.JMenuItem();
        importSignalAsIctMenuItem = new javax.swing.JMenuItem();
        importSignalAsWaveMenuItem = new javax.swing.JMenuItem();
        importSignalAsMode2MenuItem = new javax.swing.JMenuItem();
        importParametricMenu = new javax.swing.JMenu();
        importGirrMenuItem1 = new javax.swing.JMenuItem();
        importIctMenuItem1 = new javax.swing.JMenuItem();
        importLircMenuItem1 = new javax.swing.JMenuItem();
        importCcfMenuItem1 = new javax.swing.JMenuItem();
        importXcfMenuItem1 = new javax.swing.JMenuItem();
        importCmlMenuItem = new javax.swing.JMenuItem();
        importCommandFusionMenuItem = new javax.swing.JMenuItem();
        importRawMenu = new javax.swing.JMenu();
        importGirrMenuItem = new javax.swing.JMenuItem();
        importIctMenuItem = new javax.swing.JMenuItem();
        importLircMenuItem = new javax.swing.JMenuItem();
        importCcfMenuItem = new javax.swing.JMenuItem();
        importXcfMenuItem = new javax.swing.JMenuItem();
        importCmlMenuItem2 = new javax.swing.JMenuItem();
        importCommandFusionMenuItem2 = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        savePropertiesMenuItem = new javax.swing.JMenuItem();
        savePropertiesAsMenuItem = new javax.swing.JMenuItem();
        resetPropertiesMenuItem = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        pasteToDataWindowMenuItem = new javax.swing.JMenuItem();
        pasteScrutinizeToDataWindowMenuItem = new javax.swing.JMenuItem();
        saveDataTextAsMenuItem = new javax.swing.JMenuItem();
        copyDataToClipboardMenuItem = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        copyConsoleToClipboardMenuItem = new javax.swing.JMenuItem();
        clearConsoleMenuItem = new javax.swing.JMenuItem();
        saveConsoleTextAsMenuItem = new javax.swing.JMenuItem();
        jSeparator10 = new javax.swing.JPopupMenu.Separator();
        analysisToClipboardMenuItem = new javax.swing.JMenuItem();
        actionsMenu = new javax.swing.JMenu();
        reAnalyzeMenuItem = new javax.swing.JMenuItem();
        undoMenuItem = new javax.swing.JMenuItem();
        startCaptureMenuItem = new javax.swing.JMenuItem();
        jMenuItem12 = new javax.swing.JMenuItem();
        testSignalMenuItem = new javax.swing.JMenuItem();
        plotterResetMenuItem = new javax.swing.JMenuItem();
        clonePlotMenuItem = new javax.swing.JMenuItem();
        clearSignalMenuItem = new javax.swing.JMenuItem();
        openLastExportFileMenuItem = new javax.swing.JMenuItem();
        jSeparator21 = new javax.swing.JPopupMenu.Separator();
        jumpToLastPanelMenuItem = new javax.swing.JMenuItem();
        optionsMenu = new javax.swing.JMenu();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        outputFormatMenu = new javax.swing.JMenu();
        ccfRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        rawRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        rawWithoutSignsRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        analyzerBasisMenu = new javax.swing.JMenu();
        analyzerBase2RadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        analyzerBase4RadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        analyzerBase8RadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        analyzerBase10RadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        analyzerBase16RadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        lafMenu = new javax.swing.JMenu();
        usePopupsMenu = new javax.swing.JMenu();
        usePopupsForHelpCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        usePopupsForErrorsCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator16 = new javax.swing.JPopupMenu.Separator();
        timeoutMenu = new javax.swing.JMenu();
        startTimeoutMenuItem = new javax.swing.JMenuItem();
        lengthMenuItem = new javax.swing.JMenuItem();
        endingTimeoutMenuItem = new javax.swing.JMenuItem();
        jSeparator15 = new javax.swing.JPopupMenu.Separator();
        sendingTimeoutMenuItem = new javax.swing.JMenuItem();
        jSeparator26 = new javax.swing.JPopupMenu.Separator();
        globalCacheTimeoutMenuItem = new javax.swing.JMenuItem();
        protocolParametersMenu = new javax.swing.JMenu();
        absToleranceMenuItem = new javax.swing.JMenuItem();
        relToleranceMenuItem = new javax.swing.JMenuItem();
        frequencyToleranceMenuItem = new javax.swing.JMenuItem();
        minLeadoutMenuItem = new javax.swing.JMenuItem();
        minRepeatGapMenuItem = new javax.swing.JMenuItem();
        dummyGapMenuItem = new javax.swing.JMenuItem();
        chopThresholdMenuItem = new javax.swing.JMenuItem();
        fallbackFrequencyMenuItem = new javax.swing.JMenuItem();
        verboseCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        ignoreEndingSilenceCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        repeatFinderCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        cleanerCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        invokeAnalyzerCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        printAnalyzeIRPsToConsoleCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        printDecodesToConsoleCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        printAlternativeDecodesCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        parametrizedLearnIgnoreTCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        removeDefaultedParametersCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        ignoreLeadingGarbageCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        irpProtocolsIniMenu = new javax.swing.JMenu();
        irpProtocolsSelectMenuItem = new javax.swing.JMenuItem();
        irpProtocolsEditMenuItem = new javax.swing.JMenuItem();
        jSeparator45 = new javax.swing.JPopupMenu.Separator();
        secondaryIrpProtocolsSelectMenuItem = new javax.swing.JMenuItem();
        secondaryIrpProtocolsEditMenuItem = new javax.swing.JMenuItem();
        jSeparator46 = new javax.swing.JPopupMenu.Separator();
        irpProtocolsReloadMenuItem = new javax.swing.JMenuItem();
        exportFormatsMenu = new javax.swing.JMenu();
        exportFormatsEditMenuItem = new javax.swing.JMenuItem();
        exportFormatsSelectMenuItem = new javax.swing.JMenuItem();
        jSeparator47 = new javax.swing.JPopupMenu.Separator();
        secondaryExportFormatsEditMenuItem = new javax.swing.JMenuItem();
        secondaryExportFormatsSelectMenuItem = new javax.swing.JMenuItem();
        jSeparator48 = new javax.swing.JPopupMenu.Separator();
        exportFormatsReloadMenuItem = new javax.swing.JMenuItem();
        jSeparator20 = new javax.swing.JPopupMenu.Separator();
        importOptionsMenu = new javax.swing.JMenu();
        importCharsetMenuItem = new javax.swing.JMenuItem();
        openZipFilesCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        invokeDecoderCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        generateRawCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        generateCcfCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        translateProntoFontCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        girrValidateCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        girrSchemaLocationMenuItem = new javax.swing.JMenuItem();
        girrAcceptEmptyCommandsCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        exportOptionsMenu = new javax.swing.JMenu();
        exportCharsetMenuItem = new javax.swing.JMenuItem();
        creatingUserMenuItem = new javax.swing.JMenuItem();
        inquiryDeviceDataCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator22 = new javax.swing.JPopupMenu.Separator();
        debugMenu = new javax.swing.JMenu();
        offerStackTraceCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        debugCodeMenuItem = new javax.swing.JMenuItem();
        debugDecodeProtocolRegexpMenuItem = new javax.swing.JMenuItem();
        toolsMenu = new javax.swing.JMenu();
        hexCalcMenuItem = new javax.swing.JMenuItem();
        timeFrequencyCalcMenuItem = new javax.swing.JMenuItem();
        beaconListenerMenuItem = new javax.swing.JMenuItem();
        NamedCommandMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        publicKeyMenuItem = new javax.swing.JMenuItem();
        jSeparator41 = new javax.swing.JPopupMenu.Separator();
        checkUpToDateMenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();
        jSeparator31 = new javax.swing.JPopupMenu.Separator();
        mainDocuMenuItem = new javax.swing.JMenuItem();
        irpTransmogrifierHelpMenuItem = new javax.swing.JMenuItem();
        glossaryMenuItem = new javax.swing.JMenuItem();
        jSeparator32 = new javax.swing.JPopupMenu.Separator();
        tutorialMenuItem = new javax.swing.JMenuItem();
        homePageMenuItem = new javax.swing.JMenuItem();
        jSeparator33 = new javax.swing.JPopupMenu.Separator();
        releaseNotesMenuItem = new javax.swing.JMenuItem();
        jSeparator34 = new javax.swing.JPopupMenu.Separator();
        protocolSpecMenuItem = new javax.swing.JMenuItem();
        jSeparator35 = new javax.swing.JPopupMenu.Separator();
        downloadsMenuItem = new javax.swing.JMenuItem();
        gitMenuItem = new javax.swing.JMenuItem();
        disussionsMenuItem = new javax.swing.JMenuItem();
        issuesMenuItem = new javax.swing.JMenuItem();

        rawCodeClearMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/eraser.png"))); // NOI18N
        rawCodeClearMenuItem.setText("Clear");
        rawCodeClearMenuItem.setToolTipText("Clean this area");
        rawCodeClearMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeClearMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodeClearMenuItem);

        signalClearMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/delete.png"))); // NOI18N
        signalClearMenuItem.setText("Delete signal");
        signalClearMenuItem.setToolTipText("Clear this window, the analysis window, and the plot.");
        signalClearMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                signalClearMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(signalClearMenuItem);
        CCFCodePopupMenu.add(jSeparator8);

        rawCodeCopyMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/editcopy.png"))); // NOI18N
        rawCodeCopyMenuItem.setText("Copy selection");
        rawCodeCopyMenuItem.setToolTipText("Copy current selection to the clipboard");
        rawCodeCopyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeCopyMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodeCopyMenuItem);

        rawCodeCopyAllMenuItem.setText("Copy all");
        rawCodeCopyAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeCopyAllMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodeCopyAllMenuItem);

        rawCodePasteReplacingMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/editpaste.png"))); // NOI18N
        rawCodePasteReplacingMenuItem.setText("Paste (replacing)");
        rawCodePasteReplacingMenuItem.setToolTipText("Paste from clipboard, replacing previous contents.");
        rawCodePasteReplacingMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodePasteReplacingMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodePasteReplacingMenuItem);

        rawCodePasteMenuItem.setMnemonic('P');
        rawCodePasteMenuItem.setText("Paste");
        rawCodePasteMenuItem.setToolTipText("Paste content of clipboard at the position of the caret.");
        rawCodePasteMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodePasteMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodePasteMenuItem);

        rawCodePasteAnalyzeMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/cache.png"))); // NOI18N
        rawCodePasteAnalyzeMenuItem.setText("Paste & Scrutinize");
        rawCodePasteAnalyzeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodePasteAnalyzeMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodePasteAnalyzeMenuItem);

        rawCodeSelectAllMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/ark_selectall.png"))); // NOI18N
        rawCodeSelectAllMenuItem.setText("Select all");
        rawCodeSelectAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeSelectAllMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodeSelectAllMenuItem);
        CCFCodePopupMenu.add(jSeparator23);

        rawAddTestSignalMenuItem.setText("Enter test signal (NEC1 12.34 56)");
        rawAddTestSignalMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawAddTestSignalMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawAddTestSignalMenuItem);
        CCFCodePopupMenu.add(jSeparator36);

        rawCodeAnalyzeMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/gear.png"))); // NOI18N
        rawCodeAnalyzeMenuItem.setText("Scrutinize");
        rawCodeAnalyzeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeAnalyzeMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodeAnalyzeMenuItem);

        scrutinizeSignalProtocolDocuMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/info.png"))); // NOI18N
        scrutinizeSignalProtocolDocuMenuItem.setText("Docu for protocol");
        scrutinizeSignalProtocolDocuMenuItem.setToolTipText("Create a popup containing documentation for the current protocol.");
        scrutinizeSignalProtocolDocuMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scrutinizeSignalProtocolDocuMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(scrutinizeSignalProtocolDocuMenuItem);
        CCFCodePopupMenu.add(jSeparator9);

        undoDataMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/undo.png"))); // NOI18N
        undoDataMenuItem.setText("Undo Scrutinize Data");
        undoDataMenuItem.setToolTipText("Undo last change to the scrutinize data.");
        undoDataMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                undoDataMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(undoDataMenuItem);
        CCFCodePopupMenu.add(jSeparator30);

        rawCodeSaveMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/filesaveas.png"))); // NOI18N
        rawCodeSaveMenuItem.setText("Save as text...");
        rawCodeSaveMenuItem.setToolTipText("Save current content of the data window to a text file.");
        rawCodeSaveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeSaveMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodeSaveMenuItem);

        importCaptureMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/fileimport.png"))); // NOI18N
        importCaptureMenu.setText("Import");

        importSignalAsGirrMenuItem1.setText("Girr");
        importSignalAsGirrMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importSignalAsGirrMenuItemActionPerformed(evt);
            }
        });
        importCaptureMenu.add(importSignalAsGirrMenuItem1);

        importSignalAsIctMenuItem1.setText("IrScope ICT");
        importSignalAsIctMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importSignalAsIctMenuItemActionPerformed(evt);
            }
        });
        importCaptureMenu.add(importSignalAsIctMenuItem1);

        importSignalAsWaveMenuItem1.setText("Wave");
        importSignalAsWaveMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importSignalAsWaveMenuItemActionPerformed(evt);
            }
        });
        importCaptureMenu.add(importSignalAsWaveMenuItem1);

        importSignalAsMode2MenuItem1.setText("Mode2");
        importSignalAsMode2MenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importSignalAsMode2MenuItem1importSignalAsWaveMenuItemActionPerformed(evt);
            }
        });
        importCaptureMenu.add(importSignalAsMode2MenuItem1);

        CCFCodePopupMenu.add(importCaptureMenu);

        exportCapturedMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/fileexport.png"))); // NOI18N
        exportCapturedMenu.setText("Export");

        exportSignalGirrMenuItem1.setText("Girr");
        exportSignalGirrMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSignalGirrMenuItemActionPerformed(evt);
            }
        });
        exportCapturedMenu.add(exportSignalGirrMenuItem1);

        exportSignalIctMenuItem1.setText("IrScope ICT");
        exportSignalIctMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSignalIctMenuItemActionPerformed(evt);
            }
        });
        exportCapturedMenu.add(exportSignalIctMenuItem1);

        signalSignalTextMenuItem1.setText("Text");
        signalSignalTextMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                signalSignalTextMenuItemActionPerformed(evt);
            }
        });
        exportCapturedMenu.add(signalSignalTextMenuItem1);

        exportSignalWaveMenuItem1.setText("Wave");
        exportSignalWaveMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSignalWaveMenuItemActionPerformed(evt);
            }
        });
        exportCapturedMenu.add(exportSignalWaveMenuItem1);

        CCFCodePopupMenu.add(exportCapturedMenu);

        CCFCodePopupMenu.add(importSignalMenu);

        parametricSorterCheckBoxMenuItem.setSelected(properties.getSorterOnParametrizedTable());
        parametricSorterCheckBoxMenuItem.setText("Enable sorter");
        parametricSorterCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parametricSorterCheckBoxMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(parametricSorterCheckBoxMenuItem);
        parameterTablePopupMenu.add(jSeparator24);

        moveUpMenuItem1.setMnemonic('U');
        moveUpMenuItem1.setText("Move Up");
        moveUpMenuItem1.setEnabled(!properties.getSorterOnParametrizedTable());
        moveUpMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveUpMenuItem1ActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(moveUpMenuItem1);

        moveDownMenuItem1.setMnemonic('D');
        moveDownMenuItem1.setText("Move Down");
        moveDownMenuItem1.setEnabled(!properties.getSorterOnParametrizedTable());
        moveDownMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveDownMenuItem1ActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(moveDownMenuItem1);
        parameterTablePopupMenu.add(jSeparator17);

        addEmptyParametrizedSignalMenuItem.setText("Add empty signal");
        addEmptyParametrizedSignalMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addEmptyParametrizedSignalMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(addEmptyParametrizedSignalMenuItem);

        addParametrizedTestSignalMenuItem.setText("Add Test Signal (NEC1 12.34 56)");
        addParametrizedTestSignalMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addParametrizedTestSignalMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(addParametrizedTestSignalMenuItem);
        parameterTablePopupMenu.add(jSeparator13);

        scrutinizeParametricMenuItem.setText("Copy selected to \"Scrutinize Signal\"");
        scrutinizeParametricMenuItem.setToolTipText("Copy the (single) selected signal to the Scrutinize signal pane.");
        scrutinizeParametricMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scrutinizeParametricMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(scrutinizeParametricMenuItem);

        transmitMenuItem.setMnemonic('T');
        transmitMenuItem.setText("Transmit selected");
        transmitMenuItem.setToolTipText("Transmit selected signal using selected sendiing hardware");
        transmitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transmitMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(transmitMenuItem);

        protocolDocuMenuItem.setText("Protocol documentation for selected");
        protocolDocuMenuItem.setToolTipText("Generate a popup containing protocol documentation for selected signal.");
        protocolDocuMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolDocuMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(protocolDocuMenuItem);
        parameterTablePopupMenu.add(jSeparator37);

        duplicateParametricMenuItem.setText("Duplicate selected");
        duplicateParametricMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                duplicateParametricMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(duplicateParametricMenuItem);

        deleteMenuItem1.setText("Delete selected");
        deleteMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteMenuItem1ActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(deleteMenuItem1);

        exportSelectedCookedMenuItem.setText("Export selected");
        exportSelectedCookedMenuItem.setToolTipText("Export the selected signal in selected format,");
        exportSelectedCookedMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSelectedCookedMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(exportSelectedCookedMenuItem);

        printTableRowMenuItem.setText("Print selected to console");
        printTableRowMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printTableRowMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(printTableRowMenuItem);

        clearSelectionParametricMenuItem.setText("Clear selection");
        clearSelectionParametricMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearSelectionParametricMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(clearSelectionParametricMenuItem);
        parameterTablePopupMenu.add(jSeparator14);

        deleteAllMenuItem.setText("Delete all");
        deleteAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteAllMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(deleteAllMenuItem);

        checkParametrizedSignalsMenuItem.setText("Check commands");
        checkParametrizedSignalsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkParametrizedSignalsMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(checkParametrizedSignalsMenuItem);

        parameterUniquefyMenuItem.setText("Make names unique");
        parameterUniquefyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parameterUniquefyMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(parameterUniquefyMenuItem);

        exportCookedMenuItem.setMnemonic('E');
        exportCookedMenuItem.setText("Export all");
        exportCookedMenuItem.setToolTipText("Export all signals in the selected format");
        exportCookedMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportCookedMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(exportCookedMenuItem);
        parameterTablePopupMenu.add(jSeparator19);

        parametrizedCopyAllMenuItem.setText("Copy all to clipboard");
        parametrizedCopyAllMenuItem.setToolTipText("");
        parametrizedCopyAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parametrizedCopyAllMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(parametrizedCopyAllMenuItem);

        parametrizedCopySelectionMenuItem.setText("Copy selected to clipboard");
        parametrizedCopySelectionMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parametrizedCopySelectionMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(parametrizedCopySelectionMenuItem);
        parameterTablePopupMenu.add(jSeparator27);

        searchParametrizedMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        searchParametrizedMenuItem.setText("Search name...");
        searchParametrizedMenuItem.setToolTipText("Selects all commands containing a particular string");
        searchParametrizedMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchParametrizedMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(searchParametrizedMenuItem);
        parameterTablePopupMenu.add(jSeparator43);

        parametrizedPasteColumnMenuItem.setText("Paste in column");
        parametrizedPasteColumnMenuItem.setToolTipText("Paste clipboard into column, starting with selected row");
        parametrizedPasteColumnMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parametrizedPasteColumnMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(parametrizedPasteColumnMenuItem);
        parameterTablePopupMenu.add(jSeparator40);

        hideColumnMenuItem1.setText("Hide selected column");
        hideColumnMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hideColumnMenuItem1ActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(hideColumnMenuItem1);

        resetRawTableColumnsMenuItem1.setText("Reset columns");
        resetRawTableColumnsMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetRawTableColumnsMenuItem1ActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(resetRawTableColumnsMenuItem1);

        hideUnusedMenuItem.setText("Hide unused columns");
        hideUnusedMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hideUnusedMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(hideUnusedMenuItem);

        hideUninterestingColumnsMenuItem.setText("Hide uninteresting columns");
        hideUninterestingColumnsMenuItem.setToolTipText("Hide the columns #, Date, as well as all columns with identical content.");
        hideUninterestingColumnsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hideUninterestingColumnsMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(hideUninterestingColumnsMenuItem);

        deleteParametrizedCommentMenuItem.setText("Delete all comments");
        deleteParametrizedCommentMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteParametrizedCommentMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(deleteParametrizedCommentMenuItem);

        parametrizedAdvancedMenu.setText("Advanced");

        setProtocolMenuItem.setText("Set protocol...");
        setProtocolMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setProtocolMenuItemActionPerformed(evt);
            }
        });
        parametrizedAdvancedMenu.add(setProtocolMenuItem);

        setDMenuItem.setText("Set D (deviceno) ...");
        setDMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setDMenuItemActionPerformed(evt);
            }
        });
        parametrizedAdvancedMenu.add(setDMenuItem);

        setSMenuItem.setText("Set S (subdevice)...");
        setSMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setSMenuItemActionPerformed(evt);
            }
        });
        parametrizedAdvancedMenu.add(setSMenuItem);

        setFMenuItem.setText("Set F...");
        setFMenuItem.setToolTipText("Set the F parameter for all commands");
        setFMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setFMenuItemActionPerformed(evt);
            }
        });
        parametrizedAdvancedMenu.add(setFMenuItem);

        setTMenuItem.setText("Set T...");
        setTMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setTMenuItemActionPerformed(evt);
            }
        });
        parametrizedAdvancedMenu.add(setTMenuItem);

        unsetTMenuItem.setText("Unset T");
        unsetTMenuItem.setToolTipText("Remove the setting of T");
        unsetTMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unsetTMenuItemActionPerformed(evt);
            }
        });
        parametrizedAdvancedMenu.add(unsetTMenuItem);

        setMiscParamsMenuItem.setText("Set \"Misc. Params\"");
        setMiscParamsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setMiscParamsMenuItemActionPerformed(evt);
            }
        });
        parametrizedAdvancedMenu.add(setMiscParamsMenuItem);

        transformNamesMenu.setText("Name transformations");
        transformNamesMenu.setToolTipText("Transformations on the command names.");

        lowercaseNameMenuItem.setText("To lowercase");
        lowercaseNameMenuItem.setToolTipText("Transform names to lower case");
        lowercaseNameMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lowercaseNameMenuItemActionPerformed(evt);
            }
        });
        transformNamesMenu.add(lowercaseNameMenuItem);

        upperCaseMenuItem.setText("To uppercase");
        upperCaseMenuItem.setToolTipText("Transform names to upper case");
        upperCaseMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upperCaseMenuItemActionPerformed(evt);
            }
        });
        transformNamesMenu.add(upperCaseMenuItem);

        captitalizeNameMenuItem.setText("Capitalize names");
        captitalizeNameMenuItem.setToolTipText("Transform names to Capitalized form.");
        captitalizeNameMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                captitalizeNameMenuItemActionPerformed(evt);
            }
        });
        transformNamesMenu.add(captitalizeNameMenuItem);
        transformNamesMenu.add(jSeparator42);

        addNamePrefixMenuItem.setText("Add prefix...");
        addNamePrefixMenuItem.setToolTipText("Add a prefix to all names.");
        addNamePrefixMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addNamePrefixMenuItemActionPerformed(evt);
            }
        });
        transformNamesMenu.add(addNamePrefixMenuItem);

        transformNameMenuItem.setText("Replace...");
        transformNameMenuItem.setToolTipText("Replace a substring (regular expression) in command names.");
        transformNameMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transformNameMenuItemActionPerformed(evt);
            }
        });
        transformNamesMenu.add(transformNameMenuItem);

        parametrizedAdvancedMenu.add(transformNamesMenu);
        parametrizedAdvancedMenu.add(jSeparator4);

        addMissingFsMenuItem.setText("Add missing Fs");
        addMissingFsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addMissingFsMenuItemActionPerformed(evt);
            }
        });
        parametrizedAdvancedMenu.add(addMissingFsMenuItem);

        addMissingNamesMenuItem.setText("Add missing names");
        addMissingNamesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addMissingNamesMenuItemActionPerformed(evt);
            }
        });
        parametrizedAdvancedMenu.add(addMissingNamesMenuItem);

        deleteDefaultedSignalsMenuItem.setText("Delete default-named signals");
        deleteDefaultedSignalsMenuItem.setToolTipText("Delete signals which were added by \"Add missing Fs\" and not changed.");
        deleteDefaultedSignalsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteDefaultedSignalsMenuItemActionPerformed(evt);
            }
        });
        parametrizedAdvancedMenu.add(deleteDefaultedSignalsMenuItem);

        deleteEmptyParametrizedSignalsMenuItem.setText("Delete empty Signals");
        deleteEmptyParametrizedSignalsMenuItem.setToolTipText("Remove all signals that do not have content.");
        deleteEmptyParametrizedSignalsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteEmptyParametrizedSignalsMenuItemActionPerformed(evt);
            }
        });
        parametrizedAdvancedMenu.add(deleteEmptyParametrizedSignalsMenuItem);

        parameterTablePopupMenu.add(parametrizedAdvancedMenu);

        rawSorterCheckBoxMenuItem.setSelected(properties.getSorterOnRawTable());
        rawSorterCheckBoxMenuItem.setText("Enable sorter");
        rawSorterCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawSorterCheckBoxMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(rawSorterCheckBoxMenuItem);
        rawTablePopupMenu.add(jSeparator25);

        moveUpMenuItem.setMnemonic('U');
        moveUpMenuItem.setText("Move Up");
        moveUpMenuItem.setEnabled(!properties.getSorterOnRawTable());
        moveUpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveUpMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(moveUpMenuItem);

        moveDownMenuItem.setMnemonic('D');
        moveDownMenuItem.setText("Move Down");
        moveDownMenuItem.setEnabled(!properties.getSorterOnRawTable());
        moveDownMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveDownMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(moveDownMenuItem);
        rawTablePopupMenu.add(jSeparator11);

        addEmptyRawMenuItem.setText("Add Empty Signal");
        addEmptyRawMenuItem.setToolTipText("Add an empty row");
        addEmptyRawMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addEmptyRawMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(addEmptyRawMenuItem);

        addRawTestSignalMenuItem.setText("Add Test Signal (NEC1 12.34 56)");
        addRawTestSignalMenuItem.setToolTipText("Add a standard signal");
        addRawTestSignalMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addRawTestSignalMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(addRawTestSignalMenuItem);

        rawFromClipboardMenuItem.setText("Import signal from clipboard");
        rawFromClipboardMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawFromClipboardMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(rawFromClipboardMenuItem);
        rawTablePopupMenu.add(jSeparator18);

        rawChopMenuItem.setText("Chop selected signals at long gaps");
        rawChopMenuItem.setToolTipText("Chop selected signals and enter the fragments as new signals");
        rawChopMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawChopMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(rawChopMenuItem);

        scrutinizeMenuItem.setText("Copy selected to \"Scrutinize Signal\"");
        scrutinizeMenuItem.setToolTipText("Copy the (single) selected signal to the \"Scrutinize Signal\" pane.");
        scrutinizeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scrutinizeMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(scrutinizeMenuItem);

        sendMenuItem.setMnemonic('T');
        sendMenuItem.setText("Transmit selected");
        sendMenuItem.setToolTipText("Transmit signal using selected hardware.");
        sendMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(sendMenuItem);

        rawProtocolDocuMenuItem.setMnemonic('C');
        rawProtocolDocuMenuItem.setText("Protocol documentation for selected");
        rawProtocolDocuMenuItem.setToolTipText("Create popup containg documentation for the protocol of the selected signal.");
        rawProtocolDocuMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawProtocolDocuMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(rawProtocolDocuMenuItem);
        rawTablePopupMenu.add(jSeparator38);

        duplicateRawMenuItem.setText("Duplicate Selected");
        duplicateRawMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                duplicateRawMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(duplicateRawMenuItem);

        deleteMenuItem.setText("Delete selected");
        deleteMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(deleteMenuItem);

        saveSelectedRawTableRowMenuItem.setText("Export selected");
        saveSelectedRawTableRowMenuItem.setToolTipText("Export selected signal to selected format.");
        saveSelectedRawTableRowMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveSelectedRawTableRowMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(saveSelectedRawTableRowMenuItem);

        printRawTableRowMenuItem.setText("Print selected to console");
        printRawTableRowMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printRawTableRowMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(printRawTableRowMenuItem);

        copyToParametrizedMenuItem.setText("Copy selection to Parametric Remote");
        copyToParametrizedMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyToParametrizedMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(copyToParametrizedMenuItem);

        clearSelectionRawMenuItem.setText("Clear selection");
        clearSelectionRawMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearSelectionRawMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(clearSelectionRawMenuItem);
        rawTablePopupMenu.add(jSeparator12);

        deleteAllRawsMenuItem.setText("Delete all");
        deleteAllRawsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteAllRawsMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(deleteAllRawsMenuItem);

        checkRawCommandsMenuItem.setText("Check commands");
        checkRawCommandsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkRawCommandsMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(checkRawCommandsMenuItem);

        rawUniquefyMenuItem.setText("Make names unique");
        rawUniquefyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawUniquefyMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(rawUniquefyMenuItem);

        saveRawMenuItem.setMnemonic('E');
        saveRawMenuItem.setText("Export");
        saveRawMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveRawMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(saveRawMenuItem);
        rawTablePopupMenu.add(jSeparator28);

        rawCopyAllMenuItem.setText("Copy all to clipboard");
        rawCopyAllMenuItem.setToolTipText("");
        rawCopyAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCopyAllMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(rawCopyAllMenuItem);

        rawCopySelectionMenuItem.setText("Copy selected to clipboard");
        rawCopySelectionMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCopySelectionMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(rawCopySelectionMenuItem);
        rawTablePopupMenu.add(jSeparator44);

        searchRawMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        searchRawMenuItem.setText("Search name...");
        searchRawMenuItem.setToolTipText("Select all commands which names contain a given string");
        searchRawMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchRawMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(searchRawMenuItem);
        rawTablePopupMenu.add(jSeparator39);

        rawPasteColumnMenuItem.setText("Paste in column");
        rawPasteColumnMenuItem.setToolTipText("Paste clipboard into column, starting with selected row");
        rawPasteColumnMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawPasteColumnMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(rawPasteColumnMenuItem);
        rawTablePopupMenu.add(jSeparator29);

        hideColumnMenuItem.setText("Hide selected column");
        hideColumnMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hideColumnMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(hideColumnMenuItem);

        resetRawTableColumnsMenuItem.setText("Reset columns");
        resetRawTableColumnsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetRawTableColumnsMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(resetRawTableColumnsMenuItem);

        removeUnusedMenuItem1.setText("Hide unused columns");
        removeUnusedMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeUnusedMenuItem1ActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(removeUnusedMenuItem1);

        hideUninterestingColumnsMenuItem1.setText("Hide uninteresting columns");
        hideUninterestingColumnsMenuItem1.setToolTipText("Hide the columns #, Date, as well as all columns with identical content.");
        hideUninterestingColumnsMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hideUninterestingColumnsMenuItem1ActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(hideUninterestingColumnsMenuItem1);

        deleteRawCommentMenuItem.setText("Delete all comments");
        deleteRawCommentMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteRawCommentMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(deleteRawCommentMenuItem);

        deleteEmptyRawSignalsMenuItem.setText("Delete empty Signals");
        deleteEmptyRawSignalsMenuItem.setToolTipText("Remove all signals that do not have a content.");
        deleteEmptyRawSignalsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteEmptyRawSignalsMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(deleteEmptyRawSignalsMenuItem);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);

        topLevelSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        console.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        topLevelSplitPane.setBottomComponent(console);

        topLevelTabbedPane.setToolTipText("This tabbed pane selects between different use cases.");
        topLevelTabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                topLevelTabbedPaneStateChanged(evt);
            }
        });

        signalScrutinizerPanel.setToolTipText("This panel is devoted to the use case of capturing and analyzing ONE infrared signal.");
        signalScrutinizerPanel.setPreferredSize(new java.awt.Dimension(1016, 300));

        capturedDataTextArea.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        capturedDataTextArea.setColumns(20);
        capturedDataTextArea.setLineWrap(true);
        capturedDataTextArea.setToolTipText("This is the data window, where the captured data goes. It may be edited. Press right mouse button for a menu.");
        capturedDataTextArea.setWrapStyleWord(true);
        capturedDataTextArea.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                capturedDataTextAreaMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                capturedDataTextAreaMouseReleased(evt);
            }
        });
        capturedDataScrollPane.setViewportView(capturedDataTextArea);

        infoPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel3.setText("Decode:");

        jLabel4.setText("IRP:");

        jLabel7.setText("Intro length:");

        introLengthLabel.setText("000");

        jLabel9.setText("Rep. length:");

        repLengthLabel.setText("000");

        jLabel11.setText("# reps.:");

        noRepsLabel.setText("00");

        jLabel13.setText("Ending length:");

        endingLengthLabel.setText("000");

        startButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/mix_record.png"))); // NOI18N
        startButton.setText("Capture");
        startButton.setToolTipText("Reads an IR signal from the hardware configured in the Capturing Hardware tab");
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        jLabel1.setText("Frequency:");

        frequencyLabel.setText("-");

        signalExportButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/fileexport.png"))); // NOI18N
        signalExportButton.setText("Export");
        signalExportButton.setToolTipText("Export the content of the data window to a selected export format.");
        signalExportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                signalExportButtonActionPerformed(evt);
            }
        });

        analyzerTextField.setEditable(false);
        analyzerTextField.setComponentPopupMenu(copyPopupMenu);

        decodeIRTextField.setEditable(false);
        decodeIRTextField.setToolTipText("Result from decoding.");
        decodeIRTextField.setComponentPopupMenu(copyPopupMenu);

        transmitSignalButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/1rightarrow.png"))); // NOI18N
        transmitSignalButton.setText("Transmit");
        transmitSignalButton.setToolTipText("Send the IR signal defined by the data window to the selected sending hardware");
        transmitSignalButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transmitSignalButtonActionPerformed(evt);
            }
        });

        scrutinizeSignalHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        scrutinizeSignalHelpButton.setText("Help");
        scrutinizeSignalHelpButton.setToolTipText("Show help for current panel");
        scrutinizeSignalHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scrutinizeSignalHelpButtonActionPerformed(evt);
            }
        });

        transmitSignalButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/gear.png"))); // NOI18N
        transmitSignalButton1.setText("Scrutinize");
        transmitSignalButton1.setToolTipText("(Re-)analyze the content of the data window");
        transmitSignalButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transmitSignalButton1ActionPerformed(evt);
            }
        });

        pasteAnalyzeButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/launch.png"))); // NOI18N
        pasteAnalyzeButton.setText("Paste & Scr.");
        pasteAnalyzeButton.setToolTipText("Paste the clipboard content to the data window, then analyze it.");
        pasteAnalyzeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pasteAnalyzeButtonActionPerformed(evt);
            }
        });

        continuousCaptureButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/reload.png"))); // NOI18N
        continuousCaptureButton.setText("Capt. (cont.)");
        continuousCaptureButton.setToolTipText("Reads an IR signal from the hardware configured in the Capturing Hardware tab");
        continuousCaptureButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                continuousCaptureButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout infoPanelLayout = new javax.swing.GroupLayout(infoPanel);
        infoPanel.setLayout(infoPanelLayout);
        infoPanelLayout.setHorizontalGroup(
            infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(infoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(infoPanelLayout.createSequentialGroup()
                        .addGroup(infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4)
                            .addComponent(jLabel7))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, infoPanelLayout.createSequentialGroup()
                                .addComponent(introLengthLabel)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(repLengthLabel)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel11)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(noRepsLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel13)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(endingLengthLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(frequencyLabel)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, infoPanelLayout.createSequentialGroup()
                                .addGroup(infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(analyzerTextField, javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(decodeIRTextField))
                                .addContainerGap())))
                    .addGroup(infoPanelLayout.createSequentialGroup()
                        .addComponent(startButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(continuousCaptureButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(transmitSignalButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(pasteAnalyzeButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(transmitSignalButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(signalExportButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 34, Short.MAX_VALUE)
                        .addComponent(scrutinizeSignalHelpButton, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );

        infoPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel3, jLabel4});

        infoPanelLayout.setVerticalGroup(
            infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(infoPanelLayout.createSequentialGroup()
                .addGroup(infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(decodeIRTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(analyzerTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(introLengthLabel)
                    .addComponent(jLabel9)
                    .addComponent(repLengthLabel)
                    .addComponent(jLabel11)
                    .addComponent(noRepsLabel)
                    .addComponent(jLabel13)
                    .addComponent(endingLengthLabel)
                    .addComponent(jLabel1)
                    .addComponent(frequencyLabel))
                .addGap(12, 12, 12)
                .addGroup(infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(startButton)
                    .addComponent(scrutinizeSignalHelpButton)
                    .addComponent(transmitSignalButton1)
                    .addComponent(transmitSignalButton)
                    .addComponent(signalExportButton)
                    .addComponent(pasteAnalyzeButton)
                    .addComponent(continuousCaptureButton))
                .addGap(5, 5, 5))
        );

        infoPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jLabel3, jLabel4});

        plotScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        irPlotter.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        irPlotter.setToolTipText("Plot of IR signal above. Press right mouse button for a menu, mouse wheel to zoom, left mouse buttol + drag to zoom selection.");
        irPlotter.setAutoscrolls(true);
        irPlotter.setPreferredSize(new java.awt.Dimension(749, 100));

        javax.swing.GroupLayout irPlotterLayout = new javax.swing.GroupLayout(irPlotter);
        irPlotter.setLayout(irPlotterLayout);
        irPlotterLayout.setHorizontalGroup(
            irPlotterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 984, Short.MAX_VALUE)
        );
        irPlotterLayout.setVerticalGroup(
            irPlotterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 124, Short.MAX_VALUE)
        );

        plotScrollPane.setViewportView(irPlotter);

        javax.swing.GroupLayout signalScrutinizerPanelLayout = new javax.swing.GroupLayout(signalScrutinizerPanel);
        signalScrutinizerPanel.setLayout(signalScrutinizerPanelLayout);
        signalScrutinizerPanelLayout.setHorizontalGroup(
            signalScrutinizerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(plotScrollPane)
            .addComponent(capturedDataScrollPane)
            .addComponent(infoPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        signalScrutinizerPanelLayout.setVerticalGroup(
            signalScrutinizerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(signalScrutinizerPanelLayout.createSequentialGroup()
                .addComponent(capturedDataScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 144, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(infoPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(plotScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        topLevelTabbedPane.addTab("Scrutinize signal", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/search.png")), signalScrutinizerPanel); // NOI18N

        remoteScrutinizerPanel.setToolTipText("This panel handles the case of several signals and saving them as a unit.");

        rawCookedTabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rawCookedTabbedPaneStateChanged(evt);
            }
        });

        parameterTableScrollPane.setToolTipText("Girr files can be directly imported by dropping them here.");
        parameterTableScrollPane.setComponentPopupMenu(parameterTablePopupMenu);

        parameterTable.setModel(parameterTableModel);
        parameterTable.setToolTipText("Press right button for a menu of table actions, middle to transmit signal under mouse.");
        parameterTable.setInheritsPopupMenu(true);
        parameterTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                parameterTableMouseReleased(evt);
            }
        });
        parameterTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                parameterTableKeyReleased(evt);
            }
        });
        parameterTableScrollPane.setViewportView(parameterTable);
        parameterTable.setColumnModel(parameterTableColumnModel);

        javax.swing.GroupLayout cookedPanelLayout = new javax.swing.GroupLayout(cookedPanel);
        cookedPanel.setLayout(cookedPanelLayout);
        cookedPanelLayout.setHorizontalGroup(
            cookedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(parameterTableScrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 988, Short.MAX_VALUE)
        );
        cookedPanelLayout.setVerticalGroup(
            cookedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(parameterTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE)
        );

        rawCookedTabbedPane.addTab("Parametric Remote", cookedPanel);

        rawTableScrollPane.setToolTipText("Girr files can be directly imported by dropping them here.");
        rawTableScrollPane.setAutoscrolls(true);
        rawTableScrollPane.setComponentPopupMenu(rawTablePopupMenu);

        rawTable.setModel(rawTableModel);
        rawTable.setToolTipText("Press right button for a menu of table actions.");
        rawTable.setColumnSelectionAllowed(true);
        rawTable.setInheritsPopupMenu(true);
        rawTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                rawTableMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                rawTableMouseReleased(evt);
            }
        });
        rawTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                rawTableKeyReleased(evt);
            }
        });
        rawTableScrollPane.setViewportView(rawTable);
        rawTable.setColumnModel(this.rawTableColumnModel);

        javax.swing.GroupLayout rawPanelLayout = new javax.swing.GroupLayout(rawPanel);
        rawPanel.setLayout(rawPanelLayout);
        rawPanelLayout.setHorizontalGroup(
            rawPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 988, Short.MAX_VALUE)
            .addGroup(rawPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(rawTableScrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 988, Short.MAX_VALUE))
        );
        rawPanelLayout.setVerticalGroup(
            rawPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 323, Short.MAX_VALUE)
            .addGroup(rawPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(rawTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE))
        );

        rawCookedTabbedPane.addTab("Raw Remote", rawPanel);

        rawCookedTabbedPane.setSelectedIndex(properties.getSelectedRemoteIndex());

        startStopToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/mix_record.png"))); // NOI18N
        startStopToggleButton.setMnemonic('C');
        startStopToggleButton.setText("Capture");
        startStopToggleButton.setToolTipText("Starts capturing of signals, using the hardware currently selected. Capturing takes place in its own thread which runs until the button is pressed again.");
        startStopToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startStopToggleButtonActionPerformed(evt);
            }
        });

        editingTextField.setToolTipText("Editing area; press RETURN to finish input.");
        editingTextField.setComponentPopupMenu(copyPastePopupMenu);
        editingTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editingTextFieldActionPerformed(evt);
            }
        });

        parametricOrRawExportButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/fileexport.png"))); // NOI18N
        parametricOrRawExportButton.setMnemonic('X');
        parametricOrRawExportButton.setText("Export");
        parametricOrRawExportButton.setToolTipText("Exports the selected (raw or parametric) remote to the export format currently selected.");
        parametricOrRawExportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parametricOrRawExportButtonActionPerformed(evt);
            }
        });

        scrutinizeRemoteHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        scrutinizeRemoteHelpButton.setText("Help");
        scrutinizeRemoteHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scrutinizeRemoteHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout remoteScrutinizerPanelLayout = new javax.swing.GroupLayout(remoteScrutinizerPanel);
        remoteScrutinizerPanel.setLayout(remoteScrutinizerPanelLayout);
        remoteScrutinizerPanelLayout.setHorizontalGroup(
            remoteScrutinizerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, remoteScrutinizerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(editingTextField)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(parametricOrRawExportButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(startStopToggleButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrutinizeRemoteHelpButton)
                .addContainerGap())
            .addComponent(rawCookedTabbedPane, javax.swing.GroupLayout.Alignment.TRAILING)
        );

        remoteScrutinizerPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {parametricOrRawExportButton, scrutinizeRemoteHelpButton, startStopToggleButton});

        remoteScrutinizerPanelLayout.setVerticalGroup(
            remoteScrutinizerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(remoteScrutinizerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(remoteScrutinizerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(startStopToggleButton)
                    .addComponent(editingTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(parametricOrRawExportButton)
                    .addComponent(scrutinizeRemoteHelpButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rawCookedTabbedPane)
                .addGap(0, 0, 0))
        );

        topLevelTabbedPane.addTab("Scrutinize remote", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/list.png")), remoteScrutinizerPanel); // NOI18N

        generatePanel.setToolTipText("Generate IR signal from protocol and parameters.");

        toScrutinizeButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/next.png"))); // NOI18N
        toScrutinizeButton.setText("To \"Scrutinize signal\"");
        toScrutinizeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toScrutinizeButtonActionPerformed(evt);
            }
        });

        transferToRawRemoteButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/next.png"))); // NOI18N
        transferToRawRemoteButton.setText("To raw remote");
        transferToRawRemoteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transferToRawRemoteButtonActionPerformed(evt);
            }
        });

        transmitGenerateButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/1rightarrow.png"))); // NOI18N
        transmitGenerateButton.setText("Transmit");
        transmitGenerateButton.setToolTipText("Transmit using the sending hardware currently selected.");
        transmitGenerateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transmitGenerateButtonActionPerformed(evt);
            }
        });

        generateExportButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/fileexport.png"))); // NOI18N
        generateExportButton.setText("Export");
        generateExportButton.setToolTipText("Export to the file format currently selected.");
        generateExportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generateExportButtonActionPerformed(evt);
            }
        });

        generateTextArea.setEditable(false);
        generateTextArea.setColumns(20);
        generateTextArea.setLineWrap(true);
        generateTextArea.setRows(5);
        generateTextArea.setToolTipText("The generated signal, in the output format preferred, is given here.");
        generateTextArea.setWrapStyleWord(true);
        generateTextArea.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        generateTextArea.setComponentPopupMenu(copyPopupMenu);
        jScrollPane1.setViewportView(generateTextArea);

        irpMasterBean.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        generateButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/gear.png"))); // NOI18N
        generateButton.setText("Render");
        generateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generateButtonActionPerformed(evt);
            }
        });

        generateHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        generateHelpButton.setText("Help");
        generateHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generateHelpButtonActionPerformed(evt);
            }
        });

        transferToParametricRemoteButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/next.png"))); // NOI18N
        transferToParametricRemoteButton.setText("To param. remote");
        transferToParametricRemoteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transferToParametricRemoteButtonActionPerformed(evt);
            }
        });

        openLastFileButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/edit.png"))); // NOI18N
        openLastFileButton1.setText("Open last export");
        openLastFileButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openLastFileButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout generatePanelLayout = new javax.swing.GroupLayout(generatePanel);
        generatePanel.setLayout(generatePanelLayout);
        generatePanelLayout.setHorizontalGroup(
            generatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(generatePanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(generatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addComponent(irpMasterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 981, Short.MAX_VALUE)
                    .addGroup(generatePanelLayout.createSequentialGroup()
                        .addGroup(generatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(generatePanelLayout.createSequentialGroup()
                                .addComponent(toScrutinizeButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(transferToRawRemoteButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(transferToParametricRemoteButton))
                            .addGroup(generatePanelLayout.createSequentialGroup()
                                .addComponent(generateButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(generateExportButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openLastFileButton1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(transmitGenerateButton)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(generateHelpButton)))
                .addGap(6, 6, 6))
        );
        generatePanelLayout.setVerticalGroup(
            generatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, generatePanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(irpMasterBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 208, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(generatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(generatePanelLayout.createSequentialGroup()
                        .addGroup(generatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(generateButton)
                            .addComponent(generateExportButton)
                            .addComponent(openLastFileButton1)
                            .addComponent(transmitGenerateButton))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(generatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(toScrutinizeButton)
                            .addComponent(transferToRawRemoteButton)
                            .addComponent(transferToParametricRemoteButton)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, generatePanelLayout.createSequentialGroup()
                        .addComponent(generateHelpButton)
                        .addContainerGap())))
        );

        topLevelTabbedPane.addTab("Render", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/gear.png")), generatePanel); // NOI18N

        controlTowerImportButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/reload.png"))); // NOI18N
        controlTowerImportButton.setText("Load");
        controlTowerImportButton.setEnabled(false);
        controlTowerImportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                controlTowerImportButtonActionPerformed(evt);
            }
        });

        controlTowerBrowseButton.setText("Web site");
        controlTowerBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                controlTowerBrowseButtonActionPerformed(evt);
            }
        });

        controlTowerManufacturerComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select me to load" }));
        controlTowerManufacturerComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                controlTowerManufacturerComboBoxActionPerformed(evt);
            }
        });

        controlTowerDeviceTypeComboBox.setEnabled(false);
        controlTowerDeviceTypeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                controlTowerDeviceTypeComboBoxActionPerformed(evt);
            }
        });

        controlTowerCodeSetComboBox.setEnabled(false);

        jLabel50.setText("Manufacturer");

        jLabel51.setText("Device Type");

        jLabel52.setText("Setup Code");

        importControlTowerHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        importControlTowerHelpButton.setText("Help");
        importControlTowerHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importControlTowerHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout controlTowerPanelLayout = new javax.swing.GroupLayout(controlTowerPanel);
        controlTowerPanel.setLayout(controlTowerPanelLayout);
        controlTowerPanelLayout.setHorizontalGroup(
            controlTowerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(controlTowerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(controlTowerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel50)
                    .addComponent(controlTowerManufacturerComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(controlTowerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(controlTowerDeviceTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel51))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(controlTowerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(controlTowerPanelLayout.createSequentialGroup()
                        .addComponent(jLabel52)
                        .addGap(461, 461, 461))
                    .addGroup(controlTowerPanelLayout.createSequentialGroup()
                        .addComponent(controlTowerCodeSetComboBox, 0, 318, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(controlTowerImportButton)
                        .addGap(169, 169, 169))))
            .addGroup(controlTowerPanelLayout.createSequentialGroup()
                .addComponent(controlTowerTreeImporter, javax.swing.GroupLayout.DEFAULT_SIZE, 735, Short.MAX_VALUE)
                .addGap(111, 111, 111)
                .addGroup(controlTowerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(importControlTowerHelpButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(controlTowerBrowseButton, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE))
                .addContainerGap())
        );

        controlTowerPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {controlTowerBrowseButton, importControlTowerHelpButton});

        controlTowerPanelLayout.setVerticalGroup(
            controlTowerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(controlTowerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(controlTowerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel50)
                    .addComponent(jLabel51)
                    .addComponent(jLabel52))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(controlTowerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(controlTowerDeviceTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(controlTowerCodeSetComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(controlTowerImportButton)
                    .addComponent(controlTowerManufacturerComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(controlTowerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(controlTowerTreeImporter, javax.swing.GroupLayout.DEFAULT_SIZE, 272, Short.MAX_VALUE)
                    .addGroup(controlTowerPanelLayout.createSequentialGroup()
                        .addGap(52, 52, 52)
                        .addComponent(controlTowerBrowseButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(importControlTowerHelpButton)
                        .addContainerGap())))
        );

        controlTowerPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {controlTowerBrowseButton, importControlTowerHelpButton});

        importTabbedPane.addTab("ControlTower Database", controlTowerPanel);

        remoteLocatorBrowseButton.setText("Visit Web site");
        remoteLocatorBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remoteLocatorBrowseButtonActionPerformed(evt);
            }
        });

        remoteLocatorImportButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/reload.png"))); // NOI18N
        remoteLocatorImportButton.setText("Load");
        remoteLocatorImportButton.setEnabled(false);
        remoteLocatorImportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remoteLocatorImportButtonActionPerformed(evt);
            }
        });

        remoteLocatorManufacturerComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select me to load" }));
        remoteLocatorManufacturerComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remoteLocatorManufacturerComboBoxActionPerformed(evt);
            }
        });

        remoteLocatorDeviceTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "__" }));
        remoteLocatorDeviceTypeComboBox.setEnabled(false);
        remoteLocatorDeviceTypeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remoteLocatorDeviceTypeComboBoxActionPerformed(evt);
            }
        });

        remoteLocatorRemoteComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "--" }));
        remoteLocatorRemoteComboBox.setEnabled(false);
        remoteLocatorRemoteComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remoteLocatorRemoteComboBoxActionPerformed(evt);
            }
        });

        jLabel45.setText("Manufacturer");

        jLabel46.setText("Device Type");

        jLabel49.setText("Remote Name");

        remoteLocatorBrowseRemoteButton.setText("Browse");
        remoteLocatorBrowseRemoteButton.setToolTipText("Load for all parameters and protocols");
        remoteLocatorBrowseRemoteButton.setEnabled(false);
        remoteLocatorBrowseRemoteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remoteLocatorBrowseRemoteButtonActionPerformed(evt);
            }
        });

        remoteLocatorHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        remoteLocatorHelpButton.setText("Help");
        remoteLocatorHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remoteLocatorHelpButtonActionPerformed(evt);
            }
        });

        remoteLocatorImporterKindTextField.setEditable(false);
        remoteLocatorImporterKindTextField.setToolTipText("The origin kind of remote ");

        jLabel10.setText("Kind");

        javax.swing.GroupLayout remoteLocatorPanelLayout = new javax.swing.GroupLayout(remoteLocatorPanel);
        remoteLocatorPanel.setLayout(remoteLocatorPanelLayout);
        remoteLocatorPanelLayout.setHorizontalGroup(
            remoteLocatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(remoteLocatorPanelLayout.createSequentialGroup()
                .addGroup(remoteLocatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(remoteLocatorPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(remoteLocatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(remoteLocatorManufacturerComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel45))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(remoteLocatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(remoteLocatorDeviceTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 245, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel46))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(remoteLocatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(remoteLocatorPanelLayout.createSequentialGroup()
                                .addComponent(remoteLocatorRemoteComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED))
                            .addGroup(remoteLocatorPanelLayout.createSequentialGroup()
                                .addComponent(jLabel49)
                                .addGap(167, 167, 167)))
                        .addGroup(remoteLocatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(remoteLocatorImporterKindTextField)
                            .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(26, 26, 26))
                    .addGroup(remoteLocatorPanelLayout.createSequentialGroup()
                        .addComponent(remoteLocatorTreeImporter, javax.swing.GroupLayout.DEFAULT_SIZE, 735, Short.MAX_VALUE)
                        .addGap(114, 114, 114)))
                .addGroup(remoteLocatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(remoteLocatorHelpButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(remoteLocatorBrowseButton, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(remoteLocatorBrowseRemoteButton, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(remoteLocatorImportButton, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGap(7, 7, 7))
        );

        remoteLocatorPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {remoteLocatorBrowseButton, remoteLocatorBrowseRemoteButton, remoteLocatorHelpButton, remoteLocatorImportButton});

        remoteLocatorPanelLayout.setVerticalGroup(
            remoteLocatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, remoteLocatorPanelLayout.createSequentialGroup()
                .addGap(75, 75, 75)
                .addComponent(remoteLocatorImportButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(remoteLocatorBrowseRemoteButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(remoteLocatorBrowseButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 104, Short.MAX_VALUE)
                .addComponent(remoteLocatorHelpButton)
                .addContainerGap())
            .addGroup(remoteLocatorPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(remoteLocatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel45)
                    .addComponent(jLabel46)
                    .addComponent(jLabel49)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(remoteLocatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(remoteLocatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(remoteLocatorDeviceTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(remoteLocatorRemoteComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(remoteLocatorImporterKindTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(remoteLocatorManufacturerComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(remoteLocatorTreeImporter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(3, 3, 3))
        );

        remoteLocatorPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {remoteLocatorBrowseButton, remoteLocatorBrowseRemoteButton, remoteLocatorHelpButton, remoteLocatorImportButton});

        importTabbedPane.addTab("RemoteLocator", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/database.png")), remoteLocatorPanel); // NOI18N

        girrWebSiteButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/translate.png"))); // NOI18N
        girrWebSiteButton.setText("Web site");
        girrWebSiteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                girrWebSiteButtonActionPerformed(evt);
            }
        });

        importGirrSignalHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        importGirrSignalHelpButton.setText("Help");
        importGirrSignalHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importGirrSignalHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout girrImportPanelLayout = new javax.swing.GroupLayout(girrImportPanel);
        girrImportPanel.setLayout(girrImportPanelLayout);
        girrImportPanelLayout.setHorizontalGroup(
            girrImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(girrImportPanelLayout.createSequentialGroup()
                .addComponent(girrFileImporterBean, javax.swing.GroupLayout.PREFERRED_SIZE, 734, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(girrImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(importGirrSignalHelpButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(girrWebSiteButton, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );

        girrImportPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {girrWebSiteButton, importGirrSignalHelpButton});

        girrImportPanelLayout.setVerticalGroup(
            girrImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(girrImportPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(girrWebSiteButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(importGirrSignalHelpButton)
                .addContainerGap())
            .addComponent(girrFileImporterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE)
        );

        importTabbedPane.addTab("Girr", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/translate.png")), girrImportPanel); // NOI18N

        jButton20.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/lirc/favicon-2.png"))); // NOI18N
        jButton20.setText("Web site");
        jButton20.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton20ActionPerformed(evt);
            }
        });

        importLircHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        importLircHelpButton.setText("Help");
        importLircHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importLircHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout lircImportPanelLayout = new javax.swing.GroupLayout(lircImportPanel);
        lircImportPanel.setLayout(lircImportPanelLayout);
        lircImportPanelLayout.setHorizontalGroup(
            lircImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lircImportPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(lircFileImporterBean, javax.swing.GroupLayout.PREFERRED_SIZE, 734, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 125, Short.MAX_VALUE)
                .addGroup(lircImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(importLircHelpButton, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton20, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );

        lircImportPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {importLircHelpButton, jButton20});

        lircImportPanelLayout.setVerticalGroup(
            lircImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lircFileImporterBean, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, lircImportPanelLayout.createSequentialGroup()
                .addGap(53, 53, 53)
                .addComponent(jButton20)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(importLircHelpButton)
                .addGap(12, 12, 12))
        );

        importTabbedPane.addTab("LIRC", new javax.swing.ImageIcon(getClass().getResource("/icons/lirc/favicon-0.png")), lircImportPanel); // NOI18N

        irTransWebButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/irtrans/favicon.png"))); // NOI18N
        irTransWebButton.setText("Web site");
        irTransWebButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irTransWebButtonActionPerformed(evt);
            }
        });

        importIrTransHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        importIrTransHelpButton.setText("Help");
        importIrTransHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importIrTransHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout irtransImportPanelLayout = new javax.swing.GroupLayout(irtransImportPanel);
        irtransImportPanel.setLayout(irtransImportPanelLayout);
        irtransImportPanelLayout.setHorizontalGroup(
            irtransImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irtransImportPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(irTransFileImporterBean, javax.swing.GroupLayout.PREFERRED_SIZE, 734, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 125, Short.MAX_VALUE)
                .addGroup(irtransImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(importIrTransHelpButton, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(irTransWebButton, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        irtransImportPanelLayout.setVerticalGroup(
            irtransImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(irTransFileImporterBean, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, irtransImportPanelLayout.createSequentialGroup()
                .addGap(53, 53, 53)
                .addComponent(irTransWebButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(importIrTransHelpButton)
                .addGap(12, 12, 12))
        );

        importTabbedPane.addTab("IrTrans", new javax.swing.ImageIcon(getClass().getResource("/icons/irtrans/favicon.png")), irtransImportPanel); // NOI18N

        importProntoClassicHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        importProntoClassicHelpButton.setText("Help");
        importProntoClassicHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importProntoClassicHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout ccfImportPanelLayout = new javax.swing.GroupLayout(ccfImportPanel);
        ccfImportPanel.setLayout(ccfImportPanelLayout);
        ccfImportPanelLayout.setHorizontalGroup(
            ccfImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ccfImportPanelLayout.createSequentialGroup()
                .addComponent(ccfFileImporterBean, javax.swing.GroupLayout.PREFERRED_SIZE, 734, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 150, Short.MAX_VALUE)
                .addComponent(importProntoClassicHelpButton)
                .addContainerGap())
        );
        ccfImportPanelLayout.setVerticalGroup(
            ccfImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ccfImportPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(importProntoClassicHelpButton)
                .addContainerGap())
            .addComponent(ccfFileImporterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE)
        );

        importTabbedPane.addTab("Pronto Classic (.ccf)", ccfImportPanel);

        importProntoProfessionalHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        importProntoProfessionalHelpButton.setText("Help");
        importProntoProfessionalHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importProntoProfessionalHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout xcfImportPanelLayout = new javax.swing.GroupLayout(xcfImportPanel);
        xcfImportPanel.setLayout(xcfImportPanelLayout);
        xcfImportPanelLayout.setHorizontalGroup(
            xcfImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(xcfImportPanelLayout.createSequentialGroup()
                .addComponent(xcfFileImporterBean, javax.swing.GroupLayout.PREFERRED_SIZE, 734, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 150, Short.MAX_VALUE)
                .addComponent(importProntoProfessionalHelpButton)
                .addContainerGap())
        );
        xcfImportPanelLayout.setVerticalGroup(
            xcfImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(xcfImportPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(importProntoProfessionalHelpButton)
                .addContainerGap())
            .addComponent(xcfFileImporterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE)
        );

        importTabbedPane.addTab("Pronto Prof (.xcf)", xcfImportPanel);

        importIctHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        importIctHelpButton.setText("Help");
        importIctHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importIctHelpButtonActionPerformed(evt);
            }
        });

        chopIctImportCheckBox.setSelected(properties.getChopIctImports());
        chopIctImportCheckBox.setText("Chop long signals");
        chopIctImportCheckBox.setToolTipText("If selected, the signals of an ICT imports are chopped according to the chopThreshold vaiable.");
        chopIctImportCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chopIctImportCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout ictImportPanelLayout = new javax.swing.GroupLayout(ictImportPanel);
        ictImportPanel.setLayout(ictImportPanelLayout);
        ictImportPanelLayout.setHorizontalGroup(
            ictImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ictImportPanelLayout.createSequentialGroup()
                .addComponent(ictFileImporterBean, javax.swing.GroupLayout.PREFERRED_SIZE, 734, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 150, Short.MAX_VALUE)
                .addComponent(importIctHelpButton)
                .addContainerGap())
            .addGroup(ictImportPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(chopIctImportCheckBox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        ictImportPanelLayout.setVerticalGroup(
            ictImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ictImportPanelLayout.createSequentialGroup()
                .addComponent(chopIctImportCheckBox)
                .addGap(4, 4, 4)
                .addComponent(ictFileImporterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 316, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ictImportPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(importIctHelpButton)
                .addGap(12, 12, 12))
        );

        importTabbedPane.addTab("ICT", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/usb.png")), ictImportPanel); // NOI18N

        jLabel17.setText("Note: invokeRepeatFinder is used.");

        importMode2HelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        importMode2HelpButton.setText("Help");
        importMode2HelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importMode2HelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout mode2ImportPanelLayout = new javax.swing.GroupLayout(mode2ImportPanel);
        mode2ImportPanel.setLayout(mode2ImportPanelLayout);
        mode2ImportPanelLayout.setHorizontalGroup(
            mode2ImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mode2ImportPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel17)
                .addContainerGap(731, Short.MAX_VALUE))
            .addGroup(mode2ImportPanelLayout.createSequentialGroup()
                .addComponent(mode2FileImporterBean, javax.swing.GroupLayout.PREFERRED_SIZE, 734, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(importMode2HelpButton)
                .addContainerGap())
        );
        mode2ImportPanelLayout.setVerticalGroup(
            mode2ImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mode2ImportPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mode2FileImporterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 316, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mode2ImportPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(importMode2HelpButton)
                .addGap(12, 12, 12))
        );

        importTabbedPane.addTab("mode2", mode2ImportPanel);

        rawNameColumnComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "-", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" }));
        rawNameColumnComboBox.setSelectedIndex(properties.getRawNameColumn());
        rawNameColumnComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawNameColumnComboBoxActionPerformed(evt);
            }
        });

        rawCodeColumnComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "-1" }));
        rawCodeColumnComboBox1.setSelectedItem(Integer.toString(properties.getCodeColumn()));
        rawCodeColumnComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeColumnComboBox1ActionPerformed(evt);
            }
        });

        includeSubsequenctColumnsCheckBox1.setSelected(properties.getIncludeTail());
        includeSubsequenctColumnsCheckBox1.setText("and subseq. columns");
        includeSubsequenctColumnsCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                includeSubsequenctColumnsCheckBox1ActionPerformed(evt);
            }
        });

        jLabel25.setText("Name col.");

        jLabel26.setText("Raw signal col.");

        csvRawSeparatorComboBox.setModel(new DefaultComboBoxModel(CsvImporter.separatorsArray()));
        csvRawSeparatorComboBox.setSelectedIndex(properties.getRawSeparatorIndex());
        csvRawSeparatorComboBox.setToolTipText("Separator between the fields");
        csvRawSeparatorComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                csvRawSeparatorComboBoxActionPerformed(evt);
            }
        });

        jLabel30.setText("Field separator");

        importTextRawHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        importTextRawHelpButton.setText("Help");
        importTextRawHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importTextRawHelpButtonActionPerformed(evt);
            }
        });

        rawMultiColumnNameCheckBox.setSelected(properties.getRawNameMultiColumn());
        rawMultiColumnNameCheckBox.setText("Multi Col. name");
        rawMultiColumnNameCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawMultiColumnNameCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout csvRawImportPanelLayout = new javax.swing.GroupLayout(csvRawImportPanel);
        csvRawImportPanel.setLayout(csvRawImportPanelLayout);
        csvRawImportPanelLayout.setHorizontalGroup(
            csvRawImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(csvRawImportPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(csvRawImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(csvRawFileImporterBean, javax.swing.GroupLayout.PREFERRED_SIZE, 734, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(csvRawImportPanelLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(csvRawImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(csvRawImportPanelLayout.createSequentialGroup()
                                .addComponent(rawNameColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(rawMultiColumnNameCheckBox))
                            .addComponent(jLabel25))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(csvRawImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(csvRawImportPanelLayout.createSequentialGroup()
                                .addComponent(rawCodeColumnComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(includeSubsequenctColumnsCheckBox1))
                            .addComponent(jLabel26))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(csvRawImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(csvRawSeparatorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel30))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 216, Short.MAX_VALUE)
                .addComponent(importTextRawHelpButton)
                .addContainerGap())
        );
        csvRawImportPanelLayout.setVerticalGroup(
            csvRawImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(csvRawImportPanelLayout.createSequentialGroup()
                .addGroup(csvRawImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel26)
                    .addComponent(jLabel25, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel30))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(csvRawImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(includeSubsequenctColumnsCheckBox1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(csvRawImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(rawNameColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(rawMultiColumnNameCheckBox)
                        .addComponent(rawCodeColumnComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(csvRawSeparatorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(csvRawImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(csvRawImportPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(importTextRawHelpButton)
                        .addGap(12, 12, 12))
                    .addGroup(csvRawImportPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(csvRawFileImporterBean, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))))
        );

        parametrizedRawTabbedPane.addTab("Raw", csvRawImportPanel);

        importTextRawLineBasedHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        importTextRawLineBasedHelpButton.setText("Help");
        importTextRawLineBasedHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importTextRawLineBasedHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout rawLineCsvImportPanelLayout = new javax.swing.GroupLayout(rawLineCsvImportPanel);
        rawLineCsvImportPanel.setLayout(rawLineCsvImportPanelLayout);
        rawLineCsvImportPanelLayout.setHorizontalGroup(
            rawLineCsvImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rawLineCsvImportPanelLayout.createSequentialGroup()
                .addComponent(rawLineCsvFileImporterBean, javax.swing.GroupLayout.PREFERRED_SIZE, 734, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 216, Short.MAX_VALUE)
                .addComponent(importTextRawLineBasedHelpButton)
                .addContainerGap())
        );
        rawLineCsvImportPanelLayout.setVerticalGroup(
            rawLineCsvImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rawLineCsvImportPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(importTextRawLineBasedHelpButton)
                .addContainerGap())
            .addGroup(rawLineCsvImportPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(rawLineCsvFileImporterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 304, Short.MAX_VALUE))
        );

        parametrizedRawTabbedPane.addTab("Raw, line-based", rawLineCsvImportPanel);

        parametrizedNameColumnComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" }));
        parametrizedNameColumnComboBox.setSelectedIndex(properties.getParametricNameColumn() - 1);
        parametrizedNameColumnComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parametrizedNameColumnComboBoxActionPerformed(evt);
            }
        });

        protocolColumnComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "-", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" }));
        protocolColumnComboBox.setSelectedIndex(properties.getProtocolColumn());
        protocolColumnComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolColumnComboBoxActionPerformed(evt);
            }
        });

        dColumnComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "-", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" }));
        dColumnComboBox.setSelectedIndex(properties.getDColumn());
        dColumnComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dColumnComboBoxActionPerformed(evt);
            }
        });

        sColumnComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "-", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" }));
        sColumnComboBox.setSelectedIndex(properties.getSColumn());
        sColumnComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sColumnComboBoxActionPerformed(evt);
            }
        });

        fColumnComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "-", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" }));
        fColumnComboBox.setSelectedItem(Integer.toString(properties.getFColumn()));
        fColumnComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fColumnComboBoxActionPerformed(evt);
            }
        });

        jLabel18.setText("Name col.");

        jLabel19.setText("Protocol");

        jLabel20.setText("D");

        jLabel21.setText("S");

        parametrizedCsvSeparatorComboBox.setModel(new DefaultComboBoxModel(CsvImporter.separatorsArray()));
        parametrizedCsvSeparatorComboBox.setSelectedIndex(properties.getParametricSeparatorIndex());
        parametrizedCsvSeparatorComboBox.setToolTipText("Separator between the fields");
        parametrizedCsvSeparatorComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parametrizedCsvSeparatorComboBoxActionPerformed(evt);
            }
        });

        jLabel14.setText("F");

        parametrizedBaseComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "2", "8", "10", "16" }));
        parametrizedBaseComboBox.setSelectedIndex(properties.getParametricNumberBaseIndex());
        parametrizedBaseComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parametrizedBaseComboBoxActionPerformed(evt);
            }
        });

        importTextParametrizedHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        importTextParametrizedHelpButton.setText("Help");
        importTextParametrizedHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importTextParametrizedHelpButtonActionPerformed(evt);
            }
        });

        jLabel2.setText("Field separator");

        jLabel23.setText("Radix");

        parametrizedMultiColumnNameCheckBox.setSelected(properties.getParametrizedNameMultiColumn());
        parametrizedMultiColumnNameCheckBox.setText("Multi col. name");
        parametrizedMultiColumnNameCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parametrizedMultiColumnNameCheckBoxActionPerformed(evt);
            }
        });

        miscParametersColumnComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "-", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" }));
        miscParametersColumnComboBox.setSelectedItem(Integer.toString(properties.getMiscParametersColumn()));
        miscParametersColumnComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miscParametersColumnComboBoxActionPerformed(evt);
            }
        });

        jLabel29.setText("Misc.");

        javax.swing.GroupLayout parametrizedCsvImportPanelLayout = new javax.swing.GroupLayout(parametrizedCsvImportPanel);
        parametrizedCsvImportPanel.setLayout(parametrizedCsvImportPanelLayout);
        parametrizedCsvImportPanelLayout.setHorizontalGroup(
            parametrizedCsvImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(parametrizedCsvImportPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(parametrizedCsvImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(parametrizedCsvImportPanelLayout.createSequentialGroup()
                        .addComponent(parametrizedNameColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(parametrizedMultiColumnNameCheckBox))
                    .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(parametrizedCsvImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(parametrizedCsvImportPanelLayout.createSequentialGroup()
                        .addComponent(protocolColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(dColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(parametrizedCsvImportPanelLayout.createSequentialGroup()
                        .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel20)))
                .addGroup(parametrizedCsvImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(parametrizedCsvImportPanelLayout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addComponent(jLabel21)
                        .addGap(34, 34, 34)
                        .addComponent(jLabel14)
                        .addGap(46, 46, 46)
                        .addComponent(jLabel29))
                    .addGroup(parametrizedCsvImportPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(sColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(fColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(miscParametersColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(parametrizedCsvImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(parametrizedCsvSeparatorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(parametrizedCsvImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel23)
                    .addComponent(parametrizedBaseComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(335, 335, 335))
            .addGroup(parametrizedCsvImportPanelLayout.createSequentialGroup()
                .addComponent(csvParametrizedFileImporterBean, javax.swing.GroupLayout.PREFERRED_SIZE, 734, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(importTextParametrizedHelpButton)
                .addContainerGap())
        );
        parametrizedCsvImportPanelLayout.setVerticalGroup(
            parametrizedCsvImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(parametrizedCsvImportPanelLayout.createSequentialGroup()
                .addGroup(parametrizedCsvImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(jLabel19)
                    .addComponent(jLabel20)
                    .addComponent(jLabel21)
                    .addComponent(jLabel14)
                    .addComponent(jLabel2)
                    .addComponent(jLabel23)
                    .addComponent(jLabel29))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(parametrizedCsvImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(parametrizedNameColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(protocolColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(dColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(parametrizedCsvSeparatorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(parametrizedMultiColumnNameCheckBox)
                    .addComponent(parametrizedBaseComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(miscParametersColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(parametrizedCsvImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(csvParametrizedFileImporterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 265, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, parametrizedCsvImportPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(importTextParametrizedHelpButton)
                        .addGap(12, 12, 12))))
        );

        parametrizedRawTabbedPane.addTab("Parametrized", parametrizedCsvImportPanel);

        parametrizedRawTabbedPane.setSelectedIndex(properties.getParametrizedRawTabbedPaneSelectedIndex());

        javax.swing.GroupLayout csvImportPanelLayout = new javax.swing.GroupLayout(csvImportPanel);
        csvImportPanel.setLayout(csvImportPanelLayout);
        csvImportPanelLayout.setHorizontalGroup(
            csvImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(csvImportPanelLayout.createSequentialGroup()
                .addComponent(parametrizedRawTabbedPane)
                .addGap(0, 0, 0))
        );
        csvImportPanelLayout.setVerticalGroup(
            csvImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(csvImportPanelLayout.createSequentialGroup()
                .addComponent(parametrizedRawTabbedPane)
                .addGap(0, 0, 0))
        );

        importTabbedPane.addTab("Text/CSV", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/mimetypes/txt.png")), csvImportPanel); // NOI18N

        importWaveDivideCarrierCheckBox1.setSelected(properties.getImportWaveDivideCarrier());
        importWaveDivideCarrierCheckBox1.setText("Half carrier frequency");
        importWaveDivideCarrierCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importWaveDivideCarrierCheckBox1ActionPerformed(evt);
            }
        });

        importWaveHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        importWaveHelpButton.setText("Help");
        importWaveHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importWaveHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout waveImportPanelLayout = new javax.swing.GroupLayout(waveImportPanel);
        waveImportPanel.setLayout(waveImportPanelLayout);
        waveImportPanelLayout.setHorizontalGroup(
            waveImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(waveImportPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(waveImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(waveImportPanelLayout.createSequentialGroup()
                        .addComponent(waveFileImporterBean, javax.swing.GroupLayout.PREFERRED_SIZE, 734, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 150, Short.MAX_VALUE)
                        .addComponent(importWaveHelpButton))
                    .addComponent(importWaveDivideCarrierCheckBox1))
                .addContainerGap())
        );
        waveImportPanelLayout.setVerticalGroup(
            waveImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(waveImportPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(importWaveDivideCarrierCheckBox1)
                .addGap(0, 0, 0)
                .addComponent(waveFileImporterBean, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, waveImportPanelLayout.createSequentialGroup()
                .addContainerGap(299, Short.MAX_VALUE)
                .addComponent(importWaveHelpButton)
                .addContainerGap())
        );

        importTabbedPane.addTab("Wave", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/mix_audio.png")), waveImportPanel); // NOI18N

        importCmlHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        importCmlHelpButton.setText("Help");
        importCmlHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importCmlHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout cmlImportPanelLayout = new javax.swing.GroupLayout(cmlImportPanel);
        cmlImportPanel.setLayout(cmlImportPanelLayout);
        cmlImportPanelLayout.setHorizontalGroup(
            cmlImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cmlImportPanelLayout.createSequentialGroup()
                .addComponent(cmlFileImporterBean, javax.swing.GroupLayout.PREFERRED_SIZE, 734, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 150, Short.MAX_VALUE)
                .addComponent(importCmlHelpButton)
                .addContainerGap())
        );
        cmlImportPanelLayout.setVerticalGroup(
            cmlImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cmlImportPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(importCmlHelpButton)
                .addContainerGap())
            .addComponent(cmlFileImporterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE)
        );

        importTabbedPane.addTab("CML (RTI)", cmlImportPanel);

        importCommandFusionHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        importCommandFusionHelpButton.setText("Help");
        importCommandFusionHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importCommandFusionHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout commandFusionImportPanelLayout = new javax.swing.GroupLayout(commandFusionImportPanel);
        commandFusionImportPanel.setLayout(commandFusionImportPanelLayout);
        commandFusionImportPanelLayout.setHorizontalGroup(
            commandFusionImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(commandFusionImportPanelLayout.createSequentialGroup()
                .addComponent(commandFusionFileImporterBean, javax.swing.GroupLayout.PREFERRED_SIZE, 734, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 150, Short.MAX_VALUE)
                .addComponent(importCommandFusionHelpButton)
                .addContainerGap())
        );
        commandFusionImportPanelLayout.setVerticalGroup(
            commandFusionImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(commandFusionImportPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(importCommandFusionHelpButton)
                .addContainerGap())
            .addComponent(commandFusionFileImporterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE)
        );

        importTabbedPane.addTab("CommandFusion", commandFusionImportPanel);

        importTabbedPane.setSelectedIndex(properties.getImportPaneSelectedIndex());

        javax.swing.GroupLayout importPanelLayout = new javax.swing.GroupLayout(importPanel);
        importPanel.setLayout(importPanelLayout);
        importPanelLayout.setHorizontalGroup(
            importPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, importPanelLayout.createSequentialGroup()
                .addComponent(importTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 993, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );
        importPanelLayout.setVerticalGroup(
            importPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(importTabbedPane, javax.swing.GroupLayout.Alignment.TRAILING)
        );

        importTabbedPane.getAccessibleContext().setAccessibleName("importTabbedPane");

        topLevelTabbedPane.addTab("Import", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/fileimport.png")), importPanel); // NOI18N

        exportPanel.setToolTipText("This pane contains export options");

        exportFormatComboBox.setModel(new DefaultComboBoxModel(exportFormatManager.toArray()));
        exportFormatComboBox.setSelectedItem(properties.getExportFormatName());
        exportFormatComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportFormatComboBoxActionPerformed(evt);
            }
        });

        jLabel6.setText("Export dir.");

        exportDirectoryTextField.setText(properties.getExportDir());
        exportDirectoryTextField.setComponentPopupMenu(copyPastePopupMenu);
        exportDirectoryTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportDirectoryTextFieldActionPerformed(evt);
            }
        });

        exportDirSelectButton.setText("...");
        exportDirSelectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportDirSelectButtonActionPerformed(evt);
            }
        });

        exportDirOpenButton.setText("Open");
        exportDirOpenButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportDirOpenButtonActionPerformed(evt);
            }
        });

        jLabel8.setText("Export Format");

        exportSignalButton.setText("Export signal");
        exportSignalButton.setToolTipText("Export the signal on the \"Scrutinize signal\" pane");
        exportSignalButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSignalButtonActionPerformed(evt);
            }
        });

        automaticExportFilenamesCheckBox.setSelected(properties.getExportAutomaticFilenames());
        automaticExportFilenamesCheckBox.setText("Autogenerate file names");
        automaticExportFilenamesCheckBox.setToolTipText("If checked, file names are generated automatically, otherwise the user must enter it.");
        automaticExportFilenamesCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                automaticExportFilenamesCheckBoxActionPerformed(evt);
            }
        });

        openLastFileButton.setText("Open last file");
        openLastFileButton.setToolTipText("Open the last file generated, if any.");
        openLastFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openLastFileButtonActionPerformed(evt);
            }
        });

        exportRawRemoteButton.setText("Export raw remote");
        exportRawRemoteButton.setToolTipText("Export the signals in the raw table of the \"Scrutinize remote\" pane.");
        exportRawRemoteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportRawRemoteButtonActionPerformed(evt);
            }
        });

        exportParametricRemoteButton.setText("Export param. remote");
        exportParametricRemoteButton.setToolTipText("Export the signals in the parametric table of \"Scrutinize remote\" pane.");
        exportParametricRemoteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportParametricRemoteButtonActionPerformed(evt);
            }
        });

        exportRepeatComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "0", "1", "2", "3", "4", "5", "7", "10", "15", "20", "30", "50", "70", "100" }));
        exportRepeatComboBox.setSelectedItem(Integer.toString(properties.getExportNoRepeats()));
        exportRepeatComboBox.setEnabled(this.exportGenerateSendIrCheckBox.isSelected());
        exportRepeatComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportRepeatComboBoxActionPerformed(evt);
            }
        });

        exportNumberRepeatsLabel.setText("# repeats");

        exportHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        exportHelpButton.setText("Help");
        exportHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportHelpButtonActionPerformed(evt);
            }
        });

        exportRawRemoteButton1.setText("Export rendered");
        exportRawRemoteButton1.setToolTipText("Export the signal(s) of the \"Render\" pane.");
        exportRawRemoteButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generateExportButtonActionPerformed(evt);
            }
        });

        autoOpenExportsCheckBox.setSelected(properties.getAutoOpenExports());
        autoOpenExportsCheckBox.setText("Auto open exports");
        autoOpenExportsCheckBox.setToolTipText("If checked, exports will be automatically opened.");
        autoOpenExportsCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoOpenExportsCheckBoxActionPerformed(evt);
            }
        });

        exportFormatDocumentationButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/contents.png"))); // NOI18N
        exportFormatDocumentationButton.setText("Format documentation");
        exportFormatDocumentationButton.setToolTipText("Display documentation for the current export format");
        exportFormatDocumentationButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportFormatDocumentationButtonActionPerformed(evt);
            }
        });

        exportFormatParametersPane.setBorder(javax.swing.BorderFactory.createTitledBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED), "Protocol Parameters"));
        exportFormatParametersPane.setLayout(new javax.swing.OverlayLayout(exportFormatParametersPane));

        exportGirrHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        exportGirrHelpButton.setText("Help");
        exportGirrHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportGirrHelpButtonActionPerformed(evt);
            }
        });

        girrFatRawCheckBox.setSelected(properties.getGirrFatRaw());
        girrFatRawCheckBox.setText("Fat form raw");
        girrFatRawCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                girrFatRawCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout girrExportOptionsPanelLayout = new javax.swing.GroupLayout(girrExportOptionsPanel);
        girrExportOptionsPanel.setLayout(girrExportOptionsPanelLayout);
        girrExportOptionsPanelLayout.setHorizontalGroup(
            girrExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(girrExportOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(girrFatRawCheckBox)
                .addGap(94, 94, 94)
                .addComponent(exportGirrHelpButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        girrExportOptionsPanelLayout.setVerticalGroup(
            girrExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(girrExportOptionsPanelLayout.createSequentialGroup()
                .addGap(37, 37, 37)
                .addGroup(girrExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(girrFatRawCheckBox)
                    .addComponent(exportGirrHelpButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        exportFormatParametersPane.add(girrExportOptionsPanel);

        prontoModelComboBox.setModel(new DefaultComboBoxModel(ProntoClassicExporter.prontoModelNames));
        prontoModelComboBox.setSelectedIndex(properties.getProntoModelIndex());
        prontoModelComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prontoModelComboBoxActionPerformed(evt);
            }
        });

        jLabel22.setText("Pronto Model");

        jLabel27.setText("Screen size");

        prontoExportScreenWidthTextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        prontoExportScreenWidthTextField.setText(Integer.toString(properties.getProntoScreenSizeX()));
        prontoExportScreenWidthTextField.setMinimumSize(new java.awt.Dimension(12, 40));
        prontoExportScreenWidthTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                prontoExportScreenWidthTextFieldFocusLost(evt);
            }
        });

        prontoExportScreenHeightTextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        prontoExportScreenHeightTextField.setText(Integer.toString(properties.getProntoScreenSizeY()));
        prontoExportScreenHeightTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                prontoExportScreenHeightTextFieldFocusLost(evt);
            }
        });

        jLabel31.setText("Button size");

        prontoExportButtonWidthTextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        prontoExportButtonWidthTextField.setText(Integer.toString(properties.getProntoButtonSizeX()));
        prontoExportButtonWidthTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                prontoExportButtonWidthTextFieldFocusLost(evt);
            }
        });

        prontoExportButtonHeightTextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        prontoExportButtonHeightTextField.setText(Integer.toString(properties.getProntoButtonSizeY()));
        prontoExportButtonHeightTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                prontoExportButtonHeightTextFieldFocusLost(evt);
            }
        });

        exportProntoHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        exportProntoHelpButton.setText("Help");
        exportProntoHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportProntoHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout prontoClassicExportOptionsPanelLayout = new javax.swing.GroupLayout(prontoClassicExportOptionsPanel);
        prontoClassicExportOptionsPanel.setLayout(prontoClassicExportOptionsPanelLayout);
        prontoClassicExportOptionsPanelLayout.setHorizontalGroup(
            prontoClassicExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(prontoClassicExportOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(prontoClassicExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel31)
                    .addComponent(jLabel22)
                    .addComponent(jLabel27))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(prontoClassicExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(prontoClassicExportOptionsPanelLayout.createSequentialGroup()
                        .addComponent(prontoModelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(exportProntoHelpButton))
                    .addGroup(prontoClassicExportOptionsPanelLayout.createSequentialGroup()
                        .addComponent(prontoExportScreenHeightTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(prontoExportScreenWidthTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(prontoClassicExportOptionsPanelLayout.createSequentialGroup()
                        .addComponent(prontoExportButtonWidthTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(prontoExportButtonHeightTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        prontoClassicExportOptionsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {prontoExportButtonWidthTextField, prontoExportScreenWidthTextField});

        prontoClassicExportOptionsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {prontoExportButtonHeightTextField, prontoExportScreenHeightTextField});

        prontoClassicExportOptionsPanelLayout.setVerticalGroup(
            prontoClassicExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(prontoClassicExportOptionsPanelLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(prontoClassicExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(prontoModelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel22)
                    .addComponent(exportProntoHelpButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(prontoClassicExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel27)
                    .addComponent(prontoExportScreenHeightTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(prontoExportScreenWidthTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(prontoClassicExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel31)
                    .addComponent(prontoExportButtonWidthTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(prontoExportButtonHeightTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        prontoClassicExportOptionsPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {prontoExportButtonHeightTextField, prontoExportButtonWidthTextField, prontoExportScreenHeightTextField, prontoExportScreenWidthTextField});

        exportFormatParametersPane.add(prontoClassicExportOptionsPanel);

        exportWaveHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        exportWaveHelpButton.setText("Help");
        exportWaveHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportWaveHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout waveExportOptionsPanelLayout = new javax.swing.GroupLayout(waveExportOptionsPanel);
        waveExportOptionsPanel.setLayout(waveExportOptionsPanelLayout);
        waveExportOptionsPanelLayout.setHorizontalGroup(
            waveExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(waveExportOptionsPanelLayout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addComponent(exportAudioParametersBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(exportWaveHelpButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        waveExportOptionsPanelLayout.setVerticalGroup(
            waveExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(waveExportOptionsPanelLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(waveExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(exportAudioParametersBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(waveExportOptionsPanelLayout.createSequentialGroup()
                        .addGap(9, 9, 9)
                        .addComponent(exportWaveHelpButton)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        exportFormatParametersPane.setLayer(waveExportOptionsPanel, javax.swing.JLayeredPane.MODAL_LAYER);
        exportFormatParametersPane.add(waveExportOptionsPanel);

        subformatsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED), "Subformats"));
        subformatsPanel.setLayout(new java.awt.GridLayout(4, 2));

        exportGenerateParametersCheckBox.setSelected(properties.getExportGenerateParameters());
        exportGenerateParametersCheckBox.setText("Parameters");
        exportGenerateParametersCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportGenerateParametersCheckBoxActionPerformed(evt);
            }
        });
        subformatsPanel.add(exportGenerateParametersCheckBox);

        exportGenerateShortCcfCheckBox.setSelected(properties.getExportGenerateShortCcf());
        exportGenerateShortCcfCheckBox.setText("short Pronto Hex");
        exportGenerateShortCcfCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportGenerateShortCcfCheckBoxActionPerformed(evt);
            }
        });
        subformatsPanel.add(exportGenerateShortCcfCheckBox);

        exportGenerateRawCheckBox.setSelected(properties.getExportGenerateRaw());
        exportGenerateRawCheckBox.setText("Raw");
        exportGenerateRawCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportGenerateRawCheckBoxActionPerformed(evt);
            }
        });
        subformatsPanel.add(exportGenerateRawCheckBox);

        exportGenerateBroadlinkHexCheckBox.setSelected(properties.getExportGenerateBroadlinkHex());
        exportGenerateBroadlinkHexCheckBox.setText("Broadlink Hex");
        exportGenerateBroadlinkHexCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportGenerateBroadlinkHexCheckBoxActionPerformed(evt);
            }
        });
        subformatsPanel.add(exportGenerateBroadlinkHexCheckBox);

        exportGenerateCcfCheckBox.setSelected(properties.getExportGenerateCcf());
        exportGenerateCcfCheckBox.setText("Pronto Hex");
        exportGenerateCcfCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportGenerateCcfCheckBoxActionPerformed(evt);
            }
        });
        subformatsPanel.add(exportGenerateCcfCheckBox);

        exportGenerateBroadlinkBase64CheckBox.setSelected(properties.getExportGenerateBroadlinkBase64());
        exportGenerateBroadlinkBase64CheckBox.setText("Broadlink Base64");
        exportGenerateBroadlinkBase64CheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportGenerateBroadlinkBase64CheckBoxActionPerformed(evt);
            }
        });
        subformatsPanel.add(exportGenerateBroadlinkBase64CheckBox);

        exportGenerateSendIrCheckBox.setSelected(properties.getExportGenerateSendIr());
        exportGenerateSendIrCheckBox.setText("sendir");
        exportGenerateSendIrCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportGenerateSendIrCheckBoxActionPerformed(evt);
            }
        });
        subformatsPanel.add(exportGenerateSendIrCheckBox);

        javax.swing.GroupLayout exportPanelLayout = new javax.swing.GroupLayout(exportPanel);
        exportPanel.setLayout(exportPanelLayout);
        exportPanelLayout.setHorizontalGroup(
            exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(exportPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(exportPanelLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(exportPanelLayout.createSequentialGroup()
                                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(automaticExportFilenamesCheckBox)
                                    .addComponent(autoOpenExportsCheckBox))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(exportPanelLayout.createSequentialGroup()
                                .addComponent(exportSignalButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(exportParametricRemoteButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(exportRawRemoteButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(exportRawRemoteButton1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(openLastFileButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(exportHelpButton))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, exportPanelLayout.createSequentialGroup()
                                .addComponent(subformatsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 133, Short.MAX_VALUE)
                                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(exportFormatDocumentationButton, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(exportFormatParametersPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(50, 50, 50))))
                    .addGroup(exportPanelLayout.createSequentialGroup()
                        .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel8)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(exportPanelLayout.createSequentialGroup()
                                .addComponent(exportDirectoryTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 637, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(exportDirSelectButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(exportDirOpenButton))
                            .addComponent(exportFormatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 246, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 107, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(exportPanelLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(exportNumberRepeatsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(exportRepeatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        exportPanelLayout.setVerticalGroup(
            exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(exportPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(exportDirectoryTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(exportDirSelectButton)
                    .addComponent(exportDirOpenButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(exportFormatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8)
                    .addComponent(exportFormatDocumentationButton))
                .addGap(16, 16, 16)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(exportFormatParametersPane, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(subformatsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(7, 7, 7)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(exportRepeatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(exportNumberRepeatsLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 37, Short.MAX_VALUE)
                .addComponent(automaticExportFilenamesCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(autoOpenExportsCheckBox)
                .addGap(18, 18, 18)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(exportSignalButton)
                    .addComponent(exportParametricRemoteButton)
                    .addComponent(exportRawRemoteButton1)
                    .addComponent(openLastFileButton)
                    .addComponent(exportRawRemoteButton)
                    .addComponent(exportHelpButton))
                .addContainerGap())
        );

        topLevelTabbedPane.addTab("Export", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/fileexport.png")), exportPanel); // NOI18N

        sendingHardwareTabbedPane.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        sendingHardwareTabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sendingHardwareTabbedPaneStateChanged(evt);
            }
        });

        sendingGlobalCacheHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        sendingGlobalCacheHelpButton.setText("Help");
        sendingGlobalCacheHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendingGlobalCacheHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout globalCachePanelLayout = new javax.swing.GroupLayout(globalCachePanel);
        globalCachePanel.setLayout(globalCachePanelLayout);
        globalCachePanelLayout.setHorizontalGroup(
            globalCachePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(globalCachePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(globalCacheIrSenderSelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(216, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, globalCachePanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(sendingGlobalCacheHelpButton)
                .addContainerGap())
        );
        globalCachePanelLayout.setVerticalGroup(
            globalCachePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(globalCachePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(globalCacheIrSenderSelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(sendingGlobalCacheHelpButton)
                .addContainerGap())
        );

        sendingHardwareTabbedPane.addTab("Global Caché", globalCachePanel);

        sendingDevLircHardwareHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        sendingDevLircHardwareHelpButton.setText("Help");
        sendingDevLircHardwareHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendingDevLircHardwareHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout devLircPanelLayout = new javax.swing.GroupLayout(devLircPanel);
        devLircPanel.setLayout(devLircPanelLayout);
        devLircPanelLayout.setHorizontalGroup(
            devLircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(devLircPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(devLircBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(31, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, devLircPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(sendingDevLircHardwareHelpButton)
                .addContainerGap())
        );
        devLircPanelLayout.setVerticalGroup(
            devLircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(devLircPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(devLircBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 100, Short.MAX_VALUE)
                .addComponent(sendingDevLircHardwareHelpButton)
                .addContainerGap())
        );

        sendingHardwareTabbedPane.addTab("/dev/lirc", new javax.swing.ImageIcon(getClass().getResource("/icons/tux/tux-22.png")), devLircPanel); // NOI18N

        transmitAudioHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        transmitAudioHelpButton.setText("Help");
        transmitAudioHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transmitAudioHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout audioPanelLayout = new javax.swing.GroupLayout(audioPanel);
        audioPanel.setLayout(audioPanelLayout);
        audioPanelLayout.setHorizontalGroup(
            audioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(audioPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(transmitAudioBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(407, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, audioPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(transmitAudioHelpButton)
                .addContainerGap())
        );
        audioPanelLayout.setVerticalGroup(
            audioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(audioPanelLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(transmitAudioBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 105, Short.MAX_VALUE)
                .addComponent(transmitAudioHelpButton)
                .addContainerGap())
        );

        sendingHardwareTabbedPane.addTab("Audio Port", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/mix_audio.png")), audioPanel); // NOI18N

        sendingGirsClientHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        sendingGirsClientHelpButton.setText("Help");
        sendingGirsClientHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendingGirsClientHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout girsClientPanelLayout = new javax.swing.GroupLayout(girsClientPanel);
        girsClientPanel.setLayout(girsClientPanelLayout);
        girsClientPanelLayout.setHorizontalGroup(
            girsClientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, girsClientPanelLayout.createSequentialGroup()
                .addContainerGap(763, Short.MAX_VALUE)
                .addComponent(sendingGirsClientHelpButton)
                .addContainerGap())
            .addGroup(girsClientPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(girsTcpSerialComboBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        girsClientPanelLayout.setVerticalGroup(
            girsClientPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(girsClientPanelLayout.createSequentialGroup()
                .addComponent(girsTcpSerialComboBean, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(sendingGirsClientHelpButton)
                .addContainerGap())
        );

        sendingHardwareTabbedPane.addTab("Girs Client", girsClientPanel);

        sendingCommandFusionHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        sendingCommandFusionHelpButton.setText("Help");
        sendingCommandFusionHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendingCommandFusionHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout commandFusionSendPanelLayout = new javax.swing.GroupLayout(commandFusionSendPanel);
        commandFusionSendPanel.setLayout(commandFusionSendPanelLayout);
        commandFusionSendPanelLayout.setHorizontalGroup(
            commandFusionSendPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, commandFusionSendPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(sendingCommandFusionHelpButton)
                .addContainerGap())
            .addGroup(commandFusionSendPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(commandFusionBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(31, Short.MAX_VALUE))
        );
        commandFusionSendPanelLayout.setVerticalGroup(
            commandFusionSendPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(commandFusionSendPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(commandFusionBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 100, Short.MAX_VALUE)
                .addComponent(sendingCommandFusionHelpButton)
                .addContainerGap())
        );

        sendingHardwareTabbedPane.addTab("Command Fusion", commandFusionSendPanel);

        irWidgetHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        irWidgetHelpButton.setText("Help");
        irWidgetHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irWidgetHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout captureIrWidgetPanelLayout = new javax.swing.GroupLayout(captureIrWidgetPanel);
        captureIrWidgetPanel.setLayout(captureIrWidgetPanelLayout);
        captureIrWidgetPanelLayout.setHorizontalGroup(
            captureIrWidgetPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(captureIrWidgetPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(irWidgetBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, captureIrWidgetPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(irWidgetHelpButton)
                .addContainerGap())
        );
        captureIrWidgetPanelLayout.setVerticalGroup(
            captureIrWidgetPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(captureIrWidgetPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(irWidgetBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 100, Short.MAX_VALUE)
                .addComponent(irWidgetHelpButton)
                .addContainerGap())
        );

        sendingHardwareTabbedPane.addTab("IrWidget", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/usb.png")), captureIrWidgetPanel); // NOI18N

        noTransmitsComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5", "6", "7", "10", "12", "15", "20", "30", "40", "50", "70", "100" }));
        noTransmitsComboBox.setSelectedItem(Integer.toString(properties.getTransmitGeneratedCount()));
        noTransmitsComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                noTransmitsComboBoxActionPerformed(evt);
            }
        });

        jLabel5.setText("Count");

        transmitScrutinizedButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/1rightarrow.png"))); // NOI18N
        transmitScrutinizedButton.setText("Transmit scrutinized");
        transmitScrutinizedButton.setToolTipText("Transmit the signal defined on the \"Scrutinize signal\" pane using the selected hardware.");
        transmitScrutinizedButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transmitScrutinizedButtonActionPerformed(evt);
            }
        });

        transmitGenerateButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/1rightarrow.png"))); // NOI18N
        transmitGenerateButton2.setText("Transmit rendered");
        transmitGenerateButton2.setToolTipText("Transmit the signal defined on the \"Render\" pane using the selected hardware.");
        transmitGenerateButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transmitGenerateButton2ActionPerformed(evt);
            }
        });

        sendingHardwareHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        sendingHardwareHelpButton.setText("Help");
        sendingHardwareHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendingHardwareHelpButtonActionPerformed(evt);
            }
        });

        captureTestButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/mix_record.png"))); // NOI18N
        captureTestButton.setText("Test capture");
        captureTestButton.setToolTipText("For testing the setup only.  Use \"Scrutinize signal\" or \"Scrutinize remote\" for deployment.");
        captureTestButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                captureTestButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout sendingPanelLayout = new javax.swing.GroupLayout(sendingPanel);
        sendingPanel.setLayout(sendingPanelLayout);
        sendingPanelLayout.setHorizontalGroup(
            sendingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sendingPanelLayout.createSequentialGroup()
                .addGroup(sendingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, sendingPanelLayout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(sendingHardwareHelpButton))
                    .addGroup(sendingPanelLayout.createSequentialGroup()
                        .addGroup(sendingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(sendingPanelLayout.createSequentialGroup()
                                .addComponent(transmitScrutinizedButton)
                                .addGap(18, 18, 18)
                                .addComponent(transmitGenerateButton2)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(noTransmitsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(98, 98, 98)
                                .addComponent(captureTestButton, javax.swing.GroupLayout.PREFERRED_SIZE, 191, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(sendingHardwareTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 853, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 128, Short.MAX_VALUE)))
                .addContainerGap())
        );

        sendingPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {transmitGenerateButton2, transmitScrutinizedButton});

        sendingPanelLayout.setVerticalGroup(
            sendingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sendingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sendingHardwareTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(sendingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(transmitScrutinizedButton)
                    .addComponent(transmitGenerateButton2)
                    .addComponent(jLabel5)
                    .addComponent(noTransmitsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(captureTestButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 19, Short.MAX_VALUE)
                .addComponent(sendingHardwareHelpButton)
                .addContainerGap())
        );

        topLevelTabbedPane.addTab("Hardware", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/hardware.png")), sendingPanel); // NOI18N

        topLevelTabbedPane.setSelectedIndex(properties.getSelectedMainPaneIndex());

        topLevelSplitPane.setTopComponent(topLevelTabbedPane);

        fileMenu.setMnemonic('F');
        fileMenu.setText("File");

        saveMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/fileexport.png"))); // NOI18N
        saveMenu.setMnemonic('S');
        saveMenu.setText("Export");
        saveMenu.setToolTipText("Save in the program's native format.");

        saveCapturedMenu.setText("Captured signal");

        exportSignalGirrMenuItem.setText("Girr");
        exportSignalGirrMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSignalGirrMenuItemActionPerformed(evt);
            }
        });
        saveCapturedMenu.add(exportSignalGirrMenuItem);

        exportSignalIctMenuItem.setText("IrScope ICT");
        exportSignalIctMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSignalIctMenuItemActionPerformed(evt);
            }
        });
        saveCapturedMenu.add(exportSignalIctMenuItem);

        signalSignalTextMenuItem.setText("Text");
        signalSignalTextMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                signalSignalTextMenuItemActionPerformed(evt);
            }
        });
        saveCapturedMenu.add(signalSignalTextMenuItem);

        exportSignalWaveMenuItem.setText("Wave");
        exportSignalWaveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSignalWaveMenuItemActionPerformed(evt);
            }
        });
        saveCapturedMenu.add(exportSignalWaveMenuItem);

        saveMenu.add(saveCapturedMenu);

        saveParametrizedMenu.setText("Parametrized Signals");

        exportParametricAsGirrMenuItem.setText("Girr");
        exportParametricAsGirrMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportParametricAsGirrMenuItemActionPerformed(evt);
            }
        });
        saveParametrizedMenu.add(exportParametricAsGirrMenuItem);

        exportParametricAsTextMenuItem.setText("Text");
        exportParametricAsTextMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportParametricAsTextMenuItemActionPerformed(evt);
            }
        });
        saveParametrizedMenu.add(exportParametricAsTextMenuItem);

        saveMenu.add(saveParametrizedMenu);

        saveRawMenu.setText("Raw Signals");

        exportRawAsGirrMenuItem.setText("Girr");
        exportRawAsGirrMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportRawAsGirrMenuItemActionPerformed(evt);
            }
        });
        saveRawMenu.add(exportRawAsGirrMenuItem);

        exportRawAsTextMenuItem.setText("Text");
        exportRawAsTextMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportRawAsTextMenuItemActionPerformed(evt);
            }
        });
        saveRawMenu.add(exportRawAsTextMenuItem);

        saveMenu.add(saveRawMenu);

        fileMenu.add(saveMenu);

        loadMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/fileimport.png"))); // NOI18N
        loadMenu.setText("Import");

        importSignalMenu.setText("Import as single sequence (possibly concatenating)");

        importSignalAsGirrMenuItem.setText("Girr");
        importSignalAsGirrMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importSignalAsGirrMenuItemActionPerformed(evt);
            }
        });
        importSignalMenu.add(importSignalAsGirrMenuItem);

        importSignalAsIctMenuItem.setText("IrScope ICT");
        importSignalAsIctMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importSignalAsIctMenuItemActionPerformed(evt);
            }
        });
        importSignalMenu.add(importSignalAsIctMenuItem);

        importSignalAsWaveMenuItem.setText("Wave");
        importSignalAsWaveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importSignalAsWaveMenuItemActionPerformed(evt);
            }
        });
        importSignalMenu.add(importSignalAsWaveMenuItem);

        importSignalAsMode2MenuItem.setText("Mode2");
        importSignalAsMode2MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importSignalAsMode2MenuItemActionPerformed(evt);
            }
        });
        importSignalMenu.add(importSignalAsMode2MenuItem);

        loadMenu.add(importSignalMenu);

        importParametricMenu.setText("Import as set of parametric signals");

        importGirrMenuItem1.setText("Girr");
        importGirrMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importGirrMenuItem1ActionPerformed(evt);
            }
        });
        importParametricMenu.add(importGirrMenuItem1);

        importIctMenuItem1.setText("IrScope ict");
        importIctMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importIctMenuItem1ActionPerformed(evt);
            }
        });
        importParametricMenu.add(importIctMenuItem1);

        importLircMenuItem1.setText("LIRC");
        importLircMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importLircMenuItem1ActionPerformed(evt);
            }
        });
        importParametricMenu.add(importLircMenuItem1);

        importCcfMenuItem1.setText("Pronto Classic");
        importCcfMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importCcfMenuItem1ActionPerformed(evt);
            }
        });
        importParametricMenu.add(importCcfMenuItem1);

        importXcfMenuItem1.setText("Pronto xcf");
        importXcfMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importXcfMenuItem1ActionPerformed(evt);
            }
        });
        importParametricMenu.add(importXcfMenuItem1);

        importCmlMenuItem.setText("CML");
        importCmlMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importCmlMenuItemActionPerformed(evt);
            }
        });
        importParametricMenu.add(importCmlMenuItem);

        importCommandFusionMenuItem.setText("CommandFusion");
        importCommandFusionMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importCommandFusionMenuItemActionPerformed(evt);
            }
        });
        importParametricMenu.add(importCommandFusionMenuItem);

        loadMenu.add(importParametricMenu);

        importRawMenu.setText("Import as set of raw signals");

        importGirrMenuItem.setText("Girr");
        importGirrMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importGirrMenuItemActionPerformed(evt);
            }
        });
        importRawMenu.add(importGirrMenuItem);

        importIctMenuItem.setText("IrScope ict");
        importIctMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importIctMenuItemActionPerformed(evt);
            }
        });
        importRawMenu.add(importIctMenuItem);

        importLircMenuItem.setText("LIRC");
        importLircMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importLircMenuItemActionPerformed(evt);
            }
        });
        importRawMenu.add(importLircMenuItem);

        importCcfMenuItem.setText("Pronto Classic");
        importCcfMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importCcfMenuItemActionPerformed(evt);
            }
        });
        importRawMenu.add(importCcfMenuItem);

        importXcfMenuItem.setText("Pronto xcf");
        importXcfMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importXcfMenuItemActionPerformed(evt);
            }
        });
        importRawMenu.add(importXcfMenuItem);

        importCmlMenuItem2.setText("CML");
        importCmlMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importCmlMenuItem2ActionPerformed(evt);
            }
        });
        importRawMenu.add(importCmlMenuItem2);

        importCommandFusionMenuItem2.setText("CommandFusion");
        importCommandFusionMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importCommandFusionMenuItem2ActionPerformed(evt);
            }
        });
        importRawMenu.add(importCommandFusionMenuItem2);

        loadMenu.add(importRawMenu);

        fileMenu.add(loadMenu);
        fileMenu.add(jSeparator5);

        savePropertiesMenuItem.setText("Save properties");
        savePropertiesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                savePropertiesMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(savePropertiesMenuItem);

        savePropertiesAsMenuItem.setText("Save properties as...");
        savePropertiesAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                savePropertiesAsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(savePropertiesAsMenuItem);

        resetPropertiesMenuItem.setText("Set properties to default");
        resetPropertiesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetPropertiesMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(resetPropertiesMenuItem);
        fileMenu.add(jSeparator6);

        exitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        exitMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/stop.png"))); // NOI18N
        exitMenuItem.setMnemonic('X');
        exitMenuItem.setText("Exit");
        exitMenuItem.setToolTipText("You have three guesses what this does...");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setMnemonic('E');
        editMenu.setText("Edit");

        pasteToDataWindowMenuItem.setText("Paste signal to data window");
        pasteToDataWindowMenuItem.setToolTipText("Paste clipboard content to the data window and analyze.");
        pasteToDataWindowMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pasteToDataWindowMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(pasteToDataWindowMenuItem);

        pasteScrutinizeToDataWindowMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, 0));
        pasteScrutinizeToDataWindowMenuItem.setText("Paste & scrutinize signal");
        pasteScrutinizeToDataWindowMenuItem.setToolTipText("Paste clipboard content to the data window and analyze.");
        pasteScrutinizeToDataWindowMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pasteScrutinizeToDataWindowMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(pasteScrutinizeToDataWindowMenuItem);

        saveDataTextAsMenuItem.setText("Save data text as...");
        saveDataTextAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveDataTextAsMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(saveDataTextAsMenuItem);

        copyDataToClipboardMenuItem.setText("Copy data to clipboard");
        copyDataToClipboardMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyDataToClipboardMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(copyDataToClipboardMenuItem);
        editMenu.add(jSeparator7);

        copyConsoleToClipboardMenuItem.setText("Copy console to clipboard");
        copyConsoleToClipboardMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyConsoleToClipboardMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(copyConsoleToClipboardMenuItem);

        clearConsoleMenuItem.setText("Clear console");
        clearConsoleMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearConsoleMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(clearConsoleMenuItem);

        saveConsoleTextAsMenuItem.setText("Save console text as...");
        saveConsoleTextAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveConsoleTextAsMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(saveConsoleTextAsMenuItem);
        editMenu.add(jSeparator10);

        analysisToClipboardMenuItem.setText("Enter analysis into clipboard");
        analysisToClipboardMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                analysisToClipboardMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(analysisToClipboardMenuItem);

        menuBar.add(editMenu);

        actionsMenu.setMnemonic('A');
        actionsMenu.setText("Actions");

        reAnalyzeMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
        reAnalyzeMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/gear.png"))); // NOI18N
        reAnalyzeMenuItem.setText("Re-Scrutinize");
        reAnalyzeMenuItem.setToolTipText("Act on the data presently in the data window");
        reAnalyzeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reAnalyzeMenuItemActionPerformed(evt);
            }
        });
        actionsMenu.add(reAnalyzeMenuItem);

        undoMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        undoMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/undo.png"))); // NOI18N
        undoMenuItem.setMnemonic('U');
        undoMenuItem.setText("Undo Scrutinize Data");
        undoMenuItem.setToolTipText("Undo last operation that changed the scrutinize data");
        undoMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                undoMenuItemActionPerformed(evt);
            }
        });
        actionsMenu.add(undoMenuItem);

        startCaptureMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/mix_record.png"))); // NOI18N
        startCaptureMenuItem.setText("Start capture");
        startCaptureMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startCaptureMenuItemActionPerformed(evt);
            }
        });
        actionsMenu.add(startCaptureMenuItem);

        jMenuItem12.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/player_stop.png"))); // NOI18N
        jMenuItem12.setText("Stop capture");
        jMenuItem12.setToolTipText("not yet implemented");
        jMenuItem12.setEnabled(false);
        actionsMenu.add(jMenuItem12);

        testSignalMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F9, 0));
        testSignalMenuItem.setText("Enter test signal (NEC1 12.34 56)");
        testSignalMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testSignalMenuItemActionPerformed(evt);
            }
        });
        actionsMenu.add(testSignalMenuItem);

        plotterResetMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, 0));
        plotterResetMenuItem.setText("Reset plot");
        plotterResetMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                plotterResetMenuItemActionPerformed(evt);
            }
        });
        actionsMenu.add(plotterResetMenuItem);

        clonePlotMenuItem.setText("Clone plot");
        clonePlotMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clonePlotMenuItemActionPerformed(evt);
            }
        });
        actionsMenu.add(clonePlotMenuItem);

        clearSignalMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/eraser.png"))); // NOI18N
        clearSignalMenuItem.setText("Clear signal");
        clearSignalMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearSignalMenuItemActionPerformed(evt);
            }
        });
        actionsMenu.add(clearSignalMenuItem);

        openLastExportFileMenuItem.setText("Open last export file");
        openLastExportFileMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openLastExportFileMenuItemActionPerformed(evt);
            }
        });
        actionsMenu.add(openLastExportFileMenuItem);
        actionsMenu.add(jSeparator21);

        jumpToLastPanelMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, java.awt.event.InputEvent.ALT_DOWN_MASK));
        jumpToLastPanelMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/previous.png"))); // NOI18N
        jumpToLastPanelMenuItem.setText("Jump to last panel");
        jumpToLastPanelMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jumpToLastPanelMenuItemActionPerformed(evt);
            }
        });
        actionsMenu.add(jumpToLastPanelMenuItem);

        menuBar.add(actionsMenu);

        optionsMenu.setMnemonic('O');
        optionsMenu.setText("Options");
        optionsMenu.setToolTipText("If selected, decoded parameters having their default values are silently removed.");
        optionsMenu.add(jSeparator3);

        outputFormatMenu.setText("Output Text Format");
        outputFormatMenu.setToolTipText("The text format for writing IR signals in the data window.");

        ccfRadioButtonMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.ALT_DOWN_MASK));
        ccfRadioButtonMenuItem.setSelected(true);
        ccfRadioButtonMenuItem.setText("Pronto Hex");
        ccfRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ccfRadioButtonMenuItemActionPerformed(evt);
            }
        });
        outputFormatMenu.add(ccfRadioButtonMenuItem);

        rawRadioButtonMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.ALT_DOWN_MASK));
        rawRadioButtonMenuItem.setText("Raw (with signs)");
        rawRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawRadioButtonMenuItemActionPerformed(evt);
            }
        });
        outputFormatMenu.add(rawRadioButtonMenuItem);

        rawWithoutSignsRadioButtonMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        rawWithoutSignsRadioButtonMenuItem.setText("Raw (without signs)");
        rawWithoutSignsRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawWithoutSignsRadioButtonMenuItemActionPerformed(evt);
            }
        });
        outputFormatMenu.add(rawWithoutSignsRadioButtonMenuItem);

        optionsMenu.add(outputFormatMenu);

        analyzerBasisMenu.setText("Analyzer Number Basis");
        analyzerBasisMenu.setToolTipText("Radix for the analyzer to print the parameters.");

        analyzerBaseButtonGroup.add(analyzerBase2RadioButtonMenuItem);
        analyzerBase2RadioButtonMenuItem.setSelected(properties.getAnalyzerBase() == 2);
        analyzerBase2RadioButtonMenuItem.setText("2");
        analyzerBase2RadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                analyzerBase2RadioButtonMenuItemActionPerformed(evt);
            }
        });
        analyzerBasisMenu.add(analyzerBase2RadioButtonMenuItem);

        analyzerBaseButtonGroup.add(analyzerBase4RadioButtonMenuItem);
        analyzerBase4RadioButtonMenuItem.setSelected(properties.getAnalyzerBase() == 4);
        analyzerBase4RadioButtonMenuItem.setText("4");
        analyzerBase4RadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                analyzerBase4RadioButtonMenuItemActionPerformed(evt);
            }
        });
        analyzerBasisMenu.add(analyzerBase4RadioButtonMenuItem);

        analyzerBaseButtonGroup.add(analyzerBase8RadioButtonMenuItem);
        analyzerBase8RadioButtonMenuItem.setSelected(properties.getAnalyzerBase() == 8);
        analyzerBase8RadioButtonMenuItem.setText("8");
        analyzerBase8RadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                analyzerBase8RadioButtonMenuItemActionPerformed(evt);
            }
        });
        analyzerBasisMenu.add(analyzerBase8RadioButtonMenuItem);

        analyzerBaseButtonGroup.add(analyzerBase10RadioButtonMenuItem);
        analyzerBase10RadioButtonMenuItem.setSelected(properties.getAnalyzerBase() == 10);
        analyzerBase10RadioButtonMenuItem.setText("10");
        analyzerBase10RadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                analyzerBase10RadioButtonMenuItemActionPerformed(evt);
            }
        });
        analyzerBasisMenu.add(analyzerBase10RadioButtonMenuItem);

        analyzerBaseButtonGroup.add(analyzerBase16RadioButtonMenuItem);
        analyzerBase16RadioButtonMenuItem.setSelected(properties.getAnalyzerBase() == 16);
        analyzerBase16RadioButtonMenuItem.setText("16");
        analyzerBase16RadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                analyzerBase16RadioButtonMenuItemActionPerformed(evt);
            }
        });
        analyzerBasisMenu.add(analyzerBase16RadioButtonMenuItem);

        optionsMenu.add(analyzerBasisMenu);
        optionsMenu.add(jSeparator2);

        lafMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/looknfeel.png"))); // NOI18N
        lafMenu.setText("Look and Feel");
        optionsMenu.add(lafMenu);

        usePopupsMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/window_new.png"))); // NOI18N
        usePopupsMenu.setText("Use popups for...");

        usePopupsForHelpCheckBoxMenuItem.setSelected(properties.getUsePopupsForHelp());
        usePopupsForHelpCheckBoxMenuItem.setText("Use popups for help");
        usePopupsForHelpCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                usePopupsForHelpCheckBoxMenuItemActionPerformed(evt);
            }
        });
        usePopupsMenu.add(usePopupsForHelpCheckBoxMenuItem);

        usePopupsForErrorsCheckBoxMenuItem.setSelected(properties.getUsePopupsForErrors());
        usePopupsForErrorsCheckBoxMenuItem.setText("Use popups for errors etc.");
        usePopupsForErrorsCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                usePopupsForErrorsCheckBoxMenuItemActionPerformed(evt);
            }
        });
        usePopupsMenu.add(usePopupsForErrorsCheckBoxMenuItem);

        optionsMenu.add(usePopupsMenu);
        optionsMenu.add(jSeparator16);

        timeoutMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/xclock.png"))); // NOI18N
        timeoutMenu.setText("Timeouts");
        timeoutMenu.setToolTipText("Configure different timeout values.");

        startTimeoutMenuItem.setText("Start capture (beginTimeout) ...");
        startTimeoutMenuItem.setToolTipText("Set the beginTimeout parameter. This determines how long the capturer waits for the signal to begin.");
        startTimeoutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startTimeoutMenuItemActionPerformed(evt);
            }
        });
        timeoutMenu.add(startTimeoutMenuItem);

        lengthMenuItem.setText("Max capture length (captureMaxSize)...");
        lengthMenuItem.setToolTipText("Set the maximal number of durations the capturing device will accept.");
        lengthMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lengthMenuItemActionPerformed(evt);
            }
        });
        timeoutMenu.add(lengthMenuItem);

        endingTimeoutMenuItem.setText("Ending silence (endingTimeout)...");
        endingTimeoutMenuItem.setToolTipText("Sets the endingTimeout parameters. This determines the period of silence required to end an IrSequence.");
        endingTimeoutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                endingTimeoutMenuItemActionPerformed(evt);
            }
        });
        timeoutMenu.add(endingTimeoutMenuItem);
        timeoutMenu.add(jSeparator15);

        sendingTimeoutMenuItem.setText("Timeout sending");
        sendingTimeoutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendingTimeoutMenuItemActionPerformed(evt);
            }
        });
        timeoutMenu.add(sendingTimeoutMenuItem);
        timeoutMenu.add(jSeparator26);

        globalCacheTimeoutMenuItem.setText("GlobalCache socket timeout");
        globalCacheTimeoutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                globalCacheTimeoutMenuItemActionPerformed(evt);
            }
        });
        timeoutMenu.add(globalCacheTimeoutMenuItem);

        optionsMenu.add(timeoutMenu);

        protocolParametersMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/package_settings.png"))); // NOI18N
        protocolParametersMenu.setText("Protocol Parameters");
        protocolParametersMenu.setToolTipText("Parameters for IrpTransmogrifier (see IrpTransmogrifier for documentation).");

        absToleranceMenuItem.setText("Absolute tolerance (µs)");
        absToleranceMenuItem.setToolTipText("Absolute tolerance for duration comparisons");
        absToleranceMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                absToleranceMenuItemActionPerformed(evt);
            }
        });
        protocolParametersMenu.add(absToleranceMenuItem);

        relToleranceMenuItem.setText("Relative tolerance (fraction <1)");
        relToleranceMenuItem.setToolTipText("Relative tolerance for duration comparisons");
        relToleranceMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                relToleranceMenuItemActionPerformed(evt);
            }
        });
        protocolParametersMenu.add(relToleranceMenuItem);

        frequencyToleranceMenuItem.setText("Frequency tolerance (Hz)");
        frequencyToleranceMenuItem.setToolTipText("Absolute tolerance for frequency comparisons");
        frequencyToleranceMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                frequencyToleranceMenuItemActionPerformed(evt);
            }
        });
        protocolParametersMenu.add(frequencyToleranceMenuItem);

        minLeadoutMenuItem.setText("Min. Leadout (µs)");
        minLeadoutMenuItem.setToolTipText("Threshold value for lead-out in microseconds.");
        minLeadoutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                minLeadoutMenuItemActionPerformed(evt);
            }
        });
        protocolParametersMenu.add(minLeadoutMenuItem);

        minRepeatGapMenuItem.setText("Min. Repeatgap (µs)");
        minRepeatGapMenuItem.setToolTipText("Minumal value in micro seconds to be considered as ending a repeat.");
        minRepeatGapMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                minRepeatGapMenuItemActionPerformed(evt);
            }
        });
        protocolParametersMenu.add(minRepeatGapMenuItem);

        dummyGapMenuItem.setText("Dummy Gap (µs)");
        dummyGapMenuItem.setToolTipText("Dummy gap added for sequences with an odd number of durations.");
        dummyGapMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dummyGapMenuItemActionPerformed(evt);
            }
        });
        protocolParametersMenu.add(dummyGapMenuItem);

        chopThresholdMenuItem.setText("Chop threshold (ms)");
        chopThresholdMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chopThresholdMenuItemActionPerformed(evt);
            }
        });
        protocolParametersMenu.add(chopThresholdMenuItem);

        optionsMenu.add(protocolParametersMenu);

        fallbackFrequencyMenuItem.setText("Fallback Frequency...");
        fallbackFrequencyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fallbackFrequencyMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(fallbackFrequencyMenuItem);

        verboseCheckBoxMenuItem.setSelected(properties.getVerbose());
        verboseCheckBoxMenuItem.setText("Verbose");
        verboseCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verboseCheckBoxMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(verboseCheckBoxMenuItem);

        ignoreEndingSilenceCheckBoxMenuItem.setSelected(properties.getIgnoreEndingSilence());
        ignoreEndingSilenceCheckBoxMenuItem.setText("Ignore ending silence");
        ignoreEndingSilenceCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ignoreEndingSilenceCheckBoxMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(ignoreEndingSilenceCheckBoxMenuItem);

        repeatFinderCheckBoxMenuItem.setSelected(properties.getInvokeRepeatFinder());
        repeatFinderCheckBoxMenuItem.setText("Invoke repeat finder");
        repeatFinderCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                repeatFinderCheckBoxMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(repeatFinderCheckBoxMenuItem);

        cleanerCheckBoxMenuItem.setSelected(properties.getInvokeCleaner());
        cleanerCheckBoxMenuItem.setText("Invoke signal cleaner");
        cleanerCheckBoxMenuItem.setToolTipText("If selected, signal cleaning will be invoked on signals from the data pane and from the clipboard");
        cleanerCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cleanerCheckBoxMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(cleanerCheckBoxMenuItem);

        invokeAnalyzerCheckBoxMenuItem.setSelected(properties.getInvokeAnalyzer());
        invokeAnalyzerCheckBoxMenuItem.setText("Invoke analyzer");
        invokeAnalyzerCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                invokeAnalyzerCheckBoxMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(invokeAnalyzerCheckBoxMenuItem);

        printAnalyzeIRPsToConsoleCheckBoxMenuItem.setSelected(properties.getPrintAnalyzerIRPsToConsole());
        printAnalyzeIRPsToConsoleCheckBoxMenuItem.setText("Print analyze-IRPs to console");
        printAnalyzeIRPsToConsoleCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printAnalyzeIRPsToConsoleCheckBoxMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(printAnalyzeIRPsToConsoleCheckBoxMenuItem);

        printDecodesToConsoleCheckBoxMenuItem.setSelected(properties.getPrintDecodesToConsole());
        printDecodesToConsoleCheckBoxMenuItem.setText("Print decodes to console");
        printDecodesToConsoleCheckBoxMenuItem.setToolTipText("If selected, all decodes are additionally printed to the console window.");
        printDecodesToConsoleCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printDecodesToConsoleCheckBoxMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(printDecodesToConsoleCheckBoxMenuItem);

        printAlternativeDecodesCheckBoxMenuItem.setSelected(properties.getPrintAlternativeDecodes());
        printAlternativeDecodesCheckBoxMenuItem.setText("Print alternative decodes");
        printAlternativeDecodesCheckBoxMenuItem.setToolTipText("If an IrSignal/IrSequence has more than one decode, print the alternative ones to the console.");
        printAlternativeDecodesCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printAlternativeDecodesCheckBoxMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(printAlternativeDecodesCheckBoxMenuItem);

        parametrizedLearnIgnoreTCheckBoxMenuItem.setSelected(properties.getParametrizedLearnIgnoreT());
        parametrizedLearnIgnoreTCheckBoxMenuItem.setText("Ignore T on parametric learns");
        parametrizedLearnIgnoreTCheckBoxMenuItem.setToolTipText("If selected, the value of the T variable (if present) will discarded on captured variables.");
        parametrizedLearnIgnoreTCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parametrizedLearnIgnoreTCheckBoxMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(parametrizedLearnIgnoreTCheckBoxMenuItem);

        removeDefaultedParametersCheckBoxMenuItem.setSelected(properties.getRemoveDefaultedParameters());
        removeDefaultedParametersCheckBoxMenuItem.setText("Remove defaulted parameters in decode");
        removeDefaultedParametersCheckBoxMenuItem.setToolTipText("If selected, decode parameters having their default values are silently removed.");
        removeDefaultedParametersCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeDefaultedParametersCheckBoxMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(removeDefaultedParametersCheckBoxMenuItem);

        ignoreLeadingGarbageCheckBoxMenuItem.setSelected(properties.getIgnoreLeadingGarbage());
        ignoreLeadingGarbageCheckBoxMenuItem.setText("Ignore leading garbage on decode");
        ignoreLeadingGarbageCheckBoxMenuItem.setToolTipText("Experimental!!");
        ignoreLeadingGarbageCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ignoreLeadingGarbageCheckBoxMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(ignoreLeadingGarbageCheckBoxMenuItem);

        irpProtocolsIniMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/database.png"))); // NOI18N
        irpProtocolsIniMenu.setText("IRP protocol database");

        irpProtocolsSelectMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/fileopen.png"))); // NOI18N
        irpProtocolsSelectMenuItem.setText("Select...");
        irpProtocolsSelectMenuItem.setToolTipText("Select IrpProtocols.xml to use.");
        irpProtocolsSelectMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irpProtocolsSelectMenuItemActionPerformed(evt);
            }
        });
        irpProtocolsIniMenu.add(irpProtocolsSelectMenuItem);

        irpProtocolsEditMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/edit.png"))); // NOI18N
        irpProtocolsEditMenuItem.setText("Edit...");
        irpProtocolsEditMenuItem.setToolTipText("Edit selected IrpProtocols.xml.");
        irpProtocolsEditMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irpProtocolsEditMenuItemActionPerformed(evt);
            }
        });
        irpProtocolsIniMenu.add(irpProtocolsEditMenuItem);
        irpProtocolsIniMenu.add(jSeparator45);

        secondaryIrpProtocolsSelectMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/fileopen.png"))); // NOI18N
        secondaryIrpProtocolsSelectMenuItem.setText("Select secondary...");
        secondaryIrpProtocolsSelectMenuItem.setToolTipText("Select a private version of IrpProtocols. to extend the standard one.");
        secondaryIrpProtocolsSelectMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                secondaryIrpProtocolsSelectMenuItemActionPerformed(evt);
            }
        });
        irpProtocolsIniMenu.add(secondaryIrpProtocolsSelectMenuItem);

        secondaryIrpProtocolsEditMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/edit.png"))); // NOI18N
        secondaryIrpProtocolsEditMenuItem.setText("Edit secondary...");
        secondaryIrpProtocolsEditMenuItem.setToolTipText("Edit the private extension file for IrpProtocols.xml");
        secondaryIrpProtocolsEditMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                secondaryIrpProtocolsEditMenuItemActionPerformed(evt);
            }
        });
        irpProtocolsIniMenu.add(secondaryIrpProtocolsEditMenuItem);
        irpProtocolsIniMenu.add(jSeparator46);

        irpProtocolsReloadMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/reload.png"))); // NOI18N
        irpProtocolsReloadMenuItem.setText("Reload");
        irpProtocolsReloadMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irpProtocolsReloadMenuItemActionPerformed(evt);
            }
        });
        irpProtocolsIniMenu.add(irpProtocolsReloadMenuItem);

        optionsMenu.add(irpProtocolsIniMenu);

        exportFormatsMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/database.png"))); // NOI18N
        exportFormatsMenu.setText("Export formats database");

        exportFormatsEditMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/edit.png"))); // NOI18N
        exportFormatsEditMenuItem.setText("Open...");
        exportFormatsEditMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportFormatsEditMenuItemActionPerformed(evt);
            }
        });
        exportFormatsMenu.add(exportFormatsEditMenuItem);

        exportFormatsSelectMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/fileopen.png"))); // NOI18N
        exportFormatsSelectMenuItem.setText("Select...");
        exportFormatsSelectMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportFormatsSelectMenuItemActionPerformed(evt);
            }
        });
        exportFormatsMenu.add(exportFormatsSelectMenuItem);
        exportFormatsMenu.add(jSeparator47);

        secondaryExportFormatsEditMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/edit.png"))); // NOI18N
        secondaryExportFormatsEditMenuItem.setText("Open secondary...");
        secondaryExportFormatsEditMenuItem.setToolTipText("Edit the private export formats file or directory.");
        secondaryExportFormatsEditMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                secondaryExportFormatsEditMenuItemActionPerformed(evt);
            }
        });
        exportFormatsMenu.add(secondaryExportFormatsEditMenuItem);

        secondaryExportFormatsSelectMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/fileopen.png"))); // NOI18N
        secondaryExportFormatsSelectMenuItem.setText("Select secondary...");
        secondaryExportFormatsSelectMenuItem.setToolTipText("Select a private export formats file or directory.");
        secondaryExportFormatsSelectMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                secondaryExportFormatsSelectMenuItemActionPerformed(evt);
            }
        });
        exportFormatsMenu.add(secondaryExportFormatsSelectMenuItem);
        exportFormatsMenu.add(jSeparator48);

        exportFormatsReloadMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/reload.png"))); // NOI18N
        exportFormatsReloadMenuItem.setText("Reload");
        exportFormatsReloadMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportFormatsReloadMenuItemActionPerformed(evt);
            }
        });
        exportFormatsMenu.add(exportFormatsReloadMenuItem);

        optionsMenu.add(exportFormatsMenu);
        optionsMenu.add(jSeparator20);

        importOptionsMenu.setText("Import options");

        importCharsetMenuItem.setText("Character Set...");
        importCharsetMenuItem.setToolTipText("Select the character set used by imports.");
        importCharsetMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importCharsetMenuItemActionPerformed(evt);
            }
        });
        importOptionsMenu.add(importCharsetMenuItem);

        openZipFilesCheckBoxMenuItem.setSelected(properties.getImportOpensZipFiles());
        openZipFilesCheckBoxMenuItem.setText("Open ZIP files");
        openZipFilesCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openZipFilesCheckBoxMenuItemActionPerformed(evt);
            }
        });
        importOptionsMenu.add(openZipFilesCheckBoxMenuItem);

        invokeDecoderCheckBoxMenuItem.setSelected(properties.getInvokeDecoder());
        invokeDecoderCheckBoxMenuItem.setText("Invoke Decoder");
        invokeDecoderCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                invokeDecoderCheckBoxMenuItemActionPerformed(evt);
            }
        });
        importOptionsMenu.add(invokeDecoderCheckBoxMenuItem);

        generateRawCheckBoxMenuItem.setSelected(properties.getGenerateRaw());
        generateRawCheckBoxMenuItem.setText("Generate Raw");
        generateRawCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generateRawCheckBoxMenuItemActionPerformed(evt);
            }
        });
        importOptionsMenu.add(generateRawCheckBoxMenuItem);

        generateCcfCheckBoxMenuItem.setSelected(properties.getGenerateCcf());
        generateCcfCheckBoxMenuItem.setText("Generate CCF");
        generateCcfCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generateCcfCheckBoxMenuItemActionPerformed(evt);
            }
        });
        importOptionsMenu.add(generateCcfCheckBoxMenuItem);

        translateProntoFontCheckBoxMenuItem.setSelected(properties.getTranslateProntoFont());
        translateProntoFontCheckBoxMenuItem.setText("Translate Pronto font");
        translateProntoFontCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                translateProntoFontCheckBoxMenuItemActionPerformed(evt);
            }
        });
        importOptionsMenu.add(translateProntoFontCheckBoxMenuItem);

        girrValidateCheckBoxMenuItem.setSelected(properties.getGirrValidate());
        girrValidateCheckBoxMenuItem.setText("Girr validate");
        girrValidateCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                girrValidateCheckBoxMenuItemActionPerformed(evt);
            }
        });
        importOptionsMenu.add(girrValidateCheckBoxMenuItem);

        girrSchemaLocationMenuItem.setText("Girr Schema location...");
        girrSchemaLocationMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                girrSchemaLocationMenuItemActionPerformed(evt);
            }
        });
        importOptionsMenu.add(girrSchemaLocationMenuItem);

        girrAcceptEmptyCommandsCheckBoxMenuItem.setSelected(properties.getAllowEmptyGirrCommands());
        girrAcceptEmptyCommandsCheckBoxMenuItem.setText("Accept empty Girr commands on import");
        girrAcceptEmptyCommandsCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                girrAcceptEmptyCommandsCheckBoxMenuItemActionPerformed(evt);
            }
        });
        importOptionsMenu.add(girrAcceptEmptyCommandsCheckBoxMenuItem);

        optionsMenu.add(importOptionsMenu);

        exportOptionsMenu.setText("Export options");

        exportCharsetMenuItem.setText("Character Set...");
        exportCharsetMenuItem.setToolTipText("Select the character set used by exports.");
        exportCharsetMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportCharsetMenuItemActionPerformed(evt);
            }
        });
        exportOptionsMenu.add(exportCharsetMenuItem);

        creatingUserMenuItem.setText("Creating User...");
        creatingUserMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                creatingUserMenuItemActionPerformed(evt);
            }
        });
        exportOptionsMenu.add(creatingUserMenuItem);

        inquiryDeviceDataCheckBoxMenuItem.setSelected(properties.getExportInquireDeviceData());
        inquiryDeviceDataCheckBoxMenuItem.setText("Inquire Device Metadata");
        inquiryDeviceDataCheckBoxMenuItem.setToolTipText("If true, device data (manufacturer, model, name, etc) are inquired before export.");
        inquiryDeviceDataCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inquiryDeviceDataCheckBoxMenuItemActionPerformed(evt);
            }
        });
        exportOptionsMenu.add(inquiryDeviceDataCheckBoxMenuItem);

        optionsMenu.add(exportOptionsMenu);
        optionsMenu.add(jSeparator22);

        debugMenu.setText("Debug");
        debugMenu.setToolTipText("not documented, really... ;-)");

        offerStackTraceCheckBoxMenuItem.setSelected(properties.getOfferStackTrace());
        offerStackTraceCheckBoxMenuItem.setText("Offer Stack Traces");
        offerStackTraceCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                offerStackTraceCheckBoxMenuItemActionPerformed(evt);
            }
        });
        debugMenu.add(offerStackTraceCheckBoxMenuItem);

        debugCodeMenuItem.setText("Debug Code...");
        debugCodeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                debugCodeMenuItemActionPerformed(evt);
            }
        });
        debugMenu.add(debugCodeMenuItem);

        debugDecodeProtocolRegexpMenuItem.setText("Debug Decode Protocol Regexp");
        debugDecodeProtocolRegexpMenuItem.setToolTipText("For debugging only");
        debugDecodeProtocolRegexpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                debugDecodeProtocolRegexpMenuItemActionPerformed(evt);
            }
        });
        debugMenu.add(debugDecodeProtocolRegexpMenuItem);

        optionsMenu.add(debugMenu);

        menuBar.add(optionsMenu);

        toolsMenu.setMnemonic('T');
        toolsMenu.setText("Tools");

        hexCalcMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/calc.png"))); // NOI18N
        hexCalcMenuItem.setText("Hex calculator");
        hexCalcMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hexCalcMenuItemActionPerformed(evt);
            }
        });
        toolsMenu.add(hexCalcMenuItem);

        timeFrequencyCalcMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/xclock.png"))); // NOI18N
        timeFrequencyCalcMenuItem.setText("Time/Frequency Calculator");
        timeFrequencyCalcMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                timeFrequencyCalcMenuItemActionPerformed(evt);
            }
        });
        toolsMenu.add(timeFrequencyCalcMenuItem);

        beaconListenerMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/find.png"))); // NOI18N
        beaconListenerMenuItem.setText("AMX Beacon Listener");
        beaconListenerMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                beaconListenerMenuItemActionPerformed(evt);
            }
        });
        toolsMenu.add(beaconListenerMenuItem);

        NamedCommandMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/cache.png"))); // NOI18N
        NamedCommandMenuItem.setText("Named Command Sender");
        NamedCommandMenuItem.setToolTipText("Tool for sending named command from named remotes.");
        NamedCommandMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NamedCommandMenuItemActionPerformed(evt);
            }
        });
        toolsMenu.add(NamedCommandMenuItem);
        toolsMenu.add(jSeparator1);

        publicKeyMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/password.png"))); // NOI18N
        publicKeyMenuItem.setMnemonic('P');
        publicKeyMenuItem.setText("Author's public PGP key");
        publicKeyMenuItem.setToolTipText("Show the author's public PGP key.");
        publicKeyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                publicKeyMenuItemActionPerformed(evt);
            }
        });
        toolsMenu.add(publicKeyMenuItem);
        toolsMenu.add(jSeparator41);

        checkUpToDateMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/agt_update-product.png"))); // NOI18N
        checkUpToDateMenuItem.setText("Check for Updates");
        checkUpToDateMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkUpToDateMenuItemActionPerformed(evt);
            }
        });
        toolsMenu.add(checkUpToDateMenuItem);

        menuBar.add(toolsMenu);

        helpMenu.setMnemonic('H');
        helpMenu.setText("Help");

        aboutMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/info.png"))); // NOI18N
        aboutMenuItem.setMnemonic('A');
        aboutMenuItem.setText("About...");
        aboutMenuItem.setToolTipText("The obligatory About popup");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);
        helpMenu.add(jSeparator31);

        mainDocuMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        mainDocuMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/mimetypes/man.png"))); // NOI18N
        mainDocuMenuItem.setMnemonic('D');
        mainDocuMenuItem.setText("Program Documentation");
        mainDocuMenuItem.setToolTipText("Show program reference documentation.");
        mainDocuMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mainDocuMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(mainDocuMenuItem);

        irpTransmogrifierHelpMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/mimetypes/source.png"))); // NOI18N
        irpTransmogrifierHelpMenuItem.setText("IrpTransmogrifier Documentation");
        irpTransmogrifierHelpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irpTransmogrifierHelpMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(irpTransmogrifierHelpMenuItem);

        glossaryMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/playlist.png"))); // NOI18N
        glossaryMenuItem.setText("Glossary");
        glossaryMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                glossaryMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(glossaryMenuItem);
        helpMenu.add(jSeparator32);

        tutorialMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        tutorialMenuItem.setMnemonic('T');
        tutorialMenuItem.setText("Tutorial");
        tutorialMenuItem.setToolTipText("View tutorial in the browser");
        tutorialMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tutorialMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(tutorialMenuItem);

        homePageMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/kfm_home.png"))); // NOI18N
        homePageMenuItem.setMnemonic('H');
        homePageMenuItem.setText("Project home page");
        homePageMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                homePageMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(homePageMenuItem);
        helpMenu.add(jSeparator33);

        releaseNotesMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/mimetypes/document.png"))); // NOI18N
        releaseNotesMenuItem.setMnemonic('R');
        releaseNotesMenuItem.setText("Release Notes");
        releaseNotesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                releaseNotesMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(releaseNotesMenuItem);
        helpMenu.add(jSeparator34);

        protocolSpecMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/contents.png"))); // NOI18N
        protocolSpecMenuItem.setText("Browse Protocols");
        protocolSpecMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolSpecMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(protocolSpecMenuItem);
        helpMenu.add(jSeparator35);

        downloadsMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/warehause.png"))); // NOI18N
        downloadsMenuItem.setText("Browse Releases");
        downloadsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadsMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(downloadsMenuItem);

        gitMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/github/GitHub-Mark-24px.png"))); // NOI18N
        gitMenuItem.setText("Browse Sources on GitHub");
        gitMenuItem.setToolTipText("Direct the browser to the Github source repository.");
        gitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gitMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(gitMenuItem);

        disussionsMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/mimetypes/news.png"))); // NOI18N
        disussionsMenuItem.setMnemonic('U');
        disussionsMenuItem.setText("Discussions (GitHub)");
        disussionsMenuItem.setToolTipText("Participate in discussions or give/request help");
        disussionsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disussionsMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(disussionsMenuItem);

        issuesMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/idea.png"))); // NOI18N
        issuesMenuItem.setMnemonic('H');
        issuesMenuItem.setText("Issues (GitHub)");
        issuesMenuItem.setToolTipText("Open the Github issues in the browser.");
        issuesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                issuesMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(issuesMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(topLevelSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1000, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(topLevelSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 573, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        if (aboutBox == null) {
            aboutBox = new AboutPopup(this, false);
            aboutBox.setLocationRelativeTo(this);
        }
        aboutBox.setVisible(true);
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void homePageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_homePageMenuItemActionPerformed
        try {
            guiUtils.browse(new URI(Version.homepageUrl));
        } catch (URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_homePageMenuItemActionPerformed

    private void mainDocuMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mainDocuMenuItemActionPerformed
        try {
            guiUtils.browse(new File(properties.mkPathAbsolute(properties.getHelpfilePath())));
        } catch (MalformedURLException | URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_mainDocuMenuItemActionPerformed

    private void protocolSpecMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolSpecMenuItemActionPerformed
        try {
            guiUtils.browse(new File(properties.mkPathAbsolute(properties.getProtocolDocfilePath())));
        } catch (MalformedURLException | URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_protocolSpecMenuItemActionPerformed

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        boolean shouldQuit = checkUnsavedStuff();
        if (shouldQuit) {
            killRunningCapture();
            System.exit(IrpUtils.EXIT_SUCCESS);
        }
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void verboseCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_verboseCheckBoxMenuItemActionPerformed
        boolean verbose = verboseCheckBoxMenuItem.isSelected();
        properties.setVerbose(verbose);
    }//GEN-LAST:event_verboseCheckBoxMenuItemActionPerformed

    private void rawRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawRadioButtonMenuItemActionPerformed
        updateOutputFormat(OutputTextFormat.rawWithSigns);
    }//GEN-LAST:event_rawRadioButtonMenuItemActionPerformed

    private void ccfRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ccfRadioButtonMenuItemActionPerformed
        updateOutputFormat(OutputTextFormat.prontoHex);
    }//GEN-LAST:event_ccfRadioButtonMenuItemActionPerformed

    private void checkUpToDateMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkUpToDateMenuItemActionPerformed
        guiUtils.checkUpToDate(Version.currentVersionUrl, Version.versionString);
    }//GEN-LAST:event_checkUpToDateMenuItemActionPerformed

    private void hexCalcMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hexCalcMenuItemActionPerformed
        HarcletFrame.newHarcletFrame(this, new HexCalculator(), false, lookAndFeelManager.getCurrentLAFClassName());
    }//GEN-LAST:event_hexCalcMenuItemActionPerformed

    private void timeFrequencyCalcMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_timeFrequencyCalcMenuItemActionPerformed
        HarcletFrame.newHarcletFrame(this, new TimeFrequencyCalculator(), false, lookAndFeelManager.getCurrentLAFClassName());
    }//GEN-LAST:event_timeFrequencyCalcMenuItemActionPerformed

    private void startTimeoutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startTimeoutMenuItemActionPerformed
        try {
            Integer t = guiUtils.getIntegerInput("Starting timeout in milliseconds", properties.getCaptureBeginTimeout());
            if (t != null)
                properties.setCaptureBeginTimeout(t);
        } catch (NumberFormatException ex) {
            guiUtils.error("Invalid number: " + ex.getMessage());
        }
    }//GEN-LAST:event_startTimeoutMenuItemActionPerformed

    private void lengthMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lengthMenuItemActionPerformed
        try {
            Integer t = guiUtils.getIntegerInput("Maximal recording length in milliseconds", properties.getCaptureMaxSize());
            if (t != null)
                properties.setCaptureMaxSize(t);
        } catch (NumberFormatException ex) {
            guiUtils.error("Invalid number: " + ex.getMessage());
        }
    }//GEN-LAST:event_lengthMenuItemActionPerformed

    private void endingTimeoutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_endingTimeoutMenuItemActionPerformed
        try {
            Integer t = guiUtils.getIntegerInput("Require ending silence in milliseconds (max " + MAX_ENDING_TIMEOUT + ")", properties.getCaptureEndingTimeout());
            if (t == null)
                return;

             if (t > MAX_ENDING_TIMEOUT)
                guiUtils.error("endingTimeout must be \u2264 " + MAX_ENDING_TIMEOUT + ".");
            else
                properties.setCaptureEndingTimeout(t);
        } catch (NumberFormatException ex) {
            guiUtils.error("Invalid number: " + ex.getMessage());
        }
    }//GEN-LAST:event_endingTimeoutMenuItemActionPerformed

    private void fallbackFrequencyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fallbackFrequencyMenuItemActionPerformed
        try {
            Double t = guiUtils.getDoubleInput("Fallback frequency (for demodulating sensors) and for interpreting raw sequences, in Hz", properties.getFallbackFrequency());
            if (t != null)
                properties.setFallbackFrequency(t);
        } catch (NumberFormatException ex) {
            guiUtils.error("Invalid number: " + ex.getMessage());
        }
    }//GEN-LAST:event_fallbackFrequencyMenuItemActionPerformed

    private void savePropertiesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_savePropertiesMenuItemActionPerformed
        try {
            String result = properties.save();
            guiUtils.info(result == null ? "No need to save properties." : ("Property file written to " + result + "."));
        } catch (IOException e) {
            guiUtils.error("Problems saving properties: " + e.getMessage());
        }
    }//GEN-LAST:event_savePropertiesMenuItemActionPerformed

    private void savePropertiesAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_savePropertiesAsMenuItemActionPerformed
        try {
            File props = SelectFile.selectFile(this, "Select properties save", null, true, true, "XML Files (*.xml)", "xml");
            if (props != null) { // null: user pressed cancel
                properties.save(props);
                guiUtils.info("Property file written to " + props + ".");
            }
        } catch (IOException e) {
            guiUtils.error(e);
        }
    }//GEN-LAST:event_savePropertiesAsMenuItemActionPerformed

    private void resetPropertiesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetPropertiesMenuItemActionPerformed
        properties.reset();
        guiUtils.warning("All properties reset to defaults.\n"
                    + "The program is presently in an inconsistent state,\n"
                    + "and should be restarted immediately.");
    }//GEN-LAST:event_resetPropertiesMenuItemActionPerformed

    private void usePopupsForHelpCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_usePopupsForHelpCheckBoxMenuItemActionPerformed
        boolean b = usePopupsForHelpCheckBoxMenuItem.isSelected();
        properties.setUsePopupsForHelp(b);
        guiUtils.setUsePopupsForHelp(b);
    }//GEN-LAST:event_usePopupsForHelpCheckBoxMenuItemActionPerformed

    private void usePopupsForErrorsCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_usePopupsForErrorsCheckBoxMenuItemActionPerformed
        boolean b = usePopupsForErrorsCheckBoxMenuItem.isSelected();
        properties.setUsePopupsForErrors(b);
        guiUtils.setUsePopupsForErrors(b);
    }//GEN-LAST:event_usePopupsForErrorsCheckBoxMenuItemActionPerformed

    private void analyzerBase2RadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_analyzerBase2RadioButtonMenuItemActionPerformed
        properties.setAnalyzerBase(2);
    }//GEN-LAST:event_analyzerBase2RadioButtonMenuItemActionPerformed

    private void analyzerBase8RadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_analyzerBase8RadioButtonMenuItemActionPerformed
        properties.setAnalyzerBase(8);
    }//GEN-LAST:event_analyzerBase8RadioButtonMenuItemActionPerformed

    private void analyzerBase10RadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_analyzerBase10RadioButtonMenuItemActionPerformed
        properties.setAnalyzerBase(10);
    }//GEN-LAST:event_analyzerBase10RadioButtonMenuItemActionPerformed

    private void analyzerBase16RadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_analyzerBase16RadioButtonMenuItemActionPerformed
        properties.setAnalyzerBase(16);
    }//GEN-LAST:event_analyzerBase16RadioButtonMenuItemActionPerformed

    private void ignoreEndingSilenceCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ignoreEndingSilenceCheckBoxMenuItemActionPerformed
        properties.setIgnoreEndingSilence(ignoreEndingSilenceCheckBoxMenuItem.isSelected());
        irPlotter.setIgnoreLast(ignoreEndingSilenceCheckBoxMenuItem.isSelected());
        //guiUtils.message("Replot not implemented (yet?)");
    }//GEN-LAST:event_ignoreEndingSilenceCheckBoxMenuItemActionPerformed

    private void testSignalMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testSignalMenuItemActionPerformed
        if (rawPanel.isShowing())
            addRawTestSignalMenuItemActionPerformed(evt);
        else if (cookedPanel.isShowing())
            addParametrizedTestSignalMenuItemActionPerformed(evt);
        else
            rawAddTestSignalMenuItemActionPerformed(evt);
    }//GEN-LAST:event_testSignalMenuItemActionPerformed

    private void copyConsoleToClipboardMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyConsoleToClipboardMenuItemActionPerformed
        console.copyToClipboard();
    }//GEN-LAST:event_copyConsoleToClipboardMenuItemActionPerformed

    private void clearConsoleMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearConsoleMenuItemActionPerformed
        console.clear();
    }//GEN-LAST:event_clearConsoleMenuItemActionPerformed

    private void saveConsoleTextAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveConsoleTextAsMenuItemActionPerformed
        try {
            File file = SelectFile.selectFile(this, "Save console text as...", null, true,
                    false, "Text file (*.txt)", "txt");
            if (file != null)
                console.save(file);
        } catch (FileNotFoundException ex) {
            guiUtils.error("File saving error");
        }
    }//GEN-LAST:event_saveConsoleTextAsMenuItemActionPerformed

    private void rawCodeClearMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodeClearMenuItemActionPerformed
        capturedDataTextArea.setText("");
    }//GEN-LAST:event_rawCodeClearMenuItemActionPerformed

    private void rawCodeCopyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodeCopyMenuItemActionPerformed
        (new CopyClipboardText(null)).toClipboard(capturedDataTextArea.getSelectedText());
    }//GEN-LAST:event_rawCodeCopyMenuItemActionPerformed

    private void rawCodeCopyAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodeCopyAllMenuItemActionPerformed
        (new CopyClipboardText(null)).toClipboard(capturedDataTextArea.getText().trim());
    }//GEN-LAST:event_rawCodeCopyAllMenuItemActionPerformed

    private void rawCodePasteReplacingMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodePasteReplacingMenuItemActionPerformed
        setCapturedDataTextAreaFromClipboard();
    }//GEN-LAST:event_rawCodePasteReplacingMenuItemActionPerformed

    private void rawCodeSelectAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodeSelectAllMenuItemActionPerformed
        capturedDataTextArea.selectAll();
    }//GEN-LAST:event_rawCodeSelectAllMenuItemActionPerformed

    private void rawCodeSaveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodeSaveMenuItemActionPerformed
        saveCapturedDataAsText();
    }//GEN-LAST:event_rawCodeSaveMenuItemActionPerformed

    private void saveDataTextAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveDataTextAsMenuItemActionPerformed
        rawCodeSaveMenuItemActionPerformed(evt);
    }//GEN-LAST:event_saveDataTextAsMenuItemActionPerformed

    private void copyDataToClipboardMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyDataToClipboardMenuItemActionPerformed
        (new CopyClipboardText(null)).toClipboard(capturedDataTextArea.getText().trim());
    }//GEN-LAST:event_copyDataToClipboardMenuItemActionPerformed

    private void startCaptureMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startCaptureMenuItemActionPerformed
        startButton.setSelected(true); // presently useless...
        startButtonActionPerformed(evt);
        startButton.setSelected(false);
    }//GEN-LAST:event_startCaptureMenuItemActionPerformed

    private void plotterResetMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_plotterResetMenuItemActionPerformed
        irPlotter.reset();
    }//GEN-LAST:event_plotterResetMenuItemActionPerformed

    private void reAnalyzeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reAnalyzeMenuItemActionPerformed
        reAnalyze();
    }//GEN-LAST:event_reAnalyzeMenuItemActionPerformed

    private void analysisToClipboardMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_analysisToClipboardMenuItemActionPerformed
        String s = analysisString();
        (new CopyClipboardText(null)).toClipboard(s);
    }//GEN-LAST:event_analysisToClipboardMenuItemActionPerformed

    private void pasteToDataWindowMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasteToDataWindowMenuItemActionPerformed
        setCapturedDataTextAreaFromClipboard();
        //reAnalyze();
    }//GEN-LAST:event_pasteToDataWindowMenuItemActionPerformed

    private void importSignalAsIctMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importSignalAsIctMenuItemActionPerformed
        importConcatenatedCommandsByFileSelector(ictImporter);
    }//GEN-LAST:event_importSignalAsIctMenuItemActionPerformed

    private void exportSignalIctMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSignalIctMenuItemActionPerformed
        try {
            Exporter exporter = newExporter("ICT");
            saveSignal(getCapturedIrSignal(), exporter);
        } catch (IOException | TransformerException | IrpException | GirrException | IrCoreException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_exportSignalIctMenuItemActionPerformed

    private void editingTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editingTextFieldActionPerformed
        JTable table = rawCookedTabbedPane.getSelectedIndex() == 1 ? rawTable : parameterTable;
        NamedIrSignal.LearnedIrSignalTableModel tableModel = (NamedIrSignal.LearnedIrSignalTableModel) table.getModel();
        int row = table.getSelectedRow();
        int column = table.getSelectedColumn();
        if (row >= 0 && column >= 0) {
            int r = table.convertRowIndexToModel(row);
            int c = table.convertColumnIndexToModel(column);
            Class<?> clazz = tableModel.getColumnClass(column);
            String str = editingTextField.getText();
            Object thing = str.trim().isEmpty() ? null
                    : clazz == Integer.class ? Integer.valueOf(str)
                    : clazz == Boolean.class ? Boolean.valueOf(str)
                    : str;

            Object oldValue = tableModel.getValueAt(r, c);
            try {
                tableModel.setValueAt(thing, r, c);
            } catch (NumberFormatException ex) {
                // Tried to set something numeric to something non-numeric
                guiUtils.error("Value has to be an integer!");
                tableModel.setValueAt(oldValue, r, c);
            }
            table.repaint();
        }
    }//GEN-LAST:event_editingTextFieldActionPerformed

    private void rawTableMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_rawTableMousePressed

    }//GEN-LAST:event_rawTableMousePressed

    private void rawTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_rawTableMouseReleased
        tableMouseReleased(rawTable, evt);
    }//GEN-LAST:event_rawTableMouseReleased

    private void deleteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteMenuItemActionPerformed
        try {
            tableUtils.deleteTableSelectedRows(rawTable);
        } catch (ErroneousSelectionException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_deleteMenuItemActionPerformed

    private void deleteAllRawsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteAllRawsMenuItemActionPerformed
        tableUtils.clearTableConfirm(rawTable);
    }//GEN-LAST:event_deleteAllRawsMenuItemActionPerformed

    private void moveUpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveUpMenuItemActionPerformed
        try {
            tableUtils.tableMoveSelection(rawTable, true);
        } catch (ErroneousSelectionException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_moveUpMenuItemActionPerformed

    private void moveDownMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveDownMenuItemActionPerformed
        try {
            tableUtils.tableMoveSelection(rawTable, false);
        } catch (ErroneousSelectionException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_moveDownMenuItemActionPerformed

    private void exportSignalGirrMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSignalGirrMenuItemActionPerformed
        try {
            saveSignal(getCapturedIrSignal(), newGirrExporter());
        } catch (IOException | TransformerException | IrpException | GirrException | IrCoreException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_exportSignalGirrMenuItemActionPerformed

    private void clearSignalMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearSignalMenuItemActionPerformed
        clearSignal();
    }//GEN-LAST:event_clearSignalMenuItemActionPerformed

    private void startStopToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startStopToggleButtonActionPerformed
        if (!hardwareManager.isReady()) {
            guiUtils.error("No capture device selected, aborting");
            startStopToggleButton.setSelected(false);
            return;
        }
        if (startStopToggleButton.isSelected() && captureThreadRunning()) {
            guiUtils.error("Another capture thread is running. This must first be ended.");
            startStopToggleButton.setSelected(false);
            return;
        }
        if (!captureThreadRunning()) {
            if (rawCookedTabbedPane.getSelectedIndex() == 1) {
                enableRawCaptureOnly(true);
            }
            captureThread = new CaptureThread(new CaptureThreadClient() {
                @Override
                public JToggleButton getButton() {
                    return startStopToggleButton;
                }

                @Override
                public void processSequence(ModulatedIrSequence sequence) {
                    try {
                        if (properties.getInvokeRepeatFinder()) {
                            IrSignal irSignal = InterpretString.interpretIrSequence(sequence,
                                    properties.getInvokeRepeatFinder(),
                                    properties.getInvokeCleaner(), properties.getAbsoluteTolerance(), properties.getRelativeTolerance());
                            if (rawPanel.isVisible())
                                registerRawSignal(irSignal, null, null);
                            else
                                registerParameterSignal(irSignal);
                        } else {
                            ModulatedIrSequence modulatedIrSequence = properties.getInvokeCleaner()
                                    ? Cleaner.clean(sequence, properties.getAbsoluteTolerance(), properties.getRelativeTolerance()) : sequence;
                            if (rawPanel.isVisible())
                                registerRawSignal(modulatedIrSequence, null, null);
                            else
                                registerParameterSignal(modulatedIrSequence);

                        }
                    } catch (InvalidArgumentException ex) {
                    }
                }
            });
            captureThread.start();
            topLevelTabbedPane.setEnabled(false);
            jumpToLastPanelMenuItem.setEnabled(false);
        }
    }//GEN-LAST:event_startStopToggleButtonActionPerformed

    private void printRawTableRowMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printRawTableRowMenuItemActionPerformed
        tableUtils.printTableSelectedRows(rawTable);
    }//GEN-LAST:event_printRawTableRowMenuItemActionPerformed

    private void hideColumnMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hideColumnMenuItemActionPerformed
        int selectedColumn = rawTable.getSelectedColumn();
        try {
            rawTableColumnModel.removeColumn(selectedColumn);
        } catch (ArrayIndexOutOfBoundsException ex) {
            guiUtils.error(selectedColumn < 0 ? "No column selected." : "No column # " + selectedColumn + ".");
        }
    }//GEN-LAST:event_hideColumnMenuItemActionPerformed

    private void resetRawTableColumnsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetRawTableColumnsMenuItemActionPerformed
        rawTableColumnModel.reset();
    }//GEN-LAST:event_resetRawTableColumnsMenuItemActionPerformed

    private void scrutinizeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scrutinizeMenuItemActionPerformed
        try {
            RawIrSignal cir = rawTableModel.getCapturedIrSignal(rawTable.convertRowIndexToModel(rawTable.getSelectedRow()));
            scrutinizeIrSignal(cir.getIrSignal());
            //??? I do not like hard jumps like that, although logical
            //topLevelTabbedPane.setSelectedIndex(0);
        } catch (InvalidArgumentException ex) {
            guiUtils.error(ex);        }
    }//GEN-LAST:event_scrutinizeMenuItemActionPerformed

    private void moveUpMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveUpMenuItem1ActionPerformed
        try {
            tableUtils.tableMoveSelection(parameterTable, true);
        } catch (ErroneousSelectionException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_moveUpMenuItem1ActionPerformed

    private void moveDownMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveDownMenuItem1ActionPerformed
        try {
            tableUtils.tableMoveSelection(parameterTable, false);
        } catch (ErroneousSelectionException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_moveDownMenuItem1ActionPerformed

    private void deleteMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteMenuItem1ActionPerformed
        try {
            tableUtils.deleteTableSelectedRows(parameterTable);
        } catch (ErroneousSelectionException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_deleteMenuItem1ActionPerformed

    private void printTableRowMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printTableRowMenuItemActionPerformed
        tableUtils.printTableSelectedRows(parameterTable);
    }//GEN-LAST:event_printTableRowMenuItemActionPerformed

    private void deleteAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteAllMenuItemActionPerformed
        tableUtils.clearTableConfirm(parameterTable);
    }//GEN-LAST:event_deleteAllMenuItemActionPerformed

    private void hideColumnMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hideColumnMenuItem1ActionPerformed
        int selectedColumn = parameterTable.getSelectedColumn();
        try {
            parameterTableColumnModel.removeColumn(selectedColumn);
        } catch (ArrayIndexOutOfBoundsException ex) {
            guiUtils.error(selectedColumn < 0 ? "No column selected." : "No column # " + selectedColumn + ".");
        }
    }//GEN-LAST:event_hideColumnMenuItem1ActionPerformed

    private void resetRawTableColumnsMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetRawTableColumnsMenuItem1ActionPerformed
        parameterTableColumnModel.reset();
    }//GEN-LAST:event_resetRawTableColumnsMenuItem1ActionPerformed

    private void beaconListenerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_beaconListenerMenuItemActionPerformed
        HarcletFrame.newHarcletFrame(this, new AmxBeaconListenerPanel(), false, lookAndFeelManager.getCurrentLAFClassName());
    }//GEN-LAST:event_beaconListenerMenuItemActionPerformed

    private void parametricOrRawExportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_parametricOrRawExportButtonActionPerformed
        try {
            saveAllCommands(rawCookedTabbedPane.getSelectedComponent() == this.cookedPanel ? parameterTable : rawTable);
        } catch (IOException | TransformerException | GirrException | IrpException | IrCoreException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_parametricOrRawExportButtonActionPerformed

    private void importLircMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importLircMenuItemActionPerformed
        importRemoteByFileSelector(lircImporter, true);
    }//GEN-LAST:event_importLircMenuItemActionPerformed

    private void importCcfMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importCcfMenuItemActionPerformed
        importRemoteByFileSelector(ccfImporter, true);
    }//GEN-LAST:event_importCcfMenuItemActionPerformed

    private void importXcfMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importXcfMenuItemActionPerformed
        importRemoteByFileSelector(xcfImporter, true);
    }//GEN-LAST:event_importXcfMenuItemActionPerformed

    private void remoteLocatorBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remoteLocatorBrowseButtonActionPerformed
        guiUtils.browse(RemoteLocatorImporter.getHomeUri());
    }//GEN-LAST:event_remoteLocatorBrowseButtonActionPerformed

    private void remoteLocatorManufacturerComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remoteLocatorManufacturerComboBoxActionPerformed
        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
            if (remoteLocatorImporter == null)
                remoteLocatorImporter = new RemoteLocatorImporter(properties.getVerbose());
            if (remoteLocatorManufacturerComboBox.getModel().getSize() == 1) {
                String[] manufacturers = remoteLocatorImporter.getManufacturers();
                //Arrays.sort(manufacturers);
                DefaultComboBoxModel<String> dcbm = new DefaultComboBoxModel<>(manufacturers);
                remoteLocatorManufacturerComboBox.setModel(dcbm);
                remoteLocatorManufacturerComboBox.setSelectedIndex(0);
                if (remoteLocatorManufacturerComboBox.getModel().getSize() != 1)
                    remoteLocatorManufacturerComboBoxActionPerformed(null);
            } else {
                String manufacturer = (String) remoteLocatorManufacturerComboBox.getSelectedItem();
                String[] list = remoteLocatorImporter.getDeviceClasses(manufacturer);
                DefaultComboBoxModel<String> dcbm = new DefaultComboBoxModel<>(list);
                remoteLocatorDeviceTypeComboBox.setModel(dcbm);
                remoteLocatorDeviceTypeComboBox.setEnabled(true);
                remoteLocatorDeviceTypeComboBoxActionPerformed(evt);
            }
        } catch (IOException | NotFoundException | SAXException ex) {
            guiUtils.error(ex);
        } finally {
            busyWindow.unBusy();
        }
    }//GEN-LAST:event_remoteLocatorManufacturerComboBoxActionPerformed

    private void remoteLocatorDeviceTypeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remoteLocatorDeviceTypeComboBoxActionPerformed
        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
            String manufacturer = (String) remoteLocatorManufacturerComboBox.getSelectedItem();
            String deviceType = (String) remoteLocatorDeviceTypeComboBox.getSelectedItem();
            String[] remoteNames = remoteLocatorImporter.getRemotes(manufacturer, deviceType);
            DefaultComboBoxModel<String> dcbm = new DefaultComboBoxModel<>(remoteNames);
            remoteLocatorRemoteComboBox.setModel(dcbm);
            remoteLocatorRemoteComboBox.setEnabled(true);
            remoteLocatorRemoteComboBoxActionPerformed(evt);
        } catch (NotFoundException ex) {
            guiUtils.error(ex);
        } finally {
            busyWindow.unBusy();
        }
    }//GEN-LAST:event_remoteLocatorDeviceTypeComboBoxActionPerformed

    private void remoteLocatorImportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remoteLocatorImportButtonActionPerformed
        try {
            remoteLocatorImporter.loadRemote();
            remoteLocatorTreeImporter.setRemoteSet(remoteLocatorImporter.getRemoteSet(), "RemoteLocator");
        } catch (IOException | Girrable.NotGirrableException | NotFoundException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_remoteLocatorImportButtonActionPerformed

    private void importIctMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importIctMenuItemActionPerformed
        importRemoteByFileSelector(ictImporter, true);
    }//GEN-LAST:event_importIctMenuItemActionPerformed

    private void exportParametricAsGirrMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportParametricAsGirrMenuItemActionPerformed
        try {
            saveAllCommands(parameterTable, newGirrExporter());
        } catch (IOException | TransformerException | GirrException | IrpException | IrCoreException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_exportParametricAsGirrMenuItemActionPerformed

    private void exportSelectedCookedMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSelectedCookedMenuItemActionPerformed
        try {
            saveSelectedCommands(parameterTable);
        } catch (GirrException | IOException | TransformerException | IrpException | IrCoreException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_exportSelectedCookedMenuItemActionPerformed

    private void saveSelectedRawTableRowMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveSelectedRawTableRowMenuItemActionPerformed
        try {
            saveSelectedCommands(rawTable);
        } catch (GirrException | IOException | TransformerException | IrpException | IrCoreException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_saveSelectedRawTableRowMenuItemActionPerformed

    private void saveRawMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveRawMenuItemActionPerformed
        try {
            saveAllCommands(rawTable);
        } catch (IOException | TransformerException | GirrException | IrpException | IrCoreException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_saveRawMenuItemActionPerformed

    private void exportCookedMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportCookedMenuItemActionPerformed
        try {
            saveAllCommands(parameterTable);
        } catch (IOException | TransformerException | GirrException | IrpException | IrCoreException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_exportCookedMenuItemActionPerformed

    private void addEmptyParametrizedSignalMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addEmptyParametrizedSignalMenuItemActionPerformed
        ParametrizedIrSignal signal = new ParametrizedIrSignal();
        registerParameterSignal(signal);
    }//GEN-LAST:event_addEmptyParametrizedSignalMenuItemActionPerformed

    private void rawFromClipboardMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawFromClipboardMenuItemActionPerformed
        String text = (new CopyClipboardText(null)).fromClipboard();
        if (text == null || text.trim().isEmpty()) {
            guiUtils.error("Clipboard is empty");
            return;
        }

        try {
            IrSignal irSignal = InterpretString.interpretString(text, properties.getFallbackFrequency(), properties.getDummyGap(),
                    properties.getInvokeRepeatFinder(), properties.getInvokeCleaner(),
                    properties.getAbsoluteTolerance(), properties.getRelativeTolerance(), properties.getMinRepeatLastGap());
            RawIrSignal rawIrSignal = new RawIrSignal(irSignal, "clipboard", "from clipboard");
            registerRawCommand(rawIrSignal);
        } catch (InvalidArgumentException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_rawFromClipboardMenuItemActionPerformed

    private void exportRawAsGirrMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportRawAsGirrMenuItemActionPerformed
        try {
            saveAllCommands(rawTable, newGirrExporter());
        } catch (IOException | TransformerException | GirrException | IrpException | IrCoreException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_exportRawAsGirrMenuItemActionPerformed

    private void repeatFinderCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_repeatFinderCheckBoxMenuItemActionPerformed
        properties.setInvokeRepeatFinder(repeatFinderCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_repeatFinderCheckBoxMenuItemActionPerformed

    private void setProtocolMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setProtocolMenuItemActionPerformed
        List<Integer> rows = tableUtils.modelLinesSelected(parameterTable);
        if (rows.isEmpty()) {
            guiUtils.error("Nothing selected");
            return;
        }
        String newProtocol = guiUtils.getInput("Enter new protocol", "Protocol request", "NEC1");
        if (newProtocol == null) // Cancel pressed
            return;
        if (irpDatabase.isKnown(newProtocol)
                || guiUtils.confirm("The protocol \"" + newProtocol + "\" is unknown. Proceed anyhow?")) {
            parameterTableModel.setProtocol(newProtocol, rows);
        }
    }//GEN-LAST:event_setProtocolMenuItemActionPerformed

    private void setDMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setDMenuItemActionPerformed
        setParameter("D");
    }//GEN-LAST:event_setDMenuItemActionPerformed

    private void setSMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setSMenuItemActionPerformed
        setParameter("S");
    }//GEN-LAST:event_setSMenuItemActionPerformed

    private void setFMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setFMenuItemActionPerformed
        setParameter("F");
    }//GEN-LAST:event_setFMenuItemActionPerformed

    private void setTMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setTMenuItemActionPerformed
        setParameter("T");
    }//GEN-LAST:event_setTMenuItemActionPerformed

    private void hideUnusedMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hideUnusedMenuItemActionPerformed
        ArrayList<Integer>list = parameterTableModel.getUnusedColumns();
        parameterTableColumnModel.removeColumns(list);
    }//GEN-LAST:event_hideUnusedMenuItemActionPerformed

    private void removeUnusedMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeUnusedMenuItem1ActionPerformed
        ArrayList<Integer>list = rawTableModel.getUnusedColumns();
        rawTableColumnModel.removeColumns(list);
    }//GEN-LAST:event_removeUnusedMenuItem1ActionPerformed

    private void girrWebSiteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_girrWebSiteButtonActionPerformed
        try {
            guiUtils.browse(new URI(org.harctoolbox.girr.XmlStatic.GIRR_HOMEPAGE));
        } catch (URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_girrWebSiteButtonActionPerformed

    private void invokeDecoderCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_invokeDecoderCheckBoxMenuItemActionPerformed
        properties.setInvokeDecoder(invokeDecoderCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_invokeDecoderCheckBoxMenuItemActionPerformed

    private void generateRawCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generateRawCheckBoxMenuItemActionPerformed
        properties.setGenerateRaw(generateRawCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_generateRawCheckBoxMenuItemActionPerformed

    private void generateCcfCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generateCcfCheckBoxMenuItemActionPerformed
        properties.setGenerateCcf(generateCcfCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_generateCcfCheckBoxMenuItemActionPerformed

    private void translateProntoFontCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_translateProntoFontCheckBoxMenuItemActionPerformed
        properties.setTranslateProntoFont(translateProntoFontCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_translateProntoFontCheckBoxMenuItemActionPerformed

    private void remoteLocatorBrowseRemoteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remoteLocatorBrowseRemoteButtonActionPerformed
        URL url = remoteLocatorImporter.getRemoteURL();
        try {
            guiUtils.browse(url.toURI());
        } catch (URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_remoteLocatorBrowseRemoteButtonActionPerformed

    private void jButton20ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton20ActionPerformed
        try {
            guiUtils.browse(new URI(LircImporter.homeUrl));
        } catch (URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_jButton20ActionPerformed

    private void jumpToLastPanelMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jumpToLastPanelMenuItemActionPerformed
        selectLastPane();
    }//GEN-LAST:event_jumpToLastPanelMenuItemActionPerformed

    private void sColumnComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sColumnComboBoxActionPerformed
        properties.setSColumn(sColumnComboBox.getSelectedIndex());
    }//GEN-LAST:event_sColumnComboBoxActionPerformed

    private void dColumnComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dColumnComboBoxActionPerformed
        properties.setDColumn(dColumnComboBox.getSelectedIndex());
    }//GEN-LAST:event_dColumnComboBoxActionPerformed

    private void protocolColumnComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolColumnComboBoxActionPerformed
        properties.setProtocolColumn(protocolColumnComboBox.getSelectedIndex());
    }//GEN-LAST:event_protocolColumnComboBoxActionPerformed

    private void creatingUserMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_creatingUserMenuItemActionPerformed
        String creator = guiUtils.getInput("Enter creating user name (empty to be anonymous)", "User name inquiry", properties.getCreatingUser());
        if (creator != null)
            properties.setCreatingUser(creator);
        Exporter.setCreatingUser(creator);
    }//GEN-LAST:event_creatingUserMenuItemActionPerformed

    private void csvRawSeparatorComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_csvRawSeparatorComboBoxActionPerformed
        properties.setRawSeparatorIndex(csvRawSeparatorComboBox.getSelectedIndex());
    }//GEN-LAST:event_csvRawSeparatorComboBoxActionPerformed

    private void rawNameColumnComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawNameColumnComboBoxActionPerformed
        properties.setRawNameColumn(rawNameColumnComboBox.getSelectedIndex());
    }//GEN-LAST:event_rawNameColumnComboBoxActionPerformed

    private void rawCodeColumnComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodeColumnComboBox1ActionPerformed
        properties.setCodeColumn(Integer.parseInt((String)rawCodeColumnComboBox1.getSelectedItem()));
    }//GEN-LAST:event_rawCodeColumnComboBox1ActionPerformed

    private void includeSubsequenctColumnsCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_includeSubsequenctColumnsCheckBox1ActionPerformed
        properties.setIncludeTail(includeSubsequenctColumnsCheckBox1.isSelected());
    }//GEN-LAST:event_includeSubsequenctColumnsCheckBox1ActionPerformed

    private void parametrizedNameColumnComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_parametrizedNameColumnComboBoxActionPerformed
        properties.setParametricNameColumn(parametrizedNameColumnComboBox.getSelectedIndex() + 1);
    }//GEN-LAST:event_parametrizedNameColumnComboBoxActionPerformed

    private void parametrizedBaseComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_parametrizedBaseComboBoxActionPerformed
        properties.setParametricNumberBaseIndex(parametrizedBaseComboBox.getSelectedIndex());
    }//GEN-LAST:event_parametrizedBaseComboBoxActionPerformed

    private void parametrizedCsvSeparatorComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_parametrizedCsvSeparatorComboBoxActionPerformed
        properties.setParametricSeparatorIndex(parametrizedCsvSeparatorComboBox.getSelectedIndex());
    }//GEN-LAST:event_parametrizedCsvSeparatorComboBoxActionPerformed

    private void fColumnComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fColumnComboBoxActionPerformed
        properties.setFColumn(fColumnComboBox.getSelectedIndex());
    }//GEN-LAST:event_fColumnComboBoxActionPerformed

    private void girrSchemaLocationMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_girrSchemaLocationMenuItemActionPerformed
        String s = guiUtils.getInput("Enter URL for Girr schema", "Girr schema inquiry", properties.getGirrSchemaLocation());
        if (s != null)
            properties.setGirrSchemaLocation(s);
    }//GEN-LAST:event_girrSchemaLocationMenuItemActionPerformed

    private void girrValidateCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_girrValidateCheckBoxMenuItemActionPerformed
        properties.setGirrValidate(girrValidateCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_girrValidateCheckBoxMenuItemActionPerformed

    private void generateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generateButtonActionPerformed
        try {
            IrSignal irSignal = irpMasterBean.render();
            generateTextArea.setText(formatIrSignal(irSignal));
        } catch (ParseException | IrCoreException | IrpException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_generateButtonActionPerformed

    private void transmitGenerateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transmitGenerateButtonActionPerformed
        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
            boolean success = transmit(irpMasterBean.render());
            if (!success)
                guiUtils.error("Transmit failed.");
        } catch (IOException | ParseException | HarcHardwareException | IrCoreException | IrpException | HardwareUnavailableException ex) {
            guiUtils.error(ex);
        } finally {
            busyWindow.unBusy();
        }
    }//GEN-LAST:event_transmitGenerateButtonActionPerformed

    private void toScrutinizeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_toScrutinizeButtonActionPerformed
        try {
            scrutinizeIrSignal(irpMasterBean.render());
        } catch (ParseException | IrCoreException | IrpException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_toScrutinizeButtonActionPerformed

    private void transferToRawRemoteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transferToRawRemoteButtonActionPerformed
        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
            Collection<Command> commands = irpMasterBean.getCommands().values();
            registerRawCommands(commands);
        } catch (ParseException | GirrException | IrCoreException | IrpException ex) {
            guiUtils.error(ex);
        } finally {
            busyWindow.unBusy();
        }
    }//GEN-LAST:event_transferToRawRemoteButtonActionPerformed

    private void exportDirectoryTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportDirectoryTextFieldActionPerformed
        checkChangeExportDirectory(new File(exportDirectoryTextField.getText()));
    }//GEN-LAST:event_exportDirectoryTextFieldActionPerformed

    private void exportDirSelectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportDirSelectButtonActionPerformed
        File file = SelectFile.selectFile(this, "Select directory for export files", properties.getExportDir(),
                false, false, JFileChooser.DIRECTORIES_ONLY, (String[][]) null);
        if (file != null && checkChangeExportDirectory(file))
            exportDirectoryTextField.setText(file.getPath());
    }//GEN-LAST:event_exportDirSelectButtonActionPerformed

    private void exportDirOpenButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportDirOpenButtonActionPerformed
        try {
            guiUtils.open(new File(properties.getExportDir()));
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_exportDirOpenButtonActionPerformed

    private void automaticExportFilenamesCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_automaticExportFilenamesCheckBoxActionPerformed
        properties.setExportAutomaticFilenames(automaticExportFilenamesCheckBox.isSelected());
    }//GEN-LAST:event_automaticExportFilenamesCheckBoxActionPerformed

    private void openLastFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openLastFileButtonActionPerformed
        try {
            File file = Exporter.getLastSaveFileOrCopy();
            if (file != null)
                guiUtils.open(file);
            else
                guiUtils.error("There is no \"last file\".");
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_openLastFileButtonActionPerformed

    private void exportGenerateRawCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportGenerateRawCheckBoxActionPerformed
        properties.setExportGenerateRaw(exportGenerateRawCheckBox.isSelected());
    }//GEN-LAST:event_exportGenerateRawCheckBoxActionPerformed

    private void exportGenerateParametersCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportGenerateParametersCheckBoxActionPerformed
        properties.setExportGenerateParameters(exportGenerateParametersCheckBox.isSelected());
    }//GEN-LAST:event_exportGenerateParametersCheckBoxActionPerformed

    private void exportGenerateCcfCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportGenerateCcfCheckBoxActionPerformed
        properties.setExportGenerateCcf(exportGenerateCcfCheckBox.isSelected());
    }//GEN-LAST:event_exportGenerateCcfCheckBoxActionPerformed

    private void exportSignalButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSignalButtonActionPerformed
        try {
            saveSignal(getCapturedIrSignal());
        } catch (IOException | TransformerException | IrpException | GirrException | IrCoreException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_exportSignalButtonActionPerformed

    private void exportParametricRemoteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportParametricRemoteButtonActionPerformed
        try {
            saveAllCommands(parameterTable);
        } catch (IOException | TransformerException | GirrException | IrpException | IrCoreException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_exportParametricRemoteButtonActionPerformed

    private void exportRawRemoteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportRawRemoteButtonActionPerformed
        try {
            saveAllCommands(rawTable);
        } catch (IOException | TransformerException | GirrException | IrpException | IrCoreException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_exportRawRemoteButtonActionPerformed

    private void girrFatRawCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_girrFatRawCheckBoxActionPerformed
        properties.setGirrFatRaw(girrFatRawCheckBox.isSelected());
    }//GEN-LAST:event_girrFatRawCheckBoxActionPerformed

    private void importSignalAsGirrMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importSignalAsGirrMenuItemActionPerformed
        importConcatenatedCommandsByFileSelector(girrImporter);
    }//GEN-LAST:event_importSignalAsGirrMenuItemActionPerformed

    private void importGirrMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importGirrMenuItemActionPerformed
        importRemoteByFileSelector(girrImporter, true);
    }//GEN-LAST:event_importGirrMenuItemActionPerformed

    private void importGirrMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importGirrMenuItem1ActionPerformed
        importRemoteByFileSelector(girrImporter, false);
    }//GEN-LAST:event_importGirrMenuItem1ActionPerformed

    private void importIctMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importIctMenuItem1ActionPerformed
        importRemoteByFileSelector(ictImporter, false);
    }//GEN-LAST:event_importIctMenuItem1ActionPerformed

    private void importLircMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importLircMenuItem1ActionPerformed
        importRemoteByFileSelector(lircImporter, false);
    }//GEN-LAST:event_importLircMenuItem1ActionPerformed

    private void importCcfMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importCcfMenuItem1ActionPerformed
        importRemoteByFileSelector(ccfImporter, false);
    }//GEN-LAST:event_importCcfMenuItem1ActionPerformed

    private void importXcfMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importXcfMenuItem1ActionPerformed
        importRemoteByFileSelector(xcfImporter, false);
    }//GEN-LAST:event_importXcfMenuItem1ActionPerformed

    private void openLastExportFileMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openLastExportFileMenuItemActionPerformed
        try {
            File file = Exporter.getLastSaveFileOrCopy();
            if (file != null)
                guiUtils.open(file);
            else
                guiUtils.error("No export has been made in this session.");
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_openLastExportFileMenuItemActionPerformed

    private void signalSignalTextMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_signalSignalTextMenuItemActionPerformed
        try {
            saveSignal(getCapturedIrSignal(), newTextExporter());
        } catch (IOException | TransformerException | IrCoreException | IrpException | GirrException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_signalSignalTextMenuItemActionPerformed

    private void exportSignalWaveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSignalWaveMenuItemActionPerformed
        try {
            saveSignal(getCapturedIrSignal(), new WaveExporter(exportAudioParametersBean));
        } catch (IOException | TransformerException | IrCoreException | IrpException | GirrException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_exportSignalWaveMenuItemActionPerformed

    private void generateExportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generateExportButtonActionPerformed
        try {
            saveCommands(irpMasterBean.getCommands(), Version.appName + " generated signals");
        } catch (IOException | TransformerException | ParseException | GirrException | IrCoreException | IrpException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_generateExportButtonActionPerformed

    private void prontoModelComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prontoModelComboBoxActionPerformed
        int index = prontoModelComboBox.getSelectedIndex();
        properties.setProntoModelIndex(index);
        ProntoModel prontomodel = ProntoModel.getModelByName((String) prontoModelComboBox.getSelectedItem());
        Dimension size = prontomodel.getScreenSize();
        prontoExportScreenWidthTextField.setText(Integer.toString(size.width));
        properties.setProntoScreenSizeX(size.width);
        prontoExportScreenHeightTextField.setText(Integer.toString(size.height));
        properties.setProntoScreenSizeY(size.height);
    }//GEN-LAST:event_prontoModelComboBoxActionPerformed

    private void exportParametricAsTextMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportParametricAsTextMenuItemActionPerformed
        try {
            saveAllCommands(parameterTable, newTextExporter());
        } catch (IOException | TransformerException | GirrException | IrpException | IrCoreException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_exportParametricAsTextMenuItemActionPerformed

    private void exportRawAsTextMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportRawAsTextMenuItemActionPerformed
        try {
            saveAllCommands(rawTable, newTextExporter());
        } catch (IOException | TransformerException | GirrException | IrpException | IrCoreException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_exportRawAsTextMenuItemActionPerformed

    private void exportRepeatComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportRepeatComboBoxActionPerformed
        properties.setExportNoRepeats(Integer.parseInt((String) exportRepeatComboBox.getSelectedItem()));
    }//GEN-LAST:event_exportRepeatComboBoxActionPerformed

    private void exportGenerateSendIrCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportGenerateSendIrCheckBoxActionPerformed
        properties.setExportGenerateSendIr(exportGenerateSendIrCheckBox.isSelected());
        exportRepeatComboBox.setEnabled(exportGenerateSendIrCheckBox.isSelected()
                || exportGenerateBroadlinkHexCheckBox.isSelected()
                || exportGenerateBroadlinkBase64CheckBox.isSelected());
    }//GEN-LAST:event_exportGenerateSendIrCheckBoxActionPerformed

    private void exportFormatComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportFormatComboBoxActionPerformed
        String formatName = (String) exportFormatComboBox.getSelectedItem();
        selectFormat(formatName);
    }//GEN-LAST:event_exportFormatComboBoxActionPerformed

    private void exportFormatsReloadMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportFormatsReloadMenuItemActionPerformed
        loadExportFormatsGuiRefresh();
    }//GEN-LAST:event_exportFormatsReloadMenuItemActionPerformed

    private void importSignalAsWaveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importSignalAsWaveMenuItemActionPerformed
        importModulatedIrSequenceByFileSelector(new WaveImporter(properties.getImportWaveDivideCarrier()));
    }//GEN-LAST:event_importSignalAsWaveMenuItemActionPerformed

    private void importWaveDivideCarrierCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importWaveDivideCarrierCheckBox1ActionPerformed
        properties.setImportWaveDivideCarrier(importWaveDivideCarrierCheckBox1.isSelected());
    }//GEN-LAST:event_importWaveDivideCarrierCheckBox1ActionPerformed

    private void sendingHardwareTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sendingHardwareTabbedPaneStateChanged
        if (hardwareManager == null)
            return;
        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
            for (String hardwareName : hardwareManager) {
                if (hardwareManager.getBean(hardwareName).getParent() == sendingHardwareTabbedPane.getSelectedComponent()) {
                    hardwareManager.select(hardwareName);
                    break;
                }
            }
        } catch (HardwareUnavailableException ex) {
            guiUtils.error(ex);
        } finally {
            busyWindow.unBusy();
        }
    }//GEN-LAST:event_sendingHardwareTabbedPaneStateChanged

    private void transmitScrutinizedButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transmitScrutinizedButtonActionPerformed
        transmitSignalButtonActionPerformed(evt);
    }//GEN-LAST:event_transmitScrutinizedButtonActionPerformed

    private void transmitGenerateButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transmitGenerateButton2ActionPerformed
        transmitGenerateButtonActionPerformed(evt);
    }//GEN-LAST:event_transmitGenerateButton2ActionPerformed

    private void rawCodeAnalyzeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodeAnalyzeMenuItemActionPerformed
        reAnalyze();
    }//GEN-LAST:event_rawCodeAnalyzeMenuItemActionPerformed

    private void rawCodePasteAnalyzeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodePasteAnalyzeMenuItemActionPerformed
        setCapturedDataTextAreaFromClipboard();
        reAnalyze();
    }//GEN-LAST:event_rawCodePasteAnalyzeMenuItemActionPerformed

    private void signalClearMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_signalClearMenuItemActionPerformed
        clearSignal();
    }//GEN-LAST:event_signalClearMenuItemActionPerformed

    private void pasteScrutinizeToDataWindowMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasteScrutinizeToDataWindowMenuItemActionPerformed
        setCapturedDataTextAreaFromClipboard();
        reAnalyze();
    }//GEN-LAST:event_pasteScrutinizeToDataWindowMenuItemActionPerformed

    private void irpProtocolsEditMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irpProtocolsEditMenuItemActionPerformed
        try {
            guiUtils.open(new File(properties.mkPathAbsolute(properties.getIrpProtocolsPath())));
            guiUtils.warning("If editing the file, changes will not take effect before you save the file AND restart the program.");
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_irpProtocolsEditMenuItemActionPerformed

    private void irpProtocolsSelectMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irpProtocolsSelectMenuItemActionPerformed
        String oldDir = new File(properties.mkPathAbsolute(properties.getIrpProtocolsPath())).getParent();
        File f = SelectFile.selectFile(this, "Select protocol file (typically IrpProtocols.xml)", oldDir, false, false, "XML files (*.xml)", "xml");
        if (f == null)
            return;

        properties.setIrpProtocolsPath(f.getAbsolutePath());
        try {
            setupIrpDatabase();
        } catch (IOException | IrpParseException | SAXException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_irpProtocolsSelectMenuItemActionPerformed

    private void exportFormatsEditMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportFormatsEditMenuItemActionPerformed
        try {
            guiUtils.open(new File(properties.mkPathAbsolute(properties.getExportFormatFilePath())));
            guiUtils.warning("If editing, changes will not take effect before reloading.");
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_exportFormatsEditMenuItemActionPerformed

    private void exportFormatsSelectMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportFormatsSelectMenuItemActionPerformed
        String oldDir = new File(properties.mkPathAbsolute(properties.getExportFormatFilePath())).getParent();
        File f = SelectFile.selectFile(this, "Select export format file or directory", oldDir, false, false, JFileChooser.FILES_AND_DIRECTORIES, "XML files (*.xml)", "xml");
        if (f == null || f.getAbsolutePath().equals(properties.mkPathAbsolute(properties.getExportFormatFilePath())))
            return;

        properties.setExportFormatFilePath(f.getAbsolutePath());
        loadExportFormatsGuiRefresh();
    }//GEN-LAST:event_exportFormatsSelectMenuItemActionPerformed

    private void scrutinizeRemoteHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scrutinizeRemoteHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.scrutinizeRemoteHelp);
    }//GEN-LAST:event_scrutinizeRemoteHelpButtonActionPerformed

    private void generateHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generateHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.generateHelp);
    }//GEN-LAST:event_generateHelpButtonActionPerformed

    private void remoteLocatorHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remoteLocatorHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.remoteLocatorHelp);
    }//GEN-LAST:event_remoteLocatorHelpButtonActionPerformed

    private void importGirrSignalHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importGirrSignalHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.importGirrHelp);
    }//GEN-LAST:event_importGirrSignalHelpButtonActionPerformed

    private void importLircHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importLircHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.importLircHelp);
    }//GEN-LAST:event_importLircHelpButtonActionPerformed

    private void importProntoClassicHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importProntoClassicHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.importProntoClassicHelp);
    }//GEN-LAST:event_importProntoClassicHelpButtonActionPerformed

    private void importIctHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importIctHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.importIctHelp);
    }//GEN-LAST:event_importIctHelpButtonActionPerformed

    private void importProntoProfessionalHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importProntoProfessionalHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.importProntoProfessionalHelp);
    }//GEN-LAST:event_importProntoProfessionalHelpButtonActionPerformed

    private void importTextRawHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importTextRawHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.importTextRawHelp);
    }//GEN-LAST:event_importTextRawHelpButtonActionPerformed

    private void importWaveHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importWaveHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.importWaveHelp);
    }//GEN-LAST:event_importWaveHelpButtonActionPerformed

    private void exportHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.exportHelp);
    }//GEN-LAST:event_exportHelpButtonActionPerformed

    private void printDecodesToConsoleCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printDecodesToConsoleCheckBoxMenuItemActionPerformed
        properties.setPrintDecodesToConsole(printDecodesToConsoleCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_printDecodesToConsoleCheckBoxMenuItemActionPerformed

    private void scrutinizeSignalHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scrutinizeSignalHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.scrutinizeSignalHelp);
    }//GEN-LAST:event_scrutinizeSignalHelpButtonActionPerformed

    private void transmitSignalButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transmitSignalButtonActionPerformed
        IrSignal irSignal;
        try {
            irSignal = getCapturedIrSignal();
        } catch (InvalidArgumentException ex) {
            guiUtils.error(ex);
            return;
        }
        if (irSignal == null || irSignal.isEmpty()) {
            guiUtils.error("Nothing to transmit");
            return;
        }
        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
            transmit(irSignal);
        } catch (IOException | HarcHardwareException | InvalidArgumentException | HardwareUnavailableException ex) {
            guiUtils.error(ex);
        } finally {
            busyWindow.unBusy();
        }
    }//GEN-LAST:event_transmitSignalButtonActionPerformed

    private void signalExportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_signalExportButtonActionPerformed
        try {
            saveSignal(getCapturedIrSignal());
        } catch (IOException | TransformerException | IrCoreException | IrpException | GirrException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_signalExportButtonActionPerformed

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        if (captureThreadRunning()) {
            guiUtils.error("A capture thread is running. This must first be ended.");
            return;
        }
        if (!hardwareManager.isReady()) {
            guiUtils.error("Selected device not ready (not opened?).");
            return;
        }
        if (!hardwareManager.canCapture()) {
            guiUtils.error("Selected device cannot capture.");
            return;
        }

        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
            ModulatedIrSequence modulatedIrSequence = captureIrSequence();

            if (modulatedIrSequence != null)
                processIr(modulatedIrSequence);
            else
                guiUtils.message("no signal received");
        } catch (IOException | HarcHardwareException | InvalidArgumentException | HardwareUnavailableException ex) {
            guiUtils.error(ex);
        } finally {
            busyWindow.unBusy();
        }
    }//GEN-LAST:event_startButtonActionPerformed

    private void capturedDataTextAreaMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_capturedDataTextAreaMouseReleased
        if (evt.isPopupTrigger())
            CCFCodePopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    }//GEN-LAST:event_capturedDataTextAreaMouseReleased

    private void capturedDataTextAreaMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_capturedDataTextAreaMousePressed
        if (evt.isPopupTrigger())
            CCFCodePopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        else if (evt.getButton() == MouseEvent.BUTTON2) { // X-Windows type paste
            String selection = CopyClipboardText.getSelection();
            int where = capturedDataTextArea.viewToModel(evt.getPoint());
            capturedDataTextArea.insert(selection, where);
        }
    }//GEN-LAST:event_capturedDataTextAreaMousePressed

    private void transferToParametricRemoteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transferToParametricRemoteButtonActionPerformed
        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
            Collection<Command> commands = irpMasterBean.getCommands().values();
            registerParameterSignal(commands);
        } catch (ParseException | GirrException | IrCoreException | IrpException ex) {
            guiUtils.error(ex);
        } finally {
            busyWindow.unBusy();
        }
    }//GEN-LAST:event_transferToParametricRemoteButtonActionPerformed

    private void transmitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transmitMenuItemActionPerformed
        try {
            transmit(parameterTable);
        } catch (HardwareUnavailableException ex) {
            guiUtils.error("Transmitting hardware not selected or not ready.");
        } catch (IOException | GirrException | ErroneousSelectionException | HarcHardwareException | IrCoreException | IrpException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_transmitMenuItemActionPerformed

    private void sendMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendMenuItemActionPerformed
        try {
            transmit(rawTable);
        } catch (GirrException | ErroneousSelectionException | IrpException | IrCoreException | IOException | HardwareUnavailableException | HarcHardwareException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_sendMenuItemActionPerformed

    private void hideUninterestingColumnsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hideUninterestingColumnsMenuItemActionPerformed
        ArrayList<Integer>list = parameterTableModel.getUninterestingColumns();
        parameterTableColumnModel.removeColumns(list);
    }//GEN-LAST:event_hideUninterestingColumnsMenuItemActionPerformed

    private void hideUninterestingColumnsMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hideUninterestingColumnsMenuItem1ActionPerformed
        ArrayList<Integer>list = rawTableModel.getUninterestingColumns();
        rawTableColumnModel.removeColumns(list);
    }//GEN-LAST:event_hideUninterestingColumnsMenuItem1ActionPerformed

    private void addMissingFsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addMissingFsMenuItemActionPerformed
        try {
            parameterTableAddMissingF();
        } catch (GirrException | ErroneousSelectionException | IrCoreException | IrpException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_addMissingFsMenuItemActionPerformed

    private void parametricSorterCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_parametricSorterCheckBoxMenuItemActionPerformed
        boolean state = parametricSorterCheckBoxMenuItem.isSelected();
        properties.setSorterOnParametrizedTable(state);
        enableSorter(parameterTable, state);
        moveDownMenuItem1.setEnabled(!state);
        moveUpMenuItem1.setEnabled(!state);
    }//GEN-LAST:event_parametricSorterCheckBoxMenuItemActionPerformed

    private void rawSorterCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawSorterCheckBoxMenuItemActionPerformed
        boolean state = rawSorterCheckBoxMenuItem.isSelected();
        properties.setSorterOnRawTable(state);
        enableSorter(rawTable, state);
        moveDownMenuItem.setEnabled(!state);
        moveUpMenuItem.setEnabled(!state);
    }//GEN-LAST:event_rawSorterCheckBoxMenuItemActionPerformed

    private void transmitSignalButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transmitSignalButton1ActionPerformed
        reAnalyze();
    }//GEN-LAST:event_transmitSignalButton1ActionPerformed

    private void pasteAnalyzeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasteAnalyzeButtonActionPerformed
        setCapturedDataTextAreaFromClipboard();
        reAnalyze();
    }//GEN-LAST:event_pasteAnalyzeButtonActionPerformed

    private void sendingHardwareHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendingHardwareHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.sendingHardwareHelp);
    }//GEN-LAST:event_sendingHardwareHelpButtonActionPerformed

    private void sendingTimeoutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendingTimeoutMenuItemActionPerformed
        try {
            Integer t = guiUtils.getIntegerInput("Sending time-out in milliseconds", properties.getSendingTimeout());
            if (t != null)
                properties.setSendingTimeout(t);
        } catch (NumberFormatException ex) {
            guiUtils.error("Invalid number: " + ex.getMessage());
        }
    }//GEN-LAST:event_sendingTimeoutMenuItemActionPerformed

    private void globalCacheTimeoutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_globalCacheTimeoutMenuItemActionPerformed
        try {
            Integer t = guiUtils.getIntegerInput("GlobalCache socket time-out in milliseconds", properties.getGlobalCacheTimeout());
            if (t != null) {
                properties.setGlobalCacheTimeout(t);
                globalCacheIrSenderSelector.setTimeout(t);
//                globalCacheCaptureSelector.setTimeout(t);
            }
        } catch (NumberFormatException ex) {
            guiUtils.error("Invalid number: " + ex.getMessage());
        }
    }//GEN-LAST:event_globalCacheTimeoutMenuItemActionPerformed

    private void clonePlotMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clonePlotMenuItemActionPerformed
        clonePlot();
    }//GEN-LAST:event_clonePlotMenuItemActionPerformed

    private void irWidgetHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irWidgetHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.irWidgetHardwareHelp);
    }//GEN-LAST:event_irWidgetHelpButtonActionPerformed

    private void exportGirrHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportGirrHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.exportGirrHelp);
    }//GEN-LAST:event_exportGirrHelpButtonActionPerformed

    private void exportWaveHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportWaveHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.exportWaveHelp);
    }//GEN-LAST:event_exportWaveHelpButtonActionPerformed

    private void exportProntoHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportProntoHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.exportProntoHelp);
    }//GEN-LAST:event_exportProntoHelpButtonActionPerformed

    private void sendingGlobalCacheHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendingGlobalCacheHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.globalCacheHelp);
    }//GEN-LAST:event_sendingGlobalCacheHelpButtonActionPerformed

    private void transmitAudioHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transmitAudioHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.sendingAudioHelp);
    }//GEN-LAST:event_transmitAudioHelpButtonActionPerformed

    private void scrutinizeParametricMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scrutinizeParametricMenuItemActionPerformed
        try {
            scrutinizeIrSignal(parameterTable);
        } catch (GirrException | ErroneousSelectionException | IrCoreException | IrpException ex) {
             guiUtils.error(ex);
        }
    }//GEN-LAST:event_scrutinizeParametricMenuItemActionPerformed

    private void importTextRawLineBasedHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importTextRawLineBasedHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.importTextRawLineBasedHelp);
    }//GEN-LAST:event_importTextRawLineBasedHelpButtonActionPerformed

    private void importTextParametrizedHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importTextParametrizedHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.importTextParametrizedHelp);
    }//GEN-LAST:event_importTextParametrizedHelpButtonActionPerformed

    private void parametrizedLearnIgnoreTCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_parametrizedLearnIgnoreTCheckBoxMenuItemActionPerformed
        properties.setParametrizedLearnIgnoreT(this.parametrizedLearnIgnoreTCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_parametrizedLearnIgnoreTCheckBoxMenuItemActionPerformed

    private void inquiryDeviceDataCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inquiryDeviceDataCheckBoxMenuItemActionPerformed
        properties.setExportInquireDeviceData(inquiryDeviceDataCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_inquiryDeviceDataCheckBoxMenuItemActionPerformed

    private void parametrizedCopyAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_parametrizedCopyAllMenuItemActionPerformed
        copyTableToClipboard(parameterTable, false);
    }//GEN-LAST:event_parametrizedCopyAllMenuItemActionPerformed

    private void parametrizedCopySelectionMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_parametrizedCopySelectionMenuItemActionPerformed
        copyTableToClipboard(parameterTable, true);
    }//GEN-LAST:event_parametrizedCopySelectionMenuItemActionPerformed

    private void rawCopyAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCopyAllMenuItemActionPerformed
        copyTableToClipboard(rawTable, false);
    }//GEN-LAST:event_rawCopyAllMenuItemActionPerformed

    private void rawCopySelectionMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCopySelectionMenuItemActionPerformed
        copyTableToClipboard(rawTable, true);
    }//GEN-LAST:event_rawCopySelectionMenuItemActionPerformed

    private void invokeAnalyzerCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_invokeAnalyzerCheckBoxMenuItemActionPerformed
        properties.setInvokeAnalyzer(invokeAnalyzerCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_invokeAnalyzerCheckBoxMenuItemActionPerformed

    private void openZipFilesCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openZipFilesCheckBoxMenuItemActionPerformed
        properties.setImportOpensZipFiles(openZipFilesCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_openZipFilesCheckBoxMenuItemActionPerformed

    private void deleteParametrizedCommentMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteParametrizedCommentMenuItemActionPerformed
        parameterTableModel.clearComment();
    }//GEN-LAST:event_deleteParametrizedCommentMenuItemActionPerformed

    private void deleteRawCommentMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteRawCommentMenuItemActionPerformed
        rawTableModel.clearComment();
    }//GEN-LAST:event_deleteRawCommentMenuItemActionPerformed

    private void exportGenerateShortCcfCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportGenerateShortCcfCheckBoxActionPerformed
        properties.setExportGenerateShortCcf(exportGenerateShortCcfCheckBox.isSelected());
    }//GEN-LAST:event_exportGenerateShortCcfCheckBoxActionPerformed

    private void offerStackTraceCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_offerStackTraceCheckBoxMenuItemActionPerformed
        boolean state = offerStackTraceCheckBoxMenuItem.isSelected();
        guiUtils.setOfferStackTrace(state);
        properties.setOfferStackTrace(state);
    }//GEN-LAST:event_offerStackTraceCheckBoxMenuItemActionPerformed

    private void setMiscParamsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setMiscParamsMenuItemActionPerformed
        List<Integer> rows = tableUtils.modelLinesSelected(parameterTable);
        if (rows.isEmpty()) {
            guiUtils.error("Nothing selected");
            return;
        }
        String value = guiUtils.getInput("Enter \"Misc. Params\" as string", "Parameters entry", "X=0");
        parameterTableModel.setMiscParameters(value, rows);
    }//GEN-LAST:event_setMiscParamsMenuItemActionPerformed

    private void parametrizedMultiColumnNameCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_parametrizedMultiColumnNameCheckBoxActionPerformed
        properties.setParametrizedNameMultiColumn(parametrizedMultiColumnNameCheckBox.isSelected());
    }//GEN-LAST:event_parametrizedMultiColumnNameCheckBoxActionPerformed

    private void rawMultiColumnNameCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawMultiColumnNameCheckBoxActionPerformed
        properties.setRawNameMultiColumn(rawMultiColumnNameCheckBox.isSelected());
    }//GEN-LAST:event_rawMultiColumnNameCheckBoxActionPerformed

    private void tableMouseReleased(JTable table, java.awt.event.MouseEvent evt) {
        if (evt.getButton() == transmitSignalMouseButton) {
            int row = table.rowAtPoint(evt.getPoint());
            if (row == -1)
                return;
            table.setRowSelectionInterval(row, row);
            try {
                transmit(table);
            } catch (IOException | GirrException | ErroneousSelectionException | HarcHardwareException | IrCoreException | IrpException | HardwareUnavailableException ex) {
                guiUtils.error(ex);
            }
        } else {
            int row = table.getSelectedRow();
            int column = table.getSelectedColumn();
            if (row >= 0 && column >= 0) {
                row = table.convertRowIndexToModel(row);
                column = table.convertColumnIndexToModel(column);
                Object thing = table.getModel().getValueAt(row, column);
                editingTextField.setText(thing != null ? thing.toString() : null);
                editingTextField.setEditable(table.getModel().isCellEditable(row, column));
                //editingTextField.setEnabled(parameterTableModel.getColumnClass(column) != Boolean.class);
            }
        }
    }

    private void parameterTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_parameterTableMouseReleased
        tableMouseReleased(parameterTable, evt);
    }//GEN-LAST:event_parameterTableMouseReleased

    private void noTransmitsComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_noTransmitsComboBoxActionPerformed
        properties.setTransmitGeneratedCount(Integer.parseInt((String) noTransmitsComboBox.getSelectedItem()));
    }//GEN-LAST:event_noTransmitsComboBoxActionPerformed

    private void irTransWebButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irTransWebButtonActionPerformed
        try {
            guiUtils.browse(new URI(IrTransImporter.homeUrl));
        } catch (URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_irTransWebButtonActionPerformed

    private void importIrTransHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importIrTransHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.importIrTransHelp);
    }//GEN-LAST:event_importIrTransHelpButtonActionPerformed

    private void releaseNotesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_releaseNotesMenuItemActionPerformed
        try {
            guiUtils.browse(new File(properties.mkPathAbsolute(properties.getReleaseNotesPath())));
        } catch (URISyntaxException | MalformedURLException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_releaseNotesMenuItemActionPerformed

    private void importSignalAsMode2MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importSignalAsMode2MenuItemActionPerformed
        importModulatedIrSequenceByFileSelector(new Mode2Importer());
    }//GEN-LAST:event_importSignalAsMode2MenuItemActionPerformed

    private void importSignalAsMode2MenuItem1importSignalAsWaveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importSignalAsMode2MenuItem1importSignalAsWaveMenuItemActionPerformed
        importModulatedIrSequenceByFileSelector(new Mode2Importer());
    }//GEN-LAST:event_importSignalAsMode2MenuItem1importSignalAsWaveMenuItemActionPerformed

    private void unsetTMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unsetTMenuItemActionPerformed
        List<Integer> rows = tableUtils.modelLinesSelected(parameterTable);
        if (rows.isEmpty()) {
            guiUtils.error("Nothing selected");
            return;
        }
        parameterTableModel.unsetParameter("T", rows);
    }//GEN-LAST:event_unsetTMenuItemActionPerformed

    private void debugCodeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debugCodeMenuItemActionPerformed
        guiUtils.getInput("TODO", "Not yet implemented", "");
    }//GEN-LAST:event_debugCodeMenuItemActionPerformed

    private void sendingGirsClientHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendingGirsClientHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.sendingGirsHelp);
    }//GEN-LAST:event_sendingGirsClientHelpButtonActionPerformed

    private void sendingCommandFusionHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendingCommandFusionHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.sendingCommandFusionHelp);
    }//GEN-LAST:event_sendingCommandFusionHelpButtonActionPerformed

    private void importCmlHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importCmlHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.importCmlHelp);
    }//GEN-LAST:event_importCmlHelpButtonActionPerformed

    private void importCommandFusionHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importCommandFusionHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.importCommandFusionHelp);
    }//GEN-LAST:event_importCommandFusionHelpButtonActionPerformed

    private void importCmlMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importCmlMenuItemActionPerformed
        importRemoteByFileSelector(cmlImporter, false);
    }//GEN-LAST:event_importCmlMenuItemActionPerformed

    private void importCommandFusionMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importCommandFusionMenuItemActionPerformed
        importRemoteByFileSelector(commandFusionImporter, false);
    }//GEN-LAST:event_importCommandFusionMenuItemActionPerformed

    private void importCmlMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importCmlMenuItem2ActionPerformed
        importRemoteByFileSelector(cmlImporter, true);
    }//GEN-LAST:event_importCmlMenuItem2ActionPerformed

    private void importCommandFusionMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importCommandFusionMenuItem2ActionPerformed
        importRemoteByFileSelector(commandFusionImporter, true);
    }//GEN-LAST:event_importCommandFusionMenuItem2ActionPerformed

    private void tutorialMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tutorialMenuItemActionPerformed
        try {
            guiUtils.browse(new URI(properties.getTutorialUrl()));
        } catch (URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_tutorialMenuItemActionPerformed

    private void exportCharsetMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportCharsetMenuItemActionPerformed
        String charSet = guiUtils.getInput("Enter character set to be used for export (e.g. US-ASCII, UTF-8, ISO-8859-1, WINDOWS-1252)", "Export character set inquiry", properties.getExportCharsetName());
        if (charSet == null)
            return;
        if (Charset.isSupported(charSet)) {
            properties.setExportCharsetName(charSet);
            Exporter.setEncoding(charSet);
        } else
            guiUtils.error("Character set \"" + charSet + "\" is not supported");
    }//GEN-LAST:event_exportCharsetMenuItemActionPerformed

    private void importCharsetMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importCharsetMenuItemActionPerformed
        String s = guiUtils.getInput("Enter character set to be used for import (e.g. US-ASCII, UTF-8, ISO-8859-1, WINDOWS-1252)", "Import character set inquiry", properties.getExportCharsetName());
         if (s == null)
            return;
        if (Charset.isSupported(s))
            properties.setImportCharsetName(s);
        else
            guiUtils.error("Character set \"" + s + "\" is not supported");
    }//GEN-LAST:event_importCharsetMenuItemActionPerformed

    private void gitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gitMenuItemActionPerformed
        try {
            guiUtils.browse(new URI(GIT_URL));
        } catch (URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_gitMenuItemActionPerformed

    private void issuesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_issuesMenuItemActionPerformed
        try {
            guiUtils.browse(new URI(ISSUES_URL));
        } catch (URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_issuesMenuItemActionPerformed

    private void cleanerCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cleanerCheckBoxMenuItemActionPerformed
        properties.setInvokeCleaner(cleanerCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_cleanerCheckBoxMenuItemActionPerformed

    private void sendingDevLircHardwareHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendingDevLircHardwareHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.devLircHelp);
    }//GEN-LAST:event_sendingDevLircHardwareHelpButtonActionPerformed

    private void importMode2HelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importMode2HelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.importMode2Help);
    }//GEN-LAST:event_importMode2HelpButtonActionPerformed

    private void controlTowerImportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_controlTowerImportButtonActionPerformed
        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
            String manufacturer = (String) controlTowerManufacturerComboBox.getSelectedItem();
            String deviceType = (String) controlTowerDeviceTypeComboBox.getSelectedItem();
            String modelName = (String) controlTowerCodeSetComboBox.getSelectedItem();
            String codeSet = controlTowerCodesetTable.get(modelName);
            controlTowerIrDatabase.load(manufacturer, deviceType, codeSet);
            controlTowerTreeImporter.setRemoteSet(controlTowerIrDatabase.getRemoteSet(), "Control Tower");
        } catch (IOException ex) {
            guiUtils.error(ex);
        } finally {
            busyWindow.unBusy();
        }
    }//GEN-LAST:event_controlTowerImportButtonActionPerformed

    private void controlTowerBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_controlTowerBrowseButtonActionPerformed
        try {
            guiUtils.browse(new URI("http", ControlTowerIrDatabase.controlTowerIrDatabaseHost, null));
        } catch (URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_controlTowerBrowseButtonActionPerformed

    private void controlTowerManufacturerComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_controlTowerManufacturerComboBoxActionPerformed
        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
            if (controlTowerIrDatabase == null) {
                controlTowerIrDatabase = new ControlTowerIrDatabase(properties.getVerbose());
                Collection<String> manufacturers = controlTowerIrDatabase.getManufacturers();
                String[] arr = manufacturers.toArray(new String[0]);
                //resetCursor(oldCursor);
                Arrays.sort(arr, String.CASE_INSENSITIVE_ORDER);
                DefaultComboBoxModel dcbm = new DefaultComboBoxModel(arr);
                controlTowerManufacturerComboBox.setModel(dcbm);
                controlTowerManufacturerComboBoxActionPerformed(null);
            } else {
                String manufacturer = (String) controlTowerManufacturerComboBox.getSelectedItem();
                Collection<String> devTypes = controlTowerIrDatabase.getDeviceTypes(manufacturer);
                String[] arr = devTypes.toArray(new String[0]);
                Arrays.sort(arr, String.CASE_INSENSITIVE_ORDER);
                DefaultComboBoxModel dcbm = new DefaultComboBoxModel(arr);
                controlTowerDeviceTypeComboBox.setModel(dcbm);
                controlTowerDeviceTypeComboBoxActionPerformed(null);
                controlTowerDeviceTypeComboBox.setEnabled(true);
            }
            controlTowerTreeImporter.clear();
        } catch (IOException ex) {
            guiUtils.error(ex);
        } finally {
            busyWindow.unBusy();
        }
    }//GEN-LAST:event_controlTowerManufacturerComboBoxActionPerformed

    private void controlTowerDeviceTypeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_controlTowerDeviceTypeComboBoxActionPerformed
        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
            String manufacturer = (String) controlTowerManufacturerComboBox.getSelectedItem();
            String deviceType = (String) controlTowerDeviceTypeComboBox.getSelectedItem();
            controlTowerCodesetTable = controlTowerIrDatabase.getCodesetTable(manufacturer, deviceType);
            String[] arr = controlTowerCodesetTable.keySet().toArray(new String[controlTowerCodesetTable.size()]);
            Arrays.sort(arr, String.CASE_INSENSITIVE_ORDER);
            DefaultComboBoxModel dcbm = new DefaultComboBoxModel(arr);
            controlTowerCodeSetComboBox.setModel(dcbm);
            controlTowerCodeSetComboBox.setEnabled(true);
            controlTowerImportButton.setEnabled(true);
        } catch (IOException ex) {
            guiUtils.error(ex);
        } finally {
            busyWindow.unBusy();
        }
    }//GEN-LAST:event_controlTowerDeviceTypeComboBoxActionPerformed

    private void importControlTowerHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importControlTowerHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.importControlTowerHelp);
    }//GEN-LAST:event_importControlTowerHelpButtonActionPerformed

    private void topLevelTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_topLevelTabbedPaneStateChanged
        // Must make sure this is not effective during early initComponents().
        if (hardwareManager != null) {
            lastPane = currentPane;
            currentPane = topLevelTabbedPane.getSelectedComponent();
            properties.setSelectedMainPaneIndex(topLevelTabbedPane.getSelectedIndex());
        }
    }//GEN-LAST:event_topLevelTabbedPaneStateChanged

    private void rawCookedTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rawCookedTabbedPaneStateChanged
        // Must make sure this is not effective during early initComponents().
        if (hardwareManager != null)
            properties.setSelectedRemoteIndex(rawCookedTabbedPane.getSelectedIndex());
    }//GEN-LAST:event_rawCookedTabbedPaneStateChanged

    private void rawCodePasteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodePasteMenuItemActionPerformed
        insertCapturedDataTextAreaFromClipboard();
    }//GEN-LAST:event_rawCodePasteMenuItemActionPerformed

    private void continuousCaptureButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_continuousCaptureButtonActionPerformed
        if (!hardwareManager.isReady()) {
            guiUtils.error("No capture device selected, aborting");
            continuousCaptureButton.setSelected(false);
            return;
        }
        if (continuousCaptureButton.isSelected() && captureThreadRunning()) {
            guiUtils.error("Another capture thread is running. This must first be ended.");
            continuousCaptureButton.setSelected(false);
            return;
        }

        if (continuousCaptureButton.isSelected()) {
            captureThread = new CaptureThread(new CaptureThreadClient() {
                @Override
                public JToggleButton getButton() {
                    return continuousCaptureButton;
                }

                @Override
                public void processSequence(ModulatedIrSequence sequence) {
                    try {
                        processIr(sequence);
                    } catch (InvalidArgumentException ex) {
                    }
                }
            });
            captureThread.start();
            topLevelTabbedPane.setEnabled(false);
            jumpToLastPanelMenuItem.setEnabled(false);
        }
    }//GEN-LAST:event_continuousCaptureButtonActionPerformed

    private void checkTable(NamedIrSignal.LearnedIrSignalTableModel tableModel) {
        boolean success = tableModel.sanityCheck(guiUtils);
        guiUtils.info(success ? "No problems found" : "Problems found");
    }

    private void checkParametrizedSignalsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkParametrizedSignalsMenuItemActionPerformed
        checkTable(parameterTableModel);
    }//GEN-LAST:event_checkParametrizedSignalsMenuItemActionPerformed

    private void checkRawCommandsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkRawCommandsMenuItemActionPerformed
        checkTable(rawTableModel);
    }//GEN-LAST:event_checkRawCommandsMenuItemActionPerformed

    private void addMissingNamesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addMissingNamesMenuItemActionPerformed
        parameterTableModel.addDummyNames();
    }//GEN-LAST:event_addMissingNamesMenuItemActionPerformed

    private void addNamePrefixMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addNamePrefixMenuItemActionPerformed
        if (! checkRowsSelected(parameterTable))
            return;

        String prefix = guiUtils.getInput("Enter desired prefix", "Prefix inquiry", "cmd_");
        if (prefix != null)
            transformNameActionPerformed(parameterTable, parameterTableModel, (String s) -> prefix + s);
    }//GEN-LAST:event_addNamePrefixMenuItemActionPerformed

    private void transformNameMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transformNameMenuItemActionPerformed
        if (! checkRowsSelected(parameterTable))
            return;

        String old = guiUtils.getInput("Enter string to be replaced (regular expression)", "String inquiry", "test");
        if (old == null)
            return;
        String replacement = guiUtils.getInput("Replace \"" + old + "\" by ...", "String inquiry", old);
        if (replacement != null)
            transformNameActionPerformed(parameterTable, parameterTableModel, (String s) -> s.replaceFirst(old, replacement));
    }//GEN-LAST:event_transformNameMenuItemActionPerformed

    private void deleteDefaultedSignalsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteDefaultedSignalsMenuItemActionPerformed
        boolean isOk = guiUtils.confirm("This will delete all signal with automatically generated names.\n"
                + "The operation cannot be undone. Consider saving first.\n\nDo you want to continue?");
        if (isOk)
            parameterTableModel.deleteDefaultedSignals();
    }//GEN-LAST:event_deleteDefaultedSignalsMenuItemActionPerformed

    private void printAnalyzeIRPsToConsoleCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printAnalyzeIRPsToConsoleCheckBoxMenuItemActionPerformed
        properties.setPrintAnalyzerIRPsToConsole(printAnalyzeIRPsToConsoleCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_printAnalyzeIRPsToConsoleCheckBoxMenuItemActionPerformed

    private void undoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_undoMenuItemActionPerformed
        try {
            capturedDataTextArea.undo();
        } catch (UndoableJTextArea.UndoHistoryEmptyException ex) {
            guiUtils.error("No (further) undo informationm");
        }
    }//GEN-LAST:event_undoMenuItemActionPerformed

    private void undoDataMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_undoDataMenuItemActionPerformed
        undoMenuItemActionPerformed(evt);
    }//GEN-LAST:event_undoDataMenuItemActionPerformed

    private void rawWithoutSignsRadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawWithoutSignsRadioButtonMenuItemActionPerformed
         updateOutputFormat(OutputTextFormat.rawWithoutSigns);
    }//GEN-LAST:event_rawWithoutSignsRadioButtonMenuItemActionPerformed

    private void absToleranceMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_absToleranceMenuItemActionPerformed
        try {
            Double t = guiUtils.getDoubleInput("Absolute tolerance for duration comparisons (ms).", properties.getAbsoluteTolerance());
            if (t != null)
                properties.setAbsoluteTolerance(t);
        } catch (NumberFormatException ex) {
            guiUtils.error("Invalid number: " + ex.getMessage());
        }
    }//GEN-LAST:event_absToleranceMenuItemActionPerformed

    private void relToleranceMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_relToleranceMenuItemActionPerformed
        try {
            Double t = guiUtils.getDoubleInput("Relative tolerance for duration comparisons (between 0.0 and 1.0).", properties.getRelativeTolerance());
            if (t != null)
                properties.setRelativeTolerance(t);
        } catch (NumberFormatException ex) {
            guiUtils.error("Invalid number: " + ex.getMessage());
        }
    }//GEN-LAST:event_relToleranceMenuItemActionPerformed

    private void frequencyToleranceMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_frequencyToleranceMenuItemActionPerformed
        try {
            Double t = guiUtils.getDoubleInput("Absolute tolerance for frequency comparisons (Hz).", properties.getFrequencyTolerance());
            if (t != null)
                properties.setFrequencyTolerance(t);
        } catch (NumberFormatException ex) {
            guiUtils.error("Invalid number: " + ex.getMessage());
        }
    }//GEN-LAST:event_frequencyToleranceMenuItemActionPerformed

    private void minLeadoutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_minLeadoutMenuItemActionPerformed
        try {
            Double t = guiUtils.getDoubleInput("Threshold value for lead-out (ms).", properties.getMinLeadOut());
            if (t != null)
                properties.setMinLeadOut(t);
        } catch (NumberFormatException ex) {
            guiUtils.error("Invalid number: " + ex.getMessage());
        }
    }//GEN-LAST:event_minLeadoutMenuItemActionPerformed

    private void minRepeatGapMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_minRepeatGapMenuItemActionPerformed
        try {
            Double t = guiUtils.getDoubleInput("Minumal value in micro seconds to be considered as ending a repeat.", properties.getMinRepeatLastGap());
            if (t != null)
                properties.setMinRepeatLastGap(t);
        } catch (NumberFormatException ex) {
            guiUtils.error("Invalid number: " + ex.getMessage());
        }
    }//GEN-LAST:event_minRepeatGapMenuItemActionPerformed

    private void dummyGapMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dummyGapMenuItemActionPerformed
        try {
            Double t = guiUtils.getDoubleInput("Dummy gap added for sequences with an odd number of durations (ms).", properties.getDummyGap());
            if (t != null)
                properties.setDummyGap(t);
        } catch (NumberFormatException ex) {
            guiUtils.error("Invalid number: " + ex.getMessage());
        }
    }//GEN-LAST:event_dummyGapMenuItemActionPerformed

    private void debugDecodeProtocolRegexpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debugDecodeProtocolRegexpMenuItemActionPerformed
        String s = guiUtils.getInput("Regular exporession (lowercase) for protocol name", "Debug info", Decoder.getDebugProtocolRegExp());
        if (s != null)
            Decoder.setDebugProtocolRegExp(s.isEmpty() ? null : s);
    }//GEN-LAST:event_debugDecodeProtocolRegexpMenuItemActionPerformed

    private void downloadsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downloadsMenuItemActionPerformed
        try {
            guiUtils.browse(new URI(DOWNLOADS_URL));
        } catch (URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_downloadsMenuItemActionPerformed

    private void irpTransmogrifierHelpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irpTransmogrifierHelpMenuItemActionPerformed
        try {
            guiUtils.browse(new File(properties.mkPathAbsolute(properties.getIrpTransmogrifierHelpfilePath())));
        } catch (MalformedURLException | URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_irpTransmogrifierHelpMenuItemActionPerformed

    private void glossaryMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_glossaryMenuItemActionPerformed
        try {
            guiUtils.browse(new File(properties.mkPathAbsolute(properties.getGlossaryFilePath())));
        } catch (MalformedURLException | URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_glossaryMenuItemActionPerformed

    private void rawAddTestSignalMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawAddTestSignalMenuItemActionPerformed
        try {
            scrutinizeIrSignal(testSignal);
        } catch (InvalidArgumentException ex) {
            throw new ThisCannotHappenException();
        }
    }//GEN-LAST:event_rawAddTestSignalMenuItemActionPerformed

    private void addRawTestSignalMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addRawTestSignalMenuItemActionPerformed
        RawIrSignal cir = new RawIrSignal(testSignal, "test", "Generated signal (NEC1 12.34 56)");
        registerRawCommand(cir);
    }//GEN-LAST:event_addRawTestSignalMenuItemActionPerformed

    private void addParametrizedTestSignalMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addParametrizedTestSignalMenuItemActionPerformed
        try {
            ParametrizedIrSignal signal = new ParametrizedIrSignal(testSignal, "test", "", false);
            registerParameterSignal(signal);
        } catch (NoDecodeException ex) {
            throw new ThisCannotHappenException();
        }
    }//GEN-LAST:event_addParametrizedTestSignalMenuItemActionPerformed

    private void addEmptyRawMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addEmptyRawMenuItemActionPerformed
        RawIrSignal signal = new RawIrSignal();
        registerRawCommand(signal);
    }//GEN-LAST:event_addEmptyRawMenuItemActionPerformed

    private void duplicateParametricMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_duplicateParametricMenuItemActionPerformed
        try {
            tableUtils.duplicateTableSelectedRow(parameterTable);
        } catch (ErroneousSelectionException | GirrException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_duplicateParametricMenuItemActionPerformed

    private void duplicateRawMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_duplicateRawMenuItemActionPerformed
        try {
            tableUtils.duplicateTableSelectedRow(rawTable);
        } catch (ErroneousSelectionException | GirrException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_duplicateRawMenuItemActionPerformed

    private void protocolDocuMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolDocuMenuItemActionPerformed
        try {
            protocolDocuPopup(tableUtils.commandTableSelectedRow(parameterTable));
        } catch (ErroneousSelectionException | GirrException | IrpException | IrCoreException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_protocolDocuMenuItemActionPerformed

    private void scrutinizeSignalProtocolDocuMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scrutinizeSignalProtocolDocuMenuItemActionPerformed
        String str = decodeIRTextField.getText();
        String[] arr = str.split(":");
        try {
            protocolDocuPopup(arr[0]);
        } catch (IrpException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_scrutinizeSignalProtocolDocuMenuItemActionPerformed

    private void rawProtocolDocuMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawProtocolDocuMenuItemActionPerformed
         try {
            protocolDocuPopup(tableUtils.commandTableSelectedRow(rawTable));
        } catch (ErroneousSelectionException | GirrException | IrpException | IrCoreException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_rawProtocolDocuMenuItemActionPerformed

    private void autoOpenExportsCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoOpenExportsCheckBoxActionPerformed
        properties.setAutoOpenExports(autoOpenExportsCheckBox.isSelected());
    }//GEN-LAST:event_autoOpenExportsCheckBoxActionPerformed

    private void miscParametersColumnComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miscParametersColumnComboBoxActionPerformed
        properties.setMiscParametersColumn(miscParametersColumnComboBox.getSelectedIndex());
    }//GEN-LAST:event_miscParametersColumnComboBoxActionPerformed

    private void rawUniquefyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawUniquefyMenuItemActionPerformed
        rawTableModel.uniquifyNames(UNIQUE_SEPARATOR);
    }//GEN-LAST:event_rawUniquefyMenuItemActionPerformed

    private void parameterUniquefyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_parameterUniquefyMenuItemActionPerformed
        parameterTableModel.uniquifyNames(UNIQUE_SEPARATOR);
    }//GEN-LAST:event_parameterUniquefyMenuItemActionPerformed

    private void rawPasteColumnMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawPasteColumnMenuItemActionPerformed
        if (properties.getSorterOnRawTable()) {
            guiUtils.error("Column paste not possible with enabled sorter.");
            return;
        }
        try {
            pasteColumnInTable(rawTable, rawTableModel);
        } catch (NoSelectionException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_rawPasteColumnMenuItemActionPerformed

    private void parametrizedPasteColumnMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_parametrizedPasteColumnMenuItemActionPerformed
        if (properties.getSorterOnParametrizedTable()) {
            guiUtils.error("Column paste not possible with enabled sorter.");
            return;
        }
        try {
            pasteColumnInTable(parameterTable, parameterTableModel);
        } catch (NoSelectionException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_parametrizedPasteColumnMenuItemActionPerformed

    private void ignoreLeadingGarbageCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ignoreLeadingGarbageCheckBoxMenuItemActionPerformed
        properties.setIgnoreLeadingGarbage(ignoreLeadingGarbageCheckBoxMenuItem.isSelected());
        decoderParameters.setIgnoreLeadingGarbage(ignoreLeadingGarbageCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_ignoreLeadingGarbageCheckBoxMenuItemActionPerformed

    private void clearSelectionParametricMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearSelectionParametricMenuItemActionPerformed
        parameterTable.clearSelection();
    }//GEN-LAST:event_clearSelectionParametricMenuItemActionPerformed

    private void clearSelectionRawMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearSelectionRawMenuItemActionPerformed
        rawTable.clearSelection();
    }//GEN-LAST:event_clearSelectionRawMenuItemActionPerformed

    private void copyToParametrizedMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyToParametrizedMenuItemActionPerformed
        try {
            Map<String, Command> selected = tableUtils.commandTableSelected(rawTable);
            if (selected.isEmpty()) {
                guiUtils.error("Nothing selected");
                return;
            }
            importCommands(selected.values(), null, false);
        } catch (GirrException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_copyToParametrizedMenuItemActionPerformed

    private void publicKeyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_publicKeyMenuItemActionPerformed
        HelpPopup.newHelpPopup(this, Version.publicKey, "Author's public key");
    }//GEN-LAST:event_publicKeyMenuItemActionPerformed

    private void removeDefaultedParametersCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeDefaultedParametersCheckBoxMenuItemActionPerformed
        properties.setRemoveDefaultedParameters(removeDefaultedParametersCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_removeDefaultedParametersCheckBoxMenuItemActionPerformed

    private void rawChopMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawChopMenuItemActionPerformed
        List<Integer> rows = tableUtils.modelLinesSelected(rawTable);
        if (rows.isEmpty()) {
            guiUtils.error("Nothing selected");
            return;
        }

        try {
            rawTableModel.chopSignals(rows, properties.getChopThreshold());
        } catch (IrpException | IrCoreException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_rawChopMenuItemActionPerformed

    private void chopThresholdMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chopThresholdMenuItemActionPerformed
          try {
            Double t = guiUtils.getDoubleInput("Threshold value in milliseconds for chopping long signals.", properties.getChopThreshold());
            if (t != null)
                properties.setChopThreshold(t);
        } catch (NumberFormatException ex) {
            guiUtils.error("Invalid number: " + ex.getMessage());
        }
    }//GEN-LAST:event_chopThresholdMenuItemActionPerformed

    private void chopIctImportCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chopIctImportCheckBoxActionPerformed
        boolean isSel = chopIctImportCheckBox.isSelected();
        ictImporter.setChop(isSel);
        properties.setChopIctImports(isSel);
    }//GEN-LAST:event_chopIctImportCheckBoxActionPerformed

    private void prontoExportScreenWidthTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_prontoExportScreenWidthTextFieldFocusLost
        properties.setProntoScreenSizeX(Integer.parseInt(prontoExportScreenWidthTextField.getText()));
    }//GEN-LAST:event_prontoExportScreenWidthTextFieldFocusLost

    private void prontoExportScreenHeightTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_prontoExportScreenHeightTextFieldFocusLost
        properties.setProntoScreenSizeY(Integer.parseInt(prontoExportScreenHeightTextField.getText()));
    }//GEN-LAST:event_prontoExportScreenHeightTextFieldFocusLost

    private void prontoExportButtonWidthTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_prontoExportButtonWidthTextFieldFocusLost
        properties.setProntoButtonSizeX(Integer.parseInt(prontoExportButtonWidthTextField.getText()));
    }//GEN-LAST:event_prontoExportButtonWidthTextFieldFocusLost

    private void prontoExportButtonHeightTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_prontoExportButtonHeightTextFieldFocusLost
        properties.setProntoButtonSizeY(Integer.parseInt(prontoExportButtonHeightTextField.getText()));
    }//GEN-LAST:event_prontoExportButtonHeightTextFieldFocusLost

    private void remoteLocatorRemoteComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remoteLocatorRemoteComboBoxActionPerformed
        String manufacturer = (String) remoteLocatorManufacturerComboBox.getSelectedItem();
        String deviceType = (String) remoteLocatorDeviceTypeComboBox.getSelectedItem();
        String remoteName = (String) remoteLocatorRemoteComboBox.getSelectedItem();
        remoteLocatorImportButton.setEnabled(false);
        remoteLocatorBrowseRemoteButton.setEnabled(false);
        remoteLocatorTreeImporter.clear();
        if (remoteName == null)
            return;
        try {
            remoteLocatorImporter.load(manufacturer, deviceType, remoteName);
            remoteLocatorImporterKindTextField.setText(remoteLocatorImporter.getkind());
            remoteLocatorImportButton.setEnabled(remoteLocatorImporter.isLoadable());
            remoteLocatorBrowseRemoteButton.setEnabled(true);
        } catch (NotFoundException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_remoteLocatorRemoteComboBoxActionPerformed

    private void analyzerBase4RadioButtonMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_analyzerBase4RadioButtonMenuItemActionPerformed
        properties.setAnalyzerBase(4);
    }//GEN-LAST:event_analyzerBase4RadioButtonMenuItemActionPerformed

    private void NamedCommandMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NamedCommandMenuItemActionPerformed
        HarcletFrame.newHarcletFrame(this, new NamedCommandPanel(), false, lookAndFeelManager.getCurrentLAFClassName());
    }//GEN-LAST:event_NamedCommandMenuItemActionPerformed

    private void captureTestButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_captureTestButtonActionPerformed
        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
            if (captureThreadRunning()) {
                guiUtils.error("A capture thread is running. This must first be ended.");
                return;
            }
            if (!hardwareManager.isReady()) {
                guiUtils.error("Selected device not ready (not opened?).");
                return;
            }
            if (!hardwareManager.canCapture()) {
                guiUtils.error("Selected device cannot capture.");
                return;
            }

            ModulatedIrSequence modulatedIrSequence = captureIrSequence();

            if (modulatedIrSequence == null) {
                guiUtils.error("No signal received.");
                return;
            }

            IrSignal signal = InterpretString.interpretIrSequence(modulatedIrSequence, true/*properties.getInvokeRepeatFinder()*/,
                    true /*properties.getInvokeCleaner()*/, properties.getAbsoluteTolerance(), properties.getRelativeTolerance());
            guiUtils.message(modulatedIrSequence.toString(true));
            guiUtils.message("f=" + Math.round(modulatedIrSequence.getFrequency()));
            Decoder.SimpleDecodesSet decodes = decoder.decodeIrSignal(signal, decoderParameters);
            if (decodes.isEmpty())
                guiUtils.message("No decodes.");
            else
                decodes.forEach((decode) -> {
                    guiUtils.message(decode.toString());
                });
        } catch (TimeoutException ex) {
            guiUtils.error("Timeout capturing signal");
        } catch (HardwareUnavailableException | IOException | HarcHardwareException | NumberFormatException | InvalidArgumentException ex) {
            guiUtils.error(ex);
        } finally {
            busyWindow.unBusy();
        }
    }//GEN-LAST:event_captureTestButtonActionPerformed

    private void exportFormatDocumentationButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportFormatDocumentationButtonActionPerformed
//        IExporterFactory factory = exportFormatManager.getSelected();
//        ICommandExporter exporter = factory.newExporter();
        org.w3c.dom.Document doc = exportFormatManager.getDocument();
        String name = exportFormatManager.getFormatName();
        if (doc != null)
            HelpPopup.newHelpPopup(this, doc, "Documentation for " + name);
        else
            guiUtils.error("No documentation available for format " + name);
    }//GEN-LAST:event_exportFormatDocumentationButtonActionPerformed

    private void lowercaseNameMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lowercaseNameMenuItemActionPerformed
        transformNameActionPerformed(parameterTable, parameterTableModel, (String s) -> s.toLowerCase(namesLocale));
    }//GEN-LAST:event_lowercaseNameMenuItemActionPerformed

    private void upperCaseMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upperCaseMenuItemActionPerformed
        transformNameActionPerformed(parameterTable, parameterTableModel, (String s) -> s.toUpperCase(namesLocale));
    }//GEN-LAST:event_upperCaseMenuItemActionPerformed

    private void captitalizeNameMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_captitalizeNameMenuItemActionPerformed
        transformNameActionPerformed(parameterTable, parameterTableModel, (String s) -> s.substring(0, 1).toUpperCase(namesLocale) + s.substring(1).toLowerCase(namesLocale));
    }//GEN-LAST:event_captitalizeNameMenuItemActionPerformed

    private void printAlternativeDecodesCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printAlternativeDecodesCheckBoxMenuItemActionPerformed
        properties.setPrintAlternativeDecodes(this.printAlternativeDecodesCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_printAlternativeDecodesCheckBoxMenuItemActionPerformed

    private void exportGenerateBroadlinkHexCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportGenerateBroadlinkHexCheckBoxActionPerformed
        properties.setExportGenerateBroadlinkHex(exportGenerateBroadlinkHexCheckBox.isSelected());
        exportRepeatComboBox.setEnabled(exportGenerateSendIrCheckBox.isSelected()
                || exportGenerateBroadlinkHexCheckBox.isSelected()
                || exportGenerateBroadlinkBase64CheckBox.isSelected());
    }//GEN-LAST:event_exportGenerateBroadlinkHexCheckBoxActionPerformed

    private void exportGenerateBroadlinkBase64CheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportGenerateBroadlinkBase64CheckBoxActionPerformed
        properties.setExportGenerateBroadlinkBase64(exportGenerateBroadlinkBase64CheckBox.isSelected());
        exportRepeatComboBox.setEnabled(exportGenerateSendIrCheckBox.isSelected()
                || exportGenerateBroadlinkHexCheckBox.isSelected()
                || exportGenerateBroadlinkBase64CheckBox.isSelected());
    }//GEN-LAST:event_exportGenerateBroadlinkBase64CheckBoxActionPerformed

    private void disussionsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disussionsMenuItemActionPerformed
        try {
            guiUtils.browse(new URI(DISCUSSIONS_URL));
        } catch (URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_disussionsMenuItemActionPerformed

    private void girrAcceptEmptyCommandsCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_girrAcceptEmptyCommandsCheckBoxMenuItemActionPerformed
        boolean status = girrAcceptEmptyCommandsCheckBoxMenuItem.isSelected();
        properties.setAllowEmptyGirrCommands(status);
        Command.setAcceptEmptyCommands(status);
    }//GEN-LAST:event_girrAcceptEmptyCommandsCheckBoxMenuItemActionPerformed

    private void deleteEmptyParametrizedSignalsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteEmptyParametrizedSignalsMenuItemActionPerformed
        boolean isOk = guiUtils.confirm("This will delete all signal with no protocol and no parameters.\n"
                + "The operation cannot be undone. Consider saving first.\n\nDo you want to continue?");
        if (isOk)
            parameterTableModel.deleteEmptySignals();
    }//GEN-LAST:event_deleteEmptyParametrizedSignalsMenuItemActionPerformed

    private void deleteEmptyRawSignalsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteEmptyRawSignalsMenuItemActionPerformed
        boolean isOk = guiUtils.confirm("This will delete all signal with no non-empty sequences.\n"
                + "The operation cannot be undone. Consider saving first.\n\nDo you want to continue?");
        if (isOk)
            rawTableModel.deleteEmptySignals();
    }//GEN-LAST:event_deleteEmptyRawSignalsMenuItemActionPerformed

    private void searchParametrizedMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchParametrizedMenuItemActionPerformed
        tableUtils.searchNameInTable(parameterTable);
    }//GEN-LAST:event_searchParametrizedMenuItemActionPerformed

    private void parameterTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_parameterTableKeyReleased
        tableKeyReleased(parameterTable, evt);
    }//GEN-LAST:event_parameterTableKeyReleased

    private void rawTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_rawTableKeyReleased
        tableKeyReleased(rawTable, evt);
    }//GEN-LAST:event_rawTableKeyReleased

    private void searchRawMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchRawMenuItemActionPerformed
        tableUtils.searchNameInTable(rawTable);
    }//GEN-LAST:event_searchRawMenuItemActionPerformed

    private void secondaryIrpProtocolsSelectMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_secondaryIrpProtocolsSelectMenuItemActionPerformed
        String secondary = properties.getSecondaryIrpProtocolsPath();
        String selectorStartDir = secondary.isEmpty() ? System.getProperty("user.home") : new File(properties.mkPathAbsolute(secondary)).getParent();
        File f = SelectFile.selectFile(this, "Select secondary protocol file", selectorStartDir, false, false, "XML files (*.xml)", "xml");
        if (f == null)
            return;
        if (f.toString().equals("/dev/null") || f.getName().equalsIgnoreCase("NULL:")) {
            properties.setSecondaryIrpProtocolsPath("");
            guiUtils.message("secondary IrpProtocol was removed.");
        } else {
            properties.setSecondaryIrpProtocolsPath(f.getAbsolutePath());
        }
        try {
            setupIrpDatabase();
        } catch (IOException | IrpParseException | SAXException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_secondaryIrpProtocolsSelectMenuItemActionPerformed

    private void secondaryIrpProtocolsEditMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_secondaryIrpProtocolsEditMenuItemActionPerformed
        try {
            String secondary = properties.getSecondaryIrpProtocolsPath();
            if (secondary.isEmpty()) {
                guiUtils.error("No secondary IrpProtocol selected. Nothing to edit.");
                return;
            }
            guiUtils.open(new File(properties.mkPathAbsolute(secondary)));
            guiUtils.warning("If editing the file, changes will not take effect before you save the file, select \"reload\", (or restart the program).");
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_secondaryIrpProtocolsEditMenuItemActionPerformed

    private void irpProtocolsReloadMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irpProtocolsReloadMenuItemActionPerformed
        try {
            setupIrpDatabase();
        } catch (IOException | IrpParseException | SAXException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_irpProtocolsReloadMenuItemActionPerformed

    private void secondaryExportFormatsEditMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_secondaryExportFormatsEditMenuItemActionPerformed
        try {
            String secondary = properties.getSecondaryExportFormatFilePath();
            if (secondary.isEmpty()) {
                guiUtils.error("No secondary export format selected. Nothing to edit.");
                return;
            }
            guiUtils.open(new File(properties.mkPathAbsolute(secondary)));
            guiUtils.warning("If editing, changes will not take effect before reloading.");
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_secondaryExportFormatsEditMenuItemActionPerformed

    private void secondaryExportFormatsSelectMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_secondaryExportFormatsSelectMenuItemActionPerformed
        String secondary = properties.getSecondaryExportFormatFilePath();
        String selectorStartDir = secondary.isEmpty() ? System.getProperty("user.home") : new File(properties.mkPathAbsolute(secondary)).getParent();
        File f = SelectFile.selectFile(this, "Select secondary export format file/directory", selectorStartDir, false, false, JFileChooser.FILES_AND_DIRECTORIES, "XML files (*.xml)", "xml");

        if (f == null)
            return;
        if (f.toString().equals("/dev/null") || f.getName().equalsIgnoreCase("NULL:")) {
            properties.setSecondaryExportFormatFilePath("");
            guiUtils.message("secondary export format removed.");
        } else {
            properties.setSecondaryExportFormatFilePath(f.getAbsolutePath());
        }
        loadExportFormatsGuiRefresh();
    }//GEN-LAST:event_secondaryExportFormatsSelectMenuItemActionPerformed

    private void tableKeyReleased(JTable table, KeyEvent evt) {
        if (evt.getModifiersEx() == java.awt.event.InputEvent.CTRL_DOWN_MASK
                && evt.getExtendedKeyCode() == java.awt.event.KeyEvent.VK_F ) {
            tableUtils.searchNameInTable(table);
        }
    }
    //<editor-fold defaultstate="collapsed" desc="Automatic variable declarations">
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPopupMenu CCFCodePopupMenu;
    private javax.swing.JMenuItem NamedCommandMenuItem;
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JMenuItem absToleranceMenuItem;
    private javax.swing.JMenu actionsMenu;
    private javax.swing.JMenuItem addEmptyParametrizedSignalMenuItem;
    private javax.swing.JMenuItem addEmptyRawMenuItem;
    private javax.swing.JMenuItem addMissingFsMenuItem;
    private javax.swing.JMenuItem addMissingNamesMenuItem;
    private javax.swing.JMenuItem addNamePrefixMenuItem;
    private javax.swing.JMenuItem addParametrizedTestSignalMenuItem;
    private javax.swing.JMenuItem addRawTestSignalMenuItem;
    private javax.swing.JMenuItem analysisToClipboardMenuItem;
    private javax.swing.JRadioButtonMenuItem analyzerBase10RadioButtonMenuItem;
    private javax.swing.JRadioButtonMenuItem analyzerBase16RadioButtonMenuItem;
    private javax.swing.JRadioButtonMenuItem analyzerBase2RadioButtonMenuItem;
    private javax.swing.JRadioButtonMenuItem analyzerBase4RadioButtonMenuItem;
    private javax.swing.JRadioButtonMenuItem analyzerBase8RadioButtonMenuItem;
    private javax.swing.ButtonGroup analyzerBaseButtonGroup;
    private javax.swing.JMenu analyzerBasisMenu;
    private javax.swing.JTextField analyzerTextField;
    private javax.swing.JPanel audioPanel;
    private javax.swing.JCheckBox autoOpenExportsCheckBox;
    private javax.swing.JCheckBox automaticExportFilenamesCheckBox;
    private javax.swing.JMenuItem beaconListenerMenuItem;
    private javax.swing.JMenuItem captitalizeNameMenuItem;
    private javax.swing.JPanel captureIrWidgetPanel;
    private javax.swing.JButton captureTestButton;
    private javax.swing.JScrollPane capturedDataScrollPane;
    private org.harctoolbox.guicomponents.UndoableJTextArea capturedDataTextArea;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<CcfImporter> ccfFileImporterBean;
    private javax.swing.JPanel ccfImportPanel;
    private javax.swing.JRadioButtonMenuItem ccfRadioButtonMenuItem;
    private javax.swing.JMenuItem checkParametrizedSignalsMenuItem;
    private javax.swing.JMenuItem checkRawCommandsMenuItem;
    private javax.swing.JMenuItem checkUpToDateMenuItem;
    private javax.swing.JCheckBox chopIctImportCheckBox;
    private javax.swing.JMenuItem chopThresholdMenuItem;
    private javax.swing.JCheckBoxMenuItem cleanerCheckBoxMenuItem;
    private javax.swing.JMenuItem clearConsoleMenuItem;
    private javax.swing.JMenuItem clearSelectionParametricMenuItem;
    private javax.swing.JMenuItem clearSelectionRawMenuItem;
    private javax.swing.JMenuItem clearSignalMenuItem;
    private javax.swing.JMenuItem clonePlotMenuItem;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<CmlImporter> cmlFileImporterBean;
    private javax.swing.JPanel cmlImportPanel;
    private org.harctoolbox.guicomponents.CommandFusionBean commandFusionBean;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<CommandFusionImporter> commandFusionFileImporterBean;
    private javax.swing.JPanel commandFusionImportPanel;
    private javax.swing.JPanel commandFusionSendPanel;
    private org.harctoolbox.guicomponents.Console console;
    private javax.swing.JToggleButton continuousCaptureButton;
    private javax.swing.JButton controlTowerBrowseButton;
    private javax.swing.JComboBox<String> controlTowerCodeSetComboBox;
    private javax.swing.JComboBox<String> controlTowerDeviceTypeComboBox;
    private javax.swing.JButton controlTowerImportButton;
    private javax.swing.JComboBox<String> controlTowerManufacturerComboBox;
    private javax.swing.JPanel controlTowerPanel;
    private org.harctoolbox.irscrutinizer.importer.TreeImporter controlTowerTreeImporter;
    private javax.swing.JPanel cookedPanel;
    private javax.swing.JMenuItem copyConsoleToClipboardMenuItem;
    private javax.swing.JMenuItem copyDataToClipboardMenuItem;
    private org.harctoolbox.guicomponents.CopyPastePopupMenu copyPastePopupMenu;
    private org.harctoolbox.guicomponents.CopyPastePopupMenu copyPopupMenu;
    private javax.swing.JMenuItem copyToParametrizedMenuItem;
    private javax.swing.JMenuItem creatingUserMenuItem;
    private javax.swing.JPanel csvImportPanel;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<CsvParametrizedImporter> csvParametrizedFileImporterBean;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<CsvRawImporter> csvRawFileImporterBean;
    private javax.swing.JPanel csvRawImportPanel;
    private javax.swing.JComboBox<String> csvRawSeparatorComboBox;
    private javax.swing.JComboBox<String> dColumnComboBox;
    private javax.swing.JMenuItem debugCodeMenuItem;
    private javax.swing.JMenuItem debugDecodeProtocolRegexpMenuItem;
    private javax.swing.JMenu debugMenu;
    private javax.swing.JTextField decodeIRTextField;
    private javax.swing.JMenuItem deleteAllMenuItem;
    private javax.swing.JMenuItem deleteAllRawsMenuItem;
    private javax.swing.JMenuItem deleteDefaultedSignalsMenuItem;
    private javax.swing.JMenuItem deleteEmptyParametrizedSignalsMenuItem;
    private javax.swing.JMenuItem deleteEmptyRawSignalsMenuItem;
    private javax.swing.JMenuItem deleteMenuItem;
    private javax.swing.JMenuItem deleteMenuItem1;
    private javax.swing.JMenuItem deleteParametrizedCommentMenuItem;
    private javax.swing.JMenuItem deleteRawCommentMenuItem;
    private org.harctoolbox.guicomponents.DevLircBean devLircBean;
    private javax.swing.JPanel devLircPanel;
    private javax.swing.JMenuItem disussionsMenuItem;
    private javax.swing.JMenuItem downloadsMenuItem;
    private javax.swing.JMenuItem dummyGapMenuItem;
    private javax.swing.JMenuItem duplicateParametricMenuItem;
    private javax.swing.JMenuItem duplicateRawMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JTextField editingTextField;
    private javax.swing.JLabel endingLengthLabel;
    private javax.swing.JMenuItem endingTimeoutMenuItem;
    private javax.swing.JMenuItem exitMenuItem;
    private org.harctoolbox.guicomponents.AudioParametersBean exportAudioParametersBean;
    private javax.swing.JMenu exportCapturedMenu;
    private javax.swing.JMenuItem exportCharsetMenuItem;
    private javax.swing.JMenuItem exportCookedMenuItem;
    private javax.swing.JButton exportDirOpenButton;
    private javax.swing.JButton exportDirSelectButton;
    private javax.swing.JTextField exportDirectoryTextField;
    private javax.swing.JComboBox<String> exportFormatComboBox;
    private javax.swing.JButton exportFormatDocumentationButton;
    private javax.swing.JLayeredPane exportFormatParametersPane;
    private javax.swing.JMenuItem exportFormatsEditMenuItem;
    private javax.swing.JMenu exportFormatsMenu;
    private javax.swing.JMenuItem exportFormatsReloadMenuItem;
    private javax.swing.JMenuItem exportFormatsSelectMenuItem;
    private javax.swing.JCheckBox exportGenerateBroadlinkBase64CheckBox;
    private javax.swing.JCheckBox exportGenerateBroadlinkHexCheckBox;
    private javax.swing.JCheckBox exportGenerateCcfCheckBox;
    private javax.swing.JCheckBox exportGenerateParametersCheckBox;
    private javax.swing.JCheckBox exportGenerateRawCheckBox;
    private javax.swing.JCheckBox exportGenerateSendIrCheckBox;
    private javax.swing.JCheckBox exportGenerateShortCcfCheckBox;
    private javax.swing.JButton exportGirrHelpButton;
    private javax.swing.JButton exportHelpButton;
    private javax.swing.JLabel exportNumberRepeatsLabel;
    private javax.swing.JMenu exportOptionsMenu;
    private javax.swing.JPanel exportPanel;
    private javax.swing.JMenuItem exportParametricAsGirrMenuItem;
    private javax.swing.JMenuItem exportParametricAsTextMenuItem;
    private javax.swing.JButton exportParametricRemoteButton;
    private javax.swing.JButton exportProntoHelpButton;
    private javax.swing.JMenuItem exportRawAsGirrMenuItem;
    private javax.swing.JMenuItem exportRawAsTextMenuItem;
    private javax.swing.JButton exportRawRemoteButton;
    private javax.swing.JButton exportRawRemoteButton1;
    private javax.swing.JComboBox<String> exportRepeatComboBox;
    private javax.swing.JMenuItem exportSelectedCookedMenuItem;
    private javax.swing.JButton exportSignalButton;
    private javax.swing.JMenuItem exportSignalGirrMenuItem;
    private javax.swing.JMenuItem exportSignalGirrMenuItem1;
    private javax.swing.JMenuItem exportSignalIctMenuItem;
    private javax.swing.JMenuItem exportSignalIctMenuItem1;
    private javax.swing.JMenuItem exportSignalWaveMenuItem;
    private javax.swing.JMenuItem exportSignalWaveMenuItem1;
    private javax.swing.JButton exportWaveHelpButton;
    private javax.swing.JComboBox<String> fColumnComboBox;
    private javax.swing.JMenuItem fallbackFrequencyMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JLabel frequencyLabel;
    private javax.swing.JMenuItem frequencyToleranceMenuItem;
    private javax.swing.JButton generateButton;
    private javax.swing.JCheckBoxMenuItem generateCcfCheckBoxMenuItem;
    private javax.swing.JButton generateExportButton;
    private javax.swing.JButton generateHelpButton;
    private javax.swing.JPanel generatePanel;
    private javax.swing.JCheckBoxMenuItem generateRawCheckBoxMenuItem;
    private javax.swing.JTextArea generateTextArea;
    private javax.swing.JCheckBoxMenuItem girrAcceptEmptyCommandsCheckBoxMenuItem;
    private javax.swing.JPanel girrExportOptionsPanel;
    private javax.swing.JCheckBox girrFatRawCheckBox;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<GirrImporter> girrFileImporterBean;
    private javax.swing.JPanel girrImportPanel;
    private javax.swing.JMenuItem girrSchemaLocationMenuItem;
    private javax.swing.JCheckBoxMenuItem girrValidateCheckBoxMenuItem;
    private javax.swing.JButton girrWebSiteButton;
    private javax.swing.JPanel girsClientPanel;
    private org.harctoolbox.guicomponents.GirsClientBean girsTcpSerialComboBean;
    private javax.swing.JMenuItem gitMenuItem;
    private org.harctoolbox.guicomponents.GlobalCacheBean globalCacheIrSenderSelector;
    private javax.swing.JPanel globalCachePanel;
    private javax.swing.JMenuItem globalCacheTimeoutMenuItem;
    private javax.swing.JMenuItem glossaryMenuItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem hexCalcMenuItem;
    private javax.swing.JMenuItem hideColumnMenuItem;
    private javax.swing.JMenuItem hideColumnMenuItem1;
    private javax.swing.JMenuItem hideUninterestingColumnsMenuItem;
    private javax.swing.JMenuItem hideUninterestingColumnsMenuItem1;
    private javax.swing.JMenuItem hideUnusedMenuItem;
    private javax.swing.JMenuItem homePageMenuItem;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<IctImporter> ictFileImporterBean;
    private javax.swing.JPanel ictImportPanel;
    private javax.swing.JCheckBoxMenuItem ignoreEndingSilenceCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem ignoreLeadingGarbageCheckBoxMenuItem;
    private javax.swing.JMenu importCaptureMenu;
    private javax.swing.JMenuItem importCcfMenuItem;
    private javax.swing.JMenuItem importCcfMenuItem1;
    private javax.swing.JMenuItem importCharsetMenuItem;
    private javax.swing.JButton importCmlHelpButton;
    private javax.swing.JMenuItem importCmlMenuItem;
    private javax.swing.JMenuItem importCmlMenuItem2;
    private javax.swing.JButton importCommandFusionHelpButton;
    private javax.swing.JMenuItem importCommandFusionMenuItem;
    private javax.swing.JMenuItem importCommandFusionMenuItem2;
    private javax.swing.JButton importControlTowerHelpButton;
    private javax.swing.JMenuItem importGirrMenuItem;
    private javax.swing.JMenuItem importGirrMenuItem1;
    private javax.swing.JButton importGirrSignalHelpButton;
    private javax.swing.JButton importIctHelpButton;
    private javax.swing.JMenuItem importIctMenuItem;
    private javax.swing.JMenuItem importIctMenuItem1;
    private javax.swing.JButton importIrTransHelpButton;
    private javax.swing.JButton importLircHelpButton;
    private javax.swing.JMenuItem importLircMenuItem;
    private javax.swing.JMenuItem importLircMenuItem1;
    private javax.swing.JButton importMode2HelpButton;
    private javax.swing.JMenu importOptionsMenu;
    private javax.swing.JPanel importPanel;
    private javax.swing.JMenu importParametricMenu;
    private javax.swing.JButton importProntoClassicHelpButton;
    private javax.swing.JButton importProntoProfessionalHelpButton;
    private javax.swing.JMenu importRawMenu;
    private javax.swing.JMenuItem importSignalAsGirrMenuItem;
    private javax.swing.JMenuItem importSignalAsGirrMenuItem1;
    private javax.swing.JMenuItem importSignalAsIctMenuItem;
    private javax.swing.JMenuItem importSignalAsIctMenuItem1;
    private javax.swing.JMenuItem importSignalAsMode2MenuItem;
    private javax.swing.JMenuItem importSignalAsMode2MenuItem1;
    private javax.swing.JMenuItem importSignalAsWaveMenuItem;
    private javax.swing.JMenuItem importSignalAsWaveMenuItem1;
    private javax.swing.JMenu importSignalMenu;
    private javax.swing.JTabbedPane importTabbedPane;
    private javax.swing.JButton importTextParametrizedHelpButton;
    private javax.swing.JButton importTextRawHelpButton;
    private javax.swing.JButton importTextRawLineBasedHelpButton;
    private javax.swing.JCheckBox importWaveDivideCarrierCheckBox1;
    private javax.swing.JButton importWaveHelpButton;
    private javax.swing.JMenuItem importXcfMenuItem;
    private javax.swing.JMenuItem importXcfMenuItem1;
    private javax.swing.JCheckBox includeSubsequenctColumnsCheckBox1;
    private javax.swing.JPanel infoPanel;
    private javax.swing.JCheckBoxMenuItem inquiryDeviceDataCheckBoxMenuItem;
    private javax.swing.JLabel introLengthLabel;
    private javax.swing.JCheckBoxMenuItem invokeAnalyzerCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem invokeDecoderCheckBoxMenuItem;
    private org.harctoolbox.guicomponents.IrPlotter irPlotter;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<IrTransImporter> irTransFileImporterBean;
    private javax.swing.JButton irTransWebButton;
    private org.harctoolbox.guicomponents.IrWidgetBean irWidgetBean;
    private javax.swing.JButton irWidgetHelpButton;
    private org.harctoolbox.guicomponents.IrpRenderBean irpMasterBean;
    private javax.swing.JMenuItem irpProtocolsEditMenuItem;
    private javax.swing.JMenu irpProtocolsIniMenu;
    private javax.swing.JMenuItem irpProtocolsReloadMenuItem;
    private javax.swing.JMenuItem irpProtocolsSelectMenuItem;
    private javax.swing.JMenuItem irpTransmogrifierHelpMenuItem;
    private javax.swing.JPanel irtransImportPanel;
    private javax.swing.JMenuItem issuesMenuItem;
    private javax.swing.JButton jButton20;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenuItem jMenuItem12;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator10;
    private javax.swing.JPopupMenu.Separator jSeparator11;
    private javax.swing.JPopupMenu.Separator jSeparator12;
    private javax.swing.JPopupMenu.Separator jSeparator13;
    private javax.swing.JPopupMenu.Separator jSeparator14;
    private javax.swing.JPopupMenu.Separator jSeparator15;
    private javax.swing.JPopupMenu.Separator jSeparator16;
    private javax.swing.JPopupMenu.Separator jSeparator17;
    private javax.swing.JPopupMenu.Separator jSeparator18;
    private javax.swing.JPopupMenu.Separator jSeparator19;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator20;
    private javax.swing.JPopupMenu.Separator jSeparator21;
    private javax.swing.JPopupMenu.Separator jSeparator22;
    private javax.swing.JPopupMenu.Separator jSeparator23;
    private javax.swing.JPopupMenu.Separator jSeparator24;
    private javax.swing.JPopupMenu.Separator jSeparator25;
    private javax.swing.JPopupMenu.Separator jSeparator26;
    private javax.swing.JPopupMenu.Separator jSeparator27;
    private javax.swing.JPopupMenu.Separator jSeparator28;
    private javax.swing.JPopupMenu.Separator jSeparator29;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator30;
    private javax.swing.JPopupMenu.Separator jSeparator31;
    private javax.swing.JPopupMenu.Separator jSeparator32;
    private javax.swing.JPopupMenu.Separator jSeparator33;
    private javax.swing.JPopupMenu.Separator jSeparator34;
    private javax.swing.JPopupMenu.Separator jSeparator35;
    private javax.swing.JPopupMenu.Separator jSeparator36;
    private javax.swing.JPopupMenu.Separator jSeparator37;
    private javax.swing.JPopupMenu.Separator jSeparator38;
    private javax.swing.JPopupMenu.Separator jSeparator39;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator40;
    private javax.swing.JPopupMenu.Separator jSeparator41;
    private javax.swing.JPopupMenu.Separator jSeparator42;
    private javax.swing.JPopupMenu.Separator jSeparator43;
    private javax.swing.JPopupMenu.Separator jSeparator44;
    private javax.swing.JPopupMenu.Separator jSeparator45;
    private javax.swing.JPopupMenu.Separator jSeparator46;
    private javax.swing.JPopupMenu.Separator jSeparator47;
    private javax.swing.JPopupMenu.Separator jSeparator48;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JPopupMenu.Separator jSeparator8;
    private javax.swing.JPopupMenu.Separator jSeparator9;
    private javax.swing.JMenuItem jumpToLastPanelMenuItem;
    private javax.swing.JMenu lafMenu;
    private javax.swing.JMenuItem lengthMenuItem;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<LircImporter> lircFileImporterBean;
    private javax.swing.JPanel lircImportPanel;
    private javax.swing.JMenu loadMenu;
    private javax.swing.JMenuItem lowercaseNameMenuItem;
    private javax.swing.JMenuItem mainDocuMenuItem;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem minLeadoutMenuItem;
    private javax.swing.JMenuItem minRepeatGapMenuItem;
    private javax.swing.JComboBox<String> miscParametersColumnComboBox;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<Mode2Importer> mode2FileImporterBean;
    private javax.swing.JPanel mode2ImportPanel;
    private javax.swing.JMenuItem moveDownMenuItem;
    private javax.swing.JMenuItem moveDownMenuItem1;
    private javax.swing.JMenuItem moveUpMenuItem;
    private javax.swing.JMenuItem moveUpMenuItem1;
    private javax.swing.JLabel noRepsLabel;
    private javax.swing.JComboBox<String> noTransmitsComboBox;
    private javax.swing.JCheckBoxMenuItem offerStackTraceCheckBoxMenuItem;
    private javax.swing.JMenuItem openLastExportFileMenuItem;
    private javax.swing.JButton openLastFileButton;
    private javax.swing.JButton openLastFileButton1;
    private javax.swing.JCheckBoxMenuItem openZipFilesCheckBoxMenuItem;
    private javax.swing.JMenu optionsMenu;
    private javax.swing.JMenu outputFormatMenu;
    private javax.swing.JTable parameterTable;
    private javax.swing.JPopupMenu parameterTablePopupMenu;
    private javax.swing.JScrollPane parameterTableScrollPane;
    private javax.swing.JMenuItem parameterUniquefyMenuItem;
    private javax.swing.JButton parametricOrRawExportButton;
    private javax.swing.JCheckBoxMenuItem parametricSorterCheckBoxMenuItem;
    private javax.swing.JMenu parametrizedAdvancedMenu;
    private javax.swing.JComboBox<String> parametrizedBaseComboBox;
    private javax.swing.JMenuItem parametrizedCopyAllMenuItem;
    private javax.swing.JMenuItem parametrizedCopySelectionMenuItem;
    private javax.swing.JPanel parametrizedCsvImportPanel;
    private javax.swing.JComboBox<String> parametrizedCsvSeparatorComboBox;
    private javax.swing.JCheckBoxMenuItem parametrizedLearnIgnoreTCheckBoxMenuItem;
    private javax.swing.JCheckBox parametrizedMultiColumnNameCheckBox;
    private javax.swing.JComboBox<String> parametrizedNameColumnComboBox;
    private javax.swing.JMenuItem parametrizedPasteColumnMenuItem;
    private javax.swing.JTabbedPane parametrizedRawTabbedPane;
    private javax.swing.JButton pasteAnalyzeButton;
    private javax.swing.JMenuItem pasteScrutinizeToDataWindowMenuItem;
    private javax.swing.JMenuItem pasteToDataWindowMenuItem;
    private javax.swing.JScrollPane plotScrollPane;
    private javax.swing.JMenuItem plotterResetMenuItem;
    private javax.swing.JCheckBoxMenuItem printAlternativeDecodesCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem printAnalyzeIRPsToConsoleCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem printDecodesToConsoleCheckBoxMenuItem;
    private javax.swing.JMenuItem printRawTableRowMenuItem;
    private javax.swing.JMenuItem printTableRowMenuItem;
    private javax.swing.JPanel prontoClassicExportOptionsPanel;
    private javax.swing.JTextField prontoExportButtonHeightTextField;
    private javax.swing.JTextField prontoExportButtonWidthTextField;
    private javax.swing.JTextField prontoExportScreenHeightTextField;
    private javax.swing.JTextField prontoExportScreenWidthTextField;
    private javax.swing.JComboBox<String> prontoModelComboBox;
    private javax.swing.JComboBox<String> protocolColumnComboBox;
    private javax.swing.JMenuItem protocolDocuMenuItem;
    private javax.swing.JMenu protocolParametersMenu;
    private javax.swing.JMenuItem protocolSpecMenuItem;
    private javax.swing.JMenuItem publicKeyMenuItem;
    private javax.swing.JMenuItem rawAddTestSignalMenuItem;
    private javax.swing.JMenuItem rawChopMenuItem;
    private javax.swing.JMenuItem rawCodeAnalyzeMenuItem;
    private javax.swing.JMenuItem rawCodeClearMenuItem;
    private javax.swing.JComboBox<String> rawCodeColumnComboBox1;
    private javax.swing.JMenuItem rawCodeCopyAllMenuItem;
    private javax.swing.JMenuItem rawCodeCopyMenuItem;
    private javax.swing.JMenuItem rawCodePasteAnalyzeMenuItem;
    private javax.swing.JMenuItem rawCodePasteMenuItem;
    private javax.swing.JMenuItem rawCodePasteReplacingMenuItem;
    private javax.swing.JMenuItem rawCodeSaveMenuItem;
    private javax.swing.JMenuItem rawCodeSelectAllMenuItem;
    private javax.swing.JTabbedPane rawCookedTabbedPane;
    private javax.swing.JMenuItem rawCopyAllMenuItem;
    private javax.swing.JMenuItem rawCopySelectionMenuItem;
    private javax.swing.JMenuItem rawFromClipboardMenuItem;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<RawLineImporter> rawLineCsvFileImporterBean;
    private javax.swing.JPanel rawLineCsvImportPanel;
    private javax.swing.JCheckBox rawMultiColumnNameCheckBox;
    private javax.swing.JComboBox<String> rawNameColumnComboBox;
    private javax.swing.JPanel rawPanel;
    private javax.swing.JMenuItem rawPasteColumnMenuItem;
    private javax.swing.JMenuItem rawProtocolDocuMenuItem;
    private javax.swing.JRadioButtonMenuItem rawRadioButtonMenuItem;
    private javax.swing.JCheckBoxMenuItem rawSorterCheckBoxMenuItem;
    private javax.swing.JTable rawTable;
    private javax.swing.JPopupMenu rawTablePopupMenu;
    private javax.swing.JScrollPane rawTableScrollPane;
    private javax.swing.JMenuItem rawUniquefyMenuItem;
    private javax.swing.JRadioButtonMenuItem rawWithoutSignsRadioButtonMenuItem;
    private javax.swing.JMenuItem reAnalyzeMenuItem;
    private javax.swing.JMenuItem relToleranceMenuItem;
    private javax.swing.JMenuItem releaseNotesMenuItem;
    private javax.swing.JButton remoteLocatorBrowseButton;
    private javax.swing.JButton remoteLocatorBrowseRemoteButton;
    private javax.swing.JComboBox<String> remoteLocatorDeviceTypeComboBox;
    private javax.swing.JButton remoteLocatorHelpButton;
    private javax.swing.JButton remoteLocatorImportButton;
    private javax.swing.JTextField remoteLocatorImporterKindTextField;
    private javax.swing.JComboBox<String> remoteLocatorManufacturerComboBox;
    private javax.swing.JPanel remoteLocatorPanel;
    private javax.swing.JComboBox<String> remoteLocatorRemoteComboBox;
    private org.harctoolbox.irscrutinizer.importer.TreeImporter remoteLocatorTreeImporter;
    private javax.swing.JPanel remoteScrutinizerPanel;
    private javax.swing.JCheckBoxMenuItem removeDefaultedParametersCheckBoxMenuItem;
    private javax.swing.JMenuItem removeUnusedMenuItem1;
    private javax.swing.JLabel repLengthLabel;
    private javax.swing.JCheckBoxMenuItem repeatFinderCheckBoxMenuItem;
    private javax.swing.JMenuItem resetPropertiesMenuItem;
    private javax.swing.JMenuItem resetRawTableColumnsMenuItem;
    private javax.swing.JMenuItem resetRawTableColumnsMenuItem1;
    private javax.swing.JComboBox<String> sColumnComboBox;
    private javax.swing.JMenu saveCapturedMenu;
    private javax.swing.JMenuItem saveConsoleTextAsMenuItem;
    private javax.swing.JMenuItem saveDataTextAsMenuItem;
    private javax.swing.JMenu saveMenu;
    private javax.swing.JMenu saveParametrizedMenu;
    private javax.swing.JMenuItem savePropertiesAsMenuItem;
    private javax.swing.JMenuItem savePropertiesMenuItem;
    private javax.swing.JMenu saveRawMenu;
    private javax.swing.JMenuItem saveRawMenuItem;
    private javax.swing.JMenuItem saveSelectedRawTableRowMenuItem;
    private javax.swing.JMenuItem scrutinizeMenuItem;
    private javax.swing.JMenuItem scrutinizeParametricMenuItem;
    private javax.swing.JButton scrutinizeRemoteHelpButton;
    private javax.swing.JButton scrutinizeSignalHelpButton;
    private javax.swing.JMenuItem scrutinizeSignalProtocolDocuMenuItem;
    private javax.swing.JMenuItem searchParametrizedMenuItem;
    private javax.swing.JMenuItem searchRawMenuItem;
    private javax.swing.JMenuItem secondaryExportFormatsEditMenuItem;
    private javax.swing.JMenuItem secondaryExportFormatsSelectMenuItem;
    private javax.swing.JMenuItem secondaryIrpProtocolsEditMenuItem;
    private javax.swing.JMenuItem secondaryIrpProtocolsSelectMenuItem;
    private javax.swing.JMenuItem sendMenuItem;
    private javax.swing.JButton sendingCommandFusionHelpButton;
    private javax.swing.JButton sendingDevLircHardwareHelpButton;
    private javax.swing.JButton sendingGirsClientHelpButton;
    private javax.swing.JButton sendingGlobalCacheHelpButton;
    private javax.swing.JButton sendingHardwareHelpButton;
    private javax.swing.JTabbedPane sendingHardwareTabbedPane;
    private javax.swing.JPanel sendingPanel;
    private javax.swing.JMenuItem sendingTimeoutMenuItem;
    private javax.swing.JMenuItem setDMenuItem;
    private javax.swing.JMenuItem setFMenuItem;
    private javax.swing.JMenuItem setMiscParamsMenuItem;
    private javax.swing.JMenuItem setProtocolMenuItem;
    private javax.swing.JMenuItem setSMenuItem;
    private javax.swing.JMenuItem setTMenuItem;
    private javax.swing.JMenuItem signalClearMenuItem;
    private javax.swing.JButton signalExportButton;
    private javax.swing.JPanel signalScrutinizerPanel;
    private javax.swing.JMenuItem signalSignalTextMenuItem;
    private javax.swing.JMenuItem signalSignalTextMenuItem1;
    private javax.swing.JButton startButton;
    private javax.swing.JMenuItem startCaptureMenuItem;
    private javax.swing.JToggleButton startStopToggleButton;
    private javax.swing.JMenuItem startTimeoutMenuItem;
    private javax.swing.JPanel subformatsPanel;
    private javax.swing.JMenuItem testSignalMenuItem;
    private javax.swing.JMenuItem timeFrequencyCalcMenuItem;
    private javax.swing.JMenu timeoutMenu;
    private javax.swing.JButton toScrutinizeButton;
    private javax.swing.JMenu toolsMenu;
    private javax.swing.JSplitPane topLevelSplitPane;
    private javax.swing.JTabbedPane topLevelTabbedPane;
    private javax.swing.JButton transferToParametricRemoteButton;
    private javax.swing.JButton transferToRawRemoteButton;
    private javax.swing.JMenuItem transformNameMenuItem;
    private javax.swing.JMenu transformNamesMenu;
    private javax.swing.JCheckBoxMenuItem translateProntoFontCheckBoxMenuItem;
    private org.harctoolbox.guicomponents.AudioTransmitterBean transmitAudioBean;
    private javax.swing.JButton transmitAudioHelpButton;
    private javax.swing.JButton transmitGenerateButton;
    private javax.swing.JButton transmitGenerateButton2;
    private javax.swing.JMenuItem transmitMenuItem;
    private javax.swing.JButton transmitScrutinizedButton;
    private javax.swing.JButton transmitSignalButton;
    private javax.swing.JButton transmitSignalButton1;
    private javax.swing.JMenuItem tutorialMenuItem;
    private javax.swing.JMenuItem undoDataMenuItem;
    private javax.swing.JMenuItem undoMenuItem;
    private javax.swing.JMenuItem unsetTMenuItem;
    private javax.swing.JMenuItem upperCaseMenuItem;
    private javax.swing.JCheckBoxMenuItem usePopupsForErrorsCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem usePopupsForHelpCheckBoxMenuItem;
    private javax.swing.JMenu usePopupsMenu;
    private javax.swing.JCheckBoxMenuItem verboseCheckBoxMenuItem;
    private javax.swing.JPanel waveExportOptionsPanel;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<WaveImporter> waveFileImporterBean;
    private javax.swing.JPanel waveImportPanel;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<XcfImporter> xcfFileImporterBean;
    private javax.swing.JPanel xcfImportPanel;
    // End of variables declaration//GEN-END:variables
    //</editor-fold>
}
