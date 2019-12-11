/*
Copyright (C) 2013, 2014 Bengt Martensson.

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

/**
 * Misleading name, should be called e.g. IrpTransmogrifierBean ...
 */
package org.harctoolbox.guicomponents;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.irp.InvalidNameException;
import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.irp.IrpInvalidArgumentException;
import org.harctoolbox.irp.NameUnassignedException;
import org.harctoolbox.irp.Protocol;
import org.harctoolbox.irp.UnknownProtocolException;
import org.harctoolbox.irp.UnsupportedRepeatException;
import org.harctoolbox.irscrutinizer.DefaultSignalNameFormatter;
import org.harctoolbox.irscrutinizer.ISignalNameFormatter;
import org.harctoolbox.valuesets.InputVariableSetValues;
import org.harctoolbox.valuesets.RandomValueSet;

/**
 *
 */
public final class IrpMasterBean extends javax.swing.JPanel {
    private static final String invalidParameterString = Long.toString(IrCoreUtils.INVALID);

    private static final String FALLBACK_PROTOCOL_NAME = "NEC1";

    public static final String PROP_PROTOCOL_NAME = "protocolName";
    public static final String PROP_D = "D";
    public static final String PROP_S = "S";
    public static final String PROP_F = "F";
    public static final String PROP_T = "T";
    public static final String PROP_ADDITIONAL_PARAMS = "ADDITIONAL_PARAMS";
    private JFrame frame;
    private transient IrpDatabase irpDatabase;
    private String protocolName;
    private Protocol protocol;
    private GuiUtils guiUtils;
    private transient ISignalNameFormatter signalNameFormatter;

    // presently only used for the property change support
    private String D = invalidParameterString;
    private String S = invalidParameterString;
    private String F = invalidParameterString;
    private String T = invalidParameterString;
    private String additionalParameters = "";

    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    private static String mkString(Long x) {
        return x == null ? "" : Long.toString(x);
    }

    public void setSignalNameFormatter(ISignalNameFormatter signalNameFormatter) {
        this.signalNameFormatter = signalNameFormatter;
    }

    /**
     * @param frame the frame to set
     */
    public void setFrame(JFrame frame) {
        this.frame = frame;
    }

    /**
     * @param irpMaster the irpMaster to set
     */
    public void setIrpMaster(IrpDatabase irpMaster) {
        this.irpDatabase = irpMaster;
    }

    public String getProtocolName() {
        return protocolName;
    }

    /**
     * Creates new form IrpMasterBean
     */
    public IrpMasterBean() {
        initComponents();
    }

    public IrpMasterBean(JFrame frame, GuiUtils guiUtils, IrpDatabase irpMaster, String intialProtocol) {
        this(frame, guiUtils, irpMaster, intialProtocol, "0", invalidParameterString, "0", "-", "");
    }

    public IrpMasterBean(JFrame frame, GuiUtils guiUtils, IrpDatabase irpDatabase, String intialProtocol,
            String initialD, String initialS, String initialF, String initialT, String initialAdditionalParameters) {
        this.signalNameFormatter = new DefaultSignalNameFormatter();
        this.frame = frame;
        this.guiUtils = guiUtils;
        this.irpDatabase = irpDatabase;
        this.protocolName = irpDatabase.isKnown(intialProtocol) ? intialProtocol : FALLBACK_PROTOCOL_NAME;
        initComponents();
        try {
            setupProtocol(protocolName, initialD, initialS, initialF, initialT, initialAdditionalParameters);
        } catch (IrpException ex) {
            guiUtils.error(ex);
        }
        D = initialD;
        S = initialS;
        F = initialF;
        T = initialT;
        additionalParameters = initialAdditionalParameters;
    }

    private String[] irpMasterProtocols() {
        if (irpDatabase == null)
            return new String[]{"--"};

        String[] protocolList = irpDatabase.getNames().toArray(new String[irpDatabase.getNames().size()]);
        java.util.Arrays.sort(protocolList, String.CASE_INSENSITIVE_ORDER);
        return protocolList;
    }

    private void checkParam(JTextField textField, JLabel label, String parameterName, String oldValueStr) {
        if (protocol.hasParameter(parameterName)) {
            // TODO: check validity
            textField.setEnabled(true);
            label.setEnabled(true);
            if (!oldValueStr.equals(invalidParameterString))
                textField.setText(oldValueStr);
        } else {
            textField.setEnabled(false);
            label.setEnabled(false);
            textField.setText(null);
        }
    }

    private void checkParam(JComboBox comboBox, JLabel label, String parameterName, String initalValue) {
        if (protocol.hasParameter(parameterName))
            comboBox.setSelectedItem(initalValue);
        else
            comboBox.setSelectedItem("-");

        comboBox.setEnabled(protocol.hasParameter(parameterName));
        label.setEnabled(protocol.hasParameter(parameterName));
    }

    private void setupProtocol(String protocolName, String initialD, String initialS, String initialF,
            String initalT, String initialAdditionalParameters)
            throws UnknownProtocolException, UnsupportedRepeatException, NameUnassignedException, InvalidNameException, IrpInvalidArgumentException {
        protocol = irpDatabase.getProtocol(protocolName);
        irpTextField.setText(irpDatabase.getIrp(protocolName));
        setParameters(initialD, initialS, initialF, initalT, initialAdditionalParameters);
    }

    public void setParameters(Map<String, Long> parameters) {
        setParameters(mkString(parameters.get("D")), mkString(parameters.get("S")),
                mkString(parameters.get("F")), mkString(parameters.get("T")), formatMiscParams(parameters));
    }

    private void setParameters(String initialD, String initialS, String initialF,
            String initalT, String initialAdditionalParameters) {
        checkParam(dTextField, dLabel, "D", initialD);
        checkParam(sTextField, sLabel, "S", initialS);
        checkParam(fTextField, fLabel, "F", initialF);
        checkParam(toggleComboBox, tLabel, "T", initalT);

        additionalParametersTextField.setText(initialAdditionalParameters);
        additionalParametersLabel.setEnabled(protocol.hasNonStandardParameters());
        additionalParametersTextField.setEnabled(protocol.hasNonStandardParameters());
    }

    private static String formatMiscParams(Map<String, Long> params) {
        if (params == null)
            return "";
        StringBuilder str = new StringBuilder(16);
        params.entrySet().forEach((kvp) -> {
            String key = kvp.getKey();
            if (!(key.equals("D") || key.equals("S") || key.equals("F") || key.equals("T"))) {
                if (str.length() > 0)
                    str.append(" ");
                str.append(key).append("=").append(kvp.getValue());
            }
        });
        return str.toString();
    }

    public String getSignalName(/*long fOverride*/) throws IrpException, IrCoreException, ParseException {
        Map<String, Long> parameters = getParameters(/*fOverride*/);
        return this.signalNameFormatter.format(protocolName, parameters);
    }

    public Map<String, Long> getParameters() throws IrpException, IrCoreException, ParseException {
        InputVariableSetValues parameterSets = getIntervalParameters();
        Iterator<LinkedHashMap<String, Long>> it = parameterSets.iterator();
        return it.hasNext() ? it.next() : null;
    }

    private void processParameter(Map<String, String> parameters, String name, JTextField textField) {
        if (protocol.hasParameter(name)
                && !(protocol.getParameterDefault(name) != null && textField.getText().trim().isEmpty()))
            parameters.put(name, textField.getText());
    }

    public InputVariableSetValues getIntervalParameters() throws UnknownProtocolException, NameUnassignedException, InvalidArgumentException, UnsupportedRepeatException, InvalidNameException, IrpInvalidArgumentException, ParseException {
        Map<String, String> parameters = new LinkedHashMap<>(4);

        processParameter(parameters, "D", dTextField);
        processParameter(parameters, "S", sTextField);
        processParameter(parameters, "F", fTextField);
        String toggle = (String) toggleComboBox.getModel().getSelectedItem();
        if (!toggle.equals("-"))
            parameters.put("T", toggle);
        String addParams = protocol.hasNonStandardParameters() ? additionalParametersTextField.getText() : null;
        if (addParams != null && !addParams.trim().isEmpty()) {
            String[] str = addParams.trim().split("[ \t]+");
            for (String s : str) {
                String[] q = s.split("=");
                if (q.length == 2)
                    parameters.put(q[0], q[1]);
            }
        }
        RandomValueSet.initRng(); // just to be on the safe side
        return new InputVariableSetValues(parameters, true, protocol);
    }

    public IrSignal render() throws IrpException, IrCoreException, ParseException {
        Map<String, Long> parameters = getParameters();
        IrSignal irSignal = protocol.toIrSignal(parameters);
        return irSignal;
    }

    public Map<String, Command> getCommands() throws IrCoreException, IrpException, ParseException, GirrException {
        InputVariableSetValues intervals = getIntervalParameters();
        LinkedHashMap<String, Command> commands = new LinkedHashMap<>(16);
        for (LinkedHashMap<String, Long> params : intervals) {
            String name = this.signalNameFormatter.format(getProtocolName(), params);
            Command command = new Command(name, null, getProtocolName(), params);
            commands.put(name, command);
        }
        return commands;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        // just to be Javabeans safe
        if (propertyChangeSupport == null)
            super.addPropertyChangeListener(listener);
        else
            propertyChangeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        copyPastePopupMenu = new org.harctoolbox.guicomponents.CopyPastePopupMenu(true);
        copyPopupMenu = new org.harctoolbox.guicomponents.CopyPastePopupMenu();
        protocolComboBox = new javax.swing.JComboBox<>();
        toggleComboBox = new javax.swing.JComboBox<>();
        protocolDocuButton = new javax.swing.JButton();
        dTextField = new javax.swing.JTextField();
        sTextField = new javax.swing.JTextField();
        fTextField = new javax.swing.JTextField();
        additionalParametersTextField = new javax.swing.JTextField();
        irpTextField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        dLabel = new javax.swing.JLabel();
        sLabel = new javax.swing.JLabel();
        fLabel = new javax.swing.JLabel();
        tLabel = new javax.swing.JLabel();
        additionalParametersLabel = new javax.swing.JLabel();
        randomParametersButton = new javax.swing.JButton();

        protocolComboBox.setModel(new DefaultComboBoxModel(irpMasterProtocols()));
        protocolComboBox.setSelectedItem(protocolName);
        protocolComboBox.setToolTipText("Name of protocol");
        protocolComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolComboBoxActionPerformed(evt);
            }
        });

        toggleComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "-", "0", "1", "*" }));
        toggleComboBox.setToolTipText("toggle code");
        toggleComboBox.setEnabled(false);
        toggleComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleComboBoxActionPerformed(evt);
            }
        });

        protocolDocuButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/mimetypes/info.png"))); // NOI18N
        protocolDocuButton.setMnemonic('D');
        protocolDocuButton.setText("Docu");
        protocolDocuButton.setToolTipText("Display notes to the selected protocol");
        protocolDocuButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolDocuButtonActionPerformed(evt);
            }
        });

        dTextField.setToolTipText("device number");
        dTextField.setComponentPopupMenu(copyPastePopupMenu);
        dTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                dTextFieldFocusLost(evt);
            }
        });
        dTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dTextFieldActionPerformed(evt);
            }
        });

        sTextField.setToolTipText("subdevice number");
        sTextField.setComponentPopupMenu(copyPastePopupMenu);
        sTextField.setEnabled(false);
        sTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                sTextFieldFocusLost(evt);
            }
        });
        sTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sTextFieldActionPerformed(evt);
            }
        });

        fTextField.setToolTipText("function number (OBC)");
        fTextField.setComponentPopupMenu(copyPastePopupMenu);
        fTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fTextFieldActionPerformed(evt);
            }
        });
        fTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                fTextFieldFocusLost(evt);
            }
        });

        additionalParametersTextField.setToolTipText("Other parameters than D, S, F, T, in syntayx variable1=val1 variable2=val2");
        additionalParametersTextField.setComponentPopupMenu(copyPastePopupMenu);
        additionalParametersTextField.setEnabled(false);
        additionalParametersTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                additionalParametersTextFieldFocusLost(evt);
            }
        });
        additionalParametersTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                additionalParametersTextFieldActionPerformed(evt);
            }
        });

        irpTextField.setEditable(false);
        irpTextField.setText("{38.4k,564}<1,-1|1,-3>(16,-8,D:8,S:8,F:8,~F:8,1,^108m,(16,-4,1,^108m)*) [D:0..255,S:0..255=255-D,F:0..255]");
        irpTextField.setToolTipText("IRP notation of the selected protocol");
        irpTextField.setComponentPopupMenu(copyPopupMenu);

        jLabel1.setText("Protocol");

        dLabel.setText("D");

        sLabel.setText("S");
        sLabel.setEnabled(false);

        fLabel.setText("F");

        tLabel.setText("T");
        tLabel.setEnabled(false);

        additionalParametersLabel.setText("Additional parameters");
        additionalParametersLabel.setEnabled(false);

        randomParametersButton.setMnemonic('R');
        randomParametersButton.setText("Random params");
        randomParametersButton.setToolTipText("Assign random, but for the protocol valid, values to all parameters.");
        randomParametersButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                randomParametersButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(irpTextField)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(protocolComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(dTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(sLabel)
                            .addComponent(sTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(fLabel)
                            .addComponent(fTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tLabel)
                            .addComponent(toggleComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(additionalParametersLabel)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(additionalParametersTextField)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(randomParametersButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(protocolDocuButton)))))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {dTextField, fTextField, sTextField});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(dLabel)
                    .addComponent(sLabel)
                    .addComponent(fLabel)
                    .addComponent(tLabel)
                    .addComponent(additionalParametersLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(randomParametersButton)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(protocolComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(toggleComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(protocolDocuButton)
                        .addComponent(dTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(sTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(fTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(additionalParametersTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(irpTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {protocolDocuButton, randomParametersButton});

    }// </editor-fold>//GEN-END:initComponents

    private void protocolDocuButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolDocuButtonActionPerformed
        StringBuilder str = new StringBuilder(irpDatabase.getIrp(protocolName)).append("\n\n");
        try {
            String docu = irpDatabase.getDocumentation(protocolName);
            if (docu != null)
                str.append(docu);
            HelpPopup.newHelpPopup(frame, str.toString());
        } catch (UnknownProtocolException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_protocolDocuButtonActionPerformed

    private void protocolComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolComboBoxActionPerformed
        try {
            String oldProtocolName = protocolName;
            protocolName = (String) protocolComboBox.getSelectedItem();
            setupProtocol(protocolName, invalidParameterString, invalidParameterString, invalidParameterString, invalidParameterString, "");
            propertyChangeSupport.firePropertyChange(PROP_PROTOCOL_NAME, oldProtocolName, protocolName);
        } catch (InvalidNameException | IrpInvalidArgumentException | NameUnassignedException | UnknownProtocolException | UnsupportedRepeatException ex) {
            guiUtils.error(ex);
        }
    }//GEN-LAST:event_protocolComboBoxActionPerformed

    private void dTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dTextFieldActionPerformed
        String oldD = D;
        D = dTextField.getText();
        // TODO: Validity check?
        propertyChangeSupport.firePropertyChange(PROP_D, oldD, D);
    }//GEN-LAST:event_dTextFieldActionPerformed

    private void sTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sTextFieldActionPerformed
        String oldS = S;
        S = sTextField.getText();
        // TODO: Validity check?
        propertyChangeSupport.firePropertyChange(PROP_S, oldS, S);
    }//GEN-LAST:event_sTextFieldActionPerformed

    private void fTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fTextFieldActionPerformed
        String oldF = F;
        F = fTextField.getText();
        // TODO: Validity check?
        propertyChangeSupport.firePropertyChange(PROP_F, oldF, F);
    }//GEN-LAST:event_fTextFieldActionPerformed

    private void dTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_dTextFieldFocusLost
        dTextFieldActionPerformed(null);
    }//GEN-LAST:event_dTextFieldFocusLost

    private void sTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_sTextFieldFocusLost
        sTextFieldActionPerformed(null);
    }//GEN-LAST:event_sTextFieldFocusLost

    private void fTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_fTextFieldFocusLost
        fTextFieldActionPerformed(null);
    }//GEN-LAST:event_fTextFieldFocusLost

    private void toggleComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_toggleComboBoxActionPerformed
        String oldT = T;
        T = (String) toggleComboBox.getSelectedItem();
        propertyChangeSupport.firePropertyChange(PROP_T, oldT, T);
    }//GEN-LAST:event_toggleComboBoxActionPerformed

    private void additionalParametersTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_additionalParametersTextFieldActionPerformed
        String oldParams = additionalParameters;
        additionalParameters = additionalParametersTextField.getText().trim();
        // TODO: Validity check?
        propertyChangeSupport.firePropertyChange(PROP_ADDITIONAL_PARAMS, oldParams, additionalParameters);
    }//GEN-LAST:event_additionalParametersTextFieldActionPerformed

    private void additionalParametersTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_additionalParametersTextFieldFocusLost
        additionalParametersTextFieldActionPerformed(null);
    }//GEN-LAST:event_additionalParametersTextFieldFocusLost

    private void randomParametersButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_randomParametersButtonActionPerformed
        Map<String, Long> params = protocol.randomParameters();
        setParameters(params);
    }//GEN-LAST:event_randomParametersButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel additionalParametersLabel;
    private javax.swing.JTextField additionalParametersTextField;
    private org.harctoolbox.guicomponents.CopyPastePopupMenu copyPastePopupMenu;
    private org.harctoolbox.guicomponents.CopyPastePopupMenu copyPopupMenu;
    private javax.swing.JLabel dLabel;
    private javax.swing.JTextField dTextField;
    private javax.swing.JLabel fLabel;
    private javax.swing.JTextField fTextField;
    private javax.swing.JTextField irpTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JComboBox<String> protocolComboBox;
    private javax.swing.JButton protocolDocuButton;
    private javax.swing.JButton randomParametersButton;
    private javax.swing.JLabel sLabel;
    private javax.swing.JTextField sTextField;
    private javax.swing.JLabel tLabel;
    private javax.swing.JComboBox<String> toggleComboBox;
    // End of variables declaration//GEN-END:variables
}
