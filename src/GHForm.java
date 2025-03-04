/*
Copyright (c) 2025 Arman Jussupgaliyev

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
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
		loaded = false;
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
	
	void safeInsert(Thread thread, int n, Item item) {
		if (thread != this.thread) throw GH.cancelException;
		insert(n, item);
	}
	
	abstract void loadInternal(Thread thread) throws Exception;
	
	void closed(boolean destroy) {
		if (destroy) cancel();
	}

}
