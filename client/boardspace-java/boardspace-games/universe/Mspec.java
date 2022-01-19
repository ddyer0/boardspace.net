package universe;

import lib.OStack;

class MspecStack extends OStack<Mspec>
{
	public Mspec[] newComponentArray(int n) { return(new Mspec[n]); }
}
// this is a minimalist move specifier used in the first pass of the polysolver move generator
public class Mspec {
	UniverseCell c; 
	UniverseChip ch;
	Mspec(UniverseCell cc,UniverseChip chh) { c = cc; ch = chh; }
}
