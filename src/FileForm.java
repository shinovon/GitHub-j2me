import java.util.Hashtable;

import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONObject;

public class FileForm extends GHForm {

	public FileForm(String url, String title) {
		super(title);
		this.url = url;
	}

	void loadInternal(Thread thread) throws Exception {
		JSONObject r = (JSONObject) GH.api(url);
		
		String s;
		String name = r.getString("name");
		if ("base64".equals(r.getString("encoding"))) {
			int[] len = new int[1];
			s = new String(GH.decodeBase64(r.getString("content").getBytes(), len), 0, len[0], "UTF-8");
		} else {
			s = r.getString("content");
		}
		r = null;
		
		if (!name.toLowerCase().endsWith(".md")) {
			StringItem item = new StringItem(null, s);
			item.setFont(GH.smallPlainFont);
			safeAppend(thread, item);
			return;
		}
		
		if (urls == null) {
			urls = new Hashtable();
		} else {
			urls.clear();
		}
		GH.parseMarkdown(thread, this, s, 0, urls);
	}

}
