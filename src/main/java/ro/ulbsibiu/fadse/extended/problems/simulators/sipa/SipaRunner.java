
package ro.ulbsibiu.fadse.extended.problems.simulators.sipa;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mathworks.engine.MatlabEngine;

import ro.ulbsibiu.fadse.environment.parameters.Parameter;
import ro.ulbsibiu.fadse.extended.problems.simulators.SimulatorBase;
import ro.ulbsibiu.fadse.extended.problems.simulators.SimulatorRunner;
//import  com.mathworks.engine.*;

public class SipaRunner extends SimulatorRunner {

    private File benchmarkDirectory;
    private MatlabEngine eng;

    public SipaRunner(SimulatorBase simulator) {
        super(simulator);
        
        String simExecPath = this.simulator.getInputDocument().getSimulatorParameter("simulator_folder");
    	String simExec = this.simulator.getInputDocument().getSimulatorParameter("simulator_executable");
        String pathToSipaModel = simExecPath + simExec;
        
        SipaMatlabInstance sipaMatlabInst = SipaMatlabInstance.getInstance(pathToSipaModel);
        eng = sipaMatlabInst.getMatlabEngine();
    }
    
    
    @Override
    public void prepareParameters() {
        super.prepareParameters();
    }

    @Override
    protected String[] getCommandLine() {
        String[] result = new String[1];
        return result;
    }
    
    public String create_input_file(String outputPath) {
    	
    	String file_name = "input_param_file.m";
    	String file_content = new String();
    	String output_file_path = outputPath + "\\" + file_name;
    	String p = new String();
    	
        for (Parameter param : individual.getParameters()) {             
        	p += param.getValue() + "; ";
        }
        
        // Remove the last two chars: "; "
        p = p.substring(0, p.length() - 2);
        
        file_content = "param = [ " + p +  "]\n";
        file_content += "output_path = \"" + outputPath + "\"";
        
        // Write the parameters as an matlab array
        TextWrite(file_name, file_content); 
        
        return output_file_path;
    }
    
    public String createOutputPath(String outputPath) {
    	
    	String outputPathVar = new String();
    	
    	outputPathVar = "output_path = \"" + outputPath + "\"";
             
        return outputPathVar;
    }
    
    public String createParamArray() {	
    	String paramArray = new String();
    	String p = new String();
    	
        for (Parameter param : individual.getParameters()) {             
        	p += param.getValue() + "; ";
        }
        
        // Remove the last two chars: "; "
        p = p.substring(0, p.length() - 2);
        
        paramArray = "param = [ " + p +  "]\n";
         
        return paramArray;
    }
    
    public void run () {
    	String outputFolderPath;
    	
    	// Target Directory for Benchmark
        benchmarkDirectory = new File(simulator.getSimulatorOutputFile() + "_" + individual.getBenchmark());
        benchmarkDirectory.mkdirs();
 
        System.out.println("benchmarkDirectory: " + benchmarkDirectory.getAbsolutePath());
    	
        long start = System.currentTimeMillis();

        try {
        	// Create input variables (param and output_var) for Matlab
			eng.eval(createParamArray());
			eng.eval(createOutputPath(benchmarkDirectory.getAbsolutePath()));
			
			create_input_file(benchmarkDirectory.getAbsolutePath());
			
			// Run the SIPA simulation
	        eng.eval("Output = sim(PROJ.Model.Main{1},param)");
	        
	        // Dump the results into a file
	        eng.eval("save(output_path + \"\\\\performance.txt\", 'Output', '-ascii','-double','-append')");
		} catch (CancellationException | InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  		
       
        long end = System.currentTimeMillis();
        long ellapsedSeconds = (end - start) / 1000;
        System.out.println("- Simulation completed (with or without errors) in " + ellapsedSeconds + " seconds.");
    }
    
   
    private boolean TextWrite(String path, Object value) {
        BufferedWriter out = null;
        try {
            String str = String.valueOf(value);

            out = new BufferedWriter(new FileWriter(new File(benchmarkDirectory + "\\" + path)));
            out.write(str);
            out.flush();
            out.close();
            return true;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SipaRunner.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SipaRunner.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                out.close();
            } catch (IOException ex) {
                Logger.getLogger(SipaRunner.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }
}
