
package ro.ulbsibiu.fadse.extended.problems.simulators.sipa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

import ro.ulbsibiu.fadse.environment.parameters.IntegerParameter;
import ro.ulbsibiu.fadse.environment.parameters.Parameter;
import ro.ulbsibiu.fadse.extended.problems.simulators.SimulatorBase;
import ro.ulbsibiu.fadse.extended.problems.simulators.SimulatorRunner;

public class SipaRunner extends SimulatorRunner {

    private File benchmarkDirectory;
    private static final long TIME_BETWEEN_PROCESS_CHECKS = 5000;
    private static final long MAX_SIMULATION_TIME = 300; //MAX simulation time - if not finished, restart

    public SipaRunner(SimulatorBase simulator) {
        super(simulator);
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
    
    public void run () {
    	String input_file;
    	
    	// Target Directory for Benchmark
        benchmarkDirectory = new File(simulator.getSimulatorOutputFile() + "_" + individual.getBenchmark());
        benchmarkDirectory.mkdirs();
 
        System.out.println("benchmarkDirectory: " + benchmarkDirectory.getAbsolutePath());
    	
        input_file = create_input_file(benchmarkDirectory.getAbsolutePath());
        
        long start = System.currentTimeMillis();

        try {   	
        	String simExecPath = this.simulator.getInputDocument().getSimulatorParameter("simulator_folder");
        	String simExec = this.simulator.getInputDocument().getSimulatorParameter("simulator_executable");	
                	
        	String executeCommand = "matlab -r \"run " + input_file  + "; run " + simExecPath + simExec + "; exit\" -nodesktop -nosplash -minimize -wait";
        	
            System.out.println(
                    "- Starting simulator: ["
                    + simulator.getInputDocument().getSimulatorName()
                    + "] with the following command: \n" + executeCommand);
            
            p = Runtime.getRuntime().exec(executeCommand, null, benchmarkDirectory);
            Boolean isTerminated = false;
            Boolean doTerminate = false;

            // Execute the benchmark
            do {
                try {
                    System.out.println(
                            "Simulation: Let's wait for " + TIME_BETWEEN_PROCESS_CHECKS + " ms...");
                    synchronized (p) {
                        p.wait(TIME_BETWEEN_PROCESS_CHECKS);
                    }
                } catch (Exception iex) {
                    System.out.println("Trouble while waiting: " + iex.getMessage());
                }

                // Check if we want to stop waiting for the simulation...
                isTerminated = isExecutionTerminated(p);

                if (isTerminated) {
                    doTerminate = true;
                }
                else {
                    long currentEllapsed = System.currentTimeMillis();
                    long ellapsedTime = (currentEllapsed - start) / 1000; 
                    if(ellapsedTime > MAX_SIMULATION_TIME){
                         if (p != null && !isExecutionTerminated(p)) {
                             p.destroy();                                
                         }
                         p = Runtime.getRuntime().exec(executeCommand, null, benchmarkDirectory);
                         start = System.currentTimeMillis();
                    }
                }
            } while (!doTerminate);

        } catch (IOException ex) {
            Logger.getLogger(SipaRunner.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception e) {
            e.printStackTrace();
            this.getIndividual().markAsInfeasibleAndSetBadValuesForObjectives(
                    "Exception while running, " + e.getMessage());
        } finally {
            System.out.println("Now let's terminate the process if it is still existing.");
            // Kill process if necessary (it is still there)
            if (p != null && !isExecutionTerminated(p)) {
                System.out.println("There is a process, it is not terminated - kill it.");
                try {
                    p.destroy();
                    System.out.println("  Mission accomplished.");
                } catch (Exception e) {
                    System.out.println("  Exception during destroying process: " + e.getMessage());
                }
            }

            // Delete Benchmark directory if wanted... todo.
        }

        p = null;
        long end = System.currentTimeMillis();
        long ellapsedSeconds = (end - start) / 1000;
        System.out.println("- Simulation completed (with or without errors) in " + ellapsedSeconds + " seconds.");
    }
    
    /**
     * Checks if the simulation is still running
     */
     private boolean isExecutionTerminated(Process p) {
        if (p == null) {
            System.out.println("Process is null - terminate!");
            return true;
        }

        // If running => we will get an exception!
        try {
            int exit_value = p.exitValue();
            System.out.println("Exit value is: " + exit_value);
            return true;
        } catch (IllegalThreadStateException ex) {
            // ex.printStackTrace();
            return false;
        }
    }

    private boolean BinaryWrite(String path, Object value) {
        FileOutputStream fos = null;
        try {
            String str = String.valueOf(value);
            byte[] data = str.getBytes();
            fos = new FileOutputStream(new File(benchmarkDirectory + "\\" + path));
            fos.write(data, 0, data.length);
            fos.flush();
            fos.close();
            return true;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SipaRunner.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SipaRunner.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fos.close();
            } catch (IOException ex) {
                Logger.getLogger(SipaRunner.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
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
