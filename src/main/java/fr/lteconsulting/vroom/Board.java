package fr.lteconsulting.vroom;

import static fr.lteconsulting.vroom.Piece.Nothing;

public class Board
{
	public final int width;
	public final int height;

	public final Piece[] board;

	public Board( int width, int height )
	{
		this.width = width;
		this.height = height;

		int size = width * height;
		board = new Piece[size];
		for( int i = 0; i < size; i++ )
			board[i] = Nothing;
	}

	public Board duplicate()
	{
		Board res = new Board( width, height );
		int size = width * height;
		for( int i = 0; i < size; i++ )
			res.board[i] = board[i];
		return res;
	}

	public void switchPlayer()
	{
		int size = width * height;
		for( int i = 0; i < size; i++ )
		{
			switch( board[i] )
			{
				case MeRound:
					board[i] = Piece.OtherRound;
					break;
				case MeSquare:
					board[i] = Piece.OtherSquare;
					break;
				case OtherRound:
					board[i] = Piece.MeRound;
					break;
				case OtherSquare:
					board[i] = Piece.MeSquare;
					break;
				default:
			}
		}
	}

	public Board recenter( Coordinate center, int nbAround, int nbUsed )
	{
		int size = 2 * nbAround + 1;
		Board res = new Board( size, size );
		for( int i = -nbUsed; i <= nbUsed; i++ )
		{
			for( int j = -nbUsed; j <= nbUsed; j++ )
			{
				Coordinate coor = new Coordinate( center.x + i, center.y + j );
				if( isCoordinateValid( coor ) )
				{
					Piece piece = get( coor );
					res.set( nbAround + i, nbAround + j, piece );
				}
			}
		}
		return res;
	}

	public Piece set( Coordinate coordinate, Piece piece )
	{
		return set( coordinate.x, coordinate.y, piece );
	}

	public Piece set( int x, int y, Piece piece )
	{
		if( piece == null )
			piece = Nothing;

		Piece before = board[y * width + x];
		board[y * width + x] = piece;
		return before;
	}

	public Piece get( Coordinate coordinate )
	{
		return get( coordinate.x, coordinate.y );
	}

	public Piece get( int x, int y )
	{
		return board[y * width + x];
	}

	public boolean isCoordinateValid( Coordinate coordinate )
	{
		return coordinate.x >= 0 && coordinate.x < width && coordinate.y >= 0 && coordinate.y < height;
	}

	public Coordinate next( Coordinate coordinate, Direction direction )
	{
		coordinate = coordinate.next( direction );
		if( coordinate == null || !isCoordinateValid( coordinate ) )
			return null;

		return coordinate;
	}
}