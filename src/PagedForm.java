import java.io.IOException;

import cc.nnproject.json.JSONArray;

public abstract class PagedForm extends GHForm {
	
	int perPage = 30;
	int page = 1;
	private boolean more;
	private String title;
	private String pageText;

	public PagedForm(String title) {
		super(title);
		this.title = title;
		pageText = "";
		addCommand(GH.gotoPageCmd);
	}
	
	void nextPage() {
		if (!more) return;
		gotoPage(page + 1);
	}
	
	void prevPage() {
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
		super.setTitle(title.concat(pageText));
		this.title = title;
	}
	
	// wrapper for pagination
	JSONArray pagedApi(Thread thread, String url) throws IOException {
		StringBuffer sb = new StringBuffer(url);
		if (sb.charAt(url.length() - 1) != '?') {
			sb.append('&');
		}
		sb.append("per_page=").append(perPage)
		.append("&page=").append(page);
		
		String[] s = new String[1];
		JSONArray r = (JSONArray) GH.api(sb.toString(), s);

// <https://api.github.com/user/43963888/repos?page=2>; rel="next", <https://api.github.com/user/43963888/repos?page=2>; rel="last"
// <https://api.github.com/user/43963888/repos?page=1>; rel="prev", <https://api.github.com/user/43963888/repos?page=1>; rel="first"
		int last = page;
		try {
			if (s[0] != null) {
				System.out.println(s[0]);
				if (more = s[0].indexOf("rel=\"next\"") != -1) {
					s = GH.split(s[0], ',');
					for (int i = 0; i < s.length; ++i) {
						if (s[i].indexOf("rel=\"last\"") != -1) {
							int k = s[i].indexOf("&page=");
							last = Integer.parseInt(s[i].substring(k + 6, s[i].indexOf('>', k + 6)));
							break;
						}
					}
				}
			}
		} catch (Exception ignored) {}
		
		if (thread != this.thread) throw GH.cancelException;
		
		sb.setLength(0);
		sb.append(" (").append(page).append('/').append(last).append(')');
		
		super.setTitle(title.concat(pageText = sb.toString()));
		
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
		}
		
		return r;
	}
	

}
