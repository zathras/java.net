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

import java.awt.Component;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * @author Aleksi Peebles (aleksi.peebles@infocast.fi)
 * @version $Revision: 1.1 $ $Date: 2003/01/17 14:40:29 $
 */
public class XletTreeCellRenderer extends DefaultTreeCellRenderer
{
    private ImageIcon signedIcon = 
        new ImageIcon(getClass().getResource("signed.png"));
    
    public Component getTreeCellRendererComponent(
        JTree source, Object value, boolean selected, boolean expanded, 
        boolean leaf, int row, boolean hasFocus)
    {
        super.getTreeCellRendererComponent(
            source, value, selected, expanded, leaf, row, hasFocus);

        if (value.equals(source.getModel().getRoot()))
        {
            setText(File.separator);
        }
        else if (value instanceof File)
        {
            File file = (File)value;
            setText(file.getName());
            if (((XletTreeModel)source.getModel()).getAlgorithm(file) != null)
            {
                setIcon(signedIcon);
            }
        }
        
        return this;
    }    
}