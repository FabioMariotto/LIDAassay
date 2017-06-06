package modules;

import edu.memphis.ccrg.lida.environment.EnvironmentImpl;
import edu.memphis.ccrg.lida.framework.tasks.FrameworkTaskImpl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import ws3dproxy.WS3DProxy;
import ws3dproxy.model.Creature;
import ws3dproxy.model.Leaflet;
import ws3dproxy.model.Thing;
import ws3dproxy.model.World;
import ws3dproxy.util.Constants;

public class Environment extends EnvironmentImpl {

    private static final int DEFAULT_TICKS_PER_RUN = 100;
    private int ticksPerRun;
    private WS3DProxy proxy;
    private Creature creature;
    private Thing food;
    private Thing jewel;
    private Thing blockAhead;
    private List<Thing> thingAhead;
    private Thing leafletJewel;
    private String currentAction; 
    public String Map="";
    private int gridSize = 25;
    private int envXSize = 800;
    private int envYSize = 600;
    private int numXCell = (int)Math.ceil(envXSize/gridSize);
    private int numYCell = (int)Math.ceil(envYSize/gridSize);
    private boolean[][] envGrid = new boolean[numXCell][numYCell];
    private int targetXCell = numXCell-1;
    private int targetYCell = numYCell-1;
    private List<int[]> possibleCells = new ArrayList<>();
    private List<int[]> pathToTarget = new ArrayList<>();
    
    public Environment() {
        this.ticksPerRun = DEFAULT_TICKS_PER_RUN;
        this.proxy = new WS3DProxy();
        this.creature = null;
        this.food = null;
        this.jewel = null;
        this.thingAhead = new ArrayList<>();
        this.leafletJewel = null;
        this.blockAhead = null;
        this.currentAction = "rotate";
    }

    @Override
    public void init() {
        super.init();
        ticksPerRun = (Integer) getParam("environment.ticksPerRun", DEFAULT_TICKS_PER_RUN);
        taskSpawner.addTask(new BackgroundTask(ticksPerRun));
        try {
            System.out.println("Reseting the WS3D World ...");
            proxy.getWorld().reset();
            creature = proxy.createCreature(75,75,0);
            World.createBrick(0, 795, 0, 800, 600);
            World.createBrick(0, 0, 0, 800, 5);
            World.createBrick(0, 0, 595, 800, 600);
            World.createBrick(0, 0, 0, 5, 600);
            World.createBrick(1, 150, 0, 155, 400);
            World.createBrick(1, 350, 0, 355, 400);
            World.createBrick(1, 550, 0, 555, 400);
            World.createBrick(2, 250, 200, 255, 600);
            World.createBrick(2, 450, 200, 455, 600);
            World.createBrick(2, 650, 200, 655, 600);
            creature.moveto(3.0, 80, 80); 
            creature.stop();
            System.out.println("Starting the WS3D Resource Generator ... ");
            //auto generates random startup enviroment
            //World.grow(1);
            Thread.sleep(50);
            creature.updateState();
            System.out.println("DemoLIDA has started...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class BackgroundTask extends FrameworkTaskImpl {

        public BackgroundTask(int ticksPerRun) {
            super(ticksPerRun);
        }

        @Override
        protected void runThisFrameworkTask() {
            updateEnvironment();
            
            recordBricks(creature.getThingsInVision());
            performAction(currentAction);
            pathToTarget=searchPath();
            Map=mapStringer();
            System.out.println(Map);
        }
    }

    @Override
    public void resetState() {
        currentAction = "rotate";
    }

    @Override
    public Object getState(Map<String, ?> params) {
        Object requestedObject = null;
        String mode = (String) params.get("mode");
        switch (mode) {
            case "food":
                requestedObject = food;
                break;
            case "jewel":
                requestedObject = jewel;
                break;
            case "thingAhead":
                requestedObject = thingAhead;
                break;
            case "leafletJewel":
                requestedObject = leafletJewel;
                break;
            case "blockAhead":
                requestedObject = blockAhead;
            default:
                break;
        }
        return requestedObject;
    }

    
    public void updateEnvironment() {
        creature.updateState();
        food = null;
        jewel = null;
        leafletJewel = null;
        blockAhead = null;
        thingAhead.clear();
                
        for (Thing thing : creature.getThingsInVision()) {
            if (creature.calculateDistanceTo(thing) <= Constants.OFFSET) {
                // Identifica o objeto proximo
                thingAhead.add(thing);
                break;
            } else if (thing.getCategory() == Constants.categoryJEWEL) {
                if (leafletJewel == null) {
                    // Identifica se a joia esta no leaflet
                    for(Leaflet leaflet: creature.getLeaflets()){
                        if (leaflet.ifInLeaflet(thing.getMaterial().getColorName()) &&
                                leaflet.getTotalNumberOfType(thing.getMaterial().getColorName()) > leaflet.getCollectedNumberOfType(thing.getMaterial().getColorName())){
                            leafletJewel = thing;
                            break;
                        }
                    }
                } else {
                    // Identifica a joia que nao esta no leaflet
                    jewel = thing;
                }
            } else if (food == null && creature.getFuel() <= 300.0
                        && (thing.getCategory() == Constants.categoryFOOD
                        || thing.getCategory() == Constants.categoryPFOOD
                        || thing.getCategory() == Constants.categoryNPFOOD)) {
                
                    // Identifica qualquer tipo de comida
                    food = thing;
            }
            else if (blockAhead == null && thing.getCategory() == Constants.categoryBRICK) {
                    // Identifica parede mais próxima
                    blockAhead = thing;
            }
           
        }
    }
    
    
    
    @Override
    public void processAction(Object action) {
        String actionName = (String) action;
        currentAction = actionName.substring(actionName.indexOf(".") + 1);
    }

    // Checks if a Thing is inside the cell grid x,y
    private boolean isInsideGrid(Thing thing, int x, int y){
        int xmin = gridSize* x - gridSize;
        int xmax = gridSize* x;
        int ymin = gridSize* y - gridSize;
        int ymax = gridSize* y;
        if ((((int)thing.getX1()>=xmin && (int)thing.getX1()<xmax)
                && ((int)thing.getY1()>=ymin && (int)thing.getY1()<ymax))
                || (((int)thing.getX2()>=xmin && (int)thing.getX2()<xmax)
                && ((int)thing.getY2()>=ymin && (int)thing.getY2()<ymax))
                ||(((int)thing.getX1()>=xmin && (int)thing.getX1()<xmax)
                && ((int)thing.getY2()>=ymin && (int)thing.getY2()<ymax))
                || (((int)thing.getX2()>=xmin && (int)thing.getX2()<xmax)
                && ((int)thing.getY1()>=ymin && (int)thing.getY1()<ymax))){
            return true;
        }
        else if (((xmin>=(int)thing.getX1() && xmin<(int)thing.getX2())
                && (ymin>=(int)thing.getY1() && ymin<(int)thing.getY2()))
                || ((xmax>=(int)thing.getX1() && xmax<(int)thing.getX2())
                && (ymax>=(int)thing.getY1() && ymax<(int)thing.getY2()))
                ||((xmin>=(int)thing.getX1() && xmin<(int)thing.getX2())
                && (ymax>=(int)thing.getY1() && ymax<(int)thing.getY2()))
                || ((xmax>=(int)thing.getX1() && xmax<(int)thing.getX2())
                && (ymin>=(int)thing.getY1() && ymin<(int)thing.getY2()))){
            return true;
                    }
        else if ((((((int)thing.getX1()<xmin && (int)thing.getX2()>=xmax)))
                && (((int)thing.getY1()>=ymin && (int)thing.getY1()<ymax) || ((int)thing.getY2()>=ymin && (int)thing.getY2()<ymax)))
                ||((((int)thing.getY1()<ymin && (int)thing.getY2()>=ymax))
                && (((int)thing.getX1()>=xmin && (int)thing.getX1()<xmax) || ((int)thing.getX2()>=xmin && (int)thing.getX2()<xmax)))){
            return true;
        }
        return false;
    }
    
    
    //Add seen bricks to memory
    private void recordBricks(List<Thing> things){
        int xCels = numXCell;
        int yCels = numYCell;
        for (int i = 1; i <= yCels; i++) {
            for (int j = 1; j <= xCels; j++) {
                for (Thing thing : things) {
                    if (isInsideGrid(thing, j, i)){
                        envGrid[j-1][i-1]=true;
                    }
                }
            }
        }
    }
    
    //Clear bricks from memory
    private void clearBricks(){
        int xCels = numXCell;
        int yCels = numYCell;
        for (int i = 1; i <= yCels; i++) {
            for (int j = 1; j <= xCels; j++) {
                envGrid[j-1][i-1]= false;
            }
        }
    }
    
    
    //returns a string that visually represents the grids which are or not ocupied in the enviroment
    private String mapStringer(){
        String response = "";
        int xCels = numXCell;
        int yCels = numYCell;
        String pos;
        
        for (int i = 1; i <= yCels; i++) {
            for (int j = 1; j <= xCels; j++) {
                pos = "\u2591";
                
                if (envGrid[j-1][i-1]==true)
                    pos = "\u2588";
                for (int[] caminho : pathToTarget) {
                    if (caminho[0]==j && caminho[1]==i){
                        pos = "\u256C";
                    }
                }
                response += pos;
                
            }
            response += System.getProperty("line.separator");
        }
        return response;
    }
    
    private String pathStringer(List<int[]> caminhos){
        String response = "";
        String pos;
        for (int i = 1; i <= numYCell; i++) {
            for (int j = 1; j <= numXCell; j++) {
                pos = "_";
                for (int[] caminho : caminhos) {
                    if (caminho[0]==j && caminho[1]==i){
                        pos = "O";
                    }
                }
                response += pos;
            }
            response += System.getProperty("line.separator");
        }
        return response;
    }
    
    //Returns the cells list to the target
    private List<int[]> searchPath(){
        List<int[]> thisPathToTarget = new ArrayList<>();
        int cxpos = (int)Math.ceil(creature.getPosition().getX()/gridSize);
        int cypos = (int)Math.ceil(creature.getPosition().getY()/gridSize);
        
        possibleCells.clear();
        possibleCells.add(new int[]{cxpos,cypos});
        List<int[]> creaPos = new ArrayList();
        creaPos.add(new int[]{cxpos,cypos});
        thisPathToTarget = recursivePath(creaPos);
        String pathString = "";
        for (int[] cell : thisPathToTarget){
        pathString+=("("+cell[0]+","+cell[1]+")"+System.getProperty("line.separator"));
        }
        //System.out.println(pathString);
        
        return thisPathToTarget;
    }
    
    //Checks if a INT[] is inside a LIST<INT[]>
    private boolean ListContains(List<int[]> Lista, int[] cell){
        boolean aux = false;
        for (int[] tcell: Lista){
            if (tcell[0]==cell[0] && tcell[1]==cell[1])
                aux = true;
        }
        return aux;
    }
    
    
    //RECURSEVELY search for the Target cell
    private List<int[]> recursivePath(List<int[]> cells){
        List<int[]> localCells = new ArrayList<>();
        List<int[]> aux = new ArrayList<>();
        
        String pathString = "";
        for (int[] cell : cells){
        pathString+=("("+cell[0]+","+cell[1]+")");
        }
        //System.out.println(pathString+System.getProperty("line.separator"));
        
        //registra esse level da busca
        for (int[] cell:cells){
            for (int[] tcell : sideFreeCell(cell)){
                if (!ListContains(possibleCells,tcell)){
                    //adiciona a lista de células já procuradas
                    possibleCells.add(tcell);
                    aux.add(tcell);
                    
                }
            }
        }
        
        // checa se achou target
        for (int[] cell:cells){
            for (int[] tcell : sideFreeCell(cell)){
                if (ListContains(aux,tcell)){
                    if (tcell[0]==targetXCell && tcell[1]==targetYCell){
                            localCells.add(tcell);
                            localCells.add(cell);
                            return localCells;
                        }
                }
            }
        }
        try{
        localCells =recursivePath(aux);
        }
        catch (Exception name) {
            System.out.println(name.getLocalizedMessage());
        }
        int[] lastCell=localCells.get(localCells.size() - 1);
        for (int[] cell:cells){
            for (int[] tcell : sideFreeCell(cell)){
                if (tcell[0]==lastCell[0] && tcell[1]==lastCell[1]){
                    localCells.add(cell);
                    return localCells;
                }
            }
        }
        return  null;
    }
    
       
    //Checks for a list of FREE CELLS adjacent to a desired cell
    private List<int[]> sideFreeCell(int[] currentCell){
        List<int[]> freeCells = new ArrayList<>();
        try{
        
        if (currentCell[0]+1 <= numXCell)
            if(envGrid[currentCell[0]][currentCell[1]-1]==false)
                freeCells.add(new int[]{currentCell[0]+1,currentCell[1]});
        if (currentCell[0]-1 >0)
            if(envGrid[currentCell[0]-2][currentCell[1]-1]==false)
                freeCells.add(new int[]{currentCell[0]-1,currentCell[1]});
        if (currentCell[1]+1 <= numYCell)
            if(envGrid[currentCell[0]-1][currentCell[1]]==false)
                freeCells.add(new int[]{currentCell[0],currentCell[1]+1});
        if (currentCell[1]-1 > 0)
            if(envGrid[currentCell[0]-1][currentCell[1]-2]==false)
                freeCells.add(new int[]{currentCell[0],currentCell[1]-1});
        
        }
        catch (Exception e){
            System.out.println("Error on sidefreeCell:" +e.getMessage());
        }
            return freeCells; 
    }
    
    
    //Performs the ACTION choosen by the system
    private void performAction(String currentAction) {
        try {
            //System.out.println("Action: "+currentAction);
            switch ("a"){//currentAction) {
                case "rotate":
                    creature.rotate(1.0);
                    //CommandUtility.sendSetTurn(creature.getIndex(), -1.0, -1.0, 3.0);
                    break;
                case "gotoFood":
                    if (food != null) 
                        creature.moveto(3.0, food.getX1(), food.getY1());
                        //CommandUtility.sendGoTo(creature.getIndex(), 3.0, 3.0, food.getX1(), food.getY1());
                    break;
                case "gotoJewel":
                    if (leafletJewel != null)
                        creature.moveto(3.0, leafletJewel.getX1(), leafletJewel.getY1());
                        //CommandUtility.sendGoTo(creature.getIndex(), 3.0, 3.0, leafletJewel.getX1(), leafletJewel.getY1());
                    break;
                case "goAround":
                    if (blockAhead != null)
                        //TO DO: implement equation to move to extreme part of object
                        creature.moveto(3.0, 400, 300);//blockAhead.getX1(), blockAhead.getY1());
                        //CommandUtility.sendGoTo(creature.getIndex(), 3.0, 3.0, leafletJewel.getX1(), leafletJewel.getY1());
                    break;                    
                case "get":
                    creature.move(0.0, 0.0, 0.0);
                    //CommandUtility.sendSetTurn(creature.getIndex(), 0.0, 0.0, 0.0);
                    if (thingAhead != null) {
                        for (Thing thing : thingAhead) {
                            if (thing.getCategory() == Constants.categoryJEWEL) {
                                creature.putInSack(thing.getName());
                            } else if (thing.getCategory() == Constants.categoryFOOD || thing.getCategory() == Constants.categoryNPFOOD || thing.getCategory() == Constants.categoryPFOOD) {
                                creature.eatIt(thing.getName());
                            }
                        }
                    }
                    this.resetState();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
