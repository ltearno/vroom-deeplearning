package fr.lteconsulting.vroom;

public class Coordinate
{
	public final int x;
	public final int y;

	public Coordinate( int x, int y )
	{
		this.x = x;
		this.y = y;
	}

	public Coordinate next( Direction direction )
	{
		switch( direction )
		{
			case North:
				return new Coordinate( x, y + 1 );
			case NorthEast:
				return new Coordinate( x + 1, y + 1 );
			case East:
				return new Coordinate( x + 1, y );
			case SouthEast:
				return new Coordinate( x + 1, y - 1 );
			case South:
				return new Coordinate( x, y - 1 );
			case SouthWest:
				return new Coordinate( x - 1, y - 1 );
			case West:
				return new Coordinate( x - 1, y );
			case NorthWest:
				return new Coordinate( x - 1, y + 1 );
		}

		throw new IllegalArgumentException();
	}

	@Override
	public String toString()
	{
		return "[x=" + x + ", y=" + y + "]";
	}
}