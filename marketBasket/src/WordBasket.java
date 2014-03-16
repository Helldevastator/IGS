import java.util.Arrays;
import java.util.List;

public class WordBasket
{
  public String[] words;
  
  public WordBasket(String[] _words)
  {
    words=_words;
    Arrays.sort(words);
  }
  
  public WordBasket(String[] _words,boolean sorted)
  {
    words=_words;
     if(!sorted) Arrays.sort(words);
  }
  
  public WordBasket(List<String> _words)
  {
    this(_words.toArray(new String[0]));
  }
  
  public WordBasket(String _word)
  {
    this(new String[]{_word});
  }
  
  @Override
public boolean equals(Object _o)
  {
    if(!(_o instanceof WordBasket))
      return false;
    
    return Arrays.equals(words,((WordBasket)_o).words);
  }
  
  @Override
public int hashCode()
  {
	  return Arrays.deepHashCode(words);
  }
  
  public boolean contains(String _word)
  {
    return Arrays.binarySearch(words,_word)>=0;
  }
  
  public boolean containsAll(WordBasket _words)
  {
    for(String word:_words.words)
      if(!contains(word))
        return false;
    return true;
  }
  
}