SRCS = Common.class DesperationWindow.class Omegle.class OmegleSpy.class OmegleSpyPanel.class OmegleSpyWindow.class OmegleSpyApplet.class

FILES = base.html names.txt makefile server_name.txt


all: OmegleSpyAll jar

OmegleSpyAll: $(SRCS)

Common.class: Common.java
	javac Common.java

Omegle.class: Omegle.java
	javac Omegle.java
OmegleSpy.class: OmegleSpy.java
	javac OmegleSpy.java
OmegleSpyPanel.class: OmegleSpyPanel.java
	javac OmegleSpyPanel.java
OmegleSpyWindow.class: OmegleSpyWindow.java
	javac OmegleSpyWindow.java
OmegleSpyApplet.class: OmegleSpyApplet.java
	javac OmegleSpyApplet.java

DesperationWindow.class: DesperationWindow.java
	javac DesperationWindow.java

clean:
	rm -f *.class

jar:
	jar cmf META-INF/MANIFEST.MF OmegleSpy.jar *.java *.class $(FILES)
