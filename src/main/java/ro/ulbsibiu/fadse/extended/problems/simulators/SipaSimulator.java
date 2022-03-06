/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ro.ulbsibiu.fadse.extended.problems.simulators;

import ro.ulbsibiu.fadse.environment.Environment;
import ro.ulbsibiu.fadse.extended.problems.simulators.sipa.SipaOutputParser;
import ro.ulbsibiu.fadse.extended.problems.simulators.sipa.SipaRunner;

public class SipaSimulator extends SimulatorBase {

    public SipaSimulator(Environment environment) throws ClassNotFoundException {
        super(environment);
        // TODO: Where should this constant be kept?
        this.simulatorOutputFile = environment.getInputDocument().getSimulatorParameter("simulator_final_results");
        this.simulatorOutputParser = new SipaOutputParser(this);
        this.simulatorRunner = new SipaRunner(this);
    }
}