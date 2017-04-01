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

package org.harctoolbox.guicomponents;

import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;
import org.harctoolbox.IrpMaster.Wave;
import org.harctoolbox.harchardware.ir.IrAudioDevice;
import org.harctoolbox.irscrutinizer.Props;

public class AudioParametersBean extends javax.swing.JPanel {

    private final static AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
    private final static boolean bigEndian = false;
    private final static boolean divideCarrier = true;
    private final static boolean square = true;
    private boolean verbose;
    private final Props properties;
    private final PropertyChangeSupport propertyChangeSupport;
    private final static String PROP_SAMPLEFREQUENCY = "PROP_SAMPLEFREQUENCY";
    private final static String PROP_NOCHANNELS = "PROP_NOCHANNELS";
    private final static String PROP_SAMPLESIZE = "PROP_SAMPLESIZE";
    private final static String PROP_OMITTRAILINGGAP = "PROP_OMITTRAILINGGAP";
    private transient IrAudioDevice hardware;

    /**
     * Creates new form AudioParametersBean
     */
    public AudioParametersBean() {
        this(null);
    }

    /**
     * Creates new form AudioParametersBean
     * @param properties
     */
    public AudioParametersBean(Props properties) {
        this.verbose = false;
        this.properties = properties;
        initComponents();
        this.propertyChangeSupport = new java.beans.PropertyChangeSupport(this);
    }

    /**
     * @param verbose the verbose to set
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * @return the sampleSize
     */
    public int getSampleSize() {
        return Integer.parseInt((String) sampleSizeComboBox.getSelectedItem());
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
        return Integer.parseInt((String) channelsComboBox.getSelectedItem());
    }

    public int getSampleFrequency() {
        return Integer.parseInt((String) sampleFrequencyComboBox.getSelectedItem());
    }

    public boolean getOmitTrailingGap() {
        return omitTrailingGapCheckBox.isSelected();
    }

    private void setSampleSize(int sampleSize) {
        int old = Integer.parseInt((String) sampleSizeComboBox.getSelectedItem());
        sampleSizeComboBox.setSelectedItem(Integer.toString(sampleSize));

        propertyChangeSupport.firePropertyChange(PROP_SAMPLESIZE, old, sampleSize);
        if (properties != null)
            properties.setWaveSampleSize(sampleSize);
        if (hardware != null)
            hardware.setSampleSize(sampleSize);
    }

    private void setSampleFrequency(int sampleFrequency) {
        int old = Integer.parseInt((String) sampleFrequencyComboBox.getSelectedItem());
        sampleSizeComboBox.setSelectedItem(Integer.toString(sampleFrequency));

        propertyChangeSupport.firePropertyChange(PROP_SAMPLEFREQUENCY, old, sampleFrequency);
        if (properties != null)
            properties.setWaveSampleFrequency(sampleFrequency);
        if (hardware != null)
            hardware.setSampleFrequency(sampleFrequency);
    }

    private void setChannels(int channels) {
        int old = Integer.parseInt((String) channelsComboBox.getSelectedItem());
        channelsComboBox.setSelectedItem(Integer.toString(channels));

        propertyChangeSupport.firePropertyChange(PROP_NOCHANNELS, old, channels);
        if (properties != null)
            properties.setWaveChannels(channels);
        if (hardware != null)
            hardware.setChannels(channels);
    }

    private void setOmitTrailingGap(boolean omitTrailingGap) {
        boolean old = omitTrailingGapCheckBox.isSelected();
        omitTrailingGapCheckBox.setSelected(omitTrailingGap);

        propertyChangeSupport.firePropertyChange(PROP_OMITTRAILINGGAP, old, omitTrailingGap);
        if (properties != null)
            properties.setWaveOmitTrailingGap(omitTrailingGap);
        if (hardware != null)
            hardware.setOmitTail(omitTrailingGap);
    }

    public AudioFormat getAudioFormat() {
        return new AudioFormat(encoding, getSampleFrequency(), getSampleSize(), getChannels(),
                getSampleSize()/8*getChannels(), getSampleFrequency(), getBigEndian());
    }

    public Wave newWave(ModulatedIrSequence irSequence) throws IncompatibleArgumentException {
        return new Wave(irSequence, getAudioFormat(), getOmitTrailingGap(), getSquare(), getDivideCarrier());
    }

    public void setupHardware() {
        hardware = new IrAudioDevice(getSampleFrequency(), getSampleSize(), getChannels(), getOmitTrailingGap(), verbose);
    }

    public IrAudioDevice getHardware() {
        return hardware;
    }

    public void export(File file, ModulatedIrSequence irSequence) throws IncompatibleArgumentException {
        newWave(irSequence).export(file);
    }

    public void play(ModulatedIrSequence irSequence) throws LineUnavailableException, IncompatibleArgumentException, IOException {
        newWave(irSequence).play();
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

        sampleFrequencyComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "44100", "48000", "96000", "192000" }));
        sampleFrequencyComboBox.setSelectedItem(properties != null ? Integer.toString(properties.getWaveSampleFrequency()) : "48000");
        sampleFrequencyComboBox.setToolTipText("Sample frequency of the generated signal.");
        sampleFrequencyComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sampleFrequencyComboBoxActionPerformed(evt);
            }
        });

        channelsComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2" }));
        channelsComboBox.setSelectedItem(properties != null ? Integer.toString(properties.getWaveChannels()) : "1");
        channelsComboBox.setToolTipText("Number of channnels in generated signal. Normal is one channel. If two, the second channel is just exactly the opposite of the first.");
        channelsComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                channelsComboBoxActionPerformed(evt);
            }
        });

        omitTrailingGapCheckBox.setMnemonic('O');
        omitTrailingGapCheckBox.setSelected(properties != null ? properties.getWaveOmitTrailingGap() : false);
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
        sampleSizeComboBox.setSelectedItem(properties != null ? Integer.toString(properties.getWaveSampleSize()) : "8");
        sampleSizeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sampleSizeComboBoxActionPerformed(evt);
            }
        });

        jLabel3.setText("Bits");

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
                        .addComponent(omitTrailingGapCheckBox))
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                    .addComponent(sampleSizeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> channelsComboBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JCheckBox omitTrailingGapCheckBox;
    private javax.swing.JComboBox<String> sampleFrequencyComboBox;
    private javax.swing.JComboBox<String> sampleSizeComboBox;
    // End of variables declaration//GEN-END:variables
}
