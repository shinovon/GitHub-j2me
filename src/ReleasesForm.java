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
	private Hashtable urls;
	private boolean tags;

	public ReleasesForm(String repo, boolean tags) {
		super(repo.concat(tags ? " - Tags" : " - Releases"));
		this.perPage = tags ? 30 : 10;
		this.url = repo;
		this.tags = tags;
		addCommand(tags ? GH.releasesCmd : GH.tagsCmd);
	}

	void loadInternal(Thread thread) throws Exception {
		deleteAll();
		
		JSONArray r = pagedApi(thread, "repos/".concat(url).concat(tags ? "/tags?" : "/releases?"));
		int l = r.size();
		
		if (urls == null) {
			urls = new Hashtable();
		} else {
			urls.clear();
		}
		StringItem s;
		String t;
		for (int i = 0; i < l && thread == this.thread; ++i) {
			JSONObject j = r.getObject(i);
			
			s = new StringItem(null, j.getString("name", j.getString("tag_name", "")));
			s.setFont(GH.largefont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			safeAppend(thread, s);
			
			if (j.has("commit")) {
				s = new StringItem(null, j.getObject("commit").getString("sha").substring(0, 7));
				s.setFont(GH.smallfont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
				safeAppend(thread, s);
			}
			
			if (j.has("body") && (t = j.getString("body")) != null && t.length() != 0) {
				if (t.length() < 100 || i == 0) {
					GH.parseMarkdown(thread, this, t, -1);
				} else {
					s = new StringItem(null, "Show text", Item.HYPERLINK);
					s.setFont(GH.medfont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
					s.setDefaultCommand(GH.spoilerCmd);
					s.setItemCommandListener(this);
	
					urls.put(s, new Object[] { new Integer(size()), t});
					safeAppend(thread, s);
				}
			}
			
			if (j.has("assets")) {
				JSONArray assets = j.getArray("assets");
				int l2 = assets.size();
						
				s = new StringItem(null, "Assets (" + (l2 + 1) + "):");
				s.setFont(GH.smallboldfont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
				safeAppend(thread, s);
				
				if (i == 0) {
					parseAssets(thread, assets, j.getString("zipball_url"), -1);
				} else {
					s = new StringItem(null, "Show assets", Item.HYPERLINK);
					s.setFont(GH.medfont);
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
					s.setDefaultCommand(GH.spoilerCmd);
					s.setItemCommandListener(this);
	
					urls.put(s, new Object[] { new Integer(size()), assets, j.getString("zipball_url")});
					safeAppend(thread, s);
				}
			} else {
				s = new StringItem(null, "Source code (zip)");
				s.setFont(GH.medfont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
				s.addCommand(GH.downloadCmd);
				s.setDefaultCommand(GH.downloadCmd);
				s.setItemCommandListener(this);
				urls.put(s, j.getString("zipball_url"));
				safeAppend(thread, s);
			}
			
			safeAppend(thread, "\n");
		}
	}

	private void parseAssets(Thread thread, JSONArray assets, String zipball, int i) {
		StringItem s;
		int l = assets.size();
		
		StringBuffer sb = new StringBuffer();
		for (int k = 0; k < l; ++k) {
			JSONObject asset = assets.getObject(k);
			
			sb.setLength(0);
			sb.append(asset.getString("name")).append(" (");
			int size = asset.getInt("size");
			if (size < 1024) {
				sb.append(size).append(" B");
			} else if (size < 1024*1024) {
				sb.append(((int) ((size / (1024D)) * 100)) / 100D).append(" KB");
			} else if (size < 1024*1024*1024) {
				sb.append(((int) ((size / (1024D * 1024D) * 100))) / 100D).append(" MB");
			} else {
				sb.append(((int) ((size / (1024D * 1024D * 1024D)) * 100)) / 100D).append(" GB");
			}
			sb.append(')');
			
			s = new StringItem(null, sb.toString());
			s.setFont(GH.medfont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			s.setDefaultCommand(GH.downloadCmd);
			s.setItemCommandListener(this);
			urls.put(s, asset.getString("browser_download_url"));
			
			if (i == -1) safeAppend(thread, s);
			else safeInsert(thread, i + k, s);
		}
		
		s = new StringItem(null, "Source code (zip)");
		s.setFont(GH.medfont);
		s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
		s.addCommand(GH.downloadCmd);
		s.setDefaultCommand(GH.downloadCmd);
		s.setItemCommandListener(this);
		urls.put(s, zipball);
		
		if (i == -1) safeAppend(thread, s);
		else safeInsert(thread, i + l, s);
	}

	public void commandAction(Command c, Item item) {
		if (urls == null) return;
		if (c == GH.downloadCmd) {
			String url = (String) urls.get(item);
			GH.midlet.browse(url);
			return;
		}
		if (c == GH.spoilerCmd) {
			Object[] data = (Object[]) urls.get(item);
			if (data == null) return;
			
			int i = ((Integer) data[0]).intValue();
			int l = size();
			do {
				if (get(i) == item) break;
			} while (++i < l);
			if (i == l) return;
			
			super.delete(i);
			urls.remove(item);
			
			if (data.length == 2) {
				GH.parseMarkdown(null, this, (String) data[1], i);
				return;
			}
			
			try {
				parseAssets(null, (JSONArray) data[1], (String) data[2], i);
			} catch (Exception ignored) {}
			return;
		}
	}

	void toggleMode() {
		cancel();
		
		tags = !tags;
		perPage = tags ? 30 : 10;
		setTitle(url.concat(tags ? " - Tags" : " - Releases"));

		GH.midlet.start(GH.RUN_LOAD_FORM, this);
	}

}
