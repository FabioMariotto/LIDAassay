package modules;

import edu.memphis.ccrg.lida.sensorymemory.SensoryMemoryImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ws3dproxy.model.Thing;

public class SensoryMemory extends SensoryMemoryImpl {

    private Map<String, Object> sensorParam;
    private boolean freeRight;
    private boolean freeLeft;
    private boolean freeUp;
    private boolean freeDown;

    public SensoryMemory() {
        this.sensorParam = new HashMap<>();
        this.freeRight = false;
        this.freeLeft= false;
        this.freeUp= false;
        this.freeDown= false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void runSensors() {
        sensorParam.clear();
        sensorParam.put("mode", "freeRight");
        freeRight = (boolean) environment.getState(sensorParam);
        sensorParam.clear();
        sensorParam.put("mode", "freeLeft");
        freeLeft = (boolean) environment.getState(sensorParam);
        sensorParam.clear();
        sensorParam.put("mode", "freeUp");
        freeUp = (boolean) environment.getState(sensorParam);
        sensorParam.clear();
        sensorParam.put("mode", "freeDown");
        freeDown = (boolean) environment.getState(sensorParam);
    }

    @Override
    public Object getSensoryContent(String modality, Map<String, Object> params) {
        boolean response=false;
        String mode = (String) params.get("mode");
        switch (mode) {
            case "freeRight":
                response = freeRight;
                break;
            case "freeLeft":
                response = freeLeft;
                break;
            case "freeUp":
                response = freeUp;
                break;
            case "freeDown":
                response = freeDown;
                break;
            default:
                break;
        }
        return response;
    }

    @Override
    public Object getModuleContent(Object... os) {
        return null;
    }

    @Override
    public void decayModule(long ticks) {
    }
}
