import javax.swing.*;

public class OmegleSpyApplet extends JApplet
{
	public void init ()
	{
		Omegle.setOmegleRoot("http://users.wpi.edu/~sfoley/omegle_spy/");
		add(new OmegleSpyPanel());
	}
}
