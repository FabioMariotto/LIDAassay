package detectors;

import java.util.HashMap;
import java.util.Map;

import edu.memphis.ccrg.lida.pam.tasks.BasicDetectionAlgorithm;
import ws3dproxy.model.Thing;

public class blockAheadDetector extends BasicDetectionAlgorithm {

    private final String modality = "";
    private Map<String, Object> detectorParams = new HashMap<>();

    @Override
    public void init() {
        super.init();
        detectorParams.put("mode", "blockAhead");
    }

    @Override
    public double detect() {
        Thing block = (Thing) sensoryMemory.getSensoryContent(modality, detectorParams);
        double activation = 0.0;
        if (block != null) {
            activation = 1.0;
        }
        return activation;
    }
}
