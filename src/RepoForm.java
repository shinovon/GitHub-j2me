import cc.nnproject.json.JSONObject;

public class RepoForm extends GHForm {

	String url;

	public RepoForm(String url) {
		super(url);
		this.url = url;
		addCommand(GH.releasesCmd);
		addCommand(GH.ownerCmd);
		addCommand(GH.forksCmd);
	}

	void loadInternal() throws Exception {
		// TODO
//		JSONObject r = (JSONObject) GH.api("repos/".concat(url));
//		append(r.toString());
		append("TODO");
	}

}
