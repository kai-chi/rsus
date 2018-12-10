/*
 * Copyright (c) 2007 Sun Microsystems, Inc.
 * Copyright (c) 2010 Oracle
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.sunspotworld.demo;

import com.sun.spot.peripheral.ota.OTACommandServer;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import javax.swing.*;
import java.util.*;
import java.text.*;

/**
 * GUI creating code to make a window to display accelerometer data gathered
 * from a remote SPOT. Provides the user interface to interact with the SPOT
 * and to control the telemetry data collected.
 *
 * @author Ron Goldman<br>
 * date: May 2, 2006<br>
 * revised: August 1, 2007<br>
 * revised: August 1, 2010
 */
public class TelemetryFrame extends JFrame implements Printable {

    public static String version = "2.0";
    public static String versionDate = "August 1, 2010";
    private static final Font footerFont = new Font("Serif", Font.PLAIN, 9);
    private static final DateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy  HH:mm z");
    
    private AccelerometerListener listener;
    private GraphView graphView;
    private JPanel axisView;
    private boolean sendData = false;
    private File file = null;
    private boolean fixedData = false;
    private boolean clearedData = true;

    private PrinterJob printJob = PrinterJob.getPrinterJob();
    private PageFormat pageFormat = printJob.defaultPage();

    private int[] scales = { 2, 6 };
    private int currentScale = 0;

    /**
     * Check that new window has a unique name.
     *
     * @param str proposed new window name
     * @return true if current name is unique, false if it is the same as another window
     */
    private boolean checkTitle(String str) {
        boolean results = true;
        for (Enumeration e = SpotListener.getWindows().elements() ; e.hasMoreElements() ;) {
            JFrame fr = (JFrame)e.nextElement();
            if (str.equals(fr.getTitle())) {
                results = false;
                break;
            }
        }
        return results;
    }

    /**
     * Creates a new TelemetryFrame window.
     *
     * @param ieee address of SPOT
     */
    public TelemetryFrame(String ieee) {
        init(ieee, null, null);
    }
    
    /**
     * Creates a new TelemetryFrame window with an associated file.
     *
     * @param file the file to read/write accelerometer data from/to
     */
    public TelemetryFrame(File file, GraphView graphView) {
        init(null, file, graphView);
    }

    /**
     * Initialize the new TelemetryFrame
     */
    private void init(String ieee, File file, GraphView graphView) {
        initComponents();
        setupAcceleratorKeys();
        if (System.getProperty("os.name").toLowerCase().startsWith("mac os x")) {
            aboutMenuItem.setVisible(false);
            quitMenuItem.setVisible(false);
            jSeparator2.setVisible(false);
        }

        if (graphView == null) {
            setGraphView(new GraphView());
        } else {
            setGraphView(graphView);
        }
        if (ieee != null) {
            this.setTitle("SPOT " + ieee);
            listener = new AccelerometerListener(ieee, this, graphView);
            listener.start();
            setConnectionStatus(true, "Connected");
        }
        this.file = file;
        if (file != null) {
            this.setTitle(file.getName());
            fixedData = true;
            clearedData = false;
            blinkButton.setVisible(false);
        } else {
        }
        pageFormat.setOrientation(PageFormat.LANDSCAPE);

        setVisible(true);
        String str = getTitle();
        if (!checkTitle(str)) {
            int i = 1;
            while (true) {
                if (checkTitle(str + "-" + i)) {
                    setTitle(str + "-" + i);
                    break;
                } else {
                    i++;
                }
            }
        }
        SpotListener.addFrame(this);
    }
    
    /**
     * Make sure the correct command key is used.
     */
    private void setupAcceleratorKeys() {
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        closeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, mask));
        quitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, mask));
    }

    /**
     * Set the GraphView to display accelerometer values for this window.
     */
    private void setGraphView(GraphView gv) {
        graphView = gv;
        Integer fieldWidth = (Integer)nodeNumberField.getValue();
        graphView.setFilterWidth(fieldWidth.intValue() - 1);
        final GraphView gview = gv;
        axisView = new JPanel(){
            public Dimension getPreferredSize() {
                return new Dimension(GraphView.AXIS_WIDTH, gview.getPreferredSize().height);
            }
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.BLACK);
                gview.paintYaxis(g);
            }
        };
        axisView.setBackground(Color.WHITE);
        graphView.setAxisPanel(axisView);
        if (fixedData) {
        }
    }

    private void setScale() {
    }

    public void setScale(int newScale) {
        graphView.setScale(newScale);
    }

    /**
     * Display the current connection status to a remote SPOT. 
     * Called by the AccelerometerListener whenever the radio connection status changes.
     *
     * @param conn true if now connected to a remote SPOT
     * @param msg the String message to display, includes the 
     */
    public void setConnectionStatus(boolean conn, String msg) {
        blinkButton.setEnabled(conn);
        if (!fixedData) {
            if (conn) {
                scales = listener.getScales();
                currentScale = listener.getCurrentScale();
                setScale();
            }
        }
    }
    
    /**
     * Select a (new) file to save the accelerometer data in.
     */
    private void doSaveAs() {
        JFileChooser chooser;
        if (file != null) {
            chooser = new JFileChooser(file.getParent());
        } else {
            chooser = new JFileChooser(System.getProperty("user.dir"));
        }
        int returnVal = chooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
            if (file.exists()) {
                int n = JOptionPane.showConfirmDialog(this, "The file: " + file.getName() + 
                                                      " already exists. Do you wish to replace it?",
                                                      "File Already Exists",
                                                      JOptionPane.YES_NO_OPTION);
                if (n != JOptionPane.YES_OPTION) {
                    return;                             // cancel the Save As command
                }
            }
            setTitle(file.getName());
            doSave();
        }
    }
    
    /**
     * Save the current accelerometer data to the file associated with this window.
     */
    private void doSave() {
        if (graphView.writeData(file)) {
        }
    }
    
    /**
     * Routine to print out each page of the current graph with a footer.
     *
     * @param g the graphics context to use to print
     * @param pageFormat how big is each page
     * @param pageIndex the page to print
     */
    public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
        double xscale = 0.5;
        double yscale = 0.75;
        int mx = 40;
        int my = 30;
        double x0 = pageFormat.getImageableX() + mx;
        double y0 = pageFormat.getImageableY() + my;
        double axisW = GraphView.AXIS_WIDTH * xscale;
        double w = pageFormat.getImageableWidth() - axisW - 2 * mx;
        double h = pageFormat.getImageableHeight() - 2 * my;
        int pagesNeeded = (int) (xscale * graphView.getMaxWidth() / w);
        if (pageIndex > pagesNeeded) {
            return(NO_SUCH_PAGE);
        } else {
            Graphics2D g2d = (Graphics2D)g;
            // first print our footer
            int y = (int) (y0 + h + 18);
            g2d.setPaint(Color.black);
            g2d.setFont(footerFont);
            g2d.drawString(dateFormat.format(new Date()).toString(), (int) (x0 + 5), y);
            if (file != null) {
                String name = file.getName();
                g2d.drawString(name, (int) (x0 + w/2 - 2 * name.length() / 2), y);
            }
            g2d.drawString((pageIndex + 1) + "/" + (pagesNeeded + 1), (int) (x0 + w - 20), y);
            
            // now print the Y-axis
            axisView.setDoubleBuffered(false);
            g2d.translate(x0, y0);
            g2d.scale(xscale, yscale);
            axisView.paint(g2d);
            axisView.setDoubleBuffered(true);

            // now have graph view print the next page
            // note: while the values to translate & setClip work they seem wrong. Why 2 * axisW ???
            graphView.setDoubleBuffered(false);
            g2d.translate(2 * axisW + 1 - (w * pageIndex) / xscale, 0);
            g2d.setClip((int)((w * pageIndex) / xscale + 2), 0, (int)(w / xscale), (int)(h / yscale));
            graphView.paint(g2d);
            graphView.setDoubleBuffered(true);
                    
            return(PAGE_EXISTS);
        }
    }

    /**
     * Routine to bring the user selected window to the front.
     *
     * @param evt the menu command with the name of the selected window
     */
    private void windowSelected(ActionEvent evt) {
        String str = evt.getActionCommand();
        for (Enumeration e = SpotListener.getWindows().elements() ; e.hasMoreElements() ;) {
            JFrame fr = (JFrame)e.nextElement();
            if (str.equals(fr.getTitle())) {
                fr.setVisible(true);
                break;
            }
        }
    }


    /**
     * Cleanly exit.
     */
    private void doQuit() {
        SpotListener.doQuit();
    }
    
    // GUI code generated using NetBeans GUI editor:

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        buttonPanel = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        nodeNumberField = new javax.swing.JFormattedTextField();
        jLabel5 = new javax.swing.JLabel();
        nodeNumberField1 = new javax.swing.JFormattedTextField();
        jLabel6 = new javax.swing.JLabel();
        blinkButton = new javax.swing.JButton();
        blinkButton1 = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        closeMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        quitMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("RSUS Project");
        setName("rsus_project"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });
        getContentPane().setLayout(new java.awt.BorderLayout(0, 5));

        jPanel1.setMaximumSize(new java.awt.Dimension(25000, 25000));
        jPanel1.setPreferredSize(new java.awt.Dimension(480, 480));
        jPanel1.setLayout(new java.awt.GridLayout());
        getContentPane().add(jPanel1, java.awt.BorderLayout.LINE_START);

        jPanel3.setAlignmentX(0.0F);
        jPanel3.setAlignmentY(0.0F);
        jPanel3.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jPanel3.setMaximumSize(new java.awt.Dimension(32767, 147));
        jPanel3.setMinimumSize(new java.awt.Dimension(480, 103));
        jPanel3.setPreferredSize(new java.awt.Dimension(480, 103));
        jPanel3.setLayout(new java.awt.GridBagLayout());

        buttonPanel.setAlignmentX(1.0F);
        buttonPanel.setAlignmentY(0.0F);
        buttonPanel.setMaximumSize(new java.awt.Dimension(566, 39));
        buttonPanel.setMinimumSize(new java.awt.Dimension(550, 30));
        buttonPanel.setPreferredSize(new java.awt.Dimension(550, 35));
        buttonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 5));

        jLabel4.setText("Node no:");
        buttonPanel.add(jLabel4);

        nodeNumberField.setColumns(2);
        nodeNumberField.setText("1");
        nodeNumberField.setAlignmentY(1.0F);
        nodeNumberField.setMaximumSize(new java.awt.Dimension(32, 22));
        nodeNumberField.setMinimumSize(new java.awt.Dimension(32, 22));
        nodeNumberField.setValue(new Integer(5));
        nodeNumberField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nodeNumberFieldActionPerformed(evt);
            }
        });
        nodeNumberField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                nodeNumberFieldPropertyChange(evt);
            }
        });
        buttonPanel.add(nodeNumberField);

        jLabel5.setText("   Set interval:");
        buttonPanel.add(jLabel5);

        nodeNumberField1.setColumns(2);
        nodeNumberField1.setText("1");
        nodeNumberField1.setAlignmentY(1.0F);
        nodeNumberField1.setMaximumSize(new java.awt.Dimension(32, 22));
        nodeNumberField1.setMinimumSize(new java.awt.Dimension(32, 22));
        nodeNumberField1.setValue(new Integer(5));
        nodeNumberField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nodeNumberField1ActionPerformed(evt);
            }
        });
        nodeNumberField1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                nodeNumberField1PropertyChange(evt);
            }
        });
        buttonPanel.add(nodeNumberField1);

        jLabel6.setText("   ");
        buttonPanel.add(jLabel6);

        blinkButton.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        blinkButton.setText("Blink LEDs");
        blinkButton.setMinimumSize(new java.awt.Dimension(90, 29));
        blinkButton.setPreferredSize(new java.awt.Dimension(94, 29));
        blinkButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                blinkButtonActionPerformed(evt);
            }
        });
        buttonPanel.add(blinkButton);

        blinkButton1.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        blinkButton1.setText("Set Interval");
        blinkButton1.setMinimumSize(new java.awt.Dimension(90, 29));
        blinkButton1.setPreferredSize(new java.awt.Dimension(94, 29));
        blinkButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                blinkButton1ActionPerformed(evt);
            }
        });
        buttonPanel.add(blinkButton1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel3.add(buttonPanel, gridBagConstraints);

        getContentPane().add(jPanel3, java.awt.BorderLayout.SOUTH);

        fileMenu.setText("File");

        aboutMenuItem.setText("About...");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(aboutMenuItem);
        fileMenu.add(jSeparator3);

        closeMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.CTRL_MASK));
        closeMenuItem.setText("Close");
        closeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(closeMenuItem);
        fileMenu.add(jSeparator2);

        quitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        quitMenuItem.setText("Quit");
        quitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(quitMenuItem);

        jMenuBar1.add(fileMenu);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void blinkButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_blinkButtonActionPerformed
        listener.doBlink(Byte.valueOf(nodeNumberField.getText()));
    }//GEN-LAST:event_blinkButtonActionPerformed

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        JOptionPane.showMessageDialog(this,
                "Sun SPOTs Telemetry Demo (Version " + version + ")\n\nA demo showing how to collect data from a SPOT and \nsend it to a desktop application to be displayed.\n\nAuthor: Ron Goldman, Sun Labs\nDate: " + versionDate,
                "About Telemetry Demo",
                JOptionPane.INFORMATION_MESSAGE,
                SpotListener.aboutIcon);
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void nodeNumberFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_nodeNumberFieldPropertyChange
        Integer fieldWidth = (Integer)nodeNumberField.getValue();
        int w = fieldWidth.intValue();
        if (w <= 0) {
            w = 2;
        }
        if ((w % 2) == 0) {
            w++;
            nodeNumberField.setValue(new Integer(w));
        }
        if (graphView != null) {
            graphView.setFilterWidth(w - 1);
        }
    }//GEN-LAST:event_nodeNumberFieldPropertyChange

    private void quitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quitMenuItemActionPerformed
        doQuit();
    }//GEN-LAST:event_quitMenuItemActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        SpotListener.removeFrame(this);
        if (listener != null) {
            listener.doQuit();
        }
    }//GEN-LAST:event_formWindowClosed

    private void closeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeMenuItemActionPerformed
        setVisible(false);
        dispose();
}//GEN-LAST:event_closeMenuItemActionPerformed

    private void nodeNumberFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nodeNumberFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_nodeNumberFieldActionPerformed

    private void nodeNumberField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nodeNumberField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_nodeNumberField1ActionPerformed

    private void nodeNumberField1PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_nodeNumberField1PropertyChange
        // TODO add your handling code here:
    }//GEN-LAST:event_nodeNumberField1PropertyChange

    private void blinkButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_blinkButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_blinkButton1ActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JButton blinkButton;
    private javax.swing.JButton blinkButton1;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JMenuItem closeMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JFormattedTextField nodeNumberField;
    private javax.swing.JFormattedTextField nodeNumberField1;
    private javax.swing.JMenuItem quitMenuItem;
    // End of variables declaration//GEN-END:variables
    
}
