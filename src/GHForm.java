import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Ticker;

public abstract class GHForm extends Form {
	
	boolean loaded, finished, canceled;
	Thread thread;

	public GHForm(String title) {
		super(title);
		addCommand(GH.backCmd);
		setCommandListener(GH.midlet);
	}
	
	void load() throws Exception {
		if (loaded) return;
		loaded = true;
		canceled = finished = false;
		
		thread = Thread.currentThread();
		setTicker(new Ticker("Loading"));
		try {
			loadInternal();
			finished = true;
		} finally {
			setTicker(null);
			thread = null;
		}
	}
	
	void cancel() {
		if (finished || thread == null) return;
		canceled = true;
		thread.interrupt();
	}
	
	abstract void loadInternal() throws Exception;
	
	void closed(boolean destroy) {}

}
