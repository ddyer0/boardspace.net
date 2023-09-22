import javax.swing.JOptionPane;
/*
This file is part of the Boardspace project.

Boardspace is free software: you can redistribute it and/or modify it under the terms of 
the GNU General Public License as published by the Free Software Foundation, 
either version 3 of the License, or (at your option) any later version.

Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Boardspace.
If not, see https://www.gnu.org/licenses/. 
*/
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
