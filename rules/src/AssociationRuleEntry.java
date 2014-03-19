
public class AssociationRuleEntry
{
  public WordBasket lhs;
  public WordBasket rhs;
  public double supportLHS_RHS;
  public double supportLHS;
  
  public AssociationRuleEntry(WordBasket _lhs,WordBasket _rhs,double _supportLHS_RHS,double _supportLHS)
  {
    lhs=_lhs;
    rhs=_rhs;
    supportLHS_RHS=_supportLHS_RHS;
    supportLHS=_supportLHS;
  }
  
  public double getConfidence()
  {
    return supportLHS_RHS/supportLHS;
  }
}
