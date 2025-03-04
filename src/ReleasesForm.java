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

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class ReleasesForm extends PagedForm implements ItemCommandListener {

	String url;
	Hashtable urls;

	public ReleasesForm(String url) {
		super(url + " - Releases");
		this.url = url;
	}

	void loadInternal(Thread thread) throws Exception {
		deleteAll();
		
		JSONArray r = pagedApi(thread, "repos/".concat(url).concat("/releases?"));
		int l = r.size();
		
		if (urls == null) {
			urls = new Hashtable();
		} else {
			urls.clear();
		}
		StringItem s;
		for (int i = 0; i < l && thread == this.thread; ++i) {
			JSONObject j = r.getObject(i);
			
			s = new StringItem(null, j.getString("name", j.getString("tag_name")));
			s.setFont(GH.largefont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			safeAppend(thread, s);
			
			GH.parseMarkdown(j.getString("body"), this);
			

			JSONArray assets = j.getArray("assets");
			int l2 = assets.size();
					
			s = new StringItem(null, "Assets (" + (l2 + 1) + "):");
			s.setFont(GH.smallboldfont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			safeAppend(thread, s);
			
			for (int k = 0; k < l2; ++k) {
				JSONObject asset = assets.getObject(k);
				
				s = new StringItem(null, asset.getString("name") + " (" + asset.getString("size") + " bytes)");
				s.setFont(GH.medfont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
				s.addCommand(GH.downloadCmd);
				s.setDefaultCommand(GH.downloadCmd);
				s.setItemCommandListener(this);
				urls.put(s, asset.getString("browser_download_url"));
				safeAppend(thread, s);
			}
			
			s = new StringItem(null, "Source code (zip)");
			s.setFont(GH.medfont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			s.addCommand(GH.downloadCmd);
			s.setDefaultCommand(GH.downloadCmd);
			s.setItemCommandListener(this);
			urls.put(s, j.getString("zipball_url"));
			safeAppend(thread, s);
			
			safeAppend(thread, "\n\n");
		}
	}

	public void commandAction(Command c, Item item) {
		if (urls == null) return;
		String url = (String) urls.get(item);
		GH.midlet.browse(url);
	}

}
