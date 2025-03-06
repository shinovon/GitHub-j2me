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
import java.util.Hashtable;

import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public abstract class DiscussionsForm extends PagedForm implements ItemCommandListener {
	
	String state = "open";
	Hashtable urls;

	public DiscussionsForm(String title) {
		super(title);
	}

	void loadInternal(Thread thread) throws Exception {
		deleteAll();
		
		JSONArray r = request();
		int l = r.size();

		if ("open".equals(state)) {
			removeCommand(GH.showOpenCmd);
			addCommand(GH.showClosedCmd);
			addCommand(GH.showAllCmd);
		} else if ("all".equals(state)) {
			removeCommand(GH.showAllCmd);
			addCommand(GH.showOpenCmd);
			addCommand(GH.showClosedCmd);
		} else {
			removeCommand(GH.showClosedCmd);
			addCommand(GH.showOpenCmd);
			addCommand(GH.showAllCmd);
		}

		StringBuffer sb = new StringBuffer();
		if (urls == null) {
			urls = new Hashtable();
		} else {
			urls.clear();
		}
		StringItem s;
		String t;
		for (int i = 0; i < l && thread == this.thread; ++i) {
			JSONObject j = r.getObject(i);
			
			s = new StringItem(null, j.getString("title"));
			s.setFont(GH.medfont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
			s.setDefaultCommand(GH.openCmd);
			
			urls.put(s, t = j.getString("number"));
			safeAppend(thread, s);

			sb.setLength(0);
			sb.append('#').append(t);
			
			t = j.getObject("user").getString("login");
			if ("open".equals(j.getString("state"))) {
				sb.append(" opened ").append(GH.localizeDate(j.getString("created_at"), 1))
				.append(" by ").append(t);
			} else {
				sb.append(" by ").append(t);
				if ((t = j.getString("merged_at", null)) != null) {
					sb.append(" was merged ").append(GH.localizeDate(t, 1));
				} else {
					sb.append(" was closed ").append(GH.localizeDate(j.getString("closed_at"), 1));
				}
			}
			
			s = new StringItem(null, sb.toString());
			s.setFont(GH.smallfont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
			safeAppend(thread, s);
		}
	}

	abstract JSONArray request() throws Exception;

}
