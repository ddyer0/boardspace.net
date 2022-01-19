package bridge;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import lib.G;

public class Util {

	public static void writeObject(Object value, DataOutputStream out) {
		G.Error("only implemented by codename1 branch");
		
	}

	public static Object readObject(DataInputStream in) {
		throw G.Error("only implemented by codename1 branch");
	}

	public static void writeUTF(String picture, DataOutputStream out) {
		G.Error("only implemented by codename1 branch");
		
	}

	public static String readUTF(DataInputStream in) {
		throw G.Error("only implemented by codename1 branch");
	}

	public static void register(String string, Class<?> class1) {
		// only necessary in Codename1, safe as a no-op here.
	}

}
