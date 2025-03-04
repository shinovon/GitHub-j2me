import cc.nnproject.json.JSONObject;

public class UserForm extends GHForm {

	String user;

	public UserForm(String user) {
		super(user);
		this.user = user;
		addCommand(GH.reposCmd);
	}

	void loadInternal() throws Exception {
		// TODO
		JSONObject r = (JSONObject) GH.api("users/".concat(user));
		append(r.toString());
	}

}
