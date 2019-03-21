package net.sf.bse.gui;

/*
 * Copyright (c) 2002-2003 BSE project contributors 
 * (http://bse.sourceforge.net/)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

/**
 * @author Aleksi Peebles (aleksi.peebles@infocast.fi)
 * @version $Revision: 1.1 $ $Date: 2003/01/17 14:40:27 $
 */
public class XletTree extends JTree implements ActionListener, TreeModelListener
{    
    private JPopupMenu popup;
    
    public XletTree()
    {
        this(new File(System.getProperty("user.dir")));
    }
    
    public XletTree(File root)
    {
        super(new XletTreeModel(root));
        setCellRenderer(new XletTreeCellRenderer());
        getModel().addTreeModelListener(this);
        
        popup = new JPopupMenu();
        JMenuItem item = new JMenuItem("Set Signed");
        item.addActionListener(this);
        popup.add(item);
        item = new JMenuItem("Set Unsigned");
        item.addActionListener(this);
        popup.add(item);
        
        MouseListener popupListener = new PopupListener();
        addMouseListener(popupListener);
    }
    
    public void setRoot(File root)
    {
        ((XletTreeModel)getModel()).setRoot(root);
    }
    
    public File getRoot()
    {
        return (File)((XletTreeModel)getModel()).getRoot();
    }
    
    public void setRecursiveSelection(boolean recursive)
    {
        ((XletTreeModel)getModel()).setRecursiveSelection(recursive);
    }
    
    public boolean isRecursiveSelection()
    {
        return ((XletTreeModel)getModel()).isRecursiveSelection();
    }
    
    public String[] getSignFileNames()
    {
        return ((XletTreeModel)getModel()).getSignFileNames();
    }
    
    //
    // ActionListener interface method
    //
    
    /** 
     * Invoked when a popup menu item is selected.
     */
    public void actionPerformed(ActionEvent e)
    {
        XletTreeModel model = (XletTreeModel)getModel();
        if (e.getActionCommand().equals("Set Signed"))
        {
            // Hopefully other algorithms will be supported in the future...
            model.setSigned(getSelectionModel(), "MD5");
        }
        else
        {
            model.setUnsigned(getSelectionModel());
        }
    }
    
    //
    // TreeModelListener interface methods
    //
    
    public void treeNodesChanged(TreeModelEvent e)
    {
        repaint();
    }
    
    public void treeNodesInserted(TreeModelEvent e)
    {
    }
    
    public void treeNodesRemoved(TreeModelEvent e)
    {
    }
    
    public void treeStructureChanged(TreeModelEvent e)
    {
    }
    
    /**
     * Popup menu listener class
     */    
    private class PopupListener extends MouseAdapter
    {
        public void mousePressed(MouseEvent e) 
        {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e)
        {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e)
        {
            if (getSelectionCount() > 0 && e.isPopupTrigger())
            {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
}