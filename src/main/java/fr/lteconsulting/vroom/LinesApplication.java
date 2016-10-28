package fr.lteconsulting.vroom;

import java.util.ArrayList;
import java.util.List;

import fr.lteconsulting.vroom.PlayerNetwork.Feedback;

public class LinesApplication
{
	PlayerNetwork net;

	public void run()
	{
		int batchSize = 100;

		net = new PlayerNetwork();
		net.init();

		int l = 0;
		while( true )
		{
			l++;

			System.out.println( "Train the network..." );
			List<Feedback> feedbacks = makeSomeTestData( batchSize );
			net.learnFromFeedbacks( feedbacks );

			System.out.println( "Preparing test data..." );
			List<Feedback> referenceSet = makeSomeTestData( 500 );

			System.out.println( "Testing the network" );
			float score = net.evaluate( referenceSet );
			System.out.println( "evaluation " + l + " score : " + score );

			for( int n = 0; n < 1; n++ )
			{
				Board r = random();
				if( r != null )
				{
					Vroom.printBoard( r );
					float[] res = net.whatDoYouThink( r, new Coordinate( (net.inputLayerSize - 1) / 2, (net.inputLayerSize - 1) / 2 ) );
					System.out.println( res[0] + " ; " + res[1] + " ; " + res[2] + " ; " + res[3] );
				}
			}
		}
	}

	private List<Feedback> makeSomeTestData( int nb )
	{
		List<Feedback> feedbacks = new ArrayList<>();

		while( feedbacks.size() < nb )
		{
			Board board = horizontal();
			if( board != null )
			{
				// Vroom.printBoard( board );

				feedbacks.add( new Feedback( board, new Coordinate( (net.inputLayerSize - 1) / 2, (net.inputLayerSize - 1) / 2 ), new float[] { 1, 0, 0, 0, 0, 0, 0, 0 } ) );
			}

			board = vertical();
			if( board != null )
			{
				// Vroom.printBoard( board );

				feedbacks.add( new Feedback( board, new Coordinate( (net.inputLayerSize - 1) / 2, (net.inputLayerSize - 1) / 2 ), new float[] { 0, 1, 0, 0, 0, 0, 0, 0 } ) );
			}

			board = diagonal();
			if( board != null )
			{
				// Vroom.printBoard( board );

				feedbacks.add( new Feedback( board, new Coordinate( (net.inputLayerSize - 1) / 2, (net.inputLayerSize - 1) / 2 ), new float[] { 0, 0, 1, 0, 0, 0, 0, 0 } ) );
			}

			board = square();
			if( board != null )
			{
				// Vroom.printBoard( board );

				feedbacks.add( new Feedback( board, new Coordinate( (net.inputLayerSize - 1) / 2, (net.inputLayerSize - 1) / 2 ), new float[] { 0, 0, 0, 1, 0, 0, 0, 0 } ) );
			}
		}

		return feedbacks;
	}

	private Board random()
	{
		boolean done = false;
		Board board = new Board( net.inputLayerSize, net.inputLayerSize );

		int nb = (int) (3 * Math.random());
		for( int i = 0; i < nb; i++ )
		{
			int dice = rand( 0, 4 );
			switch( dice )
			{
				case 0:
					done = hLine( done, board );
					break;
				case 1:
					done = vLine( done, board );
					break;
				case 2:
					done = dLine( done, board );
					break;
				case 3:
					return square();
			}
		}

		return done ? board : null;
	}

	private Board horizontal()
	{
		boolean done = false;
		Board board = new Board( net.inputLayerSize, net.inputLayerSize );

		int nb = (int) (5 * Math.random());
		for( int i = 0; i < nb; i++ )
		{
			done = hLine( done, board );
		}

		return done ? board : null;
	}

	private boolean hLine( boolean done, Board board )
	{
		int len = rand( 4, board.width );
		int x = (int) ((board.width - len) * Math.random());
		int y = (int) (board.height * Math.random());

		for( int j = 0; j < len; j++ )
		{
			board.set( x + j, y, Piece.Border );
			done = true;
		}
		return done;
	}

	private Board vertical()
	{
		boolean done = false;
		Board board = new Board( net.inputLayerSize, net.inputLayerSize );

		int nb = (int) (5 * Math.random());
		for( int i = 0; i < nb; i++ )
		{
			done = vLine( done, board );
		}

		return done ? board : null;
	}

	private boolean vLine( boolean done, Board board )
	{
		int len = rand( 4, board.height );
		int y = (int) ((board.height - len) * Math.random());
		int x = (int) (board.width * Math.random());

		for( int j = 0; j < len; j++ )
		{
			board.set( x, y + j, Piece.Border );
			done = true;
		}
		return done;
	}

	private Board diagonal()
	{
		boolean done = false;
		Board board = new Board( net.inputLayerSize, net.inputLayerSize );

		int nb = (int) (5 * Math.random());
		for( int i = 0; i < nb; i++ )
		{
			done = dLine( done, board );
		}

		return done ? board : null;
	}

	private boolean dLine( boolean done, Board board )
	{
		int len = rand( 4, board.width );
		int y = rand( 0, board.height - len );
		int x = rand( 0, board.width - len );

		for( int j = 0; j < len; j++ )
		{
			board.set( x + j, y + j, Piece.Border );
			done = true;
		}
		return done;
	}

	private Board square()
	{
		boolean done = false;
		Board board = new Board( net.inputLayerSize, net.inputLayerSize );

		int nb = (int) (3 * Math.random());
		for( int i = 0; i < nb; i++ )
		{
			int len = Math.min( 4, rand( 3, board.width ) );
			int y = rand( 0, board.height - len );
			int x = rand( 0, board.width - len );

			for( int j = 0; j < len; j++ )
			{
				board.set( x, y + j, Piece.Border );
				board.set( x + len - 1, y + j, Piece.Border );
				board.set( x + j, y, Piece.Border );
				board.set( x + j, y + len - 1, Piece.Border );

				done = true;
			}
		}

		return done ? board : null;
	}

	private int rand( int min, int max )
	{
		return min + (int) ((max - min) * Math.random());
	}
}
