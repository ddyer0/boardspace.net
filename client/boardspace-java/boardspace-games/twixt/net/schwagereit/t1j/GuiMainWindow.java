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

import bridge.XJMenu;
import lib.SimpleObservable;
import lib.SimpleObserver;

import java.awt.*;
import java.awt.event.*;


/**
 * This class is based on "GUIHauptfenster.java", which is a class of
 * www.twixt.de, Copyright (C) 2002 Agnes Gruber, Dennis Riegelbauer, Manuela
 * Kinzel, Mike Wiechmann
 *
 * @author Johannes Schwagereit (mail(at)johannes-schwagereit.de)
 */
@SuppressWarnings("serial")
public final class GuiMainWindow extends JFrame implements SimpleObserver
{

   /** Number of color schemes. */
   public static final int SCHEME_NUMBER = 8;
   private static final JCheckBoxMenuItem[] SCHEME_ENTRIES =
         new JCheckBoxMenuItem[SCHEME_NUMBER];
   private JMenu fileMenu;

   /**
    * Labels for GUI
    */
//   public static final class LabelField extends JTextField
//   {
// --Commented out by Inspection START (17.03.07 22:11):
//      /**
//       * Cons'tor
//       *
//       * @param text
//       *           text to show
//       */
//      LabelField(final String text)
//      {
//         super(LABEL_LENGTH);
//         setText(text);
//         setEditable(false);
//         setForeground(GuiBoard.FONTCOLOR);
//         setBackground(GuiBoard.BACKCOLOR);
//         setBorder(null);
//      }
// --Commented out by Inspection STOP (17.03.07 22:11)
//   }

   private static final int XSIZE = 800;

   private static final int YSIZE = 600;

   //coordinates of the board
   private static final int LEFT_B = 10;

   private static final int UP_B = 10;

   private final GuiBoard guiBoard;

   //private GUIMeldungsThread meldungsThread;

   private final Match match;

   //   private URL urlToImage = getClass().getClassLoader().getResource(
   //         "images/StartBild2.gif");

   //private ImageIcon bild = new ImageIcon(urlToImage);

   /**
    * ActionListener for colorScheme-Menuentry.
    */
   private class ColorActionListener implements ActionListener
   {
      private final int ident;

      /**
       * C'tor for Listener.
       * @param i the number of this colorscheme.
       */
      public ColorActionListener(int i)
      {
         ident = i ;
      }

      public void actionPerformed(final ActionEvent e)
      {
         //set only the current entry as selected
         for (int i=0;i < SCHEME_NUMBER; i++)
         {
             SCHEME_ENTRIES[i].setState(i == ident);
         }
         GuiBoard.setColorScheme(ident);
         GeneralSettings.getInstance().mdColorscheme = ident;
         GeneralSettings.getInstance().savePreferences();
         setNameForNext();
         guiBoard.repaint();
      }
   }                   

   
   /**
    * Cons'tor.
    *
    * @param matchIn
    *           match played
    */
   GuiMainWindow(final Match matchIn)
   {

      this.match = matchIn;
      this.match.addObserver(this);
      this.match.setFrame(this);
      this.setResizable(true);
      this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);


      defineMenu();

      setTitle(Messages.getString("GuiMainWindow.title"));
      setSize(XSIZE, YSIZE);


      Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
      this.setLocation(dimension.width / 2 - this.getSize().width / 2, dimension.height / 2
            - this.getSize().height / 2);

      guiBoard = new GuiBoard(matchIn);
      JPanel leftPanel = guiBoard;
      leftPanel.addMouseListener(new MyMouseListener());
      leftPanel.addMouseMotionListener(new MouseMotionHandler());

      Container contentPane = getContentPane();
      contentPane.setLayout(new BorderLayout());
      contentPane.setBackground(GuiBoard.BACKCOLOR);
      contentPane.add(leftPanel, BorderLayout.CENTER);
      RightPanel.getInstance().setMatch(match);
      contentPane.add(RightPanel.getInstance(), BorderLayout.EAST);

      // Icon for systemtray and corner of Window
      setIconImage(new ImageIcon(RightPanel.getInstance().getIconURL()).getImage());
      SCHEME_ENTRIES[GeneralSettings.getInstance().mdColorscheme].setState(true);


      setNameForNext();

      // if window is closed
      addWindowListener(new WindowAdapter()
      {
         /**
          * @see java.awt.event.WindowListener#windowClosing(java.awt.event.WindowEvent)
          */
         public void windowClosing(final WindowEvent ev)
         {
            System.exit(0);
            //meldungsThread.meldungenAnhalten();
         }
      });
      // myThread.start();
   }


   /**
    * Define the menu.
    */
   private void defineMenu()
   {
      final GuiMainWindow thiswindow = this;
      ActionListener aListener;
      JMenuBar mbar = new JMenuBar();

      fileMenu = addMenuBarItem(mbar, Messages.getString("GuiMainWindow.file"));

      aListener = new ActionListener()
            {
               public void actionPerformed(final ActionEvent e)
               {
                  Control.newGame();
               }
            };
      addMenuItem(fileMenu, Messages.getString("GuiMainWindow.newGame"), 'N', aListener);

      aListener = new ActionListener()
            {
               public void actionPerformed(final ActionEvent e)
               {
                  Control.changeGame();
               }
            };
      addMenuItem(fileMenu, Messages.getString("GuiMainWindow.change"), (char) 0, aListener);

      aListener = new ActionListener()
            {
               public void actionPerformed(final ActionEvent e)
               {
                  Control.loadGame();
               }
            };
      addMenuItem(fileMenu, Messages.getString("GuiMainWindow.load"), 'O', aListener);

      aListener = new ActionListener()
            {
               public void actionPerformed(final ActionEvent e)
               {
                  Control.saveGame();
               }
            };
      addMenuItem(fileMenu, Messages.getString("GuiMainWindow.save"), 'S', aListener);

      aListener = new ActionListener()
      {
         public void actionPerformed(final ActionEvent e)
         {
            System.exit(0);
         }
      };
      addMenuItem(fileMenu, Messages.getString("GuiMainWindow.quit"), 'Q', aListener);

      JMenu setupMenu = addMenuBarItem(mbar, Messages.getString("GuiMainWindow.setup"));

      final JCheckBoxMenuItem areaLines = (JCheckBoxMenuItem) processMnemonic(
            Messages.getString("GuiMainWindow.areaLines"), true);
      areaLines.setState(true);
      areaLines.addActionListener(new ActionListener()
      {
         public void actionPerformed(final ActionEvent e)
         {
            Control.setShowAreaLines(areaLines.getState());
            guiBoard.repaint(LEFT_B, UP_B, guiBoard.getWidth(), guiBoard.getHeight());
         }
      });
      setupMenu.add(areaLines);

      aListener = new ActionListener()
            {
               public void actionPerformed(final ActionEvent e)
               {
                  Control.changeLevel();
               }
            };
      addMenuItem(setupMenu, Messages.getString("GuiMainWindow.level"), 'V', aListener);
      /*
      JMenuItem menuItem = addMenuItem(setupMenu, Messages.getString("GuiMainWindow.options"),
            (char) 0, null);
      menuItem.setEnabled(false);
      */
      JMenu colorMenu = new JMenu(Messages.getString("GuiMainWindow.colorscheme"));
      setupMenu.add(colorMenu);
      addColorSchemes(colorMenu);

      JMenu helpMenu = addMenuBarItem(mbar, Messages.getString("GuiMainWindow.help"));

      aListener = new ActionListener()
      {
         public void actionPerformed(final ActionEvent e)
         {
            StringBuffer text = new StringBuffer("");
            String line;
            int count = 1;

            while (!(line = Messages.getString("About." + count))
                  .startsWith("!!"))
            {
               text.append(line).append(System.getProperty("line.separator"));
               count++;
            }

            JOptionPane.showMessageDialog(thiswindow, text.toString(),
                  Messages.getString("GuiMainWindow.about").replaceFirst("_", ""),
                  JOptionPane.PLAIN_MESSAGE, RightPanel.getInstance().getLogo());
         }
      };
      addMenuItem(helpMenu, Messages.getString("GuiMainWindow.about"), (char) 0, aListener);


      this.setJMenuBar(mbar);
   }

   /**
    * Add a color schemu to list in menu.
    * @param colorMenu the menu to add
    */
   private void addColorSchemes(JMenu colorMenu)
   {
      JCheckBoxMenuItem currentMenu;
      for (int i = 0; i < SCHEME_NUMBER; i++)
      {
         currentMenu = new JCheckBoxMenuItem();
         currentMenu.setText(Messages.getString("GuiMainWindow.color" + i));
         currentMenu.addActionListener(new ColorActionListener(i));

         SCHEME_ENTRIES[i] = currentMenu;
         colorMenu.add(currentMenu);
      }
   }


   /**
    * Process the Mnemonic for a new JMenuItem.
    * @param text Text of the item
    * @param check Is this a CheckBoxItem?
    * @return the new MenuItem
    */
   private static JMenuItem processMnemonic(String text, boolean check)
   {
      JMenuItem menuItem = (check) ? new JCheckBoxMenuItem() : new JMenuItem();

      if (text.indexOf("_") > -1)
      {
         int pos = text.indexOf("_");
         char c = text.charAt(pos + 1);
         StringBuffer sb = new StringBuffer(text).delete(pos, pos + 1);
         menuItem.setText(sb.toString());
         menuItem.setMnemonic(c);
         return menuItem;
      }
      else
      {
         menuItem.setText(text);
         return menuItem;
      }
   }

   /**
    * Create menuitem with hotkey.
    * @param menu the parentmenu
    * @param text text of menu
    * @param keyChar hotkey
    * @param al actionlistener
    */
   private static void addMenuItem(JMenu menu, String text, char keyChar,
                                        ActionListener al)
   {
      JMenuItem menuItem = processMnemonic(text, false);
      menu.add(menuItem);
      if (keyChar != 0)
      {
         menuItem.setAccelerator(KeyStroke.getKeyStroke(keyChar, InputEvent.CTRL_DOWN_MASK));
      }
      if (al != null)
      {
         menuItem.addActionListener(al);
      }
   }

   /**
    * Add menu to menubar.
    * @param menuBar menuBar
    * @param text Text for Entry
    * @return the new menu
    */
   private JMenu addMenuBarItem(JMenuBar menuBar, String text)
   {
      JMenu menu;
      if (text.indexOf("_") > -1)
      {
         int pos = text.indexOf("_");
         char c = text.charAt(pos + 1);
         StringBuffer sb = new StringBuffer(text).delete(pos, pos + 1);
         menu = new XJMenu(sb.toString(),false);
         menu.setMnemonic(c);
      }
      else
      {
         menu = new XJMenu(text,false);
      }
      menuBar.add(menu);
      return menu;
   }

   /**
    * Set Name of next Player.
    */
   private void setNameForNext()
   {
      String name;
      JTextField whosNext = RightPanel.getInstance().getWhosNext();

      if (match.getNextPlayer() == Board.XPLAYER)
      {
         whosNext.setForeground(GuiBoard.getXColor());
         name = match.getMatchData().mdPlayerX;
      } else
      {
         whosNext.setForeground(GuiBoard.getYColor());
         name = match.getMatchData().mdPlayerY;
      }
      whosNext.setText(name);
   }

   /**
    * Check if Back-Button can be active.
    * Button is not active if there is a move to undone and not computer vs. computer
    * @return true, if BackButton can be active
    */
   private boolean checkBackButton()
   {
      return (match.getMoveNr() > 1 || match.getMoveNr() > 0
            && match.getMatchData().mdXhuman && match.getMatchData().mdYhuman)
            && !match.computerVsComputer();
   }

   /**
    * Update Method which is called by Observer if anything has changed.
    * 
    * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
    */
   public void update(final SimpleObservable obs, Object eventType, final Object arg)
   {
      try
      {
         guiBoard.repaint(LEFT_B, UP_B, guiBoard.getWidth(), guiBoard.getHeight());

         setNameForNext();

         //en- od disable Undo- and Redo-Buttons
         RightPanel.getInstance().getBackButton().setEnabled(checkBackButton());
         RightPanel.getInstance().getNextButton().setEnabled(
               match.getHighestMoveNr() > match.getMoveNr());

         RightPanel.getInstance().getEndMatchButton().setVisible(match.computerVsComputer());
         RightPanel.getInstance().updateUI();

         fileMenu.setEnabled(!match.computerVsComputer());

         RightPanel.getInstance().setMoveList();

         RightPanel.getInstance().getWhosNext().repaint();
         // match.setDrawing(false); //drawing is finished
         
      } catch (NullPointerException e)
      {
         System.out.println("Oh, oh, ... Exception in GuiMainWindow.update()"); //$NON-NLS-1$
      }
   }


   /**
    * mouse-click sets pin
    */
   final class MyMouseListener extends MouseAdapter
   {
      /**
       * mouse button pressed
       * 
       * @param event
       *           event
       */
      public void mousePressed(final MouseEvent event)
      {
         if ((event.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0)
         {
            int x = guiBoard.pixelToX(event.getX());
            int y = guiBoard.pixelToY(event.getY());
            if ((!match.isGuiBlocked()) && !match.isGameOver() && !match.computerVsComputer() && 
                  match.setlastMove(x, y))
            { //if move okay
               match.evaluateAndUpdateGui();
            }
         }
      }
   }

   /**
    * show current position of mouse.
    */
   final class MouseMotionHandler implements MouseMotionListener
   {

      /**
       * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
       */
      public void mouseMoved(final MouseEvent event)
      {
         int x = event.getX();
         int y = event.getY();
         int sx = match.getXsize();
         int sy = match.getYsize();

         int ax = guiBoard.pixelToX(x) + 1;
         int ay = guiBoard.pixelToY(y) + 1;

         // set shape of mouse-cursor as required
         if (x < guiBoard.getFieldX() || y < guiBoard.getFieldY() ||
               x > guiBoard.getFieldWidth() + guiBoard.getFieldX() ||
               y > guiBoard.getFieldHeight() + guiBoard.getFieldY())
         {
            setCursor(Cursor.getDefaultCursor());
            RightPanel.getInstance().setPosText(""); //$NON-NLS-1$
         } else
         {

            if (!match.isGuiBlocked())
            {
               setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            }
            else
            {
               setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            }
            if (ax > 0 && ax <= sx && ay > 0 && ay <= sy)
            {
               RightPanel.getInstance().setPosText(" " + GuiBoard.COL_NAMES[ax - 1] + ay);
            }
            if ((ax == 1 && ay == 1) || (ax == sx && ay == 1) || (ax == 1 && ay == sy)
                  || (ax == sx && ay == sy))
            {
               RightPanel.getInstance().setPosText(""); //$NON-NLS-1$
            }
         }
      }

      /**
       * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
       */
      public void mouseDragged(final MouseEvent event)
      {
         // nothing to do...
      }

   }


}