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

	void loadInternal() throws Exception {
		deleteAll();
		StringBuffer sb = new StringBuffer("repos/");
		sb.append(url).append("/releases?per_page").append(perPage).append("&page=").append(page);
		
		JSONArray r = (JSONArray) GH.api(sb.toString());
		int l = r.size();
		
		urls = new Hashtable();
		sb.setLength(0);
		StringItem s;
		for (int i = 0; i < l; ++i) {
			JSONObject j = r.getObject(i);
			
			s = new StringItem(null, j.getString("name", j.getString("tag_name")));
			s.setFont(GH.largefont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			append(s);
			
			GH.parseMarkdown(j.getString("body"), this);
			

			JSONArray assets = j.getArray("assets");
			int l2 = assets.size();
					
			s = new StringItem(null, "Assets (" + (l2 + 1) + ")");
			s.setFont(GH.smallboldfont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			append(s);
			
			for (int k = 0; k < l2; ++k) {
				JSONObject asset = assets.getObject(k);
				
				s = new StringItem(null, asset.getString("name") + " (" + asset.getString("size") + " bytes)");
				s.setFont(GH.medfont);
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
				s.addCommand(GH.downloadCmd);
				s.setDefaultCommand(GH.downloadCmd);
				s.setItemCommandListener(this);
				urls.put(s, asset.getString("browser_download_url"));
				append(s);
			}
			
			s = new StringItem(null, "Source code (zip)");
			s.setFont(GH.medfont);
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE);
			s.addCommand(GH.downloadCmd);
			s.setDefaultCommand(GH.downloadCmd);
			s.setItemCommandListener(this);
			urls.put(s, j.getString("zipball_url"));
			append(s);
			
			append("\n\n");
		}
	}

	public void commandAction(Command c, Item item) {
		if (urls == null) return;
		String url = (String) urls.get(item);
		GH.midlet.browse(url);
	}

}
