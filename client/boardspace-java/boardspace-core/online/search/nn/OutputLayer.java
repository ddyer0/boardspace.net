package online.search.nn;

public class OutputLayer extends FCLayer 
{
	public OutputLayer(String name, int size, TransferFunction f) {
		super(name, size, f);
	}

	public boolean isOutputLayer() { return(true); }
}
