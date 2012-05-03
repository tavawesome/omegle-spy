import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class DesperationWindow extends JFrame implements ListSelectionListener,
    HyperlinkListener, ActionListener
{
    JList convoList;
    MutableComboBoxModel lister;
    JPanel panepane;
    List<JEditorPane> panes;
    JEditorPane currentEp;
    JButton save;
    
    public DesperationWindow ()
    {
        super("All logs");
        
        convoList = new JList(lister = new DefaultComboBoxModel());
        convoList.addListSelectionListener(this);
        convoList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panepane = new JPanel();
        panepane.setLayout(new BorderLayout());
        panes = new ArrayList<JEditorPane>();
        currentEp = null;
        
        save = new JButton("Save this convo");
        save.addActionListener(this);
        save.setVisible(false);
        
        setLayout(new BorderLayout());
        add(Common.scroller(convoList), BorderLayout.WEST);
        add(panepane, BorderLayout.CENTER);
        add(save, BorderLayout.SOUTH);
    }
    
    public void hyperlinkUpdate (HyperlinkEvent ev)
    {
        if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
        {
            try
            {
                Common.openURL(ev.getURL().toString());
            }
            catch (NullPointerException ex)
            {
                Common.showError(this, "Invalid URL");
            }
            catch (Exception ex)
            {
                Common.showError(this, ex.getMessage());
            }
        }
    }

    
    public void addHTML (String baseHTML, String html)
    {
        String all = baseHTML.replace("<!--%s-->", html);
        JEditorPane ep = new JEditorPane("text/html", all);
        ep.addHyperlinkListener(this);
        ep.setEditable(false);
        panes.add(ep);
        lister.addElement("Convo " + panes.size());
    }
    
    public void valueChanged (ListSelectionEvent ev)
    {
        panepane.removeAll();
        int index = convoList.getSelectedIndex();
        panepane.add(Common.scroller(currentEp = panes.get(index)));
        if (!save.isVisible()) save.setVisible(true);
        //panepane.validate();
        validate();
    }
    
    public void actionPerformed (ActionEvent ev)
    {
        Object src = ev.getSource();
        if (src == save)
        {
            try
            {
                Common.guiWriteHtmlFile(currentEp.getText(), this);
            }
            catch (IOException ex)
            {
                Common.showError(this,"Could not save file: "+ex.getMessage());
            }
        }
    }
}
