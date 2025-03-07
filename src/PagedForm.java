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
import java.io.IOException;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

// base class for pagination
public abstract class PagedForm extends GHForm {
	
	int perPage = 30;
	int page = 1;
	private boolean more;
	private String title;
	String pageText = "";

	public PagedForm(String title) {
		super(title);
		this.title = title;
	}
	
	void changePage(boolean next) {
		if (next) {
			if (!more) return;
			gotoPage(page + 1);
			return;
		}
		gotoPage(page <= 1 ? 1 : page - 1);
	}
	
	void gotoPage(int n) {
		page = n;
		loaded = false;
		
		cancel();
		GH.midlet.start(GH.RUN_LOAD_FORM, this);
	}
	
	// overriden from Form
	public void setTitle(String title) {
		super.setTitle(title == null ? null : title.concat(pageText == null ? "" : pageText));
		this.title = title;
	}
	
	// wrapped api request
	JSONArray pagedApi(Thread thread, String url) throws IOException {
		StringBuffer sb = new StringBuffer(url);
		if (sb.charAt(url.length() - 1) != '?') {
			sb.append('&');
		}
		sb.append(GH.apiMode == GH.API_GITEA ? "limit=" : "per_page=").append(perPage)
		.append("&page=").append(page);
		
		String[] s = new String[1];
		Object r = GH.api(sb.toString(), s);
		
		if (r instanceof JSONObject) {
			r = ((JSONObject) r).getArray(GH.apiMode == GH.API_GITEA ? "data" : "items");
		}

// <https://api.github.com/user/43963888/repos?page=2>; rel="next", <https://api.github.com/user/43963888/repos?page=2>; rel="last"
// <https://api.github.com/user/43963888/repos?page=1>; rel="prev", <https://api.github.com/user/43963888/repos?page=1>; rel="first"
		int last = page;
		try {
			if (s[0] != null) {
				if (more = s[0].indexOf("rel=\"next\"") != -1) {
					s = GH.split(s[0], ',');
					for (int i = 0; i < s.length; ++i) {
						if (s[i].indexOf("rel=\"last\"") != -1) {
							int k = s[i].indexOf("&page=");
							int m = s[i].indexOf('&', k + 6);
							last = Integer.parseInt(s[i].substring(k + 6, m != -1 ? m : s[i].indexOf('>', k + 6)));
							break;
						}
					}
				}
			}
		} catch (Exception ignored) {}
		
		if (thread != this.thread) throw GH.cancelException;
		
		// add page number to title
		sb.setLength(0);
		if (!(page == 1 && page == last)) {
			sb.append(" (").append(page).append('/').append(last).append(')');
		}
		
		super.setTitle(title.concat(pageText = sb.toString()));
		
		// pagination commands
		if (page != 1) {
			addCommand(GH.prevPageCmd);
		} else {
			removeCommand(GH.prevPageCmd);
		}
		if (more) {
			addCommand(GH.nextPageCmd);
		} else {
			removeCommand(GH.nextPageCmd);
		}
		
		if (page == 1 && !more) {
			removeCommand(GH.gotoPageCmd);
		} else {
			addCommand(GH.gotoPageCmd);
		}
		
		return (JSONArray) r;
	}
	

}
