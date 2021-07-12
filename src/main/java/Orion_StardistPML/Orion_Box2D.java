package Orion_StardistPML;

public class Orion_Box2D {
    
    public final long xmin, xmax, ymin, ymax;
    
    public Orion_Box2D(long xmin, long xmax, long ymin, long ymax) {
        assert (xmin <= xmax) && (ymin <= ymax);
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;
    }
    
    public long area() {
        return (xmax-xmin)*(ymax-ymin); 
    }
    
    public long intersection_area(Orion_Box2D box) {
        long ixmin = Math.max(this.xmin, box.xmin);
        long ixmax = Math.min(this.xmax, box.xmax);
        long iymin = Math.max(this.ymin, box.ymin);
        long iymax = Math.min(this.ymax, box.ymax);
        if ((ixmin < ixmax) && (iymin < iymax))
            return (ixmax-ixmin)*(iymax-iymin);
        else
            return 0;
    }
    
    public boolean does_intersect(Orion_Box2D box) {
        return (  box.xmin <= this.xmax &&
                 this.xmin <=  box.xmax &&
                  box.ymin <= this.ymax &&
                 this.ymin <=  box.ymax );
    }

}
