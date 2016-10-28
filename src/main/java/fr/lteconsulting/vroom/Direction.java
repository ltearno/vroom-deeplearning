package fr.lteconsulting.vroom;

public enum Direction
{
	North( false ),
	NorthEast( true ),
	East( false ),
	SouthEast( true ),
	South( false ),
	SouthWest( true ),
	West( false ),
	NorthWest( true );

	public final boolean isDiagonal;
	public final boolean isOrthogonal;

	private Direction( boolean isDiagonal )
	{
		this.isDiagonal = isDiagonal;
		this.isOrthogonal = !isDiagonal;
	}
}