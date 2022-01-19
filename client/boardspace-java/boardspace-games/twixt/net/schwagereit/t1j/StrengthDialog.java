/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package twixt.net.schwagereit.t1j;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Dialog to ask for setting of playing strength.
 *
 * @author mail@johannes-schwagereit.de
 */
@SuppressWarnings("serial")
public final class StrengthDialog extends JDialog
{
   /** Default value for insets. */
   private static final int INSET = 5;

   /** Initial size of dialog. */
   private static final int XSIZE = 250;
   /** Initial size of dialog. */
   private static final int YSIZE = 150;

   /** data of the match. */
   private final GeneralSettings settings = GeneralSettings.getInstance();


   private final JRadioButton plyRadio;
   private final JRadioButton timeRadio;
   private final SpinnerNumberModel plyS;
   private final SpinnerNumberModel timeS;
   private final JSpinner spinnerTime;
   private final JSpinner spinnerPly;

   /**
    * constructor.
    *
    * @param owner parent-dialog
    */
   private StrengthDialog(final JFrame owner)
   {
      super(owner, Messages.getString("StrengthDialog.Title"),
            true);
      setSize(XSIZE, YSIZE);
      this.setLocationRelativeTo(owner);
      getContentPane().setLayout(new BorderLayout(INSET, INSET));

      JPanel dataPanel = new JPanel(new GridBagLayout());

      ButtonGroup group = new ButtonGroup();
      plyRadio = new JRadioButton(
            Messages.getString("StrengthDialog.PlyRadio"), true);

      group.add(plyRadio);
      dataPanel.add(plyRadio, createGridBagConstraint(0, 0, true));

      timeRadio = new JRadioButton(
            Messages.getString("StrengthDialog.TimeRadio"), false); //$NON-NLS-1$
      group.add(timeRadio);
      dataPanel.add(timeRadio, createGridBagConstraint(0, 1, true));

      JPanel plyLine = new JPanel(new FlowLayout());
      //  value, min, max, step
      plyS = new SpinnerNumberModel(5, 1, 10, 1);
      spinnerPly = new JSpinner(plyS);

      plyLine.add(spinnerPly);
      plyLine.add(new JLabel(Messages.getString("StrengthDialog.ply")));
      dataPanel.add(plyLine, createGridBagConstraint(1, 0, false));

      JPanel timeLine = new JPanel(new FlowLayout());
      //  value, min, max, step
      timeS = new SpinnerNumberModel(5, 1, 60, 1);
      spinnerTime = new JSpinner(timeS);
      spinnerTime.setSize(100,1);
      timeLine.add(spinnerTime);
      timeLine.add(new JLabel(Messages.getString("StrengthDialog.seconds")));
      dataPanel.add(timeLine, createGridBagConstraint(1, 1, false));

      getContentPane().add(dataPanel, BorderLayout.CENTER);
      JPanel botLine = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      getContentPane().add(botLine, BorderLayout.SOUTH);
      JButton okButton = new JButton(Messages.getString("StrengthDialog.Ok")); //$NON-NLS-1$
      okButton.addActionListener(new ActionListener()
            {
               public void actionPerformed(final ActionEvent e)
               {
                  readData();
                  dispose();
               }
            });
      JButton cancelButton = new JButton(Messages.getString("StrengthDialog.Cancel")); //$NON-NLS-1$
      cancelButton.addActionListener(new ActionListener()
            {
               public void actionPerformed(final ActionEvent e)
               {
                  dispose();
               }
            });

      getRootPane().setDefaultButton(okButton);
      botLine.add(okButton);
      botLine.add(cancelButton);

      timeRadio.addChangeListener(new ChangeListener()
      {
         public void stateChanged(ChangeEvent e)
         {
            spinnerTime.setEnabled(timeRadio.isSelected());
            spinnerPly.setEnabled(!timeRadio.isSelected());
         }
      });

      //dia.pack();
   }

   /**
    * Convenience method to create constraints.
    * @param x column
    * @param y row
    * @param fill fill space?
    * @return a new GridBagConstraint
    */
   private static GridBagConstraints createGridBagConstraint(final int x,
         final int y, final boolean fill)
   {
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.gridx = x;
      gbc.gridy = y;
      if (fill)
      {
         gbc.weightx = 1;
         gbc.fill = GridBagConstraints.HORIZONTAL;
      }
      else
      {
         gbc.anchor = (x > 0) ? GridBagConstraints.WEST : GridBagConstraints.EAST;
      }
      gbc.insets = new Insets(INSET, INSET, INSET, INSET);
      return gbc;
   }

   /**
    * create new data as copy of previous match.
    */
   private void setData()
   {
      this.plyS.setValue(settings.mdPly);
      this.timeS.setValue(settings.mdTime);
      if (settings.mdFixedPly)
      {
         this.plyRadio.setSelected(true);
         this.spinnerTime.setEnabled(false);
      }
      else
      {
         this.timeRadio.setSelected(true);
         this.spinnerPly.setEnabled(false);
      }
   }

   /**
    * Read the data from the GUI.
    */
   private void readData()
   {
      settings.mdFixedPly = this.plyRadio.isSelected();
      settings.mdTime = ((Number) this.timeS.getValue()).intValue();
      settings.mdPly = ((Number) this.plyS.getValue()).intValue();
      settings.savePreferences();

   }


   /**
    * Ask for parameters of new game.
    * @param owner Parentdialog
    */
   static void showStrengthDialog(final JFrame owner)
   {
      StrengthDialog dia = new StrengthDialog(owner);
      dia.setData();
      dia.setVisible(true);
      dia.dispose();
   }


}
