import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
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
		
		setTicker(new Ticker("Loading.."));
		try {
			loadInternal(thread = Thread.currentThread());
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
		thread = null;
	}
	
	// for use with PagedForm
	void safeAppend(Thread thread, Item item) {
		if (thread != this.thread) throw GH.cancelException;
		append(item);
	}
	
	void safeAppend(Thread thread, String item) {
		if (thread != this.thread) throw GH.cancelException;
		append(item);
	}
	
	abstract void loadInternal(Thread thread) throws Exception;
	
	void closed(boolean destroy) {
		if (destroy) cancel();
	}

}
