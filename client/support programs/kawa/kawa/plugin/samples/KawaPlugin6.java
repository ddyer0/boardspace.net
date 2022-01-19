import java.util.*;
import com.tektools.kawa.plugin.*;

/** This command enumerates all the files in each project */
public class KawaPlugin6
{
	public static void main(String[] args)
	{
		System.out.println("Arrived...");
		for (int i=0;i<args.length;i++)
			System.out.println("Arg["+i+"] - "+args[i]);
	}
}