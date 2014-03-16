

public class Item implements Comparable<Item> {
	WordBasket basket;	//is sorted
	int occurences;
	double support;
	
	@Override
	public int hashCode()
	{
		return basket.hashCode();
	}
	
	@Override
	public boolean equals(Object _o) {
		if(!(_o instanceof Item))
		      return false;
		return basket.equals(((Item)_o).basket);
	}
	
	@Override
	public int compareTo(Item other) {
		//always has the same length
		for(int i = 0; i <  basket.words.length;i++) {
			int res = basket.words[i].compareTo(other.basket.words[i]);
			if(res != 0)
				return res;
		}
		
		return 0;
	}
}
