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

public class PlayerNetwork
{
	public int totalLearntSamples = 0;
	public float lastAccuracy = -1;
	public float lastPrecision = -1;

	public final int inputLayerSize = 11;//27;// 11;
	private final int convolutionSize = 5;
	private final int convolutionDepth = 20;
	private final int iterations = 1;

	private MultiLayerNetwork model;

	public static class Feedback
	{
		public final Board board;
		public final Coordinate position;
		public final float[] estimates;

		public Feedback( Board board, Coordinate position, float[] estimates )
		{
			this.board = board;
			this.position = position;
			this.estimates = estimates;
		}
	}

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
				.layer( 1, new DenseLayer.Builder().activation( "relu" ).nOut( 250 ).build() )
				.layer( 2, new OutputLayer.Builder( LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD ).nOut( 8 ).activation( "softmax" ).build() ) //softmax
				.backprop( true )
				.pretrain( false );
		//@formatter:on

		new ConvolutionLayerSetup( builder, inputLayerSize, inputLayerSize, Vroom.NbDimensions );

		MultiLayerConfiguration conf = builder.build();
		model = new MultiLayerNetwork( conf );
		model.init();

		model.setListeners( new ScoreIterationListener( 1 ) );
	}

	public float[] whatDoYouThink( Board board, Coordinate position )
	{
		INDArray input = Nd4j.create( new int[] { 1, Vroom.NbDimensions, inputLayerSize, inputLayerSize } );
		boardToFeature( board, position, -1, input, 0 );

		DataSet ds = new DataSet( input, Nd4j.zeros( 1, 8 ) );
		INDArray output = model.output( ds.getFeatureMatrix(), TrainingMode.TEST );

		float[] res = extractLabels( output, 0 );

		return res;
	}

	public void learnFromFeedbacks( List<Feedback> feedbacks )
	{
		int nbFeedbacks = feedbacks.size();

		INDArray input = Nd4j.create( new int[] { nbFeedbacks, Vroom.NbDimensions, inputLayerSize, inputLayerSize } );
		INDArray trainLabels = Nd4j.create( nbFeedbacks, 8 );

		for( int i = 0; i < nbFeedbacks; i++ )
		{
			Feedback feedback = feedbacks.get( i );

			boardToFeature( feedback.board, feedback.position, -1, input, i );

			Vroom.normalizeSoftMax( feedback.estimates );

			injectLabels( trainLabels, i, feedback.estimates );
		}

		// make it learn that
		DataSet ds = new DataSet( input, trainLabels );
		model.fit( ds );

		totalLearntSamples += feedbacks.size();
	}

	public float evaluate( List<Feedback> feedbacks )
	{
		int nbFeedbacks = feedbacks.size();

		INDArray input = Nd4j.create( new int[] { nbFeedbacks, Vroom.NbDimensions, inputLayerSize, inputLayerSize } );
		INDArray trainLabels = Nd4j.create( nbFeedbacks, 8 );

		for( int i = 0; i < nbFeedbacks; i++ )
		{
			Feedback feedback = feedbacks.get( i );
			boardToFeature( feedback.board, feedback.position, -1, input, i );
			Vroom.normalizeSoftMax( feedback.estimates );
			injectLabels( trainLabels, i, feedback.estimates );
		}

		Evaluation<?> eval = new Evaluation<>( 8 );
		INDArray output = model.output( input, TrainingMode.TEST );
		eval.eval( trainLabels, output );

		System.out.println( "Evaluation stats :" );
		System.out.println( eval.stats() );

		lastAccuracy = (float) eval.accuracy();
		lastPrecision = (float) eval.precision();

		return lastAccuracy;
	}

	private void boardToFeature( Board board, Coordinate coord, int nbUsed, INDArray feature, int sampleIndex )
	{
		int half = (inputLayerSize - 1) / 2;
		if( nbUsed >= 0 )
			half = Math.min( half, nbUsed );

		float[] zero = new float[Vroom.NbDimensions];

		for( int x = -half; x <= half; x++ )
			for( int y = -half; y <= half; y++ )
			{
				float[] pixel;
				if( !board.isCoordinateValid( new Coordinate( coord.x + x, coord.y + y ) ) )
					pixel = zero;
				else
					pixel = Vroom.getPositionVector( board, coord.x + x, coord.y + y );

				for( int channel = 0; channel < Vroom.NbDimensions; channel++ )
					feature.putScalar( new int[] { sampleIndex, channel, x + half, y + half }, pixel[channel] );
			}
	}

	private float[] extractLabels( INDArray array, int indexSample )
	{
		float[] res = new float[8];
		for( int direction = 0; direction < 8; direction++ )
			res[direction] = array.getFloat( new int[] { indexSample, direction } );
		return res;
	}

	private void injectLabels( INDArray array, int indexSample, float[] values )
	{
		for( int direction = 0; direction < 8; direction++ )
			array.putScalar( new int[] { indexSample, direction }, values[direction] );
	}
}
