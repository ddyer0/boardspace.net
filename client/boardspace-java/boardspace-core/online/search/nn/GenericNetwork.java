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
package online.search.nn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import lib.G;
import lib.IStack;
import lib.Random;

public class GenericNetwork implements Network
{	public String mainTransferFunction = "SIGMOID";
	public String loadedFromFile = "raw network";
	public String name = "Network";
	public String getName() { return(name); }
	public String getInfo() { return(name+" loaded from "+loadedFromFile);}
	public double learningRate = 0.1;
	public int VERSION = 2;
	public int trainingSteps = 0;
	public double trainingError = 0;
	public String options="";
	public double defaultBias = 0.0;
	public double defaultScale = 1.0;
	public int[] layerSpecs = null;
	public String layerOptions[] = null;
	public CoordinateMap coordinateMap = null;
	public CoordinateMap getCoordinateMap() { return(coordinateMap); }
	public void setCoordinateMap(CoordinateMap map) { coordinateMap = map; }
	public double getLearningRate() { return(learningRate); }
	Layer layers[];
	public Layer[] getLayers() {return(layers); }
	public Neuron[] getOutputNeurons() { return(getOutputNeurons(getLayer("OUT"))); }
	public Neuron[] getOutputNeurons(Layer layer) 
		{ 	G.Assert(layer.isOutputLayer(),"should be an output layer");
			return(layer.getNeurons()) ;
		}
	
	public double[] getValues() 
	{	return(getValues("OUT"));
	}
	
	public double[] getValues(String layerName)
	{	Layer out = getLayer(layerName);
		return(getValues(out));
	}
	public double[] getValues(Layer out)
	{	G.Assert(out.isOutputLayer(),"should be an output layer");
		double val[] = out.getValues();
		return(val);
	}
	public double getTotalValues()
	{
		return(getTotalValues("OUT"));
	}
	public double getTotalValues(String layer)
	{
		return(getTotalValues(getLayer(layer)));
	}
	public double getTotalValues(Layer layer) { return(layer.getTotalValues()); }
	public void setFromFile(String file) { loadedFromFile = file; }
	public static Network loadNetwork(String file) 
	{
		InputStream s=null;
		Network net = null;
		try {
			s = new FileInputStream(new File(file));
			if(s!=null)
				{BufferedReader ps = new BufferedReader(new InputStreamReader(s));
				net = readNetwork(ps);
				net.setFromFile(file);
				s.close();
				}
		} catch (IOException e) {
			G.Error("Load network error %s",e);
		}
		
		return(net);
	}
	public double testNetwork()
	{
		int n = getInputNeurons().length;
		double in[] = new double[n];
		double values0[] = calculateValues(0,in);
		double sum0 = 0;
		for(double v : values0) { sum0 += v; }
		
		double sum1 = 0;
		for(int i=0;i<n;i++) { in[i]=((i&1)==0)?1:-1; }
		double values1[] = calculateValues(0,in);
		for(double v : values1) { sum1 += v; }
		
		double sum2 = 0;
		for(int i=0;i<n;i++) { in[i]=((i&2)==0)?0:((i&4)==0)?-1:1; }
		double values2[] = calculateValues(0,in);
		for(double v : values2) { sum2 += v; }
		
		double finalv = Math.abs(sum0-sum1) + Math.abs(sum0-sum2) + Math.abs(sum1-sum2);
		G.print("net "+finalv+" "+sum0+" "+sum1+" "+sum2);
		
		return(finalv);
		
	}
	static String nextToken(StringTokenizer s)
	{
		return(G.nextToken(s).toUpperCase());
	}
	static StringTokenizer stringTokenizer(String s)
	{
		return new StringTokenizer(s.trim(),"( )",true);
	}
	public static Network readNetwork(BufferedReader s) throws IOException
	{	
		String banner = s.readLine();
		StringTokenizer bannerTok = stringTokenizer(banner);
		@SuppressWarnings("unused")
		String className = nextToken(bannerTok);
		int vers = G.IntToken(bannerTok);
		String stats = vers>=2 ? G.trimQuotes(s.readLine()) : "";
		String options = G.trimQuotes(s.readLine());
		String layerSpecs = s.readLine();
		StringTokenizer layerTok = stringTokenizer(layerSpecs);
		IStack layerStack = new IStack();
		while(layerTok.hasMoreTokens()) { layerStack.push(G.IntToken(layerTok)); }
		int layers[] = layerStack.toArray();
		
		GenericNetwork n = new GenericNetwork(options,layers);
		n.parseOptions(stats);
		Hashtable<String,Weight> madeConnections = new Hashtable<String,Weight>();
		Hashtable<String,Weight> connections = n.getWeights();
		String newLine = null;
		while( (newLine = s.readLine()) !=null )
		{	
			newLine = newLine.trim();
			if((newLine.length()>0) && (!(newLine.charAt(0)=='/')))
			{	
				StringTokenizer tok = stringTokenizer(newLine);
				while(tok.hasMoreTokens())
				{
				String from = nextToken(tok);
				String to = nextToken(tok);
				double val = G.DoubleToken(tok);
				String key = from+" "+to;
				Weight conn = connections.get(key);
				if(conn!=null)
				{
					conn.setWeight(val);
					connections.remove(key);
					madeConnections.put(key,conn);
				}
				else {
					if(madeConnections.get(key)!=null)
					{
						G.Error("connection %s already initialized, new value is %s",key,val);
					}
					else {
					G.Error("connection %s not found",key);
					}
				}}
				
			}}
			
		if(connections.size()>0)
		{	G.print(""+connections.size()+" were not initialized");
			Enumeration<String> key = connections.keys();
			while(key.hasMoreElements()) { G.print(key.nextElement()); }				
			
		}
		
		return(n);
	}
	
	@SuppressWarnings("deprecation")
	public void saveNetwork(PrintStream out,String comment)
	{
		out.print(this.getClass().getName());
		out.print(" ");
		out.println(VERSION);
		out.println("\"STEPS "+trainingSteps+" ERROR "+trainingError+"\"");
		out.print("\"");
		out.print(options);
		out.println("\"");
		for(int l : layerSpecs) { out.print(" "); out.print(l); }
		out.println();
		out.print("// ");
		out.println(comment);
		printNetworkWeights(out);
		out.println();
	}
	
	// get all the weights in the network as a hash table.  This is used
	// as preliminary to reloading weights from a file
	public Hashtable<String,Weight> getWeights()
	{
		Hashtable<String,Weight>conns = new Hashtable<String,Weight>();
		for(Layer l : getLayers())
		{	
			for(Neuron n : l.getNeurons())
			{
			Weight inc[] = n.getWeights();
			if(inc!=null)
			{
			for(Weight c : inc)
			{
			if(c!=null)
			{	String id = c.getId();
				Weight was = conns.get(id);
				if(was!=c)
				{
				G.Assert(was==null,
							"Connection id %s already exists, but should be unique; was %s is %s",
							id,was,c);
				conns.put(id, c);
				}
			}
			}}
			}
		}
		return(conns);
	}
	public void printNetworkWeights(PrintStream out)
	{	for(Layer l : getLayers()) { l.printNetworkWeights(out); }
	}

	public Neuron[] getInputNeurons()
		{
		 return(layers[0].getNeurons());
		}
	public void calculate()
	{
		for(Layer l : layers) { l.calculate(); }
	}
	public TransferFunction makeTransferFunction(String type)
	{	
		if("LINEAR".equals(type)) { return(new Linear()); }
		else if("SIGMOID".equals(type)) { return(new Sigmoid()); }
		else if("TANH".equals(type)) { return(new Tanh()); }
		else if("LOG".equals(type)) { return(new Log()); }
		else { throw G.Error("transfer function %s not defined",type); }
	}
	public void parseCoordinateType(String val,StringTokenizer s)
	{
		if("HEX".equals(val))
		{	int span = G.IntToken(s);
			setCoordinateMap(new HexCoordinateMap(span));
		}
		else if("SQUARE".equals(val))
		{	int cols = G.IntToken(s);
			int rows = G.IntToken(s);
			setCoordinateMap(new SquareCoordinateMap(cols,rows));
			
		}
		else { G.Error("Coordinate type %s not defined",val); }
	}
	
	// the general form is "LAYER <number> ( option value option2 value2 )"
	// this parses the <number> and everything between and including the ()
	// the StringTokenizer is created to consider ( ) as delimiters
	public void parseLayerOptions(String lan,StringTokenizer s,int nextLayer)
	{	
		int layern = "next".equalsIgnoreCase(lan) ? nextLayer : G.IntToken(lan);
		String tok = nextToken(s);
		G.Assert("(".equals(tok),"must be () after layer %s",layern);
		String val = "";
		while(!")".equals((tok=nextToken(s))))
		{
			val += tok; 
			val += " ";
		}
		layerOptions[layern] = val;
	}
	
	// parse the options for a network.  This should completely define
	// the geometry and connectivity for the network, so it is all loadNetwork needs
	public void parseOptions(String properties)
	{	StringTokenizer s = stringTokenizer(properties);
		int nextLayer = 0;
		while(s.hasMoreTokens())
		{
			String key = nextToken(s);
			if(!"".equals(key))
			{
			String val = nextToken(s);
			if("LAYER".equals(key)) { parseLayerOptions(val,s,nextLayer); nextLayer++; }
			else if("NAME".equals(key)) { name = val; }
			else if("COORDINATES".equals(key)) { parseCoordinateType(val,s); }
			else if("TRANSFER_FUNCTION".equals(key)) { mainTransferFunction = val; }
			else if("LEARNING_RATE".equals(key)) { learningRate = Double.parseDouble(val); } 
			else if("STEPS".equals(key)) { trainingSteps = Integer.parseInt(val); }
			else if("ERROR".equals(key)) { trainingError = Double.parseDouble(val); }
			else if("DEFAULT_BIAS".equals(key)) { defaultBias = Double.parseDouble(val); }
			else if("DEFAULT_SCALE".equals(key)) { defaultScale = Double.parseDouble(val); }
			else { G.Error("Option %s not parsed",key); }
			}
		}
	}
	private void makeLayer(int idx)
	{	String options = layerOptions[idx];
		StringTokenizer tok = stringTokenizer(options);
		String type = "FC";
		String name = ""+idx;
		int size = layerSpecs[idx];
		CoordinateMap map = coordinateMap;
		TransferFunction transfer = makeTransferFunction(mainTransferFunction);
		LayerStack toLayer = new LayerStack();
		double outputBias = defaultBias;
		double outputScale = defaultScale;
		String layerID = null;
		LayerStack singleLayer = new LayerStack();
		while(tok.hasMoreTokens())
		{
			String key = nextToken(tok);
			String val = nextToken(tok);
			if("TYPE".equals(key)) { type = val; }
			else if("ID".equals(key)) { layerID = val; }
			else if("TO".equals(key))
				{ Layer la = getLayer(val);
				  G.Assert(la!=null, "Single layer %s not found",val);
				  toLayer.push(la); 
				}
			else if("TOSINGLE".equals(key)) 
				{ Layer la = getLayer(val);
				  G.Assert(la!=null, "Single layer %s not found",val);
				  singleLayer.push(la); 
				}
			else if("TRANSFER".equals(key)) { transfer = makeTransferFunction(val); }
			else if("BIAS".equals(key)) { outputBias = G.DoubleToken(val); }
			else if("SCALE".equals(key)) { outputScale = G.DoubleToken(val); }
			else { throw G.Error("Layer option %s not expected",key); }
		}
		transfer.setBias(outputBias);
		transfer.setScale(outputScale);
		
		if(!type.equals("I")&& (toLayer.size()==0)) 
		{ 
		Layer la = layers[idx-1];
		toLayer.push(la);
		}
		if("O".equals(type))
		{
			layers[idx] = new OutputLayer(name,
						size,
						transfer);
		}		
		else if("FC".equals(type))
		{
			layers[idx] = new FCLayer(name,
						size,
						transfer);
		}
		else if("FILTER".equals(type))
		{	FilterLayer fil = new FilterLayer(name,
									size,
									transfer,
									map,
									(singleLayer.size()>0) ? singleLayer.toArray() : null 
									);
			layers[idx] = fil;
		}
		else if("POOL".equals(type))
		{
			layers[idx] = new PoolLayer(name,
					transfer,
					map);
		}

		else if("I".equals(type))
		{
			layers[idx] = new InputLayer(name,size);
			toLayer.clear();
		}
		else { throw G.Error("layer type %s not defined",type); }
		
		if(layerID!=null) { layers[idx].setID(layerID); }
		
		if(toLayer.size()>0) 
			{ Layer l = layers[idx];
			  l.connectFrom(toLayer.toArray()); 
			}
	}
	

	
	/** construct a network with layer sizes specified.
	 * the first layer will be an input layer.  The value
	 * of the overall network will be the summary of the last
	 * layer.
	 * @param layersize
	 */
	public GenericNetwork(String properties,int... layersize)
	{	
		options = properties;
		layerSpecs = layersize;
		int len = layerSpecs.length;
		layerOptions = new String[len];
		
		// default layer options, layer 0 is an input layer, last layer is an output layer
		// the rest are fully connected layers in a chain.
		layerOptions[0] = "TYPE I ID IN ";
		layerOptions[len-1] = "TYPE O ID OUT TO "+(len-2);
		for(int i=1;i<len-1;i++)
		{
			layerOptions[i] = "TYPE FC TO "+(i-1);
		}
		
		parseOptions(properties);		// this might override the default layer specs
		
		layers = new Layer[layersize.length];
			
		// construct all the layers
		for(int i=0;i<len;i++)	{	makeLayer(i);	}

		initializeWeights(new Random(),-0.5,2.0);
	}

	public void initializeWeights(Random r,double offset,double scale)
	{	for(Layer l : layers) { l.initializeWeights(r,offset,scale); }
	}


	public double[] getErrors(Layer layer,double expectedValues[])
	{	double vals[] = getValues(layer);
		for(int lim=expectedValues.length-1; lim>=0; lim--)
		{ vals[lim] = expectedValues[lim]-vals[lim];
		}
		return(vals);
	}
	public double sumErrors(double []errors)
	{
		double val = 0;
		for(double e : errors) { val+= Math.abs(e);}
		return(val);
	}
	boolean outputMapped = false;
	private void mapOutputConnections()
	{
		if(!outputMapped)
		{
			for(Layer l : layers) { l.mapConnections(); }
			outputMapped = true;
		}
	}
	
	// learn from expected values of the default output layer
	public void learn(double expectedValues[])
	{
		learn(getLayer("OUT"),expectedValues);
	}
	public void learn(Layer layer,double expectedValues[])
	{	// adjust weights, accumulate the attributable error for input nodes
		mapOutputConnections();
		double vals[] = getErrors(layer,expectedValues);
		double error = sumErrors(vals);
		trainingSteps++;
		trainingError = error;
		if(error>0.001)
		{
		boolean replay = false;
		do {	
		replay = false;
		updateNetworkWeights(layer,vals);
		calculate();
		double newvals[] = getErrors(layer,expectedValues);
		double newerror = sumErrors(newvals);
		if(newerror>(error+error*learningRate)) 
			{ //G.print("error increased from "+error+" to "+newerror);
			  //dumpWeights(false);
			  revertConnectionWeights();
			  //calculate();
			  //getErrors(expectedValues);
			  //dumpWeights();
			  //learningRate = learningRate/2;
			  //G.print("New learning rate "+learningRate);  
			  //replay = true;
			}
		} while(replay);
		}
	}


	   public void revertConnectionWeights()
	   {
		   for(Layer l : layers)
		   {
			   for(Neuron n : l.getNeurons())
			   {
				   n.revertConnectionWeights();
			   }
		   }
	   }


    // from Backpropagation
	/**
	 * This method implements weight update procedure for the whole network
	 * for the specified  output error vector
	 * 
	 * @param outputError
	 *            output error vector
	 */
	protected boolean updateNetworkWeights(Layer layer,double[] outputError) {
		boolean rangeError =
				this.calculateErrorAndUpdateOutputNeurons(layer,outputError)
				| this.calculateErrorAndUpdateHiddenNeurons(); 
		
		if(rangeError)
		{   G.print("update cancelled, error out of range");
		    dumpWeights(false);
		    learningRate = learningRate/2;
		    G.print("New learning rate is "+learningRate);
			revertConnectionWeights();
		}
		return(rangeError);
	}
	public boolean updateNetwork(Layer layer,double []error)
	{	return updateNetworkWeights(layer,error);
	}    

	/**
	 * This method implements weights update procedure for the output neurons
	 * Calculates delta/error and calls updateNeuronWeights to update neuron's weights
         * for each output neuron
         * 
	 * @param outputError
	 *            error vector for output neurons
	 */
	protected boolean calculateErrorAndUpdateOutputNeurons(Layer layer,double[] outputError) {
		int i = 0;
		boolean rangeError=false;
                // for all output neurons
		for(Neuron neuron : getOutputNeurons(layer)) {
                        // if error is zero, just set zero error and continue to next neuron
			if (outputError[i] == 0) {
				neuron.setError(0);
				i++;
				continue;
			}
			
                        // otherwise calculate and set error/delta for the current neuron
			TransferFunction transferFunction = neuron.getTransferFunction();
			double value = neuron.getValue();
			double delta = outputError[i] * transferFunction.getDerivative(value); // delta = (d-y)*df(net)
			rangeError |= neuron.setError(delta);
                        
                        // and update weights of the current neuron
			neuron.updateNeuronWeights(learningRate);				
			i++;
		} // for	
		return(rangeError);
	}        

	/**
	 * This method implements weights adjustment for the hidden layers
	 */
	public boolean calculateErrorAndUpdateHiddenNeurons() {
		Layer[] layers = getLayers();
		boolean rangeError = false;
		// chain errors starting at the output layer-1 to the input layer
		for(int lim = layers.length-1; lim>=0; lim--)
			{ Layer l = layers[lim];
			  if(!l.isOutputLayer())
			  {
			  rangeError |= l.calculateErrorAndUpdateHiddenNeurons(learningRate);
			  }
			}

		return rangeError;
	}
	// this is used to create copies of a network for multiple threads.
	// it's much better for each thread to have its own copy of the
	// network than to endure the locking time when threads share.
	public Network duplicateInstance()
	{	return new GenericNetwork(options,layerSpecs);
	}
	public double[] calculateValues(int layer,double...in)
	{	Layer[]ls = getLayers();
		return calculateValues(ls[layer],in);
	}
	public double[] calculateValues(Layer layer,double...in)
	{	synchronized(this)
		{
		layer.setInputs(in);
		calculate();
		return(getValues());
		}
	}
	public double[] calculateValues(int layer,double in[],int layer2,double[] in2)
	{	Layer[]ls = getLayers();
		return calculateValues(ls[layer],in,ls[layer2],in2);
	}
	public double[] calculateValues(Layer layer,double in[],Layer layer2,double[] in2)
	{	synchronized(this)
		{
		layer.setInputs(in);
		layer2.setInputs(in2);
		calculate();
		return(getValues());
		}
	}
	
	public double[] calculateValues(Layer layer,double in[],Layer layer2,double[] in2,Layer layer3,double[]in3)
	{	synchronized(this)
		{
		layer.setInputs(in);
		layer2.setInputs(in2);
		layer3.setInputs(in3);
		calculate();
		return(getValues());
		}
	}
	
	public double[] calculateValues(Layer layer,double in[],Layer layer2,double[] in2,Layer layer3,double[]in3,Layer layer4,double[] value4)
	{	synchronized(this)
		{
		layer.setInputs(in);
		layer2.setInputs(in2);
		layer3.setInputs(in3);
		layer4.setInputs(value4);
		calculate();
		return(getValues());
		}
	}
	
	public Layer getLayer(String id)
	{
		for(Layer l : getLayers()) 
			{ if((l!=null)
					&& (id.equals(l.getID()) || id.equals(l.getName()))
					) return(l); }
		return(null);
	}
	public int inputNetworkSize() { return(getLayers()[0].getNeurons().length);}
	
	public void saveNetwork(String file,String comment) 
	{
		OutputStream s=null;
		try {
			s = new FileOutputStream(new File(file));
			if(s!=null)
				{PrintStream ps = new PrintStream(s);
				saveNetwork(ps,comment);
				ps.close();
				s.close();
				}
		} catch (IOException e) {
			G.Error("Save network error %s",e);
		}
	}
	public void dumpWeights(boolean values)
	{	System.out.println();
		for(Layer l : getLayers())
		{	l.dumpWeights(values);
		}
	}
	public void copyWeights(Network from)
	{	Layer[] toLayers = getLayers();
		Layer[] fromLayers = from.getLayers();
		
		for(int layern = 0; layern<toLayers.length; layern++)
		{	toLayers[layern].copyWeights(fromLayers[layern]);
			
		}
	}
	public Network duplicate()
	{
		Network newnet = duplicateInstance();
		newnet.copyWeights(this);
		return(newnet);
	}
}
