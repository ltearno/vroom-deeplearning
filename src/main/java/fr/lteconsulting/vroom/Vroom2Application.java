package fr.lteconsulting.vroom;

import java.util.ArrayList;
import java.util.List;

import fr.lteconsulting.vroom.EvaluationNetwork.Feedback;

public class Vroom2Application
{
	private final int learnThreshold = 100;

	private EvaluationNetwork net;
	private List<Feedback> feedbacks = new ArrayList<>();

	private void tests()
	{
	}

	public void run()
	{
		System.out.println( "Vroom learning application" );

		net = new EvaluationNetwork();
		net.init();

		tests();

		int gameNo = 0;
		int[] billBoard = new int[2];

		while( true )
		{
			boolean printGame = gameNo % 20 == 0;

			gameNo++;

			int moveNo = 0;

			List<Board> boardHistory = new ArrayList<>();
			for( int i = 0; i < net.historySize - 1; i++ )
				boardHistory.add( null );

			// add starting board
			boardHistory.add( Vroom.createNormalStartingBoard() );

			System.out.println( "new game: " + gameNo );

			boolean turn = false; // false = white, true = black

			// play a move while I am alive
			while( !Vroom.isFinished( boardHistory.get( boardHistory.size() - 1 ) ) )
			{
				Board board = boardHistory.get( boardHistory.size() - 1 );

				if( printGame )
				{
					// current evaluation
					Board[] currentPositionEvaluationBoard = new Board[net.historySize];
					for( int i = 0; i < net.historySize; i++ )
						currentPositionEvaluationBoard[i] = boardHistory.get( i );
					float[] currentPositionEvaluations = net.whatDoYouThink( currentPositionEvaluationBoard );

					StringBuilder sb = new StringBuilder();
					for( int i = 0; i < net.historySize; i++ )
					{
						if( i > 0 )
							sb.append( ", " );
						sb.append( currentPositionEvaluations[i] );
					}

					Vroom.printBoard( board, turn );
					System.out.println( "To " + player( turn ) + " position eval: " + sb.toString() );
				}

				Pair<Float, Move> bestChoice = findBestMove( boardHistory, board );
				float bestScore = bestChoice.first;
				Move bestMove = bestChoice.last;

				if( printGame )
				{
					System.out.println( "game " + gameNo + " move " + moveNo + " " + player( turn ) + " chooses move (score:" + bestScore + ") " + bestMove );
					System.out.println( net.totalLearntSamples + " learnt, " + feedbacks.size() + " feedbacks ready, accuracy/precision: " + net.lastAccuracy + " / " + net.lastPrecision );
					System.out.println( "billboard : " + billBoard[0] + " vs " + billBoard[1] );
				}

				// execute move
				Board nextBoard = Vroom.doMove( board, bestMove );

				// push to history
				moveNo++;
				boardHistory.add( nextBoard );
				boardHistory.remove( 0 );

				if( Vroom.isFinished( nextBoard ) )
				{
					if( Vroom.isLooser( nextBoard ) )
					{
						if( printGame )
							System.out.println( player( turn ) + " just committed a suicide !" );
					}
					else if( Vroom.isWinner( nextBoard ) )
					{
						if( printGame )
							System.out.println( player( turn ) + " wins !" );
					}
					else
					{
						throw new IllegalStateException();
					}
				}
				else
				{
					nextBoard.switchPlayer();
					turn = !turn;
				}

				maybeLearn();
			}

			System.out.println( "game finished with " + moveNo + " moves" );

			billBoard[Vroom.isLooser( boardHistory.get( boardHistory.size() - 1 ) ) ? 0 : 1]++;
		}

	}

	private Pair<Float, Move> findBestMove( List<Board> boardHistory, Board board )
	{
		MaxChooser<Move> chooser = new MaxChooser<>();

		List<Move> possibleMoves = Vroom.possibleMoves( board );
		for( Move possibleMove : possibleMoves )
		{
			maybeLearn();

			Board[] evaluationBoards = new Board[net.historySize];

			for( int i = 1; i < net.historySize; i++ )
				evaluationBoards[i - 1] = boardHistory.get( i );

			Board testBoard = Vroom.doMove( board, possibleMove );

			evaluationBoards[net.historySize - 1] = testBoard;

			float[] evaluations = net.whatDoYouThink( evaluationBoards );

			boolean learn = false;
			if( Vroom.isLooser( testBoard ) )
			{
				evaluations[net.historySize - 1] = 0;
				learn = true;
			}
			else if( Vroom.isWinner( testBoard ) )
			{
				evaluations[net.historySize - 1] = 1;
				learn = true;
			}

			float moveScore = evaluations[net.historySize - 1];

			chooser.add( new Pair<Float, Move>( (float) (int) (moveScore * 100), possibleMove ) );

			if( learn )
				feedbacks.add( new Feedback( evaluationBoards, evaluations ) );
		}

		Pair<Float, Move> bestChoice = chooser.get();
		return bestChoice;
	}

	private String player( boolean turn )
	{
		return(turn ? "BLACK" : "WHITE");
	}

	private void maybeLearn()
	{
		if( feedbacks == null || feedbacks.size() < learnThreshold )
			return;

		System.out.println( "learning from " + feedbacks.size() + " feedbacks..." );

		// for( Feedback f : feedbacks )
		// {
		// System.out.println( "FEEDBACK : " + f.evaluations[net.historySize - 1] );
		// Vroom.printBoard( f.boards[net.historySize - 1] );
		// }

		net.learnFromFeedbacks( feedbacks );

		System.out.println( "learning session finished" );

		feedbacks.clear();
	}
}
