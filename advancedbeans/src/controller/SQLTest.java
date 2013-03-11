package controller;

//True - Test is hazardous 
//false safe
public class SQLTest {
	
	public static  boolean test (String query)
	{
		if (query== null)
		{
			return false;
		}
		else if (query.length() > 128)
		{
			return true;
			
		}
		else if (query.contains("javascript"))
		{
			return true;
			
		}
		else if (query.contains("drop") && query.contains("database"))
		{
			return true;
			
		}
		else if (query.contains("drop") && query.contains("table"))
		{
			return true;
			
		}
		else if (query.contains("*") && query.contains("select") )
		{
			return true;
			
		}
		else if (query.contains("<") || query.contains(">") )
		{
			return true;
			
		}
		else if (query.contains("</"))
		{
			return true;
			
		}
		
		return false;
	}

}
