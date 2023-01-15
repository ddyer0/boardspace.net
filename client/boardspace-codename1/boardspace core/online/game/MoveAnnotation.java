package online.game;

import java.io.PrintStream;
import lib.G;
import lib.OStack;
import lib.StackIterator;
import lib.Tokenizer;
import online.game.AnnotationMenu.Annotation;
import online.game.sgf.export.sgf_names;
/**
 * this is the object type stored on commonMove 
 * 
 * @author ddyer
 *
 */
class MoveAnnotationStack  extends OStack<MoveAnnotation>
{
	public MoveAnnotation[] newComponentArray(int sz) {
		return new MoveAnnotation[sz];
	}
}
public class MoveAnnotation implements StackIterator<MoveAnnotation> , sgf_names
{
		Annotation annotation;
		int xPos;
		int yPos;
		String zone;
		public MoveAnnotation(Annotation an, String z, int dx, int dy) {
			annotation = an;
			zone = z;
			xPos = dx;
			yPos = dy;
		}
		public String toString() { return "<MoveAnnotation "+annotation+" "+zone+" "+xPos + " "+yPos +">"; }
		// hooks for StackIterator
		public int size() {
			return 1;
		}	
		public MoveAnnotation elementAt(int i)
		{	if(i==0) { return(this); }
			return(null);
		}
		public StackIterator<MoveAnnotation> push(MoveAnnotation m)
		{	StackIterator<MoveAnnotation> news = new MoveAnnotationStack();
			news.push(this);
			news.push(m);
			return(news);
		}
		public StackIterator<MoveAnnotation>insertElementAt(MoveAnnotation m,int at)
		{
			StackIterator<MoveAnnotation> news = new MoveAnnotationStack();
			news.push(this);
			return news.insertElementAt(m,at);
		}
		public StackIterator<MoveAnnotation> remove(int n)
		{	if(n==0) { return(null); }
			return(this);
		}
		public StackIterator<MoveAnnotation> remove(MoveAnnotation item)
		{	if(item==this) { return(null); }
			return(this);
		}
		public static void printAnnotations(PrintStream ps, commonMove m) {
			StackIterator<MoveAnnotation> annotations = m.getAnnotations();
			if(annotations!=null)
			{	ps.print(annotation_property);
				ps.print("[");
				for(int sz = annotations.size(),i=0; i<sz; i++)
				{
					MoveAnnotation annotation = annotations.elementAt(i);
					ps.print(annotation.toReadableString());
					ps.print(" ");
				}
				ps.print("]");
			}
		}
		public String toReadableString()
		{	return(G.concat("(",AnnotationMenu.ANNOTATION_TAG," ",annotation.name()," ",zone," ",xPos," ",yPos,")"));
		}
		public static String toReadableString(StackIterator<MoveAnnotation>an)
		{
			if(an!=null)
			{	StringBuilder b = new StringBuilder();
				for(int i=0,len=an.size(); i<len; i++)
				{	b.append(an.elementAt(i).toReadableString());
				}
				return b.toString();
			}
			return null;
		}
		public static StackIterator<MoveAnnotation> fromReadableString(String str)
		{	StackIterator<MoveAnnotation>val = null;
			Tokenizer s = new lib.Tokenizer(str);
			while(s.hasMoreElements() && "(".equals(s.nextElement()))
			{	
				String key = s.nextElement();
				if("A1".equals(key))
				{	Annotation ann = Annotation.valueOf(s.nextElement());
					String zone = s.nextElement();
					int xpos = s.intToken();
					int ypos = s.intToken();
					
					MoveAnnotation aa = new MoveAnnotation(ann,zone,xpos,ypos);
					G.Assert(")".equals(s.nextElement()),"no end ) after %s in %s",aa,str);
					if(val==null) { val= aa; } else { val = val.push(aa);}				
					}
				else { G.Error("Unexpected token %s in %s",key,str); }				}

			return val;
		}
}
