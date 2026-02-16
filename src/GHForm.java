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
import java.io.InterruptedIOException;
import java.util.Hashtable;

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.Ticker;

public class GHForm extends Form implements LangConstants {

	String url;
	Hashtable urls;

	boolean loaded, finished, canceled;
	Thread thread;

	public GHForm(String title) {
		super(title);
		addCommand(GH.backCmd);
		addCommand(GH.homeCmd);
		setCommandListener(GH.midlet);
	}

	void load() {
		if (loaded) return;
		loaded = true;
		canceled = finished = false;

		Ticker ticker;
		setTicker(ticker = new Ticker(GH.L[LLoading]));
		if (GH.useLoadingForm) {
			GH.display(GH.loadingForm);
		}
		Thread thread = this.thread = Thread.currentThread();
		try {
			deleteAll();
			
			loadInternal(thread);
			finished = true;
			if (thread != this.thread) return;
			if (GH.useLoadingForm && GH.current == this) {
				GH.display(this);
			}
		} catch (InterruptedException ignored) {
		} catch (InterruptedIOException ignored) {
		} catch (Exception e) {
			if (e == GH.cancelException || canceled || this.thread != thread) {
				// ignore exception if cancel detected
				return;
			}
			GH.display(GH.errorAlert(e.toString()), this);
			e.printStackTrace();
		} finally {
			if (getTicker() == ticker) {
				setTicker(null);
			}
			
			if (this.thread == thread) {
				this.thread = null;
			}
		}
	}

	void cancel() {
		loaded = false;
		if (finished || thread == null) return;
		canceled = true;
		thread.interrupt();
		thread = null;
	}

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

	/* abstract */ void loadInternal(Thread thread) throws Exception {
		GH.parseMarkdown(thread, null, url, 0, urls);
	}

	void closed(boolean destroy) {
		if (destroy) cancel();
	}

}
