/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package twixt.net.schwagereit.t1j;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Dialog to ask for parameters of new game.
 * 
 * @author mail@johannes-schwagereit.de
 */
final class NewDialog extends JDialog
{
   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

/** really start new game?. */
   private boolean newGame = false;
   
   /** data of the new match. */
   private MatchData newMatch;
   
   /** Default value for insets. */
   private static final int INSET = 5;
   
   /** Initial size of dialog. */
   private static final int XSIZE = 550;
   /** Initial size of dialog. */
   private static final int YSIZE = 300;
   
   private final PlayerPanel player1Panel;
   private final PlayerPanel player2Panel;
   private final JCheckBox firstPinCheck;
   private final JCheckBox pieCheck;
   private static boolean isVisible = false;
      
   /**
    * Create a panel for one player.
    */
   public final class PlayerPanel extends JPanel
   {   
      /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final JTextField playerName = new JTextField();
      private final JRadioButton computerRadio;
      private final JRadioButton humanRadio;
      private final SpinnerNumberModel sizeS;
      private boolean computerSelected = false;

      /**
       * Cons'tor.
       * @param playerHead The headline
       * @param changeOnly true, if gamedata are only changed (not a new game)
       */
      PlayerPanel(final String playerHead, boolean changeOnly)
      {
         this.setLayout(new GridBagLayout());
         
         setBorder(new TitledBorder(new EtchedBorder(), 
               Messages.getString("NewDialog." + playerHead))); //$NON-NLS-1$
         
         //-------------
         
         add(new JLabel(Messages.getString("NewDialog.Name"), SwingConstants.TRAILING),  //$NON-NLS-1$
               createGridBagConstraint(0, 0, false)); 
         
         add(playerName, createGridBagConstraint(1, 0, true));
         
         //-------------
         add(new JLabel(Messages.getString("NewDialog.Status"), SwingConstants.TRAILING),  //$NON-NLS-1$
               createGridBagConstraint(0, 1, false)); 
         
         JPanel buttonLine = new JPanel(new FlowLayout());
         ButtonGroup group = new ButtonGroup();
         computerRadio = new JRadioButton(
               Messages.getString("NewDialog.Computer"), true); //$NON-NLS-1$
         computerRadio.addChangeListener(new ChangeListener()
               {
            public void stateChanged(final ChangeEvent e)
            {
               if (isVisible)
               {
                  if (computerRadio.isSelected() != computerSelected)
                  {
                     playerName.setText(Messages.getString(computerRadio.isSelected()
                           ? "MatchData.Computer" :  "MatchData.Player"));
                     computerSelected = computerRadio.isSelected();
                  }
               }
            }
               });
         group.add(computerRadio);
         buttonLine.add(computerRadio);
         humanRadio = new JRadioButton(
               Messages.getString("NewDialog.Human"), false); //$NON-NLS-1$
         group.add(humanRadio);
         buttonLine.add(humanRadio);
         
         add(buttonLine, createGridBagConstraint(1, 1, false));
         //player1.addElement(butt2, createGridBagConstraint(0, 2, false));
         
         //player1.addElement(new JTextField(), createGridBagConstraint(1, 1, true));
         
         //-------------
         add(new JLabel(Messages.getString("NewDialog.size"), SwingConstants.TRAILING),  //$NON-NLS-1$
               createGridBagConstraint(0, 2, false)); 
         
         //  value, min, max, step
         sizeS = new SpinnerNumberModel(Board.DEFAULTDIM, Board.MINDIM, Board.MAXDIM, 1);
         JSpinner spinner = new JSpinner(sizeS);
         spinner.setEnabled(!changeOnly);
         add(spinner, createGridBagConstraint(1, 2, false));
      }
   }
   
   /**
    * constructor.
    *
    * @param owner parent-dialog
    * @param changeOnly true, if gamedata are only changed (not a new game)
    */
   private NewDialog(final JFrame owner, boolean changeOnly)
   {
      super(owner, Messages.getString(changeOnly ? "NewDialog.Change" : "NewDialog.New"),
            true); 
      setSize(XSIZE, YSIZE);
      this.setLocationRelativeTo(owner);
      getContentPane().setLayout(new BorderLayout(INSET, INSET));
      //dia.getContentPane().addElement(new JLabel("Dagmar"), BorderLayout.CENTER);
      
      JPanel dataPanel = new JPanel(new BorderLayout(INSET, INSET));

      Box pBox = Box.createHorizontalBox();
      player1Panel = new PlayerPanel("Player1", changeOnly); //$NON-NLS-1$
      player2Panel = new PlayerPanel("Player2", changeOnly); //$NON-NLS-1$
      
      pBox.add(player1Panel); 
      pBox.add(player2Panel); 
      
      Box optionBox = Box.createVerticalBox(); 
      firstPinCheck = new JCheckBox(Messages.getString("NewDialog.firstPin"), true);
      firstPinCheck.setEnabled(!changeOnly);
      optionBox.add(firstPinCheck); 
      pieCheck = new JCheckBox(Messages.getString("NewDialog.pieRule"), true);
      pieCheck.setEnabled(!changeOnly);
      optionBox.add(pieCheck);

      dataPanel.add(pBox, BorderLayout.CENTER);
      dataPanel.add(optionBox, BorderLayout.SOUTH);

      getContentPane().add(dataPanel, BorderLayout.CENTER);
      
      //create Bottonline for Buttons
      JPanel botLine = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      getContentPane().add(botLine, BorderLayout.SOUTH);
      JButton okButton = new JButton(Messages.getString("NewDialog.Ok")); //$NON-NLS-1$
      okButton.addActionListener(new ActionListener()
            {
               public void actionPerformed(final ActionEvent e)
               {
                  allowNewGame();
                  dispose();
               }
            });
      JButton cancelButton = new JButton(Messages.getString("NewDialog.Cancel")); //$NON-NLS-1$
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
    * @param oldMatch data of old match
    */
   private void setData(final MatchData oldMatch)
   {
      newMatch = (MatchData) oldMatch.clone();
      newMatch.setDefaultNames(false);
      this.player2Panel.playerName.setText(newMatch.mdPlayerX);
      this.player1Panel.playerName.setText(newMatch.mdPlayerY);
      if (newMatch.mdYhuman)
      {
         this.player1Panel.humanRadio.setSelected(true);
      }
      else
      {
         this.player1Panel.computerRadio.setSelected(true);
         this.player1Panel.computerSelected = true;
      }
      if (newMatch.mdXhuman)
      {
         this.player2Panel.humanRadio.setSelected(true);
      }
      else
      {
         this.player2Panel.computerRadio.setSelected(true);
         this.player2Panel.computerSelected = true;
      }
      this.player2Panel.sizeS.setValue(newMatch.mdXsize);
      this.player1Panel.sizeS.setValue(newMatch.mdYsize);
      this.firstPinCheck.setSelected(newMatch.mdYstarts);
      this.pieCheck.setSelected(newMatch.mdPieRule);
   }
   
   /**
    * Read the data from the GUI.
    */
   private void readData()
   {
      newMatch.mdPlayerX = this.player2Panel.playerName.getText();
      newMatch.mdPlayerY = this.player1Panel.playerName.getText();
      newMatch.mdXhuman = this.player2Panel.humanRadio.isSelected();
      newMatch.mdYhuman = this.player1Panel.humanRadio.isSelected();
      newMatch.mdXsize = ((Integer) this.player2Panel.sizeS.getValue()).intValue();
      newMatch.mdYsize = ((Integer) this.player1Panel.sizeS.getValue()).intValue();
      newMatch.mdYstarts = this.firstPinCheck.isSelected();
      newMatch.mdPieRule = this.pieCheck.isSelected();
      newMatch.savePreferences();
   }
   
   /**
    * Allow a new game.
    */
   private void allowNewGame()
   {
      this.newGame = true;
   }

   /**
    * Ask for parameters of new game.
    * @param owner Parentdialog
    * @param oldMatch data of previous match
    * @param changeOnly true, if gamedata are only changed (not a new game)
    * @return MatchData of new match or null
    */
   static MatchData showNewDialog(final JFrame owner, final MatchData oldMatch,
                                  boolean changeOnly)
   {
      NewDialog dia = new NewDialog(owner, changeOnly);
      dia.setData(oldMatch);
      isVisible = true;
      dia.setVisible(true);
      isVisible = false;
      dia.dispose();
      if (dia.newGame)
      {
         dia.readData();
         return dia.newMatch;
      }
      else 
      {
         return null;
      }
   }

   
}
