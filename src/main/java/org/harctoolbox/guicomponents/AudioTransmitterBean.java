/*
Copyright (C) 2013, 2021 Bengt Martensson.

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

package org.harctoolbox.guicomponents;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.ir.IrAudioDevice;
import static org.harctoolbox.harchardware.ir.IrAudioDevice.DEFAULT_CHANNELS;
import static org.harctoolbox.harchardware.ir.IrAudioDevice.DEFAULT_SAMPLE_FREQUENCY;
import static org.harctoolbox.harchardware.ir.IrAudioDevice.DEFAULT_SAMPLE_SIZE;
import org.harctoolbox.harchardware.ir.Wave;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.irscrutinizer.HardwareUnavailableException;
import org.harctoolbox.irscrutinizer.exporter.WaveExporter;

public class AudioTransmitterBean extends HardwareBean {

    // These parameters are sort-of "symbolic".
    // They allow for custiomization of the result, although presently the GUI does not allow for changing them.
    private final static AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
    private final static boolean bigEndian = false;
    private final static boolean divideCarrier = true;
    private final static boolean square = true;

    public final static String PROP_SAMPLEFREQUENCY =  "PROP_SAMPLEFREQUENCY";
    public final static String PROP_NOCHANNELS =       "PROP_NOCHANNELS";
    public final static String PROP_SAMPLESIZE =       "PROP_SAMPLESIZE";
    public final static String PROP_OMITTRAILINGGAP =  "PROP_OMITTRAILINGGAP";

    private int sampleSize;
    private int channels;
    private int sampleFrequency;
    private boolean omitTrailingGap;

    /**
     * Creates new form AudioTransmitterBean
     */
    public AudioTransmitterBean() {
        this(null);
    }

    /**
     * Creates new form AudioParametersBean
     * @param properties
     */
    public AudioTransmitterBean(GuiUtils guiUtils) {
        this(guiUtils, false, DEFAULT_SAMPLE_FREQUENCY, DEFAULT_CHANNELS, DEFAULT_SAMPLE_SIZE, false);
    }

    /**
     * Creates new form AudioParametersBean
     * @param properties
     * @param verbose
     */
    public AudioTransmitterBean(GuiUtils guiUtils, boolean verbose, int sampleFrequency, int channels, int sampleSize, boolean omitTrailingGap) {
        super(guiUtils, verbose, 0);
        initComponents();
        setSampleFrequency(sampleFrequency);
        setSampleSize(sampleSize);
        setChannels(channels);
        setOmitTrailingGap(omitTrailingGap);
    }

    /**
     * @return the sampleSize
     */
    public int getSampleSize() {
        return sampleSize;
    }

    /**
     * @return the bigEndian
     */
    public boolean getBigEndian() {
        return bigEndian;
    }

    /**
     * @return the divideCarrier
     */
    public boolean getDivideCarrier() {
        return divideCarrier;
    }

    /**
     * @return the square
     */
    public boolean getSquare() {
        return square;
    }

    public int getChannels() {
        return channels;
    }

    public int getSampleFrequency() {
        return sampleFrequency;
    }

    public boolean getOmitTrailingGap() {
        return omitTrailingGap;
    }

    private void setSampleSize(int sampleSize) {
        int old = this.sampleSize;
        this.sampleSize = sampleSize;
        sampleSizeComboBox.setSelectedItem(Integer.toString(sampleSize));

        propertyChangeSupport.firePropertyChange(PROP_SAMPLESIZE, old, sampleSize);
        if (hardware != null)
            ((IrAudioDevice) hardware).setSampleSize(sampleSize);
    }

    private void setSampleFrequency(int sampleFrequency) {
        int old = this.sampleFrequency;
        this.sampleFrequency = sampleFrequency;
        sampleFrequencyComboBox.setSelectedItem(Integer.toString(sampleFrequency));

        propertyChangeSupport.firePropertyChange(PROP_SAMPLEFREQUENCY, old, sampleFrequency);
        if (hardware != null)
            ((IrAudioDevice) hardware).setSampleFrequency(sampleFrequency);
    }

    private void setChannels(int channels) {
        int old = this.channels;
        this.channels = channels;
        channelsComboBox.setSelectedItem(Integer.toString(channels));

        propertyChangeSupport.firePropertyChange(PROP_NOCHANNELS, old, channels);
        if (hardware != null)
            ((IrAudioDevice) hardware).setChannels(channels);
    }

    private void setOmitTrailingGap(boolean omitTrailingGap) {
        boolean old = this.omitTrailingGap;
        this.omitTrailingGap = omitTrailingGap;
        omitTrailingGapCheckBox.setSelected(omitTrailingGap);

        propertyChangeSupport.firePropertyChange(PROP_OMITTRAILINGGAP, old, omitTrailingGap);
        if (hardware != null)
            ((IrAudioDevice) hardware).setOmitTail(omitTrailingGap);
    }

    public AudioFormat getAudioFormat() {
        return new AudioFormat(encoding, getSampleFrequency(), getSampleSize(), getChannels(),
                getSampleSize()/8*getChannels(), getSampleFrequency(), getBigEndian());
    }

    public Wave newWave(ModulatedIrSequence irSequence) throws InvalidArgumentException {
        return new Wave(irSequence, getAudioFormat(), getOmitTrailingGap(), getSquare(), getDivideCarrier());
    }

    public void setupHardware() {
        hardware = new IrAudioDevice(getSampleFrequency(), getSampleSize(), getChannels(), getOmitTrailingGap(), verbose);
    }

    public void export(File file, ModulatedIrSequence irSequence) throws InvalidArgumentException {
        newWave(irSequence).export(file);
    }

    public void play(ModulatedIrSequence irSequence) throws LineUnavailableException, IOException, InvalidArgumentException {
        newWave(irSequence).play();
    }

    public WaveExporter newWaveExporter() {
        return new WaveExporter(getSampleFrequency(), getSampleSize(),
                getChannels(), getBigEndian(),
                getOmitTrailingGap(), getSquare(),
                getDivideCarrier());
    }

    @Override
    public String getName() {
        return "Audio Port";
    }

    /**
     * Default implementation, override whenever the subclass can send.
     * @return false
     */
    @Override
    public boolean canSend() {
        return true;
    }

    @Override
    public boolean sendIr(IrSignal irSignal, int count) throws HardwareUnavailableException, HarcHardwareException, IOException, InvalidArgumentException {
        assertHardwareValid();
        return ((IrAudioDevice) hardware).sendIr(irSignal, count, null);
    }

    @Override
    public void open() throws IOException, HarcHardwareException {
        if (hardware == null)
            setupHardware();
        hardware.open();
    }

    @Override
    public void close() throws IOException {
        if (hardware != null)
            hardware.close();
        hardware = null;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sampleFrequencyComboBox = new javax.swing.JComboBox<>();
        channelsComboBox = new javax.swing.JComboBox<>();
        omitTrailingGapCheckBox = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        sampleSizeComboBox = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        openToggleButton = new javax.swing.JToggleButton();

        sampleFrequencyComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "44100", "48000", "96000", "192000" }));
        sampleFrequencyComboBox.setSelectedIndex(1);
        sampleFrequencyComboBox.setToolTipText("Sample frequency of the generated signal.");
        sampleFrequencyComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sampleFrequencyComboBoxActionPerformed(evt);
            }
        });

        channelsComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2" }));
        channelsComboBox.setToolTipText("Number of channnels in generated signal. Normal is one channel. If two, the second channel is just exactly the opposite of the first.");
        channelsComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                channelsComboBoxActionPerformed(evt);
            }
        });

        omitTrailingGapCheckBox.setMnemonic('O');
        omitTrailingGapCheckBox.setText("Omit trailing gap");
        omitTrailingGapCheckBox.setToolTipText("If true, the last silence is left out of the generated wave signal.");
        omitTrailingGapCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                omitTrailingGapCheckBoxActionPerformed(evt);
            }
        });

        jLabel1.setText("Sample freq.");

        jLabel2.setText("# Channels");

        sampleSizeComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "8", "16" }));
        sampleSizeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sampleSizeComboBoxActionPerformed(evt);
            }
        });

        jLabel3.setText("Bits");

        openToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Crystal-Clear/22x22/actions/connect_creating.png"))); // NOI18N
        openToggleButton.setText("Open");
        openToggleButton.setToolTipText("Open or close connection to device.");
        openToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openToggleButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sampleFrequencyComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(channelsComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(9, 9, 9)
                        .addComponent(sampleSizeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(omitTrailingGapCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(openToggleButton, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3)))
                .addContainerGap(125, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sampleFrequencyComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(channelsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(omitTrailingGapCheckBox)
                    .addComponent(sampleSizeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(openToggleButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void sampleSizeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sampleSizeComboBoxActionPerformed
        setSampleSize(Integer.parseInt((String) sampleSizeComboBox.getSelectedItem()));
    }//GEN-LAST:event_sampleSizeComboBoxActionPerformed

    private void sampleFrequencyComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sampleFrequencyComboBoxActionPerformed
        setSampleFrequency(Integer.parseInt((String) sampleFrequencyComboBox.getSelectedItem()));
    }//GEN-LAST:event_sampleFrequencyComboBoxActionPerformed

    private void channelsComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_channelsComboBoxActionPerformed
        setChannels(Integer.parseInt((String) channelsComboBox.getSelectedItem()));
    }//GEN-LAST:event_channelsComboBoxActionPerformed

    private void omitTrailingGapCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_omitTrailingGapCheckBoxActionPerformed
        setOmitTrailingGap(omitTrailingGapCheckBox.isSelected());
    }//GEN-LAST:event_omitTrailingGapCheckBoxActionPerformed

    private void openToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openToggleButtonActionPerformed
        try {
            openClose(openToggleButton.isSelected());
        } catch (IOException | HarcHardwareException ex) {
            guiUtils.error(ex);
        } finally {
            openToggleButton.setSelected(isOpen());
        }
    }//GEN-LAST:event_openToggleButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> channelsComboBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JCheckBox omitTrailingGapCheckBox;
    private javax.swing.JToggleButton openToggleButton;
    private javax.swing.JComboBox<String> sampleFrequencyComboBox;
    private javax.swing.JComboBox<String> sampleSizeComboBox;
    // End of variables declaration//GEN-END:variables
}
