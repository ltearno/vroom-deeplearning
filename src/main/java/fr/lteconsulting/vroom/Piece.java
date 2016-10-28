package fr.lteconsulting.vroom;

public enum Piece
{
	Nothing( " ", " " ),
	Border( "#", "#" ),
	OtherSquare( "B", "W" ),
	OtherRound( "b", "w" ),
	MeSquare( "W", "B" ),
	MeRound( "w", "b" );

	public final String display;
	public final String displayOpposite;

	private Piece( String display, String displayOpposite )
	{
		this.display = display;
		this.displayOpposite = displayOpposite;
	}
}