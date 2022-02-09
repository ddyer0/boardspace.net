import javax.swing.JOptionPane;

public class Speed
{
    public static long Date()
    {	return System.currentTimeMillis();
    }
    public static long fact(int n)
    {
        return n==0 ? 1 : n*fact(n-1);
    }
    
    public static double cpuTest()
    {	long now = Date();
        	for(int j=0;j<1000000;j++) { fact(20); }
    	long later = Date();
       	return(28.80/(later-now+1));	// 1.0 based on the codename1 simulator running on my machine 1/2016
    } 
    public static void main(String args[])
    {
    	JOptionPane.showMessageDialog(null, "Speed is "+cpuTest(), "Speed test", JOptionPane.INFORMATION_MESSAGE);
    }
}
