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
import javax.microedition.lcdui.Spacer;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class IssuesForm extends PagedForm implements ItemCommandListener {
	
	String state;
	int mode;

	// 0 - issues, 1 - pulls, 2 - search issues and pulls
	public IssuesForm(String url, int mode) {
		super(mode == 2 ? GH.L[Search] : GH.L[mode == 1 ? Pulls : Issues].concat(" - ").concat(url));
		this.url = url;
		this.mode = mode;
		if (mode != 2) addCommand(GH.saveBookmarkCmd);
		if (mode == 0 && GH.login != null) addCommand(GH.createIssueCmd);
	}

	void loadInternal(Thread thread) throws Exception {
		StringBuffer sb = new StringBuffer(
				mode == 2 || (mode == 0 && GH.apiMode != GH.API_GITEA) ?
						(GH.apiMode == GH.API_GITEA ? "repos/issues/search?" : "search/issues?") : "repos/");
		if (mode == 2) {
			GH.appendUrl(sb.append("q="), url);
		} else if (mode == 0 && GH.apiMode != GH.API_GITEA) {
			GH.appendUrl(sb.append("q="), "is:issue repo:".concat(url));
			if (!"all".equals(state)) {
				GH.appendUrl(sb, " is:").append("closed".equals(state) ? "closed" : "open");
			}
		} else {
			sb.append(url).append(mode == 1 ? "/pulls?" : "/issues?");
			if (GH.apiMode == GH.API_GITEA) sb.append("type=issues");
			if (state != null) sb.append("state=").append(state);
		}
		
		JSONArray r = pagedApi(thread, sb);
		int l = r.size();

		if (mode != 2 || GH.apiMode == GH.API_GITEA) {
			if ("closed".equals(state)) {
				removeCommand(GH.showClosedCmd);
				addCommand(GH.showOpenCmd);
				addCommand(GH.showAllCmd);
			} else if ("all".equals(state)) {
				removeCommand(GH.showAllCmd);
				addCommand(GH.showOpenCmd);
				addCommand(GH.showClosedCmd);
			} else {
				removeCommand(GH.showOpenCmd);
				addCommand(GH.showClosedCmd);
				addCommand(GH.showAllCmd);
			}
		}

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
			s.setFont(GH.medPlainFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE);
			s.setDefaultCommand(GH.openCmd);
			s.setItemCommandListener(this);
			safeAppend(thread, s);
			
			t = j.getString("number");
			urls.put(s, mode == 2 ? j.getString("url") : t);

			sb.setLength(0);
			sb.append('#').append(t);
			
			t = j.getObject("user").getString("login");
			if ("open".equals(j.getString("state"))) {
				sb.append(GH.L[_opened]).append(GH.localizeDate(j.getString("created_at"), 1))
				.append(GH.L[_by]).append(t);
			} else {
				sb.append(GH.L[_by]).append(t);
				if ((t = j.getString("merged_at", null)) != null) {
					sb.append(GH.L[_was_merged]).append(GH.localizeDate(t, 1));
				} else {
					sb.append(GH.L[_was_closed]).append(GH.localizeDate(j.getString("closed_at"), 1));
				}
			}
			
			s = new StringItem(null, sb.toString());
			s.setFont(GH.smallPlainFont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
			safeAppend(thread, s);
			safeAppend(thread, new Spacer(10, 8));
		}
	}

	public void commandAction(Command c, Item item) {
		if (urls == null) return;
		String s = (String) urls.get(item);
		GH.openUrl(mode == 2 ? s : url.concat(mode == 1 ? "/pull/" : "/issues/").concat(s));
	}

}
