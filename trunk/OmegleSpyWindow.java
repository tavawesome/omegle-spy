import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;

public class OmegleSpyWindow extends JFrame implements HyperlinkListener
{
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
    public OmegleSpyWindow ()
    {
        OmegleSpyPanel osp = new OmegleSpyPanel();
        setLayout(new BorderLayout());
        add(osp);
        osp.addHyperlinkListener(this);
    }
    public static void main (String[] args)
    {
        OmegleSpyWindow osw = new OmegleSpyWindow();
        osw.setTitle("Spy on Omegle!");
        osw.setSize(800, 600);
        Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension s = osw.getSize();
        osw.setLocation((ss.width - s.width)/2, (ss.height - s.height)/2);
        osw.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        osw.setVisible(true);
    }
}
