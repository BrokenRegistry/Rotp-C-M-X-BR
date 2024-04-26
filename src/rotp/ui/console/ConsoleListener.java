package rotp.ui.console;

public interface ConsoleListener {
	public String getEmpireInfo(String sep);
	public String getMessageRemark();
	public String getMessageRemarkDetail();
	public String[] getDataLines();
	public String[][] getOptions();
	public boolean consoleResponse(int i);
	public void initForConsole();
}
