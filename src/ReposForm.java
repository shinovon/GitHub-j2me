import cc.nnproject.json.JSONArray;

// список репозиториев (форки, т.д)
public class ReposForm extends PagedForm {

	String url;

	public ReposForm(String url) {
		super("Repos " + url);
		this.url = url;
	}

	void loadInternal() throws Exception {
		// TODO
		JSONArray r = (JSONArray) GH.api(url);
		append(r.toString());
	}

}
