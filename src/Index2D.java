public class Index2D implements Pixel2D
{
    private final int X;
    private final int Y;

    public Index2D(int w, int h)
    {
        this.X = w;
        this.Y = h;
    }
    public Index2D(Pixel2D other)
    {
        this.X = other.getX();
        this.Y = other.getY();
    }
    @Override
    public int getX()
    {
        return this.X;
    }

    @Override
    public int getY() {

        return this.Y;
    }

    @Override
    public double distance2D(Pixel2D p2)
    {
        if (p2 == null)
        {
            throw new IllegalArgumentException("p2 can't be null");
        }
        double dx= this.X- p2.getX();
        double dy = this.Y- p2.getY();

        return (Math.sqrt(dx*dx + dy*dy));
    }

    @Override
    public String toString() {

        // NOTE (English): Return coordinates in the exact format expected by
        // other parts of the project (for example, Ex3Algo.stringToPixel)
        // which parse a string in the form "x,y". Previously this method
        // returned "(x,y)", causing Integer.parseInt to fail when the
        // surrounding parentheses were present. Returning "x,y" ensures
        // consistent, parse-friendly output.
        return this.X + "," + this.Y;
    }

    @Override
    public boolean equals(Object p) {

        if (!(p instanceof Pixel2D p1)) return false;
        return this.X == p1.getX() && this.Y == p1.getY();
    }


}
