package fr.lteconsulting.vroom;

public class Pair<U, V>
{
	public final U first;
	public final V last;

	public Pair( U first, V last )
	{
		this.first = first;
		this.last = last;
	}

	@Override
	public String toString()
	{
		return "Pair [first=" + first + ", last=" + last + "]";
	}
}