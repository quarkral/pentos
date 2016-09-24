package pentos.g5.util;

import java.util.*;

import pentos.sim.Land;
import pentos.sim.Cell;

public class LandUtil {

    public enum Direction {INWARDS, OUTWARDS};

    public Land land;
    public int lastLoopLevel;

    public LandUtil(Land l) {
        land = l;
    }

    public Pair getDiag(BuildingUtil bu, Direction dir, Set<Pair> rejects) {

        Pair[] buildingHull = bu.Hull();

        int numLoops = land.side - 1;
        Looper looper;
        looper = new Looper(0, numLoops*2, 1);

        int loop;
        while(looper.hasNext()) {
            loop = looper.next();
            lastLoopLevel = loop;

            if (loop <= numLoops) {
                int i = loop;
                int j = 0;
                for (; j <= loop; j++) {
                    i = loop - j;
                    int actualI;
                    int actualJ;
                    if (dir == Direction.OUTWARDS) {
                        // finding cell that factory would be placed on
                        actualI = numLoops - i - buildingHull[1].i;
                        actualJ = numLoops - j - buildingHull[1].j;
                    } else {
                        actualI = i;
                        actualJ = j;
                    }
                    if (actualI < 0 || actualJ < 0)
                        continue;
                    Pair loc = new Pair(actualI, actualJ);
                    if (!rejects.contains(loc) && land.buildable(bu.building, new Cell(actualI,actualJ))) {
                        return loc;
                    }
                }
            }
            else {
                int i = numLoops;
                int j = loop - numLoops;
                for (; j <= numLoops; j++) {
                    i = loop - j;
                    int actualI;
                    int actualJ;
                    if (dir == Direction.OUTWARDS) {
                        // finding cell that factory would be placed on
                        actualI = numLoops - i - buildingHull[1].i;
                        actualJ = numLoops - j - buildingHull[1].j;
                    } else {
                        actualI = i;
                        actualJ = j;
                    }
                    if (actualI < 0 || actualJ < 0)
                        continue;
                    Pair loc = new Pair(actualI, actualJ);
                    if ((!rejects.contains(loc) && land.buildable(bu.building, new Cell(actualI,actualJ)))) {
                        return loc;
                    }
                }
            }
        }
        return new Pair(-1, -1);
    }

    public Pair getCup(BuildingUtil bu, Direction dir, Set<Pair> rejects) {

        Pair[] buildingHull = bu.Hull();

        int numLoops = (land.side+1) / 2;
        int maxI = land.side - buildingHull[1].i;
        int maxJ = land.side - buildingHull[1].j;
        int midI = (int) Math.ceil(maxI / 2.0);

        Looper looper;
        if( LandUtil.Direction.OUTWARDS == dir ) {
            looper = new Looper(numLoops-1, 0, -1);
        } else {
            looper = new Looper(0, numLoops-1, 1);
        }

        // for(int loop=0; loop < numLoops ; ++loop ) {
        int loop;
        while(looper.hasNext()) {
            loop = looper.next();
            lastLoopLevel = loop;

            // DEBUG System.err.println("Trying to build at level: "+loop);
            int i = midI-(buildingHull[1].i+1);
            int j = loop;
            for(; i > loop; --i) {
                // System.out.println(new Pair(i,j));
                Pair loc = new Pair(i, j);
                if((!rejects.contains(loc) && land.buildable( bu.building, new Cell(i,j)))) {
                    return loc;
                }
            }
            assert (i == loop);
            assert (j == loop);
            for(; j< maxJ - loop; ++j) {
                // System.out.println(new Pair(i,j));
                Pair loc = new Pair(i, j);
                if((!rejects.contains(loc) && land.buildable( bu.building, new Cell(i,j)))) {
                    return loc;
                }
            }   // Traverse all in the top row
            for(; i< maxI - loop; ++i) {
                // System.out.println(new Pair(i,j));
                Pair loc = new Pair(i, j);
                if((!rejects.contains(loc) && land.buildable( bu.building, new Cell(i,j)))) {
                    return loc;
                }
            }   // Traverse all in the left column
            for(; j>loop; --j) {
                // System.out.println(new Pair(i,j));
                Pair loc = new Pair(i, j);
                if((!rejects.contains(loc) && land.buildable( bu.building, new Cell(i,j)))) {
                    return loc;
                }
            }
            assert (i == maxI-loop);
            assert (j == loop);
            for(; i > midI; --i) {
                // System.out.println(new Pair(i,j));
                Pair loc = new Pair(i, j);
                if((!rejects.contains(loc) && land.buildable( bu.building, new Cell(i,j)))) {
                    return loc;
                }
            }
        }
        return new Pair(-1,-1);
    }

}