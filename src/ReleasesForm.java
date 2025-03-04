import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class ReleasesForm extends PagedForm implements ItemCommandListener {

	String url;

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
		
		sb.setLength(0);
		StringItem s;
		for (int i = 0; i < l; ++i) {
			JSONObject j = r.getObject(i);
			
			s = new StringItem(null, j.getString("name"));
		}
	}

	public void commandAction(Command c, Item item) {
		
	}

}
