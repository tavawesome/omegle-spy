import java.io.*;
import java.util.*;

public class OmegleSpy implements OmegleListener
{
    Omegle chat, partner;
    String name;
    boolean connected, disconnected;
    private boolean blocking;
    List<OmegleSpyListener> listeners;
    
    public OmegleSpy (String name)
    {
        chat = new Omegle();
        chat.addOmegleListener(this);
        
        this.name = name;
        
        partner = null;
        
        connected = false;
        blocking = false;
        disconnected = false;
        
        listeners = new LinkedList<OmegleSpyListener>();
    }
    
    public void setPartner (Omegle o)
    {
        partner = o;
    }
    public String getName ()
    {
        return name;
    }
    public void setBlocking (boolean b)
    {
        blocking = b;
        if (blocking) partner.stoppedTyping();
    }
    public boolean isBlocking ()
    {
        return blocking;
    }
    
    public void addOmegleSpyListener (OmegleSpyListener osl)
    {
        listeners.add(osl);
    }
    public void removeOmegleSpyListener (OmegleSpyListener osl)
    {
        listeners.remove(osl);
    }
    
    private boolean sendMsg (String msg)
    {
        return partner.sendMsg(msg);
    }
    
    public boolean startChat ()
    {
        if (partner == null) return false;
        if (!chat.start()) return false;
        
        while (!connected) Common.rest(50);
        return true;
    }
    
    public boolean sendExternalMessage (String msg)
    {
        boolean b = chat.sendMsg(msg);
        if (b)
        {
            for (OmegleSpyListener osl : listeners)
                osl.externalMessageSent(this, msg);
        }
        
        return b;
    }
    
    public boolean disconnect ()
    {
        boolean b = chat.disconnect();
        if (b) disconnected = true;
        return b;
    }
    
    private void gotMessage (String msg)
    {
        if (!isBlocking())
        {
            sendMsg(msg);
            for (OmegleSpyListener osl : listeners)
            {
                osl.messageTransferred(this, msg);
            }
        }
        else
        {
            for (OmegleSpyListener osl : listeners)
                osl.messageBlocked(this, msg);
        }
    }
    
    public void eventFired (Omegle src, String event, String... args)
    {
        if (event.equals(Omegle.EV_CONNECTED))
        {
            connected = true;
        }
        else if (event.equals(Omegle.EV_TYPING))
        {
            if (!isBlocking())
            {
                partner.typing();
            }
            for (OmegleSpyListener osl : listeners)
                osl.isTyping(this);
        }
        else if (event.equals(Omegle.EV_STOPPED_TYPING))
        {
            partner.stoppedTyping();
            for (OmegleSpyListener osl : listeners)
                osl.stoppedTyping(this);
        }
        else if (event.equals(Omegle.EV_MSG))
        {
            gotMessage(args[0]);
        }
        else if (event.equals(Omegle.EV_DISCONNECT))
        {
            disconnected = true;
        }
    }
    public void messageSent (Omegle src, String msg) {}
}

interface OmegleSpyListener
{
    public void messageTransferred (OmegleSpy src, String msg);
    public void messageBlocked (OmegleSpy src, String msg);
    public void externalMessageSent (OmegleSpy src, String msg);
    public void isTyping (OmegleSpy src);
    public void stoppedTyping (OmegleSpy src);
}
