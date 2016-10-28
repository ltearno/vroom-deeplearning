package fr.lteconsulting.vroom;

public class Move
{
	public final Coordinate coordinate;
	public final Direction direction;

	public Move( int x, int y, Direction direction )
	{
		this( new Coordinate( x, y ), direction );
	}

	public Move( Coordinate coordinate, Direction direction )
	{
		this.coordinate = coordinate;
		this.direction = direction;
	}

	@Override
	public String toString()
	{
		return "Move [" + coordinate + ", " + direction + "]";
	}
}