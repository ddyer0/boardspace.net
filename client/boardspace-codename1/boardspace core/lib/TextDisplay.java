package lib;

import bridge.*;
public class TextDisplay extends XFrame implements ActionListener,ItemListener,Config
{
    /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	public static int default_bufferchars = 10000;
    public TextArea area;		// not jtextarea, keep scrolling functionality
    public JCheckBoxMenuItem[] fontItems;
    public JCheckBoxMenuItem[] styleItems;
    public JCheckBoxMenuItem[] sizeItems;
    public JMenu editMenu = new XJMenu("Edit",true);
    public JMenu fontMenu = new XJMenu("Font",true);
    public JMenu sizeMenu = new XJMenu("Size",true);
    public JMenu styleMenu = new XJMenu("Style",true);
    public JMenuItem selectAllItem = new JMenuItem("Select all");
    String[] fonts = FONT_FAMILIES;
    String[] styles = { "Plain", "Bold", "Italic" };
    G.Style styleCode[] = { G.Style.Plain,G.Style.Bold,G.Style.Italic};
    String[] sizes = { "8", "9", "10", "12", "14", "16", "18", "24" };
    String fname;
    G.Style fstyle;
    int fsize;
    int bufferchars;

    public void setBounds(int l,int t,int w,int h)
    {	super.setBounds(l, t, w, h);
    	//area.setBounds(l, t, w, h);
    }
    public TextDisplay(String title)
    {	super(title);
        // Initialize
        area = new TextArea("", 30, 80);	// not jtextarea, keep scrolling functionality
        area.setVisible(true);
        fname = "Courier";
        fstyle = G.Style.Plain;
        fsize = G.standardizeFontSize(G.defaultFontSize);

        // Create edit menu
        editMenu.add(selectAllItem);
        editMenu.add(new JMenuItem("-"));
         
        //    editMenu.add(new MenuItem("Find..."));
        addToMenuBar(editMenu);

        // Create font menu.  This is only good starting jdk1.4
        // fonts = G.getFontFamilies();
        // this method is deprecated, but available since forever
        // fonts = getToolkit().getFontList();

        if (fonts != null)
        {
            fontItems = new JCheckBoxMenuItem[fonts.length];

            for (int i = 0; i < fonts.length; i++)
            {
                fontItems[i] = new JCheckBoxMenuItem(fonts[i]);
                fontMenu.add(fontItems[i]);

                if (fonts[i].equals(fname))
                {
                    fontItems[i].setState(true);
                }
            }

            addToMenuBar(fontMenu);
        }

        // Create style menu
        styleItems = new JCheckBoxMenuItem[styles.length];

        for (int i = 0; i < styles.length; i++)
        {
            styleItems[i] = new JCheckBoxMenuItem(styles[i]);
            styleMenu.add(styleItems[i]);

            if (styleCode[i] == fstyle)
            {
                styleItems[i].setState(true);
            }
        }

       addToMenuBar(styleMenu);

        // Create size menu
        sizeItems = new JCheckBoxMenuItem[sizes.length];

        for (int i = 0; i < sizes.length; i++)
        {
            sizeItems[i] = new JCheckBoxMenuItem(sizes[i]);
            sizeMenu.add(sizeItems[i]);

            if (sizes[i].equals(String.valueOf(fsize)))
            {
                sizeItems[i].setState(true);
            }
        }

        addToMenuBar(sizeMenu);
 
        // Show window
        area.setFont(G.getFont(fname, fstyle, fsize));

        add(area);
        
        selectAllItem.addActionListener(this);
        fontMenu.addActionListener(this);
        styleMenu.addActionListener(this);
        sizeMenu.addActionListener(this);
        editMenu.addActionListener(this);
       
        if (fontItems != null)
        {
            for (int i = 0; i < fontItems.length; i++)
            {
                fontItems[i].addItemListener(this);
            }
        }

        for (int i = 0; i < styleItems.length; i++)
        {
            styleItems[i].addItemListener(this);
        }

        for (int i = 0; i < sizeItems.length; i++)
        {
            sizeItems[i].addItemListener(this);
        }
        setVisible(true);
        // this makes the popup menu on the toolbar appear immediately
		MasterForm.getMasterPanel().adjustTabStyles(); 

    }

    // Handle component events
    public boolean doActionPerformed(ActionEvent ev)
    {
        if (ev.getSource()==selectAllItem)
        {
            area.selectAll();

            return (true);
        }

        return (false);
    }

    public boolean doItemStateChanged(JCheckBoxMenuItem label)
    {
        boolean handled = false;

        // Handle font menu
        for (int i = 0; i < fontItems.length; i++)
        {
            if (label.equals(fontItems[i]))
            {
                for (int j = 0; j < fontItems.length; j++)
                    if (i != j)
                    {
                        fontItems[j].setState(false);
                    }

                fname = fonts[i];
                area.setFont(G.getFont(fname, fstyle, fsize));
                handled = true;
            }
        }

        // Handle size menu
        for (int i = 0; i < sizeItems.length; i++)
        {
            if (label.equals(sizeItems[i]))
            {
                for (int j = 0; j < sizeItems.length; j++)
                    if (i != j)
                    {
                        sizeItems[j].setState(false);
                    }

                fsize = G.IntToken(sizes[i]);
                area.setFont(G.getFont(fname, fstyle, fsize));
                handled = true;
            }
        }

        // Handle style menu
        for (int i = 0; i < styleItems.length; i++)
        {
            if (label.equals(styleItems[i]))
            {
                for (int j = 0; j < styleItems.length; j++)
                    if (i != j)
                    {
                        styleItems[j].setState(false);
                    }

                fstyle = styleCode[i];
                area.setFont(G.getFont(fname, fstyle, fsize));
                handled = true;
            }
        }

        return (handled);
    }
    // Handle component events
    public void actionPerformed(ActionEvent evt)
    {
             doActionPerformed(evt);
    }

    public void itemStateChanged(ItemEvent evt)
    {	JCheckBoxMenuItem lab = (JCheckBoxMenuItem)evt.getItem();
        doItemStateChanged(lab);
    }
    public void setText(String s)
    {
        area.setText(s);
        repaint();
    }

    
}
