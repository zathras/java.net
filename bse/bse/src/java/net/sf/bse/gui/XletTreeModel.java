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

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * @author Aleksi Peebles (aleksi.peebles@infocast.fi)
 * @version $Revision: 1.1 $ $Date: 2003/01/17 14:40:31 $
 */
public class XletTreeModel implements TreeModel
{
    private File root;
    private Vector listeners;
    private Hashtable signFiles;
    private boolean recursive;
    
    public XletTreeModel()
    {
        this(new File(System.getProperty("user.dir")));
    }
    
    public XletTreeModel(File root)
    {
        this.root = root;
        listeners = new Vector();
        signFiles = new Hashtable();
        recursive = true;
    }
    
    //
    // Public methods
    //
    
    public void setRoot(File root)
    {
        this.root = root;
        signFiles.clear();
        for (Enumeration e = listeners.elements(); e.hasMoreElements();)
        {
            ((TreeModelListener)e.nextElement()).treeStructureChanged(
                new TreeModelEvent(this, new File[] { root }));
        }        
    }
    
    public void setRecursiveSelection(boolean recursive)
    {
        this.recursive = recursive;
    }
    
    public boolean isRecursiveSelection()
    {
        return recursive;
    }
    
    public String getAlgorithm(File file)
    {
        return (String)signFiles.get(file);
    }
    
    public String[] getSignFileNames()
    {
        String[] fileNames = new String[signFiles.size()];
        Enumeration e = signFiles.keys();
        int i = 0;
        while (e.hasMoreElements())
        {
            fileNames[i] = ((File)e.nextElement()).getAbsolutePath();
            i++;
        }
        return fileNames;
    }
    
    public void setSigned(TreeSelectionModel selection, String algorithm)
    {
        TreePath[] paths = selection.getSelectionPaths();
        for (int i = 0; i < paths.length; i++)
        {
            File f = (File)paths[i].getLastPathComponent();
            if (f.isFile())
            {
                signFiles.put(f, algorithm);
            }
            else if (f.isDirectory() && recursive)
            {
                setSignedDirectory(f, algorithm);                
            }
        }
        fileTreeNodesChangedEvent();
    }
    
    public void setUnsigned(TreeSelectionModel selection)
    {
        TreePath[] paths = selection.getSelectionPaths();
        for (int i = 0; i < paths.length; i++)
        {
            File f = (File)paths[i].getLastPathComponent();            
            if (f.isFile())
            {
                signFiles.remove(f);
            }
            else if (f.isDirectory() && recursive)
            {
                setUnsignedDirectory(f);
            }
        }
        fileTreeNodesChangedEvent();
    }
    
    //
    // TreeModel interface methods
    //
    
    public void addTreeModelListener(TreeModelListener l)
    {
        listeners.add(l);
    }
    
    public Object getChild(Object parent, int index)
    {
        File[] files = ((File)parent).listFiles();
        if (index > -1 && index < files.length)
        {
            return files[index];
        }
        else
        {
            return null;
        }
    }
    
    public int getChildCount(Object parent)
    {
        if (((File)parent).isDirectory())
        {
            return ((File)parent).list().length;
        }
        else
        {
            return 0;
        }
    }
    
    public int getIndexOfChild(Object parent, Object child)
    {
        if (parent == null || child == null)
        {
            return -1;
        }
        
        File[] files = ((File)parent).listFiles();
        for (int i = 0; i < files.length; i++)
        {
            if (files[i] == child)
            {
                return i;
            }
        }
        return -1;
    }
    
    public Object getRoot()
    {
        return root;
    }
    
    public boolean isLeaf(Object node)
    {
        return !((File)node).isDirectory();
    }
    
    public void removeTreeModelListener(TreeModelListener l)
    {
        listeners.remove(l);
    }
    
    public void valueForPathChanged(TreePath path, Object newValue)
    {
    }    

    //
    // Private methods
    //
    
    private void setSignedDirectory(File dir, String algorithm)
    {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++)
        {
            if (files[i].isDirectory())
            {
                setSignedDirectory(files[i], algorithm);
            }
            else
            {
                signFiles.put(files[i], algorithm);
            }
        }
    }
    
    private void setUnsignedDirectory(File dir)
    {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++)
        {
            if (files[i].isDirectory())
            {
                setUnsignedDirectory(files[i]);
            }
            else
            {
                signFiles.remove(files[i]);
            }
        }
    }
    
    private void fileTreeNodesChangedEvent()
    {
        for (Enumeration e = listeners.elements(); e.hasMoreElements();)
        {
            ((TreeModelListener)e.nextElement()).treeNodesChanged(
                new TreeModelEvent(this, new File[] { root }));
        }        
    }    
}