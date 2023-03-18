package ro.ulbsibiu.fadse.extended.problems.simulators.sipa;

import java.util.concurrent.ExecutionException;

import com.mathworks.engine.EngineException;
import com.mathworks.engine.MatlabEngine;

public final class SipaMatlabInstance {
	
	private static SipaMatlabInstance INSTANCE;
	private MatlabEngine eng;
		
	private SipaMatlabInstance(String pathToSipaModel) {
		try {
			System.out.println("Starting a new Matlab instance. ");
			eng = MatlabEngine.startMatlab();
			
			System.out.println("Loading SIPA model. ");
			eng.eval("load('" + pathToSipaModel + "')");
		} catch (EngineException | IllegalArgumentException | IllegalStateException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected void finalize() {
    	try {
    		System.out.println("Atempting to close the Matlab instance... ");
			eng.close();
			System.out.print("[DONE]");
		} catch (EngineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	public static SipaMatlabInstance getInstance(String pathToSipaModel) {
	    if(INSTANCE == null) {
	        INSTANCE = new SipaMatlabInstance(pathToSipaModel);
	    }
	    
	    return INSTANCE;
	}
	
	public MatlabEngine getMatlabEngine() {
		return eng;
	}

}