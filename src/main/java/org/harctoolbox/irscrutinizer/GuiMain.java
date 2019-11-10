/*
Copyright (C) 2013, 2014, 2015, 2017, 2018, 2019 Bengt Martensson.

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
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.guicomponents.*;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.TimeoutException;
import org.harctoolbox.harchardware.ir.*;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.Pronto;
import org.harctoolbox.ircore.ThisCannotHappenException;
import org.harctoolbox.irp.BitDirection;
import org.harctoolbox.irp.Decoder;
import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.irp.IrpParseException;
import org.harctoolbox.irp.IrpUtils;
import org.harctoolbox.irp.Protocol;
import org.harctoolbox.irp.UnknownProtocolException;
import org.harctoolbox.irscrutinizer.capturinghardware.*;
import org.harctoolbox.irscrutinizer.exporter.*;
import org.harctoolbox.irscrutinizer.importer.*;
import org.harctoolbox.irscrutinizer.sendinghardware.*;
import org.xml.sax.SAXException;

public final class GuiMain extends javax.swing.JFrame {

    private Props properties;
    private final transient LookAndFeelManager lookAndFeelManager;
    private GuiUtils guiUtils;
    private transient GlobalCacheIrDatabase globalCacheIrDatabase = null;
    private transient ControlTowerIrDatabase controlTowerIrDatabase = null;
    private Map<String, String> controlTowerCodesetTable = null;
    private transient IrdbImporter irdbImporter = null;
    private transient IrpDatabase irpDatabase = null;
    private transient Decoder decoder = null;
    private Decoder.DecoderParameters decoderParameters = null;
    private ProtocolsIni protocolsIni = null;
    private transient CcfImporter ccfImporter;
    private transient XcfImporter xcfImporter;
    private CmlImporter cmlImporter;
    private CommandFusionImporter commandFusionImporter;
    private CsvRawImporter csvRawImporter;
    private CsvParametrizedImporter csvParametrizedImporter;
    private RmduImporter rmduImporter;
    private IctImporter ictImporter;
    private transient GirrImporter girrImporter;
    private final LircImporter lircImporter;
    private final transient IrTransImporter irTransImporter;
    private transient WaveImporter waveImporter;
    private final String applicationHome;
    private java.awt.Component lastPane;
    private int dynamicExportFormatsMenuPosition;
    private boolean initialized = false;
    private boolean stdinHasBeenClosed = false;
    private final transient TableUtils tableUtils;
    private Proxy proxy = Proxy.NO_PROXY;

    private final String testSignalCcf = // NEC1 12.34 56
            "0000 006C 0022 0002 015B 00AD 0016 0016 0016 0016 0016 0041 0016 0041 "
            + "0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 "
            + "0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 "
            + "0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0041 "
            + "0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 "
            + "0016 06A4 015B 0057 0016 0E6C";

    private final RawIrSignal.RawTableColumnModel rawTableColumnModel;
    private final ParametrizedIrSignal.ParameterIrSignalTableColumnModel parameterTableColumnModel;
    private final RawIrSignal.RawTableModel rawTableModel;
    private final ParametrizedIrSignal.ParameterIrSignalTableModel parameterTableModel;
    private transient CaptureThread captureThread = null;
    private final String[] prontoModelNames;
    private ExportFormatManager exportFormatManager;
    private transient SendingHardwareManager sendingHardwareManager = null;
    private transient SendingLircClient sendingLircClient;
    private transient CapturingHardwareManager capturingHardwareManager;

    private transient Remote.MetaData metaData = new Remote.MetaData("unnamed");

    private final static String feedbackMail = "feedback@harctoolbox.org";
    private final static String issuesUrl = "https://github.com/bengtmartensson/harctoolboxbundle/issues";
    private final static String gitUrl = "https://github.com/bengtmartensson/harctoolboxbundle/";
    private final static String downloadsUrl = "https://github.com/bengtmartensson/harctoolboxbundle/releases";
    private final static int importSequenceAskThreshold = 3;
    private final static int maxCharsInGuiMessages = 150;
    private final static int transmitSignalMouseButton = 2;

    private AboutPopup aboutBox;
    private Component currentPane = null;
    private boolean ignoreLeadingGarbage = false;

    private Proxy mkProxy() {
        return mkProxy(properties.getProxyHostName(), properties.getProxyPort());
    }

    private static Proxy mkProxy(String hostName, int port) {
        return hostName == null || hostName.isEmpty()
                ? Proxy.NO_PROXY
                : new Proxy(Proxy.Type.HTTP, new InetSocketAddress(hostName, port));
    }

    private void setupProxy() {
        proxy = mkProxy();
        IrdbImporter.setProxy(proxy);
        ControlTowerIrDatabase.setProxy(proxy);
        GlobalCacheIrDatabase.setProxy(proxy);
        ReaderImporter.setProxy(proxy);
    }

    // Preferences for the analyzer. Possibly these should be made properties?
    private boolean analyzerEliminateConstantVars = false;
    private double analyzerMaxRoundingError = Burst.Preferences.DEFAULT_MAX_ROUNDING_ERROR;
    private double analyzerMaxUnits = Burst.Preferences.DEFAULT_MAX_UNITS;
    private double analyzerMaxMicroSeconds = Burst.Preferences.DEFAULT_MAX_MICROSECONDS;
    private String analyzerTimeBase = null;
    private boolean analyzerLsb = false;
    private boolean analyzerExtent = false;
    private List<Integer> analyzerParameterWidths = new ArrayList<>(0);
    private int analyzerMaxParameterWidth = 64;
    private boolean analyzerInvert = false;

    // ... and some more preferences for the decoding, that should probably be
    // properties too. (See IrpTransmogrifier for the semantics.)
    private boolean decodeStrict = false;
    private boolean decodeAllDecodes = false;
    private boolean decodeRemoveDefaultedParameters = true;
    private boolean decodeRecursive = false;
    private boolean decodeOverride = false;

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

    /**
     * Main class for the GUI. Throws exceptions if configuration files cannot be found or on similar errors.
     * It may of course be questioned if this is necessary, however, it is faster for the user to
     * install the files than for me to write code for semi-working in semi-broken situations.
     *
     * @param applicationHome
     * @param propsfilename Name of properties file. Null for system default.
     * @param verbose Verbose execution of some commands, dependent on invoked programs.
     * @param userlevel Presently not used.
     * @param arguments
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws java.text.ParseException
     * @throws org.harctoolbox.irp.IrpParseException
     */
    public GuiMain(String applicationHome, String propsfilename, boolean verbose,
            int userlevel, List<String> arguments) throws IOException, java.text.ParseException, ParserConfigurationException, SAXException, IrpParseException {
        this.applicationHome = applicationHome;
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

        properties = new Props(propsfilename, this.applicationHome);
        if (verbose)
            properties.setVerbose(true);
        Importer.setProperties(properties);

        parameterTableModel = new ParametrizedIrSignal.ParameterIrSignalTableModel();
        rawTableModel = new RawIrSignal.RawTableModel();
        parameterTableColumnModel = new ParametrizedIrSignal.ParameterIrSignalTableColumnModel();
        rawTableColumnModel = new RawIrSignal.RawTableColumnModel();

        guiUtils = new GuiUtils(this, Version.appName, maxCharsInGuiMessages);
        guiUtils.setUsePopupsForErrors(false); // during initialization, popup errors might not work
        guiUtils.setUsePopupsForHelp(properties.getUsePopupsForHelp());
        guiUtils.setOfferStackTrace(properties.getOfferStackTrace());
        guiUtils.setVerbose(properties.getVerbose());
        tableUtils = new TableUtils(guiUtils);

        ProntoModel[] prontomodels = ProntoModel.getModels();
        prontoModelNames = new String[prontomodels.length];
        for (int i = 0; i < prontomodels.length; i++)
            prontoModelNames[i] = prontomodels[i].toString();

        lircImporter = new LircImporter();
        lircImporter.setRejectLircCode(properties.getRejectLircCodeImports());
        properties.addRejectLircCodeImportsChangeListener((String name1, Object oldValue, Object newValue) -> {
            lircImporter.setRejectLircCode((Boolean) newValue);
        });

        irTransImporter = new IrTransImporter();
        cmlImporter = new CmlImporter();
        commandFusionImporter = new CommandFusionImporter();
        ccfImporter = new CcfImporter();
        ccfImporter.setTranslateProntoFont(properties.getTranslateProntoFont());
        xcfImporter = new XcfImporter();
        xcfImporter.setTranslateProntoFont(properties.getTranslateProntoFont());
        properties.addTranslateProntoFontChangeListener((String name1, Object oldValue, Object newValue) -> {
            ccfImporter.setTranslateProntoFont((Boolean) newValue);
            xcfImporter.setTranslateProntoFont((Boolean) newValue);
        });

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

        csvParametrizedImporter = new CsvParametrizedImporter(
                CsvImporter.getSeparator(properties.getParametricSeparatorIndex()),
                properties.getParametricNameColumn(),
                properties.getParametrizedNameMultiColumn(),
                properties.getVerbose(),
                numberbaseIndex2numberbase(properties.getParametricNumberBaseIndex()),
                properties.getFColumn(),
                properties.getDColumn(),
                properties.getSColumn(),
                properties.getProtocolColumn());

        properties.addParametricSeparatorIndexChangeListener((String name1, Object oldValue, Object newValue) -> {
            csvParametrizedImporter.setSeparatorIndex((Integer) newValue);
        });

        properties.addParametricNameColumnChangeListener((String name1, Object oldValue, Object newValue) -> {
            csvParametrizedImporter.setNameColumn((Integer) newValue);
        });

        properties.addParametricNumberBaseIndexChangeListener((String name1, Object oldValue, Object newValue) -> {
            csvParametrizedImporter.setNumberBase(numberbaseIndex2numberbase((Integer) newValue));
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

        ictImporter = new IctImporter();

        girrImporter = new GirrImporter(properties.getGirrValidate(), new URL(properties.getGirrSchemaLocation()));
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

        waveImporter = new WaveImporter(properties.getImportWaveDivideCarrier());
        properties.addImportWaveDivideCarrierChangeListener((String name1, Object oldValue, Object newValue) -> {
            waveImporter.setDivideCarrier((Boolean) newValue);
        });

        RepeatFinder.setDefaultAbsoluteTolerance(properties.getAbsoluteTolerance());
        RepeatFinder.setDefaultRelativeTolerance(properties.getRelativeTolerance());

        irpDatabase = new IrpDatabase(properties.mkPathAbsolute(properties.getIrpProtocolsPath())); // must come before initComponents
        decoder = new Decoder(irpDatabase);
        decoderParameters = new Decoder.DecoderParameters(decodeStrict,
                decodeAllDecodes,
                decodeRemoveDefaultedParameters,
                decodeRecursive,
                properties.getFrequencyTolerance(),
                properties.getAbsoluteTolerance(),
                properties.getRelativeTolerance(),
                properties.getMinLeadOut(),
                decodeOverride,
                ignoreLeadingGarbage
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

        loadExportFormats(); // must come before initComponents

        loadProtocolsIni();

        initComponents();

        currentPane = topLevelTabbedPane.getSelectedComponent();

        cookedPanel.setTransferHandler(new GirrImporterBeanTransferHandler(false));
        rawPanel.setTransferHandler(new GirrImporterBeanTransferHandler(true));

        capturedDataTextArea.setTransferHandler(new SignalScrutinizerTransferHandler(capturedDataTextArea.getTransferHandler()));

        // Cannot do this in initComponents, since then it will be called therein
        importTabbedPane.addChangeListener((javax.swing.event.ChangeEvent evt) -> {
            int index  = importTabbedPane.getSelectedIndex();
            properties.setImportPaneSelectedIndex(index);
        });

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

        enableSorter(rawTable, properties.getSorterOnRawTable());
        enableSorter(parameterTable, properties.getSorterOnParametrizedTable());

        setupProxy();

        dynamicExportFormatsMenuPosition = optionsMenu.getItemCount();
        optionsMenu.add(this.exportFormatManager.getMenu(properties.getExportFormatName()));
        optionsMenu.add(new Separator());

        Command.setIrpMaster(irpDatabase);
        ParametrizedIrSignal.setDecoder(decoder);
        ParametrizedIrSignal.setGenerateCcf(properties.getGenerateCcf());
        ParametrizedIrSignal.setGenerateRaw(properties.getGenerateRaw());

        properties.addGenerateCcfChangeListener((String name1, Object oldValue, Object newValue) -> {
            ParametrizedIrSignal.setGenerateCcf((Boolean) newValue);
        });
        properties.addGenerateRawChangeListener((String name1, Object oldValue, Object newValue) -> {
            ParametrizedIrSignal.setGenerateRaw((Boolean) newValue);
        });

        RawIrSignal.setDecoder(decoder);
        RawIrSignal.setInvokeAnalyzer(properties.getInvokeAnalyzer());
        RawIrSignal.setInvokeDecoder(properties.getInvokeDecoder());
        RawIrSignal.setAbsoluteTolerance(properties.getAbsoluteTolerance());
        RawIrSignal.setRelativeTolerance(properties.getRelativeTolerance());
        RepeatFinder.setDefaultAbsoluteTolerance(properties.getAbsoluteTolerance());
        RepeatFinder.setDefaultRelativeTolerance(properties.getRelativeTolerance());

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

        sendingHardwareManager = new SendingHardwareManager(guiUtils, properties, sendingHardwareTabbedPane);
        properties.addVerboseChangeListener((String name1, Object oldValue, Object newValue) -> {
            sendingHardwareManager.setVerbose((Boolean)newValue);
            guiUtils.setVerbose((Boolean)newValue);
        });

        SendingGlobalCache sendingGlobalCache = new SendingGlobalCache(globalCachePanel, properties,
                guiUtils, globalCacheIrSenderSelector);
        sendingHardwareManager.add(sendingGlobalCache);

        sendingLircClient = new SendingLircClient(lircPanel, properties,
                guiUtils, lircInternetHostPanel, lircNamedCommandLauncher);
        sendingHardwareManager.add(sendingLircClient);

        SendingIrTrans sendingIrTrans = new SendingIrTrans(irTransPanel, properties,
                guiUtils, irTransInternetHostPanel, irTransNamedCommandLauncher);
        sendingHardwareManager.add(sendingIrTrans);

        SendingIrAudioPort sendingIrAudioPort = new SendingIrAudioPort(audioPanel,
                transmitAudioParametersBean, properties, guiUtils);
        sendingHardwareManager.add(sendingIrAudioPort);

        SendingSerial<IrToy> sendingIrToy = new SendingSerial<>(IrToy.class, irToyPanel, //irToyVersionLabel,
                irToySerialPortBean, properties, guiUtils);
        sendingHardwareManager.add(sendingIrToy);

        SendingDevLirc sendingDevLirc = null;
        if (LircHardware.isLibraryLoaded()) {
            sendingDevLirc = new SendingDevLirc(devLircPanel, devLircBean, properties, guiUtils);
            sendingHardwareManager.add(sendingDevLirc);
        }

        SendingGirsClient sendingGirsClient = new SendingGirsClient(girsClientPanel,
                girsTcpSerialComboBean, properties, guiUtils);
        sendingHardwareManager.add(sendingGirsClient);

        SendingSerial<CommandFusion> sendingcommandFusion = new SendingSerial<>(CommandFusion.class, commandFusionSendPanel,
                commandFusionSendingSerialPortBean, properties, guiUtils);
        sendingHardwareManager.add(sendingcommandFusion);

        SendingGenericSerialPort sendingGenericSerialPort = new SendingGenericSerialPort(
                genericSerialPanel,
                genericSerialSenderBean,
                properties,
                guiUtils);
        if (userlevel != 0)
            sendingHardwareManager.add(sendingGenericSerialPort);

        try {
            sendingHardwareManager.select(properties.getTransmitHardware());
        } catch (HarcHardwareException ex) {
            guiUtils.error(ex);
        }

        optionsMenu.add(sendingHardwareManager.getMenu());

        capturingHardwareManager = new CapturingHardwareManager(guiUtils, properties,
                capturingHardwareTabbedPane/*, startButton*/);

        capturingHardwareManager.add(new CapturingSerial<>(IrWidget.class, captureIrWidgetPanel,
                irWidgetSerialPortSimpleBean, properties, guiUtils, capturingHardwareManager));

        capturingHardwareManager.add(new CapturingGlobalCache(properties.getGlobalCacheCaptureIpName(),
                globalCacheCaptureSelector, captureGlobalCachePanel,
                properties, guiUtils, capturingHardwareManager));

        capturingHardwareManager.add(new CapturingLircMode2(properties.getLircMode2UseStdin(), properties.getLircMode2Command(),
                captureLircMode2Panel, properties, guiUtils, capturingHardwareManager));

        capturingHardwareManager.add(new CapturingSendingHardware<GirsClient>(captureGirsPanel, girsClientPanel,
                girsClientCapturingSendingBean, girsTcpSerialComboBean, sendingGirsClient,
                properties, guiUtils, capturingHardwareManager));

        if (LircHardware.isLibraryLoaded())
            capturingHardwareManager.add(new CapturingSendingHardware<>(captureDevLircPanel, devLircPanel,
                    devLircCapturingSendingBean, devLircBean, sendingDevLirc,
                    properties, guiUtils, capturingHardwareManager));

        capturingHardwareManager.add(new CapturingSendingHardware<>(captureCommandFusionPanel, commandFusionSendPanel,
                commandFusionCapturingSendingBean, commandFusionSendingSerialPortBean, sendingcommandFusion,
                properties, guiUtils, capturingHardwareManager));

        capturingHardwareManager.add(new CapturingSendingHardware<>(captureIrToyPanel, irToyPanel,
                irtoyCapturingSendingBean, irToySerialPortBean, sendingIrToy,
                properties, guiUtils, capturingHardwareManager));

        try {
            capturingHardwareManager.select(properties.getCaptureDevice());
        } catch (HarcHardwareException ex) {
            guiUtils.error(ex);
        }

        properties.addCaptureBeginTimeoutChangeListener((String name1, Object oldValue, Object newValue) -> {
            try {
                capturingHardwareManager.getCapturer().setBeginTimeout((Integer) newValue);
            } catch (IOException ex) {
                guiUtils.error(ex);
            }
        });

        properties.addCaptureMaxSizeChangeListener((String name1, Object oldValue, Object newValue) -> {
            capturingHardwareManager.getCapturer().setCaptureMaxSize((Integer) newValue);
        });

        properties.addCaptureEndingTimeoutChangeListener((String name1, Object oldValue, Object newValue) -> {
            capturingHardwareManager.getCapturer().setEndingTimeout((Integer) newValue);
        });

        properties.addVerboseChangeListener((String name1, Object oldValue, Object newValue) -> {
            capturingHardwareManager.setVerbose((Boolean)newValue);
        });

        optionsMenu.add(capturingHardwareManager.getMenu());

        irpMasterBean.addPropertyChangeListener((java.beans.PropertyChangeEvent evt) -> {
            switch (evt.getPropertyName()) {
                case IrpMasterBean.PROP_PROTOCOL_NAME:
                    properties.setIrpMasterCurrentProtocol((String) evt.getNewValue());
                    break;
                case IrpMasterBean.PROP_D:
                    properties.setIrpMasterCurrentD((String) evt.getNewValue());
                    break;
                case IrpMasterBean.PROP_S:
                    properties.setIrpMasterCurrentS((String) evt.getNewValue());
                    break;
                case IrpMasterBean.PROP_F:
                    properties.setIrpMasterCurrentF((String) evt.getNewValue());
                    break;
                case IrpMasterBean.PROP_T:
                    properties.setIrpMasterCurrentT((String) evt.getNewValue());
                    break;
                case IrpMasterBean.PROP_ADDITIONAL_PARAMS:
                    properties.setIrpMasterCurrentAdditionalParameters((String) evt.getNewValue());
                    break;
                default:
                    guiUtils.error("Programming error detected: " + evt.getPropertyName());
                    break;
            }
        });

        initializePlot();
        String title = Version.appName + " (powered by IrpTransmogrifier) version " + Version.version;
        if (System.getenv("APPIMAGE") != null)
            title += " AppImage";
        super.setTitle(title);
        updateOutputFormat(properties.getOutputFormatIndex());

        LookAndFeelManager.ILookAndFeelManagerCaller caller = new ScrutinizeIrCaller();
        lookAndFeelManager = new LookAndFeelManager(this, lafMenu, caller);
        lookAndFeelManager.setLAF(properties.getLookAndFeel());
        lookAndFeelManager.updateLAF();
        lafMenu.setVisible(true);

        selectFormat();

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

        // When the Tonto stuff loads, it tries to load a library called jnijcomm,
        // see javax.comm.DriverGenUnix. This will fail, but that is no concern for us.
        // However, it writes an ugly stacktrace on stderr, which is scaring the user.
        // Therefore, redirect stderr to nirvana, and make that call now.
        PrintStream nullPrintStream;
        try {
            nullPrintStream = new PrintStream(new ByteArrayOutputStream(), false, "US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            throw new ThisCannotHappenException();
        }
        System.setErr(nullPrintStream);
        DriverGenUnix ignored = new DriverGenUnix();

        console.setStdErr();
        console.setStdOut();

        guiUtils.setUsePopupsForErrors(properties.getUsePopupsForErrors());
        if (userlevel == 0) { // ! experimental
            sendingHardwareTabbedPane.remove(genericSerialPanel);
        }
        if (!LircHardware.isLibraryLoaded()) {
            sendingHardwareTabbedPane.remove(devLircPanel);
            capturingHardwareTabbedPane.remove(captureDevLircPanel);
        }

        processArguments(arguments);
        initialized = true;
    } // end of constructor

    // Just snarfed from IrpMaster
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

    private void cleanupForShutdown() {
        try {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, "US-ASCII"));
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, "US-ASCII"));
        } catch (UnsupportedEncodingException ex) {
            throw new InternalError("This cannot happen");
        }
        capturingHardwareManager.close();
        sendingHardwareManager.close();
        try {
            if (!properties.getWasReset())
                properties.setBounds(getBounds());
            properties.save();
        } catch (IOException e) {
            System.err.println("Problems saving properties; " + e.getMessage());
        }
    }

    public int importGirr(File file, boolean raw, String charsetName) throws ParseException, InvalidArgumentException, IOException {
        return importCommands(girrImporter.getAllCommands(file, charsetName), raw);
    }

    public int importGirr(File file, boolean raw) throws ParseException, InvalidArgumentException, IOException {
        return importGirr(file, raw, properties.getImportCharsetName());
    }

    public void selectSenderHardware(javax.swing.JPanel panel) {
        lastPane = topLevelTabbedPane.getSelectedComponent();
        topLevelTabbedPane.setSelectedComponent(sendingPanel);
        sendingHardwareTabbedPane.setSelectedComponent(panel);
    }

    private Command.CommandTextFormat[] setupExtraTextFormats() {
        ArrayList<Command.CommandTextFormat> formats = new ArrayList<>(8);
        if (properties.getExportGenerateShortCcf())
            formats.add(new ShortCcfFormatter());
        if (properties.getExportGenerateSendIr())
            formats.add(new SendIrFormatter(properties.getExportSendIrModule(),
                    properties.getExportSendIrConnector(),
                    properties.getExportSendIrCompressed()));
        return formats.toArray(new Command.CommandTextFormat[formats.size()]);
    }

    private GirrExporter newGirrExporter() {
        return new GirrExporter(properties.getCreatingUser(),
                properties.getGirrStyleSheetType(),
                properties.getGirrStyleSheetUrl(),
                properties.getGirrFatRaw(),
                properties.getGirrCreateSchemaLocation(),
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

    public ProntoClassicExporter newProntoClassicExporter() {
        ProntoModel prontomodel = ProntoModel.getModelByName((String) prontoModelComboBox.getSelectedItem());
        int buttonwidth = Integer.parseInt(prontoExportButtonWidthTextField.getText());
        int buttonheight = Integer.parseInt(prontoExportButtonHeightTextField.getText());
        int screenwidth = Integer.parseInt(prontoExportScreenWidthTextField.getText());
        int screenheight = Integer.parseInt(prontoExportScreenHeightTextField.getText());
        return new ProntoClassicExporter(/*new File(properties.getExportDir()),*/ prontomodel,
                buttonwidth, buttonheight, screenwidth, screenheight);
    }

    private void loadExportFormats() throws ParserConfigurationException, SAXException, IOException {
        exportFormatManager = new ExportFormatManager(guiUtils,
                new File(properties.mkPathAbsolute(properties.getExportFormatFilePath())), //new File(properties.getExportDir()),
                (String name1) -> {
                    selectFormat(name1);
                }, () -> newGirrExporter(), () -> exportAudioParametersBean.newWaveExporter(), () -> newTextExporter(), () -> newProntoClassicExporter());
    }

    private RemoteSetExporter newRemoteExporter() {
        ICommandExporter exporter = newExporter();
        if (exporter != null && !RemoteSetExporter.class.isInstance(exporter)) {
            guiUtils.error("Selected export format only supports single commands");
            return null;
        }
        return (RemoteSetExporter) exporter;
    }

    private ICommandExporter newExporter() {
        return newExporter((String) exportFormatComboBox.getSelectedItem());
    }

    private ICommandExporter newExporter(String formatName) {
        ICommandExporter exporter = exportFormatManager.get(formatName).newExporter();
        boolean supportsEmbedded = IRemoteSetExporter.class.isInstance(exporter) && ((IRemoteSetExporter) exporter).supportsEmbeddedFormats();
        if (supportsEmbedded && !properties.getExportGenerateParameters()
                && !properties.getExportGenerateCcf() && !properties.getExportGenerateRaw()) {
            boolean answer = guiUtils.confirm("All of \"Parameters\", \"Raw\", and \"Pronto Hex\" deselected in the export. Continue?");
            if (!answer)
                return null;
        }
        return exporter;
    }

    private IrSignal getCapturedIrSignal() throws InvalidArgumentException {
        String str = capturedDataTextArea.getText();
        if (str.trim().isEmpty())
            return null;

        return InterpretString.interpretString(str, getFrequency(), properties.getDummyGap(),
                properties.getInvokeRepeatFinder(), properties.getInvokeCleaner(),
                properties.getAbsoluteTolerance(), properties.getRelativeTolerance(), properties.getMinRepeatLastGap());
    }

    private void loadProtocolsIni() throws IOException, java.text.ParseException {
        protocolsIni = new ProtocolsIni(new File(properties.mkPathAbsolute(properties.getProtocolsIniPath())));
        if (rmduImporter == null)
            rmduImporter = new RmduImporter(protocolsIni);
        else
            rmduImporter.setProtocolsIni(protocolsIni);
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

    private ModulatedIrSequence captureIrSequence() throws IOException, HarcHardwareException, InvalidArgumentException {
        if (!capturingHardwareManager.isReady())
            return null;

        try {
            return capturingHardwareManager.capture();
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
        frequencyLabel.setText(frequency != null ? Long.toString(Math.round(frequency)) : "");
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
        Iterable<Decoder.Decode> decodes = irSignal.introOnly() || irSignal.repeatOnly()
                ? decodeIrSequence(irSignal.toModulatedIrSequence())
                : decodeIrSignal(irSignal);
        setDecodeResult(decodes);
    }

    private Collection<Decoder.Decode> decodeIrSequence(ModulatedIrSequence irSequence) {
        Decoder.DecodeTree decodeTree = decoder.decode(irSequence, decoderParameters);
        List<Decoder.Decode> decodes = new ArrayList<>(4);
        for (Decoder.TrunkDecodeTree decode : decodeTree)
            decodes.add(decode.getTrunk());
        return decodes;
    }

    private Iterable<Decoder.Decode> decodeIrSignal(IrSignal irSignal) {
        Decoder.SimpleDecodesSet decodes = decoder.decodeIrSignal(irSignal, decoderParameters);
        return decodes;
    }

    private void setDecodeResult(Iterable<Decoder.Decode> decodes) {
        Iterator<Decoder.Decode> iterator = decodes.iterator();
        if (!iterator.hasNext()) {
            decodeIRTextField.setText("");
            return;
        }

        int index = 0;
        StringBuilder decodeString = new StringBuilder(40);
        for (Decoder.Decode decode : decodes) {
            if (index == 0)
                decodeString.append(decode.toString());
            if (properties.getPrintDecodesToConsole())
                guiUtils.message(decode.toString());
            index++;
        }

        if (index == 2)
            decodeString.append(" + one more decode");
        else if (index > 2)
            decodeString.append(" + ").append(Integer.toString(index - 1)).append(" more decodes");

        decodeIRTextField.setText(decodeString.toString());
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
                analyzerExtent, analyzerParameterWidths, analyzerMaxParameterWidth, analyzerInvert, burstPrefs);
        List<Protocol> protocols = null;
        try {
            protocols = analyzer.searchBestProtocol(params);
        } catch (NoDecoderMatchException ex) {
            Logger.getLogger(GuiMain.class.getName()).log(Level.SEVERE, null, ex);
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

    private int numberbaseIndex2numberbase(int index) {
        return index == 0 ? 2
                : index == 1 ? 8
                : index == 2 ? 10
                : 16;
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

    public void scrutinizeIrSignal(JTable table) throws ErroneousSelectionException, GirrException, IrpException, IrCoreException {
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

    private void saveParametricSignals(RemoteSetExporter exporter) {
        try {
            File file = saveCommands(parameterTableModel, Version.appName + " parametric export", exporter);
            if (file != null)
                guiUtils.message("File " + file + " was successfully written.");
        } catch (IOException | TransformerException | GirrException | IrCoreException | IrpException ex) {
            guiUtils.error(ex);
        }
    }

    private void saveRawSignals(RemoteSetExporter exporter) {
        try {
            File file = saveCommands(rawTableModel, Version.appName + " raw export", exporter);
            if (file != null)
                guiUtils.message("File " + file + " was successfully written.");
        } catch (IOException | TransformerException | GirrException | IrCoreException | IrpException ex) {
            guiUtils.error(ex);
        }
    }

    private File saveCommands(NamedIrSignal.LearnedIrSignalTableModel tableModel, String title, RemoteSetExporter exporter) throws IOException, TransformerException, GirrException, IrpException, IrCoreException {
        Map<String, Command> commands = tableModel.getCommandsWithSanityCheck(guiUtils);
        if (commands == null)
            return null;

        File file = saveCommands(commands, "IrScrutinizer " + tableModel.getType() + " table", title, exporter);
        if (file != null)
            tableModel.clearUnsavedChanges();
        return file;
    }

    private File saveCommands(Map<String, Command> commands, String source, String title, RemoteSetExporter exporter)
            throws IOException, TransformerException, GirrException, IrpException, IrCoreException {
        if (properties.getExportInquireDeviceData() && exporter.supportsMetaData()) {
            Remote.MetaData newMetaData = MetaDataDialog.inquireMetaData(metaData, this);
            if (newMetaData == null) // user bailed out
                return null;

            metaData = newMetaData;
        }

        File file = exporter.export(commands, source, title, metaData,
                properties.getExportNoRepeats(), properties.getExportAutomaticFilenames(), this,
                new File(properties.getExportDir()), properties.getExportCharsetName());
        return file;
    }

    private File saveSignal(Command command, String title, ICommandExporter exporter) throws IOException, TransformerException, IrCoreException, IrpException, GirrException {
        return exporter.export(command, "IrScrutinizer captured signal", title,
                properties.getExportNoRepeats(), properties.getExportAutomaticFilenames(), this,
                new File(properties.getExportDir()), properties.getExportCharsetName());
    }

    private void saveSelectedSignal(JTable table, String title) {
        try {
            File savedFile = saveSignal(tableUtils.commandTableSelectedRow(table), title, newExporter());
            guiUtils.message("File " + savedFile.getPath() + " successfully writtten");
        } catch (IOException | TransformerException | GirrException | ErroneousSelectionException | IrCoreException | IrpException ex) {
            guiUtils.error(ex);
        }
    }

    private double getFrequency() {
        try {
            return Double.parseDouble(frequencyLabel.getText());
        } catch (NumberFormatException | NullPointerException ex) {
        }
        double f = properties.getFallbackFrequency();
        return f > 0 ? f : ModulatedIrSequence.DEFAULT_FREQUENCY;
    }

    private void saveSignals(Map<String, Command> commands) throws IOException, TransformerException, GirrException, IrpException, IrCoreException {
        if (commands.isEmpty())
            guiUtils.error("Nothing to export");
        else if (commands.size() == 1) {
            // exporting just a single command
            File savedFile = saveSignal(commands.values().iterator().next(), "IrScrutinizer generated signal", newExporter());
            if (savedFile != null)
                guiUtils.message("File " + savedFile.getPath() + " successfully written with one signal.");
        } else {
            RemoteSetExporter exporter = newRemoteExporter();
            if (exporter == null)
                return; // error has already been reported to the user.
            File savedFile = saveCommands(commands, null, null, exporter);
            if (savedFile != null)
                guiUtils.message("File " + savedFile.getPath() + " successfully written with " + commands.size() + " signals.");
        }
    }

    private void saveSignal(IrSignal irSignal) {
        saveSignal(irSignal, newExporter());
    }

    private void saveSignal(IrSignal irSignal, ICommandExporter exporter) {
        try {
            if (irSignal == null) {
                guiUtils.error("Not exporting empty signal.");
                return;
            }
            Command command = new Command("IrScrutinizer captured signal", null, irSignal);
            File savedFile = saveSignal(command, "IrScrutinizer scrutinized signal", exporter);
            if (savedFile != null)
                guiUtils.message("File " + savedFile.getPath() + " successfully writtten");
        } catch (IOException | TransformerException | GirrException | IrCoreException | IrpException ex) {
        }
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

    public int importCommands(Collection<Command> commands, boolean raw) {
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

    public void importCommand(Command command, boolean raw) throws IrpException, IrCoreException {
        if (command == null)
            return;

        if (raw)
            registerRawCommand(command);
        else
            registerParameterCommand(command);
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
                importCommands(importer.getCommands(), raw);
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

    public void importModulatedIrSequenceFile(File file) throws InvalidArgumentException {
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
            while (! terminate && client.getButton().isSelected()) {
                try {
                    ModulatedIrSequence sequence = captureIrSequence();
                    if (sequence != null)
                        client.processSequence(sequence);

                    if (!capturingHardwareManager.isReady()) {
                        guiUtils.error("Selected capture device is no longer ready");
                        client.getButton().setSelected(false);
                    }
                } catch (IOException | HarcHardwareException | InvalidArgumentException ex) {
                    guiUtils.error(ex);
                    client.getButton().setSelected(false);
                }
            }
            captureThread = null; // thread suicide
            startButton.setEnabled(true);
            enableRawCaptureOnly(false);
        }

        private void terminate() {
            terminate = true;
        }
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
        Long answer = guiUtils.getLongInput("Enter new value for " + name, 0);
        if (answer != null)
            parameterTableModel.setParameter(name, answer);
    }

    private void setupIrTrans() throws UnknownHostException, IOException {
        String irTransIp = irTransInternetHostPanel.getIpName();
        IrTransIRDB irTransIRDB = new IrTransIRDB(irTransIp, properties.getVerbose(), properties.getSendingTimeout());
        //irTrans = new IrTrans(irTransIp, properties.getVerbose(), properties.getStartTimeout());
        irTransInternetHostPanel.setHardware(irTransIRDB);
        irTransNamedCommandLauncher.setHardware(irTransIRDB);
        properties.setIrTransIpName(irTransIp);
    }

    public boolean transmit(IrSignal irSignal) throws IOException, HardwareUnavailableException, HarcHardwareException, NoSuchTransmitterException, InvalidArgumentException {
        return sendingHardwareManager.sendIr(irSignal, Integer.parseInt((String)noTransmitsComboBox.getSelectedItem()));
    }

    public boolean transmit(Command command) throws IrpException, IrCoreException, IOException, HardwareUnavailableException, HarcHardwareException {
        return transmit(command.toIrSignal());
    }

    private boolean transmit(JTable table) throws ErroneousSelectionException, GirrException, IrpException, IrCoreException, IOException, HardwareUnavailableException, HarcHardwareException {
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
        exportGenerateParametersCheckBox.setEnabled(select);
        exportGenerateRawCheckBox.setEnabled(select);
        exportGenerateCcfCheckBox.setEnabled(select);
        exportGenerateShortCcfCheckBox.setEnabled(select);
        exportGenerateSendIrCheckBox.setEnabled(select);
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

        ICommandExporter exporter = format.newExporter();
        boolean supportsEmbedded = IRemoteSetExporter.class.isInstance(exporter) && ((IRemoteSetExporter) exporter).supportsEmbeddedFormats();
        enableSubFormats(supportsEmbedded);
        exportRepeatComboBox.setEnabled(supportsEmbedded && exportGenerateSendIrCheckBox.isSelected() || exporter.considersRepetitions());
        if (!formatName.equals(exportFormatComboBox.getSelectedItem()))
            exportFormatComboBox.setSelectedItem(formatName);
        exportFormatManager.setMenuSelection(formatName);
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

    private boolean mode2UseStdin() {
        return mode2TabbedPane.getSelectedIndex() == 0;
    }

    private boolean checkChangeExportDirectory(File fileName) {
        try {
            File dir = fileName.getCanonicalFile();
            if (dir.isDirectory() && dir.canWrite()) {
                properties.setExportDir(exportDirectoryTextField.getText());
                return true;
            } else
                guiUtils.error(exportDirectoryTextField.getText() + " is not an existing and writable directory.");
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
        return false;
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
        rawCodeAnalyzeMenuItem = new javax.swing.JMenuItem();
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
        jSeparator13 = new javax.swing.JPopupMenu.Separator();
        scrutinizeParametricMenuItem = new javax.swing.JMenuItem();
        transmitMenuItem = new javax.swing.JMenuItem();
        deleteMenuItem1 = new javax.swing.JMenuItem();
        saveSelectedCookedMenuItem = new javax.swing.JMenuItem();
        debugTableRowMenuItem1 = new javax.swing.JMenuItem();
        jSeparator14 = new javax.swing.JPopupMenu.Separator();
        clearMenuItem1 = new javax.swing.JMenuItem();
        checkParametrizedSignalsMenuItem = new javax.swing.JMenuItem();
        saveCookedMenuItem = new javax.swing.JMenuItem();
        jSeparator19 = new javax.swing.JPopupMenu.Separator();
        parametrizedCopyAllMenuItem = new javax.swing.JMenuItem();
        parametrizedCopySelectionMenuItem = new javax.swing.JMenuItem();
        jSeparator27 = new javax.swing.JPopupMenu.Separator();
        hideColumnMenuItem1 = new javax.swing.JMenuItem();
        resetRawTableColumnsMenuItem1 = new javax.swing.JMenuItem();
        hideUnusedMenuItem = new javax.swing.JMenuItem();
        hideUninterestingColumnsMenuItem = new javax.swing.JMenuItem();
        clearParametrizedCommentMenuItem = new javax.swing.JMenuItem();
        parametrizedAdvancedMenu = new javax.swing.JMenu();
        setProtocolMenuItem = new javax.swing.JMenuItem();
        setDMenuItem = new javax.swing.JMenuItem();
        setSMenuItem = new javax.swing.JMenuItem();
        setFMenuItem = new javax.swing.JMenuItem();
        setTMenuItem = new javax.swing.JMenuItem();
        unsetTMenuItem = new javax.swing.JMenuItem();
        setMiscParamsMenuItem = new javax.swing.JMenuItem();
        transformNamesMenu = new javax.swing.JMenu();
        addNamePrefixMenuItem = new javax.swing.JMenuItem();
        transformNameMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        addMissingFsMenuItem = new javax.swing.JMenuItem();
        addMissingNamesMenuItem = new javax.swing.JMenuItem();
        deleteDefaultedSignalsMenuItem = new javax.swing.JMenuItem();
        rawTablePopupMenu = new javax.swing.JPopupMenu();
        rawSorterCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator25 = new javax.swing.JPopupMenu.Separator();
        moveUpMenuItem = new javax.swing.JMenuItem();
        moveDownMenuItem = new javax.swing.JMenuItem();
        jSeparator11 = new javax.swing.JPopupMenu.Separator();
        rawFromClipboardMenuItem = new javax.swing.JMenuItem();
        jSeparator18 = new javax.swing.JPopupMenu.Separator();
        scrutinizeMenuItem = new javax.swing.JMenuItem();
        sendMenuItem = new javax.swing.JMenuItem();
        deleteMenuItem = new javax.swing.JMenuItem();
        saveSelectedRawTableRowMenuItem = new javax.swing.JMenuItem();
        debugTableRowMenuItem = new javax.swing.JMenuItem();
        jSeparator12 = new javax.swing.JPopupMenu.Separator();
        clearMenuItem = new javax.swing.JMenuItem();
        checkRawCommandsMenuItem = new javax.swing.JMenuItem();
        saveRawMenuItem = new javax.swing.JMenuItem();
        jSeparator28 = new javax.swing.JPopupMenu.Separator();
        rawCopyAllMenuItem = new javax.swing.JMenuItem();
        rawCopySelectionMenuItem = new javax.swing.JMenuItem();
        jSeparator29 = new javax.swing.JPopupMenu.Separator();
        hideColumnMenuItem = new javax.swing.JMenuItem();
        resetRawTableColumnsMenuItem = new javax.swing.JMenuItem();
        removeUnusedMenuItem1 = new javax.swing.JMenuItem();
        hideUninterestingColumnsMenuItem1 = new javax.swing.JMenuItem();
        clearRawCommentMenuItem = new javax.swing.JMenuItem();
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
        irpMasterBean = new org.harctoolbox.guicomponents.IrpMasterBean(this, guiUtils, irpDatabase, properties.getIrpMasterCurrentProtocol(), properties.getIrpMasterCurrentD(), properties.getIrpMasterCurrentS(), properties.getIrpMasterCurrentF(), properties.getIrpMasterCurrentT(), properties.getIrpMasterCurrentAdditionalParameters());
        generateButton = new javax.swing.JButton();
        generateHelpButton = new javax.swing.JButton();
        transferToParametricRemoteButton = new javax.swing.JButton();
        openLastFileButton1 = new javax.swing.JButton();
        importPanel = new javax.swing.JPanel();
        importTabbedPane = new javax.swing.JTabbedPane();
        gcdbPanel = new javax.swing.JPanel();
        gcdbImportButton = new javax.swing.JButton();
        globalCacheDBBrowseButton = new javax.swing.JButton();
        apiKeyButton = new javax.swing.JButton();
        gcdbManufacturerComboBox = new javax.swing.JComboBox<>();
        gcdbDeviceTypeComboBox = new javax.swing.JComboBox<>();
        gcdbCodeSetComboBox = new javax.swing.JComboBox<>();
        jLabel44 = new javax.swing.JLabel();
        jLabel47 = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        gcdbTreeImporter = new org.harctoolbox.irscrutinizer.importer.TreeImporter(this.guiUtils);
        importGlobalCacheHelpButton = new javax.swing.JButton();
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
        irdbPanel = new javax.swing.JPanel();
        irdbBrowseButton = new javax.swing.JButton();
        irdbImportButton = new javax.swing.JButton();
        irdbManufacturerComboBox = new javax.swing.JComboBox<>();
        irdbDeviceTypeComboBox = new javax.swing.JComboBox<>();
        irdbCodeSetComboBox = new javax.swing.JComboBox<>();
        jLabel45 = new javax.swing.JLabel();
        jLabel46 = new javax.swing.JLabel();
        jLabel49 = new javax.swing.JLabel();
        irdbTreeImporter = new org.harctoolbox.irscrutinizer.importer.TreeImporter(this.guiUtils);
        irdbImportAllButton = new javax.swing.JButton();
        importIrdbHelpButton = new javax.swing.JButton();
        girrImportPanel = new javax.swing.JPanel();
        girrWebSiteButton = new javax.swing.JButton();
        girrFileImporterBean = new org.harctoolbox.irscrutinizer.importer.FileImporterBean<>(guiUtils, properties, girrImporter);
        importGirrSignalHelpButton = new javax.swing.JButton();
        lircImportPanel = new javax.swing.JPanel();
        jButton20 = new javax.swing.JButton();
        lircFileImporterBean = new org.harctoolbox.irscrutinizer.importer.FileImporterBean<>(guiUtils, properties, lircImporter);
        importLircHelpButton = new javax.swing.JButton();
        rmduImportPanel = new javax.swing.JPanel();
        webRmduButton = new javax.swing.JButton();
        rmduImporterBean = new org.harctoolbox.irscrutinizer.importer.FileImporterBean<>(guiUtils, properties, rmduImporter);
        importRemoteMasterHelpButton = new javax.swing.JButton();
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
        jLabel15 = new javax.swing.JLabel();
        importIctHelpButton = new javax.swing.JButton();
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
        exportGenerateRawCheckBox = new javax.swing.JCheckBox();
        exportGenerateCcfCheckBox = new javax.swing.JCheckBox();
        exportGenerateParametersCheckBox = new javax.swing.JCheckBox();
        exportSignalButton = new javax.swing.JButton();
        automaticExportFilenamesCheckBox = new javax.swing.JCheckBox();
        openLastFileButton = new javax.swing.JButton();
        exportRawRemoteButton = new javax.swing.JButton();
        exportSpecificOptionsTabbedPane = new javax.swing.JTabbedPane();
        girrExportOptionsPanel = new javax.swing.JPanel();
        girrStylesheetUrlTextField = new javax.swing.JTextField();
        jLabel28 = new javax.swing.JLabel();
        girrSchemaLinkCheckBox = new javax.swing.JCheckBox();
        girrFatRawCheckBox = new javax.swing.JCheckBox();
        girrStylesheetTypeComboBox = new javax.swing.JComboBox<>();
        jLabel33 = new javax.swing.JLabel();
        exportGirrHelpButton = new javax.swing.JButton();
        waveExportOptionsPanel = new javax.swing.JPanel();
        exportAudioParametersBean = new org.harctoolbox.guicomponents.AudioParametersBean(properties);
        exportWaveHelpButton = new javax.swing.JButton();
        sendirExportOptionsPanel = new javax.swing.JPanel();
        sendirModuleComboBox = new javax.swing.JComboBox<>();
        sendirConnectorComboBox = new javax.swing.JComboBox<>();
        sendirCompressedCheckBox = new javax.swing.JCheckBox();
        jLabel10 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        exportSendirHelpButton = new javax.swing.JButton();
        prontoClassicExportOptionsPanel = new javax.swing.JPanel();
        prontoModelComboBox = new javax.swing.JComboBox<>();
        prontoExportScreenWidthTextField = new javax.swing.JTextField();
        prontoExportScreenHeightTextField = new javax.swing.JTextField();
        prontoExportButtonWidthTextField = new javax.swing.JTextField();
        prontoExportButtonHeightTextField = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        jLabel31 = new javax.swing.JLabel();
        exportProntoHelpButton = new javax.swing.JButton();
        exportParametricRemoteButton = new javax.swing.JButton();
        exportGenerateSendIrCheckBox = new javax.swing.JCheckBox();
        exportRepeatComboBox = new javax.swing.JComboBox<>();
        jLabel16 = new javax.swing.JLabel();
        exportHelpButton = new javax.swing.JButton();
        exportRawRemoteButton1 = new javax.swing.JButton();
        exportGenerateShortCcfCheckBox = new javax.swing.JCheckBox();
        sendingPanel = new javax.swing.JPanel();
        sendingHardwareTabbedPane = new javax.swing.JTabbedPane();
        globalCachePanel = new javax.swing.JPanel();
        globalCacheIrSenderSelector = new org.harctoolbox.guicomponents.GlobalCacheIrSenderSelector(guiUtils, properties.getVerbose(), properties.getGlobalCacheTimeout(), true);
        sendingGlobalCacheHelpButton = new javax.swing.JButton();
        lircPanel = new javax.swing.JPanel();
        lircInternetHostPanel = new org.harctoolbox.guicomponents.InternetHostPanel(guiUtils, true, true, true);
        lircNamedCommandLauncher = new org.harctoolbox.guicomponents.NamedCommandLauncher(guiUtils);
        sendingLircHelpButton = new javax.swing.JButton();
        devLircPanel = new javax.swing.JPanel();
        devLircBean = new org.harctoolbox.guicomponents.DevLircBean(guiUtils, properties.getDevLircName(), true);
        sendingDevLircHardwareHelpButton = new javax.swing.JButton();
        irTransPanel = new javax.swing.JPanel();
        irTransInternetHostPanel = new org.harctoolbox.guicomponents.InternetHostPanel(guiUtils, false, true, true);
        irTransNamedCommandLauncher = new org.harctoolbox.guicomponents.NamedCommandLauncher(guiUtils);
        sendingIrTransHelpButton = new javax.swing.JButton();
        audioPanel = new javax.swing.JPanel();
        transmitAudioParametersBean = new org.harctoolbox.guicomponents.AudioParametersBean(properties);
        sendingAudioHelpButton = new javax.swing.JButton();
        irToyPanel = new javax.swing.JPanel();
        irToySerialPortBean = new org.harctoolbox.guicomponents.SerialPortSimpleBean(guiUtils, properties.getIrToyPortName(), properties.getIrToyPortBaudRate(), true);
        sendingIrToyHelpButton = new javax.swing.JButton();
        girsClientPanel = new javax.swing.JPanel();
        sendingGirsClientHelpButton = new javax.swing.JButton();
        girsTcpSerialComboBean = new org.harctoolbox.guicomponents.GirsClientBean(guiUtils, properties);
        commandFusionSendPanel = new javax.swing.JPanel();
        commandFusionSendingSerialPortBean = new org.harctoolbox.guicomponents.SerialPortSimpleBean(guiUtils, properties.getCommandFusionPortName(), CommandFusion.DEFAULTBAUDRATE, false);
        sendingCommandFusionHelpButton = new javax.swing.JButton();
        genericSerialPanel = new javax.swing.JPanel();
        genericSerialSenderBean = new org.harctoolbox.irscrutinizer.sendinghardware.GenericSerialSenderBean(guiUtils);
        sendingGenericSerialPortHelpButton = new javax.swing.JButton();
        noTransmitsComboBox = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        transmitScrutinizedButton = new javax.swing.JButton();
        transmitGenerateButton2 = new javax.swing.JButton();
        sendingHardwareHelpButton = new javax.swing.JButton();
        capturingPanel = new javax.swing.JPanel();
        capturingHardwareTabbedPane = new javax.swing.JTabbedPane();
        captureIrWidgetPanel = new javax.swing.JPanel();
        irWidgetSerialPortSimpleBean = new org.harctoolbox.guicomponents.SerialPortSimpleBean(guiUtils, properties.getIrWidgetCapturePortName(), 115200, false);
        capturingIrWidgetHardwareHelpButton = new javax.swing.JButton();
        captureGlobalCachePanel = new javax.swing.JPanel();
        globalCacheCaptureSelector = new org.harctoolbox.guicomponents.GlobalCacheIrSenderSelector(guiUtils, properties.getVerbose(), properties.getSendingTimeout(), false);
        capturingGlobalCacheHardwareHelpButton = new javax.swing.JButton();
        captureLircMode2Panel = new javax.swing.JPanel();
        startMode2Button = new javax.swing.JButton();
        stopMode2Button = new javax.swing.JButton();
        capturingMode2HardwareHelpButton = new javax.swing.JButton();
        mode2TabbedPane = new javax.swing.JTabbedPane();
        mode2StdinPane = new javax.swing.JPanel();
        mode2SubprocessPane = new javax.swing.JPanel();
        lircMode2CommandTextField = new javax.swing.JTextField();
        mode2CommandLabel = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        jLabel32 = new javax.swing.JLabel();
        captureDevLircPanel = new javax.swing.JPanel();
        capturingDevLircHardwareHelpButton = new javax.swing.JButton();
        devLircCapturingSendingBean = new CapturingSendingBean(this);
        captureIrToyPanel = new javax.swing.JPanel();
        capturingIrToyHardwareHelpButton = new javax.swing.JButton();
        irtoyCapturingSendingBean = new CapturingSendingBean(this);
        captureGirsPanel = new javax.swing.JPanel();
        capturingGirsHardwareHelpButton = new javax.swing.JButton();
        girsClientCapturingSendingBean = new CapturingSendingBean(this);
        captureCommandFusionPanel = new javax.swing.JPanel();
        capturingCommandFusionHardwareHelpButton = new javax.swing.JButton();
        commandFusionCapturingSendingBean = new CapturingSendingBean(this);
        captureTestButton = new javax.swing.JButton();
        capturingHardwareHelpButton = new javax.swing.JButton();
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
        importRmduMenuItem1 = new javax.swing.JMenuItem();
        importCmlMenuItem = new javax.swing.JMenuItem();
        importCommandFusionMenuItem = new javax.swing.JMenuItem();
        importRawMenu = new javax.swing.JMenu();
        importGirrMenuItem = new javax.swing.JMenuItem();
        importIctMenuItem = new javax.swing.JMenuItem();
        importLircMenuItem = new javax.swing.JMenuItem();
        importCcfMenuItem = new javax.swing.JMenuItem();
        importXcfMenuItem = new javax.swing.JMenuItem();
        importRmduMenuItem = new javax.swing.JMenuItem();
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
        lircTimeoutMenuItem = new javax.swing.JMenuItem();
        protocolParametersMenu = new javax.swing.JMenu();
        absToleranceMenuItem = new javax.swing.JMenuItem();
        relToleranceMenuItem = new javax.swing.JMenuItem();
        frequencyToleranceMenuItem = new javax.swing.JMenuItem();
        minLeadoutMenuItem = new javax.swing.JMenuItem();
        minRepeatGapMenuItem = new javax.swing.JMenuItem();
        dummyGapMenuItem = new javax.swing.JMenuItem();
        fallbackFrequencyMenuItem = new javax.swing.JMenuItem();
        verboseCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        ignoreEndingSilenceCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        repeatFinderCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        cleanerCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        invokeAnalyzerCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        printAnalyzeIRPsToConsoleCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        printDecodesToConsoleCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        parametrizedLearnIgnoreTCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        proxyMenuItem = new javax.swing.JMenuItem();
        irpProtocolsIniMenu = new javax.swing.JMenu();
        irpProtocolsEditMenuItem = new javax.swing.JMenuItem();
        irpProtocolsSelectMenuItem = new javax.swing.JMenuItem();
        irpFormatsIniReloadMenuItem = new javax.swing.JMenuItem();
        exportFormatsMenu = new javax.swing.JMenu();
        exportFormatsEditMenuItem = new javax.swing.JMenuItem();
        exportFormatsSelectMenuItem = new javax.swing.JMenuItem();
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
        rejectLircCodeImports = new javax.swing.JCheckBoxMenuItem();
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
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
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
        issuesMenuItem = new javax.swing.JMenuItem();
        feedbackMenuItem = new javax.swing.JMenuItem();

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
        signalClearMenuItem.setText("Clear signal");
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

        rawCodeAnalyzeMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/gear.png"))); // NOI18N
        rawCodeAnalyzeMenuItem.setText("Scrutinize");
        rawCodeAnalyzeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawCodeAnalyzeMenuItemActionPerformed(evt);
            }
        });
        CCFCodePopupMenu.add(rawCodeAnalyzeMenuItem);
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

        moveUpMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, java.awt.event.InputEvent.CTRL_MASK));
        moveUpMenuItem1.setMnemonic('U');
        moveUpMenuItem1.setText("Move Up");
        moveUpMenuItem1.setEnabled(!properties.getSorterOnParametrizedTable());
        moveUpMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveUpMenuItem1ActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(moveUpMenuItem1);

        moveDownMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, java.awt.event.InputEvent.CTRL_MASK));
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
        parameterTablePopupMenu.add(jSeparator13);

        scrutinizeParametricMenuItem.setText("Scrutinize selected");
        scrutinizeParametricMenuItem.setToolTipText("Send the selected signal to Scrutinize signal, if possible.");
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

        deleteMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        deleteMenuItem1.setText("Delete selected");
        deleteMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteMenuItem1ActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(deleteMenuItem1);

        saveSelectedCookedMenuItem.setText("Export selected...");
        saveSelectedCookedMenuItem.setToolTipText("Export the selected signal in selected format,");
        saveSelectedCookedMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveSelectedCookedMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(saveSelectedCookedMenuItem);

        debugTableRowMenuItem1.setText("Print selected to console");
        debugTableRowMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                debugTableRowMenuItem1ActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(debugTableRowMenuItem1);
        parameterTablePopupMenu.add(jSeparator14);

        clearMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, java.awt.event.InputEvent.CTRL_MASK));
        clearMenuItem1.setMnemonic('C');
        clearMenuItem1.setText("Clear all");
        clearMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearMenuItem1ActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(clearMenuItem1);

        checkParametrizedSignalsMenuItem.setText("Check commands");
        checkParametrizedSignalsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkParametrizedSignalsMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(checkParametrizedSignalsMenuItem);

        saveCookedMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, 0));
        saveCookedMenuItem.setMnemonic('E');
        saveCookedMenuItem.setText("Export");
        saveCookedMenuItem.setToolTipText("Export in the selected format");
        saveCookedMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveCookedMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(saveCookedMenuItem);
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

        clearParametrizedCommentMenuItem.setText("Clear all comments");
        clearParametrizedCommentMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearParametrizedCommentMenuItemActionPerformed(evt);
            }
        });
        parameterTablePopupMenu.add(clearParametrizedCommentMenuItem);

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

        moveUpMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, java.awt.event.InputEvent.CTRL_MASK));
        moveUpMenuItem.setMnemonic('U');
        moveUpMenuItem.setText("Move Up");
        moveUpMenuItem.setEnabled(!properties.getSorterOnRawTable());
        moveUpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveUpMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(moveUpMenuItem);

        moveDownMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, java.awt.event.InputEvent.CTRL_MASK));
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

        rawFromClipboardMenuItem.setText("Import signal from clipboard");
        rawFromClipboardMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawFromClipboardMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(rawFromClipboardMenuItem);
        rawTablePopupMenu.add(jSeparator18);

        scrutinizeMenuItem.setText("Scrutinize selected");
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

        deleteMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        deleteMenuItem.setText("Delete selected");
        deleteMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(deleteMenuItem);

        saveSelectedRawTableRowMenuItem.setText("Export selected...");
        saveSelectedRawTableRowMenuItem.setToolTipText("Export selected signal to selected format.");
        saveSelectedRawTableRowMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveSelectedRawTableRowMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(saveSelectedRawTableRowMenuItem);

        debugTableRowMenuItem.setText("Print selected to console");
        debugTableRowMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                debugTableRowMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(debugTableRowMenuItem);
        rawTablePopupMenu.add(jSeparator12);

        clearMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, java.awt.event.InputEvent.CTRL_MASK));
        clearMenuItem.setMnemonic('C');
        clearMenuItem.setText("Clear all");
        clearMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(clearMenuItem);

        checkRawCommandsMenuItem.setText("Check commands");
        checkRawCommandsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkRawCommandsMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(checkRawCommandsMenuItem);

        saveRawMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, 0));
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

        clearRawCommentMenuItem.setText("Clear all comments");
        clearRawCommentMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearRawCommentMenuItemActionPerformed(evt);
            }
        });
        rawTablePopupMenu.add(clearRawCommentMenuItem);

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
        irPlotter.setToolTipText("Plot of captured IR signal. Press right mouse button for a menu.");
        irPlotter.setAutoscrolls(true);
        irPlotter.setPreferredSize(new java.awt.Dimension(749, 100));

        javax.swing.GroupLayout irPlotterLayout = new javax.swing.GroupLayout(irPlotter);
        irPlotter.setLayout(irPlotterLayout);
        irPlotterLayout.setHorizontalGroup(
            irPlotterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 982, Short.MAX_VALUE)
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
                .addComponent(capturedDataScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 216, Short.MAX_VALUE)
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
        parameterTableScrollPane.setViewportView(parameterTable);
        parameterTable.setColumnModel(parameterTableColumnModel);

        javax.swing.GroupLayout cookedPanelLayout = new javax.swing.GroupLayout(cookedPanel);
        cookedPanel.setLayout(cookedPanelLayout);
        cookedPanelLayout.setHorizontalGroup(
            cookedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(parameterTableScrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 984, Short.MAX_VALUE)
        );
        cookedPanelLayout.setVerticalGroup(
            cookedPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(parameterTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
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
        rawTableScrollPane.setViewportView(rawTable);
        rawTable.setColumnModel(this.rawTableColumnModel);

        javax.swing.GroupLayout rawPanelLayout = new javax.swing.GroupLayout(rawPanel);
        rawPanel.setLayout(rawPanelLayout);
        rawPanelLayout.setHorizontalGroup(
            rawPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 984, Short.MAX_VALUE)
            .addGroup(rawPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(rawTableScrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 984, Short.MAX_VALUE))
        );
        rawPanelLayout.setVerticalGroup(
            rawPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 325, Short.MAX_VALUE)
            .addGroup(rawPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(rawTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE))
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
                .addComponent(rawCookedTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 367, Short.MAX_VALUE)
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
        generateButton.setText("Generate");
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
                    .addComponent(irpMasterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 980, Short.MAX_VALUE)
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
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
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

        topLevelTabbedPane.addTab("Generate", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/gear.png")), generatePanel); // NOI18N

        gcdbImportButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/reload.png"))); // NOI18N
        gcdbImportButton.setText("Load");
        gcdbImportButton.setEnabled(false);
        gcdbImportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gcdbImportButtonActionPerformed(evt);
            }
        });

        globalCacheDBBrowseButton.setText("Web site");
        globalCacheDBBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                globalCacheDBBrowseButtonActionPerformed(evt);
            }
        });

        apiKeyButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/password.png"))); // NOI18N
        apiKeyButton.setText("ApiKey...");
        apiKeyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                apiKeyButtonActionPerformed(evt);
            }
        });

        gcdbManufacturerComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select me to load" }));
        gcdbManufacturerComboBox.setToolTipText(properties.getGlobalCacheApiKey().isEmpty() ? "API-Key not entered" : null);
        gcdbManufacturerComboBox.setEnabled(!properties.getGlobalCacheApiKey().isEmpty());
        gcdbManufacturerComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gcdbManufacturerComboBoxActionPerformed(evt);
            }
        });

        gcdbDeviceTypeComboBox.setEnabled(false);
        gcdbDeviceTypeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gcdbDeviceTypeComboBoxActionPerformed(evt);
            }
        });

        gcdbCodeSetComboBox.setEnabled(false);

        jLabel44.setText("Manufacturer");

        jLabel47.setText("Device Type");

        jLabel48.setText("Setup Code");

        importGlobalCacheHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        importGlobalCacheHelpButton.setText("Help");
        importGlobalCacheHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importGlobalCacheHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout gcdbPanelLayout = new javax.swing.GroupLayout(gcdbPanel);
        gcdbPanel.setLayout(gcdbPanelLayout);
        gcdbPanelLayout.setHorizontalGroup(
            gcdbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(gcdbPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(gcdbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel44)
                    .addComponent(gcdbManufacturerComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(gcdbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(gcdbDeviceTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel47))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(gcdbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(gcdbPanelLayout.createSequentialGroup()
                        .addComponent(gcdbCodeSetComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(gcdbImportButton)
                        .addGap(169, 169, 169))
                    .addGroup(gcdbPanelLayout.createSequentialGroup()
                        .addComponent(jLabel48)
                        .addGap(461, 461, 461))))
            .addGroup(gcdbPanelLayout.createSequentialGroup()
                .addComponent(gcdbTreeImporter, javax.swing.GroupLayout.DEFAULT_SIZE, 728, Short.MAX_VALUE)
                .addGroup(gcdbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(gcdbPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 130, Short.MAX_VALUE)
                        .addComponent(importGlobalCacheHelpButton)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, gcdbPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(gcdbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(apiKeyButton, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(globalCacheDBBrowseButton, javax.swing.GroupLayout.Alignment.TRAILING))))
                .addContainerGap())
        );

        gcdbPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {apiKeyButton, globalCacheDBBrowseButton, importGlobalCacheHelpButton});

        gcdbPanelLayout.setVerticalGroup(
            gcdbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(gcdbPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(gcdbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel44)
                    .addComponent(jLabel47)
                    .addComponent(jLabel48))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(gcdbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(gcdbManufacturerComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gcdbDeviceTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gcdbCodeSetComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gcdbImportButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(gcdbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(gcdbTreeImporter, javax.swing.GroupLayout.DEFAULT_SIZE, 267, Short.MAX_VALUE)
                    .addGroup(gcdbPanelLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(apiKeyButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(globalCacheDBBrowseButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 148, Short.MAX_VALUE)
                        .addComponent(importGlobalCacheHelpButton)
                        .addContainerGap())))
        );

        importTabbedPane.addTab("GlobalCache DB (old)", gcdbPanel);

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
                        .addComponent(controlTowerCodeSetComboBox, 0, 317, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(controlTowerImportButton)
                        .addGap(169, 169, 169))))
            .addGroup(controlTowerPanelLayout.createSequentialGroup()
                .addComponent(controlTowerTreeImporter, javax.swing.GroupLayout.DEFAULT_SIZE, 734, Short.MAX_VALUE)
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
                    .addComponent(controlTowerTreeImporter, javax.swing.GroupLayout.DEFAULT_SIZE, 267, Short.MAX_VALUE)
                    .addGroup(controlTowerPanelLayout.createSequentialGroup()
                        .addGap(52, 52, 52)
                        .addComponent(controlTowerBrowseButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(importControlTowerHelpButton)
                        .addContainerGap())))
        );

        controlTowerPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {controlTowerBrowseButton, importControlTowerHelpButton});

        importTabbedPane.addTab("ControlTower Database", controlTowerPanel);

        irdbBrowseButton.setText("Visit Web site");
        irdbBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irdbBrowseButtonActionPerformed(evt);
            }
        });

        irdbImportButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/reload.png"))); // NOI18N
        irdbImportButton.setText("Load");
        irdbImportButton.setEnabled(false);
        irdbImportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irdbImportButtonActionPerformed(evt);
            }
        });

        irdbManufacturerComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Select me to load" }));
        irdbManufacturerComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irdbManufacturerComboBoxActionPerformed(evt);
            }
        });

        irdbDeviceTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "__" }));
        irdbDeviceTypeComboBox.setEnabled(false);
        irdbDeviceTypeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irdbDeviceTypeComboBoxActionPerformed(evt);
            }
        });

        irdbCodeSetComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "--" }));
        irdbCodeSetComboBox.setEnabled(false);

        jLabel45.setText("Manufacturer");

        jLabel46.setText("Device Type");

        jLabel49.setText("Protocol & Parameters");

        irdbImportAllButton.setText("Load all");
        irdbImportAllButton.setToolTipText("Load for all parameters and protocols");
        irdbImportAllButton.setEnabled(false);
        irdbImportAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irdbImportAllButtonActionPerformed(evt);
            }
        });

        importIrdbHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        importIrdbHelpButton.setText("Help");
        importIrdbHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importIrdbHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout irdbPanelLayout = new javax.swing.GroupLayout(irdbPanel);
        irdbPanel.setLayout(irdbPanelLayout);
        irdbPanelLayout.setHorizontalGroup(
            irdbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irdbPanelLayout.createSequentialGroup()
                .addGroup(irdbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(irdbPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(irdbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(irdbManufacturerComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel45))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(irdbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(irdbDeviceTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 245, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel46))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(irdbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel49)
                            .addComponent(irdbCodeSetComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 354, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(irdbPanelLayout.createSequentialGroup()
                        .addComponent(irdbTreeImporter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(102, 102, 102)))
                .addGap(12, 12, 12)
                .addGroup(irdbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(importIrdbHelpButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irdbBrowseButton, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(irdbImportAllButton, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(irdbImportButton, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGap(7, 7, 7))
        );

        irdbPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {importIrdbHelpButton, irdbBrowseButton, irdbImportAllButton, irdbImportButton});

        irdbPanelLayout.setVerticalGroup(
            irdbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, irdbPanelLayout.createSequentialGroup()
                .addGap(75, 75, 75)
                .addComponent(irdbImportButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(irdbImportAllButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(irdbBrowseButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 99, Short.MAX_VALUE)
                .addComponent(importIrdbHelpButton)
                .addContainerGap())
            .addGroup(irdbPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(irdbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel45)
                    .addComponent(jLabel46)
                    .addComponent(jLabel49))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(irdbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(irdbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(irdbDeviceTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(irdbCodeSetComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(irdbManufacturerComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(irdbTreeImporter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(3, 3, 3))
        );

        irdbPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {importIrdbHelpButton, irdbBrowseButton, irdbImportAllButton, irdbImportButton});

        importTabbedPane.addTab("IRDB", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/database.png")), irdbPanel); // NOI18N

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
            .addComponent(girrFileImporterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 338, Short.MAX_VALUE)
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 124, Short.MAX_VALUE)
                .addGroup(lircImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(importLircHelpButton, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton20, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );

        lircImportPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {importLircHelpButton, jButton20});

        lircImportPanelLayout.setVerticalGroup(
            lircImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lircFileImporterBean, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 338, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, lircImportPanelLayout.createSequentialGroup()
                .addGap(53, 53, 53)
                .addComponent(jButton20)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(importLircHelpButton)
                .addGap(12, 12, 12))
        );

        importTabbedPane.addTab("LIRC", new javax.swing.ImageIcon(getClass().getResource("/icons/lirc/favicon-0.png")), lircImportPanel); // NOI18N

        webRmduButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/jp1/favicon-2.png"))); // NOI18N
        webRmduButton.setText("Web site");
        webRmduButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                webRmduButtonActionPerformed(evt);
            }
        });

        importRemoteMasterHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        importRemoteMasterHelpButton.setText("Help");
        importRemoteMasterHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importRemoteMasterHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout rmduImportPanelLayout = new javax.swing.GroupLayout(rmduImportPanel);
        rmduImportPanel.setLayout(rmduImportPanelLayout);
        rmduImportPanelLayout.setHorizontalGroup(
            rmduImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rmduImportPanelLayout.createSequentialGroup()
                .addComponent(rmduImporterBean, javax.swing.GroupLayout.PREFERRED_SIZE, 734, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 125, Short.MAX_VALUE)
                .addGroup(rmduImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(importRemoteMasterHelpButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(webRmduButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        rmduImportPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {importRemoteMasterHelpButton, webRmduButton});

        rmduImportPanelLayout.setVerticalGroup(
            rmduImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rmduImportPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(rmduImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(rmduImportPanelLayout.createSequentialGroup()
                        .addGap(56, 56, 56)
                        .addComponent(webRmduButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 206, Short.MAX_VALUE)
                        .addComponent(importRemoteMasterHelpButton)
                        .addContainerGap())
                    .addComponent(rmduImporterBean, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
        );

        rmduImportPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {importRemoteMasterHelpButton, webRmduButton});

        importTabbedPane.addTab("RemoteMaster", new javax.swing.ImageIcon(getClass().getResource("/icons/jp1/favicon-2.png")), rmduImportPanel); // NOI18N

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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 124, Short.MAX_VALUE)
                .addGroup(irtransImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(importIrTransHelpButton, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(irTransWebButton, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        irtransImportPanelLayout.setVerticalGroup(
            irtransImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(irTransFileImporterBean, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 338, Short.MAX_VALUE)
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 149, Short.MAX_VALUE)
                .addComponent(importProntoClassicHelpButton)
                .addContainerGap())
        );
        ccfImportPanelLayout.setVerticalGroup(
            ccfImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ccfImportPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(importProntoClassicHelpButton)
                .addContainerGap())
            .addComponent(ccfFileImporterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 338, Short.MAX_VALUE)
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 149, Short.MAX_VALUE)
                .addComponent(importProntoProfessionalHelpButton)
                .addContainerGap())
        );
        xcfImportPanelLayout.setVerticalGroup(
            xcfImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(xcfImportPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(importProntoProfessionalHelpButton)
                .addContainerGap())
            .addComponent(xcfFileImporterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 338, Short.MAX_VALUE)
        );

        importTabbedPane.addTab("Pronto Prof (.xcf)", xcfImportPanel);

        jLabel15.setText("Note: invokeRepeatFinder is used.");

        importIctHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        importIctHelpButton.setText("Help");
        importIctHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importIctHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout ictImportPanelLayout = new javax.swing.GroupLayout(ictImportPanel);
        ictImportPanel.setLayout(ictImportPanelLayout);
        ictImportPanelLayout.setHorizontalGroup(
            ictImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ictImportPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel15)
                .addContainerGap(730, Short.MAX_VALUE))
            .addGroup(ictImportPanelLayout.createSequentialGroup()
                .addComponent(ictFileImporterBean, javax.swing.GroupLayout.PREFERRED_SIZE, 734, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(importIctHelpButton)
                .addContainerGap())
        );
        ictImportPanelLayout.setVerticalGroup(
            ictImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ictImportPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jLabel15)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ictFileImporterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE))
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
                .addContainerGap(730, Short.MAX_VALUE))
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
                .addComponent(mode2FileImporterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 144, Short.MAX_VALUE)
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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 221, Short.MAX_VALUE)
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 144, Short.MAX_VALUE)
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
                .addComponent(rawLineCsvFileImporterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 299, Short.MAX_VALUE))
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

        fColumnComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "-1" }));
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

        jLabel23.setText("Number base");

        parametrizedMultiColumnNameCheckBox.setSelected(properties.getParametrizedNameMultiColumn());
        parametrizedMultiColumnNameCheckBox.setText("Multi col. name");
        parametrizedMultiColumnNameCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parametrizedMultiColumnNameCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout parametrizedCsvImportPanelLayout = new javax.swing.GroupLayout(parametrizedCsvImportPanel);
        parametrizedCsvImportPanel.setLayout(parametrizedCsvImportPanelLayout);
        parametrizedCsvImportPanelLayout.setHorizontalGroup(
            parametrizedCsvImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(parametrizedCsvImportPanelLayout.createSequentialGroup()
                .addGroup(parametrizedCsvImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
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
                                .addGap(54, 54, 54)
                                .addComponent(jLabel14))
                            .addGroup(parametrizedCsvImportPanelLayout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addComponent(sColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(fColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(parametrizedCsvImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(parametrizedCsvImportPanelLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(jLabel2))
                            .addGroup(parametrizedCsvImportPanelLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(parametrizedCsvSeparatorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(parametrizedCsvImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel23)
                            .addComponent(parametrizedBaseComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(parametrizedCsvImportPanelLayout.createSequentialGroup()
                        .addComponent(csvParametrizedFileImporterBean, javax.swing.GroupLayout.PREFERRED_SIZE, 734, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 144, Short.MAX_VALUE)
                        .addComponent(importTextParametrizedHelpButton)))
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
                    .addComponent(jLabel23))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(parametrizedCsvImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(parametrizedNameColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(protocolColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(dColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fColumnComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(parametrizedCsvSeparatorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(parametrizedMultiColumnNameCheckBox)
                    .addComponent(parametrizedBaseComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(parametrizedCsvImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(csvParametrizedFileImporterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, parametrizedCsvImportPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(importTextParametrizedHelpButton)
                        .addGap(12, 12, 12))))
        );

        parametrizedRawTabbedPane.addTab("Parametrized", parametrizedCsvImportPanel);

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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 149, Short.MAX_VALUE)
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
                .addContainerGap(294, Short.MAX_VALUE)
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 149, Short.MAX_VALUE)
                .addComponent(importCmlHelpButton)
                .addContainerGap())
        );
        cmlImportPanelLayout.setVerticalGroup(
            cmlImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cmlImportPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(importCmlHelpButton)
                .addContainerGap())
            .addComponent(cmlFileImporterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 338, Short.MAX_VALUE)
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 149, Short.MAX_VALUE)
                .addComponent(importCommandFusionHelpButton)
                .addContainerGap())
        );
        commandFusionImportPanelLayout.setVerticalGroup(
            commandFusionImportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(commandFusionImportPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(importCommandFusionHelpButton)
                .addContainerGap())
            .addComponent(commandFusionFileImporterBean, javax.swing.GroupLayout.DEFAULT_SIZE, 338, Short.MAX_VALUE)
        );

        importTabbedPane.addTab("CommandFusion", commandFusionImportPanel);

        importTabbedPane.setSelectedIndex(properties.getImportPaneSelectedIndex());

        javax.swing.GroupLayout importPanelLayout = new javax.swing.GroupLayout(importPanel);
        importPanel.setLayout(importPanelLayout);
        importPanelLayout.setHorizontalGroup(
            importPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, importPanelLayout.createSequentialGroup()
                .addComponent(importTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 992, Short.MAX_VALUE)
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

        exportGenerateRawCheckBox.setSelected(properties.getExportGenerateRaw());
        exportGenerateRawCheckBox.setText("Raw");
        exportGenerateRawCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportGenerateRawCheckBoxActionPerformed(evt);
            }
        });

        exportGenerateCcfCheckBox.setSelected(properties.getExportGenerateCcf());
        exportGenerateCcfCheckBox.setText("Pronto Hex");
        exportGenerateCcfCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportGenerateCcfCheckBoxActionPerformed(evt);
            }
        });

        exportGenerateParametersCheckBox.setSelected(properties.getExportGenerateParameters());
        exportGenerateParametersCheckBox.setText("Parameters");
        exportGenerateParametersCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportGenerateParametersCheckBoxActionPerformed(evt);
            }
        });

        exportSignalButton.setText("Export signal");
        exportSignalButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSignalButtonActionPerformed(evt);
            }
        });

        automaticExportFilenamesCheckBox.setSelected(properties.getExportAutomaticFilenames());
        automaticExportFilenamesCheckBox.setText("Automatic file names");
        automaticExportFilenamesCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                automaticExportFilenamesCheckBoxActionPerformed(evt);
            }
        });

        openLastFileButton.setText("Open last file");
        openLastFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openLastFileButtonActionPerformed(evt);
            }
        });

        exportRawRemoteButton.setText("Export raw remote");
        exportRawRemoteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportRawRemoteButtonActionPerformed(evt);
            }
        });

        exportSpecificOptionsTabbedPane.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        girrStylesheetUrlTextField.setText(properties.getGirrStyleSheetUrl());
        girrStylesheetUrlTextField.setComponentPopupMenu(copyPastePopupMenu);
        girrStylesheetUrlTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                girrStylesheetUrlTextFieldActionPerformed(evt);
            }
        });

        jLabel28.setText("Stylesheet URL");

        girrSchemaLinkCheckBox.setSelected(properties.getGirrCreateSchemaLocation());
        girrSchemaLinkCheckBox.setText("Schema link");
        girrSchemaLinkCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                girrSchemaLinkCheckBoxActionPerformed(evt);
            }
        });

        girrFatRawCheckBox.setSelected(properties.getGirrFatRaw());
        girrFatRawCheckBox.setText("Fat form raw");
        girrFatRawCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                girrFatRawCheckBoxActionPerformed(evt);
            }
        });

        girrStylesheetTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "xslt", "css" }));
        girrStylesheetTypeComboBox.setSelectedItem(properties.getGirrStyleSheetType());
        girrStylesheetTypeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                girrStylesheetTypeComboBoxActionPerformed(evt);
            }
        });

        jLabel33.setText("Stylesheet type");

        exportGirrHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        exportGirrHelpButton.setText("Help");
        exportGirrHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportGirrHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout girrExportOptionsPanelLayout = new javax.swing.GroupLayout(girrExportOptionsPanel);
        girrExportOptionsPanel.setLayout(girrExportOptionsPanelLayout);
        girrExportOptionsPanelLayout.setHorizontalGroup(
            girrExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(girrExportOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(girrExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(girrExportOptionsPanelLayout.createSequentialGroup()
                        .addComponent(jLabel28)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(girrStylesheetUrlTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 315, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 342, Short.MAX_VALUE)
                        .addComponent(jLabel33)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(girrStylesheetTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(girrExportOptionsPanelLayout.createSequentialGroup()
                        .addComponent(girrSchemaLinkCheckBox)
                        .addGap(132, 132, 132)
                        .addComponent(girrFatRawCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(exportGirrHelpButton)))
                .addContainerGap())
        );
        girrExportOptionsPanelLayout.setVerticalGroup(
            girrExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(girrExportOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(girrExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(girrStylesheetUrlTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel28)
                    .addComponent(girrStylesheetTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel33))
                .addGroup(girrExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(girrExportOptionsPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(girrExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(girrSchemaLinkCheckBox)
                            .addComponent(girrFatRawCheckBox)))
                    .addGroup(girrExportOptionsPanelLayout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addComponent(exportGirrHelpButton)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        exportSpecificOptionsTabbedPane.addTab("Girr", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/translate.png")), girrExportOptionsPanel); // NOI18N

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
                .addContainerGap(890, Short.MAX_VALUE)
                .addComponent(exportWaveHelpButton)
                .addContainerGap())
            .addGroup(waveExportOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(exportAudioParametersBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        waveExportOptionsPanelLayout.setVerticalGroup(
            waveExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(waveExportOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(exportAudioParametersBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(exportWaveHelpButton)
                .addContainerGap())
        );

        exportSpecificOptionsTabbedPane.addTab("Wave", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/mix_audio.png")), waveExportOptionsPanel); // NOI18N

        sendirModuleComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "4", "5" }));
        sendirModuleComboBox.setSelectedItem(Integer.toString(properties.getExportSendIrModule()));
        sendirModuleComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendirModuleComboBoxActionPerformed(evt);
            }
        });

        sendirConnectorComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3" }));
        sendirConnectorComboBox.setSelectedItem(Integer.toString(properties.getExportSendIrConnector()));
        sendirConnectorComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendirConnectorComboBoxActionPerformed(evt);
            }
        });

        sendirCompressedCheckBox.setSelected(properties.getExportSendIrCompressed());
        sendirCompressedCheckBox.setText("Compressed");
        sendirCompressedCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendirCompressedCheckBoxActionPerformed(evt);
            }
        });

        jLabel10.setText("Module");

        jLabel12.setText("Connector");

        exportSendirHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        exportSendirHelpButton.setText("Help");
        exportSendirHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSendirHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout sendirExportOptionsPanelLayout = new javax.swing.GroupLayout(sendirExportOptionsPanel);
        sendirExportOptionsPanel.setLayout(sendirExportOptionsPanelLayout);
        sendirExportOptionsPanelLayout.setHorizontalGroup(
            sendirExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sendirExportOptionsPanelLayout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addGroup(sendirExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sendirModuleComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(sendirExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(sendirExportOptionsPanelLayout.createSequentialGroup()
                        .addComponent(sendirConnectorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(sendirCompressedCheckBox))
                    .addComponent(jLabel12))
                .addContainerGap(709, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, sendirExportOptionsPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(exportSendirHelpButton)
                .addContainerGap())
        );
        sendirExportOptionsPanelLayout.setVerticalGroup(
            sendirExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sendirExportOptionsPanelLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(sendirExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(sendirExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sendirModuleComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sendirConnectorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sendirCompressedCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(exportSendirHelpButton)
                .addContainerGap())
        );

        exportSpecificOptionsTabbedPane.addTab("Sendir (GlobalCache)", sendirExportOptionsPanel);

        prontoModelComboBox.setModel(new DefaultComboBoxModel(prontoModelNames));
        prontoModelComboBox.setSelectedIndex(properties.getProntoModelIndex());
        prontoModelComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prontoModelComboBoxActionPerformed(evt);
            }
        });

        prontoExportScreenWidthTextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        prontoExportScreenWidthTextField.setText(Integer.toString(properties.getProntoScreenSizeX()));
        prontoExportScreenWidthTextField.setMinimumSize(new java.awt.Dimension(12, 40));
        prontoExportScreenWidthTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prontoExportScreenWidthTextFieldActionPerformed(evt);
            }
        });

        prontoExportScreenHeightTextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        prontoExportScreenHeightTextField.setText(Integer.toString(properties.getProntoScreenSizeY()));
        prontoExportScreenHeightTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prontoExportScreenHeightTextFieldActionPerformed(evt);
            }
        });

        prontoExportButtonWidthTextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        prontoExportButtonWidthTextField.setText(Integer.toString(properties.getProntoButtonSizeX()));
        prontoExportButtonWidthTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prontoExportButtonWidthTextFieldActionPerformed(evt);
            }
        });

        prontoExportButtonHeightTextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        prontoExportButtonHeightTextField.setText(Integer.toString(properties.getProntoButtonSizeY()));
        prontoExportButtonHeightTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prontoExportButtonHeightTextFieldActionPerformed(evt);
            }
        });

        jLabel22.setText("Pronto Model");

        jLabel27.setText("Screen size");

        jLabel31.setText("Button size");

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
                    .addGroup(prontoClassicExportOptionsPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(exportProntoHelpButton))
                    .addGroup(prontoClassicExportOptionsPanelLayout.createSequentialGroup()
                        .addComponent(jLabel22)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(prontoModelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(30, 30, 30)
                        .addComponent(jLabel27)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(prontoExportScreenWidthTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(prontoExportScreenHeightTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel31)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(prontoExportButtonWidthTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(prontoExportButtonHeightTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
        );

        prontoClassicExportOptionsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {prontoExportButtonWidthTextField, prontoExportScreenWidthTextField});

        prontoClassicExportOptionsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {prontoExportButtonHeightTextField, prontoExportScreenHeightTextField});

        prontoClassicExportOptionsPanelLayout.setVerticalGroup(
            prontoClassicExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(prontoClassicExportOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(prontoClassicExportOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22)
                    .addComponent(prontoModelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel27)
                    .addComponent(prontoExportScreenWidthTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(prontoExportScreenHeightTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel31)
                    .addComponent(prontoExportButtonWidthTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(prontoExportButtonHeightTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 85, Short.MAX_VALUE)
                .addComponent(exportProntoHelpButton)
                .addContainerGap())
        );

        prontoClassicExportOptionsPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {prontoExportButtonHeightTextField, prontoExportButtonWidthTextField, prontoExportScreenHeightTextField, prontoExportScreenWidthTextField});

        exportSpecificOptionsTabbedPane.addTab("Pronto Classic", prontoClassicExportOptionsPanel);

        exportParametricRemoteButton.setText("Export param. remote");
        exportParametricRemoteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportParametricRemoteButtonActionPerformed(evt);
            }
        });

        exportGenerateSendIrCheckBox.setSelected(properties.getExportGenerateSendIr());
        exportGenerateSendIrCheckBox.setText("sendir");
        exportGenerateSendIrCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportGenerateSendIrCheckBoxActionPerformed(evt);
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

        jLabel16.setText("# repeats");

        exportHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        exportHelpButton.setText("Help");
        exportHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportHelpButtonActionPerformed(evt);
            }
        });

        exportRawRemoteButton1.setText("Export generated");
        exportRawRemoteButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generateExportButtonActionPerformed(evt);
            }
        });

        exportGenerateShortCcfCheckBox.setSelected(properties.getExportGenerateShortCcf());
        exportGenerateShortCcfCheckBox.setText("short Pronto Hex");
        exportGenerateShortCcfCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportGenerateShortCcfCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout exportPanelLayout = new javax.swing.GroupLayout(exportPanel);
        exportPanel.setLayout(exportPanelLayout);
        exportPanelLayout.setHorizontalGroup(
            exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(exportPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(exportSpecificOptionsTabbedPane)
                    .addGroup(exportPanelLayout.createSequentialGroup()
                        .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(exportPanelLayout.createSequentialGroup()
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(exportDirectoryTextField)
                                .addGap(12, 12, 12))
                            .addGroup(exportPanelLayout.createSequentialGroup()
                                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(automaticExportFilenamesCheckBox)
                                    .addGroup(exportPanelLayout.createSequentialGroup()
                                        .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel8)
                                            .addComponent(exportFormatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGroup(exportPanelLayout.createSequentialGroup()
                                                .addComponent(jLabel16)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(exportRepeatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addGap(27, 27, 27)
                                        .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(exportPanelLayout.createSequentialGroup()
                                                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addComponent(exportGenerateParametersCheckBox)
                                                    .addComponent(exportGenerateCcfCheckBox))
                                                .addGap(36, 36, 36)
                                                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addComponent(exportGenerateSendIrCheckBox)
                                                    .addComponent(exportGenerateShortCcfCheckBox)))
                                            .addComponent(exportGenerateRawCheckBox)))
                                    .addGroup(exportPanelLayout.createSequentialGroup()
                                        .addComponent(exportSignalButton)
                                        .addGap(18, 18, 18)
                                        .addComponent(exportParametricRemoteButton)
                                        .addGap(18, 18, 18)
                                        .addComponent(exportRawRemoteButton)
                                        .addGap(18, 18, 18)
                                        .addComponent(exportRawRemoteButton1)
                                        .addGap(18, 18, 18)
                                        .addComponent(openLastFileButton)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 10, Short.MAX_VALUE)))
                        .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(exportPanelLayout.createSequentialGroup()
                                .addComponent(exportDirSelectButton)
                                .addGap(12, 12, 12)
                                .addComponent(exportDirOpenButton))
                            .addComponent(exportHelpButton, javax.swing.GroupLayout.Alignment.TRAILING)))))
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
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(exportPanelLayout.createSequentialGroup()
                        .addComponent(automaticExportFilenamesCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel8)
                            .addComponent(exportGenerateParametersCheckBox)))
                    .addComponent(exportHelpButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(exportFormatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(exportGenerateRawCheckBox)
                    .addComponent(exportGenerateSendIrCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(exportGenerateShortCcfCheckBox)
                    .addComponent(exportGenerateCcfCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(exportRepeatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(23, 23, 23)
                .addGroup(exportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(exportSignalButton)
                    .addComponent(exportParametricRemoteButton)
                    .addComponent(exportRawRemoteButton)
                    .addComponent(exportRawRemoteButton1)
                    .addComponent(openLastFileButton))
                .addGap(23, 23, 23)
                .addComponent(exportSpecificOptionsTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE)
                .addGap(0, 0, 0))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 38, Short.MAX_VALUE)
                .addComponent(sendingGlobalCacheHelpButton)
                .addContainerGap())
        );

        sendingHardwareTabbedPane.addTab("Global Caché", globalCachePanel);

        lircInternetHostPanel.setIpName(properties.getLircIpName());
        lircInternetHostPanel.setPortNumber(properties.getLircPort());

        sendingLircHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        sendingLircHelpButton.setText("Help");
        sendingLircHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendingLircHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout lircPanelLayout = new javax.swing.GroupLayout(lircPanel);
        lircPanel.setLayout(lircPanelLayout);
        lircPanelLayout.setHorizontalGroup(
            lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lircPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, lircPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(sendingLircHelpButton))
                    .addGroup(lircPanelLayout.createSequentialGroup()
                        .addGroup(lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lircNamedCommandLauncher, javax.swing.GroupLayout.PREFERRED_SIZE, 785, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lircInternetHostPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 770, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 34, Short.MAX_VALUE)))
                .addContainerGap())
        );
        lircPanelLayout.setVerticalGroup(
            lircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lircPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lircInternetHostPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(lircNamedCommandLauncher, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(sendingLircHelpButton)
                .addContainerGap())
        );

        sendingHardwareTabbedPane.addTab("Lirc", new javax.swing.ImageIcon(getClass().getResource("/icons/lirc/favicon-2.png")), lircPanel); // NOI18N

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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 61, Short.MAX_VALUE)
                .addComponent(sendingDevLircHardwareHelpButton)
                .addContainerGap())
        );

        sendingHardwareTabbedPane.addTab("/dev/lirc", new javax.swing.ImageIcon(getClass().getResource("/icons/lirc/favicon-2.png")), devLircPanel); // NOI18N

        irTransInternetHostPanel.setIpName(null);
        irTransInternetHostPanel.setPortNumber(IrTrans.portNumber);
        irTransInternetHostPanel.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                irTransInternetHostPanelPropertyChange(evt);
            }
        });

        sendingIrTransHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        sendingIrTransHelpButton.setText("Help");
        sendingIrTransHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendingIrTransHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout irTransPanelLayout = new javax.swing.GroupLayout(irTransPanel);
        irTransPanel.setLayout(irTransPanelLayout);
        irTransPanelLayout.setHorizontalGroup(
            irTransPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irTransPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(irTransPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, irTransPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(sendingIrTransHelpButton))
                    .addGroup(irTransPanelLayout.createSequentialGroup()
                        .addGroup(irTransPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(irTransNamedCommandLauncher, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(irTransInternetHostPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(0, 47, Short.MAX_VALUE)))
                .addContainerGap())
        );
        irTransPanelLayout.setVerticalGroup(
            irTransPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irTransPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(irTransInternetHostPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(irTransNamedCommandLauncher, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(sendingIrTransHelpButton)
                .addContainerGap())
        );

        sendingHardwareTabbedPane.addTab("IrTrans", new javax.swing.ImageIcon(getClass().getResource("/icons/irtrans/favicon.png")), irTransPanel); // NOI18N

        sendingAudioHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        sendingAudioHelpButton.setText("Help");
        sendingAudioHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendingAudioHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout audioPanelLayout = new javax.swing.GroupLayout(audioPanel);
        audioPanel.setLayout(audioPanelLayout);
        audioPanelLayout.setHorizontalGroup(
            audioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(audioPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(transmitAudioParametersBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(407, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, audioPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(sendingAudioHelpButton)
                .addContainerGap())
        );
        audioPanelLayout.setVerticalGroup(
            audioPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(audioPanelLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(transmitAudioParametersBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 65, Short.MAX_VALUE)
                .addComponent(sendingAudioHelpButton)
                .addContainerGap())
        );

        sendingHardwareTabbedPane.addTab("Audio Port", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/mix_audio.png")), audioPanel); // NOI18N

        sendingIrToyHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        sendingIrToyHelpButton.setText("Help");
        sendingIrToyHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendingIrToyHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout irToyPanelLayout = new javax.swing.GroupLayout(irToyPanel);
        irToyPanel.setLayout(irToyPanelLayout);
        irToyPanelLayout.setHorizontalGroup(
            irToyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, irToyPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(sendingIrToyHelpButton)
                .addContainerGap())
            .addGroup(irToyPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(irToySerialPortBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(261, Short.MAX_VALUE))
        );
        irToyPanelLayout.setVerticalGroup(
            irToyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irToyPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(irToySerialPortBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 61, Short.MAX_VALUE)
                .addComponent(sendingIrToyHelpButton)
                .addContainerGap())
        );

        sendingHardwareTabbedPane.addTab("IrToy", new javax.swing.ImageIcon(getClass().getResource("/icons/dangerousprototypes/favicon.png")), irToyPanel); // NOI18N

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

        commandFusionSendingSerialPortBean.setBaudRate(115200);

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
                .addComponent(commandFusionSendingSerialPortBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(31, Short.MAX_VALUE))
        );
        commandFusionSendPanelLayout.setVerticalGroup(
            commandFusionSendPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(commandFusionSendPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(commandFusionSendingSerialPortBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 61, Short.MAX_VALUE)
                .addComponent(sendingCommandFusionHelpButton)
                .addContainerGap())
        );

        sendingHardwareTabbedPane.addTab("CommandFusion", commandFusionSendPanel);

        sendingGenericSerialPortHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        sendingGenericSerialPortHelpButton.setText("Help");
        sendingGenericSerialPortHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendingGenericSerialPortHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout genericSerialPanelLayout = new javax.swing.GroupLayout(genericSerialPanel);
        genericSerialPanel.setLayout(genericSerialPanelLayout);
        genericSerialPanelLayout.setHorizontalGroup(
            genericSerialPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(genericSerialPanelLayout.createSequentialGroup()
                .addGroup(genericSerialPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(genericSerialPanelLayout.createSequentialGroup()
                        .addComponent(genericSerialSenderBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 42, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, genericSerialPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(sendingGenericSerialPortHelpButton)))
                .addContainerGap())
        );
        genericSerialPanelLayout.setVerticalGroup(
            genericSerialPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(genericSerialPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(genericSerialSenderBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(sendingGenericSerialPortHelpButton)
                .addContainerGap())
        );

        sendingHardwareTabbedPane.addTab("Generic serial port", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/filesystems/socket.png")), genericSerialPanel); // NOI18N

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
        transmitGenerateButton2.setText("Transmit generated");
        transmitGenerateButton2.setToolTipText("Transmit the signal defined on the \"Generate\" pane using the selected hardware.");
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

        javax.swing.GroupLayout sendingPanelLayout = new javax.swing.GroupLayout(sendingPanel);
        sendingPanel.setLayout(sendingPanelLayout);
        sendingPanelLayout.setHorizontalGroup(
            sendingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sendingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(transmitScrutinizedButton)
                .addGap(18, 18, 18)
                .addComponent(transmitGenerateButton2)
                .addGap(18, 18, 18)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(noTransmitsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(sendingPanelLayout.createSequentialGroup()
                .addGroup(sendingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(sendingPanelLayout.createSequentialGroup()
                        .addComponent(sendingHardwareTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 853, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 127, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, sendingPanelLayout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(sendingHardwareHelpButton)))
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
                    .addComponent(noTransmitsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 104, Short.MAX_VALUE)
                .addComponent(sendingHardwareHelpButton)
                .addContainerGap())
        );

        topLevelTabbedPane.addTab("Sending hw", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/hardware.png")), sendingPanel); // NOI18N

        capturingHardwareTabbedPane.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        capturingHardwareTabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                capturingHardwareTabbedPaneStateChanged(evt);
            }
        });

        capturingIrWidgetHardwareHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        capturingIrWidgetHardwareHelpButton.setText("Help");
        capturingIrWidgetHardwareHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                capturingIrWidgetHardwareHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout captureIrWidgetPanelLayout = new javax.swing.GroupLayout(captureIrWidgetPanel);
        captureIrWidgetPanel.setLayout(captureIrWidgetPanelLayout);
        captureIrWidgetPanelLayout.setHorizontalGroup(
            captureIrWidgetPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(captureIrWidgetPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(irWidgetSerialPortSimpleBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, captureIrWidgetPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(capturingIrWidgetHardwareHelpButton)
                .addContainerGap())
        );
        captureIrWidgetPanelLayout.setVerticalGroup(
            captureIrWidgetPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(captureIrWidgetPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(irWidgetSerialPortSimpleBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 67, Short.MAX_VALUE)
                .addComponent(capturingIrWidgetHardwareHelpButton)
                .addContainerGap())
        );

        capturingHardwareTabbedPane.addTab("IrWidget", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/usb.png")), captureIrWidgetPanel); // NOI18N

        capturingGlobalCacheHardwareHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        capturingGlobalCacheHardwareHelpButton.setText("Help");
        capturingGlobalCacheHardwareHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                capturingGlobalCacheHardwareHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout captureGlobalCachePanelLayout = new javax.swing.GroupLayout(captureGlobalCachePanel);
        captureGlobalCachePanel.setLayout(captureGlobalCachePanelLayout);
        captureGlobalCachePanelLayout.setHorizontalGroup(
            captureGlobalCachePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(captureGlobalCachePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(globalCacheCaptureSelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, captureGlobalCachePanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(capturingGlobalCacheHardwareHelpButton)
                .addContainerGap())
        );
        captureGlobalCachePanelLayout.setVerticalGroup(
            captureGlobalCachePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(captureGlobalCachePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(globalCacheCaptureSelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 44, Short.MAX_VALUE)
                .addComponent(capturingGlobalCacheHardwareHelpButton)
                .addContainerGap())
        );

        capturingHardwareTabbedPane.addTab("Global Caché", captureGlobalCachePanel);

        startMode2Button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/connect_creating.png"))); // NOI18N
        startMode2Button.setMnemonic('S');
        startMode2Button.setText("Start");
        startMode2Button.setToolTipText("Start command above in an internal process.");
        startMode2Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startMode2ButtonActionPerformed(evt);
            }
        });

        stopMode2Button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/stop.png"))); // NOI18N
        stopMode2Button.setMnemonic('O');
        stopMode2Button.setText("Stop");
        stopMode2Button.setToolTipText("Terminate the internal process.");
        stopMode2Button.setEnabled(false);
        stopMode2Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopMode2ButtonActionPerformed(evt);
            }
        });

        capturingMode2HardwareHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        capturingMode2HardwareHelpButton.setText("Help");
        capturingMode2HardwareHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                capturingMode2HardwareHelpButtonActionPerformed(evt);
            }
        });

        mode2TabbedPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                mode2TabbedPaneStateChanged(evt);
            }
        });

        javax.swing.GroupLayout mode2StdinPaneLayout = new javax.swing.GroupLayout(mode2StdinPane);
        mode2StdinPane.setLayout(mode2StdinPaneLayout);
        mode2StdinPaneLayout.setHorizontalGroup(
            mode2StdinPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        mode2StdinPaneLayout.setVerticalGroup(
            mode2StdinPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        mode2TabbedPane.addTab("Standard Input", mode2StdinPane);

        lircMode2CommandTextField.setText(properties.getLircMode2Command());

        mode2CommandLabel.setText("Mode 2 command");

        javax.swing.GroupLayout mode2SubprocessPaneLayout = new javax.swing.GroupLayout(mode2SubprocessPane);
        mode2SubprocessPane.setLayout(mode2SubprocessPaneLayout);
        mode2SubprocessPaneLayout.setHorizontalGroup(
            mode2SubprocessPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mode2SubprocessPaneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mode2SubprocessPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mode2CommandLabel)
                    .addComponent(lircMode2CommandTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 589, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        mode2SubprocessPaneLayout.setVerticalGroup(
            mode2SubprocessPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mode2SubprocessPaneLayout.createSequentialGroup()
                .addContainerGap(13, Short.MAX_VALUE)
                .addComponent(mode2CommandLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lircMode2CommandTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(47, 47, 47))
        );

        mode2TabbedPane.addTab("Command in Sub-process", mode2SubprocessPane);

        mode2TabbedPane.setSelectedIndex(properties.getLircMode2UseStdin() ? 0 : 1);

        jLabel24.setText("Consider using");

        jLabel32.setText("/dev/lirc instead!");

        javax.swing.GroupLayout captureLircMode2PanelLayout = new javax.swing.GroupLayout(captureLircMode2Panel);
        captureLircMode2Panel.setLayout(captureLircMode2PanelLayout);
        captureLircMode2PanelLayout.setHorizontalGroup(
            captureLircMode2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(captureLircMode2PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(captureLircMode2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, captureLircMode2PanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(capturingMode2HardwareHelpButton))
                    .addGroup(captureLircMode2PanelLayout.createSequentialGroup()
                        .addComponent(startMode2Button)
                        .addGap(18, 18, 18)
                        .addComponent(stopMode2Button)
                        .addGap(200, 611, Short.MAX_VALUE))
                    .addGroup(captureLircMode2PanelLayout.createSequentialGroup()
                        .addComponent(mode2TabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(captureLircMode2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel24)
                            .addComponent(jLabel32))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        captureLircMode2PanelLayout.setVerticalGroup(
            captureLircMode2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(captureLircMode2PanelLayout.createSequentialGroup()
                .addGroup(captureLircMode2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(captureLircMode2PanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(mode2TabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(captureLircMode2PanelLayout.createSequentialGroup()
                        .addGap(58, 58, 58)
                        .addComponent(jLabel24)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel32)))
                .addGroup(captureLircMode2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(captureLircMode2PanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(capturingMode2HardwareHelpButton)
                        .addContainerGap())
                    .addGroup(captureLircMode2PanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(captureLircMode2PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(startMode2Button)
                            .addComponent(stopMode2Button))
                        .addContainerGap(35, Short.MAX_VALUE))))
        );

        capturingHardwareTabbedPane.addTab("LIRC Mode 2", new javax.swing.ImageIcon(getClass().getResource("/icons/lirc/favicon-2.png")), captureLircMode2Panel, ""); // NOI18N

        capturingDevLircHardwareHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        capturingDevLircHardwareHelpButton.setText("Help");
        capturingDevLircHardwareHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                capturingDevLircHardwareHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout captureDevLircPanelLayout = new javax.swing.GroupLayout(captureDevLircPanel);
        captureDevLircPanel.setLayout(captureDevLircPanelLayout);
        captureDevLircPanelLayout.setHorizontalGroup(
            captureDevLircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, captureDevLircPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(capturingDevLircHardwareHelpButton)
                .addContainerGap())
            .addGroup(captureDevLircPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(devLircCapturingSendingBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        captureDevLircPanelLayout.setVerticalGroup(
            captureDevLircPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(captureDevLircPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(devLircCapturingSendingBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 21, Short.MAX_VALUE)
                .addComponent(capturingDevLircHardwareHelpButton)
                .addContainerGap())
        );

        capturingHardwareTabbedPane.addTab("/dev/lirc", new javax.swing.ImageIcon(getClass().getResource("/icons/lirc/favicon-2.png")), captureDevLircPanel); // NOI18N

        capturingIrToyHardwareHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        capturingIrToyHardwareHelpButton.setText("Help");
        capturingIrToyHardwareHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                capturingIrToyHardwareHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout captureIrToyPanelLayout = new javax.swing.GroupLayout(captureIrToyPanel);
        captureIrToyPanel.setLayout(captureIrToyPanelLayout);
        captureIrToyPanelLayout.setHorizontalGroup(
            captureIrToyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, captureIrToyPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(capturingIrToyHardwareHelpButton)
                .addContainerGap())
            .addGroup(captureIrToyPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(irtoyCapturingSendingBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        captureIrToyPanelLayout.setVerticalGroup(
            captureIrToyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(captureIrToyPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(irtoyCapturingSendingBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 21, Short.MAX_VALUE)
                .addComponent(capturingIrToyHardwareHelpButton)
                .addContainerGap())
        );

        capturingHardwareTabbedPane.addTab("IrToy", new javax.swing.ImageIcon(getClass().getResource("/icons/dangerousprototypes/favicon.png")), captureIrToyPanel); // NOI18N

        capturingGirsHardwareHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        capturingGirsHardwareHelpButton.setText("Help");
        capturingGirsHardwareHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                capturingGirsHardwareHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout captureGirsPanelLayout = new javax.swing.GroupLayout(captureGirsPanel);
        captureGirsPanel.setLayout(captureGirsPanelLayout);
        captureGirsPanelLayout.setHorizontalGroup(
            captureGirsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, captureGirsPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(capturingGirsHardwareHelpButton)
                .addContainerGap())
            .addGroup(captureGirsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(girsClientCapturingSendingBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        captureGirsPanelLayout.setVerticalGroup(
            captureGirsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(captureGirsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(girsClientCapturingSendingBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 21, Short.MAX_VALUE)
                .addComponent(capturingGirsHardwareHelpButton)
                .addContainerGap())
        );

        capturingHardwareTabbedPane.addTab("Girs Client", captureGirsPanel);

        capturingCommandFusionHardwareHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        capturingCommandFusionHardwareHelpButton.setText("Help");
        capturingCommandFusionHardwareHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                capturingCommandFusionHardwareHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout captureCommandFusionPanelLayout = new javax.swing.GroupLayout(captureCommandFusionPanel);
        captureCommandFusionPanel.setLayout(captureCommandFusionPanelLayout);
        captureCommandFusionPanelLayout.setHorizontalGroup(
            captureCommandFusionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, captureCommandFusionPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(capturingCommandFusionHardwareHelpButton)
                .addContainerGap())
            .addGroup(captureCommandFusionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(commandFusionCapturingSendingBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        captureCommandFusionPanelLayout.setVerticalGroup(
            captureCommandFusionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(captureCommandFusionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(commandFusionCapturingSendingBean, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 21, Short.MAX_VALUE)
                .addComponent(capturingCommandFusionHardwareHelpButton)
                .addContainerGap())
        );

        capturingHardwareTabbedPane.addTab("CommandFusion", captureCommandFusionPanel);

        captureTestButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/mix_record.png"))); // NOI18N
        captureTestButton.setText("Test");
        captureTestButton.setToolTipText("For testing the setup only.  Use \"Scrutinize signal\" or \"Scrutinize remote\" for deployment.");
        captureTestButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                captureTestButtonActionPerformed(evt);
            }
        });

        capturingHardwareHelpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/help.png"))); // NOI18N
        capturingHardwareHelpButton.setText("Help");
        capturingHardwareHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                capturingHardwareHelpButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout capturingPanelLayout = new javax.swing.GroupLayout(capturingPanel);
        capturingPanel.setLayout(capturingPanelLayout);
        capturingPanelLayout.setHorizontalGroup(
            capturingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(capturingPanelLayout.createSequentialGroup()
                .addGroup(capturingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, capturingPanelLayout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(capturingHardwareHelpButton))
                    .addGroup(capturingPanelLayout.createSequentialGroup()
                        .addGroup(capturingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(capturingHardwareTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 853, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(capturingPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(captureTestButton, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 127, Short.MAX_VALUE)))
                .addContainerGap())
        );
        capturingPanelLayout.setVerticalGroup(
            capturingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(capturingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(capturingHardwareTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 257, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(captureTestButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 128, Short.MAX_VALUE)
                .addComponent(capturingHardwareHelpButton)
                .addContainerGap())
        );

        topLevelTabbedPane.addTab("Capturing hw", new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/mix_microphone.png")), capturingPanel); // NOI18N

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

        importRmduMenuItem1.setText("RMDU (incomplete)");
        importRmduMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importRmduMenuItem1ActionPerformed(evt);
            }
        });
        importParametricMenu.add(importRmduMenuItem1);

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

        importRmduMenuItem.setText("RMDU (incomplete)");
        importRmduMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importRmduMenuItemActionPerformed(evt);
            }
        });
        importRawMenu.add(importRmduMenuItem);

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

        exitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0));
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

        undoMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
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

        clearSignalMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
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

        jumpToLastPanelMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, java.awt.event.InputEvent.ALT_MASK));
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
        optionsMenu.add(jSeparator3);

        outputFormatMenu.setText("Output Text Format");
        outputFormatMenu.setToolTipText("The text format for writing IR signals in the data window.");

        ccfRadioButtonMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.ALT_MASK));
        ccfRadioButtonMenuItem.setSelected(true);
        ccfRadioButtonMenuItem.setText("Pronto Hex");
        ccfRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ccfRadioButtonMenuItemActionPerformed(evt);
            }
        });
        outputFormatMenu.add(ccfRadioButtonMenuItem);

        rawRadioButtonMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.ALT_MASK));
        rawRadioButtonMenuItem.setText("Raw (with signs)");
        rawRadioButtonMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rawRadioButtonMenuItemActionPerformed(evt);
            }
        });
        outputFormatMenu.add(rawRadioButtonMenuItem);

        rawWithoutSignsRadioButtonMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.SHIFT_MASK));
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

        startTimeoutMenuItem.setText("Start capture...");
        startTimeoutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startTimeoutMenuItemActionPerformed(evt);
            }
        });
        timeoutMenu.add(startTimeoutMenuItem);

        lengthMenuItem.setText("Max capture length...");
        lengthMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lengthMenuItemActionPerformed(evt);
            }
        });
        timeoutMenu.add(lengthMenuItem);

        endingTimeoutMenuItem.setText("Ending silence...");
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

        lircTimeoutMenuItem.setText("Lirc timeout");
        lircTimeoutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lircTimeoutMenuItemActionPerformed(evt);
            }
        });
        timeoutMenu.add(lircTimeoutMenuItem);

        optionsMenu.add(timeoutMenu);

        protocolParametersMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/package_settings.png"))); // NOI18N
        protocolParametersMenu.setText("Protocol Parameters");
        protocolParametersMenu.setToolTipText("Parameters for IrpTransmogrifier (see IrpTransmogrifier for documentation).");

        absToleranceMenuItem.setText("Absolute tolerance (ms)");
        absToleranceMenuItem.setToolTipText("Absolute tolerance for duration comparisons");
        absToleranceMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                absToleranceMenuItemActionPerformed(evt);
            }
        });
        protocolParametersMenu.add(absToleranceMenuItem);

        relToleranceMenuItem.setText("Relative tolerance (1)");
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

        minLeadoutMenuItem.setText("Min. Leadout (ms)");
        minLeadoutMenuItem.setToolTipText("Threshold value for lead-out in microseconds.");
        minLeadoutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                minLeadoutMenuItemActionPerformed(evt);
            }
        });
        protocolParametersMenu.add(minLeadoutMenuItem);

        minRepeatGapMenuItem.setText("Min. Repeatgap (ms)");
        minRepeatGapMenuItem.setToolTipText("Minumal value in micro seconds to be considered as ending a repeat.");
        minRepeatGapMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                minRepeatGapMenuItemActionPerformed(evt);
            }
        });
        protocolParametersMenu.add(minRepeatGapMenuItem);

        dummyGapMenuItem.setText("Dummy Gap (ms)");
        dummyGapMenuItem.setToolTipText("Dummy gap added for sequences with an odd number of durations.");
        dummyGapMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dummyGapMenuItemActionPerformed(evt);
            }
        });
        protocolParametersMenu.add(dummyGapMenuItem);

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

        parametrizedLearnIgnoreTCheckBoxMenuItem.setSelected(properties.getParametrizedLearnIgnoreT());
        parametrizedLearnIgnoreTCheckBoxMenuItem.setText("Ignore T on parametric learns");
        parametrizedLearnIgnoreTCheckBoxMenuItem.setToolTipText("If selected, the value of the T variable (if present) will discarded on captured variables.");
        parametrizedLearnIgnoreTCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parametrizedLearnIgnoreTCheckBoxMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(parametrizedLearnIgnoreTCheckBoxMenuItem);

        proxyMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/Network Connection Manager.png"))); // NOI18N
        proxyMenuItem.setText("Proxy Configuration ...");
        proxyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proxyMenuItemActionPerformed(evt);
            }
        });
        optionsMenu.add(proxyMenuItem);

        irpProtocolsIniMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/apps/database.png"))); // NOI18N
        irpProtocolsIniMenu.setText("IRP protocol database");

        irpProtocolsEditMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/edit.png"))); // NOI18N
        irpProtocolsEditMenuItem.setText("Open...");
        irpProtocolsEditMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irpProtocolsEditMenuItemActionPerformed(evt);
            }
        });
        irpProtocolsIniMenu.add(irpProtocolsEditMenuItem);

        irpProtocolsSelectMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/fileopen.png"))); // NOI18N
        irpProtocolsSelectMenuItem.setText("Select...");
        irpProtocolsSelectMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irpProtocolsSelectMenuItemActionPerformed(evt);
            }
        });
        irpProtocolsIniMenu.add(irpProtocolsSelectMenuItem);

        irpFormatsIniReloadMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/reload.png"))); // NOI18N
        irpFormatsIniReloadMenuItem.setText("Reload");
        irpFormatsIniReloadMenuItem.setToolTipText("Reload data base (not yet implemented)");
        irpFormatsIniReloadMenuItem.setEnabled(false);
        irpProtocolsIniMenu.add(irpFormatsIniReloadMenuItem);

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

        rejectLircCodeImports.setSelected(properties.getRejectLircCodeImports());
        rejectLircCodeImports.setText("Reject Lirc Imports without timings");
        rejectLircCodeImports.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rejectLircCodeImportsActionPerformed(evt);
            }
        });
        importOptionsMenu.add(rejectLircCodeImports);

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
        toolsMenu.add(jSeparator1);

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

        feedbackMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/mail_generic2.png"))); // NOI18N
        feedbackMenuItem.setMnemonic('H');
        feedbackMenuItem.setText("Send feedback with Email");
        feedbackMenuItem.setToolTipText("Start the mail program for sendiing feedback.");
        feedbackMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                feedbackMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(feedbackMenuItem);

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
        guiUtils.checkUpToDate(Version.currentVersionUrl, Version.versionString, proxy);
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
            Integer t = guiUtils.getIntegerInput("Require ending silence in milliseconds", properties.getCaptureEndingTimeout());
            if (t != null)
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
        try {
            IrSignal irSignal = Pronto.parse(testSignalCcf);
            if (rawPanel.isShowing()) {
                RawIrSignal cir = new RawIrSignal(irSignal, "test_signal", "Generated signal (NEC1 12.34 56)");
                registerRawCommand(cir);
            } else if (cookedPanel.isShowing()) {
                ParametrizedIrSignal signal = new ParametrizedIrSignal(irSignal, "test_signal", "Generated signal NEC 12.34 56", false);
                registerParameterSignal(signal);
            } else
                scrutinizeIrSignal(irSignal);
        } catch (InvalidArgumentException | Pronto.NonProntoFormatException | NoDecodeException ex) {
            throw new ThisCannotHappenException();
        }
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
            ICommandExporter exporter = newExporter("ICT");
            saveSignal(getCapturedIrSignal(), exporter);
        } catch (InvalidArgumentException ex) {
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
                    : clazz == Integer.class ? Integer.parseInt(str)
                    : clazz == Boolean.class ? Boolean.parseBoolean(str)
                    : str;

            tableModel.setValueAt(thing, r, c);
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

    private void clearMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearMenuItemActionPerformed
        tableUtils.clearTableConfirm(rawTable);
    }//GEN-LAST:event_clearMenuItemActionPerformed

    private void moveUpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveUpMenuItemActionPerformed
       tableUtils.tableMoveSelection(rawTable, true);
    }//GEN-LAST:event_moveUpMenuItemActionPerformed

    private void moveDownMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveDownMenuItemActionPerformed
        tableUtils.tableMoveSelection(rawTable, false);
    }//GEN-LAST:event_moveDownMenuItemActionPerformed

    private void exportSignalGirrMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSignalGirrMenuItemActionPerformed
        try {
            saveSignal(getCapturedIrSignal(), newGirrExporter());
        } catch (InvalidArgumentException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_exportSignalGirrMenuItemActionPerformed

    private void clearSignalMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearSignalMenuItemActionPerformed
        clearSignal();
    }//GEN-LAST:event_clearSignalMenuItemActionPerformed

    private void startStopToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startStopToggleButtonActionPerformed
        if (!capturingHardwareManager.isReady()) {
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
        }
        topLevelTabbedPane.setEnabled(! startStopToggleButton.isSelected());
        jumpToLastPanelMenuItem.setEnabled(! startStopToggleButton.isSelected());
    }//GEN-LAST:event_startStopToggleButtonActionPerformed

    private void debugTableRowMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debugTableRowMenuItemActionPerformed
        try {
            tableUtils.printTableSelectedRow(rawTable);
        } catch (ErroneousSelectionException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_debugTableRowMenuItemActionPerformed

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
        tableUtils.tableMoveSelection(parameterTable, true);
    }//GEN-LAST:event_moveUpMenuItem1ActionPerformed

    private void moveDownMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveDownMenuItem1ActionPerformed
        tableUtils.tableMoveSelection(parameterTable, false);
    }//GEN-LAST:event_moveDownMenuItem1ActionPerformed

    private void deleteMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteMenuItem1ActionPerformed
        try {
            tableUtils.deleteTableSelectedRows(parameterTable);
        } catch (ErroneousSelectionException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_deleteMenuItem1ActionPerformed

    private void debugTableRowMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debugTableRowMenuItem1ActionPerformed
        try {
            tableUtils.printTableSelectedRow(parameterTable);
        } catch (ErroneousSelectionException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_debugTableRowMenuItem1ActionPerformed

    private void clearMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearMenuItem1ActionPerformed
        tableUtils.clearTableConfirm(parameterTable);
    }//GEN-LAST:event_clearMenuItem1ActionPerformed

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
        RemoteSetExporter exporter = newRemoteExporter();
        if (exporter != null) {
            if (rawCookedTabbedPane.getSelectedComponent() == this.cookedPanel)
                saveParametricSignals(exporter);
            else
                saveRawSignals(exporter);
        } else {
            // error has already been reported, nothing to do.
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

    private void globalCacheDBBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_globalCacheDBBrowseButtonActionPerformed
        try {
            guiUtils.browse(new URI("http", GlobalCacheIrDatabase.globalCacheIrDatabaseHost, null));
        } catch (URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_globalCacheDBBrowseButtonActionPerformed

    private void gcdbManufacturerComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gcdbManufacturerComboBoxActionPerformed
        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
            if (globalCacheIrDatabase == null) {
                globalCacheIrDatabase = new GlobalCacheIrDatabase(properties.getGlobalCacheApiKey(), properties.getVerbose());
                Collection<String> manufacturers = globalCacheIrDatabase.getManufacturers();
                String[] arr = manufacturers.toArray(new String[manufacturers.size()]);
                Arrays.sort(arr);
                DefaultComboBoxModel dcbm = new DefaultComboBoxModel(arr);
                gcdbManufacturerComboBox.setModel(dcbm);
                gcdbManufacturerComboBoxActionPerformed(null);
            } else {
                String manufacturer = (String) gcdbManufacturerComboBox.getSelectedItem();
                Collection<String> devTypes = globalCacheIrDatabase.getDeviceTypes(manufacturer);
                String[] arr = devTypes.toArray(new String[devTypes.size()]);
                Arrays.sort(arr);
                DefaultComboBoxModel dcbm = new DefaultComboBoxModel(arr);
                gcdbDeviceTypeComboBox.setModel(dcbm);
                gcdbDeviceTypeComboBoxActionPerformed(null);
                gcdbDeviceTypeComboBox.setEnabled(true);
            }
            gcdbTreeImporter.clear();
        } catch (IOException ex) {
            guiUtils.error(ex);
        } finally {
            busyWindow.unBusy();
        }
    }//GEN-LAST:event_gcdbManufacturerComboBoxActionPerformed

    private void gcdbDeviceTypeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gcdbDeviceTypeComboBoxActionPerformed
        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
            String manufacturer = (String) gcdbManufacturerComboBox.getSelectedItem();
            String deviceType = (String) gcdbDeviceTypeComboBox.getSelectedItem();
            Collection<String> codeSets = globalCacheIrDatabase.getCodeset(manufacturer, deviceType);
            String[] arr = codeSets.toArray(new String[codeSets.size()]);
            Arrays.sort(arr);
            DefaultComboBoxModel dcbm = new DefaultComboBoxModel(arr);
            gcdbCodeSetComboBox.setModel(dcbm);
            gcdbCodeSetComboBox.setEnabled(true);
            gcdbImportButton.setEnabled(true);
        } catch (IOException ex) {
            guiUtils.error(ex);
        } finally {
            busyWindow.unBusy();
        }
    }//GEN-LAST:event_gcdbDeviceTypeComboBoxActionPerformed

    private void gcdbImportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gcdbImportButtonActionPerformed
        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
            String manufacturer = (String) gcdbManufacturerComboBox.getSelectedItem();
            String deviceType = (String) gcdbDeviceTypeComboBox.getSelectedItem();
            String codeSet = (String) gcdbCodeSetComboBox.getSelectedItem();
            globalCacheIrDatabase.load(manufacturer, deviceType, codeSet);
            gcdbTreeImporter.setRemoteSet(globalCacheIrDatabase.getRemoteSet());
        } catch (IOException | InvalidArgumentException ex) {
            guiUtils.error(ex);
        } finally {
            busyWindow.unBusy();
        }
    }//GEN-LAST:event_gcdbImportButtonActionPerformed

    private void irdbBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irdbBrowseButtonActionPerformed
        guiUtils.browse(IrdbImporter.getHomeUri());
    }//GEN-LAST:event_irdbBrowseButtonActionPerformed

    private void irdbManufacturerComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irdbManufacturerComboBoxActionPerformed
        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
            if (irdbManufacturerComboBox.getModel().getSize() == 1) {
                String[] manufacturers = IrdbImporter.getManufacturers(properties.getVerbose());
                Arrays.sort(manufacturers);
                DefaultComboBoxModel<String> dcbm = new DefaultComboBoxModel<>(manufacturers);
                irdbManufacturerComboBox.setModel(dcbm);
                irdbManufacturerComboBox.setSelectedIndex(0);
                if (irdbManufacturerComboBox.getModel().getSize() != 1)
                    irdbManufacturerComboBoxActionPerformed(null);
            } else {
                String manufacturer = (String) irdbManufacturerComboBox.getSelectedItem();
                irdbImporter = new IrdbImporter(manufacturer, properties.getVerbose());
                irdbTreeImporter.clear();
                Set<String> arr = irdbImporter.getDeviceTypes();
                DefaultComboBoxModel<String> dcbm = new DefaultComboBoxModel<>(arr.toArray(new String[arr.size()]));
                irdbDeviceTypeComboBox.setModel(dcbm);
                irdbDeviceTypeComboBox.setEnabled(true);
                irdbDeviceTypeComboBoxActionPerformed(null);
                irdbImportButton.setEnabled(true);
                irdbImportAllButton.setEnabled(true);
            }
        } catch (IOException ex) {
            guiUtils.error(ex);
        } finally {
            busyWindow.unBusy();
        }
    }//GEN-LAST:event_irdbManufacturerComboBoxActionPerformed

    private void irdbDeviceTypeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irdbDeviceTypeComboBoxActionPerformed
        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        String deviceType = (String) irdbDeviceTypeComboBox.getSelectedItem();
        Set<IrdbImporter.ProtocolDeviceSubdevice> pdss = irdbImporter.getProtocolDeviceSubdevice(deviceType);
        DefaultComboBoxModel dcbm = new DefaultComboBoxModel(pdss.toArray(new IrdbImporter.ProtocolDeviceSubdevice[pdss.size()]));
        irdbCodeSetComboBox.setModel(dcbm);
        irdbCodeSetComboBox.setEnabled(true);
        irdbTreeImporter.clear();
        busyWindow.unBusy();
    }//GEN-LAST:event_irdbDeviceTypeComboBoxActionPerformed

    private void irdbImportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irdbImportButtonActionPerformed
        String deviceType = (String) irdbDeviceTypeComboBox.getSelectedItem();
        IrdbImporter.ProtocolDeviceSubdevice pds = (IrdbImporter.ProtocolDeviceSubdevice) irdbCodeSetComboBox.getSelectedItem();
        try {
            irdbImporter.load(deviceType, pds);
            irdbTreeImporter.setRemoteSet(irdbImporter.getRemoteSet());
        } catch (Exception ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_irdbImportButtonActionPerformed

    private void importIctMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importIctMenuItemActionPerformed
        importRemoteByFileSelector(ictImporter, true);
    }//GEN-LAST:event_importIctMenuItemActionPerformed

    private void exportParametricAsGirrMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportParametricAsGirrMenuItemActionPerformed
        saveParametricSignals(newGirrExporter());
    }//GEN-LAST:event_exportParametricAsGirrMenuItemActionPerformed

    private void saveSelectedCookedMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveSelectedCookedMenuItemActionPerformed
        saveSelectedSignal(parameterTable, "IrScrutinizer parametrized signal");
    }//GEN-LAST:event_saveSelectedCookedMenuItemActionPerformed

    private void saveSelectedRawTableRowMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveSelectedRawTableRowMenuItemActionPerformed
        saveSelectedSignal(rawTable, "IrScrutinizer raw signal");
    }//GEN-LAST:event_saveSelectedRawTableRowMenuItemActionPerformed

    private void saveRawMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveRawMenuItemActionPerformed
        saveRawSignals(newRemoteExporter());
    }//GEN-LAST:event_saveRawMenuItemActionPerformed

    private void saveCookedMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveCookedMenuItemActionPerformed
        saveParametricSignals(newRemoteExporter());
    }//GEN-LAST:event_saveCookedMenuItemActionPerformed

    private void addEmptyParametrizedSignalMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addEmptyParametrizedSignalMenuItemActionPerformed
        ParametrizedIrSignal signal = new ParametrizedIrSignal(null, IrCoreUtils.INVALID, IrCoreUtils.INVALID, IrCoreUtils.INVALID, "empty_signal", null);
        registerParameterSignal(signal);
    }//GEN-LAST:event_addEmptyParametrizedSignalMenuItemActionPerformed

    private void rawFromClipboardMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawFromClipboardMenuItemActionPerformed
        String text = (new CopyClipboardText(null)).fromClipboard();
        try {
            IrSignal irSignal = InterpretString.interpretString(text, properties.getFallbackFrequency(), properties.getDummyGap(),
                    properties.getInvokeRepeatFinder(), properties.getInvokeCleaner(),
                    properties.getAbsoluteTolerance(), properties.getRelativeTolerance(), properties.getMinRepeatLastGap());
            RawIrSignal rawIrSignal = new RawIrSignal(irSignal, "clipboard", "Signal read from clipboard");
            registerRawCommand(rawIrSignal);
        } catch (InvalidArgumentException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_rawFromClipboardMenuItemActionPerformed

    private void exportRawAsGirrMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportRawAsGirrMenuItemActionPerformed
        saveRawSignals(newGirrExporter());
    }//GEN-LAST:event_exportRawAsGirrMenuItemActionPerformed

    private void repeatFinderCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_repeatFinderCheckBoxMenuItemActionPerformed
        properties.setInvokeRepeatFinder(repeatFinderCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_repeatFinderCheckBoxMenuItemActionPerformed

    private void importRmduMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importRmduMenuItemActionPerformed
        importRemoteByFileSelector(xcfImporter, true);
    }//GEN-LAST:event_importRmduMenuItemActionPerformed

    private void setProtocolMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setProtocolMenuItemActionPerformed
        String newProtocol = guiUtils.getInput("Enter new protocol", "Protocol request", "NEC1");
        if (newProtocol == null) // Cancel pressed
            return;
        if (irpDatabase.isKnown(newProtocol) ||
            guiUtils.confirm("The protocol \"" + newProtocol + "\" is unknown. Proceed anyhow?")) {
            parameterTableModel.setProtocol(newProtocol);
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
            guiUtils.browse(new URI(org.harctoolbox.girr.XmlExporter.GIRR_HOMEPAGE));
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

    private void irdbImportAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irdbImportAllButtonActionPerformed
        String deviceType = (String) irdbDeviceTypeComboBox.getSelectedItem();
        try {
            irdbImporter.load(deviceType);
            irdbTreeImporter.setRemoteSet(irdbImporter.getRemoteSet());
        } catch (Exception ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_irdbImportAllButtonActionPerformed

    private void jButton20ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton20ActionPerformed
        try {
            guiUtils.browse(new URI(LircImporter.homeUrl));
        } catch (URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_jButton20ActionPerformed

    private void webRmduButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_webRmduButtonActionPerformed
        try {
            guiUtils.browse(new URI(RmduImporter.homeUrl));
        } catch (URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_webRmduButtonActionPerformed

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
        String s = guiUtils.getInput("Enter creating user name", "User name inquiry", properties.getCreatingUser());
        if (s != null)
            properties.setCreatingUser(s);
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
        int f = Integer.parseInt((String) fColumnComboBox.getSelectedItem());
        properties.setFColumn(f);
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
        guiUtils.open(new File(properties.getExportDir()));
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
        } catch (InvalidArgumentException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_exportSignalButtonActionPerformed

    private void exportParametricRemoteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportParametricRemoteButtonActionPerformed
        RemoteSetExporter exporter = newRemoteExporter();
        if (exporter != null)
            saveParametricSignals(exporter);
    }//GEN-LAST:event_exportParametricRemoteButtonActionPerformed

    private void exportRawRemoteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportRawRemoteButtonActionPerformed
        saveRawSignals(newRemoteExporter());
    }//GEN-LAST:event_exportRawRemoteButtonActionPerformed

    private void girrSchemaLinkCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_girrSchemaLinkCheckBoxActionPerformed
        properties.setGirrCreateSchemaLocation(girrSchemaLinkCheckBox.isSelected());
    }//GEN-LAST:event_girrSchemaLinkCheckBoxActionPerformed

    private void girrFatRawCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_girrFatRawCheckBoxActionPerformed
        properties.setGirrFatRaw(girrFatRawCheckBox.isSelected());
    }//GEN-LAST:event_girrFatRawCheckBoxActionPerformed

    private void girrStylesheetUrlTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_girrStylesheetUrlTextFieldActionPerformed
        properties.setGirrStyleSheetUrl(girrStylesheetUrlTextField.getText());
    }//GEN-LAST:event_girrStylesheetUrlTextFieldActionPerformed

    private void girrStylesheetTypeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_girrStylesheetTypeComboBoxActionPerformed
        properties.setGirrStyleSheetType((String)girrStylesheetTypeComboBox.getSelectedItem());
    }//GEN-LAST:event_girrStylesheetTypeComboBoxActionPerformed

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

    private void importRmduMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importRmduMenuItem1ActionPerformed
        importRemoteByFileSelector(rmduImporter, false);
    }//GEN-LAST:event_importRmduMenuItem1ActionPerformed

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
        } catch (InvalidArgumentException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_signalSignalTextMenuItemActionPerformed

    private void exportSignalWaveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSignalWaveMenuItemActionPerformed
        try {
            saveSignal(getCapturedIrSignal(), new WaveExporter(exportAudioParametersBean));
        } catch (InvalidArgumentException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_exportSignalWaveMenuItemActionPerformed

    private void generateExportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generateExportButtonActionPerformed
        try {
            saveSignals(irpMasterBean.getCommands());
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
        saveParametricSignals(newTextExporter());
    }//GEN-LAST:event_exportParametricAsTextMenuItemActionPerformed

    private void exportRawAsTextMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportRawAsTextMenuItemActionPerformed
        saveRawSignals(newTextExporter());
    }//GEN-LAST:event_exportRawAsTextMenuItemActionPerformed

    private void exportRepeatComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportRepeatComboBoxActionPerformed
        properties.setExportNoRepeats(Integer.parseInt((String) exportRepeatComboBox.getSelectedItem()));
    }//GEN-LAST:event_exportRepeatComboBoxActionPerformed

    private void exportGenerateSendIrCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportGenerateSendIrCheckBoxActionPerformed
        properties.setExportGenerateSendIr(exportGenerateSendIrCheckBox.isSelected());
        exportRepeatComboBox.setEnabled(exportGenerateSendIrCheckBox.isSelected());
    }//GEN-LAST:event_exportGenerateSendIrCheckBoxActionPerformed

    private void sendirCompressedCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendirCompressedCheckBoxActionPerformed
        properties.setExportSendIrCompressed(sendirCompressedCheckBox.isSelected());
    }//GEN-LAST:event_sendirCompressedCheckBoxActionPerformed

    private void sendirModuleComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendirModuleComboBoxActionPerformed
        properties.setExportSendIrModule(Integer.parseInt((String)sendirModuleComboBox.getSelectedItem()));
    }//GEN-LAST:event_sendirModuleComboBoxActionPerformed

    private void sendirConnectorComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendirConnectorComboBoxActionPerformed
        properties.setExportSendIrConnector(Integer.parseInt((String) this.sendirConnectorComboBox.getSelectedItem()));
    }//GEN-LAST:event_sendirConnectorComboBoxActionPerformed

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

    private void irTransInternetHostPanelPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_irTransInternetHostPanelPropertyChange
        try {
            setupIrTrans();
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_irTransInternetHostPanelPropertyChange

    private void sendingHardwareTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sendingHardwareTabbedPaneStateChanged
        if (sendingHardwareManager == null)
            return;
        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
            for (ISendingHardware<?> hardware : sendingHardwareManager.getSendingHardware()) {
                if (hardware.getPanel() == sendingHardwareTabbedPane.getSelectedComponent()) {
                    sendingHardwareManager.selectDoWork(hardware.getName());
                    break;
                }
            }
        } catch (HarcHardwareException ex) {
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

    private void capturingHardwareTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_capturingHardwareTabbedPaneStateChanged
        if (capturingHardwareManager == null)
            return;
        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
            for (ICapturingHardware<?> hardware : capturingHardwareManager.getCapturingHardware()) {
                if (hardware.getPanel() == capturingHardwareTabbedPane.getSelectedComponent()) {
                    capturingHardwareManager.selectDoWork(hardware.getName());
                    break;
                }
            }
        } catch (IOException | HarcHardwareException ex) {
            guiUtils.error(ex);
        } finally {
            busyWindow.unBusy();
        }
    }//GEN-LAST:event_capturingHardwareTabbedPaneStateChanged

    private void captureTestButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_captureTestButtonActionPerformed
        if (captureThreadRunning()) {
            guiUtils.error("A capture thread is running. This must first be ended.");
            return;
        }
        if (!capturingHardwareManager.hasSelection()) {
            guiUtils.error("No capture device selected.");
            return;
        }
        if (!capturingHardwareManager.isReady()) {
            guiUtils.error("Selected capture device not ready (not opened?).");
            return;
        }

        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
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
        } catch (IOException | HarcHardwareException | NumberFormatException | InvalidArgumentException ex) {
            guiUtils.error(ex);
        } finally {
            busyWindow.unBusy();
        }
    }//GEN-LAST:event_captureTestButtonActionPerformed

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
        guiUtils.open(new File(properties.mkPathAbsolute(properties.getIrpProtocolsPath())));
        guiUtils.warning("If editing the file, changes will not take effect before you save the file AND restart the program.");
    }//GEN-LAST:event_irpProtocolsEditMenuItemActionPerformed

    private void irpProtocolsSelectMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irpProtocolsSelectMenuItemActionPerformed
        String oldDir = new File(properties.mkPathAbsolute(properties.getIrpProtocolsPath())).getParent();
        File f = SelectFile.selectFile(this, "Select protocol file IrpProtocol.ini", oldDir, false, false, "XML files (*.xml)", "xml");
        if (f == null || f.getAbsolutePath().equals(properties.mkPathAbsolute(properties.getIrpProtocolsPath())))
            return;

        properties.setIrpProtocolsPath(f.getAbsolutePath());
        guiUtils.warning("The program must be restarted for the changes to take effect.");
    }//GEN-LAST:event_irpProtocolsSelectMenuItemActionPerformed

    private void exportFormatsEditMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportFormatsEditMenuItemActionPerformed
        guiUtils.open(new File(properties.mkPathAbsolute(properties.getExportFormatFilePath())));
        guiUtils.warning("If editing, changes will not take effect before reloading.");
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

    private void importGlobalCacheHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importGlobalCacheHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.importGlobalCacheHelp);
    }//GEN-LAST:event_importGlobalCacheHelpButtonActionPerformed

    private void importIrdbHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importIrdbHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.importIrdbHelp);
    }//GEN-LAST:event_importIrdbHelpButtonActionPerformed

    private void importGirrSignalHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importGirrSignalHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.importGirrHelp);
    }//GEN-LAST:event_importGirrSignalHelpButtonActionPerformed

    private void importLircHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importLircHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.importLircHelp);
    }//GEN-LAST:event_importLircHelpButtonActionPerformed

    private void importRemoteMasterHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importRemoteMasterHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.importRemoteMasterHelp);
    }//GEN-LAST:event_importRemoteMasterHelpButtonActionPerformed

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
        } catch (InvalidArgumentException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_signalExportButtonActionPerformed

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        if (!capturingHardwareManager.isReady()) {
            guiUtils.error("No capture device selected, aborting");
            return;
        }
        if (captureThreadRunning()) {
            guiUtils.error("A capture thread is running. This must first be ended.");
            return;
        }
        BusyWindow busyWindow = BusyWindow.mkBusyWindow(this);
        try {
            ModulatedIrSequence modulatedIrSequence = captureIrSequence();

            if (modulatedIrSequence != null)
                processIr(modulatedIrSequence);
            else
                guiUtils.message("no signal received");
        } catch (IOException | HarcHardwareException | InvalidArgumentException ex) {
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

    private void capturingHardwareHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_capturingHardwareHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.capturingHardwareHelp);
    }//GEN-LAST:event_capturingHardwareHelpButtonActionPerformed

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
                globalCacheCaptureSelector.setTimeout(t);
            }
        } catch (NumberFormatException ex) {
            guiUtils.error("Invalid number: " + ex.getMessage());
        }
    }//GEN-LAST:event_globalCacheTimeoutMenuItemActionPerformed

    private void startMode2ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startMode2ButtonActionPerformed
        CapturingLircMode2 lircMode2 = (CapturingLircMode2) capturingHardwareManager.getCapturingHardware("LIRC Mode 2");
        String cmd = lircMode2CommandTextField.getText();
        properties.setLircMode2Command(cmd);
        lircMode2.setCommandName(mode2UseStdin(), cmd);
        try {
            lircMode2.open();
            lircMode2CommandTextField.setEnabled(false);
            stopMode2Button.setEnabled(true);
            startMode2Button.setEnabled(false);
            mode2TabbedPane.setEnabled(false);
            mode2CommandLabel.setEnabled(false);
        } catch (IOException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_startMode2ButtonActionPerformed

    private void stopMode2ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopMode2ButtonActionPerformed
        CapturingLircMode2 lircMode2 = (CapturingLircMode2) capturingHardwareManager.getCapturingHardware("LIRC Mode 2");
        lircMode2.close();
        if (mode2UseStdin())
            stdinHasBeenClosed = true;
        lircMode2CommandTextField.setEnabled(true);
        stopMode2Button.setEnabled(false);
        startMode2Button.setEnabled(!stdinHasBeenClosed);
        mode2TabbedPane.setEnabled(true);
        mode2CommandLabel.setEnabled(true);

    }//GEN-LAST:event_stopMode2ButtonActionPerformed

    private void clonePlotMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clonePlotMenuItemActionPerformed
        clonePlot();
    }//GEN-LAST:event_clonePlotMenuItemActionPerformed

    private void capturingIrToyHardwareHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_capturingIrToyHardwareHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.capturingIrToyHardwareHelp);
    }//GEN-LAST:event_capturingIrToyHardwareHelpButtonActionPerformed

    private void capturingIrWidgetHardwareHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_capturingIrWidgetHardwareHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.capturingIrWidgetHardwareHelp);
    }//GEN-LAST:event_capturingIrWidgetHardwareHelpButtonActionPerformed

    private void capturingGlobalCacheHardwareHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_capturingGlobalCacheHardwareHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.capturingGlobalCacheHardwareHelp);
    }//GEN-LAST:event_capturingGlobalCacheHardwareHelpButtonActionPerformed

    private void capturingMode2HardwareHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_capturingMode2HardwareHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.capturingMode2HardwareHelp);
    }//GEN-LAST:event_capturingMode2HardwareHelpButtonActionPerformed

    private void exportGirrHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportGirrHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.exportGirrHelp);
    }//GEN-LAST:event_exportGirrHelpButtonActionPerformed

    private void exportWaveHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportWaveHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.exportWaveHelp);
    }//GEN-LAST:event_exportWaveHelpButtonActionPerformed

    private void exportSendirHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSendirHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.exportSendirHelp);
    }//GEN-LAST:event_exportSendirHelpButtonActionPerformed

    private void exportProntoHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportProntoHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.exportProntoHelp);
    }//GEN-LAST:event_exportProntoHelpButtonActionPerformed

    private void sendingGlobalCacheHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendingGlobalCacheHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.sendingGlobalCacheHelp);
    }//GEN-LAST:event_sendingGlobalCacheHelpButtonActionPerformed

    private void sendingAudioHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendingAudioHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.sendingAudioHelp);
    }//GEN-LAST:event_sendingAudioHelpButtonActionPerformed

    private void sendingIrToyHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendingIrToyHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.sendingIrToyHelp);
    }//GEN-LAST:event_sendingIrToyHelpButtonActionPerformed

    private void sendingGenericSerialPortHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendingGenericSerialPortHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.sendingGenericSerialPortHelp);
    }//GEN-LAST:event_sendingGenericSerialPortHelpButtonActionPerformed

    private void sendingLircHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendingLircHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.sendingLircHelp);
    }//GEN-LAST:event_sendingLircHelpButtonActionPerformed

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

    private void sendingIrTransHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendingIrTransHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.sendingIrTransHelp);
    }//GEN-LAST:event_sendingIrTransHelpButtonActionPerformed

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

    private void clearParametrizedCommentMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearParametrizedCommentMenuItemActionPerformed
        parameterTableModel.clearComment();
    }//GEN-LAST:event_clearParametrizedCommentMenuItemActionPerformed

    private void clearRawCommentMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearRawCommentMenuItemActionPerformed
        rawTableModel.clearComment();
    }//GEN-LAST:event_clearRawCommentMenuItemActionPerformed

    private void exportGenerateShortCcfCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportGenerateShortCcfCheckBoxActionPerformed
        properties.setExportGenerateShortCcf(exportGenerateShortCcfCheckBox.isSelected());
    }//GEN-LAST:event_exportGenerateShortCcfCheckBoxActionPerformed

    private void offerStackTraceCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_offerStackTraceCheckBoxMenuItemActionPerformed
        boolean state = offerStackTraceCheckBoxMenuItem.isSelected();
        guiUtils.setOfferStackTrace(state);
        properties.setOfferStackTrace(state);
    }//GEN-LAST:event_offerStackTraceCheckBoxMenuItemActionPerformed

    private void setMiscParamsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setMiscParamsMenuItemActionPerformed
        String value = guiUtils.getInput("Enter \"Misc. Params\" as string", "Parameters entry", "X=0");
        parameterTableModel.setMiscParameters(value);
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
                editingTextField.setEditable(parameterTableModel.isCellEditable(row, column));
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

    private void prontoExportScreenWidthTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prontoExportScreenWidthTextFieldActionPerformed
        properties.setProntoScreenSizeX(Integer.parseInt(prontoExportScreenWidthTextField.getText()));
    }//GEN-LAST:event_prontoExportScreenWidthTextFieldActionPerformed

    private void prontoExportScreenHeightTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prontoExportScreenHeightTextFieldActionPerformed
        properties.setProntoScreenSizeY(Integer.parseInt(prontoExportScreenHeightTextField.getText()));
    }//GEN-LAST:event_prontoExportScreenHeightTextFieldActionPerformed

    private void prontoExportButtonWidthTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prontoExportButtonWidthTextFieldActionPerformed
        properties.setProntoButtonSizeX(Integer.parseInt(prontoExportButtonWidthTextField.getText()));
    }//GEN-LAST:event_prontoExportButtonWidthTextFieldActionPerformed

    private void prontoExportButtonHeightTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prontoExportButtonHeightTextFieldActionPerformed
        properties.setProntoButtonSizeY(Integer.parseInt(prontoExportButtonHeightTextField.getText()));
    }//GEN-LAST:event_prontoExportButtonHeightTextFieldActionPerformed

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
        parameterTableModel.unsetParameter("T");
    }//GEN-LAST:event_unsetTMenuItemActionPerformed

    private void debugCodeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debugCodeMenuItemActionPerformed
        String s = guiUtils.getInput("TODO", "Not yet implemented", "");
        if (s != null)
            ;
    }//GEN-LAST:event_debugCodeMenuItemActionPerformed

    private void sendingGirsClientHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendingGirsClientHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.sendingGirsHelp);
    }//GEN-LAST:event_sendingGirsClientHelpButtonActionPerformed

    private void sendingCommandFusionHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendingCommandFusionHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.sendingCommandFusionHelp);
    }//GEN-LAST:event_sendingCommandFusionHelpButtonActionPerformed

    private void capturingGirsHardwareHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_capturingGirsHardwareHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.capturingGirsHelp);
    }//GEN-LAST:event_capturingGirsHardwareHelpButtonActionPerformed

    private void capturingCommandFusionHardwareHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_capturingCommandFusionHardwareHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.capturingCommandFusionHelp);
    }//GEN-LAST:event_capturingCommandFusionHardwareHelpButtonActionPerformed

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
        String s = guiUtils.getInput("Enter character set to be used for export (e.g. US-ASCII, UTF-8, ISO-8859-1, WINDOWS-1252)", "Export character set inquiry", properties.getExportCharsetName());
        if (s == null)
            return;
        if (Charset.isSupported(s))
            properties.setExportCharsetName(s);
        else
            guiUtils.error("Character set \"" + s + "\" is not supported");
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
            guiUtils.browse(new URI(gitUrl));
        } catch (URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_gitMenuItemActionPerformed

    private void issuesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_issuesMenuItemActionPerformed
        try {
            guiUtils.browse(new URI(issuesUrl));
        } catch (URISyntaxException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_issuesMenuItemActionPerformed

    private void feedbackMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_feedbackMenuItemActionPerformed
          try {
            String body = String.format(
                    "# Enter message here%%0D%%0A%%0D%%0A%6$s%%0D%%0A%7$s%%0D%%0A"
                    + "IrpTransmogrifier%%3D%1$s%%0D%%0AJava%%3D%2$s %3$s%%0D%%0AOperating system%%3D%4$s-%5$s%%0D%%0ALAF=%8$s",
                    org.harctoolbox.irp.Version.version,
                    System.getProperty("java.vendor"), System.getProperty("java.version"),
                    System.getProperty("os.name"), System.getProperty("os.arch"),
                    Version.versionString, applicationHome,
                    lookAndFeelManager.getCurrentLAFClassName()
            ).replace(" ", "%20").replace("\\", "%5C");
            String subject = ("Feedback to " + Version.versionString).replace(" ", "%20");
            guiUtils.mail(feedbackMail, subject, body);
        } catch (URISyntaxException | IOException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_feedbackMenuItemActionPerformed

    private void cleanerCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cleanerCheckBoxMenuItemActionPerformed
        properties.setInvokeCleaner(cleanerCheckBoxMenuItem.isSelected());
    }//GEN-LAST:event_cleanerCheckBoxMenuItemActionPerformed

    private void capturingDevLircHardwareHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_capturingDevLircHardwareHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.capturingDevLircHelp);
    }//GEN-LAST:event_capturingDevLircHardwareHelpButtonActionPerformed

    private void sendingDevLircHardwareHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendingDevLircHardwareHelpButtonActionPerformed
        HelpPopup.newHelpPopup(this, HelpTexts.sendingDevLircHelp);
    }//GEN-LAST:event_sendingDevLircHardwareHelpButtonActionPerformed

    private void rejectLircCodeImportsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rejectLircCodeImportsActionPerformed
        properties.setRejectLircCodeImports(rejectLircCodeImports.isSelected());
    }//GEN-LAST:event_rejectLircCodeImportsActionPerformed

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
            controlTowerTreeImporter.setRemoteSet(controlTowerIrDatabase.getRemoteSet());
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
                String[] arr = manufacturers.toArray(new String[manufacturers.size()]);
                //resetCursor(oldCursor);
                Arrays.sort(arr, String.CASE_INSENSITIVE_ORDER);
                DefaultComboBoxModel dcbm = new DefaultComboBoxModel(arr);
                controlTowerManufacturerComboBox.setModel(dcbm);
                controlTowerManufacturerComboBoxActionPerformed(null);
            } else {
                String manufacturer = (String) controlTowerManufacturerComboBox.getSelectedItem();
                Collection<String> devTypes = controlTowerIrDatabase.getDeviceTypes(manufacturer);
                String[] arr = devTypes.toArray(new String[devTypes.size()]);
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

    private void apiKeyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_apiKeyButtonActionPerformed
        String apiKey = guiUtils.getInput("Enter GlobalCache API key", "API key entry", properties.getGlobalCacheApiKey());
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            properties.setGlobalCacheApiKey(apiKey.trim());
            gcdbManufacturerComboBox.setEnabled(true);
            gcdbManufacturerComboBox.setToolTipText(null);
        }
    }//GEN-LAST:event_apiKeyButtonActionPerformed

    private void topLevelTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_topLevelTabbedPaneStateChanged
        // Must make sure this is not effective during early initComponents().
        if (sendingHardwareManager != null) {
            lastPane = currentPane;
            currentPane = topLevelTabbedPane.getSelectedComponent();
            properties.setSelectedMainPaneIndex(topLevelTabbedPane.getSelectedIndex());
        }
    }//GEN-LAST:event_topLevelTabbedPaneStateChanged

    private void rawCookedTabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rawCookedTabbedPaneStateChanged
        // Must make sure this is not effective during early initComponents().
        if (sendingHardwareManager != null)
            properties.setSelectedRemoteIndex(rawCookedTabbedPane.getSelectedIndex());
    }//GEN-LAST:event_rawCookedTabbedPaneStateChanged

    private void lircTimeoutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lircTimeoutMenuItemActionPerformed
        try {
            Integer t = guiUtils.getIntegerInput("Lirc socket time-out in milliseconds", properties.getLircTimeout());
            if (t != null) {
                properties.setLircTimeout(t);
                sendingLircClient.setTimeout(t);
            }
        } catch (NumberFormatException ex) {
            guiUtils.error("Invalid number: " + ex.getMessage());
        }
    }//GEN-LAST:event_lircTimeoutMenuItemActionPerformed

    private void rawCodePasteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rawCodePasteMenuItemActionPerformed
        insertCapturedDataTextAreaFromClipboard();
    }//GEN-LAST:event_rawCodePasteMenuItemActionPerformed

    private void mode2TabbedPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_mode2TabbedPaneStateChanged
        if (initialized)
            properties.setLircMode2UseStdin(mode2TabbedPane.getSelectedIndex() == 0);
        startMode2Button.setEnabled(!mode2UseStdin() || !stdinHasBeenClosed);
    }//GEN-LAST:event_mode2TabbedPaneStateChanged

    private void continuousCaptureButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_continuousCaptureButtonActionPerformed
        if (!capturingHardwareManager.isReady()) {
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
        }
        topLevelTabbedPane.setEnabled(! continuousCaptureButton.isSelected());
        jumpToLastPanelMenuItem.setEnabled(! continuousCaptureButton.isSelected());
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
        String prefix = guiUtils.getInput("Enter desired prefix", "Prefix inquiry", "cmd_");
        if (prefix != null)
            parameterTableModel.namesTransform("^", prefix);
    }//GEN-LAST:event_addNamePrefixMenuItemActionPerformed

    private void transformNameMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transformNameMenuItemActionPerformed
        String old = guiUtils.getInput("Enter string to be replaced (regular expression)", "String inquiry", "test");
        if (old == null)
            return;
        String replacement = guiUtils.getInput("Replace \"" + old + "\" by ...", "String inquiry", old);
        if (replacement != null)
            parameterTableModel.namesTransform(old, replacement);
    }//GEN-LAST:event_transformNameMenuItemActionPerformed

    private void proxyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proxyMenuItemActionPerformed
        ProxyConfigDialog.inquireProxyConfig(this, properties);
        setupProxy();
    }//GEN-LAST:event_proxyMenuItemActionPerformed

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
            guiUtils.browse(new URI(downloadsUrl));
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

    //<editor-fold defaultstate="collapsed" desc="Automatic variable declarations">
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPopupMenu CCFCodePopupMenu;
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JMenuItem absToleranceMenuItem;
    private javax.swing.JMenu actionsMenu;
    private javax.swing.JMenuItem addEmptyParametrizedSignalMenuItem;
    private javax.swing.JMenuItem addMissingFsMenuItem;
    private javax.swing.JMenuItem addMissingNamesMenuItem;
    private javax.swing.JMenuItem addNamePrefixMenuItem;
    private javax.swing.JMenuItem analysisToClipboardMenuItem;
    private javax.swing.JRadioButtonMenuItem analyzerBase10RadioButtonMenuItem;
    private javax.swing.JRadioButtonMenuItem analyzerBase16RadioButtonMenuItem;
    private javax.swing.JRadioButtonMenuItem analyzerBase2RadioButtonMenuItem;
    private javax.swing.JRadioButtonMenuItem analyzerBase8RadioButtonMenuItem;
    private javax.swing.ButtonGroup analyzerBaseButtonGroup;
    private javax.swing.JMenu analyzerBasisMenu;
    private javax.swing.JTextField analyzerTextField;
    private javax.swing.JButton apiKeyButton;
    private javax.swing.JPanel audioPanel;
    private javax.swing.JCheckBox automaticExportFilenamesCheckBox;
    private javax.swing.JMenuItem beaconListenerMenuItem;
    private javax.swing.JPanel captureCommandFusionPanel;
    private javax.swing.JPanel captureDevLircPanel;
    private javax.swing.JPanel captureGirsPanel;
    private javax.swing.JPanel captureGlobalCachePanel;
    private javax.swing.JPanel captureIrToyPanel;
    private javax.swing.JPanel captureIrWidgetPanel;
    private javax.swing.JPanel captureLircMode2Panel;
    private javax.swing.JButton captureTestButton;
    private javax.swing.JScrollPane capturedDataScrollPane;
    private org.harctoolbox.guicomponents.UndoableJTextArea capturedDataTextArea;
    private javax.swing.JButton capturingCommandFusionHardwareHelpButton;
    private javax.swing.JButton capturingDevLircHardwareHelpButton;
    private javax.swing.JButton capturingGirsHardwareHelpButton;
    private javax.swing.JButton capturingGlobalCacheHardwareHelpButton;
    private javax.swing.JButton capturingHardwareHelpButton;
    private javax.swing.JTabbedPane capturingHardwareTabbedPane;
    private javax.swing.JButton capturingIrToyHardwareHelpButton;
    private javax.swing.JButton capturingIrWidgetHardwareHelpButton;
    private javax.swing.JButton capturingMode2HardwareHelpButton;
    private javax.swing.JPanel capturingPanel;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<CcfImporter> ccfFileImporterBean;
    private javax.swing.JPanel ccfImportPanel;
    private javax.swing.JRadioButtonMenuItem ccfRadioButtonMenuItem;
    private javax.swing.JMenuItem checkParametrizedSignalsMenuItem;
    private javax.swing.JMenuItem checkRawCommandsMenuItem;
    private javax.swing.JMenuItem checkUpToDateMenuItem;
    private javax.swing.JCheckBoxMenuItem cleanerCheckBoxMenuItem;
    private javax.swing.JMenuItem clearConsoleMenuItem;
    private javax.swing.JMenuItem clearMenuItem;
    private javax.swing.JMenuItem clearMenuItem1;
    private javax.swing.JMenuItem clearParametrizedCommentMenuItem;
    private javax.swing.JMenuItem clearRawCommentMenuItem;
    private javax.swing.JMenuItem clearSignalMenuItem;
    private javax.swing.JMenuItem clonePlotMenuItem;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<CmlImporter> cmlFileImporterBean;
    private javax.swing.JPanel cmlImportPanel;
    private org.harctoolbox.guicomponents.CapturingSendingBean commandFusionCapturingSendingBean;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<CommandFusionImporter> commandFusionFileImporterBean;
    private javax.swing.JPanel commandFusionImportPanel;
    private javax.swing.JPanel commandFusionSendPanel;
    private org.harctoolbox.guicomponents.SerialPortSimpleBean commandFusionSendingSerialPortBean;
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
    private javax.swing.JMenuItem debugTableRowMenuItem;
    private javax.swing.JMenuItem debugTableRowMenuItem1;
    private javax.swing.JTextField decodeIRTextField;
    private javax.swing.JMenuItem deleteDefaultedSignalsMenuItem;
    private javax.swing.JMenuItem deleteMenuItem;
    private javax.swing.JMenuItem deleteMenuItem1;
    private org.harctoolbox.guicomponents.DevLircBean devLircBean;
    private org.harctoolbox.guicomponents.CapturingSendingBean devLircCapturingSendingBean;
    private javax.swing.JPanel devLircPanel;
    private javax.swing.JMenuItem downloadsMenuItem;
    private javax.swing.JMenuItem dummyGapMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JTextField editingTextField;
    private javax.swing.JLabel endingLengthLabel;
    private javax.swing.JMenuItem endingTimeoutMenuItem;
    private javax.swing.JMenuItem exitMenuItem;
    private org.harctoolbox.guicomponents.AudioParametersBean exportAudioParametersBean;
    private javax.swing.JMenu exportCapturedMenu;
    private javax.swing.JMenuItem exportCharsetMenuItem;
    private javax.swing.JButton exportDirOpenButton;
    private javax.swing.JButton exportDirSelectButton;
    private javax.swing.JTextField exportDirectoryTextField;
    private javax.swing.JComboBox<String> exportFormatComboBox;
    private javax.swing.JMenuItem exportFormatsEditMenuItem;
    private javax.swing.JMenu exportFormatsMenu;
    private javax.swing.JMenuItem exportFormatsReloadMenuItem;
    private javax.swing.JMenuItem exportFormatsSelectMenuItem;
    private javax.swing.JCheckBox exportGenerateCcfCheckBox;
    private javax.swing.JCheckBox exportGenerateParametersCheckBox;
    private javax.swing.JCheckBox exportGenerateRawCheckBox;
    private javax.swing.JCheckBox exportGenerateSendIrCheckBox;
    private javax.swing.JCheckBox exportGenerateShortCcfCheckBox;
    private javax.swing.JButton exportGirrHelpButton;
    private javax.swing.JButton exportHelpButton;
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
    private javax.swing.JButton exportSendirHelpButton;
    private javax.swing.JButton exportSignalButton;
    private javax.swing.JMenuItem exportSignalGirrMenuItem;
    private javax.swing.JMenuItem exportSignalGirrMenuItem1;
    private javax.swing.JMenuItem exportSignalIctMenuItem;
    private javax.swing.JMenuItem exportSignalIctMenuItem1;
    private javax.swing.JMenuItem exportSignalWaveMenuItem;
    private javax.swing.JMenuItem exportSignalWaveMenuItem1;
    private javax.swing.JTabbedPane exportSpecificOptionsTabbedPane;
    private javax.swing.JButton exportWaveHelpButton;
    private javax.swing.JComboBox<String> fColumnComboBox;
    private javax.swing.JMenuItem fallbackFrequencyMenuItem;
    private javax.swing.JMenuItem feedbackMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JLabel frequencyLabel;
    private javax.swing.JMenuItem frequencyToleranceMenuItem;
    private javax.swing.JComboBox<String> gcdbCodeSetComboBox;
    private javax.swing.JComboBox<String> gcdbDeviceTypeComboBox;
    private javax.swing.JButton gcdbImportButton;
    private javax.swing.JComboBox<String> gcdbManufacturerComboBox;
    private javax.swing.JPanel gcdbPanel;
    private org.harctoolbox.irscrutinizer.importer.TreeImporter gcdbTreeImporter;
    private javax.swing.JButton generateButton;
    private javax.swing.JCheckBoxMenuItem generateCcfCheckBoxMenuItem;
    private javax.swing.JButton generateExportButton;
    private javax.swing.JButton generateHelpButton;
    private javax.swing.JPanel generatePanel;
    private javax.swing.JCheckBoxMenuItem generateRawCheckBoxMenuItem;
    private javax.swing.JTextArea generateTextArea;
    private javax.swing.JPanel genericSerialPanel;
    private org.harctoolbox.irscrutinizer.sendinghardware.GenericSerialSenderBean genericSerialSenderBean;
    private javax.swing.JPanel girrExportOptionsPanel;
    private javax.swing.JCheckBox girrFatRawCheckBox;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<GirrImporter> girrFileImporterBean;
    private javax.swing.JPanel girrImportPanel;
    private javax.swing.JCheckBox girrSchemaLinkCheckBox;
    private javax.swing.JMenuItem girrSchemaLocationMenuItem;
    private javax.swing.JComboBox<String> girrStylesheetTypeComboBox;
    private javax.swing.JTextField girrStylesheetUrlTextField;
    private javax.swing.JCheckBoxMenuItem girrValidateCheckBoxMenuItem;
    private javax.swing.JButton girrWebSiteButton;
    private org.harctoolbox.guicomponents.CapturingSendingBean girsClientCapturingSendingBean;
    private javax.swing.JPanel girsClientPanel;
    private org.harctoolbox.guicomponents.GirsClientBean girsTcpSerialComboBean;
    private javax.swing.JMenuItem gitMenuItem;
    private org.harctoolbox.guicomponents.GlobalCacheIrSenderSelector globalCacheCaptureSelector;
    private javax.swing.JButton globalCacheDBBrowseButton;
    private org.harctoolbox.guicomponents.GlobalCacheIrSenderSelector globalCacheIrSenderSelector;
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
    private javax.swing.JButton importGlobalCacheHelpButton;
    private javax.swing.JButton importIctHelpButton;
    private javax.swing.JMenuItem importIctMenuItem;
    private javax.swing.JMenuItem importIctMenuItem1;
    private javax.swing.JButton importIrTransHelpButton;
    private javax.swing.JButton importIrdbHelpButton;
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
    private javax.swing.JButton importRemoteMasterHelpButton;
    private javax.swing.JMenuItem importRmduMenuItem;
    private javax.swing.JMenuItem importRmduMenuItem1;
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
    private javax.swing.JPanel irToyPanel;
    private org.harctoolbox.guicomponents.SerialPortSimpleBean irToySerialPortBean;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<IrTransImporter> irTransFileImporterBean;
    private org.harctoolbox.guicomponents.InternetHostPanel irTransInternetHostPanel;
    private org.harctoolbox.guicomponents.NamedCommandLauncher irTransNamedCommandLauncher;
    private javax.swing.JPanel irTransPanel;
    private javax.swing.JButton irTransWebButton;
    private org.harctoolbox.guicomponents.SerialPortSimpleBean irWidgetSerialPortSimpleBean;
    private javax.swing.JButton irdbBrowseButton;
    private javax.swing.JComboBox<String> irdbCodeSetComboBox;
    private javax.swing.JComboBox<String> irdbDeviceTypeComboBox;
    private javax.swing.JButton irdbImportAllButton;
    private javax.swing.JButton irdbImportButton;
    private javax.swing.JComboBox<String> irdbManufacturerComboBox;
    private javax.swing.JPanel irdbPanel;
    private org.harctoolbox.irscrutinizer.importer.TreeImporter irdbTreeImporter;
    private javax.swing.JMenuItem irpFormatsIniReloadMenuItem;
    private org.harctoolbox.guicomponents.IrpMasterBean irpMasterBean;
    private javax.swing.JMenuItem irpProtocolsEditMenuItem;
    private javax.swing.JMenu irpProtocolsIniMenu;
    private javax.swing.JMenuItem irpProtocolsSelectMenuItem;
    private javax.swing.JMenuItem irpTransmogrifierHelpMenuItem;
    private org.harctoolbox.guicomponents.CapturingSendingBean irtoyCapturingSendingBean;
    private javax.swing.JPanel irtransImportPanel;
    private javax.swing.JMenuItem issuesMenuItem;
    private javax.swing.JButton jButton20;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
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
    private javax.swing.JPopupMenu.Separator jSeparator4;
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
    private org.harctoolbox.guicomponents.InternetHostPanel lircInternetHostPanel;
    private javax.swing.JTextField lircMode2CommandTextField;
    private org.harctoolbox.guicomponents.NamedCommandLauncher lircNamedCommandLauncher;
    private javax.swing.JPanel lircPanel;
    private javax.swing.JMenuItem lircTimeoutMenuItem;
    private javax.swing.JMenu loadMenu;
    private javax.swing.JMenuItem mainDocuMenuItem;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem minLeadoutMenuItem;
    private javax.swing.JMenuItem minRepeatGapMenuItem;
    private javax.swing.JLabel mode2CommandLabel;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<Mode2Importer> mode2FileImporterBean;
    private javax.swing.JPanel mode2ImportPanel;
    private javax.swing.JPanel mode2StdinPane;
    private javax.swing.JPanel mode2SubprocessPane;
    private javax.swing.JTabbedPane mode2TabbedPane;
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
    private javax.swing.JTabbedPane parametrizedRawTabbedPane;
    private javax.swing.JButton pasteAnalyzeButton;
    private javax.swing.JMenuItem pasteScrutinizeToDataWindowMenuItem;
    private javax.swing.JMenuItem pasteToDataWindowMenuItem;
    private javax.swing.JScrollPane plotScrollPane;
    private javax.swing.JMenuItem plotterResetMenuItem;
    private javax.swing.JCheckBoxMenuItem printAnalyzeIRPsToConsoleCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem printDecodesToConsoleCheckBoxMenuItem;
    private javax.swing.JPanel prontoClassicExportOptionsPanel;
    private javax.swing.JTextField prontoExportButtonHeightTextField;
    private javax.swing.JTextField prontoExportButtonWidthTextField;
    private javax.swing.JTextField prontoExportScreenHeightTextField;
    private javax.swing.JTextField prontoExportScreenWidthTextField;
    private javax.swing.JComboBox<String> prontoModelComboBox;
    private javax.swing.JComboBox<String> protocolColumnComboBox;
    private javax.swing.JMenu protocolParametersMenu;
    private javax.swing.JMenuItem protocolSpecMenuItem;
    private javax.swing.JMenuItem proxyMenuItem;
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
    private javax.swing.JRadioButtonMenuItem rawRadioButtonMenuItem;
    private javax.swing.JCheckBoxMenuItem rawSorterCheckBoxMenuItem;
    private javax.swing.JTable rawTable;
    private javax.swing.JPopupMenu rawTablePopupMenu;
    private javax.swing.JScrollPane rawTableScrollPane;
    private javax.swing.JRadioButtonMenuItem rawWithoutSignsRadioButtonMenuItem;
    private javax.swing.JMenuItem reAnalyzeMenuItem;
    private javax.swing.JCheckBoxMenuItem rejectLircCodeImports;
    private javax.swing.JMenuItem relToleranceMenuItem;
    private javax.swing.JMenuItem releaseNotesMenuItem;
    private javax.swing.JPanel remoteScrutinizerPanel;
    private javax.swing.JMenuItem removeUnusedMenuItem1;
    private javax.swing.JLabel repLengthLabel;
    private javax.swing.JCheckBoxMenuItem repeatFinderCheckBoxMenuItem;
    private javax.swing.JMenuItem resetPropertiesMenuItem;
    private javax.swing.JMenuItem resetRawTableColumnsMenuItem;
    private javax.swing.JMenuItem resetRawTableColumnsMenuItem1;
    private javax.swing.JPanel rmduImportPanel;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<RmduImporter> rmduImporterBean;
    private javax.swing.JComboBox<String> sColumnComboBox;
    private javax.swing.JMenu saveCapturedMenu;
    private javax.swing.JMenuItem saveConsoleTextAsMenuItem;
    private javax.swing.JMenuItem saveCookedMenuItem;
    private javax.swing.JMenuItem saveDataTextAsMenuItem;
    private javax.swing.JMenu saveMenu;
    private javax.swing.JMenu saveParametrizedMenu;
    private javax.swing.JMenuItem savePropertiesAsMenuItem;
    private javax.swing.JMenuItem savePropertiesMenuItem;
    private javax.swing.JMenu saveRawMenu;
    private javax.swing.JMenuItem saveRawMenuItem;
    private javax.swing.JMenuItem saveSelectedCookedMenuItem;
    private javax.swing.JMenuItem saveSelectedRawTableRowMenuItem;
    private javax.swing.JMenuItem scrutinizeMenuItem;
    private javax.swing.JMenuItem scrutinizeParametricMenuItem;
    private javax.swing.JButton scrutinizeRemoteHelpButton;
    private javax.swing.JButton scrutinizeSignalHelpButton;
    private javax.swing.JMenuItem sendMenuItem;
    private javax.swing.JButton sendingAudioHelpButton;
    private javax.swing.JButton sendingCommandFusionHelpButton;
    private javax.swing.JButton sendingDevLircHardwareHelpButton;
    private javax.swing.JButton sendingGenericSerialPortHelpButton;
    private javax.swing.JButton sendingGirsClientHelpButton;
    private javax.swing.JButton sendingGlobalCacheHelpButton;
    private javax.swing.JButton sendingHardwareHelpButton;
    private javax.swing.JTabbedPane sendingHardwareTabbedPane;
    private javax.swing.JButton sendingIrToyHelpButton;
    private javax.swing.JButton sendingIrTransHelpButton;
    private javax.swing.JButton sendingLircHelpButton;
    private javax.swing.JPanel sendingPanel;
    private javax.swing.JMenuItem sendingTimeoutMenuItem;
    private javax.swing.JCheckBox sendirCompressedCheckBox;
    private javax.swing.JComboBox<String> sendirConnectorComboBox;
    private javax.swing.JPanel sendirExportOptionsPanel;
    private javax.swing.JComboBox<String> sendirModuleComboBox;
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
    private javax.swing.JButton startMode2Button;
    private javax.swing.JToggleButton startStopToggleButton;
    private javax.swing.JMenuItem startTimeoutMenuItem;
    private javax.swing.JButton stopMode2Button;
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
    private org.harctoolbox.guicomponents.AudioParametersBean transmitAudioParametersBean;
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
    private javax.swing.JCheckBoxMenuItem usePopupsForErrorsCheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem usePopupsForHelpCheckBoxMenuItem;
    private javax.swing.JMenu usePopupsMenu;
    private javax.swing.JCheckBoxMenuItem verboseCheckBoxMenuItem;
    private javax.swing.JPanel waveExportOptionsPanel;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<WaveImporter> waveFileImporterBean;
    private javax.swing.JPanel waveImportPanel;
    private javax.swing.JButton webRmduButton;
    private org.harctoolbox.irscrutinizer.importer.FileImporterBean<XcfImporter> xcfFileImporterBean;
    private javax.swing.JPanel xcfImportPanel;
    // End of variables declaration//GEN-END:variables
    //</editor-fold>
}
