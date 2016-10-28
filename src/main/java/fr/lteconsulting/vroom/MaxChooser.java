package fr.lteconsulting.vroom;

import java.util.ArrayList;
import java.util.List;

public class MaxChooser<T>
{
	List<Pair<Float, T>> best = new ArrayList<>();
	float bestValue = -1;

	void add( Pair<Float, T> pair )
	{
		float value = pair.first;

		if( value > bestValue )
		{
			bestValue = value;
			best.clear();
			best.add( pair );
		}
		else if( value == bestValue )
		{
			best.add( pair );
		}
	}

	Pair<Float, T> get()
	{
		if( best.isEmpty() )
			return null;

		return best.get( (int) (best.size() * Math.random()) );
	}
}