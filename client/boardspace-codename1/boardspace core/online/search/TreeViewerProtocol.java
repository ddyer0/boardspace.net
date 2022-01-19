package online.search;

/**
 * this protocol pairs a viewing window with a source of a search tree
 * 
 * @author Ddyer
 *
 */
public interface TreeViewerProtocol {
	public void setTree(TreeProviderProtocol driver); 
}
