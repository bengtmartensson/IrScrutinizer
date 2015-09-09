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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.ModulatedIrSequence;

// TODO: Generation of hardcopy, svg, postscript, etc.
// TODO: Copy to clipboard.
// TODO: Check scaling.
/**
  * A plotter class, specialized for plotting IR signals. Not yet implemented: Printing, clipboard support.
  */
@SuppressWarnings("serial")
public class IrPlotter extends HarcPanel {
    private static final String versionString = "0.1.0";
    // FIXME: This help text is not really generically usable.
    private static final String helpText = "This window shows a frozen copy of a signal originally shown in the plot window of IrScrutinizer.";

    private static final String aboutText =
            "IrPlotter version " + versionString + ".\n"
            + "Copyright 2013-2015 by Bengt Martensson.\n\n"
            + "License: GPL3.\n\n"
            + "Project home page: http://www.harctoolbox.org";

    /** max horizontal coordinate, in microseconds */
    private int xmin = 0;
    private int xmax = 200000;
    private int[] irdata = null;
    private int noIntroBursts;
    private int noRepeatBursts;
    private int noRepeats;
    private int onY;
    private int offY;
    private int dragBeginX = invalid;
    private int dragEndX = invalid;

    private JMenu plotterWidthsMenu;
    private JPopupMenu plotterPopupMenu;

    private final static int topMargin = 10;
    private final static int bottomMargin = 40;
    private final static int leftMargin = 15;
    private final static int rightMargin = 15;
    private final static int tickBarLength = 10;
    private final static int charWidth = 8; // This is very simplistic, but good enough here.
    private final static int numberOffset = 30;

    private final static int pixelsPerTick = 75;
    private final static int preferredWidth = 400;
    private final static int preferredHeight = 50;
    private final static int dragThreshold = 20;

    private final static Color plotBackgroundColor = Color.WHITE;
    private final static Color plotDraggingBackgroundColor = Color.DARK_GRAY;
    private final static Color tickMarkColor = Color.GRAY;
    private final static Color numberColor = Color.BLACK;
    private final static Color introColor = Color.RED;
    private final static Color repeatColor = Color.BLUE;
    private final static Color endingColor = Color.GREEN;

    private boolean usePulseAsTiming = false;
    private boolean ignoreLast = false;
    private double frequency;

    private boolean useMilliSeconds = false;

    private final static int invalid = -1;
    private final int[] plotterWidths = { 1000, 1500, 2000, 2500, 3000, 4000, 5000, 6000, 7000, 8000, 10000 };

    public IrPlotter(boolean zoomSupport) {
        initializePlotterWidthsMenu();
        initializePlotterMenu();
        if (zoomSupport)
            initializeMouse();
    }

    public IrPlotter() {
        this(true);
    }

    public IrPlotter(IrPlotter irPlotter, boolean zoomSupport) {
        this(zoomSupport);
        this.noIntroBursts = irPlotter.noIntroBursts;
        this.noRepeatBursts = irPlotter.noRepeatBursts;
        this.noRepeats = irPlotter.noRepeats;
        irdata = new int[irPlotter.irdata.length];
        System.arraycopy(irPlotter.irdata, 0, this.irdata, 0, irPlotter.irdata.length);
        this.frequency = irPlotter.frequency;
        this.ignoreLast = irPlotter.ignoreLast;
        //resetXminXmax();
        this.xmin = irPlotter.xmin;
        this.xmax = irPlotter.xmax;
        repaint();
    }

    /**
     * Called to setup the plotter to plot the IrSequence given as argument.
     * @param irdata
     * @param noIntroBursts
     * @param frequency
     * @param noRepeats
     * @param repeatBursts
     */
    private void plot(int[] irdata, int noIntroBursts, int noRepeatBursts, int noRepeats, double frequency) {
        this.noIntroBursts = noIntroBursts;
        this.noRepeatBursts = noRepeatBursts;
        this.noRepeats = noRepeats;
        this.irdata = irdata;
        this.frequency = frequency;
        resetXminXmax();
    }

    /**
     * Called to setup the plotter to plot the arguments.
     * @param irSequence
     * @param noIntroBursts
     * @param noRepeatBursts
     * @param noRepetitions
     */
    public void plot(ModulatedIrSequence irSequence, int noIntroBursts, int noRepeatBursts, int noRepetitions) {
        plot(irSequence.toInts(), noIntroBursts, noRepeatBursts, noRepetitions, irSequence.getFrequency());
        repaint();
    }

    /**
     * Called to setup the plotter to plot the argument.
     * @param irSequence
     */
    public void plot(ModulatedIrSequence irSequence) {
        plot(irSequence, irSequence.getNumberBursts(), 0, 0);
    }

    /**
     * Called to setup the plotter to plot the argument.
     * @param irSignal
     */
    public void plot(IrSignal irSignal) {
        plot(irSignal.toModulatedIrSequence(true, 1, true), irSignal.getIntroBursts(), irSignal.getRepeatBursts(), 1);
    }

    /**
     * Clears plot.
     */
    public void clear() {
        plot(null, 0, 0, 0, 0d);
        reset();
    }

    public boolean isEmpty() {
        return irdata == null || irdata.length == 0;
    }

    /**
     * Shows the plot x-axis as a function of modulation periods, not as time. Not yet implemented.
     * @param val true for pulses, false for time.
     * @throws IncompatibleArgumentException If pulses selected, but frequency is zero, making pulse time undefined.
     */
    public void setPulseAsTiming(boolean val) throws IncompatibleArgumentException {
        if (val && frequency <= 0)
            throw new IncompatibleArgumentException("");
        usePulseAsTiming = val;
    }

    /**
     * Makes the last point, which is always a silence, be omitted from the plot.
     * @param ignoreLast
     */
    public void setIgnoreLast(boolean ignoreLast) {
        if (this.ignoreLast != ignoreLast) {
            this.ignoreLast = ignoreLast;
            resetXminXmax();
            repaint();
        }
    }

    private void setDragBegin(int x) {
        dragBeginX = Math.max(x, leftMargin);
        //System.err.println("Start: "  + x + " " + screenX2x(x));
    }

    private void setDragEnd(int x) {
        if (dragBeginX != invalid && Math.abs(dragBeginX - x) > dragThreshold) {
            xmin = screenX2x(Math.min(dragBeginX, x));
            xmax = screenX2x(Math.max(dragBeginX, x));
            //System.err.println("End: " + x + " " + screenX2x(x));
        }

        dragBeginX = invalid;
        dragEndX = invalid;
        repaint();
    }

    private void drag(int x) {
        //System.err.println("Drag "  + x + " " + screenX2x(x));
        if (dragBeginX != invalid) {
            dragEndX = x;
            repaint();
        }
    }

    private void resetXminXmax() {
        xmin = 0;
        xmax = 0;
        if (irdata != null)
            for (int i = 0; i < (ignoreLast ? irdata.length - 1 : irdata.length); i++)
                xmax += Math.abs(irdata[i]);
    }

    /**
     * Resets the size and the axis of the plotting domain.
     */
    public void reset() {
        resetSize();
        resetXminXmax();
    }

    /**
     * Resets the size of the widget to its preferred size.
     */
    public void resetSize() {
        setMinimumSize(new Dimension(0, 0));
        setMaximumSize(new Dimension(32000, 32000));
        setSize(new Dimension(preferredWidth, preferredHeight));
        setPreferredSize(new Dimension(preferredWidth, preferredHeight));
    }

    /**
     * Sets the width of the plotter frame.
     * @param width
     */
    public void setWidth(int width) {
        if (width == 0) {
            reset();
            repaint();
        } else {
            resetXminXmax();
            Dimension d = new Dimension(width, getHeight());
            setMinimumSize(d);
            setMaximumSize(d);
            setPreferredSize(d);
            setSize(d);
            repaint();
        }
    }

    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        onY = topMargin;
        offY = getHeight() - bottomMargin;
        drawBackground(graphics);
        drawDraggingBackground(graphics);
        drawTicks(graphics);
        drawPlot(graphics);
    }

    private void drawBackground(Graphics graphics) {
        graphics.setColor(plotBackgroundColor);
        graphics.fillRect(leftMargin, onY, getWidth() - leftMargin - rightMargin, getHeight() - topMargin - bottomMargin);
    }

    private void drawDraggingBackground(Graphics graphics) {
        if (dragBeginX != invalid && dragEndX != invalid) {
            int minX = Math.min(dragBeginX, dragEndX);
            int maxX = Math.max(dragBeginX, dragEndX);
            graphics.setColor(plotDraggingBackgroundColor);
            graphics.fillRect(minX, onY, maxX - minX, getHeight() - topMargin - bottomMargin);
        }
    }

    private void drawPlot(Graphics graphics) {
        if (irdata == null) {
            //graphics.drawString("No data available", 100, 50);
            return;
        }

        int t = 0;
        graphics.setColor(introColor);
        for (int i = 0; i < irdata.length && t <= xmax; i++) {
            boolean drawTransition = true;
            int screenXStart = x2screenX(t);
            if (screenXStart < leftMargin) {
                screenXStart = leftMargin;
                drawTransition = false;
            }
            t += Math.min(irdata[i], xmax);
            int screenXEnd = x2screenX(t);
            if (screenXEnd < leftMargin)
                continue;

            if (i == 2*noIntroBursts)
                graphics.setColor(repeatColor);
            if (i == 2*(noIntroBursts + noRepeats*noRepeatBursts))
                graphics.setColor(endingColor);
            if (i % 2 == 0) {
                // Pulse
                if (drawTransition) // Draw vertical line only if real transition
                    graphics.drawLine(screenXStart, offY, screenXStart, onY);
                graphics.drawLine(screenXStart, onY, screenXEnd, onY);
            } else {
                // Gap
                if (drawTransition || (ignoreLast && i == irdata.length - 1))
                    graphics.drawLine(screenXStart, onY, screenXStart, offY);
                if (!ignoreLast || i != irdata.length - 1)
                    graphics.drawLine(screenXStart, offY, screenXEnd, offY); // ignore
            }
        }
    }

    private int[] getTickValues(int xmin, int xmax, int pixelWidth) {
        if (xmin == xmax)
            return new int[0];
        double goalNoTicks = ((double)pixelWidth)/pixelsPerTick;
        double goalWidth = ((double)(xmax-xmin))/goalNoTicks; // microseconds
        int tickWidth = rounder((int)Math.round(goalWidth));
        useMilliSeconds = tickWidth >= 5000;
        int newXmin = (int) ((double) xmin)/tickWidth*tickWidth;
        this.xmin = newXmin;
        int noTicks = (int) Math.ceil(((double)(xmax-newXmin))/tickWidth) + 1;
        if (noTicks <= 0) {
            System.err.println(">>>>>>>>>>>>>>" + xmax + " " + newXmin + " " + tickWidth + " " + goalWidth);
            return new int[0];
        }
        int[] result = new int[noTicks];
        for (int i = 0; i < noTicks; i++)
            result[i] = newXmin + i*tickWidth;
        return result;
    }

    private int[] getTickValues() {
        return getTickValues(xmin, xmax, getWidth() - leftMargin - rightMargin);
    }

    private int rounder(double x) {
        double log = Math.log10(x);
        int divisor = pow10((int)log);
        int r = (int) Math.round(x/divisor);
        r *= divisor;
        //System.err.println(x + "->" + r);
        return r == 0 ? 1 : r;
    }

    private int pow10(int x) {
        return x <= 0 ? 1 : 10*pow10(x-1);
    }

    private int x2screenX(int x) {
        return (int) Math.round(((double)(x - xmin))/(xmax - xmin) * (getWidth() - leftMargin - rightMargin) + leftMargin);
    }

    private int screenX2x(int X) {
        return (int) Math.round(((double)(X - leftMargin))/(getWidth() - leftMargin - rightMargin) * (xmax - xmin) + xmin);
    }

    private void drawTicks(Graphics graphics) {
        int[] tickValues = getTickValues();
        for (int i = 0; i < tickValues.length; i++)
            drawTick(tickValues[i], graphics);
    }

    private void drawTick(int x, Graphics graphics) {
        graphics.setColor(tickMarkColor);
        graphics.drawLine(x2screenX(x), offY + tickBarLength, x2screenX(x), offY);
        String str = Integer.toString(useMilliSeconds ? x/1000 : x);
        graphics.setColor(numberColor);
        int strLength = charWidth*str.length(); // silly, but accurate enough here
        int xcoord = x2screenX(x) - strLength/2 < leftMargin ? leftMargin
                : x2screenX(x) + strLength/2 > getWidth() - rightMargin ? getWidth() - rightMargin - strLength
                : x2screenX(x) - strLength/2;

        graphics.drawString(str, xcoord, offY+numberOffset);
    }

    private void initializeMouse() {
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (evt.getButton() == MouseEvent.BUTTON1)
                    setDragBegin(evt.getX());
                else if (evt.getButton() == MouseEvent.BUTTON3)
                    if (evt.isPopupTrigger())
                        plotterPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (evt.getButton() == MouseEvent.BUTTON1)
                    setDragEnd(evt.getX());
                else if (evt.getButton() == MouseEvent.BUTTON3)
                    if (evt.isPopupTrigger())
                        plotterPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
            }
        });

        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                drag(evt.getX());
            }
        });
    }

    private void initializePlotterMenu() {
        plotterPopupMenu = new JPopupMenu();
        JMenuItem resetMenuItem = new JMenuItem();
        resetMenuItem.setText("Reset plotter");
        resetMenuItem.setToolTipText("Reset the axis of the plotter");
        //resetMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, 0));
        resetMenuItem.setMnemonic('R');
        resetMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reset();
            }
        });
        plotterPopupMenu.add(resetMenuItem);
        plotterPopupMenu.add(plotterWidthsMenu);
        JMenuItem cloneMenuItem = new JMenuItem();
        cloneMenuItem.setText("Clone Plot");
        cloneMenuItem.setToolTipText("Create a frozen clone of the current plot in a popup window.");
        cloneMenuItem.setMnemonic('C');
        cloneMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createPopupClone();
            }
        });
        plotterPopupMenu.add(cloneMenuItem);
    }

    /**
     * Create a read-only clone of the current plot in a new popup window.
     */
    public void createPopupClone() {
        if (isEmpty()) {
            //guiUtils.error("No plot to clone.");
            return;
        }
        IrPlotter clone = new IrPlotter(this, false);
        clone.setPreferredSize(getSize());
        HarcletFrame.newHarcletFrame(this, clone, false, null);
    }

    private void initializePlotterWidthsMenu() {
        plotterWidthsMenu = new JMenu();//.removeAll();
        plotterWidthsMenu.setText("Set width");
        plotterWidthsMenu.setToolTipText("Set width of the plotter explicitly. It can then be horizontally scrolled.");
        for (int width : plotterWidths) {
            JMenuItem menuItem = new JMenuItem(Integer.toString(width));
            final int localWidth = width;
            menuItem.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    //properties.setPlotterWidth(localWidth);
                    setWidth(localWidth);
                }
            });

            plotterWidthsMenu.add(menuItem);
        }
    }

    @Override
    public String getHelpMessage() {
        return helpText;
    }

    @Override
    public String getAboutMessage() {
        return aboutText;
    }

    @Override
    public String getProgName() {
        return "IrPlotter";
    }

    @Override
    public String getIconPath() {
        return "/icons/Crystal-Clear/22x22/actions/pert_chart.png";
    }
}
