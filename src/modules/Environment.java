package modules;

import edu.memphis.ccrg.lida.environment.EnvironmentImpl;
import edu.memphis.ccrg.lida.framework.tasks.FrameworkTaskImpl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import ws3dproxy.CommandUtility;
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
    public String currentAction; 
    public String Map="";
    private int gridSize = 25;
    private int safeSpace = 10;
    private int envXSize = 800;
    private int envYSize = 600;
    private int numXCell = (int)Math.ceil(envXSize/gridSize);
    private int numYCell = (int)Math.ceil(envYSize/gridSize);
    private boolean[][] envGrid = new boolean[numXCell][numYCell];
    private int targetXCell = numXCell-2;
    private int targetYCell = numYCell-2;
    private List<int[]> possibleCells = new ArrayList<>();
    public List<int[]> pathToTarget = new ArrayList<>();
    
    public Environment() {
        this.ticksPerRun = DEFAULT_TICKS_PER_RUN;
        this.proxy = new WS3DProxy();
        this.creature = null;
        this.currentAction = "none";
    }

    
    @Override
    public void init() {
        super.init();
        ticksPerRun = (Integer) getParam("environment.ticksPerRun", DEFAULT_TICKS_PER_RUN);
        taskSpawner.addTask(new BackgroundTask(ticksPerRun));
        try {
            System.out.println("Reseting the WS3D World ...");
            proxy.getWorld().reset();
            creature = proxy.createCreature(50,50,0);
            World.createBrick(2, 795, 0, 800, 600);
            World.createBrick(2, 0, 0, 800, 5);
            World.createBrick(2, 0, 595, 800, 600);
            World.createBrick(2, 0, 0, 5, 600);
            
            World.createBrick(2, 127, 0, 130, 400);
            World.createBrick(2, 250, 150, 255, 600);
            World.createBrick(2, 250, 145, 325, 150);
            //World.createBrick(2, 400, 145, 475, 150);
            World.createBrick(2, 450, 150, 455, 300);
            World.createBrick(2, 395, 295, 450, 300);
            //World.createBrick(1, 350, 0, 355, 400);
            //World.createBrick(2, 450, 200, 455, 600);
            
            World.createBrick(0, 395, 295, 400, 450);
            World.createBrick(0, 400, 445, 550, 450);
            World.createBrick(0, 550, 0, 555, 450);
            World.createBrick(2, 650, 200, 655, 600);
            
            World.createBrick(1, 720, 595, 775, 601);
            
            World.createFood(1, 790, 580);
            creature.start();
            creature.moveto(3.0, 80, 80); 
            creature.move(0, 0, 0);
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
            performAction(currentAction);
           
        }
    }

    @Override
    public void resetState() {
        currentAction = "default";
    }

    //return the state of the creature environment
    @Override
    public Object getState(Map<String, ?> params) {
        String mode = (String) params.get("mode");
        int[] cpos;
        boolean flag=false;
        switch (mode) {
            case "freeRight":
                cpos=currentCreatureCell();
                if (cpos[0]+1<=numXCell)
                    if(ListContains(pathToTarget, new int[]{cpos[0]+1,cpos[1]}))
                        flag=true;
                break;
            case "freeLeft":
                cpos=currentCreatureCell();
                if (cpos[0]-1>0)
                    if(ListContains(pathToTarget, new int[]{cpos[0]-1,cpos[1]}))
                        flag=true;
                break;
            case "freeDown":
                cpos=currentCreatureCell();
                if (cpos[1]+1<=numYCell)
                    if(ListContains(pathToTarget, new int[]{cpos[0],cpos[1]+1}))
                        flag=true;
                break;
            case "freeUp":
                cpos=currentCreatureCell();
                if (cpos[1]-1>0)
                    if(ListContains(pathToTarget, new int[]{cpos[0],cpos[1]-1}))
                        flag=true;
                break;
            default:
                break;
        }
        return flag;
    }

    
    public void updateEnvironment() {
        creature.updateState();
        recordBricks(creature.getThingsInVision());
        pathToTarget=searchPath();
        Map=mapStringer();
    }
    
    //returns the current creature Cell x,y
    public int[] currentCreatureCell(){
        int cxpos = (int)Math.ceil(creature.getPosition().getX()/gridSize);
        int cypos = (int)Math.ceil(creature.getPosition().getY()/gridSize);
        return new int[]{cxpos,cypos};
    }
   

    // Checks if a Thing is inside the cell grid x,y
    private boolean isInsideGrid(Thing thing, int x, int y){
        int xmin = gridSize* x - gridSize;
        int xmax = gridSize* x;
        int ymin = gridSize* y - gridSize;
        int ymax = gridSize* y;
        int y1 = (int)thing.getY1()-safeSpace;
        int y2 = (int)thing.getY2()+safeSpace;
        int x1 = (int)thing.getX1()-safeSpace;
        int x2 = (int)thing.getX2()+safeSpace;
        if (((x1>=xmin && x1<=xmax)
                && (y1>=ymin && y1<=ymax))
                || ((x2>=xmin && x2<=xmax)
                && (y2>=ymin && y2<=ymax))
                ||((x1>=xmin && x1<=xmax)
                && (y2>=ymin && y2<=ymax))
                || ((x2>=xmin && x2<=xmax)
                && (y1>=ymin && y1<=ymax))){
            return true;
        }
        else if (((xmin>=x1 && xmin<=x2)
                && (ymin>=y1 && ymin<=y2))
                || ((xmax>=x1 && xmax<=x2)
                && (ymax>=y1 && ymax<=y2))
                ||((xmin>=x1 && xmin<=x2)
                && (ymax>=y1 && ymax<=y2))
                || ((xmax>=x1 && xmax<=x2)
                && (ymin>=y1 && ymin<=y2))){
            return true;
                    }
        else if (((((x1<=xmin && x2>=xmax)))
                && ((y1>=ymin && y1<=ymax) || (y2>=ymin && y2<=ymax)))
                ||(((y1<=ymin && y2>=ymax))
                && ((x1>=xmin && x1<=xmax) || (x2>=xmin && x2<=xmax)))){
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
        thisPathToTarget.remove(thisPathToTarget.size() - 1);
        //String pathString = "";
        //for (int[] cell : thisPathToTarget){
        //pathString+=("("+cell[0]+","+cell[1]+")"+System.getProperty("line.separator"));
        //}
        //System.out.println(pathString);
        
        return thisPathToTarget;
    }
    
    //Checks if a INT[] is inside a LIST<INT[]>
    public boolean ListContains(List<int[]> Lista, int[] cell){
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
    
    
     @Override
    public void processAction(Object action) {
        String actionName = (String) action;
        currentAction = actionName.substring(actionName.indexOf(".") + 1);
    }
    
    //Performs the ACTION choosen by the system
    private void performAction(String currentAction) {
        try {
            //System.out.println("Action: "+currentAction);
            switch (currentAction){//currentAction) {
                case "goRight":
                    creature.moveto(2.0, creature.getPosition().getX()+5, creature.getPosition().getY());
                    break;
                case "goLeft":
                    creature.moveto(2.0, creature.getPosition().getX()-5, creature.getPosition().getY());
                    break;
                case "goDown":
                    creature.moveto(2.0, creature.getPosition().getX(), creature.getPosition().getY()+5);                     
                    break;
                case "goUp":
                    creature.moveto(2.0, creature.getPosition().getX(), creature.getPosition().getY()-5);
                    //CommandUtility.sendGoTo(creature.getIndex(), 3.0, 3.0, creature.getPosition().getX(), creature.getPosition().getY()-5);
                    break;                    
                default:
                    creature.rotate(3.0);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
