package fr.lteconsulting.vroom;

import java.util.List;

import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.Layer.TrainingMode;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.setup.ConvolutionLayerSetup;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class EvaluationNetwork
{
	public final int historySize = 5;
	public final int inputWidth = 7 + 2;
	public final int inputHeight = 13 + 2;

	public int totalLearntSamples = 0;
	public float lastAccuracy = -1;
	public float lastPrecision = -1;

	private final int convolutionSize = 3;
	private final int convolutionDepth = 20;
	private final int iterations = 1;

	private MultiLayerNetwork model;

	public void init()
	{
		if( model != null )
			return;

		int seed = 123;

		//@formatter:off
		MultiLayerConfiguration.Builder builder = new NeuralNetConfiguration.Builder()
				.seed( seed )
				.iterations( iterations )
				.regularization( true )
				.l2( 0.0005 )
				.learningRate( 0.04 )
				.weightInit( WeightInit.XAVIER )
				.optimizationAlgo( OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT )
				.updater( Updater.NESTEROVS )
				.momentum( 0.9 )
				.list( 3 )
				.layer( 0, new ConvolutionLayer.Builder( convolutionSize, convolutionSize ).nIn( 3 ).nOut( convolutionDepth ).stride( 1, 1 ).dropOut( 0.5 ).activation( "relu" ).build() )
				//.layer( 1, new SubsamplingLayer.Builder( SubsamplingLayer.PoolingType.MAX ).kernelSize( 2, 2 ).stride( 2, 2 ).build() )
				.layer( 1, new DenseLayer.Builder().activation( "relu" ).nOut( 500 ).build() )
				.layer( 2, new OutputLayer.Builder( LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD ).nOut( historySize ).activation( "sigmoid" ).build() ) //softmax
				.backprop( true )
				.pretrain( false );
		//@formatter:on

		new ConvolutionLayerSetup( builder, inputHeight, inputWidth, historySize * Vroom.NbDimensions );

		MultiLayerConfiguration conf = builder.build();
		model = new MultiLayerNetwork( conf );
		model.init();

		model.setListeners( new ScoreIterationListener( 1 ) );
	}

	/**
	 * Evaluates a board history
	 * 
	 * @param boards List that should be 'historySize' long
	 * @return evaluation of each board
	 */
	public float[] whatDoYouThink( Board[] boards )
	{
		INDArray input = Nd4j.zeros( new int[] { 1, historySize * Vroom.NbDimensions, inputHeight, inputWidth } );
		boardsToFeature( boards, input, 0 );

		DataSet ds = new DataSet( input, Nd4j.zeros( 1, historySize ) );

		INDArray output = model.output( ds.getFeatureMatrix(), TrainingMode.TEST );

		float[] res = extractEvaluations( output, 0 );

		return res;
	}

	public static class Feedback
	{
		public final Board[] boards;
		public final float[] evaluations;

		public Feedback( Board[] boards, float[] evaluations )
		{
			this.boards = boards;
			this.evaluations = evaluations;
		}
	}

	public void learnFromFeedbacks( List<Feedback> feedbacks )
	{
		int nbFeedbacks = feedbacks.size();

		INDArray input = Nd4j.zeros( new int[] { nbFeedbacks, historySize * Vroom.NbDimensions, inputHeight, inputWidth } );
		INDArray trainLabels = Nd4j.create( nbFeedbacks, historySize );

		for( int i = 0; i < nbFeedbacks; i++ )
		{
			Feedback feedback = feedbacks.get( i );

			boardsToFeature( feedback.boards, input, i );

			//Vroom.normalizeSoftMax( feedback.evaluations );

			injectLabels( trainLabels, i, feedback.evaluations );
		}

		// make it learn that
		DataSet ds = new DataSet( input, trainLabels );
		model.fit( ds );

		totalLearntSamples += feedbacks.size();
	}

	public float evaluate( List<Feedback> feedbacks )
	{
		int nbFeedbacks = feedbacks.size();

		INDArray input = Nd4j.zeros( new int[] { nbFeedbacks, historySize * Vroom.NbDimensions, inputHeight, inputWidth } );
		INDArray trainLabels = Nd4j.create( nbFeedbacks, historySize );

		for( int i = 0; i < nbFeedbacks; i++ )
		{
			Feedback feedback = feedbacks.get( i );
			boardsToFeature( feedback.boards, input, i );
			//Vroom.normalizeSoftMax( feedback.evaluations );
			injectLabels( trainLabels, i, feedback.evaluations );
		}

		Evaluation<?> eval = new Evaluation<>( historySize );
		INDArray output = model.output( input, TrainingMode.TEST );
		eval.eval( trainLabels, output );

		System.out.println( "Evaluation stats :" );
		System.out.println( eval.stats() );

		lastAccuracy = (float) eval.accuracy();
		lastPrecision = (float) eval.precision();

		return lastAccuracy;
	}

	private void boardsToFeature( Board[] boards, INDArray feature, int sampleIndex )
	{
		for( int b = 0; b < boards.length; b++ )
		{
			Board board = boards[b];
			if( board == null )
				continue;

			for( int x = 0; x < inputWidth; x++ )
			{
				for( int y = 0; y < inputHeight; y++ )
				{
					if( board.isCoordinateValid( new Coordinate( x, y ) ) )
					{
						float[] pixel = Vroom.getPositionVector( board, x, y );

						for( int channel = 0; channel < Vroom.NbDimensions; channel++ )
							feature.putScalar( new int[] { sampleIndex, historySize * b + channel, y, x }, pixel[channel] );
					}
				}
			}
		}
	}

	private float[] extractEvaluations( INDArray array, int indexSample )
	{
		float[] res = new float[historySize];
		for( int history = 0; history < historySize; history++ )
			res[history] = array.getFloat( new int[] { indexSample, history } );
		return res;
	}

	private void injectLabels( INDArray array, int indexSample, float[] values )
	{
		for( int history = 0; history < historySize; history++ )
			array.putScalar( new int[] { indexSample, history }, values[history] );
	}
}
