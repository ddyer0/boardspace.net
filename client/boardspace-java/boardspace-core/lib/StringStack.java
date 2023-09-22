/*
	Copyright 2006-2023 by Dave Dyer

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
package lib;
/**
 * an {@link OStack} of Strings
 * @author Ddyer
 *
 */
public class StringStack extends OStack<String>  implements Digestable
{ public String[]newComponentArray(int n) 
	{ return(new String[n]); 
	}
  public boolean eq(String o,String x) { return((o==null) ? x==o : o.equals(x)); }
 
  public static long Digest(Random r,String str)
  {
	  long v = str==null ? 0 : G.hashChecksum(str,str.length());
	  return v*r.nextLong();
  }

  public long Digest(Random r) 
  {
	  int val = 0;
	  for(int i=0;i<size();i++)
	  {
		  val ^= 0x246467*(i+1)*Digest(r,elementAt(i));
	  }
	  return val;
  }

}
