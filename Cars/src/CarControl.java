//Prototype implementation of Car Control
//Mandatory assignment
//Course 02158 Concurrent Programming, DTU, Fall 2015

//Hans Henrik Løvengreen    Oct 6,  2015


import java.awt.Color;

class Barrier {
	lol;
	int barrierCount = 0;
	Semaphore g = new Semaphore(0);
	
    public void sync() {
    	barrierCount++;
    	if(barrierCount == 8) {
    		barrierCount = 0;
    		for(int i = 0; i < 8; i++) {
    			g.V();
    		}
		}
    	try {g.P(); } catch (InterruptedException e) {}
    }  // Wait for others to arrive (if barrier active)

	   public void on() {}    // Activate barrier

	   public void off() {}   // Deactivate barrier 

	}

class Alley {
	
	Semaphore cw = new Semaphore(1);
	Semaphore ccw = new Semaphore(1);
	
	
	
	int alleyCount = 0;
	
	public void enter(int no) {
		if (no > 0 && no < 5) {
			try { cw.P();} catch (InterruptedException e) {}
			if(alleyCount == 0) {
				try { ccw.P();} catch (InterruptedException e) {}
			}
			cw.V();
			
		}
		else if (no > 4 && no < 9) {
			try { ccw.P();} catch (InterruptedException e) {}
			if(alleyCount == 0) {
				try { cw.P();} catch (InterruptedException e) {}
			}
			ccw.V();
		}
		
		alleyCount++;
	}
	
	public void leave(int no) {
		if (alleyCount == 1 && no > 0 && no < 5) {
			ccw.V();
		}
		else if (alleyCount == 1 && no > 4 && no < 9) {
			cw.V();
		}
		
		alleyCount--;
	}
}

class Map {
	
	Semaphore[][] map = new Semaphore[11][12];
	
	public Map(Car[] cars) {
		boolean notFound = true;
		for(int i = 0; i < 11; i++) {
			for(int j = 0; j < 12; j++) {
				notFound = true;
				for(int k = 0; k < 9; k++) {
					if (i == cars[k].startpos.row && j == cars[k].startpos.col) {
						map[i][j] = new Semaphore(0);
						notFound = false;
					}
				}
				if(notFound) map[i][j] = new Semaphore(1);
			}
		}
		for(int i = 0; i < 11; i++) {
			for(int j = 0; j < 12; j++) {
				//System.out.print(map[i][j].toString() + "\t");
			}
			//System.out.println("\n");
		}
	}
	
	public void enter(Pos curPos, Pos nextPos) {
		//System.out.println("hej");
		//System.out.println(curPos.row + ":" + curPos.col);
		try { map[nextPos.row][nextPos.col].P(); } catch (InterruptedException e) {}
		
		
	}
	public void signalenter(Pos curpos, Pos newpos) {
		map[curpos.row][curpos.col].V();
	}
	
}



class Gate {

    Semaphore g = new Semaphore(0);
    Semaphore e = new Semaphore(1);
    boolean isopen = false;

    public void pass() throws InterruptedException {
        g.P(); 
        g.V();
    }

    public void open() {
        try { e.P(); } catch (InterruptedException e) {}
        if (!isopen) { g.V();  isopen = true; }
        e.V();
    }

    public void close() {
        try { e.P(); } catch (InterruptedException e) {}
        if (isopen) { 
            try { g.P(); } catch (InterruptedException e) {}
            isopen = false;
        }
        e.V();
    }

}

class Car extends Thread {

    int basespeed = 100;             // Rather: degree of slowness
    int variation =  50;             // Percentage of base speed

    CarDisplayI cd;                  // GUI part

    int no;                          // Car number
    Pos startpos;                    // Startpositon (provided by GUI)
    Pos barpos;                      // Barrierpositon (provided by GUI)
    Color col;                       // Car  color
    Gate mygate;                     // Gate at startposition


    int speed;                       // Current car speed
    Pos curpos;                      // Current position 
    Pos newpos;                      // New position to go to
    
    Car[] cars;
    Map map;
    Alley alley;
    Barrier barrier;

    public Car(int no, CarDisplayI cd, Gate g, Car[] cars, Alley alley, Barrier barrier) {
        this.no = no;
        this.cd = cd;
        this.cars = cars;
        this.alley = alley;
        this.barrier = barrier;
        mygate = g;
        startpos = cd.getStartPos(no);
        barpos = cd.getBarrierPos(no);  // For later use

        col = chooseColor();

        // do not change the special settings for car no. 0
        if (no==0) {
            basespeed = 0;  
            variation = 0; 
            setPriority(Thread.MAX_PRIORITY); 
        }
        
    }
    
    public void setMap(Map map) {
    	this.map = map;
    }
    
    public synchronized void setSpeed(int speed) { 
        if (no != 0 && speed >= 0) {
            basespeed = speed;
        }
        else
            cd.println("Illegal speed settings");
    }

    public synchronized void setVariation(int var) { 
        if (no != 0 && 0 <= var && var <= 100) {
            variation = var;
        }
        else
            cd.println("Illegal variation settings");
    }

    synchronized int chooseSpeed() { 
        double factor = (1.0D+(Math.random()-0.5D)*2*variation/100);
        return (int)Math.round(factor*basespeed);
    }

    private int speed() {
        // Slow down if requested
        final int slowfactor = 3;  
        return speed * (cd.isSlow(curpos)? slowfactor : 1);
    }

    Color chooseColor() { 
        return Color.blue; // You can get any color, as longs as it's blue 
    }

    Pos nextPos(Pos pos) {
        // Get my track from display
        return cd.nextPos(no,pos);
    }

    boolean atGate(Pos pos) {
        return pos.equals(startpos);
    }
    
   boolean atAlley(Pos nextPos, int no) {
	
		if (nextPos.row > 0 && nextPos.row < 11 && nextPos.col == 0) {
			return true;
		} else if(nextPos.row == 9 && nextPos.col < 3) {
			return true;
		}
		return false;
   }
    
   boolean atBarrier(Pos pos) {
	   if(pos.row == 4 && pos.col > 2 && pos.col < 8) {
		   return true;
	   } else if(pos.row == 5 && pos.col > 7) {
		   return true;
	   }
	   return false;
   }

   public void run() {
        try {

            speed = chooseSpeed();
            curpos = startpos;
            cd.mark(curpos,col,no);

            while (true) { 
                sleep(speed());

                if (atGate(curpos)) { 
                    mygate.pass(); 
                    speed = chooseSpeed();
                }
               
                newpos = nextPos(curpos);
                
                if (!atAlley(curpos, no) && atAlley(newpos,no)) {
                	alley.enter(no);
                }
                
                if (atAlley(curpos, no) && !atAlley(newpos,no)) {
                	alley.leave(no);
                }
                
                if(atBarrier(curpos)) {
                	barrier.sync();
                }
                
                //System.out.println(newpos.row + ":" + newpos.col);
                map.enter(curpos, newpos);
                
                //  Move to new position 
                cd.clear(curpos);
                cd.mark(curpos,newpos,col,no);
                sleep(speed());
                cd.clear(curpos,newpos);
                cd.mark(newpos,col,no);
                
                //map.signalenter(curpos, newpos);
                map.signalenter(curpos, newpos);
                curpos = newpos;
            }

        } catch (Exception e) {
            cd.println("Exception in Car no. " + no);
            System.err.println("Exception in Car no. " + no + ":" + e);
            e.printStackTrace();
        }
    }

}

public class CarControl implements CarControlI{

    CarDisplayI cd;           // Reference to GUI
    Car[]  car;               // Cars
    Gate[] gate;              // Gates
    Map map;
    Alley alley;
    Barrier barrier;

    public CarControl(CarDisplayI cd) {
        this.cd = cd;
        car  = new  Car[9];
        gate = new Gate[9];
        alley = new Alley();
        barrier = new Barrier();

        for (int no = 0; no < 9; no++) {
            gate[no] = new Gate();
            car[no] = new Car(no,cd,gate[no],car, alley,barrier);
        }
        
        this.map = new Map(car);
        for(int i=0; i<9; i++){
        	car[i].setMap(map);
        }
       
        
        
        for (int no = 0; no < 9; no++) {
        	car[no].start();
        } 
    }

   public void startCar(int no) {
        gate[no].open();
    }

    public void stopCar(int no) {
        gate[no].close();
    }

    public void barrierOn() { 
        cd.println("Barrier On not implemented in this version");
    }

    public void barrierOff() { 
        cd.println("Barrier Off not implemented in this version");
    }

    public void barrierShutDown() { 
        cd.println("Barrier shut down not implemented in this version");
        // This sleep is for illustrating how blocking affects the GUI
        // Remove when shutdown is implemented.
        try { Thread.sleep(3000); } catch (InterruptedException e) { }
        // Recommendation: 
        //   If not implemented call barrier.off() instead to make graphics consistent
    }

    public void setLimit(int k) { 
        cd.println("Setting of bridge limit not implemented in this version");
    }

    public void removeCar(int no) { 
        cd.println("Remove Car not implemented in this version");
    }

    public void restoreCar(int no) { 
        cd.println("Restore Car not implemented in this version");
    }

    /* Speed settings for testing purposes */

    public void setSpeed(int no, int speed) { 
        car[no].setSpeed(speed);
    }

    public void setVariation(int no, int var) { 
        car[no].setVariation(var);
    }

}






