import java.io.IOException;

import javax.microedition.lcdui.Form;

public abstract class GHForm extends Form {

	public GHForm(String title) {
		super(title);
		addCommand(GH.backCmd);
		setCommandListener(GH.midlet);
	}
	
	abstract void load() throws IOException;

}
