/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package detectors;

import java.util.HashMap;
import java.util.Map;


import edu.memphis.ccrg.lida.pam.tasks.BasicDetectionAlgorithm;
import modules.Environment;
import ws3dproxy.model.Thing;

/**
 *
 * @author Note Fabio M
 */
public class freeLeftDetector extends BasicDetectionAlgorithm{
    
private final String modality = "";
    private Map<String, Object> detectorParams = new HashMap<>();

    @Override
    public void init() {
        super.init();
        detectorParams.put("mode", "freeLeft");
    }
    
    @Override
    public double detect() {
        boolean response = (boolean) sensoryMemory.getSensoryContent(modality, detectorParams);
        double activation = 0.0;
        if (response == true) {
            activation = 1.0;
        }
        return activation;
    }
}
