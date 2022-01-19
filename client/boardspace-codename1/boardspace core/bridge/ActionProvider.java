package bridge;
import com.codename1.ui.events.ActionListener;

//
// identifies classes that accept codename1 action events.
//
public interface ActionProvider {
	public void addActionListener(ActionListener<?> m);
	public void removeActionListener(ActionListener<?>m);
}
