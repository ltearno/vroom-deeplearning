package fr.lteconsulting.vroom;

import java.util.ArrayList;
import java.util.List;

import fr.lteconsulting.vroom.PlayerNetwork.Feedback;

public class VroomApplication
{
	float randomMoveProbability = 0.0f;

	int learningBatchSize = 100;
	final int learntSampleThresholdForNN = 0;

	boolean useWins = true;
	boolean useLosses = true;
	boolean useNeutrals = false;

	int nbWins = 0;
	int nbLosses = 0;
	int nbNeutrals = 0;

	PlayerNetwork network;

	int nbGamesPlayed = 0;
	int nbMovesPlayed = 0;
	int nbRandomMoves = 0;
	int nbNNMoves = 0;

	public void run()
	{
		prepareNeuralNetwork();

		List<Feedback> feedbacks = new ArrayList<>();

		// create a list of feedbacks of known moves
		Board squarePieceBoard = new Board( 1, 1 );
		squarePieceBoard.set( 0, 0, Piece.MeSquare );
		Board roundPieceBoard = new Board( 1, 1 );
		roundPieceBoard.set( 0, 0, Piece.MeRound );
		for( int i = 0; i < learningBatchSize; i++ )
		{
			feedbacks.add( new Feedback( squarePieceBoard, new Coordinate( 0, 0 ), new float[] { 1, 0, 1, 0, 1, 0, 1, 0 } ) );
			feedbacks.add( new Feedback( roundPieceBoard, new Coordinate( 0, 0 ), new float[] { 0, 1, 0, 1, 0, 1, 0, 1 } ) );
		}

		long lastFeedbackTime = 0;

		boolean finished = false;
		float neuralConfidence = -1;
		int iter = 0;
		while( !finished )
		{
			iter++;

			if( feedbacks.size() >= learningBatchSize )
			{
				System.out.println( "learning..." );
				network.learnFromFeedbacks( feedbacks );
				feedbacks = new ArrayList<>();
			}

			long currentTime = System.currentTimeMillis();
			if( currentTime - lastFeedbackTime > 2500 )
			{
				lastFeedbackTime = currentTime;

				float[] squarePieceTest = network.whatDoYouThink( squarePieceBoard, new Coordinate( 0, 0 ) );
				printLabels( squarePieceTest, "Square piece test" );

				System.out.println( "### ITERATION " + iter );
				System.out.println( "average game length : " + (nbGamesPlayed > 0 ? nbMovesPlayed / nbGamesPlayed : 0) );
				System.out.println( "random / nn moves : " + nbRandomMoves + " / " + nbNNMoves );
				System.out.println( "wins / losses / neutral : " + nbWins + " / " + nbLosses + " / " + nbNeutrals );
				System.out.println( "current feedbacks : " + feedbacks.size() );
				System.out.println( "learned samples : " + network.totalLearntSamples );
				if( neuralConfidence >= 0 )
					System.out.println( "neural confidence : " + neuralConfidence );
				if( network.lastPrecision >= 0 )
					System.out.println( "last precision : " + network.lastPrecision );
				if( network.lastAccuracy >= 0 )
					System.out.println( "last accuracy : " + network.lastAccuracy );

				nbGamesPlayed = 0;
				nbMovesPlayed = 0;
			}

			Board board;
			do
			{
				// board = Vroom.createRandomStartingBoard();
				board = Vroom.createNormalStartingBoard();
			}
			while( Vroom.isLooser( board ) );
			// Vroom.printBoard( board );

			boolean turn = false; // false == white
			int nbMoves = 0;
			while( true )
			{
				if( Vroom.isLooser( board ) )
					break;

				nbMoves++;

				if( nbMoves % 100 == 0 )
				{
					// System.out.println( "play in progress, here is the current board..." );
					// Vroom.printBoard( board );
				}

				Board boardBefore = board;

				MaxChooser<Pair<Move, float[]>> chooser = new MaxChooser<>();

				List<Coordinate> pieces = Vroom.possiblePieces( board );
				for( Coordinate piece : pieces )
				{
					float[] likes = network.whatDoYouThink( board, piece );
					for( int d = 0; d < 8; d++ )
					{
						Direction direction = Direction.values()[d];
						Move move = new Move( piece, direction );
						if( Vroom.canDoMove( board, move ) )
							chooser.add( new Pair<Float, Pair<Move, float[]>>( likes[d], new Pair<Move, float[]>( move, likes ) ) );
					}
				}

				// take the x best moves and test them

				Pair<Float, Pair<Move, float[]>> result = chooser.get();
				neuralConfidence = result.first;
				Move move = result.last.first;
				float[] likes = result.last.last;

				if( nbNNMoves % 500 == 0 )
				{
					System.out.println( "sample of a played neural with score " + result );
					Board rec = board.recenter( move.coordinate, (network.inputLayerSize - 1) / 2, (network.inputLayerSize - 1) / 2 );
					System.out.println( "Direction : " + move.direction );
					Vroom.printBoard( rec );
				}

				nbNNMoves++;

				assert move != null;

				board = Vroom.doMove( board, move );

				if( Vroom.isLooser( board ) )
				{
					// System.out.println( "Bad move: " + move );
					if( useLosses )
					{
						nbLosses++;
						likes[move.direction.ordinal()] = 0;
						feedbacks.add( new Feedback( boardBefore, move.coordinate, likes ) );
					}
				}
				else if( Vroom.isWinner( board ) )
				{
					// System.out.println( "Good move: " + move );
					if( useWins )// && nbWins < (nbLosses + nbNeutrals) / 2 + 10 )
					{
						nbWins++;
						likes[move.direction.ordinal()] = 1;
						feedbacks.add( new Feedback( boardBefore, move.coordinate, likes ) );
					}
				}
				else
				{
					// at least the player is still alive
					if( useNeutrals )// && nbNeutrals < (nbLosses + nbWins) / 2 + 10 )
					{
						nbNeutrals++;
						likes[move.direction.ordinal()] = 0.15f;
						feedbacks.add( new Feedback( boardBefore, move.coordinate, likes ) );
					}
				}

				nbGamesPlayed++;
				nbMovesPlayed += nbMoves;

				board.switchPlayer();
				turn = !turn;
			}
		}
	}

	private void prepareNeuralNetwork()
	{
		if( network != null )
			return;

		network = new PlayerNetwork();
		network.init();
	}

	private void printLabels( float[] labels, String caption )
	{
		StringBuilder sb = new StringBuilder();
		sb.append( caption );
		sb.append( "\n" );
		for( int i = 0; i < 8; i++ )
		{
			sb.append( Direction.values()[i] );
			sb.append( " = " );
			sb.append( labels[i] );
			sb.append( "\n" );
		}

		System.out.println( sb.toString() );
	}
}
