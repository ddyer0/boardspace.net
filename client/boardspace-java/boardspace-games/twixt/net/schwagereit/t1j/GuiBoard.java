/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package twixt.net.schwagereit.t1j;

import java.awt.*;

import javax.swing.JPanel;



/**
 * This class is based on "GUISpielfeld.java", which is a class of www.twixt.de,
 * Copyright (C) 2002 Agnes Gruber, Dennis Riegelbauer, Manuela Kinzel, Mike
 * Wiechmann
 *
 *
 * @author Johannes Schwagereit (mail(at)johannes-schwagereit.de)
 */
@SuppressWarnings("serial")
public final class GuiBoard extends JPanel
{
   /** Array of column headers. */
   static final String[] COL_NAMES = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
         "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "AA",
         "AB", "AC", "AD", "AE", "AF", "AG", "AH", "AI", "AJ" };

   static final Color[][] colorSchemes = {
         {Color.red, Color.blue, Color.white, Color.gray.darker()},
         {Color.blue, Color.red, Color.white, Color.gray.darker()},
         {Color.green.darker().darker(), Color.red.darker(), Color.white, Color.gray.darker()},
         {Color.red.darker(), Color.green.darker().darker(), Color.white, Color.gray.darker()},
         {Color.white, Color.black, Color.lightGray, Color.gray },
         {Color.black, Color.white, Color.lightGray, Color.gray },
         {Color.yellow, Color.cyan, Color.lightGray, Color.gray },
         {Color.cyan, Color.yellow, Color.lightGray, Color.gray}  };

   // private static final int BOARD_WIDTH = 490; //width of board

   private int dist = 1; //distance between two points

   private int diameter = 0; //diameter of a hole

   private static final int BEGIN_X = 50; //coordinates for 1st hole

   private static final int BEGIN_Y = 40;

   private final Match match;

   //static final Color BACKCOLOR = new Color(235, 217, 144);
   static final Color BACKCOLOR = Color.lightGray;

   //static final Color BOARDCOLOR = new Color(175, 177, 208);
   private static final Color BOARDCOLOR = new Color(170, 170, 170);

   static final Color FONTCOLOR = Color.black;

   private static final int MARGIN = 10;

   // Coordinates of playing field
   private int fieldX;
   private int fieldY;
   private int fieldWidth;
   private int fieldHeight;
   private static int colorScheme = 0;

   /**
    * Cons'tor.
    *
    * @param spiel
    *           match
    */
   public GuiBoard(final Match spiel)
   {
      this.setBackground(BACKCOLOR);
      setColorScheme(GeneralSettings.getInstance().mdColorscheme);
      this.match = spiel;
   }

   /**
    * get the name of given hole as String, e.g. D4
    * @param x x
    * @param y y
    * @param withSpace print Move with spaces
    *
    * @return Name of Hole
    */
   public static String getHoleName(final int x, final int y, final boolean withSpace)
   {
      if (x < 0 || y < 0)
      {
         return "";
      }
      return "" + COL_NAMES[x] + (withSpace ? " " : "") + (y + 1);
   }

   /**
    * Paint the board.
    * @param g Graphics
    */
   public void paintComponent(final Graphics g)
   {
      //Compute sizes
      int dx = (this.getWidth() - MARGIN - BEGIN_X) / match.getXsize();
      int dy = (this.getHeight() - MARGIN - BEGIN_Y) / match.getYsize();
      dist = Math.min(dx, dy);

      diameter = dist / 2;
      //diameter = (maxSize <= smallB) ? bigD : ((maxSize >= bigB) ? smallD : mediumD);

      // now do the painting
      super.paintComponent(g);
      this.setBackground(BACKCOLOR);
      Graphics2D g2 = (Graphics2D) g;
      // antialiasing for Text
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      // antialiasing for Graphic
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      drawColumnLabels(g2, dist);
      drawField(g2);
   }

   /**
    * Draw labels for lines and columns on the outside.
    *
    * @param g2 Graphics2D
    * @param distance distance in pixel between pins
    */
   private void drawColumnLabels(final Graphics2D g2, int distance)
   {
      //set Font
      final int bigFont = 12;
      final int hInit = 50;
      final int horBor = 26;
      final int verBor = 20;
      // int fontsize = (distance < 8) ? smallFont : bigFont;
      int fontsize = Math.min(bigFont, distance);
      g2.setFont(new Font(null, Font.PLAIN, fontsize));
      g2.setColor(FONTCOLOR);
      //horizontal
      int h = hInit;
      for (int i = 0; i < match.getXsize(); h += dist, i++)
         g2.drawString(COL_NAMES[i], h, horBor);
      //vertical
      h = hInit;
      for (int i = 1; i <= match.getYsize(); h += dist, i++)
         g2.drawString("" + i, verBor, h);
   }

   /**
    * Draw the field.
    *
    * @param g2 Graphics2D
    */
   private void drawField(final Graphics2D g2)
   {
      g2.setColor(BOARDCOLOR);
      fieldX = BEGIN_X - MARGIN;
      fieldY = BEGIN_Y - MARGIN;
      fieldWidth = match.getXsize() * dist + MARGIN;
      fieldHeight = match.getYsize() * dist + MARGIN;

      g2.fill3DRect(fieldX, fieldY, fieldWidth, fieldHeight, true);

      drawStartingLines(g2);
      if (Control.isShowAreaLines())
      {
         drawDiagonalLines(g2);
      }
      drawHoles(g2);

   }

   /**
    * Draw all the holes.
    *
    * @param g2 Graphics2D
    */
   private void drawHoles(final Graphics2D g2)
   {

      boolean isPoint;
      //g2.setColor(Color.white);
      int l = BEGIN_X; //diameter + x1;
      for (int i = 0; i < match.getXsize(); l += dist, i++) //x
      {
         int j = BEGIN_Y; //diameter + y1;
         for (int k = 0; k < match.getYsize(); j += dist, k++) //y
         {
            if ((i == 0 || i == match.getXsize() - 1) && (k == 0 || k == match.getYsize() - 1))
            {
               continue;
            }
            if (match.getBoardDisplay().getPin(i, k) == Board.XPLAYER)
            {
               g2.setColor(getXColor());
               isPoint = true;
            } else if (match.getBoardDisplay().getPin(i, k) == Board.YPLAYER)
            {
               g2.setColor(getYColor());
               isPoint = true;
            } else
            {
               g2.setColor(getHoleColor());
               isPoint = false;
            }
            g2.fillOval(l, j, diameter, diameter);

            //mark last move
            if (isPoint && i == match.getMoveX(match.getMoveNr())
                  && k == match.getMoveY(match.getMoveNr()))
            {
               g2.drawOval(l - 2, j - 2, diameter + 3, diameter + 3);
            }


            //now lets draw the bridges
            g2.setStroke(new BasicStroke(2));
            int tox, toy;
            if (isPoint) //only real points have bridges
            {
               for (int index = 0; index < 4; index++)
               {
                  if (match.getBoardDisplay().isBridged(i, k, index))
                  {
                     final int mult = 1000;
                     tox = Board.bridgeEnd(i, k, index) / mult;
                     toy = Board.bridgeEnd(i, k, index) % mult;
                     int xvon = BEGIN_X + (i * this.dist) + diameter / 2;
                     int yvon = BEGIN_Y + (k * this.dist) + diameter / 2;
                     int xnach = BEGIN_X + (tox * this.dist) + diameter / 2;
                     int ynach = BEGIN_Y + (toy * this.dist) + diameter / 2;
                     g2.drawLine(xvon - 1, yvon - 1, xnach - 1, ynach - 1);
                  }
               }
            }
            g2.setStroke(new BasicStroke(1));
         }
      }
   }

   /**
    * Draw the 4 starting lines.
    *
    * @param g2 Graphics2D
    */
   private void drawStartingLines(final Graphics2D g2)
   {
      final int ysize = match.getYsize();
      final int xsize = match.getXsize();
      g2.setColor(getYColor());
      // top line
      g2.fill3DRect(BEGIN_X + dist, BEGIN_Y + diameter + (dist - diameter) / 2 - 1, (xsize - 3)
            * dist + diameter, 2, true);
      //bottom line
      g2.fill3DRect(BEGIN_X + dist, BEGIN_Y + dist * (ysize - 2) + diameter + (dist - diameter)
            / 2 - 1, (xsize - 3) * dist + diameter, 2, true);
      g2.setColor(getXColor());
      //left line
      g2.fill3DRect(BEGIN_X + diameter + (dist - diameter) / 2 - 1, BEGIN_Y + dist, 2,
            (ysize - 3) * dist + diameter, true);
      // right line
      g2.fill3DRect(BEGIN_X + dist * (xsize - 2) + diameter + (dist - diameter) / 2 - 1,
            BEGIN_Y + dist, 2, (ysize - 3) * dist + diameter, true);
   }

   /**
    * Draw diagonal lines.
    *
    * @param g2 Graphics2D
    */
   private void drawDiagonalLines(final Graphics2D g2)
   {
      final int ysize = match.getYsize();
      final int xsize = match.getXsize();

      int offx = BEGIN_X + 3;
      int offy = BEGIN_Y + 3;

      int ys2 = Math.min(xsize - 3, (ysize - 3) / 2);
      int xs2 = Math.min(ysize - 3, (xsize - 3) / 2);
      int ys = ys2 * 2;
      int xs = xs2 * 2;

      g2.setColor(getArealineColor());
      //upper left
      g2.drawLine(offx + dist, offy + dist, offx + dist * (xs + 1), offy + dist * (xs2 + 1));
      g2.drawLine(offx + dist, offy + dist, offx + dist * (ys2 + 1), offy + dist * (ys + 1));
      //upper right
      g2.drawLine(offx + dist * (xsize - 2), offy + 2 + dist, offx + dist * (xsize - 2 - xs),
            offy + 2 + dist * (xs2 + 1));
      g2.drawLine(offx + 2 + dist * (xsize - 2), offy + dist, offx + 2 + dist
            * (xsize - 2 - ys2), offy + dist * (ys + 1));
      //lower left
      g2.drawLine(offx + 2 + dist, offy + 1 + dist * (ysize - 2), offx + 2 + dist * (xs + 1),
            offy + 1 + dist * (ysize - 2 - xs2));
      g2.drawLine(offx + 2 + dist, offy + dist * (ysize - 2), offx + 2 + dist * (ys2 + 1),
            offy + dist * (ysize - 2 - ys));
      //lower right
      g2.drawLine(offx + dist * (xsize - 2), offy + 1 + dist * (ysize - 2), offx + dist
            * (xsize - 2 - xs), offy + 1 + dist * (ysize - 2 - xs2));
      g2.drawLine(offx + 1 + dist * (xsize - 2), offy + dist * (ysize - 2), offx + 1 + dist
            * (xsize - 2 - ys2), offy + dist * (ysize - 2 - ys));
   }

   /**
    * repaint window.
    */
   void update()
   {
      this.repaint();
   }

   /**
    * Compute X-column from Mousecoordinates.
    *
    * @param x mousecolumn
    * @return column of field
    */
   public int pixelToX(final int x)
   {
      return (x - BEGIN_X - diameter / 2 + dist / 2) / dist;
   }

   /**
    * Compute Y-Row from Mousecoordinates.
    * 
    * @param y mouserow
    * @return row of field
    */
   public int pixelToY(final int y)
   {
      return (y - BEGIN_Y - diameter / 2 + dist / 2) / dist;
   }

   /**
    * Get Height of playing field.
    * @return height
    */
   public int getFieldHeight()
   {
      return fieldHeight;
   }

   /**
    * Get Width of playing field.
    * @return width
    */
   public int getFieldWidth()
   {
      return fieldWidth;
   }

   /**
    * Get x-coordinate of upper left corner of playing field.
    * @return x
    */
   public int getFieldX()
   {
      return fieldX;
   }

   /**
    * Get y-coordinate of upper left corner of playing field.
    * @return y
    */
   public int getFieldY()
   {
      return fieldY;
   }

   /**
    * Get Color for X pins and bridges.
    * @return color
    */
   public static Color getXColor()
   {
      return colorSchemes[getColorScheme()][1];
   }
   /**
    * Get Color for Y pins and bridges.
    * @return color
    */
   public static Color getYColor()
   {
      return colorSchemes[getColorScheme()][0];
   }
   /**
    * Get Color for empty holes.
    * @return color
    */
   private static Color getHoleColor()
   {
      return colorSchemes[getColorScheme()][2];
   }
   /**
    * Get Color for arealines.
    * @return color
    */
   public static Color getArealineColor()
   {
      return colorSchemes[getColorScheme()][3];
   }


   /**
    * Get current Colorscheme.
    * @return colorscheme
    */
   private static int getColorScheme()
   {
      return colorScheme;
   }

   /**
    * Set Colorscheme.
    * @param colorSchemeIn color scheme
    */
   public static void setColorScheme(int colorSchemeIn)
   {
      colorScheme = colorSchemeIn;
   }
}

