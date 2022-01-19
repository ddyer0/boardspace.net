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
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.net.URL;

/**
 * The right panel of the board.
 * 
 * Created by IntelliJ IDEA.
 * @author Johannes Schwagereit (mail(at)johannes-schwagereit.de)
 */
@SuppressWarnings("serial")
final class RightPanel extends JPanel
{
   private static final RightPanel ourInstance = new RightPanel();


   private Match match = null;

   private static final int NEXT_LEN = 8;
   private static final int MOVES_ROWS = 8;
   private static final int MOVES_COLS = 8;

   private static final int POS_LEN = 5;
   private static final int POS_FONT_SIZE = 18;

   /** Default value for insets. */
   private static final int INSET = 7;


   private final Button nextButton;
   private final Button backButton;
   private final Button endMatchButton;

   private final JTextField whosNext;
   private final JTextArea moveList;

   private final JTextField pos;
   private static final int FONT_WIN = 13;
   private final int buttonFontSize;

   public static final String LOGOPATH = "twixt/net/schwagereit/t1j/images/t1j.png";
   private final URL urlToImage = getClass().getClassLoader().getResource(LOGOPATH);
   private final ImageIcon logo = new ImageIcon(urlToImage);

   public static final String ICONPATH = "twixt/net/schwagereit/t1j/images/t1jIcon.gif";
   private final URL iconURL = getClass().getClassLoader().getResource(ICONPATH);


   /**
     * Return the RightPanel-Object.
     *
     * @return RightPanel-Object
     */
    public static RightPanel getInstance()
    {
       return ourInstance;
    }


   /**
    * Convenience method to create constraints.
    * @param x column
    * @param y row
    * @param fill fill space?
    * @param width number of columns
    * @return a new GridBagConstraint
    */
   private static GridBagConstraints createGridBagConstraint(final int x,
                                                             final int y, final int width,
                                                             final boolean fill)
   {

      GridBagConstraints gbc = new GridBagConstraints();
      gbc.gridx = x;
      gbc.gridy = y;
      gbc.gridwidth = width;
      if (fill)
      {
         gbc.weighty = 1;
         gbc.fill = GridBagConstraints.VERTICAL;
      }
      else
      {
         gbc.anchor = (x > 0) ? GridBagConstraints.WEST : GridBagConstraints.NORTHEAST;
         if (width > 1)
         {
            gbc.anchor = GridBagConstraints.NORTH;
         }
      }
      gbc.insets = new Insets(INSET, INSET, INSET, INSET);
      return gbc;
   }

   /**
    * Cons'tor. Singleton.
    */
   private RightPanel()
   {
      //for some unknown reasons this seems to be necessary
//      buttonFontSize = (System.getProperty("os.name").equals("Linux") &&
//            System.getProperty("java.version").startsWith("1.4")) ?
//            FONT_LIN : FONT_WIN;
      buttonFontSize = FONT_WIN;

      this.setLayout(new GridBagLayout());
      this.setBackground(GuiBoard.BACKCOLOR);

      // Label title = new Label("T1j");
      JLabel title = new JLabel(logo);
      //title.setFont(new Font(null, Font.BOLD, BIG_FONT));
      //title.setAlignment(Label.CENTER);
      //title.setForeground(Color.blue);
      add(title, createGridBagConstraint(0, 0, 2, false));

      backButton = createButton(Messages.getString("GuiMainWindow.undo"), new ListenerBack());
      nextButton = createButton(Messages.getString("GuiMainWindow.redo"), new ListenerForward());

      JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
      buttons.add(backButton);
      buttons.add(nextButton);
      buttons.setBackground(GuiBoard.BACKCOLOR);
      buttons.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

      add(buttons, createGridBagConstraint(0, 1, 2, false));

      Label amZug = new Label(Messages.getString("GuiMainWindow.nextPlayer"));
      amZug.setFont(new Font("SansSerif", Font.PLAIN, buttonFontSize));

      whosNext = new JTextField(NEXT_LEN);
      whosNext.setEditable(false);
      whosNext.setBackground(GuiBoard.BACKCOLOR);
      whosNext.setBorder(BorderFactory.createBevelBorder(1)); //LOWERED

      add(amZug,    createGridBagConstraint(0, 2, 1, false));
      add(whosNext, createGridBagConstraint(1, 2, 1, false));

      Label zug = new Label(Messages.getString("GuiMainWindow.moves")); //$NON-NLS-1$
      zug.setFont(new Font("SansSerif", Font.PLAIN, buttonFontSize));

      moveList = new JTextArea(MOVES_ROWS, MOVES_COLS);
      JScrollPane moveScrollPane = new JScrollPane(moveList);
      moveList.setEditable(false);
      moveList.setBackground(GuiBoard.BACKCOLOR);
      moveScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      moveScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

      add(zug,            createGridBagConstraint(0, 3, 1, false));
      add(moveScrollPane, createGridBagConstraint(1, 3, 1, true));

      Label position = new Label(Messages.getString("GuiMainWindow.position"));
      position.setFont(new Font("SansSerif", Font.PLAIN, buttonFontSize));

      pos = new JTextField(POS_LEN);
      pos.setBorder(BorderFactory.createBevelBorder(1)); //LOWERED
      pos.setEditable(false);
      pos.setBackground(GuiBoard.BACKCOLOR);
      pos.setFont(new Font("SansSerif", Font.PLAIN, POS_FONT_SIZE));

      add(position, createGridBagConstraint(0, 4, 1, false));
      add(pos,      createGridBagConstraint(1, 4, 1, false));

      endMatchButton = createButton(Messages.getString("GuiMainWindow.quitGame"),
            new ListenerQuit());
      endMatchButton.setEnabled(true);
      endMatchButton.setVisible(false);

      add(endMatchButton, createGridBagConstraint(0, 5, 2, false));
   }

   /**
    * Create a button for the right panel.
    * @param text Button-label
    * @param listener The action-listener
    * @return the button
    */
   private Button createButton(String text, ActionListener listener)
   {
      Button ourButton = new Button(text);
      ourButton.setFocusable(false);
      ourButton.setBackground(Color.LIGHT_GRAY);
      ourButton.setEnabled(false);
      ourButton.addActionListener(listener);
      ourButton.setFont(new Font("SansSerif", Font.PLAIN, buttonFontSize));
      return ourButton;
   }

   /**
    * Set the moveList to given string.
    */
   public void setMoveList()
   {
      moveList.setText(match.getMoveList());
   }

   /**
    * Set String for current position.
    * @param s String to set
    */
   public void setPosText(String s)
   {
      pos.setText(s);
   }

   /**
    * Listen for quit-Button.
    */
   final class ListenerQuit implements ActionListener
   {
      /**
       * Action for event.
       * @param evt Event
       */
      public void actionPerformed(final ActionEvent evt)
      {
         // stop match computer vs. computer
         endMatchButton.setEnabled(false);
         match.getMatchData().mdYhuman = true;
         match.getMatchData().mdXhuman = true;
         match.getMatchData().setDefaultNames(true);
      }
   }

   /**
    * Listen for Back-Button (Undo).
    */
   final class ListenerBack implements ActionListener
   {
      /**
       * Action for event.
       * @param evt Event
       */
      public void actionPerformed(final ActionEvent evt)
      {
         if (match.getMoveNr() > 0 && !match.isGuiBlocked())
         {
            match.removeMove();
         }
      }
   }

   /**
    * Listen for Forward-Button (Redo).
    */
   final class ListenerForward implements ActionListener
   {
      /**
       * Action for event.
       * @param evt Event
       */
      public void actionPerformed(final ActionEvent evt)
      {
         if (match.getHighestMoveNr() > match.getMoveNr() && !match.isGuiBlocked())
         {
            match.redoMove();
         }
      }
   }


   /**
    * Return NextButton.
    * @return NextButton
    */
   public Button getNextButton()
   {
      return nextButton;
   }

   /**
    * Return BackButton.
    * @return BackButton
    */
   public Button getBackButton()
   {
      return backButton;
   }
   /**
    * Return endMatchButton.
    * @return endMatchButton
    */
   public Button getEndMatchButton()
   {
      return endMatchButton;
   }

   /**
    * Set Match.
    * @param matchIn Match to set
    */
   public void setMatch(Match matchIn)
   {
      match = matchIn;
   }

   /**
    * get Textfield for name of next player.
    * @return the JTextField
    */
   public JTextField getWhosNext()
   {
      return whosNext;
   }

   /**
    * Get small Icon as url.
    * @return Icon-Url
    */
   public URL getIconURL()
   {
      return iconURL;
   }

   /**
    * Get Logo.
    * @return Logo
    */
   public Icon getLogo()
   {
      return logo;
   }

}
