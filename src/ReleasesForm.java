import java.io.IOException;

import cc.nnproject.json.JSONArray;

public class ReleasesForm extends GHForm {

	private String url;

	public ReleasesForm(String url) {
		super(url + " - Releases");
		this.url = url;
	}

	void load() throws IOException {
		JSONArray r = (JSONArray) GH.api("repos/".concat(url).concat("/releases"));
		append(r.toString());
	}

}
