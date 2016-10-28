package fr.lteconsulting.vroom;

import static fr.lteconsulting.vroom.Piece.OtherRound;
import static fr.lteconsulting.vroom.Piece.OtherSquare;
import static fr.lteconsulting.vroom.Piece.Border;
import static fr.lteconsulting.vroom.Piece.Nothing;
import static fr.lteconsulting.vroom.Piece.MeRound;
import static fr.lteconsulting.vroom.Piece.MeSquare;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Vroom
{
	public static final int NbDimensions = 5;

	public static Board emptyBoard()
	{
		return new Board( 9, 15 );
	}

	public static void addBorder( Board board )
	{
		for( int i = 0; i < board.width; i++ )
		{
			board.set( i, 0, Border );
			board.set( i, board.height - 1, Border );
		}

		for( int i = 1; i < board.height - 1; i++ )
		{
			board.set( 0, i, Border );
			board.set( board.width - 1, i, Border );
		}
	}

	public static Board createNormalStartingBoard()
	{
		Board board = new Board( 9, 15 );

		addBorder( board );

		for( int i = 1; i < 8; i += 2 )
		{
			board.set( i, 1, OtherSquare );
			board.set( i, 13, MeSquare );
		}

		for( int i = 2; i < 8; i += 2 )
		{
			board.set( i, 1, OtherRound );
			board.set( i, 13, MeRound );
		}

		board.set( 2, 7, Border );
		board.set( 4, 7, Border );
		board.set( 6, 7, Border );

		return board;
	}

	public static Board createRandomStartingBoard()
	{
		Board board = new Board( 9, 15 );

		addBorder( board );

		for( int i = 1; i < 8; i += 2 )
		{
			board.set( (int) (1 + 7 * Math.random()), (int) (1 + 8 * Math.random()), OtherSquare );
			board.set( (int) (1 + 7 * Math.random()), (int) (13 - 8 * Math.random()), MeSquare );
		}

		for( int i = 2; i < 8; i += 2 )
		{
			board.set( (int) (1 + 7 * Math.random()), (int) (1 + 8 * Math.random()), OtherRound );
			board.set( (int) (1 + 7 * Math.random()), (int) (13 - 8 * Math.random()), MeRound );
		}

		board.set( 2, 7, Border );
		board.set( 4, 7, Border );
		board.set( 6, 7, Border );

		return board;
	}

	public static Board createMiniStartingBoard()
	{
		Board board = new Board( 7, 7 );

		for( int i = 0; i < 7; i++ )
		{
			board.set( i, 0, Border );
			board.set( i, 6, Border );
			board.set( 0, i, Border );
			board.set( 6, i, Border );
		}

		board.set( 1, 1, Piece.OtherSquare );
		board.set( 2, 1, Piece.OtherRound );
		board.set( 3, 1, Piece.OtherSquare );

		board.set( 1, 3, MeSquare );
		board.set( 2, 3, MeRound );
		board.set( 3, 3, MeSquare );

		return board;
	}

	public static Board createNanoStartingBoard()
	{
		Board board = new Board( 5, 5 );

		for( int i = 0; i < 5; i++ )
		{
			board.set( i, 0, Border );
			board.set( i, 4, Border );
			board.set( 0, i, Border );
			board.set( 4, i, Border );
		}

		board.set( 1, 1, Piece.OtherSquare );
		board.set( 2, 1, Piece.OtherRound );
		board.set( 3, 1, Piece.OtherSquare );

		board.set( 1, 3, MeSquare );
		board.set( 2, 3, MeRound );
		board.set( 3, 3, MeSquare );

		return board;
	}

	public static void printBoard( Board board )
	{
		printBoard( board, false );
	}

	public static void printBoard( Board board, boolean opposite )
	{
		StringBuilder sb = new StringBuilder();
		int centerX = (board.width - 1) / 2 + 1;
		int centerY = (board.height - 1) / 2;
		for( int i = 0; i < board.width + 2; i++ )
			if( i == centerX )
				sb.append( "|" );
			else
				sb.append( "*" );
		sb.append( "\n" );

		for( int j = board.height - 1; j >= 0; j-- )
		{
			if( j == centerY )
				sb.append( "-" );
			else
				sb.append( "*" );
			for( int i = 0; i < board.width; i++ )
				sb.append( opposite ? board.get( i, j ).displayOpposite : board.get( i, j ).display );
			if( j == centerY )
				sb.append( "-" );
			else
				sb.append( "*" );
			sb.append( "\n" );
		}
		for( int i = 0; i < board.width + 2; i++ )
			if( i == centerX )
				sb.append( "|" );
			else
				sb.append( "*" );

		System.out.println( sb.toString() );
	}

	// me square, me round, other square, other round, border
	public static float[] getPositionVector( Board board, int x, int y )
	{
		float[] pixel = null;

		Piece piece = board.get( x, y );
		switch( piece )
		{
			case OtherSquare:
				pixel = new float[] { 0, 0, 1, 0, 0 };
				break;

			case MeSquare:
				pixel = new float[] { 1, 0, 0, 0, 0 };
				break;

			case OtherRound:
				pixel = new float[] { 0, 0, 0, 1, 0 };
				break;

			case MeRound:
				pixel = new float[] { 0, 1, 0, 0, 0 };
				break;

			case Border:
				pixel = new float[] { 0, 0, 0, 0, 1 };
				break;

			case Nothing:
				pixel = new float[] { 0, 0, 0, 0, 0 };
				break;
		}

		return pixel;
	}

	public static Board doMove( Board board, Move move )
	{
		boolean canDo = canDoMove( board, move );
		if( !canDo )
			throw new IllegalArgumentException( "illegal move !" );

		board = board.duplicate();

		Piece piece = board.get( move.coordinate );
		if( piece == OtherSquare || piece == Piece.MeSquare )
		{
			pushPiece( board, move );
		}
		else if( piece == Piece.OtherRound || piece == Piece.MeRound )
		{
			throwPiece( board, move );
		}

		return board;
	}

	public static void throwPiece( Board board, Move move )
	{
		Piece piece = board.get( move.coordinate );
		if( piece == Piece.Nothing )
			return;

		board.set( move.coordinate, Nothing );

		Coordinate target = null;
		Coordinate next = board.next( move.coordinate, move.direction );
		while( next != null && board.get( next ) == Nothing )
		{
			target = next;
			next = next.next( move.direction );
		}

		if( target != null )
			board.set( target, piece );
	}

	public static void pushPiece( Board board, Move move )
	{
		Piece piece = board.get( move.coordinate );
		if( piece == Piece.Nothing )
			return;

		board.set( move.coordinate, Nothing );

		Coordinate next = move.coordinate.next( move.direction );

		if( board.isCoordinateValid( next ) )
		{
			pushPiece( board, new Move( next, move.direction ) );

			board.set( next, piece );
		}
	}

	public static boolean canDoMove( Board board, Move move )
	{
		Piece piece = board.get( move.coordinate );

		if( move.direction.isOrthogonal )
		{
			if( piece != Piece.MeSquare )
				return false;

			int score = 0;
			Coordinate currentCoordinate = move.coordinate;
			while( currentCoordinate != null )
			{
				Piece current = board.get( currentCoordinate );
				switch( current )
				{
					case Nothing:
						return score > 0;

					case Border:
						return false;

					case OtherSquare:
						score -= 1;
						break;

					case MeSquare:
						score += 1;
						break;

					default:
						break;
				}

				currentCoordinate = board.next( currentCoordinate, move.direction );
			}

			return true;
		}
		else
		{
			if( piece != Piece.MeRound )
				return false;

			Coordinate next = board.next( move.coordinate, move.direction );
			if( next == null )
				return false;

			return board.get( next ) == Nothing;
		}
	}

	public static boolean isLooser( Board board )
	{
		for( int j = 0; j < board.height; j++ )
		{
			for( int i = 0; i < board.width; i++ )
			{
				Coordinate coordinate = new Coordinate( i, j );
				Piece piece = board.get( coordinate );
				if( piece != Piece.MeRound )
					continue;

				if( isBlocked( board, coordinate ) )
					return true;
			}
		}

		return false;
	}

	public static boolean isWinner( Board board )
	{
		for( int j = 0; j < board.height; j++ )
		{
			for( int i = 0; i < board.width; i++ )
			{
				Coordinate coordinate = new Coordinate( i, j );
				Piece piece = board.get( coordinate );
				if( piece != Piece.OtherRound )
					continue;

				if( isBlocked( board, coordinate ) )
					return true;
			}
		}

		return false;
	}

	public static boolean isFinished( Board board )
	{
		for( int j = 0; j < board.height; j++ )
		{
			for( int i = 0; i < board.width; i++ )
			{
				Coordinate coordinate = new Coordinate( i, j );
				Piece piece = board.get( coordinate );
				if( piece != Piece.MeRound && piece != OtherRound )
					continue;

				if( isBlocked( board, coordinate ) )
					return true;
			}
		}

		return false;
	}

	public static boolean isBlocked( Board board, Coordinate coordinate )
	{
		for( Direction direction : Arrays.asList( Direction.NorthEast, Direction.NorthWest, Direction.SouthEast, Direction.SouthWest ) )
		{
			Coordinate next = board.next( coordinate, direction );
			if( next != null && board.get( next ) == Piece.Nothing )
				return false;
		}

		return true;
	}

	public static List<Coordinate> possiblePieces( Board board )
	{
		List<Coordinate> res = new ArrayList<>();

		for( int j = 0; j < board.height; j++ )
		{
			for( int i = 0; i < board.width; i++ )
			{
				Coordinate coordinate = new Coordinate( i, j );
				Piece piece = board.get( coordinate );
				if( piece == MeSquare || piece == Piece.MeRound )
					res.add( coordinate );
			}
		}

		return res;
	}

	public static List<Move> possibleMoves( Board board )
	{
		List<Move> res = new ArrayList<>();

		for( int j = 0; j < board.height; j++ )
		{
			for( int i = 0; i < board.width; i++ )
			{
				for( Direction direction : Direction.values() )
				{
					Move move = new Move( new Coordinate( i, j ), direction );

					if( canDoMove( board, move ) )
						res.add( move );
				}
			}
		}

		return res;
	}

	public static void normalizeSoftMax( float[] vector )
	{
		float sum = 0;
		for( int i = 0; i < vector.length; i++ )
		{
			vector[i] = (float) Math.exp( vector[i] );
			sum += vector[i];
		}

		for( int i = 0; i < vector.length; i++ )
		{
			vector[i] /= sum;
		}
	}
}
