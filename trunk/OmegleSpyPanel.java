import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.List;

public class OmegleSpyPanel extends JPanel implements Runnable,
	ActionListener, OmegleSpyListener, ComponentListener, HyperlinkListener
{
	public static final int REST_TIME = 100;
	public static final Font FONT = new Font("Verdana", Font.PLAIN, 14);
	public static final Pattern esc_html_regex = Pattern.compile("[&<>\"\']");
	public static final Pattern nl_regex = Pattern.compile
	(
		"\\r\\n|\\n\\r|\\n|\\r"
	);
	public static final Pattern url_regex = Pattern.compile
	(
		"([a-z]{2,6}://)?(?:[a-z0-9\\-]+\\.)+[a-z]{2,6}(?::\\d+)?(/\\S*)?",
		Pattern.CASE_INSENSITIVE
	);
	public static final Pattern email_regex = Pattern.compile
	(
		"[a-z0-9!#$%&\'*+\\-/=?\\^_`{|}~.]+@(?:[a-z0-9\\-]+.)+[a-z]{2,6}",
		Pattern.CASE_INSENSITIVE
	);
	//$1 = protocol (optional)
	//$2 = path (optional)
	
	//some constants:
	static final String[] CLASS_NAMES = {"youmsg", "strangermsg"};
	static final String BTN_LINK = "btn-link",
						CONVO_LINK = "convo-link";
	static final Pattern delcon_regex = Pattern.compile("delete-convo-(\\d+)");
	static final Pattern savcon_regex = Pattern.compile("save-convo-(\\d+)"); 
	static final Pattern convo_regex = Pattern.compile("convo-(\\d+)");
	
	String[] possibleNames;
	
	JPanel buttonPanel;
	JLabel countLabel;
	JButton reset, stopper, clearScreen, showDw;
	JCheckBox blocker, linger, autopilot, autoscroll;
	
	JEditorPane console;
	HTMLDocument doc;
	Element logbox, currentChat, currentConvo;
	//the chat is the entire thing: delete/save buttons, and horizontal rules
	//the convo is just the talk part, the part you will save
	private int convoNum;
	
	JScrollPane scroller;
	JScrollBar vbar;
	
	DesperationWindow dw;
	
	OmegleSpy[] spies;
	String[] names;
	JPanel usersPanel;
	JPanel[] panels;
	JLabel[] ls;
	JTextField[] flds;
	JLabel[] lbls;
	JButton[] btns;
	
	String baseHTML;
	
	List<HyperlinkListener> hlListeners = new LinkedList<HyperlinkListener>();
	
	static
	{
		try
		{
		   UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception ex) {}
	}
	
	public String ts ()
	{
		return "<span class='timestamp'>[" + Common.timestamp() + "]</span>";
	}
	
	private void printLogItem (Element e, String line)
	{
		printHTML(e, "<div class='logitem'>" + ts() + " " + line + "</div>");
	}
	private void printStatusLog (Element e, String sl)
	{
		printLogItem(e, "<span class='statuslog'>" + sl + "</span>");
	}
	private void printLabelledMsg (String className, String from, String msg)
	{
		//from = escapeHTML(from);
		msg = escapeHTML(msg);
		
		StringBuffer sb = new StringBuffer();
		Matcher m = url_regex.matcher(msg);
		while (m.find())
		{
			String rep;
			if (m.group(1) != null || m.group(2) != null)
			{
				String proto = (m.group(1) == null) ? "http://" : "";
				rep = "<a href='" + proto + "$0' target='_blank' " +
					  "class='" + CONVO_LINK + "'>$0</a>";
			}
			else
			{
				rep = m.group();
			}
			m.appendReplacement(sb, rep);
		}
		m.appendTail(sb);
		msg = sb.toString();
		
		Element e = currentConvo;
		printLogItem(e, "<span class='"+className+"'>"+from+":</span> "+msg);
	}
	
	private void printRegMsg (OmegleSpy from, String msg)
	{
		printLabelledMsg(CLASS_NAMES[indexOf(from)], from.getName(), msg);
	}
	private void printBlockedMsg (OmegleSpy from, String msg)
	{
		String className = CLASS_NAMES[indexOf(from)] + "-blocked";
		String fromLbl = "<s>&lt;&lt;"+from.getName()+"&gt;&gt;</s>";
		printLabelledMsg(className, fromLbl, msg);
	}
	private void printSecretMsg (OmegleSpy to, String msg)
	{
		int otherIndex = spies.length - indexOf(to) - 1;
		String className = CLASS_NAMES[otherIndex] + "-secret";
		printLabelledMsg(className, "{{from " + names[otherIndex] + "}}", msg);
	}
	
	Map<Integer, String> blocks = new HashMap<Integer, String>();
	private void printHTML (Element e, String html)
	{
		try
		{
			if (e == currentConvo)
			{
				String record = blocks.get(convoNum);
				record = (record == null) ? html : record + html;
				blocks.put(convoNum, record);
			}
			doc.insertBeforeEnd(e, html);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	
	public static String escapeHTML (String text)
	{
		if (text == null) return null;
		Matcher m = esc_html_regex.matcher(text);
		StringBuffer sb = new StringBuffer();
		while (m.find())
		{
			char c = m.group().charAt(0);
			String replace;
			switch (m.group().charAt(0))
			{
				case '&': replace = "amp"; break;
				case '<': replace = "lt"; break;
				case '>': replace = "gt"; break;
				case '"': replace = "quot"; break;
				
				default: replace = "#" + (int)c; break;
			}
			m.appendReplacement(sb, "&" + replace + ";");
		}
		m.appendTail(sb);
		
		m = nl_regex.matcher(sb);
		return m.replaceAll("$0<br>");
	}
	
	public void hyperlinkUpdate (HyperlinkEvent ev)
	{
		if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) try
		{
			Element e = ev.getSourceElement();
			HTMLDocument.RunElement re = (HTMLDocument.RunElement)e;
			AttributeSet atts = (AttributeSet)re.getAttributes().
									getAttribute(HTML.Tag.A);
			String className = (String)atts.getAttribute(HTML.Attribute.CLASS);
			if (className.equals(BTN_LINK))
			{
				String id = (String)atts.getAttribute(HTML.Attribute.ID);
				Matcher m;
				if ((m = savcon_regex.matcher(id)).matches())
				{
					int ci = Integer.parseInt(m.group(1));
					String ct = baseHTML.replace("<!--%s-->", blocks.get(ci));
					Common.guiWriteHtmlFile(ct, this);
				}
				return;
			}
		}
		catch (ClassCastException ex)
		{
			//then i want to keep going
		}
		catch (NullPointerException ex)
		{
			//then i want to keep going
		}
		catch (IOException ex)
		{
			Common.showError(this, "Could not save file: " + ex.getMessage());
			return;
		}
		
		for (HyperlinkListener hl : hlListeners)
		{
			hl.hyperlinkUpdate(ev);
		}
	}
	public void addHyperlinkListener (HyperlinkListener l)
	{
		hlListeners.add(l);
	}
	public void removeHyperlinkListener (HyperlinkListener l)
	{
		hlListeners.remove(l);
	}
	
	public void actionPerformed (ActionEvent ev)
	{
		Object src = ev.getSource();
		if (src == flds[0] || src == flds[1])
		{
			JTextField fld = (JTextField)src;
			int index = (fld == flds[0]) ? 0 : 1;
			if (fld.getText().length() > 0)
			{
				OmegleSpy spy = spies[index];
				if (spy.sendExternalMessage(fld.getText())) fld.setText("");
			}
		}
		else if (src == btns[0] || src == btns[1])
		{
			JButton btn = (JButton)src;
			int index = (btn == btns[0]) ? 0 : 1;
			OmegleSpy otherSpy = spies[spies.length - index - 1];
			if (otherSpy != null)
			{
				linger.setSelected(true);
			}
			OmegleSpy spy = spies[index];
			spy.disconnect();
		}
		else if (src == reset)
		{
			new Thread(this).start();
		}
		else if (src == stopper)
		{
			for (OmegleSpy s : spies) if (s != null) s.disconnect();
		}
		else if (src == blocker)
		{
			for (OmegleSpy s : spies)
				if (s != null) s.setBlocking(blocker.isSelected());
		}
		else if (src == clearScreen)
		{
			if (currentConvo != null)
			{
				String convoText = blocks.get(convoNum);
				String html = baseHTML.replace("<!--%s-->",
					"<div id='logbox'><div id='chat-" + convoNum + "'>" +
					"<div id='convo-" + convoNum + "'>" + convoText +
					"</div></div></div>");
				console.setText(html);
				doc = (HTMLDocument)console.getDocument();
				logbox = doc.getElement("logbox");
				currentChat = doc.getElement("chat-" + convoNum);
				currentConvo = doc.getElement("convo-" + convoNum);
				blocks.clear();
				blocks.put(convoNum, convoText);
			}
			else
			{
				String html = baseHTML.replace("<!--%s-->",
											   "<div id='logbox'></div>");
				console.setText(html);
				doc = (HTMLDocument)console.getDocument();
				logbox = doc.getElement("logbox");
			}
		}
		else if (src == showDw)
		{
			dw.setVisible(true);
		}
	}
	
	public void componentHidden (ComponentEvent ev) {}
	public void componentShown (ComponentEvent ev) {}
	public void componentMoved (ComponentEvent ev) {}
	public void componentResized (ComponentEvent ev)
	{
		if (autoscroll.isSelected())
		{
			vbar.setValue(vbar.getMaximum() - vbar.getVisibleAmount());
		}
	}
	
	private int indexOf (OmegleSpy spy)
	{
		return spy == spies[0] ? 0 : 1;
	}
	
	public void run ()
	{
		buttonPanel.removeAll();
		buttonPanel.add(stopper);
		stopper.setEnabled(false);
		
		validate();
		buttonPanel.repaint();
		
		// GETTING THE CHAT READY
		
		while (true)
		{
			randomizeNames();
			for (int k=0; k<spies.length; k++)
			{
				String n = names[k], on = names[names.length - k - 1];
				OmegleSpy spy = spies[k] = new OmegleSpy(n);
				ls[k].setText("To "+ n +"; From " + on);
				btns[k].setText("Disconnect " + n);
				spy.addOmegleSpyListener(this);
			}
			
			spies[0].setPartner(spies[1].chat);
			spies[1].setPartner(spies[0].chat);
			
			convoNum++;
			
			String chatID = "chat-" + convoNum;
			String convoID = "convo-" + convoNum;
			printHTML(logbox, "<div id='" + chatID + "'>" +
							  "<div id='" + convoID + "'>" +
							  "</div></div>");
			currentChat = doc.getElement(chatID);
			currentConvo = doc.getElement(convoID);
			
			printStatusLog(currentConvo, "Finding two strangers...");
			for (OmegleSpy spy : spies)
			{
				spy.startChat();
				printStatusLog(currentConvo, spy.name + " connected");
			}
			
			
			//CHAT ACTUALLY STARTS HERE
			for (OmegleSpy s : spies) s.setBlocking(blocker.isSelected());
			
			usersPanel.setVisible(true);
			stopper.setEnabled(true);
			
			validate();
			buttonPanel.repaint();
			
			
			int index = -1;
			firstDisc:
			while (true)
			{
				for (int k=0; k<spies.length; k++)
				{
					if (spies[k].disconnected)
					{
						index = k;
						break firstDisc;
					}
				}
				Common.rest(REST_TIME);
			}
			printStatusLog(currentConvo, spies[index].name + " disconnected");
			lbls[index].setText(" ");
			spies[index] = null;
			
			usersPanel.remove(panels[index]);
			validate();
			
			
			int otherIndex = spies.length - index - 1;
			OmegleSpy other = spies[otherIndex];
			if (linger.isSelected())
			{
				while (!other.disconnected) Common.rest(REST_TIME);
			}
			else
			{
				other.disconnect();
			}
			printStatusLog(currentConvo, other.name + " disconnected");
			
			usersPanel.add(panels[index], index);
			
			lbls[otherIndex].setText(" ");
			spies[otherIndex] = null;
			
			// CHAT ENDS HERE
			stopper.setEnabled(false);
			
			usersPanel.setVisible(false);
			validate();
			buttonPanel.repaint();
			
			printHTML(currentChat,
					  "<div>You can <a href='#' class='" + BTN_LINK + "' " +
					  "id='save-convo-" + convoNum + "'>" + 
					  "save this conversation</a></div>");
			printHTML(currentChat, "<hr>");
			
			
			currentConvo = null;
			currentChat = null;
			
			dw.addHTML(baseHTML, blocks.get(convoNum));
			
			if (!autopilot.isSelected())
			{
				break;
			}
		}
		
		buttonPanel.removeAll();
		buttonPanel.add(reset);
	}
	
	public void messageTransferred (OmegleSpy src, String msg)
	{
		printRegMsg(src, msg);
		int index = indexOf(src);
		lbls[index].setText(" ");
	}
	public void messageBlocked (OmegleSpy src, String msg)
	{
		printBlockedMsg(src, msg);
		int index = indexOf(src);
		lbls[index].setText(" ");
	}
	public void externalMessageSent (OmegleSpy src, String msg)
	{
		printSecretMsg(src, msg);
	}
	
	public void isTyping (OmegleSpy src)
	{
		lbls[indexOf(src)].setText(src.name + " is typing...");
	}
	public void stoppedTyping (OmegleSpy src)
	{
		lbls[indexOf(src)].setText(" ");
	}
	
	private boolean loadNames ()
	{
		try
		{
			List<String> namesList = new LinkedList<String>();
			InputStream is = Common.getLocalFileInputStream("names.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = br.readLine()) != null)
			{
				if (line.trim().length() > 0)
				{
					namesList.add(line);
				}
			}
			possibleNames = namesList.toArray(names);
			return true;
		}
		catch (Exception ex)
		{
			possibleNames = new String[0];
			System.err.println("Could not load names file: "+ex.getMessage());
			return false;
		}
	}
	private boolean randomizeNames ()
	{
		//assume this is called when a chat is NOT going on
		if (possibleNames.length > 2)
		{
			int firstIndex = (int)(Math.random()*possibleNames.length);
			int secondIndex;
			do
			{
				secondIndex = (int)(Math.random()*possibleNames.length);
			}
			while (firstIndex == secondIndex);
			names[0] = possibleNames[firstIndex];
			names[1] = possibleNames[secondIndex];
			return true;
		}
		else
		{
			System.err.println("length = " + possibleNames.length);
			return false;
		}
	}
	
	
	public OmegleSpyPanel ()
	{
		setLayout(new BorderLayout());
		
		convoNum = 0;
		
		reset = new JButton("Start new conversation");
		reset.setFont(FONT);
		stopper = new JButton("Disconnect conversation");
		stopper.setFont(FONT);
		stopper.setEnabled(false);
		
		countLabel = new JLabel(" " + Omegle.count());
		countLabel.setFont(FONT);
		
		blocker = new JCheckBox("Block messages");
		blocker.setFont(FONT);
		linger = new JCheckBox("Linger");
		linger.setFont(FONT);
		linger.setSelected(true);
		autopilot = new JCheckBox("Autopilot");
		autopilot.setFont(FONT);
		autoscroll = new JCheckBox("Auto scroll");
		autoscroll.setFont(FONT);
		autoscroll.setSelected(true);
		showDw = new JButton("Desperation!");
		showDw.setFont(FONT);
		showDw.addActionListener(this);
		
		try
		{
			baseHTML = Common.fileAsString("base.html");
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			System.exit(1);
			//stupid java:
			baseHTML = "";
		}
		String html = baseHTML.replace("<!--%s-->", "<div id='logbox'></div>");
		console = new JEditorPane("text/html", html);
		console.addComponentListener(this);
		console.addHyperlinkListener(this);
		doc = (HTMLDocument)console.getDocument();
		logbox = doc.getElement("logbox");
		currentChat = null;
		currentConvo = null;
		console.setEditable(false);
		scroller = new JScrollPane(console);
		vbar = scroller.getVerticalScrollBar();
		vbar.setUnitIncrement(16);
		dw = new DesperationWindow();
		dw.setSize(640, 480);
		dw.setLocation(100, 100);
		
		spies = new OmegleSpy[2];
		names = new String[]{"Alpha", "Beta"};
		if (loadNames()) randomizeNames();
		usersPanel = new JPanel(new GridLayout(1, 2));
		usersPanel.setVisible(false);
		panels = new JPanel[2];
		ls = new JLabel[2];
		flds = new JTextField[2];
		lbls = new JLabel[2];
		btns = new JButton[2];
		
		buttonPanel = new JPanel(new BorderLayout());
		
		for (int k=0; k<panels.length; k++)
		{
			JTextField tf = flds[k] = new JTextField();
			tf.setFont(FONT);
			tf.setActionCommand("" + k);
			tf.addActionListener(this);
			
			JLabel toLabel = ls[k] = new JLabel(" ");
			toLabel.setFont(FONT);
			toLabel.setForeground(Color.GRAY);
			JPanel toPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			toPanel.add(toLabel);
			
			JLabel label = lbls[k] = new JLabel(" ");
			label.setFont(FONT);
			
			JButton b = btns[k] = new JButton();
			b.setFont(FONT);
			b.setActionCommand("" + k);
			b.addActionListener(this);
			
			JPanel p = panels[k] = new JPanel(new GridLayout(4, 1));
			p.add(tf);
			p.add(toPanel);
			p.add(label);
			p.add(b);
			
			usersPanel.add(p);
		}
		
		reset.addActionListener(this);
		stopper.addActionListener(this);
		blocker.addActionListener(this);
		
		buttonPanel.add(reset);
		
		clearScreen = new JButton("Clear screen");
		clearScreen.setFont(FONT);
		clearScreen.addActionListener(this);
		
		new Thread()
		{
			public void run ()
			{
				while (true)
				{
					countLabel.setText(" " + Omegle.count());
					Common.rest(2000);
				}
			}
		}.start();
		
		JPanel settingsPanel = new JPanel(new FlowLayout());
		settingsPanel.add(blocker);
		settingsPanel.add(linger);
		settingsPanel.add(autopilot);
		settingsPanel.add(clearScreen);
		settingsPanel.add(showDw);
		settingsPanel.add(autoscroll);
		JPanel northPanel = new JPanel(new GridLayout(3, 1));
		northPanel.add(buttonPanel);
		northPanel.add(countLabel);
		northPanel.add(settingsPanel);
		
		add(northPanel, BorderLayout.NORTH);
		add(scroller, BorderLayout.CENTER);
		add(usersPanel, BorderLayout.SOUTH);
	}
}
