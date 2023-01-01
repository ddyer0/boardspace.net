package online.game;

import java.io.PrintStream;
import lib.G;
import lib.OStack;
import lib.StackIterator;
import online.game.AnnotationMenu.Annotation;
import online.game.sgf.export.sgf_names;

class MoveAnnotationStack  extends OStack<MoveAnnotation>
{
	public MoveAnnotation[] newComponentArray(int sz) {
		return new MoveAnnotation[sz];
	}
}
public class MoveAnnotation implements StackIterator<MoveAnnotation> , sgf_names
{
		Annotation annotation;
		double xPos;
		double yPos;
		
		public MoveAnnotation(Annotation an, double dx, double dy) {
			annotation = an;
			xPos = dx;
			yPos = dy;
		}
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
		{	return(G.concat("(A1 ",annotation.name()," ",G.format("%6D",xPos)," ",G.format("%6D",yPos)," )"));
		}
}
