import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Calendar;
import java.util.Vector;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.Ticker;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import cc.nnproject.json.JSONObject;

public class GH extends MIDlet implements CommandListener, ItemCommandListener, Runnable {
	
	static final int RUN_LOAD_FORM = 1;
	
	private static final String APIURL = "https://api.github.com/";
	private static final String API_VERSION = "2022-11-28";

	// fonts
	static final Font largefont = Font.getFont(0, 0, Font.SIZE_LARGE);
	static final Font medboldfont = Font.getFont(0, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
	static final Font medfont = Font.getFont(0, 0, Font.SIZE_MEDIUM);
	static final Font smallboldfont = Font.getFont(0, Font.STYLE_BOLD, Font.SIZE_SMALL);
	static final Font smallfont = Font.getFont(0, 0, Font.SIZE_SMALL);

	private static Display display;
	static GH midlet;

	private static String version;
	
	// settings
	private static String proxyUrl = "http://nnp.nnchan.ru/hproxy.php?";
	private static boolean useProxy = false;

	// threading
	private static int run;
	private static Object runParam;
	private static int running;

	// ui commands
	private static Command exitCmd;
	static Command backCmd;
	private static Command settingsCmd;
	private static Command aboutCmd;
	
	private static Command goCmd;
	static Command downloadCmd;
	static Command openCmd;
	
	static Command ownerCmd;
	static Command releasesCmd;
	static Command forksCmd;
	static Command reposCmd;

	// ui
	private static Form mainForm;
	private static Vector formHistory = new Vector();

	private static TextField field;

	protected void destroyApp(boolean unconditional)  {}

	protected void pauseApp() {}

	protected void startApp() {
		if (midlet != null) return;
		midlet = this;
		
		version = getAppProperty("MIDlet-Version");
		display = Display.getDisplay(this);
		
		exitCmd = new Command("Exit", Command.EXIT, 2);
		backCmd = new Command("Back", Command.BACK, 2);
		settingsCmd = new Command("Settings", Command.SCREEN, 3);
		aboutCmd = new Command("About", Command.SCREEN, 4);
		
		goCmd = new Command("Go", Command.ITEM, 1);
		downloadCmd = new Command("Download", Command.ITEM, 1);
		openCmd = new Command("Open", Command.ITEM, 1);
		
		ownerCmd = new Command("Owner", Command.SCREEN, 4);
		releasesCmd = new Command("Releases", Command.SCREEN, 3);
		forksCmd = new Command("Forks", Command.SCREEN, 5);
		reposCmd = new Command("Repositories", Command.SCREEN, 5);
		
		Form f = new Form("GitHub");
		f.addCommand(exitCmd);
		f.addCommand(settingsCmd);
		f.addCommand(aboutCmd);
		
		field = new TextField("user or user/repo", "", 200, TextField.ANY);
		field.addCommand(goCmd);
		field.setItemCommandListener(this);
		field.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		f.append(field);
		
		StringItem btn = new StringItem(null, "Go", StringItem.BUTTON);
		btn.addCommand(goCmd);
		btn.setDefaultCommand(goCmd);
		btn.setItemCommandListener(this);
		btn.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		f.append(btn);
		
		display.setCurrent(mainForm = f);
	}

	public void commandAction(Command c, Displayable d) {
		if (d == mainForm) {
			if (c == goCmd) {
				String url = field.getString();
				GHForm f;
				if (url.indexOf('/') == -1) {
					f = new UserForm(url);
				} else {
					f = new RepoForm(url);
				}
				display(f);
				start(RUN_LOAD_FORM, f);
				return;
			}
		}
		if (c == ownerCmd) {
			String url = ((RepoForm) d).url;
			url = url.substring(0, url.indexOf('/'));
			
			UserForm f = null;
			// search in previous screens
			synchronized (formHistory) {
				int l = formHistory.size();
				for (int i = 0; i < l; ++i) {
					Object o = formHistory.elementAt(i);
					if (!(o instanceof UserForm) || !url.equals(((UserForm) o).user)) {
						break;
					}
					f = (UserForm) o;
				}
			}
			if (f == null) {
				f = new UserForm(url);
			}
			display(f);
			start(RUN_LOAD_FORM, f);
			return;
		}
		if (c == releasesCmd) {
			ReleasesForm f = new ReleasesForm(((RepoForm) d).url);
			display(f);
			start(RUN_LOAD_FORM, f);
			return;
		}
		if (c == reposCmd) {
			ReposForm f = new ReposForm("users/".concat(((UserForm) d).user).concat("/repos"));
			display(f);
			start(RUN_LOAD_FORM, f);
			return;
		}
		if (c == forksCmd) {
			ReposForm f = new ReposForm("repos/".concat(((RepoForm) d).url).concat("/forks"));
			display(f);
			start(RUN_LOAD_FORM, f);
			return;
		}
		if (c == backCmd) {
			if (formHistory.size() == 0) {
				display(mainForm, true);
				return;
			}
			Displayable p = null;
			synchronized (formHistory) {
				int i = formHistory.size();
				while (i-- != 0) {
					if (formHistory.elementAt(i) == d) {
						break;
					}
				}
				if (i > 0) {
					p = (Displayable) formHistory.elementAt(i - 1);
					formHistory.removeElementAt(i);
				}
			}
			display(p, true);
			return;
		}
		if (c == exitCmd) {
			notifyDestroyed();
		}
	}

	public void commandAction(Command c, Item item) {
		commandAction(c, display.getCurrent());
	}
	
	public void run() {
		int run;
		Object param;
		synchronized(this) {
			run = GH.run;
			param = GH.runParam;
			notify();
		}
//		System.out.println("run ".concat(n(run)));
		System.out.println("run " + run + " " + param);
		running++;
		switch (run) {
		case RUN_LOAD_FORM: {
			try {
				((GHForm) param).loadInternal();
			} catch (InterruptedException e) {
			} catch (InterruptedIOException e) {
			} catch (Exception e) {
				display(errorAlert(e.toString()), (Displayable) param);
				e.printStackTrace();
			}
			break;
		}
		}
		running--;
	}
	
	Thread start(int i, Object param) {
		Thread t = null;
		try {
			synchronized(this) {
				run = i;
				runParam = param;
				(t = new Thread(this)).start();
				wait();
			}
		} catch (Exception e) {}
		return t;
	}
	
	static void display(Alert a, Displayable d) {
		if (d == null) {
			display.setCurrent(a);
			return;
		}
		display.setCurrent(a, d);
	}
	
	static void display(Displayable d) {
		display(d, false);
	}

	static void display(Displayable d, boolean back) {
		if (d instanceof Alert) {
			display.setCurrent((Alert) d, mainForm);
			return;
		}
		if (d == null) {
			d = mainForm;
		}
		if (d == mainForm) {
			formHistory.removeAllElements();
		}
		Displayable p = display.getCurrent();
		display.setCurrent(d);
		if (p == null || p == d) return;
		
		if (p instanceof GHForm) {
			((GHForm) p).closed(back);
		}
		if (!back && d != mainForm) {
			formHistory.addElement(d);
		}
	}

	private static Alert errorAlert(String text) {
		Alert a = new Alert("");
		a.setType(AlertType.ERROR);
		a.setString(text);
		a.setTimeout(3000);
		return a;
	}
	
	private static Alert infoAlert(String text) {
		Alert a = new Alert("");
		a.setType(AlertType.CONFIRMATION);
		a.setString(text);
		a.setTimeout(1500);
		return a;
	}
	
	private static Alert loadingAlert() {
		Alert a = new Alert("", "Loading", null, null);
		a.setCommandListener(midlet);
		a.addCommand(Alert.DISMISS_COMMAND);
		a.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
		a.setTimeout(30000);
		return a;
	}
	
	static Object api(String url) throws IOException {
		Object res;

		HttpConnection hc = null;
		InputStream in = null;
		try {
			hc = open(proxyUrl(APIURL.concat(url)));
			hc.setRequestMethod("GET");
			hc.setRequestProperty("Accept", "application/vnd.github+json");
			hc.setRequestProperty("X-Github-Api-Version", API_VERSION);
			
			int c = hc.getResponseCode();
			res = JSONObject.parseJSON(readUtf(in = hc.openInputStream(), (int) hc.getLength()));
			if (c >= 400) {
				throw new APIException(url, c, res);
			}
		} finally {
			if (in != null) try {
				in.close();
			} catch (IOException e) {}
			if (hc != null) try {
				hc.close();
			} catch (IOException e) {}
		}
		System.out.println(res);
		return res;
	}
	
	private static String proxyUrl(String url) {
		System.out.println(url);
		if (url == null
				|| (!useProxy && url.indexOf(";tw=") == -1)
				|| proxyUrl == null || proxyUrl.length() == 0 || "https://".equals(proxyUrl)) {
			return url;
		}
		return proxyUrl + url(url);
	}
	
	private static byte[] readBytes(InputStream inputStream, int initialSize, int bufferSize, int expandSize)
			throws IOException {
		if (initialSize <= 0) initialSize = bufferSize;
		byte[] buf = new byte[initialSize];
		int count = 0;
		byte[] readBuf = new byte[bufferSize];
		int readLen;
		while ((readLen = inputStream.read(readBuf)) != -1) {
			if (count + readLen > buf.length) {
				System.arraycopy(buf, 0, buf = new byte[count + expandSize], 0, count);
			}
			System.arraycopy(readBuf, 0, buf, count, readLen);
			count += readLen;
		}
		if (buf.length == count) {
			return buf;
		}
		byte[] res = new byte[count];
		System.arraycopy(buf, 0, res, 0, count);
		return res;
	}
	
	private static String readUtf(InputStream in, int i) throws IOException {
		byte[] buf = new byte[i <= 0 ? 1024 : i];
		i = 0;
		int j;
		while ((j = in.read(buf, i, buf.length - i)) != -1) {
			if ((i += j) >= buf.length) {
				System.arraycopy(buf, 0, buf = new byte[i + 2048], 0, i);
			}
		}
		return new String(buf, 0, i, "UTF-8");
	}
	
	private static byte[] get(String url) throws IOException {
		HttpConnection hc = null;
		InputStream in = null;
		try {
			hc = open(url);
			hc.setRequestMethod("GET");
			int r;
			if ((r = hc.getResponseCode()) >= 400) {
				throw new IOException("HTTP ".concat(Integer.toString(r)));
			}
			in = hc.openInputStream();
			return readBytes(in, (int) hc.getLength(), 8*1024, 16*1024);
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException e) {}
			try {
				if (hc != null) hc.close();
			} catch (IOException e) {}
		}
	}
	
	private static HttpConnection open(String url) throws IOException {
		HttpConnection hc = (HttpConnection) Connector.open(url);
		hc.setRequestProperty("User-Agent", "j2me-client/" + version + " (https://github.com/shinovon)");
		return hc;
	}
	
	public static String url(String url) {
		StringBuffer sb = new StringBuffer();
		char[] chars = url.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			int c = chars[i];
			if (65 <= c && c <= 90) {
				sb.append((char) c);
			} else if (97 <= c && c <= 122) {
				sb.append((char) c);
			} else if (48 <= c && c <= 57) {
				sb.append((char) c);
			} else if (c == 32) {
				sb.append("%20");
			} else if (c == 45 || c == 95 || c == 46 || c == 33 || c == 126 || c == 42 || c == 39 || c == 40
					|| c == 41) {
				sb.append((char) c);
			} else if (c <= 127) {
				sb.append(hex(c));
			} else if (c <= 2047) {
				sb.append(hex(0xC0 | c >> 6));
				sb.append(hex(0x80 | c & 0x3F));
			} else {
				sb.append(hex(0xE0 | c >> 12));
				sb.append(hex(0x80 | c >> 6 & 0x3F));
				sb.append(hex(0x80 | c & 0x3F));
			}
		}
		return sb.toString();
	}

	private static String hex(int i) {
		String s = Integer.toHexString(i);
		return "%".concat(s.length() < 2 ? "0" : "").concat(s);
	}
	
	static long parseDateGMT(String date) {
		Calendar c = parseDate(date);
		return c.getTime().getTime() + c.getTimeZone().getRawOffset() - parseTimeZone(date);
	}
	
	// парсер даты ISO 8601 без учета часового пояса
	static Calendar parseDate(String date) {
		Calendar c = Calendar.getInstance();
		if (date.indexOf('T') != -1) {
			String[] timeSplit = split(date.substring(date.indexOf('T')+1), ':');
			if (timeSplit.length == 1) {
				c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeSplit[0]));
				c.set(Calendar.MINUTE, Integer.parseInt(timeSplit[0]));
			} else {
				if (timeSplit.length == 3) {
					String second = timeSplit[2];
					int i = second.indexOf('+');
					if (i == -1) {
						i = second.indexOf('-');
					}
					if (i == -1) {
						i = second.indexOf('Z');
					}
					if (i != -1) {
						second = second.substring(0, i);
					}
					i = second.indexOf('.');
					if (i != -1) {
						i = second.indexOf(',');
					}
					if (i != -1) {
						c.set(Calendar.MILLISECOND, Integer.parseInt(second.substring(i + 1)));
						second = second.substring(0, i);
					}
					c.set(Calendar.SECOND, Integer.parseInt(second));
				}
				
				c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeSplit[0]));
				c.set(Calendar.MINUTE, Integer.parseInt(timeSplit[1]));
			}
			date = date.substring(0, date.indexOf('T'));
		}
		String[] dateSplit = split(date, '-');
		if (dateSplit.length == 1) {
			c.set(Calendar.YEAR, Integer.parseInt(date.substring(0, 4)));
			c.set(Calendar.MONTH, Integer.parseInt(date.substring(4, 6))-1);
			c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(date.substring(6, 8)));
		} else {
			c.set(Calendar.YEAR, Integer.parseInt(dateSplit[0]));
			c.set(Calendar.MONTH, Integer.parseInt(dateSplit[1])-1);
			if (dateSplit.length == 3)
				c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateSplit[2]));
		}
		return c;
	}
	
	// отрезать таймзону из даты
	static String getTimeZoneStr(String date) {
		int i = date.lastIndexOf('+');
		if (i == -1)
			i = date.lastIndexOf('-');
		if (i == -1)
			return null;
		return date.substring(i);
	}

	// получение оффсета таймзоны даты в миллисекундах
	static int parseTimeZone(String date) {
		int i = date.lastIndexOf('+');
		boolean m = false;
		if (i == -1) {
			i = date.lastIndexOf('-');
			m = true;
		}
		if (i == -1)
			return 0;
		date = date.substring(i + 1);
		int offset = date.lastIndexOf(':');
		if (offset == -1) {
			if (date.length() == 4) {
				offset = (Integer.parseInt(date.substring(0, 2)) * 3600000) +
						(Integer.parseInt(date.substring(2, 4)) * 60000);
			} else {
				offset = (Integer.parseInt(date) * 3600000);
			}
		} else {
			offset = (Integer.parseInt(date.substring(0, offset)) * 3600000) +
					(Integer.parseInt(date.substring(offset + 1)) * 60000);
		}
		return m ? -offset : offset;
	}
	
	static String n(int n) {
		if (n < 10) {
			return "0".concat(Integer.toString(n));
		} else return Integer.toString(n);
	}
	
	static String[] split(String str, char d) {
		int i = str.indexOf(d);
		if (i == -1)
			return new String[] {str};
		Vector v = new Vector();
		v.addElement(str.substring(0, i));
		while (i != -1) {
			str = str.substring(i + 1);
			if ((i = str.indexOf(d)) != -1)
				v.addElement(str.substring(0, i));
			i = str.indexOf(d);
		}
		v.addElement(str);
		String[] r = new String[v.size()];
		v.copyInto(r);
		return r;
	}

	void browse(String url) {
		try {
			if (platformRequest(url)) notifyDestroyed();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void parseMarkdown(String body, Form form) {
		// TODO
		if (body.trim().length() == 0) return;
		StringItem s = new StringItem(null, body);
		s.setFont(GH.medfont);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		form.append(s);
	}

}
